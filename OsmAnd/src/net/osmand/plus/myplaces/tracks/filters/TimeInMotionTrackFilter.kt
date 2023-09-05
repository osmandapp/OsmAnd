package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.TIME_IN_MOTION

class TimeInMotionTrackFilter(filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(R.string.moving_time, TIME_IN_MOTION, filterChangedListener) {

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val duration = trackItem.dataItem?.analysis?.timeMoving
			if (duration == null || (duration == 0L)) {
				return true
			}
			val durationMinutes = duration.toDouble() / 1000 / 60
			return durationMinutes < getValueFrom() || durationMinutes > getValueTo()
		}
		return false
	}
}