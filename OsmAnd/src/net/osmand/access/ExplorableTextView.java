package net.osmand.access;

import java.lang.Math;

import android.content.Context;
import android.text.Layout;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityEvent;
import android.view.MotionEvent;
import android.widget.TextView;

// This class is intended to be used in place of the TextView
// to provide accessible exploration capability by arrow keys.
//
public class ExplorableTextView extends TextView {

    private int currentPosition;
    private int previousPosition;
    private int selectionStart;
    private int selectionEnd;
    private boolean cursorTrackingEnabled = true;


    // Conventional constructors.

    public ExplorableTextView(Context context) {
        super(context);
    }

    public ExplorableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExplorableTextView(Context context, AttributeSet attrs, int style) {
        super(context, attrs, style);
    }


    // Overrided callbacks to provide accessible exploration means.

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        cursorTrackingEnabled = false;
        boolean result = super.dispatchPopulateAccessibilityEvent(event);
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            event.setAddedCount(selectionEnd - selectionStart);
            event.setFromIndex(selectionStart);
            result = true;
        }
        cursorTrackingEnabled =true;
        return result;
    }

    @Override
    protected MovementMethod getDefaultMovementMethod() {
        return ArrowKeyMovementMethod.getInstance();
    }

    @Override
    protected void onSelectionChanged(int start, int end) {
        if (cursorTrackingEnabled) {
            previousPosition = currentPosition;
            currentPosition = end;
            if (currentPosition >= getText().length())
                previousPosition = currentPosition;
            if (currentPosition != previousPosition) {
                if (Math.abs(currentPosition - previousPosition) > 1) {
                    final Layout layout = getLayout();
                    final int line = layout.getLineForOffset(currentPosition);
                    selectionStart = layout.getLineStart(line);
                    selectionEnd = layout.getLineEnd(line);
                } else {
                    selectionStart = currentPosition;
                    selectionEnd = currentPosition + 1;
                }
                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
            }
        }
    }

}
