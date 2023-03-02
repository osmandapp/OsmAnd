package net.osmand.plus.views;

import android.graphics.PointF;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.lang.reflect.Method;


public class MultiTouchSupport {

	private static final Log log = PlatformUtil.getLog(MultiTouchSupport.class);

	public static final int ACTION_MASK = 255;
	public static final int ACTION_POINTER_ID_SHIFT = 8;
	private float angleStarted;
	private float angleRelative;

	public interface MultiTouchZoomListener {

		void onZoomStarted(PointF centerPoint);

		void onZoomingOrRotating(double relativeToStart, float angle);

		void onZoomOrRotationEnded(double relativeToStart, float angleRelative);

		void onGestureInit(float x1, float y1, float x2, float y2);

		void onActionPointerUp();

		void onActionCancel();

		void onChangingViewAngle(float angle);

		void onChangeViewAngleStarted();
	}

	private final OsmandApplication app;
	private final MultiTouchZoomListener listener;

	private Method getPointerCount;
	private Method getX;
	private Method getY;
	private boolean multiTouchAPISupported;
	private MODE startedMode = MODE.NONE;


	public MultiTouchSupport(@NonNull OsmandApplication app, @NonNull MultiTouchZoomListener listener) {
		this.app = app;
		this.listener = listener;
		initMethods();
	}

	public boolean isMultiTouchSupported() {
		return multiTouchAPISupported;
	}

	public boolean isInZoomMode() {
		return inZoomMode;
	}

	public boolean isInTiltMode() {
		return inTiltMode;
	}

	private void initMethods() {
		try {
			getPointerCount = MotionEvent.class.getMethod("getPointerCount");
			getX = MotionEvent.class.getMethod("getX", Integer.TYPE);
			getY = MotionEvent.class.getMethod("getY", Integer.TYPE);
			multiTouchAPISupported = true;
		} catch (Exception e) {
			multiTouchAPISupported = false;
			log.info("Multi touch not supported", e);
		}
	}

	private boolean inZoomMode;
	private boolean inTiltMode;
	private double zoomStartedDistance = 100;
	private double zoomRelative = 1;
	private PointF centerPoint = new PointF();
	private PointF firstFingerStart = new PointF();
	private PointF secondFingerStart = new PointF();
	private static final int TILT_X_THRESHOLD_PX = 40;
	private static final int TILT_Y_THRESHOLD_PX = 40;
	private static final int TILT_DY_THRESHOLD_PX = 40;

	public boolean onTouchEvent(MotionEvent event) {
		if (!isMultiTouchSupported()) {
			return false;
		}
		int actionCode = event.getAction() & ACTION_MASK;
		try {
			if (actionCode == MotionEvent.ACTION_CANCEL) {
				startedMode = MODE.NONE;
				listener.onActionCancel();
			}
			Integer pointCount = (Integer) getPointerCount.invoke(event);
			if (pointCount < 2) {
				if (inZoomMode) {
					listener.onZoomOrRotationEnded(zoomRelative, angleRelative);
					inZoomMode = false;
					return true;
				} else if (inTiltMode) {
					inTiltMode = false;
					return true;
				}
				return false;
			}
			Float x1 = (Float) getX.invoke(event, 0);
			Float x2 = (Float) getX.invoke(event, 1);
			Float y1 = (Float) getY.invoke(event, 0);
			Float y2 = (Float) getY.invoke(event, 1);
			float distance = (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
			float angle = 0;
			boolean angleDefined = false;
			if (x1 != x2 || y1 != y2) {
				angleDefined = true;
				angle = (float) (Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI);
			}
			if (actionCode == MotionEvent.ACTION_UP || actionCode == MotionEvent.ACTION_POINTER_UP) {
				listener.onActionPointerUp();
			}
			if (actionCode == MotionEvent.ACTION_UP) {
				startedMode = MODE.NONE;
			}
			if (actionCode == MotionEvent.ACTION_POINTER_DOWN) {
				centerPoint = new PointF((x1 + x2) / 2, (y1 + y2) / 2);
				firstFingerStart = new PointF(x1, y1);
				secondFingerStart = new PointF(x2, y2);
				listener.onGestureInit(x1, y1, x2, y2);
				listener.onZoomStarted(centerPoint);
				zoomStartedDistance = distance;
				angleStarted = angle;
				return true;
			} else if (actionCode == MotionEvent.ACTION_POINTER_UP) {
				if (inZoomMode) {
					listener.onZoomOrRotationEnded(zoomRelative, angleRelative);
					inZoomMode = false;
				} else if (inTiltMode) {
					inTiltMode = false;
				}
				return true;
			} else if (actionCode == MotionEvent.ACTION_MOVE) {
				if (inZoomMode) {

					// Keep zoom center fixed or flexible
					centerPoint = new PointF((x1 + x2) / 2, (y1 + y2) / 2);

					if (angleDefined) {
						angleRelative = MapUtils.unifyRotationTo360(angle - angleStarted);
					}
					zoomRelative = distance / zoomStartedDistance;
					listener.onZoomingOrRotating(zoomRelative, angleRelative);
				} else if (inTiltMode) {
					float dy2 = secondFingerStart.y - y2;
					float viewAngle = dy2 / 8f;
					listener.onChangingViewAngle(viewAngle);
				} else if (isTiltSupportEnabled(app)) {
					float dx1 = Math.abs(firstFingerStart.x - x1);
					float dx2 = Math.abs(secondFingerStart.x - x2);
					float dy1 = Math.abs(firstFingerStart.y - y1);
					float dy2 = Math.abs(secondFingerStart.y - y2);
					float startDy = Math.abs(secondFingerStart.y - firstFingerStart.y);
					if (dx1 < TILT_X_THRESHOLD_PX && dx2 < TILT_X_THRESHOLD_PX
							&& dy1 > TILT_Y_THRESHOLD_PX && dy2 > TILT_Y_THRESHOLD_PX
							&& startDy < TILT_Y_THRESHOLD_PX * 6
							&& Math.abs(dy2 - dy1) < TILT_DY_THRESHOLD_PX
							&& (startedMode == MODE.NONE || startedMode == MODE.TILT)) {
						listener.onChangeViewAngleStarted();
						startedMode = MODE.TILT;
						inTiltMode = true;
					} else if (dx1 > TILT_X_THRESHOLD_PX || dx2 > TILT_X_THRESHOLD_PX
							|| Math.abs(dy1 - dy2) > TILT_DY_THRESHOLD_PX
							&& (startedMode == MODE.NONE || startedMode == MODE.ZOOM)) {
						angleRelative = 0;
						zoomRelative = 0;
						startedMode = MODE.ZOOM;
						inZoomMode = true;
					}
				} else {
					startedMode = MODE.ZOOM;
					inZoomMode = true;
				}
				return true;
			}
		} catch (Exception e) {
			log.debug("Multi touch exception", e);
		}
		return false;
	}

	public PointF getCenterPoint() {
		return centerPoint;
	}

	public static boolean isTiltSupportEnabled(@NonNull OsmandApplication app) {
		return isTiltSupported(app) && app.getSettings().ENABLE_3D_VIEW.get();
	}

	public static boolean isTiltSupported(@NonNull OsmandApplication app) {
		return app.useOpenGlRenderer();
	}

	private enum MODE {
		NONE, ZOOM, TILT
	}

}
