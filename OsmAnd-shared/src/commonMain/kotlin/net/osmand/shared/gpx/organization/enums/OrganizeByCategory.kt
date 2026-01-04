package net.osmand.shared.gpx.organization.enums

import net.osmand.shared.util.Localization

enum class OrganizeByCategory(val nameResId: String) {
	GENERAL(
		nameResId = "group_general"
	),
	DATE_TIME(
		nameResId = "group_date_time"
	),
	LOCATION(
		nameResId = "shared_string_location"
	),
	SPEED(
		nameResId = "shared_string_speed"
	),
	ALTITUDE_ELEVATION(
		nameResId = "group_altitude_elevation"
	),
	SENSORS(
		nameResId = "group_sensors"
	);

	fun getName() = Localization.getString(nameResId)
}