package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.TrackItem

interface TracksGroup {
	fun getName(): String

	fun getTrackItems(): List<TrackItem>
}
