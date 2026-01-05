package net.osmand.shared.gpx.organization.strategy

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.gpx.organization.OrganizeByRules
import net.osmand.shared.gpx.organization.OrganizeTracksResourceMapper

// TODO: only for development purposes, delete after that
object NoImplementedStrategy: OrganizeByStrategy {
	override fun apply(
		originalGroup: TracksGroup,
		rules: OrganizeByRules,
		resourcesMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>? {
		return null
	}
}