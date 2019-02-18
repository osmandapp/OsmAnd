package net.osmand.plus.base.bottomsheetmenu;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.v4.widget.CompoundButtonCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class BottomSheetItemWithCompoundButton extends BottomSheetItemWithDescription {

	private boolean checked;
	private ColorStateList buttonTintList;
	private OnCheckedChangeListener onCheckedChangeListener;

	private CompoundButton compoundButton;

	public boolean isChecked() {
		return checked;
	}

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
											 int descriptionMaxLines,
											 boolean checked,
											 ColorStateList buttonTintList,
											 OnCheckedChangeListener onCheckedChangeListener) {
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
				descriptionColorId,
				descriptionMaxLines);
		this.checked = checked;
		this.buttonTintList = buttonTintList;
		this.onCheckedChangeListener = onCheckedChangeListener;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
		if (compoundButton != null) {
			compoundButton.setChecked(checked);
		}
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		super.inflate(app, container, nightMode);
		compoundButton = (CompoundButton) view.findViewById(R.id.compound_button);
		if (compoundButton != null) {
			compoundButton.setChecked(checked);
			CompoundButtonCompat.setButtonTintList(compoundButton, buttonTintList);
			compoundButton.setOnCheckedChangeListener(onCheckedChangeListener);
		}
	}

	public static class Builder extends BottomSheetItemWithDescription.Builder {

		protected boolean checked;
		protected ColorStateList buttonTintList;
		protected OnCheckedChangeListener onCheckedChangeListener;

		public Builder setChecked(boolean checked) {
			this.checked = checked;
			return this;
		}

		public Builder setButtonTintList(ColorStateList buttonTintList) {
			this.buttonTintList = buttonTintList;
			return this;
		}

		public Builder setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
			this.onCheckedChangeListener = onCheckedChangeListener;
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
					descriptionMaxLines,
					checked,
					buttonTintList,
					onCheckedChangeListener);
		}
	}
}
