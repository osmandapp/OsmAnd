package net.osmand.plus.routing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.MapUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class RouteDirectionCheckTest {

	private static final LatLon RIDER = new LatLon(52.52, 13.42);
	// not 90, which checkWrongMovementDirection takes as an invalid bearing
	private static final float RIDER_BEARING = 100;

	@Test
	public void routeFromBesideTheRoadIsForward() {
		List<Location> route = startAtRider();
		LatLon road = MapUtils.rhumbDestinationPoint(RIDER, 45, RIDER_BEARING - 110);
		route.add(location(road));
		addStraight(route, road, RIDER_BEARING, 20, 10);

		assertTrue("the next node goes back", RoutingHelperUtils.checkWrongMovementDirection(rider(), null, route.get(1)));
		assertFalse(isAgainstMovement(route));
	}

	@Test
	public void routeAgainstTheMovementIsBackward() {
		List<Location> route = startAtRider();
		addStraight(route, RIDER, RIDER_BEARING + 180, 20, 10);

		assertTrue(isAgainstMovement(route));
	}

	@Test
	public void uTurnAfterMissedTurnIsBackward() {
		List<Location> route = startAtRider();
		LatLon junction = MapUtils.rhumbDestinationPoint(RIDER, 20, RIDER_BEARING + 180);
		route.add(location(junction));
		addStraight(route, junction, RIDER_BEARING + 90, 20, 10);

		assertTrue(isAgainstMovement(route));
	}

	@Test
	public void sharpTurnAheadIsForward() {
		List<Location> route = startAtRider();
		LatLon hairpin = MapUtils.rhumbDestinationPoint(RIDER, 30, RIDER_BEARING);
		route.add(location(hairpin));
		addStraight(route, MapUtils.rhumbDestinationPoint(hairpin, 10, RIDER_BEARING + 90), RIDER_BEARING + 180, 20, 10);

		Location ahead = calculationResult(route).getRouteLocationByDistance(SuppressedRecalculationPrompt.DIRECTION_CHECK_DISTANCE);
		assertTrue("the node ahead goes back", RoutingHelperUtils.checkWrongMovementDirection(rider(), null, ahead));
		assertFalse(isAgainstMovement(route));
	}

	@Test
	public void shortRouteAgainstTheMovementIsBackward() {
		List<Location> route = startAtRider();
		addStraight(route, RIDER, RIDER_BEARING + 180, 20, 2);

		assertTrue(isAgainstMovement(route));
	}

	private static boolean isAgainstMovement(@NonNull List<Location> route) {
		return SuppressedRecalculationPrompt.isRouteAgainstMovement(rider(), calculationResult(route));
	}

	@NonNull
	private static RouteCalculationResult calculationResult(@NonNull List<Location> route) {
		RouteCalculationParams params = new RouteCalculationParams();
		params.mode = ApplicationMode.BICYCLE;
		return new RouteCalculationResult(route, null, params, null, false);
	}

	@NonNull
	private static List<Location> startAtRider() {
		List<Location> route = new ArrayList<>();
		route.add(location(RIDER));
		return route;
	}

	private static void addStraight(@NonNull List<Location> route, @NonNull LatLon from, double bearing, double step, int count) {
		for (int i = 1; i <= count; i++) {
			route.add(location(MapUtils.rhumbDestinationPoint(from, step * i, bearing)));
		}
	}

	@NonNull
	private static Location rider() {
		Location location = location(RIDER);
		location.setBearing(RIDER_BEARING);
		return location;
	}

	@NonNull
	private static Location location(@NonNull LatLon latLon) {
		return new Location("test", latLon.getLatitude(), latLon.getLongitude());
	}
}
