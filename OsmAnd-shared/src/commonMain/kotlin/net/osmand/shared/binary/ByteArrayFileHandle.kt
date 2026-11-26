package net.osmand.shared.binary

import okio.FileHandle

/**
 * A read-only [FileHandle] over bytes in memory, so that a [CodedInputStream] can read a message
 * that was read whole before, as the raw blocks of a name index are.
 */
internal class ByteArrayFileHandle(private val data: ByteArray) : FileHandle(false) {

	override fun protectedRead(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int): Int {
		if (fileOffset >= data.size) {
			return -1
		}
		val count = minOf(byteCount.toLong(), data.size - fileOffset).toInt()
		data.copyInto(array, arrayOffset, fileOffset.toInt(), fileOffset.toInt() + count)
		return count
	}

	override fun protectedWrite(fileOffset: Long, array: ByteArray, arrayOffset: Int, byteCount: Int) {
		throw UnsupportedOperationException()
	}

	override fun protectedFlush() {
		throw UnsupportedOperationException()
	}

	override fun protectedResize(size: Long) {
		throw UnsupportedOperationException()
	}

	override fun protectedSize(): Long = data.size.toLong()

	override fun protectedClose() {
	}
}
