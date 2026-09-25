package net.osmand.core.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import net.osmand.IndexConstants;
import net.osmand.core.jni.*;
import net.osmand.data.GeometryTile;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.SourceFingerprint;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.api.SQLiteAPI.SQLiteCursor;
import net.osmand.plus.api.SQLiteAPI.SQLiteStatement;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.panoramax.PanoramaxFilterState;
import net.osmand.plus.plugins.panoramax.PanoramaxImage;
import net.osmand.plus.plugins.panoramax.PanoramaxPlugin;
import net.osmand.plus.plugins.panoramax.PanoramaxVectorLayer;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.GeometryTilesCache;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.MapUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PanoramaxTilesProvider extends interface_ImageMapLayerProvider {

	private static final int MIN_ZOOM = TileSourceManager.getPanoramaxVectorSource().getMinimumZoomSupported();
	private static final int MAX_ZOOM = TileSourceManager.getPanoramaxVectorSource().getMaximumZoomSupported();

	private static final int TILE_LOAD_TIMEOUT = 30000;
	private static final int MAX_GEOMETRY_SIZE = 32000;

	// Serializes raster writes with invalidation so obsolete providers cannot persist tiles.
	private static final Object RASTER_CACHE_LOCK = new Object();
	private static long rasterCacheGeneration;

	private final ITileSource tileSource;
	private final ResourceManager rm;
	private final OsmandSettings settings;
	private final PanoramaxPlugin plugin;
	// The filter this provider was created for. PanoramaxVectorLayer replaces the provider when
	// the filter changes, so the cached tiles below always belong to this one state.
	private final PanoramaxFilterState filterState;
	private final Paint paintLine;
	private final Paint paintPoint;
	private final Bitmap bitmapPoint;
	private final float density;
	private final OsmandApplication app;
	private final File tilesDir;

	private final ConcurrentHashMap <AreaI, QuadTree<PanoramaxImage>> pointsMap;
	private final ConcurrentHashMap <AreaI, TileRequest> lazyLoadMap;
	private final GeometryTilesCache geometryTilesCache;
	private final PanoramaxBitmapTileCache panoramaxBitmapTileCache;
	private final long generation;
	private AreaI storedEnlargedBBox31;
	public static final int MAX_SEQUENCE_LAYER_ZOOM = PanoramaxVectorLayer.MAX_SEQUENCE_LAYER_ZOOM;
	public static final int MIN_IMAGE_LAYER_ZOOM = PanoramaxVectorLayer.MIN_IMAGE_LAYER_ZOOM;
	public static final int MIN_POINTS_ZOOM = PanoramaxVectorLayer.MIN_POINTS_ZOOM;
	public static final double EXTENT = PanoramaxVectorLayer.EXTENT;

	public PanoramaxTilesProvider(@NonNull OsmandApplication app, @NonNull ITileSource tileSource, float density,
			@NonNull PanoramaxFilterState filterState) {
		this.tileSource = tileSource;
		this.filterState = filterState;
		this.app = app;
		this.rm = app.getResourceManager();
		this.settings = app.getSettings();
		this.geometryTilesCache = rm.getMapillaryVectorTilesCache();
		this.tilesDir = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
		this.plugin = PluginsHelper.getPlugin(PanoramaxPlugin.class);
		this.panoramaxBitmapTileCache = new PanoramaxBitmapTileCache();
		this.paintPoint = new Paint();
		this.density = density;
		this.storedEnlargedBBox31 = null;
		this.pointsMap = new ConcurrentHashMap<>();
		this.lazyLoadMap = new ConcurrentHashMap<>();
		Drawable drawable = AppCompatResources.getDrawable(app, R.drawable.map_panoramax_photo_dot);
		// TODO: resize for Android auto
		if (drawable != null) {
			bitmapPoint = AndroidUtils.createScaledBitmap(drawable, 1.0f);
		} else {
			bitmapPoint = UiUtilities.decodeResource(app.getResources(), R.drawable.map_panoramax_photo_dot);
		}
		paintLine = new Paint();
		paintLine.setStyle(Paint.Style.STROKE);
		paintLine.setAntiAlias(true);
		paintLine.setColor(ContextCompat.getColor(app.getApplicationContext(), R.color.panoramax_color));
		paintLine.setStrokeWidth(AndroidUtils.dpToPxAuto(app.getApplicationContext(), 2.0f));
		paintLine.setStrokeCap(Paint.Cap.ROUND);
		invalidateRasterCacheIfFilterChanged();
		synchronized (RASTER_CACHE_LOCK) {
			generation = rasterCacheGeneration;
		}
	}

	/**
	 * Invalidates persistent raster tiles when they were rendered for another filter state.
	 */
	private void invalidateRasterCacheIfFilterChanged() {
		if (plugin == null) {
			return;
		}
		String cacheKey = filterState.getCacheKey();
		// An empty key means the existing cache predates this metadata.
		if (!cacheKey.equals(plugin.PANORAMAX_RASTER_CACHE_KEY.get())) {
			panoramaxBitmapTileCache.clearCache();
			plugin.PANORAMAX_RASTER_CACHE_KEY.set(cacheKey);
		}
	}

	@Override
	public MapStubStyle getDesiredStubsStyle() {
		return MapStubStyle.Unspecified;
	}

	@Override
	public ZoomLevel getMinZoom() {
		return ZoomLevel.swigToEnum(MIN_ZOOM);
	}

	@Override
	public ZoomLevel getMaxZoom() {
		return ZoomLevel.swigToEnum(MAX_ZOOM);
	}

	@Override
	public ZoomLevel getMinVisibleZoom() {
		int minVisibleZoom = Math.max(MIN_ZOOM, tileSource.getMinimumZoomSupported());
		return ZoomLevel.swigToEnum(minVisibleZoom);
	}

	@Override
	public ZoomLevel getMaxVisibleZoom() {
		int maxVisibleZoom = Math.min(MAX_ZOOM, tileSource.getMaximumZoomSupported());
		return ZoomLevel.swigToEnum(maxVisibleZoom);
	}

	@Override
	public boolean supportsNaturalObtainData() {
		return true;
	}

	@Override
	public long obtainImageData(IMapTiledDataProvider.Request request, SWIGTYPE_p_QByteArray byteArray) {
		IQueryController queryController = request.getQueryController();
		if (queryController != null && queryController.isAborted()) {
			return 0;
		}
		int requestZoom = request.getZoom().swigValue();
		ZoomLevel swigZoom = request.getZoom();
		TileId swigTileId = request.getTileId();

		int tileZoom = getZoomForRequest(requestZoom);
		int absZoomShift = requestZoom - tileZoom;
		TileId shiftedTile = Utilities.getTileIdOverscaledByZoomShift(swigTileId, absZoomShift);
		int tileX = shiftedTile.getX();
		int tileY = shiftedTile.getY();
		String tileId = rm.calculateTileId(tileSource, tileX, tileY, tileZoom);
		File sourceFile = new File(tilesDir, tileId);
		String filterKey = filterState.getCacheKey();

		boolean useInternet = (PluginsHelper.isActive(OsmandRasterMapsPlugin.class)
				|| PluginsHelper.isActive(PanoramaxPlugin.class))
				&& settings.isInternetConnectionAvailable() && tileSource.couldBeDownloadedFromInternet();

		long expiration = tileSource.getExpirationTimeMillis();
		SourceFingerprint source = SourceFingerprint.of(sourceFile);

		if (isSourceUsable(source.getLastModified(), expiration, System.currentTimeMillis(), useInternet)) {
			int x = swigTileId.getX();
			int y = swigTileId.getY();
			int z = requestZoom;
			Bitmap bitmapFromCache = getCachedRaster(x, y, z, source, filterKey);
			if (bitmapFromCache != null) {
				// Only picture tiles need lazy point loading.
				if (requestZoom >= MIN_POINTS_ZOOM) {
					AreaI tileBBox31 = Utilities.tileBoundingBox31(swigTileId, swigZoom);
					lazyLoadMap.put(tileBBox31, new TileRequest(x, y, z));
				}
				byte[] bytes = AndroidUtils.getByteArrayFromBitmap(bitmapFromCache);
				SwigUtilities.appendToQByteArray(byteArray, bytes);
				return (long) bitmapFromCache.getHeight() << 32 | bitmapFromCache.getWidth();
			}
		}

		int tileSize = getNormalizedTileSize();
		Bitmap resultTileBitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(resultTileBitmap);

		if (requestZoom < MIN_POINTS_ZOOM) {
			geometryTilesCache.useForMapillarySequenceLayer();
		} else {
			geometryTilesCache.useForMapillaryImageLayer();
		}

		GeometryTile tile = null;
		GeometryTile staleTile = null;
		boolean imgExist = geometryTilesCache.isTileDownloaded(tileId, tileSource, tileX, tileY, tileZoom);
		long requestTimestamp = System.currentTimeMillis();
		boolean awaitRefresh = mustAwaitRefresh(source.getLastModified(), expiration, requestTimestamp, useInternet);
		boolean requested = false;
		if (imgExist || useInternet) {
			do {
				if (queryController != null && queryController.isAborted()) {
					return 0;
				}
				boolean sourceMoved = !source.equals(SourceFingerprint.of(sourceFile));
				// Re-ask only once the file moved on, or every poll re-parses the same tile.
				if (!requested || !awaitRefresh || sourceMoved) {
					requested = true;
					tile = geometryTilesCache.getTileForMapSync(tileId, tileSource, tileX, tileY,
							tileZoom, useInternet, requestTimestamp);
					if (tile != null && !matchesSource(tile, sourceFile)) {
						// Drop stale geometry so the retry reads the replaced file.
						geometryTilesCache.remove(tileId);
						tile = null;
					} else if (tile != null && awaitRefresh && !sourceMoved) {
						// The expired tile triggered its own refresh; it is kept only as a fallback.
						staleTile = tile;
						tile = null;
					}
					if (tile != null) {
						break;
					}
				}
				try {
					Thread.sleep(50);
				} catch (InterruptedException ignored) {
				}
			} while (System.currentTimeMillis() - requestTimestamp < TILE_LOAD_TIMEOUT);
		}
		// A source still waiting to be replaced may be drawn so the layer does not go blank,
		// but its render may not be stored.
		boolean cacheable = tile != null;
		if (tile == null) {
			tile = staleTile;
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
				if (requestZoom < MIN_POINTS_ZOOM || geometries.size() < MAX_GEOMETRY_SIZE) {
					int strokeHalfWidth31 = (int) Math.ceil(paintLine.getStrokeWidth() * px31Size / 2.0);
					AreaI enlargedBBox31 = tileBBox31.getEnlargedBy(strokeHalfWidth31);
					isDrawLines = drawLines(canvas, shiftedTile, queryController, geometries, tileBBox31, enlargedBBox31, mult, zoomShift, tileSize, tileSize31);
				}
				if (requestZoom >= MIN_POINTS_ZOOM) {
					int pointHalfSize31 = (int) Math.ceil(bitmapPoint.getWidth() * px31Size / 2.0);
					AreaI enlargedBBox31 = tileBBox31.getEnlargedBy(pointHalfSize31);
					isDrawPoints = drawPoints(canvas, shiftedTile, queryController, geometries, tileBBox31, enlargedBBox31, mult, zoomShift, tileSize, tileSize31);
				}
				if ((isDrawLines || isDrawPoints) && cacheable && matchesSource(tile, sourceFile)) {
					panoramaxBitmapTileCache.saveTile(resultTileBitmap, swigTileId, requestZoom,
							tile.getSourceFingerprint(), filterKey);
				}
				byte[] bytes = AndroidUtils.getByteArrayFromBitmap(resultTileBitmap);
				SwigUtilities.appendToQByteArray(byteArray, bytes);
				return (long) resultTileBitmap.getHeight() << 32 | resultTileBitmap.getWidth();
			}
		}
		return 0;
	}

	private int getNormalizedTileSize() {
		return (int) (256 * density);
	}

	private boolean drawLines(Canvas canvas, TileId tileId, IQueryController queryController, List<Geometry> geometries,
	                       AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		boolean isDraw = false;
		for (Geometry geometry : geometries) {
			// Sequence tiles also contain picture points, so skip non-line geometries before filtering.
			if (!(geometry instanceof LineString) && !(geometry instanceof MultiLineString)) {
				continue;
			}
			if (geometry.isEmpty() || filterState.filtered(geometry.getUserData())) {
				continue;
			}
			if (geometry instanceof MultiLineString) {
				if (drawMultiLineString(canvas, tileId, queryController, (MultiLineString) geometry, paintLine,
						tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31)) {
					isDraw = true;
				}
			} else {
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
		return false;
	}

	@Override
	public boolean supportsNaturalObtainDataAsync() {
		return true;
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
		if (zoom < MIN_POINTS_ZOOM) {
			return;
		}
		if (storedEnlargedBBox31 != null && storedEnlargedBBox31.contains(visibleBBox31)) {
			return;
		}

		int delta = visibleBBox31.width();
		//enlarge visible bbox in 2 times
		AreaI enlargedBbox31 = visibleBBox31.getEnlargedBy(delta / 2);

		// Pruned in place. Tile workers keep inserting while this runs, so rebuilding the maps
		// here would drop whatever they added between the copy and the reassignment.
		pointsMap.keySet().removeIf(tileArea -> !enlargedBbox31.intersects(tileArea));
		lazyLoadMap.keySet().removeIf(tileArea -> !enlargedBbox31.intersects(tileArea));

		storedEnlargedBBox31 = enlargedBbox31;
	}

	/**
	 * Uses the expiration of the source handed to GeometryTilesCache, so raster validity and
	 * vector refresh cannot disagree.
	 */
	static boolean isSourceUsable(long modified, long expiration, long now, boolean useInternet) {
		return modified != 0 && !mustAwaitRefresh(modified, expiration, now, useInternet);
	}

	/**
	 * Online expired sources must be refreshed before being served.
	 * Offline sources remain usable.
	 */
	static boolean mustAwaitRefresh(long modified, long expiration, long now, boolean useInternet) {
		return modified != 0 && useInternet && !isTileFresh(modified, expiration, now);
	}

	private static boolean matchesSource(@NonNull GeometryTile tile, @NonNull File sourceFile) {
		return tile.getSourceFingerprint().equals(SourceFingerprint.of(sourceFile));
	}

	static boolean isTileFresh(long modified, long expiration, long now) {
		if (modified == 0) {
			return false;
		}
		// Like TilesCache, only -1 means the source never expires.
		return expiration == -1 || now - modified <= expiration;
	}

	private int getZoomForRequest(int zoom) {
		if (zoom < MIN_POINTS_ZOOM) {
			return MAX_SEQUENCE_LAYER_ZOOM;
		} else {
			return MIN_IMAGE_LAYER_ZOOM;
		}
	}

	private boolean drawLine(Canvas canvas, TileId tileId, IQueryController queryController, LineString line, Paint paintLine,
	                      AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		if (line.getCoordinateSequence().size() == 0
				|| (queryController != null && queryController.isAborted())) {
			return false;
		}

		PointI start31 = tileBBox31.getTopLeft();
		int start31X = start31.getX();
		int start31Y = start31.getY();

		PointI topLeft = tileBBox31Enlarged.getTopLeft();
		int left31 = topLeft.getX();
		int top31 = topLeft.getY();
		int right31 = left31 + tileBBox31Enlarged.width();
		int bottom31 = top31 + tileBBox31Enlarged.height();

		Coordinate[] coordinates = line.getCoordinateSequence().toCoordinateArray();

		boolean draw = false;
		float x1, y1, x2, y2;
		Coordinate firstPnt = coordinates[0];
		double px = firstPnt.x / EXTENT;
		double py = firstPnt.y / EXTENT;
		double previousTileX = ((tileId.getX() << zoomShift) + (tileSize31 * px)) * mult;
		double previousTileY = ((tileId.getY() << zoomShift) + (tileSize31 * py)) * mult;
		x1 = (float) (((previousTileX - start31X) / tileSize31) * tileSize);
		y1 = (float) (((previousTileY - start31Y) / tileSize31) * tileSize);

		boolean recalculateLastXY = false;
		int size = coordinates.length;
		for (int i = 1; i < size; i++) {
			if (queryController != null && i % 10 == 0 && queryController.isAborted()) {
				break;
			}

			Coordinate point = coordinates[i];
			px = point.x / EXTENT;
			py = point.y / EXTENT;

			double tileX = ((tileId.getX() << zoomShift) + (tileSize31 * px)) * mult;
			double tileY = ((tileId.getY() << zoomShift) + (tileSize31 * py)) * mult;

			boolean intersectsTile = Math.min(previousTileX, tileX) < right31
					&& Math.min(previousTileY, tileY) < bottom31
					&& Math.max(previousTileX, tileX) > left31
					&& Math.max(previousTileY, tileY) > top31;
			if (intersectsTile) {
				x2 = (float) (((tileX - start31X) / tileSize31) * tileSize);
				y2 = (float) (((tileY - start31Y) / tileSize31) * tileSize);
				if (recalculateLastXY)
				{
					x1 = (float) (((previousTileX - start31X) / tileSize31) * tileSize);
					y1 = (float) (((previousTileY - start31Y) / tileSize31) * tileSize);
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
			previousTileX = tileX;
			previousTileY = tileY;
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
		QuadTree<PanoramaxImage> tileQuadTree = new QuadTree<>(new QuadRect(leftLon, topLat, rightLon, bottomLat), 8, 0.55f);
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

				if (tileBBox31Enlarged.contains((int) tileX, (int) tileY) && !filterState.filtered(userData)) {
					double x = ((tileX - tileBBox31Left) / tileSize31) * tileSize - bitmapHalfSize;
					double y = ((tileY - tileBBox31Top) / tileSize31) * tileSize - bitmapHalfSize;
					canvas.drawBitmap(bitmapPoint, (float) x, (float) y, paintPoint);
					isDraw = true;
					PanoramaxImage img = new PanoramaxImage(lat, lon);
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

	public QuadTree<PanoramaxImage> getQuadTreeByPoint(PointI point31) {
		lazyLoadClickMap(point31);
		QuadTree<PanoramaxImage> quadTree = null;
		for (Map.Entry<AreaI, QuadTree<PanoramaxImage>> entry : pointsMap.entrySet()) {
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
		if (request.zoom < MIN_IMAGE_LAYER_ZOOM) {
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
				QuadTree<PanoramaxImage> tileQuadTree = new QuadTree<>(rect, 8, 0.55f);
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

						if (!filterState.filtered(userData)) {
							PanoramaxImage img = new PanoramaxImage(lat, lon);
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
		panoramaxBitmapTileCache.clearCache();
	}

	@NonNull
	private static File getRasterCacheFile(@NonNull OsmandApplication app) {
		String dbName = TileSourceManager.getPanoramaxCacheSource().getName() + IndexConstants.SQLITE_EXT;
		return new File(app.getAppPath(IndexConstants.TILES_INDEX_DIR), dbName);
	}

	@NonNull
	private static PanoramaxRasterCache openRasterCache(@NonNull OsmandApplication app) {
		PanoramaxRasterCache source = new PanoramaxRasterCache(app, getRasterCacheFile(app));
		source.createDataBase();
		return source;
	}

	/**
	 * Narrower than ResourceManager.clearCacheAndTiles(), which would also empty the
	 * in-memory bitmap cache shared with every other raster layer.
	 */
	public static void clearRasterCache(@NonNull OsmandApplication app) {
		if (!getRasterCacheFile(app).exists()) {
			return;
		}
		PanoramaxRasterCache source = openRasterCache(app);
		try {
			synchronized (RASTER_CACHE_LOCK) {
				rasterCacheGeneration++;
				source.deleteAllTiles();
			}
		} finally {
			// Only this temporary handle is closed; the provider keeps using its own.
			source.closeDB();
		}
	}

	@Nullable
	private Bitmap getCachedRaster(int x, int y, int zoom, @NonNull SourceFingerprint source,
	                               @NonNull String filterKey) {
		RasterTile cached = panoramaxBitmapTileCache.getTile(x, y, zoom);
		if (cached == null || !matchesStoredSource(cached.source, cached.filterKey, source, filterKey)) {
			return null;
		}
		return BitmapFactory.decodeByteArray(cached.png, 0, cached.png.length);
	}

	/** A row without provenance, written before this cache stored any, is a miss. */
	static boolean matchesStoredSource(@NonNull SourceFingerprint stored,
	                                   @Nullable String storedFilterKey,
	                                   @NonNull SourceFingerprint source, @NonNull String filterKey) {
		return stored.equals(source) && filterKey.equals(storedFilterKey);
	}

	static class RasterTile {
		final byte[] png;
		final SourceFingerprint source;
		final String filterKey;

		RasterTile(@NonNull byte[] png, @NonNull SourceFingerprint source, @Nullable String filterKey) {
			this.png = png;
			this.source = source;
			this.filterKey = filterKey;
		}
	}

	/**
	 * Keeps the provenance in columns beside the image so tiles.image stays an ordinary PNG and
	 * a generic consumer still decodes every row instead of deleting it as broken.
	 */
	private static class PanoramaxRasterCache extends SQLiteTileSource {

		private static final String SOURCE_TIME = "panoramax_source_time";
		private static final String SOURCE_LENGTH = "panoramax_source_length";
		private static final String FILTER_KEY = "panoramax_filter_key";

		private boolean provenanceSupported;

		PanoramaxRasterCache(@NonNull OsmandApplication app, @NonNull File file) {
			super(app, file, TileSourceManager.getKnownSourceTemplates());
		}

		@Override
		public void createDataBase() {
			super.createDataBase();
			SQLiteConnection db = getDatabase();
			if (db == null) {
				return;
			}
			// Concurrent opens can both see a missing column, and SQLite has no
			// ADD COLUMN IF NOT EXISTS, so the probe and the migration have to be one step.
			synchronized (RASTER_CACHE_LOCK) {
				if (!db.isReadOnly()) {
					addColumn(db, SOURCE_TIME, "long");
					addColumn(db, SOURCE_LENGTH, "long");
					addColumn(db, FILTER_KEY, "text");
				}
				// A database that could not get the columns keeps working, every row is a miss.
				provenanceSupported = hasColumn(db, SOURCE_TIME) && hasColumn(db, SOURCE_LENGTH)
						&& hasColumn(db, FILTER_KEY);
			}
		}

		@Nullable
		RasterTile getTile(int x, int y, int zoom) {
			SQLiteConnection db = getDatabase();
			if (db == null) {
				return null;
			}
			String columns = provenanceSupported
					? "image, " + SOURCE_TIME + ", " + SOURCE_LENGTH + ", " + FILTER_KEY
					: "image";
			// One row of one cursor: image and provenance cannot come from different writes.
			SQLiteCursor cursor = db.rawQuery("SELECT " + columns
					+ " FROM tiles WHERE x = ? AND y = ? AND z = ?", getTileDbParams(x, y, zoom));
			if (cursor == null) {
				return null;
			}
			try {
				byte[] png = cursor.moveToFirst() ? cursor.getBlob(0) : null;
				if (png == null) {
					return null;
				}
				if (!provenanceSupported || cursor.isNull(1) || cursor.isNull(2) || cursor.isNull(3)) {
					return new RasterTile(png, SourceFingerprint.EMPTY, null);
				}
				return new RasterTile(png, new SourceFingerprint(cursor.getLong(1), cursor.getLong(2)),
						cursor.getString(3));
			} finally {
				cursor.close();
			}
		}

		void putTile(int x, int y, int zoom, @NonNull byte[] png, @NonNull SourceFingerprint source,
		             @NonNull String filterKey) {
			SQLiteConnection db = getDatabase();
			if (db == null || db.isReadOnly() || !provenanceSupported) {
				return;
			}
			// One statement, so an image can never be stored with the provenance of another render.
			SQLiteStatement statement = db.compileStatement("INSERT OR REPLACE INTO tiles(x,y,z,s,image,"
					+ SOURCE_TIME + "," + SOURCE_LENGTH + "," + FILTER_KEY + ") VALUES(?,?,?,?,?,?,?,?)");
			if (statement == null) {
				return;
			}
			try {
				// The same key the reads use, so both sides stay on one zoom convention.
				String[] key = getTileDbParams(x, y, zoom);
				statement.bindString(1, key[0]);
				statement.bindString(2, key[1]);
				statement.bindString(3, key[2]);
				statement.bindLong(4, 0);
				statement.bindBlob(5, png);
				statement.bindLong(6, source.getLastModified());
				statement.bindLong(7, source.getLength());
				statement.bindString(8, filterKey);
				statement.execute();
			} finally {
				statement.close();
			}
		}

		void deleteAllTiles() {
			SQLiteConnection db = getDatabase();
			if (db == null || db.isReadOnly()) {
				return;
			}
			// Not deleteTiles(): its VACUUM and checkpoint would fight readers on other connections.
			db.execSQL("DELETE FROM tiles");
		}

		private static void addColumn(@NonNull SQLiteConnection db, @NonNull String name,
		                              @NonNull String type) {
			if (!hasColumn(db, name)) {
				db.execSQL("ALTER TABLE tiles ADD COLUMN " + name + " " + type);
			}
		}

		private static boolean hasColumn(@NonNull SQLiteConnection db, @NonNull String name) {
			SQLiteCursor cursor = db.rawQuery("SELECT * FROM tiles LIMIT 0", null);
			if (cursor == null) {
				return false;
			}
			try {
				return Arrays.asList(cursor.getColumnNames()).contains(name);
			} finally {
				cursor.close();
			}
		}
	}

	private class PanoramaxBitmapTileCache {
		private final PanoramaxRasterCache sqlTileSource;

		public PanoramaxBitmapTileCache() {
			sqlTileSource = openRasterCache(app);
		}

		public void clearCache() {
			synchronized (RASTER_CACHE_LOCK) {
				rasterCacheGeneration++;
				sqlTileSource.deleteAllTiles();
			}
		}

		@Nullable
		public RasterTile getTile(int x, int y, int zoom) {
			return sqlTileSource.getTile(x, y, zoom);
		}

		public void saveTile(Bitmap bmp, TileId tileId, int zoom, SourceFingerprint source,
		                     String filterKey) {
			if (bmp == null) {
				return;
			}
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bmp.compress(Bitmap.CompressFormat.PNG, 85, stream);
			byte[] png = stream.toByteArray();
			synchronized (RASTER_CACHE_LOCK) {
				if (generation != rasterCacheGeneration) {
					return;
				}
				sqlTileSource.putTile(tileId.getX(), tileId.getY(), zoom, png, source, filterKey);
			}
		}

	}
}
