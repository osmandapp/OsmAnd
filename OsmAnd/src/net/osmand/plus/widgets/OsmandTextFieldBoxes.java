package net.osmand.plus.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.UiUtilities;

import studio.carbonylgroup.textfieldboxes.TextFieldBoxes;

public class OsmandTextFieldBoxes extends TextFieldBoxes {

	public OsmandTextFieldBoxes(Context context) {
		super(context);
	}

	public OsmandTextFieldBoxes(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public OsmandTextFieldBoxes(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		editText.setOnFocusChangeListener((view, hasFocus) -> {
			setHasFocus(hasFocus);
			if (hasFocus) {
				inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
				performClick();
			}
		});
	}

	public void makeCompactPadding() {
		floatingLabel.setVisibility(View.GONE);
		labelSpace.setVisibility(View.GONE);
		labelSpaceBelow.setVisibility(View.GONE);
		int paddingV = getResources().getDimensionPixelSize(R.dimen.route_info_card_details_margin);
		inputLayout.setPadding(0, paddingV, 0, paddingV);
	}

	public void setGravityFloatingLabel(int gravity) {
		floatingLabel.setGravity(gravity);
	}

	@Override
	public void setLabelText(String labelText) {
		super.setLabelText(labelText);
		floatingLabel.post(() -> {
			if (floatingLabel.getLineCount() > 1) {
				int topPaddingRes = useDenseSpacing
						? R.dimen.favorites_my_places_icon_size
						: R.dimen.list_content_padding_large;
				int topPadding = getResources().getDimensionPixelSize(topPaddingRes);
				inputLayout.setPadding(inputLayout.getPaddingLeft(), topPadding,
						inputLayout.getPaddingRight(), inputLayout.getPaddingBottom());
			}
		});
	}

	public void setClearButton(Drawable clearIcon) {
		showClearButton();
		clearButton.setColorFilter(null);
		clearButton.setImageDrawable(clearIcon);
		clearButton.setContentDescription(getContext().getString(R.string.shared_string_clear));
	}

	public void hideClearButton() {
		AndroidUiHelper.updateVisibility(clearButton, false);
	}

	public void showClearButton() {
		AndroidUiHelper.updateVisibility(clearButton, true);
	}
}