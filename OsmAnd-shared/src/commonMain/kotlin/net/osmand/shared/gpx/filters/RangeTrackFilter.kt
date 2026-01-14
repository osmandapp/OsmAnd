package net.osmand.shared.gpx.filters

import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.TrackItem
import kotlin.math.ceil
import kotlin.math.floor

@Serializable(with = RangeTrackFilterSerializer::class)
open class RangeTrackFilter<T : Comparable<T>> : BaseTrackFilter {

	constructor(
		minValue: T,
		maxValue: T,
		trackFilterType: TrackFilterType,
		filterChangedListener: FilterChangedListener?
	) : super(
		trackFilterType,
		filterChangedListener
	) {
		this.minValue = minValue
		this.maxValue = maxValue
		this.valueFrom = minValue
		this.valueTo = maxValue
	}

	@Serializable
	var minValue: T

	@Serializable
	var maxValue: T
		private set

	@Serializable
	var valueFrom: T

	@Serializable
	var valueTo: T

	open fun setValueFrom(from: T, updateListeners: Boolean = true) {
		valueFrom = maxOf(minValue, from)
		valueFrom = minOf(valueFrom, valueTo)
		if (updateListeners) {
			filterChangedListener?.onFilterChanged()
		}
	}

	fun setValueTo(to: String, updateListeners: Boolean = true) {
		val baseValue = getComparableValue(
			trackFilterType.measureUnitType.getBaseValueFromFormatted(to)
		)
		setValueTo(baseValue, updateListeners)
	}

	fun setValueFrom(from: String, updateListeners: Boolean = true) {
		val property = getProperty()
		val value = trackFilterType.measureUnitType.getBaseValueFromFormatted(from)
		val baseValue = getComparableValue(value).toString()
		setValueFrom(property.getValueFromString(baseValue) as T, updateListeners)
	}

	private fun setValueTo(to: T, updateListeners: Boolean = true) {
		valueTo = to
		if (valueTo > maxValue) {
			maxValue = valueTo
		}
		valueTo = maxOf(valueFrom, valueTo)
		if (updateListeners) {
			filterChangedListener?.onFilterChanged()
		}
	}

	override fun isEnabled(): Boolean {
		return valueFrom > minValue || valueTo < maxValue
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		val value: Comparable<Any> = trackItem.dataItem?.getParameter(trackFilterType.property!!)
			?: return false
		val comparableValue = getComparableValue(value)
		return comparableValue in valueFrom..valueTo
				|| comparableValue < minValue && valueFrom == minValue
				|| comparableValue > maxValue && valueTo == maxValue
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is RangeTrackFilter<*>) {
			check(value.minValue)?.let { minValue = it }
			check(value.maxValue)?.let { maxValue = it }
			check(value.valueFrom)?.let { valueFrom = it }
			check(value.valueTo)?.let { valueTo = it }
			super.initWithValue(value)
		}
	}

	private fun check(value: Comparable<*>): T? {
		return getProperty().check(value)
	}

	fun setMaxValue(value: String) {
		setMaxValue(getComparableValue(value))
	}

	fun setMaxValue(value: T) {
		maxValue = getComparableValue(value)
		valueTo = getComparableValue(value)
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is RangeTrackFilter<*> &&
				other.minValue == minValue &&
				other.maxValue == maxValue &&
				other.valueFrom == valueFrom &&
				other.valueTo == valueTo
	}

	private fun flor(value: T): String {
		return when (value) {
			is Float -> {
				floor(value as Float).toString()
			}

			is Double -> {
				floor(value as Double).toString()
			}

			else -> {
				value.toString()
			}
		}
	}

	fun ceilMaxValue(): String {
		return ceil(maxValue)
	}

	fun ceilValueTo(): String {
		return ceil(valueTo)
	}

	fun ceilMinValue(): String {
		return ceil(minValue)
	}

	private fun ceil(value: T): String {
		return when (value) {
			is Float -> {
				ceil(value as Float).toString()
			}

			is Double -> {
				ceil(value as Double).toString()
			}

			else -> {
				value.toString()
			}
		}
	}

	private fun getProperty(): GpxParameter {
		return trackFilterType.property!!
	}

	private fun getComparableValue(value: Any): T {
		return getProperty().getComparableValue(value)
	}

	override fun hashCode(): Int {
		var result = minValue.hashCode()
		result = 31 * result + maxValue.hashCode()
		result = 31 * result + valueFrom.hashCode()
		result = 31 * result + valueTo.hashCode()
		return result
	}

	override fun isValid(): Boolean {
		return maxValue > minValue
	}
}