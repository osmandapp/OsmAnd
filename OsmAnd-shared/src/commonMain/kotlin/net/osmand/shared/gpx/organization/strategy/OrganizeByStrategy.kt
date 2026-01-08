package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.organization.OrganizeByParameter
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.enums.OrganizeByType

interface OrganizeByStrategy {

	fun apply(
		originalGroup: TracksGroup,
		param: OrganizeByParameter,
		resourcesMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>?

	fun getBaseId(originalGroup: TracksGroup, type: OrganizeByType): String {
		val parentId = originalGroup.getId()
		val typeName = type.name.lowercase()
		return "${parentId}__organized_by_${typeName}__"
	}
}