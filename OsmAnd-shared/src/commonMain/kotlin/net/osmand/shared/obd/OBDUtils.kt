package net.osmand.shared.obd

import kotlin.math.roundToInt

object OBDUtils {
	const val INVALID_RESPONSE_CODE = "-1"

	fun parseSupportedCommandsResponse(response: String): String {
		val responseParts = response.trim().split(" ")

		if (responseParts.size >= 3 && responseParts[0] == "41") {
			var supportedPIDs = ""
			for (i in 2 until responseParts.size) {
				val byteValue = responseParts[i].toInt(16)
				for (bitIndex in 0..7) {
					if ((byteValue and (1 shl (7 - bitIndex))) != 0) {
						val pidNumber = ((i - 2) * 8) + bitIndex + 1
						supportedPIDs += (" ${pidNumber.toString(16)}")
					}
				}
			}
			return supportedPIDs
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseRpmResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues.size >= 4) {
			val A = hexValues[2].toInt(16)
			val B = hexValues[3].toInt(16)
			return (((A * 256) + B) / 4).toString()
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseSpeedResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues.size >= 3 && hexValues[0] == "41" && hexValues[1] == OBDCommand.OBD_SPEED_COMMAND.command.lowercase()) {
			return (hexValues[2].toInt(16)).toString()
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseIntakeAirTempResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues[0] == "41" && hexValues[1] == OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND.command.lowercase()) {
			val intakeAirTemp = hexValues[2].toInt(16)
			return (intakeAirTemp - 40).toString()
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseAmbientTempResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues[0] == "41" && hexValues[1] == OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND.command.lowercase()) {
			val intakeAirTemp = hexValues[2].toInt(16)
			return (intakeAirTemp - 40).toString()
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseEngineCoolantTempResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues.size >= 3 && hexValues[0] == "41" && hexValues[1] == OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND.command.lowercase()) {
			return (hexValues[2].toInt(16) - 40).toString()
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseFuelLevelResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues.size >= 3 && hexValues[0] == "41" && hexValues[1] == OBDCommand.OBD_FUEL_LEVEL_COMMAND.command.lowercase()) {
			return ((hexValues[2].toInt(16)
				.toFloat() / 255 * 100)).toString()
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseBatteryVoltageResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues.size >= 4 && hexValues[0] == "41" && hexValues[1] == OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND.command.lowercase()) {
			val a = hexValues[2].toInt(16).toFloat()
			val b = hexValues[3].toInt(16).toFloat()
			return (((a * 256) + b) / 1000).toString()
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseFuelTypeResponse(response: String): String {
		val responseParts = response.split(" ")

		if (responseParts[0] == "41" && responseParts[1] == OBDCommand.OBD_FUEL_TYPE_COMMAND.command.lowercase()) {
			return responseParts[2]
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseVINResponse(response: String): String {
		val responseParts = response.split(" ")

		if (responseParts[0] == "49" &&
			responseParts[1] == OBDCommand.OBD_VIN_COMMAND.command.lowercase() &&
			responseParts.size > 3) {
			val vinBuilder = StringBuilder()
			for (i in 3 .. responseParts.size) {
				val hexByte = responseParts[i]
				vinBuilder.append(hexByte.toInt(16).toChar())
			}
			return vinBuilder.toString()
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseFuelConsumptionRateResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues.size >= 4 && hexValues[0] == "41" && hexValues[1] == OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND.command.lowercase()) {
			val a = hexValues[2].toInt(16)
			val b = hexValues[3].toInt(16)
			return (((a * 256) + b) / 20.0).toString()
		}
		return INVALID_RESPONSE_CODE
	}
}