package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.R
import kotlin.math.max
import kotlin.math.min

abstract class RangeTrackFilter(
	displayNameId: Int,
	filterType: FilterType,
	filterChangedListener: FilterChangedListener)
	: BaseTrackFilter(displayNameId, filterType, filterChangedListener) {
	@Expose
	private var valueFrom = 0

	@Expose
	private var valueTo = 300

	@Expose
	var minValue = 0

	@Expose
	var maxValue = 300

	open val unitResId = R.string.shared_string_minute_lowercase

	fun setValueFrom(from: Int, updateListeners: Boolean = true) {
		valueFrom = max(minValue, from)
		valueFrom = min(valueFrom, valueTo)
		updateEnabled()
		if (updateListeners) {
			filterChangedListener.onFilterChanged()
		}
	}

	fun setValueTo(to: Int, updateListeners: Boolean = true) {
		valueTo = min(to, maxValue)
		valueTo = max(valueFrom, valueTo)
		updateEnabled()
		if (updateListeners) {
			filterChangedListener.onFilterChanged()
		}
	}

	fun getValueFrom(): Int {
		return valueFrom
	}

	fun getValueTo(): Int {
		return valueTo
	}

	private fun updateEnabled() {
		enabled = valueFrom > minValue || valueTo < maxValue
	}
}