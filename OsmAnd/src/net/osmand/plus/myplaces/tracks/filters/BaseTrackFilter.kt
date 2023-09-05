package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.osmand.plus.configmap.tracks.TrackItem

abstract class BaseTrackFilter(
	val displayNameId: Int,
	@Expose
	@SerializedName("filterType") var filterType: FilterType,
	var filterChangedListener: FilterChangedListener) {

	@Expose
	@SerializedName("filterEnabled")
	open var enabled: Boolean = false

	abstract fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean

	fun getFilerType(): FilterType {
		return filterType
	}
}