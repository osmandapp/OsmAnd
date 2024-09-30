package net.osmand.shared.gpx.filters

import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil

@Serializable
class OtherTrackFilter : BaseTrackFilter {

	constructor(
		trackFilterType: TrackFilterType,
		filterChangedListener: FilterChangedListener?) : super(
		trackFilterType,
		filterChangedListener)

	@Serializable
	var selectedParams = ArrayList<OtherTrackParam>()

	val parameters = ArrayList<OtherTrackParam>()

	@Serializable
	private var isVisibleOnMap: Boolean = false

	@Serializable
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
		return !KAlgorithms.isEmpty(selectedParams)
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
				PlatformUtil.getOsmAndContext().isGpxFileVisible(trackItem.path)
			}

			OtherTrackParam.WITH_WAYPOINTS -> {
				val wptPointsCount = trackItem.dataItem?.getAnalysis()?.wptPoints ?: 0
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
			super.initWithValue(value)
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