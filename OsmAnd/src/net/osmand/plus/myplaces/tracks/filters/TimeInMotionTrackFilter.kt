package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.TIME_IN_MOTION

class TimeInMotionTrackFilter(
	app: OsmandApplication,
	filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(app, R.string.moving_time, TIME_IN_MOTION, filterChangedListener) {

	@Expose
	override var minValue: Float = 0f

	@Expose
	override var maxValue: Float = TrackFiltersConstants.DEFAULT_MAX_VALUE

	@Expose
	override var valueFrom: Float = minValue

	@Expose
	override var valueTo: Float = maxValue


	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isEnabled()) {
			val duration = trackItem.dataItem?.analysis?.timeMoving
			if (duration == null || (duration == 0L)) {
				return false
			}
			val durationMinutes = duration.toDouble() / 1000 / 60
			return durationMinutes > valueFrom && durationMinutes < valueTo
					|| durationMinutes < minValue && valueFrom == minValue
					|| durationMinutes > maxValue && valueTo == maxValue
		}
		return false
	}
}