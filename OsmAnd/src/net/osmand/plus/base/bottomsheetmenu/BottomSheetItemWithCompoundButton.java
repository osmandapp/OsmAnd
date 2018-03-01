package net.osmand.plus.base.bottomsheetmenu;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.v4.widget.CompoundButtonCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class BottomSheetItemWithCompoundButton extends BottomSheetItemWithDescription {

	private boolean checked;
	private ColorStateList buttonTintList;

	public BottomSheetItemWithCompoundButton(View customView,
											 @LayoutRes int layoutId,
											 Object tag,
											 boolean disabled,
											 View.OnClickListener onClickListener,
											 int position,
											 Drawable icon,
											 String title,
											 @ColorRes int titleColorId,
											 CharSequence description,
											 @ColorRes int descriptionColorId,
											 boolean checked,
											 ColorStateList buttonTintList) {
		super(customView,
				layoutId,
				tag,
				disabled,
				onClickListener,
				position,
				icon,
				title,
				titleColorId,
				description,
				descriptionColorId);
		this.checked = checked;
		this.buttonTintList = buttonTintList;
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);
		CompoundButton compoundButton = (CompoundButton) view.findViewById(R.id.compound_button);
		compoundButton.setChecked(checked);
		CompoundButtonCompat.setButtonTintList(compoundButton, buttonTintList);
	}

	public static class Builder extends BottomSheetItemWithDescription.Builder {

		protected boolean checked;
		protected ColorStateList buttonTintList;

		public Builder setChecked(boolean checked) {
			this.checked = checked;
			return this;
		}

		public Builder setButtonTintList(ColorStateList buttonTintList) {
			this.buttonTintList = buttonTintList;
			return this;
		}

		public BottomSheetItemWithCompoundButton create() {
			return new BottomSheetItemWithCompoundButton(customView,
					layoutId,
					tag,
					disabled,
					onClickListener,
					position,
					icon,
					title,
					titleColorId,
					description,
					descriptionColorId,
					checked,
					buttonTintList);
		}
	}
}
