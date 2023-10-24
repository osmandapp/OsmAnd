package net.osmand.plus.track.data

import android.content.Context
import com.google.gson.annotations.Expose
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter
import net.osmand.plus.track.ComparableTracksGroup

class SmartFolder(folderName: String) : TracksGroup, ComparableTracksGroup {

	private var trackItems: ArrayList<TrackItem> = ArrayList()

	constructor() : this("") {
		trackItems = ArrayList()
	}

	@Expose
	var folderName = folderName

	@Expose
	var creationTime = 0L

	@Expose
	var filters: MutableList<BaseTrackFilter>? = null

	private var folderAnalysis: TrackFolderAnalysis? = null

	override fun getName(context: Context): String {
		return folderName
	}

	override fun getTrackItems(): List<TrackItem> {
		return trackItems
	}

	fun addTrackItem(trackItem: TrackItem) {
		trackItems.add(trackItem)
		folderAnalysis = null
	}

	fun addAllTrackItem(trackItem: Collection<TrackItem>) {
		trackItems.addAll(trackItem)
		folderAnalysis = null
	}

	override fun getFolderAnalysis(): TrackFolderAnalysis {
		if (folderAnalysis == null) {
			folderAnalysis = TrackFolderAnalysis(this)
		}
		return folderAnalysis!!
	}

	fun updateAnalysis() {
		folderAnalysis = TrackFolderAnalysis(this)
	}

	override fun getDirName(): String {
		return folderName
	}

	override fun lastModified(): Long {
		return creationTime
	}

	fun resetItems() {
		trackItems.clear()
		folderAnalysis = null
	}
}