package net.osmand.shared.gpx.filters

import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.filters.TrackFilterType.FOLDER

open class FolderTrackFilter(
	filterChangedListener: FilterChangedListener?
) :
	ListTrackFilter(FOLDER, filterChangedListener) {

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		val trackItemPropertyValue = getTrackPropertyValue(trackItem)
		for (item in selectedItems) {//***
			if (trackItemPropertyValue.startsWith(item)) {
				return true
			}
		}
		return false
	}
}