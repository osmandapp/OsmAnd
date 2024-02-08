package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.gpx.GpxParameter
import net.osmand.plus.configmap.tracks.TrackItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateTrackFilter(
	trackFilterType: TrackFilterType,
	dateFrom: Long,
	filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(trackFilterType, filterChangedListener) {
	var initialValueFrom = dateFrom
	var initialValueTo = Date().time

	@Expose
	var valueFrom = initialValueFrom
		set(value) {
			field = value
			filterChangedListener?.onFilterChanged()
		}

	@Expose
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

	private fun isDatesEquals(day1: Long, day2: Long): Boolean {
		val day1String: String = getDateFormat().format(day1)
		val day2String: String = getDateFormat().format(day2)
		return day1String == day2String
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		return if (trackItem.dataItem == null)
			false
		else {
			val result =
				trackItem.dataItem!!.getParameter(GpxParameter.FILE_CREATION_TIME) in valueFrom..valueTo
			result
		}
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

	private fun getDateFormat(): SimpleDateFormat {
		return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
	}
}