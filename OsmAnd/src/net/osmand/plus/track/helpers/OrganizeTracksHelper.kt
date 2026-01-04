package net.osmand.plus.track.helpers

import net.osmand.plus.OsmandApplication
import net.osmand.plus.track.AndroidOrganizeTracksResourceMapper
import net.osmand.shared.gpx.organization.OrganizeByRules
import net.osmand.shared.gpx.organization.enums.OrganizeByType

// TODO: needed only for testing purposes
class OrganizeTracksHelper(val app: OsmandApplication) {

	init {
		// TODO: use better approach to initialize mapper
		AndroidOrganizeTracksResourceMapper.app = app
	}

	private val typeCache = mutableMapOf<String, OrganizeByType>()
	private val stepCache = mutableMapOf<String, Double>()

	fun getOrganizeByType(folderId: String) = typeCache[folderId]

	fun getStepSize(folderId: String) = stepCache[folderId]

	fun setOrganizeByType(folderId: String, type: OrganizeByType?) {
		val previousType = typeCache[folderId]
		val typeChanged = previousType != type

		if (type != null) {
			typeCache[folderId] = type
		} else {
			typeCache.remove(folderId)
		}

		// Update step size on flight when type changed and current type has step range
		var stepSize = stepCache[folderId]
		if (type?.stepRange != null) {
			if (typeChanged || stepSize == null) {
				stepSize = type.getDisplayUnits().toBase(type.stepRange!!.getMidpoint())
			}
			setStepSize(folderId, stepSize)
		} else {
			updateFolder(folderId, type, null)
		}
	}

	fun setStepSize(folderId: String, stepSize: Double?) {
		if (stepSize != null) {
			stepCache[folderId] = stepSize
		} else {
			stepCache.remove(folderId)
		}
		updateFolder(folderId, typeCache[folderId], stepSize)
	}

	private fun updateFolder(folderId: String, type: OrganizeByType?, stepSize: Double?) {
		val folder = app.smartFolderHelper.getSmartFolderById(folderId)
		val rules = if (type != null) OrganizeByRules(type, stepSize) else null
		folder?.setOrganizeByRules(rules)
		// TODO: try to find better way to update
		app.smartFolderHelper.notifyFolderUpdatedListeners(folder ?: return)
	}
}