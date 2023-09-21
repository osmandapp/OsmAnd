package net.osmand.plus.myplaces.tracks.filters.viewholders

import android.view.View
import net.osmand.plus.utils.OsmAndFormatter

class FilterDurationViewHolder(itemView: View, nightMode: Boolean) :
	FilterRangeViewHolder(itemView, nightMode) {

	override fun updateSelectedValue(valueFromMinutes: Float, valueToMinutes: Float) {

		val fromTxt = OsmAndFormatter.getFormattedDuration(valueFromMinutes.toInt() * 60, app)
		val toTxt = OsmAndFormatter.getFormattedDuration(valueToMinutes.toInt() * 60, app)
		selectedValue.text = "$fromTxt - $toTxt"
	}
}