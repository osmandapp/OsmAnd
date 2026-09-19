package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Pair;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import net.osmand.Location;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.VectorDouble;
import net.osmand.core.jni.VectorLineBuilder;
import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.render.OsmandDashPathEffect;
import net.osmand.shared.routing.ColoringType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.Renderable;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.geometry.GeometryWayPathAlgorithms;
import net.osmand.plus.views.layers.geometry.GpxGeometryWay;
import net.osmand.plus.views.layers.geometry.GpxGeometryWayContext;
import net.osmand.plus.views.mapwidgets.MarkersWidgetsHelper;
import net.osmand.shared.gpx.primitives.TrkSegment;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MapMarkersLayer extends OsmandMapLayer implements IContextMenuProvider,
		IContextMenuProviderSelection, ContextMenuLayer.IMoveObjectProvider {

	private static final int START_ZOOM = 3;
	private static final long USE_FINGER_LOCATION_DELAY = 1000;
	private static final int MAP_REFRESH_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 6;
	protected static final int DIST_TO_SHOW = 80;

	private MarkersWidgetsHelper markersWidgetsHelper;

	private Paint bitmapPaint;
	private Bitmap markerBitmapBlue;
	private Bitmap markerBitmapGreen;
	private Bitmap markerBitmapOrange;
	private Bitmap markerBitmapRed;
	private Bitmap markerBitmapYellow;
	private Bitmap markerBitmapTeal;
	private Bitmap markerBitmapPurple;

	private Paint bitmapPaintDestBlue;
	private Paint bitmapPaintDestGreen;
	private Paint bitmapPaintDestOrange;
	private Paint bitmapPaintDestRed;
	private Paint bitmapPaintDestYellow;
	private Paint bitmapPaintDestTeal;
	private Paint bitmapPaintDestPurple;
	private Bitmap arrowLight;
	private Bitmap arrowToDestination;
	private Bitmap arrowShadow;
	private final float[] calculations = new float[2];

	private final TextPaint textPaint = new TextPaint();
	private final RenderingLineAttributes lineAttrs = new RenderingLineAttributes("measureDistanceLine");
	private final RenderingLineAttributes textAttrs = new RenderingLineAttributes("rulerLineFont");
	private final RenderingLineAttributes planRouteAttrs = new RenderingLineAttributes("markerPlanRouteline");
	private TrkSegment route;

	private float textSize;
	private float verticalOffset;

	private LatLon fingerLocation;
	private boolean hasMoved;
	private boolean moving;
	private boolean useFingerLocation;
	private GestureDetector longTapDetector;
	private Handler handler;

	private ContextMenuLayer contextMenuLayer;

	private boolean inPlanRouteMode;
	private boolean defaultAppMode = true;
	private boolean carView;
	private float textScale = 1f;
	private double markerSizePx;

	public CustomMapObjects<MapMarker> customObjectsDelegate;

	//OpenGL
	private int markersCount;
	private VectorLinesCollection vectorLinesCollection;
	private boolean needDrawLines = true;
	private final List<MapMarker> displayedMarkers = new ArrayList<>();
	private int displayedWidgets;
	private List<WptPt> cachedPoints = null;
	private Renderable.RenderableSegment cachedRenderer;
	private Location savedLoc;
	private PointI cachedTarget31;
	private int cachedZoom = 0;
	private final HashMap<Integer, Path> cachedPaths = new HashMap<>();

	private final List<Amenity> amenities = new ArrayList<>();

	public MapMarkersLayer(@NonNull Context context) {
		super(context);
	}

	public MarkersWidgetsHelper getMarkersWidgetsHelper() {
		return markersWidgetsHelper;
	}

	public boolean isInPlanRouteMode() {
		return inPlanRouteMode;
	}

	public void setInPlanRouteMode(boolean inPlanRouteMode) {
		this.inPlanRouteMode = inPlanRouteMode;
	}

	public void setDefaultAppMode(boolean defaultAppMode) {
		this.defaultAppMode = defaultAppMode;
	}

	private void initUI() {
		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setFilterBitmap(true);

		updateBitmaps(true);

		bitmapPaintDestBlue = createPaintDest(R.color.marker_blue);
		bitmapPaintDestGreen = createPaintDest(R.color.marker_green);
		bitmapPaintDestOrange = createPaintDest(R.color.marker_orange);
		bitmapPaintDestRed = createPaintDest(R.color.marker_red);
		bitmapPaintDestYellow = createPaintDest(R.color.marker_yellow);
		bitmapPaintDestTeal = createPaintDest(R.color.marker_teal);
		bitmapPaintDestPurple = createPaintDest(R.color.marker_purple);

		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		initUI();
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			markersWidgetsHelper = new MarkersWidgetsHelper(mapActivity);
			longTapDetector = new GestureDetector(mapActivity, new GestureDetector.SimpleOnGestureListener() {
				@Override
				public void onLongPress(MotionEvent e) {
					cancelFingerAction();
				}
			});
		} else {
			if (markersWidgetsHelper != null) {
				markersWidgetsHelper.clearListeners();
				markersWidgetsHelper = null;
			}
			longTapDetector = null;
		}
	}

	private Paint createPaintDest(int colorId) {
		Paint paint = new Paint();
		paint.setDither(true);
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		int color = ContextCompat.getColor(getContext(), colorId);
		paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
		return paint;
	}

	private Paint getMarkerDestPaint(int colorIndex) {
		switch (colorIndex) {
			case 0:
				return bitmapPaintDestBlue;
			case 1:
				return bitmapPaintDestGreen;
			case 2:
				return bitmapPaintDestOrange;
			case 3:
				return bitmapPaintDestRed;
			case 4:
				return bitmapPaintDestYellow;
			case 5:
				return bitmapPaintDestTeal;
			case 6:
				return bitmapPaintDestPurple;
			default:
				return bitmapPaintDestBlue;
		}
	}

	private Bitmap getMapMarkerBitmap(int colorIndex) {
		switch (colorIndex) {
			case 0:
				return markerBitmapBlue;
			case 1:
				return markerBitmapGreen;
			case 2:
				return markerBitmapOrange;
			case 3:
				return markerBitmapRed;
			case 4:
				return markerBitmapYellow;
			case 5:
				return markerBitmapTeal;
			case 6:
				return markerBitmapPurple;
			default:
				return markerBitmapBlue;
		}
	}

	public void setRoute(TrkSegment route) {
		this.route = route;
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		handler = new Handler();
		initUI();
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings drawSettings) {
		super.onPrepareBufferImage(canvas, tileBox, drawSettings);
		OsmandApplication app = getApplication();
		OsmandSettings settings = app.getSettings();
		if ((!settings.SHOW_MAP_MARKERS.get() && customObjectsDelegate == null)
				|| (customObjectsDelegate != null && Algorithms.isEmpty(customObjectsDelegate.getMapObjects()))) {
			clearMapMarkersCollections();
			clearVectorLinesCollections();
			resetCachedRenderer();
			return;
		}

		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<MapMarker> activeMapMarkers = (customObjectsDelegate != null) ? customObjectsDelegate.getMapObjects() : markersHelper.getMapMarkers();
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (markersCount != activeMapMarkers.size() || mapActivityInvalidated) {
				clearMapMarkersCollections();
				clearVectorLinesCollections();
				cachedPaths.clear();
				cachedTarget31 = null;
			}
			initMarkersCollection();
			markersCount = activeMapMarkers.size();
			mapActivityInvalidated = false;
		}

		if (route != null && route.getPoints().size() > 0) {
			planRouteAttrs.updatePaints(app, drawSettings, tileBox);
			if (mapRenderer != null) {
				boolean shouldDraw = shouldDrawPoints();
				if (shouldDraw || mapActivityInvalidated) {
					resetCachedRenderer();
					int baseOrder = getPointsOrder() - 10;
					QuadRect correctedQuadRect = getCorrectedQuadRect(tileBox.getLatLonBounds());
					Renderable.RenderableSegment renderer = new Renderable.StandardTrack(new ArrayList<>(route.getPoints()), 17.2);
					route.setRenderer(renderer);
					GpxGeometryWayContext wayContext = new GpxGeometryWayContext(getContext(), view.getDensity());
					GpxGeometryWay geometryWay = new GpxGeometryWay(wayContext);
					geometryWay.baseOrder = baseOrder;
					renderer.setTrackParams(lineAttrs.paint.getColor(), "", ColoringType.TRACK_SOLID, null, null);
					renderer.setDrawArrows(false);
					renderer.setGeometryWay(geometryWay);
					cachedRenderer = renderer;
					cachedPoints = new ArrayList<>(route.getPoints());
					renderer.drawGeometry(canvas, tileBox, correctedQuadRect, planRouteAttrs.paint.getColor(),
							planRouteAttrs.paint.getStrokeWidth(), getDashPattern(planRouteAttrs.paint));
				}
			} else {
				new Renderable.StandardTrack(new ArrayList<>(route.getPoints()), 17.2).
						drawSegment(view.getZoom(), defaultAppMode ? planRouteAttrs.paint : planRouteAttrs.paint2, canvas, tileBox);
			}
		} else {
			resetCachedRenderer();
			cachedPoints = null;
		}

		if (settings.SHOW_LINES_TO_FIRST_MARKERS.get() && mapRenderer == null) {
			drawLineAndText(canvas, tileBox, drawSettings);
		}
	}

	private void resetCachedRenderer() {
		if (cachedRenderer != null) {
			GpxGeometryWay geometryWay = cachedRenderer.getGeometryWay();
			if (geometryWay != null) {
				geometryWay.resetSymbolProviders();
			}
		}
	}

	private boolean shouldDrawPoints() {
		boolean shouldDraw = true;
		if (cachedPoints != null && cachedPoints.size() == route.getPoints().size()) {
			shouldDraw = false;
			for (int i = 0; i < cachedPoints.size(); i++) {
				if (!route.getPoints().get(i).equals(cachedPoints.get(i))) {
					shouldDraw = true;
					break;
				}
			}
		}
		return shouldDraw;
	}

	@Nullable
	private float[] getDashPattern(@NonNull Paint paint) {
		float[] intervals = null;
		PathEffect pathEffect = paint.getPathEffect();
		if (pathEffect instanceof OsmandDashPathEffect) {
			intervals = ((OsmandDashPathEffect) pathEffect).getIntervals();
		}
		return intervals;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		if (markersWidgetsHelper != null) {
			markersWidgetsHelper.setCustomLatLon(useFingerLocation ? fingerLocation : null);
		}
		OsmandApplication app = getApplication();
		OsmandSettings settings = app.getSettings();

		if (customObjectsDelegate != null && Algorithms.isEmpty(customObjectsDelegate.getMapObjects())
				|| customObjectsDelegate == null && (tileBox.getZoom() < 3 || !settings.SHOW_MAP_MARKERS.get())) {
			clearVectorLinesCollections();
			return;
		}

		MapRendererView mapRenderer = getMapRenderer();
		int displayedWidgets = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get();
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		updateBitmaps(false);

		List<MapMarker> markers = customObjectsDelegate != null ? customObjectsDelegate.getMapObjects() : markersHelper.getMapMarkers();
		if (mapRenderer == null) {
			for (MapMarker marker : markers) {
				if (isMarkerVisible(tileBox, marker) && !overlappedByWaypoint(marker)
						&& !isInMotion(marker) && !isSynced(marker)) {
					Bitmap bmp = getMapMarkerBitmap(marker.colorIndex);
					int marginX = bmp.getWidth() / 6;
					int marginY = bmp.getHeight();
					int locationX = tileBox.getPixXFromLonNoRot(marker.getLongitude());
					int locationY = tileBox.getPixYFromLatNoRot(marker.getLatitude());
					canvas.rotate(-tileBox.getRotate(), locationX, locationY);
					canvas.drawBitmap(bmp, locationX - marginX, locationY - marginY, bitmapPaint);
					canvas.rotate(tileBox.getRotate(), locationX, locationY);
				}
			}
		}

		if (settings.SHOW_LINES_TO_FIRST_MARKERS.get() && mapRenderer != null) {
			drawLineAndText(canvas, tileBox, nightMode);
		} else {
			clearVectorLinesCollections();
		}
		if (settings.SHOW_ARROWS_TO_FIRST_MARKERS.get()) {
			LatLon loc = tileBox.getCenterLatLon();
			int i = 0;
			for (MapMarker marker : markers) {
				if (!isLocationVisible(tileBox, marker) && !isInMotion(marker)) {
					canvas.save();
					float bearing;
					float radiusBearing = DIST_TO_SHOW * tileBox.getDensity();
					float cx;
					float cy;
					if (mapRenderer != null) {
						PointI marker31 = NativeUtilities.getPoint31FromLatLon(marker.getLatitude(), marker.getLongitude());
						PointI center31 = NativeUtilities.get31FromElevatedPixel(mapRenderer, tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
						if (center31 == null) {
							continue;
						}
						Pair<PointF, PointF> line =
								NativeUtilities.clipLineInVisibleRect(mapRenderer, tileBox, center31, marker31);
						if (line == null) {
							continue;
						}
						PointF centerPixel = new PointF(tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
						cx = centerPixel.x;
						cy = centerPixel.y;
						bearing = (float) getAngleBetween(centerPixel, line.second) - tileBox.getRotate();
					} else {
						QuadPoint cp = tileBox.getCenterPixelPoint();
						cx = cp.x;
						cy = cp.y;
						net.osmand.Location.distanceBetween(loc.getLatitude(), loc.getLongitude(),
								marker.getLatitude(), marker.getLongitude(), calculations);
						bearing = calculations[1] - 90;
					}
					canvas.rotate(bearing, cx, cy);
					canvas.translate(-24 * tileBox.getDensity() + radiusBearing, -22 * tileBox.getDensity());
					canvas.drawBitmap(arrowShadow, cx, cy, bitmapPaint);
					canvas.drawBitmap(arrowToDestination, cx, cy, getMarkerDestPaint(marker.colorIndex));
					canvas.drawBitmap(arrowLight, cx, cy, bitmapPaint);
					canvas.restore();
				}
				i++;
				if (i > displayedWidgets - 1) {
					break;
				}
			}
		}
		Object movableObject = contextMenuLayer.getMoveableObject();
		if (movableObject instanceof MapMarker) {
			MapMarker movableMarker = (MapMarker) movableObject;
			setMovableObject(movableMarker.getLatitude(), movableMarker.getLongitude());
			drawMovableMarker(canvas, tileBox, (MapMarker) movableObject);
		}
		if (this.movableObject != null && !contextMenuLayer.isInChangeMarkerPositionMode()) {
			cancelMovableObject();
		}
	}

	private void updateBitmaps(boolean forceUpdate) {
		OsmandApplication app = getApplication();
		float textScale = getTextScale();
		boolean carView = app.getOsmandMap().getMapView().isCarView();
		if (this.textScale != textScale || this.carView != carView || forceUpdate) {
			this.textScale = textScale;
			this.carView = carView;
			recreateBitmaps();
			textSize = app.getResources().getDimensionPixelSize(R.dimen.guide_line_text_size) * textScale;
			verticalOffset = app.getResources().getDimensionPixelSize(R.dimen.guide_line_vertical_offset) * textScale;
		}
	}

	private void recreateBitmaps() {
		markerBitmapBlue = getScaledBitmap(R.drawable.map_marker_blue);
		markerBitmapGreen = getScaledBitmap(R.drawable.map_marker_green);
		markerBitmapOrange = getScaledBitmap(R.drawable.map_marker_orange);
		markerBitmapRed = getScaledBitmap(R.drawable.map_marker_red);
		markerBitmapYellow = getScaledBitmap(R.drawable.map_marker_yellow);
		markerBitmapTeal = getScaledBitmap(R.drawable.map_marker_teal);
		markerBitmapPurple = getScaledBitmap(R.drawable.map_marker_purple);

		markerSizePx = Math.sqrt(markerBitmapBlue.getWidth() * markerBitmapBlue.getWidth()
				+ markerBitmapBlue.getHeight() * markerBitmapBlue.getHeight());

		arrowLight = getScaledBitmap(R.drawable.map_marker_direction_arrow_p1_light);
		arrowToDestination = getScaledBitmap(R.drawable.map_marker_direction_arrow_p2_color);
		arrowShadow = getScaledBitmap(R.drawable.map_marker_direction_arrow_p3_shadow);
	}

	@Nullable
	@Override
	protected Bitmap getScaledBitmap(int drawableId) {
		return getScaledBitmap(drawableId, textScale);
	}

	private boolean isSynced(@NonNull MapMarker marker) {
		return marker.wptPt != null || marker.favouritePoint != null;
	}

	private boolean isInMotion(@NonNull MapMarker marker) {
		return marker.equals(contextMenuLayer.getMoveableObject());
	}

	public boolean isLocationVisible(RotatedTileBox tb, MapMarker marker) {
		//noinspection SimplifiableIfStatement
		if (marker == null || tb == null) {
			return false;
		}
		return containsLatLon(tb, marker.getLatitude(), marker.getLongitude(), 0, 0);
	}

	public boolean isMarkerVisible(RotatedTileBox tb, MapMarker marker) {
		//noinspection SimplifiableIfStatement
		if (marker == null || tb == null) {
			return false;
		}
		return containsLatLon(tb, marker.getLatitude(), marker.getLongitude(), markerSizePx, markerSizePx);
	}

	public boolean containsLatLon(RotatedTileBox tb, double lat, double lon, double w, double h) {
		double widgetHeight = 0;
		if (markersWidgetsHelper != null
				&& markersWidgetsHelper.isMapMarkersBarWidgetVisible()
				&& !getApplication().getOsmandMap().getMapView().isCarView()) {
			widgetHeight = markersWidgetsHelper.getMapMarkersBarWidgetHeight();
		}
		PointF pixel = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, lat, lon);
		double tx = pixel.x;
		double ty = pixel.y;
		return tx >= -w && tx <= tb.getPixWidth() + w && ty >= widgetHeight - h && ty <= tb.getPixHeight() + h;
	}

	public boolean overlappedByWaypoint(MapMarker marker) {
		List<TargetPoint> targetPoints = getApplication().getTargetPointsHelper().getAllPoints();
		for (TargetPoint t : targetPoints) {
			if (t.point.equals(marker.point)) {
				return true;
			}
		}
		return false;
	}

	private void drawMovableMarker(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox, @NonNull MapMarker movableMarker) {
		PointF point = contextMenuLayer.getMovableCenterPoint(tileBox);
		Bitmap bitmap = getMapMarkerBitmap(movableMarker.colorIndex);
		int marginX = bitmap.getWidth() / 6;
		int marginY = bitmap.getHeight();

		canvas.save();
		canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		canvas.drawBitmap(bitmap, point.x - marginX, point.y - marginY, bitmapPaint);
		canvas.restore();
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event, @NonNull RotatedTileBox tileBox) {
		if (longTapDetector != null && !longTapDetector.onTouchEvent(event)) {
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					float x = event.getX();
					float y = event.getY();
					fingerLocation = NativeUtilities.getLatLonFromElevatedPixel(getMapRenderer(), tileBox, x, y);
					hasMoved = false;
					moving = true;
					break;

				case MotionEvent.ACTION_MOVE:
					if (!hasMoved) {
						if (!handler.hasMessages(MAP_REFRESH_MESSAGE)) {
							Message msg = Message.obtain(handler, () -> {
								handler.removeMessages(MAP_REFRESH_MESSAGE);
								if (moving) {
									if (!useFingerLocation) {
										useFingerLocation = true;
										getApplication().getOsmandMap().refreshMap();
									}
								}
							});
							msg.what = MAP_REFRESH_MESSAGE;
							handler.sendMessageDelayed(msg, USE_FINGER_LOCATION_DELAY);
						}
						hasMoved = true;
					}
					break;

				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					cancelFingerAction();
					break;
			}
		}
		return super.onTouchEvent(event, tileBox);
	}

	private void cancelFingerAction() {
		handler.removeMessages(MAP_REFRESH_MESSAGE);
		useFingerLocation = false;
		moving = false;
		fingerLocation = null;
		getApplication().getOsmandMap().refreshMap();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean disableSingleTap() {
		return inPlanRouteMode;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return inPlanRouteMode;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		MapActivity mapActivity = getMapActivity();
		OsmandSettings settings = getApplication().getSettings();
		if (unknownLocation
				|| mapActivity == null
				|| !(o instanceof MapMarker)
				|| !settings.SELECT_MARKER_ON_SINGLE_TAP.get()
				|| !settings.SHOW_MAP_MARKERS.get()) {
			return false;
		}
		MapMarkersHelper helper = getApplication().getMapMarkersHelper();
		MapMarker old = helper.getMapMarkers().get(0);
		helper.moveMarkerToTop((MapMarker) o);
		String title = getContext().getString(R.string.marker_activated, helper.getMapMarkers().get(0).getName(getContext()));
		Snackbar.make(mapActivity.findViewById(R.id.bottomFragmentContainer), title, Snackbar.LENGTH_LONG)
				.setAction(R.string.shared_string_cancel, v -> helper.moveMarkerToTop(old))
				.show();
		return true;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		OsmandApplication app = getApplication();
		OsmandSettings settings = app.getSettings();
		List<MapMarker> mapMarkers = app.getMapMarkersHelper().getMapMarkers();

		if (tileBox.getZoom() < START_ZOOM
				|| !settings.SHOW_MAP_MARKERS.get()
				|| excludeUntouchableObjects
				|| Algorithms.isEmpty(mapMarkers)) {
			return;
		}

		amenities.clear();
		MapRendererView mapRenderer = getMapRenderer();
		float radius = getScaledTouchRadius(app, tileBox.getDefaultRadiusPoi()) * TOUCH_RADIUS_MULTIPLIER;
		QuadRect screenArea = new QuadRect(
				point.x - radius,
				point.y - radius / 2f,
				point.x + radius,
				point.y + radius * 4f
		);
		List<PointI> touchPolygon31 = null;
		if (mapRenderer != null) {
			touchPolygon31 = NativeUtilities.getPolygon31FromScreenArea(mapRenderer, screenArea);
			if (touchPolygon31 == null) {
				return;
			}
		}

		boolean selectMarkerOnSingleTap = settings.SELECT_MARKER_ON_SINGLE_TAP.get();

		for (MapMarker marker : mapMarkers) {
			if ((!unknownLocation && selectMarkerOnSingleTap) || !isSynced(marker)) {
				LatLon latLon = marker.point;
				if (latLon != null) {
					boolean add = mapRenderer != null
							? NativeUtilities.isPointInsidePolygon(latLon, touchPolygon31)
							: tileBox.isLatLonInsidePixelArea(latLon, screenArea);
					if (add) {
						if (!unknownLocation && selectMarkerOnSingleTap) {
							o.add(marker);
						} else {
							if (isMarkerOnFavorite(marker) && settings.SHOW_FAVORITES.get()
									|| isMarkerOnWaypoint(marker) && settings.SHOW_WPT.get()) {
								continue;
							}
							Amenity mapObj = getMapObjectByMarker(marker);
							if (mapObj != null) {
								amenities.add(mapObj);
								o.add(mapObj);
							} else {
								o.add(marker);
							}
						}
					}
				}
			}
		}
	}

	private boolean isMarkerOnWaypoint(@NonNull MapMarker marker) {
		return marker.point != null && getApplication().getSelectedGpxHelper().getVisibleWayPointByLatLon(marker.point) != null;
	}

	private boolean isMarkerOnFavorite(@NonNull MapMarker marker) {
		return marker.point != null && getApplication().getFavoritesHelper().getVisibleFavByLatLon(marker.point) != null;
	}

	@Nullable
	public Amenity getMapObjectByMarker(@NonNull MapMarker marker) {
		if (marker.mapObjectName != null && marker.point != null) {
			String mapObjName = marker.mapObjectName.split("_")[0];
			return MapSelectionHelper.findAmenity(getApplication(), marker.point, Collections.singletonList(mapObjName), -1, 15);
		}
		return null;
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof MapMarker) {
			return ((MapMarker) o).point;
		} else if (o instanceof Amenity && amenities.contains(o)) {
			return ((Amenity) o).getLocation();
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof MapMarker) {
			return ((MapMarker) o).getPointDescription(getContext());
		}
		return null;
	}

	@Override
	public int getOrder(Object o) {
		return 0;
	}

	@Override
	public void setSelectedObject(Object o) {
	}

	@Override
	public void clearSelectedObject() {
	}

	@Override
	public boolean isObjectMovable(Object o) {
		return o instanceof MapMarker;
	}

	@Override
	public void applyNewObjectPosition(@NonNull Object o, @NonNull LatLon position,
	                                   @Nullable ApplyMovedObjectCallback callback) {
		boolean result = false;
		MapMarker newObject = null;
		if (o instanceof MapMarker) {
			MapMarkersHelper markersHelper = getApplication().getMapMarkersHelper();
			MapMarker marker = (MapMarker) o;

			PointDescription originalDescription = marker.getOriginalPointDescription();
			if (originalDescription.isLocation()) {
				originalDescription.setName(PointDescription.getSearchAddressStr(getContext()));
			}
			markersHelper.moveMapMarker(marker, position);
			int index = markersHelper.getMapMarkers().indexOf(marker);
			if (index != -1) {
				newObject = markersHelper.getMapMarkers().get(index);
			}
			result = true;
			if (displayedMarkers.contains(marker)) {
				clearVectorLinesCollections();
			}
		}
		if (callback != null) {
			callback.onApplyMovedObject(result, newObject == null ? o : newObject);
		}
		applyMovableObject(position);
	}

	/**
	 * OpenGL
	 */
	private void initMarkersCollection() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (mapMarkersCollection != null && mapRenderer.hasSymbolsProvider(mapMarkersCollection)) {
			return;
		}
		mapMarkersCollection = new MapMarkersCollection();
		OsmandApplication app = getApplication();
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		updateBitmaps(false);

		for (MapMarker marker : markersHelper.getMapMarkers()) {
			if (!overlappedByWaypoint(marker) && !isSynced(marker)) {
				Bitmap bmp = getMapMarkerBitmap(marker.colorIndex);
				MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
				PointI pointI = NativeUtilities.getPoint31FromLatLon(marker.getLatitude(), marker.getLongitude());
				int color = getColorByIndex(marker.colorIndex);
				boolean isMoveable = isInMotion(marker);

				mapMarkerBuilder.setIsAccuracyCircleSupported(false)
						.setBaseOrder(getPointsOrder())
						.setIsHidden(isMoveable)
						.setPinIcon(NativeUtilities.createSkImageFromBitmap(bmp))
						.setPosition(pointI)
						.setPinIconVerticalAlignment(net.osmand.core.jni.MapMarker.PinIconVerticalAlignment.Top)
						.setPinIconHorisontalAlignment(net.osmand.core.jni.MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
						.setPinIconOffset(new PointI(bmp.getWidth() / 3, 0))
						.setAccuracyCircleBaseColor(NativeUtilities.createFColorRGB(color))
						.buildAndAddToCollection(mapMarkersCollection);
			}
		}
		mapRenderer.addSymbolsProvider(mapMarkersCollection);
	}

	/**
	 * OpenGL
	 */
	private void initVectorLinesCollection(LatLon loc, MapMarker marker, int color, boolean isLast) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || !needDrawLines) {
			return;
		}

		PointI start = NativeUtilities.getPoint31FromLatLon(loc);
		PointI end = NativeUtilities.getPoint31FromLatLon(marker.getLatitude(), marker.getLongitude());

		if (vectorLinesCollection == null) {
			vectorLinesCollection = new VectorLinesCollection();
		}

		QVectorPointI points = new QVectorPointI();
		points.add(start);
		points.add(end);

		VectorLineBuilder outlineBuilder = new VectorLineBuilder();
		VectorDouble outlinePattern = new VectorDouble();
		outlinePattern.add(75.0d / (double) getMapDensity());
		outlinePattern.add(55.0d / (double) getMapDensity());
		FColorARGB outlineColor = new FColorARGB(1.0f, 1.0f, 1.0f, 1.0f);
		double strokeWidth = 20.0d;
		int outlineId = isLast ? 20 : 10;
		int lineId = isLast ? 21 : 11;
		outlineBuilder.setBaseOrder(getBaseOrder() + lineId + 1)
				.setIsHidden(false)
				.setLineId(outlineId)
				.setLineWidth(strokeWidth * 1.5)
				.setLineDash(outlinePattern)
				.setPoints(points)
				.setFillColor(outlineColor);
		outlineBuilder.buildAndAddToCollection(vectorLinesCollection);

		VectorLineBuilder inlineBuilder = new VectorLineBuilder();
		VectorDouble inlinePattern = new VectorDouble();
		inlinePattern.add(-strokeWidth / 2 / getMapDensity());
		inlinePattern.add((75 - strokeWidth) / getMapDensity());
		inlinePattern.add((55 + strokeWidth) / getMapDensity());
		inlineBuilder.setBaseOrder(getBaseOrder() + lineId)
				.setIsHidden(false)
				.setLineId(lineId)
				.setLineWidth(strokeWidth)
				.setLineDash(inlinePattern)
				.setPoints(points)
				.setFillColor(NativeUtilities.createFColorARGB(color));
		inlineBuilder.buildAndAddToCollection(vectorLinesCollection);
		if (isLast) {
			mapRenderer.addSymbolsProvider(vectorLinesCollection);
			needDrawLines = false;
		}
	}

	/**
	 * OpenGL
	 */
	protected void clearVectorLinesCollections() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null && vectorLinesCollection != null) {
			mapRenderer.removeSymbolsProvider(vectorLinesCollection);
			vectorLinesCollection = null;
			needDrawLines = true;
		}
	}

	/**
	 * OpenGL
	 */
	private double getAngleBetween(PointF start, PointF end) {
		double dx = start.x - end.x;
		double dy = start.y - end.y;
		double radians = dx != 0 ? Math.atan(dy / dx) : (dy < 0 ? Math.PI / 2 : -Math.PI / 2);
		return Math.toDegrees(radians - (start.x > end.x ? Math.PI : 0));
	}

	@ColorInt
	private int getColorByIndex(int colorIndex) {
		int colorResId;
		switch (colorIndex) {
			case 1:
				colorResId = R.color.marker_green;
				break;
			case 2:
				colorResId = R.color.marker_orange;
				break;
			case 3:
				colorResId = R.color.marker_red;
				break;
			case 4:
				colorResId = R.color.marker_yellow;
				break;
			case 5:
				colorResId = R.color.marker_teal;
				break;
			case 6:
				colorResId = R.color.marker_purple;
				break;
			default:
				colorResId = R.color.marker_blue;
				break;
		}
		return getContext().getResources().getColor(colorResId, null);
	}

	private void drawLineAndText(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		Location myLoc;
		OsmandApplication app = getApplication();
		MapRendererView mapRenderer = getMapRenderer();
		if (useFingerLocation && fingerLocation != null) {
			myLoc = new Location("");
			myLoc.setLatitude(fingerLocation.getLatitude());
			myLoc.setLongitude(fingerLocation.getLongitude());
		} else {
			myLoc = app.getLocationProvider().getLastStaleKnownLocation();
		}
		if (myLoc == null) {
			clearVectorLinesCollections();
			return;
		}

		if (savedLoc != null && !MapUtils.areLatLonEqual(myLoc, savedLoc)) {
			clearVectorLinesCollections();
		}
		savedLoc = myLoc;
		OsmandSettings settings = app.getSettings();
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		List<MapMarker> activeMapMarkers = markersHelper.getMapMarkers();
		if (displayedWidgets != settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get()) {
			displayedWidgets = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get();
			clearVectorLinesCollections();
		} else {
			for (int i = 0; mapRenderer != null && i < activeMapMarkers.size() && i < displayedMarkers.size(); i++) {
				if (displayedMarkers.get(i) != activeMapMarkers.get(i)) {
					clearVectorLinesCollections();
					break;
				}
			}
		}

		displayedMarkers.clear();
		textAttrs.paint.setTextSize(textSize);
		textAttrs.paint2.setTextSize(textSize);
		lineAttrs.updatePaints(app, nightMode, tileBox);
		textAttrs.updatePaints(app, nightMode, tileBox);
		textAttrs.paint.setStyle(Paint.Style.FILL);
		textPaint.set(textAttrs.paint);

		boolean drawMarkerName = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 1;
		float locX;
		float locY;
		LatLon loc;
		MapViewTrackingUtilities mapViewTrackingUtilities = app.getMapViewTrackingUtilities();
		if (mapViewTrackingUtilities.isMapLinkedToLocation()
				&& !MapViewTrackingUtilities.isSmallSpeedForAnimation(myLoc)
				&& !mapViewTrackingUtilities.isMovingToMyLocation()) {
			loc = new LatLon(tileBox.getLatitude(), tileBox.getLongitude());
		} else {
			loc = new LatLon(myLoc.getLatitude(), myLoc.getLongitude());
		}
		int[] colors = MapMarker.getColors(getContext());
		for (int i = 0; i < activeMapMarkers.size() && i < displayedWidgets; i++) {
			List<Float> tx = new ArrayList<>();
			List<Float> ty = new ArrayList<>();
			Path linePath = new Path();
			MapMarker marker = activeMapMarkers.get(i);
			float markerX;
			float markerY;
			int color = colors[marker.colorIndex];
			if (mapRenderer != null) {
				boolean isLast = (i == activeMapMarkers.size() - 1) || (i == displayedWidgets - 1);
				//draw line in OpenGL
				initVectorLinesCollection(loc, marker, color, isLast);
				displayedMarkers.add(marker);

				PointI lineStart31 = NativeUtilities.getPoint31FromLatLon(loc);
				PointI lineEnd31 = NativeUtilities.getPoint31FromLatLon(marker.getLatitude(), marker.getLongitude());
				Pair<PointF, PointF> line = NativeUtilities.clipLineInVisibleRect(mapRenderer, tileBox, lineStart31, lineEnd31);
				if (line != null) {
					locX = line.first.x;
					locY = line.first.y;
					markerX = line.second.x;
					markerY = line.second.y;
				} else {
					continue;
				}
			} else {
				locX = tileBox.getPixXFromLatLon(loc.getLatitude(), loc.getLongitude());
				locY = tileBox.getPixYFromLatLon(loc.getLatitude(), loc.getLongitude());
				markerX = tileBox.getPixXFromLatLon(marker.getLatitude(), marker.getLongitude());
				markerY = tileBox.getPixYFromLatLon(marker.getLatitude(), marker.getLongitude());
			}

			if (mapRenderer != null) {
				PointI target31 = mapRenderer.getTarget();
				if (cachedTarget31 != null && cachedTarget31.getX() == target31.getX() && cachedTarget31.getY() == target31.getY()) {
					cachedPaths.clear();
				}
				cachedTarget31 = target31;
				if (view.getZoom() != cachedZoom) {
					cachedPaths.clear();
					cachedZoom = view.getZoom();
				}
			}

			if (mapRenderer == null || !cachedPaths.containsKey(i)) {
				tx.add(locX);
				ty.add(locY);
				tx.add(markerX);
				ty.add(markerY);
				GeometryWayPathAlgorithms.calculatePath(tileBox, tx, ty, linePath);
				cachedPaths.put(i, linePath);
			} else {
				linePath = cachedPaths.get(i);
			}

			PathMeasure pm = new PathMeasure(linePath, false);
			float[] pos = new float[2];
			pm.getPosTan(pm.getLength() / 2, pos, null);

			float dist = (float) MapUtils.getDistance(myLoc.getLatitude(), myLoc.getLongitude(), marker.getLatitude(), marker.getLongitude());
			String distSt = OsmAndFormatter.getFormattedDistance(dist, view.getApplication());
			String text = distSt + (drawMarkerName ? " • " + marker.getName(getContext()) : "");
			text = TextUtils.ellipsize(text, textPaint, pm.getLength(), TextUtils.TruncateAt.END).toString();
			Rect bounds = new Rect();
			textAttrs.paint.getTextBounds(text, 0, text.length(), bounds);
			float hOffset = pm.getLength() / 2 - bounds.width() / 2f;
			lineAttrs.paint.setColor(color);

			canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
			if (mapRenderer == null) {
				canvas.drawPath(linePath, lineAttrs.paint);
			}
			if (locX >= markerX) {
				canvas.rotate(180, pos[0], pos[1]);
				canvas.drawTextOnPath(text, linePath, hOffset, bounds.height() + verticalOffset, textAttrs.paint2);
				canvas.drawTextOnPath(text, linePath, hOffset, bounds.height() + verticalOffset, textAttrs.paint);
				canvas.rotate(-180, pos[0], pos[1]);
			} else {
				canvas.drawTextOnPath(text, linePath, hOffset, -verticalOffset, textAttrs.paint2);
				canvas.drawTextOnPath(text, linePath, hOffset, -verticalOffset, textAttrs.paint);
			}
			canvas.rotate(tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		}
	}

	public void setCustomMapObjects(List<MapMarker> mapMarkers) {
		if (customObjectsDelegate != null) {
			customObjectsDelegate.setCustomMapObjects(mapMarkers);
			getApplication().getOsmandMap().refreshMap();
		}
	}
}
