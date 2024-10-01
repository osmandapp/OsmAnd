package net.osmand.shared.obd

enum class OBDCommand(
	val commandGroup: String,
	val command: String,
	private val responseParser: (String) -> String,
	val isStale: Boolean = false,
	val isMultiPartResponse: Boolean = false) {
	OBD_SUPPORTED_LIST1_COMMAND("01", "00", OBDUtils::parseSupportedCommandsResponse, true),
	OBD_SUPPORTED_LIST2_COMMAND("01", "20", OBDUtils::parseSupportedCommandsResponse, true),
	OBD_SUPPORTED_LIST3_COMMAND("01", "40", OBDUtils::parseSupportedCommandsResponse, true),
	OBD_BATTERY_VOLTAGE_COMMAND("01", "42", OBDUtils::parseBatteryVoltageResponse),
	OBD_AMBIENT_AIR_TEMPERATURE_COMMAND("01", "46", OBDUtils::parseAmbientTempResponse),
	OBD_RPM_COMMAND("01", "0C", OBDUtils::parseRpmResponse),
	OBD_SPEED_COMMAND("01", "0D", OBDUtils::parseSpeedResponse),
	OBD_AIR_INTAKE_TEMP_COMMAND("01", "0F", OBDUtils::parseIntakeAirTempResponse),
	OBD_ENGINE_COOLANT_TEMP_COMMAND("01", "05", OBDUtils::parseEngineCoolantTempResponse),
	OBD_FUEL_CONSUMPTION_RATE_COMMAND("01", "5E", OBDUtils::parseFuelConsumptionRateResponse),
	OBD_FUEL_TYPE_COMMAND("01", "51", OBDUtils::parseFuelTypeResponse, true),
	OBD_VIN_COMMAND("09", "02", OBDUtils::parseVINResponse, true, true),
	OBD_FUEL_LEVEL_COMMAND("01", "2F", OBDUtils::parseFuelLevelResponse);

	fun parseResponse(response: String): String {
		return responseParser.invoke(response.lowercase())
	}
}