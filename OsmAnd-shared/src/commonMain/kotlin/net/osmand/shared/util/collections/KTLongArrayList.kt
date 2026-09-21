package net.osmand.shared.util.collections

import kotlin.jvm.JvmOverloads

/**
 * Growable `long` array, the [KTIntArrayList] of longs: a minimal replacement for
 * `gnu.trove.list.array.TLongArrayList`, storing values in a flat [LongArray] so nothing is boxed
 * and nothing is allocated per element.
 *
 * Not thread safe.
 */
class KTLongArrayList @JvmOverloads constructor(
	initialCapacity: Int = DEFAULT_CAPACITY
) {

	@PublishedApi
	internal var data: LongArray = LongArray(if (initialCapacity > 0) initialCapacity else DEFAULT_CAPACITY)

	var size: Int = 0
		private set

	fun isEmpty(): Boolean = size == 0

	fun isNotEmpty(): Boolean = size != 0

	/** Trove compatible alias of [size]. */
	fun size(): Int = size

	operator fun get(index: Int): Long {
		checkIndex(index)
		return data[index]
	}

	/** Unchecked read, callers must guarantee `0 <= index < size`. */
	fun getQuick(index: Int): Long = data[index]

	operator fun set(index: Int, value: Long) {
		checkIndex(index)
		data[index] = value
	}

	fun add(value: Long) {
		val current = data
		if (size < current.size) {
			current[size++] = value
		} else {
			grow(size + 1)
			data[size++] = value
		}
	}

	fun add(values: LongArray) {
		if (values.isEmpty()) {
			return
		}
		ensureCapacity(size + values.size)
		values.copyInto(data, size)
		size += values.size
	}

	fun indexOf(value: Long): Int {
		for (i in 0 until size) {
			if (data[i] == value) {
				return i
			}
		}
		return -1
	}

	operator fun contains(value: Long): Boolean = indexOf(value) >= 0

	/** Drops all elements, keeps the current capacity. */
	fun clear() {
		size = 0
	}

	fun toArray(): LongArray = data.copyOf(size)

	fun ensureCapacity(capacity: Int) {
		if (capacity > data.size) {
			grow(capacity)
		}
	}

	private fun grow(capacity: Int) {
		val current = data.size
		var newCapacity = if (current == 0) DEFAULT_CAPACITY else current shl 1
		if (newCapacity < capacity) {
			newCapacity = capacity
		}
		check(newCapacity > 0) { "Capacity overflow for $capacity elements" }
		data = data.copyOf(newCapacity)
	}

	inline fun forEach(action: (value: Long) -> Unit) {
		val values = data
		for (i in 0 until size) {
			action(values[i])
		}
	}

	private fun checkIndex(index: Int) {
		if (index < 0 || index >= size) {
			throw IndexOutOfBoundsException("Index $index out of bounds for size $size")
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is KTLongArrayList) return false
		if (size != other.size) return false
		for (i in 0 until size) {
			if (data[i] != other.data[i]) {
				return false
			}
		}
		return true
	}

	override fun hashCode(): Int {
		var result = 1
		for (i in 0 until size) {
			result = 31 * result + data[i].hashCode()
		}
		return result
	}

	override fun toString(): String {
		val builder = StringBuilder("[")
		for (i in 0 until size) {
			if (i > 0) {
				builder.append(", ")
			}
			builder.append(data[i])
		}
		return builder.append(']').toString()
	}

	companion object {
		const val DEFAULT_CAPACITY = 10
	}
}
