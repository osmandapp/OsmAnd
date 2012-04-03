package net.osmand.access;

import net.osmand.access.AccessibleContent;

import android.view.MotionEvent;

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

    @Override
    public boolean dispatchNativeTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return accessibleContent.dispatchTouchEvent(event, this);
    }

}
