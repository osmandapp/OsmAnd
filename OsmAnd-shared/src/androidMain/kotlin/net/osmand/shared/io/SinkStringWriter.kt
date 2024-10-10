package net.osmand.shared.io

import okio.IOException
import okio.Sink
import okio.buffer
import java.io.StringWriter

class SinkStringWriter(sink: Sink) : StringWriter() {

	private val bufferedSink = sink.buffer()

	@Throws(IOException::class)
	override fun write(c: Int) {
		bufferedSink.writeUtf8(c.toChar().toString())
	}

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
		bufferedSink.writeUtf8(str.substring(off, off + len))
	}

	@Throws(IOException::class)
	override fun flush() {
		bufferedSink.flush()
	}

	override fun toString(): String {
		return bufferedSink.toString()
	}
}
