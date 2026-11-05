package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_TARGET_DISTANCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

public class TargetDistanceWidget extends SimpleWidget {

	private final OsmandMapTileView mapView;
	private float cachedTargetDistance = -1;

	public TargetDistanceWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, DEV_TARGET_DISTANCE, customId, widgetsPanel);
		this.mapView = mapActivity.getMapView();
		updateSimpleWidgetInfo(null);
		setIcons(DEV_TARGET_DISTANCE);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		float targetDistance = getTargetDistanceInMeters();
		if (isUpdateNeeded() || targetDistance != cachedTargetDistance) {
			cachedTargetDistance = targetDistance;
			if (cachedTargetDistance > 0) {
				OsmAndFormatter.FormattedValue formattedDistance = OsmAndFormatter.getFormattedDistanceValue(cachedTargetDistance, app);
				setText(formattedDistance.value, formattedDistance.unit);
			} else {
				setText(NO_VALUE, null);
			}
		}
	}

	private float getTargetDistanceInMeters() {
		MapRendererView mapRenderer = mapView.getMapRenderer();
		if (mapRenderer != null) {
			PointI screenPoint = mapRenderer.getTargetScreenPosition();
			if (screenPoint.getX() < 0 || screenPoint.getY() < 0) {
				RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();
				screenPoint = new PointI(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());				
			}
			PointI location = new PointI();
			if (mapRenderer.getLocationFromElevatedPoint(screenPoint, location)) {
				return mapRenderer.getMapTargetDistance(location, true) * 1000;
			}			
		}
		return 0;
	}

}