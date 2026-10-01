package net.osmand.plus.plugins.externalsensors.devices.ble

/**
 * Decodes BLE tire pressure sensors (TPMS). They never accept a GATT connection:
 * pressure, temperature and battery are broadcast in the manufacturer data of the advertisement.
 *
 * Formats follow the open decoders bkbilly/tpms_ble (MIT) and VincentMasselis/TPMS-advanced (Apache-2.0).
 * Offsets are relative to the manufacturer data after the 2-byte company id.
 */
object TpmsAdvertisementDecoder {

	/** Generic Chinese cap sensors (ZEEPIN, Aramox, TP630, "TPMSII" app, Sysgration OEM). */
	const val COMPANY_GENERIC = 0x0100

	/** WODHMIEY / ITPMS (K) / LYTPMS. */
	const val COMPANY_WODHMIEY = 0x00AC

	/** Salutica FOBO Tyre. */
	const val COMPANY_FOBO = 0x0127

	@JvmField
	val COMPANY_IDS = intArrayOf(COMPANY_GENERIC, COMPANY_WODHMIEY, COMPANY_FOBO)

	data class Reading(
		/** Gauge pressure. */
		val pressureKpa: Float,
		val temperatureC: Float?,
		/** 0..100 or null when the sensor reports only a voltage. */
		val batteryPercent: Int?,
		val alarm: Boolean,
		/** 0 - front left, 1 - front right, 2 - rear left, 3 - rear right; null when not broadcast. */
		val wheel: Int?
	)

	@JvmStatic
	fun decode(companyId: Int, data: ByteArray): Reading? = when (companyId) {
		COMPANY_GENERIC -> decodeGeneric(data)
		COMPANY_WODHMIEY -> decodeWodhmiey(data)
		COMPANY_FOBO -> decodeFobo(data)
		else -> null
	}

	// [0] 0x80 + wheel, [1..2] EA CA, [3..5] id, [6..9] Pa int32 LE, [10..13] 0.01 °C int32 LE, [14] battery %, [15] alarm
	private fun decodeGeneric(d: ByteArray): Reading? {
		if (d.size != 16 || u8(d, 1) != 0xEA || u8(d, 2) != 0xCA) {
			return null
		}
		val wheel = u8(d, 0) - 0x80
		return Reading(
			pressureKpa = int32le(d, 6) / 1000f,
			temperatureC = int32le(d, 10) / 100f,
			batteryPercent = u8(d, 14).coerceIn(0, 100),
			alarm = d[15].toInt() != 0,
			wheel = if (wheel in 0..3) wheel else null
		)
	}

	// [0] voltage (x0.01 + 1.22 V), [1] pressure low byte, [2] °C + 55, [3] alarm, [6] pressure high bit
	private fun decodeWodhmiey(d: ByteArray): Reading? {
		if (d.size < 7 || u8(d, 6) > 1) {
			return null
		}
		val raw = u8(d, 1) + (u8(d, 6) shl 8)
		val voltage = u8(d, 0) * 0.01f + 1.22f
		return Reading(
			pressureKpa = raw * 3.144f,
			temperatureC = (u8(d, 2) - 55).toFloat(),
			batteryPercent = ((voltage - 1.8f) / 1.2f * 100).toInt().coerceIn(0, 100),
			alarm = u8(d, 3) != 0,
			wheel = null
		)
	}

	// last 4 bytes: [0] status, [1] °C + 50, [2..3] BE: bits 0-9 kPa, bits 10-15 battery x100 mV
	private fun decodeFobo(d: ByteArray): Reading? {
		if (d.size < 4) {
			return null
		}
		val o = d.size - 4
		val raw = (u8(d, o + 2) shl 8) or u8(d, o + 3)
		val voltage = ((raw shr 10) and 0x3F) / 10f
		return Reading(
			pressureKpa = (raw and 0x3FF).toFloat(),
			temperatureC = (u8(d, o + 1) - 50).toFloat(),
			batteryPercent = lithiumPercent(voltage),
			alarm = false,
			wheel = null
		)
	}

	private fun lithiumPercent(voltage: Float): Int = when {
		voltage >= 3.0f -> 100
		voltage <= 2.6f -> 0
		else -> ((voltage - 2.6f) / 0.4f * 100).toInt()
	}

	private fun u8(d: ByteArray, i: Int): Int = d[i].toInt() and 0xFF

	private fun int32le(d: ByteArray, i: Int): Int =
		u8(d, i) or (u8(d, i + 1) shl 8) or (u8(d, i + 2) shl 16) or (u8(d, i + 3) shl 24)
}
