package net.osmand.shared.obd

enum class OBDCommand(val command: String, val responseParser: (String) -> String) {
	OBD_RPM_COMMAND("010C\r", OBDUtils::parseRpmResponse),
	OBD_SPEED_COMMAND("010D\r", OBDUtils::parseSpeedResponse),
	OBD_ENGINE_COOLANT_TEMP_COMMAND("0105\r", OBDUtils::parseEngineCoolantTempResponse)

}