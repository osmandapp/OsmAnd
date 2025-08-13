package net.osmand.shared.io

import okio.BufferedSource
import okio.IOException
import okio.Source
import okio.buffer
import java.io.InputStream

class SourceInputStream(source: Source) : InputStream() {
	private val bufferedSource: BufferedSource = source.buffer()

	@Throws(IOException::class)
	override fun read(): Int {
		return if (bufferedSource.exhausted()) {
			-1 // End of stream
		} else {
			bufferedSource.readByte().toInt() and 0xff // Read a single byte
		}
	}

	@Throws(IOException::class)
	override fun read(b: ByteArray, off: Int, len: Int): Int {
		return if (bufferedSource.exhausted()) {
			-1 // End of stream
		} else {
			val bytesRead = bufferedSource.read(b, off, len)
			if (bytesRead != 0) bytesRead else -1
		}
	}

	@Throws(IOException::class)
	override fun close() = bufferedSource.close()
}
