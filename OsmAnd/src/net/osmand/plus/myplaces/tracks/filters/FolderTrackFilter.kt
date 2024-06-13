package net.osmand.plus.myplaces.tracks.filters

import net.osmand.plus.OsmandApplication
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.TrackFilterType.FOLDER

open class FolderTrackFilter(
	app: OsmandApplication,
	filterChangedListener: FilterChangedListener?
) :
	ListTrackFilter(app, FOLDER, filterChangedListener) {

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