package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.view.View;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.AngularConstants;
import net.osmand.plus.OsmandSettings.RulerMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class RulerControlLayer extends OsmandMapLayer {

	private static final int VERTICAL_OFFSET = 15;
	private static final long DRAW_TIME = 2000;
	private static final long DELAY_BEFORE_DRAW = 500;
	private static final int TEXT_SIZE = 14;
	private static final int DISTANCE_TEXT_SIZE = 16;
	private static final float COMPASS_CIRCLE_FITTING_RADIUS_COEF = 1.25f;

	private final MapActivity mapActivity;
	private OsmandApplication app;
	private OsmandMapTileView view;
	private View rightWidgetsPanel;

	private TextSide textSide;
	private int maxRadiusInDp;
	private float maxRadius;
	private int radius;
	private double roundedDist;
	private boolean showTwoFingersDistance;
	private boolean showDistBetweenFingerAndLocation;
	private boolean touchOutside;
	private int acceptableTouchRadius;

	private QuadPoint cacheCenter;
	private float cacheMapDensity;
	private OsmandSettings.OsmandPreference<Float> mapDensity;
	private OsmandSettings.MetricsConstants cacheMetricSystem;
	private int cacheIntZoom;
	private double cacheTileX;
	private double cacheTileY;
	private long cacheMultiTouchEndTime;
	private ArrayList<String> cacheDistances;
	private LatLon touchPointLatLon;
	private PointF touchPoint;
	private long touchStartTime;
	private long touchEndTime;
	private boolean touched;
	private boolean wasZoom;

	private List<Float> tx = new ArrayList<>();
	private List<Float> ty = new ArrayList<>();
	private Path linePath = new Path();

	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Paint bitmapPaint;
	private Paint triangleHeadingPaint;
	private Paint triangleNorthPaint;
	private Paint redLinesPaint;
	private Paint blueLinesPaint;

	private RenderingLineAttributes lineAttrs;
	private RenderingLineAttributes lineFontAttrs;
	private RenderingLineAttributes circleAttrs;
	private RenderingLineAttributes circleAttrsAlt;

	private Path compass = new Path();
	private Path arrow = new Path();
	private Path arrowArc = new Path();
	private Path redCompassLines = new Path();

	private double[] degrees = new double[72];
	private String[] cardinalDirections = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private int[] arcColors = {Color.parseColor("#00237BFF"), Color.parseColor("#237BFF"), Color.parseColor("#00237BFF")};

	private float cachedHeading = 0;

	private Handler handler;

	public RulerControlLayer(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	public boolean isShowTwoFingersDistance() {
		return showTwoFingersDistance;
	}

	public boolean isShowDistBetweenFingerAndLocation() {
		return showDistBetweenFingerAndLocation;
	}

	public LatLon getTouchPointLatLon() {
		return touchPointLatLon;
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		app = mapActivity.getMyApplication();
		this.view = view;
		mapDensity = app.getSettings().MAP_DENSITY;
		cacheMetricSystem = app.getSettings().METRIC_SYSTEM.get();
		cacheMapDensity = mapDensity.get();
		cacheDistances = new ArrayList<>();
		cacheCenter = new QuadPoint();
		maxRadiusInDp = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_ruler_width);
		rightWidgetsPanel = mapActivity.findViewById(R.id.map_right_widgets_panel);
		touchPoint = new PointF();
		acceptableTouchRadius = mapActivity.getResources().getDimensionPixelSize(R.dimen.acceptable_touch_radius);

		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);

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

		lineAttrs = new RenderingLineAttributes("rulerLine");

		float circleTextSize = TEXT_SIZE * mapActivity.getResources().getDisplayMetrics().density;
		float lineTextSize = DISTANCE_TEXT_SIZE * mapActivity.getResources().getDisplayMetrics().density;

		lineFontAttrs = new RenderingLineAttributes("rulerLineFont");
		lineFontAttrs.paint.setTextSize(lineTextSize);
		lineFontAttrs.paint2.setTextSize(lineTextSize);

		circleAttrs = new RenderingLineAttributes("rulerCircle");
		circleAttrs.paint2.setTextSize(circleTextSize);
		circleAttrs.paint3.setTextSize(circleTextSize);

		circleAttrsAlt = new RenderingLineAttributes("rulerCircleAlt");
		circleAttrsAlt.paint2.setTextSize(circleTextSize);
		circleAttrsAlt.paint3.setTextSize(circleTextSize);

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				view.refreshMap();
			}
		};

		for (int i = 0; i < 72; i++) {
			degrees[i] = Math.toRadians(i * 5);
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
	public boolean isMapGestureAllowed(MapGestureType type) {
		if (rulerModeOn() && type == MapGestureType.TWO_POINTERS_ZOOM_OUT) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		if (rulerModeOn() && !showTwoFingersDistance) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				touched = true;
				touchOutside = false;
				touchPoint.set(event.getX(), event.getY());
				touchPointLatLon = tileBox.getLatLonFromPixel(event.getX(), event.getY());
				touchStartTime = System.currentTimeMillis();
				wasZoom = false;
			} else if (event.getAction() == MotionEvent.ACTION_MOVE && !touchOutside &&
					!(touched && showDistBetweenFingerAndLocation)) {
				double d = Math.sqrt(Math.pow(event.getX() - touchPoint.x, 2) + Math.pow(event.getY() - touchPoint.y, 2));
				if (d > acceptableTouchRadius) {
					touchOutside = true;
				}
			} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
				touched = false;
				touchEndTime = System.currentTimeMillis();
				refreshMapDelayed();
			}
		}
		return false;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tb, DrawSettings settings) {
		if (rulerModeOn()) {
			OsmandApplication app = view.getApplication();
			lineAttrs.updatePaints(app, settings, tb);
			lineFontAttrs.updatePaints(app, settings, tb);
			lineFontAttrs.paint.setStyle(Style.FILL);
			circleAttrs.updatePaints(app, settings, tb);
			circleAttrs.paint2.setStyle(Style.FILL);
			circleAttrsAlt.updatePaints(app, settings, tb);
			circleAttrsAlt.paint2.setStyle(Style.FILL);
			final QuadPoint center = tb.getCenterPixelPoint();
			final RulerMode mode = app.getSettings().RULER_MODE.get();
			boolean showCompass = app.getSettings().SHOW_COMPASS_CONTROL_RULER.get();
			final long currentTime = System.currentTimeMillis();

			if (cacheMultiTouchEndTime != view.getMultiTouchEndTime()) {
				cacheMultiTouchEndTime = view.getMultiTouchEndTime();
				refreshMapDelayed();
			}
			if (touched && view.isMultiTouch()) {
				touched = false;
				touchEndTime = currentTime;
			}
			if (tb.isZoomAnimated()) {
				wasZoom = true;
			}

			showTwoFingersDistance = !tb.isZoomAnimated() &&
					!view.isWasZoomInMultiTouch() &&
					currentTime - view.getMultiTouchStartTime() > DELAY_BEFORE_DRAW &&
					(view.isMultiTouch() || currentTime - cacheMultiTouchEndTime < DRAW_TIME);

			showDistBetweenFingerAndLocation = !wasZoom &&
					!showTwoFingersDistance &&
					!view.isMultiTouch() &&
					!touchOutside &&
					touchStartTime - view.getMultiTouchStartTime() > DELAY_BEFORE_DRAW &&
					currentTime - touchStartTime > DELAY_BEFORE_DRAW &&
					(touched || currentTime - touchEndTime < DRAW_TIME);

			drawCenterIcon(canvas, tb, center, settings.isNightMode(), mode);
			Location currentLoc = app.getLocationProvider().getLastKnownLocation();
			if (showDistBetweenFingerAndLocation && currentLoc != null) {
				drawDistBetweenFingerAndLocation(canvas, tb, currentLoc, settings.isNightMode());
			} else if (showTwoFingersDistance) {
				drawTwoFingersDistance(canvas, tb, view.getFirstTouchPointLatLon(), view.getSecondTouchPointLatLon(), settings.isNightMode());
			}
			if (mode == RulerMode.FIRST || mode == RulerMode.SECOND) {
				updateData(tb, center);
				if (showCompass) {
					updateHeading();
					resetDrawingPaths();
				}
				RenderingLineAttributes attrs = mode == RulerMode.FIRST ? circleAttrs : circleAttrsAlt;
				int compassCircleId = getCompassCircleId(tb, center);
				for (int i = 1; i <= cacheDistances.size(); i++) {
					if (showCompass && i == compassCircleId) {
						drawCompassCircle(canvas, tb, compassCircleId, center, attrs);
					} else {
						drawCircle(canvas, tb, i, center, attrs);
					}
				}
			}
		}
	}

	public boolean rulerModeOn() {
		return mapActivity.getMapLayers().getMapWidgetRegistry().isVisible("ruler") &&
				rightWidgetsPanel.getVisibility() == View.VISIBLE;
	}

	private int getCompassCircleId(RotatedTileBox tileBox, QuadPoint center) {
		int compassCircleId = 2;
		float radiusLength = radius * compassCircleId;
		float top = center.y - radiusLength;
		float bottom = center.y + radiusLength;
		float left = center.x - radiusLength;
		float right = center.x + radiusLength;

		int width = tileBox.getPixWidth();
		int height = tileBox.getPixHeight();

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
			compassCircleId = 1;
		}

		return compassCircleId;
	}

	private void updateHeading() {
		Float heading = mapActivity.getMapViewTrackingUtilities().getHeading();
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

	private void refreshMapDelayed() {
		handler.sendEmptyMessageDelayed(0, DRAW_TIME + 50);
	}

	private void drawTwoFingersDistance(Canvas canvas, RotatedTileBox tb, LatLon firstTouch, LatLon secondTouch, boolean nightMode) {
		float x1 = tb.getPixXFromLatLon(firstTouch.getLatitude(), firstTouch.getLongitude());
		float y1 = tb.getPixYFromLatLon(firstTouch.getLatitude(), firstTouch.getLongitude());
		float x2 = tb.getPixXFromLatLon(secondTouch.getLatitude(), secondTouch.getLongitude());
		float y2 = tb.getPixYFromLatLon(secondTouch.getLatitude(), secondTouch.getLongitude());

		Path path = new Path();
		path.moveTo(x1, y1);
		path.lineTo(x2, y2);

		String text = OsmAndFormatter.getFormattedDistance((float) MapUtils.getDistance(firstTouch, secondTouch), app);

		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		canvas.drawPath(path, lineAttrs.paint);
		drawFingerTouchIcon(canvas, x1, y1, nightMode);
		drawFingerTouchIcon(canvas, x2, y2, nightMode);
		drawTextOnCenterOfPath(canvas, x1, x2, path, text);
		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	private void drawTextOnCenterOfPath(Canvas canvas, float x1, float x2, Path path, String text) {
		PathMeasure pm = new PathMeasure(path, false);
		Rect bounds = new Rect();
		lineFontAttrs.paint.getTextBounds(text, 0, text.length(), bounds);
		float hOffset = pm.getLength() / 2 - bounds.width() / 2;

		if (x1 >= x2) {
			float[] pos = new float[2];
			pm.getPosTan(pm.getLength() / 2, pos, null);
			canvas.rotate(180, pos[0], pos[1]);
			canvas.drawTextOnPath(text, path, hOffset, bounds.height() + VERTICAL_OFFSET, lineFontAttrs.paint2);
			canvas.drawTextOnPath(text, path, hOffset, bounds.height() + VERTICAL_OFFSET, lineFontAttrs.paint);
			canvas.rotate(-180, pos[0], pos[1]);
		} else {
			canvas.drawTextOnPath(text, path, hOffset, -VERTICAL_OFFSET, lineFontAttrs.paint2);
			canvas.drawTextOnPath(text, path, hOffset, -VERTICAL_OFFSET, lineFontAttrs.paint);
		}
	}

	private void drawFingerTouchIcon(Canvas canvas, float x, float y, boolean nightMode) {
		if (nightMode) {
			canvas.drawBitmap(centerIconNight, x - centerIconNight.getWidth() / 2,
					y - centerIconNight.getHeight() / 2, bitmapPaint);
		} else {
			canvas.drawBitmap(centerIconDay, x - centerIconDay.getWidth() / 2,
					y - centerIconDay.getHeight() / 2, bitmapPaint);
		}
	}

	private void drawCenterIcon(Canvas canvas, RotatedTileBox tb, QuadPoint center, boolean nightMode,
	                            RulerMode mode) {
		canvas.rotate(-tb.getRotate(), center.x, center.y);
		if (nightMode || mode == RulerMode.SECOND) {
			canvas.drawBitmap(centerIconNight, center.x - centerIconNight.getWidth() / 2,
					center.y - centerIconNight.getHeight() / 2, bitmapPaint);
		} else {
			canvas.drawBitmap(centerIconDay, center.x - centerIconDay.getWidth() / 2,
					center.y - centerIconDay.getHeight() / 2, bitmapPaint);
		}
		canvas.rotate(tb.getRotate(), center.x, center.y);
	}

	private void drawDistBetweenFingerAndLocation(Canvas canvas, RotatedTileBox tb, Location currLoc, boolean night) {
		float x = tb.getPixXFromLatLon(touchPointLatLon.getLatitude(), touchPointLatLon.getLongitude());
		float y = tb.getPixYFromLatLon(touchPointLatLon.getLatitude(), touchPointLatLon.getLongitude());
		float currX = tb.getPixXFromLatLon(currLoc.getLatitude(), currLoc.getLongitude());
		float currY = tb.getPixYFromLatLon(currLoc.getLatitude(), currLoc.getLongitude());

		linePath.reset();
		tx.clear();
		ty.clear();

		tx.add(x);
		ty.add(y);
		tx.add(currX);
		ty.add(currY);

		calculatePath(tb, tx, ty, linePath);

		float dist = (float) MapUtils.getDistance(touchPointLatLon, currLoc.getLatitude(), currLoc.getLongitude());
		String text = OsmAndFormatter.getFormattedDistance(dist, app);

		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		canvas.drawPath(linePath, lineAttrs.paint);
		drawFingerTouchIcon(canvas, x, y, night);
		drawTextOnCenterOfPath(canvas, x, currX, linePath, text);
		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	private void updateData(RotatedTileBox tb, QuadPoint center) {
		if (tb.getPixHeight() > 0 && tb.getPixWidth() > 0 && maxRadiusInDp > 0
				&& !Double.isNaN(tb.getLatitude()) && !Double.isNaN(tb.getLongitude())) {
			if (cacheCenter.y != center.y || cacheCenter.x != center.x) {
				cacheCenter = center;
				updateCenter(tb, center);
			}

			OsmandSettings.MetricsConstants currentMetricSystem = app.getSettings().METRIC_SYSTEM.get();
			boolean updateCache = tb.getZoom() != cacheIntZoom || Math.abs(tb.getCenterTileX() - cacheTileX) > 1
					|| Math.abs(tb.getCenterTileY() - cacheTileY) > 1 || mapDensity.get() != cacheMapDensity
					|| cacheMetricSystem != currentMetricSystem;

			if (!tb.isZoomAnimated() && updateCache) {
				cacheMetricSystem = currentMetricSystem;
				cacheIntZoom = tb.getZoom();
				cacheTileX = tb.getCenterTileX();
				cacheTileY = tb.getCenterTileY();
				cacheMapDensity = mapDensity.get();
				updateDistance(tb);
			}
		}
	}

	private void updateCenter(RotatedTileBox tb, QuadPoint center) {
		float topDist = center.y;
		float bottomDist = tb.getPixHeight() - center.y;
		float leftDist = center.x;
		float rightDist = tb.getPixWidth() - center.x;
		float maxVertical = topDist >= bottomDist ? topDist : bottomDist;
		float maxHorizontal = rightDist >= leftDist ? rightDist : leftDist;

		if (maxVertical >= maxHorizontal) {
			maxRadius = maxVertical;
			textSide = TextSide.VERTICAL;
		} else {
			maxRadius = maxHorizontal;
			textSide = TextSide.HORIZONTAL;
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
			cacheDistances.add(OsmAndFormatter.getFormattedDistance((float) roundedDist * i++, app, false));
		}
	}

	private void drawCircle(Canvas canvas, RotatedTileBox tb, int circleNumber, QuadPoint center,
	                        RenderingLineAttributes attrs) {
		if (!tb.isZoomAnimated()) {
			float circleRadius = radius * circleNumber;
			String text = cacheDistances.get(circleNumber - 1);
			float[] textCoords = calculateTextCoords(text, text, circleRadius, center, attrs);

			canvas.rotate(-tb.getRotate(), center.x, center.y);
			canvas.drawCircle(center.x, center.y, radius * circleNumber, attrs.shadowPaint);
			canvas.drawCircle(center.x, center.y, radius * circleNumber, attrs.paint);
			drawTextCoords(canvas, text, textCoords, attrs);
			canvas.rotate(tb.getRotate(), center.x, center.y);
		}
	}

	private void drawTextCoords(Canvas canvas, String text, float[] textCoords, RenderingLineAttributes attrs) {
		canvas.drawText(text, textCoords[0], textCoords[1], attrs.paint3);
		canvas.drawText(text, textCoords[0], textCoords[1], attrs.paint2);
		canvas.drawText(text, textCoords[2], textCoords[3], attrs.paint3);
		canvas.drawText(text, textCoords[2], textCoords[3], attrs.paint2);
	}

	private float[] calculateTextCoords(String topOrLeftText, String rightOrBottomText, float drawingTextRadius, QuadPoint center, RenderingLineAttributes attrs) {
		Rect boundsDistance = new Rect();
		Rect boundsHeading;

		if (topOrLeftText.equals(rightOrBottomText)) {
			boundsHeading = boundsDistance;
		} else {
			boundsHeading = new Rect();
			attrs.paint2.getTextBounds(rightOrBottomText, 0, rightOrBottomText.length(), boundsHeading);
		}
		attrs.paint2.getTextBounds(topOrLeftText, 0, topOrLeftText.length(), boundsDistance);

		// coords of left or top text
		float x1 = 0;
		float y1 = 0;
		// coords of right or bottom text
		float x2 = 0;
		float y2 = 0;

		if (textSide == TextSide.VERTICAL) {
			x1 = center.x - boundsHeading.width() / 2f;
			y1 = center.y - drawingTextRadius + boundsHeading.height() / 2f;
			x2 = center.x - boundsDistance.width() / 2f;
			y2 = center.y + drawingTextRadius + boundsDistance.height() / 2f;
		} else if (textSide == TextSide.HORIZONTAL) {
			x1 = center.x - drawingTextRadius - boundsHeading.width() / 2f;
			y1 = center.y + boundsHeading.height() / 2f;
			x2 = center.x + drawingTextRadius - boundsDistance.width() / 2f;
			y2 = center.y + boundsDistance.height() / 2f;
		}
		return new float[]{x1, y1, x2, y2};
	}

	private void drawCompassCircle(Canvas canvas, RotatedTileBox tileBox,int circleNumber, QuadPoint center,
	                               RenderingLineAttributes attrs) {
		if (!tileBox.isZoomAnimated()) {
			float radiusLength = radius * circleNumber;
			float innerRadiusLength = radiusLength - attrs.paint.getStrokeWidth() / 2;

			updateArcShader(radiusLength, center);
			updateCompassPaths(center, innerRadiusLength, radiusLength);
			drawCardinalDirections(canvas, center, radiusLength, tileBox, attrs);

			redLinesPaint.setStrokeWidth(attrs.paint.getStrokeWidth());
			blueLinesPaint.setStrokeWidth(attrs.paint.getStrokeWidth());

			canvas.drawPath(compass, attrs.shadowPaint);
			canvas.drawPath(compass, attrs.paint);
			canvas.drawPath(redCompassLines, redLinesPaint);

			canvas.rotate(cachedHeading, center.x, center.y);
			canvas.drawPath(arrowArc, blueLinesPaint);
			canvas.rotate(-cachedHeading, center.x, center.y);

			canvas.drawPath(arrow, attrs.shadowPaint);
			canvas.drawPath(arrow, triangleNorthPaint);

			canvas.rotate(cachedHeading, center.x, center.y);
			canvas.drawPath(arrow, attrs.shadowPaint);
			canvas.drawPath(arrow, triangleHeadingPaint);
			canvas.rotate(-cachedHeading, center.x, center.y);

			String distance = cacheDistances.get(circleNumber - 1);
			String heading = OsmAndFormatter.getFormattedAzimuth(cachedHeading, AngularConstants.DEGREES360) + " " + getCardinalDirectionForDegrees(cachedHeading);
			float[] textCoords = calculateTextCoords(distance, heading, radiusLength + AndroidUtils.dpToPx(app, 16), center, attrs);
			canvas.rotate(-tileBox.getRotate(), center.x, center.y);

			setAttrsPaintsTypeface(attrs, Typeface.DEFAULT_BOLD);
			canvas.drawText(heading, textCoords[0], textCoords[1], attrs.paint3);
			canvas.drawText(heading, textCoords[0], textCoords[1], attrs.paint2);
			setAttrsPaintsTypeface(attrs, null);

			canvas.drawText(distance, textCoords[2], textCoords[3], attrs.paint3);
			canvas.drawText(distance, textCoords[2], textCoords[3], attrs.paint2);
			canvas.rotate(tileBox.getRotate(), center.x, center.y);
		}
	}

	private void updateCompassPaths(QuadPoint center, float innerRadiusLength, float radiusLength) {
		compass.addCircle(center.x, center.y, radiusLength, Path.Direction.CCW);

		arrowArc.addArc(new RectF(center.x - radiusLength, center.y - radiusLength, center.x + radiusLength, center.y + radiusLength), -45, -90);

		for (int i = 0; i < degrees.length; i++) {
			double degree = degrees[i];
			float x = (float) Math.cos(degree);
			float y = -(float) Math.sin(degree);

			float lineStartX = center.x + x * innerRadiusLength;
			float lineStartY = center.y + y * innerRadiusLength;

			float lineLength = getCompassLineHeight(i);

			float lineStopX = center.x + x * (innerRadiusLength - lineLength);
			float lineStopY = center.y + y * (innerRadiusLength - lineLength);

			if (i == 18) {
				float shortLineMargin = AndroidUtils.dpToPx(app, 5.66f);
				float shortLineHeight = AndroidUtils.dpToPx(app, 2.94f);
				float startY = center.y + y * (radiusLength - shortLineMargin);
				float stopY = center.y + y * (radiusLength - shortLineMargin - shortLineHeight);

				compass.moveTo(center.x, startY);
				compass.lineTo(center.x, stopY);

				float firstPointY = center.y + y * (radiusLength + AndroidUtils.dpToPx(app, 5));

				float secondPointX = center.x - AndroidUtils.dpToPx(app, 4);
				float secondPointY = center.y + y * (radiusLength - AndroidUtils.dpToPx(app, 2));

				float thirdPointX = center.x + AndroidUtils.dpToPx(app, 4);
				float thirdPointY = center.y + y * (radiusLength - AndroidUtils.dpToPx(app, 2));

				arrow.moveTo(center.x, firstPointY);
				arrow.lineTo(secondPointX, secondPointY);
				arrow.lineTo(thirdPointX, thirdPointY);
				arrow.lineTo(center.x, firstPointY);
				arrow.close();
			} else {
				compass.moveTo(lineStartX, lineStartY);
				compass.lineTo(lineStopX, lineStopY);
			}
			if (i % 9 == 0 && i != 18) {
				redCompassLines.moveTo(lineStartX, lineStartY);
				redCompassLines.lineTo(lineStopX, lineStopY);
			}
		}
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

	private void drawCardinalDirections(Canvas canvas, QuadPoint center, float radiusLength, RotatedTileBox tileBox, RenderingLineAttributes attrs) {
		float textMargin = AndroidUtils.dpToPx(app, 14);
		attrs.paint2.setTextAlign(Paint.Align.CENTER);
		attrs.paint3.setTextAlign(Paint.Align.CENTER);
		setAttrsPaintsTypeface(attrs, Typeface.DEFAULT_BOLD);

		for (int i = 0; i < degrees.length; i += 9) {
			String cardinalDirection = getCardinalDirection(i);
			if (cardinalDirection != null) {
				float textWidth = AndroidUtils.getTextWidth(attrs.paint2.getTextSize(), cardinalDirection);

				canvas.save();
				canvas.translate(center.x, center.y);
				canvas.rotate(-i * 5 - 90);
				canvas.translate(0, radiusLength - textMargin - textWidth / 2);
				canvas.rotate(i * 5 - tileBox.getRotate() + 90);

				canvas.drawText(cardinalDirection, 0, 0, attrs.paint3);
				canvas.drawText(cardinalDirection, 0, 0, attrs.paint2);
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

	private void updateArcShader(float radiusLength, QuadPoint center) {
		float arcLength = (float) (2 * Math.PI * radiusLength * (90f / 360));
		LinearGradient shader = new LinearGradient((float) (center.x - arcLength * 0.25), center.y, (float) (center.x + arcLength * 0.25), center.y, arcColors, null, Shader.TileMode.CLAMP);
		blueLinesPaint.setShader(shader);
	}

	private String getCardinalDirection(int i) {
		if (i == 0) {
			return cardinalDirections[2];
		} else if (i == 9) {
			return cardinalDirections[1];
		} else if (i == 18) {
			return cardinalDirections[0];
		} else if (i == 27) {
			return cardinalDirections[7];
		} else if (i == 36) {
			return cardinalDirections[6];
		} else if (i == 45) {
			return cardinalDirections[5];
		} else if (i == 54) {
			return cardinalDirections[4];
		} else if (i == 63) {
			return cardinalDirections[3];
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

	private enum TextSide {
		VERTICAL,
		HORIZONTAL
	}

	@Override
	public void destroyLayer() {

	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}
}