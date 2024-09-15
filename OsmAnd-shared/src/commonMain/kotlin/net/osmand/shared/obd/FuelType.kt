package net.osmand.shared.obd

import net.osmand.shared.util.Localization

enum class FuelType(val code: String, val screenNamId: String) {
	NO_PROVIDED("0x00", "obd_fuel_type_not_provided"),
	GASOLINE("0x01", "obd_fuel_type_gasoline"),
	METHANOL("0x02", "obd_fuel_type_methanol"),
	ETHANOL("0x03", "obd_fuel_type_ethanol"),
	DIESEL("0x04", "obd_fuel_type_diesel"),
	LPG("0x05", "obd_fuel_type_lpg"),
	CNG("0x06", "obd_fuel_type_cng"),
	PROPANE("0x07", "obd_fuel_type_propane"),
	ELECTRIC("0x08", "obd_fuel_type_electric"),
	BIFUEL_RUNNING_GASOLINE("0x09", "obd_fuel_type_bifuel_gasoline"),
	BIFUEL_RUNNING_METHANOL("0x0A", "obd_fuel_type_bifuel_methanol"),
	BIFUEL_RUNNING_ETHANOL("0x0B", "obd_fuel_type_bifuel_ethanol"),
	BIFUEL_RUNNING_LPG("0x0C", "obd_fuel_type_bifuel_lpg"),
	BIFUEL_RUNNING_CNG("0x0D", "obd_fuel_type_bifuel_cng"),
	BIFUEL_RUNNING_PROPANE("0x0E", "obd_fuel_type_bifuel_propane"),
	BIFUEL_RUNNING_ELECTRICITY("0x0F", "obd_fuel_type_bifuel_electricity"),
	BIFUEL_RUNNING_ELECTRIC_COMBUSTION_ENGINE("0x10", "obd_fuel_type_bifuel_electric_combustion"),
	HYBRID_GASOLINE("0x11", "obd_fuel_type_hybrid_gasoline"),
	HYBRID_ETHANOL("0x12", "obd_fuel_type_hybrid_ethanol"),
	HYBRID_DIESEL("0x13", "obd_fuel_type_hybrid_diesel"),
	HYBRID_ELECTRIC("0x14", "obd_fuel_type_hybrid_electric"),
	HYBRID_ELECTRIC_COMBUSTION_ENGINE("0x15", "obd_fuel_type_hybrid_electric_combustion"),
	HYBRID_REGENERATIVE("0x16", "obd_fuel_type_hybrid_regenerative"),
	BIFUEL_RUNNING_HYDROGEN("0x17", "obd_fuel_type_bifuel_hydrogen"),
	HYBRID_HYDROGEN("0x18", "obd_fuel_type_hybrid_hydrogen"),
	HYDROGEN("0x19", "obd_fuel_type_hydrogen"),
	UNKNOWN("0xFF", "obd_fuel_type_unknown");

	fun getDisplayName(): String {
		return Localization.getString(screenNamId)
	}

	companion object {
		fun fromCode(code: String): FuelType {
			return when (code.uppercase()) {
				"0x00" -> NO_PROVIDED
				"0x01" -> GASOLINE
				"0x02" -> METHANOL
				"0x03" -> ETHANOL
				"0x04" -> DIESEL
				"0x05" -> LPG
				"0x06" -> CNG
				"0x07" -> PROPANE
				"0x08" -> ELECTRIC
				"0x09" -> BIFUEL_RUNNING_GASOLINE
				"0x0A" -> BIFUEL_RUNNING_METHANOL
				"0x0B" -> BIFUEL_RUNNING_ETHANOL
				"0x0C" -> BIFUEL_RUNNING_LPG
				"0x0D" -> BIFUEL_RUNNING_CNG
				"0x0E" -> BIFUEL_RUNNING_PROPANE
				"0x0F" -> BIFUEL_RUNNING_ELECTRICITY
				"0x10" -> BIFUEL_RUNNING_ELECTRIC_COMBUSTION_ENGINE
				"0x11" -> HYBRID_GASOLINE
				"0x12" -> HYBRID_ETHANOL
				"0x13" -> HYBRID_DIESEL
				"0x14" -> HYBRID_ELECTRIC
				"0x15" -> HYBRID_ELECTRIC_COMBUSTION_ENGINE
				"0x16" -> HYBRID_REGENERATIVE
				"0x17" -> BIFUEL_RUNNING_HYDROGEN
				"0x18" -> HYBRID_HYDROGEN
				"0x19" -> HYDROGEN
				else -> UNKNOWN
			}
		}
	}
}