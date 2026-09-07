package net.osmand.shared.util.collections

import kotlin.jvm.JvmOverloads

/**
 * Growable `int` array, a minimal replacement for `gnu.trove.list.array.TIntArrayList`.
 *
 * Stores values in a flat [IntArray], so unlike `MutableList<Int>` it neither boxes elements nor
 * allocates per element.
 *
 * When the final length is known, construct with that capacity: growth reallocates and copies, and
 * on the JVM that path is the only one where this list is measurably behind `TIntArrayList`.
 *
 * Trove compatibility notes:
 * - [get] is bounds checked, [getQuick] is not (same split as trove);
 * - [clear] keeps the current capacity, [reset] also zeroes the backing array.
 *
 * Not thread safe.
 */
class KTIntArrayList @JvmOverloads constructor(initialCapacity: Int = DEFAULT_CAPACITY) {

	@PublishedApi
	internal var data: IntArray = IntArray(if (initialCapacity > 0) initialCapacity else DEFAULT_CAPACITY)

	var size: Int = 0
		private set

	constructor(values: IntArray) : this(if (values.isEmpty()) DEFAULT_CAPACITY else values.size) {
		add(values)
	}

	fun isEmpty(): Boolean = size == 0

	fun isNotEmpty(): Boolean = size != 0

	fun capacity(): Int = data.size

	operator fun get(index: Int): Int {
		checkIndex(index)
		return data[index]
	}

	/** Unchecked read, callers must guarantee `0 <= index < size`. */
	fun getQuick(index: Int): Int = data[index]

	operator fun set(index: Int, value: Int) {
		checkIndex(index)
		data[index] = value
	}

	/** Unchecked write, callers must guarantee `0 <= index < size`. */
	fun setQuick(index: Int, value: Int) {
		data[index] = value
	}

	fun add(value: Int) {
		val current = data
		if (size < current.size) {
			current[size++] = value
		} else {
			// keep the growth branch out of the hot path so it stays trivially inlinable
			grow(size + 1)
			data[size++] = value
		}
	}

	fun add(values: IntArray) {
		add(values, 0, values.size)
	}

	fun add(values: IntArray, offset: Int, length: Int) {
		if (length == 0) {
			return
		}
		require(offset >= 0 && length >= 0 && offset + length <= values.size) {
			"Bad range $offset..${offset + length} for array of ${values.size}"
		}
		ensureCapacity(size + length)
		values.copyInto(data, size, offset, offset + length)
		size += length
	}

	fun addAll(other: KTIntArrayList) {
		add(other.data, 0, other.size)
	}

	fun insert(index: Int, value: Int) {
		require(index in 0..size) { "Index $index out of bounds for size $size" }
		if (index == size) {
			add(value)
			return
		}
		ensureCapacity(size + 1)
		data.copyInto(data, index + 1, index, size)
		data[index] = value
		size++
	}

	/** Removes and returns the value at [index]. */
	fun removeAt(index: Int): Int {
		checkIndex(index)
		val removed = data[index]
		if (index < size - 1) {
			data.copyInto(data, index, index + 1, size)
		}
		size--
		return removed
	}

	/** Removes the first occurrence of [value], returns true when something was removed. */
	fun removeValue(value: Int): Boolean {
		val index = indexOf(value)
		if (index < 0) {
			return false
		}
		removeAt(index)
		return true
	}

	fun indexOf(value: Int): Int {
		for (i in 0 until size) {
			if (data[i] == value) {
				return i
			}
		}
		return -1
	}

	operator fun contains(value: Int): Boolean = indexOf(value) >= 0

	/** Drops all elements, keeps the current capacity. */
	fun clear() {
		size = 0
	}

	/** Drops all elements and zeroes the backing array, like `TIntArrayList.reset()`. */
	fun reset() {
		data.fill(0, 0, size)
		size = 0
	}

	fun toArray(): IntArray = data.copyOf(size)

	fun toArray(offset: Int, length: Int): IntArray {
		require(offset >= 0 && length >= 0 && offset + length <= size) {
			"Bad range $offset..${offset + length} for size $size"
		}
		return data.copyOfRange(offset, offset + length)
	}

	/** Sorts the payload only, the unused tail of the backing array is left alone. */
	fun sort() {
		if (size > 1) {
			data.sort(0, size)
		}
	}

	fun reverse() {
		var i = 0
		var j = size - 1
		while (i < j) {
			val tmp = data[i]
			data[i] = data[j]
			data[j] = tmp
			i++
			j--
		}
	}

	fun ensureCapacity(capacity: Int) {
		if (capacity > data.size) {
			grow(capacity)
		}
	}

	private fun grow(capacity: Int) {
		val current = data.size
		// doubling, but never below what the caller asked for, so a bulk add grows once
		var newCapacity = if (current == 0) DEFAULT_CAPACITY else current shl 1
		if (newCapacity < capacity) {
			newCapacity = capacity
		}
		check(newCapacity > 0) { "Capacity overflow for $capacity elements" }
		data = data.copyOf(newCapacity)
	}

	fun trimToSize() {
		if (data.size > size) {
			data = data.copyOf(size)
		}
	}

	inline fun forEach(action: (value: Int) -> Unit) {
		val values = data
		for (i in 0 until size) {
			action(values[i])
		}
	}

	fun sum(): Long {
		var sum = 0L
		for (i in 0 until size) {
			sum += data[i]
		}
		return sum
	}

	private fun checkIndex(index: Int) {
		if (index < 0 || index >= size) {
			throw IndexOutOfBoundsException("Index $index out of bounds for size $size")
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is KTIntArrayList) return false
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
			result = 31 * result + data[i]
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
