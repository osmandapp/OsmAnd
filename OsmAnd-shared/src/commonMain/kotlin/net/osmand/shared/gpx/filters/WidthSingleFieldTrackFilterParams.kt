package net.osmand.shared.gpx.filters

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.Localization

class WidthSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {

	override fun includeEmptyValues(): Boolean {
		return true
	}

	override fun getItemText(itemName: String): String {
		return if (KAlgorithms.isEmpty(itemName)) {
			Localization.getString("not_specified")
		} else {
			itemName
		}
	}
}
