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

	private static final int NOT_ALONG_ROUTE = -1;

	enum Type {
		INTERMEDIATE(NOT_ALONG_ROUTE, 0),
		TRAFFIC_WARNING(WaypointHelper.ALARMS, 1),
		POI(WaypointHelper.POI, 2),
		FAVORITE(WaypointHelper.FAVORITES, 3),
		MANEUVER(NOT_ALONG_ROUTE, 4),
		DESTINATION(NOT_ALONG_ROUTE, 5);

		private final int waypointType;
		private final int orderAtSameRoutePoint;

		Type(int waypointType, int orderAtSameRoutePoint) {
			this.waypointType = waypointType;
			this.orderAtSameRoutePoint = orderAtSameRoutePoint;
		}

		boolean isAlongRoute() {
			return waypointType != NOT_ALONG_ROUTE;
		}

		int getWaypointType() {
			return waypointType;
		}

		int getOrderAtSameRoutePoint() {
			return orderAtSameRoutePoint;
		}

		@NonNull
		static Type ofWaypointType(int waypointType) {
			for (Type type : values()) {
				if (type.isAlongRoute() && type.waypointType == waypointType) {
					return type;
				}
			}
			throw new IllegalArgumentException("Unsupported along-route point type: " + waypointType);
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

	/**
	 * @param routePointOffset geometry index the [cumulativeInfo] was calculated for, which is the
	 * point's route index clamped to the current route; it must stay in sync with that calculation
	 * because it is also the sort key of the row.
	 */
	@NonNull
	static RouteDetailsItem alongRoute(@NonNull LocationPointWrapper locationPoint,
	                                   int routePointOffset,
	                                   @NonNull RouteCumulativeInfo cumulativeInfo) {
		return new RouteDetailsItem(Type.ofWaypointType(locationPoint.type), null, null,
				locationPoint, -1, -1, routePointOffset, cumulativeInfo.getDistanceMeters(),
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
