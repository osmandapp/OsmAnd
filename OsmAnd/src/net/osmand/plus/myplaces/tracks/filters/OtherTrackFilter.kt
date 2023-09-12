package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.OTHER

class OtherTrackFilter(val app: OsmandApplication, filterChangedListener: FilterChangedListener) :
	BaseTrackFilter(R.string.shared_string_other, OTHER, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return isVisibleOnMap || hasWaypoints
	}
	@Expose
	var isVisibleOnMap: Boolean = false
		set(value) {
			field = value
			filterChangedListener.onFilterChanged()
		}

	@Expose
	var hasWaypoints: Boolean = false
		set(value) {
			field = value
			filterChangedListener.onFilterChanged()
		}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isVisibleOnMap) {
			val selectedGpxHelper = app.selectedGpxHelper
			if(selectedGpxHelper.getSelectedFileByPath(trackItem.path) != null){
				return true
			}
		}
		if (hasWaypoints) {
			val wptPointsCount = trackItem.dataItem?.analysis?.wptPoints ?: 0
			if (wptPointsCount > 0) {
				return true
			}
		}
		return !isVisibleOnMap && !hasWaypoints
	}

	fun getSelectedParamsCount(): Int {
		var selectedCount = 0;
		if (isVisibleOnMap) selectedCount++
		if (hasWaypoints) selectedCount++
		return selectedCount
	}
}