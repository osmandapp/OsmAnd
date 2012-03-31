package net.osmand.access;

import android.view.MotionEvent;
import android.view.View;

import com.google.android.apps.analytics.easytracking.TrackedActivity;

// Provide some additional accessibility means for activity view elements.
//
// To make use of these capabilities simply derive your activity from this class
// and then add view elements you wish to be accessible
// to the accessibleContent list.
//
public class AccessibleTrackedActivity extends TrackedActivity implements AccessibleContent.Callback {

    // List of accessible views. Use accessibleContent.add(element)
    // to add element to it.
    public final AccessibleContent accessibleContent = new AccessibleContent();

    // Below are two helper methods to improve AlertDialog accessibility.
    //
    // Since usual message in an AlertDialog that is set by
    // AlertDialog.Builder.setMessage() is spoken only once at the best case
    // and there is no way to repeat it, use following two methods
    // to wrap it into a View and set it by AlertDialog.Builder.setView().
    // Such message will be focusable and so it can be repeated by selecting.

    public View accessibleMessage(CharSequence msg) {
        return TextMessage.makeView(this, msg);
    }

    public View accessibleMessage(int msgid) {
        return TextMessage.makeView(this, msgid);
    }

    @Override
    public boolean dispatchNativeTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return accessibleContent.dispatchTouchEvent(event, this);
    }

}
