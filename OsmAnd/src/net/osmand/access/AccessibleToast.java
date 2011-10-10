package net.osmand.access;

import net.osmand.plus.R;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

// Use this class instead of regular Toast to have
// accessibility feedback on toast essages.
public class AccessibleToast extends Toast {

    public AccessibleToast(Context context) {
        super(context);
    }

    public static Toast makeText(Context context, int msg, int duration) {
        final Toast toast = new AccessibleToast(context);
        View layout = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.alert, null);
        ((TextView)layout.findViewById(R.id.message)).setText(msg);
        toast.setView(layout);
        toast.setDuration(duration);
        return toast;
    }

    public static Toast makeText(Context context, CharSequence msg, int duration) {
        final Toast toast = new AccessibleToast(context);
        View layout = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.alert, null);
        ((TextView)layout.findViewById(R.id.message)).setText(msg);
        toast.setView(layout);
        toast.setDuration(duration);
        return toast;
    }

    @Override
    public void show() {
        getView().sendAccessibilityEvent(AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
        super.show();
    }

}
