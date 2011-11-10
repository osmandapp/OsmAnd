package net.osmand.access;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.MotionEvent;
import android.view.View;

// Provide some additional accessibility means for individual view elements.
// These elements will be spoken on touch. Thus, you can slide your finger
// across the screen and hear available controls.
// Lift finger up on a control to make click.
//
// To make use of these capabilities instantiate an object of this class
// and pass touch event to it via dispatchTouchEvent() method.
// Then you can add view elements you wish to be accessible to this list.
//
public class AccessibleContent extends ArrayList<View> {

    public interface Callback {
        public boolean dispatchNativeTouchEvent(MotionEvent event);
    }

    private final Rect testFrame = new Rect();
    private View nowTouched;

    private View findTouch(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        for (View v : this)
            if ((v.getVisibility() != View.INVISIBLE) && v.getGlobalVisibleRect(testFrame) && testFrame.contains(x, y))
                return v;
        return null;
    }

    public boolean dispatchTouchEvent(MotionEvent event, Callback callback) {
        int action = event.getAction();
        View newTouch;
        switch (action) {
        case MotionEvent.ACTION_MOVE:
            newTouch = findTouch(event);
            if ((newTouch != null) && (newTouch != nowTouched)) {
                float x = event.getX();
                float y = event.getY();
                float pressure = event.getPressure();
                float size = event.getSize();
                int metaState = event.getMetaState();
                float xPrecision = event.getXPrecision();
                float yPrecision = event.getYPrecision();
                int deviceId = event.getDeviceId();
                int edgeFlags = event.getEdgeFlags();
                event.setAction(MotionEvent.ACTION_CANCEL);
                callback.dispatchNativeTouchEvent(event);
                newTouch.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
                long now = SystemClock.uptimeMillis();
                event.recycle();
                event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, pressure, size,
                                           metaState, xPrecision, yPrecision, deviceId, edgeFlags);
            }
            nowTouched = newTouch;
            break;
        case MotionEvent.ACTION_DOWN:
            nowTouched = findTouch(event);
            if (nowTouched != null)
                nowTouched.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            break;
        default:
            nowTouched = null;
            break;
        }
        return callback.dispatchNativeTouchEvent(event);
    }

}
