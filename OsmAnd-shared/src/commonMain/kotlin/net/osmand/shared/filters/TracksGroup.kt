package net.osmand.plus.track.data

import net.osmand.shared.gpx.TrackItem

interface TracksGroup {
	fun getName(): String

	var trackItems: MutableList<TrackItem>
}
