package net.osmand.shared.gpx.enums

import net.osmand.shared.util.Localization

enum class OrganizeByGroupType(val nameResId: String) {
	GENERAL("group_general"),
	DATE_TIME("group_date_time"),
	LOCATION("shared_string_location"),
	SPEED("shared_string_speed"),
	ALTITUDE_ELEVATION("group_altitude_elevation"),
	SENSORS("group_sensors");

	fun getName(): String {
		return Localization.getString(nameResId)
	}
}