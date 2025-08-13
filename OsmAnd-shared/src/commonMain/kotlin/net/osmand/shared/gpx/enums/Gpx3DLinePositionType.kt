package net.osmand.shared.gpx.enums

import net.osmand.shared.util.Localization

enum class Gpx3DLinePositionType(val typeName: String, val displayNameResId: String) {
	TOP("top", "top"),
	BOTTOM("bottom", "bottom"),
	TOP_BOTTOM("top_bottom", "top_and_bottom");

	fun getDisplayName(): String {
		return Localization.getString(displayNameResId)
	}

	companion object {
		fun get3DLinePositionType(typeName: String?): Gpx3DLinePositionType {
			return entries.firstOrNull { it.typeName.equals(typeName, ignoreCase = true) } ?: TOP
		}
	}
}