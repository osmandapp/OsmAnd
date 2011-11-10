package net.osmand.access;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

// This class can be used instead of FrameLayout in order to provide
// a sort of touch exploration capability for enclosing View hierarchy.
// It means that elements will be spoken on touch.
// Thus, you can slide your finger across the screen
// and hear available controls and items.
// Lift finger up on a control to make click.
//
public class AccessibleLayout extends FrameLayout {

    private final Rect testFrame = new Rect();
    private View nowTouched;


    // Conventional public constructors

    public AccessibleLayout(Context context) {
        super(context);
    }

    public AccessibleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessibleLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    // Recursive search through View tree.
    private View findTouch(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        View root = this;
        View control = null;
        View text = null;
        while (root instanceof ViewGroup) {
            int nBranches = ((ViewGroup)root).getChildCount();
            int i;
            for (i = 0; i < nBranches; i++) {
                View child = ((ViewGroup)root).getChildAt(i);
                if ((child.getVisibility() != View.INVISIBLE) && child.getGlobalVisibleRect(testFrame) && testFrame.contains(x, y)) {
                    if (child.isClickable())
                        control = child;
                    else if (child instanceof TextView)
                        text = child;
                    root = child;
                    break;
                }
            }
            if (i == nBranches)
                break;
        }
        if (control != null)
            return control;
        return text;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction();
        View newTouch;
        switch (action) {
        case MotionEvent.ACTION_MOVE:
            newTouch = findTouch(event);
            if ((newTouch != null) && (newTouch != nowTouched)) {
                if (newTouch.isClickable()) {
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
                    super.dispatchTouchEvent(event);
                    long now = SystemClock.uptimeMillis();
                    event.recycle();
                    event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, pressure, size,
                                               metaState, xPrecision, yPrecision, deviceId, edgeFlags);
                }
                newTouch.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
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
        super.dispatchTouchEvent(event);
        return true;
    }

}
