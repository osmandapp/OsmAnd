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
		int paddingH = getResources().getDimensionPixelSize(R.dimen.route_info_card_details_margin);
		inputLayout.setPadding(0, paddingH, 0, paddingH);
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
							inputLayout.getPaddingTop() + getResources().getDimensionPixelSize(R.dimen.pages_item_padding),
							inputLayout.getPaddingRight(),
							inputLayout.getPaddingBottom()
					);
				}
			}
		});
	}
}
