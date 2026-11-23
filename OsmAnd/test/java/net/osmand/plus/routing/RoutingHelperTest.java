package net.osmand.plus.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.MapUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class RoutingHelperTest {

	private static final LatLon START = new LatLon(52.52, 13.42);
	private static final double NODE_STEP = 20;
	private static final long ROUTE_SET_WAIT_MS = 500;

	@Test
	public void routeSetDuringLocationUpdateKeepsItsStart() {
		OsmandApplication app = (OsmandApplication) InstrumentationRegistry.getInstrumentation()
				.getTargetContext().getApplicationContext();
		RoutingHelper helper = new RoutingHelper(app);
		RouteCalculationResult newRoute = route(6);
		Thread routeCalculation = new Thread(() -> helper.setRoute(newRoute));
		// a route calculation finishes while the location update moves along the old route
		IRoutingDataUpdateListener listener = () -> {
			if (routeCalculation.getState() == Thread.State.NEW) {
				routeCalculation.start();
				try {
					routeCalculation.join(ROUTE_SET_WAIT_MS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
		try {
			helper.setFinalAndCurrentLocation(node(39), null, null);
			helper.setRoute(route(40));
			helper.addRouteDataListener(listener);
			// 30 nodes ahead: the old route moves forward twice, the new route is set after the first step
			helper.setCurrentLocation(location(node(30)), false);
			routeCalculation.join();
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		} finally {
			helper.removeRouteDataListener(listener);
			app.getTransportRoutingHelper().setRoutingHelper(app.getRoutingHelper());
		}
		assertSame(newRoute, helper.getRoute());
		assertEquals(0, newRoute.getCurrentRoute());
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
