package net.osmand.access;

import net.osmand.plus.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

// This class contains only static methods intended to improve
// accessibility for AlertDialog and Toast messages.
//
// Since usual message in an AlertDialog that is set by
// AlertDialog.Builder.setMessage() is spoken only once
// at the best case and there is no way to explore or even repeat it,
// use public methods of this class to wrap it into a View
// and set it by AlertDialog.Builder.setView().
// Such message will be focusable and so it can be repeated
// by selecting.
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

    public static View makeView(Context ctx, CharSequence msg) {
        return makeView(ctx, msg, R.layout.alert);
    }

    public static View makeView(Context ctx, int msgid) {
        return makeView(ctx, msgid, R.layout.alert);
    }

}
