package net.osmand.plus.gallery.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R

enum class GallerySortMode(
	@StringRes val titleId: Int,
	@DrawableRes val iconId: Int,
	val group: Group
) {
	NEAREST(R.string.gallery_sort_nearest, R.drawable.ic_action_nearby, Group.LOCATION),
	LAST_MODIFIED(R.string.gallery_sort_last_modified, R.drawable.ic_action_time, Group.LOCATION),
	NAME_A_Z(R.string.gallery_sort_name_a_z, R.drawable.ic_action_sort_by_name_ascending, Group.NAME),
	NAME_Z_A(R.string.gallery_sort_name_z_a, R.drawable.ic_action_sort_by_name_descending, Group.NAME),
	NEWEST_FIRST(R.string.gallery_sort_newest_first, R.drawable.ic_action_sort_date_1, Group.DATE),
	OLDEST_FIRST(R.string.gallery_sort_oldest_first, R.drawable.ic_action_sort_date_31, Group.DATE),
	SIZE_LARGE_SMALL(R.string.gallery_sort_size_large_small, R.drawable.ic_action_sort_big_to_small, Group.SIZE),
	SIZE_SMALL_LARGE(R.string.gallery_sort_size_small_large, R.drawable.ic_action_sort_small_to_big, Group.SIZE),
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
		SIZE,
		DURATION
	}
}
