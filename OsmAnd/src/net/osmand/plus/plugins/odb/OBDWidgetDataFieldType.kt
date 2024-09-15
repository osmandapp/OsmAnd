package net.osmand.plus.plugins.odb

import androidx.annotation.DrawableRes
import net.osmand.plus.R
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.shared.obd.OBDCommand

enum class OBDWidgetDataFieldType(
	val widgetType: WidgetType, @DrawableRes val iconId: Int, val command: OBDCommand) {
	RPM(WidgetType.OBD_RPM, R.drawable.ic_action_thermometer, OBDCommand.OBD_RPM_COMMAND),
	SPEED(WidgetType.OBD_SPEED, R.drawable.ic_action_sensor_heart_rate_outlined, OBDCommand.OBD_SPEED_COMMAND),
	FUEL_LVL(WidgetType.OBD_FUEL_LEVEL, R.drawable.widget_battery_day, OBDCommand.OBD_FUEL_LEVEL_COMMAND),
	AIR_INTAKE_TEMP(WidgetType.OBD_AIR_INTAKE_TEMP, R.drawable.ic_action_signal, OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND),
	COOLANT_TEMP(WidgetType.OBD_ENGINE_COOLANT_TEMP, R.drawable.ic_action_sensor_bicycle_power_outlined, OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND),
	FUEL_TYPE(WidgetType.OBD_FUEL_TYPE, R.drawable.ic_action_speed_outlined, OBDCommand.OBD_FUEL_TYPE_COMMAND)
}