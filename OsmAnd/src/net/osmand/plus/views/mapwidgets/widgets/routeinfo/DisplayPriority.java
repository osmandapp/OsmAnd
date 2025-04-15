package net.osmand.plus.views.mapwidgets.widgets.routeinfo;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum DisplayPriority {
	INTERMEDIATE_FIRST(R.drawable.ic_action_intermediate_destination, R.string.intermediate_first),
	DESTINATION_FIRST(R.drawable.ic_action_point_destination, R.string.destination_first);

	@DrawableRes
	private final int iconId;
	@StringRes
	private final int titleId;

	DisplayPriority(@DrawableRes int iconId, @StringRes int titleId) {
		this.iconId = iconId;
		this.titleId = titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}
}