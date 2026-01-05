package net.osmand.shared.gpx.organization

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.SmartFolder

class TracksOrganizer(val parent: SmartFolder) {

	private var rules: OrganizeByRules? = null
	private var cachedOrganizedGroups: List<OrganizedTracksGroup>? = null

	fun getOrganizedTrackItems(
		resourceMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>? {
		rules?.type?.let {
			val rules = rules
			if (cachedOrganizedGroups == null && rules != null) {
				cachedOrganizedGroups = it.strategy?.apply(parent, rules, resourceMapper)
			}
		}
		return cachedOrganizedGroups
	}

	fun setOrganizeByRules(newRules: OrganizeByRules?) {
		if (newRules != rules) {
			rules = newRules
			clearCache()
		}
	}

	fun clearCache() {
		cachedOrganizedGroups = null
	}
}