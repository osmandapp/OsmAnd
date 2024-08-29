package net.osmand.shared.filters

import android.os.AsyncTask
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.osmand.SharedUtil
import net.osmand.StateChangedListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.myplaces.tracks.filters.SmartFolderUpdateListener
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.shared.api.KOsmAndSettings
import net.osmand.shared.api.KStateChangedListener
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.filters.BaseTrackFilter
import net.osmand.shared.filters.SmartFolder
import net.osmand.shared.filters.TrackFiltersHelper
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil
import net.osmand.util.Algorithms
import net.osmand.util.CollectionUtils
import java.io.File
import java.util.Date

class SmartFolderHelper() {

//	private val preference: CommonPreference<String> =
//		app.settings.registerStringPreference(TRACK_FILTERS_SETTINGS_PREF, "")
//			.makeGlobal()
//			.makeShared()
//	private val gson: Gson = GsonBuilder()
//		.excludeFieldsWithoutExposeAnnotation()
//		.create()

	private var smartFolderCollection: MutableList<SmartFolder> = ArrayList()
	private var allAvailableTrackItems = HashSet<TrackItem>()
	private var updateListeners: MutableList<SmartFolderUpdateListener> = mutableListOf()
	private var isWritingSettings = false
	private val osmAndContext: KOsmAndSettings
	private val settingsChangedListener = object : KStateChangedListener<String> {
		override fun stateChanged(change: String) {
			onSettingsChanged()
		}
	}

	companion object {
		//		private val LOG = PlatformUtil.getLog(SmartFolderHelper::class.java)
		private const val TRACK_FILTERS_SETTINGS_PREF = "track_filters_settings_pref"
	}

	init {
		osmAndContext = PlatformUtil.getOsmAndContext().getSettings()
		osmAndContext.registerPreference(TRACK_FILTERS_SETTINGS_PREF, "")
		osmAndContext.addPreferenceListener(TRACK_FILTERS_SETTINGS_PREF, settingsChangedListener)
		readSettings()
	}

	private fun onSettingsChanged() {
		if (!isWritingSettings) {
			withContext(Dispatchers.Default) {
				updateSmartFolderSettings()
			}
		}
	}

	private fun readSettings() {
		val newCollection = ArrayList<SmartFolder>()
		val settingsJson = osmAndContext.getStringPreference(TRACK_FILTERS_SETTINGS_PREF, "")
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

	private suspend fun updateSmartFolderSettings() {

		SmartFoldersUpdateTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
		newFolder.creationTime = Date().time
		newFolder.filters = enabledFilters
		smartFolderCollection.add(newFolder)
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
			updateListeners = CollectionUtils.addToList(updateListeners, listener)
		}
	}

	fun removeUpdateListener(listener: SmartFolderUpdateListener) {
		if (updateListeners.contains(listener)) {
			updateListeners = CollectionUtils.removeFromList(updateListeners, listener)
		}
	}

	private fun writeSettings() {
		isWritingSettings = true
		val json = gson.toJson(smartFolderCollection)
		preference.set(json)
		isWritingSettings = false
	}

	fun isSmartFolderPresent(name: String): Boolean {
		return getSmartFolderByName(name) != null
	}

	private fun getSmartFolderByName(name: String): SmartFolder? {
		for (folder in smartFolderCollection) {
			if (Algorithms.stringsEqual(folder.folderName, name)) {
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
		smartFolderCollection = CollectionUtils.removeFromList(smartFolderCollection, smartFolder)
		writeSettings()
		notifyUpdateListeners()
	}

	fun addTrackItemToSmartFolder(item: TrackItem) {
//		LOG.debug("addTrackItemToSmartFolder " + item.name)
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

	fun onGpxFileDeleted(gpxFile: File) {
		val newAllTracks = HashSet<TrackItem>(allAvailableTrackItems)
		for (trackItem in newAllTracks) {
			if (trackItem.path == gpxFile.path) {
				newAllTracks.remove(trackItem)
				allAvailableTrackItems = newAllTracks
				break
			}
		}
		updateAllSmartFoldersItems()
	}

	fun onTrackRenamed(srcTrackFile: File, destTrackFile: File) {
		val newAllTracks = HashSet<TrackItem>(allAvailableTrackItems)
		for (trackItem in newAllTracks) {
			if (trackItem.path == srcTrackFile.path) {
				newAllTracks.remove(trackItem)
				newAllTracks.add(TrackItem(SharedUtil.kFile(destTrackFile)))
				allAvailableTrackItems = newAllTracks
				break
			}
		}
		updateAllSmartFoldersItems()
		notifyUpdateListeners()
	}

	@WorkerThread
	fun updateSmartFolderItems(smartFolder: SmartFolder) {
//		LOG.debug("updateSmartFolderItems ${smartFolder.folderName}")
		smartFolder.resetItems()
		addTracksToSmartFolders(ArrayList(allAvailableTrackItems), arrayListOf(smartFolder))
		notifyFolderUpdatedListeners(smartFolder)
	}

	fun getSmartFolder(name: String): SmartFolder? {
		for (folder in smartFolderCollection) {
			if (Algorithms.stringsEqual(folder.folderName, name)) {
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


	private inner class SmartFoldersUpdateTask(
	) : AsyncTask<Void, Void, Void?>() {

		@Deprecated("Deprecated in Java")
		override fun doInBackground(vararg params: Void): Void? {
			readSettings()
			updateAllSmartFoldersItems()
			return null
		}

		@Deprecated("Deprecated in Java")
		override fun onPostExecute(result: Void?) {
			notifyUpdateListeners()
		}
	}

	private fun updateAllSmartFoldersItems() {
		for (smartFolder in smartFolderCollection) {
			updateSmartFolderItems(smartFolder)
		}
	}
}
