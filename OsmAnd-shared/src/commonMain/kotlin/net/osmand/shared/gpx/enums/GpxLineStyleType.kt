package net.osmand.shared.gpx.enums

import net.osmand.shared.util.Localization

enum class GpxLineStyleType(val typeName: String, val displayNameResId: String) {
	SOLID("solid", "gpx_line_style_solid"),
	DASHED("dashed", "gpx_line_style_dashed"),
	DOTTED("dotted", "gpx_line_style_dotted");

	fun getDisplayName(): String {
		return Localization.getString(displayNameResId)
	}

	companion object {
		fun getLineStyleType(typeName: String?): GpxLineStyleType {
			return entries.find { it.typeName.equals(typeName, ignoreCase = true) } ?: SOLID
		}
	}
}
