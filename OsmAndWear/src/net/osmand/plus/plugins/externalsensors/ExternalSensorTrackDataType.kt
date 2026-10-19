package net.osmand.plus.plugins.externalsensors

import androidx.annotation.StringRes
import net.osmand.plus.R
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType

enum class ExternalSensorTrackDataType(
	val preferenceId: String,
	val sensorType: SensorWidgetDataFieldType,
	@param:StringRes val titleId: Int
) {
	BIKE_SPEED(
		"speed_sensor_write_to_track_device",
		SensorWidgetDataFieldType.BIKE_SPEED,
		R.string.shared_string_speed
	),
	BIKE_CADENCE(
		"cadence_sensor_write_to_track_device",
		SensorWidgetDataFieldType.BIKE_CADENCE,
		R.string.map_widget_ant_bicycle_cadence
	),
	BIKE_POWER(
		"power_sensor_write_to_track_device",
		SensorWidgetDataFieldType.BIKE_POWER,
		R.string.map_widget_ant_bicycle_power
	),
	HEART_RATE(
		"heart_rate_sensor_write_to_track_device",
		SensorWidgetDataFieldType.HEART_RATE,
		R.string.map_widget_ant_heart_rate
	),
	TEMPERATURE(
		"temperature_sensor_write_to_track_device",
		SensorWidgetDataFieldType.TEMPERATURE,
		R.string.map_settings_weather_temp
	);

	companion object {
		fun getBySensorWidgetDataFieldType(dataFieldType: SensorWidgetDataFieldType): ExternalSensorTrackDataType? {
			for (widgetType in ExternalSensorTrackDataType.values()) {
				if (widgetType.sensorType == dataFieldType) {
					return widgetType
				}
			}
			return null
		}
	}
}
