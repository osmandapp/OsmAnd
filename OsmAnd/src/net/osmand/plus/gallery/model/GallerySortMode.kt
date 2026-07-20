package net.osmand.plus.gallery.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R

enum class GallerySortMode(
	@StringRes val titleId: Int,
	@DrawableRes val iconId: Int,
	val group: Group
) {
	NEAREST(R.string.gallery_sort_nearest, R.drawable.ic_action_nearest_map_center, Group.LOCATION),
	LAST_MODIFIED(R.string.gallery_sort_last_modified, R.drawable.ic_action_sort_by_date, Group.LOCATION),
	NAME_A_Z(R.string.gallery_sort_name_a_z, R.drawable.ic_action_sort_by_name_ascending, Group.NAME),
	NAME_Z_A(R.string.gallery_sort_name_z_a, R.drawable.ic_action_sort_by_name_descending, Group.NAME),
	NEWEST_FIRST(R.string.gallery_sort_newest_first, R.drawable.ic_action_sort_date_31, Group.DATE),
	OLDEST_FIRST(R.string.gallery_sort_oldest_first, R.drawable.ic_action_sort_date_1, Group.DATE),
	DURATION_LONG_SHORT(
		R.string.gallery_sort_duration_long_short,
		R.drawable.ic_action_sort_duration_long_to_short,
		Group.DURATION
	),
	DURATION_SHORT_LONG(
		R.string.gallery_sort_duration_short_long,
		R.drawable.ic_action_sort_duration_short_to_long,
		Group.DURATION
	);

	enum class Group {
		LOCATION,
		NAME,
		DATE,
		DURATION
	}
}
