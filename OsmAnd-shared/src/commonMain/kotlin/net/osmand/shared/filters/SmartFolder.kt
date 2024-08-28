package net.osmand.shared.filters

import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.util.KCollectionUtils

class SmartFolder(folderName: String) : TracksGroup, ComparableTracksGroup {

	private var trackItems: MutableList<TrackItem> = ArrayList()

	constructor() : this("") {
		trackItems = ArrayList()
	}

	@Serializable
	var folderName = folderName

	@Serializable
	var creationTime = 0L

	@Serializable
	var filters: MutableList<BaseTrackFilter>? = null

	private var folderAnalysis: TrackFolderAnalysis? = null

	override fun getName(): String {
		return folderName
	}

	override fun getTrackItems(): MutableList<TrackItem> {
		return trackItems
	}

	fun addTrackItem(trackItem: TrackItem) {
		if (!trackItems.contains(trackItem)) {
			trackItems = KCollectionUtils.addToList(trackItems, trackItem)
			folderAnalysis = null
		}
	}

	override fun getFolderAnalysis(): TrackFolderAnalysis {
		var analysis = folderAnalysis
		if (analysis == null) {
			analysis = TrackFolderAnalysis(this)
			folderAnalysis = analysis
		}
		return analysis
	}

	override fun getDirName(): String {
		return folderName
	}

	override fun lastModified(): Long {
		return creationTime
	}

	fun resetItems() {
		trackItems = ArrayList()
		folderAnalysis = null
	}
}