package net.osmand.shared.filters

import net.osmand.shared.gpx.TrackItem

interface TracksGroup {
	fun getName(): String

//	var trackItems: MutableList<TrackItem>
	fun getTrackItems(): List<TrackItem>
}
