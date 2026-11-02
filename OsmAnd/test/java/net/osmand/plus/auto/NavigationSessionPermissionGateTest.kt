package net.osmand.plus.auto

import android.annotation.SuppressLint
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
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.screens.RequestPermissionScreen
import net.osmand.plus.auto.screens.RequestPermissionScreen.LocationPermissionCheckCallback
import net.osmand.plus.auto.screens.RoutePreviewScreen
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Without the location permission a navigation intent keeps the permission screen in front and opens
 * the route preview after the grant (#26632). The session reports the permission as missing: revoking it
 * for real would kill the test process.
 */
@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class NavigationSessionPermissionGateTest {

	companion object {
		private const val BERLIN_LAT = 52.5163
		private const val BERLIN_LON = 13.3777
	}

	private lateinit var app: OsmandApplication
	private lateinit var carContext: TestCarContext
	private lateinit var session: NavigationSession
	private lateinit var controller: SessionController

	private var permissionAvailable = false
	private val testPrefix = "26632-test-" + UUID.randomUUID().toString() + "-"
	private var testStartTimeMs = 0L
	private var historyBefore: Set<String>? = null
	private var savedLastModifiedTime: Long? = null
	private var targetPointsBackup: CarTestUtils.TargetPointsBackup? = null

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		CarTestUtils.waitForAppInitialization(app)
		assumeTrue("Car navigation session must not be active", app.carNavigationSession == null)
		val routingHelper = app.routingHelper
		val targetPointsHelper = app.targetPointsHelper
		assumeTrue("Destination point must not be set", targetPointsHelper.pointToNavigate == null
				&& targetPointsHelper.intermediatePoints.isEmpty())
		assumeTrue("Navigation must not be active or in planning mode",
			!routingHelper.isFollowingMode && !routingHelper.isPauseNavigation && !routingHelper.isRoutePlanningMode)

		// the route preview opened by the navigation intent sets a destination, which adds a history entry
		savedLastModifiedTime = app.searchHistoryHelper.lastModifiedTime
		historyBefore = CarTestUtils.navigationHistoryKeys(app)
		testStartTimeMs = System.currentTimeMillis()
		CarTestUtils.assumeHistoryHasRoom(app)

		targetPointsBackup = CarTestUtils.saveTargetPointsBackup(app)
		InstrumentationRegistry.getInstrumentation().runOnMainSync {
			carContext = TestCarContext.createCarContext(app)
			carContext.updateHandshakeInfo(HandshakeInfo(CarTestUtils.TEST_HOST_PACKAGE, CarAppApiLevels.LEVEL_5))
			carContext.overrideCarService(SuggestionManager::class.java, CarTestUtils.RecordingSuggestionManager(carContext))
			session = object : NavigationSession() {
				override fun isLocationPermissionAvailable() = permissionAvailable
			}
			controller = SessionController(session, carContext, Intent())
			controller.moveToState(Lifecycle.State.RESUMED)
			// screens follow the lifecycle of TestCarContext, not of the session: without this they are never created
			carContext.lifecycleOwner.registry.currentState = Lifecycle.State.RESUMED
		}
		// the purchase screen comes first when Android Auto is not available
		assumeTrue("The permission screen must be shown", topScreen() is RequestPermissionScreen)
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
		}
	}

	@Test
	fun navigationIntentIsKeptUntilPermissionIsGranted() {
		val intent = Intent(CarContext.ACTION_NAVIGATE).setData(Uri.parse("geo:$BERLIN_LAT,$BERLIN_LON"))
		CarTestUtils.onMain { session.onNewIntent(intent) }
		assertTopScreen(RequestPermissionScreen::class.java)

		// what RequestPermissionScreen does when the permission is granted
		permissionAvailable = true
		val permissionScreen = topScreen() as RequestPermissionScreen
		val callbackField = RequestPermissionScreen::class.java.getDeclaredField("mLocationPermissionCheckCallback")
		callbackField.isAccessible = true
		val callback = callbackField.get(permissionScreen) as LocationPermissionCheckCallback
		CarTestUtils.onMain {
			callback.onPermissionGranted()
			permissionScreen.finish()
		}
		assertTopScreen(RoutePreviewScreen::class.java)
	}

	private fun assertTopScreen(expected: Class<out Screen>) {
		val stack = CarTestUtils.onMain {
			carContext.getCarService(TestScreenManager::class.java).screenStack.map { screen -> screen.javaClass.simpleName }
		}
		assertTrue("${expected.simpleName} must be on top, stack: $stack", expected.isInstance(topScreen()))
	}

	private fun topScreen(): Screen? = CarTestUtils.onMain { carContext.getCarService(TestScreenManager::class.java).top }
}
