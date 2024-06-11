package net.osmand.shared.gpx

import net.osmand.shared.util.Localization

enum class GpxSplitType(val typeName: String, val type: Int, val resId: String) {
	NO_SPLIT("no_split", -1, "shared_string_none"),
	DISTANCE("distance", 1, "distance"),
	TIME("time", 2, "shared_string_time");

	companion object {
		fun getSplitTypeByName(name: String?): GpxSplitType {
			return entries.find { it.name.equals(name, ignoreCase = true) } ?: NO_SPLIT
		}

		fun getSplitTypeByTypeId(typeId: Int): GpxSplitType {
			return entries.find { it.type == typeId } ?: NO_SPLIT
		}
	}

	fun getHumanString(): String {
		return Localization.getString(resId)
	}
}

