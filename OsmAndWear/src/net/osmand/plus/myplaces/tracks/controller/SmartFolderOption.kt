package net.osmand.plus.myplaces.tracks.controller

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R
import net.osmand.util.Algorithms
import net.osmand.util.CollectionUtils

internal enum class SmartFolderOption(
	@field:StringRes @get:StringRes
	@param:StringRes val titleId: Int, @field:DrawableRes @get:DrawableRes
	@param:DrawableRes val iconId: Int) {
	DETAILS(
		R.string.shared_string_details,
		R.drawable.ic_action_info_dark),
	SHOW_ALL_TRACKS(
		R.string.show_all_tracks_on_the_map,
		R.drawable.ic_show_on_map),
	shared_string_rename(
		R.string.shared_string_rename,
		R.drawable.ic_action_edit_dark),
	REFRESH(
		R.string.shared_string_refresh,
		R.drawable.ic_action_update),
	EDIT_FILTER(
		R.string.edit_filter,
		R.drawable.ic_action_filter_dark),
	EXPORT(
		R.string.shared_string_export,
		R.drawable.ic_action_upload),
	DELETE_FOLDER(R.string.delete_folder, R.drawable.ic_action_delete_dark);

	fun shouldShowBottomDivider(): Boolean {
		return CollectionUtils.equalsToAny(this, SHOW_ALL_TRACKS, EDIT_FILTER, EXPORT)
	}

	companion object {
		val availableOptions: Array<SmartFolderOption>
			get() = arrayOf(
				DETAILS,
				SHOW_ALL_TRACKS,
				shared_string_rename,
				REFRESH,
				EDIT_FILTER,
				EXPORT,
				DELETE_FOLDER)
	}
}