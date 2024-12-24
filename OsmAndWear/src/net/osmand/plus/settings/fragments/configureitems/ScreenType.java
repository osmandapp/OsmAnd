package net.osmand.plus.settings.fragments.configureitems;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum ScreenType {

	DRAWER(R.string.shared_string_drawer, R.drawable.ic_action_drawer, R.drawable.img_settings_customize_drawer_day, R.drawable.img_settings_customize_drawer_night),
	CONFIGURE_MAP(R.string.configure_map, R.drawable.ic_action_layers, R.drawable.img_settings_customize_configure_map_day, R.drawable.img_settings_customize_configure_map_night),
	CONTEXT_MENU_ACTIONS(R.string.context_menu_actions, R.drawable.ic_action_context_menu, R.drawable.img_settings_customize_context_menu_day, R.drawable.img_settings_customize_context_menu_night);

	@StringRes
	public final int titleId;
	@DrawableRes
	public final int iconId;
	@DrawableRes
	public final int imageDayId;
	@DrawableRes
	public final int imageNightId;

	ScreenType(@StringRes int titleId, @DrawableRes int iconId, @DrawableRes int imageDayId, @DrawableRes int imageNightId) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.imageDayId = imageDayId;
		this.imageNightId = imageNightId;
	}
}
