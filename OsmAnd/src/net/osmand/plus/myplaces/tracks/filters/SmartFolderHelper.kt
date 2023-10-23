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
import java.util.Date

class SmartFolderHelper(val app: OsmandApplication) : StateChangedListener<String> {
	private val LOG = PlatformUtil.getLog(SmartFolderHelper::class.java)

	private val preference: CommonPreference<String>
	private val gson: Gson

	private var smartFolderCollection: MutableList<SmartFolder> = ArrayList()
	private val allAvailableTrackItems = HashSet<TrackItem>()
	private val allAvailableFolders = HashSet<String>()
	private val updateListeners = ArrayList<SmartFolderUpdateListener>()

	companion object {
		private const val TRACK_FILTERS_SETTINGS_PREF = "track_filters_settings_pref"
	}

	init {
		gson = GsonBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.create()
		preference = app.settings.registerStringPreference(TRACK_FILTERS_SETTINGS_PREF, "")
			.makeGlobal()
			.makeShared()
		preference.addListener(this)
		readSettings()
	}

	private fun onSettingsChanged() {
		updateSmartFolderSettings()
	}

	private fun readSettings() {
		var newCollection = ArrayList<SmartFolder>()
		val settingsJson = preference.get()
		if (!Algorithms.isEmpty(settingsJson)) {
			TrackFilterList.parseFilters(settingsJson, this)?.let { savedFilters ->
				for (smartFolder in savedFilters) {
					smartFolder.filters?.let {
						val newFilters: MutableList<BaseTrackFilter> = mutableListOf()
						for (filter in it) {
							var newFilter =
								TrackFiltersHelper.createFilter(app, filter.filterType, null)
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

	fun updateSmartFolderSettings() {
		SmartFoldersUpdateTask(app).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
	}

	fun resetSmartFoldersItems() {
		val collection = ArrayList(smartFolderCollection)
		for (smartFolder in collection) {
			smartFolder.trackItems.clear()
		}
	}

	fun getEnabledFilters(filters: MutableList<BaseTrackFilter>?): ArrayList<BaseTrackFilter> {
		var enabledFilters = ArrayList<BaseTrackFilter>()
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
		var enabledFilters = getEnabledFilters(filters)
		smartFolder.filters = enabledFilters
		writeSettings()
		updateSmartFolderItems(smartFolder)
		notifyFolderSavedListeners(smartFolder)
	}

	fun saveNewSmartFolder(name: String, filters: MutableList<BaseTrackFilter>?) {
		var enabledFilters = getEnabledFilters(filters)
		val newFolder = SmartFolder(name)
		newFolder.creationTime = Date().time
		newFolder.filters = enabledFilters
		smartFolderCollection.add(newFolder)
		updateSmartFolderItems(newFolder);
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
			updateListeners.add(listener)
		}
	}

	fun removeUpdateListener(listener: SmartFolderUpdateListener) {
		if (updateListeners.contains(listener)) {
			updateListeners.remove(listener)
		}
	}


	private fun writeSettings() {
		val json = gson.toJson(ArrayList(smartFolderCollection))
		preference.set(json)
	}

	fun isSmartFolderPresent(name: String): Boolean {
		return getSmartFolderByName(name) != null
	}

	fun addAvailableTrackFolder(folderName: String) {
		if (!Algorithms.isEmpty(folderName)) {
			allAvailableFolders.add(folderName)
		}
	}

	fun getAvailableFolders(): Set<String> {
		val items = HashSet<String>()
		items.addAll(allAvailableFolders)
		return items
	}

	fun addTrackItemToSmartFolder(item: TrackItem) {
		LOG.debug("addTrackItemToSmartFolder")
		if (!allAvailableTrackItems.contains(item)) {
			allAvailableTrackItems.add(item)
		}
		val smartFolders = ArrayList(smartFolderCollection)
		for (smartFolder in smartFolders) {
			var trackAccepted = true
			smartFolder.filters?.let { filtersValue ->
				for (filter in filtersValue) {
					if (filter.isEnabled() && !filter.isTrackAccepted(item)) {
						trackAccepted = false
						break
					}
				}
			}
			if (trackAccepted) {
				if (!smartFolder.trackItems.contains(item)) {
					smartFolder.addTrackItem(item)
				}
			}
		}
	}

	private fun getSmartFolderByName(name: String): SmartFolder? {
		val smartFolders = ArrayList(smartFolderCollection)
		for (folder in smartFolders) {
			if (Algorithms.stringsEqual(folder.folderName, name)) {
				return folder
			}
		}
		return null
	}

	fun getSmartFolders(): MutableList<SmartFolder> {
		val smartFolders = ArrayList<SmartFolder>()
		smartFolders.addAll(smartFolderCollection)
		return smartFolders
	}


	fun renameSmartFolder(smartFolder: SmartFolder, newName: String) {
		smartFolder.folderName = newName
		writeSettings()
		notifyFolderRenamedListeners(smartFolder)
	}


	fun deleteSmartFolder(smartFolder: SmartFolder) {
		val smartFolders = ArrayList<SmartFolder>()
		smartFolders.remove(smartFolder)
		smartFolderCollection = smartFolders
		writeSettings()
		notifyUpdateListeners()
	}

	@WorkerThread
	fun updateSmartFolderItems(smartFolder: SmartFolder) {
		LOG.debug("updateSmartFolderItems ${smartFolder.folderName}")
		smartFolder.trackItems.clear()
		val filters = smartFolder.filters
		if (filters == null) {
			smartFolder.trackItems.addAll(allAvailableTrackItems)
		} else {
			for (item in allAvailableTrackItems) {
				var trackAccepted = true
				for (filter in filters) {
					if (!filter.isTrackAccepted(item)) {
						trackAccepted = false
						break
					}
				}
				if (trackAccepted) {
					smartFolder.trackItems.add(item)
				}
			}
		}
		smartFolder.updateAnalysis()
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
		return HashSet(allAvailableTrackItems)
	}

	private class SmartFoldersUpdateTask(
		private val app: OsmandApplication,
	) : AsyncTask<Void, Void, Void?>() {

		override fun doInBackground(vararg params: Void): Void? {
			app.smartFolderHelper.run {
				readSettings()
				var smartFolders = ArrayList<SmartFolder>(smartFolderCollection)
				for (folder in smartFolders) {
					updateSmartFolderItems(folder)
				}
				notifyUpdateListeners()
			}
			return null
		}

		override fun onPostExecute(result: Void?) {
		}
	}

	override fun stateChanged(p0: String?) {
		onSettingsChanged()
	}
}