package net.osmand.plus.charts;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_BIKE_POWER;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_CADENCE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_HEART_RATE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_SPEED;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_TEMPERATURE;

import android.content.Context;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.shared.gpx.PointAttributes;
import net.osmand.shared.obd.OBDCommand;

public enum GPXDataSetType {

	ALTITUDE(R.string.altitude, R.drawable.ic_action_altitude, PointAttributes.POINT_ELEVATION, R.color.gpx_chart_blue_label, R.color.gpx_chart_blue),
	SPEED(R.string.shared_string_speed, R.drawable.ic_action_speed_outlined, PointAttributes.POINT_SPEED, R.color.gpx_chart_orange_label, R.color.gpx_chart_orange),
	SLOPE(R.string.shared_string_slope, R.drawable.ic_action_slope, PointAttributes.POINT_ELEVATION, R.color.gpx_chart_green_label, R.color.gpx_chart_green),
	ALTITUDE_EXTRM(R.string.altitude, R.drawable.ic_action_altitude_average, PointAttributes.POINT_ELEVATION, R.color.gpx_chart_blue_label, R.color.gpx_chart_blue),

	SENSOR_SPEED(R.string.shared_string_speed, R.drawable.ic_action_sensor_speed_outlined, SENSOR_TAG_SPEED, R.color.gpx_chart_yellow_label, R.color.gpx_chart_yellow),
	SENSOR_HEART_RATE(R.string.map_widget_ant_heart_rate, R.drawable.ic_action_sensor_heart_rate_outlined, SENSOR_TAG_HEART_RATE, R.color.gpx_chart_pink_label, R.color.gpx_chart_pink),
	SENSOR_BIKE_POWER(R.string.map_widget_ant_bicycle_power, R.drawable.ic_action_sensor_bicycle_power_outlined, SENSOR_TAG_BIKE_POWER, R.color.gpx_chart_teal_label, R.color.gpx_chart_teal),
	SENSOR_BIKE_CADENCE(R.string.map_widget_ant_bicycle_cadence, R.drawable.ic_action_sensor_cadence_outlined, SENSOR_TAG_CADENCE, R.color.gpx_chart_indigo_label, R.color.gpx_chart_indigo),
	SENSOR_TEMPERATURE(R.string.map_settings_weather_temp, R.drawable.ic_action_thermometer, SENSOR_TAG_TEMPERATURE, R.color.gpx_chart_green_label, R.color.gpx_chart_green),

	INTAKE_TEMPERATURE(R.string.obd_air_intake_temp, R.drawable.ic_action_obd_temperature_intake, OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND.getGpxTag(), R.color.gpx_chart_intake_temperature, R.color.gpx_chart_intake_temperature),
	AMBIENT_TEMPERATURE(R.string.obd_ambient_air_temp, R.drawable.ic_action_obd_temperature_outside, OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND.getGpxTag(), R.color.gpx_chart_ambient_temperature, R.color.gpx_chart_ambient_temperature),
	COOLANT_TEMPERATURE(R.string.obd_engine_coolant_temp, R.drawable.ic_action_obd_temperature_coolant, OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND.getGpxTag(), R.color.gpx_chart_coolant_temperature, R.color.gpx_chart_coolant_temperature),
	ENGINE_OIL_TEMPERATURE(R.string.obd_engine_oil_temperature, R.drawable.ic_action_obd_temperature_engine_oil, OBDCommand.OBD_ENGINE_OIL_TEMPERATURE_COMMAND.getGpxTag(), R.color.gpx_chart_engine_oil_temperature, R.color.gpx_chart_engine_oil_temperature),
	ENGINE_SPEED(R.string.obd_widget_engine_speed, R.drawable.ic_action_obd_engine_speed, OBDCommand.OBD_RPM_COMMAND.getGpxTag(), R.color.gpx_chart_engine_speed, R.color.gpx_chart_engine_speed),
	ENGINE_RUNTIME(R.string.obd_engine_runtime, R.drawable.ic_action_car_running_time, OBDCommand.OBD_ENGINE_RUNTIME_COMMAND.getGpxTag(), R.color.gpx_chart_engine_runtime, R.color.gpx_chart_engine_runtime),
	ENGINE_LOAD(R.string.obd_calculated_engine_load, R.drawable.ic_action_car_info, OBDCommand.OBD_CALCULATED_ENGINE_LOAD_COMMAND.getGpxTag(), R.color.gpx_chart_engine_load, R.color.gpx_chart_engine_load),
	FUEL_PRESSURE(R.string.obd_fuel_pressure, R.drawable.ic_action_obd_fuel_pressure, OBDCommand.OBD_FUEL_PRESSURE_COMMAND.getGpxTag(), R.color.gpx_chart_fuel_pressure, R.color.gpx_chart_fuel_pressure),
	FUEL_CONSUMPTION(R.string.obd_fuel_consumption, R.drawable.ic_action_obd_fuel_consumption, OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND.getGpxTag(), R.color.gpx_chart_fuel_consumption, R.color.gpx_chart_fuel_consumption),
	REMAINING_FUEL(R.string.remaining_fuel, R.drawable.ic_action_obd_fuel_remaining, OBDCommand.OBD_FUEL_LEVEL_COMMAND.getGpxTag(), R.color.gpx_chart_remaining_fuel, R.color.gpx_chart_remaining_fuel),
	BATTERY_LEVEL(R.string.obd_battery_voltage, R.drawable.ic_action_obd_battery_voltage, OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND.getGpxTag(), R.color.gpx_chart_battery_level, R.color.gpx_chart_battery_level),
	VEHICLE_SPEED(R.string.obd_widget_vehicle_speed, R.drawable.ic_action_obd_speed, OBDCommand.OBD_SPEED_COMMAND.getGpxTag(), R.color.gpx_chart_vehicle_speed, R.color.gpx_chart_vehicle_speed),
	THROTTLE_POSITION(R.string.obd_throttle_position, R.drawable.ic_action_obd_throttle_position, OBDCommand.OBD_THROTTLE_POSITION_COMMAND.getGpxTag(), R.color.gpx_chart_throttle_position, R.color.gpx_chart_throttle_position),

	ZOOM_ANIMATED(R.string.zoom_animated, R.drawable.ic_action_map_zoom, PointAttributes.DEV_ANIMATED_ZOOM, R.color.gpx_chart_teal_label, R.color.gpx_chart_teal),
	ZOOM_NON_ANIMATED(R.string.zoom_non_animated, R.drawable.ic_action_map_zoom, PointAttributes.DEV_RAW_ZOOM, R.color.gpx_chart_indigo_label, R.color.gpx_chart_indigo);

	@StringRes
	private final int titleId;
	@DrawableRes
	private final int iconId;

	private final String dataKey;
	@ColorRes
	private final int textColorId;
	@ColorRes
	private final int fillColorId;

	GPXDataSetType(@StringRes int titleId, @DrawableRes int iconId, @NonNull String dataKey, @ColorRes int textColorId, @ColorRes int fillColorId) {
		this.titleId = titleId;
		this.iconId = iconId;
		this.dataKey = dataKey;
		this.textColorId = textColorId;
		this.fillColorId = fillColorId;
	}

	public String getName(@NonNull Context ctx) {
		return ctx.getString(titleId);
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}

	@NonNull
	public String getDataKey() {
		return dataKey;
	}

	@ColorRes
	public int getTextColorId(boolean additional) {
		if (this == SPEED) {
			return additional ? R.color.gpx_chart_red_label : textColorId;
		}
		return textColorId;
	}

	@ColorRes
	public int getFillColorId(boolean additional) {
		if (this == SPEED) {
			return additional ? R.color.gpx_chart_red : fillColorId;
		}
		return fillColorId;
	}

	@NonNull
	public String getMainUnitY(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		return switch (this) {
			case ALTITUDE -> {
				boolean shouldUseFeet = settings.METRIC_SYSTEM.get().shouldUseFeet();
				yield app.getString(shouldUseFeet ? R.string.foot : R.string.m);
			}
			case SLOPE -> "%";
			case SPEED, SENSOR_SPEED -> settings.SPEED_SYSTEM.get().toShortString();
			case SENSOR_HEART_RATE -> app.getString(R.string.beats_per_minute_short);
			case SENSOR_BIKE_POWER -> app.getString(R.string.power_watts_unit);
			case SENSOR_BIKE_CADENCE -> app.getString(R.string.revolutions_per_minute_unit);
			case SENSOR_TEMPERATURE -> app.getString(R.string.degree_celsius);
			default -> "";
		};
	}
}
