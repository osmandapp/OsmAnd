package net.osmand.plus.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import studio.carbonylgroup.textfieldboxes.TextFieldBoxes;

public class OsmandTextFieldBoxes extends TextFieldBoxes {

	private boolean useOsmandKeyboard;

	public OsmandTextFieldBoxes(Context context) {
		super(context);
	}

	public OsmandTextFieldBoxes(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OsmandTextFieldBoxes(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setUseOsmandKeyboard(boolean useOsmandKeyboard) {
		this.useOsmandKeyboard = useOsmandKeyboard;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		if (editText != null) {
			this.panel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					select();
				}
			});

			this.iconImageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					select();
				}
			});
		}
	}

	public void select() {
		if(!OsmandTextFieldBoxes.this.isActivated()) {
			OsmandTextFieldBoxes.this.activate(true);
		}

		OsmandTextFieldBoxes.this.setHasFocus(true);
		if (!useOsmandKeyboard) {
			OsmandTextFieldBoxes.this.inputMethodManager.showSoftInput(OsmandTextFieldBoxes.this.editText, InputMethodManager.SHOW_IMPLICIT);
		}
		performClick();
	}

}
