package net.osmand.plus.dialogs.helpscreen;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.View.OnClickListener;

public class MyMenuItem {
	private final String title;
	private final String desription;
	@DrawableRes
	private final int icon;
	private final OnClickListener onClickListener;

	public MyMenuItem(String title) {
		this(title, null);
	}

	public MyMenuItem(@StringRes int title, Context context) {
		this(context.getString(title));
	}

	public MyMenuItem(String title, OnClickListener onClickListener) {
		this(title, null, -1, onClickListener);
	}

	private MyMenuItem(String title, @StringRes int desription, @DrawableRes int icon,
					   Context context, OnClickListener onClickListener) {
		this(title, context.getString(desription), icon, onClickListener);
	}

	private MyMenuItem(String title, String desription, @DrawableRes int icon,
					   OnClickListener onClickListener) {
		this.title = title;
		this.desription = desription;
		this.icon = icon;
		this.onClickListener = onClickListener;
	}

	public String getTitle() {
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
		private String title = null;
		private String description = null;
		private int icon = -1;
		private OnClickListener listener;

		public Builder setTitle(@StringRes int titleId, Context context) {
			this.title = context.getString(titleId);
			return this;
		}

		public Builder setTitle(String title) {
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

		public Builder setListener(OnClickListener listener) {
			this.listener = listener;
			return this;
		}

		public Builder reset() {
			title = null;
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