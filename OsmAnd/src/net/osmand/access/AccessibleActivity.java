package net.osmand.access;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.R;
import net.osmand.access.AccessibilityDelegate;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Window;
import android.view.View;
import android.widget.TextView;

// Provide some additional accessibility means for activity view elements.
//
// To make use of these capabilities simply derive your activity from this class
// and then add view elements you wish to be accessible to the accessibleViews list
// or attach accessibility delegate to a view hierarchy.
//
public class AccessibleActivity extends Activity {

    // List of accessible views. Use accessibleViews.add(element)
    // to add element to it. The accessible views will be spoken on touch.
    // Thus, you can slide your finger across the screen and hear
    // available controls. Lift finger up on a control to make click.
    //
    // Use this list to improve accessibility for individual elements.
    public final List<View> accessibleViews = new ArrayList<View>();

    private final Rect testFrame = new Rect();
    private View nowTouched;

    // Provide touch exploration capability for given View hierarchy.
    // The hierarchy root provided as an argument must be
    // an instance of FrameLayout or have a parent
    // which is an instance of ViewGroup.
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

    // Below are two helper methods to improve AlertDialog accessibility.
    //
    // Since usual message in an AlertDialog that is set by
    // AlertDialog.Builder.setMessage() is spoken only once at the best case
    // and there is no way to repeat it, use following two methods
    // to wrap it into a View and set it by AlertDialog.Builder.setView().
    // Such message will be focusable and so it can be repeated by selecting.

    public static View caredMessage(Context ctx, CharSequence msg) {
        View layout = ((LayoutInflater)(ctx.getSystemService(LAYOUT_INFLATER_SERVICE))).inflate(R.layout.alert, null);
        ((TextView)layout.findViewById(R.id.message)).setText(msg);
        return layout;
    }

    public static View caredMessage(Context ctx, int msgid) {
        View layout = ((LayoutInflater)(ctx.getSystemService(LAYOUT_INFLATER_SERVICE))).inflate(R.layout.alert, null);
        ((TextView)layout.findViewById(R.id.message)).setText(msgid);
        return layout;
    }

    public View accessibleMessage(CharSequence msg) {
        return caredMessage(this, msg);
    }

    public View accessibleMessage(int msgid) {
        return caredMessage(this, msgid);
    }

    private View findTouch(MotionEvent event) {
        int x = (int)event.getX();
        int y = (int)event.getY();
        for (View v : accessibleViews)
            if ((v.getVisibility() != View.INVISIBLE) && v.getGlobalVisibleRect(testFrame) && testFrame.contains(x, y))
                return v;
        return null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
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
                super.dispatchTouchEvent(event);
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
        return super.dispatchTouchEvent(event);
    }

}
