package net.osmand.shared.binary

import net.osmand.shared.util.collections.KTIntArrayList
import okio.FileHandle

/**
 * The coded input an obf reader is built on: a small buffer over a random access file with the
 * protobuf primitives, and the push/pop limit nested messages need.
 *
 * A copy of the part of protobuf's `CodedInputStream` that `BinaryMapIndexReader` in OsmAnd-java
 * uses, with the same method names, so the two readers can be compared line by line. The buffer
 * is 5 KB like the java one, so a seek costs both readers the same refill. There are two additions:
 * [seek], which OsmAnd's patched copy of the java class has too, and [readSInt32s].
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
						malformedVarint()
					}
				}
			}
		}
		return result
	}

	/** The varint at [pos], which the caller has checked is whole in the buffer. */
	@Suppress("NOTHING_TO_INLINE")
	private inline fun readVarint32InBuffer(): Int {
		var p = pos
		val buf = buffer
		var tmp = buf[p++].toInt()
		if (tmp >= 0) {
			pos = p
			return tmp
		}
		var result = tmp and 0x7f
		tmp = buf[p++].toInt()
		if (tmp >= 0) {
			result = result or (tmp shl 7)
		} else {
			result = result or ((tmp and 0x7f) shl 7)
			tmp = buf[p++].toInt()
			if (tmp >= 0) {
				result = result or (tmp shl 14)
			} else {
				result = result or ((tmp and 0x7f) shl 14)
				tmp = buf[p++].toInt()
				if (tmp >= 0) {
					result = result or (tmp shl 21)
				} else {
					result = result or ((tmp and 0x7f) shl 21)
					tmp = buf[p++].toInt()
					result = result or (tmp shl 28)
					if (tmp < 0) {
						// Discard upper 32 bits.
						var discarded = 0
						while (buf[p++] < 0) {
							if (++discarded == 5) {
								pos = p
								malformedVarint()
							}
						}
					}
				}
			}
		}
		pos = p
		return result
	}

	/**
	 * Appends the zigzag varints up to the current limit to [out], one number each, without a call
	 * per number: this is how the loops that read coordinates take them.
	 */
	fun readSInt32s(out: KTIntArrayList) {
		val remaining = getBytesUntilLimit()
		if (remaining <= 0) {
			return
		}
		// a varint takes at least a byte, so the numbers up to the limit fit
		out.ensureCapacity(out.size + remaining.toInt())
		val data = out.data
		var size = out.size
		while (limit - bufferStart - pos > 0) {
			val n = if (bufferLength - pos >= MAX_VARINT_SIZE) readVarint32InBuffer() else readRawVarint32()
			data[size++] = (n ushr 1) xor -(n and 1)
		}
		out.size = size
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
		malformedVarint()
	}

	/** Out of line: building the message inside the readers above made every call of them slower. */
	private fun malformedVarint(): Nothing = throw IllegalStateException("malformed varint at ${getTotalBytesRead()}")

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

		/** The longest varint: a negative int is written sign extended to 64 bits, in 10 bytes. */
		private const val MAX_VARINT_SIZE = 10

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
