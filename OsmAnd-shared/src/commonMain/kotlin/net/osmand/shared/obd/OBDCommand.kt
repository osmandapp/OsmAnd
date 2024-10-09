package net.osmand.shared.obd

import net.osmand.shared.obd.Obd2Connection.COMMAND_TYPE.*

enum class OBDCommand(
	val commandGroup: Int,
	val command: Int,
	private val responseParser: (IntArray) -> OBDDataField<Any>,
	val commandType: Obd2Connection.COMMAND_TYPE = LIVE,
	val isStale: Boolean = false) {
	OBD_BATTERY_VOLTAGE_COMMAND(0x01, 0x42, OBDUtils::parseBatteryVoltageResponse),
	OBD_AMBIENT_AIR_TEMPERATURE_COMMAND(0x01, 0x46, OBDUtils::parseAmbientTempResponse),
	OBD_RPM_COMMAND(0x01, 0x0C, OBDUtils::parseRpmResponse),
	OBD_SPEED_COMMAND(0x01, 0x0D, OBDUtils::parseSpeedResponse),
	OBD_AIR_INTAKE_TEMP_COMMAND(0x01, 0x0F, OBDUtils::parseIntakeAirTempResponse),
	OBD_ENGINE_COOLANT_TEMP_COMMAND(0x01, 0x05, OBDUtils::parseEngineCoolantTempResponse),
	OBD_FUEL_CONSUMPTION_RATE_COMMAND(0x01, 0x5E, OBDUtils::parseFuelConsumptionRateResponse),
	OBD_FUEL_TYPE_COMMAND(0x01, 0x51, OBDUtils::parseFuelTypeResponse, isStale = true),
	OBD_VIN_COMMAND(0x09, 0x02, OBDUtils::parseVINResponse, IDENTIFICATION, true),
	OBD_FUEL_LEVEL_COMMAND(0x01, 0x2F, OBDUtils::parseFuelLevelResponse);

	fun parseResponse(response: IntArray): OBDDataField<Any> {
		return responseParser.invoke(response)
	}
}