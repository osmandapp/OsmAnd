package net.osmand.plus.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import net.osmand.PlatformUtil;

import org.apache.commons.logging.Log;

public class DoubleTapScaleDetector {
	private static final Log LOG = PlatformUtil.getLog(DoubleTapScaleDetector.class);
	private static final int DOUBLE_TAPPING_DELTA = ViewConfiguration.getTapTimeout() + 100;
	private static final int DP_PER_1X = 200;

	private final DoubleTapZoomListener listener;
	protected final Context ctx;

	private long startTime = 0;
	private boolean isDoubleTapping = false;
	private float startX;
	private float startY;
	private float scale;

	public DoubleTapScaleDetector(Context ctx, DoubleTapZoomListener listener) {
		this.ctx = ctx;
		this.listener = listener;
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() != 1) {
			return false;
		}
		long currentTime = System.currentTimeMillis();
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (isDoubleTapping) {
				isDoubleTapping = false;
				listener.onZoomEnded(scale, 0);
				return true;
			} else {
				startTime = currentTime;
				return true;
			}
		} else if (event.getAction() == MotionEvent.ACTION_DOWN && !isDoubleTapping
				&& currentTime - startTime < DOUBLE_TAPPING_DELTA) {
			isDoubleTapping = true;
			startX = event.getX();
			startY = event.getY();
			listener.onGestureInit(startX, startY, startX, startY);
			listener.onZoomStarted(new PointF(startX, startY));
			return true;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (isDoubleTapping) {
				float delta = convertPxToDp((int) (startY - event.getY()));
				float scaleDelta = delta / DP_PER_1X;
				scale = 1 - scaleDelta;
				listener.onZoomingOrRotating(scale, 0);
				return true;
			} else {
				return false;
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

	public interface DoubleTapZoomListener {
		public void onZoomStarted(PointF centerPoint);

		public void onZoomingOrRotating(double relativeToStart, float angle);

		public void onZoomEnded(double relativeToStart, float angleRelative);

		public void onGestureInit(float x1, float y1, float x2, float y2);
	}
}
