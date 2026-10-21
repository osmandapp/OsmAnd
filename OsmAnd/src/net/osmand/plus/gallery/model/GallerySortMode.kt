package net.osmand.plus.gallery.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R
import net.osmand.shared.media.library.MediaLibrarySortMode

enum class GallerySortMode(
	@StringRes val titleId: Int,
	@DrawableRes val iconId: Int,
	val shared: MediaLibrarySortMode
) {
	NEAREST(R.string.gallery_sort_nearest, R.drawable.ic_action_nearby, MediaLibrarySortMode.NEAREST),
	LAST_MODIFIED(R.string.gallery_sort_last_modified, R.drawable.ic_action_time, MediaLibrarySortMode.LAST_MODIFIED),
	NAME_A_Z(R.string.gallery_sort_name_a_z, R.drawable.ic_action_sort_by_name_ascending, MediaLibrarySortMode.NAME_A_Z),
	NAME_Z_A(R.string.gallery_sort_name_z_a, R.drawable.ic_action_sort_by_name_descending, MediaLibrarySortMode.NAME_Z_A),
	NEWEST_FIRST(R.string.gallery_sort_newest_first, R.drawable.ic_action_sort_date_1, MediaLibrarySortMode.NEWEST_FIRST),
	OLDEST_FIRST(R.string.gallery_sort_oldest_first, R.drawable.ic_action_sort_date_31, MediaLibrarySortMode.OLDEST_FIRST),
	SIZE_LARGE_SMALL(R.string.gallery_sort_size_large_small, R.drawable.ic_action_sort_big_to_small, MediaLibrarySortMode.SIZE_LARGE_SMALL),
	SIZE_SMALL_LARGE(R.string.gallery_sort_size_small_large, R.drawable.ic_action_sort_small_to_big, MediaLibrarySortMode.SIZE_SMALL_LARGE),
	DURATION_LONG_SHORT(
		R.string.gallery_sort_duration_long_short,
		R.drawable.ic_action_sort_duration_long_to_short,
		MediaLibrarySortMode.DURATION_LONG_SHORT
	),
	DURATION_SHORT_LONG(
		R.string.gallery_sort_duration_short_long,
		R.drawable.ic_action_sort_duration_short_to_long,
		MediaLibrarySortMode.DURATION_SHORT_LONG
	);

	val group: MediaLibrarySortMode.Group get() = shared.group
}
