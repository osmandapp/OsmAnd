package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

import static net.osmand.plus.views.mapwidgets.widgets.DistanceToPointWidget.DESTINATION_REACHED_THRESHOLD;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.OsmandMapTileView;

import java.util.ArrayList;
import java.util.List;

public class RouteInfoCalculator {

	private static final int POINTS_LIMIT = 2;

	private final MapActivity mapActivity;
	private final OsmandMapTileView mapTileView;
	private final RoutingHelper routingHelper;
	private final TargetPointsHelper targetPointsHelper;

	public RouteInfoCalculator(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		this.mapActivity = mapActivity;
		this.mapTileView = mapActivity.getMapView();
		this.routingHelper = app.getRoutingHelper();
		this.targetPointsHelper = app.getTargetPointsHelper();
	}

	@NonNull
	public List<DestinationInfo> calculateRouteInformation() {
		List<DestinationInfo> intermediateInfos = collectNotPassedIntermediatePoints();
		List<DestinationInfo> destinationInfos = new ArrayList<>(intermediateInfos);

		if (destinationInfos.size() < POINTS_LIMIT) {
			TargetPoint finalDestination = mapActivity.getPointToNavigate();
			if (finalDestination != null) {
				int distance = getDistanceToDestination(finalDestination.point);
				int leftTime = getEstimatedTimeToDestination();
				if (isPointNotPassed(distance, leftTime)) {
					destinationInfos.add(createDestinationInfo(distance, leftTime));
				}
			}
		}
		return destinationInfos;
	}

	@NonNull
	private List<DestinationInfo> collectNotPassedIntermediatePoints() {
		List<DestinationInfo> result = new ArrayList<>();

		TargetPoint intermediate;
		int intermediatePointIndex = 0;
		while ((intermediate = targetPointsHelper.getIntermediatePoint(intermediatePointIndex)) != null) {
			int distance = getDistanceToIntermediate(intermediate.point, intermediatePointIndex);
			int estimatedTime = getEstimatedTimeToIntermediate(intermediatePointIndex);

			if (isPointNotPassed(distance, estimatedTime)) {
				result.add(createDestinationInfo(distance, estimatedTime));
				if (result.size() == POINTS_LIMIT) {
					break;
				}
			}
			intermediatePointIndex++;
		}
		return result;
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
