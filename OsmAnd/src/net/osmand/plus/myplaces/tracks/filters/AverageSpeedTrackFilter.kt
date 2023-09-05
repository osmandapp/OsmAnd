package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.AVERAGE_SPEED

class AverageSpeedTrackFilter(filterChangedListener: FilterChangedListener)
	: RangeTrackFilter(R.string.average_speed, AVERAGE_SPEED, filterChangedListener) {
	override val unitResId = R.string.km_h

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val duration = trackItem.dataItem?.analysis?.avgSpeed
			if (duration == null || (duration == 0f)) {
				return true
			}
			val durationMinutes = duration.toDouble() / 1000 / 60
			return durationMinutes < getValueFrom() || durationMinutes > getValueTo()
		}
		return false
	}
}