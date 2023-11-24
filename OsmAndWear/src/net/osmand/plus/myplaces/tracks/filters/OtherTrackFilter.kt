package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.OTHER

class OtherTrackFilter(val app: OsmandApplication, filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(R.string.shared_string_other, OTHER, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return isVisibleOnMap || hasWaypoints
	}

	@Expose
	var isVisibleOnMap: Boolean = false
		set(value) {
			field = value
			filterChangedListener?.onFilterChanged()
		}

	@Expose
	var hasWaypoints: Boolean = false
		set(value) {
			field = value
			filterChangedListener?.onFilterChanged()
		}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isVisibleOnMap) {
			val selectedGpxHelper = app.selectedGpxHelper
			if (selectedGpxHelper.getSelectedFileByPath(trackItem.path) == null) {
				return false
			}
		}
		if (hasWaypoints) {
			val wptPointsCount = trackItem.dataItem?.analysis?.wptPoints ?: 0
			if (wptPointsCount == 0) {
				return false
			}
		}
		return true
	}

	fun getSelectedParamsCount(): Int {
		var selectedCount = 0;
		if (isVisibleOnMap) selectedCount++
		if (hasWaypoints) selectedCount++
		return selectedCount
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is OtherTrackFilter) {
			isVisibleOnMap = value.isVisibleOnMap
			hasWaypoints = value.hasWaypoints
			filterChangedListener?.onFilterChanged()
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is OtherTrackFilter &&
				other.isVisibleOnMap == isVisibleOnMap &&
				other.hasWaypoints == hasWaypoints
	}

}