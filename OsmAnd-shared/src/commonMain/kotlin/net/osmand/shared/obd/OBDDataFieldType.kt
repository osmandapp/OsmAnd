package net.osmand.shared.obd

import net.osmand.shared.util.Localization

enum class OBDDataFieldType(val nameId: String, private val unitNameId: String) {
	RPM("obd_rpm", "rpm_unit"),
	SPEED("obd_speed_desc", "km_h"),
	FUEL_LVL("obd_fuel_level_percent", "percent_unit"),
	AMBIENT_AIR_TEMP("obd_ambient_air_temp_desc", "degree_celsius"),
	BATTERY_VOLTAGE("obd_battery_voltage_desc", "unit_volt"),
	AIR_INTAKE_TEMP("obd_air_intake_temp_desc", "degree_celsius"),
	COOLANT_TEMP("obd_engine_coolant_temp", "degree_celsius"),
	FUEL_CONSUMPTION_RATE("obd_fuel_consumption_rate", "liter_per_hour"),
	VIN("obd_vin", ""),
	FUEL_TYPE("obd_fuel_type", ""),
	NO_DATA("", "");

	fun getDisplayName(): String {
		return Localization.getString(nameId)
	}

	fun getDisplayUnit(): String {
		return Localization.getString(unitNameId)
	}
}