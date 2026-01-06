package net.osmand.shared.gpx.organization.enums

import net.osmand.shared.data.Limits
import net.osmand.shared.gpx.filters.TrackFilterType
import net.osmand.shared.gpx.organization.strategy.NoImplementedStrategy
import net.osmand.shared.gpx.organization.strategy.OrganizeByActivityStrategy
import net.osmand.shared.gpx.organization.strategy.OrganizeByDateStrategy
import net.osmand.shared.gpx.organization.strategy.OrganizeByRangeStrategy
import net.osmand.shared.gpx.organization.strategy.OrganizeByStrategy
import net.osmand.shared.util.Localization

enum class OrganizeByType(
	val iconResId: String,
	val nameResId: String,
	val filterType: TrackFilterType,
	val category: OrganizeByCategory,
	val stepRange: Limits? = null,
	val strategy: OrganizeByStrategy
) {
	// General
	ACTIVITY(
		iconResId = "ic_action_activity",
		nameResId = "shared_string_activity",
		filterType = TrackFilterType.ACTIVITY,
		category = OrganizeByCategory.GENERAL,
		strategy = OrganizeByActivityStrategy
	),
	DURATION(
		iconResId = "ic_action_time_span_75",
		nameResId = "duration",
		filterType = TrackFilterType.DURATION,
		category = OrganizeByCategory.GENERAL,
		stepRange = Limits(1, 180),
		strategy = OrganizeByRangeStrategy
	),
	TIME_IN_MOTION(
		iconResId = "ic_action_time_in_motion",
		nameResId = "moving_time",
		filterType = TrackFilterType.TIME_IN_MOTION,
		category = OrganizeByCategory.GENERAL,
		stepRange = Limits(1, 180),
		strategy = OrganizeByRangeStrategy
	),
	LENGTH(
		iconResId = "ic_action_length",
		nameResId = "shared_string_length",
		filterType = TrackFilterType.LENGTH,
		category = OrganizeByCategory.GENERAL,
		stepRange = Limits(1, 180),
		strategy = OrganizeByRangeStrategy
	),

	// Date & Time
	YEAR_OF_CREATION(
		iconResId = "ic_action_calendar_month",
		nameResId = "year_of_creation",
		filterType = TrackFilterType.DATE_CREATION,
		category = OrganizeByCategory.DATE_TIME,
		strategy = OrganizeByDateStrategy
	),
	MONTH_AND_YEAR(
		iconResId = "ic_action_calendar_month",
		nameResId = "month_year_creation",
		filterType = TrackFilterType.DATE_CREATION,
		category = OrganizeByCategory.DATE_TIME,
		strategy = OrganizeByDateStrategy
	),

	// Location
	NEAREST_CITY(
		iconResId = "ic_action_street_name",
		nameResId = "nearest_city",
		filterType = TrackFilterType.CITY,
		category = OrganizeByCategory.LOCATION,
		strategy = NoImplementedStrategy
	),

	// Speed
	MAX_SPEED(
		iconResId = "ic_action_speed_max",
		nameResId = "organize_by_max_speed",
		filterType = TrackFilterType.MAX_SPEED,
		category = OrganizeByCategory.SPEED,
		stepRange = Limits(1, 180),
		strategy = OrganizeByRangeStrategy
	),
	AVG_SPEED(
		iconResId = "ic_action_speed_average",
		nameResId = "avg_speed",
		filterType = TrackFilterType.AVERAGE_SPEED,
		category = OrganizeByCategory.SPEED,
		stepRange = Limits(1, 180),
		strategy = OrganizeByRangeStrategy
	),

	// Altitude and elevation
	MAX_ALTITUDE(
		iconResId = "ic_action_altitude_max",
		nameResId = "organize_by_max_altitude",
		filterType = TrackFilterType.MAX_ALTITUDE,
		category = OrganizeByCategory.ALTITUDE_ELEVATION,
		stepRange = Limits(1, 1000),
		strategy = OrganizeByRangeStrategy
	),
	AVG_ALTITUDE(
		iconResId = "ic_action_altitude_average",
		nameResId = "shared_string_avg_altitude",
		filterType = TrackFilterType.AVERAGE_ALTITUDE,
		category = OrganizeByCategory.ALTITUDE_ELEVATION,
		stepRange = Limits(1, 1000),
		strategy = OrganizeByRangeStrategy
	),
	UPHILL(
		iconResId = "ic_action_altitude_ascent",
		nameResId = "shared_string_uphill",
		filterType = TrackFilterType.UPHILL,
		category = OrganizeByCategory.ALTITUDE_ELEVATION,
		stepRange = Limits(1, 1000),
		strategy = OrganizeByRangeStrategy
	),
	DOWNHILL(
		iconResId = "ic_action_altitude_descent",
		nameResId = "shared_string_downhill",
		filterType = TrackFilterType.DOWNHILL,
		category = OrganizeByCategory.ALTITUDE_ELEVATION,
		stepRange = Limits(1, 1000),
		strategy = OrganizeByRangeStrategy
	),

	// Sensors
	SENSOR_SPEED_MAX(
		iconResId = "ic_action_sensor_speed_outlined",
		nameResId = "max_sensor_speed",
		filterType = TrackFilterType.MAX_SENSOR_SPEED,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(1, 180),
		strategy = OrganizeByRangeStrategy
	),
	SENSOR_SPEED_AVG(
		iconResId = "ic_action_sensor_speed_outlined",
		nameResId = "avg_sensor_speed",
		filterType = TrackFilterType.AVERAGE_SENSOR_SPEED,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(1, 180),
		strategy = OrganizeByRangeStrategy
	),
	HEART_RATE_MAX(
		iconResId = "ic_action_sensor_heart_rate_outlined",
		nameResId = "max_sensor_heartrate",
		filterType = TrackFilterType.MAX_SENSOR_HEART_RATE,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(1, 20),
		strategy = OrganizeByRangeStrategy
	),
	HEART_RATE_AVG(
		iconResId = "ic_action_sensor_heart_rate_outlined",
		nameResId = "avg_sensor_heartrate",
		filterType = TrackFilterType.AVERAGE_SENSOR_HEART_RATE,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(1, 20),
		strategy = OrganizeByRangeStrategy
	),
	CADENCE_MAX(
		iconResId = "ic_action_sensor_cadence_outlined",
		nameResId = "max_sensor_cadence",
		filterType = TrackFilterType.MAX_SENSOR_CADENCE,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(5, 25),
		strategy = OrganizeByRangeStrategy
	),
	CADENCE_AVG(
		iconResId = "ic_action_sensor_cadence_outlined",
		nameResId = "avg_sensor_cadence",
		filterType = TrackFilterType.AVERAGE_SENSOR_CADENCE,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(5, 25),
		strategy = OrganizeByRangeStrategy
	),
	POWER_MAX(
		iconResId = "ic_action_sensor_bicycle_power_outlined",
		nameResId = "max_sensor_bycicle_power",
		filterType = TrackFilterType.MAX_SENSOR_BICYCLE_POWER,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(10, 100),
		strategy = OrganizeByRangeStrategy
	),
	POWER_AVG(
		iconResId = "ic_action_sensor_bicycle_power_outlined",
		nameResId = "avg_sensor_bycicle_power",
		filterType = TrackFilterType.AVERAGE_SENSOR_BICYCLE_POWER,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(10, 100),
		strategy = OrganizeByRangeStrategy
	),
	TEMP_MAX(
		iconResId = "ic_action_sensor_temperature_outlined",
		nameResId = "max_sensor_temperature",
		filterType = TrackFilterType.MAX_SENSOR_TEMPERATURE,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(1, 10),
		strategy = OrganizeByRangeStrategy
	),
	TEMP_AVG(
		iconResId = "ic_action_sensor_temperature_outlined",
		nameResId = "avg_sensor_temperature",
		filterType = TrackFilterType.AVERAGE_SENSOR_TEMPERATURE,
		category = OrganizeByCategory.SENSORS,
		stepRange = Limits(1, 10),
		strategy = OrganizeByRangeStrategy
	);

	fun getName() = Localization.getString(nameResId)

	fun getIconName() = iconResId

	fun getDisplayUnits() = filterType.measureUnitType.getUnit()
}