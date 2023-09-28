package net.osmand.plus.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import net.osmand.plus.R;

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
		clearButton.setAlpha(0f);
	}

	public void showClearButton() {
		clearButton.setAlpha(1f);
	}
}