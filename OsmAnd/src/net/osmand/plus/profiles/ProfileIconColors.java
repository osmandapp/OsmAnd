package net.osmand.plus.profiles;

import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;

import net.osmand.plus.R;

public enum ProfileIconColors {
	DEFAULT(R.string.rendering_value_default_name, R.color.profile_icon_color_blue_light_default, R.color.profile_icon_color_blue_dark_default),
	PURPLE(R.string.rendering_value_purple_name, R.color.profile_icon_color_purple_light, R.color.profile_icon_color_purple_dark),
	GREEN(R.string.rendering_value_green_name, R.color.profile_icon_color_green_light, R.color.profile_icon_color_green_dark),
	BLUE(R.string.rendering_value_blue_name, R.color.profile_icon_color_blue_light, R.color.profile_icon_color_blue_dark),
	RED(R.string.rendering_value_red_name, R.color.profile_icon_color_red_light, R.color.profile_icon_color_red_dark),
	DARK_YELLOW(R.string.rendering_value_darkyellow_name, R.color.profile_icon_color_yellow_light, R.color.profile_icon_color_yellow_dark),
	MAGENTA(R.string.shared_string_color_magenta, R.color.profile_icon_color_magenta_light, R.color.profile_icon_color_magenta_dark);

	@StringRes
	private int name;
	@ColorRes
	private int dayColor;
	@ColorRes
	private int nightColor;

	ProfileIconColors(@StringRes int name, @ColorRes int dayColor, @ColorRes int nightColor) {
		this.name = name;
		this.dayColor = dayColor;
		this.nightColor = nightColor;
	}

	public int getName() {
		return name;
	}

	public int getColor(boolean nightMode) {
		return nightMode ? nightColor : dayColor;
	}
}