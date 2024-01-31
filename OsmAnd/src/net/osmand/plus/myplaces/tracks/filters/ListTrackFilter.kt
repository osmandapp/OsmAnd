package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import android.util.Pair
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.osmand.gpx.GpxParameter
import net.osmand.plus.OsmandApplication
import net.osmand.plus.configmap.tracks.TrackItem
import net.osmand.util.Algorithms

open class ListTrackFilter(
	val app: OsmandApplication,
	trackFilterType: TrackFilterType,
	filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(trackFilterType, filterChangedListener) {

	var collectionFilterParams: SingleFieldTrackFilterParams

	init {
		val additionalData = trackFilterType.additionalData
		if (additionalData == null || additionalData !is SingleFieldTrackFilterParams) {
			throw IllegalArgumentException("additionalData in $trackFilterType filter should be valid instance of CollectionTrackFilterParams")
		}
		collectionFilterParams = additionalData
	}

	var firstItem: String? = null
		set(value) {
			field = value
			value?.let {
				setSelectedItems(arrayListOf(it))
			}
		}

	fun updateFullCollection(items: List<TrackItem>?) {
		if (Algorithms.isEmpty(items)) {
			allItemsCollection = HashMap()
		} else {
			val newCollection = HashMap<String, Int>()
			for (item in items!!) {
				val folderName = item.dataItem?.getParameter(GpxParameter.FILE_DIR) ?: ""
				val count = newCollection[folderName] ?: 0
				newCollection[folderName] = count + 1
			}
			allItemsCollection = newCollection
		}
	}

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedItems)
	}

	@Expose
	@SerializedName("selectedItems", alternate=["selectedFolders", "selectedCities", "electedColors", "selectedWidths"])
	var selectedItems = ArrayList<String>()
		protected set
	var allItems: MutableList<String> = arrayListOf()
		private set
	var allItemsCollection: HashMap<String, Int> = hashMapOf()

	fun setFullItemsCollection(collection: HashMap<String, Int>) {
		allItems = ArrayList(collection.keys)
		allItemsCollection = collection
	}

	@Expose
	var isSelectAllItemsSelected = false
		set(value) {
			field = value
			filterChangedListener?.onFilterChanged()
		}

	fun setFullItemsCollection(collection: List<Pair<String, Int>>) {
		val tmpAllItems = ArrayList<String>()
		val tmpAllItemsCollection = HashMap<String, Int>()
		for (pair in collection) {
			tmpAllItems.add(pair.first)
			tmpAllItemsCollection[pair.first] = pair.second
		}
		allItems = tmpAllItems
		allItemsCollection = tmpAllItemsCollection
	}

	fun setSelectedItems(selectedItems: List<String>) {
		this.selectedItems = ArrayList(selectedItems)
		filterChangedListener?.onFilterChanged()
	}

	fun setItemSelected(item: String, selected: Boolean) {
		if (selected) {
			selectedItems.add(item)
		} else {
			selectedItems.remove(item)
		}
		filterChangedListener?.onFilterChanged()
	}

	fun isItemSelected(item: String): Boolean {
		return selectedItems.contains(item)
	}

	override fun initWithValue(value: BaseTrackFilter) {
		if (value is ListTrackFilter) {
			setSelectedItems(
				if (value.selectedItems == null) {
					ArrayList()
				} else {
					ArrayList(value.selectedItems)
				})
			for (item in selectedItems) {
				if (!allItems.contains(item)) {
					allItems.add(item)
					allItemsCollection[item] = 0
				}
			}
			filterChangedListener?.onFilterChanged()
		}
	}

	fun areAllItemsSelected(items: List<String>): Boolean {
		for (item in items) {
			if (!isItemSelected(item)) {
				return false
			}
		}
		return true
	}

	open fun getItemText(itemName: String): String {
		return itemName
	}

	open fun getItemIcon(itemName: String): Drawable? {
		return null
	}

	open fun getSelectAllItemIcon(isChecked: Boolean, nightMode: Boolean): Drawable? {
		return null
	}

	fun getTracksCountForItem(itemName: String): Int {
		return allItemsCollection[itemName] ?: 0
	}

	open fun hasSelectAllVariant(): Boolean {
		return false
	}

	fun addSelectedItems(selectedItems: List<String>) {
		this.selectedItems.addAll(selectedItems)
	}

	fun clearSelectedItems() {
		selectedItems = ArrayList()
	}

	override fun isTrackAccepted(trackItem: TrackItem): Boolean {
		val trackItemPropertyValue = getTrackPropertyValue(trackItem)
		for (item in selectedItems) {//***
			if (Algorithms.stringsEqual(trackItemPropertyValue, item)) {
				return true
			}
		}
		return false
	}

	private fun getTrackPropertyValue(trackItem: TrackItem): String {
		var value = trackItem.dataItem?.getParameter<Any>(trackFilterType.property!!)
		value?.let {
			value = collectionFilterParams.trackParamToString(it)
		}
		return value?.toString() ?: ""
	}

	override fun equals(other: Any?): Boolean {
		return super.equals(other) &&
				other is ListTrackFilter &&
				other.selectedItems.size == selectedItems.size &&
				areAllItemsSelected(other.selectedItems)
	}

	override fun hashCode(): Int {
		var result = selectedItems.hashCode()
		result = 31 * result + allItems.hashCode()
		result = 31 * result + isSelectAllItemsSelected.hashCode()
		return result
	}

}