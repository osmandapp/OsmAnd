package net.osmand.plus.myplaces.tracks.controller

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R
import net.osmand.util.CollectionUtils

internal enum class OrganizedTracksOption(
	@field:StringRes @get:StringRes @param:StringRes val titleId: Int,
	@field:DrawableRes @get:DrawableRes @param:DrawableRes val iconId: Int
) {
	DETAILS(
		R.string.shared_string_details,
		R.drawable.ic_action_info_dark
	),
	SHOW_ALL_TRACKS(
		R.string.show_all_tracks_on_the_map,
		R.drawable.ic_show_on_map
	),
	EXPORT(
		R.string.shared_string_export,
		R.drawable.ic_action_upload
	);

	fun shouldShowBottomDivider(): Boolean {
		return CollectionUtils.equalsToAny(this, SHOW_ALL_TRACKS)
	}
}