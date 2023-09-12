package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.AVERAGE_ALTITUDE

class AverageAltitudeTrackFilter(
	app: OsmandApplication,
	filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(app, R.string.average_altitude, AVERAGE_ALTITUDE, filterChangedListener) {
	override val unitResId = R.string.m

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
			val elevation = trackItem.dataItem?.analysis?.avgElevation
			if (elevation == null || (elevation == 0.0)) {
				return false
			}
			return elevation > valueFrom && elevation < valueTo
					|| elevation < minValue && valueFrom == minValue
					|| elevation > maxValue && valueTo == maxValue
		}
		return true
	}
}