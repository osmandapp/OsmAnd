package net.osmand.plus.base.bottomsheetmenu;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.StringRes;
import android.view.View;

public class SimpleBottomSheetItem extends BaseBottomSheetItem {

	public static final int CONTENT_ICON_COLOR = -1;

	private Drawable icon;
	@DrawableRes
	private int iconId;
	@ColorRes
	private int iconColorId;
	private String title;
	@StringRes
	private int titleId;
	@ColorRes
	private int titleColorId;

	public SimpleBottomSheetItem(View customView,
								 @LayoutRes int layoutResId,
								 boolean clickable,
								 View.OnClickListener onClickListener,
								 int position,
								 Drawable icon,
								 @DrawableRes int iconId,
								 @ColorRes int iconColorId,
								 String title,
								 @StringRes int titleId,
								 @ColorRes int titleColorId) {
		super(customView, layoutResId, clickable, onClickListener, position);
		this.icon = icon;
		this.iconId = iconId;
		this.iconColorId = iconColorId;
		this.title = title;
		this.titleId = titleId;
		this.titleColorId = titleColorId;
	}

	public Drawable getIcon() {
		return icon;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@ColorRes
	public int getIconColorId() {
		return iconColorId;
	}

	public String getTitle() {
		return title;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@ColorRes
	public int getTitleColorId() {
		return titleColorId;
	}

	public static class Builder extends BaseBottomSheetItem.Builder {

		protected Drawable icon;
		@DrawableRes
		protected int iconId = INVALID_ID;
		@ColorRes
		protected int iconColorId = CONTENT_ICON_COLOR;
		protected String title;
		@StringRes
		protected int titleId = INVALID_ID;
		@ColorRes
		protected int titleColorId = INVALID_ID;

		public Builder setIcon(Drawable icon) {
			this.icon = icon;
			return this;
		}

		public Builder setIconId(@DrawableRes int iconId) {
			this.iconId = iconId;
			return this;
		}

		public Builder setIconColorId(@ColorRes int iconColorId) {
			this.iconColorId = iconColorId;
			return this;
		}

		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}

		public Builder setTitleId(@StringRes int titleId) {
			this.titleId = titleId;
			return this;
		}

		public Builder setTitleColorId(@ColorRes int titleColorId) {
			this.titleColorId = titleColorId;
			return this;
		}

		public SimpleBottomSheetItem create() {
			return new SimpleBottomSheetItem(customView,
					layoutId,
					disabled,
					onClickListener,
					position,
					icon,
					iconId,
					iconColorId,
					title,
					titleId,
					titleColorId);
		}
	}
}
