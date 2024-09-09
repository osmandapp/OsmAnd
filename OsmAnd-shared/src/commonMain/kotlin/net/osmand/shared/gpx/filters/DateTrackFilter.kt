package net.osmand.shared.gpx.filters

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.TrackItem

class DateTrackFilter(trackFilterType: TrackFilterType, dateFrom: Long,
	filterChangedListener: FilterChangedListener?) : BaseTrackFilter(trackFilterType, filterChangedListener) {

	var initialValueFrom = dateFrom
	var initialValueTo = currentTimeMillis()

	private var initialValueFromDateTime = Instant.fromEpochMilliseconds(initialValueFrom).toLocalDateTime(TimeZone.currentSystemDefault())
	private var initialValueToDateTime = Instant.fromEpochMilliseconds(initialValueTo).toLocalDateTime(TimeZone.currentSystemDefault())
	private var valueFromDateTime = Instant.fromEpochMilliseconds(initialValueFrom).toLocalDateTime(TimeZone.currentSystemDefault())
	private var valueToDateTime = Instant.fromEpochMilliseconds(initialValueTo).toLocalDateTime(TimeZone.currentSystemDefault())

	private fun updateDateTime() {
		initialValueFromDateTime = Instant.fromEpochMilliseconds(initialValueFrom).toLocalDateTime(TimeZone.currentSystemDefault())
		initialValueToDateTime = Instant.fromEpochMilliseconds(initialValueTo).toLocalDateTime(TimeZone.currentSystemDefault())
		valueFromDateTime = Instant.fromEpochMilliseconds(valueFrom).toLocalDateTime(TimeZone.currentSystemDefault())
		valueToDateTime = Instant.fromEpochMilliseconds(valueTo).toLocalDateTime(TimeZone.currentSystemDefault())
	}

	@Serializable
	var valueFrom = initialValueFrom
		set(value) {
			field = value
			updateDateTime()
			filterChangedListener?.onFilterChanged()
		}

	@Serializable
	var valueTo = initialValueTo
		set(value) {
			field = value
			updateDateTime()
			filterChangedListener?.onFilterChanged()
		}

	override fun isEnabled(): Boolean {
		return !isDatesEquals(initialValueFromDateTime, valueFromDateTime) || !isDatesEquals(
			initialValueToDateTime,
			valueToDateTime)
	}

	private fun isDatesEquals(date1: LocalDateTime, date2: LocalDateTime): Boolean {
		return date1.date == date2.date
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {

		trackItem.dataItem?.let {
			it.getParameter<Long>(GpxParameter.FILE_CREATION_TIME)?.let{creationTime ->
				return creationTime in valueFrom..valueTo
			}
		}
		return false
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is DateTrackFilter) {
			valueTo = value.valueTo
			valueFrom = value.valueFrom
			if (initialValueTo < valueTo) {
				initialValueTo = valueTo
			}
			if (initialValueFrom > valueFrom) {
				initialValueFrom = valueFrom
			}
			updateDateTime()
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is DateTrackFilter &&
				isDatesEquals(valueFromDateTime, other.valueFromDateTime) &&
				isDatesEquals(valueToDateTime, other.valueToDateTime)
	}

	override fun hashCode(): Int {
		return valueFrom.hashCode() + valueTo.hashCode()
	}
}