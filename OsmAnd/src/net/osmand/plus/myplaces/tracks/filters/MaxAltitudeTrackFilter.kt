package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.MAX_ALTITUDE

class MaxAltitudeTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(app, R.string.max_altitude, MAX_ALTITUDE, filterChangedListener) {
	override val unitResId = R.string.m

	init {
		maxValue = TrackFiltersConstants.ALTITUDE_MAX_VALUE
		setValueTo(maxValue, false)
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isEnabled()) {
			val elevation = trackItem.dataItem?.analysis?.maxElevation
			return if (elevation == null)
				false
			else
				elevation > getValueFrom() && elevation < getValueTo()
						|| elevation < minValue && getValueFrom() == minValue
						|| elevation > maxValue && getValueTo() == maxValue
		}
		return true
	}
}