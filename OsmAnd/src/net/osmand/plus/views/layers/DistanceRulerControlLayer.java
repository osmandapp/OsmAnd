package net.osmand.plus.views.layers;

import android.content.Context;
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

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.enums.DistanceByTapTextSize;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.geometry.GeometryWay;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class DistanceRulerControlLayer extends OsmandMapLayer {

	private static final int VERTICAL_OFFSET = 15;
	private static final long DRAW_TIME = 4000;
	private static final long DELAY_BEFORE_DRAW = 200;
	private static final int DISTANCE_TEXT_SIZE = 16;

	private OsmandApplication app;

	private boolean showTwoFingersDistance;
	private boolean showDistBetweenFingerAndLocation;
	private boolean touchOutside;
	private int acceptableTouchRadius;

	private long cacheMultiTouchEndTime;
	private LatLon touchPointLatLon;
	private PointF touchPoint;
	private long touchStartTime;
	private long touchEndTime;
	private boolean touched;
	private boolean wasZoom;

	private final List<Float> tx = new ArrayList<>();
	private final List<Float> ty = new ArrayList<>();
	private final Path linePath = new Path();

	private Bitmap centerIconDay;
	private Bitmap centerIconNight;
	private Paint bitmapPaint;

	private RenderingLineAttributes lineAttrs;
	private RenderingLineAttributes lineFontAttrs;

	private Handler handler;

	private StateChangedListener<DistanceByTapTextSize> textSizeListener;

	public DistanceRulerControlLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		app = getApplication();
		touchPoint = new PointF();
		acceptableTouchRadius = app.getResources().getDimensionPixelSize(R.dimen.acceptable_touch_radius);

		centerIconDay = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_day);
		centerIconNight = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_ruler_center_night);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);

		lineAttrs = new RenderingLineAttributes("rulerLine");
		lineFontAttrs = new RenderingLineAttributes("rulerLineFont");

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				view.refreshMap();
			}
		};
		addTextSizeListener();
		updateTextSize();
	}

	@Override
	public boolean isMapGestureAllowed(MapGestureType type) {
		return !rulerModeOn() || type != MapGestureType.TWO_POINTERS_ZOOM_OUT;
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event, @NonNull RotatedTileBox tileBox) {
		if (rulerModeOn() && !showTwoFingersDistance) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				touched = true;
				touchOutside = false;
				touchPoint.set(event.getX(), event.getY());
				touchPointLatLon = NativeUtilities.getLatLonFromPixel(getMapRenderer(), tileBox,
						event.getX(), event.getY());
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
			long currentTime = System.currentTimeMillis();

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

			Location currentLoc = app.getLocationProvider().getLastKnownLocation();
			if (showDistBetweenFingerAndLocation && currentLoc != null) {
				drawDistBetweenFingerAndLocation(canvas, tb, currentLoc, settings.isNightMode());
			} else if (showTwoFingersDistance) {
				drawTwoFingersDistance(canvas, tb, view.getFirstTouchPointLatLon(),
						view.getSecondTouchPointLatLon(), settings.isNightMode());
			}
		}
	}

	public boolean rulerModeOn() {
		return app.getSettings().SHOW_DISTANCE_RULER.get();
	}

	private void refreshMapDelayed() {
		handler.sendEmptyMessageDelayed(0, DRAW_TIME + 50);
	}

	private void drawTwoFingersDistance(Canvas canvas, RotatedTileBox tb, LatLon firstTouch,
	                                    LatLon secondTouch, boolean nightMode) {
		PointF firstScreenPoint = NativeUtilities.getPixelFromLatLon(getMapRenderer(), tb, firstTouch.getLatitude(), firstTouch.getLongitude());
		PointF secondScreenPoint = NativeUtilities.getPixelFromLatLon(getMapRenderer(), tb, secondTouch.getLatitude(), secondTouch.getLongitude());
		float x1 = firstScreenPoint.x;
		float y1 = firstScreenPoint.y;
		float x2 = secondScreenPoint.x;
		float y2 = secondScreenPoint.y;

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
		float hOffset = pm.getLength() / 2 - bounds.width() / 2f;

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
			canvas.drawBitmap(centerIconNight, x - centerIconNight.getWidth() / 2f,
					y - centerIconNight.getHeight() / 2f, bitmapPaint);

		} else {
			canvas.drawBitmap(centerIconDay, x - centerIconDay.getWidth() / 2f,
					y - centerIconDay.getHeight() / 2f, bitmapPaint);
		}
	}

	private void drawDistBetweenFingerAndLocation(Canvas canvas, RotatedTileBox tb, Location currLoc, boolean night) {
		PointF firstScreenPoint = NativeUtilities.getPixelFromLatLon(getMapRenderer(), tb, touchPointLatLon.getLatitude(), touchPointLatLon.getLongitude());
		PointF secondScreenPoint = NativeUtilities.getPixelFromLatLon(getMapRenderer(), tb, currLoc.getLatitude(), currLoc.getLongitude());
		float x = firstScreenPoint.x;
		float y = firstScreenPoint.y;
		float currX = secondScreenPoint.x;
		float currY = secondScreenPoint.y;

		linePath.reset();
		tx.clear();
		ty.clear();

		tx.add(x);
		ty.add(y);
		tx.add(currX);
		ty.add(currY);

		GeometryWay.calculatePath(tb, tx, ty, linePath);

		float dist = (float) MapUtils.getDistance(touchPointLatLon, currLoc.getLatitude(), currLoc.getLongitude());
		String text = OsmAndFormatter.getFormattedDistance(dist, app);

		canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		canvas.drawPath(linePath, lineAttrs.paint);
		drawFingerTouchIcon(canvas, x, y, night);
		drawTextOnCenterOfPath(canvas, x, currX, linePath, text);
		canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	private void addTextSizeListener() {
		textSizeListener = change -> updateTextSize();
		app.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.addListener(textSizeListener);
	}

	private void updateTextSize() {
		DistanceByTapTextSize textSize = app.getSettings().DISTANCE_BY_TAP_TEXT_SIZE.get();
		float lineTextSize = app.getResources().getDimension(textSize.getTextSizeId());

		lineFontAttrs.paint.setTextSize(lineTextSize);
		lineFontAttrs.paint2.setTextSize(lineTextSize);
	}
}
