package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.WidgetType;

public enum SensorWidgetDataFieldType {
	TEMPERATURE(WidgetType.TEMPERATURE, R.drawable.ic_action_sensor_temperature_outlined, R.drawable.widget_sensor_temperature_day, R.drawable.widget_sensor_temperature_night, R.drawable.ic_action_sensor_temperature_battery_outlined, R.drawable.widget_sensor_temperature_battery_day, R.drawable.widget_sensor_temperature_battery_night),
	HEART_RATE(WidgetType.HEART_RATE, R.drawable.ic_action_sensor_heart_rate_outlined, R.drawable.widget_sensor_heart_rate_day, R.drawable.widget_sensor_heart_rate_night, R.drawable.ic_action_sensor_heart_rate_battery_outlined, R.drawable.widget_sensor_heart_rate_battery_day, R.drawable.widget_sensor_heart_rate_battery_night),
	BATTERY(WidgetType.BATTERY, R.drawable.widget_battery_day, R.drawable.widget_battery_day, R.drawable.widget_battery_day, R.drawable.widget_battery_day, R.drawable.widget_battery_day, R.drawable.widget_battery_day),
	RSSI(WidgetType.RSSI, R.drawable.ic_action_signal, R.drawable.ic_action_signal, R.drawable.ic_action_signal, R.drawable.ic_action_signal, R.drawable.ic_action_signal, R.drawable.ic_action_signal),
	BIKE_POWER(WidgetType.BICYCLE_POWER, R.drawable.ic_action_sensor_bicycle_power_outlined, R.drawable.widget_sensor_bicycle_power_day, R.drawable.widget_sensor_bicycle_power_night, R.drawable.ic_action_sensor_bicycle_power_battery_outlined, R.drawable.widget_sensor_bicycle_power_battery_day, R.drawable.widget_sensor_bicycle_power_battery_night),
	BIKE_SPEED(WidgetType.BICYCLE_SPEED, R.drawable.ic_action_sensor_speed_outlined, R.drawable.widget_sensor_speed_day, R.drawable.widget_sensor_speed_night, R.drawable.ic_action_sensor_speed_battery_outlined, R.drawable.widget_sensor_speed_battery_day, R.drawable.widget_sensor_speed_battery_night),
	BIKE_CADENCE(WidgetType.BICYCLE_CADENCE, R.drawable.ic_action_sensor_cadence_outlined, R.drawable.widget_sensor_cadence_day, R.drawable.widget_sensor_cadence_night, R.drawable.ic_action_sensor_cadence_battery_outlined, R.drawable.widget_sensor_cadence_battery_day, R.drawable.widget_sensor_cadence_battery_night),
	BIKE_DISTANCE(WidgetType.BICYCLE_DISTANCE, R.drawable.ic_action_sensor_distance_outlined, R.drawable.widget_sensor_distance_day, R.drawable.widget_sensor_distance_night, R.drawable.ic_action_sensor_distance_battery_outlined, R.drawable.widget_sensort_distance_battery_day, R.drawable.widget_sensort_distance_battery_night);

	private final WidgetType widgetType;

	@DrawableRes
	public final int disconnectedIconId;
	@DrawableRes
	public final int dayIconId;
	@DrawableRes
	public final int nightIconId;
	@DrawableRes
	public final int disconnectedBatteryIconId;
	@DrawableRes
	public final int dayBatteryIconId;
	@DrawableRes
	public final int nightBatteryIconId;


	SensorWidgetDataFieldType(@NonNull WidgetType widgetType, int disconnectedIconId, int dayIconId, int nightIconId, int disconnectedBatteryIconId, int dayBatteryIconId, int nightBatteryIconId) {
		this.widgetType = widgetType;
		this.disconnectedIconId = disconnectedIconId;
		this.dayIconId = dayIconId;
		this.nightIconId = nightIconId;
		this.dayBatteryIconId = dayBatteryIconId;
		this.nightBatteryIconId = nightBatteryIconId;
		this.disconnectedBatteryIconId = disconnectedBatteryIconId;
	}

	public WidgetType getWidgetType() {
		return widgetType;
	}
}
