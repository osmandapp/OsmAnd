package net.osmand.shared.filters

import net.osmand.shared.gpx.TrackItem

interface TracksGroup {
	fun getName(): String

	fun getTrackItems(): List<TrackItem>
}
