package net.osmand.plus.auto

import android.annotation.SuppressLint
import android.content.Intent
import androidx.car.app.HandshakeInfo
import androidx.car.app.testing.SessionController
import androidx.car.app.testing.TestCarContext
import androidx.car.app.testing.TestScreenManager
import androidx.car.app.versioning.CarAppApiLevels
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.data.LatLon
import net.osmand.data.PointDescription
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmandApplication
import net.osmand.plus.auto.screens.LandingScreen
import net.osmand.plus.auto.screens.RoutePreviewScreen
import net.osmand.plus.inapp.InAppPurchaseUtils
import net.osmand.plus.settings.enums.HistorySource
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Verifies with androidx.car.app:app-testing that a launcher suggestion card intent built with
 * [CarSuggestionsHelper.createCardIntent] makes [NavigationSession] push a [RoutePreviewScreen] (issue #25977).
 */
@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class NavigationSessionIntentTest {

	companion object {
		/** Latitude for Berlin test location (Brandenburg Gate). */
		private const val BERLIN_LAT = 52.5163

		/** Longitude for Berlin test location (Brandenburg Gate). */
		private const val BERLIN_LON = 13.3777

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

	/** Navigation history keys before the test; null if the snapshot was not taken (nothing extra is removed then). */
	private var historyBefore: Set<String>? = null

	/** "Previous route" backup before the test; null while the prerequisites are not met (nothing is reset then). */
	private var targetPointsBackup: CarTestUtils.TargetPointsBackup? = null

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication

		savedNavigationHistory = app.settings.NAVIGATION_HISTORY.get()
		app.settings.NAVIGATION_HISTORY.set(true)

		CarTestUtils.waitForAppInitialization(app)
		testStartTimeMs = System.currentTimeMillis()
		historyBefore = CarTestUtils.navigationHistoryKeys(app)

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
			session = NavigationSession()
			controller = SessionController(session, carContext, Intent())
			controller.moveToState(Lifecycle.State.CREATED)
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
				InstrumentationRegistry.getInstrumentation().runOnMainSync {
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
						savedNavigationHistory?.let { saved ->
							app.settings.NAVIGATION_HISTORY.set(saved)
						}
					}
				}
			}
		}
	}

	/**
	 * Verifies that delivering a suggestion card intent built with [CarSuggestionsHelper.createCardIntent]
	 * for a test history entry processes the suggestion and pushes a [RoutePreviewScreen].
	 */
	@Test
	fun suggestionCardIntentPushesRoutePreviewScreen() {
		val testTitle = "${testPrefix}suggestion-card"
		val pd = PointDescription(PointDescription.POINT_TYPE_LOCATION, testTitle)
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

		val cardIntent = CarSuggestionsHelper.createCardIntent(app, entry!!, testTitle)

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
	}
}
