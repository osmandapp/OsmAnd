package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.osmand.plus.configmap.tracks.TrackItem

abstract class BaseTrackFilter(
	@Expose
	@SerializedName("filterType") val trackFilterType: TrackFilterType,
	var filterChangedListener: FilterChangedListener?) {

	abstract fun isEnabled(): Boolean

	abstract fun isTrackAccepted(trackItem: TrackItem): Boolean

	open fun initWithValue(value: BaseTrackFilter) {
		filterChangedListener?.onFilterChanged()
	}

	open fun isValid(): Boolean {
		return true
	}

	open fun initFilter() {}

	override fun equals(other: Any?): Boolean {
		return other is BaseTrackFilter && other.trackFilterType == trackFilterType
	}

	override fun hashCode(): Int {
		var result = trackFilterType.hashCode()
		result = 31 * result + (filterChangedListener?.hashCode() ?: 0)
		return result
	}

}