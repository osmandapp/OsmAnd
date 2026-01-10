package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.enums.TracksSortScope
import net.osmand.shared.gpx.organization.OrganizeByParams
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper

interface OrganizeByStrategy {

	fun apply(
		originalGroup: TracksGroup,
		params: OrganizeByParams,
		resourcesMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>?

	fun getTrackSortScope(): TracksSortScope
}