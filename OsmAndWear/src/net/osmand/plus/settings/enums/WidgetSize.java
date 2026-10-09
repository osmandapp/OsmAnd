package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum WidgetSize {

	SMALL(R.string.rendering_value_small_name, R.drawable.ic_action_item_size_s),
	MEDIUM(R.string.rendering_value_medium_w_name, R.drawable.ic_action_item_size_m),
	LARGE(R.string.shared_string_large, R.drawable.ic_action_item_size_l);

	@StringRes
	public final int titleId;
	@DrawableRes
	public final int iconId;

	WidgetSize(@StringRes int titleId, @DrawableRes int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}
}
