package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.UPHILL

class UphillTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener) :
	RangeTrackFilter(app, R.string.shared_string_uphill, UPHILL, filterChangedListener) {
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
			val elevation = trackItem.dataItem?.analysis?.diffElevationUp
			return if (elevation == null)
				false
			else
				elevation > valueFrom && elevation < valueTo
						|| elevation < minValue && valueFrom == minValue
						|| elevation > maxValue && valueTo == maxValue
		}
		return false
	}
}