package net.osmand.shared.gpx.filters

import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.filters.FilterType.DATE_RANGE
import net.osmand.shared.gpx.filters.FilterType.OTHER
import net.osmand.shared.gpx.filters.FilterType.RANGE
import net.osmand.shared.gpx.filters.FilterType.SINGLE_FIELD_LIST
import net.osmand.shared.gpx.filters.FilterType.TEXT
import kotlin.reflect.KClass

object TrackFiltersHelper {
	fun createTextFilter(
		filterType: TrackFilterType,
		listener: FilterChangedListener?): BaseTrackFilter {
		return TextTrackFilter(filterType, listener)
	}

	fun createDateFilter(
		trackFilterType: TrackFilterType,
		minDate: Long,
		listener: FilterChangedListener?): BaseTrackFilter {
		return DateTrackFilter(trackFilterType, minDate, listener)
	}

	fun createOtherFilter(
		trackFilterType: TrackFilterType,
		listener: FilterChangedListener?): BaseTrackFilter {
		return OtherTrackFilter(trackFilterType, listener)
	}

	fun createSingleListFilter(
		trackFilterType: TrackFilterType,
		listener: FilterChangedListener?): BaseTrackFilter {
		return if (trackFilterType === TrackFilterType.FOLDER) FolderTrackFilter(
			listener) else ListTrackFilter(trackFilterType, listener)
	}

	fun createRangeFilter(
		trackFilterType: TrackFilterType,
		listener: FilterChangedListener?): BaseTrackFilter {
		if (trackFilterType.defaultParams == null || trackFilterType.defaultParams.size < 2) {
			throw IllegalArgumentException("RangeTrackFilter needs 2 default params: minValue, maxValue")
		}
		val minValue: Any = trackFilterType.defaultParams[0]
		val maxValue: Any = trackFilterType.defaultParams[1]
		if (minValue::class != maxValue::class) {
			throw IllegalArgumentException("RangeTrackFilter's 2 default params (minValue, maxValue) must be the same type")
		}
		val parameter = trackFilterType.property
		if (isGpxParameterClass(parameter, Double::class)) {
			return RangeTrackFilter(
				minValue as Double,
				maxValue as Double,
				trackFilterType,
				listener)
		} else if (isGpxParameterClass(parameter, Float::class)) {
			return RangeTrackFilter(
				minValue as Float,
				maxValue as Float,
				trackFilterType,
				listener)
		} else if (isGpxParameterClass(parameter, Int::class)) {
			return RangeTrackFilter(
				minValue as Int,
				maxValue as Int,
				trackFilterType,
				listener)
		} else if (isGpxParameterClass(parameter, Long::class)) {
			return RangeTrackFilter(
				minValue as Long,
				maxValue as Long,
				trackFilterType,
				listener)
		}
		throw IllegalArgumentException("Unsupported gpxParameter type class " + parameter?.typeClass)
	}

	private fun isGpxParameterClass(parameter: GpxParameter?, javaClass: KClass<*>): Boolean {
		return parameter?.typeClass == javaClass
	}

	fun createFilter(
		trackFilterType: TrackFilterType,
		filterChangedListener: FilterChangedListener?): BaseTrackFilter {
		val newFilter: BaseTrackFilter = when (trackFilterType.filterType) {
			TEXT -> {
				createTextFilter(
					trackFilterType,
					filterChangedListener)
			}

			RANGE -> {
				createRangeFilter(
					trackFilterType,
					filterChangedListener)
			}

			DATE_RANGE -> {
				createDateFilter(
					trackFilterType,
					currentTimeMillis(),
					filterChangedListener)
			}

			OTHER -> {
				createOtherFilter(
					trackFilterType,
					filterChangedListener)
			}

			SINGLE_FIELD_LIST -> {
				createSingleListFilter(
					trackFilterType,
					filterChangedListener)
			}

			else -> throw IllegalArgumentException("Unknown filterType $trackFilterType")
		}
		return newFilter
	}

	fun getFilterClass(trackFilterType: TrackFilterType): KClass<out BaseTrackFilter> {
		val filterClass = when (trackFilterType.filterType) {
			TEXT -> {
				TextTrackFilter::class
			}

			RANGE -> {
				RangeTrackFilter::class
			}

			DATE_RANGE -> {
				DateTrackFilter::class
			}

			OTHER -> {
				OtherTrackFilter::class
			}

			SINGLE_FIELD_LIST -> {
				ListTrackFilter::class
			}

			else -> throw IllegalArgumentException("Unknown filterType $trackFilterType")
		}
		return filterClass
	}
}