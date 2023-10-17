package net.osmand.plus.myplaces.tracks.filters

import android.graphics.Color
import com.google.gson.annotations.Expose
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.COLOR
import net.osmand.util.Algorithms

class ColorTrackFilter(filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(R.string.shared_string_color, COLOR, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedColors)
	}

	@Expose
	val selectedColors = ArrayList<String>()

	var fullColorsList: MutableMap<String, Int> = HashMap()

	fun setColorSelected(color: String, selected: Boolean) {
		if (selected) {
			selectedColors.add(color)
		} else {
			selectedColors.remove(color)
		}
		filterChangedListener?.onFilterChanged()
	}

	fun isColorSelected(color: String): Boolean {
		return selectedColors.contains(color)
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		for (color in selectedColors) {
			if (trackItem.dataItem?.color == Color.parseColor(color)) {
				return true
			}
		}
		return false
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is ColorTrackFilter) {
			selectedColors.clear()
			selectedColors.addAll(value.selectedColors)
			for (color in value.selectedColors) {
				if (!fullColorsList.contains(color)) {
					fullColorsList[color] = 0
				}
			}
			filterChangedListener?.onFilterChanged()
		}
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is ColorTrackFilter &&
				other.selectedColors.size == selectedColors.size &&
				areAllColorsSelected(other.selectedColors)
	}

	private fun areAllColorsSelected(colors: List<String>): Boolean {
		for (color in colors) {
			if (!isColorSelected(color)) {
				return false
			}
		}
		return true
	}
}