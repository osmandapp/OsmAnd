package net.osmand.plus.myplaces.tracks.controller

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R
import net.osmand.util.Algorithms

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
	EDIT_NAME(
		R.string.edit_name,
		R.drawable.ic_action_edit_dark),
	REFRESH(
		R.string.shared_string_refresh,
		R.drawable.ic_action_update),
	CHANGE_APPEARANCE(
		R.string.change_default_appearance,
		R.drawable.ic_action_appearance),
	EXPORT(
		R.string.shared_string_export,
		R.drawable.ic_action_upload),
	DELETE_FOLDER(R.string.delete_folder, R.drawable.ic_action_delete_dark);

	fun shouldShowBottomDivider(): Boolean {
		return Algorithms.equalsToAny(this, SHOW_ALL_TRACKS, CHANGE_APPEARANCE)
	}

	companion object {
		val availableOptions: Array<SmartFolderOption>
			get() = arrayOf(
				DETAILS,
				SHOW_ALL_TRACKS,
				EDIT_NAME,
				REFRESH,
				CHANGE_APPEARANCE,
				EXPORT,
				DELETE_FOLDER)
	}
}