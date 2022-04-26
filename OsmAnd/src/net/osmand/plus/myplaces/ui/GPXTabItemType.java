package net.osmand.plus.myplaces.ui;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum GPXTabItemType {

	GPX_TAB_ITEM_GENERAL(R.string.shared_string_overview, R.drawable.ic_action_polygom_dark, R.layout.gpx_item_general),
	GPX_TAB_ITEM_ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude_average, R.layout.gpx_item_altitude),
	GPX_TAB_ITEM_SPEED(R.string.map_widget_current_speed, R.drawable.ic_action_speed, R.layout.gpx_item_speed),
	GPX_TAB_ITEM_NO_ALTITUDE(R.string.altitude, R.drawable.ic_action_desert, R.layout.gpx_item_no_altitude);

	private final int iconId;
	private final int titleId;
	private final int layoutId;

	GPXTabItemType(@StringRes int titleId, @DrawableRes int iconId, @LayoutRes int layoutId) {
		this.iconId = iconId;
		this.titleId = titleId;
		this.layoutId = layoutId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@LayoutRes
	public int getLayoutId() {
		return layoutId;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(titleId);
	}
}
