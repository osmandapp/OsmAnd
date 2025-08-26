package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.DISTANCE_TO_DESTINATION;
import static net.osmand.plus.views.mapwidgets.WidgetType.INTERMEDIATE_DESTINATION;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.utils.OsmAndFormatterParams;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;

public abstract class DistanceToPointWidget extends SimpleWidget {

	public static final int DISTANCE_CHANGE_THRESHOLD = 10;
	public static final int DESTINATION_REACHED_THRESHOLD = 20;

	private final OsmandMapTileView view;
	private final float[] calculations = new float[1];
	private int cachedMeters;

	public DistanceToPointWidget(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, widgetType, customId, widgetsPanel);
		this.view = mapActivity.getMapView();

		setIcons(widgetType);
		setText(null, null);
	}

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> onClick(view);
	}

	protected void onClick(OsmandMapTileView view) {
		AnimateDraggingMapThread thread = view.getAnimatedDraggingThread();
		LatLon pointToNavigate = getPointToNavigate();
		if (pointToNavigate != null) {
			int fZoom = Math.max(view.getZoom(), 15);
			thread.startMoving(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(), fZoom);
		}
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		int distance = getDistance();
		if (isUpdateNeeded() || cachedMeters == 0 || Math.abs(cachedMeters - distance) > DISTANCE_CHANGE_THRESHOLD) {
			cachedMeters = distance;
			if (cachedMeters <= DESTINATION_REACHED_THRESHOLD) {
				cachedMeters = 0;
				setText(isVerticalWidget() ? NO_VALUE : null, null);
			} else {
				FormattedValue formattedDistance = OsmAndFormatter
						.getFormattedDistanceValue(cachedMeters, app, OsmAndFormatterParams.USE_LOWER_BOUNDS);
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

		public DistanceToDestinationWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel panel) {
			super(mapActivity, DISTANCE_TO_DESTINATION, customId, panel);
		}

		@Override
		public LatLon getPointToNavigate() {
			TargetPoint targetPoint = mapActivity.getPointToNavigate();
			return targetPoint == null ? null : targetPoint.getLatLon();
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

		public DistanceToIntermediateDestinationWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel panel) {
			super(mapActivity, INTERMEDIATE_DESTINATION, customId, panel);
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
			return targetPoint == null ? null : targetPoint.getLatLon();
		}

		@Override
		public int getDistance() {
			return getPointToNavigate() != null && routingHelper.isRouteCalculated()
					? routingHelper.getLeftDistanceNextIntermediate()
					: super.getDistance();
		}
	}
}