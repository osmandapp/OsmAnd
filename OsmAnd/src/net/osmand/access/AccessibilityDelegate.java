package net.osmand.access;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

// This class serves as a delegate of accessibility service
// providing a sort of touch exploration capability
// for a View hierarchy. It means that elements will be spoken
// on touch. Thus, you can slide your finger across the screen
// and hear available controls and items.
// Lift finger up on a control to make click.
//
// Use static method takeCareOf() to get this functionality
// for respective objects.
//
public class AccessibilityDelegate extends FrameLayout {

    private final Rect testFrame = new Rect();
    private View nowTouched;

    private AccessibilityDelegate(Context context) {
        super(context);
    }

    // Attach itself to a target View hierarchy to intercept touch events
    // and provide on-touch accessibility feedback.
    // Target View must be an instance of FrameLayout
    // or have a parent which is an instance of ViewGroup.
    private void attach(View target) {
        ViewGroup parent;
        if (target instanceof FrameLayout) {
            parent = (ViewGroup)target;
            while (parent.getChildCount() > 0) {
                View child = parent.getChildAt(0);
                parent.removeViewAt(0);
                addView(child);
            }
            parent.addView(this, new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        } else if (target.getParent() instanceof ViewGroup) {
            parent = (ViewGroup)target.getParent();
            int position = parent.indexOfChild(target);
            ViewGroup.LayoutParams params = target.getLayoutParams();
            parent.removeViewAt(position);
            addView(target, new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            parent.addView(this, position, params);
        }
    }

    // Provide touch exploration capability for individual View
    // or whole View hierarchy. The hierarchy root specified
    // as an argument must either be an instance of FrameLayout
    // or have a parent that is an instance of ViewGroup.
    public static void takeCareOf(View hierarchy) {
        final AccessibilityDelegate delegate = new AccessibilityDelegate(hierarchy.getContext());
        delegate.attach(hierarchy);
    }

    // Provide touch exploration capability for given window.
    public static void takeCareOf(Window window) {
        takeCareOf(window.getDecorView());
    }

    // Provide touch exploration capability for an activity View content.
    // Use after setContentView().
    public static void takeCareOf(Activity activity) {
        takeCareOf(activity.getWindow());
    }

    // Provide touch exploration capability for a dialog View content.
    // Use after setContentView().
    public static void takeCareOf(Dialog dialog) {
        takeCareOf(dialog.getWindow());
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
