package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import kotlin.math.max
import kotlin.math.min

abstract class RangeTrackFilter(
	val app: OsmandApplication,
	displayNameId: Int,
	filterType: FilterType,
	filterChangedListener: FilterChangedListener)
	: BaseTrackFilter(displayNameId, filterType, filterChangedListener) {
	@Expose
	private var valueFrom = 0f

	@Expose
	private var valueTo = 300f

	@Expose
	var minValue = 0f

	@Expose
	var maxValue = 300f

	open val unitResId = R.string.shared_string_minute_lowercase

	fun setValueFrom(from: Float, updateListeners: Boolean = true) {
		valueFrom = max(minValue, from)
		valueFrom = min(valueFrom, valueTo)
		updateEnabled()
		if (updateListeners) {
			filterChangedListener.onFilterChanged()
		}
	}

	fun setValueTo(to: Float, updateListeners: Boolean = true) {
		valueTo = min(to, maxValue)
		valueTo = max(valueFrom, valueTo)
		updateEnabled()
		if (updateListeners) {
			filterChangedListener.onFilterChanged()
		}
	}

	fun getValueFrom(): Float {
		return valueFrom
	}

	fun getValueTo(): Float {
		return valueTo
	}

	private fun updateEnabled() {
		enabled = valueFrom > minValue || valueTo < maxValue
	}
}