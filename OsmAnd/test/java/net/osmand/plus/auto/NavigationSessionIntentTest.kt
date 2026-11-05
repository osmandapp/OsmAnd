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
