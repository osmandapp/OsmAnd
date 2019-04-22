package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
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
	private RenderingLineAttributes lineAttrs;
	private RenderingLineAttributes lineFontAttrs;
	private RenderingLineAttributes circleAttrs;
	private RenderingLineAttributes circleAttrsAlt;

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
		mapDensity = mapActivity.getMyApplication().getSettings().MAP_DENSITY;
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
				RenderingLineAttributes attrs = mode == RulerMode.FIRST ? circleAttrs : circleAttrsAlt;
				for (int i = 1; i <= cacheDistances.size(); i++) {
					drawCircle(canvas, tb, i, center, attrs);
				}
			}
		}
	}

	public boolean rulerModeOn() {
		return mapActivity.getMapLayers().getMapWidgetRegistry().isVisible("ruler") &&
				rightWidgetsPanel.getVisibility() == View.VISIBLE;
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

			boolean move = tb.getZoom() != cacheIntZoom || Math.abs(tb.getCenterTileX() - cacheTileX) > 1 ||
					Math.abs(tb.getCenterTileY() - cacheTileY) > 1 || mapDensity.get() != cacheMapDensity;

			if (!tb.isZoomAnimated() && move) {
				cacheIntZoom = tb.getZoom();
				cacheTileX = tb.getCenterTileX();
				cacheTileY = tb.getCenterTileY();
				cacheMapDensity = mapDensity.get();
				cacheDistances.clear();
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
		double maxCircleRadius = maxRadius;
		int i = 1;
		while ((maxCircleRadius -= radius) > 0) {
			cacheDistances.add(OsmAndFormatter
					.getFormattedDistance((float) roundedDist * i++, app, false).replaceAll(" ", ""));
		}
	}

	private void drawCircle(Canvas canvas, RotatedTileBox tb, int circleNumber, QuadPoint center,
							RenderingLineAttributes attrs) {
		if (!tb.isZoomAnimated()) {
			Rect bounds = new Rect();
			String text = cacheDistances.get(circleNumber - 1);
			attrs.paint2.getTextBounds(text, 0, text.length(), bounds);

			// coords of left or top text
			float x1 = 0;
			float y1 = 0;
			// coords of right or bottom text
			float x2 = 0;
			float y2 = 0;

			if (textSide == TextSide.VERTICAL) {
				x1 = center.x - bounds.width() / 2;
				y1 = center.y - radius * circleNumber + bounds.height() / 2;
				x2 = center.x - bounds.width() / 2;
				y2 = center.y + radius * circleNumber + bounds.height() / 2;
			} else if (textSide == TextSide.HORIZONTAL) {
				x1 = center.x - radius * circleNumber - bounds.width() / 2;
				y1 = center.y + bounds.height() / 2;
				x2 = center.x + radius * circleNumber - bounds.width() / 2;
				y2 = center.y + bounds.height() / 2;
			}

			canvas.rotate(-tb.getRotate(), center.x, center.y);
			canvas.drawCircle(center.x, center.y, radius * circleNumber, attrs.shadowPaint);
			canvas.drawCircle(center.x, center.y, radius * circleNumber, attrs.paint);
			canvas.drawText(text, x1, y1, attrs.paint3);
			canvas.drawText(text, x1, y1, attrs.paint2);
			canvas.drawText(text, x2, y2, attrs.paint3);
			canvas.drawText(text, x2, y2, attrs.paint2);
			canvas.rotate(tb.getRotate(), center.x, center.y);
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
