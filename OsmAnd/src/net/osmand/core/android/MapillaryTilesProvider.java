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

	private static final int TILE_LOAD_TIMEOUT = 30000;

	private final ITileSource tileSource;
	private final ResourceManager rm;
	private final OsmandSettings settings;
	private final MapillaryPlugin plugin;
	private final Paint paintLine;
	private final Paint paintPoint;
	private final Bitmap bitmapPoint;
	private final float density;

	private final GeometryTilesCache tilesCache;
	private final ConcurrentHashMap<QuadPointDouble, Map<?, ?>> visiblePoints = new ConcurrentHashMap<>();
	private RotatedTileBox renderedTileBox;
	public static final int MAX_SEQUENCE_LAYER_ZOOM = MapillaryVectorLayer.MAX_SEQUENCE_LAYER_ZOOM;
	public static final int MIN_IMAGE_LAYER_ZOOM = MapillaryVectorLayer.MIN_IMAGE_LAYER_ZOOM;
	public static final int MIN_POINTS_ZOOM = MapillaryVectorLayer.MIN_POINTS_ZOOM;
	public static final double EXTENT = MapillaryVectorLayer.EXTENT;

	public MapillaryTilesProvider(OsmandApplication app, @NonNull ITileSource tileSource,
	                              RotatedTileBox tileBox, float density) {
		this.tileSource = tileSource;
		this.rm = app.getResourceManager();
		this.settings = app.getSettings();
		this.tilesCache = rm.getMapillaryVectorTilesCache();
		this.renderedTileBox = tileBox;
		this.plugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
		this.paintPoint = new Paint();
		this.density = density;

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
		int tileSize = getNormalizedTileSize();
		Bitmap resultTileBitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(resultTileBitmap);

		int currentZoom = request.getZoom().swigValue();
		int absZoomShift = currentZoom - getZoomForRequest(request);
		TileId shiftedTile = Utilities.getTileIdOverscaledByZoomShift(request.getTileId(), absZoomShift);
		int tileX = shiftedTile.getX();
		int tileY = shiftedTile.getY();
		int tileZoom;

		if (currentZoom < MIN_POINTS_ZOOM) {
			tileZoom = MAX_SEQUENCE_LAYER_ZOOM;
			tilesCache.useForMapillarySequenceLayer(true);
		} else {
			tileZoom = MIN_IMAGE_LAYER_ZOOM;
			tilesCache.useForMapillaryImageLayer(true);
		}

		GeometryTile tile = null;
		boolean useInternet = (OsmandPlugin.isActive(OsmandRasterMapsPlugin.class)
				|| OsmandPlugin.isActive(MapillaryPlugin.class))
				&& settings.isInternetConnectionAvailable() && tileSource.couldBeDownloadedFromInternet();
		String tileId = rm.calculateTileId(tileSource, tileX, tileY, tileZoom);
		boolean imgExist = tilesCache.isTileDownloaded(tileId, tileSource, tileX, tileY, tileZoom);
		long requestTimestamp = System.currentTimeMillis();
		if (imgExist || useInternet) {
			//long requestTimestamp = System.currentTimeMillis();
			do {
				if (queryController != null && queryController.isAborted()) {
					return SwigUtilities.nullSkImage();
				}
				tile = tilesCache.getTileForMapSync(tileId, tileSource, tileX, tileY,
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
			Log.d("2222", "get tile " + request.getTileId().getX() + ":" + request.getTileId().getY() + ":" + request.getZoom() + " in " + (System.currentTimeMillis() - requestTimestamp));
			long drawTimestamp = System.currentTimeMillis();
			List<Geometry> geometries = tile.getData();
			if (geometries != null) {

				ZoomLevel zoom = request.getZoom();
				int dzoom = zoom.swigValue() - getZoomForRequest(request);
				int mult = (int) Math.pow(2.0, dzoom);
				int tileSize31 = (1 << (ZoomLevel.MaxZoomLevel.swigValue() - zoom.swigValue()));
				int zoomShift = ZoomLevel.MaxZoomLevel.swigValue() - zoom.swigValue();
				AreaI tileBBox31 = Utilities.tileBoundingBox31(request.getTileId(), zoom);
				double px31Size = (double)tileSize31 / (double)tileSize;
				double bitmapHalfSize = (double)tileSize / 10d;
				AreaI tileBBox31Enlarged = Utilities.tileBoundingBox31(request.getTileId(),
						zoom).getEnlargedBy((int)(bitmapHalfSize * px31Size));

				if (currentZoom < MIN_POINTS_ZOOM) {
					drawLines(canvas, shiftedTile, queryController, geometries, tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31);
				}
				int points = 0;
				if (currentZoom >= MIN_POINTS_ZOOM) {
					bitmapHalfSize = bitmapPoint.getWidth() / 2.0d;
					tileBBox31Enlarged = Utilities.tileBoundingBox31(request.getTileId(), zoom).getEnlargedBy((int)(bitmapHalfSize * px31Size));
					Map<QuadPointDouble, Map<?, ?>> drawnPoints = drawPoints(canvas, shiftedTile, queryController, geometries,
							tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31);
					points = drawnPoints.size();
					//visiblePoints.putAll(drawnPoints);
				}
				Log.d("2222", "draw tile " + request.getTileId().getX() + ":" + request.getTileId().getY() + ":" + zoom
						+ " points=" + points + " in " + (System.currentTimeMillis() - drawTimestamp) + " all = " + (System.currentTimeMillis() - requestTimestamp));
				return NativeUtilities.createSkImageFromBitmap(resultTileBitmap);
			}
		}
		return SwigUtilities.nullSkImage();
	}

	private int getNormalizedTileSize() {
		return (int) (256 * density);
	}

	private int test = -1;

	private void drawLines(Canvas canvas, TileId tileId, IQueryController queryController, List<Geometry> geometries,
	                       AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		test = test + 1;
		if (test > 4) {
			test = -1;
		}
		Paint paintLine = new Paint();
		paintLine.setStyle(Paint.Style.STROKE);
		paintLine.setAntiAlias(true);
		if (test <= 0) {
			paintLine.setColor(Color.RED);
		} else if (test == 1) {
			paintLine.setColor(Color.BLUE);
		} else if (test == 2) {
			paintLine.setColor(Color.GREEN);
		} else if (test == 3) {
			paintLine.setColor(Color.YELLOW);
		} else if (test >= 4) {
			paintLine.setColor(Color.MAGENTA);
		}
		paintLine.setStrokeWidth(AndroidUtils.dpToPxAuto(rm.getContext(), 2.0f));
		paintLine.setStrokeCap(Paint.Cap.ROUND);

		for (Geometry geometry : geometries) {
			if (geometry.isEmpty() || filtered(geometry.getUserData())) {
				continue;
			}
			if (geometry instanceof MultiLineString) {
				drawMultiLineString(canvas, tileId, queryController, (MultiLineString) geometry, paintLine,
						tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31);
			} else if (geometry instanceof LineString) {
				drawLine(canvas, tileId, queryController, (LineString) geometry, paintLine,
						tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31);
			}
		}
	}

	private void drawMultiLineString(Canvas canvas, TileId tileId, IQueryController queryController,
	                                 MultiLineString multiLineString, Paint paintLine,
	                                 AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
			Geometry geometry = multiLineString.getGeometryN(i);
			if (geometry instanceof LineString && !geometry.isEmpty()) {
				drawLine(canvas, tileId, queryController, (LineString) geometry, paintLine,
						tileBBox31, tileBBox31Enlarged, mult, zoomShift, tileSize, tileSize31);
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

	private void drawLine(Canvas canvas, TileId tileId, IQueryController queryController, LineString line, Paint paintLine,
	                      AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		if (line.getCoordinateSequence().size() == 0
				|| (queryController != null && queryController.isAborted())) {
			return;
		}
		Coordinate[] coordinates = line.getCoordinateSequence().toCoordinateArray();

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
					path.moveTo(x1, y1);
					recalculateLastXY = false;
				}
				path.lineTo(x2, y2);
				//canvas.drawLine(x1, y1, x2, y2, paintLine);
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
		if (!path.isEmpty()) {
			canvas.drawPath(path, paintLine);
		}
	}

	private Map<QuadPointDouble, Map<?, ?>> drawPoints(Canvas canvas, TileId tileId, IQueryController queryController, List<Geometry> geometries,
	                                                   AreaI tileBBox31, AreaI tileBBox31Enlarged, int mult, int zoomShift, int tileSize, double tileSize31) {
		Map<QuadPointDouble, Map<?, ?>> visiblePoints = new HashMap<>();
		if (queryController != null && queryController.isAborted()) {
			return visiblePoints;
		}
		double bitmapHalfSize = bitmapPoint.getWidth() / 2.0d;
		PointI topLeft = tileBBox31.getTopLeft();
		int tileBBox31Left = topLeft.getX();
		int tileBBox31Top = topLeft.getY();

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

				if (tileBBox31Enlarged.contains((int) tileX, (int) tileY) && !filtered(userData)) {
					double x = ((tileX - tileBBox31Left) / tileSize31) * tileSize - bitmapHalfSize;
					double y = ((tileY - tileBBox31Top) / tileSize31) * tileSize - bitmapHalfSize;
					canvas.drawBitmap(bitmapPoint, (float) x, (float) y, paintPoint);
					//visiblePoints.put(new QuadPointDouble(tileX + px, tileY + py), userData);
				}
			}
		}
		return visiblePoints;
	}
}
