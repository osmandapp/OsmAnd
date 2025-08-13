package net.osmand.shared.gpx.filters

import kotlinx.serialization.Serializable


@Serializable
open class SingleFieldTrackFilterParams {
	open fun hasSelectAllVariant(): Boolean {
		return false
	}

	open fun getItemText(itemName: String): String {
		return itemName
	}

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