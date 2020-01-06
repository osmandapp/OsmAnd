package net.osmand.plus.widgets;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import net.osmand.plus.R;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;
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

	@Override
	public void activate(boolean animated) {
		super.activate(animated);
	}

	@Override
	public void deactivate() {
		if(this.editText.getText().toString().isEmpty()) {
			ViewCompat.animate(this.floatingLabel).alpha(1.0F).scaleX(1.0F).scaleY(1.0F).translationY(0.0F).setDuration((long)this.ANIMATION_DURATION);
			this.editTextLayout.setVisibility(View.INVISIBLE);
			if(this.editText.hasFocus()) {
				if (!useOsmandKeyboard) {
					this.inputMethodManager.hideSoftInputFromWindow(this.editText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
				}
				this.editText.clearFocus();
			}
		}

		this.activated = false;
	}

	public ExtendedEditText getEditText() {
		return editText;
	}

	@Override
	public void setError(String errorText, boolean giveFocus) {
		super.setError(errorText, giveFocus);
		this.findViewById(R.id.text_field_boxes_bottom).setVisibility(View.VISIBLE);
	}

	@Override
	public void removeError() {
		super.removeError();
		this.findViewById(R.id.text_field_boxes_bottom).setVisibility(View.GONE);
	}
}
