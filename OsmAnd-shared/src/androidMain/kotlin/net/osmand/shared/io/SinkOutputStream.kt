package net.osmand.shared.io

import okio.BufferedSink
import okio.IOException
import okio.Okio
import okio.Sink
import okio.buffer
import java.io.OutputStream

class SinkOutputStream(sink: Sink) : OutputStream() {
	private val bufferedSink: BufferedSink = sink.buffer()

	@Throws(IOException::class)
	override fun write(b: Int) {
		bufferedSink.writeByte(b)
	}

	@Throws(IOException::class)
	override fun write(b: ByteArray, off: Int, len: Int) {
		bufferedSink.write(b, off, len)
	}

	@Throws(IOException::class)
	override fun flush() {
		bufferedSink.flush()
	}

	@Throws(IOException::class)
	override fun close() {
		bufferedSink.close()
	}
}
