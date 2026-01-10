package net.osmand.plus.configmap.tracks

import net.osmand.IndexConstants
import net.osmand.plus.AppInitEvents
import net.osmand.plus.OsmandApplication
import net.osmand.plus.settings.enums.TracksSortMode
import net.osmand.shared.gpx.GpxDbHelper
import net.osmand.shared.gpx.SmartFolderHelper
import net.osmand.shared.gpx.data.OrganizedTracksGroup
import net.osmand.shared.gpx.enums.TracksSortScope
import java.io.File

/**
 * Removes surplus keys and upgrades outdated ones.
 * Upgraded keys will use folder relative path as a key (V2) instead of folder name (V1).
 */
class UpgradeTrackSortModeKeysAlgorithm(
	private val app: OsmandApplication,
	private val sortModesHelper: TrackSortModesHelper,
	private val gpxDbHelper: GpxDbHelper = app.gpxDbHelper,
	private val smartFolderHelper: SmartFolderHelper = app.smartFolderHelper
) {

	companion object {
		fun execute(app: OsmandApplication, sortModesHelper: TrackSortModesHelper) {
			UpgradeTrackSortModeKeysAlgorithm(app, sortModesHelper).execute()
		}
	}

	private fun execute() {
		if (gpxDbHelper.isInitialized()) {
			executeImpl()
		} else {
			app.appInitializer.addOnProgressListener(AppInitEvents.GPX_DB_INITIALIZED) { executeImpl() }
		}
	}

	private fun executeImpl() {
		val upgradedKeys = mutableMapOf<String, TracksSortMode>()
		for (tab in listOf(TrackTabType.ON_MAP, TrackTabType.ALL, TrackTabType.FOLDERS)) {
			upgradeKey(
				upgradedKeys = upgradedKeys,
				groupId = tab.name,
				checkOutdatedId = false,
				supportedSortScopes = listOf(TracksSortScope.TRACKS)
			)
		}
		for (folderId in collectStandardFolderIds()) {
			upgradeKey(
				upgradedKeys = upgradedKeys,
				groupId = folderId,
				checkOutdatedId = true,
				supportedSortScopes = listOf(TracksSortScope.TRACKS)
			)
		}
		for (smartFolder in smartFolderHelper.getSmartFolders()) {
			val folderId = smartFolder.getId()
			upgradeKey(
				upgradedKeys = upgradedKeys,
				groupId = folderId,
				checkOutdatedId = false,
				supportedSortScopes = smartFolder.getSupportedSortScopes()
			)
			preserveOrganizedKeys(upgradedKeys, folderId)
		}
		sortModesHelper.setSortModes(upgradedKeys)
		sortModesHelper.syncSettings()
	}

	private fun collectStandardFolderIds(): Set<String> {
		val result: MutableSet<String> = HashSet()
		// add root directory
		result.add("")
		// collect all directories registered in tracks DB
		val absolutePaths: MutableSet<String> = HashSet()
		for (item in gpxDbHelper.getDirItems()) {
			val file = item.file
			absolutePaths.add(file.absolutePath())
		}
		// collect all subdirectories which contain at least one gpx track
		for (item in gpxDbHelper.getItems()) {
			val file = item.file
			val directory = file.getParentFile()
			if (directory != null) {
				absolutePaths.add(directory.absolutePath())
			}
		}
		for (absolutePath in absolutePaths) {
			result.add(getFolderIdV2(absolutePath))
		}
		return result
	}

	private fun preserveOrganizedKeys(
		upgradedKeys: MutableMap<String, TracksSortMode>,
		parentId: String
	) {
		val organizedPrefix = OrganizedTracksGroup.getBaseId(parentId)
		for (key in sortModesHelper.allCachedInternalIds) {
			if (key.startsWith(organizedPrefix)) {
				val sortMode = sortModesHelper.getRawSortMode(key)
				if (sortMode != null) {
					upgradedKeys[key] = sortMode
				}
			}
		}
	}

	private fun upgradeKey(
		upgradedKeys: MutableMap<String, TracksSortMode>,
		groupId: String,
		checkOutdatedId: Boolean,
		supportedSortScopes: Collection<TracksSortScope>
	) {
		for (scope in supportedSortScopes) {
			val sortMode = getSortMode(groupId, scope, checkOutdatedId)
			if (sortMode != null) {
				val internalId = TrackSortModesHelper.getInternalId(groupId, scope)
				upgradedKeys[internalId] = sortMode
			}
		}
	}

	private fun getSortMode(
		groupId: String,
		scope: TracksSortScope,
		checkOutdatedId: Boolean
	): TracksSortMode? {
		val sortMode = sortModesHelper.getSortMode(groupId, scope)
		if (sortMode == null && checkOutdatedId) {
			val idV1 = getFolderIdV1(groupId)
			return if (idV1 != null) sortModesHelper.getSortMode(idV1, scope) else null
		}
		return sortMode
	}

	private fun getFolderIdV1(id: String?): String? {
		if (id != null && id.isEmpty()) {
			return IndexConstants.GPX_INDEX_DIR
		}
		val index = id?.lastIndexOf(File.separator) ?: -1
		return if (index > 0) id!!.substring(index + 1) else null
	}

	private fun getFolderIdV2(absolutePath: String): String {
		return TrackSortModesHelper.getFolderId(absolutePath)
	}
}