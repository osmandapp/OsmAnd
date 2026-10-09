package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.WidgetType;

public enum SensorWidgetDataFieldType {
	TEMPERATURE(WidgetType.TEMPERATURE, R.drawable.ic_action_thermometer),
	HEART_RATE(WidgetType.HEART_RATE, R.drawable.ic_action_sensor_heart_rate_outlined),
	BATTERY(WidgetType.BATTERY, R.drawable.widget_battery_day),
	RSSI(WidgetType.RSSI, R.drawable.ic_action_signal),
	BIKE_POWER(WidgetType.BICYCLE_POWER, R.drawable.ic_action_sensor_bicycle_power_outlined),
	BIKE_SPEED(WidgetType.BICYCLE_SPEED, R.drawable.ic_action_speed_outlined),
	BIKE_CADENCE(WidgetType.BICYCLE_CADENCE, R.drawable.ic_action_sensor_cadence_outlined),
	BIKE_DISTANCE(WidgetType.BICYCLE_DISTANCE, R.drawable.ic_action_sensor_distance_outlined);

	private final WidgetType widgetType;
	@DrawableRes
	private int iconId;

	SensorWidgetDataFieldType(@NonNull WidgetType widgetType, @DrawableRes int iconId) {
		this.widgetType = widgetType;
		this.iconId = iconId;
	}

	public WidgetType getWidgetType() {
		return widgetType;
	}

	@DrawableRes
	public int getIconId() {
		return iconId;
	}
}
