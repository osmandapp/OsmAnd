package net.osmand.plus.dialogs.helpscreen;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.View.OnClickListener;

public class MyMenuItem {
	private final int title;
	private final String desription;
	@DrawableRes
	private final int icon;
	private final OnClickListener onClickListener;

	public MyMenuItem(@StringRes int title) {
		this.title = title;
		this.onClickListener = null;
		desription = null;
		icon = -1;
	}

	public MyMenuItem(@StringRes int title, OnClickListener onClickListener) {
		this.title = title;
		this.onClickListener = onClickListener;
		desription = null;
		icon = -1;
	}

	private MyMenuItem(@StringRes int title, @StringRes int desription, @DrawableRes int icon,
					   Context context, OnClickListener onClickListener) {
		this.title = title;
		this.onClickListener = onClickListener;
		this.desription = context.getString(desription);
		this.icon = icon;
	}

	private MyMenuItem(@StringRes int title, String desription, @DrawableRes int icon,
					   OnClickListener onClickListener) {
		this.title = title;
		this.desription = desription;
		this.icon = icon;
		this.onClickListener = onClickListener;
	}

	public int getTitle() {
		return title;
	}

	public String getDesription() {
		return desription;
	}

	public int getIcon() {
		return icon;
	}

	public OnClickListener getOnClickListener() {
		return onClickListener;
	}

	public static class Builder {
		@StringRes
		private int title = -1;
		private String description = null;
		private int icon = -1;
		private OnClickListener listener;

		public Builder setTitle(@StringRes int title) {
			this.title = title;
			return this;
		}

		public Builder setDescription(@StringRes int desriptionId, Context context) {
			this.description = context.getString(desriptionId);
			return this;
		}

		public Builder setDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder setIcon(@DrawableRes int icon) {
			this.icon = icon;
			return this;
		}

		public void setListener(OnClickListener listener) {
			this.listener = listener;
		}

		public Builder reset() {
			title = -1;
			description = null;
			icon = -1;
			listener = null;
			return this;
		}

		public MyMenuItem create() {
			return new MyMenuItem(title, description, icon, listener);
		}
	}
}