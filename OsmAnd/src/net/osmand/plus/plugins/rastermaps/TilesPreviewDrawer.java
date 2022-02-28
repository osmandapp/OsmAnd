package net.osmand.plus.plugins.rastermaps;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.base.BaseMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

public class TilesPreviewDrawer {

	private final OsmandApplication app;
	private final OsmandMapTileView mapView;
	private final MapTileLayer previewTilesLayer;

	private final int previewSize;

	private final Canvas minZoomCanvas;
	private final Canvas maxZoomCanvas;

	private final Bitmap minZoomBitmap;
	private final Bitmap maxZoomBitmap;

	public TilesPreviewDrawer(@NonNull OsmandApplication app) {
		this.app = app;
		this.mapView = app.getOsmandMap().getMapView();
		this.previewTilesLayer = new MapTileLayer(app, false, false);
		this.previewSize = app.getResources().getDimensionPixelSize(R.dimen.map_tile_preview_size);

		minZoomBitmap = Bitmap.createBitmap(previewSize, previewSize, Config.ARGB_8888);
		maxZoomBitmap = Bitmap.createBitmap(previewSize, previewSize, Config.ARGB_8888);

		minZoomCanvas = new Canvas(minZoomBitmap);
		maxZoomCanvas = new Canvas(maxZoomBitmap);
	}

	@NonNull
	public Pair<Bitmap, Bitmap> drawTilesPreview(@NonNull LatLon center, int minZoom, int maxZoom) {
		minZoomBitmap.eraseColor(Color.TRANSPARENT);
		maxZoomBitmap.eraseColor(Color.TRANSPARENT);
		if (updateTileSource()) {
			boolean night = app.getDaynightHelper().isNightModeForMapControls();
			DrawSettings drawSettings = new DrawSettings(night);
			drawTilePreview(minZoomCanvas, center, minZoom, drawSettings);
			drawTilePreview(maxZoomCanvas, center, maxZoom, drawSettings);
		}
		return Pair.create(minZoomBitmap, maxZoomBitmap);
	}

	private boolean updateTileSource() {
		BaseMapLayer mainLayer = mapView.getMainLayer();
		MapTileLayer mapTileLayer = mainLayer instanceof MapTileLayer ? ((MapTileLayer) mainLayer) : null;
		if (mapTileLayer == null) {
			return false;
		}
		previewTilesLayer.setMap(mapTileLayer.getMap());
		return true;
	}

	private void drawTilePreview(@NonNull Canvas canvas, @NonNull LatLon center, int zoom, @NonNull DrawSettings drawSettings) {
		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox().copy();
		tileBox.setLatLonCenter(center.getLatitude(), center.getLongitude());
		tileBox.setZoom(zoom);
		tileBox.setPixelDimensions(previewSize, previewSize);
		previewTilesLayer.drawTileMap(canvas, tileBox, drawSettings);
	}
}