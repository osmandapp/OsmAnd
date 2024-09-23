package net.osmand.shared.gpx.enums

import net.osmand.shared.util.Localization

enum class Gpx3DVisualizationType(val typeName: String, val displayNameResId: String) {
	NONE("none", "shared_string_none"),
	ALTITUDE("altitude", "altitude"),
	SPEED("shared_string_speed", "shared_string_speed"),
	HEART_RATE("map_widget_ant_heart_rate", "map_widget_ant_heart_rate"),
	BICYCLE_CADENCE("map_widget_ant_bicycle_cadence", "map_widget_ant_bicycle_cadence"),
	BICYCLE_POWER("map_widget_ant_bicycle_power", "map_widget_ant_bicycle_power"),
	TEMPERATURE("shared_string_temperature", "shared_string_temperature"),
	SPEED_SENSOR("shared_string_speed", "map_widget_ant_bicycle_speed"),
	FIXED_HEIGHT("fixed_height", "fixed_height");

	fun getDisplayName(): String {
		return Localization.getString(displayNameResId)
	}

	fun is3dType(): Boolean = this != NONE

	companion object {
		fun get3DVisualizationType(typeName: String?): Gpx3DVisualizationType {
			return entries.find { it.typeName.equals(typeName, ignoreCase = true) } ?: NONE
		}
	}
}