package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.DOWNHILL

class DownhillTrackFilter(
	minValue: Float,
	maxValue: Float,
	app: OsmandApplication, filterChangedListener: FilterChangedListener?) : RangeTrackFilter(
	minValue,
	maxValue,
	app, R.string.shared_string_downhill, DOWNHILL, filterChangedListener) {
	override val unitResId = R.string.m

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isEnabled()) {
			val elevation = trackItem.dataItem?.analysis?.diffElevationDown
			return if (elevation == null)
				false
			else
				elevation > valueFrom && elevation < valueTo
						|| elevation < minValue && valueFrom == minValue
						|| elevation > maxValue && valueTo == maxValue
		}
		return true
	}
}