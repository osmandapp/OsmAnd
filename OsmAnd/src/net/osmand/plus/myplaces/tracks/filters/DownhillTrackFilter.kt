package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.DOWNHILL

class DownhillTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(app, R.string.shared_string_downhill, DOWNHILL, filterChangedListener) {
	override val unitResId = R.string.m

	override fun isTrackAccepted(trackItem: TrackItem): Boolean { //*
		if (isEnabled()) {
			val elevation = trackItem.dataItem?.analysis?.diffElevationDown
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