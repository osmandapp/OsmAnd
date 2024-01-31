package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.util.Algorithms

class OtherTrackFilter(
	val app: OsmandApplication,
	trackFilterType: TrackFilterType,
	filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(trackFilterType, filterChangedListener) {

	@Expose
	var selectedParams = ArrayList<OtherTrackParam>()

	val parameters = ArrayList<OtherTrackParam>()

	//for migration purposes only
	@Expose
	private var isVisibleOnMap: Boolean = false

	@Expose
	private var hasWaypoints: Boolean = false

	init {
		if (trackFilterType.additionalData is List<*>) {
			for (nameResId in trackFilterType.additionalData) {
				if (nameResId is OtherTrackParam) {
					parameters.add(nameResId)
				} else {
					throw IllegalArgumentException("$trackFilterType's additionalParams should contain list of NonDbTrackParam elements")
				}
			}
		}
	}

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedParams)
	}

	fun isParamSelected(param: OtherTrackParam): Boolean {
		return selectedParams.contains(param)
	}

	fun setItemSelected(param: OtherTrackParam, selected: Boolean) {
		val newList = ArrayList(selectedParams)
		if (selected) {
			if (!newList.contains(param)) {
				newList.add(param)
			}
		} else {
			newList.remove(param)
		}
		selectedParams = newList
		filterChangedListener?.onFilterChanged()
	}

	private fun isTrackParamAccepted(trackItem: TrackItem, param: OtherTrackParam): Boolean {
		return when (param) {
			OtherTrackParam.VISIBLE_ON_MAP -> {
				val selectedGpxHelper = app.selectedGpxHelper
				selectedGpxHelper.getSelectedFileByPath(trackItem.path) != null
			}

			OtherTrackParam.WITH_WAYPOINTS -> {
				val wptPointsCount = trackItem.dataItem?.analysis?.wptPoints ?: 0
				wptPointsCount != 0
			}
		}
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		for (parameter in selectedParams) {
			if (!isTrackParamAccepted(trackItem, parameter)) {
				return false
			}
		}
		return true
	}

	fun getSelectedParamsCount(): Int {
		return selectedParams.size
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is OtherTrackFilter) {
			if (value.selectedParams != null) {
				selectedParams = ArrayList(value.selectedParams)
			} else {
				selectedParams = ArrayList()
				if (value.isVisibleOnMap == true) {
					selectedParams.add(OtherTrackParam.VISIBLE_ON_MAP)
				}
				if (value.hasWaypoints == true) {
					selectedParams.add(OtherTrackParam.WITH_WAYPOINTS)
				}
			}
			filterChangedListener?.onFilterChanged()
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is OtherTrackFilter &&
				other.trackFilterType == trackFilterType &&
				selectedParams.size == other.selectedParams.size &&
				selectedParams.containsAll(other.selectedParams)
	}

	override fun hashCode(): Int {
		return selectedParams.hashCode()
	}

}