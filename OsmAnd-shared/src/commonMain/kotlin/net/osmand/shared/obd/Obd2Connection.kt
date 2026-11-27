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
			if (!runInitCommand(command)) {
				return false
			}
		}
		log.info("Adapter: ${runImpl(ADAPTER_VERSION_COMMAND)}")
		if (!selectProtocol()) {
			return false
		}
		log.info("Protocol: ${runImpl(PROTOCOL_NUMBER_COMMAND)}")
		logSupportedPids()
		return true
	}

	private suspend fun runInitCommand(command: String): Boolean {
		var responseString = runImpl(command)
		responseString = normalizeResponseString(command, responseString, null)
		val systemResponse = getSystemResponse(responseString)
		if (systemResponse == OBDResponse.STOPPED || systemResponse == OBDResponse.ERROR) {
			log.error("error while init obd $command: $systemResponse")
			return false
		}
		return true
	}

	/**
	 * "AT SP 0" only arms the auto search; it answers OK even if no protocol works.
	 * The protocol is known only after the car answers "41 00" to "0100", so check that and,
	 * if the auto search finds nothing, try the protocols one by one.
	 */
	private suspend fun selectProtocol(): Boolean {
		for (protocol in PROTOCOLS) {
			if (finished) {
				return false
			}
			if (!runInitCommand("$SET_PROTOCOL_COMMAND $protocol")) {
				return false
			}
			if (querySupportedPids(0) != null) {
				log.info("Protocol $protocol answers 0100")
				return true
			}
			if (protocol == PROTOCOLS[0]) {
				log.info("Auto search picked protocol ${runImpl(PROTOCOL_NUMBER_COMMAND)}")
			}
			log.info("No answer to 0100 with protocol $protocol")
		}
		log.error("No OBD protocol answers 0100, the car does not respond to this adapter")
		runInitCommand("$SET_PROTOCOL_COMMAND ${PROTOCOLS[0]}")
		return false
	}

	private suspend fun logSupportedPids() {
		var pid = 0
		while (pid < 0xE0 && !finished) {
			val bits = querySupportedPids(pid) ?: break
			log.info("Supported PIDs %02X: %02X%02X%02X%02X".format(pid, bits[0], bits[1], bits[2], bits[3]))
			if (bits[3] and 1 == 0) {
				break
			}
			pid += 0x20
		}
	}

	/** Bitmask of supported PIDs from "01xx" (xx = 00, 20, 40, ...), or null if there is no valid answer. */
	private suspend fun querySupportedPids(pid: Int): IntArray? {
		val fullCommand = "01%02X".format(pid)
		val response = normalizeResponseString(fullCommand, runImpl(fullCommand), COMMAND_TYPE.LIVE)
		val hexValues = try {
			toHexValues(response)
		} catch (e: IllegalArgumentException) {
			null
		}
		if (hexValues == null || hexValues.size < 6 ||
			hexValues[0] != COMMAND_TYPE.LIVE.code || hexValues[1] != pid) {
			log("No supported PIDs answer to $fullCommand: '$response'")
			return null
		}
		return hexValues.copyOfRange(2, 6)
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
			log("runImpl($command) returned ${escapeControlChars(responseRead)}")
			if(responseRead.contains("0:")) {
				responseRead = responseRead.substring(responseRead.indexOf("0:"))
			}
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
					return OBDResponse.ERROR
				}
				hexValues = hexValues.copyOfRange(2, hexValues.size)
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
		normalizedResponse = unpackLongFrame(normalizedResponse)
		if (normalizedResponse.startsWith(unspacedCommand))
			normalizedResponse = normalizedResponse.substring(unspacedCommand.length)
		commandType?.let {
			if (!normalizedResponse.startsWith(it.responseCodeText) && fullCommand != OBDCommand.OBD_ALT_BATTERY_VOLTAGE_COMMAND.textCommand) {
				val responseStart = normalizedResponse.indexOf(it.responseCodeText)
				if (responseStart != -1) {
					normalizedResponse = normalizedResponse.substring(responseStart)
				}
			}
		}
		normalizedResponse = removeSideData(normalizedResponse)
		return normalizedResponse
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
			arrayOf("ATZ", "AT E0", "AT L0", "AT S0", "AT H0")

		private const val SET_PROTOCOL_COMMAND = "AT SP"
		private const val ADAPTER_VERSION_COMMAND = "ATI"
		private const val PROTOCOL_NUMBER_COMMAND = "AT DPN"

		// auto search first, then CAN (most cars since 2008), KWP fast/slow init, ISO 9141, J1850
		private val PROTOCOLS = charArrayOf('0', '6', '8', '7', '9', '5', '4', '3', '1', '2')

		fun isInitCommand(command: String): Boolean {
			return initCommands.contains(command) || command.startsWith(SET_PROTOCOL_COMMAND)
					|| command == ADAPTER_VERSION_COMMAND || command == PROTOCOL_NUMBER_COMMAND
		}

		/** Makes \r, \n and other invisible bytes of an adapter answer visible in the log. */
		fun escapeControlChars(value: String): String {
			val sb = StringBuilder()
			for (c in value) {
				when {
					c == '\r' -> sb.append("\\r")
					c == '\n' -> sb.append("\\n")
					c < ' ' || c.code == 0x7F -> sb.append("\\x%02X".format(c.code))
					else -> sb.append(c)
				}
			}
			return sb.toString()
		}

		fun toHexValues(buffer: String): IntArray {
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