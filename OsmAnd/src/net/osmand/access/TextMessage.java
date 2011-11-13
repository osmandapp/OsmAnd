package net.osmand.access;

import java.util.ArrayList;
import java.util.List;

import net.osmand.plus.R;

import android.content.Context;
import android.text.TextUtils.SimpleStringSplitter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

// This class contains only static methods intended to improve
// accessibility for AlertDialog and Toast messages.
//
// Since usual message in an AlertDialog that is set by
// AlertDialog.Builder.setMessage() is spoken only once
// at the best case and there is no way to explore or even repeat it,
// use methods of this class to wrap it into a View
// and set it by AlertDialog.Builder.setView().
// Such message will be focusable and so it can be repeated
// by selecting.
//
public class TextMessage {

    public static View makeView(Context ctx, CharSequence msg) {
        View layout = ((LayoutInflater)(ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE))).inflate(R.layout.alert, null);
        ((TextView)layout.findViewById(R.id.message)).setText(msg);
        return layout;
    }

    public static View makeView(Context ctx, int msgid) {
        View layout = ((LayoutInflater)(ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE))).inflate(R.layout.alert, null);
        ((TextView)layout.findViewById(R.id.message)).setText(msgid);
        return layout;
    }

}
