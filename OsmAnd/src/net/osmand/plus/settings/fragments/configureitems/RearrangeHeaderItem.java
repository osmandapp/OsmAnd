package net.osmand.plus.settings.fragments.configureitems;

import androidx.annotation.StringRes;

public class RearrangeHeaderItem {

	@StringRes
	protected final int titleId;
	@StringRes
	protected final int descriptionId;

	public RearrangeHeaderItem(@StringRes int titleId, @StringRes int descriptionId) {
		this.titleId = titleId;
		this.descriptionId = descriptionId;
	}
}
