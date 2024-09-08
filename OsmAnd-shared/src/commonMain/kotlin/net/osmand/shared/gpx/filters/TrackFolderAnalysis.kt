package net.osmand.shared.gpx.filters

import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.TrackFolder
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.io.KFile

class TrackFolderAnalysis(folder: TracksGroup) {
	var tracksCount = 0
	var totalDistance = 0f
	var timeSpan = 0
	var fileSize: Long = 0
	var diffElevationUp = 0.0
	var diffElevationDown = 0.0

	init {
		prepareInformation(folder)
	}

	private fun prepareInformation(folder: TracksGroup) {
		val items: MutableList<TrackItem> = ArrayList()
		if (folder is TrackFolder) {
			items.addAll(folder.getFlattenedTrackItems())
		} else {
			items.addAll(folder.getTrackItems())
		}
		for (trackItem in items) {
			val dataItem = trackItem.dataItem
			val analysis = dataItem?.getAnalysis()
			if (analysis != null) {
				totalDistance += analysis.totalDistance
				diffElevationUp += analysis.diffElevationUp
				diffElevationDown += analysis.diffElevationDown
				val file: KFile? = trackItem.getFile()
				if (file != null) {
					fileSize += file.length()
				}
				if (analysis.isTimeSpecified()) {
					timeSpan = (timeSpan + analysis.getDurationInMs() / 1000.0f).toInt()
				}
			}
		}
		tracksCount = items.size
	}
}
