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

	private lateinit var folderAnalysis: TrackFolderAnalysis

	override fun getName(context: Context): String {
		return folderName
	}

	override fun getTrackItems(): MutableList<TrackItem> {
		return trackItems
	}

	fun addTrackItem(trackItem: TrackItem) {
		trackItems.add(trackItem)
	}

	override fun getFolderAnalysis(): TrackFolderAnalysis {
		if (!this::folderAnalysis.isInitialized) {
			folderAnalysis = TrackFolderAnalysis(this)
		}
		return folderAnalysis
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
}