package com.osmand.views;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;

import android.content.Context;
import android.util.FloatMath;
import android.view.MotionEvent;

import com.osmand.LogUtil;

public class MultiTouchSupport {

	private static final Log log = LogUtil.getLog(MultiTouchSupport.class);
	
    public static final int ACTION_MASK = 255;
    public static final int ACTION_POINTER_ID_SHIFT = 8;
    public static final int ACTION_POINTER_DOWN     = 5;
    public static final int ACTION_POINTER_UP     = 6;

    public interface MultiTouchZoomListener {
    	
    	public void onZoomStarted(float distance);
    	
    	public void onZooming(float distance, float relativeToStart);
    	
    	public void onZoomEnded(float distance, float relativeToStart); 
    	
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
    private float zoomStartedDistance = 100;
    private float previousZoom = 1;
    
    public boolean onTouchEvent(MotionEvent event){
    	if(!isMultiTouchSupported()){
    		return false;
    	}
    	int actionCode = event.getAction() & ACTION_MASK;
    	try {
			Integer pointCount = (Integer) getPointerCount.invoke(event);
			if(pointCount < 2){
				if(inZoomMode){
					listener.onZoomEnded(zoomStartedDistance * previousZoom, previousZoom);
				}
				return false;
			}
			Float x1 = (Float) getX.invoke(event, 0);
			Float x2 = (Float) getX.invoke(event, 1);
			Float y1 = (Float) getY.invoke(event, 0);
			Float y2 = (Float) getY.invoke(event, 1);
			float distance = FloatMath.sqrt((x2 - x1)*(x2 -x1) + (y2-y1)*(y2-y1));
			previousZoom = distance / zoomStartedDistance;
			if (actionCode == ACTION_POINTER_DOWN) {
				listener.onZoomStarted(distance);
				zoomStartedDistance = distance;
				inZoomMode = true;
				return true;
			} else if(actionCode == ACTION_POINTER_UP){
				if(inZoomMode){
					listener.onZoomEnded(distance, previousZoom);
					inZoomMode = false;
				}
				return true;
			} else if(inZoomMode && actionCode == MotionEvent.ACTION_MOVE){
				listener.onZooming(distance, previousZoom);
				return true;
			}
    	} catch (Exception e) {
    		log.debug("Multi touch exception" , e); //$NON-NLS-1$
		}
    	return false;
    }

}
