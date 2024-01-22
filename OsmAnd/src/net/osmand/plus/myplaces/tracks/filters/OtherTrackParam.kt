package net.osmand.plus.myplaces.tracks.filters

import androidx.annotation.StringRes
import net.osmand.plus.R

enum class OtherTrackParam(@StringRes val displayName: Int) {
	VISIBLE_ON_MAP(R.string.shared_string_visible_on_map),
	WITH_WAYPOINTS(R.string.with_waypoints)
}