package net.osmand.shared.obd

import net.osmand.shared.extensions.format
import net.osmand.shared.util.LoggerFactory

class Obd2Connection(
	private val connection: UnderlyingTransport,
	private val obdDispatcher: OBDDispatcher) {
	enum class COMMAND_TYPE(val code: Int) {
		LIVE(0x41), FREEZE(0x42), IDENTIFICATION(0x49);

		val responseCodeText: String = "%02X".format(code)
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
	private var initialized = false
	private var finished = false

	suspend fun initialize(): Boolean {
		if (!initialized) {
			initialized = runInitCommands()
		}
		return initialized
	}

	fun isFinished() = finished

	fun isInitialized() = initialized

	fun finish() {
		finished = true
	}

	private suspend fun runInitCommands(): Boolean {
		log.debug("runInitCommands")
		for (command in initCommands) {
			var responseString = runImpl(command)
			responseString = normalizeResponseString(command, responseString, null)
			val systemResponse = getSystemResponse(responseString)
			if (systemResponse == OBDResponse.STOPPED || systemResponse == OBDResponse.ERROR) {
				log.error("error while init obd $systemResponse")
				return false
			}
		}
		return true
	}

	private suspend fun runImpl(command: String): String {
		var response = StringBuilder()
		log.info("start write $command")
		connection.write((command + "\r").encodeToByteArray())
		log.info("end write $command")
		log.info("start read")
		while (!finished) {
			var responseRead = connection.read()
			when (responseRead) {
				UnderlyingTransport.TIMEOUT,
				UnderlyingTransport.CONTEXTINACTIVE,
				UnderlyingTransport.UNABLETOREAD -> {
					response = StringBuilder(responseRead)
					return response.toString()
				}
			}
			log("runImpl($command) returned $responseRead")
			responseRead = responseRead.replace("\r", "")
				.replace("\n", "")
				.replace(" ", "")
				.replace("\t", "")
				.replace(".", "")
			val endFlagPosition = responseRead.indexOf(">")
			if (endFlagPosition != -1) {
				responseRead = responseRead.substring(0, endFlagPosition)
			}
			response.append(responseRead)
			if (endFlagPosition != -1) {
				break
			}
		}
		log.info("end read")
		val responseValue = response.toString()
		return responseValue
	}

	suspend fun run(
		fullCommand: String,
		command: OBDCommand): OBDResponse {
		if (finished) {
			return OBDResponse.ERROR
		}
		val commandCode = command.command
		val commandType = command.commandType

		var responseString = runImpl(fullCommand)
		val originalResponseValue = responseString
		responseString = normalizeResponseString(fullCommand, responseString, commandType)
		val systemResponse = getSystemResponse(responseString)
		if (systemResponse != null) {
			return systemResponse
		}
		try {
			if (command.isHexAnswer) {
				var hexValues = toHexValues(responseString)
				if (hexValues.size < 3 ||
					hexValues[0] != commandType.code ||
					hexValues[1] != commandCode) {
					log("Incorrect answer data (size ${hexValues.size}) for $fullCommand")
				} else {
					hexValues = hexValues.copyOfRange(2, hexValues.size)
				}
				return OBDResponse(hexValues)
			} else {
				return OBDResponse(
					responseString
						.encodeToByteArray()
						.map { it.toInt() }
						.toIntArray())
			}
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
				finished = true
				log.error("connection failure")
				return OBDResponse.CONNECTION_FAILURE
			}

			UnderlyingTransport.CONTEXTINACTIVE -> {
				finished = true
				log.error("context inactive")
				return OBDResponse.ERROR
			}

			UnderlyingTransport.UNABLETOREAD -> {
				finished = true
				log.error("unable to read from stream")
				return OBDResponse.ERROR
			}

			UnderlyingTransport.TIMEOUT -> {
				log.error("reading timeout")
				return OBDResponse.ERROR
			}

			"CANERROR" -> {
				log.error("CAN bus error")
				return OBDResponse.ERROR
			}

			else -> null
		}
	}

	private fun normalizeResponseString(
		fullCommand: String,
		response: String,
		commandType: COMMAND_TYPE?): String {
		var normalizedResponse = response
		val unspacedCommand = fullCommand.replace(" ", "")
		if (normalizedResponse.startsWith(unspacedCommand))
			normalizedResponse = normalizedResponse.substring(unspacedCommand.length)
		commandType?.let {
			if (!normalizedResponse.startsWith(it.responseCodeText)) {
				val responseStart = normalizedResponse.indexOf(it.responseCodeText)
				if (responseStart != -1) {
					normalizedResponse = normalizedResponse.substring(responseStart)
				}
			}
		}
		normalizedResponse = unpackLongFrame(normalizedResponse)
		normalizedResponse = removeSideData(normalizedResponse)
		return normalizedResponse
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
			arrayOf("ATZ", "AT E0", "AT L0", "AT S0", "AT H0", "AT SP 0")

		fun isInitCommand(command: String): Boolean {
			return initCommands.contains(command)
		}
	}

}

interface UnderlyingTransport {
	companion object {
		val UNABLETOREAD = "UNABLETOREAD"
		val CONTEXTINACTIVE = "CONTEXTINACTIVE"
		val TIMEOUT = "TIMEOUT"
	}

	suspend fun write(bytes: ByteArray)
	suspend fun read(): String
}