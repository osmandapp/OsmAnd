package net.osmand.shared.filters

import androidx.annotation.WorkerThread

interface SmartFolderUpdateListener {
	fun onSmartFoldersUpdated()

	@WorkerThread
	fun onSmartFolderUpdated(smartFolder: SmartFolder)
	fun onSmartFolderRenamed(smartFolder: SmartFolder)
	fun onSmartFolderSaved(smartFolder: SmartFolder)
	fun onSmartFolderCreated(smartFolder: SmartFolder)
}
