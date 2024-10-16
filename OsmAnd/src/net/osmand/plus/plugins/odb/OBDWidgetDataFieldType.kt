package net.osmand.plus.plugins.odb

import androidx.annotation.DrawableRes
import net.osmand.plus.R
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.shared.obd.OBDCommand
import net.osmand.shared.obd.OBDDataFieldType

enum class OBDWidgetDataFieldType(
	val widgetType: WidgetType, @DrawableRes val iconId: Int, val command: OBDCommand, val dataType: OBDDataFieldType) {
	RPM(WidgetType.OBD_RPM, R.drawable.ic_action_thermometer, OBDCommand.OBD_RPM_COMMAND, OBDDataFieldType.RPM),
	SPEED(WidgetType.OBD_SPEED, R.drawable.ic_action_sensor_heart_rate_outlined, OBDCommand.OBD_SPEED_COMMAND, OBDDataFieldType.SPEED),
	FUEL_LEFT_PERCENT(WidgetType.OBD_FUEL_LEVEL_PERCENT, R.drawable.widget_battery_day, OBDCommand.OBD_FUEL_LEVEL_COMMAND, OBDDataFieldType.FUEL_LVL),
	FUEL_LVL_PERCENT(WidgetType.OBD_FUEL_LEVEL_LITER, R.drawable.widget_battery_day, OBDCommand.OBD_FUEL_LEVEL_COMMAND, OBDDataFieldType.FUEL_LVL),
	FUEL_LEFT_LITER(WidgetType.OBD_FUEL_LEFT_LITER, R.drawable.widget_battery_day, OBDCommand.OBD_FUEL_LEVEL_COMMAND, OBDDataFieldType.FUEL_LVL),
	FUEL_LEFT_DISTANCE(WidgetType.OBD_FUEL_LEFT_DISTANCE, R.drawable.widget_battery_day, OBDCommand.OBD_FUEL_LEVEL_COMMAND, OBDDataFieldType.FUEL_LVL),
	FUEL_CONSUMPTION_RATE_PERCENT_HOUR(WidgetType.OBD_FUEL_CONSUMPTION_RATE_PERCENT_HOUR, R.drawable.widget_battery_day, OBDCommand.OBD_FUEL_LEVEL_COMMAND, OBDDataFieldType.FUEL_LVL),
	FUEL_CONSUMPTION_RATE_LITER_HOUR(WidgetType.OBD_FUEL_CONSUMPTION_RATE_LITER_HOUR, R.drawable.widget_battery_day, OBDCommand.OBD_FUEL_LEVEL_COMMAND, OBDDataFieldType.FUEL_LVL),
	FUEL_CONSUMPTION_RATE_SENSOR(WidgetType.OBD_FUEL_CONSUMPTION_RATE_SENSOR, R.drawable.widget_battery_day, OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND, OBDDataFieldType.FUEL_CONSUMPTION_RATE),
	AMBIENT_AIR_TEMP(WidgetType.OBD_AMBIENT_AIR_TEMP, R.drawable.ic_action_thermometer, OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND, OBDDataFieldType.AMBIENT_AIR_TEMP),
	BATTERY_VOLTAGE(WidgetType.OBD_BATTERY_VOLTAGE, R.drawable.ic_action_thermometer, OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND, OBDDataFieldType.BATTERY_VOLTAGE),
	AIR_INTAKE_TEMP(WidgetType.OBD_AIR_INTAKE_TEMP, R.drawable.ic_action_signal, OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND, OBDDataFieldType.AIR_INTAKE_TEMP),
	COOLANT_TEMP(WidgetType.OBD_ENGINE_COOLANT_TEMP, R.drawable.ic_action_sensor_bicycle_power_outlined, OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND, OBDDataFieldType.COOLANT_TEMP),
	VIN(WidgetType.OBD_VIN, R.drawable.ic_action_sensor_bicycle_power_outlined, OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND, OBDDataFieldType.COOLANT_TEMP),
	FUEL_TYPE(WidgetType.OBD_FUEL_TYPE, R.drawable.ic_action_speed_outlined, OBDCommand.OBD_FUEL_TYPE_COMMAND, OBDDataFieldType.FUEL_TYPE)
}