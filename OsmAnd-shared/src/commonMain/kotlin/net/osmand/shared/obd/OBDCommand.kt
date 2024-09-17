package net.osmand.shared.obd

enum class OBDCommand(
	val command: String,
	private val responseParser: (String) -> String,
	val isStale: Boolean = false) {
	OBD_SUPPORTED_LIST1_COMMAND("00", OBDUtils::parseSupportedCommandsResponse, true),
	OBD_SUPPORTED_LIST2_COMMAND("20", OBDUtils::parseSupportedCommandsResponse, true),
	OBD_SUPPORTED_LIST3_COMMAND("40", OBDUtils::parseSupportedCommandsResponse, true),
	OBD_BATTERY_VOLTAGE_COMMAND("42", OBDUtils::parseBatteryVoltageResponse),
	OBD_AMBIENT_AIR_TEMPERATURE_COMMAND("46", OBDUtils::parseAmbientTempResponse),
	OBD_RPM_COMMAND("0C", OBDUtils::parseRpmResponse),
	OBD_SPEED_COMMAND("0D", OBDUtils::parseSpeedResponse),
	OBD_AIR_INTAKE_TEMP_COMMAND("0F", OBDUtils::parseIntakeAirTempResponse),
	OBD_ENGINE_COOLANT_TEMP_COMMAND("05", OBDUtils::parseEngineCoolantTempResponse),
	OBD_FUEL_CONSUMPTION_RATE_COMMAND("5E", OBDUtils::parseFuelConsumptionRateResponse),
	OBD_FUEL_TYPE_COMMAND("51", OBDUtils::parseFuelTypeResponse, true),
	OBD_FUEL_LEVEL_COMMAND("2F", OBDUtils::parseFuelLevelResponse);

	fun parseResponse(response: String): String {
		return responseParser.invoke(response.lowercase())
	}
}