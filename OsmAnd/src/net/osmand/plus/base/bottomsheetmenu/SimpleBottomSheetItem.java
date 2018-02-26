package net.osmand.plus.base.bottomsheetmenu;

import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.View;

public class SimpleBottomSheetItem extends BaseBottomSheetItem {

	private Drawable icon;
	@DrawableRes
	private int iconResId;
	@ColorRes
	private int iconColorResId;
	private String title;
	@StringRes
	private int titleResId;
	@ColorRes
	private int titleColorResId;

	public SimpleBottomSheetItem(View customView,
								 int layoutResId,
								 boolean clickable,
								 View.OnClickListener onClickListener,
								 int position,
								 Drawable icon,
								 int iconResId,
								 int iconColorResId,
								 String title,
								 int titleResId,
								 int titleColorResId) {
		super(customView, layoutResId, clickable, onClickListener, position);
		this.icon = icon;
		this.iconResId = iconResId;
		this.iconColorResId = iconColorResId;
		this.title = title;
		this.titleResId = titleResId;
		this.titleColorResId = titleColorResId;
	}

	public Drawable getIcon() {
		return icon;
	}

	public int getIconResId() {
		return iconResId;
	}

	public int getIconColorResId() {
		return iconColorResId;
	}

	public String getTitle() {
		return title;
	}

	public int getTitleResId() {
		return titleResId;
	}

	public int getTitleColorResId() {
		return titleColorResId;
	}

	public static class Builder extends BaseBottomSheetItem.Builder {

		private Drawable icon;
		private int iconResId;
		private int iconColorResId;
		private String title;
		private int titleResId;
		private int titleColorResId;

		public Builder setIcon(Drawable icon) {
			this.icon = icon;
			return this;
		}

		public Builder setIconResId(int iconResId) {
			this.iconResId = iconResId;
			return this;
		}

		public Builder setIconColorResId(int iconColorResId) {
			this.iconColorResId = iconColorResId;
			return this;
		}

		public Builder setTitle(String title) {
			this.title = title;
			return this;
		}

		public Builder setTitleResId(int titleResId) {
			this.titleResId = titleResId;
			return this;
		}

		public Builder setTitleColorResId(int titleColorResId) {
			this.titleColorResId = titleColorResId;
			return this;
		}

		public SimpleBottomSheetItem createSimpleBottomSheetItem() {
			return new SimpleBottomSheetItem(customView,
					layoutResId,
					clickable,
					onClickListener,
					position,
					icon,
					iconResId,
					iconColorResId,
					title,
					titleResId,
					titleColorResId);
		}
	}
}
