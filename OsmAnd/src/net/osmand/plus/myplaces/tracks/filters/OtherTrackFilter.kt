package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.OTHER

class OtherTrackFilter(val app: OsmandApplication, filterChangedListener: FilterChangedListener) :
	BaseTrackFilter(R.string.shared_string_other, OTHER, filterChangedListener) {

	override var enabled: Boolean = false
		get() {
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

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		if (isVisibleOnMap) {
			val selectedGpxHelper = app.selectedGpxHelper
			selectedGpxHelper.getSelectedFileByPath(trackItem.path) ?: return true
		}
		if (hasWaypoints) {
			if (trackItem.dataItem?.analysis?.wptPoints == 0) {
				return true
			}
		}
		return false
	}

	fun getSelectedParamsCount(): Int {
		var selectedCount = 0;
		if (isVisibleOnMap) selectedCount++
		if (hasWaypoints) selectedCount++
		return selectedCount
	}
}