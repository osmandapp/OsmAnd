package net.osmand.plus.myplaces.tracks.filters

import android.app.Activity
import android.view.View
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.track.data.SmartFolder
import net.osmand.plus.utils.UiUtilities
import net.osmand.util.Algorithms
import studio.carbonylgroup.textfieldboxes.ExtendedEditText

class SmartFolderHelper(val app: OsmandApplication) {
	private val preference: CommonPreference<String>
	private val gson: Gson
	private val savedFilters: MutableMap<String, MutableList<BaseTrackFilter>?> = HashMap()
	private val smartFolderCollection: MutableList<SmartFolder> = ArrayList()
	val allAvailableTrackItems = ArrayList<TrackItem>()
	private val updateListeners = ArrayList<SmartFolderUpdateListener>()

	companion object {
		private const val TRACK_FILTERS_SETTINGS_PREF_ID = "track_filters_settings_pref_id"
	}

	init {
		gson = GsonBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.create()
		preference = app.settings.registerStringPreference(TRACK_FILTERS_SETTINGS_PREF_ID, "")
			.makeProfile()
			.cache()
		readSettings()
	}

	fun getFilterClass(filterType: FilterType): Class<out BaseTrackFilter> {
		return when (filterType) {
			FilterType.NAME -> TrackNameFilter::class.java
			FilterType.DURATION -> DurationTrackFilter::class.java
			FilterType.TIME_IN_MOTION -> TimeInMotionTrackFilter::class.java
			FilterType.LENGTH -> LengthTrackFilter::class.java
			FilterType.AVERAGE_SPEED -> AverageSpeedTrackFilter::class.java
			FilterType.MAX_SPEED -> MaxSpeedTrackFilter::class.java
			FilterType.AVERAGE_ALTITUDE -> AverageAltitudeTrackFilter::class.java
			FilterType.MAX_ALTITUDE -> MaxAltitudeTrackFilter::class.java
			FilterType.UPHILL -> UphillTrackFilter::class.java
			FilterType.DOWNHILL -> DownhillTrackFilter::class.java
			FilterType.DATE_CREATION -> DateCreationTrackFilter::class.java
			FilterType.CITY -> CityTrackFilter::class.java
			FilterType.OTHER -> OtherTrackFilter::class.java
		}
		throw IllegalArgumentException("Unknown filterType $filterType")
	}

	private fun readSettings() {
		val settingsJson = preference.get()
		if (!Algorithms.isEmpty(settingsJson)) {
			val savedFilters = TrackFilterList.parseFilters(settingsJson, this)
			if (savedFilters != null) {
				this.savedFilters.putAll(savedFilters)
				initSmartFolders()
			}
		}
	}

	private fun initSmartFolders() {
		for (name in savedFilters.keys) {
			val smartFolder = SmartFolder(name)
			smartFolder.filters = savedFilters[name]
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

	private fun saveNewSmartFolder(name: String, filters: MutableList<BaseTrackFilter>?) {
		var enabledFilters = getEnabledFilters(filters)
		savedFilters[name] = enabledFilters
		val newFolder = SmartFolder(name)
		newFolder.filters = enabledFilters
		smartFolderCollection.add(newFolder)
		updateSmartFolderItems(newFolder);
		writeSettings()
		notifyFolderCreatedListeners(newFolder)
	}

	private fun notifyUpdateListeners() {
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

	fun resetSmartFolders() {
		smartFolderCollection.clear()
		for (filter in savedFilters) {
			val smartFolder = SmartFolder(filter.key)
			smartFolder.filters = filter.value
			smartFolderCollection.add(smartFolder)
		}
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
			if (trackAccepted || Algorithms.isEmpty(savedFilter.value)) {
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

	fun showSaveSmartFolderDialog(
		activity: Activity,
		nightMode: Boolean,
		filters: MutableList<BaseTrackFilter>?) {
		val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
		val themedInflater = UiUtilities.getInflater(activity, nightMode)
		val customLayout: View = themedInflater.inflate(R.layout.dialog_save_smart_folder, null)
		builder.setView(customLayout)
		val dialog: AlertDialog = builder.create()
		dialog.show()
		customLayout.findViewById<View>(R.id.cancel_button).setOnClickListener {
			dialog.dismiss()
		}
		customLayout.findViewById<View>(R.id.save_button).setOnClickListener {
			val input = customLayout.findViewById<ExtendedEditText>(R.id.name_input)
			val newSmartFolderName = input.text.trim().toString()
			if (isSmartFolderPresent(newSmartFolderName)) {
				Toast.makeText(app, R.string.smart_folder_name_present, Toast.LENGTH_SHORT).show()
			} else {
				saveNewSmartFolder(newSmartFolderName, filters)
				dialog.dismiss()
			}
		}
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