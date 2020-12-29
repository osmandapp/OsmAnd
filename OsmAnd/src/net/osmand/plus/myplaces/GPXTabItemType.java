package net.osmand.plus.myplaces;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum GPXTabItemType {

	GPX_TAB_ITEM_GENERAL(R.string.shared_string_overview, R.drawable.ic_action_polygom_dark),
	GPX_TAB_ITEM_ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude_average),
	GPX_TAB_ITEM_SPEED(R.string.map_widget_speed, R.drawable.ic_action_speed);

	private final int iconId;
	private final int titleId;

	GPXTabItemType(@StringRes int titleId, @DrawableRes int iconId) {
		this.iconId = iconId;
		this.titleId = titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(titleId);
	}
}
