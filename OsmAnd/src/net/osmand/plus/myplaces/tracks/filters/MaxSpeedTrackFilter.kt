package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.MAX_SPEED

class MaxSpeedTrackFilter(filterChangedListener: FilterChangedListener)
	: RangeTrackFilter(R.string.max_speed, MAX_SPEED, filterChangedListener) {

	override val unitResId = R.string.km_h

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val duration = trackItem.dataItem?.analysis?.maxSpeed
			if (duration == null || (duration == 0f)) {
				return true
			}
			val durationMinutes = duration.toDouble() / 1000 / 60
			return durationMinutes < getValueFrom() || durationMinutes > getValueTo()
		}
		return false
	}

}