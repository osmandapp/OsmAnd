package net.osmand.plus.mapillary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.v4.content.ContextCompat;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import net.osmand.AndroidUtils;
import net.osmand.data.GeometryTile;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.MapTileLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class MapillaryVectorLayer extends MapTileLayer implements MapillaryLayer, IContextMenuProvider {

	public static final int TILE_ZOOM = 14;
	public static final double EXTENT = 4096.0;

	private LatLon selectedImageLocation;
	private Float selectedImageCameraAngle;
	private Bitmap selectedImage;
	private Bitmap headingImage;
	private Paint paintPoint;
	private Paint paintLine;
	private Bitmap point;
	private Map<QuadPointDouble, Map> visiblePoints = new HashMap<>();

	MapillaryVectorLayer() {
		super(false);
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		super.initLayer(view);

		paintPoint = new Paint();
		paintLine = new Paint();
		paintLine.setStyle(Paint.Style.STROKE);
		paintLine.setAntiAlias(true);
		paintLine.setColor(ContextCompat.getColor(view.getContext(), R.color.mapillary_color));
		paintLine.setStrokeWidth(AndroidUtils.dpToPx(view.getContext(), 4f));
		//paintLine.setStrokeJoin(Paint.Join.ROUND);
		paintLine.setStrokeCap(Paint.Cap.ROUND);

		selectedImage = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_mapillary_location);
		headingImage = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_mapillary_location_view_angle);
		point = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_mapillary_photo_dot);
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
	public boolean drawInScreenPixels() {
		return true;
	}

	private void drawSelectedPoint(Canvas canvas, RotatedTileBox tileBox) {
		if (selectedImageLocation != null) {
			float x = tileBox.getPixXFromLatLon(selectedImageLocation.getLatitude(), selectedImageLocation.getLongitude());
			float y = tileBox.getPixYFromLatLon(selectedImageLocation.getLatitude(), selectedImageLocation.getLongitude());
			if (selectedImageCameraAngle != null) {
				canvas.save();
				canvas.rotate(selectedImageCameraAngle - 180, x, y);
				canvas.drawBitmap(headingImage, x - headingImage.getWidth() / 2,
						y - headingImage.getHeight() / 2, paintPoint);
				canvas.restore();
			}
			canvas.drawBitmap(selectedImage, x - selectedImage.getWidth() / 2, y - selectedImage.getHeight() / 2, paintPoint);
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
		int dzoom = nzoom - TILE_ZOOM;
		int div = (int) Math.pow(2.0, dzoom);

		boolean useInternet = (OsmandPlugin.getEnabledPlugin(OsmandRasterMapsPlugin.class) != null || OsmandPlugin.getEnabledPlugin(MapillaryPlugin.class) != null) &&
				settings.USE_INTERNET_TO_DOWNLOAD_TILES.get() && settings.isInternetConnectionAvailable() && map.couldBeDownloadedFromInternet();

		Map<String, GeometryTile> tiles = new HashMap<>();
		Map<QuadPointDouble, Map> visiblePoints = new HashMap<>();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int tileX = (left + i) / div;
				int tileY = (top + j) / div;

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
							drawLines(canvas, tileBox, tileX, tileY, tile);
							if (nzoom > 15) {
								drawPoints(canvas, tileBox, tileX, tileY, tile, visiblePoints);
							}
						}
					}
				}
			}
		}
		this.visiblePoints = visiblePoints;
		drawSelectedPoint(canvas, tileBox);
	}

	protected void drawPoints(Canvas canvas, RotatedTileBox tileBox, int tileX, int tileY,
							  GeometryTile tile, Map<QuadPointDouble, Map> visiblePoints) {
		int dzoom = tileBox.getZoom() - TILE_ZOOM;
		int mult = (int) Math.pow(2.0, dzoom);
		QuadRect tileBounds = tileBox.getTileBounds();
		double px, py, tx, ty;
		float x, y;
		float pw = point.getWidth();
		float ph = point.getHeight();
		float pwd = pw / 2;
		float phd = ph / 2;

		for (Geometry g : tile.getData()) {
			if (g instanceof Point && !g.isEmpty() && g.getUserData() != null && g.getUserData() instanceof HashMap) {
				Point p = (Point) g;
				px = p.getCoordinate().x / EXTENT;
				py = p.getCoordinate().y / EXTENT;
				tx = (tileX + px) * mult;
				ty = (tileY + py) * mult;
				if (tileBounds.contains(tx, ty, tx, ty)) {
					if (settings.USE_MAPILLARY_FILTER.get()) {
						if (filtered(p.getUserData())) continue;
					}
					x = tileBox.getPixXFromTile(tileX + px, tileY + py, TILE_ZOOM);
					y = tileBox.getPixYFromTile(tileX + px, tileY + py, TILE_ZOOM);
					canvas.drawBitmap(point, x - pwd, y - phd, paintPoint);
					visiblePoints.put(new QuadPointDouble(tileX + px,  tileY + py), (Map) p.getUserData());
				}
			}
		}
	}

	private boolean filtered(Object data) {
		if (data == null) {
			return true;
		}
		String userKey = settings.MAPILLARY_FILTER_USER_KEY.get();
		HashMap<String, Object> userData = (HashMap<String, Object>) data;
		long capturedAt = ((Number) userData.get("captured_at")).longValue();
		long from = settings.MAPILLARY_FILTER_FROM_DATE.get();
		long to = settings.MAPILLARY_FILTER_TO_DATE.get();
		boolean pano = settings.MAPILLARY_FILTER_PANO.get();

		if (!userKey.equals("")) {
			String key = (String) userData.get("userkey");
			if (!userKey.equals(key)) {
				return true;
			}
		}
		if (from != 0 && to != 0) {
			if (capturedAt < from || capturedAt > to) {
				return true;
			}
		} else if ((from != 0 && capturedAt < from) || (to != 0 && capturedAt > to)) {
			return true;
		}

		if (pano) {
			return (long) userData.get("pano") == 0;
		}
		return false;
	}

	protected void drawLines(Canvas canvas, RotatedTileBox tileBox, int tileX, int tileY, GeometryTile tile) {
		for (Geometry g : tile.getData()) {
			if (g instanceof LineString && !g.isEmpty()) {
				LineString l = (LineString) g;
				if (l.getCoordinateSequence() != null && !l.isEmpty()) {
					if (!filtered(l.getUserData())) {
						draw(l.getCoordinateSequence().toCoordinateArray(), canvas, tileBox, tileX, tileY);
					}
				}
			} else if (g instanceof MultiLineString && !g.isEmpty()) {
				MultiLineString ml = (MultiLineString) g;
				if (!filtered(ml.getUserData())) {
					for (int i = 0; i < ml.getNumGeometries(); i++) {
                        Geometry gm = ml.getGeometryN(i);
                        if (gm instanceof LineString && !gm.isEmpty()) {
                            LineString l = (LineString) gm;
                            if (l.getCoordinateSequence() != null && !l.isEmpty()) {
                                draw(l.getCoordinateSequence().toCoordinateArray(), canvas, tileBox, tileX, tileY);
                            }
                        }
                    }
				}
			}
		}
	}

	protected void draw(Coordinate[] points, Canvas canvas, RotatedTileBox tileBox, int tileX, int tileY) {
		if (points.length > 1) {
			int dzoom = tileBox.getZoom() - TILE_ZOOM;
			int mult = (int) Math.pow(2.0, dzoom);
			QuadRect tileBounds = tileBox.getTileBounds();

			Coordinate lastPt = points[0];
			float x;
			float y;
			float lastx = 0;
			float lasty = 0;
			double px, py, tpx, tpy, tlx, tly;
			double lx = lastPt.x / EXTENT;
			double ly = lastPt.y / EXTENT;
			boolean reCalculateLastXY = true;

			int size = points.length;
			for (int i = 1; i < size; i++) {
				Coordinate pt = points[i];
				px = pt.x / EXTENT;
				py = pt.y / EXTENT;
				tpx = (tileX + px) * mult;
				tpy = (tileY + py) * mult;
				tlx = (tileX + lx) * mult;
				tly = (tileY + ly) * mult;

				if (tileBounds.contains(tpx, tpy, tpx, tpy) || tileBounds.contains(tlx, tly, tlx, tly)) {
					if (reCalculateLastXY) {
						lastx = tileBox.getPixXFromTile(tileX + lx, tileY + ly, TILE_ZOOM);
						lasty = tileBox.getPixYFromTile(tileX + lx, tileY + ly, TILE_ZOOM);
						reCalculateLastXY = false;
					}

					x = tileBox.getPixXFromTile(tileX + px, tileY + py, TILE_ZOOM);
					y = tileBox.getPixYFromTile(tileX + px, tileY + py, TILE_ZOOM);

					if (lastx != x || lasty != y) {
						canvas.drawLine(lastx, lasty, x, y, paintLine);
					}

					lastx = x;
					lasty = y;
				} else {
					reCalculateLastXY = true;
				}
				lx = px;
				ly = py;
			}
		}
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof MapillaryImage) {
			return new PointDescription(PointDescription.POINT_TYPE_MAPILLARY_IMAGE, view.getContext().getString(R.string.mapillary_image));
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects, boolean unknownLocation) {
		if (map != null && tileBox.getZoom() >= map.getMinimumZoomSupported()) {
			getImagesFromPoint(tileBox, point, objects);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof MapillaryImage) {
			MapillaryImage image = (MapillaryImage) o;
			return new LatLon(image.getLatitude(), image.getLongitude());
		}
		return null;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return o instanceof MapillaryImage;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	private void getImagesFromPoint(RotatedTileBox tb, PointF point, List<? super MapillaryImage> images) {
		Map<QuadPointDouble, Map> points = this.visiblePoints;
		if (points != null) {
			float ex = point.x;
			float ey = point.y;
			final int rp = getRadius(tb);
			int radius = rp * 3 / 2;
			float x, y;
			double minSqDist = Double.NaN;
			double sqDist;
			MapillaryImage img = null;
			for (Entry<QuadPointDouble, Map> entry : points.entrySet()) {
				x = tb.getPixXFromTile(entry.getKey().x, entry.getKey().y, TILE_ZOOM);
				y = tb.getPixYFromTile(entry.getKey().x, entry.getKey().y, TILE_ZOOM);
				if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
					sqDist = (x - ex) * (x - ex) + (y - ey) * (y - ey);
					if (img == null || minSqDist > sqDist) {
						minSqDist = sqDist;
						img = new MapillaryImage(MapUtils.getLatitudeFromTile(TILE_ZOOM, entry.getKey().y),
								MapUtils.getLongitudeFromTile(TILE_ZOOM, entry.getKey().x));
						if (!img.setData(entry.getValue())) {
							img = null;
						}
					}
				}
			}
			if (img != null) {
				images.add(img);
			}
		}
	}

	public int getRadius(RotatedTileBox tb) {
		int r;
		final double zoom = tb.getZoom();
		if (zoom < TILE_ZOOM) {
			r = 0;
		} else if (zoom <= 15) {
			r = 10;
		} else if (zoom <= 16) {
			r = 14;
		} else if (zoom <= 17) {
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * view.getScaleCoefficient());
	}
}
