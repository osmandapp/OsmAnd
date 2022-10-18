package net.osmand.plus.configmap;

import androidx.annotation.DrawableRes;

import net.osmand.plus.R;

public enum TrackTabType {

	ON_MAP(R.drawable.ic_show_on_map),
	ALL(R.drawable.ic_action_list_header),
	FOLDER(R.drawable.ic_action_folder),
	FILTER(R.drawable.ic_action_filter);

	@DrawableRes
	int iconId;

	TrackTabType(@DrawableRes int iconId) {
		this.iconId = iconId;
	}
}
