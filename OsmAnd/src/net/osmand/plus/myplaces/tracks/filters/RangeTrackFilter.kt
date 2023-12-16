package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.settings.enums.MetricsConstants
import net.osmand.plus.track.helpers.GpxParameter
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue
import kotlin.math.ceil
import kotlin.math.floor


open class RangeTrackFilter<T : Comparable<T>>(
	minValue: T,
	maxValue: T,
	val app: OsmandApplication,
	filterType: FilterType,
	filterChangedListener: FilterChangedListener?)
	: BaseTrackFilter(filterType, filterChangedListener) {

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
			throw java.lang.IllegalArgumentException("value can not be cast to ${filterType.propertyList[0].typeClass}")
		}

	}

	fun setValueTo(to: String, updateListeners: Boolean = true) {
		val baseValue = getComparableValue(getBaseValueFromFormatted(to)).toString()
		setValueTo(getValueFromString(baseValue), updateListeners)
	}

	fun setValueFrom(from: String, updateListeners: Boolean = true) {
		val baseValue = getComparableValue(getBaseValueFromFormatted(from)).toString()
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
		val value: Comparable<Any> = trackItem.dataItem?.getParameter(filterType.propertyList[0])
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

	fun setMaxValue(value: Any) {
		val displayValue = getFormattedValue(value.toString()).valueSrc
		val normalizedValue = ceil(displayValue)
		val baseValue = getBaseValueFromFormatted(normalizedValue.toString())
		val convertedValue = getComparableValue(baseValue)
		maxValue = convertedValue
		valueTo = convertedValue
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
		val formattedValue = getFormattedValue(flor(minValue))
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
		val formattedValue = getFormattedValue(ceil(maxValue))
		return formattedValue.valueSrc.toInt()
	}

	open fun getDisplayValueFrom(): Int {
		val formattedValue = getFormattedValue(valueFrom.toString())
		return formattedValue.valueSrc.toInt()
	}

	open fun getDisplayValueTo(): Int {
		val formattedValue = getFormattedValue(ceil(valueTo))
		return formattedValue.valueSrc.toInt()
	}

	private fun getBaseValueFromFormatted(value: String): Float {
		val metricsConstants: MetricsConstants = app.settings.METRIC_SYSTEM.get()
		val mode = app.settings.applicationMode
		val speedConstant = app.settings.SPEED_SYSTEM.getModeValue(mode)
		return when (filterType.measureUnitType) {
			MeasureUnitType.SPEED -> OsmAndFormatter.getBaseValueFromFormattedSpeed(
				value.toFloat(),
				app,
				speedConstant)

			MeasureUnitType.ALTITUDE -> OsmAndFormatter.getMetersFromFormattedAltitudeValue(
				value.toFloat(),
				metricsConstants)

			MeasureUnitType.DISTANCE -> OsmAndFormatter.getMetersFromFormattedDistanceValue(
				value.toFloat(),
				metricsConstants)

			MeasureUnitType.TIME_DURATION -> value.toFloat() * 1000 * 60

			else -> value.toFloat()
		}

	}

	private fun getFormattedValue(value: String): FormattedValue {
		val metricsConstants: MetricsConstants = app.settings.METRIC_SYSTEM.get()
		return when (filterType.measureUnitType) {
			MeasureUnitType.SPEED -> OsmAndFormatter.getFormattedSpeedValue(value.toFloat(), app)
			MeasureUnitType.ALTITUDE -> OsmAndFormatter.getFormattedAltitudeValue(
				value.toDouble(),
				app,
				metricsConstants)

			MeasureUnitType.DISTANCE -> OsmAndFormatter.getFormattedDistanceValue(
				value.toFloat(),
				app,
				true,
				metricsConstants)

			MeasureUnitType.TIME_DURATION -> FormattedValue(value.toFloat() / 1000 / 60, value, "")

			else -> FormattedValue(value.toFloat(), value, "")
		}
	}

	private fun getProperty(): GpxParameter {
		return filterType.propertyList[0]
	}

	private fun getComparableValue(value: Any): T {
		if(value is String) {
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
}