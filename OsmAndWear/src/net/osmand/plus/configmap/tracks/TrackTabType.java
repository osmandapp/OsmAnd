package net.osmand.plus.configmap.tracks;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum TrackTabType {

	ON_MAP(R.string.shared_string_on_map, R.drawable.ic_show_on_map),
	ALL(R.string.shared_string_all, R.drawable.ic_action_list_header),
	FOLDER(-1, R.drawable.ic_action_folder),
	SMART_FOLDER(-1, R.drawable.ic_action_folder_smart),
	FOLDERS(R.string.shared_string_folders, R.drawable.ic_action_folder);


	@DrawableRes
	public final int iconId;
	@StringRes
	public final int titleId;

	TrackTabType(@StringRes int titleId, @DrawableRes int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	public boolean shouldShowFolder() {
		return this == ON_MAP || this == ALL;
	}
}
