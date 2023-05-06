package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;

import net.osmand.plus.views.mapwidgets.WidgetType;

public enum SensorWidgetDataFieldType {
	TEMPERATURE(WidgetType.WEATHER_TEMPERATURE_WIDGET),
	HEART_RATE(WidgetType.ANT_HEART_RATE),
	BATTERY(WidgetType.BATTERY),
	BIKE_POWER(WidgetType.ANT_BICYCLE_POWER),
	BIKE_SPEED(WidgetType.ANT_BICYCLE_SPEED),
	BIKE_CADENCE(WidgetType.ANT_BICYCLE_CADENCE),
	BIKE_DISTANCE(WidgetType.ANT_BICYCLE_DISTANCE);

	private final WidgetType widgetType;

	SensorWidgetDataFieldType(@NonNull WidgetType widgetType) {
		this.widgetType = widgetType;
	}

	public WidgetType getWidgetType() {
		return widgetType;
	}
}
