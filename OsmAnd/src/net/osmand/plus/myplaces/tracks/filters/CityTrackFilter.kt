package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.CITY
import net.osmand.util.Algorithms

class CityTrackFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener?) :
	ListTrackFilter(app, R.string.nearest_cities, CITY, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedItems)
	}

	@Expose
	private var selectedCities = ArrayList<String>()

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		for (city in selectedItems) {
			if (Algorithms.stringsEqual(trackItem.dataItem?.nearestCityName, city)) {
				return true
			}
		}
		return false
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is CityTrackFilter &&
				other.selectedItems.size == selectedItems.size &&
				areAllItemsSelected(other.selectedItems)
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if(value is CityTrackFilter) {
			if(!Algorithms.isEmpty(value.selectedCities) || Algorithms.isEmpty(value.selectedItems)){
				value.selectedItems = ArrayList(value.selectedCities)
			}
		}
		super.initWithValue(value)
	}
}