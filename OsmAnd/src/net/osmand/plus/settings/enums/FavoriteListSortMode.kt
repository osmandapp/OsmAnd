package net.osmand.plus.settings.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R

enum class FavoriteListSortMode(
	@get:StringRes val nameId: Int,
	@get:DrawableRes val iconId: Int
) {

    LAST_MODIFIED(
        nameId = R.string.sort_last_modified,
        iconId = R.drawable.ic_action_time,
    ),
    NAME_ASCENDING(
        nameId = R.string.sort_name_ascending,
        iconId = R.drawable.ic_action_sort_by_name_ascending,
    ),
    NAME_DESCENDING(
        nameId = R.string.sort_name_descending,
        iconId = R.drawable.ic_action_sort_by_name_descending,
    ),
    NEAREST(
        nameId = R.string.distance_nearest,
        iconId = R.drawable.ic_action_nearby,
    ),
    FARTHEST(
        nameId = R.string.distance_farthest,
        iconId = R.drawable.ic_action_nearby,
    ),
    DATE_ASCENDING(
        nameId = R.string.sort_date_ascending,
        iconId = R.drawable.ic_action_sort_date_1,
    ),
    DATE_DESCENDING(
        nameId = R.string.sort_date_descending,
        iconId = R.drawable.ic_action_sort_date_31,
    );

    companion object {

        @JvmStatic
        fun getByValue(name: String?): FavoriteListSortMode {
            return entries.find { it.name == name } ?: getDefaultSortMode()
        }

        @JvmStatic
        fun getDefaultSortMode(): FavoriteListSortMode {
            return NAME_ASCENDING
        }

        @JvmStatic
        fun getSortModes(includeDistance: Boolean): Array<FavoriteListSortMode> {
            return if (includeDistance) {
                entries
                    .filter { it != LAST_MODIFIED }
                    .toTypedArray()
            } else {
                entries
                    .filter { it != NEAREST && it != FARTHEST }
                    .toTypedArray()
            }
        }
    }
}