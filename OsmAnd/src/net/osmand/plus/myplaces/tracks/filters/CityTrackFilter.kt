package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.CITY
import net.osmand.util.Algorithms
import java.util.*
import kotlin.collections.ArrayList

class CityTrackFilter(filterChangedListener: FilterChangedListener) :
	BaseTrackFilter(R.string.nearest_cities, CITY, filterChangedListener) {

	override var enabled: Boolean = false
		get() {
			return selectedCities.size > 0
		}

	@Expose
	val selectedCities = ArrayList<String>()

	var fullCitiesList: List<String> = ArrayList()

	fun setCitySelected(city: String, selected: Boolean) {
		if (selected) {
			selectedCities.add(city)
		} else {
			selectedCities.remove(city)
		}
		filterChangedListener.onFilterChanged()
	}

	fun isCitySelected(city: String): Boolean {
		return selectedCities.contains(city)
	}

	override fun isTrackOutOfFilterBounds(trackItem: TrackItem): Boolean {
		for (city in selectedCities) {
			if (Algorithms.stringsEqual(trackItem.dataItem?.nearestCityName, city)) {
				return false
			}
		}
		return true
	}
}