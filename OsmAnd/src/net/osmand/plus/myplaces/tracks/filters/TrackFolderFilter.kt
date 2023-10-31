package net.osmand.plus.myplaces.tracks.filters

import android.util.Pair
import com.google.gson.annotations.Expose
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.FOLDER
import net.osmand.plus.track.data.TrackFolder
import net.osmand.util.Algorithms

class TrackFolderFilter(filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(R.string.folder, FOLDER, filterChangedListener), TracksCollectionFilter {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedFolders)
	}

	@Expose
	var selectedFolders = HashSet<String>()

	@Expose
	var isAllFoldersSelected = false
		set(value) {
			field = value
			filterChangedListener?.onFilterChanged()
		}

	var currentFolder: TrackFolder? = null
		set(value) {
			field = value
			value?.let {
				selectedFolders.clear()
				selectedFolders.add(it.getDirName())
			}
		}

	var allFolders: MutableList<String> = arrayListOf()
		private set
	var allFoldersCollection: HashMap<String, Int> = hashMapOf()
		private set

	fun setFullFoldersCollection(collection: List<Pair<String, Int>>) {
		val tmAllFolders = ArrayList<String>()
		val tmpCollection = HashMap<String, Int>()
		for (pair in collection) {
			tmAllFolders.add(pair.first)
			tmpCollection[pair.first] = pair.second
		}
		allFolders = tmAllFolders
		allFoldersCollection = tmpCollection
	}

	fun setFullFoldersCollection(collection: HashMap<String, Int>) {
		allFolders = ArrayList(collection.keys)
		allFoldersCollection = collection
	}

	fun updateFullCollection(items: List<TrackItem>?) {
		if (Algorithms.isEmpty(items)) {
			allFoldersCollection = HashMap()
		} else {
			val newCollection = HashMap<String, Int>()
			for (item in items!!) {
				val folderName = item.dataItem?.containingFolder ?: ""
				val count = newCollection[folderName] ?: 0
				newCollection[folderName] = count + 1
			}
			allFoldersCollection = newCollection
		}
	}

	fun setSelectedItems(selectedItems: List<String>) {
		selectedFolders = HashSet(selectedItems)
		filterChangedListener?.onFilterChanged()
	}

	fun addSelectedItems(selectedItems: List<String>) {
		selectedFolders.addAll(selectedItems)
	}

	fun clearSelectedItems() {
		selectedFolders = HashSet()
	}

	fun setFolderSelected(folder: String, selected: Boolean) {
		if (selected) {
			selectedFolders.add(folder)
		} else {
			selectedFolders.remove(folder)
		}
		filterChangedListener?.onFilterChanged()
	}

	fun isFolderSelected(folder: String): Boolean {
		return selectedFolders.contains(folder)
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isAllFoldersSelected) {
			return true
		} else {
			if (!Algorithms.isEmpty(selectedFolders)) {
				for (folder in selectedFolders) {
					trackItem.dataItem?.let { gpxDataItem ->
						if (Algorithms.stringsEqual(gpxDataItem.containingFolder, folder)) {
							return true
						}
					}
				}
			}
		}
		return false
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is TrackFolderFilter) {
			selectedFolders.clear()
			selectedFolders.addAll(value.selectedFolders)
			for (folder in value.selectedFolders) {
				if (!allFolders.contains(folder)) {
					allFolders.add(folder)
					allFoldersCollection[folder] = 0
				}
			}
			filterChangedListener?.onFilterChanged()
		}
	}

	override fun getSelectedItems(): List<String> {
		return ArrayList(selectedFolders)
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is TrackFolderFilter &&
				other.selectedFolders.size == selectedFolders.size &&
				areAllFoldersSelected(other.selectedFolders)
	}

	private fun areAllFoldersSelected(cities: Set<String>): Boolean {
		for (city in cities) {
			if (!isFolderSelected(city)) {
				return false
			}
		}
		return true
	}
}