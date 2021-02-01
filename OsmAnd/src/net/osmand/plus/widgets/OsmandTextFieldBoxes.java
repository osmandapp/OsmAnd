package net.osmand.plus.widgets;

import android.content.Context;
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
		floatingLabel.post(new Runnable() {
			@Override
			public void run() {
				if (floatingLabel.getLineCount() > 1) {
					inputLayout.setPadding(
							inputLayout.getPaddingLeft(),
							getResources().getDimensionPixelOffset(useDenseSpacing ? R.dimen.dense_editTextLayout_padding_top : R.dimen.editTextLayout_padding_top) +
									getResources().getDimensionPixelSize(useDenseSpacing ? R.dimen.context_menu_first_line_top_margin : R.dimen.content_padding_small),
							inputLayout.getPaddingRight(),
							inputLayout.getPaddingBottom()
					);
				}
			}
		});
	}
}
