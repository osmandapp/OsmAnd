package net.osmand.plus.plugins.development.widget;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_ZOOM_LEVEL;

public class ZoomLevelWidget extends SimpleWidget {

	private final OsmandMap osmandMap;
	private final OsmandMapTileView mapView;

	private int cachedBaseZoom;
	private int cachedZoom;
	private float cachedZoomFloatPart;
	private float cachedMapDensity;

	public ZoomLevelWidget(@NonNull MapActivity mapActivity, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, DEV_ZOOM_LEVEL, customId, widgetsPanel);
		this.osmandMap = app.getOsmandMap();
		this.mapView = mapActivity.getMapView();
		updateInfo(null);
		setIcons(DEV_ZOOM_LEVEL);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		int baseZoom = mapView.getBaseZoom();
		int newZoom = mapView.getZoom();
		float newZoomFloatPart = mapView.getZoomFloatPart() + mapView.getZoomAnimation();
		float newMapDensity = osmandMap.getMapDensity();
		if (isUpdateNeeded()
				|| baseZoom != cachedBaseZoom
				|| newZoom != cachedZoom
				|| newZoomFloatPart != cachedZoomFloatPart
				|| newMapDensity != cachedMapDensity) {
			cachedBaseZoom = baseZoom;
			cachedZoom = newZoom;
			cachedZoomFloatPart = newZoomFloatPart;
			cachedMapDensity = newMapDensity;

			float visualZoom = newZoomFloatPart >= 0.0f
					? 1.0f + newZoomFloatPart
					: 1.0f + 0.5f * newZoomFloatPart;
			float targetPixelScale = (float) Math.pow(2.0, newZoom - baseZoom);
			float offsetFromLogicalZoom = getZoomDeltaFromMapScale(targetPixelScale * visualZoom * newMapDensity);
			float preFormattedOffset = Math.round(Math.abs(offsetFromLogicalZoom) * 100) / 100.0f;
			String formattedOffset = OsmAndFormatter
					.formatValue(preFormattedOffset, "", true, 2, app)
					.value;
			String sign = offsetFromLogicalZoom < 0 ? "-" : "+";
			setText(String.valueOf(baseZoom), sign + formattedOffset);
		}
	}

	private float getZoomDeltaFromMapScale(float mapScale) {
		double log2 = Math.log(mapScale) / Math.log(2);
		boolean powerOfTwo = Math.abs(log2 - (int) log2) < 0.001;

		if (powerOfTwo) {
			return (int) Math.round(log2);
		}

		int prevIntZoom;
		int nextIntZoom;
		if (mapScale >= 1.0f) {
			prevIntZoom = (int) log2;
			nextIntZoom = prevIntZoom + 1;
		} else {
			nextIntZoom = (int) log2;
			prevIntZoom = nextIntZoom - 1;
		}

		float prevPowZoom = (float) Math.pow(2, prevIntZoom);
		float nextPowZoom = (float) Math.pow(2, nextIntZoom);
		double zoomFloatPart = Math.abs(mapScale - prevPowZoom) / (nextPowZoom - prevPowZoom);
		return (float) (prevIntZoom + zoomFloatPart);
	}
}
