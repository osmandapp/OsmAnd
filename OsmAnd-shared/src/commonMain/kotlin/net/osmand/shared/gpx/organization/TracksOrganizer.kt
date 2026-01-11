package net.osmand.shared.gpx.organization

import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.data.TracksGroup
import net.osmand.shared.util.PlatformUtil

class TracksOrganizer(val parent: TracksGroup) {

	var params: OrganizeByParams? = null
		private set
	private var cachedOrganizedGroups: List<OrganizedTracksGroup>? = null

	fun initParams(params: OrganizeByParams?) {
		this.params = params
	}

	fun getOrganizedTrackItems(): List<OrganizedTracksGroup> {
		if (cachedOrganizedGroups == null) {
			params?.let { parameters ->
				parameters.type.let {
					val mapper = PlatformUtil.getOsmAndContext().getOrganizeTracksResourceMapper()
					cachedOrganizedGroups = it.strategy.apply(parent, parameters, mapper)
				}
			}
		}
		return cachedOrganizedGroups ?: emptyList()
	}

	fun setOrganizeByParams(newParameter: OrganizeByParams?) {
		if (newParameter != params) {
			params = newParameter
			clearCache()
		}
	}

	fun clearCache() {
		cachedOrganizedGroups = null
	}
}