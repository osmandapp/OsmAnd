package net.osmand.shared.util.collections

import kotlin.jvm.JvmOverloads

/**
 * Primitive `int` -> `long` map without boxing.
 *
 * Minimal replacement for `gnu.trove.map.hash.TIntLongHashMap`, covering the API the poi name index
 * uses: [get], [put], [putAll], [containsKey], [remove], [size] and [keys].
 *
 * Same open addressing scheme as [KTIntObjectMap]: power-of-two table, linear probing, Fibonacci
 * hashing, tombstones on removal. Keys here are file offsets of poi data blocks, dense enough for
 * the cheap mixer.
 *
 * A missing key reads as 0, the trove no-entry value: the name index stores file offsets, which are
 * never 0, and callers that care ask [containsKey] first.
 *
 * Not thread safe.
 */
class KTIntLongMap @JvmOverloads constructor(initialCapacity: Int = DEFAULT_CAPACITY) {

	@PublishedApi
	internal var keysArr: IntArray

	@PublishedApi
	internal var valuesArr: LongArray

	/** Per slot state, one of [FREE], [FULL], [REMOVED]. */
	@PublishedApi
	internal var statesArr: ByteArray

	private var mask: Int

	/** How far to shift the hash product down to land in the table, see [hashIndex]. */
	private var shift: Int
	private var threshold: Int

	/** Number of slots that are not [FREE], i.e. live entries plus tombstones. */
	private var occupied: Int = 0

	@PublishedApi
	internal var modCount: Int = 0

	var size: Int = 0
		private set

	init {
		val capacity = tableSizeFor(initialCapacity)
		keysArr = IntArray(capacity)
		valuesArr = LongArray(capacity)
		statesArr = ByteArray(capacity)
		mask = capacity - 1
		shift = Int.SIZE_BITS - capacity.countTrailingZeroBits()
		threshold = capacity / 2
	}

	/**
	 * Trove compatible alias of [size].
	 *
	 * Kotlin callers read the [size] property; this exists so Java code migrating off
	 * `TIntLongHashMap` keeps calling `size()` rather than the property accessor `getSize()`.
	 */
	fun size(): Int = size

	fun isEmpty(): Boolean = size == 0

	fun isNotEmpty(): Boolean = size != 0

	/** The value under [key], or 0 when there is none. */
	operator fun get(key: Int): Long {
		val index = indexOf(key)
		return if (index < 0) 0L else valuesArr[index]
	}

	fun containsKey(key: Int): Boolean = indexOf(key) >= 0

	/** Returns the value previously stored under [key], or 0. */
	fun put(key: Int, value: Long): Long {
		val index = insertionIndex(key)
		if (index < 0) {
			val existing = -index - 1
			val previous = valuesArr[existing]
			valuesArr[existing] = value
			return previous
		}
		insertAt(index, key, value)
		return 0L
	}

	fun putAll(other: KTIntLongMap) {
		ensureCapacity(size + other.size)
		other.forEach { key, value -> put(key, value) }
	}

	/** Returns the value that was under [key], or 0. */
	fun remove(key: Int): Long {
		val index = indexOf(key)
		if (index < 0) {
			return 0L
		}
		val previous = valuesArr[index]
		statesArr[index] = REMOVED
		size--
		modCount++
		return previous
	}

	fun clear() {
		if (occupied == 0) {
			return
		}
		statesArr.fill(FREE)
		size = 0
		occupied = 0
		modCount++
	}

	/** Grows the table so that [entries] entries fit without a rehash. */
	fun ensureCapacity(entries: Int) {
		if (entries > threshold) {
			rehash(tableSizeFor(entries))
		}
	}

	/**
	 * Keys of all live entries, in unspecified order.
	 *
	 * A snapshot, as in trove: removing entries while walking the returned array is safe.
	 */
	fun keys(): IntArray {
		val result = IntArray(size)
		var at = 0
		for (i in statesArr.indices) {
			if (statesArr[i] == FULL) {
				result[at++] = keysArr[i]
			}
		}
		return result
	}

	/** Values of all live entries, in unspecified order. */
	fun values(): LongArray {
		val result = LongArray(size)
		var at = 0
		for (i in statesArr.indices) {
			if (statesArr[i] == FULL) {
				result[at++] = valuesArr[i]
			}
		}
		return result
	}

	/** Allocation free iteration over live entries. */
	inline fun forEach(action: (key: Int, value: Long) -> Unit) {
		val expected = modCount
		val states = statesArr
		for (i in states.indices) {
			if (states[i] == FULL) {
				action(keysArr[i], valuesArr[i])
				check(modCount == expected) { "Map was modified during iteration" }
			}
		}
	}

	override fun toString(): String {
		val builder = StringBuilder("{")
		var first = true
		forEach { key, value ->
			if (!first) {
				builder.append(", ")
			}
			first = false
			builder.append(key).append('=').append(value)
		}
		return builder.append('}').toString()
	}

	private fun insertAt(index: Int, key: Int, value: Long) {
		val wasFree = statesArr[index] == FREE
		keysArr[index] = key
		valuesArr[index] = value
		statesArr[index] = FULL
		size++
		modCount++
		if (wasFree) {
			occupied++
			if (occupied > threshold) {
				rehash(tableSizeFor(size + 1))
			}
		}
	}

	/** Index of the slot holding [key], or -1. */
	private fun indexOf(key: Int): Int {
		var index = hashIndex(key)
		while (true) {
			val state = statesArr[index]
			if (state == FREE) {
				return -1
			}
			if (state == FULL && keysArr[index] == key) {
				return index
			}
			index = (index + 1) and mask
		}
	}

	/**
	 * Index of the slot to write [key] into, or `-existingIndex - 1` when the key is already there.
	 * Reuses the first tombstone of the probe chain, but only after proving the key is absent.
	 */
	private fun insertionIndex(key: Int): Int {
		var index = hashIndex(key)
		var firstRemoved = -1
		while (true) {
			val state = statesArr[index]
			if (state == FREE) {
				return if (firstRemoved >= 0) firstRemoved else index
			}
			if (state == FULL) {
				if (keysArr[index] == key) {
					return -index - 1
				}
			} else if (firstRemoved < 0) {
				firstRemoved = index
			}
			index = (index + 1) and mask
		}
	}

	/** Fibonacci hashing, as in [KTIntObjectMap]: multiply by the golden ratio and keep the high bits. */
	private fun hashIndex(key: Int): Int = (key * GOLDEN_RATIO) ushr shift

	private fun rehash(newCapacity: Int) {
		val oldKeys = keysArr
		val oldValues = valuesArr
		val oldStates = statesArr

		keysArr = IntArray(newCapacity)
		valuesArr = LongArray(newCapacity)
		statesArr = ByteArray(newCapacity)
		mask = newCapacity - 1
		shift = Int.SIZE_BITS - newCapacity.countTrailingZeroBits()
		threshold = newCapacity / 2
		occupied = 0
		modCount++

		for (i in oldStates.indices) {
			if (oldStates[i] == FULL) {
				val key = oldKeys[i]
				var index = hashIndex(key)
				while (statesArr[index] == FULL) {
					index = (index + 1) and mask
				}
				keysArr[index] = key
				valuesArr[index] = oldValues[i]
				statesArr[index] = FULL
				occupied++
			}
		}
	}

	companion object {
		const val DEFAULT_CAPACITY = 16

		@PublishedApi
		internal const val FREE: Byte = 0

		@PublishedApi
		internal const val FULL: Byte = 1

		@PublishedApi
		internal const val REMOVED: Byte = 2

		/** 2^32 / phi, rounded to an odd integer. */
		private const val GOLDEN_RATIO = -0x61c88647
	}
}
