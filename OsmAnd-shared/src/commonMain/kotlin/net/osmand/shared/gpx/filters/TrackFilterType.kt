package net.osmand.shared.gpx.filters

import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.filters.OtherTrackParam.VISIBLE_ON_MAP
import net.osmand.shared.gpx.filters.OtherTrackParam.WITH_WAYPOINTS
import net.osmand.shared.util.Localization
import net.osmand.shared.util.PlatformUtil

@Serializable
enum class TrackFilterType(
	private val nameResId: String,
	val filterType: FilterType,
	val property: GpxParameter?,
	val measureUnitType: MeasureUnitType,
	val defaultParams: List<Any>?,
	val updateOnOtherFiltersChangeNeeded: Boolean,
	val additionalData: Any? = null) {

	NAME(
		"shared_string_name",
		FilterType.TEXT,
		GpxParameter.FILE_NAME,
		MeasureUnitType.NONE,
		null,
		false),
	DURATION(
		"duration",
		FilterType.RANGE,
		GpxParameter.TIME_SPAN,
		MeasureUnitType.TIME_DURATION,
		listOf(0L, TrackFiltersConstants.DURATION_MAX_VALUE.toLong()),
		false),
	TIME_IN_MOTION(
		"moving_time",
		FilterType.RANGE,
		GpxParameter.TIME_MOVING,
		MeasureUnitType.TIME_DURATION,
		listOf(0L, TrackFiltersConstants.DURATION_MAX_VALUE.toLong()),
		false),
	LENGTH(
		"routing_attr_length_name",
		FilterType.RANGE,
		GpxParameter.TOTAL_DISTANCE,
		MeasureUnitType.DISTANCE,
		listOf(0.0, TrackFiltersConstants.LENGTH_MAX_VALUE.toDouble()),
		false),
	AVERAGE_SPEED(
		"average_speed",
		FilterType.RANGE,
		GpxParameter.AVG_SPEED,
		MeasureUnitType.SPEED,
		listOf(0.0, TrackFiltersConstants.SPEED_MAX_VALUE.toDouble()),
		false),
	AVERAGE_SENSOR_SPEED(
		"avg_sensor_speed",
		FilterType.RANGE,
		GpxParameter.AVG_SENSOR_SPEED,
		MeasureUnitType.SPEED,
		listOf(0.0, TrackFiltersConstants.SPEED_MAX_VALUE.toDouble()),
		false),
	MAX_SENSOR_SPEED(
		"max_sensor_speed",
		FilterType.RANGE,
		GpxParameter.MAX_SENSOR_SPEED,
		MeasureUnitType.SPEED,
		listOf(0.0, TrackFiltersConstants.SPEED_MAX_VALUE.toDouble()),
		false),
	AVERAGE_SENSOR_HEART_RATE(
		"avg_sensor_heartrate",
		FilterType.RANGE,
		GpxParameter.AVG_SENSOR_HEART_RATE,
		MeasureUnitType.BPM,
		listOf(0.0, TrackFiltersConstants.HEART_RATE_MAX_VALUE.toDouble()),
		false),
	MAX_SENSOR_HEART_RATE(
		"max_sensor_heartrate",
		FilterType.RANGE,
		GpxParameter.MAX_SENSOR_HEART_RATE,
		MeasureUnitType.BPM,
		listOf(0, TrackFiltersConstants.HEART_RATE_MAX_VALUE),
		false),
	AVERAGE_SENSOR_CADENCE(
		"avg_sensor_cadence",
		FilterType.RANGE,
		GpxParameter.AVG_SENSOR_CADENCE,
		MeasureUnitType.ROTATIONS,
		listOf(0.0, TrackFiltersConstants.DEFAULT_MAX_VALUE.toDouble()),
		false),
	MAX_SENSOR_CADENCE(
		"max_sensor_cadence",
		FilterType.RANGE,
		GpxParameter.MAX_SENSOR_CADENCE,
		MeasureUnitType.ROTATIONS,
		listOf(0.0, TrackFiltersConstants.DEFAULT_MAX_VALUE.toDouble()),
		false),
	AVERAGE_SENSOR_BICYCLE_POWER(
		"avg_sensor_bycicle_power",
		FilterType.RANGE,
		GpxParameter.AVG_SENSOR_POWER,
		MeasureUnitType.POWER,
		listOf(0.0, TrackFiltersConstants.DEFAULT_MAX_VALUE.toDouble()),
		false),
	MAX_SENSOR_BICYCLE_POWER(
		"max_sensor_bycicle_power",
		FilterType.RANGE,
		GpxParameter.MAX_SENSOR_POWER,
		MeasureUnitType.POWER,
		listOf(0, TrackFiltersConstants.DEFAULT_MAX_VALUE.toInt()),
		false),
	AVERAGE_SENSOR_TEMPERATURE(
		"avg_sensor_temperature",
		FilterType.RANGE,
		GpxParameter.AVG_SENSOR_TEMPERATURE,
		MeasureUnitType.TEMPERATURE,
		listOf(0.0, TrackFiltersConstants.TEMPERATURE_MAX_VALUE.toDouble()),
		false),
	MAX_SENSOR_TEMPERATURE(
		"max_sensor_temperature",
		FilterType.RANGE,
		GpxParameter.MAX_SENSOR_TEMPERATURE,
		MeasureUnitType.TEMPERATURE,
		listOf(0, TrackFiltersConstants.TEMPERATURE_MAX_VALUE),
		false),
	MAX_SPEED(
		"max_speed",
		FilterType.RANGE,
		GpxParameter.MAX_SPEED,
		MeasureUnitType.SPEED,
		listOf(0.0, TrackFiltersConstants.SPEED_MAX_VALUE.toDouble()),
		false),
	UPHILL(
		"shared_string_uphill",
		FilterType.RANGE,
		GpxParameter.DIFF_ELEVATION_UP,
		MeasureUnitType.ALTITUDE,
		listOf(0.0, TrackFiltersConstants.ALTITUDE_MAX_VALUE.toDouble()),
		false),
	DOWNHILL(
		"shared_string_downhill",
		FilterType.RANGE,
		GpxParameter.DIFF_ELEVATION_DOWN,
		MeasureUnitType.ALTITUDE,
		listOf(0.0, TrackFiltersConstants.ALTITUDE_MAX_VALUE.toDouble()),
		false),
	AVERAGE_ALTITUDE(
		"average_altitude",
		FilterType.RANGE,
		GpxParameter.AVG_ELEVATION,
		MeasureUnitType.ALTITUDE,
		listOf(0.0, TrackFiltersConstants.ALTITUDE_MAX_VALUE.toDouble()),
		false),
	MAX_ALTITUDE(
		"max_altitude",
		FilterType.RANGE,
		GpxParameter.MAX_ELEVATION,
		MeasureUnitType.ALTITUDE,
		listOf(0.0, TrackFiltersConstants.ALTITUDE_MAX_VALUE.toDouble()),
		false),
	DATE_CREATION(
		"date_of_creation",
		FilterType.DATE_RANGE,
		GpxParameter.FILE_CREATION_TIME,
		MeasureUnitType.NONE,
		null,
		false),
	FOLDER(
		"folder",
		FilterType.SINGLE_FIELD_LIST,
		GpxParameter.FILE_DIR,
		MeasureUnitType.NONE,
		null,
		true,
		FolderSingleFieldTrackFilterParams()),
	CITY(
		"nearest_cities",
		FilterType.SINGLE_FIELD_LIST,
		GpxParameter.NEAREST_CITY_NAME,
		MeasureUnitType.NONE,
		null,
		false,
		SingleFieldTrackFilterParams()),
	ACTIVITY(
		"type_of_activity",
		FilterType.SINGLE_FIELD_LIST,
		GpxParameter.ACTIVITY_TYPE,
		MeasureUnitType.NONE,
		null,
		false,
		ActivitySingleFieldTrackFilterParams()),
	COLOR(
		"shared_string_color",
		FilterType.SINGLE_FIELD_LIST,
		GpxParameter.COLOR,
		MeasureUnitType.NONE,
		null,
		false,
		ColorSingleFieldTrackFilterParams()),
	WIDTH(
		"shared_string_width",
		FilterType.SINGLE_FIELD_LIST,
		GpxParameter.WIDTH,
		MeasureUnitType.NONE,
		null,
		false,
		WidthSingleFieldTrackFilterParams()),
	OTHER(
		"shared_string_other",
		FilterType.OTHER,
		null,
		MeasureUnitType.NONE,
		null,
		false,
		listOf(VISIBLE_ON_MAP, WITH_WAYPOINTS));

	fun getName(): String {
		return Localization.getString(nameResId)
	}
}