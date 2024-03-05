package net.osmand.plus.myplaces.tracks.filters

import android.os.AsyncTask
import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.osmand.PlatformUtil
import net.osmand.StateChangedListener
import net.osmand.plus.OsmandApplication
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.TrackFiltersHelper
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.track.data.SmartFolder
import net.osmand.util.Algorithms
import net.osmand.util.CollectionUtils
import java.util.Date

class SmartFolderHelper(val app: OsmandApplication) {

	private val preference: CommonPreference<String> =
		app.settings.registerStringPreference(TRACK_FILTERS_SETTINGS_PREF, "")
			.makeGlobal()
			.makeShared()
	private val gson: Gson = GsonBuilder()
		.excludeFieldsWithoutExposeAnnotation()
		.create()

	private var smartFolderCollection: MutableList<SmartFolder> = ArrayList()
	private var allAvailableTrackItems = HashSet<TrackItem>()
	private var updateListeners: MutableList<SmartFolderUpdateListener> = mutableListOf()
	private var isWritingSettings = false
	private val settingsChangedListener = StateChangedListener<String> {
		onSettingsChanged()
	}

	companion object {
//		private val LOG = PlatformUtil.getLog(SmartFolderHelper::class.java)
		private const val TRACK_FILTERS_SETTINGS_PREF = "track_filters_settings_pref"
	}

	init {
		preference.addListener(settingsChangedListener)
		readSettings()
	}

	private fun onSettingsChanged() {
		if (!isWritingSettings) {
			updateSmartFolderSettings()
		}
	}

	private fun readSettings() {
		val newCollection = ArrayList<SmartFolder>()
		val settingsJson = preference.get()
		if (!Algorithms.isEmpty(settingsJson)) {
			TrackFilterList.parseFilters(settingsJson)?.let { savedFilters ->
				for (smartFolder in savedFilters) {
					smartFolder.filters?.let {
						val newFilters: MutableList<BaseTrackFilter> = mutableListOf()
						for (filter in it) {
							val newFilter =
								TrackFiltersHelper.createFilter(app, filter.trackFilterType, null)
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
		SmartFoldersUpdateTask(app).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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
		private val app: OsmandApplication,
	) : AsyncTask<Void, Void, Void?>() {

		@Deprecated("Deprecated in Java")
		override fun doInBackground(vararg params: Void): Void? {
			readSettings()
			for (folder in smartFolderCollection) {
				updateSmartFolderItems(folder)
			}
			return null
		}

		@Deprecated("Deprecated in Java")
		override fun onPostExecute(result: Void?) {
			notifyUpdateListeners()
		}
	}
}
