package net.osmand.plus.views.layers;

import static net.osmand.plus.views.mapwidgets.WidgetType.RADIUS_RULER;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatterParams;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.shared.settings.enums.AngularConstants;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class RadiusRulerControlLayer extends OsmandMapLayer implements OsmAndCompassListener {

	private static final int TEXT_SIZE = 14;
	private static final float COMPASS_CIRCLE_FITTING_RADIUS_COEF = 1.25f;
	private static final float CIRCLE_ANGLE_STEP = 5;
	private static final int SHOW_COMPASS_MIN_ZOOM = 8;
	private static final long COMPASS_REFRESH_INTERVAL_MS = 100L;
	private static final double CIRCLE_ANGLE_STEP_RADIANS = Math.toRadians(CIRCLE_ANGLE_STEP);
	private static final double MAX_GLOBE_DISTANCE = Math.PI * MapUtils.HAVERSINE_EARTH_RADIUS_METERS;
	private static final double MAX_VISIBLE_GLOBE_DISTANCE = MAX_GLOBE_DISTANCE / 2;
	private static final float PROJECTED_STEP_SLACK = 4;
	private static final float MIN_PROJECTED_STEP_DP = 24;
	private static final double MAX_GLOBE_MERCATOR_ANGLE = 2 * Math.PI - 1e-7;
	private static final long POINT31_FULL_RANGE = 1L << 31;

	private OsmandApplication app;
	private MapWidgetRegistry widgetRegistry;
	private View rightWidgetsPanel;
	private View leftWidgetsPanel;
	private View topWidgetsPanel;
	private View bottomWidgetsPanel;

	private TextAlignment textAlignment;
	private int maxRadiusInDp;
	private float maxRadius;
	private int radius;
	private double roundedDist;

	private QuadPoint cacheCenter;
	private float cacheMapDensity;
	private MetricsConstants cacheMetricSystem;
	private int cacheIntZoom;
	private boolean cacheSphericalMap;
	private float cacheElevationAngle = Float.NaN;
	private LatLon cacheCenterLatLon;
	private ArrayList<String> cacheDistances;
	private LatLon currentCenterLatLon;

	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Paint bitmapPaint;
	private Paint triangleHeadingPaint;
	private Paint triangleNorthPaint;
	private Paint redLinesPaint;
	private Paint blueLinesPaint;

	private RenderingLineAttributes circleAttrs;
	private RenderingLineAttributes circleAttrsAlt;

	private final Path compass = new Path();
	private final Path arrow = new Path();
	private final Path arrowArc = new Path();
	private final Path redCompassLines = new Path();
	private final Path rulerCircle = new Path();

	private final double[] degrees = new double[72];
	public static final String[] CARDINAL_DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private final int[] arcColors = {
			Algorithms.parseColor("#00237BFF"),
			Algorithms.parseColor("#237BFF"),
			Algorithms.parseColor("#00237BFF")
	};

	private float cachedHeading;
	private boolean sphericalMap;
	private long lastCompassRefreshTime;
	@Nullable
	private View compassRefreshView;

	private final Runnable compassRefreshRunnable = () -> {
		compassRefreshView = null;
		if (isCompassRulerVisible()) {
			refreshCompassRuler(SystemClock.uptimeMillis());
		}
	};

	public RadiusRulerControlLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		app = getApplication();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		cacheMetricSystem = app.getSettings().METRIC_SYSTEM.get();
		cacheMapDensity = getMapDensity();
		cacheDistances = new ArrayList<>();
		cacheCenter = new QuadPoint();
		maxRadiusInDp = app.getResources().getDimensionPixelSize(R.dimen.map_ruler_width);

		initCenterIcon(view);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);

		int colorNorthArrow = ContextCompat.getColor(app, R.color.compass_control_active);
		int colorHeadingArrow = ContextCompat.getColor(app, R.color.active_color_primary_light);

		triangleNorthPaint = initPaintWithStyle(Style.FILL, colorNorthArrow);
		triangleHeadingPaint = initPaintWithStyle(Style.FILL, colorHeadingArrow);
		redLinesPaint = initPaintWithStyle(Style.STROKE, colorNorthArrow);
		blueLinesPaint = initPaintWithStyle(Style.STROKE, colorHeadingArrow);

		updatePaints();

		for (int i = 0; i < 72; i++) {
			degrees[i] = Math.toRadians(i * 5);
		}
	}

	private void updatePaints() {
		float circleTextSize = TEXT_SIZE * density;

		circleAttrs = new RenderingLineAttributes("rulerCircle");
		circleAttrs.paint2.setTextSize(circleTextSize);
		circleAttrs.paint3.setTextSize(circleTextSize);

		circleAttrsAlt = new RenderingLineAttributes("rulerCircleAlt");
		circleAttrsAlt.paint2.setTextSize(circleTextSize);
		circleAttrsAlt.paint3.setTextSize(circleTextSize);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		if (view != null) {
			initCenterIcon(view);
		}
		updatePaints();
	}

	private void initCenterIcon(@NonNull OsmandMapTileView view) {
		BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
		NavigationSession session = app.getCarNavigationSession();
		int densityDpi;
		if (session != null) {
			densityDpi = session.getNavigationCarSurface().getDpi();
		} else {
			DisplayMetrics metrics = app.getResources().getDisplayMetrics();
			densityDpi = metrics.densityDpi;
		}
		bitmapOptions.inScreenDensity = densityDpi;
		bitmapOptions.inTargetDensity = densityDpi;
		bitmapOptions.inScaled = true;
		centerIconDay = UiUtilities.decodeResource(view.getResources(), R.drawable.map_ruler_center_day, bitmapOptions);
		centerIconNight = UiUtilities.decodeResource(view.getResources(), R.drawable.map_ruler_center_night, bitmapOptions);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		resetCompassRefreshState();
		getApplication().getLocationProvider().removeCompassListener(this);
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			rightWidgetsPanel = mapActivity.findViewById(R.id.map_right_widgets_panel);
			leftWidgetsPanel = mapActivity.findViewById(R.id.map_left_widgets_panel);
			topWidgetsPanel = mapActivity.findViewById(R.id.top_widgets_panel);
			bottomWidgetsPanel = mapActivity.findViewById(R.id.map_bottom_widgets_panel);
			getApplication().getLocationProvider().addCompassListener(this);
		} else {
			rightWidgetsPanel = null;
			leftWidgetsPanel = null;
			topWidgetsPanel = null;
			bottomWidgetsPanel = null;
		}
	}

	@Override
	public void destroyLayer() {
		resetCompassRefreshState();
		getApplication().getLocationProvider().removeCompassListener(this);
		super.destroyLayer();
	}

	@Override
	public void updateCompassValue(float value) {
		if (!isCompassRulerVisible() || !hasSignificantHeadingChange(value)) {
			return;
		}

		cachedHeading = value;
		long currentTime = SystemClock.uptimeMillis();
		long elapsedTime = currentTime - lastCompassRefreshTime;
		if (lastCompassRefreshTime == 0 || elapsedTime >= COMPASS_REFRESH_INTERVAL_MS) {
			cancelPendingCompassRefresh();
			refreshCompassRuler(currentTime);
		} else if (compassRefreshView == null) {
			View mapView = view.getView();
			if (mapView != null) {
				compassRefreshView = mapView;
				mapView.postDelayed(compassRefreshRunnable, COMPASS_REFRESH_INTERVAL_MS - elapsedTime);
			}
		}
	}

	private void refreshCompassRuler(long refreshTime) {
		lastCompassRefreshTime = refreshTime;
		view.refreshMap();
	}

	private boolean hasSignificantHeadingChange(float heading) {
		return Math.abs(MapUtils.degreesDiff(cachedHeading, heading))
				> MapViewTrackingUtilities.COMPASS_HEADING_THRESHOLD;
	}

	private void cancelPendingCompassRefresh() {
		if (compassRefreshView != null) {
			compassRefreshView.removeCallbacks(compassRefreshRunnable);
		}
		compassRefreshView = null;
	}

	private void resetCompassRefreshState() {
		cancelPendingCompassRefresh();
		lastCompassRefreshTime = 0;
	}

	private Paint initPaintWithStyle(Paint.Style style, int color) {
		Paint paint = new Paint();
		paint.setStyle(style);
		paint.setColor(color);
		paint.setAntiAlias(true);
		return paint;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings drawSettings) {
		OsmandMapTileView mapView = app.getOsmandMap().getMapView();
		AnimateDraggingMapThread animatedThread = mapView.getAnimatedDraggingThread();

		if (isRulerWidgetOn() && !animatedThread.isAnimatingMapZoom()) {
			OsmandApplication app = view.getApplication();
			OsmandSettings settings = app.getSettings();
			sphericalMap = hasMapRenderer() && settings.SPHERICAL_MAP.get();
			currentCenterLatLon = getCenterLatLon(tb);
			circleAttrs.updatePaints(app, drawSettings, tb);
			circleAttrs.paint2.setStyle(Style.FILL);
			circleAttrsAlt.updatePaints(app, drawSettings, tb);
			circleAttrsAlt.paint2.setStyle(Style.FILL);

			QuadPoint center = tb.getCenterPixelPoint();
			canvas.rotate(-tb.getRotate(), center.x, center.y);

			RadiusRulerMode radiusRulerMode = settings.RADIUS_RULER_MODE.get();
			boolean showRadiusRuler = radiusRulerMode == RadiusRulerMode.FIRST || radiusRulerMode == RadiusRulerMode.SECOND;
			boolean showCompass = settings.SHOW_COMPASS_ON_RADIUS_RULER.get() && tb.getZoom() >= SHOW_COMPASS_MIN_ZOOM;

			boolean radiusRulerNightMode = radiusRulerMode == RadiusRulerMode.SECOND;
			drawCenterIcon(canvas, tb, center, drawSettings.isNightMode(), radiusRulerNightMode);

			if (showRadiusRuler) {
				updateData(tb, center);
				if (showCompass) {
					updateHeading();
					resetDrawingPaths();
				}

				RenderingLineAttributes attrs = radiusRulerNightMode ? circleAttrsAlt : circleAttrs;
				int compassCircleIndex = getCompassCircleIndex(tb, center);
				for (int circleIndex = 1; circleIndex <= cacheDistances.size(); circleIndex++) {
					if (showCompass && circleIndex == compassCircleIndex) {
						drawCompassCircle(canvas, tb, compassCircleIndex, center, attrs);
					} else {
						drawRulerCircle(canvas, tb, circleIndex, center, attrs);
					}
				}
			}
			canvas.rotate(tb.getRotate(), center.x, center.y);
		}
	}

	private final List<MapWidgetInfo> rulerWidgets = new ArrayList<>();

	public boolean isRulerWidgetOn() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			ApplicationMode appMode = app.getSettings().getApplicationMode();
			ScreenLayoutMode layoutMode = ScreenLayoutMode.getDefault(activity);

			rulerWidgets.clear();
			widgetRegistry.collectWidgetsInfo(rulerWidgets, appMode, layoutMode, null, RADIUS_RULER, true);

			for (int i = 0; i < rulerWidgets.size(); i++) {
				if (isPanelVisible(rulerWidgets.get(i).getWidgetPanel())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isPanelVisible(WidgetsPanel widgetsPanel) {
		View panel = null;
		switch (widgetsPanel) {
			case TOP -> panel = topWidgetsPanel;
			case BOTTOM -> panel = bottomWidgetsPanel;
			case LEFT -> panel = leftWidgetsPanel;
			case RIGHT -> panel = rightWidgetsPanel;
		}
		return panel == null || panel.getVisibility() == View.VISIBLE;
	}

	private boolean isCompassRulerVisible() {
		return view != null
				&& app.getSettings().RADIUS_RULER_MODE.get() != RadiusRulerMode.EMPTY
				&& app.getSettings().SHOW_COMPASS_ON_RADIUS_RULER.get()
				&& view.getZoom() >= SHOW_COMPASS_MIN_ZOOM
				&& isRulerWidgetOn();
	}

	private int getCompassCircleIndex(RotatedTileBox tb, QuadPoint center) {
		int compassCircleIndex = 2;
		float radiusLength = radius * compassCircleIndex;
		float top = center.y - radiusLength;
		float bottom = center.y + radiusLength;
		float left = center.x - radiusLength;
		float right = center.x + radiusLength;
		int width = tb.getPixWidth();
		int height = tb.getPixHeight();

		if (top < 0) {
			top = 0;
		}
		if (bottom > height) {
			bottom = height;
		}
		if (left < 0) {
			left = 0;
		}
		if (right > width) {
			right = width;
		}
		int horizontal = (int) (bottom - top) / 2;
		int vertical = (int) (right - left) / 2;
		int minFittingRadius = Math.min(horizontal, vertical);
		if (radiusLength > minFittingRadius * COMPASS_CIRCLE_FITTING_RADIUS_COEF) {
			compassCircleIndex = 1;
		}

		return compassCircleIndex;
	}

	private void updateHeading() {
		Float heading = getApplication().getMapViewTrackingUtilities().getHeading();
		if (heading != null && heading != cachedHeading) {
			cachedHeading = heading;
		}
	}

	private void resetDrawingPaths() {
		redCompassLines.reset();
		arrowArc.reset();
		compass.reset();
		arrow.reset();
	}

	private void drawCenterIcon(Canvas canvas, RotatedTileBox tb, QuadPoint center,
	                            boolean nightMode, boolean radiusRulerNightMode) {
		if (nightMode || radiusRulerNightMode) {
			canvas.drawBitmap(centerIconNight, center.x - centerIconNight.getWidth() / 2f + getCachedAACanvasOffset().x,
					center.y - centerIconNight.getHeight() / 2f + getCachedAACanvasOffset().y, bitmapPaint);
		} else {
			canvas.drawBitmap(centerIconDay, center.x - centerIconDay.getWidth() / 2f + getCachedAACanvasOffset().x,
					center.y - centerIconDay.getHeight() / 2f + getCachedAACanvasOffset().y, bitmapPaint);
		}
	}

	private QuadPoint getCachedAACanvasOffset() {
		return getApplication().getOsmandMap().getMapView().getAACanvasOffset();
	}

	private void updateData(RotatedTileBox tb, QuadPoint center) {
		if (tb.getPixHeight() > 0 && tb.getPixWidth() > 0 && maxRadiusInDp > 0
				&& !Double.isNaN(tb.getLatitude()) && !Double.isNaN(tb.getLongitude())) {
			if (cacheCenter.y != center.y || cacheCenter.x != center.x) {
				cacheCenter = center;
				updateCenter(tb, center);
			}

			MetricsConstants currentMetricSystem = app.getSettings().METRIC_SYSTEM.get();
			float mapDensity = getMapDensity();
			MapRendererView mapRenderer = getMapRenderer();
			float elevationAngle = mapRenderer != null ? mapRenderer.getElevationAngle() : view.getElevationAngle();
			boolean updateCache = tb.getZoom() != cacheIntZoom
					|| !currentCenterLatLon.equals(cacheCenterLatLon) || mapDensity != cacheMapDensity
					|| cacheMetricSystem != currentMetricSystem || cacheSphericalMap != sphericalMap
					|| Float.compare(cacheElevationAngle, elevationAngle) != 0;

			if (!tb.isZoomAnimated() && updateCache) {
				cacheMetricSystem = currentMetricSystem;
				cacheIntZoom = tb.getZoom();
				cacheSphericalMap = sphericalMap;
				cacheElevationAngle = elevationAngle;
				cacheCenterLatLon = new LatLon(currentCenterLatLon.getLatitude(), currentCenterLatLon.getLongitude());
				cacheMapDensity = mapDensity;
				updateDistance(tb);
			}
		}
	}

	private void updateCenter(RotatedTileBox tb, QuadPoint center) {
		float topDist = center.y;
		float bottomDist = tb.getPixHeight() - center.y;
		float leftDist = center.x;
		float rightDist = tb.getPixWidth() - center.x;
		float maxVertical = Math.max(topDist, bottomDist);
		float maxHorizontal = Math.max(rightDist, leftDist);

		if (maxVertical >= maxHorizontal) {
			maxRadius = maxVertical;
			textAlignment = TextAlignment.VERTICAL;
		} else {
			maxRadius = maxHorizontal;
			textAlignment = TextAlignment.HORIZONTAL;
		}
		if (radius != 0) {
			updateText();
		}
	}

	private void updateDistance(RotatedTileBox tb) {
		double pixDensity = tb.getPixDensity();
		double referenceDistance = maxRadiusInDp / pixDensity;
		if (sphericalMap) {
			double globeDistance = getGlobeDistanceForPixelRadius(tb, maxRadiusInDp);
			if (isValidGlobeDistance(globeDistance)) {
				referenceDistance = globeDistance;
			}
			if (!Double.isFinite(referenceDistance) || referenceDistance <= 0) {
				referenceDistance = MAX_GLOBE_DISTANCE;
			} else {
				referenceDistance = Math.min(referenceDistance, MAX_GLOBE_DISTANCE);
			}
		}
		roundedDist = OsmAndFormatter.calculateRoundedDist(referenceDistance, app);
		radius = sphericalMap
				? Math.max(1, (int) (maxRadiusInDp * roundedDist / referenceDistance))
				: (int) (pixDensity * roundedDist);
		updateText();
	}

	private double getGlobeDistanceForPixelRadius(@NonNull RotatedTileBox tb, int pixelRadius) {
		// RotatedTileBox density follows Web Mercator, so calibrate it against the active globe projection.
		double distance = pixelRadius / tb.getPixDensity();
		if (!isValidGlobeDistance(distance)) {
			return Double.NaN;
		}
		QuadPoint center = tb.getCenterPixelPoint();
		for (int i = 0; i < 2; i++) {
			double projectedRadius = getGlobePixelRadius(tb, currentCenterLatLon, center, distance);
			if (!Double.isFinite(projectedRadius) || projectedRadius < 1) {
				return Double.NaN;
			}
			double correctedDistance = distance * pixelRadius / projectedRadius;
			if (!isValidGlobeDistance(correctedDistance)) {
				return Double.NaN;
			}
			distance = correctedDistance;
		}
		return distance;
	}

	private double getGlobePixelRadius(@NonNull RotatedTileBox tb, @NonNull LatLon centerLatLon,
	                                   @NonNull QuadPoint center, double distance) {
		if (!isVisibleGlobeDistance(distance)) {
			return Double.NaN;
		}
		double radiusSum = 0;
		int samplesCount = 0;
		for (int bearing = -90; bearing <= 90; bearing += 180) {
			LatLon latLon = calculateDestinationPoint(centerLatLon, distance, bearing, true);
			PointF screenPoint = getRulerPixelFromLatLon(tb, latLon, true);
			if (screenPoint != null) {
				radiusSum += MapUtils.getSqrtDistance(center.x, center.y, screenPoint.x, screenPoint.y);
				samplesCount++;
			}
		}
		return samplesCount > 0 ? radiusSum / samplesCount : Double.NaN;
	}

	private void updateText() {
		cacheDistances.clear();
		double maxCircleRadius = maxRadius;
		int i = 1;
		while ((maxCircleRadius -= radius) > 0) {
			double circleDistance = roundedDist * i++;
			if (sphericalMap && !isVisibleGlobeDistance(circleDistance)) {
				break;
			}
			cacheDistances.add(OsmAndFormatter.getFormattedDistance((float) circleDistance, app,
					OsmAndFormatterParams.NO_TRAILING_ZEROS));
		}
	}

	private void drawRulerCircle(Canvas canvas, RotatedTileBox tb, int circleNumber, QuadPoint center, RenderingLineAttributes attrs) {
		drawCircle(canvas, tb, circleNumber, attrs);

		String text = cacheDistances.get(circleNumber - 1);
		float circleRadius = radius * circleNumber;

		TextPositioning firstTextPositioning = TextPositioning.getFirstTextPositioning(textAlignment);
		TextPositioning secondTextPositioning = TextPositioning.getSecondTextPositioning(textAlignment);

		PointF firstTextPosition = calculateTextPosition(text, firstTextPositioning, circleRadius, tb, attrs);
		PointF secondTextPosition = calculateTextPosition(text, secondTextPositioning, circleRadius, tb, attrs);

		if (firstTextPosition != null) {
			drawTextInPosition(canvas, text, firstTextPosition, attrs);
		}
		if (secondTextPosition != null) {
			drawTextInPosition(canvas, text, secondTextPosition, attrs);
		}
	}

	private void drawCircle(Canvas canvas, RotatedTileBox tb, int circleNumber, RenderingLineAttributes attrs) {
		float circleRadius = radius * circleNumber;
		double distance = getDistanceForPixelRadius(circleRadius, tb);
		QuadPoint canvasOffset = getCachedAACanvasOffset();
		PointF previousPoint = null;
		rulerCircle.reset();
		if (sphericalMap && !isVisibleGlobeDistance(distance)) {
			return;
		}
		for (int a = -180; a <= 180; a += CIRCLE_ANGLE_STEP) {
			LatLon latLon = calculateDestinationPoint(currentCenterLatLon, distance, a, sphericalMap);
			PointF screenPoint = getRulerPixelFromLatLon(tb, latLon);
			if (screenPoint == null) {
				drawCirclePath(canvas, attrs);
				rulerCircle.reset();
				previousPoint = null;
				continue;
			}
			if (previousPoint != null && isProjectionDiscontinuity(previousPoint, screenPoint, circleRadius)) {
				// Do not connect points across a globe projection discontinuity.
				drawCirclePath(canvas, attrs);
				rulerCircle.reset();
			}
			float x = screenPoint.x + canvasOffset.x;
			float y = screenPoint.y + canvasOffset.y;
			if (rulerCircle.isEmpty()) {
				rulerCircle.moveTo(x, y);
			} else {
				rulerCircle.lineTo(x, y);
			}
			previousPoint = screenPoint;
		}
		drawCirclePath(canvas, attrs);
	}

	private void drawCirclePath(@NonNull Canvas canvas, @NonNull RenderingLineAttributes attrs) {
		if (!rulerCircle.isEmpty()) {
			canvas.drawPath(rulerCircle, attrs.shadowPaint);
			canvas.drawPath(rulerCircle, attrs.paint);
		}
	}

	private void drawTextInPosition(@NonNull Canvas canvas, @NonNull String text, @NonNull PointF textPosition,
	                                @NonNull RenderingLineAttributes attrs) {
		if (!Float.isNaN(textPosition.x) && !Float.isNaN(textPosition.y)) {
			canvas.drawText(text, textPosition.x + getCachedAACanvasOffset().x, textPosition.y + getCachedAACanvasOffset().y, attrs.paint3);
			canvas.drawText(text, textPosition.x + getCachedAACanvasOffset().x, textPosition.y + getCachedAACanvasOffset().y, attrs.paint2);
		}
	}

	@Nullable
	private PointF calculateTextPosition(@NonNull String text,
	                                     @NonNull TextPositioning textPositioning,
	                                     float circleRadius,
	                                     @NonNull RotatedTileBox tileBox,
	                                     @NonNull RenderingLineAttributes attrs) {
		float x = tileBox.getCenterPixelX();
		float y = tileBox.getCenterPixelY();

		switch (textPositioning) {
			case TOP:
				y -= circleRadius;
				break;
			case BOTTOM:
				y += circleRadius;
				break;
			case LEFT:
				x -= circleRadius;
				break;
			case RIGHT:
				x += circleRadius;
				break;
		}

		PointF textPosition = screenPointFromPoint(x, y, true, tileBox);

		if (textPosition != null) {
			Rect textBounds = new Rect();
			attrs.paint2.getTextBounds(text, 0, text.length(), textBounds);

			textPosition.x -= textBounds.width() / 2f;
			textPosition.y += textBounds.height() / 2f;
		}

		return textPosition;
	}

	private void drawCompassCircle(Canvas canvas, RotatedTileBox tb, int circleNumber,
	                               QuadPoint center, RenderingLineAttributes attrs) {
		float radiusLength = radius * circleNumber;
		float innerRadiusLength = radiusLength - attrs.paint.getStrokeWidth() / 2;
		QuadPoint centerPixels = tb.getCenterPixelPoint();

		drawCircle(canvas, tb, circleNumber, attrs);
		drawCompassCents(centerPixels, innerRadiusLength, radiusLength, tb, canvas, attrs);
		drawCardinalDirections(canvas, center, radiusLength, tb, attrs);
		drawLightingHeadingArc(radiusLength, cachedHeading, center, tb, canvas, attrs);
		drawTriangleArrowByRadius(radiusLength, 0, center, attrs.shadowPaint, triangleNorthPaint, tb, canvas);
		drawTriangleArrowByRadius(radiusLength, cachedHeading, center, attrs.shadowPaint, triangleHeadingPaint, tb, canvas);
		drawCompassCircleText(canvas, tb, circleNumber, radiusLength, center, attrs);
	}

	private void drawCompassCircleText(Canvas canvas, RotatedTileBox tb, int circleNumber, float radiusLength,
	                                   QuadPoint center, RenderingLineAttributes attrs) {
		String distance = cacheDistances.get(circleNumber - 1);
		String heading = OsmAndFormatter.getFormattedAzimuth(cachedHeading, AngularConstants.DEGREES360) + " " + getCardinalDirectionForDegrees(cachedHeading);

		float offset = (textAlignment == TextAlignment.HORIZONTAL) ? 15 : 20;
		float drawingTextRadius = radiusLength + AndroidUtils.dpToPx(app, offset);

		TextPositioning headingTextPositioning = TextPositioning.getFirstTextPositioning(textAlignment);
		TextPositioning distanceTextPositioning = TextPositioning.getSecondTextPositioning(textAlignment);

		PointF headingTextPosition = calculateTextPosition(heading, headingTextPositioning, drawingTextRadius, tb, attrs);
		PointF distanceTextPosition = calculateTextPosition(distance, distanceTextPositioning, drawingTextRadius, tb, attrs);

		setAttrsPaintsTypeface(attrs, FontCache.getMediumFont());
		if (headingTextPosition != null) {
			drawTextInPosition(canvas, heading, headingTextPosition, attrs);
		}

		setAttrsPaintsTypeface(attrs, null);
		if (distanceTextPosition != null) {
			drawTextInPosition(canvas, distance, distanceTextPosition, attrs);
		}
	}

	private void drawTriangleArrowByRadius(double radius, double angle, QuadPoint center, Paint shadowPaint, Paint colorPaint, RotatedTileBox tb, Canvas canvas) {
		double headOffsesFromRadius = AndroidUtils.dpToPx(app, 9);
		double triangleSideLength = AndroidUtils.dpToPx(app, 12);
		double triangleHeadAngle = 60;
		double zeroAngle = angle - 90 + (hasMapRenderer() ? 0 : tb.getRotate());

		double radians = Math.toRadians(zeroAngle);
		double firstPointX = center.x + Math.cos(radians) * (radius + headOffsesFromRadius);
		double firstPointY = center.y + Math.sin(radians) * (radius + headOffsesFromRadius);
		PointF firstScreenPoint = screenPointFromPoint(firstPointX, firstPointY, false, tb);

		double radians2 = Math.toRadians(zeroAngle + triangleHeadAngle / 2 + 180);
		double secondPointX = firstPointX + Math.cos(radians2) * triangleSideLength;
		double secondPointY = firstPointY + Math.sin(radians2) * triangleSideLength;
		PointF secondScreenPoint = screenPointFromPoint(secondPointX, secondPointY, false, tb);

		double radians3 = Math.toRadians(zeroAngle - triangleHeadAngle / 2 + 180);
		double thirdPointX = firstPointX + Math.cos(radians3) * triangleSideLength;
		double thirdPointY = firstPointY + Math.sin(radians3) * triangleSideLength;
		PointF thirdScreenPoint = screenPointFromPoint(thirdPointX, thirdPointY, false, tb);

		if (firstScreenPoint == null || secondScreenPoint == null || thirdScreenPoint == null) {
			return;
		}

		arrow.reset();
		arrow.moveTo(firstScreenPoint.x + getCachedAACanvasOffset().x, firstScreenPoint.y + getCachedAACanvasOffset().y);
		arrow.lineTo(secondScreenPoint.x + getCachedAACanvasOffset().x, secondScreenPoint.y + getCachedAACanvasOffset().y);
		arrow.lineTo(thirdScreenPoint.x + getCachedAACanvasOffset().x, thirdScreenPoint.y + getCachedAACanvasOffset().y);
		arrow.lineTo(firstScreenPoint.x + getCachedAACanvasOffset().x, firstScreenPoint.y + getCachedAACanvasOffset().y);
		arrow.close();
		canvas.drawPath(arrow, shadowPaint);
		canvas.drawPath(arrow, colorPaint);
	}

	private void drawLightingHeadingArc(double radius, double angle, QuadPoint center, RotatedTileBox tb, Canvas canvas, RenderingLineAttributes attrs) {
		PointF gradientArcStartPoint = getPointFromCenterByRadius(radius, (angle - 30), tb);
		PointF gradientArcEndPoint = getPointFromCenterByRadius(radius, (angle + 30), tb);
		if (gradientArcStartPoint == null || gradientArcEndPoint == null) {
			return;
		}

		LinearGradient shader = new LinearGradient(gradientArcStartPoint.x + getCachedAACanvasOffset().x,
				gradientArcStartPoint.y + getCachedAACanvasOffset().y,
				gradientArcEndPoint.x + getCachedAACanvasOffset().x,
				gradientArcEndPoint.y + getCachedAACanvasOffset().y,
				arcColors, null, Shader.TileMode.CLAMP);
		blueLinesPaint.setShader(shader);
		OsmandMapTileView mapView = getApplication().getOsmandMap().getMapView();
		boolean isCarView = mapView.isCarView();
		blueLinesPaint.setStrokeWidth(attrs.paint.getStrokeWidth() * (isCarView ? mapView.getDensity() : 1));

		arrowArc.reset();
		int startArcAngle = (int) angle - 45;
		int endArcAngle = (int) angle + 45;
		double distance = getDistanceForPixelRadius(radius, tb);
		if (sphericalMap && !isVisibleGlobeDistance(distance)) {
			return;
		}
		QuadPoint canvasOffset = getCachedAACanvasOffset();
		PointF previousPoint = null;
		for (int a = startArcAngle; a <= endArcAngle; a += CIRCLE_ANGLE_STEP) {
			LatLon latLon = calculateDestinationPoint(currentCenterLatLon, distance, a, sphericalMap);
			PointF screenPoint = getRulerPixelFromLatLon(tb, latLon);
			if (screenPoint == null) {
				drawArrowArcPath(canvas);
				arrowArc.reset();
				previousPoint = null;
				continue;
			}
			if (previousPoint != null && isProjectionDiscontinuity(previousPoint, screenPoint, radius)) {
				drawArrowArcPath(canvas);
				arrowArc.reset();
			}
			if (arrowArc.isEmpty()) {
				arrowArc.moveTo(screenPoint.x + canvasOffset.x, screenPoint.y + canvasOffset.y);
			} else {
				arrowArc.lineTo(screenPoint.x + canvasOffset.x, screenPoint.y + canvasOffset.y);
			}
			previousPoint = screenPoint;
		}
		drawArrowArcPath(canvas);
	}

	private void drawArrowArcPath(@NonNull Canvas canvas) {
		if (!arrowArc.isEmpty()) {
			canvas.drawPath(arrowArc, blueLinesPaint);
		}
	}

	private void drawCompassCents(QuadPoint center, float innerRadiusLength, float radiusLength, RotatedTileBox tb, Canvas canvas, RenderingLineAttributes attrs) {
		for (int i = 0; i < degrees.length; i++) {
			double degree = degrees[i] + (hasMapRenderer() ? 0 : tb.getRotate());
			float x = (float) Math.cos(degree);
			float y = -(float) Math.sin(degree);

			float lineStartX = center.x + x * innerRadiusLength;
			float lineStartY = center.y + y * innerRadiusLength;

			float lineLength = getCompassLineHeight(i);

			float lineStopX = center.x + x * (innerRadiusLength - lineLength);
			float lineStopY = center.y + y * (innerRadiusLength - lineLength);

			PointF ordinaryCentStartScreenPoint;
			PointF ordinaryCentStopScreenPoint;

			if (i == 18) {
				float shortLineMargin = AndroidUtils.dpToPx(app, 5.66f);
				float shortLineHeight = AndroidUtils.dpToPx(app, 2.94f);
				float startY = center.y + y * (radiusLength - shortLineMargin);
				float stopY = center.y + y * (radiusLength - shortLineMargin - shortLineHeight);

				ordinaryCentStartScreenPoint = screenPointFromPoint(center.x, startY, false, tb);
				ordinaryCentStopScreenPoint = screenPointFromPoint(center.x, stopY, false, tb);
			} else {
				ordinaryCentStartScreenPoint = screenPointFromPoint(lineStartX, lineStartY, false, tb);
				ordinaryCentStopScreenPoint = screenPointFromPoint(lineStopX, lineStopY, false, tb);
			}

			if (ordinaryCentStartScreenPoint != null && ordinaryCentStopScreenPoint != null) {
				compass.moveTo(ordinaryCentStartScreenPoint.x + getCachedAACanvasOffset().x, ordinaryCentStartScreenPoint.y + getCachedAACanvasOffset().y);
				compass.lineTo(ordinaryCentStopScreenPoint.x + getCachedAACanvasOffset().x, ordinaryCentStopScreenPoint.y + getCachedAACanvasOffset().y);
			}

			if (i % 9 == 0 && i != 18) {
				PointF startScreenPoint = screenPointFromPoint(lineStartX, lineStartY, false, tb);
				PointF stopScreenPoint = screenPointFromPoint(lineStopX, lineStopY, false, tb);
				if (startScreenPoint != null && stopScreenPoint != null) {
					redCompassLines.moveTo(startScreenPoint.x + getCachedAACanvasOffset().x, startScreenPoint.y + getCachedAACanvasOffset().y);
					redCompassLines.lineTo(stopScreenPoint.x + getCachedAACanvasOffset().x, stopScreenPoint.y + getCachedAACanvasOffset().y);
				}
			}
		}
		redLinesPaint.setStrokeWidth(attrs.paint.getStrokeWidth());
		canvas.drawPath(compass, attrs.shadowPaint);
		canvas.drawPath(compass, attrs.paint);
		canvas.drawPath(redCompassLines, redLinesPaint);
	}

	private LatLon getCenterLatLon(@NonNull RotatedTileBox tb) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			QuadPoint centerPixel = tb.getCenterPixelPoint();
			return NativeUtilities.getLatLonFromElevatedPixel(mapRenderer, tb, centerPixel.x, centerPixel.y);
		} else {
			return tb.getCenterLatLon();
		}
	}

	@Nullable
	private PointF screenPointFromPoint(double x, double y, boolean compensateMapRotation, RotatedTileBox tb) {
		QuadPoint circleCenterPoint = tb.getCenterPixelPoint();
		double dX = circleCenterPoint.x - x;
		double dY = circleCenterPoint.y - y;
		double distanceFromCenter = Math.sqrt(dX * dX + dY * dY);
		double angleFromCenter = Math.toDegrees(Math.atan2(dY, dX)) - 90;
		angleFromCenter = compensateMapRotation ? angleFromCenter - tb.getRotate() : angleFromCenter; //??
		return getPointFromCenterByRadius(distanceFromCenter, angleFromCenter, tb);
	}

	@Nullable
	private PointF getPointFromCenterByRadius(double radius, double angle, RotatedTileBox tb) {
		double distance = getDistanceForPixelRadius(radius, tb);
		if (sphericalMap && !isVisibleGlobeDistance(distance)) {
			return null;
		}
		LatLon latLon = calculateDestinationPoint(currentCenterLatLon, distance, angle, sphericalMap);
		return getRulerPixelFromLatLon(tb, latLon);
	}

	private double getDistanceForPixelRadius(double pixelRadius, @NonNull RotatedTileBox tb) {
		return sphericalMap && radius > 0
				? roundedDist * pixelRadius / radius
				: pixelRadius / tb.getPixDensity();
	}

	@Nullable
	private PointF getRulerPixelFromLatLon(@NonNull RotatedTileBox tb, @NonNull LatLon latLon) {
		return getRulerPixelFromLatLon(tb, latLon, true);
	}

	private boolean isProjectionDiscontinuity(@NonNull PointF previousPoint, @NonNull PointF currentPoint,
	                                          double pixelRadius) {
		double expectedStep = 2 * Math.abs(pixelRadius) * Math.sin(CIRCLE_ANGLE_STEP_RADIANS / 2);
		double maxProjectedStep = Math.max(MIN_PROJECTED_STEP_DP * density,
				expectedStep * PROJECTED_STEP_SLACK);
		return MapUtils.getSqrtDistance(previousPoint.x, previousPoint.y, currentPoint.x, currentPoint.y)
				> maxProjectedStep;
	}

	private static boolean isValidGlobeDistance(double distance) {
		return Double.isFinite(distance) && distance > 0 && distance <= MAX_GLOBE_DISTANCE;
	}

	private static boolean isVisibleGlobeDistance(double distance) {
		return isValidGlobeDistance(distance) && distance <= MAX_VISIBLE_GLOBE_DISTANCE;
	}

	@Nullable
	private PointF getRulerPixelFromLatLon(@NonNull RotatedTileBox tb, @NonNull LatLon latLon,
	                                      boolean allowOffscreen) {
		// Flat maps have no drawable surface beyond the Web Mercator latitude boundary.
		double maxLatitude = sphericalMap ? 90 : MapUtils.MAX_LATITUDE;
		if (Math.abs(latLon.getLatitude()) > maxLatitude) {
			return null;
		}
		MapRendererView mapRenderer = getMapRenderer();
		if (sphericalMap && mapRenderer != null) {
			double absoluteLatitude = Math.abs(latLon.getLatitude());
			PointI location31 = absoluteLatitude > MapUtils.MAX_LATITUDE
					? calculateGlobePoint31(latLon)
					: NativeUtilities.getPoint31FromLatLon(latLon);
			PointI screenPoint = new PointI();
			boolean projected = absoluteLatitude > MapUtils.MAX_LATITUDE
					? mapRenderer.getScreenPointFromLocation(location31, screenPoint, allowOffscreen)
					: mapRenderer.getElevatedPointFromLocation(location31, screenPoint, allowOffscreen);
			if (projected) {
				return new PointF(screenPoint.getX(), screenPoint.getY());
			}
			return null;
		}
		return NativeUtilities.getElevatedPixelFromLatLon(mapRenderer, tb, latLon);
	}

	@NonNull
	private static LatLon calculateDestinationPoint(@NonNull LatLon center, double distance, double bearing,
	                                               boolean sphericalMap) {
		return sphericalMap
				? MapUtils.greatCircleDestinationPoint(center.getLatitude(), center.getLongitude(), distance, bearing)
				: MapUtils.rhumbDestinationPoint(center, distance, bearing);
	}

	@NonNull
	private static PointI calculateGlobePoint31(@NonNull LatLon latLon) {
		// The globe renderer accepts signed Point31 y values beyond the Web Mercator tile range.
		// Keep polar-cap samples in that extended range instead of clamping them to +/-85.0511 degrees.
		double latitude = Math.toRadians(latLon.getLatitude());
		double mercatorAngle = Math.log(Math.tan(latitude / 2 + Math.PI / 4));
		mercatorAngle = Math.max(-MAX_GLOBE_MERCATOR_ANGLE,
				Math.min(MAX_GLOBE_MERCATOR_ANGLE, mercatorAngle));
		long y31 = (long) ((1 - mercatorAngle / Math.PI) / 2 * POINT31_FULL_RANGE);
		// Southern polar values intentionally wrap to the signed Point31 representation used by the renderer.
		return new PointI(MapUtils.get31TileNumberX(latLon.getLongitude()), (int) y31);
	}

	private LatLon point31ToLatLon(PointI point31) {
		double lon = MapUtils.get31LongitudeX(point31.getX());
		double lat = MapUtils.get31LatitudeY(point31.getY());
		return new LatLon(lat, lon);
	}

	private float getCompassLineHeight(int index) {
		if (index % 6 == 0) {
			return AndroidUtils.dpToPx(app, 8);
		} else if (index % 9 == 0 || index % 2 != 0) {
			return AndroidUtils.dpToPx(app, 3);
		} else {
			return AndroidUtils.dpToPx(app, 6);
		}
	}

	private void drawCardinalDirections(Canvas canvas, QuadPoint center, float radiusLength, RotatedTileBox tb, RenderingLineAttributes attrs) {
		float margin = 24;
		float textMargin = AndroidUtils.dpToPx(app, margin);
		attrs.paint2.setTextAlign(Paint.Align.CENTER);
		attrs.paint3.setTextAlign(Paint.Align.CENTER);
		setAttrsPaintsTypeface(attrs, FontCache.getMediumFont());

		for (int i = 0; i < degrees.length; i += 9) {
			String cardinalDirection = getCardinalDirection(i);
			if (cardinalDirection != null) {
				double textRadius = radiusLength - textMargin;
				PointF point = getPointFromCenterByRadius(textRadius, (-i * 5 - 90), tb);
				if (point == null) {
					continue;
				}

				float h2 = AndroidUtils.getTextHeight(attrs.paint2);
				float h3 = AndroidUtils.getTextHeight(attrs.paint3);
				canvas.save();
				canvas.drawText(cardinalDirection, point.x + getCachedAACanvasOffset().x, point.y + getCachedAACanvasOffset().y + h3 / 4, attrs.paint3);
				canvas.drawText(cardinalDirection, point.x + getCachedAACanvasOffset().x, point.y + getCachedAACanvasOffset().y + h2 / 4, attrs.paint2);
				canvas.restore();
			}
		}
		attrs.paint2.setTextAlign(Paint.Align.LEFT);
		attrs.paint3.setTextAlign(Paint.Align.LEFT);
		setAttrsPaintsTypeface(attrs, null);
	}

	private void setAttrsPaintsTypeface(RenderingLineAttributes attrs, Typeface typeface) {
		attrs.paint2.setTypeface(typeface);
		attrs.paint3.setTypeface(typeface);
	}

	private String getCardinalDirection(int i) {
		if (i == 0) {
			return CARDINAL_DIRECTIONS[6];
		} else if (i == 9) {
			return CARDINAL_DIRECTIONS[5];
		} else if (i == 18) {
			return CARDINAL_DIRECTIONS[4];
		} else if (i == 27) {
			return CARDINAL_DIRECTIONS[3];
		} else if (i == 36) {
			return CARDINAL_DIRECTIONS[2];
		} else if (i == 45) {
			return CARDINAL_DIRECTIONS[1];
		} else if (i == 54) {
			return CARDINAL_DIRECTIONS[0];
		} else if (i == 63) {
			return CARDINAL_DIRECTIONS[7];
		}
		return null;
	}

	public static String getCardinalDirectionForDegrees(double degrees) {
		return OsmAndFormatter.getCardinalDirectionForDegrees(degrees);
	}

	private enum TextAlignment {
		VERTICAL,
		HORIZONTAL
	}

	private enum TextPositioning {
		TOP,
		BOTTOM,
		LEFT,
		RIGHT;

		@NonNull
		private static TextPositioning getFirstTextPositioning(@NonNull TextAlignment textAlignment) {
			return textAlignment == TextAlignment.VERTICAL ? TOP : LEFT;
		}

		@NonNull
		private static TextPositioning getSecondTextPositioning(@NonNull TextAlignment textAlignment) {
			return textAlignment == TextAlignment.VERTICAL ? BOTTOM : RIGHT;
		}
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	public enum RadiusRulerMode {

		FIRST(R.string.dark_theme, R.drawable.ic_action_ruler_circle_dark),
		SECOND(R.string.light_theme, R.drawable.ic_action_ruler_circle_light),
		EMPTY(R.string.shared_string_hide, R.drawable.ic_action_hide);

		@StringRes
		public final int titleId;
		@DrawableRes
		public final int iconId;

		RadiusRulerMode(@StringRes int titleId, @DrawableRes int iconId) {
			this.titleId = titleId;
			this.iconId = iconId;
		}

		@NonNull
		public RadiusRulerMode next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}
}
