package net.osmand.shared.gpx.filters

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.osmand.shared.data.StringIntPair
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.SerialNames

@Serializable
open class ListTrackFilter : BaseTrackFilter {

	constructor(trackFilterType: TrackFilterType, filterChangedListener: FilterChangedListener?) :
			super(trackFilterType, filterChangedListener)

	@Transient
	lateinit var collectionFilterParams: SingleFieldTrackFilterParams

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
		if (KAlgorithms.isEmpty(items)) {
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
		return !KAlgorithms.isEmpty(selectedItems)
	}

	@SerialNames(
		"selectedItems",
		"selectedFolders",
		"selectedCities",
		"selectedColors",
		"selectedWidths")
	var selectedItems = ArrayList<String>()
		protected set
	var allItems: MutableList<String> = arrayListOf()
		private set
	var allItemsCollection: HashMap<String, Int> = hashMapOf()

	fun setFullItemsCollection(collection: HashMap<String, Int>) {
		allItems = ArrayList(collection.keys)
		allItemsCollection = collection
	}

	@Serializable
	var isSelectAllItemsSelected = false
		set(value) {
			field = value
			filterChangedListener?.onFilterChanged()
		}

	fun setFullItemsCollection(collection: List<StringIntPair>) {
		val tmpAllItems = ArrayList<String>()
		val tmpAllItemsCollection = HashMap<String, Int>()
		for (pair in collection) {
			if (pair.string != null && pair.integer != null) {
				tmpAllItems.add(pair.string)
				tmpAllItemsCollection[pair.string] = pair.integer
			}
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
				ArrayList(value.selectedItems)
			)
			for (item in selectedItems) {
				if (!allItems.contains(item)) {
					allItems.add(item)
					allItemsCollection[item] = 0
				}
			}
			super.initWithValue(value)
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
			if (KAlgorithms.stringsEqual(trackItemPropertyValue, item)) {
				return true
			}
		}
		return false
	}

	protected fun getTrackPropertyValue(trackItem: TrackItem): String {
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