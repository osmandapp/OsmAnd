package net.osmand.plus.myplaces.tracks;

import android.content.Context;

import androidx.annotation.LayoutRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum GPXTabItemType {

	GPX_TAB_ITEM_GENERAL(R.string.shared_string_overview, R.layout.gpx_item_general),
	GPX_TAB_ITEM_ALTITUDE(R.string.altitude, R.layout.gpx_item_altitude),
	GPX_TAB_ITEM_SPEED(R.string.shared_string_speed, R.layout.gpx_item_speed),
	GPX_TAB_ITEM_NO_ALTITUDE(R.string.altitude, R.layout.gpx_item_no_altitude);

	private final int titleId;
	private final int layoutId;

	GPXTabItemType(@StringRes int titleId, @LayoutRes int layoutId) {
		this.titleId = titleId;
		this.layoutId = layoutId;
	}

	@LayoutRes
	public int getLayoutId() {
		return layoutId;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(titleId);
	}
}
