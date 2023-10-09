package net.osmand.plus.track.data

import android.content.Context
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter

class SmartFolder(var folderName: String) : TracksGroup {

	private val trackItems = ArrayList<TrackItem>()
	var filters: MutableList<BaseTrackFilter>? = null

	override fun getName(context: Context): String {
		return folderName
	}

	override fun getTrackItems(): MutableList<TrackItem> {
		return trackItems
	}

	fun addTrackItem(trackItem: TrackItem) {
		trackItems.add(trackItem)
	}
}