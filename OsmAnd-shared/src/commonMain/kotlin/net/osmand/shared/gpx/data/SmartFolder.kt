package net.osmand.shared.gpx.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.filters.BaseTrackFilter
import net.osmand.shared.gpx.filters.TrackFilterSerializer
import net.osmand.shared.gpx.filters.TrackFolderAnalysis
import net.osmand.shared.util.KCollectionUtils

@Serializable
class SmartFolder(@Serializable var folderName: String) : TracksGroup, ComparableTracksGroup {

	@Transient
	private var trackItems: List<TrackItem>? = null

	constructor() : this("")

	@Serializable
	var creationTime = 0L

	@Serializable(with = TrackFilterSerializer::class)
	var filters: List<BaseTrackFilter>? = null

	@Transient
	private var folderAnalysis: TrackFolderAnalysis? = null

	override fun getName() = folderName

	override fun getTrackItems(): List<TrackItem> {
		var trackItems = this.trackItems
		if (trackItems == null) {
			trackItems = ArrayList()
			this.trackItems = trackItems
		}
		return trackItems
	}

	fun addTrackItem(trackItem: TrackItem) {
		if (!getTrackItems().contains(trackItem)) {
			trackItems = KCollectionUtils.addToList(getTrackItems(), trackItem)
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

	override fun getDirName() = folderName

	override fun lastModified() = creationTime

	fun resetItems() {
		trackItems = ArrayList()
		folderAnalysis = null
	}
}