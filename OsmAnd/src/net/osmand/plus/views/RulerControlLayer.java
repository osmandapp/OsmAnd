package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
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
import net.osmand.plus.OsmandSettings.RulerMode;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

import java.util.ArrayList;

import gnu.trove.list.array.TIntArrayList;

public class RulerControlLayer extends OsmandMapLayer {

	private static final long DRAW_TIME = 2000;
	private static final long DELAY_BEFORE_DRAW = 500;
	private static final int TEXT_SIZE = 14;

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
	private int cacheIntZoom;
	private double cacheTileX;
	private double cacheTileY;
	private long cacheMultiTouchEndTime;
	private ArrayList<String> cacheDistances;
	private Path distancePath;
	private TIntArrayList tx;
	private TIntArrayList ty;
	private LatLon touchPointLatLon;
	private PointF touchPoint;
	private long touchStartTime;
	private long touchEndTime;
	private boolean touched;
	private boolean wasZoom;

	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Paint bitmapPaint;
	private RenderingLineAttributes lineAttrs;
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
		cacheDistances = new ArrayList<>();
		cacheCenter = new QuadPoint();
		maxRadiusInDp = mapActivity.getResources().getDimensionPixelSize(R.dimen.map_ruler_width);
		rightWidgetsPanel = mapActivity.findViewById(R.id.map_right_widgets_panel);
		distancePath = new Path();
		tx = new TIntArrayList();
		ty = new TIntArrayList();
		touchPoint = new PointF();
		acceptableTouchRadius = mapActivity.getResources().getDimensionPixelSize(R.dimen.acceptable_touch_radius);

		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);

		lineAttrs = new RenderingLineAttributes("rulerLine");

		float textSize = TEXT_SIZE * mapActivity.getResources().getDisplayMetrics().density;

		circleAttrs = new RenderingLineAttributes("rulerCircle");
		circleAttrs.paint2.setTextSize(textSize);
		circleAttrs.paint3.setTextSize(textSize);

		circleAttrsAlt = new RenderingLineAttributes("rulerCircleAlt");
		circleAttrsAlt.paint2.setTextSize(textSize);
		circleAttrsAlt.paint3.setTextSize(textSize);

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
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
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
			lineAttrs.updatePaints(view, settings, tb);
			circleAttrs.updatePaints(view, settings, tb);
			circleAttrs.paint2.setStyle(Style.FILL);
			circleAttrsAlt.updatePaints(view, settings, tb);
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
				float x = tb.getPixXFromLonNoRot(touchPointLatLon.getLongitude());
				float y = tb.getPixYFromLatNoRot(touchPointLatLon.getLatitude());
				drawDistBetweenFingerAndLocation(canvas, tb, x, y, currentLoc, settings.isNightMode());
			} else if (showTwoFingersDistance) {
				LatLon firstTouchPoint = view.getFirstTouchPointLatLon();
				LatLon secondTouchPoint = view.getSecondTouchPointLatLon();
				float x1 = tb.getPixXFromLonNoRot(firstTouchPoint.getLongitude());
				float y1 = tb.getPixYFromLatNoRot(firstTouchPoint.getLatitude());
				float x2 = tb.getPixXFromLonNoRot(secondTouchPoint.getLongitude());
				float y2 = tb.getPixYFromLatNoRot(secondTouchPoint.getLatitude());
				drawTwoFingersDistance(canvas, x1, y1, x2, y2, settings.isNightMode());
			}
			if (mode == RulerMode.FIRST || mode == RulerMode.SECOND) {
				updateData(tb, center);
				RenderingLineAttributes attrs;
				if (mode == RulerMode.FIRST) {
					attrs = circleAttrs;
				} else {
					attrs = circleAttrsAlt;
				}
				for (int i = 1; i <= cacheDistances.size(); i++) {
					drawCircle(canvas, tb, i, center, attrs);
				}
			}
		}
	}

	private boolean rulerModeOn() {
		return mapActivity.getMapLayers().getMapWidgetRegistry().isVisible("ruler") &&
				rightWidgetsPanel.getVisibility() == View.VISIBLE;
	}

	private void refreshMapDelayed() {
		handler.sendEmptyMessageDelayed(0, DRAW_TIME + 50);
	}

	private void drawTwoFingersDistance(Canvas canvas, float x1, float y1, float x2, float y2, boolean nightMode) {
		canvas.drawLine(x1, y1, x2, y2, lineAttrs.paint);
		drawFingerTouchIcon(canvas, x1, y1, nightMode);
		drawFingerTouchIcon(canvas, x2, y2, nightMode);
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

	private void drawDistBetweenFingerAndLocation(Canvas canvas, RotatedTileBox tb, float x, float y,
												  Location currentLoc, boolean nightMode) {
		int currX = tb.getPixXFromLonNoRot(currentLoc.getLongitude());
		int currY = tb.getPixYFromLatNoRot(currentLoc.getLatitude());
		distancePath.reset();
		tx.clear();
		ty.clear();

		tx.add(currX);
		ty.add(currY);
		tx.add((int) x);
		ty.add((int) y);

		calculatePath(tb, tx, ty, distancePath);
		canvas.drawPath(distancePath, lineAttrs.paint);
//		canvas.drawLine(currX, currY, x, y, lineAttrs.paint);
		drawFingerTouchIcon(canvas, x, y, nightMode);
	}

	private void updateData(RotatedTileBox tb, QuadPoint center) {
		if (tb.getPixHeight() > 0 && tb.getPixWidth() > 0 && maxRadiusInDp > 0) {
			if (cacheCenter.y != center.y || cacheCenter.x != center.x) {
				cacheCenter = center;
				updateCenter(tb, center);
			}

			boolean move = tb.getZoom() != cacheIntZoom || Math.abs(tb.getCenterTileX() - cacheTileX) > 1 ||
					Math.abs(tb.getCenterTileY() - cacheTileY) > 1;

			if (!tb.isZoomAnimated() && move) {
				cacheIntZoom = tb.getZoom();
				cacheTileX = tb.getCenterTileX();
				cacheTileY = tb.getCenterTileY();
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
		final double dist = tb.getDistance(0, tb.getPixHeight() / 2, tb.getPixWidth(), tb.getPixHeight() / 2);
		double pixDensity = tb.getPixWidth() / dist;
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
