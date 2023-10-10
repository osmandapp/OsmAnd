package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_CAMERA_TILT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

public class CameraTiltWidget extends SimpleWidget {
	private final OsmandMapTileView mapView;
	private double cachedMapTilt;

	public CameraTiltWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, DEV_CAMERA_TILT, customId, widgetsPanel);
		this.mapView = mapActivity.getMapView();
		updateSimpleWidgetInfo(null);
		setIcons(DEV_CAMERA_TILT);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		double mapTilt = mapView.getElevationAngle();
		if (isUpdateNeeded() || Math.abs(mapTilt - cachedMapTilt) > 0.05d) {
			cachedMapTilt = mapTilt;
			setText(String.format("%.1f °", cachedMapTilt ), null);
		}
	}
}