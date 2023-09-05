package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.AVERAGE_ALTITUDE

class AverageAltitudeTrackFilter(filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(R.string.average_altitude, AVERAGE_ALTITUDE, filterChangedListener) {
	override val unitResId = R.string.m

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val duration = trackItem.dataItem?.analysis?.avgElevation
			if (duration == null || (duration == 0.0)) {
				return true
			}
			val durationMinutes = duration.toDouble() / 1000 / 60
			return durationMinutes < getValueFrom() || durationMinutes > getValueTo()
		}
		return false
	}
}