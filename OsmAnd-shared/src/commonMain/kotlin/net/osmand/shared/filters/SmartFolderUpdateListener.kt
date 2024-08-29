package net.osmand.plus.myplaces.tracks.filters

import net.osmand.shared.filters.SmartFolder

interface SmartFolderUpdateListener {
	fun onSmartFoldersUpdated() {}
	fun onSmartFolderUpdated(smartFolder: SmartFolder) {}
	fun onSmartFolderRenamed(smartFolder: SmartFolder) {}
	fun onSmartFolderSaved(smartFolder: SmartFolder) {}
	fun onSmartFolderCreated(smartFolder: SmartFolder) {}
}
