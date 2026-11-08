package net.osmand.plus.auto

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.HandshakeInfo
import androidx.car.app.OnDoneCallback
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.suggestion.SuggestionManager
import androidx.car.app.testing.SessionController
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.TestScreenManager
import androidx.car.app.versioning.CarAppApiLevels
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.data.ValueHolder
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.auto.screens.LandingScreen
import net.osmand.plus.auto.screens.RoutePreviewScreen
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.search.history.HistoryEntry
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.enums.HistorySource
import net.osmand.util.MapUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class NavigationSessionIntentTest {

	companion object {
		private const val BERLIN_LAT = 52.5163
		private const val BERLIN_LON = 13.3777
		private const val BERLIN_START_LAT = 52.5219
		private const val BERLIN_START_LON = 13.4132
		private const val BERLIN_VIA_LAT = 52.5200
		private const val BERLIN_VIA_LON = 13.3950
		private const val ROUTE_TIMEOUT_SEC = 60L
		private const val START_TIMEOUT_SEC = 15L
		private const val SHORT_COUNTDOWN_MS = 3000L
		private const val LONG_COUNTDOWN_MS = 20_000L
		private const val RECALCULATION_TIMEOUT_SEC = 10L
		private const val START_CLICK_TIMEOUT_SEC = 3L
	}

	private lateinit var app: OsmandApplication
	private lateinit var carContext: TestCarContext
	private lateinit var session: NavigationSession
	private lateinit var controller: SessionController
	private lateinit var recordingManager: CarTestUtils.RecordingSuggestionManager

	private val testPrefix = "25977-test-" + UUID.randomUUID().toString() + "-"
	private val createdDescriptions = mutableListOf<PointDescription>()
	private val testDestinations = listOf(LatLon(BERLIN_LAT, BERLIN_LON))
	private var testStartTimeMs = 0L
	private var savedNavigationHistory: Boolean? = null
	private var savedLastModifiedTime: Long = 0L
	private var historyBefore: Set<String>? = null
	private var targetPointsBackup: CarTestUtils.TargetPointsBackup? = null
	private var countdownMs = RoutePreviewScreen.CARD_COUNTDOWN_MS

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication

		savedNavigationHistory = app.settings.NAVIGATION_HISTORY.get()
		savedLastModifiedTime = app.searchHistoryHelper.lastModifiedTime
		app.settings.NAVIGATION_HISTORY.set(true)

		CarTestUtils.waitForAppInitialization(app)
		testStartTimeMs = System.currentTimeMillis()
		historyBefore = CarTestUtils.navigationHistoryKeys(app)

		CarTestUtils.assumeHistoryHasRoom(app)
		assumeTrue("Car navigation session must not be active before test execution", app.carNavigationSession == null)
		assumeTrue("Android Auto feature must be available", InAppPurchaseUtils.isAndroidAutoAvailable(app))
		assumeTrue("Location permission must be available", OsmAndLocationProvider.isLocationPermissionAvailable(app))

		val routingHelper = app.routingHelper
		val targetPointsHelper = app.targetPointsHelper
		assumeTrue("Destination point must not be set before test execution", targetPointsHelper.pointToNavigate == null)
		assumeTrue("Start point must not be set before test execution", targetPointsHelper.pointToStart == null)
		assumeTrue("Intermediate points must not be set before test execution", targetPointsHelper.intermediatePoints.isEmpty())
		assumeTrue(
			"Navigation must not be active or in planning mode",
			!routingHelper.isFollowingMode && !routingHelper.isPauseNavigation && !routingHelper.isRoutePlanningMode
		)

		targetPointsBackup = CarTestUtils.saveTargetPointsBackup(app)

		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			carContext = TestCarContext.createCarContext(app)
			carContext.updateHandshakeInfo(HandshakeInfo(CarTestUtils.TEST_HOST_PACKAGE, CarAppApiLevels.LEVEL_5))
			recordingManager = CarTestUtils.RecordingSuggestionManager(carContext)
			carContext.overrideCarService(SuggestionManager::class.java, recordingManager)
			session = object : NavigationSession() {
				override fun getCardCountdownMs(): Long = countdownMs
			}
			controller = SessionController(session, carContext, Intent())
			controller.moveToState(Lifecycle.State.CREATED)
			// screens follow the lifecycle of TestCarContext, not of the session: without this they are never created
			carContext.lifecycleOwner.registry.currentState = Lifecycle.State.CREATED
		}

		val screenManager = carContext.getCarService(TestScreenManager::class.java)
		assumeTrue(
			"Initial top screen must be LandingScreen (not purchase or permission screen)",
			CarTestUtils.onMain { screenManager.top } is LandingScreen
		)
	}

	@After
	fun tearDown() {
		try {
			if (::controller.isInitialized) {
				val instrumentation = InstrumentationRegistry.getInstrumentation()
				instrumentation.runOnMainSync {
					val routingHelper = app.routingHelper
					if (routingHelper.isRoutePlanningMode || routingHelper.isRouteCalculated || routingHelper.isFollowingMode) {
						app.stopNavigation()
					}
					// NavigationService ends car navigation only after it stops; the session must not be destroyed while navigating
					session.stopCarNavigation()
				}
				instrumentation.waitForIdleSync()
				instrumentation.runOnMainSync {
					controller.moveToState(Lifecycle.State.DESTROYED)
				}
			}
		} finally {
			val backup = targetPointsBackup
			try {
				if (backup != null) {
					InstrumentationRegistry.getInstrumentation().runOnMainSync {
						val routingHelper = app.routingHelper
						if (routingHelper.isRoutePlanningMode || routingHelper.isRouteCalculated || routingHelper.isFollowingMode) {
							app.stopNavigation()
						}
						app.targetPointsHelper.clearAllPoints(false)
					}
				}
			} finally {
				try {
					if (::app.isInitialized) {
						CarTestUtils.cleanupHistoryEntries(
							app = app,
							testPrefix = testPrefix,
							createdDescriptions = createdDescriptions,
							historyBefore = historyBefore,
							testStartTimeMs = testStartTimeMs,
							testPoints = testDestinations
						)
					}
				} finally {
					try {
						if (backup != null) {
							CarTestUtils.restoreTargetPointsBackup(app, backup)
						}
					} finally {
						if (::app.isInitialized) {
							app.searchHistoryHelper.lastModifiedTime = savedLastModifiedTime
						}
						savedNavigationHistory?.let { saved ->
							app.settings.NAVIGATION_HISTORY.set(saved)
						}
					}
				}
			}
		}
	}

	@Test
	fun suggestionCardIntentPushesRoutePreviewScreen() {
		val testTitle = "${testPrefix}suggestion-card"
		val pd = PointDescription(PointDescription.POINT_TYPE_ADDRESS, testTitle)
		createdDescriptions.add(pd)
		app.searchHistoryHelper.addNewItemToHistory(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION)

		var entry = app.searchHistoryHelper.getEntryByName(pd, HistorySource.NAVIGATION)
		if (entry == null) {
			entry = app.searchHistoryHelper.getHistoryEntries(HistorySource.NAVIGATION, false, true)
				.firstOrNull { historyEntry ->
					historyEntry.name?.name == pd.name
				}
		}
		assertNotNull("History entry must be present in searchHistoryHelper", entry)

		val cardIntent = CarSuggestionsHelper.createCardIntent(app, entry!!)

		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			session.onNewIntent(cardIntent)
		}

		val screenManager = carContext.getCarService(TestScreenManager::class.java)
		val topScreen = CarTestUtils.onMain { screenManager.top }
		assertTrue(
			"Top screen must be RoutePreviewScreen, was: ${topScreen?.javaClass?.simpleName}",
			topScreen is RoutePreviewScreen
		)
		assertTrue(
			"RoutePreviewScreen must be recorded in pushed screens",
			CarTestUtils.onMain { screenManager.screensPushed }.any { screen ->
				screen is RoutePreviewScreen
			}
		)
		val target = CarTestUtils.onMain { app.targetPointsHelper.pointToNavigate }
		assertNotNull("The route preview must set the card destination", target)
		assertTrue(
			"The destination must be the card point, was: ${target!!.latitude}, ${target.longitude}",
			MapUtils.getDistance(target.latitude, target.longitude, BERLIN_LAT, BERLIN_LON) < 1
		)
	}

	@Test
	fun suggestionCardShowsAppOnCarScreen() {
		val pd = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "${testPrefix}show-app")
		createdDescriptions.add(pd)
		val cardIntent = CarSuggestionsHelper.createCardIntent(app, HistoryEntry(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION))
		CarTestUtils.onMain { session.onNewIntent(cardIntent) }
		val appComponent = ComponentName(app, NavigationCarAppService::class.java)
		val intents = CarTestUtils.onMain { carContext.startCarAppIntents.toList() }
		assertTrue(
			"A card tap must show the app on the car screen, was: $intents",
			intents.any { intent -> intent.component == appComponent }
		)
	}

	@Test
	fun previousRouteCardIsFirstWithoutDuplicateDestination() {
		setUpPreviousRoute("previous-route-card")
		resumeAtBerlinStart()
		val destinationPrefix = "$BERLIN_LAT,$BERLIN_LON,"
		assertTrue(
			"The previous route must be published as the first card",
			waitFor(START_TIMEOUT_SEC) {
				recordingManager.publications.lastOrNull()?.firstOrNull()?.identifier?.startsWith(CarSuggestionsHelper.PREVIOUS_ROUTE_ID_PREFIX) == true
			}
		)
		val identifiers = CarTestUtils.onMain { recordingManager.publications.last().map { suggestion -> suggestion.identifier } }
		assertFalse(
			"The previous route destination must not get a second card, was: $identifiers",
			identifiers.any { identifier -> identifier.startsWith(destinationPrefix) }
		)
	}

	@Test
	fun previousRouteCardRestoresStartAndIntermediatePoints() {
		setUpPreviousRoute("previous-route-restore")
		resumeAtBerlinStart()
		val destination = CarTestUtils.onMain { app.targetPointsHelper.pointToNavigateBackup }
		assertNotNull("The previous route must be backed up", destination)
		val cardIntent = CarSuggestionsHelper.createPreviousRouteCardIntent(app, destination!!)
		CarTestUtils.onMain { session.onNewIntent(cardIntent) }

		val preview = CarTestUtils.onMain { carContext.getCarService(TestScreenManager::class.java).top }
		assertTrue("Top screen must be RoutePreviewScreen, was: ${preview?.javaClass?.simpleName}", preview is RoutePreviewScreen)
		assertTrue("A previous route card must get a countdown", CarTestUtils.onMain { (preview as RoutePreviewScreen).hasStartCountdown() })
		val targetPointsHelper = app.targetPointsHelper
		val start = CarTestUtils.onMain { targetPointsHelper.pointToStart }
		val intermediates = CarTestUtils.onMain { targetPointsHelper.intermediatePoints.toList() }
		val target = CarTestUtils.onMain { targetPointsHelper.pointToNavigate }
		assertNotNull("The start point must be restored", start)
		assertTrue("The start point must be restored, was: ${start!!.latLon}", isNear(start.latLon, BERLIN_START_LAT, BERLIN_START_LON))
		assertEquals("The intermediate point must be restored", 1, intermediates.size)
		assertTrue(
			"The intermediate point must be restored, was: ${intermediates[0].latLon}",
			isNear(intermediates[0].latLon, BERLIN_VIA_LAT, BERLIN_VIA_LON)
		)
		assertNotNull("The destination must be restored", target)
		assertTrue("The destination must be restored, was: ${target!!.latLon}", isNear(target.latLon, BERLIN_LAT, BERLIN_LON))
	}

	@Test
	fun previousRouteCardLeavesFourNewestHistoryCards() {
		val destination = setUpPreviousRoute("previous-route-limit")
		val newest = ArrayList<String>()
		for (i in 1..4) {
			if (i == 3) {
				CarTestUtils.addNewestEntry(app, destination, BERLIN_LAT, BERLIN_LON, createdDescriptions)
			}
			val pd = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "${testPrefix}limit-$i")
			CarTestUtils.addNewestEntry(app, pd, BERLIN_LAT + i * 0.001, BERLIN_LON + i * 0.001, createdDescriptions)
			newest.add(pd.name)
		}
		resumeAtBerlinStart()
		assertTrue(
			"The previous route and the newest history entries must be published",
			waitFor(START_TIMEOUT_SEC) {
				val cards = recordingManager.publications.lastOrNull()
				cards != null && cards.size == 5
						&& cards[0].identifier.startsWith(CarSuggestionsHelper.PREVIOUS_ROUTE_ID_PREFIX)
						&& cards[1].title.toString() == newest[3]
			}
		)
		val titles = CarTestUtils.onMain { recordingManager.publications.last().drop(1).map { card -> card.title.toString() } }
		assertEquals(
			"The 4 newest history entries without the previous route destination must follow the previous route",
			newest.reversed(),
			titles
		)
	}

	@Test
	fun noPreviousRouteCardWithoutBackup() {
		CarTestUtils.onMain { app.targetPointsHelper.clearBackupPoints() }
		val pd = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "${testPrefix}no-backup")
		CarTestUtils.addNewestEntry(app, pd, BERLIN_LAT, BERLIN_LON, createdDescriptions)
		resumeAtBerlinStart()
		assertTrue(
			"The newest history entry must be the first card without a previous route",
			waitFor(START_TIMEOUT_SEC) {
				recordingManager.publications.lastOrNull()?.firstOrNull()?.identifier?.contains(pd.name) == true
			}
		)
		val identifiers = CarTestUtils.onMain { recordingManager.publications.last().map { card -> card.identifier } }
		assertFalse(
			"There must be no previous route card without a backup, was: $identifiers",
			identifiers.any { identifier -> identifier.startsWith(CarSuggestionsHelper.PREVIOUS_ROUTE_ID_PREFIX) }
		)
	}

	@Test
	fun noPreviousRouteCardWhenNavigationHistoryIsOff() {
		setUpPreviousRoute("previous-route-history-off")
		app.settings.NAVIGATION_HISTORY.set(false)
		resumeAtBerlinStart()
		assertTrue("The cards must be published", waitFor(START_TIMEOUT_SEC) { recordingManager.publications.isNotEmpty() })
		val identifiers = CarTestUtils.onMain { recordingManager.publications.last().map { card -> card.identifier } }
		assertFalse(
			"There must be no previous route card when navigation history is off, was: $identifiers",
			identifiers.any { identifier -> identifier.startsWith(CarSuggestionsHelper.PREVIOUS_ROUTE_ID_PREFIX) }
		)
	}

	@Test
	fun previousRouteCardOpensPlainPreviewWhenDestinationChanged() {
		setUpPreviousRoute("previous-route-changed")
		val destination = CarTestUtils.onMain { app.targetPointsHelper.pointToNavigateBackup }
		assertNotNull("The previous route must be backed up", destination)
		val cardIntent = CarSuggestionsHelper.createPreviousRouteCardIntent(app, destination!!)
		val newDestination = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "${testPrefix}previous-route-changed-new")
		val newVia = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "${testPrefix}previous-route-changed-via")
		val newStart = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "${testPrefix}previous-route-changed-start")
		createdDescriptions.addAll(listOf(newDestination, newVia, newStart))
		CarTestUtils.onMain {
			val targetPointsHelper = app.targetPointsHelper
			targetPointsHelper.navigateToPoint(LatLon(BERLIN_VIA_LAT, BERLIN_START_LON), false, -1, newDestination)
			targetPointsHelper.navigateToPoint(LatLon(BERLIN_START_LAT, BERLIN_LON), false, 0, newVia)
			targetPointsHelper.setStartPoint(LatLon(BERLIN_LAT, BERLIN_START_LON), false, newStart)
			targetPointsHelper.clearAllPoints(false)
		}
		val intermediatesBackup = app.settings.settingsAPI.getString(
			app.settings.globalPreferences, OsmandSettings.INTERMEDIATE_POINTS_BACKUP, "")
		assertFalse("The changed previous route must have its own intermediate point", intermediatesBackup.isNullOrEmpty())
		resumeAtBerlinStart()
		CarTestUtils.onMain { session.onNewIntent(cardIntent) }

		val preview = CarTestUtils.onMain { carContext.getCarService(TestScreenManager::class.java).top }
		assertTrue("Top screen must be RoutePreviewScreen, was: ${preview?.javaClass?.simpleName}", preview is RoutePreviewScreen)
		val targetPointsHelper = app.targetPointsHelper
		val target = CarTestUtils.onMain { targetPointsHelper.pointToNavigate }
		val intermediates = CarTestUtils.onMain { targetPointsHelper.intermediatePoints.toList() }
		assertNotNull("The card destination must be set", target)
		assertTrue("The card destination must be set, was: ${target!!.latLon}", isNear(target.latLon, BERLIN_LAT, BERLIN_LON))
		assertTrue("The changed previous route must not be restored, was: $intermediates", intermediates.isEmpty())
		val start = CarTestUtils.onMain { targetPointsHelper.pointToStart }
		assertTrue("The changed previous route start must not be used, was: ${start?.latLon}", start == null)
	}

	@Test
	fun countdownWaitsUntilPreviewIsShown() {
		resumeAtBerlinStart()
		CarTestUtils.onMain { carContext.lifecycleOwner.registry.currentState = Lifecycle.State.CREATED }
		val pd = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "${testPrefix}hidden-preview")
		createdDescriptions.add(pd)
		val cardIntent = CarSuggestionsHelper.createCardIntent(app, HistoryEntry(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION))
		CarTestUtils.onMain { session.onNewIntent(cardIntent) }
		val preview = CarTestUtils.onMain { carContext.getCarService(TestScreenManager::class.java).top }
		assertTrue("Top screen must be RoutePreviewScreen, was: ${preview?.javaClass?.simpleName}", preview is RoutePreviewScreen)
		preview as RoutePreviewScreen
		assumeRouteReady()
		Thread.sleep(START_CLICK_TIMEOUT_SEC * 1000)
		assertFalse("The countdown must not run while the preview is not shown", CarTestUtils.onMain { preview.isCountdownRunning })

		resumeAtBerlinStart()
		assertTrue("The countdown must start when the preview is shown", waitFor(START_TIMEOUT_SEC) { preview.isCountdownRunning })
	}

	@Test
	fun suggestionCardShowsPreviewWithCountdownWithoutStartingNavigation() {
		val preview = openCardPreviewWithRoute("countdown-preview")
		assertTrue("The countdown must run after the route is ready", CarTestUtils.onMain { preview.isCountdownRunning })
		assertFalse("Navigation must not start before the countdown expires", CarTestUtils.onMain { app.routingHelper.isFollowingMode })
		val title = startTitle(preview)
		val startText = app.getString(R.string.shared_string_control_start)
		assertTrue("Start must show the remaining seconds, was: $title", title.startsWith("$startText (") && title.endsWith(")"))
	}

	@Test
	fun countdownExpiryStartsNavigation() {
		countdownMs = SHORT_COUNTDOWN_MS
		openCardPreviewWithRoute("countdown-expiry")
		assertTrue(
			"Navigation must start when the countdown expires",
			waitFor(START_TIMEOUT_SEC) { app.routingHelper.isFollowingMode }
		)
	}

	@Test
	fun startDuringCountdownStartsNavigationAtOnce() {
		val preview = openCardPreviewWithRoute("countdown-start")
		val startAction = CarTestUtils.onMain { startAction(preview) }
		startAction.onClickDelegate!!.sendClick(object : OnDoneCallback {})
		assertTrue("Start must start navigation at once", waitFor(START_CLICK_TIMEOUT_SEC) { app.routingHelper.isFollowingMode })
		assertFalse("The countdown must stop after Start", CarTestUtils.onMain { preview.isCountdownRunning })
		assertTrue(
			"The preview must be closed after Start",
			waitFor(START_TIMEOUT_SEC) { !preview.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED) }
		)
		Thread.sleep(countdownDurationMs())
		assertTrue("Navigation must stay active after the countdown time", CarTestUtils.onMain { app.routingHelper.isFollowingMode })
	}

	@Test
	fun backDuringCountdownDoesNotStartNavigation() {
		val preview = openCardPreviewWithRoute("countdown-back")
		CarTestUtils.onMain { carContext.getCarService(TestScreenManager::class.java).pop() }
		assertFalse("Back must stop the countdown", CarTestUtils.onMain { preview.isCountdownRunning })
		Thread.sleep(countdownDurationMs())
		assertFalse("Navigation must not start after Back", CarTestUtils.onMain { app.routingHelper.isFollowingMode })
	}

	@Test
	fun screenOverPreviewStopsCountdownForGood() {
		val preview = openCardPreviewWithRoute("countdown-pushed")
		val screenManager = carContext.getCarService(TestScreenManager::class.java)
		CarTestUtils.onMain {
			screenManager.push(object : Screen(carContext) {
				override fun onGetTemplate(): Template = MessageTemplate.Builder("test").build()
			})
		}
		assertFalse("A screen over the preview must stop the countdown", CarTestUtils.onMain { preview.isCountdownRunning })
		Thread.sleep(countdownDurationMs())
		assertFalse("Navigation must not start under another screen", CarTestUtils.onMain { app.routingHelper.isFollowingMode })

		CarTestUtils.onMain { screenManager.pop() }
		assertTrue("The preview must be back on top", CarTestUtils.onMain { screenManager.top } === preview)
		assertFalse("The countdown must not restart", CarTestUtils.onMain { preview.isCountdownRunning })
		assertEquals("Start must be a plain button", app.getString(R.string.shared_string_control_start), startTitle(preview))
		Thread.sleep(countdownDurationMs())
		assertFalse("Navigation must not start after returning to the preview", CarTestUtils.onMain { app.routingHelper.isFollowingMode })
	}

	@Test
	fun appInBackgroundStopsCountdownForGood() {
		val preview = openCardPreviewWithRoute("countdown-background")
		CarTestUtils.onMain { carContext.lifecycleOwner.registry.currentState = Lifecycle.State.CREATED }
		assertFalse(
			"The preview must be stopped in the background",
			CarTestUtils.onMain { preview.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
		)
		assertFalse("The background must stop the countdown", CarTestUtils.onMain { preview.isCountdownRunning })
		Thread.sleep(countdownDurationMs())
		assertFalse("Navigation must not start in the background", CarTestUtils.onMain { app.routingHelper.isFollowingMode })

		CarTestUtils.onMain { carContext.lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED }
		assertFalse("The countdown must not restart", CarTestUtils.onMain { preview.isCountdownRunning })
		assertEquals("Start must be a plain button", app.getString(R.string.shared_string_control_start), startTitle(preview))
	}

	@Test
	fun routeRecalculationKeepsCountdown() {
		countdownMs = LONG_COUNTDOWN_MS
		val preview = openCardPreviewWithRoute("countdown-recalculation")
		val routingHelper = app.routingHelper
		val recalculated = AtomicBoolean()
		val listener = object : IRouteInformationListener {
			override fun newRouteIsCalculated(newRoute: Boolean, showToast: ValueHolder<Boolean>) {
				recalculated.set(true)
			}

			override fun routeWasCancelled() {
			}

			override fun routeWasFinished() {
			}
		}
		Thread.sleep(2000L)
		val secondsBefore = remainingSeconds(preview)
		CarTestUtils.onMain {
			routingHelper.addListener(listener)
			routingHelper.recalculateRouteDueToSettingsChange(false)
		}
		try {
			assertTrue(
				"The route must be recalculated",
				waitFor(RECALCULATION_TIMEOUT_SEC) { recalculated.get() && !routingHelper.isRouteBeingCalculated }
			)
		} finally {
			CarTestUtils.onMain { routingHelper.removeListener(listener) }
		}
		assertTrue("The countdown must keep running after a recalculation", CarTestUtils.onMain { preview.isCountdownRunning })
		val secondsAfter = remainingSeconds(preview)
		assertTrue(
			"The countdown must not restart after a recalculation, was $secondsBefore, then $secondsAfter",
			secondsAfter <= secondsBefore
		)
		assertTrue(
			"Navigation must start when the countdown expires",
			waitFor(countdownDurationMs() / 1000 + START_TIMEOUT_SEC) { app.routingHelper.isFollowingMode }
		)
	}

	@Test
	fun navigationIntentShowsPreviewWithoutCountdown() {
		resumeAtBerlinStart()
		val intent = Intent(CarContext.ACTION_NAVIGATE).setData(Uri.parse("geo:$BERLIN_LAT,$BERLIN_LON"))
		CarTestUtils.onMain { session.onNewIntent(intent) }
		val preview = CarTestUtils.onMain { carContext.getCarService(TestScreenManager::class.java).top }
		assertTrue("Top screen must be RoutePreviewScreen, was: ${preview?.javaClass?.simpleName}", preview is RoutePreviewScreen)
		preview as RoutePreviewScreen
		assertFalse("A navigation intent must not get a countdown", CarTestUtils.onMain { preview.hasStartCountdown() })
		assumeRouteReady()
		assertFalse("A navigation intent must not run a countdown", CarTestUtils.onMain { preview.isCountdownRunning })
	}

	private fun openCardPreviewWithRoute(name: String): RoutePreviewScreen {
		resumeAtBerlinStart()
		val pd = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "$testPrefix$name")
		createdDescriptions.add(pd)
		val cardIntent = CarSuggestionsHelper.createCardIntent(app, HistoryEntry(BERLIN_LAT, BERLIN_LON, pd, HistorySource.NAVIGATION))
		CarTestUtils.onMain { session.onNewIntent(cardIntent) }
		val preview = CarTestUtils.onMain { carContext.getCarService(TestScreenManager::class.java).top }
		assertTrue("Top screen must be RoutePreviewScreen, was: ${preview?.javaClass?.simpleName}", preview is RoutePreviewScreen)
		preview as RoutePreviewScreen
		assertTrue("A card tap must get a countdown", CarTestUtils.onMain { preview.hasStartCountdown() })
		assumeRouteReady()
		assertTrue("The countdown must start after the route is ready", waitFor(START_TIMEOUT_SEC) { preview.isCountdownRunning })
		return preview
	}

	private fun setUpPreviousRoute(name: String): PointDescription {
		val start = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "$testPrefix$name-start")
		val via = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "$testPrefix$name-via")
		val destination = PointDescription(PointDescription.POINT_TYPE_ADDRESS, "$testPrefix$name-destination")
		createdDescriptions.addAll(listOf(start, via, destination))
		CarTestUtils.onMain {
			val targetPointsHelper = app.targetPointsHelper
			targetPointsHelper.navigateToPoint(LatLon(BERLIN_LAT, BERLIN_LON), false, -1, destination)
			targetPointsHelper.navigateToPoint(LatLon(BERLIN_VIA_LAT, BERLIN_VIA_LON), false, 0, via)
			targetPointsHelper.setStartPoint(LatLon(BERLIN_START_LAT, BERLIN_START_LON), false, start)
			targetPointsHelper.clearAllPoints(false)
		}
		return destination
	}

	private fun isNear(latLon: LatLon, lat: Double, lon: Double): Boolean =
		MapUtils.getDistance(latLon.latitude, latLon.longitude, lat, lon) < 1

	private fun resumeAtBerlinStart() {
		CarTestUtils.onMain {
			controller.moveToState(Lifecycle.State.RESUMED)
			carContext.lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
			val location = Location("gps", BERLIN_START_LAT, BERLIN_START_LON)
			location.accuracy = 5f
			location.time = System.currentTimeMillis()
			app.locationProvider.setLocationFromService(location)
		}
	}

	private fun assumeRouteReady() {
		val routingHelper = app.routingHelper
		val routeReady = waitFor(ROUTE_TIMEOUT_SEC) {
			routingHelper.isRouteCalculated && !routingHelper.isRouteBeingCalculated || routingHelper.route.hasMissingMaps()
		}
		assumeTrue(
			"Needs an offline map that covers the test points",
			routeReady && CarTestUtils.onMain { !routingHelper.route.hasMissingMaps() }
		)
	}

	private fun startAction(preview: RoutePreviewScreen): Action {
		val template = preview.template as MapWithContentTemplate
		return (template.contentTemplate as PaneTemplate).pane.actions.first()
	}

	private fun startTitle(preview: RoutePreviewScreen): String =
		CarTestUtils.onMain { startAction(preview).title.toString() }

	private fun remainingSeconds(preview: RoutePreviewScreen): Int {
		val title = startTitle(preview)
		val seconds = Regex("\\((\\d+)\\)").find(title)?.groupValues?.get(1)
		assertNotNull("Start must show the remaining seconds, was: $title", seconds)
		return seconds!!.toInt()
	}

	private fun countdownDurationMs(): Long = countdownMs + 1000L

	private fun waitFor(timeoutSec: Long, condition: () -> Boolean): Boolean {
		val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec)
		while (System.currentTimeMillis() < deadline) {
			if (CarTestUtils.onMain(condition)) {
				return true
			}
			Thread.sleep(CarTestUtils.INIT_POLL_MS)
		}
		return false
	}
}
