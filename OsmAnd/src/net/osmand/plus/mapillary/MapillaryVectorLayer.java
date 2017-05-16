package net.osmand.plus.mapillary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import net.osmand.data.GeometryTile;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.LinkedHashMap;
import java.util.Map;

class MapillaryVectorLayer extends MapTileLayer implements MapillaryLayer {

	private static final int TILE_ZOOM = 14;

	private LatLon selectedImageLocation;
	private Float selectedImageCameraAngle;
	private Bitmap selectedImage;
	private Bitmap headingImage;
	private Paint paintIcon;
	private Bitmap point;

	MapillaryVectorLayer() {
		super(false);
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		super.initLayer(view);
		paintIcon = new Paint();
		selectedImage = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_default_location);
		headingImage = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pedestrian_location_view_angle);
		point = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_note_small);
	}

	@Override
	public void setSelectedImageLocation(LatLon selectedImageLocation) {
		this.selectedImageLocation = selectedImageLocation;
	}

	@Override
	public void setSelectedImageCameraAngle(Float selectedImageCameraAngle) {
		this.selectedImageCameraAngle = selectedImageCameraAngle;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		super.onPrepareBufferImage(canvas, tileBox, drawSettings);
		if (selectedImageLocation != null) {
			float x = tileBox.getPixXFromLatLon(selectedImageLocation.getLatitude(), selectedImageLocation.getLongitude());
			float y = tileBox.getPixYFromLatLon(selectedImageLocation.getLatitude(), selectedImageLocation.getLongitude());
			if (selectedImageCameraAngle != null) {
				canvas.save();
				canvas.rotate(selectedImageCameraAngle - 180, x, y);
				canvas.drawBitmap(headingImage, x - headingImage.getWidth() / 2,
						y - headingImage.getHeight() / 2, paintIcon);
				canvas.restore();
			}
			canvas.drawBitmap(selectedImage, x - selectedImage.getWidth() / 2, y - selectedImage.getHeight() / 2, paintIcon);
		}
	}

	@Override
	public void drawTileMap(Canvas canvas, RotatedTileBox tileBox) {
		ITileSource map = this.map;
		if (map == null) {
			return;
		}
		int nzoom = tileBox.getZoom();
		if (nzoom < map.getMinimumZoomSupported()) {
			return;
		}
		ResourceManager mgr = resourceManager;
		final QuadRect tilesRect = tileBox.getTileBounds();

		// recalculate for ellipsoid coordinates
		float ellipticTileCorrection  = 0;
		if (map.isEllipticYTile()) {
			ellipticTileCorrection = (float) (MapUtils.getTileEllipsoidNumberY(nzoom, tileBox.getLatitude()) - tileBox.getCenterTileY());
		}

		int left = (int) Math.floor(tilesRect.left);
		int top = (int) Math.floor(tilesRect.top + ellipticTileCorrection);
		int width = (int) Math.ceil(tilesRect.right - left);
		int height = (int) Math.ceil(tilesRect.bottom + ellipticTileCorrection - top);

		boolean useInternet = (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) != null || OsmandPlugin.getEnabledPlugin(MapillaryPlugin.class) != null) &&
				settings.USE_INTERNET_TO_DOWNLOAD_TILES.get() && settings.isInternetConnectionAvailable() && map.couldBeDownloadedFromInternet();

		Map<String, GeometryTile> tiles = new LinkedHashMap<>();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int leftPlusI = left + i;
				int topPlusJ = top + j;

				int x1 = tileBox.getPixXFromTileXNoRot(leftPlusI);
				int x2 = tileBox.getPixXFromTileXNoRot(leftPlusI + 1);

				int y1 = tileBox.getPixYFromTileYNoRot(topPlusJ - ellipticTileCorrection);
				int y2 = tileBox.getPixYFromTileYNoRot(topPlusJ + 1 - ellipticTileCorrection);
				bitmapToDraw.set(x1, y1, x2, y2);

				int tileX = leftPlusI;
				int tileY = topPlusJ;

				//String tileId = mgr.calculateTileId(map, tileX, tileY, nzoom);
				int dzoom = nzoom - TILE_ZOOM;
				int div = (int) Math.pow(2.0, dzoom);
				tileX /= div;
				tileY /= div;
				String tileId = mgr.calculateTileId(map, tileX, tileY, TILE_ZOOM);
				GeometryTile tile = tiles.get(tileId);
				if (tile == null) {
					// asking tile image async
					boolean imgExist = mgr.tileExistOnFileSystem(tileId, map, tileX, tileY, TILE_ZOOM);
					if (imgExist || useInternet) {
						tile = mgr.getGeometryTilesCache().getTileForMapAsync(tileId, map, tileX, tileY, TILE_ZOOM, useInternet);
					}
					if (tile != null) {
						tiles.put(tileId, tile);
						if (tile.getData() != null) {
							drawPoints(canvas, tileBox, tileX, tileY, tile);
						}
					}
				}
			}
		}
	}

	private void drawPoints(Canvas canvas, RotatedTileBox tileBox, int tileX, int tileY, GeometryTile tile) {
		for (Geometry g : tile.getData()) {
			if (g instanceof Point && g.getCoordinate() != null) {
				int x = (int) g.getCoordinate().x;
				int y = (int) g.getCoordinate().y;
				double lat = MapUtils.getLatitudeFromTile(TILE_ZOOM, tileY + y / 4096f);
				double lon = MapUtils.getLongitudeFromTile(TILE_ZOOM, tileX + x / 4096f);
				if (tileBox.containsLatLon(lat, lon)) {
					float px = tileBox.getPixXFromLatLon(lat, lon);
					float py = tileBox.getPixYFromLatLon(lat, lon);
					canvas.drawBitmap(point, px - point.getWidth() / 2, py - point.getHeight() / 2, paintIcon);
				}
			}
		}
	}
}