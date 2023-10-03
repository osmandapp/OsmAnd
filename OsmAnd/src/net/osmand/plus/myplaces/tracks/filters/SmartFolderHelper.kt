package net.osmand.plus.myplaces.tracks.filters

import androidx.annotation.WorkerThread
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.osmand.plus.OsmandApplication
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.track.data.SmartFolder
import net.osmand.util.Algorithms

class SmartFolderHelper(val app: OsmandApplication) {
	private val preference: CommonPreference<String>
	private val gson: Gson
	private val savedFilters: MutableMap<String, MutableList<BaseTrackFilter>?> = HashMap()
	private val smartFolderCollection: MutableList<SmartFolder> = ArrayList()
	val allAvailableTrackItems = ArrayList<TrackItem>()
	private val updateListeners = ArrayList<SmartFolderUpdateListener>()

	companion object {
		private const val TRACK_FILTERS_SETTINGS_PREF = "track_filters_settings_pref"
	}

	init {
		gson = GsonBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.create()
		preference = app.settings.registerStringPreference(TRACK_FILTERS_SETTINGS_PREF, "")
			.makeProfile()
			.cache()
		readSettings()
	}

	private fun readSettings() {
		val settingsJson = preference.get()
		if (!Algorithms.isEmpty(settingsJson)) {
			val savedFilters = TrackFilterList.parseFilters(settingsJson, this)
			if (savedFilters != null) {
				this.savedFilters.putAll(savedFilters)
				resetSmartFolders()
			}
		}
	}

	fun resetSmartFolders() {
		smartFolderCollection.clear()
		for (filter in savedFilters) {
			val smartFolder = SmartFolder(filter.key)
			smartFolder.filters = filter.value
			smartFolderCollection.add(smartFolder)
		}
	}

	private fun getEnabledFilters(filters: MutableList<BaseTrackFilter>?): ArrayList<BaseTrackFilter> {
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
		savedFilters[smartFolder.folderName] = enabledFilters
		smartFolder.filters = enabledFilters
		writeSettings()
		updateSmartFolderItems(smartFolder)
		notifyFolderSavedListeners(smartFolder)
	}

	fun saveNewSmartFolder(name: String, filters: MutableList<BaseTrackFilter>?) {
		var enabledFilters = getEnabledFilters(filters)
		savedFilters[name] = enabledFilters
		val newFolder = SmartFolder(name)
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
		val json = gson.toJson(savedFilters)
		preference.set(json)
	}

	fun isSmartFolderPresent(name: String): Boolean {
		return savedFilters.contains(name)
	}

	fun addTrackItemToSmartFolder(item: TrackItem) {
		if (!allAvailableTrackItems.contains(item)) {
			allAvailableTrackItems.add(item)
		}
		for (savedFilter in savedFilters) {
			var trackAccepted = true
			savedFilter.value?.let { filtersValue ->
				for (filter in filtersValue) {
					if (filter.isEnabled() && !filter.isTrackAccepted(item)) {
						trackAccepted = false
						break
					}
				}
			}
			if (trackAccepted) {
				var smartFolder = getSmartFolderByName(savedFilter.key)
				if (smartFolder == null) {
					smartFolder = SmartFolder(savedFilter.key)
					smartFolderCollection.add(smartFolder)
				}
				if (!smartFolder.trackItems.contains(item)) {
					smartFolder.addTrackItem(item)
				}
			}
		}
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
		val smartFolders = ArrayList<SmartFolder>()
		smartFolders.addAll(smartFolderCollection)
		return smartFolderCollection
	}


	fun renameSmartFolder(smartFolder: SmartFolder, newName: String) {
		val savedFilter = savedFilters[smartFolder.folderName]
		savedFilters.remove(smartFolder.folderName)
		savedFilters[newName] = savedFilter
		smartFolder.folderName = newName
		writeSettings()
		notifyFolderRenamedListeners(smartFolder)
	}


	fun deleteSmartFolder(smartFolder: SmartFolder) {
		val savedFilter = savedFilters[smartFolder.folderName]
		savedFilter?.let {
			savedFilters.remove(smartFolder.folderName)
			smartFolderCollection.remove(smartFolder)
		}
		writeSettings()
		notifyUpdateListeners()
	}

	@WorkerThread
	fun updateSmartFolderItems(smartFolder: SmartFolder) {
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
}