package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

import static net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget.DESTINATION_REACHED_THRESHOLD;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RouteInfoCalculator {

	private static final int POINTS_LIMIT = 2;

	private final MapActivity mapActivity;
	private final OsmandMapTileView mapTileView;
	private final RoutingHelper routingHelper;
	private final TargetPointsHelper targetPointsHelper;

	public RouteInfoCalculator(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getApp();
		this.mapActivity = mapActivity;
		this.mapTileView = mapActivity.getMapView();
		this.routingHelper = app.getRoutingHelper();
		this.targetPointsHelper = app.getTargetPointsHelper();
	}

	/**
	 * Calculates route information with a priority setting.
	 *
	 * @param priority The display priority (intermediate first or final destination first).
	 * @return A list of DestinationInfo objects representing route points.
	 */
	@NonNull
	public List<DestinationInfo> calculateRouteInformation(@NonNull DisplayPriority priority) {
		DestinationInfo currentIntermediate = getCurrentIntermediateInfo();
		DestinationInfo finalDestination = getFinalDestinationInfo();
		if (currentIntermediate != null && finalDestination != null) {
			return priority == DisplayPriority.INTERMEDIATE_FIRST
					? Arrays.asList(currentIntermediate, finalDestination)
					: Arrays.asList(finalDestination, currentIntermediate);
		} else if (finalDestination != null) {
			return Collections.singletonList(finalDestination);
		}
		return Collections.emptyList();
	}

	/**
	 * Calculates route information, considering intermediate and final destination points.
	 *
	 * @return A list of DestinationInfo objects representing the upcoming route points.
	 */
	@NonNull
	public List<DestinationInfo> calculateRouteInformation() {
		List<DestinationInfo> intermediates = collectNotPassedIntermediatePoints(POINTS_LIMIT);
		List<DestinationInfo> result = new ArrayList<>(intermediates);

		if (result.size() < POINTS_LIMIT) {
			DestinationInfo finalDestination = getFinalDestinationInfo();
			if (finalDestination != null) {
				result.add(finalDestination);
			}
		}
		return result;
	}

	@Nullable
	private DestinationInfo getCurrentIntermediateInfo() {
		List<DestinationInfo> intermediateInfos = collectNotPassedIntermediatePoints(1);
		return !intermediateInfos.isEmpty() ? intermediateInfos.get(0) : null;
	}

	/**
	 * Collects unpassed intermediate route points, limited by the specified count.
	 *
	 * @param pointsLimit The maximum number of intermediate points to return.
	 * @return A list of DestinationInfo objects for upcoming intermediate points.
	 */
	@NonNull
	private List<DestinationInfo> collectNotPassedIntermediatePoints(int pointsLimit) {
		List<DestinationInfo> result = new ArrayList<>();
		TargetPoint intermediate;
		int index = 0;
		while ((intermediate = targetPointsHelper.getIntermediatePoint(index)) != null) {
			int distance = getDistanceToIntermediate(intermediate.getLatLon(), index);
			int estimatedTime = getEstimatedTimeToIntermediate(index);

			if (isPointNotPassed(distance, estimatedTime)) {
				result.add(createDestinationInfo(distance, estimatedTime));
				if (result.size() >= pointsLimit) {
					break;
				}
			}
			index++;
		}
		return result;
	}

	/**
	 * Retrieves the final destination information if available.
	 *
	 * @return The final DestinationInfo or null if the destination has been reached.
	 */
	@Nullable
	private DestinationInfo getFinalDestinationInfo() {
		TargetPoint destination = mapActivity.getPointToNavigate();
		if (destination != null) {
			int distance = getDistanceToDestination(destination.getLatLon());
			int leftTime = getEstimatedTimeToDestination();
			if (isPointNotPassed(distance, leftTime)) {
				return createDestinationInfo(distance, leftTime);
			}
		}
		return null;
	}

	private int getDistanceToIntermediate(@NonNull LatLon location, int intermediateIndexOffset) {
		if (routingHelper.isRouteCalculated()) {
			return routingHelper.getLeftDistanceToIntermediate(intermediateIndexOffset);
		}
		return calculateDefaultDistance(location);
	}

	private int getDistanceToDestination(@NonNull LatLon location) {
		if (routingHelper.isRouteCalculated()) {
			return routingHelper.getLeftDistance();
		}
		return calculateDefaultDistance(location);
	}

	private int calculateDefaultDistance(@NonNull LatLon location) {
		float[] distanceResult = new float[1];
		Location.distanceBetween(
				mapTileView.getLatitude(),
				mapTileView.getLongitude(),
				location.getLatitude(),
				location.getLongitude(),
				distanceResult
		);
		return (int) distanceResult[0];
	}

	private int getEstimatedTimeToIntermediate(int intermediateIndexOffset) {
		return routingHelper.getLeftTimeNextIntermediate(intermediateIndexOffset);
	}

	private int getEstimatedTimeToDestination() {
		return routingHelper.getLeftTime();
	}

	private boolean isPointNotPassed(int distance, int leftSeconds) {
		return distance > DESTINATION_REACHED_THRESHOLD && leftSeconds > 0;
	}

	@NonNull
	private DestinationInfo createDestinationInfo(int distance, int leftSeconds) {
		long timeToGo = leftSeconds * 1000L;
		long arrivalTime = System.currentTimeMillis() + timeToGo;
		return new DestinationInfo(distance, arrivalTime, timeToGo);
	}

	public record DestinationInfo(int distance, long arrivalTime, long timeToGo) { }
}
