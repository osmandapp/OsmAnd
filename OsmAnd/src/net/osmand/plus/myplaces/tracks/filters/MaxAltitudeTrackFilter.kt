package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.MAX_ALTITUDE

class MaxAltitudeTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(app, R.string.max_altitude, MAX_ALTITUDE, filterChangedListener) {
	override val unitResId = R.string.m

	init {
		maxValue = 12000f
	}

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val elevation = trackItem.dataItem?.analysis?.maxElevation
			return if (elevation == null)
				true
			else
				elevation < getValueFrom() || elevation > getValueTo()
		}
		return false
	}
}