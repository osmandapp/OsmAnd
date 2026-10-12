package net.osmand.plus.views.layers;

import static net.osmand.plus.views.mapwidgets.WidgetType.RADIUS_RULER;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
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
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.AngularConstants;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class RadiusRulerControlLayer extends OsmandMapLayer {

	private static final int TEXT_SIZE = 14;
	private static final float COMPASS_CIRCLE_FITTING_RADIUS_COEF = 1.25f;
	private static final float CIRCLE_ANGLE_STEP = 5;
	private static final int SHOW_COMPASS_MIN_ZOOM = 8;

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
	private LatLon cacheCenterLatLon;
	private ArrayList<String> cacheDistances;

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

	private final double[] degrees = new double[72];
	private final String[] cardinalDirections = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private final int[] arcColors = {
			Algorithms.parseColor("#00237BFF"),
			Algorithms.parseColor("#237BFF"),
			Algorithms.parseColor("#00237BFF")
	};

	private float cachedHeading;
	private boolean isCarViewMap = false;

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
		float circleTextSize = TEXT_SIZE * app.getResources().getDisplayMetrics().density;

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
		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day, bitmapOptions);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night, bitmapOptions);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity mapActivity) {
		super.setMapActivity(mapActivity);
		if (mapActivity != null) {
			rightWidgetsPanel = mapActivity.findViewById(R.id.map_right_widgets_panel);
			leftWidgetsPanel = mapActivity.findViewById(R.id.map_left_widgets_panel);
			topWidgetsPanel = mapActivity.findViewById(R.id.top_widgets_panel);
			bottomWidgetsPanel = mapActivity.findViewById(R.id.map_bottom_widgets_panel);
		} else {
			rightWidgetsPanel = null;
			leftWidgetsPanel = null;
			topWidgetsPanel = null;
			bottomWidgetsPanel = null;
		}
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

	public boolean isRulerWidgetOn() {
		boolean isWidgetVisible = false;
		List<MapWidgetInfo> widgets = widgetRegistry.getWidgetInfoForType(RADIUS_RULER);
		for (MapWidgetInfo widget : widgets) {
			isWidgetVisible = isWidgetVisible(widget) && isPanelVisible(widget.getWidgetPanel());
			if (isWidgetVisible) break;
		}
		return isWidgetVisible;
	}

	private boolean isWidgetVisible(@NonNull MapWidgetInfo widgetInfo) {
		return widgetRegistry.isWidgetVisible(widgetInfo);
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
			canvas.drawBitmap(centerIconNight, center.x - centerIconNight.getWidth() / 2f,
					center.y - centerIconNight.getHeight() / 2f, bitmapPaint);
		} else {
			canvas.drawBitmap(centerIconDay, center.x - centerIconDay.getWidth() / 2f,
					center.y - centerIconDay.getHeight() / 2f, bitmapPaint);
		}
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
			boolean updateCache = tb.getZoom() != cacheIntZoom
					|| !tb.getCenterLatLon().equals(cacheCenterLatLon) || mapDensity != cacheMapDensity
					|| cacheMetricSystem != currentMetricSystem;

			if (!tb.isZoomAnimated() && updateCache) {
				cacheMetricSystem = currentMetricSystem;
				cacheIntZoom = tb.getZoom();
				LatLon centerLatLon = tb.getCenterLatLon();
				cacheCenterLatLon = new LatLon(centerLatLon.getLatitude(), centerLatLon.getLongitude());
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
		roundedDist = OsmAndFormatter.calculateRoundedDist(maxRadiusInDp / pixDensity, app);
		radius = (int) (pixDensity * roundedDist);
		updateText();
	}

	private void updateText() {
		cacheDistances.clear();
		double maxCircleRadius = maxRadius;
		int i = 1;
		while ((maxCircleRadius -= radius) > 0) {
			cacheDistances.add(OsmAndFormatter.getFormattedDistance((float) roundedDist * i++, app,
					OsmAndFormatter.OsmAndFormatterParams.NO_TRAILING_ZEROS));
		}
	}

	private void drawRulerCircle(Canvas canvas, RotatedTileBox tb, int circleNumber, QuadPoint center, RenderingLineAttributes attrs) {
		drawCircle(canvas, tb, circleNumber, center, attrs);

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

	private void drawCircle(Canvas canvas, RotatedTileBox tb, int circleNumber, QuadPoint center,
	                        RenderingLineAttributes attrs) {
		float circleRadius = radius * circleNumber;
		List<List<QuadPoint>> arrays = new ArrayList<>();
		List<QuadPoint> points = new ArrayList<>();
		LatLon centerLatLon = getCenterLatLon(tb);
		for (int a = -180; a <= 180; a += CIRCLE_ANGLE_STEP) {
			LatLon latLon = MapUtils.rhumbDestinationPoint(centerLatLon, circleRadius / tb.getPixDensity(), a);
			if (Math.abs(latLon.getLatitude()) > 90) {
				if (points.size() > 0) {
					arrays.add(points);
					points = new ArrayList<>();
				}
				continue;
			}

			PointF screenPoint = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, latLon);
			points.add(new QuadPoint(screenPoint.x, screenPoint.y));
		}
		if (points.size() > 0) {
			arrays.add(points);
		}

		for (List<QuadPoint> pts : arrays) {
			Path path = new Path();
			for (QuadPoint pt : pts) {
				if (path.isEmpty()) {
					path.moveTo(pt.x, pt.y);
				} else {
					path.lineTo(pt.x, pt.y);
				}
			}
			canvas.drawPath(path, attrs.shadowPaint);
			canvas.drawPath(path, attrs.paint);
		}
	}

	private void drawTextInPosition(@NonNull Canvas canvas, @NonNull String text, @NonNull PointF textPosition,
	                                @NonNull RenderingLineAttributes attrs) {
		if (!Float.isNaN(textPosition.x) && !Float.isNaN(textPosition.y)) {
			canvas.drawText(text, textPosition.x, textPosition.y, attrs.paint3);
			canvas.drawText(text, textPosition.x, textPosition.y, attrs.paint2);
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

		drawCircle(canvas, tb, circleNumber, center, attrs);
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
		arrow.moveTo(firstScreenPoint.x, firstScreenPoint.y);
		arrow.lineTo(secondScreenPoint.x, secondScreenPoint.y);
		arrow.lineTo(thirdScreenPoint.x, thirdScreenPoint.y);
		arrow.lineTo(firstScreenPoint.x, firstScreenPoint.y);
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

		LinearGradient shader = new LinearGradient(gradientArcStartPoint.x, gradientArcStartPoint.y, gradientArcEndPoint.x, gradientArcEndPoint.y, arcColors, null, Shader.TileMode.CLAMP);
		blueLinesPaint.setShader(shader);
		blueLinesPaint.setStrokeWidth(attrs.paint.getStrokeWidth());

		arrowArc.reset();
		int startArcAngle = (int) angle - 45;
		int endArcAngle = (int) angle + 45;
		LatLon centerLatLon = getCenterLatLon(tb);
		for (int a = startArcAngle; a <= endArcAngle; a += CIRCLE_ANGLE_STEP) {
			LatLon latLon = MapUtils.rhumbDestinationPoint(centerLatLon, radius / tb.getPixDensity(), a);
			PointF screenPoint = NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, latLon);
			if (arrowArc.isEmpty()) {
				arrowArc.moveTo(screenPoint.x, screenPoint.y);
			} else {
				arrowArc.lineTo(screenPoint.x, screenPoint.y);
			}
		}
		canvas.drawPath(arrowArc, blueLinesPaint);
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
				compass.moveTo(ordinaryCentStartScreenPoint.x, ordinaryCentStartScreenPoint.y);
				compass.lineTo(ordinaryCentStopScreenPoint.x, ordinaryCentStopScreenPoint.y);
			}

			if (i % 9 == 0 && i != 18) {
				PointF startScreenPoint = screenPointFromPoint(lineStartX, lineStartY, false, tb);
				PointF stopScreenPoint = screenPointFromPoint(lineStopX, lineStopY, false, tb);
				if (startScreenPoint != null && stopScreenPoint != null) {
					redCompassLines.moveTo(startScreenPoint.x, startScreenPoint.y);
					redCompassLines.lineTo(stopScreenPoint.x, stopScreenPoint.y);
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
		LatLon centerLatLon = getCenterLatLon(tb);
		LatLon latLon = MapUtils.rhumbDestinationPoint(centerLatLon, radius / tb.getPixDensity(), angle);
		return Math.abs(latLon.getLatitude()) > 90
				? null
				: NativeUtilities.getElevatedPixelFromLatLon(getMapRenderer(), tb, latLon);
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
				canvas.drawText(cardinalDirection, point.x, point.y + h3 / 4, attrs.paint3);
				canvas.drawText(cardinalDirection, point.x, point.y + h2 / 4, attrs.paint2);
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
			return cardinalDirections[6];
		} else if (i == 9) {
			return cardinalDirections[5];
		} else if (i == 18) {
			return cardinalDirections[4];
		} else if (i == 27) {
			return cardinalDirections[3];
		} else if (i == 36) {
			return cardinalDirections[2];
		} else if (i == 45) {
			return cardinalDirections[1];
		} else if (i == 54) {
			return cardinalDirections[0];
		} else if (i == 63) {
			return cardinalDirections[7];
		}
		return null;
	}

	private String getCardinalDirectionForDegrees(double degrees) {
		while (degrees < 0) {
			degrees += 360;
		}
		int index = (int) Math.floor(((degrees + 22.5) % 360) / 45);
		if (index >= 0 && cardinalDirections.length > index) {
			return cardinalDirections[index];
		} else {
			return "";
		}
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
