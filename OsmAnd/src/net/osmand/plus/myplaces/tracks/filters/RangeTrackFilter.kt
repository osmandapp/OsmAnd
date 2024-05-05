package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.gpx.GpxParameter
import net.osmand.plus.OsmandApplication
import net.osmand.plus.configmap.tracks.TrackItem
import kotlin.math.ceil
import kotlin.math.floor


open class RangeTrackFilter<T : Comparable<T>>(
	minValue: T,
	maxValue: T,
	val app: OsmandApplication,
	trackFilterType: TrackFilterType,
	filterChangedListener: FilterChangedListener?)
	: BaseTrackFilter(trackFilterType, filterChangedListener) {

	@Expose
	var minValue: T

	@Expose
	var maxValue: T
		private set

	@Expose
	var valueFrom: T

	@Expose
	var valueTo: T

	init {
		this.minValue = minValue
		this.maxValue = maxValue
		this.valueFrom = minValue
		this.valueTo = maxValue
	}

	open fun setValueFrom(from: T, updateListeners: Boolean = true) {
		valueFrom = maxOf(minValue, from)
		valueFrom = minOf(valueFrom, valueTo)
		if (updateListeners) {
			filterChangedListener?.onFilterChanged()
		}
	}

	private fun getValueFromString(value: String): T {
		val convertedValue: T? = when (getProperty().typeClass) {
			java.lang.Double::class.java -> {
				check(value.toDouble() as java.lang.Double)
			}

			java.lang.Float::class.java -> {
				check(value.toFloat() as java.lang.Float)
			}

			java.lang.Integer::class.java -> {
				check(value.toInt() as java.lang.Integer)
			}

			java.lang.Long::class.java -> {
				check(value.toLong() as java.lang.Long)
			}

			else -> {
				null
			}
		}
		if (convertedValue != null) {
			return convertedValue
		} else {
			throw java.lang.IllegalArgumentException("value can not be cast to ${trackFilterType.property!!.typeClass}")
		}

	}

	fun setValueTo(to: String, updateListeners: Boolean = true) {
		val baseValue = getComparableValue(
			trackFilterType.measureUnitType.getBaseValueFromFormatted(
				app,
				to))
		setValueTo(baseValue, updateListeners)
	}

	fun setValueFrom(from: String, updateListeners: Boolean = true) {
		val baseValue = getComparableValue(
			trackFilterType.measureUnitType.getBaseValueFromFormatted(
				app,
				from)).toString()
		setValueFrom(getValueFromString(baseValue), updateListeners)
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
		return comparableValue > valueFrom && comparableValue < valueTo
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

	@Suppress("UNCHECKED_CAST")
	private fun check(value: Comparable<*>): T? {
		return try {
			value as T
		} catch (err: ClassCastException) {
			null
		}
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

	open fun getDisplayMinValue(): Int {
		val formattedValue = trackFilterType.measureUnitType.getFormattedValue(app, flor(minValue))
		return formattedValue.valueSrc.toInt()
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

	open fun getDisplayMaxValue(): Int {
		val formattedValue = trackFilterType.measureUnitType.getFormattedValue(app, ceil(maxValue))
		return formattedValue.valueSrc.toInt()
	}

	open fun getDisplayValueFrom(): Int {
		val formattedValue =
			trackFilterType.measureUnitType.getFormattedValue(app, valueFrom.toString())
		return formattedValue.valueSrc.toInt()
	}

	open fun getDisplayValueTo(): Int {
		val formattedValue = trackFilterType.measureUnitType.getFormattedValue(app, ceil(valueTo))
		return formattedValue.valueSrc.toInt()
	}

	private fun getProperty(): GpxParameter {
		return trackFilterType.property!!
	}

	private fun getComparableValue(value: Any): T {
		if (value is String) {
			return getValueFromString(value)
		} else if (value is Number) {
			return when (getProperty().typeClass) {
				java.lang.Integer::class.java -> {
					check(value.toInt()) as T
				}

				java.lang.Double::class.java -> {
					check(value.toDouble()) as T
				}

				java.lang.Long::class.java -> {
					check(value.toLong()) as T
				}

				java.lang.Float::class.java -> {
					check(value.toFloat()) as T
				}

				else -> {
					throw IllegalArgumentException("Can not cast $value to " + getProperty().typeClass)
				}
			}
		}
		throw IllegalArgumentException("$value is not a number")
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