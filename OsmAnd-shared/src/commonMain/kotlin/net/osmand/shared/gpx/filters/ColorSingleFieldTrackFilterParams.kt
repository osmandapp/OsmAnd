package net.osmand.shared.gpx.filters

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.Localization
import net.osmand.shared.util.PlatformUtil

class ColorSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {
	override fun getItemText(itemName: String): String {
		return if (KAlgorithms.isEmpty(itemName)) {
			Localization.getString("not_specified")
		} else {
			itemName
		}
	}

	override fun trackParamToString(trackParam: Any): String {
		return KAlgorithms.colorToString(trackParam as Int)
	}

	override fun includeEmptyValues(): Boolean {
		return true
	}
}