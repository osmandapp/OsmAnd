package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.util.Algorithms

class OtherTrackFilter(
	val app: OsmandApplication,
	filterType: FilterType,
	filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(filterType, filterChangedListener) {

	@Expose
	var selectedParams = ArrayList<NonDbTrackParam>()

	val parameters = ArrayList<NonDbTrackParam>()

	init {
		if (filterType.additionalData is List<*>) {
			for (nameResId in filterType.additionalData) {
				if (nameResId is NonDbTrackParam) {
					parameters.add(nameResId)
				} else {
					throw IllegalArgumentException("$filterType's additionalParams should contain list of NonDbTrackParam elements")
				}
			}
		}
	}

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedParams)
	}

	fun isParamSelected(param: NonDbTrackParam): Boolean {
		return selectedParams.contains(param)
	}

	fun setItemSelected(param: NonDbTrackParam, selected: Boolean) {
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

	private fun isTrackParamAccepted(trackItem: TrackItem, param: NonDbTrackParam): Boolean {
		return when (param) {
			NonDbTrackParam.VISIBLE_ON_MAP -> {
				val selectedGpxHelper = app.selectedGpxHelper
				selectedGpxHelper.getSelectedFileByPath(trackItem.path) != null
			}

			NonDbTrackParam.WITH_WAYPOINTS -> {
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
			selectedParams = ArrayList(value.selectedParams)
			filterChangedListener?.onFilterChanged()
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is OtherTrackFilter &&
				other.filterType == filterType &&
				selectedParams.size == other.selectedParams.size &&
				selectedParams.containsAll(other.selectedParams)
	}

	override fun hashCode(): Int {
		return selectedParams.hashCode()
	}

}