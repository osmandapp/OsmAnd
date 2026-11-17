package net.osmand.plus.myplaces.tracks.controller;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.util.CollectionUtils;

enum TrackFolderOption {
	DETAILS(R.string.shared_string_details, R.drawable.ic_action_info_dark),
	SHOW_ALL_TRACKS(R.string.show_all_tracks_on_the_map, R.drawable.ic_show_on_map),
	shared_string_rename(R.string.shared_string_rename, R.drawable.ic_action_edit_dark),
	CHANGE_APPEARANCE(R.string.change_default_appearance, R.drawable.ic_action_appearance),
	EXPORT(R.string.shared_string_export, R.drawable.ic_action_upload),
	MOVE(R.string.shared_string_move, R.drawable.ic_action_folder_move),
	DELETE_FOLDER(R.string.delete_folder, R.drawable.ic_action_delete_dark);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	TrackFolderOption(@StringRes int titleId, @DrawableRes int iconId) {
		this.titleId = titleId;
		this.iconId = iconId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	public boolean shouldShowBottomDivider() {
		return CollectionUtils.equalsToAny(this, SHOW_ALL_TRACKS, CHANGE_APPEARANCE, MOVE);
	}

	@NonNull
	public static TrackFolderOption[] getAvailableOptions() {
		return new TrackFolderOption[] {SHOW_ALL_TRACKS, shared_string_rename, CHANGE_APPEARANCE, EXPORT, DELETE_FOLDER};
	}
}
