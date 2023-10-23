package net.osmand.plus.myplaces.tracks.filters

import android.util.Pair
import com.google.gson.annotations.Expose
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.FOLDER
import net.osmand.plus.track.data.TrackFolder
import net.osmand.util.Algorithms
import java.io.File

class TrackFolderFilter(filterChangedListener: FilterChangedListener) :
	BaseTrackFilter(R.string.folder, FOLDER, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedFolders)
	}

	@Expose
	val selectedFolders = ArrayList<String>()

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
		for (pair in collection) {
			allFolders.add(pair.first)
			allFoldersCollection[pair.first] = pair.second
		}
	}

	fun setSelectedFolders(folder: String, selected: Boolean) {
		if (selected) {
			if (!selectedFolders.contains(folder)) {
				selectedFolders.add(folder)
			}
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
						gpxDataItem.file.parent?.let { parentFolderName ->
							var parent = File(parentFolderName).name
							if (Algorithms.stringsEqual(parent, folder)) {
								return true
							}
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

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is TrackFolderFilter &&
				other.selectedFolders.size == selectedFolders.size &&
				areAllFoldersSelected(other.selectedFolders)
	}

	private fun areAllFoldersSelected(cities: List<String>): Boolean {
		for (city in cities) {
			if (!isFolderSelected(city)) {
				return false
			}
		}
		return true
	}
}