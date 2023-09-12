package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.DATE_CREATION
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateCreationTrackFilter(filterChangedListener: FilterChangedListener) :
	BaseTrackFilter(R.string.date_of_creation, DATE_CREATION, filterChangedListener) {
	private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
	var initialValueFrom = Date().time
	var initialValueTo = Date().time

	@Expose
	var valueFrom = Date().time
		set(value) {
			field = value
			filterChangedListener.onFilterChanged()
		}

	@Expose
	var valueTo = Date().time
		set(value) {
			field = value
			filterChangedListener.onFilterChanged()
		}

	override fun isEnabled(): Boolean {
		return !isDatesEquals(initialValueFrom, valueFrom) || !isDatesEquals(
			initialValueTo,
			valueTo)
	}

	private fun isDatesEquals(day1: Long, day2: Long): Boolean {
		return DATE_FORMAT.format(day1).equals(DATE_FORMAT.format(day2))
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		return if (trackItem.dataItem == null)
			false
		else {
			trackItem.dataItem!!.fileCreationTime > valueFrom && trackItem.dataItem!!.fileCreationTime < valueTo
		}
	}
}