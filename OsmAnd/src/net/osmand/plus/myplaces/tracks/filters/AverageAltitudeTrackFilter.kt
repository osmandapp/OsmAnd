package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.AVERAGE_ALTITUDE

class AverageAltitudeTrackFilter(
	minValue: Float,
	maxValue: Float,
	app: OsmandApplication,
	filterChangedListener: FilterChangedListener?) : RangeTrackFilter(
	minValue,
	maxValue,
	app, R.string.average_altitude, AVERAGE_ALTITUDE, filterChangedListener) {
	override val unitResId = R.string.m

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		val elevation = trackItem.dataItem?.analysis?.avgElevation
		if (elevation == null || (elevation == 0.0)) {
			return false
		}
		return elevation > valueFrom && elevation < valueTo
				|| elevation < minValue && valueFrom == minValue
				|| elevation > maxValue && valueTo == maxValue
	}
}