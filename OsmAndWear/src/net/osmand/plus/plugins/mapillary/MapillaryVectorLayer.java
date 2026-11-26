package net.osmand.plus.plugins.mapillary;

import static net.osmand.plus.plugins.mapillary.MapillaryImage.CAPTURED_AT_KEY;
import static net.osmand.plus.plugins.mapillary.MapillaryImage.IS_PANORAMIC_KEY;

import android.content.Context;
import android.graphics.Bitmap;
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

import net.osmand.core.android.MapRendererView;
import net.osmand.core.android.MapillaryTilesProvider;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.data.GeometryTile;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.GeometryTilesCache;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
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
	private boolean carView;
	private float textScale = 1f;
	private Map<QuadPointDouble, Map<?, ?>> visiblePoints = new HashMap<>();

	//OpenGL
	private MapillaryTilesProvider mapillaryTilesProvider;
	private MapMarkersCollection mapMarkersCollection;
	Bitmap selectedImageBitmap;
	Bitmap headingImageBitmap;
	private long filterKey = 0;

	MapillaryVectorLayer(@NonNull Context context) {
		super(context, false);
		plugin = PluginsHelper.getPlugin(MapillaryPlugin.class);
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

		updateBitmaps(true);
	}
	private void updateBitmaps(boolean forceUpdate) {
		OsmandApplication app = getApplication();
		float textScale = getTextScale();
		boolean carView = app.getOsmandMap().getMapView().isCarView();
		if (this.textScale != textScale || this.carView != carView || forceUpdate) {
			this.textScale = textScale;
			this.carView = carView;
			recreateBitmaps();
		}
	}

	private void recreateBitmaps() {
		selectedImage = getScaledBitmap(R.drawable.map_mapillary_location);
		headingImage = getScaledBitmap(R.drawable.map_mapillary_location_view_angle);
		point = getScaledBitmap(R.drawable.map_mapillary_photo_dot);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		updateBitmaps(true);
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		super.onPrepareBufferImage(canvas, tileBox, drawSettings);
		updateBitmaps(false);
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			int layerIndex = view.getLayerIndex(this);
			if (map == null) {
				if (mapillaryTilesProvider != null) {
					mapRenderer.resetMapLayerProvider(layerIndex);
					clearMapMarkersCollections();
					mapillaryTilesProvider = null;
				}
				return;
			}
			if (filterKey != getFilterKey()) {
				if (mapillaryTilesProvider != null) {
					mapillaryTilesProvider.clearCache();
					mapRenderer.resetMapLayerProvider(layerIndex);
					clearMapMarkersCollections();
					mapillaryTilesProvider = null;
				}
				filterKey = getFilterKey();
			}
			if (mapillaryTilesProvider == null) {
				mapillaryTilesProvider = new MapillaryTilesProvider(getApplication(), map, view.getDensity());
				mapRenderer.setMapLayerProvider(layerIndex, mapillaryTilesProvider.instantiateProxy(true));
				mapillaryTilesProvider.swigReleaseOwnership();
			} else {
				mapillaryTilesProvider.setVisibleBBox31(mapRenderer.getVisibleBBox31(), tileBox.getZoom());
			}
		} else {
			super.onPrepareBufferImage(canvas, tileBox, drawSettings);
		}
	}

	@Override
	public void setSelectedImageLocation(LatLon selectedImageLocation) {
		this.selectedImageLocation = selectedImageLocation;
		showMarkerIfNeeded();
	}

	@Override
	public void setSelectedImageCameraAngle(Float selectedImageCameraAngle) {
		this.selectedImageCameraAngle = selectedImageCameraAngle;
		showMarkerIfNeeded();
	}

	/**OpenGL*/
	private void showMarkerIfNeeded() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			initMarkersCollectionIfNeeded();
			if (mapMarkersCollection != null && mapMarkersCollection.getMarkers().size() > 0) {
				MapMarker marker = mapMarkersCollection.getMarkers().get(0);
				if (selectedImageLocation != null) {
					int x = MapUtils.get31TileNumberX(selectedImageLocation.getLongitude());
					int y = MapUtils.get31TileNumberY(selectedImageLocation.getLatitude());
					PointI pointI = new PointI(x, y);
					marker.setPosition(pointI);
					marker.setIsHidden(false);
				} else {
					marker.setIsHidden(true);
				}
				if (selectedImageCameraAngle != null) {
					marker.setOnMapSurfaceIconDirection(SwigUtilities.getOnSurfaceIconKey(2), selectedImageCameraAngle);
				}
			}
		}
	}

	/**OpenGL*/
	private void initMarkersCollectionIfNeeded() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || mapMarkersCollection != null) {
			return;
		}
		updateBitmaps(false);

		selectedImageBitmap = Bitmap.createBitmap(selectedImage.getWidth(), selectedImage.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas selectedImageCanvas = new Canvas(selectedImageBitmap);
		selectedImageCanvas.drawBitmap(selectedImage, 0, 0, paintPoint);

		headingImageBitmap = Bitmap.createBitmap(headingImage.getWidth(), headingImage.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas headingImageCanvas = new Canvas(headingImageBitmap);
		headingImageCanvas.drawBitmap(headingImage, 0, 0, paintPoint);

		mapMarkersCollection = new MapMarkersCollection();
		MapMarkerBuilder imageAndCourseMarkerBuilder = new MapMarkerBuilder();
		imageAndCourseMarkerBuilder
				.setIsHidden(true)
				.setIsAccuracyCircleSupported(false)
				.setBaseOrder(getPointsOrder())
				.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
				.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top)
				.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(1), NativeUtilities.createSkImageFromBitmap(selectedImageBitmap))
				.addOnMapSurfaceIcon(SwigUtilities.getOnSurfaceIconKey(2), NativeUtilities.createSkImageFromBitmap(headingImageBitmap))
				.buildAndAddToCollection(mapMarkersCollection);
		mapRenderer.addSymbolsProvider(mapMarkersCollection);
	}

	/**OpenGL*/
	@Override
	protected void clearMapMarkersCollections() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && mapMarkersCollection != null) {
			mapRenderer.removeSymbolsProvider(mapMarkersCollection);
			mapMarkersCollection = null;
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void drawTileMap(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		TileSourceTemplate mapillaryTemplate = TileSourceManager.getMapillaryVectorSource();
		ITileSource map = this.map;
		if (map == null) {
			return;
		}

		int currentZoom = tileBox.getZoom();
		if (currentZoom < Math.max(mapillaryTemplate.getMinimumZoomSupported(), map.getMinimumZoomSupported())
				|| currentZoom > Math.min(mapillaryTemplate.getMaximumZoomSupported(), map.getMaximumZoomSupported())) {
			return;
		}

		ResourceManager mgr = resourceManager;
		GeometryTilesCache tilesCache = mgr.getMapillaryVectorTilesCache();

		int tileZoom;
		if (currentZoom < MIN_POINTS_ZOOM) {
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

		boolean useInternet = (PluginsHelper.isActive(OsmandRasterMapsPlugin.class)
				|| PluginsHelper.isActive(MapillaryPlugin.class))
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
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
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
	protected void cleanupResources() {
		super.cleanupResources();
		clearMapProviders();
	}

	private void clearMapProviders() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (mapMarkersCollection != null) {
			mapRenderer.removeSymbolsProvider(mapMarkersCollection);
			mapMarkersCollection = null;
		}
		if (mapillaryTilesProvider != null) {
			int layerIndex = view.getLayerIndex(this);
			mapRenderer.resetMapLayerProvider(layerIndex);
			mapillaryTilesProvider = null;
		}
	}

	private void getImagesFromPoint(RotatedTileBox tb, PointF point, List<? super MapillaryImage> images) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && mapillaryTilesProvider != null) {
			getImagesFromPointOpenGL(tb, point, images);
		} else {
			getImagesFromPointCanvas(tb, point, images);
		}
	}

	/**OpenGL*/
	private void getImagesFromPointOpenGL(RotatedTileBox tb, PointF point, List<? super MapillaryImage> images) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}

		PointI center31 = NativeUtilities.get31FromElevatedPixel(mapRenderer, point.x, point.y);
		if (center31 == null) {
			return;
		}

		double centerLat = MapUtils.get31LatitudeY(center31.getY());
		double centerLon = MapUtils.get31LongitudeX(center31.getX());

		int radius = getScaledTouchRadius(getApplication(), tb.getDefaultRadiusPoi());
		QuadRect latLonRect = getLatLonRectFromPointOpenGl(mapRenderer, point, radius);
		if (latLonRect == null) {
			return;
		}

		QuadTree<MapillaryImage> quadTree = mapillaryTilesProvider.getQuadTreeByPoint(center31);
		if (quadTree == null) {
			return;
		}

		List<MapillaryImage> res = new ArrayList<>();
		quadTree.queryInBox(latLonRect, res);
		if (res.isEmpty()) {
			return;
		}

		double minDistance = Double.NaN;
		MapillaryImage closestImage = null;
		for (MapillaryImage image : res) {
			if (image == null) {
				continue;
			}

			double distance = MapUtils.getDistance(centerLat, centerLon, image.getLatitude(), image.getLongitude());
			if (closestImage == null || distance < minDistance) {
				closestImage = image;
				minDistance = distance;
			}
		}

		if (closestImage != null) {
			double lat = closestImage.getLatitude();
			double lon = closestImage.getLongitude();
			PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(mapRenderer, tb, lat, lon);
			if (Math.abs(pixel.x - point.x) <= radius && Math.abs(pixel.y - point.y) <= radius) {
				images.add(closestImage);
			}
		}
	}

	@Nullable
	private QuadRect getLatLonRectFromPointOpenGl(@NonNull MapRendererView mapRenderer,
	                                              @NonNull PointF pixel,
	                                              float radius) {
		List<PointI> touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, pixel, radius);
		if (touchPolygon31 == null) {
			return null;
		}

		int minX31 = Integer.MAX_VALUE;
		int minY31 = Integer.MAX_VALUE;
		int maxX31 = Integer.MIN_VALUE;
		int maxY31 = Integer.MIN_VALUE;
		for (PointI point31 : touchPolygon31) {
			int x31 = point31.getX();
			int y31 = point31.getY();

			minX31 = Math.min(minX31, x31);
			minY31 = Math.min(minY31, y31);
			maxX31 = Math.max(maxX31, x31);
			maxY31 = Math.max(maxY31, y31);
		}

		return new QuadRect(
				MapUtils.get31LongitudeX(minX31),
				MapUtils.get31LatitudeY(minY31),
				MapUtils.get31LongitudeX(maxX31),
				MapUtils.get31LatitudeY(maxY31)
		);
	}

	private void getImagesFromPointCanvas(RotatedTileBox tb, PointF point, List<? super MapillaryImage> images) {
		Map<QuadPointDouble, Map<?, ?>> points = this.visiblePoints;
		float ex = point.x;
		float ey = point.y;
		int radius = getScaledTouchRadius(getApplication(), tb.getDefaultRadiusPoi());
		float x, y;
		double minSqDist = Double.NaN;
		double sqDist;
		MapillaryImage img = null;

		for (Entry<QuadPointDouble, Map<?, ?>> entry : points.entrySet()) {
			double tileX = entry.getKey().x;
			double tileY = entry.getKey().y;
			Map<?, ?> userData = entry.getValue();

			x = tb.getPixXFromTile(tileX, tileY, MIN_IMAGE_LAYER_ZOOM);
			y = tb.getPixYFromTile(tileX, tileY, MIN_IMAGE_LAYER_ZOOM);
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

	private long getFilterKey() {
		int hasFilter = plugin.USE_MAPILLARY_FILTER.get() ? 1 : 0;
		long from = 0;
		long to = 0;
		if (hasFilter == 1) {
			from = plugin.MAPILLARY_FILTER_FROM_DATE.get();
			to = plugin.MAPILLARY_FILTER_TO_DATE.get();
		}
		int pano = plugin.MAPILLARY_FILTER_PANO.get() ? 1 : 0;
		return (hasFilter << 1) + from + to + pano;
	}
}
