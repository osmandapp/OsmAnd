package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.TIME_IN_MOTION

class TimeInMotionTrackFilter(
	minValue: Float,
	maxValue: Float,
	app: OsmandApplication,
	filterChangedListener: FilterChangedListener?) : RangeTrackFilter(
	minValue,
	maxValue,
	app, R.string.moving_time, TIME_IN_MOTION, filterChangedListener) {

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		val duration = trackItem.dataItem?.analysis?.timeMoving
		if (duration == null || (duration == 0L)) {
			return false
		}
		val durationMinutes = duration.toDouble() / 1000 / 60
		return durationMinutes > valueFrom && durationMinutes < valueTo
				|| durationMinutes < minValue && valueFrom == minValue
				|| durationMinutes > maxValue && valueTo == maxValue
	}
}