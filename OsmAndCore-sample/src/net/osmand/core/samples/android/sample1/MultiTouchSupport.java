package net.osmand.core.samples.android.sample1;

import android.content.Context;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;

import net.osmand.util.MapUtils;

import java.lang.reflect.Method;

public class MultiTouchSupport {

	private static final String TAG = "MultiTouchSupport";

	public static final int ACTION_MASK = 255;
	protected final Context ctx;
	private final MultiTouchZoomListener listener;
	protected Method getPointerCount;
	protected Method getX;
	protected Method getY;
	protected Method getPointerId;

	private float initialAngle;
	private float rotation;
	private static final float ROTATION_THRESHOLD_DEG = 15.0f;
	private boolean isRotating;

	private boolean multiTouchAPISupported = false;

	private boolean inTiltMode = false;
	private PointF firstFingerStart = new PointF();
	private PointF secondFingerStart = new PointF();
	private static final int TILT_X_THRESHOLD_PX = 40;
	private static final int TILT_Y_THRESHOLD_PX = 40;
	private static final int TILT_DY_THRESHOLD_PX = 40;

	private boolean inZoomMode = false;
	private float initialDistance = 100;
	private float scale = 1;
	private PointF centerPoint = new PointF();

	private boolean multiTouch = false;

	public MultiTouchSupport(Context ctx, MultiTouchZoomListener listener) {
		this.ctx = ctx;
		this.listener = listener;
		initMethods();
	}

	public boolean isMultiTouchSupported(){
		return multiTouchAPISupported;
	}

	public boolean isInZoomMode(){
		return inZoomMode;
	}

	public boolean isInTiltMode() {
		return inTiltMode;
	}

	public boolean isInMultiTouch() {
		return multiTouch;
	}

	private void initMethods() {
		try {
			getPointerCount = MotionEvent.class.getMethod("getPointerCount"); //$NON-NLS-1$
			getPointerId = MotionEvent.class.getMethod("getPointerId", Integer.TYPE); //$NON-NLS-1$
			getX = MotionEvent.class.getMethod("getX", Integer.TYPE); //$NON-NLS-1$
			getY = MotionEvent.class.getMethod("getY", Integer.TYPE); //$NON-NLS-1$
			multiTouchAPISupported = true;
		} catch (Exception e) {
			multiTouchAPISupported = false;
		}
	}

	public boolean onTouchEvent(MotionEvent event){
		if(!isMultiTouchSupported()){
			return false;
		}
		int actionCode = event.getAction() & ACTION_MASK;
		try {
			if (actionCode == MotionEvent.ACTION_UP || actionCode == MotionEvent.ACTION_CANCEL) {
				multiTouch = false;
			}
			Integer pointCount = (Integer) getPointerCount.invoke(event);
			if (pointCount < 2) {
				if (inZoomMode || inTiltMode) {
					listener.onGestureFinished(scale, rotation);
					inZoomMode = false;
					inTiltMode = false;
				}
				return multiTouch;
			} else {
				multiTouch = true;
			}

			Float x1 = (Float) getX.invoke(event, 0);
			Float x2 = (Float) getX.invoke(event, 1);
			Float y1 = (Float) getY.invoke(event, 0);
			Float y2 = (Float) getY.invoke(event, 1);
			float distance = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
			float angle = 0;
			boolean angleDefined = false;
			if (x1.floatValue() != x2.floatValue() || y1.floatValue() != y2.floatValue()) {
				angleDefined = true;
				angle = (float) (Math.atan2(y2 - y1, x2 -x1) * 180 / Math.PI);
			}

			switch (actionCode) {

				case MotionEvent.ACTION_POINTER_DOWN: {

					centerPoint = new PointF((x1 + x2) / 2, (y1 + y2) / 2);
					firstFingerStart = new PointF(x1, y1);
					secondFingerStart = new PointF(x2, y2);
					listener.onGestureInit(x1, y1, x2, y2);
					return true;
				}
				case MotionEvent.ACTION_POINTER_UP: {

					if (inZoomMode || inTiltMode) {
						listener.onGestureFinished(scale, rotation);
						inZoomMode = false;
						inTiltMode = false;
					}
					return true;
				}
				case MotionEvent.ACTION_MOVE: {

					if (inZoomMode) {
						// Keep zoom center fixed or flexible
						centerPoint = new PointF((x1 + x2) / 2, (y1 + y2) / 2);
						if (angleDefined) {
							float a = MapUtils.unifyRotationTo360(angle - initialAngle);
							if (!isRotating && Math.abs(a) > ROTATION_THRESHOLD_DEG) {
								isRotating = true;
								initialAngle = angle;
							} else if (isRotating) {
								rotation = a;
							}
						}
						scale = distance / initialDistance;

						listener.onZoomingOrRotating(scale, rotation);
						return true;

					} else if (inTiltMode) {
						float dy2 = secondFingerStart.y - y2;
						float viewAngle = dy2 / 8f;
						listener.onChangingViewAngle(viewAngle);

					} else {
						float dx1 = Math.abs(firstFingerStart.x - x1);
						float dx2 = Math.abs(secondFingerStart.x - x2);
						float dy1 = Math.abs(firstFingerStart.y - y1);
						float dy2 = Math.abs(secondFingerStart.y - y2);
						float startDy = Math.abs(secondFingerStart.y - firstFingerStart.y);
						if (dx1 < TILT_X_THRESHOLD_PX && dx2 < TILT_X_THRESHOLD_PX
								&& dy1 > TILT_Y_THRESHOLD_PX && dy2 > TILT_Y_THRESHOLD_PX
								&& startDy < TILT_Y_THRESHOLD_PX * 6
								&& Math.abs(dy2 - dy1) < TILT_DY_THRESHOLD_PX) {
							listener.onChangeViewAngleStarted();
							inTiltMode = true;

						} else if (dx1 > TILT_X_THRESHOLD_PX || dx2 > TILT_X_THRESHOLD_PX
								|| Math.abs(dy2 - dy1) > TILT_DY_THRESHOLD_PX
								|| Math.abs(dy1 - dy2) > TILT_DY_THRESHOLD_PX) {
							listener.onZoomStarted(centerPoint);
							initialDistance = distance;
							initialAngle = angle;
							rotation = 0;
							scale = 0;
							isRotating = false;
							inZoomMode = true;
						}
					}
				}
				default:
					break;
			}

		} catch (Exception e) {
			Log.e(TAG, "Multi touch exception" , e); //$NON-NLS-1$
		}
		return true;
	}

	public PointF getCenterPoint() {
		return centerPoint;
	}

	public interface MultiTouchZoomListener {

		void onZoomStarted(PointF centerPoint);

		void onZoomingOrRotating(float scale, float rotation);

		void onChangeViewAngleStarted();

		void onChangingViewAngle(float angle);

		void onGestureFinished(float scale, float rotation);

		void onGestureInit(float x1, float y1, float x2, float y2);

	}
}
