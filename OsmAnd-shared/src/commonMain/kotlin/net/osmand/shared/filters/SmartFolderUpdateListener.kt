package net.osmand.shared.filters

interface SmartFolderUpdateListener {
	fun onSmartFoldersUpdated()
	fun onSmartFolderUpdated(smartFolder: SmartFolder)
	fun onSmartFolderRenamed(smartFolder: SmartFolder)
	fun onSmartFolderSaved(smartFolder: SmartFolder)
	fun onSmartFolderCreated(smartFolder: SmartFolder)
}
