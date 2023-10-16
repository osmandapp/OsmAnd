package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

abstract class RangeTrackFilter(
	minValue: Float,
	maxValue: Float,
	val app: OsmandApplication,
	displayNameId: Int,
	filterType: FilterType,
	filterChangedListener: FilterChangedListener?)
	: BaseTrackFilter(displayNameId, filterType, filterChangedListener) {

	@Expose
	var minValue: Float

	@Expose
	var maxValue: Float
		private set

	@Expose
	var valueFrom: Float

	@Expose
	var valueTo: Float

	init {
		this.minValue = minValue
		this.maxValue = maxValue
		this.valueFrom = minValue
		this.valueTo = maxValue
	}

	open val unitResId = R.string.shared_string_minute_lowercase

	open fun setValueFrom(from: Float, updateListeners: Boolean = true) {
		valueFrom = max(minValue, from)
		valueFrom = min(valueFrom, valueTo)
		if (updateListeners) {
			filterChangedListener?.onFilterChanged()
		}
	}

	open fun setValueTo(to: Float, updateListeners: Boolean = true) {
		valueTo = to
		if (valueTo > maxValue) {
			maxValue = valueTo
		}
		valueTo = max(valueFrom, valueTo)
		if (updateListeners) {
			filterChangedListener?.onFilterChanged()
		}
	}

	override fun isEnabled(): Boolean {
		return valueFrom > minValue || valueTo < maxValue
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is RangeTrackFilter) {
			minValue = value.minValue
			maxValue = value.maxValue
			valueFrom = value.valueFrom
			valueTo = value.valueTo
			filterChangedListener?.onFilterChanged()
		}
	}

	fun setMaxValue(value: Float) {
		maxValue = value
		valueTo = value
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is RangeTrackFilter &&
				other.minValue == minValue &&
				other.maxValue == maxValue &&
				other.valueFrom == valueFrom &&
				other.valueTo == valueTo
	}

	open fun getDisplayMinValue(): Int {
		return floor(minValue).toInt()
	}

	open fun getDisplayMaxValue(): Int {
		return ceil(maxValue).toInt()
	}

	open fun getDisplayValueFrom(): Int {
		return floor(valueFrom).toInt()
	}

	open fun getDisplayValueTo(): Int {
		return ceil(valueTo).toInt()
	}

}