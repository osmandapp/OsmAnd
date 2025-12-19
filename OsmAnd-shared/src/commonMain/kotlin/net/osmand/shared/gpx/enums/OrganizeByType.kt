package net.osmand.shared.gpx.enums

import net.osmand.shared.gpx.filters.TrackFilterType
import net.osmand.shared.util.Localization

enum class OrganizeByType(
	val iconResId: String,
	val nameResId: String,
	val filterType: TrackFilterType,
	val group: OrganizeByGroupType
) {
	// General
	ACTIVITY(
		"ic_action_activity",
		"shared_string_activity",
		TrackFilterType.ACTIVITY,
		OrganizeByGroupType.GENERAL),
	DURATION(
		"ic_action_time_span_25",
		"duration",
		TrackFilterType.DURATION,
		OrganizeByGroupType.GENERAL),
	TIME_IN_MOTION(
		"ic_action_time_in_motion",
		"moving_time",
		TrackFilterType.TIME_IN_MOTION,
		OrganizeByGroupType.GENERAL),
	LENGTH(
		"ic_action_length",
		"shared_string_length",
		TrackFilterType.LENGTH,
		OrganizeByGroupType.GENERAL),

	// Date & Time
	YEAR_OF_CREATION(
		"ic_action_calendar_month",
		"year_of_creation",
		TrackFilterType.DATE_CREATION,
		OrganizeByGroupType.DATE_TIME),
	MONTH_AND_YEAR(
		"ic_action_calendar_month",
		"month_year_creation",
		TrackFilterType.DATE_CREATION,
		OrganizeByGroupType.DATE_TIME),

	// Location
	NEAREST_CITY(
		"ic_action_street_name",
		"nearest_city",
		TrackFilterType.CITY,
		OrganizeByGroupType.LOCATION),
	COUNTRY("ic_world_globe_dark", "country", TrackFilterType.CITY, OrganizeByGroupType.LOCATION),

	// Speed
	MAX_SPEED("ic_action_speed", "max_speed", TrackFilterType.MAX_SPEED, OrganizeByGroupType.SPEED),
	AVG_SPEED(
		"ic_action_speed",
		"avg_speed",
		TrackFilterType.AVERAGE_SPEED,
		OrganizeByGroupType.SPEED),

	// Altitude and elevation
	MAX_ALTITUDE(
		"ic_action_altitude_max",
		"ic_action_altitude_average",
		TrackFilterType.MAX_ALTITUDE,
		OrganizeByGroupType.ALTITUDE_ELEVATION),
	AVG_ALTITUDE(
		"ic_action_altitude_average",
		"shared_string_avg_altitude",
		TrackFilterType.AVERAGE_ALTITUDE,
		OrganizeByGroupType.ALTITUDE_ELEVATION),
	UPHILL(
		"ic_action_altitude_ascent",
		"shared_string_uphill",
		TrackFilterType.UPHILL,
		OrganizeByGroupType.ALTITUDE_ELEVATION),
	DOWNHILL(
		"ic_action_altitude_descent",
		"shared_string_downhill",
		TrackFilterType.DOWNHILL,
		OrganizeByGroupType.ALTITUDE_ELEVATION),

	// Sensors
	SENSOR_SPEED_MAX(
		"ic_action_sensor_speed_outlined",
		"max_sensor_speed",
		TrackFilterType.MAX_SENSOR_SPEED,
		OrganizeByGroupType.SENSORS),
	SENSOR_SPEED_AVG(
		"ic_action_sensor_speed_outlined",
		"avg_sensor_speed",
		TrackFilterType.AVERAGE_SENSOR_SPEED,
		OrganizeByGroupType.SENSORS),
	HEART_RATE_MAX(
		"ic_action_sensor_heart_rate_outlined",
		"max_sensor_heartrate",
		TrackFilterType.MAX_SENSOR_HEART_RATE,
		OrganizeByGroupType.SENSORS),
	HEART_RATE_AVG(
		"ic_action_sensor_heart_rate_outlined",
		"avg_sensor_heartrate",
		TrackFilterType.AVERAGE_SENSOR_HEART_RATE,
		OrganizeByGroupType.SENSORS),
	CADENCE_MAX(
		"ic_action_sensor_cadence_outlined",
		"max_sensor_cadence",
		TrackFilterType.MAX_SENSOR_CADENCE,
		OrganizeByGroupType.SENSORS),
	CADENCE_AVG(
		"ic_action_sensor_cadence_outlined",
		"avg_sensor_cadence",
		TrackFilterType.AVERAGE_SENSOR_CADENCE,
		OrganizeByGroupType.SENSORS),
	POWER_MAX(
		"ic_action_sensor_bicycle_power_outlined",
		"max_sensor_bycicle_power",
		TrackFilterType.MAX_SENSOR_BICYCLE_POWER,
		OrganizeByGroupType.SENSORS),
	POWER_AVG(
		"ic_action_sensor_bicycle_power_outlined",
		"avg_sensor_bycicle_power",
		TrackFilterType.AVERAGE_SENSOR_BICYCLE_POWER,
		OrganizeByGroupType.SENSORS),
	TEMP_MAX(
		"ic_action_sensor_temperature_outlined",
		"max_sensor_temperature",
		TrackFilterType.MAX_SENSOR_TEMPERATURE,
		OrganizeByGroupType.SENSORS),
	TEMP_AVG(
		"ic_action_sensor_temperature_outlined",
		"avg_sensor_temperature",
		TrackFilterType.AVERAGE_SENSOR_TEMPERATURE,
		OrganizeByGroupType.SENSORS);

	fun getName(): String {
		return Localization.getString(nameResId)
	}

	fun getIconName(): String {
		return iconResId
	}
}