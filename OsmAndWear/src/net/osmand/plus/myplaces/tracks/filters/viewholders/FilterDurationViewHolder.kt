package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import net.osmand.plus.utils.OsmAndFormatter

class FilterDurationViewHolder(itemView: View, nightMode: Boolean) :
	FilterRangeViewHolder(itemView, nightMode) {

	override fun updateSelectedValue(valueFromMinutes: Int, valueToMinutes: Int) {
		val fromTxt = OsmAndFormatter.getFormattedDuration(valueFromMinutes * 60L, app)
		val toTxt = OsmAndFormatter.getFormattedDuration(valueToMinutes * 60L, app)
		selectedValue.text = "$fromTxt - $toTxt"
	}
}