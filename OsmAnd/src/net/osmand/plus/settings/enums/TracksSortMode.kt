package net.osmand.plus.settings.enums

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.R
import net.osmand.shared.gpx.enums.TracksSortScope

enum class TracksSortMode(
	@get:StringRes val nameId: Int,
	@get:DrawableRes val iconId: Int,
	val scopes: Array<TracksSortScope>
) {
	NEAREST(
		nameId = R.string.shared_string_nearest,
		iconId = R.drawable.ic_action_nearby,
		scopes = arrayOf(TracksSortScope.TRACKS)
	),
	LAST_MODIFIED(
		nameId = R.string.sort_last_modified,
		iconId = R.drawable.ic_action_time,
		scopes = arrayOf(TracksSortScope.TRACKS)
	),
	NAME_ASCENDING(
		nameId = R.string.sort_name_ascending,
		iconId = R.drawable.ic_action_sort_by_name_ascending,
		scopes = arrayOf(TracksSortScope.TRACKS, TracksSortScope.ORGANIZED_BY_NAME)
	),
	NAME_DESCENDING(
		nameId = R.string.sort_name_descending,
		iconId = R.drawable.ic_action_sort_by_name_descending,
		scopes = arrayOf(TracksSortScope.TRACKS, TracksSortScope.ORGANIZED_BY_NAME)
	),
	VALUE_DESCENDING(
		nameId = R.string.sort_highest_first,
		iconId = R.drawable.ic_action_sort_long_to_short,
		scopes = arrayOf(TracksSortScope.ORGANIZED_BY_VALUE)
	),
	VALUE_ASCENDING(
		nameId = R.string.sort_lowest_first,
		iconId = R.drawable.ic_action_sort_short_to_long,
		scopes = arrayOf(TracksSortScope.ORGANIZED_BY_VALUE)
	),
	DATE_ASCENDING(
		nameId = R.string.sort_date_ascending,
		iconId = R.drawable.ic_action_sort_date_1,
		scopes = arrayOf(TracksSortScope.TRACKS)
	),
	DATE_DESCENDING(
		nameId = R.string.sort_date_descending,
		iconId = R.drawable.ic_action_sort_date_31,
		scopes = arrayOf(TracksSortScope.TRACKS)
	),
	DISTANCE_DESCENDING(
		nameId = R.string.sort_distance_descending,
		iconId = R.drawable.ic_action_sort_long_to_short,
		scopes = arrayOf(TracksSortScope.TRACKS)
	),
	DISTANCE_ASCENDING(
		nameId = R.string.sort_distance_ascending,
		iconId = R.drawable.ic_action_sort_short_to_long,
		scopes = arrayOf(TracksSortScope.TRACKS)
	),
	DURATION_DESCENDING(
		nameId = R.string.sort_duration_descending,
		iconId = R.drawable.ic_action_sort_duration_long_to_short,
		scopes = arrayOf(TracksSortScope.TRACKS)
	),
	DURATION_ASCENDING(
		nameId = R.string.sort_duration_ascending,
		iconId = R.drawable.ic_action_sort_duration_short_to_long,
		scopes = arrayOf(TracksSortScope.TRACKS)
	);

	fun isAllowedIn(scope: TracksSortScope): Boolean = scopes.contains(scope)

	companion object {

		private const val REC_FOLDER = "rec"
		private const val IMPORT_FOLDER = "import"

		@JvmStatic
		fun valuesOf(scope: TracksSortScope): Array<TracksSortMode> {
			return entries.filter { it.isAllowedIn(scope) }.toTypedArray()
		}

		@JvmStatic
		fun getByValue(name: String?): TracksSortMode {
			return entries.find { it.name == name } ?: getDefaultSortMode("")
		}

		@JvmStatic
		fun getValidOrDefault(
			sortEntryId: String?,
			scope: TracksSortScope,
			mode: TracksSortMode
		): TracksSortMode {
			return if (mode.isAllowedIn(scope)) mode else getDefaultSortMode(sortEntryId, scope)
		}

		@JvmStatic
		fun getDefaultSortMode(sortEntryId: String?): TracksSortMode {
			return getDefaultSortMode(sortEntryId, TracksSortScope.TRACKS)
		}

		@JvmStatic
		fun getDefaultSortMode(sortEntryId: String?, scope: TracksSortScope): TracksSortMode {
			return when (scope) {
				TracksSortScope.ORGANIZED_BY_VALUE -> VALUE_ASCENDING
				TracksSortScope.ORGANIZED_BY_NAME -> NAME_ASCENDING
				TracksSortScope.TRACKS -> {
					if (sortEntryId.isNullOrEmpty() || sortEntryId == REC_FOLDER || sortEntryId == IMPORT_FOLDER) {
						LAST_MODIFIED
					} else {
						NAME_ASCENDING
					}
				}
			}
		}
	}
}