package net.osmand.shared.gpx.filters

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.extensions.minLocalDateTime
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.TrackItem

@Serializable
class DateTrackFilter : BaseTrackFilter {

	constructor(
		trackFilterType: TrackFilterType, dateFrom: Long,
		filterChangedListener: FilterChangedListener?) : super(
		trackFilterType,
		filterChangedListener) {
		initialValueFrom = dateFrom
		initialValueFromDateTime = Instant.fromEpochMilliseconds(initialValueFrom)
			.toLocalDateTime(TimeZone.currentSystemDefault())
		valueFromDateTime = Instant.fromEpochMilliseconds(initialValueFrom)
			.toLocalDateTime(TimeZone.currentSystemDefault())
		valueFrom = initialValueFrom
		valueTo = initialValueTo
	}

	@Transient
	var initialValueFrom: Long = 0
	@Transient
	var initialValueTo = currentTimeMillis()

	@Transient
	private var initialValueFromDateTime: LocalDateTime = minLocalDateTime()

	@Transient
	private var initialValueToDateTime = Instant.fromEpochMilliseconds(initialValueTo)
		.toLocalDateTime(TimeZone.currentSystemDefault())
	@Transient
	private var valueFromDateTime: LocalDateTime = minLocalDateTime()
	private var valueToDateTime = Instant.fromEpochMilliseconds(initialValueTo)
		.toLocalDateTime(TimeZone.currentSystemDefault())

	private fun updateDateTime() {
		initialValueFromDateTime = Instant.fromEpochMilliseconds(initialValueFrom)
			.toLocalDateTime(TimeZone.currentSystemDefault())
		initialValueToDateTime = Instant.fromEpochMilliseconds(initialValueTo)
			.toLocalDateTime(TimeZone.currentSystemDefault())
		valueFromDateTime = Instant.fromEpochMilliseconds(valueFrom)
			.toLocalDateTime(TimeZone.currentSystemDefault())
		valueToDateTime =
			Instant.fromEpochMilliseconds(valueTo).toLocalDateTime(TimeZone.currentSystemDefault())
	}

	@Serializable
	var valueFrom: Long = 0
		set(value) {
			field = value
			updateDateTime()
			filterChangedListener?.onFilterChanged()
		}

	@Serializable
	var valueTo: Long = 0
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
			it.getParameter<Long>(GpxParameter.FILE_CREATION_TIME)?.let { creationTime ->
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