package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.DATE_CREATION
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateCreationTrackFilter(filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(R.string.date_of_creation, DATE_CREATION, filterChangedListener) {
	private var DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
	var initialValueFrom = Date().time
	var initialValueTo = Date().time

	@Expose
	var valueFrom = Date().time
		set(value) {
			field = value
			filterChangedListener?.onFilterChanged()
		}

	@Expose
	var valueTo = Date().time
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
		val day1String = getDateFormat().format(day1)
		val day2String = getDateFormat().format(day2)
		return day1String.equals(day2String)
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		return if (trackItem.dataItem == null)
			false
		else {
			trackItem.dataItem!!.fileCreationTime >= valueFrom && trackItem.dataItem!!.fileCreationTime <= valueTo
		}
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is DateCreationTrackFilter) {
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
				other is DateCreationTrackFilter &&
				isDatesEquals(valueFrom, other.valueFrom) &&
				isDatesEquals(valueTo, other.valueTo)
	}

	private fun getDateFormat(): SimpleDateFormat {
		if (DATE_FORMAT == null) {
			DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
		}
		return DATE_FORMAT
	}
}