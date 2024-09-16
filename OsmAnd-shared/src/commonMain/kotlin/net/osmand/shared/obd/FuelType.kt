package net.osmand.shared.obd

import net.osmand.shared.util.Localization

enum class FuelType(val code: String, val screenNamId: String) {
	NO_PROVIDED("00", "obd_fuel_type_not_provided"),
	GASOLINE("01", "obd_fuel_type_gasoline"),
	METHANOL("02", "obd_fuel_type_methanol"),
	ETHANOL("03", "obd_fuel_type_ethanol"),
	DIESEL("04", "obd_fuel_type_diesel"),
	LPG("05", "obd_fuel_type_lpg"),
	CNG("06", "obd_fuel_type_cng"),
	PROPANE("07", "obd_fuel_type_propane"),
	ELECTRIC("08", "obd_fuel_type_electric"),
	BIFUEL_RUNNING_GASOLINE("09", "obd_fuel_type_bifuel_gasoline"),
	BIFUEL_RUNNING_METHANOL("0A", "obd_fuel_type_bifuel_methanol"),
	BIFUEL_RUNNING_ETHANOL("0B", "obd_fuel_type_bifuel_ethanol"),
	BIFUEL_RUNNING_LPG("0C", "obd_fuel_type_bifuel_lpg"),
	BIFUEL_RUNNING_CNG("0D", "obd_fuel_type_bifuel_cng"),
	BIFUEL_RUNNING_PROPANE("0E", "obd_fuel_type_bifuel_propane"),
	BIFUEL_RUNNING_ELECTRICITY("0F", "obd_fuel_type_bifuel_electricity"),
	BIFUEL_RUNNING_ELECTRIC_COMBUSTION_ENGINE("10", "obd_fuel_type_bifuel_electric_combustion"),
	HYBRID_GASOLINE("11", "obd_fuel_type_hybrid_gasoline"),
	HYBRID_ETHANOL("12", "obd_fuel_type_hybrid_ethanol"),
	HYBRID_DIESEL("13", "obd_fuel_type_hybrid_diesel"),
	HYBRID_ELECTRIC("14", "obd_fuel_type_hybrid_electric"),
	HYBRID_ELECTRIC_COMBUSTION_ENGINE("15", "obd_fuel_type_hybrid_electric_combustion"),
	HYBRID_REGENERATIVE("16", "obd_fuel_type_hybrid_regenerative"),
	BIFUEL_RUNNING_HYDROGEN("17", "obd_fuel_type_bifuel_hydrogen"),
	HYBRID_HYDROGEN("18", "obd_fuel_type_hybrid_hydrogen"),
	HYDROGEN("19", "obd_fuel_type_hydrogen"),
	UNKNOWN("FF", "obd_fuel_type_unknown");

	fun getDisplayName(): String {
		return Localization.getString(screenNamId)
	}

	companion object {
		fun fromCode(code: String): FuelType {
			return when (code.uppercase()) {
				"00" -> NO_PROVIDED
				"01" -> GASOLINE
				"02" -> METHANOL
				"03" -> ETHANOL
				"04" -> DIESEL
				"05" -> LPG
				"06" -> CNG
				"07" -> PROPANE
				"08" -> ELECTRIC
				"09" -> BIFUEL_RUNNING_GASOLINE
				"0A" -> BIFUEL_RUNNING_METHANOL
				"0B" -> BIFUEL_RUNNING_ETHANOL
				"0C" -> BIFUEL_RUNNING_LPG
				"0D" -> BIFUEL_RUNNING_CNG
				"0E" -> BIFUEL_RUNNING_PROPANE
				"0F" -> BIFUEL_RUNNING_ELECTRICITY
				"10" -> BIFUEL_RUNNING_ELECTRIC_COMBUSTION_ENGINE
				"11" -> HYBRID_GASOLINE
				"12" -> HYBRID_ETHANOL
				"13" -> HYBRID_DIESEL
				"14" -> HYBRID_ELECTRIC
				"15" -> HYBRID_ELECTRIC_COMBUSTION_ENGINE
				"16" -> HYBRID_REGENERATIVE
				"17" -> BIFUEL_RUNNING_HYDROGEN
				"18" -> HYBRID_HYDROGEN
				"19" -> HYDROGEN
				else -> UNKNOWN
			}
		}
	}
}