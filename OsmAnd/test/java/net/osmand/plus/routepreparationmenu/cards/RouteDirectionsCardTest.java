package net.osmand.plus.routepreparationmenu.cards;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.osmand.data.LatLon;
import net.osmand.plus.helpers.LocationPointWrapper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.RouteCalculationResult.IntermediatePointInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.router.TurnType;
import net.osmand.shared.routing.details.RouteCumulativeInfo;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RouteDirectionsCardTest {

	@Test
	public void insertsIntermediateDestinationsBeforeNextManeuver() {
		List<RouteDirectionInfo> directions = Arrays.asList(
				direction(0, 100),
				direction(10, 100),
				direction(20, 0));
		List<IntermediatePointInfo> intermediateInfos = Arrays.asList(
				new IntermediatePointInfo(5, 50, 5),
				new IntermediatePointInfo(10, 100, 10));
		TargetPoint first = new TargetPoint(new LatLon(1, 1), null, 0);
		TargetPoint second = new TargetPoint(new LatLon(2, 2), null, 1);

		List<RouteDetailsItem> items = RouteDirectionsCard.buildRouteDirectionItems(
				directions, intermediateInfos, Arrays.asList(first, second));

		assertEquals(5, items.size());
		assertFalse(items.get(0).isIntermediate());
		assertTrue(items.get(1).isIntermediate());
		assertSame(first, items.get(1).getTargetPoint());
		assertEquals(50, items.get(1).getCumulativeDistance());
		assertEquals(5, items.get(1).getCumulativeTime());
		assertEquals(1, items.get(1).getDirectionIndex());
		assertTrue(items.get(2).isIntermediate());
		assertSame(second, items.get(2).getTargetPoint());
		assertEquals(100, items.get(2).getCumulativeDistance());
		assertFalse(items.get(3).isIntermediate());
		assertEquals(100, items.get(3).getCumulativeDistance());
		assertTrue(items.get(4).isDestination());
		assertEquals(200, items.get(4).getCumulativeDistance());
	}

	@Test
	public void keepsRouteIntermediateWhenTargetMetadataIsUnavailable() {
		List<RouteDetailsItem> items = RouteDirectionsCard.buildRouteDirectionItems(
				Arrays.asList(direction(0, 100), direction(10, 0)),
				Collections.singletonList(new IntermediatePointInfo(5, 50, 5)),
				Collections.emptyList());

		assertEquals(3, items.size());
		assertTrue(items.get(1).isIntermediate());
		assertNull(items.get(1).getTargetPoint());
	}

	@Test
	public void mergesEnabledFuturePointsByRouteIndexWithDeterministicTieOrder() {
		List<RouteDetailsItem> coreItems = RouteDetailsListBuilder.buildCoreItems(
				Arrays.asList(direction(0, 100), direction(10, 100), direction(20, 0)),
				Collections.singletonList(new IntermediatePointInfo(10, 100, 10)),
				Collections.emptyList());
		LocationPointWrapper passedWarning = point(WaypointHelper.ALARMS, 2);
		LocationPointWrapper hiddenPoi = point(WaypointHelper.POI, 5);
		LocationPointWrapper warning = point(WaypointHelper.ALARMS, 10);
		LocationPointWrapper firstFavorite = point(WaypointHelper.FAVORITES, 10);
		LocationPointWrapper secondFavorite = point(WaypointHelper.FAVORITES, 10);
		List<RouteDetailsItem> alongRouteItems = Arrays.asList(
				alongRoute(passedWarning),
				alongRoute(hiddenPoi),
				alongRoute(warning),
				alongRoute(firstFavorite),
				alongRoute(secondFavorite));

		List<RouteDetailsItem> result = RouteDetailsListBuilder.mergeAlongRouteItems(
				coreItems, alongRouteItems,
				RouteDirectionsCard.getVisibleAlongRouteTypes(
						RouteDirectionsCard.FILTER_TRAFFIC_WARNINGS
								| RouteDirectionsCard.FILTER_FAVORITES), 2);

		List<RouteDetailsItem.Type> itemTypes = new java.util.ArrayList<>();
		for (RouteDetailsItem item : result) {
			itemTypes.add(item.getType());
		}
		assertEquals(Arrays.asList(
				RouteDetailsItem.Type.MANEUVER,
				RouteDetailsItem.Type.INTERMEDIATE,
				RouteDetailsItem.Type.TRAFFIC_WARNING,
				RouteDetailsItem.Type.FAVORITE,
				RouteDetailsItem.Type.FAVORITE,
				RouteDetailsItem.Type.MANEUVER,
				RouteDetailsItem.Type.DESTINATION), itemTypes);
		assertSame(warning, result.get(2).getLocationPoint());
		assertSame(firstFavorite, result.get(3).getLocationPoint());
		assertSame(secondFavorite, result.get(4).getLocationPoint());
	}

	private static RouteDetailsItem alongRoute(LocationPointWrapper point) {
		return RouteDetailsItem.alongRoute(point, new RouteCumulativeInfo(
				point.getRouteIndex() * 10, point.getRouteIndex()));
	}

	private static LocationPointWrapper point(int type, int routeIndex) {
		return new LocationPointWrapper(type, null, 0, routeIndex);
	}

	private static RouteDirectionInfo direction(int routePointOffset, int distance) {
		RouteDirectionInfo direction = new RouteDirectionInfo(10, TurnType.straight());
		direction.routePointOffset = routePointOffset;
		direction.distance = distance;
		return direction;
	}
}
