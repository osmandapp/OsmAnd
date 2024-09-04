package net.osmand.shared.filters

import net.osmand.shared.data.KWidthMode
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.PlatformUtil

class WidthSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {
	override fun getItemText(itemName: String): String {
		return if (KAlgorithms.isEmpty(itemName)) {
			PlatformUtil.getStringResource("not_specified")
		} else {
			when (itemName) {
				KWidthMode.THIN.key -> PlatformUtil.getStringResource("rendering_value_thin_name")
				KWidthMode.MEDIUM.key -> PlatformUtil.getStringResource("rendering_value_medium_name")
				KWidthMode.BOLD.key -> PlatformUtil.getStringResource("rendering_value_bold_name")
				else -> {
					"${PlatformUtil.getStringResource("shared_string_custom")}: $itemName"
				}

			}
		}
	}

	override fun includeEmptyValues(): Boolean {
		return true
	}
}