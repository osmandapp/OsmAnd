package net.osmand.plus.myplaces.tracks.filters


open class SingleFieldTrackFilterParams {
	open fun hasSelectAllVariant(): Boolean {
		return false
	}

	open fun getItemText(itemName: String): String {
		return itemName
	}

//	@Nullable
//	fun getItemIcon(
//		app: OsmandApplication?,
//		itemName: String?): android.graphics.drawable.Drawable? {
//		return null
//	}
//
//	@Nullable
//	fun getAllItemsIcon(
//		app: OsmandApplication?,
//		isChecked: Boolean,
//		nightMode: Boolean): android.graphics.drawable.Drawable? {
//		return null
//	}

	open fun trackParamToString(trackParam: Any): String {
		return trackParam.toString()
	}

	open fun includeEmptyValues(): Boolean {
		return false
	}

	open fun sortByName(): Boolean {
		return false
	}

	open fun sortDescending(): Boolean {
		return true
	}
}