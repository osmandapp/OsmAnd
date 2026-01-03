package net.osmand.shared.gpx.organization

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.organization.strategy.OrganizeByStrategy

class TracksOrganizer(val parent: SmartFolder) {

	private var cachedRules: OrganizeByRules? = null
	private var cachedOrganizedGroups: List<OrganizedTracksGroup>? = null
	private var strategy: OrganizeByStrategy<*>? = null

	fun getOrganizedTrackItems(
		rules: OrganizeByRules,
		resourceMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>? {
		var strategyChanged = false

		val oldRules = cachedRules
		if (strategy == null || oldRules == null || oldRules.type != rules.type) {
			// Update strategy if organization type changed
			strategy = rules.type.strategy
			cachedRules = rules
			strategyChanged = true
		}

		val stepChanged = (oldRules?.stepSize ?: false) != rules.stepSize
		if (!strategyChanged && !stepChanged && cachedOrganizedGroups != null) {
			return cachedOrganizedGroups
		}
		return strategy?.apply(parent, rules, resourceMapper)
	}

	fun clearCache() {
		cachedRules = null
		cachedOrganizedGroups = null
	}
}