package net.osmand.access;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

// This class contains only static methods intended to improve
// accessibility for AlertDialog and Toast messages.
//
public class TextMessage {

    protected static View makeView(Context ctx, CharSequence msg, int resid) {
        View layout = ((LayoutInflater)(ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE))).inflate(resid, null);
        ((TextView)layout.findViewById(R.id.message)).setText(msg);
        return layout;
    }

    protected static View makeView(Context ctx, int msgid, int resid) {
        View layout = ((LayoutInflater)(ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE))).inflate(resid, null);
        ((TextView)layout.findViewById(R.id.message)).setText(msgid);
        return layout;
    }

    protected static boolean accessibilityExtensions(Context context) {
        return ((OsmandApplication)(context.getApplicationContext())).getSettings().ACCESSIBILITY_EXTENSIONS.get();
    }

}
