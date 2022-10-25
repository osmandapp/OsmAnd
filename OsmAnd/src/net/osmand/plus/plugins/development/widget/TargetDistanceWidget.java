package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_TARGET_DISTANCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class TargetDistanceWidget extends TextInfoWidget {

	private static final String NO_VALUE = "—";

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
			String text = cachedTargetDistance > 0 ? formatDistance(cachedTargetDistance) : NO_VALUE;
			setText(text, null);
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

	private String formatDistance(float distanceInMeters) {
		return OsmAndFormatter.getFormattedDistance(distanceInMeters, app, true,
				MetricsConstants.KILOMETERS_AND_METERS);
	}

}