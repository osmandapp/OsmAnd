package net.osmand.access;

import android.content.Context;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

// This class is intended to be used in place of the TextView
// when constructing toast notifications to provide accessibility feedback.
//
public class NotificationTextView extends TextView {

    // Conventional constructors.

    public NotificationTextView(Context context) {
        super(context);
    }

    public NotificationTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NotificationTextView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
    }


    // Overridden callback methods to provide accessible exploration means.

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        boolean result = super.dispatchPopulateAccessibilityEvent(event);
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            event.getText().clear();
            event.getText().add(getText().subSequence(0, Math.min(getText().length(), AccessibilityEvent.MAX_TEXT_LENGTH)));
            result = true;
        }
        return result;
    }

}
