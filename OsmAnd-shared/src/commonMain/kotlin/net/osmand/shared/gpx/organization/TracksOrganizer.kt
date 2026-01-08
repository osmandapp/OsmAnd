package net.osmand.shared.gpx.organization

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.organization.strategy.OrganizeByStrategy

class TracksOrganizer(val parent: SmartFolder) {

	var params: OrganizeByParameter? = null
		private set
	private var cachedOrganizedGroups: List<OrganizedTracksGroup>? = null

	fun getOrganizedTrackItems(
		resourceMapper: OrganizeTracksResourceMapper
	): List<OrganizedTracksGroup>? {
		params?.let { parameters ->
			parameters.type.let {
				if (cachedOrganizedGroups == null) {
					cachedOrganizedGroups = it.strategy.apply(parent, parameters, resourceMapper)
				}
			}
		}
		return cachedOrganizedGroups
	}

	fun setOrganizeByParams(newParameter: OrganizeByParameter?) {
		if (newParameter != params) {
			params = newParameter
			clearCache()
		}
	}

	fun clearCache() {
		cachedOrganizedGroups = null
	}

	fun getOrganizeByStrategy(): OrganizeByStrategy? {
		return params?.type?.strategy
	}
}