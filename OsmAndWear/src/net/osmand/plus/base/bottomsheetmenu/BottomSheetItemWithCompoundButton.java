package net.osmand.plus.base.bottomsheetmenu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;

import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;

public class BottomSheetItemWithCompoundButton extends BottomSheetItemWithDescription {

	private boolean checked;
	private final ColorStateList buttonTintList;
	private final OnCheckedChangeListener onCheckedChangeListener;
	@ColorRes private int compoundButtonColorId;
	@ColorInt private Integer compoundButtonColor;

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
											 Drawable background,
											 CharSequence title,
											 @ColorRes int titleColorId,
											 boolean iconHidden,
											 CharSequence description,
											 @ColorRes int descriptionColorId,
											 int descriptionMaxLines,
											 boolean descriptionLinksClickable,
											 boolean checked,
											 ColorStateList buttonTintList,
											 OnCheckedChangeListener onCheckedChangeListener,
											 @ColorRes int compoundButtonColorId) {
		super(customView,
				layoutId,
				tag,
				disabled,
				onClickListener,
				position,
				icon,
				background,
				title,
				titleColorId,
				iconHidden,
				description,
				descriptionColorId,
				descriptionMaxLines,
				descriptionLinksClickable);
		this.checked = checked;
		this.buttonTintList = buttonTintList;
		this.onCheckedChangeListener = onCheckedChangeListener;
		this.compoundButtonColorId = compoundButtonColorId;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
		if (compoundButton != null) {
			compoundButton.setChecked(checked);
		}
	}
	
	public void setCompoundButtonColorId(@ColorRes int compoundButtonColorId) {
		this.compoundButtonColorId = compoundButtonColorId;
	}

	public void setCompoundButtonColor(@ColorInt int compoundButtonColor) {
		this.compoundButtonColor = compoundButtonColor;
	}

	public CompoundButton getCompoundButton() {
		return compoundButton;
	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		super.inflate(context, container, nightMode);
		compoundButton = view.findViewById(R.id.compound_button);
		if (compoundButton != null) {
			compoundButton.setChecked(checked);
			compoundButton.setOnCheckedChangeListener(onCheckedChangeListener);
			if (compoundButtonColor != null) {
				UiUtilities.setupCompoundButton(nightMode, compoundButtonColor, compoundButton);
			} else if (compoundButtonColorId != INVALID_ID) {
				UiUtilities.setupCompoundButton(nightMode, ContextCompat.getColor(context, compoundButtonColorId), compoundButton);
			} else {
				CompoundButtonCompat.setButtonTintList(compoundButton, buttonTintList);
			}
		}
	}

	public static class Builder extends BottomSheetItemWithDescription.Builder {

		protected boolean checked;
		protected ColorStateList buttonTintList;
		protected OnCheckedChangeListener onCheckedChangeListener;
		@ColorRes protected int compoundButtonColorId = INVALID_ID;
		@ColorInt protected Integer compoundButtonColor;

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

		public Builder setCompoundButtonColorId(@ColorRes int compoundButtonColorId) {
			this.compoundButtonColorId = compoundButtonColorId;
			return this;
		}

		public Builder setCompoundButtonColor(@ColorInt int compoundButtonColor) {
			this.compoundButtonColor = compoundButtonColor;
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
					background,
					title,
					titleColorId,
					iconHidden,
					description,
					descriptionColorId,
					descriptionMaxLines,
					descriptionLinksClickable,
					checked,
					buttonTintList,
					onCheckedChangeListener,
					compoundButtonColorId);
		}
	}
}
