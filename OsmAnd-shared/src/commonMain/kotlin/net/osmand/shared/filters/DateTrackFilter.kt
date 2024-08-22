package net.osmand.shared.filters

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.serialization.Serializable
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.util.PlatformUtil

//import java.text.SimpleDateFormat
//import java.util.Locale

private const val DATE_PATTERN = "dd.MM.yyyy"

class DateTrackFilter(
	trackFilterType: TrackFilterType,
	dateFrom: Long,
	filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(trackFilterType, filterChangedListener) {
	var initialValueFrom = Instant.fromEpochMilliseconds(dateFrom)
	var initialValueTo = Clock.System.now()

	@Serializable
	var valueFrom = initialValueFrom
		set(value) {
			field = value
			filterChangedListener?.onFilterChanged()
		}

	@Serializable
	var valueTo = initialValueTo
		set(value) {
			field = value
			filterChangedListener?.onFilterChanged()
		}

	override fun isEnabled(): Boolean {
		return !isDatesEquals(initialValueFrom, valueFrom) || !isDatesEquals(
			initialValueTo,
			valueTo)
	}

	private fun isDatesEquals(day1: Instant, day2: Instant): Boolean {
		val day1String: String = PlatformUtil.formatDate(day1, DATE_PATTERN)
		val day2String: String = PlatformUtil.formatDate(day2, DATE_PATTERN)
		return day1String == day2String
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {

		trackItem.dataItem?.let {
			it.getParameter<Long>(GpxParameter.FILE_CREATION_TIME)?.let{creationTime ->
				return Instant.fromEpochMilliseconds(creationTime) in valueFrom..valueTo
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
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is DateTrackFilter &&
				isDatesEquals(valueFrom, other.valueFrom) &&
				isDatesEquals(valueTo, other.valueTo)
	}

	override fun hashCode(): Int {
		return valueFrom.hashCode() + valueTo.hashCode()
	}
}