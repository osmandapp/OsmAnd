package net.osmand.shared.obd

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import net.osmand.shared.util.LoggerFactory
import kotlin.coroutines.CoroutineContext

class Obd2Connection(
	private val connection: UnderlyingTransport,
	private val obdDispatcher: OBDDispatcher,
	private val context: CoroutineContext) {
	enum class COMMAND_TYPE(val code: Int) {
		LIVE(0x41), FREEZE(0x42), IDENTIFICATION(0x49)
	}

	private val sideDataToRemoveFromAnswer = arrayOf(
		"SEARCHING",
		"ERROR",
		"BUS INIT",
		"BUSINIT",
		"BUS ERROR",
		"BUSERROR"
	)

	private val log = LoggerFactory.getLogger("Obd2Connection")
	private var finished = false

	init {
		runInitCommands()
	}

	fun isFinished() = finished

	fun finish() {
		finished = true
	}

	fun reInit() {
		runInitCommands()
	}

	private fun runInitCommands(): Boolean = runBlocking (context) {
		for (command in initCommands) {
			var responseString = runImpl(command)
			responseString = normalizeResponseString(command, responseString)
			val systemResponse = getSystemResponse(responseString)
			if (systemResponse == OBDResponse.STOPPED || systemResponse == OBDResponse.ERROR) {
				log.error("error while init obd $systemResponse")
				finished = true
//				connection.onInitFailed()
				return@runBlocking  false
			}
			delay(200)
		}
		return@runBlocking  true
	}

	private fun runImpl(command: String): String {
		val response = StringBuilder()
		connection.write((command + "\r").encodeToByteArray())
//		log("runImpl start reading...")
		while (!finished) {
			context.ensureActive()
			val value = connection.readByte() ?: continue
			val c = value.toChar()
//			log("runImpl($command) returning $c")
			// this is the prompt, stop here
			if (c == '>') break
			if (c == '\r' || c == '\n' || c == ' ' || c == '\t' || c == '.') continue
			response.append(c)
		}
		val responseValue = response.toString()
		log("runImpl($command) returned $responseValue")
		return responseValue
	}

	fun run(
		fullCommand: String,
		command: Int,
		commandType: COMMAND_TYPE = COMMAND_TYPE.LIVE): OBDResponse {
		if (finished) {
			return OBDResponse.ERROR
		}
		var responseString = runImpl(fullCommand)
		val originalResponseValue = responseString
		responseString = normalizeResponseString(fullCommand, responseString)
		val systemResponse = getSystemResponse(responseString)
		if (systemResponse != null) {
			return systemResponse
		}
		try {
			var hexValues = toHexValues(responseString)
			if (hexValues.size < 3 ||
				hexValues[0] != commandType.code ||
				hexValues[1] != command) {
				log("Incorrect answer data (size ${hexValues.size}) for $fullCommand")
			} else {
				hexValues = hexValues.copyOfRange(2, hexValues.size)
			}
			return OBDResponse(hexValues)
		} catch (e: IllegalArgumentException) {
			log(
				"Conversion error: command: '$fullCommand', original response: '$originalResponseValue', processed response: '$responseString'"
			)
			return OBDResponse.ERROR
		}
	}

	private fun getSystemResponse(responseString: String): OBDResponse? {
		return when (responseString) {
			"STOPPED" -> return OBDResponse.STOPPED
			"OK" -> return OBDResponse.OK
			"?" -> return OBDResponse.QUESTION_MARK
			"NODATA" -> return OBDResponse.NO_DATA
			"UNABLETOCONNECT" -> {
				log.error("connection failure")
				return OBDResponse.ERROR
			}

			"CANERROR" -> {
				log.error("CAN bus error")
				return OBDResponse.ERROR
			}

			else -> null
		}
	}

	private fun normalizeResponseString(fullCommand: String, response: String): String {
		var response1 = response
		val unspacedCommand = fullCommand.replace(" ", "")
		if (response1.startsWith(unspacedCommand))
			response1 = response1.substring(unspacedCommand.length)
		response1 = unpackLongFrame(response1)
		response1 = removeSideData(response1)
		return response1
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

	private fun removeSideData(response: String): String {
		var result = response
		for (pattern in sideDataToRemoveFromAnswer) {
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

	private fun log(msg: String) {
		if (obdDispatcher.debug) {
			log.debug(msg)
		} else {
			log.info(msg)
		}
	}

	companion object {
		private val initCommands =
			arrayOf("ATD", "ATZ", "AT E0", "AT L1", "AT S0", "AT H0", "AT AR", "AT SP 0")

		fun isInitCommand(command: String): Boolean {
			return initCommands.contains(command)
		}
	}

}

interface UnderlyingTransport {
	fun write(bytes: ByteArray)
	fun readByte(): Byte?
}