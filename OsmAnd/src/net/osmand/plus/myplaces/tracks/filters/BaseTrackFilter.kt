package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.osmand.plus.configmap.tracks.TrackItem

abstract class BaseTrackFilter(
	val displayNameId: Int,
	@Expose
	@SerializedName("filterType") val filterType: FilterType,
	var filterChangedListener: FilterChangedListener?) {

	abstract fun isEnabled(): Boolean

	abstract fun isTrackAccepted(trackItem: TrackItem): Boolean

	abstract fun initWithValue(value: BaseTrackFilter)

	open fun initFilter() {}

	override fun equals(other: Any?): Boolean {
		return other is BaseTrackFilter && other.filterType == filterType
	}
}