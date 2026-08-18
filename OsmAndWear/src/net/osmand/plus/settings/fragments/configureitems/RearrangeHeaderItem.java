package net.osmand.plus.settings.fragments.configureitems;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class RearrangeHeaderItem {

	@StringRes
	public final int titleId;
	public final String description;

	public RearrangeHeaderItem(@StringRes int titleId, @Nullable String description) {
		this.titleId = titleId;
		this.description = description;
	}
}
