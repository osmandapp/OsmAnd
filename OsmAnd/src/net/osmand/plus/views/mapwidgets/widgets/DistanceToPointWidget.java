package net.osmand.plus.views.mapwidgets.widgets;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class DistanceToPointWidget extends TextInfoWidget {

	private static final int DISTANCE_CHANGE_THRESHOLD = 10;
	private static final int DESTINATION_REACHED_THRESHOLD = 20;

	private final OsmandMapTileView view;
	private final float[] calculations = new float[1];
	private int cachedMeters;

	public DistanceToPointWidget(@NonNull MapActivity mapActivity, @NonNull WidgetParams widgetParams) {
		super(mapActivity);
		this.view = mapActivity.getMapView();

		setIcons(widgetParams);
		setText(null, null);
		setOnClickListener(v -> onClick(view));
	}

	protected void onClick(final OsmandMapTileView view) {
		AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
		LatLon pointToNavigate = getPointToNavigate();
		if (pointToNavigate != null) {
			int fZoom = Math.max(view.getZoom(), 15);
			thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom, true);
		}
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		int distance = getDistance();
		if (isUpdateNeeded() || cachedMeters == 0 || Math.abs(cachedMeters - distance) > DISTANCE_CHANGE_THRESHOLD) {
			cachedMeters = distance;
			if (cachedMeters <= DESTINATION_REACHED_THRESHOLD) {
				cachedMeters = 0;
				setText(null, null);
			} else {
				FormattedValue formattedDistance = OsmAndFormatter
						.getFormattedDistanceValue(cachedMeters, app, true, settings.METRIC_SYSTEM.get());
				setText(formattedDistance.value, formattedDistance.unit);
			}
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}

	public abstract LatLon getPointToNavigate();

	public int getDistance() {
		int d = 0;
		LatLon l = getPointToNavigate();
		if (l != null) {
			Location.distanceBetween(view.getLatitude(), view.getLongitude(), l.getLatitude(), l.getLongitude(), calculations);
			d = (int) calculations[0];
		}
		return d;
	}

	public static class DistanceToDestinationWidget extends DistanceToPointWidget {

		public DistanceToDestinationWidget(@NonNull MapActivity mapActivity) {
			super(mapActivity, WidgetParams.DISTANCE_TO_DESTINATION);
		}

		@Override
		public LatLon getPointToNavigate() {
			TargetPoint targetPoint = mapActivity.getPointToNavigate();
			return targetPoint == null ? null : targetPoint.point;
		}

		@Override
		public int getDistance() {
			return routingHelper.isRouteCalculated()
					? routingHelper.getLeftDistance()
					: super.getDistance();
		}
	}

	public static class DistanceToIntermediateDestinationWidget extends DistanceToPointWidget {

		private final TargetPointsHelper targetPointsHelper;

		public DistanceToIntermediateDestinationWidget(@NonNull MapActivity mapActivity) {
			super(mapActivity, WidgetParams.INTERMEDIATE_DESTINATION);
			targetPointsHelper = app.getTargetPointsHelper();
		}

		@Override
		protected void onClick(OsmandMapTileView view) {
			if (targetPointsHelper.getIntermediatePoints().size() > 1) {
				mapActivity.getMapActions().openIntermediatePointsDialog();
			} else {
				super.onClick(view);
			}
		}

		@Override
		public LatLon getPointToNavigate() {
			TargetPoint targetPoint = targetPointsHelper.getFirstIntermediatePoint();
			return targetPoint == null ? null : targetPoint.point;
		}

		@Override
		public int getDistance() {
			return getPointToNavigate() != null && routingHelper.isRouteCalculated()
					? routingHelper.getLeftDistanceNextIntermediate()
					: super.getDistance();
		}
	}
}