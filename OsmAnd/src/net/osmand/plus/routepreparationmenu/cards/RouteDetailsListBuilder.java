package net.osmand.plus.routepreparationmenu.cards;

import androidx.annotation.NonNull;

import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.routing.RouteCalculationResult.IntermediatePointInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.SharedRouteDetailsProvider;
import net.osmand.shared.routing.details.RouteCumulativeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Builds the Route Details row sequence without any Android view dependencies. */
class RouteDetailsListBuilder {

	private static final Comparator<RouteDetailsItem> ROUTE_ORDER = (first, second) -> {
		if (first.isDestination() != second.isDestination()) {
			return first.isDestination() ? 1 : -1;
		}
		int routePointOrder = Integer.compare(first.getRoutePointOffset(), second.getRoutePointOffset());
		if (routePointOrder != 0) {
			return routePointOrder;
		}
		return Integer.compare(first.getType().getOrderAtSameRoutePoint(),
				second.getType().getOrderAtSameRoutePoint());
	};

	private RouteDetailsListBuilder() {
	}

	@NonNull
	static List<RouteDetailsItem> buildCoreItems(
			@NonNull List<RouteDirectionInfo> routeDirections,
			@NonNull List<IntermediatePointInfo> intermediatePointInfos,
			@NonNull List<TargetPoint> intermediatePoints) {
		List<RouteDetailsItem> items = new ArrayList<>(
				routeDirections.size() + intermediatePointInfos.size());
		List<RouteCumulativeInfo> cumulativeInfoByPosition =
				SharedRouteDetailsProvider.getCumulativeInfoByPosition(routeDirections);
		for (int directionIndex = 0; directionIndex < routeDirections.size(); directionIndex++) {
			RouteDirectionInfo direction = routeDirections.get(directionIndex);
			boolean destination = directionIndex == routeDirections.size() - 1
					&& direction.distance == 0;
			items.add(RouteDetailsItem.direction(direction, directionIndex,
					cumulativeInfoByPosition.get(directionIndex), destination));
		}

		int nextDirectionIndex = 0;
		for (int intermediateIndex = 0;
		     intermediateIndex < intermediatePointInfos.size(); intermediateIndex++) {
			IntermediatePointInfo info = intermediatePointInfos.get(intermediateIndex);
			while (nextDirectionIndex < routeDirections.size()
					&& routeDirections.get(nextDirectionIndex).routePointOffset
					< info.getRoutePointOffset()) {
				nextDirectionIndex++;
			}
			int directionIndex = Math.min(nextDirectionIndex,
					Math.max(0, routeDirections.size() - 1));
			TargetPoint targetPoint = intermediateIndex < intermediatePoints.size()
					? intermediatePoints.get(intermediateIndex) : null;
			items.add(RouteDetailsItem.intermediate(targetPoint, intermediateIndex,
					directionIndex, info.getRoutePointOffset(), info.getDistance(), info.getTime()));
		}
		items.sort(ROUTE_ORDER);
		return items;
	}

	@NonNull
	static List<RouteDetailsItem> mergeAlongRouteItems(
			@NonNull List<RouteDetailsItem> coreItems,
			@NonNull List<RouteDetailsItem> alongRouteItems,
			@NonNull Set<RouteDetailsItem.Type> visibleTypes,
			int currentRoutePointIndex) {
		List<RouteDetailsItem> result = new ArrayList<>(coreItems.size() + alongRouteItems.size());
		result.addAll(coreItems);
		for (RouteDetailsItem item : alongRouteItems) {
			if (!item.isAlongRoute()) {
				throw new IllegalArgumentException("Only along-route items can be merged");
			}
			if (item.getRoutePointOffset() > currentRoutePointIndex
					&& visibleTypes.contains(item.getType())) {
				result.add(item);
			}
		}
		result.sort(ROUTE_ORDER);
		return result;
	}
}
