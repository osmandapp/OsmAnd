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
	private static final int LONG_PRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
	public static final int SCALE_PER_SCREEN = 4;

	private final DoubleTapZoomListener listener;
	protected final Context ctx;

	private int displayHeightPx;
	private PointF centerScreen;

	private boolean mIsInZoomMode = false;
	private float scale;
	private MotionEvent firstDown;
	private MotionEvent firstUp;
	private MotionEvent secondDown;
	private int mTouchSlopSquare;
	private int mDoubleTapSlopSquare;
	private boolean mIsDoubleTapping;
	private boolean mScrolling;

	public DoubleTapScaleDetector(Activity ctx, DoubleTapZoomListener listener) {
		this.ctx = ctx;
		this.listener = listener;
		Display defaultDisplay = ctx.getWindowManager().getDefaultDisplay();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			Point size = new Point();
			defaultDisplay.getSize(size);
			displayHeightPx = size.y;
			centerScreen = new PointF(size.x / 2, size.y / 2);
		} else {
			displayHeightPx = defaultDisplay.getHeight();
			centerScreen = new PointF(defaultDisplay.getWidth() / 2, defaultDisplay.getHeight() / 2);
		}
		final ViewConfiguration configuration = ViewConfiguration.get(ctx);
		int touchSlop = configuration.getScaledTouchSlop();
		mTouchSlopSquare = touchSlop * touchSlop;
		int doubleTapSlop = (int) (configuration.getScaledDoubleTapSlop() * 0.5);
		mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() != 1) {
			resetEvents();
			mIsDoubleTapping = false;
			mScrolling = false;
			return false;
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			boolean handled = false;
			if (mIsInZoomMode) {
				mIsInZoomMode = false;
				listener.onZoomEnded(scale);
				handled = true;
			} else if (secondDown != null) {
				if (calculateSqaredDistance(secondDown, event) < mDoubleTapSlopSquare
					&& event.getEventTime() - secondDown.getEventTime() < LONG_PRESS_TIMEOUT) {
					listener.onDoubleTap(event);
				}
				handled = true;
			} else if (!mScrolling) {
				firstUp = MotionEvent.obtain(event);
			} else {
				resetEvents();
			}

			if (handled) {
				resetEvents();
			}
			mIsDoubleTapping = false;
			mScrolling = false;
			return handled;
		} else {
			if (event.getAction() == MotionEvent.ACTION_DOWN && !mIsInZoomMode) {
				if (isConsideredDoubleTap(firstDown, firstUp, event)) {
					mIsDoubleTapping = true;
					secondDown = MotionEvent.obtain(event);
					float x = event.getX();
					float y = event.getY();
					listener.onGestureInit(x, y, x, y);
					listener.onZoomStarted(centerScreen);
					return true;
				} else {
					firstDown = MotionEvent.obtain(event);
				}
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				if (!mScrolling && secondDown == null && firstDown != null) {
					mScrolling = calculateSqaredDistance(firstDown, event) > mTouchSlopSquare;
				}
				if (isConfirmedScale(secondDown, event)) {
					mIsInZoomMode = true;
				}
				if (mIsInZoomMode) {
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

	private void resetEvents() {
		firstUp = null;
		firstDown = null;
		secondDown = null;
	}

	public boolean isInZoomMode() {
		return mIsInZoomMode;
	}

	public boolean isDoubleTapping() {
		return mIsDoubleTapping;
	}

	private int convertPxToDp(int px) {
		return Math.round(px / (Resources.getSystem().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
	}

	private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp,
										  MotionEvent secondDown) {
		if (firstDown == null || firstUp == null || secondDown == null) {
			return false;
		}
		if (secondDown.getEventTime() - firstUp.getEventTime() > DOUBLE_TAP_TIMEOUT) {
			return false;
		}
		return calculateSqaredDistance(firstDown, secondDown) < mDoubleTapSlopSquare;
	}

	private int calculateSqaredDistance(MotionEvent first,
										   MotionEvent second) {
		int deltaXDown = (int) first.getX() - (int) second.getX();
		int deltaYDown = (int) first.getY() - (int) second.getY();
		return deltaXDown * deltaXDown + deltaYDown * deltaYDown;
	}

	private boolean isConfirmedScale(MotionEvent secondDown, MotionEvent moveEvent) {
		if (secondDown == null || moveEvent == null) {
			return false;
		}
		return calculateSqaredDistance(secondDown, moveEvent) > mDoubleTapSlopSquare;
	}

	public float getCenterX() {
		return firstUp.getX();
	}

	public float getCenterY() {
		return firstUp.getY();
	}

	public interface DoubleTapZoomListener {
		void onZoomStarted(PointF centerPoint);

		void onZooming(double relativeToStart);

		void onZoomEnded(double relativeToStart);

		void onGestureInit(float x1, float y1, float x2, float y2);

		boolean onDoubleTap(MotionEvent e);
	}
}
