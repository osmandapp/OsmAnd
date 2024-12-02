package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.TrackItem

interface TracksGroup {
	fun getId(): String

	fun getName(): String

	fun getTrackItems(): List<TrackItem>
}
