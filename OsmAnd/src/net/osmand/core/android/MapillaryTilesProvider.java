package net.osmand.core.android;

import static net.osmand.plus.plugins.mapillary.MapillaryImage.CAPTURED_AT_KEY;
import static net.osmand.plus.plugins.mapillary.MapillaryImage.IS_PANORAMIC_KEY;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import net.osmand.core.jni.AlphaChannelPresence;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.IMapTiledDataProvider;
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
import net.osmand.data.QuadPointDouble;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.plugins.mapillary.MapillaryVectorLayer;
import net.osmand.plus.plugins.rastermaps.OsmandRasterMapsPlugin;
import net.osmand.plus.resources.GeometryTilesCache;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapillaryTilesProvider extends interface_ImageMapLayerProvider {

	private final ITileSource tileSource;
	private final ResourceManager rm;
	private final OsmandSettings settings;
	private final MapillaryPlugin plugin;
	private final Paint paintLine;
	private final Paint paintPoint;
	private final Bitmap bitmapPoint;

	private GeometryTilesCache tilesCache;
	public long mapRefreshTimestamp;
	private ConcurrentHashMap<QuadPointDouble, Map<?, ?>> visiblePoints;
	private RotatedTileBox renderedTileBox;
	public static final int MAX_SEQUENCE_LAYER_ZOOM = MapillaryVectorLayer.MAX_SEQUENCE_LAYER_ZOOM;
	public static final int MIN_IMAGE_LAYER_ZOOM = MapillaryVectorLayer.MIN_IMAGE_LAYER_ZOOM;
	public static final int MIN_POINTS_ZOOM = MapillaryVectorLayer.MIN_POINTS_ZOOM;
	public static final double EXTENT = MapillaryVectorLayer.EXTENT;

	public MapillaryTilesProvider(OsmandApplication app, @NonNull ITileSource tileSource, long mapRefreshTimestamp, RotatedTileBox tileBox, float density) {
		this.tileSource = tileSource;
		this.rm = app.getResourceManager();
		settings = app.getSettings();
		tilesCache = rm.getMapillaryVectorTilesCache();
		this.mapRefreshTimestamp = mapRefreshTimestamp;
		visiblePoints = new ConcurrentHashMap<>();
		renderedTileBox = tileBox;
		plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
		paintPoint = new Paint();

		Drawable drawablePoint = ResourcesCompat.getDrawable(app.getResources(), R.drawable.map_mapillary_photo_dot, null);
		if (drawablePoint != null) {
			bitmapPoint = AndroidUtils.createScaledBitmap(drawablePoint, 1.0f / density);
		} else {
			bitmapPoint = BitmapFactory.decodeResource(app.getResources(), R.drawable.map_mapillary_photo_dot);
		}
		paintLine = new Paint();
		paintLine.setStyle(Paint.Style.STROKE);
		paintLine.setAntiAlias(true);
		paintLine.setColor(ContextCompat.getColor(app.getApplicationContext(), R.color.mapillary_color));
		paintLine.setStrokeWidth(AndroidUtils.dpToPx(app.getApplicationContext(), 4.0f / density));
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
		float tileSize = tileSource.getTileSize();
		Bitmap resultTileBitmap = Bitmap.createBitmap((int)tileSize, (int)tileSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(resultTileBitmap);

		int currentZoom = request.getZoom().swigValue();
		int absZoomShift = currentZoom - getZoomForRequest(request);
		TileId shiftedTile = Utilities.getTileIdOverscaledByZoomShift(request.getTileId(), absZoomShift);
		int tileX = shiftedTile.getX();
		int tileY = shiftedTile.getY();
		int tileZoom;

		if (currentZoom < MIN_POINTS_ZOOM) {
			tileZoom = MAX_SEQUENCE_LAYER_ZOOM;
			tilesCache.useForMapillarySequenceLayer();
		} else {
			tileZoom = MIN_IMAGE_LAYER_ZOOM;
			tilesCache.useForMapillaryImageLayer();
		}

		GeometryTile tile = null;
		boolean useInternet = (OsmandPlugin.isActive(OsmandRasterMapsPlugin.class)
				|| OsmandPlugin.isActive(MapillaryPlugin.class))
				&& settings.isInternetConnectionAvailable() && tileSource.couldBeDownloadedFromInternet();
		String tileId = rm.calculateTileId(tileSource, tileX, tileY, tileZoom);
		boolean imgExist = tilesCache.isTileDownloaded(tileId, tileSource, tileX, tileY, tileZoom);
		if (imgExist || useInternet) {
			//TODO perhaps better to use tilesCache.getTileForMapAsync here
			tile = tilesCache.getTileForMapSync(tileId, tileSource, tileX, tileY,
					tileZoom, useInternet, mapRefreshTimestamp);
		}
		if (request.getQueryController() != null && request.getQueryController().isAborted()) {
			return SwigUtilities.nullSkImage();
		}
		if (tile != null) {
			List<Geometry> geometries = tile.getData();
			if (geometries != null) {
				drawLines(canvas, shiftedTile, request, geometries);
				if (currentZoom >= MIN_POINTS_ZOOM) {
					Map<QuadPointDouble, Map<?, ?>> drawnPoints = drawPoints(canvas, shiftedTile, request, geometries);
					visiblePoints.putAll(drawnPoints);
				}
				return NativeUtilities.createSkImageFromBitmap(resultTileBitmap);
			}
		}
		return SwigUtilities.nullSkImage();
	}

	private void drawLines(Canvas canvas, TileId tileId, IMapTiledDataProvider.Request request, List<Geometry> geometries) {
		for (Geometry geometry : geometries) {
			if (geometry.isEmpty() || filtered(geometry.getUserData())) {
				continue;
			}
			if (geometry instanceof MultiLineString) {
				drawMultiLineString(canvas, tileId, request, (MultiLineString) geometry);
			} else if (geometry instanceof LineString) {
				drawLine(canvas, tileId,request, (LineString) geometry);
			}
		}
	}

	private void drawMultiLineString(Canvas canvas, TileId tileId, IMapTiledDataProvider.Request request, MultiLineString multiLineString) {
		for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
			Geometry geometry = multiLineString.getGeometryN(i);
			if (geometry instanceof LineString && !geometry.isEmpty()) {
				drawLine(canvas, tileId, request, (LineString) geometry);
			}
		}
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
		return tileSource.getTileSize();
	}

	@Override
	public float getTileDensityFactor() {
		return 1.0f;
	}

	@Override
	public AlphaChannelPresence getAlphaChannelPresence() {
		return AlphaChannelPresence.Present;
	}

	public void setMapRefreshTimestamp(long timestamp) {
		mapRefreshTimestamp = timestamp;
	}

	public void setRenderedTileBox(RotatedTileBox tileBox) {
		if (renderedTileBox.getZoom() != tileBox.getZoom()) {
			visiblePoints.clear();
		}
		/*if (!renderedTileBox.getCenterLatLon().equals(tileBox.getCenterLatLon())) {
			visiblePoints.clear();
		}*/
		renderedTileBox = tileBox;
	}

	public Map<QuadPointDouble, Map<?, ?>> getVisiblePoints() {
		return visiblePoints;
	}

	private int getZoomForRequest(IMapTiledDataProvider.Request req) {
		if (req.getZoom().swigValue() < MIN_POINTS_ZOOM) {
			return MAX_SEQUENCE_LAYER_ZOOM;
		} else {
			return MIN_IMAGE_LAYER_ZOOM;
		}
	}

	private boolean filtered(Object data) {
		if (data == null) {
			return true;
		}

		boolean shouldFilter = plugin.USE_MAPILLARY_FILTER.get();
		long from = plugin.MAPILLARY_FILTER_FROM_DATE.get();
		long to = plugin.MAPILLARY_FILTER_TO_DATE.get();
		boolean pano = plugin.MAPILLARY_FILTER_PANO.get();

		HashMap<String, Object> userData = (HashMap<String, Object>) data;
		long capturedAt = ((Number) userData.get(CAPTURED_AT_KEY)).longValue();

		if (shouldFilter) {
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

	private void drawLine(Canvas canvas, TileId tileId, IMapTiledDataProvider.Request req, LineString line) {
		if (line.getCoordinateSequence().size() == 0) {
			return;
		}

		Coordinate[] coordinates = line.getCoordinateSequence().toCoordinateArray();
		int dzoom = req.getZoom().swigValue() - getZoomForRequest(req);
		int mult = (int) Math.pow(2.0, dzoom);
		double px, py;
		int tileSize31 = (1 << (ZoomLevel.MaxZoomLevel.swigValue() - req.getZoom().swigValue()));
		int zoomShift = ZoomLevel.MaxZoomLevel.swigValue() - req.getZoom().swigValue();
		AreaI tileBBox31 = Utilities.tileBoundingBox31(req.getTileId(), req.getZoom());
		int tileSize = tileSource.getTileSize();
		double px31Size = (double)tileSize31 / (double)tileSize;
		double bitmapHalfSize = (double)tileSize / 2.0d;
		AreaI tileBBox31Enlarged = Utilities.tileBoundingBox31(req.getTileId(), req.getZoom()).getEnlargedBy((int)(bitmapHalfSize * px31Size));

		float x1, y1, x2, y2 = 0;
		float lastTileX, lastTileY;
		Coordinate firstPnt = coordinates[0];
		px = firstPnt.x / EXTENT;
		py = firstPnt.y / EXTENT;
		lastTileX = (float)((tileId.getX() << zoomShift) + (tileSize31 * px)) * mult;
		lastTileY = (float) ((tileId.getY() << zoomShift) + (tileSize31 * py)) * mult;
		PointI topLeft = tileBBox31.getTopLeft();
		int tileBBox31Left = topLeft.getX();
		int tileBBox31Top = topLeft.getY();
		x1 = ((lastTileX - tileBBox31Left) / tileSize31) * tileSize;
		y1 = ((lastTileY - tileBBox31Top) / tileSize31) * tileSize;

		boolean recalculateLastXY = false;
		int size = coordinates.length;
		for (int i = 1; i < size; i++) {
			Coordinate point = coordinates[i];
			px = point.x / EXTENT;
			py = point.y / EXTENT;

			float tileX = (float)((tileId.getX() << zoomShift) + (tileSize31 * px)) * mult;
			float tileY = (float)((tileId.getY() << zoomShift) + (tileSize31 * py)) * mult;

			if (tileBBox31Enlarged.contains((int)tileX, (int)tileY)) {
				x2 = (float)((tileX - tileBBox31Left) / tileSize31) * tileSize;
				y2 = (float)((tileY - tileBBox31Top) / tileSize31) * tileSize;

				if (recalculateLastXY)
				{
					x1 = ((lastTileX - tileBBox31Left) / tileSize31) * tileSize;
					y1 = ((lastTileY - tileBBox31Top) / tileSize31) * tileSize;
					recalculateLastXY = false;
				}
				canvas.drawLine(x1, y1, x2, y2, paintLine);
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
	}

	private Map<QuadPointDouble, Map<?, ?>> drawPoints(Canvas canvas, TileId tileId, IMapTiledDataProvider.Request req, List<Geometry> geometries) {
		int dzoom = req.getZoom().swigValue() - getZoomForRequest(req);
		double mult = (int) Math.pow(2.0, dzoom);
		int tileSize31 = (1 << (ZoomLevel.MaxZoomLevel.swigValue() - req.getZoom().swigValue()));
		int zoomShift = ZoomLevel.MaxZoomLevel.swigValue() - req.getZoom().swigValue();
		AreaI tileBBox31 = Utilities.tileBoundingBox31(req.getTileId(), req.getZoom());
		int tileSize = tileSource.getTileSize();
		double px31Size = (double)tileSize31 / (double)tileSize;
		double bitmapHalfSize = bitmapPoint.getWidth() / 2.0d;
		AreaI tileBBox31Enlarged = Utilities.tileBoundingBox31(req.getTileId(), req.getZoom()).getEnlargedBy((int)(bitmapHalfSize * px31Size));
		PointI topLeft = tileBBox31.getTopLeft();
		int tileBBox31Left = topLeft.getX();
		int tileBBox31Top = topLeft.getY();

		Map<QuadPointDouble, Map<?, ?>> visiblePoints = new HashMap<>();
		for (Geometry g : geometries) {
			Map<?, ?> userData = g.getUserData() instanceof HashMap ? ((HashMap<?, ?>) g.getUserData()) : null;
			if (g instanceof Point && !g.isEmpty() && userData != null) {
				double px, py;
				Point p = (Point) g;
				px = p.getCoordinate().x / EXTENT;
				py = p.getCoordinate().y / EXTENT;

				double tileX = ((tileId.getX() << zoomShift) + (tileSize31 * px)) * mult;
				double tileY = ((tileId.getY() << zoomShift) + (tileSize31 * py)) * mult;

				if (tileBBox31Enlarged.contains((int)tileX, (int)tileY) && !filtered(userData)) {
					double x = ((tileX - tileBBox31Left) / tileSize31) * tileSize - bitmapHalfSize;
					double y = ((tileY - tileBBox31Top) / tileSize31) * tileSize - bitmapHalfSize;
					canvas.drawBitmap(bitmapPoint, (float) x, (float) y, paintPoint);
					visiblePoints.put(new QuadPointDouble(tileX + px, tileY + py), userData);
				}
			}
		}
		return visiblePoints;
	}
}
