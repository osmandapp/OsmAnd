package net.osmand.shared.io

import okio.BufferedSink
import okio.IOException
import okio.Sink
import okio.buffer
import java.io.OutputStream

/**
 * [OutputStream] over an okio [Sink].
 *
 * With [closeSink] = false, [close] only flushes — for caller-owned sinks that must
 * stay open (e.g. AtomicFile streams closed by finishWrite()).
 */
class SinkOutputStream(sink: Sink, private val closeSink: Boolean = true) : OutputStream() {

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

	/**
	 * Closes the underlying sink, or — when [closeSink] is false — only flushes it.
	 *
	 * The flush-only mode deliberately deviates from the [OutputStream.close] contract:
	 * all buffered bytes are pushed through, but the sink stays open and the caller
	 * remains responsible for closing it.
	 */
	@Throws(IOException::class)
	override fun close() {
		if (closeSink) {
			bufferedSink.close()
		} else {
			flush()
		}
	}
}
