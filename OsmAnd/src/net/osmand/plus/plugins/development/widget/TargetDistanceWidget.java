package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_TARGET_DISTANCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class TargetDistanceWidget extends TextInfoWidget {

	private final OsmandMapTileView mapView;
	private float cachedTargetDistance = -1;

	public TargetDistanceWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, WidgetType.DEV_TARGET_DISTANCE);
		this.mapView = mapActivity.getMapView();
		updateInfo(null);
		setIcons(DEV_TARGET_DISTANCE);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		float targetDistance = getTargetDistanceInMeters();
		if (isUpdateNeeded() || targetDistance != cachedTargetDistance) {
			cachedTargetDistance = targetDistance;
			if (cachedTargetDistance > 0) {
				MetricsConstants metricsConstants = settings.METRIC_SYSTEM.get();
				OsmAndFormatter.FormattedValue formattedDistance = OsmAndFormatter.getFormattedDistanceValue(cachedTargetDistance,
						app, false, metricsConstants);
				setText(formattedDistance.value, formattedDistance.unit);
			} else {
				setText(NO_VALUE, null);
			}
		}
	}

	private float getTargetDistanceInMeters() {
		MapRendererView mapRenderer = mapView.getMapRenderer();
		if (mapRenderer != null) {
			return NativeUtilities.getMetersToFixedElevatedTarget(mapRenderer);
		}
		return 0;
	}
}