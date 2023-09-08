package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.UPHILL

class UphillTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(app, R.string.shared_string_uphill, UPHILL, filterChangedListener) {
	override val unitResId = R.string.m

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val elevation = trackItem.dataItem?.analysis?.diffElevationUp
			return if(elevation == null)
				true
			else
				elevation < getValueFrom() || elevation > getValueTo()
		}
		return false
	}
}