package net.osmand.plus.plugins.rastermaps;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;

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
		this.previewTilesLayer = new MapTileLayer(app, false);
		this.previewTilesLayer.setUpscaleAllowed(false);
		this.previewSize = app.getResources().getDimensionPixelSize(R.dimen.map_tile_preview_size);

		minZoomBitmap = Bitmap.createBitmap(previewSize, previewSize, Config.ARGB_8888);
		maxZoomBitmap = Bitmap.createBitmap(previewSize, previewSize, Config.ARGB_8888);

		minZoomCanvas = new Canvas(minZoomBitmap);
		maxZoomCanvas = new Canvas(maxZoomBitmap);
	}

	@NonNull
	public Pair<Bitmap, Bitmap> drawTilesPreview(@Nullable ITileSource tileSource,
	                                             @NonNull LatLon center, int minZoom, int maxZoom) {
		previewTilesLayer.setMap(tileSource);

		minZoomBitmap.eraseColor(Color.TRANSPARENT);
		maxZoomBitmap.eraseColor(Color.TRANSPARENT);

		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		DrawSettings drawSettings = new DrawSettings(night);
		drawTilePreview(minZoomCanvas, center, minZoom, drawSettings);
		drawTilePreview(maxZoomCanvas, center, maxZoom, drawSettings);

		return Pair.create(minZoomBitmap, maxZoomBitmap);
	}


	private void drawTilePreview(@NonNull Canvas canvas, @NonNull LatLon center, int zoom, @NonNull DrawSettings drawSettings) {
		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox().copy();
		tileBox.setLatLonCenter(center.getLatitude(), center.getLongitude());
		tileBox.setZoom(zoom);
		tileBox.setPixelDimensions(previewSize, previewSize);
		previewTilesLayer.drawTileMap(canvas, tileBox, drawSettings);
	}
}