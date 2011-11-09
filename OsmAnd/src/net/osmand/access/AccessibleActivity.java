package net.osmand.access;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.R;
import net.osmand.access.AccessibleContent;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

// Provide some additional accessibility means for activity view elements.
//
// To make use of these capabilities simply derive your activity from this class
// and then add view elements you wish to be accessible
// to the accessibleContent list.
//
public class AccessibleActivity extends Activity {

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

    // Original touch event dispatcher
    private boolean TouchEventCallback(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return accessibleContent.dispatchTouchEvent(event,
                                                    new AccessibleContent.Callback() {
                                                        @Override
                                                        public boolean dispatchTouchEvent(MotionEvent event) {
                                                            return TouchEventCallback(event);
                                                        }
                                                    });
    }

}
