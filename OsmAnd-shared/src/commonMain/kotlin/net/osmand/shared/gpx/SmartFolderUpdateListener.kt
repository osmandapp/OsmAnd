package net.osmand.shared.gpx

import net.osmand.shared.gpx.data.SmartFolder

interface SmartFolderUpdateListener {
	fun onSmartFoldersUpdated()

	fun onSmartFolderUpdated(smartFolder: SmartFolder)
	fun onSmartFolderRenamed(smartFolder: SmartFolder)
	fun onSmartFolderSaved(smartFolder: SmartFolder)
	fun onSmartFolderCreated(smartFolder: SmartFolder)
}
