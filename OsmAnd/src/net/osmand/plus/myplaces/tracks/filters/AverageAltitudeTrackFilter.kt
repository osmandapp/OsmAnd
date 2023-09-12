package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.AVERAGE_ALTITUDE

class AverageAltitudeTrackFilter(
	app: OsmandApplication,
	filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(app, R.string.average_altitude, AVERAGE_ALTITUDE, filterChangedListener) {
	override val unitResId = R.string.m

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isEnabled()) {
			val elevation = trackItem.dataItem?.analysis?.avgElevation
			if (elevation == null || (elevation == 0.0)) {
				return false
			}
			return elevation > getValueFrom() && elevation < getValueTo()
					|| elevation < minValue && getValueFrom() == minValue
					|| elevation > maxValue && getValueTo() == maxValue
		}
		return true
	}
}