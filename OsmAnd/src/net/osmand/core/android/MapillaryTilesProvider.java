package net.osmand.core.android;

import static net.osmand.plus.plugins.mapillary.MapillaryImage.CAPTURED_AT_KEY;
import static net.osmand.plus.plugins.mapillary.MapillaryImage.IS_PANORAMIC_KEY;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import net.osmand.IndexConstants;
import net.osmand.core.jni.AlphaChannelPresence;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.IMapTiledDataProvider;
import net.osmand.core.jni.IQueryController;
import net.osmand.core.jni.ImageMapLayerProvider;
import net.osmand.core.jni.MapStubStyle;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.SWIGTYPE_p_sk_spT_SkImage_const_t;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.TileId;
import net.osmand.core.jni.Utilities;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.core.jni.interface_ImageMapLayerProvider;
import net.osmand.data.GeometryTile;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryImage;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryVectorLayer;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.GeometryTilesCache;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.util.MapUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapillaryTilesProvider extends interface_ImageMapLayerProvider {

	private static final int TILE_LOAD_TIMEOUT = 30000;
	private static final int MAX_GEOMETRY_SIZE = 32000;

	private final ITileSource tileSource;
	private final ResourceManager rm;
	private final OsmandSettings settings;
	private final MapillaryPlugin plugin;
	private final Paint paintLine;
	private final Paint paintPoint;
	private final Bitmap bitmapPoint;
	private final float density;
	private final OsmandApplication app;

	private ConcurrentHashMap <AreaI, QuadTree<MapillaryImage>> pointsMap;
	private final ConcurrentHashMap <AreaI, TileRequest> lazyLoadMap;
	private final GeometryTilesCache geometryTilesCache;
	private final MapillaryBitmapTileCache mapillaryBitmapTileCache;
	private AreaI storedEnlargedBBox31;
	public static final int MAX_SEQUENCE_LAYER_ZOOM = MapillaryVectorLayer.MAX_SEQUENCE_LAYER_ZOOM;
	public static final int MIN_IMAGE_LAYER_ZOOM = MapillaryVectorLayer.MIN_IMAGE_LAYER_ZOOM;
	public static final int MIN_POINTS_ZOOM = MapillaryVectorLayer.MIN_POINTS_ZOOM;
	public static final double EXTENT = MapillaryVectorLayer.EXTENT;

	public MapillaryTilesProvider(OsmandApplication app, @NonNull ITileSource tileSource, float density) {
		this.tileSource = tileSource;
		this.app = app;
		this.rm = app.getResourceManager();
		this.settings = app.getSettings();
		this.geometryTilesCache = rm.getMapillaryVectorTilesCache();
		this.plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
		this.mapillaryBitmapTileCache = new MapillaryBitmapTileCache();
		this.paintPoint = new Paint();
		this.density = density;
		this.storedEnlargedBBox31 = null;
		this.pointsMap = new ConcurrentHashMap<>();
		this.lazyLoadMap = new ConcurrentHashMap<>();
		Drawable drawablePoint = ResourcesCompat.getDrawable(app.getResources(), R.drawable.map_mapillary_photo_dot, null);
		// TODO: resize for Android auto
		if (drawablePoint != null) {
			bitmapPoint = AndroidUtils.createScaledBitmap(drawablePoint, 1.0f);
		} else {
			bitmapPoint = BitmapFactory.decodeResource(app.getResources(), R.drawable.map_mapillary_photo_dot);
		}
		paintLine = new Paint();
		paintLine.setStyle(Paint.Style.STROKE);
		paintLine.setAntiAlias(true);
		paintLine.setColor(ContextCompat.getColor(app.getApplicationContext(), R.color.mapillary_color));
		paintLine.setStrokeWidth(AndroidUtils.dpToPxAuto(app.getApplicationContext(), 2.0f));
		paintLine.setStrokeCap(Paint.Cap.ROUND);
	}

	@Override
	public MapStubStyle getDesiredStubsStyle() {
		return MapStubStyle.Unspecified;
	}

	@Override
	public ZoomLevel getMinZoom() {
		return ZoomLevel.swigToEnum(tileSource.getMinimumZoomSupported());
	}

	@Override
	public ZoomLevel getMaxZoom() {
		return ZoomLevel.swigToEnum(tileSource.getMaximumZoomSupported());
	}

	@Override
	public boolean supportsNaturalObtainData() {
		return true;
	}

	@Override
	public SWIGTYPE_p_sk_spT_SkImage_const_t obtainImage(IMapTiledDataProvider.Request request) {
		IQueryController queryController = request.getQueryController();
		if (queryController != null && queryController.isAborted()) {
			return SwigUtilities.nullSkImage();
		}
		int requestZoom = request.getZoom().swigValue();
		ZoomLevel swigZoom = request.getZoom();
		TileId swigTileId = request.getTileId();
		if (mapillaryBitmapTileCache.isTileExist(swigTileId, requestZoom)) {
			int x = swigTileId.getX();
			int y = swigTileId.getY();
			int z = requestZoom;
			Bitmap bitmapFromCache = mapillaryBitmapTileCache.getTile(x, y, z);
			if (bitmapFromCache != null) {
				AreaI tileBBox31 = Utilities.tileBoundingBox31(swigTileId, swigZoom);
				lazyLoadMap.put(tileBBox31, new TileRequest(x, y, z));
				return NativeUtilities.createSkImageFromBitmap(bitmapFromCache);
			}
		}

		int tileSize = getNormalizedTileSize();
		Bitmap resultTileBitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(resultTileBitmap);


		int absZoomShift = requestZoom - getZoomForRequest(requestZoom);
		TileId shiftedTile = Utilities.getTileIdOverscaledByZoomShift(swigTileId, absZoomShift);
		int tileX = shiftedTile.getX();
		int tileY = shiftedTile.getY();
		int tileZoom;

		if (requestZoom < MIN_POINTS_ZOOM) {
			tileZoom = MAX_SEQUENCE_LAYER_ZOOM;
			geometryTilesCache.useForMapillarySequenceLayer();
		} else {
			tileZoom = MIN_IMAGE_LAYER_ZOOM;
			geometryTilesCache.useForMapillaryImageLayer();
		}

		GeometryTile tile = null;
		boolean useInternet = (OsmandPlugin.isActive(OsmandRasterMapsPlugin.class)
				|| OsmandPlugin.isActive(MapillaryPlugin.class))
				&& settings.isInternetConnectionAvailable() && tileSource.couldBeDownloadedFromInternet();
		String tileId = rm.calculateTileId(tileSource, tileX, tileY, tileZoom);
		boolean imgExist = geometryTilesCache.isTileDownloaded(tileId, tileSource, tileX, tileY, tileZoom);
		long requestTimestamp = System.currentTimeMillis();
		if (imgExist || useInternet) {
			do {
				if (queryController != null && queryController.isAborted()) {
					return SwigUtilities.nullSkImage();
				}
				tile = geometryTilesCache.getTileForMapSync(tileId, tileSource, tileX, tileY,
						tileZoom, useInternet, requestTimestamp);
				if (tile != null) {
					break;
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException ignored) {
				}
			} while (System.currentTimeMillis() - requestTimestamp < TILE_LOAD_TIMEOUT);
		}
		if (tile != null) {
			List<Geometry> geometries = tile.getData();
			if (geometries != null) {
				boolean isDrawLines = false;
				boolean isDrawPoints = false;
				int dzoom = requestZoom - getZoomForRequest(requestZoom);
				int mult = (int) Math.pow(2.0, dzoom);
				int tileSize31 = (1 << (ZoomLevel.MaxZoomLevel.swigValue() - requestZoom));
				int zoomShift = ZoomLevel.MaxZoomLevel.swigValue() - requestZoom;
				AreaI tileBBox31 = Utilities.tileBoundingBox31(swigTileId, swigZoom);
				double px31Size = (double)tileSize31 / (double)tileSize;
				double bitmapHalfSize = (double)tileSize / 10d;
				AreaI tileBBox31Enlarged = Utilities.tileBoundingBox31(swigTileId,
						swigZoom).getEnlargedBy((int)(bitmapHalfSize * px31Size));
				if (requestZoom < MIN_POINTS_ZOOM || geometries.size() < MAX_GEOMETRY_SIZE) {
					isDrawLines = drawLines(canvas, shiftedTile, queryController, geometries, tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31);
				}
				if (requestZoom >= MIN_POINTS_ZOOM) {
					bitmapHalfSize = bitmapPoint.getWidth() / 2.0d;
					tileBBox31Enlarged = Utilities.tileBoundingBox31(swigTileId, swigZoom).getEnlargedBy((int)(bitmapHalfSize * px31Size));
					isDrawPoints = drawPoints(canvas, shiftedTile, queryController, geometries, tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31);
				}
				if (isDrawLines || isDrawPoints) {
					mapillaryBitmapTileCache.saveTile(resultTileBitmap, swigTileId, requestZoom);
				}
				return NativeUtilities.createSkImageFromBitmap(resultTileBitmap);
			}
		}
		return SwigUtilities.nullSkImage();
	}

	private int getNormalizedTileSize() {
		return (int) (256 * density);
	}

	private boolean drawLines(Canvas canvas, TileId tileId, IQueryController queryController, List<Geometry> geometries,
	                       AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		boolean isDraw = false;
		for (Geometry geometry : geometries) {
			if (geometry.isEmpty() || filtered(geometry.getUserData())) {
				continue;
			}
			if (geometry instanceof MultiLineString) {
				if (drawMultiLineString(canvas, tileId, queryController, (MultiLineString) geometry, paintLine,
						tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31)) {
					isDraw = true;
				}
			} else if (geometry instanceof LineString) {
				if (drawLine(canvas, tileId, queryController, (LineString) geometry, paintLine,
						tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31)) {
					isDraw = true;
				}
			}
		}
		return isDraw;
	}

	private boolean drawMultiLineString(Canvas canvas, TileId tileId, IQueryController queryController,
	                                 MultiLineString multiLineString, Paint paintLine,
	                                 AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		boolean isDraw = false;
		for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
			Geometry geometry = multiLineString.getGeometryN(i);
			if (geometry instanceof LineString && !geometry.isEmpty()) {
				if (drawLine(canvas, tileId, queryController, (LineString) geometry, paintLine,
						tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31)) {
					isDraw = true;
				}
			}
		}
		return isDraw;
	}

	@Override
	public boolean supportsObtainImage() {
		return true;
	}

	@Override
	public boolean supportsNaturalObtainDataAsync() {
		return true;
	}

	@Override
	public void obtainImageAsync(IMapTiledDataProvider.Request request, ImageMapLayerProvider.AsyncImageData asyncImage) {
	}

	@Override
	public long getTileSize() {
		return getNormalizedTileSize();
	}

	@Override
	public float getTileDensityFactor() {
		return 1.0f;
	}

	@Override
	public AlphaChannelPresence getAlphaChannelPresence() {
		return AlphaChannelPresence.Present;
	}

	public void setVisibleBBox31(AreaI visibleBBox31, int zoom) {
		if (zoom < MIN_POINTS_ZOOM || pointsMap == null || pointsMap.size() == 0) {
			return;
		}
		if (storedEnlargedBBox31 != null && storedEnlargedBBox31.contains(visibleBBox31)) {
			return;
		}

		int delta = visibleBBox31.width();
		//enlarge visible bbox in 2 times
		AreaI enlargedBbox31 = visibleBBox31.getEnlargedBy(delta / 2);

		Map<AreaI, QuadTree<MapillaryImage>> resultMap = new HashMap<>();
		for (Map.Entry<AreaI, QuadTree<MapillaryImage>> entry : pointsMap.entrySet()) {
			AreaI tileArea = entry.getKey();
			if (enlargedBbox31.intersects(tileArea)) {
				resultMap.put(tileArea, entry.getValue());
			}
		}
		pointsMap.clear();
		if (resultMap.size() > 0) {
			pointsMap = new ConcurrentHashMap<>(resultMap);
		}
		storedEnlargedBBox31 = enlargedBbox31;
	}

	private int getZoomForRequest(int zoom) {
		if (zoom < MIN_POINTS_ZOOM) {
			return MAX_SEQUENCE_LAYER_ZOOM;
		} else {
			return MIN_IMAGE_LAYER_ZOOM;
		}
	}

	private boolean filtered(Object data) {
		if (data == null) {
			return true;
		}
		HashMap<String, Object> userData = (HashMap<String, Object>) data;
		boolean shouldFilter = plugin.USE_MAPILLARY_FILTER.get();
		if (shouldFilter) {
			long from = plugin.MAPILLARY_FILTER_FROM_DATE.get();
			long to = plugin.MAPILLARY_FILTER_TO_DATE.get();
			long capturedAt = ((Number) userData.get(CAPTURED_AT_KEY)).longValue();
			if (from != 0 && to != 0) {
				if (capturedAt < from || capturedAt > to) {
					return true;
				}
			} else if ((from != 0 && capturedAt < from) || (to != 0 && capturedAt > to)) {
				return true;
			}
		}
		// Always filter by image type
		boolean pano = plugin.MAPILLARY_FILTER_PANO.get();
		if (pano) {
			boolean isPanoramicImage = (boolean) userData.get(IS_PANORAMIC_KEY);
			return !isPanoramicImage;
		}
		return false;
	}

	private boolean drawLine(Canvas canvas, TileId tileId, IQueryController queryController, LineString line, Paint paintLine,
	                      AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		if (line.getCoordinateSequence().size() == 0
				|| (queryController != null && queryController.isAborted())) {
			return false;
		}
		Coordinate[] coordinates = line.getCoordinateSequence().toCoordinateArray();

		boolean draw = false;
		float x1, y1, x2, y2;
		float lastTileX, lastTileY;
		Coordinate firstPnt = coordinates[0];
		double px = firstPnt.x / EXTENT;
		double py = firstPnt.y / EXTENT;
		lastTileX = (float)((tileId.getX() << zoomShift) + (tileSize31 * px)) * mult;
		lastTileY = (float) ((tileId.getY() << zoomShift) + (tileSize31 * py)) * mult;
		PointI topLeft = tileBBox31.getTopLeft();
		int tileBBox31Left = topLeft.getX();
		int tileBBox31Top = topLeft.getY();
		x1 = (float) (((lastTileX - tileBBox31Left) / tileSize31) * tileSize);
		y1 = (float) (((lastTileY - tileBBox31Top) / tileSize31) * tileSize);

		boolean recalculateLastXY = false;
		int size = coordinates.length;
		Path path = new Path();
		path.moveTo(x1, y1);
		for (int i = 1; i < size; i++) {
			if (queryController != null && i % 10 == 0 && queryController.isAborted()) {
				break;
			}

			Coordinate point = coordinates[i];
			px = point.x / EXTENT;
			py = point.y / EXTENT;

			float tileX = (float)((tileId.getX() << zoomShift) + (tileSize31 * px)) * mult;
			float tileY = (float)((tileId.getY() << zoomShift) + (tileSize31 * py)) * mult;

			if (tileBBox31Enlarged.contains((int)tileX, (int)tileY)) {
				x2 = (float) (((tileX - tileBBox31Left) / tileSize31) * tileSize);
				y2 = (float) (((tileY - tileBBox31Top) / tileSize31) * tileSize);
				if (recalculateLastXY)
				{
					x1 = (float) (((lastTileX - tileBBox31Left) / tileSize31) * tileSize);
					y1 = (float) (((lastTileY - tileBBox31Top) / tileSize31) * tileSize);
					recalculateLastXY = false;
				}
				canvas.drawLine(x1, y1, x2, y2, paintLine);
				draw = true;
				x1 = x2;
				y1 = y2;
			}
			else
			{
				recalculateLastXY = true;
			}
			lastTileX = tileX;
			lastTileY = tileY;
		}
		return draw;
	}

	private boolean drawPoints(Canvas canvas, TileId tileId, IQueryController queryController, List<Geometry> geometries,
	                        AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		if (queryController != null && queryController.isAborted()) {
			return false;
		}
		boolean isDraw = false;
		double bitmapHalfSize = bitmapPoint.getWidth() / 2.0d;
		PointI topLeft = tileBBox31.getTopLeft();
		PointI bottomRight = tileBBox31.getBottomRight();
		int tileBBox31Left = topLeft.getX();
		int tileBBox31Top = topLeft.getY();

		double leftLon = MapUtils.get31LongitudeX(topLeft.getX());
		double topLat = MapUtils.get31LatitudeY(topLeft.getY());
		double rightLon = MapUtils.get31LongitudeX(bottomRight.getX());
		double bottomLat = MapUtils.get31LatitudeY(bottomRight.getY());
		QuadTree<MapillaryImage> tileQuadTree = new QuadTree<>(new QuadRect(leftLon, topLat, rightLon, bottomLat), 8, 0.55f);
		boolean arePointsInTile = false;

		for (int i = 0; i < geometries.size(); i++) {
			Geometry g = geometries.get(i);
			if (queryController != null && i % 100 == 0 && queryController.isAborted()) {
				break;
			}
			Map<?, ?> userData = g.getUserData() instanceof HashMap ? ((HashMap<?, ?>) g.getUserData()) : null;
			if (g instanceof Point && !g.isEmpty() && userData != null) {
				double px, py;
				Point p = (Point) g;
				px = p.getCoordinate().x / EXTENT;
				py = p.getCoordinate().y / EXTENT;

				double tileX = ((tileId.getX() << zoomShift) + (tileSize31 * px)) * mult;
				double tileY = ((tileId.getY() << zoomShift) + (tileSize31 * py)) * mult;
				double lat = MapUtils.get31LatitudeY((int) tileY);
				double lon = MapUtils.get31LongitudeX((int) tileX);

				if (tileBBox31Enlarged.contains((int) tileX, (int) tileY) && !filtered(userData)) {
					double x = ((tileX - tileBBox31Left) / tileSize31) * tileSize - bitmapHalfSize;
					double y = ((tileY - tileBBox31Top) / tileSize31) * tileSize - bitmapHalfSize;
					canvas.drawBitmap(bitmapPoint, (float) x, (float) y, paintPoint);
					isDraw = true;
					MapillaryImage img = new MapillaryImage(lat, lon);
					if (img.setData(userData)) {
						tileQuadTree.insert(img, (float) lon, (float) lat);
						arePointsInTile = true;
					}
				}
			}
		}
		if (arePointsInTile) {
			pointsMap.put(tileBBox31, tileQuadTree);
		}
		return isDraw;
	}

	public QuadTree<MapillaryImage> getQuadTreeByPoint(PointI point31) {
		lazyLoadClickMap(point31);
		QuadTree<MapillaryImage> quadTree = null;
		for (Map.Entry<AreaI, QuadTree<MapillaryImage>> entry : pointsMap.entrySet()) {
			AreaI tileBbox31 = entry.getKey();
			if (tileBbox31.contains(point31)) {
				quadTree = entry.getValue();
				break;
			}
		}
		return quadTree;
	}

	private void lazyLoadClickMap(PointI point31) {
		AreaI tileBBox31 = null;
		TileRequest request = null;
		for (Map.Entry<AreaI, TileRequest> entry : lazyLoadMap.entrySet()) {
			AreaI tileArea = entry.getKey();
			if (tileArea.contains(point31)) {
				tileBBox31 = entry.getKey();
				request = entry.getValue();
				break;
			}
		}
		if (tileBBox31 == null || request == null) {
			return;
		}

		int currentZoom = request.zoom;
		int absZoomShift = currentZoom - MIN_IMAGE_LAYER_ZOOM;
		TileId tileIdRequest = new TileId();
		tileIdRequest.setX(request.x);
		tileIdRequest.setY(request.y);
		TileId shiftedTile = Utilities.getTileIdOverscaledByZoomShift(tileIdRequest, absZoomShift);
		int tileX = shiftedTile.getX();
		int tileY = shiftedTile.getY();
		int tileZoom = MIN_IMAGE_LAYER_ZOOM;

		GeometryTile tile = null;
		String tileId = rm.calculateTileId(tileSource, tileX, tileY, tileZoom);
		boolean imgExist = geometryTilesCache.isTileDownloaded(tileId, tileSource, tileX, tileY, tileZoom);
		long requestTimestamp = System.currentTimeMillis();
		if (imgExist) {
			tile = geometryTilesCache.getTileForMapSync(tileId, tileSource, tileX, tileY, tileZoom, false, requestTimestamp);
		}
		if (tile != null) {
			List<Geometry> geometries = tile.getData();
			if (geometries != null) {
				int dzoom = request.zoom - MIN_IMAGE_LAYER_ZOOM;
				int mult = (int) Math.pow(2.0, dzoom);
				int tileSize31 = (1 << (ZoomLevel.MaxZoomLevel.swigValue() - request.zoom));
				int zoomShift = ZoomLevel.MaxZoomLevel.swigValue() - request.zoom;

				PointI topLeft = tileBBox31.getTopLeft();
				PointI bottomRight = tileBBox31.getBottomRight();
				double leftLon = MapUtils.get31LongitudeX(topLeft.getX());
				double topLat = MapUtils.get31LatitudeY(topLeft.getY());
				double rightLon = MapUtils.get31LongitudeX(bottomRight.getX());
				double bottomLat = MapUtils.get31LatitudeY(bottomRight.getY());
				QuadRect rect = new QuadRect(leftLon, topLat, rightLon, bottomLat);
				QuadTree<MapillaryImage> tileQuadTree = new QuadTree<>(rect, 8, 0.55f);
				boolean arePointsInTile = false;

				for (int i = 0; i < geometries.size(); i++) {
					Geometry g = geometries.get(i);
					Map<?, ?> userData = g.getUserData() instanceof HashMap ? ((HashMap<?, ?>) g.getUserData()) : null;
					if (g instanceof Point && !g.isEmpty() && userData != null) {
						double px, py;
						Point p = (Point) g;
						px = p.getCoordinate().x / EXTENT;
						py = p.getCoordinate().y / EXTENT;

						double x31= ((shiftedTile.getX() << zoomShift) + (tileSize31 * px)) * mult;
						double y31 = ((shiftedTile.getY() << zoomShift) + (tileSize31 * py)) * mult;
						double lat = MapUtils.get31LatitudeY((int) y31);
						double lon = MapUtils.get31LongitudeX((int) x31);

						if (!filtered(userData)) {
							MapillaryImage img = new MapillaryImage(lat, lon);
							if (img.setData(userData)) {
								tileQuadTree.insert(img, (float) lon, (float) lat);
								arePointsInTile = true;
							}
						}
					}
				}
				if (arePointsInTile) {
					pointsMap.put(tileBBox31, tileQuadTree);
					lazyLoadMap.remove(tileBBox31);
				}
			}
		}
	}


	private static class TileRequest {
		public int x;
		public int y;
		public int zoom;
		public TileRequest(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.zoom = z;
		}
	}

	public void clearCache() {
		lazyLoadMap.clear();
		pointsMap.clear();
		mapillaryBitmapTileCache.clearCache();
	}

	private class MapillaryBitmapTileCache {
		private final SQLiteTileSource sqlTileSource;
		boolean storedShouldFilter;
		boolean storedPano;

		public MapillaryBitmapTileCache() {
			String dbName = TileSourceManager.getMapillaryCacheSource().getName();
			dbName += IndexConstants.SQLITE_EXT;
			File tilesDir = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
			File dbFile = new File(tilesDir, dbName);
			sqlTileSource = new SQLiteTileSource(app,  dbFile, TileSourceManager.getKnownSourceTemplates());
			sqlTileSource.createDataBase();
			storedShouldFilter = plugin.USE_MAPILLARY_FILTER.get();
			storedPano = plugin.MAPILLARY_FILTER_PANO.get();
		}

		public void clearCache() {
			rm.clearCacheAndTiles(sqlTileSource);
		}

		public boolean isTileExist(TileId tileId, int zoom) {
			return sqlTileSource.exists(tileId.getX(), tileId.getY(), zoom);
		}

		public Bitmap getTile(int x, int y, int zoom) {
			return sqlTileSource.getImage(x, y, zoom, null);
		}

		public void saveTile(Bitmap bmp, TileId tileId, int zoom) {
			if (bmp == null) {
				return;
			}
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bmp.compress(Bitmap.CompressFormat.PNG, 85, stream);
			byte[] byteArray = stream.toByteArray();

			try {
				sqlTileSource.insertImage(tileId.getX(), tileId.getY(), zoom, byteArray);
			} catch (IOException e) {
				Log.w("Tile x=" + tileId.getX() + " y=" + tileId.getY() + " z=" + zoom + " couldn't be read", e);
			}
		}

		public void deleteTile(IMapTiledDataProvider.Request request) {
			TileId tileId = request.getTileId();
			int zoom = request.getZoom().swigValue();
			sqlTileSource.deleteImage(tileId.getX(), tileId.getY(), zoom);
		}

	}
}
