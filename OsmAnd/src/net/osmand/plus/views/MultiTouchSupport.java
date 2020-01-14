package net.osmand.plus.views;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.lang.reflect.Method;


public class MultiTouchSupport {

	private static final Log log = PlatformUtil.getLog(MultiTouchSupport.class);
	
	public static final int ACTION_MASK = 255;
	public static final int ACTION_POINTER_ID_SHIFT = 8;
	public static final int ACTION_POINTER_DOWN     = 5;
	public static final int ACTION_POINTER_UP     = 6;
	private float angleStarted;
	private float angleRelative;

	public interface MultiTouchZoomListener {

    		public void onZoomStarted(PointF centerPoint);

    		public void onZoomingOrRotating(double relativeToStart, float angle);

    		public void onZoomOrRotationEnded(double relativeToStart, float angleRelative);

    		public void onGestureInit(float x1, float y1, float x2, float y2);

			public void onActionPointerUp();

			public void onActionCancel();

			public void onChangingViewAngle(float angle);

			public void onChangeViewAngleStarted();
	}

	private boolean multiTouchAPISupported = false;
	private final MultiTouchZoomListener listener;
	protected final Context ctx;

	protected Method getPointerCount;
	protected Method getX;
	protected Method getY;
	protected Method getPointerId;


	public MultiTouchSupport(Context ctx, MultiTouchZoomListener listener){
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

	private boolean isTiltSupported() {
		return ((OsmandApplication) ctx.getApplicationContext()).getSettings().USE_OPENGL_RENDER.get() && NativeCoreContext.isInit();
	}

	private void initMethods(){
		try {
			getPointerCount = MotionEvent.class.getMethod("getPointerCount"); //$NON-NLS-1$
			getPointerId = MotionEvent.class.getMethod("getPointerId", Integer.TYPE); //$NON-NLS-1$
			getX = MotionEvent.class.getMethod("getX", Integer.TYPE); //$NON-NLS-1$
			getY = MotionEvent.class.getMethod("getY", Integer.TYPE); //$NON-NLS-1$	
			multiTouchAPISupported = true;
		} catch (Exception e) {
			multiTouchAPISupported = false;
			log.info("Multi touch not supported", e); //$NON-NLS-1$
		}
	}

	private boolean inZoomMode = false;
	private boolean inTiltMode = false;
	private double zoomStartedDistance = 100;
	private double zoomRelative = 1;
	private PointF centerPoint = new PointF();
	private PointF firstFingerStart = new PointF();
	private PointF secondFingerStart = new PointF();
	private static final int TILT_X_THRESHOLD_PX = 40;
	private static final int TILT_Y_THRESHOLD_PX = 40;
	private static final int TILT_DY_THRESHOLD_PX = 40;

	public boolean onTouchEvent(MotionEvent event){
		if(!isMultiTouchSupported()){
			return false;
		}
		int actionCode = event.getAction() & ACTION_MASK;
		try {
			if (actionCode == MotionEvent.ACTION_CANCEL) {
				listener.onActionCancel();
			}
			Integer pointCount = (Integer) getPointerCount.invoke(event);
			if(pointCount < 2){
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
			if(x1 != x2 || y1 != y2) {
				angleDefined = true;
				angle = (float) (Math.atan2(y2 - y1, x2 -x1) * 180 / Math.PI);
			}
			if (actionCode == MotionEvent.ACTION_UP || actionCode == MotionEvent.ACTION_POINTER_UP) {
				listener.onActionPointerUp();
			}
			if (actionCode == ACTION_POINTER_DOWN) {
				centerPoint = new PointF((x1 + x2) / 2, (y1 + y2) / 2);
				firstFingerStart = new PointF(x1, y1);
				secondFingerStart = new PointF(x2, y2);
				listener.onGestureInit(x1, y1, x2, y2);
				listener.onZoomStarted(centerPoint);
				zoomStartedDistance = distance;
				angleStarted = angle;
				return true;
			} else if(actionCode == ACTION_POINTER_UP){
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
				} else if (isTiltSupported()) {
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
						angleRelative = 0;
						zoomRelative = 0;
						inZoomMode = true;
					}
				} else {
					inZoomMode = true;
				}
				return true;
			}
		} catch (Exception e) {
			log.debug("Multi touch exception" , e); //$NON-NLS-1$
		}
		return false;
	}

	public PointF getCenterPoint() {
		return centerPoint;
	}
}
