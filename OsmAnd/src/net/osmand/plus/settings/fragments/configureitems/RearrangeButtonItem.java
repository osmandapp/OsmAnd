package net.osmand.plus.settings.fragments.configureitems;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class RearrangeButtonItem {

	@StringRes
	protected final int titleId;
	@DrawableRes
	protected final int iconId;
	@Nullable
	protected final View.OnClickListener listener;

	public RearrangeButtonItem(@StringRes int titleId, @DrawableRes int iconId, @Nullable View.OnClickListener listener) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.listener = listener;
	}
}
