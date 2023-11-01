package net.osmand.plus.myplaces.tracks.filters

import android.graphics.Color
import android.util.Pair
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

	var allColors: MutableList<String> = arrayListOf()
		private set
	var allColorsCollection: HashMap<String, Int> = hashMapOf()
		private set

	fun setFullColorsCollection(collection: List<Pair<String, Int>>) {
		for (pair in collection) {
			allColors.add(pair.first)
			allColorsCollection[pair.first] = pair.second
		}
	}


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
			val trackItemColor = trackItem.dataItem?.color
			if (trackItemColor == Color.parseColor(color) ||
				trackItemColor == 0 && Algorithms.isEmpty(color)) {
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
				if (!allColors.contains(color)) {
					allColors.add(color)
					allColorsCollection[color] = 0
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

	fun getTracksCountForColor(colorName: String): Int {
		return allColorsCollection[colorName] ?: 0
	}
}