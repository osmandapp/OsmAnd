package net.osmand.plus.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.util.MapUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class RoutingHelperTest {

	private static final LatLon START = new LatLon(52.52, 13.42);
	private static final double NODE_STEP = 20;
	private static final long ROUTE_CALCULATION_TIMEOUT_MS = 5000;

	private OsmandApplication app;
	private RoutingHelper helper;
	private RouteCalculationResult oldRoute;

	@Before
	public void setUp() {
		app = (OsmandApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
		helper = new RoutingHelper(app);
		helper.setAppMode(ApplicationMode.CAR);
		helper.setFinalAndCurrentLocation(node(39), null, null);
		oldRoute = route(40);
		helper.setRoute(oldRoute, new RouteCalculationProgress());
	}

	@After
	public void tearDown() {
		app.getTransportRoutingHelper().setRoutingHelper(app.getRoutingHelper());
	}

	@Test
	public void routeSetDuringLocationUpdateKeepsItsStart() throws InterruptedException {
		RouteCalculationResult newRoute = route(6);
		setRouteDuringLocationUpdate(newRoute, new RouteCalculationProgress(), () -> {});

		assertSame(newRoute, helper.getRoute());
		assertEquals(0, newRoute.getCurrentRoute());
	}

	@Test
	public void routeStoppedDuringLocationUpdateIsNotSet() throws InterruptedException {
		RouteCalculationProgress progress = new RouteCalculationProgress();
		// as stopCalculation() does when the location update reaches the destination
		setRouteDuringLocationUpdate(route(6), progress, () -> progress.isCancelled = true);

		assertSame(oldRoute, helper.getRoute());
	}

	/**
	 * A route calculation finishes while the location update moves along the old route: the location is
	 * 30 nodes ahead, so the old route moves forward twice, and the new route is set after the first step.
	 */
	private void setRouteDuringLocationUpdate(@NonNull RouteCalculationResult newRoute,
	                                          @NonNull RouteCalculationProgress progress,
	                                          @NonNull Runnable whileSetting) throws InterruptedException {
		Thread routeCalculation = new Thread(() -> helper.setRoute(newRoute, progress));
		IRoutingDataUpdateListener listener = () -> {
			if (routeCalculation.getState() == Thread.State.NEW) {
				routeCalculation.start();
				awaitBlockedOrFinished(routeCalculation);
				whileSetting.run();
			}
		};
		helper.addRouteDataListener(listener);
		try {
			helper.setCurrentLocation(location(node(30)), false);
		} finally {
			helper.removeRouteDataListener(listener);
		}
		routeCalculation.join(ROUTE_CALCULATION_TIMEOUT_MS);
	}

	private static void awaitBlockedOrFinished(@NonNull Thread thread) {
		long deadline = SystemClock.uptimeMillis() + ROUTE_CALCULATION_TIMEOUT_MS;
		while (thread.isAlive() && thread.getState() != Thread.State.BLOCKED) {
			assertTrue("the route was neither set nor waiting for the lock", SystemClock.uptimeMillis() < deadline);
			SystemClock.sleep(1);
		}
	}

	@NonNull
	private static RouteCalculationResult route(int nodes) {
		List<Location> locations = new ArrayList<>();
		for (int i = 0; i < nodes; i++) {
			locations.add(location(node(i)));
		}
		RouteCalculationParams params = new RouteCalculationParams();
		params.mode = ApplicationMode.CAR;
		return new RouteCalculationResult(locations, null, params, null, false);
	}

	@NonNull
	private static LatLon node(int index) {
		return MapUtils.rhumbDestinationPoint(START, NODE_STEP * index, 90);
	}

	@NonNull
	private static Location location(@NonNull LatLon latLon) {
		return new Location("test", latLon.getLatitude(), latLon.getLongitude());
	}
}
