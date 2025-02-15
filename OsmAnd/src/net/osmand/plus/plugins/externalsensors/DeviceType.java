package net.osmand.plus.plugins.externalsensors;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

public enum DeviceType {

	ANT_HEART_RATE(R.string.map_widget_ant_heart_rate, R.drawable.ic_action_sensor_heart_rate_outlined, R.drawable.widget_sensor_heart_rate_day, R.drawable.widget_sensor_heart_rate_night),
	ANT_BICYCLE_POWER(R.string.map_widget_ant_bicycle_power, R.drawable.ic_action_sensor_bicycle_power_outlined, R.drawable.widget_sensor_bicycle_power_day, R.drawable.widget_sensor_bicycle_power_night),
	ANT_BICYCLE_SC(R.string.bicycle_scd_device_name, R.drawable.ic_action_sensor_cadence_outlined, R.drawable.widget_sensor_cadence_day, R.drawable.widget_sensor_cadence_night),
	ANT_BICYCLE_SD(R.string.bicycle_scd_device_name, R.drawable.ic_action_sensor_cadence_outlined, R.drawable.widget_sensor_cadence_day, R.drawable.widget_sensor_cadence_night),
	ANT_TEMPERATURE(R.string.map_settings_weather_temp, R.drawable.ic_action_thermometer, R.drawable.widget_weather_temperature_day, R.drawable.widget_weather_temperature_night),

	BLE_OBD(R.string.shared_string_obd, R.drawable.widget_battery_day, R.drawable.widget_battery_day, R.drawable.widget_battery_night),

	BLE_BATTERY(R.string.map_widget_battery, R.drawable.widget_battery_day, R.drawable.widget_battery_day, R.drawable.widget_battery_night),
	BLE_TEMPERATURE(R.string.map_settings_weather_temp, R.drawable.ic_action_thermometer, R.drawable.widget_weather_temperature_day, R.drawable.widget_weather_temperature_night),
	BLE_HEART_RATE(R.string.map_widget_ant_heart_rate, R.drawable.ic_action_sensor_heart_rate_outlined, R.drawable.widget_sensor_heart_rate_day, R.drawable.widget_sensor_heart_rate_night),
	BLE_BLOOD_PRESSURE(R.string.external_device_blood_pressure, R.drawable.ic_action_sensor_heart_rate_outlined, R.drawable.widget_sensor_heart_rate_day, R.drawable.widget_sensor_heart_rate_night),
	BLE_BICYCLE_SCD(R.string.bicycle_scd_device_name, R.drawable.ic_action_sensor_cadence_outlined, R.drawable.widget_sensor_cadence_day, R.drawable.widget_sensor_cadence_night),
	BLE_RUNNING_SCDS(R.string.running_scds_device_name, R.drawable.ic_action_sensor_cadence_outlined, R.drawable.widget_sensor_cadence_day, R.drawable.widget_sensor_cadence_night);

	@StringRes
	public final int titleId;
	@DrawableRes
	public final int disconnectedIconId;
	@DrawableRes
	public final int dayIconId;
	@DrawableRes
	public final int nightIconId;

	DeviceType(int titleId, int disconnectedIconId,  int dayIconId, int nightIconId) {
		this.titleId = titleId;
		this.disconnectedIconId = disconnectedIconId;
		this.dayIconId = dayIconId;
		this.nightIconId = nightIconId;
	}
}