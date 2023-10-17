package net.osmand.plus.myplaces.tracks.filters

import com.google.gson.annotations.Expose
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.WIDTH
import net.osmand.util.Algorithms

class WidthTrackFilter(filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(R.string.shared_string_width, WIDTH, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedWidths)
	}

	@Expose
	val selectedWidths = ArrayList<String>()

	var fullWidthList: MutableMap<String, Int> = HashMap()

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
			if (Algorithms.stringsEqual(trackItem.dataItem?.width, width)) {
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
				if (!fullWidthList.contains(city)) {
					fullWidthList[city] = 0
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
}