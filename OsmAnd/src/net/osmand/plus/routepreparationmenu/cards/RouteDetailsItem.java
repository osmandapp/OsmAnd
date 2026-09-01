package net.osmand.plus.routepreparationmenu.cards;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.helpers.LocationPointWrapper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.shared.routing.details.RouteCumulativeInfo;

/** Data-only model for one ordered row in the Route Details list. */
class RouteDetailsItem {

	enum Type {
		INTERMEDIATE(false, 0),
		TRAFFIC_WARNING(true, 1),
		POI(true, 2),
		FAVORITE(true, 3),
		MANEUVER(false, 4),
		DESTINATION(false, 5);

		private final boolean alongRoute;
		private final int orderAtSameRoutePoint;

		Type(boolean alongRoute, int orderAtSameRoutePoint) {
			this.alongRoute = alongRoute;
			this.orderAtSameRoutePoint = orderAtSameRoutePoint;
		}

		boolean isAlongRoute() {
			return alongRoute;
		}

		int getOrderAtSameRoutePoint() {
			return orderAtSameRoutePoint;
		}
	}

	@NonNull
	private final Type type;
	@Nullable
	private final RouteDirectionInfo direction;
	@Nullable
	private final TargetPoint targetPoint;
	@Nullable
	private final LocationPointWrapper locationPoint;
	private final int intermediateIndex;
	private final int directionIndex;
	private final int routePointOffset;
	private final int cumulativeDistance;
	private final int cumulativeTime;

	private RouteDetailsItem(@NonNull Type type, @Nullable RouteDirectionInfo direction,
	                         @Nullable TargetPoint targetPoint,
	                         @Nullable LocationPointWrapper locationPoint, int intermediateIndex,
	                         int directionIndex, int routePointOffset, int cumulativeDistance,
	                         int cumulativeTime) {
		this.type = type;
		this.direction = direction;
		this.targetPoint = targetPoint;
		this.locationPoint = locationPoint;
		this.intermediateIndex = intermediateIndex;
		this.directionIndex = directionIndex;
		this.routePointOffset = routePointOffset;
		this.cumulativeDistance = cumulativeDistance;
		this.cumulativeTime = cumulativeTime;
	}

	@NonNull
	static RouteDetailsItem direction(@NonNull RouteDirectionInfo direction, int directionIndex,
	                                  @NonNull RouteCumulativeInfo cumulativeInfo,
	                                  boolean destination) {
		return new RouteDetailsItem(destination ? Type.DESTINATION : Type.MANEUVER, direction,
				null, null, -1, directionIndex, direction.routePointOffset,
				cumulativeInfo.getDistanceMeters(), cumulativeInfo.getTimeSeconds());
	}

	@NonNull
	static RouteDetailsItem intermediate(@Nullable TargetPoint targetPoint, int intermediateIndex,
	                                     int directionIndex, int routePointOffset,
	                                     int cumulativeDistance, int cumulativeTime) {
		return new RouteDetailsItem(Type.INTERMEDIATE, null, targetPoint, null,
				intermediateIndex, directionIndex, routePointOffset, cumulativeDistance,
				cumulativeTime);
	}

	@NonNull
	static RouteDetailsItem alongRoute(@NonNull LocationPointWrapper locationPoint,
	                                  @NonNull RouteCumulativeInfo cumulativeInfo) {
		Type type;
		switch (locationPoint.type) {
			case WaypointHelper.ALARMS:
				type = Type.TRAFFIC_WARNING;
				break;
			case WaypointHelper.POI:
				type = Type.POI;
				break;
			case WaypointHelper.FAVORITES:
				type = Type.FAVORITE;
				break;
			default:
				throw new IllegalArgumentException("Unsupported along-route point type: "
						+ locationPoint.type);
		}
		return new RouteDetailsItem(type, null, null, locationPoint, -1, -1,
				locationPoint.getRouteIndex(), cumulativeInfo.getDistanceMeters(),
				cumulativeInfo.getTimeSeconds());
	}

	@NonNull
	Type getType() {
		return type;
	}

	boolean isIntermediate() {
		return type == Type.INTERMEDIATE;
	}

	boolean isDestination() {
		return type == Type.DESTINATION;
	}

	boolean isAlongRoute() {
		return type.isAlongRoute();
	}

	@Nullable
	RouteDirectionInfo getDirection() {
		return direction;
	}

	@Nullable
	TargetPoint getTargetPoint() {
		return targetPoint;
	}

	@Nullable
	LocationPointWrapper getLocationPoint() {
		return locationPoint;
	}

	int getIntermediateIndex() {
		return intermediateIndex;
	}

	int getDirectionIndex() {
		return directionIndex;
	}

	int getRoutePointOffset() {
		return routePointOffset;
	}

	int getCumulativeDistance() {
		return cumulativeDistance;
	}

	int getCumulativeTime() {
		return cumulativeTime;
	}
}
