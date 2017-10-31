package net.osmand.plus.widgets;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import studio.carbonylgroup.textfieldboxes.TextFieldBoxes;

public class OsmandEditTextFieldBoxes extends TextFieldBoxes {

	public OsmandEditTextFieldBoxes(Context context) {
		super(context);
	}

	public OsmandEditTextFieldBoxes(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OsmandEditTextFieldBoxes(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void activate(boolean animated) {
		super.activate(animated);
	}
}
