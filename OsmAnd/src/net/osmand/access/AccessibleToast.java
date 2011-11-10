package net.osmand.access;

import net.osmand.access.TextMessage;
import net.osmand.plus.R;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

// Use this class instead of regular Toast to have
// accessibility feedback on toast messages.
public class AccessibleToast extends Toast {

    public AccessibleToast(Context context) {
        super(context);
    }

    public static Toast makeText(Context context, int msg, int duration) {
        final Toast toast = new AccessibleToast(context);
        toast.setView(TextMessage.makeView(context, msg));
        toast.setDuration(duration);
        return toast;
    }

    public static Toast makeText(Context context, CharSequence msg, int duration) {
        final Toast toast = new AccessibleToast(context);
        toast.setView(TextMessage.makeView(context, msg));
        toast.setDuration(duration);
        return toast;
    }

    @Override
    public void show() {
        getView().sendAccessibilityEvent(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
        super.show();
    }

}
