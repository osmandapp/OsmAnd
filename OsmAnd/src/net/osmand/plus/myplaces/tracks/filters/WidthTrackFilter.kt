package net.osmand.plus.myplaces.tracks.filters

import android.util.Pair
import com.google.gson.annotations.Expose
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.WIDTH
import net.osmand.util.Algorithms

class WidthTrackFilter(filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(R.string.shared_string_width, WIDTH, filterChangedListener), TracksCollectionFilter {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedWidths)
	}

	@Expose
	var selectedWidths = ArrayList<String>()
		private set
	var allWidth: MutableList<String> = arrayListOf()
		private set
	var allWidthCollection: HashMap<String, Int> = hashMapOf()
		private set

	fun setFullWidthCollection(collection: HashMap<String, Int>) {
		allWidth = ArrayList(collection.keys)
		allWidthCollection = collection
	}

	fun setFullWidthCollection(collection: List<Pair<String, Int>>) {
		for (pair in collection) {
			allWidth.add(pair.first)
			allWidthCollection[pair.first] = pair.second
		}
	}

	fun setSelectedWidths(selectedItems: List<String>) {
		selectedWidths = ArrayList(selectedItems)
		filterChangedListener?.onFilterChanged()
	}

	fun setWidthSelected(width: String, selected: Boolean) {
		if (selected) {
			selectedWidths.add(width)
		} else {
			selectedWidths.remove(width)
		}
		filterChangedListener?.onFilterChanged()
	}

	fun isWidthSelected(color: String): Boolean {
		return selectedWidths.contains(color)
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		for (width in selectedWidths) {
			val trackItemWidth = trackItem.dataItem?.width
			if (Algorithms.stringsEqual(trackItemWidth, width) ||
				Algorithms.isEmpty(trackItemWidth) && Algorithms.isEmpty(width)) {
				return true
			}
		}
		return false
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is WidthTrackFilter) {
			selectedWidths.clear()
			selectedWidths.addAll(value.selectedWidths)
			for (city in value.selectedWidths) {
				if (!allWidth.contains(city)) {
					allWidth.add(city)
					allWidthCollection[city] = 0
				}
			}
			filterChangedListener?.onFilterChanged()
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is WidthTrackFilter &&
				other.selectedWidths.size == selectedWidths.size &&
				areAllWidthSelected(other.selectedWidths)
	}

	private fun areAllWidthSelected(widths: List<String>): Boolean {
		for (city in widths) {
			if (!isWidthSelected(city)) {
				return false
			}
		}
		return true
	}

	fun getTracksCountForWidth (widthName: String): Int {
		return allWidthCollection[widthName] ?: 0
	}

	override fun getSelectedItems(): List<String> {
		return ArrayList(selectedWidths)
	}

}