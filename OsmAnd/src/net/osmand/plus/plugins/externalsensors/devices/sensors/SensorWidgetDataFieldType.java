package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;

import net.osmand.plus.views.mapwidgets.WidgetType;

public enum SensorWidgetDataFieldType {
	TEMPERATURE(WidgetType.TEMPERATURE),
	HEART_RATE(WidgetType.HEART_RATE),
	BATTERY(WidgetType.BATTERY),
	RSSI(WidgetType.RSSI),
	BIKE_POWER(WidgetType.BICYCLE_POWER),
	BIKE_SPEED(WidgetType.BICYCLE_SPEED),
	BIKE_CADENCE(WidgetType.BICYCLE_CADENCE),
	BIKE_DISTANCE(WidgetType.BICYCLE_DISTANCE);

	private final WidgetType widgetType;

	SensorWidgetDataFieldType(@NonNull WidgetType widgetType) {
		this.widgetType = widgetType;
	}

	public WidgetType getWidgetType() {
		return widgetType;
	}
}
