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

	fun parseRpmResponse(response: IntArray): OBDDataField<Any> {
		val result = (response[0] * 256 + response[1]) / 4
		return OBDDataField(
			OBDDataFieldType.RPM,
			result)
	}

	fun parseSpeedResponse(response: IntArray): OBDDataField<Any> {
		if(response.isNotEmpty()) {
			return OBDDataField(
				OBDDataFieldType.SPEED,
				response[0])
		} else {
			return OBDDataField(
				OBDDataFieldType.SPEED,
				0)
		}
	}

	fun parseIntakeAirTempResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * (100.0f / 255.0f)
		return OBDDataField(
			OBDDataFieldType.AIR_INTAKE_TEMP,
			result)
	}

	fun parseAmbientTempResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * (100.0f / 255.0f)
		return OBDDataField(
			OBDDataFieldType.AMBIENT_AIR_TEMP,
			result)
	}

	fun parseEngineCoolantTempResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * (100.0f / 255.0f)
		return OBDDataField(
			OBDDataFieldType.COOLANT_TEMP,
			result)
	}

	fun parseFuelLevelResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * (100.0f / 255.0f)
		return OBDDataField(
			OBDDataFieldType.FUEL_LVL,
			result)
	}

	fun parseBatteryVoltageResponse(response: IntArray): OBDDataField<Any> {
		return OBDDataField(
			OBDDataFieldType.BATTERY_VOLTAGE,
			((response[0] * 256) + response[1]) / 1000)
	}

	fun parseFuelTypeResponse(response: IntArray): OBDDataField<Any> {
		return OBDDataField(
			OBDDataFieldType.FUEL_TYPE,
			response[0])
	}

	fun parseVINResponse(response: IntArray): OBDDataField<Any> {
		val vin = StringBuilder()
		for (i in 1 until response.size) {
			vin.append(response[i].toChar())
		}
		return OBDDataField(
			OBDDataFieldType.VIN,
			vin)
	}

	fun parseFuelConsumptionRateResponse(response: IntArray): OBDDataField<Any> {
		val result = ((response[0] * 256) + response[1]) / 20.0
		return OBDDataField(
			OBDDataFieldType.FUEL_CONSUMPTION_RATE,
			result)
	}
}