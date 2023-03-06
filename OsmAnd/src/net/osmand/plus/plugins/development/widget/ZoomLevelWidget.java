package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_ZOOM_LEVEL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;

public class ZoomLevelWidget extends TextInfoWidget {

	private final OsmandMapTileView mapView;
	private int cachedZoom;
	private float cachedZoomFloatPart;

	public ZoomLevelWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, DEV_ZOOM_LEVEL);
		this.mapView = mapActivity.getMapView();
		updateInfo(null);
		setIcons(DEV_ZOOM_LEVEL);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		int newZoom = mapView.getZoom();
		float newZoomFloatPart = mapView.getZoomFloatPart() + mapView.getZoomAnimation();
		if (isUpdateNeeded() || newZoom != cachedZoom || newZoomFloatPart != cachedZoomFloatPart) {
			cachedZoom = newZoom;
			cachedZoomFloatPart = newZoomFloatPart;

			int formattedZoomFloatPart = Math.round(newZoomFloatPart * 100);
			setText(String.valueOf(cachedZoom), String.valueOf(formattedZoomFloatPart));
		}
	}
}
