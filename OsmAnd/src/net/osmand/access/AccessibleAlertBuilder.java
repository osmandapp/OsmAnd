package net.osmand.access;

import net.osmand.access.TextMessage;
import net.osmand.plus.R;

import android.app.AlertDialog.Builder;
import android.content.Context;

// Since usual message in an AlertDialog that is set by
// AlertDialog.Builder.setMessage() is spoken only once
// at the best case and there is no way to explore or even repeat it,
// this class provides more accessible alternative
// that wraps the message into a dedicated view.
// Such message will be focusable and so it can be repeated
// by selecting.
//
// Note: when accessibility extensions option is not checked
// or system accessibility service is turned off this class
// acts just identical to it's direct parent.
//
public class AccessibleAlertBuilder extends Builder {

    // The method getContext() is only available
    // starting from API level 11, so store it here.
    private final Context context;

    // Conventional constructor.
    public AccessibleAlertBuilder(Context context) {
        super(context);
        this.context = context;
    }


    // Provided setMessage() alternatives.

    @Override
    public Builder setMessage(CharSequence msg) {
        if (TextMessage.accessibilityExtensions(context))
            return setView(TextMessage.makeView(context, msg, R.layout.alert));
        return super.setMessage(msg);
    }

    @Override
    public Builder setMessage(int msgid) {
        if (TextMessage.accessibilityExtensions(context))
            return setView(TextMessage.makeView(context, msgid, R.layout.alert));
        return super.setMessage(msgid);
    }

}
