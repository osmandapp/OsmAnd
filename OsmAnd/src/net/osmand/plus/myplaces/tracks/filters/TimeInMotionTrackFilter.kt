package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.TIME_IN_MOTION

class TimeInMotionTrackFilter(
	app: OsmandApplication,
	filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(app, R.string.moving_time, TIME_IN_MOTION, filterChangedListener) {

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isEnabled()) {
			val duration = trackItem.dataItem?.analysis?.timeMoving
			if (duration == null || (duration == 0L)) {
				return false
			}
			val durationMinutes = duration.toDouble() / 1000 / 60
			return durationMinutes > getValueFrom() && durationMinutes < getValueTo()
					|| durationMinutes < minValue && getValueFrom() == minValue
					|| durationMinutes > maxValue && getValueTo() == maxValue
		}
		return false
	}
}