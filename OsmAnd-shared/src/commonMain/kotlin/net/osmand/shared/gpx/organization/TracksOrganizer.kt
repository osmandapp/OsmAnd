package net.osmand.shared.gpx.organization

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.organization.strategy.OrganizeByStrategy

class TracksOrganizer {

	private var strategy: OrganizeByStrategy<*>? = null
	private var cachedRules: OrganizeByRules? = null

	fun execute(
		parent: SmartFolder,
		rules: OrganizeByRules,
		resourceMapper: OrganizeByResourceMapper
	): List<OrganizedTracksGroup>? {
		val oldRules = cachedRules
		if (strategy == null || oldRules == null || oldRules.type != rules.type) {
			// Update strategy if organization type changed
			strategy = rules.type.strategy
			cachedRules = rules
		}
		return strategy?.apply(parent, rules, resourceMapper)
	}
}