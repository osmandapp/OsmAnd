package net.osmand.plus.settings.fragments.configureitems;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class RearrangeButtonItem {

	@StringRes
	public final int titleId;
	@DrawableRes
	public final int iconId;
	@Nullable
	public final View.OnClickListener listener;

	public RearrangeButtonItem(@StringRes int titleId, @DrawableRes int iconId, @Nullable View.OnClickListener listener) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.listener = listener;
	}
}
