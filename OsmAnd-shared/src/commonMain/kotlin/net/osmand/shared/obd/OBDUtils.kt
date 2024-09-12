package net.osmand.shared.obd

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
		if (hexValues[0] == "41" && hexValues[1] == OBDCommand.OBD_INTAKE_AIR_TEMP_COMMAND.command.lowercase()) {
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
			return (hexValues[2].toInt(16) / 255 * 100).toString()
		}
		return INVALID_RESPONSE_CODE
	}

	fun parseFuelTypeResponse(response: String): String {
		val responseParts = response.split(" ")

		if (responseParts[0] == "41" && responseParts[1] == OBDCommand.OBD_FUEL_TYPE_COMMAND.command.lowercase()) {
			val fuelTypeCode = responseParts[2].toInt(16)
			return when (fuelTypeCode) {
				0x01 -> "Бензин"
				0x02 -> "Метанол"
				0x03 -> "Этанол"
				0x04 -> "Дизель"
				0x05 -> "Пропан"
				0x06 -> "Природный газ (сжатый)"
				0x07 -> "Природный газ (сжиженный)"
				0x08 -> "Сжиженный нефтяной газ (LPG)"
				0x09 -> "Электричество"
				0x0A -> "Гибрид (бензин/электричество)"
				0x0B -> "Гибрид (дизель/электричество)"
				0x0C -> "Гибрид (сжатый природный газ)"
				0x0D -> "Гибрид (сжиженный природный газ)"
				0x0E -> "Гибрид (сжиженный нефтяной газ)"
				0x0F -> "Гибрид (аккумулятор)"
				else -> "Неизвестный тип топлива"
			}
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