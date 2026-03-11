package net.osmand.plus.widgets.popup;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

public class PopUpMenuItem {

	public enum CompoundButtonType {
		RADIO,
		CHECKBOX
	}

	private final CharSequence title;
	@ColorInt
	private final Integer titleColor;
	private final Integer titleSize;
	private final Drawable icon;
	private final OnPopUpMenuItemClickListener onClickListener;
	@ColorInt
	private final Integer compoundBtnColor;
	private final CompoundButtonType compoundButtonType;
	private final boolean selected;
	private final boolean showTopDivider;
	private final boolean titleBold;
	private final boolean dismissOnClick;
	private final Object tag;

	private PopUpMenuItem(CharSequence title,
	                      @ColorInt @Nullable Integer titleColor,
	                      Integer titleSize,
	                      Drawable icon,
	                      OnPopUpMenuItemClickListener onClickListener,
	                      Integer compoundBtnColor,
	                      CompoundButtonType compoundButtonType,
	                      boolean selected,
	                      boolean showTopDivider,
	                      boolean titleBold,
	                      boolean dismissOnClick,
	                      Object tag) {
		this.title = title;
		this.titleColor = titleColor;
		this.titleSize = titleSize;
		this.icon = icon;
		this.onClickListener = onClickListener;
		this.compoundBtnColor = compoundBtnColor;
		this.compoundButtonType = compoundButtonType;
		this.selected = selected;
		this.showTopDivider = showTopDivider;
		this.titleBold = titleBold;
		this.dismissOnClick = dismissOnClick;
		this.tag = tag;
	}

	public CharSequence getTitle() {
		return title;
	}

	@ColorInt
	@Nullable
	public Integer getTitleColor() {
		return titleColor;
	}

	public Integer getTitleSize() {
		return titleSize;
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

	@Nullable
	public CompoundButtonType getCompoundButtonType() {
		return compoundButtonType;
	}

	public boolean isSelected() {
		return selected;
	}

	public boolean shouldShowTopDivider() {
		return showTopDivider;
	}

	public boolean isTitleBold() {
		return titleBold;
	}

	public boolean shouldDismissOnClick() {
		return dismissOnClick;
	}

	public Object getTag() {
		return tag;
	}

	public boolean hasCustomization() {
		return isShowCompoundBtn() || getTitleColor() != null || titleBold;
	}

	public static class Builder {
		private final Context ctx;
		private CharSequence title;
		@ColorInt
		private Integer titleColor;
		private Integer titleSize = 16; //SP
		private Drawable icon;
		private OnPopUpMenuItemClickListener onClickListener;
		@ColorInt
		private Integer compoundBtnColor;
		private CompoundButtonType compoundButtonType;
		private boolean selected;
		private boolean showTopDivider;
		private boolean titleBold;
		private boolean dismissOnClick = true;
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

		public Builder setTitleColor(@ColorInt Integer titleColor) {
			this.titleColor = titleColor;
			return this;
		}

		/**
		 * Sets title text size in SP units.
		 *
		 * @param titleSizeSp text size in scaled pixels (SP)
		 */
		public Builder setTitleSize(Integer titleSizeSp) {
			this.titleSize = titleSizeSp;
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
			this.compoundButtonType = CompoundButtonType.RADIO;
			return this;
		}

		public Builder showCompoundBtn(int compoundBtnColor, @Nullable CompoundButtonType compoundButtonType) {
			this.compoundBtnColor = compoundBtnColor;
			this.compoundButtonType = compoundButtonType;
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

		public Builder setTitleBold(boolean titleBold) {
			this.titleBold = titleBold;
			return this;
		}

		public Builder setDismissOnClick(boolean dismissOnClick) {
			this.dismissOnClick = dismissOnClick;
			return this;
		}

		public Builder setTag(Object tag) {
			this.tag = tag;
			return this;
		}

		public PopUpMenuItem create() {
			return new PopUpMenuItem(title, titleColor, titleSize, icon,
					onClickListener, compoundBtnColor, compoundButtonType, selected,
					showTopDivider, titleBold, dismissOnClick, tag);
		}
	}
}
