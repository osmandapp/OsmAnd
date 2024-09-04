package net.osmand.shared.obd

object OBDUtils {
	fun parseRpmResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues.size >= 4) {
			val A = hexValues[2].toInt(16)
			val B = hexValues[3].toInt(16)
			return (((A * 256) + B) / 4).toString()
		}
		return (-1).toString()  // Invalid response
	}

	fun parseSpeedResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues.size >= 3 && hexValues[0] == "41" && hexValues[1] == "0D") {
			return (hexValues[3].toInt(16)).toString()
		}
		return (-1).toString()  // Invalid response
	}

	fun parseEngineCoolantTempResponse(response: String): String {
		val hexValues = response.trim().split(" ")
		if (hexValues.size >= 3 && hexValues[0] == "41" && hexValues[1] == "05") {
			return (hexValues[3].toInt(16) - 40).toString()
		}
		return (-1).toString()  // Invalid response
	}
}