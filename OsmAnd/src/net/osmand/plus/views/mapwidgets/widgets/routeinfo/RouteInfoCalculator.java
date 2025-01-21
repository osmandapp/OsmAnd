package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

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

	private final OsmandApplication app;
	private final MapActivity mapActivity;
	private final OsmandMapTileView mapTileView;
	private final RoutingHelper routingHelper;
	private final TargetPointsHelper targetPointsHelper;

	public RouteInfoCalculator(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
		this.mapTileView = mapActivity.getMapView();
		this.routingHelper = app.getRoutingHelper();
		this.targetPointsHelper = app.getTargetPointsHelper();
	}

	@NonNull
	public List<DestinationInfo> calculateRouteInformation() {
		List<DestinationInfo> destinationInfos = new ArrayList<>();

		TargetPoint currentIntermediate = targetPointsHelper.getIntermediatePoint(0);
		TargetPoint nextIntermediate = targetPointsHelper.getIntermediatePoint(1);
		TargetPoint finalDestination = mapActivity.getPointToNavigate();

		if (currentIntermediate != null) {
			addIntermediateInfo(destinationInfos, currentIntermediate, 0);

			if (nextIntermediate != null) {
				addIntermediateInfo(destinationInfos, nextIntermediate, 1);
			}
		}

		if (finalDestination != null) {
			addFinalDestinationInfo(destinationInfos, finalDestination);
		}

		return destinationInfos;
	}

	private void addIntermediateInfo(@NonNull List<DestinationInfo> destinationInfos,
	                                 @NonNull TargetPoint intermediate, int intermediateIndexOffset) {
		int distance = getDistanceToIntermediate(intermediate.point, intermediateIndexOffset);
		int estimatedTime = getEstimatedTimeToIntermediate(intermediateIndexOffset);
		destinationInfos.add(createDestinationInfo(distance, estimatedTime));
	}

	private void addFinalDestinationInfo(@NonNull List<DestinationInfo> destinationInfos,
	                                     @NonNull TargetPoint finalDestination) {
		int distance = getDistanceToDestination(finalDestination.point);
		int estimatedTime = getEstimatedTimeToDestination();
		destinationInfos.add(createDestinationInfo(distance, estimatedTime));
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

	@NonNull
	private DestinationInfo createDestinationInfo(int distance, int estimatedTime) {
		long arrivalTimestamp = System.currentTimeMillis() + estimatedTime * 1000L;
		return new DestinationInfo(
				RouteInfoWidget.formatDistance(app, distance),
				RouteInfoWidget.formatArrivalTime(arrivalTimestamp),
				RouteInfoWidget.formatDuration(app, estimatedTime * 1000L)
		);
	}

	public record DestinationInfo(String distance, String arrivalTime, String duration) { }
}
