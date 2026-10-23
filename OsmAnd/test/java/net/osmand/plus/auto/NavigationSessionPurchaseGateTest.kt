package net.osmand.plus.auto

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.HandshakeInfo
import androidx.car.app.Screen
import androidx.car.app.suggestion.SuggestionManager
import androidx.car.app.testing.SessionController
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.TestScreenManager
import androidx.car.app.versioning.CarAppApiLevels
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.LatLon
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmandApplication
import net.osmand.plus.Version
import net.osmand.plus.auto.screens.RequestPurchaseScreen
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Without the Android Auto purchase nothing may replace or cover the purchase screen (#26632).
 * Runs on a free build: the cached purchase states and the free period after the install or update are
 * removed for the test and restored after it.
 */
@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class NavigationSessionPurchaseGateTest {

	companion object {
		private const val BERLIN_LAT = 52.5163
		private const val BERLIN_LON = 13.3777

		// AppInitializer keeps the install and update times in the preferences of the map activity
		private const val START_PREFS = "plus.activities.MapActivity"
		private const val FIRST_INSTALLED = "FIRST_INSTALLED"
		private const val UPDATE_TIME_MS = "UPDATE_TIME_MS"
	}

	private lateinit var app: OsmandApplication
	private lateinit var carContext: TestCarContext
	private lateinit var session: NavigationSession
	private lateinit var controller: SessionController

	private val testPrefix = "26632-test-" + UUID.randomUUID().toString() + "-"
	private var testStartTimeMs = 0L
	private var historyBefore: Set<String>? = null
	private var savedLastModifiedTime: Long? = null
	private var targetPointsBackup: CarTestUtils.TargetPointsBackup? = null
	private var purchasePrefs: List<OsmandPreference<Boolean>> = emptyList()
	private var savedPurchases: List<Boolean> = emptyList()
	private var savedInstallTimes: Map<String, Long?>? = null

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		CarTestUtils.waitForAppInitialization(app)
		assumeTrue("Needs a free build, a paid one always has Android Auto", Version.isFreeVersion(app))
		assumeTrue("Location permission must be available", OsmAndLocationProvider.isLocationPermissionAvailable(app))
		assumeTrue("Car navigation session must not be active", app.carNavigationSession == null)
		val routingHelper = app.routingHelper
		val targetPointsHelper = app.targetPointsHelper
		assumeTrue("Destination point must not be set", targetPointsHelper.pointToNavigate == null
				&& targetPointsHelper.intermediatePoints.isEmpty())
		assumeTrue("Navigation must not be active or in planning mode",
			!routingHelper.isFollowingMode && !routingHelper.isPauseNavigation && !routingHelper.isRoutePlanningMode)

		// without the fix the navigation intent below sets a destination, which adds a history entry
		savedLastModifiedTime = app.searchHistoryHelper.lastModifiedTime
		historyBefore = CarTestUtils.navigationHistoryKeys(app)
		testStartTimeMs = System.currentTimeMillis()
		CarTestUtils.assumeHistoryHasRoom(app)

		removeAndroidAutoPurchase()
		assumeFalse("Android Auto must be unavailable without the purchase", InAppPurchaseUtils.isAndroidAutoAvailable(app))

		targetPointsBackup = CarTestUtils.saveTargetPointsBackup(app)
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			carContext = TestCarContext.createCarContext(app)
			carContext.updateHandshakeInfo(HandshakeInfo(CarTestUtils.TEST_HOST_PACKAGE, CarAppApiLevels.LEVEL_5))
			carContext.overrideCarService(SuggestionManager::class.java, CarTestUtils.RecordingSuggestionManager(carContext))
			session = NavigationSession()
			controller = SessionController(session, carContext, Intent())
			controller.moveToState(Lifecycle.State.RESUMED)
			// screens follow the lifecycle of TestCarContext, not of the session: without this they are never created
			carContext.lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
		}
		assumeTrue("The purchase screen must be shown", topScreen() is RequestPurchaseScreen)
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
					session.stopCarNavigation()
				}
				instrumentation.waitForIdleSync()
				instrumentation.runOnMainSync {
					controller.moveToState(Lifecycle.State.DESTROYED)
					app.targetPointsHelper.clearAllPoints(false)
				}
			}
		} finally {
			try {
				targetPointsBackup?.let { backup -> CarTestUtils.restoreTargetPointsBackup(app, backup) }
				savedLastModifiedTime?.let { time ->
					CarTestUtils.cleanupHistoryEntries(
						app = app,
						testPrefix = testPrefix,
						historyBefore = historyBefore,
						testStartTimeMs = testStartTimeMs,
						testPoints = listOf(LatLon(BERLIN_LAT, BERLIN_LON))
					)
					app.searchHistoryHelper.lastModifiedTime = time
				}
			} finally {
				restoreAndroidAutoPurchase()
			}
		}
	}

	@Test
	fun backKeepsPurchaseScreen() {
		CarTestUtils.onMain { carContext.onBackPressedDispatcher.onBackPressed() }

		assertPurchaseScreenOnTop()
	}

	@Test
	fun navigationIntentDoesNotOpenRoutePreview() {
		val intent = Intent(CarContext.ACTION_NAVIGATE).setData(Uri.parse("geo:$BERLIN_LAT,$BERLIN_LON"))
		CarTestUtils.onMain { session.onNewIntent(intent) }

		assertPurchaseScreenOnTop()
	}

	@Test
	fun navigationScreenIsNotOpenedOverPurchaseScreen() {
		// what navigation started on the phone leads to
		CarTestUtils.onMain { session.startNavigationScreen() }

		assertPurchaseScreenOnTop()
	}

	private fun assertPurchaseScreenOnTop() {
		val stack = CarTestUtils.onMain {
			carContext.getCarService(TestScreenManager::class.java).screenStack.map { screen -> screen.javaClass.simpleName }
		}
		assertTrue("The purchase screen must stay on top, stack: $stack", topScreen() is RequestPurchaseScreen)
	}

	private fun topScreen(): Screen? = CarTestUtils.onMain { carContext.getCarService(TestScreenManager::class.java).top }

	private fun startPrefs() = app.getSharedPreferences(START_PREFS, Context.MODE_PRIVATE)

	private fun removeAndroidAutoPurchase() {
		val settings = app.settings
		purchasePrefs = listOf(settings.FULL_VERSION_PURCHASED, settings.LIVE_UPDATES_PURCHASED,
			settings.OSMAND_MAPS_PURCHASED, settings.OSMAND_PRO_PURCHASED, settings.BACKUP_PURCHASE_ACTIVE)
		savedPurchases = purchasePrefs.map { pref -> pref.get() }
		purchasePrefs.forEach { pref -> pref.set(false) }

		val prefs = startPrefs()
		savedInstallTimes = listOf(FIRST_INSTALLED, UPDATE_TIME_MS).associateWith { key ->
			if (prefs.contains(key)) prefs.getLong(key, 0) else null
		}
		// Android Auto is free for 10 days after the install or update
		val past = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
		prefs.edit().putLong(FIRST_INSTALLED, past).putLong(UPDATE_TIME_MS, past).commit()
	}

	private fun restoreAndroidAutoPurchase() {
		purchasePrefs.forEachIndexed { i, pref -> pref.set(savedPurchases[i]) }
		purchasePrefs = emptyList()
		savedInstallTimes?.let { times ->
			val editor = startPrefs().edit()
			for ((key, value) in times) {
				if (value != null) editor.putLong(key, value) else editor.remove(key)
			}
			editor.commit()
		}
		savedInstallTimes = null
	}
}
