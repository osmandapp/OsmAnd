package net.osmand.plus.routing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.router.TurnType;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Route locations lie on a meridian: location i is (50.00 + i * 0.01, 30.00), ~1.1 km apart.
 */
@RunWith(AndroidJUnit4.class)
public class RouteCalculationResultTest {

	private static final int LOCATIONS_COUNT = 10;
	// ~71 km east of the route
	private static final double DISTANT_LON = 31.00;

	@Test
	public void calculateIntermediateIndexesMatchesNearbyIntermediates() {
		List<RouteDirectionInfo> directions = createDirections(0, 3, 7, 9);
		List<LatLon> intermediates = Arrays.asList(
				new LatLon(50.03, 30.00),
				new LatLon(50.07, 30.00));

		assertArrayEquals(new int[] {1, 2}, calculateIntermediateIndexes(intermediates, directions));
	}

	@Test
	public void calculateIntermediateIndexesMatchesDistantIntermediateToClosestLocation() {
		List<RouteDirectionInfo> directions = createDirections(0, 2, 5, 8, 9);
		List<LatLon> intermediates = Arrays.asList(
				new LatLon(50.02, 30.00),
				new LatLon(50.05, DISTANT_LON),
				new LatLon(50.08, 30.00));

		assertArrayEquals(new int[] {1, 2, 3}, calculateIntermediateIndexes(intermediates, directions));
	}

	@Test
	public void calculateIntermediateIndexesMatchesAllDistantIntermediates() {
		List<RouteDirectionInfo> directions = createDirections(0, 2, 7, 9);
		List<LatLon> intermediates = Arrays.asList(
				new LatLon(50.02, DISTANT_LON),
				new LatLon(50.07, DISTANT_LON));

		assertArrayEquals(new int[] {1, 2}, calculateIntermediateIndexes(intermediates, directions));
	}

	@Test
	public void calculateIntermediateIndexesAddsDirectionForIntermediateAfterLastDirection() {
		List<RouteDirectionInfo> directions = createDirections(0, 3, 5);
		List<LatLon> intermediates = Arrays.asList(
				new LatLon(50.03, 30.00),
				new LatLon(50.08, 30.00));

		assertArrayEquals(new int[] {1, 3}, calculateIntermediateIndexes(intermediates, directions));
		assertEquals(4, directions.size());
		assertEquals(8, directions.get(3).routePointOffset);
	}

	private static int[] calculateIntermediateIndexes(List<LatLon> intermediates, List<RouteDirectionInfo> directions) {
		List<Location> locations = new ArrayList<>();
		for (int i = 0; i < LOCATIONS_COUNT; i++) {
			Location location = new Location("test");
			location.setLatitude(50.00 + i * 0.01);
			location.setLongitude(30.00);
			locations.add(location);
		}
		int[] intermediatePoints = new int[intermediates.size()];
		RouteCalculationResult.calculateIntermediateIndexes(ApplicationProvider.getApplicationContext(), locations, intermediates, directions, intermediatePoints);
		return intermediatePoints;
	}

	private static List<RouteDirectionInfo> createDirections(int... routePointOffsets) {
		List<RouteDirectionInfo> directions = new ArrayList<>();
		for (int routePointOffset : routePointOffsets) {
			RouteDirectionInfo info = new RouteDirectionInfo(10.0f, TurnType.straight());
			info.routePointOffset = routePointOffset;
			directions.add(info);
		}
		return directions;
	}
}
