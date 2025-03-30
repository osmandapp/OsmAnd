package net.osmand.plus.settings.enums;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum GridLabelsPosition implements EnumWithTitleId {

	EDGES(R.drawable.ic_action_grid_label_edges, R.string.shared_string_edges),
	CENTER(R.drawable.ic_action_grid_label_center, R.string.position_on_map_center);

	@DrawableRes
	private final int iconId;

	@StringRes
	private final int titleId;

	GridLabelsPosition(@DrawableRes int iconId, @StringRes int titleId) {
		this.iconId = iconId;
		this.titleId = titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@Override
	@StringRes
	public int getTitleId() {
		return titleId;
	}
}

