package net.osmand.shared.gpx.filters

import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.OrganizedTrackGroup
import net.osmand.shared.gpx.enums.OrganizeByType
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

@Serializable(with = RangeTrackFilterSerializer::class)
open class RangeTrackFilter<T : Comparable<T>>
	: BaseTrackFilter {

	constructor(
		minValue: T,
		maxValue: T,
		trackFilterType: TrackFilterType,
		filterChangedListener: FilterChangedListener?) : super(
		trackFilterType,
		filterChangedListener) {
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

	private var organizedByGroups: MutableList<OrganizedTrackGroup>? = null
	private var firstBoundaryInt: Int = 0
	private var lastBoundaryInt: Int = 0

	open fun setValueFrom(from: T, updateListeners: Boolean = true) {
		valueFrom = maxOf(minValue, from)
		valueFrom = minOf(valueFrom, valueTo)
		if (updateListeners) {
			filterChangedListener?.onFilterChanged()
		}
	}

	private fun getValueFromString(value: String): T {
		val convertedValue: T? = when (getProperty().typeClass) {
			Double::class -> check(value.toDouble())
			Float::class -> check(value.toFloat())
			Int::class -> check(value.toInt())
			Long::class -> check(value.toLong())
			else -> null
		}
		if (convertedValue != null) {
			return convertedValue
		} else {
			throw IllegalArgumentException("value can not be cast to ${trackFilterType.property!!.typeClass}")
		}

	}

	fun setValueTo(to: String, updateListeners: Boolean = true) {
		val baseValue = getComparableValue(
			trackFilterType.measureUnitType.getBaseValueFromFormatted(to)
		)
		setValueTo(baseValue, updateListeners)
	}

	fun setValueFrom(from: String, updateListeners: Boolean = true) {
		val baseValue = getComparableValue(
			trackFilterType.measureUnitType.getBaseValueFromFormatted(from)
		).toString()
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

	@Suppress("UNCHECKED_CAST")
	private fun check(value: Comparable<*>): T? {
		return try {
			value as T
		} catch (err: ClassCastException) {
			null
		}
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
		if (value is String) {
			return getValueFromString(value)
		} else if (value is Number) {
			return when (getProperty().typeClass) {
				Int::class -> check(value.toInt()) as T
				Double::class -> check(value.toDouble()) as T
				Long::class -> check(value.toLong()) as T
				Float::class -> check(value.toFloat()) as T
				else -> throw IllegalArgumentException("Can not cast $value to " + getProperty().typeClass)
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

	override fun initOrganizedByGroups(organizeByType: OrganizeByType) {
		val stepRange = organizeByRange ?: organizeByType.organizedByStepRange ?: return

		val minVal = getInt(minValue)
		val maxVal = max(getInt(maxValue), stepRange.second)
		val step = organizeByStep ?: ((maxVal - minVal) / 2)
		if (step != organizeByStep) {
			organizeByStep = step
		}

		val totalRange = maxVal - minVal
		val remainder = totalRange % step
		val smallPart = remainder / 2

		this.firstBoundaryInt = minVal + smallPart
		this.lastBoundaryInt = maxVal - (remainder - smallPart)

		val numFullIntervals = totalRange / step
		val mutableGroups = mutableListOf<OrganizedTrackGroup>()
		mutableGroups.add(OrganizedTrackGroup("< $firstBoundaryInt", organizeByType.iconResId))

		for (i in 0 until numFullIntervals) {
			val start = firstBoundaryInt + (i * step)
			val end = start + step
			mutableGroups.add(OrganizedTrackGroup("$start - $end", organizeByType.iconResId))
		}
		mutableGroups.add(OrganizedTrackGroup("> $lastBoundaryInt", organizeByType.iconResId))
		this.organizedByGroups = mutableGroups
	}

	override fun getOrganizedByGroup(trackItem: TrackItem): OrganizedTrackGroup? {
		var group: OrganizedTrackGroup? = null
		val groups = organizedByGroups ?: return null
		organizeByStep?.let { step ->
			if (organizeByStep == 0) return null
			val value: Comparable<Any> =
				trackItem.dataItem?.getParameter(getProperty()) ?: return null
			val valueInt = getInt(getComparableValue(value))

			val index = when {
				valueInt < firstBoundaryInt -> 0
				valueInt >= lastBoundaryInt -> groups.lastIndex
				else -> {
					val middleIndex = ((valueInt - firstBoundaryInt) / step) + 1
					middleIndex.coerceAtMost(groups.lastIndex - 1)
				}
			}
			group = groups.getOrNull(index)
		}
		return group
	}

	private fun getInt(value: T): Int {
		return when (value) {
			is Number -> value.toInt()
			else -> value.toString().toDouble().toInt()
		}
	}
}