package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum Map3DModeVisibility {

	HIDDEN(R.string.shared_string_hidden, R.drawable.ic_action_button_3d_off),
	VISIBLE(R.string.shared_string_visible, R.drawable.ic_action_button_3d),
	VISIBLE_IN_3D_MODE(R.string.visible_in_3d_mode, R.drawable.ic_action_button_3d);

	@StringRes
	public final int titleId;
	@DrawableRes
	public final int iconId;

	Map3DModeVisibility(@StringRes int titleId, @DrawableRes int iconId) {
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
