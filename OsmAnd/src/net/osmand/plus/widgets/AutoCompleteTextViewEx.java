package net.osmand.plus.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class AutoCompleteTextViewEx extends androidx.appcompat.widget.AppCompatAutoCompleteTextView {
	public AutoCompleteTextViewEx(Context context) {
		super(context);
	}

	public AutoCompleteTextViewEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AutoCompleteTextViewEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public boolean enoughToFilter() {
		return true;
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
		if (focused && getAdapter() != null) {
			performFiltering(getText(), 0);
		}
	}
}
