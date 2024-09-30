package net.osmand.shared.gpx.filters

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.extensions.millisToLocalDateTime
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
		initialValueTo = currentTimeMillis()
		valueFrom = initialValueFrom
		valueTo = initialValueTo
		updateDateTime()
	}

	@Transient
	var initialValueFrom: Long = 0
	@Transient
	var initialValueTo: Long = 0

	@Transient
	private var initialValueFromDateTime: LocalDateTime? = null
	@Transient
	private var initialValueToDateTime: LocalDateTime? = null
	@Transient
	private var valueFromDateTime: LocalDateTime? = null
	@Transient
	private var valueToDateTime: LocalDateTime? = null

	private fun updateDateTime() {
		initialValueFromDateTime = millisToLocalDateTime(initialValueFrom)
		initialValueToDateTime = millisToLocalDateTime(initialValueTo)
		valueFromDateTime = millisToLocalDateTime(valueFrom)
		valueToDateTime = millisToLocalDateTime(valueTo)
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

	override fun isEnabled() =
		!isDatesEquals(initialValueFromDateTime, valueFromDateTime) ||
				!isDatesEquals(initialValueToDateTime, valueToDateTime)

	private fun isDatesEquals(date1: LocalDateTime?, date2: LocalDateTime?) = date1?.date == date2?.date

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
			super.initWithValue(value)
		}
	}

	override fun initFilter() {
		super.initFilter()
		updateDateTime()
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is DateTrackFilter &&
				isDatesEquals(valueFromDateTime, other.valueFromDateTime) &&
				isDatesEquals(valueToDateTime, other.valueToDateTime)
	}

	override fun hashCode() = valueFrom.hashCode() + valueTo.hashCode()
}