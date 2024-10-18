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
		return OBDDataField(result)
	}

	fun parseSpeedResponse(response: IntArray): OBDDataField<Any> {
		return if(response.isNotEmpty()) {
			OBDDataField(response[0])
		} else {
			OBDDataField(0)
		}
	}

	fun parseIntakeAirTempResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * (100.0f / 255.0f)
		return OBDDataField(result)
	}

	fun parseAmbientTempResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * (100.0f / 255.0f)
		return OBDDataField(result)
	}

	fun parseEngineCoolantTempResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * (100.0f / 255.0f)
		return OBDDataField(result)
	}

	fun parseFuelLevelResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * (100.0f / 255.0f)
		return OBDDataField(result)
	}

	fun parseBatteryVoltageResponse(response: IntArray): OBDDataField<Any> {
		val result = ((response[0] * 256) + response[1]).toFloat() / 1000
		return OBDDataField(result)
	}

	fun parseFuelTypeResponse(response: IntArray): OBDDataField<Any> {
		return OBDDataField(response[0])
	}

	fun parseVINResponse(response: IntArray): OBDDataField<Any> {
		val vin = StringBuilder()
		for (i in 1 until response.size) {
			vin.append(response[i].toChar())
		}
		return OBDDataField(vin)
	}

	fun parseFuelConsumptionRateResponse(response: IntArray): OBDDataField<Any> {
		val result = ((response[0] * 256) + response[1]) / 20.0
		return OBDDataField(result)
	}
}