package net.osmand.plus.keyevent;

import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum AssignmentsCategory {

	ACTIONS(R.string.key_event_category_actions),
	MAP_INTERACTIONS(R.string.key_event_category_map_interactions);

	@StringRes
	private final int titleId;

	AssignmentsCategory(@StringRes int titleId) {
		this.titleId = titleId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}
}
