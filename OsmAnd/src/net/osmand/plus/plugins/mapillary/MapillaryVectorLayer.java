package net.osmand.plus.plugins.mapillary;

import static net.osmand.plus.plugins.mapillary.MapillaryImage.CAPTURED_AT_KEY;
import static net.osmand.plus.plugins.mapillary.MapillaryImage.IS_PANORAMIC_KEY;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import net.osmand.data.GeometryTile;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.GeometryTilesCache;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.util.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MapillaryVectorLayer extends MapTileLayer implements MapillaryLayer, IContextMenuProvider {

	public static final int MAX_SEQUENCE_LAYER_ZOOM = 13;
	public static final int MIN_IMAGE_LAYER_ZOOM = 14;
	public static final int MIN_POINTS_ZOOM = 17;
	public static final double EXTENT = 4096.0;

	private final MapillaryPlugin plugin;

	private LatLon selectedImageLocation;
	private Float selectedImageCameraAngle;
	private Bitmap selectedImage;
	private Bitmap headingImage;
	private Paint paintPoint;
	private Paint paintLine;
	private Bitmap point;
	private Map<QuadPointDouble, Map<?, ?>> visiblePoints = new HashMap<>();

	MapillaryVectorLayer(@NonNull Context context) {
		super(context, false);
		plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		paintPoint = new Paint();
		paintLine = new Paint();
		paintLine.setStyle(Paint.Style.STROKE);
		paintLine.setAntiAlias(true);
		paintLine.setColor(ContextCompat.getColor(getContext(), R.color.mapillary_color));
		paintLine.setStrokeWidth(AndroidUtils.dpToPx(getContext(), 4f));
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

	@Override
	public void drawTileMap(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		ITileSource map = this.map;
		if (map == null) {
			return;
		}
		ResourceManager mgr = resourceManager;
		GeometryTilesCache tilesCache = mgr.getMapillaryVectorTilesCache();

		int currentZoom = tileBox.getZoom();
		int tileZoom;
		if (currentZoom < map.getMinimumZoomSupported()) {
			return;
		} else if (currentZoom < MIN_POINTS_ZOOM) {
			tileZoom = MAX_SEQUENCE_LAYER_ZOOM;
			tilesCache.useForMapillarySequenceLayer();
		} else {
			tileZoom = MIN_IMAGE_LAYER_ZOOM;
			tilesCache.useForMapillaryImageLayer();
		}

		// recalculate for ellipsoid coordinates
		float ellipticTileCorrection = 0;
		if (map.isEllipticYTile()) {
			double tileEllipsoidNumberY = MapUtils.getTileEllipsoidNumberY(currentZoom, tileBox.getLatitude());
			ellipticTileCorrection = (float) (tileEllipsoidNumberY - tileBox.getCenterTileY());
		}

		QuadRect tilesRect = tileBox.getTileBounds();
		int left = (int) Math.floor(tilesRect.left);
		int top = (int) Math.floor(tilesRect.top + ellipticTileCorrection);
		int width = (int) Math.ceil(tilesRect.right - left);
		int height = (int) Math.ceil(tilesRect.bottom + ellipticTileCorrection - top);

		int zoomDiff = currentZoom - tileZoom;
		int div = (int) Math.pow(2.0, zoomDiff);

		boolean useInternet = (OsmandPlugin.isActive(OsmandRasterMapsPlugin.class)
				|| OsmandPlugin.isActive(MapillaryPlugin.class))
				&& settings.isInternetConnectionAvailable() && map.couldBeDownloadedFromInternet();

		Map<String, GeometryTile> tiles = new HashMap<>();
		Map<QuadPointDouble, Map<?, ?>> visiblePoints = new HashMap<>();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int tileX = (left + i) / div;
				int tileY = (top + j) / div;

				String tileId = mgr.calculateTileId(map, tileX, tileY, tileZoom);
				GeometryTile tile = tiles.get(tileId);
				if (tile == null) {
					// asking tile image async
					boolean imgExist = tilesCache.isTileDownloaded(tileId, map, tileX, tileY, tileZoom);
					if (imgExist || useInternet) {
						tile = tilesCache.getTileForMapAsync(tileId, map, tileX, tileY,
								tileZoom, useInternet, drawSettings.mapRefreshTimestamp);
					}
					if (tile != null) {
						tiles.put(tileId, tile);
						List<Geometry> geometries = tile.getData();
						if (geometries != null) {
							drawLines(canvas, tileBox, geometries, tileX, tileY, tileZoom);
							if (currentZoom >= MIN_POINTS_ZOOM) {
								Map<QuadPointDouble, Map<?, ?>> drawnPoints = drawPoints(canvas, tileBox, geometries, tileX, tileY);
								visiblePoints.putAll(drawnPoints);
							}
						}
					}
				}
			}
		}
		this.visiblePoints = visiblePoints;
		drawSelectedPoint(canvas, tileBox);
	}

	private void drawLines(Canvas canvas, RotatedTileBox tileBox, List<Geometry> geometries,
	                       int tileX, int tileY, int tileZoom) {
		for (Geometry geometry : geometries) {
			if (geometry.isEmpty() || filtered(geometry.getUserData())) {
				continue;
			}

			if (geometry instanceof MultiLineString) {
				drawMultiLineString(canvas, tileBox, (MultiLineString) geometry, tileX, tileY, tileZoom);
			} else if (geometry instanceof LineString) {
				drawLineString(canvas, tileBox, (LineString) geometry, tileX, tileY, tileZoom);
			}
		}
	}

	private void drawMultiLineString(Canvas canvas, RotatedTileBox tileBox, MultiLineString multiLineString,
	                                 int tileX, int tileY, int tileZoom) {
		for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
			Geometry geometry = multiLineString.getGeometryN(i);
			if (geometry instanceof LineString && !geometry.isEmpty()) {
				drawLineString(canvas, tileBox, ((LineString) geometry), tileX, tileY, tileZoom);
			}
		}
	}

	private void drawLineString(Canvas canvas, RotatedTileBox tileBox, LineString line,
	                            int tileX, int tileY, int tileZoom) {
		Coordinate[] coordinates = line.getCoordinateSequence().toCoordinateArray();
		draw(coordinates, canvas, tileBox, tileX, tileY, tileZoom);
	}

	private void draw(Coordinate[] points, Canvas canvas, RotatedTileBox tileBox,
	                  int tileX, int tileY, int tileZoom) {
		if (points.length > 1) {
			int zoomDiff = tileBox.getZoom() - tileZoom;
			int mult = (int) Math.pow(2.0, zoomDiff);
			QuadRect tileBounds = tileBox.getTileBounds();

			Coordinate lastPt = points[0];
			float x;
			float y;
			float lastX = 0;
			float lastY = 0;
			double px, py, tpx, tpy, tlx, tly;
			double lx = lastPt.x / EXTENT;
			double ly = lastPt.y / EXTENT;
			boolean recalculateLastXY = true;

			int size = points.length;
			for (int i = 1; i < size; i++) {
				Coordinate pt = points[i];
				px = pt.x / EXTENT;
				py = pt.y / EXTENT;
				tpx = (tileX + px) * mult;
				tpy = (tileY + py) * mult;
				tlx = (tileX + lx) * mult;
				tly = (tileY + ly) * mult;

				if (Math.min(tpx, tlx) < tileBounds.right && Math.max(tpx, tlx) > tileBounds.left
						&& Math.max(tpy, tly) > tileBounds.top && Math.min(tpy, tly) < tileBounds.bottom) {
					if (recalculateLastXY) {
						lastX = tileBox.getPixXFromTile(tileX + lx, tileY + ly, tileZoom);
						lastY = tileBox.getPixYFromTile(tileX + lx, tileY + ly, tileZoom);
						recalculateLastXY = false;
					}

					x = tileBox.getPixXFromTile(tileX + px, tileY + py, tileZoom);
					y = tileBox.getPixYFromTile(tileX + px, tileY + py, tileZoom);

					if (lastX != x || lastY != y) {
						canvas.drawLine(lastX, lastY, x, y, paintLine);
					}

					lastX = x;
					lastY = y;
				} else {
					recalculateLastXY = true;
				}
				lx = px;
				ly = py;
			}
		}
	}

	private Map<QuadPointDouble, Map<?, ?>> drawPoints(Canvas canvas, RotatedTileBox tileBox,
	                                             List<Geometry> geometries, int tileX, int tileY) {
		int dzoom = tileBox.getZoom() - MIN_IMAGE_LAYER_ZOOM;
		int mult = (int) Math.pow(2.0, dzoom);
		QuadRect tileBounds = tileBox.getTileBounds();
		double px, py, tx, ty;
		float x, y;
		float pw = point.getWidth();
		float ph = point.getHeight();
		float pwd = pw / 2;
		float phd = ph / 2;

		Map<QuadPointDouble, Map<?, ?>> visiblePoints = new HashMap<>();
		for (Geometry g : geometries) {
			Map<?, ?> userData = g.getUserData() instanceof HashMap ? ((HashMap<?, ?>) g.getUserData()) : null;
			if (g instanceof Point && !g.isEmpty() && userData != null) {
				Point p = (Point) g;
				px = p.getCoordinate().x / EXTENT;
				py = p.getCoordinate().y / EXTENT;
				tx = (tileX + px) * mult;
				ty = (tileY + py) * mult;
				if (tileBounds.contains(tx, ty, tx, ty) && !filtered(userData)) {
					x = tileBox.getPixXFromTile(tileX + px, tileY + py, MIN_IMAGE_LAYER_ZOOM);
					y = tileBox.getPixYFromTile(tileX + px, tileY + py, MIN_IMAGE_LAYER_ZOOM);
					canvas.drawBitmap(point, x - pwd, y - phd, paintPoint);
					visiblePoints.put(new QuadPointDouble(tileX + px, tileY + py), userData);
				}
			}
		}

		return visiblePoints;
	}

	private boolean filtered(Object data) {
		if (data == null) {
			return true;
		}

		boolean shouldFilter = plugin.USE_MAPILLARY_FILTER.get();
//		String userKey = plugin.MAPILLARY_FILTER_USER_KEY.get();
		long from = plugin.MAPILLARY_FILTER_FROM_DATE.get();
		long to = plugin.MAPILLARY_FILTER_TO_DATE.get();
		boolean pano = plugin.MAPILLARY_FILTER_PANO.get();

		HashMap<String, Object> userData = (HashMap<String, Object>) data;
		long capturedAt = ((Number) userData.get(CAPTURED_AT_KEY)).longValue();

		if (shouldFilter) {
//  		Filter by user name unavailable in current API version
//			if (!userKey.isEmpty()) {
//				String key = (String) userData.get("userkey");
//				if (!userKey.equals(key)) {
//					return true;
//				}
//			}

			if (from != 0 && to != 0) {
				if (capturedAt < from || capturedAt > to) {
					return true;
				}
			} else if ((from != 0 && capturedAt < from) || (to != 0 && capturedAt > to)) {
				return true;
			}
		}

		// Always filter by image type
		if (pano) {
			boolean isPanoramicImage = (boolean) userData.get(IS_PANORAMIC_KEY);
			return !isPanoramicImage;
		}
		return false;
	}

	private void drawSelectedPoint(Canvas canvas, RotatedTileBox tileBox) {
		if (selectedImageLocation != null) {
			double imageLat = selectedImageLocation.getLatitude();
			double imageLon = selectedImageLocation.getLongitude();
			float x = tileBox.getPixXFromLatLon(imageLat, imageLon);
			float y = tileBox.getPixYFromLatLon(imageLat, imageLon);
			if (selectedImageCameraAngle != null) {
				canvas.save();
				canvas.rotate(selectedImageCameraAngle - 180, x, y);
				float headingLeftBound = x - headingImage.getWidth() / 2f;
				float headingTopBound = y - headingImage.getHeight() / 2f;
				canvas.drawBitmap(headingImage, headingLeftBound, headingTopBound, paintPoint);
				canvas.restore();
			}
			float pointLeftBound = x - selectedImage.getWidth() / 2f;
			float pointTopBound = y - selectedImage.getHeight() / 2f;
			canvas.drawBitmap(selectedImage, pointLeftBound, pointTopBound, paintPoint);
		}
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof MapillaryImage) {
			String description = getContext().getString(R.string.mapillary_image);
			return new PointDescription(PointDescription.POINT_TYPE_MAPILLARY_IMAGE, description);
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects, boolean unknownLocation) {
		if (map != null && tileBox.getZoom() >= MIN_POINTS_ZOOM) {
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

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	private void getImagesFromPoint(RotatedTileBox tb, PointF point, List<? super MapillaryImage> images) {
		Map<QuadPointDouble, Map<?, ?>> points = this.visiblePoints;
		float ex = point.x;
		float ey = point.y;
		int rp = getRadius(tb);
		int radius = rp * 3 / 2;
		float x, y;
		double minSqDist = Double.NaN;
		double sqDist;
		MapillaryImage img = null;

		for (Entry<QuadPointDouble, Map<?, ?>> entry : points.entrySet()) {
			double tileX = entry.getKey().x;
			double tileY = entry.getKey().y;
			Map<?, ?> userData = entry.getValue();

			PointF pixel = NativeUtilities.getPixelFromLatLon(getMapRenderer(), tb,
					MapUtils.getLatitudeFromTile(MIN_IMAGE_LAYER_ZOOM, tileY),
					MapUtils.getLongitudeFromTile(MIN_IMAGE_LAYER_ZOOM, tileX));
			x = pixel.x;
			y = pixel.y;
			if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
				sqDist = (x - ex) * (x - ex) + (y - ey) * (y - ey);
				if (img == null || minSqDist > sqDist) {
					minSqDist = sqDist;
					double lat = MapUtils.getLatitudeFromTile(MIN_IMAGE_LAYER_ZOOM, tileY);
					double lon = MapUtils.getLongitudeFromTile(MIN_IMAGE_LAYER_ZOOM, tileX);
					img = new MapillaryImage(lat, lon);
					if (!img.setData(userData)) {
						img = null;
					}
				}
			}
		}
		if (img != null) {
			images.add(img);
		}
	}

	public int getRadius(RotatedTileBox tb) {
		int r;
		double zoom = tb.getZoom();
		if (zoom < MIN_IMAGE_LAYER_ZOOM) {
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
