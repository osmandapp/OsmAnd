package net.osmand.shared.io

import okio.BufferedSink
import okio.IOException
import okio.Sink
import okio.buffer
import java.io.Writer

class SinkStringWriter(sink: Sink) : Writer() {

	private val bufferedSink: BufferedSink = sink.buffer()

	@Throws(IOException::class)
	override fun write(c: Int) {
		bufferedSink.writeUtf8CodePoint(c)
	}

	@Throws(IOException::class)
	override fun write(str: String?) {
		if (str != null) {
			bufferedSink.writeUtf8(str)
		}
	}

	@Throws(IOException::class)
	override fun write(cbuf: CharArray, off: Int, len: Int) {
		bufferedSink.writeUtf8(String(cbuf, off, len))
	}

	@Throws(IOException::class)
	override fun write(str: String, off: Int, len: Int) {
		bufferedSink.writeUtf8(str, off, off + len)
	}

	@Throws(IOException::class)
	override fun flush() {
		bufferedSink.flush()
	}

	@Throws(IOException::class)
	override fun close() {
		flush()
	}
}
