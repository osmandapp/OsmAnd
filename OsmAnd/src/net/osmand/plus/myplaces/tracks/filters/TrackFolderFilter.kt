package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.plus.myplaces.tracks.filters.FilterType.FOLDER
import net.osmand.plus.track.data.TrackFolder
import net.osmand.plus.track.helpers.GpxDbUtils
import net.osmand.util.Algorithms

class TrackFolderFilter(app: OsmandApplication, filterChangedListener: FilterChangedListener?) :
	ListTrackFilter(app, R.string.folder, FOLDER, filterChangedListener) {

	@Expose
	private var selectedFolders = HashSet<String>()

	var currentFolder: TrackFolder? = null
		set(value) {
			field = value
			value?.let {
				selectedItems = arrayListOf(it.getDirName())
			}
		}

	fun updateFullCollection(items: List<TrackItem>?) {
		if (Algorithms.isEmpty(items)) {
			allItemsCollection = HashMap()
		} else {
			val newCollection = HashMap<String, Int>()
			for (item in items!!) {
				val folderName = item.dataItem?.file?.let {
					GpxDbUtils.getGpxFileDir(app, it)
				} ?: ""
				val count = newCollection[folderName] ?: 0
				newCollection[folderName] = count + 1
			}
			allItemsCollection = newCollection
		}
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		if (isSelectAllItemsSelected) {
			return true
		} else {
			if (!Algorithms.isEmpty(selectedItems)) {
				for (folder in selectedItems) {
					trackItem.dataItem?.let { gpxDataItem ->
						if (Algorithms.stringsEqual(GpxDbUtils.getGpxFileDir(app, gpxDataItem.file), folder)) {
							return true
						}
					}
				}
			}
		}
		return false
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is TrackFolderFilter &&
				other.selectedItems.size == selectedItems.size &&
				areAllItemsSelected(other.selectedItems)
	}

	override fun hasSelectAllVariant(): Boolean {
		return true
	}

	override fun getItemText(itemName: String): String {
		return if (Algorithms.isEmpty(itemName)) {
			app.getString(R.string.root_folder)
		} else {
			itemName.replace("/", " / ")
		}
	}

	override fun getItemIcon(itemName: String): Drawable? {
		return app.uiUtilities.getPaintedIcon(
			R.drawable.ic_action_folder,
			app.getColor(R.color.icon_color_default_light))
	}

	override fun getSelectAllItemIcon(isChecked: Boolean, nightMode: Boolean): Drawable? {
		return if (isChecked) {
			app.uiUtilities.getActiveIcon(
				R.drawable.ic_action_group_select_all,
				nightMode)
		} else {
			app.uiUtilities.getPaintedIcon(
				R.drawable.ic_action_group_select_all,
				app.getColor(R.color.icon_color_default_light))
		}
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is TrackFolderFilter) {
			if (!Algorithms.isEmpty(value.selectedFolders) || Algorithms.isEmpty(value.selectedItems)) {
				value.selectedItems = ArrayList(value.selectedFolders)
			}
		}
		super.initWithValue(value)
	}

	override fun updateOnOtherFiltersChangeNeeded(): Boolean {
		return true
	}
}