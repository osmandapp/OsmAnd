package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.enums.TracksSortScope
import net.osmand.shared.gpx.organization.OrganizeByParams
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByType

interface OrganizeByStrategy {

	fun apply(
		originalGroup: TracksGroup,
		params: OrganizeByParams,
	): List<OrganizedTracksGroup>?

	fun getName(type: OrganizeByType, value: Any): String {
		return OrganizeTracksResourceMapper.getName(type, value)
	}

	fun getIconName(type: OrganizeByType, value: Any): String {
		return OrganizeTracksResourceMapper.getIconName(type, value)
	}

	fun getTrackSortScope(): TracksSortScope
}