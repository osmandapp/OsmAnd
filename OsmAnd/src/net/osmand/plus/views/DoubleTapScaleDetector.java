package net.osmand.plus.views;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

public class DoubleTapScaleDetector {
	private static final Log LOG = PlatformUtil.getLog(DoubleTapScaleDetector.class);
	private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
	private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
	private static final int DOUBLE_TAP_MIN_TIME = 40;
	public static final int SCALE_PER_SCREEN = 4;

	private final DoubleTapZoomListener listener;
	protected final Context ctx;

	private int displayHeightPx;

	private boolean isDoubleTapping = false;
	private float scale;
	private MotionEvent firstDown;
	private MotionEvent firstUp;
	private MotionEvent secondDown;
	private int mDoubleTapSlopSquare;

	public DoubleTapScaleDetector(Activity ctx, DoubleTapZoomListener listener) {
		this.ctx = ctx;
		this.listener = listener;
		Display defaultDisplay = ctx.getWindowManager().getDefaultDisplay();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			Point size = new Point();
			defaultDisplay.getSize(size);
			displayHeightPx = size.y;
		} else {
			displayHeightPx = defaultDisplay.getHeight();
		}
		final ViewConfiguration configuration = ViewConfiguration.get(ctx);
		int doubleTapSlop = configuration.getScaledTouchSlop();
		mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() != 1) {
			return false;
		}
		long currentTime = System.currentTimeMillis();
		if (event.getAction() == MotionEvent.ACTION_UP) {
			secondDown = null;
			if (isDoubleTapping) {
				isDoubleTapping = false;
				listener.onZoomEnded(scale);
				return true;
			} else {
				firstUp = MotionEvent.obtain(event);
			}
		} else {
			if (event.getAction() == MotionEvent.ACTION_DOWN && !isDoubleTapping) {
				if (isConsideredDoubleTap(firstDown, firstUp, event)) {
					secondDown = MotionEvent.obtain(event);
					float x = event.getX();
					float y = event.getY();
					listener.onGestureInit(x, y, x, y);
					listener.onZoomStarted(new PointF(x, y));
					return true;
				} else {
					firstDown = MotionEvent.obtain(event);
				}
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				if (isConfirmedScale(secondDown, event)) {
					isDoubleTapping = true;
				}
				if (isDoubleTapping) {
					float delta = convertPxToDp((int) (firstDown.getY() - event.getY()));
					float scaleDelta = delta / (displayHeightPx / SCALE_PER_SCREEN);
					scale = 1 - scaleDelta;
					listener.onZooming(scale);
					return true;
				}
			}
		}
		return false;
	}

	public boolean isInZoomMode() {
		return isDoubleTapping;
	}

	private int convertPxToDp(int px) {
		return Math.round(px / (Resources.getSystem().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
	}

	private final boolean isConsideredDoubleTap(MotionEvent firstDown,
										  MotionEvent firstUp,
										  MotionEvent secondDown) {
		if (firstDown == null || firstUp == null || secondDown == null) {
			return false;
		}
		final long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
		if (deltaTime > DOUBLE_TAP_TIMEOUT || deltaTime < DOUBLE_TAP_MIN_TIME) {
			return false;
		}

		int deltaXDown = (int) firstDown.getX() - (int) secondDown.getX();
		int deltaYDown = (int) firstDown.getY() - (int) secondDown.getY();
		int squaredDown = deltaXDown * deltaXDown + deltaYDown * deltaYDown;

		int deltaXUp = (int) firstUp.getX() - (int) secondDown.getX();
		int deltaYUp = (int) firstUp.getY() - (int) secondDown.getY();
		int squaredUp = deltaXUp * deltaXUp + deltaYUp * deltaYUp;

		return squaredDown < mDoubleTapSlopSquare && squaredUp < mDoubleTapSlopSquare;
	}

	private static final boolean isConfirmedScale(MotionEvent secondDown,
										   MotionEvent moveEvent) {
		if (secondDown == null || moveEvent == null) {
			return false;
		}
		return moveEvent.getEventTime() - secondDown.getEventTime() > TAP_TIMEOUT - 50;
	}

	public float getCenterX() {
		return firstUp.getX();
	}

	public float getCenterY() {
		return firstUp.getY();
	}

	public interface DoubleTapZoomListener {
		public void onZoomStarted(PointF centerPoint);

		public void onZooming(double relativeToStart);

		public void onZoomEnded(double relativeToStart);

		public void onGestureInit(float x1, float y1, float x2, float y2);
	}
}
