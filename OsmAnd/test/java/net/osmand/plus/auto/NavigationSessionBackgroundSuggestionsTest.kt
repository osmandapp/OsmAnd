package net.osmand.plus.auto

import android.annotation.SuppressLint
import android.content.Intent
import androidx.car.app.HandshakeInfo
import androidx.car.app.suggestion.SuggestionManager
import androidx.car.app.testing.SessionController
import androidx.car.app.testing.TestCarContext
import androidx.car.app.versioning.CarAppApiLevels
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.osmand.plus.OsmAndConstants
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmandApplication
import net.osmand.plus.inapp.InAppPurchaseUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Verifies that the launcher suggestion cards are refreshed when the route is cancelled while OsmAnd
 * is in the background on the head unit (session CREATED, not STARTED), e.g. navigation is stopped on
 * the phone (issue #25977).
 */
@RunWith(AndroidJUnit4::class)
@SuppressLint("RestrictedApi")
class NavigationSessionBackgroundSuggestionsTest {

	companion object {
		/** Maximum time in seconds to wait for a suggestions publication. */
		private const val PUBLICATION_TIMEOUT_SEC = 5L

		/** Polling interval in milliseconds while waiting for a suggestions publication. */
		private const val POLL_INTERVAL_MS = 100L
	}

	private lateinit var app: OsmandApplication
	private lateinit var controller: SessionController
	private lateinit var session: NavigationSession
	private lateinit var recordingManager: CarTestUtils.RecordingSuggestionManager

	@Before
	fun setup() {
		app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as OsmandApplication
		CarTestUtils.waitForAppInitialization(app)

		val routingHelper = app.routingHelper
		val targetPointsHelper = app.targetPointsHelper
		assumeTrue("Car navigation session must not be active", app.carNavigationSession == null)
		assumeTrue("Android Auto feature must be available", InAppPurchaseUtils.isAndroidAutoAvailable(app))
		assumeTrue("Location permission must be available", OsmAndLocationProvider.isLocationPermissionAvailable(app))
		assumeTrue("Navigation history must be enabled", app.settings.NAVIGATION_HISTORY.get())
		assumeTrue(
			"No route, destination or followed track must be set",
			targetPointsHelper.pointToNavigate == null
					&& targetPointsHelper.pointToStart == null
					&& targetPointsHelper.intermediatePoints.isEmpty()
					&& app.settings.FOLLOW_THE_GPX_ROUTE.get() == null
					&& !routingHelper.isRouteCalculated
					&& !routingHelper.isRouteBeingCalculated
		)
		assumeTrue(
			"Navigation must not be active, paused or planned",
			!routingHelper.isFollowingMode && !routingHelper.isPauseNavigation && !routingHelper.isRoutePlanningMode
		)

		CarTestUtils.onMain {
			val carContext = TestCarContext.createCarContext(app)
			carContext.updateHandshakeInfo(HandshakeInfo(CarTestUtils.TEST_HOST_PACKAGE, CarAppApiLevels.LEVEL_5))
			recordingManager = CarTestUtils.RecordingSuggestionManager(carContext)
			carContext.overrideCarService(SuggestionManager::class.java, recordingManager)
			carContext.lifecycleOwner.registry.currentState = Lifecycle.State.CREATED
			session = NavigationSession()
			controller = SessionController(session, carContext, Intent())
			controller.moveToState(Lifecycle.State.CREATED)
		}
	}

	@After
	fun tearDown() {
		if (::controller.isInitialized) {
			CarTestUtils.onMain {
				controller.moveToState(Lifecycle.State.DESTROYED)
			}
		}
	}

	@Test
	fun routeCancelledInBackgroundPublishesSuggestions() {
		CarTestUtils.onMain {
			assertEquals("Session must stay in the background", Lifecycle.State.CREATED, session.lifecycle.currentState)
			app.uiHandler.removeMessages(OsmAndConstants.UI_HANDLER_CAR_SUGGESTIONS + 1)
			recordingManager.clear()
			app.routingHelper.clearCurrentRoute(null, ArrayList())
		}

		val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(PUBLICATION_TIMEOUT_SEC)
		while (recordingManager.publications.isEmpty() && System.currentTimeMillis() < deadline) {
			Thread.sleep(POLL_INTERVAL_MS)
		}
		assertTrue(
			"Cancelling the route must refresh the cards while the session is in the background",
			recordingManager.publications.isNotEmpty()
		)
	}
}
