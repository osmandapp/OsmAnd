package net.osmand.plus.widgets.popup;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public class PopUpMenuItem {

	private final CharSequence title;
	private final Drawable icon;
	private final OnPopUpMenuItemClickListener onClickListener;
	@ColorInt
	private final Integer compoundBtnColor;
	private final boolean selected;
	private final boolean showTopDivider;
	private final Object tag;

	private PopUpMenuItem(CharSequence title,
	                      Drawable icon,
	                      OnPopUpMenuItemClickListener onClickListener,
	                      Integer compoundBtnColor,
	                      boolean selected,
	                      boolean showTopDivider,
	                      Object tag) {
		this.title = title;
		this.icon = icon;
		this.onClickListener = onClickListener;
		this.compoundBtnColor = compoundBtnColor;
		this.selected = selected;
		this.showTopDivider = showTopDivider;
		this.tag = tag;
	}

	public CharSequence getTitle() {
		return title;
	}

	public Drawable getIcon() {
		return icon;
	}

	@Nullable
	public OnPopUpMenuItemClickListener getOnClickListener() {
		return onClickListener;
	}

	public boolean isShowCompoundBtn() {
		return compoundBtnColor != null;
	}

	public Integer getCompoundBtnColor() {
		return compoundBtnColor;
	}

	public boolean isSelected() {
		return selected;
	}

	public boolean shouldShowTopDivider() {
		return showTopDivider;
	}

	public Object getTag() {
		return tag;
	}

	public static class Builder {
		private final Context ctx;
		private CharSequence title;
		private Drawable icon;
		private OnPopUpMenuItemClickListener onClickListener;
		@ColorInt
		private Integer compoundBtnColor;
		private boolean selected;
		private boolean showTopDivider;
		private Object tag;

		public Builder(Context ctx) {
			this.ctx = ctx;
		}

		public Builder setTitleId(int titleId) {
			this.title = ctx.getString(titleId);
			return this;
		}

		public Builder setTitle(CharSequence title) {
			this.title = title;
			return this;
		}

		public Builder setIcon(Drawable icon) {
			this.icon = icon;
			return this;
		}

		public Builder setOnClickListener(@Nullable OnPopUpMenuItemClickListener onClickListener) {
			this.onClickListener = onClickListener;
			return this;
		}

		public Builder showCompoundBtn(int compoundBtnColor) {
			this.compoundBtnColor = compoundBtnColor;
			return this;
		}

		public Builder setSelected(boolean selected) {
			this.selected = selected;
			return this;
		}

		public Builder showTopDivider(boolean showTopDivider) {
			this.showTopDivider = showTopDivider;
			return this;
		}

		public Builder setTag(Object tag) {
			this.tag = tag;
			return this;
		}

		public PopUpMenuItem create() {
			return new PopUpMenuItem(title, icon, onClickListener,
					compoundBtnColor, selected, showTopDivider, tag);
		}
	}
}