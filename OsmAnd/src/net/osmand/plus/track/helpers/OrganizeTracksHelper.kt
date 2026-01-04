package net.osmand.plus.track.helpers

import net.osmand.plus.OsmandApplication
import net.osmand.plus.track.AndroidOrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.OrganizeByRules
import net.osmand.shared.gpx.organization.enums.OrganizeByType

// TODO: need to be improved
class OrganizeTracksHelper(val app: OsmandApplication) {

	init {
		// TODO: use better approach to initialize mapper
		AndroidOrganizeTracksResourceMapper.app = app
	}

	private val typeCache = mutableMapOf<String, OrganizeByType>()
	private val stepCache = mutableMapOf<String, Int>()

	fun getStepSize(folderId: String): Int? {
		return stepCache[folderId]
	}

	fun getOrganizeByType(folderId: String): OrganizeByType? {
		return typeCache[folderId]
	}

	fun setStepSize(folderId: String, stepSize: Int?) {
		if (stepSize != null) {
			stepCache[folderId] = stepSize
		} else {
			stepCache.remove(folderId)
		}
		updateFolder(folderId)
	}

	fun setOrganizeByType(folderId: String, type: OrganizeByType?) {
		if (type != null) {
			typeCache[folderId] = type
		} else {
			typeCache.remove(folderId)
		}
		updateFolder(folderId)
	}

	private fun updateFolder(folderId: String) {
		val folder = app.smartFolderHelper.getSmartFolderById(folderId)
		val type = typeCache[folderId]
		val stepSize = stepCache[folderId] ?: 10_000 // TODO: if type changed - use preselected step size value
		val rules = if (type != null) OrganizeByRules(type, stepSize) else null
		folder?.setOrganizeByRules(rules)
		// TODO: try to find better way to update
		app.smartFolderHelper.notifyFolderUpdatedListeners(folder ?: return)
	}
}