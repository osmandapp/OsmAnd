package net.osmand.shared.obd

import net.osmand.shared.extensions.format
import net.osmand.shared.util.LoggerFactory

class Obd2Connection(private val connection: UnderlyingTransport) {
	enum class COMMAND_TYPE(val code: Int) {
		LIVE(0x41), FREEZE(0x42), IDENTIFICATION(0x49)
	}

	private val initCommands = arrayOf("ATD", "ATZ", "AT E0", "AT L0", "AT S0", "AT H0", "AT SP 0")
	private val log = LoggerFactory.getLogger("Obd2Connection")

	init {
		runInitCommands()
	}

	private fun runInitCommands() {
		for (command in initCommands) {
			runImpl(command)
		}
	}

	private fun runImpl(command: String): String {
		val response = StringBuilder()
		log.debug("runImpl($command)")
		connection.write((command + "\r").encodeToByteArray())
		while (true) {
			val value = connection.readByte() ?: continue
			val c = value.toChar()
			// this is the prompt, stop here
			if (c == '>') break
			if (c == '\r' || c == '\n' || c == ' ' || c == '\t' || c == '.') continue
			response.append(c)
		}
		val responseValue = response.toString()
		log.debug("runImpl() returned $responseValue")
		return responseValue
	}

	fun run(
		fullCommand: String,
		command: Int,
		commandType: COMMAND_TYPE = COMMAND_TYPE.LIVE): IntArray {
		var response = runImpl(fullCommand)
		val originalResponseValue = response
		val unspacedCommand = fullCommand.replace(" ", "")
		if (response.startsWith(unspacedCommand))
			response = response.substring(unspacedCommand.length)
		response = unpackLongFrame(response)
		log.debug("post-processed response $response")
		response = removeSideData(
			response,
			"SEARCHING",
			"ERROR",
			"BUS INIT",
			"BUSINIT",
			"BUS ERROR",
			"BUSERROR",
			"STOPPED"
		)
		when (response) {
			"OK" -> return intArrayOf(1)
			"?" -> return intArrayOf(0)
			"NODATA" -> return intArrayOf()
			"UNABLETOCONNECT" -> throw Exception("connection failure")
			"CANERROR" -> throw Exception("CAN bus error")
		}
		try {
			var hexValues = toHexValues(response)
			if (hexValues.size < 3 ||
				hexValues[0] != commandType.code ||
				hexValues[1] != command) {
				log.debug("Incorrect answer data (size ${hexValues.size}) for $fullCommand")
			} else {
				hexValues = hexValues.copyOfRange(2, hexValues.size)
			}
			return hexValues
		} catch (e: IllegalArgumentException) {
			log.debug(
				"Conversion error: command: '$fullCommand', original response: '$originalResponseValue', processed response: '$response'"
			)
			throw e
		}
	}

	private fun toHexValues(buffer: String): IntArray {
		val values = IntArray(buffer.length / 2)
		for (i in values.indices) {
			values[i] = 16 * toDigitValue(buffer[2 * i]) + toDigitValue(buffer[2 * i + 1])
		}
		return values
	}

	private fun toDigitValue(c: Char): Int {
		return when (c) {
			in '0'..'9' -> c - '0'
			'a', 'A' -> 10
			'b', 'B' -> 11
			'c', 'C' -> 12
			'd', 'D' -> 13
			'e', 'E' -> 14
			'f', 'F' -> 15
			else -> throw IllegalArgumentException("$c is not a valid hex digit")
		}
	}

	private fun removeSideData(response: String, vararg patterns: String): String {
		var result = response
		for (pattern in patterns) {
			result = result.replace(pattern, "")
		}
		return result
	}

	private fun unpackLongFrame(response: String): String {
		if (!response.contains(':')) return response
		var result = response.substring(response.indexOf(':') + 1)
		result = result.replace(Regex("[0-9]:"), "")
		return result
	}

	class FourByteBitSet(b0: Byte, b1: Byte, b2: Byte, b3: Byte) {
		companion object {
			private val masks = intArrayOf(
				0b00000001,
				0b00000010,
				0b00000100,
				0b00001000,
				0b00010000,
				0b00100000,
				0b01000000,
				0b10000000
			)
		}

		private val bytes = byteArrayOf(b0, b1, b2, b3)

		private fun getByte(index: Int): Byte {
			require(index in 0..3) { "$index is not a valid byte index" }
			return bytes[index]
		}

		private fun getBit(b: Byte, index: Int): Boolean {
			require(index in masks.indices) { "$index is not a valid bit index" }
			return b.toInt() and masks[index] != 0
		}

		fun getBit(b: Int, index: Int): Boolean {
			return getBit(getByte(b), index)
		}
	}

	fun getSupportedPIDs(): Set<Int> {
		val result = mutableSetOf<Int>()
		val pids = arrayOf("0100", "0120", "0140", "0160")
		var basePid = 1
		for (pid in pids) {
			val responseData = run(pid, 0x01)
			if (responseData.size >= 6) {
				val byte0 = responseData[2].toByte()
				val byte1 = responseData[3].toByte()
				val byte2 = responseData[4].toByte()
				val byte3 = responseData[5].toByte()
				log.debug(
					"Supported PID at base $basePid payload %02X%02X%02X%02X".format(
						byte0,
						byte1,
						byte2,
						byte3
					)
				)
				val bitSet = FourByteBitSet(byte0, byte1, byte2, byte3)
				for (byteIndex in 0..3) {
					for (bitIndex in 7 downTo 0) {
						if (bitSet.getBit(byteIndex, bitIndex)) {
							val command = basePid + 8 * byteIndex + 7 - bitIndex
							log.debug("Command $command found supported")
							result.add(command)
						}
					}
				}
			}
			basePid += 0x20
		}
		return result
	}

}

interface UnderlyingTransport {
	fun write(bytes: ByteArray)
	fun readByte(): Byte?
}