package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.CITY
import net.osmand.util.Algorithms

class CityTrackFilter(filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(R.string.nearest_cities, CITY, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedCities)
	}

	@Expose
	val selectedCities = ArrayList<String>()

	var fullCitiesList: MutableList<String> = ArrayList()

	fun setCitySelected(city: String, selected: Boolean) {
		if (selected) {
			selectedCities.add(city)
		} else {
			selectedCities.remove(city)
		}
		filterChangedListener?.onFilterChanged()
	}

	fun isCitySelected(city: String): Boolean {
		return selectedCities.contains(city)
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		for (city in selectedCities) {
			if (Algorithms.stringsEqual(trackItem.dataItem?.nearestCityName, city)) {
				return true
			}
		}
		return false
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is CityTrackFilter) {
			selectedCities.clear()
			selectedCities.addAll(value.selectedCities)
			for (city in value.selectedCities) {
				if (!fullCitiesList.contains(city)) {
					fullCitiesList.add(city)
				}
			}
			filterChangedListener?.onFilterChanged()
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is CityTrackFilter &&
				other.selectedCities.size == selectedCities.size &&
				areAllCitiesSelected(other.selectedCities)
	}

	private fun areAllCitiesSelected(cities: List<String>): Boolean {
		for (city in cities) {
			if (!isCitySelected(city)) {
				return false
			}
		}
		return true
	}
}