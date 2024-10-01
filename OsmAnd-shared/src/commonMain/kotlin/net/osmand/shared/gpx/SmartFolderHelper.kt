package net.osmand.shared.gpx

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.osmand.shared.KAsyncTask
import net.osmand.shared.api.KStateChangedListener
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.gpx.data.SmartFolder
import net.osmand.shared.gpx.filters.BaseTrackFilter
import net.osmand.shared.gpx.filters.TrackFilterList
import net.osmand.shared.gpx.filters.TrackFiltersHelper
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.PlatformUtil

object SmartFolderHelper {

	private const val TRACK_FILTERS_SETTINGS_PREF = "track_filters_settings_pref"

	private var smartFolderCollection: List<SmartFolder> = listOf()
	private var allAvailableTrackItems = HashSet<TrackItem>()
	private var updateListeners: List<SmartFolderUpdateListener> = listOf()
	private var isWritingSettings = false
	private val osmAndSettings: SettingsAPI = PlatformUtil.getOsmAndContext().getSettings()
	private val settingsChangedListener = object : KStateChangedListener<String> {
		override fun stateChanged(change: String) {
			onSettingsChanged()
		}
	}

	val json = Json {
		isLenient = true
		ignoreUnknownKeys = true
		classDiscriminator = "className"
	}

	init {
		osmAndSettings.registerPreference(TRACK_FILTERS_SETTINGS_PREF, "")
		osmAndSettings.addPreferenceListener(TRACK_FILTERS_SETTINGS_PREF, settingsChangedListener)
		readSettings()
	}

	private fun onSettingsChanged() {
		if (!isWritingSettings) {
			updateSmartFolderSettings()
		}
	}

	private fun readSettings() {
		val newCollection = ArrayList<SmartFolder>()
		val settingsJson = osmAndSettings.getStringPreference(TRACK_FILTERS_SETTINGS_PREF, "")
		if (!KAlgorithms.isEmpty(settingsJson)) {
			TrackFilterList.parseFilters(settingsJson)?.let { savedFilters ->
				for (smartFolder in savedFilters) {
					smartFolder.filters?.let {
						val newFilters: MutableList<BaseTrackFilter> = mutableListOf()
						for (filter in it) {
							val newFilter =
								TrackFiltersHelper.createFilter(filter.trackFilterType, null)
							newFilter.initWithValue(filter)
							newFilters.add(newFilter)
						}
						smartFolder.filters = newFilters
					}
				}
				newCollection.addAll(savedFilters)
			}
		}
		smartFolderCollection = newCollection
	}

	private fun updateSmartFolderSettings() {
		SmartFoldersUpdateTask().execute()
	}

	fun resetSmartFoldersItems() {
		val collection = ArrayList(smartFolderCollection)
		for (smartFolder in collection) {
			smartFolder.resetItems()
		}
	}

	private fun getEnabledFilters(filters: MutableList<BaseTrackFilter>?): ArrayList<BaseTrackFilter> {
		val enabledFilters = ArrayList<BaseTrackFilter>()
		filters?.let {
			for (filter in filters) {
				if (filter.isEnabled()) {
					enabledFilters.add(filter)
				}
			}
		}
		return enabledFilters
	}

	fun saveSmartFolder(smartFolder: SmartFolder, filters: MutableList<BaseTrackFilter>?) {
		val enabledFilters = getEnabledFilters(filters)
		smartFolder.filters = enabledFilters
		writeSettings()
		updateSmartFolderItems(smartFolder)
		notifyFolderSavedListeners(smartFolder)
	}

	fun saveNewSmartFolder(name: String, filters: MutableList<BaseTrackFilter>?) {
		val enabledFilters = getEnabledFilters(filters)
		val newFolder = SmartFolder(name)
		newFolder.creationTime = currentTimeMillis()
		newFolder.filters = enabledFilters
		smartFolderCollection = KCollectionUtils.addToList(smartFolderCollection, newFolder)
		updateSmartFolderItems(newFolder)
		writeSettings()
		notifyFolderCreatedListeners(newFolder)
	}

	fun notifyUpdateListeners() {
		for (listener in updateListeners) {
			listener.onSmartFoldersUpdated()
		}
	}

	private fun notifyFolderCreatedListeners(smartFolder: SmartFolder) {
		for (listener in updateListeners) {
			listener.onSmartFolderCreated(smartFolder)
		}
	}

	private fun notifyFolderUpdatedListeners(smartFolder: SmartFolder) {
		for (listener in updateListeners) {
			listener.onSmartFolderUpdated(smartFolder)
		}
	}

	private fun notifyFolderSavedListeners(smartFolder: SmartFolder) {
		for (listener in updateListeners) {
			listener.onSmartFolderSaved(smartFolder)
		}
	}

	private fun notifyFolderRenamedListeners(smartFolder: SmartFolder) {
		for (listener in updateListeners) {
			listener.onSmartFolderRenamed(smartFolder)
		}
	}

	fun addUpdateListener(listener: SmartFolderUpdateListener) {
		if (!updateListeners.contains(listener)) {
			updateListeners = KCollectionUtils.addToList(updateListeners, listener)
		}
	}

	fun removeUpdateListener(listener: SmartFolderUpdateListener) {
		if (updateListeners.contains(listener)) {
			updateListeners = KCollectionUtils.removeFromList(updateListeners, listener)
		}
	}

	private fun writeSettings() {
		isWritingSettings = true
		val json = json.encodeToString(smartFolderCollection)
		osmAndSettings.setStringPreference(TRACK_FILTERS_SETTINGS_PREF, json)
		isWritingSettings = false
	}

	fun isSmartFolderPresent(name: String): Boolean {
		return getSmartFolderByName(name) != null
	}

	private fun getSmartFolderByName(name: String): SmartFolder? {
		for (folder in smartFolderCollection) {
			if (KAlgorithms.stringsEqual(folder.folderName, name)) {
				return folder
			}
		}
		return null
	}

	fun getSmartFolders(): MutableList<SmartFolder> {
		return ArrayList(smartFolderCollection)
	}

	fun renameSmartFolder(smartFolder: SmartFolder, newName: String) {
		smartFolder.folderName = newName
		writeSettings()
		notifyFolderRenamedListeners(smartFolder)
	}

	fun deleteSmartFolder(smartFolder: SmartFolder) {
		smartFolderCollection = KCollectionUtils.removeFromList(smartFolderCollection, smartFolder)
		writeSettings()
		notifyUpdateListeners()
	}

	fun addTrackItemToSmartFolder(item: TrackItem) {
		val newSet = allAvailableTrackItems
		newSet.add(item)
		allAvailableTrackItems = newSet
		addTracksToSmartFolders(arrayListOf(item), smartFolderCollection)
	}

	fun addTrackItemsToSmartFolder(items: List<TrackItem>) {
		if (smartFolderCollection.isEmpty()) {
			return
		}
		val newSet = allAvailableTrackItems
		newSet.addAll(items)
		allAvailableTrackItems = newSet
		addTracksToSmartFolders(items, smartFolderCollection)
	}

	private fun addTracksToSmartFolders(items: List<TrackItem>, smartFolders: List<SmartFolder>) {
		for (item in items) {
			for (smartFolder in smartFolders) {
				var trackAccepted = true
				smartFolder.filters?.let { smartFolderFilters ->
					for (filter in smartFolderFilters) {
						if (!filter.isTrackAccepted(item)) {
							trackAccepted = false
							break
						}
					}
				}
				if (trackAccepted) {
					smartFolder.addTrackItem(item)
				}
			}
		}
	}

	fun onGpxFileDeleted(gpxFile: KFile) {
		val newAllTracks = HashSet<TrackItem>(allAvailableTrackItems)
		for (trackItem in newAllTracks) {
			if (trackItem.path == gpxFile.absolutePath()) {
				newAllTracks.remove(trackItem)
				allAvailableTrackItems = newAllTracks
				break
			}
		}
		updateAllSmartFoldersItems()
	}

	fun onTrackRenamed(srcTrackFile: KFile, destTrackFile: KFile) {
		val newAllTracks = HashSet<TrackItem>(allAvailableTrackItems)
		for (trackItem in newAllTracks) {
			if (trackItem.path == srcTrackFile.absolutePath()) {
				newAllTracks.remove(trackItem)
				newAllTracks.add(TrackItem(destTrackFile))
				allAvailableTrackItems = newAllTracks
				break
			}
		}
		updateAllSmartFoldersItems()
		notifyUpdateListeners()
	}

	private fun updateSmartFolderItems(smartFolder: SmartFolder) {
		smartFolder.resetItems()
		addTracksToSmartFolders(ArrayList(allAvailableTrackItems), arrayListOf(smartFolder))
		notifyFolderUpdatedListeners(smartFolder)
	}

	fun getSmartFolder(name: String): SmartFolder? {
		for (folder in smartFolderCollection) {
			if (KAlgorithms.stringsEqual(folder.folderName, name)) {
				return folder
			}
		}
		return null
	}

	fun refreshSmartFolder(smartFolder: SmartFolder) {
		updateSmartFolderItems(smartFolder)
	}

	fun getAllAvailableTrackItems(): HashSet<TrackItem> {
		return allAvailableTrackItems
	}

	private class SmartFoldersUpdateTask : KAsyncTask<Unit, Unit, Unit>() {

		override suspend fun doInBackground(vararg params: Unit) {
			readSettings()
			updateAllSmartFoldersItems()
		}

		override fun onPostExecute(result: Unit) {
			notifyUpdateListeners()
		}
	}

	private fun updateAllSmartFoldersItems() {
		for (smartFolder in smartFolderCollection) {
			updateSmartFolderItems(smartFolder)
		}
	}
}
