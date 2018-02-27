package net.osmand.plus.base.bottomsheetmenu;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.view.View;

public class BottomSheetItemWithDescription extends SimpleBottomSheetItem {

	private String description;
	@StringRes
	private int descriptionId;
	@ColorRes
	private int descriptionColorId;

	public BottomSheetItemWithDescription(View customView,
										  @LayoutRes int layoutResId,
										  boolean clickable,
										  View.OnClickListener onClickListener,
										  int position,
										  Drawable icon,
										  @DrawableRes int iconId,
										  @ColorRes int iconColorId,
										  String title,
										  @StringRes int titleId,
										  @ColorRes int titleColorId,
										  String description,
										  @StringRes int descriptionId,
										  @ColorRes int descriptionColorId) {
		super(customView, layoutResId, clickable, onClickListener, position, icon, iconId, iconColorId, title, titleId, titleColorId);
		this.description = description;
		this.descriptionId = descriptionId;
		this.descriptionColorId = descriptionColorId;
	}

	public String getDescription() {
		return description;
	}

	@StringRes
	public int getDescriptionId() {
		return descriptionId;
	}

	@ColorRes
	public int getDescriptionColorId() {
		return descriptionColorId;
	}

	public static class Builder extends SimpleBottomSheetItem.Builder {

		protected String description;
		@StringRes
		protected int descriptionId = INVALID_ID;
		@ColorRes
		protected int descriptionColorId = INVALID_ID;

		public Builder setDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder setDescriptionId(@StringRes int descriptionId) {
			this.descriptionId = descriptionId;
			return this;
		}

		public Builder setDescriptionColorId(@ColorRes int descriptionColorId) {
			this.descriptionColorId = descriptionColorId;
			return this;
		}

		public BottomSheetItemWithDescription create() {
			return new BottomSheetItemWithDescription(customView,
					layoutId,
					disabled,
					onClickListener,
					position,
					icon,
					iconId,
					iconColorId,
					title,
					titleId,
					titleColorId,
					description,
					descriptionId,
					descriptionColorId);
		}
	}
}
