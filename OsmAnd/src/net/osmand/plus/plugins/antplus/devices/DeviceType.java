package net.osmand.plus.plugins.antplus.devices;

import net.osmand.plus.plugins.antplus.GattAttributes;
import net.osmand.plus.views.mapwidgets.WidgetType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public enum DeviceType {
	HRM(GattAttributes.SERVICE_HEART_RATE, "Heart rate", WidgetType.ANT_HEART_RATE, UUID.fromString(GattAttributes.SERVICE_HEART_RATE)),
	BIKE_SPEED(GattAttributes.SERVICE_CYCLING_SPEED_AND_CADENCE, "Cycle speed", WidgetType.ANT_BICYCLE_SPEED, UUID.fromString(GattAttributes.SERVICE_CYCLING_SPEED_AND_CADENCE)),
	BIKE_CADENCE(GattAttributes.SERVICE_CYCLING_SPEED_AND_CADENCE, "Cycle cadence", WidgetType.ANT_BICYCLE_CADENCE, UUID.fromString(GattAttributes.SERVICE_CYCLING_SPEED_AND_CADENCE)),
	TEMPERATURE(GattAttributes.SERVICE_TEMPERATURE, "Temperature", WidgetType.WEATHER_TEMPERATURE_WIDGET, UUID.fromString(GattAttributes.CHAR_TEMPERATURE_MEASUREMENT));

	private final String uuidService;
	private final ArrayList<UUID> sensorCharacteristicUUIDs = new ArrayList<>();
	private final String name;
	private final WidgetType widgetType;

	DeviceType(String uuidService, String name, WidgetType widgetType, UUID... uuids) {
		this.uuidService = uuidService;
		this.name = name;
		this.sensorCharacteristicUUIDs.addAll(Arrays.asList(uuids));
		this.widgetType = widgetType;
	}

	public String getUUIDService() {
		return uuidService;
	}

	public String getName() {
		return name;
	}

	public WidgetType getWidgetType() {
		return widgetType;
	}

	private boolean isDeviceAttribute(UUID uuidToCheck) {
		for (UUID uuid : sensorCharacteristicUUIDs) {
			if (uuid.equals(uuidToCheck)) {
				return true;
			}
		}
		return false;
	}

	public static DeviceType getDeviceTypeByUuid(UUID uuid) {
		for (DeviceType type : values()) {
			if (type.isDeviceAttribute(uuid)) {
				return type;
			}
		}
		return null;
	}
}