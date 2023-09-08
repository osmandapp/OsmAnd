package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.DURATION

class DurationTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener)
	: RangeTrackFilter(app, R.string.duration, DURATION, filterChangedListener) {
	override val unitResId = R.string.shared_string_minute_lowercase

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val duration = trackItem.dataItem?.analysis?.timeSpan
			if (duration == null || (duration == 0L)) {
				return true
			}
			val durationMinutes = duration.toDouble() / 1000 / 60
			return durationMinutes < getValueFrom() || durationMinutes > getValueTo()
		}
		return false
	}
}