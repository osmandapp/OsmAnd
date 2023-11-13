package net.osmand.plus.myplaces.tracks.filters

import android.graphics.drawable.Drawable
import android.util.Pair
import com.google.gson.annotations.Expose
import net.osmand.plus.OsmandApplication
import net.osmand.util.Algorithms

abstract class ListTrackFilter(
	val app: OsmandApplication,
	displayNameId: Int, filterType: FilterType,
	filterChangedListener: FilterChangedListener?) :
	BaseTrackFilter(displayNameId, filterType, filterChangedListener) {

	override fun isEnabled(): Boolean {
		return !Algorithms.isEmpty(selectedItems)
	}

	@Expose
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
			selectedItems = if (value.selectedItems == null) {
				ArrayList()
			} else {
				ArrayList(value.selectedItems)
			}
			for (item in value.selectedItems) {
				if (!allItems.contains(item)) {
					allItems.add(item)
					allItemsCollection[item] = 0
				}
			}
			filterChangedListener?.onFilterChanged()
		}
	}

	fun getSelectedItems(): List<String> {
		return ArrayList(selectedItems)
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
}