package net.osmand.shared.xml

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import net.osmand.shared.api.OutputStreamAPI
import okio.Buffer
import okio.IOException
import okio.Sink
import platform.posix.uint8_tVar
import kotlin.math.min

class SinkOutputStream(private val sink: Sink) : OutputStreamAPI {
	private val BUFFER_SIZE = (8192)

	@Throws(IOException::class)
	@OptIn(ExperimentalForeignApi::class)
	override fun write(buffer: CPointer<uint8_tVar>?, maxLength: Int): Int {
		if (buffer == null) {
			throw IOException("Stream is null")
		}

		val size = min(BUFFER_SIZE, maxLength)
		val okioBuffer = Buffer()
		val data = buffer.readBytes(size)
		okioBuffer.write(data)

		return try {
			sink.write(okioBuffer, okioBuffer.size)
			sink.flush()
			size
		} catch (e: IOException) {
			-1
		}
	}

	@Throws(IOException::class)
	override fun flush() {
		sink.flush()
	}
}