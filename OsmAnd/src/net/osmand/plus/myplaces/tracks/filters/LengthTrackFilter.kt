package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.LENGTH

class LengthTrackFilter(filterChangedListener: FilterChangedListener)
	: RangeTrackFilter(R.string.routing_attr_length_name, LENGTH, filterChangedListener) {

	override val unitResId = R.string.km

	init {
		maxValue = 700000
		setValueTo(maxValue, false)
	}

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (enabled) {
			val length = trackItem.dataItem?.analysis?.totalDistance
			if (length == null || (length == 0f)) {
				return true
			}
			return length < getValueFrom() || length > getValueTo()
		}
		return false
	}
}