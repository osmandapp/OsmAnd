package net.osmand.shared.gpx.filters

import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.filters.TrackFilterType.FOLDER

@Serializable
open class FolderTrackFilter : ListTrackFilter {

	constructor(filterChangedListener: FilterChangedListener?) : super(
		FOLDER,
		filterChangedListener)

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