package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.enums.TracksSortScope

interface TracksGroup {
	fun getId(): String

	fun getName(): String

	fun getTrackItems(): List<TrackItem>

	fun getSubgroupById(subgroupId: String): TracksGroup? = null

	fun getTracksSortScope() = TracksSortScope.TRACKS

	fun getSupportedSortScopes(): List<TracksSortScope> = listOf(TracksSortScope.TRACKS)
}
