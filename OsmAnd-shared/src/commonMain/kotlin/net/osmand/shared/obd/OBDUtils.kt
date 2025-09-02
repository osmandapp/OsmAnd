package net.osmand.shared.obd


object OBDUtils {

	fun parseRpmResponse(response: IntArray): OBDDataField<Any> {
		val result = (response[0] * 256 + response[1]) / 4
		return OBDDataField(result)
	}

	fun parseEngineRuntime(response: IntArray): OBDDataField<Any> {
		val result = (256 * response[0]) + response[1]
		return OBDDataField(result)
	}

	fun parseSpeedResponse(response: IntArray): OBDDataField<Any> {
		return if(response.isNotEmpty()) {
			OBDDataField(response[0])
		} else {
			OBDDataField(0)
		}
	}

	fun parseTempResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] - 40
		return OBDDataField(result)
	}

	fun parseFuelPressureResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * 3
		return OBDDataField(result)
	}

	fun parsePercentResponse(response: IntArray): OBDDataField<Any> {
		val result = response[0] * (100.0f / 255.0f)
		return OBDDataField(result)
	}

	fun parseBatteryVoltageResponse(response: IntArray): OBDDataField<Any> {
		val result = ((response[0] * 256) + response[1]).toFloat() / 1000
		return OBDDataField(result)
	}

	fun parseAltBatteryVoltageResponse(response: IntArray): OBDDataField<Any> {
		val byteArray = response.map { it.toByte() }.toByteArray()
		var strValue = byteArray.decodeToString()
		strValue = strValue.substring(0, strValue.length - 1)
		val fValue = strValue.toFloat() / 10f
		return OBDDataField(fValue)
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