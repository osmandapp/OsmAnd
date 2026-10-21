package net.osmand.shared.binary

import net.osmand.shared.util.collections.KTIntArrayList
import okio.FileSystem
import okio.Path
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [CodedInputStream.readSInt32s] reads a field of varints straight out of the buffer while the
 * buffer holds enough bytes for any varint, and through [CodedInputStream.readRawVarint32] near its
 * end. Each buffer size below puts the end of the buffer at another place in the varints, so both
 * ways read every value, and they have to agree on the value, on where the stream stands afterwards
 * and on the error a malformed varint gives.
 */
class CodedInputStreamTest {

	private val file: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "coded-input-${Random.nextLong().toULong()}.bin"

	@AfterTest
	fun delete() {
		FileSystem.SYSTEM.delete(file, mustExist = false)
	}

	@Test
	fun varintsReadTheSameAtEveryBufferBoundary() {
		val random = Random(24092026)
		val values = ArrayList<Int>()
		values.addAll(listOf(0, 1, 127, 128, 16383, 16384, 2097151, 2097152, 268435455, 268435456,
			Int.MAX_VALUE, Int.MIN_VALUE, -1, -127, -128))
		repeat(3000) {
			val bits = 1 + random.nextInt(32)
			values.add(random.nextInt() ushr (32 - bits))
		}
		val bytes = ArrayList<Byte>()
		val ends = ArrayList<Long>()
		for (v in values) {
			encode(v, bytes)
			ends.add(bytes.size.toLong())
		}
		FileSystem.SYSTEM.write(file) { write(bytes.toByteArray()) }
		for (bufferSize in BUFFER_SIZES) {
			read(bufferSize) { input ->
				for (i in values.indices) {
					assertEquals(values[i], input.readRawVarint32(), "value $i, buffer $bufferSize")
					assertEquals(ends[i], input.getTotalBytesRead(), "position after value $i, buffer $bufferSize")
				}
			}
		}
	}

	@Test
	fun sint32sAreReadUpToTheLimitAtEveryBufferBoundary() {
		val random = Random(25092026)
		val values = ArrayList<Int>()
		values.addAll(listOf(0, 1, -1, 63, -64, 64, -65, 8191, -8192, 8192, Int.MAX_VALUE, Int.MIN_VALUE))
		repeat(3000) {
			val bits = 1 + random.nextInt(32)
			values.add(random.nextInt() shr (32 - bits))
		}
		val bytes = ArrayList<Byte>()
		for (v in values) {
			encodeUnsigned((v shl 1) xor (v shr 31), bytes)
		}
		val fieldLength = bytes.size.toLong()
		encodeUnsigned(12345, bytes)
		FileSystem.SYSTEM.write(file) { write(bytes.toByteArray()) }
		for (bufferSize in BUFFER_SIZES) {
			read(bufferSize) { input ->
				val old = input.pushLimitLong(fieldLength)
				val out = KTIntArrayList()
				out.add(42)
				input.readSInt32s(out)
				assertEquals(listOf(42) + values, out.toArray().toList(), "buffer $bufferSize")
				assertEquals(fieldLength, input.getTotalBytesRead(), "position at the limit, buffer $bufferSize")
				input.readSInt32s(out)
				assertEquals(values.size + 1, out.size, "nothing past the limit, buffer $bufferSize")
				input.popLimit(old)
				assertEquals(12345, input.readRawVarint32(), "the varint after the limit, buffer $bufferSize")
			}
		}
	}

	@Test
	fun malformedVarintFailsTheSameAtEveryBufferBoundary() {
		// a valid varint first, so that the buffer is already filled when the malformed one is read
		FileSystem.SYSTEM.write(file) { write(byteArrayOf(1) + ByteArray(12) { 0x80.toByte() }) }
		val messages = BUFFER_SIZES.map { bufferSize ->
			read(bufferSize) { input ->
				assertEquals(1, input.readRawVarint32())
				assertFailsWith<IllegalStateException> { input.readRawVarint32() }.message
			}
		}
		assertEquals(List(BUFFER_SIZES.size) { "malformed varint at 11" }, messages)
		val inField = BUFFER_SIZES.map { bufferSize ->
			read(bufferSize) { input ->
				input.pushLimitLong(13)
				assertFailsWith<IllegalStateException> { input.readSInt32s(KTIntArrayList()) }.message
			}
		}
		assertEquals(List(BUFFER_SIZES.size) { "malformed varint at 11" }, inField)
	}

	private fun <T> read(bufferSize: Int, block: (CodedInputStream) -> T): T {
		val handle = FileSystem.SYSTEM.openReadOnly(file)
		try {
			return block(CodedInputStream(handle, bufferSize))
		} finally {
			handle.close()
		}
	}

	/** As protobuf writes a uint32, and so a sint32 once zigzag encoded: at most 5 bytes. */
	private fun encodeUnsigned(value: Int, out: MutableList<Byte>) {
		var v = value
		while (v and 0x7f.inv() != 0) {
			out.add(((v and 0x7f) or 0x80).toByte())
			v = v ushr 7
		}
		out.add(v.toByte())
	}

	/** As protobuf writes an int32: a negative one sign extended to 64 bits, in 10 bytes. */
	private fun encode(value: Int, out: MutableList<Byte>) {
		var v = value.toLong()
		while (true) {
			if (v and 0x7fL.inv() == 0L) {
				out.add(v.toByte())
				return
			}
			out.add(((v and 0x7f) or 0x80).toByte())
			v = v ushr 7
		}
	}

	companion object {
		private val BUFFER_SIZES = (1..16).toList() + CodedInputStream.DEFAULT_BUFFER_SIZE
	}
}
