package net.osmand.shared.binary

import okio.FileHandle

/**
 * The coded input an obf reader is built on: a small buffer over a random access file with the
 * protobuf primitives, and the push/pop limit nested messages need.
 *
 * A copy of the part of protobuf's `CodedInputStream` that `BinaryMapIndexReader` in OsmAnd-java
 * uses, with the same method names, so the two readers can be compared line by line. The buffer
 * is 5 KB like the java one, so a seek costs both readers the same refill. There is one addition:
 * [seek], which OsmAnd's patched copy of the java class has too.
 */
class CodedInputStream(private val handle: FileHandle, bufferSize: Int = DEFAULT_BUFFER_SIZE) {

	private val buffer = ByteArray(bufferSize)
	private var bufferStart = 0L
	private var bufferLength = 0
	private var pos = 0
	private var limit = Long.MAX_VALUE

	/** Bytes fetched from the file since the last [resetBytesRead]; what a load costs in I/O. */
	var bytesRead = 0L
		private set

	fun resetBytesRead() {
		bytesRead = 0L
	}

	fun getTotalBytesRead(): Long = bufferStart + pos

	fun seek(offset: Long) {
		if (offset >= bufferStart && offset < bufferStart + bufferLength) {
			pos = (offset - bufferStart).toInt()
		} else {
			bufferStart = offset
			bufferLength = 0
			pos = 0
		}
	}

	private fun fill() {
		bufferStart += pos
		pos = 0
		bufferLength = handle.read(bufferStart, buffer, 0, buffer.size)
		if (bufferLength <= 0) {
			bufferLength = 0
			throw IllegalStateException("read past the end of the file at $bufferStart")
		}
		bytesRead += bufferLength
	}

	fun readRawByte(): Byte {
		if (pos >= bufferLength) {
			fill()
		}
		return buffer[pos++]
	}

	fun readRawVarint32(): Int {
		var tmp = readRawByte().toInt()
		if (tmp >= 0) {
			return tmp
		}
		var result = tmp and 0x7f
		tmp = readRawByte().toInt()
		if (tmp >= 0) {
			result = result or (tmp shl 7)
		} else {
			result = result or ((tmp and 0x7f) shl 7)
			tmp = readRawByte().toInt()
			if (tmp >= 0) {
				result = result or (tmp shl 14)
			} else {
				result = result or ((tmp and 0x7f) shl 14)
				tmp = readRawByte().toInt()
				if (tmp >= 0) {
					result = result or (tmp shl 21)
				} else {
					result = result or ((tmp and 0x7f) shl 21)
					tmp = readRawByte().toInt()
					result = result or (tmp shl 28)
					if (tmp < 0) {
						// Discard upper 32 bits.
						for (i in 0 until 5) {
							if (readRawByte() >= 0) {
								return result
							}
						}
						throw IllegalStateException("malformed varint at ${getTotalBytesRead()}")
					}
				}
			}
		}
		return result
	}

	fun readRawVarint64(): Long {
		var shift = 0
		var result = 0L
		while (shift < 64) {
			val b = readRawByte().toInt()
			result = result or ((b.toLong() and 0x7fL) shl shift)
			if (b and 0x80 == 0) {
				return result
			}
			shift += 7
		}
		throw IllegalStateException("malformed varint at ${getTotalBytesRead()}")
	}

	fun readInt32(): Int = readRawVarint32()

	fun readUInt32(): Int = readRawVarint32()

	fun readInt64(): Long = readRawVarint64()

	fun readUInt64(): Long = readRawVarint64()

	fun readSInt32(): Int {
		val n = readRawVarint32()
		return (n ushr 1) xor -(n and 1)
	}

	fun readSInt64(): Long {
		val n = readRawVarint64()
		return (n ushr 1) xor -(n and 1L)
	}

	fun readBool(): Boolean = readRawVarint32() != 0

	fun readString(): String {
		val size = readRawVarint32()
		if (size <= bufferLength - pos) {
			val value = buffer.decodeToString(pos, pos + size)
			pos += size
			return value
		}
		val bytes = ByteArray(size)
		for (i in 0 until size) {
			bytes[i] = readRawByte()
		}
		return bytes.decodeToString()
	}

	/** A `bytes` field: its varint length, then that many raw bytes. */
	fun readBytes(): ByteArray {
		val size = readRawVarint32()
		if (size <= bufferLength - pos) {
			val value = buffer.copyOfRange(pos, pos + size)
			pos += size
			return value
		}
		val bytes = ByteArray(size)
		for (i in 0 until size) {
			bytes[i] = readRawByte()
		}
		return bytes
	}

	/** The next tag, or 0 at the current limit, which is how the reader ends every message loop. */
	fun readTag(): Int {
		if (getBytesUntilLimit() <= 0) {
			return 0
		}
		return readRawVarint32()
	}

	/** The next [byteLimit] bytes are the message. Returns the outer limit, for [popLimit]. */
	fun pushLimitLong(byteLimit: Long): Long {
		val old = limit
		limit = getTotalBytesRead() + byteLimit
		return old
	}

	fun popLimit(oldLimit: Long) {
		limit = oldLimit
	}

	fun getBytesUntilLimit(): Long = limit - getTotalBytesRead()

	fun skipRawBytes(size: Long) {
		seek(getTotalBytesRead() + size)
	}

	/** Skips a field of a standard protobuf wire type; the reader handles OsmAnd's own type 6 itself. */
	fun skipField(tag: Int) {
		when (tag and 7) {
			WIRETYPE_VARINT -> readRawVarint64()
			WIRETYPE_FIXED64 -> skipRawBytes(8)
			WIRETYPE_LENGTH_DELIMITED -> skipRawBytes(readRawVarint32().toLong())
			WIRETYPE_FIXED32 -> skipRawBytes(4)
			else -> throw IllegalStateException("unsupported wire type in tag $tag at ${getTotalBytesRead()}")
		}
	}

	companion object {
		const val DEFAULT_BUFFER_SIZE = 5 * 1024

		const val WIRETYPE_VARINT = 0
		const val WIRETYPE_FIXED64 = 1
		const val WIRETYPE_LENGTH_DELIMITED = 2
		const val WIRETYPE_FIXED32 = 5

		/** OsmAnd's own wire type: a message behind a big endian length, see [BinaryMapIndexReader.readInt]. */
		const val WIRETYPE_FIXED32_LENGTH_DELIMITED = 6

		fun getTagFieldNumber(tag: Int): Int = tag ushr 3

		fun getTagWireType(tag: Int): Int = tag and 7
	}
}
