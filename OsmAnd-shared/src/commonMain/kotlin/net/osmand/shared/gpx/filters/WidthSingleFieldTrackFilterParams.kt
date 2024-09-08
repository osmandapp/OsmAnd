package net.osmand.shared.gpx.filters

import net.osmand.shared.gpx.data.KWidthMode
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.Localization

class WidthSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {
	override fun getItemText(itemName: String): String {
		return if (KAlgorithms.isEmpty(itemName)) {
			Localization.getString("not_specified")
		} else {
			when (itemName) {
				KWidthMode.THIN.key -> Localization.getString("rendering_value_thin_name")
				KWidthMode.MEDIUM.key -> Localization.getString("rendering_value_medium_name")
				KWidthMode.BOLD.key -> Localization.getString("rendering_value_bold_name")
				else -> {
					"${Localization.getString("shared_string_custom")}: $itemName"
				}

			}
		}
	}

	override fun includeEmptyValues(): Boolean {
		return true
	}
}