package net.osmand.shared.gpx.filters

import net.osmand.shared.extensions.format
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.data.TrackFolder
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.io.KFile
import net.osmand.shared.util.LoggerFactory

class TrackFolderAnalysis(folder: TracksGroup) {
	var tracksCount = 0
	var totalDistance = 0f
	var timeSpan = 0
	var fileSize: Long = 0
	var diffElevationUp = 0.0
	var diffElevationDown = 0.0

	companion object {
		private val log = LoggerFactory.getLogger("TrackFolderAnalysis")
	}

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
		var totalDistanceSum = 0.0
		var timeSpanSum = 0.0
		for (trackItem in items) {
			val dataItem = trackItem.dataItem
			val analysis = dataItem?.getAnalysis()
			if (analysis != null) {
				totalDistanceSum += analysis.totalDistance
				diffElevationUp += analysis.diffElevationUp
				diffElevationDown += analysis.diffElevationDown
				val file: KFile? = trackItem.getFile()
				if (file != null) {
					fileSize += file.length()
				}
				if (analysis.isTimeSpecified()) {
					timeSpanSum += analysis.getDurationInSeconds()
				}
			}
		}
		totalDistance = totalDistanceSum.toFloat()
		timeSpan = timeSpanSum.toInt()
		tracksCount = items.size

		log.info(">>>> ${folder.getId()} = (tracks: $tracksCount, totalDistance: ${"%.2f".format(totalDistance)}, " +
				"timeSpan: $timeSpan, fileSize: $fileSize, diffElevationUp: ${"%.2f".format(diffElevationUp)}, diffElevationDown: ${"%.2f".format(diffElevationDown)}")
	}
}
