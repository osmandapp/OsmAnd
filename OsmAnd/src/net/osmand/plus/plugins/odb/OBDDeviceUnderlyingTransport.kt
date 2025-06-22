package net.osmand.plus.plugins.odb

import net.osmand.shared.obd.UnderlyingTransport
import okio.Buffer
import okio.Sink
import okio.Source

private const val WAIT_OBD_RESPONSE_TIMEOUT = 20000

class OBDDeviceUnderlyingTransport(var inputStream: Source?, var outputStream: Sink?) :
	UnderlyingTransport {

	override fun write(bytes: ByteArray) {
		val buffer = Buffer().apply { write(bytes) }
		outputStream?.write(buffer, buffer.size)
	}

	fun readByte(): Byte? {
		val readBuffer = Buffer()
		return if (inputStream?.read(readBuffer, 1) == 1L) readBuffer.readByte() else null
	}

	override fun cleanupResources() {
		inputStream = null
		outputStream = null
	}

	override fun readResponse(): String {
		val response = StringBuilder()
		val readResponseStart = System.currentTimeMillis()
		while (System.currentTimeMillis() - readResponseStart < WAIT_OBD_RESPONSE_TIMEOUT) {
			val value = readByte() ?: continue
			val c = Char(value.toInt())
			// this is the prompt, stop here
			if (c == '>') break
			if (c == '\r' || c == '\n' || c == ' ' || c == '\t' || c == '.') continue
			response.append(c)
		}
		return response.toString()
	}
}