package net.osmand.plus.widgets.popup;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.ColorInt;

public class PopUpMenuItem {
	private CharSequence title;
	private Drawable icon;
	private View.OnClickListener onClickListener;
	private boolean selected;
	@ColorInt
	private Integer compoundBtnColor;

	private PopUpMenuItem(CharSequence title,
	                     Drawable icon,
	                     View.OnClickListener onClickListener,
	                     boolean selected,
	                     Integer compoundBtnColor) {
		this.title = title;
		this.icon = icon;
		this.onClickListener = onClickListener;
		this.selected = selected;
		this.compoundBtnColor = compoundBtnColor;
	}

	public CharSequence getTitle() {
		return title;
	}

	public Drawable getIcon() {
		return icon;
	}

	public View.OnClickListener getOnClickListener() {
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

	public static class Builder {
		private Context ctx;
		private CharSequence title;
		private Drawable icon;
		private View.OnClickListener onClickListener;
		@ColorInt
		private Integer compoundBtnColor;
		private boolean selected;

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

		public Builder setOnClickListener(View.OnClickListener onClickListener) {
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

		public PopUpMenuItem create() {
			return new PopUpMenuItem(title, icon, onClickListener, selected, compoundBtnColor);
		}
	}
}
