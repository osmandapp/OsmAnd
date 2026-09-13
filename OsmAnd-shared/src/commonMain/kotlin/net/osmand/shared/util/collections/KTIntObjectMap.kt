package net.osmand.shared.util.collections

import kotlin.jvm.JvmOverloads

/**
 * Primitive `int` -> object map without boxing of keys.
 *
 * Minimal replacement for `gnu.trove.map.hash.TIntObjectHashMap`, covering the API actually used by
 * the routing code: [get], [put], [containsKey], [remove], [size], [clear], [keys], [values] and
 * iteration. The `long` keyed twin is [KTLongObjectMap] and the two behave identically.
 *
 * Implementation notes:
 * - open addressing with linear probing over power-of-two tables (cache friendly, no per-entry
 *   allocation, unlike a bucket-per-entry hash map);
 * - keys are mixed by Fibonacci hashing rather than the MurmurHash3 finalizer that [KTLongObjectMap]
 *   uses. Keys here are mostly small dense integers, encoding rule ids and string table indices,
 *   and a full mixer only adds work without spreading them any better;
 * - deletions leave tombstones, which are purged on the next rehash. Routing removes entries very
 *   rarely, so this keeps [remove] simple and iteration-safe.
 *
 * Values may not be null: a null result from [get] always means "no such key".
 * The map is not thread safe.
 */
class KTIntObjectMap<V : Any> @JvmOverloads constructor(
	initialCapacity: Int = DEFAULT_CAPACITY
) {

	@PublishedApi
	internal var keysArr: IntArray

	@PublishedApi
	internal var valuesArr: Array<Any?>

	/** Per slot state, one of [FREE], [FULL], [REMOVED]. */
	@PublishedApi
	internal var statesArr: ByteArray

	private var mask: Int

	/** How far to shift the hash product down to land in the table, see [hashIndex]. */
	private var shift: Int
	private var threshold: Int

	/** Number of slots that are not [FREE], i.e. live entries plus tombstones. */
	private var occupied: Int = 0

	/** Bumped on every structural change, used to detect modification during iteration. */
	@PublishedApi
	internal var modCount: Int = 0

	var size: Int = 0
		private set

	init {
		val capacity = tableSizeFor(initialCapacity)
		keysArr = IntArray(capacity)
		valuesArr = arrayOfNulls(capacity)
		statesArr = ByteArray(capacity)
		mask = capacity - 1
		shift = Int.SIZE_BITS - capacity.countTrailingZeroBits()
		threshold = capacity / 2
	}

	/**
	 * Trove compatible alias of [size].
	 *
	 * Kotlin callers read the [size] property; this exists so Java code migrating off
	 * `TIntObjectHashMap` keeps calling `size()` rather than the property accessor `getSize()`.
	 */
	fun size(): Int = size

	fun isEmpty(): Boolean = size == 0

	fun isNotEmpty(): Boolean = size != 0

	@Suppress("UNCHECKED_CAST")
	operator fun get(key: Int): V? {
		val index = indexOf(key)
		return if (index < 0) null else valuesArr[index] as V
	}

	fun containsKey(key: Int): Boolean = indexOf(key) >= 0

	/** Returns the value previously stored under [key], or null. */
	@Suppress("UNCHECKED_CAST")
	fun put(key: Int, value: V): V? {
		val index = insertionIndex(key)
		if (index < 0) {
			val existing = -index - 1
			val previous = valuesArr[existing]
			valuesArr[existing] = value
			return previous as V
		}
		insertAt(index, key, value)
		return null
	}

	/** Stores [value] only when [key] is absent. Returns the value that is in the map afterwards. */
	@Suppress("UNCHECKED_CAST")
	fun putIfAbsent(key: Int, value: V): V {
		val index = insertionIndex(key)
		if (index < 0) {
			return valuesArr[-index - 1] as V
		}
		insertAt(index, key, value)
		return value
	}

	/**
	 * Returns the value stored under [key], inserting the result of [defaultValue] when absent.
	 * Saves the second lookup of the `containsKey` / `put` / `get` sequence.
	 */
	@Suppress("UNCHECKED_CAST")
	fun getOrPut(key: Int, defaultValue: () -> V): V {
		val index = insertionIndex(key)
		if (index < 0) {
			return valuesArr[-index - 1] as V
		}
		val expected = modCount
		val value = defaultValue()
		if (modCount != expected) {
			// defaultValue() touched this map, the resolved slot is stale
			val slot = insertionIndex(key)
			if (slot < 0) {
				return valuesArr[-slot - 1] as V
			}
			insertAt(slot, key, value)
			return value
		}
		insertAt(index, key, value)
		return value
	}

	@Suppress("UNCHECKED_CAST")
	fun remove(key: Int): V? {
		val index = indexOf(key)
		if (index < 0) {
			return null
		}
		val previous = valuesArr[index]
		valuesArr[index] = null
		statesArr[index] = REMOVED
		size--
		modCount++
		return previous as V
	}

	fun clear() {
		if (occupied == 0) {
			return
		}
		statesArr.fill(FREE)
		valuesArr.fill(null)
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

	/** Keys of all live entries, in unspecified order. */
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
	@Suppress("UNCHECKED_CAST")
	fun values(): List<V> {
		val result = ArrayList<V>(size)
		for (i in statesArr.indices) {
			if (statesArr[i] == FULL) {
				result.add(valuesArr[i] as V)
			}
		}
		return result
	}

	/** Trove compatible alias of [values]. */
	fun valueCollection(): List<V> = values()

	fun iterator(): KTIntObjectIterator<V> = KTIntObjectIterator(this)

	/** Allocation free iteration over live entries. */
	@Suppress("UNCHECKED_CAST")
	inline fun forEach(action: (key: Int, value: V) -> Unit) {
		val expected = modCount
		val states = statesArr
		for (i in states.indices) {
			if (states[i] == FULL) {
				action(keysArr[i], valuesArr[i] as V)
				check(modCount == expected) { "Map was modified during iteration" }
			}
		}
	}

	/** Allocation free iteration over live values. */
	@Suppress("UNCHECKED_CAST")
	inline fun forEachValue(action: (value: V) -> Unit) {
		val expected = modCount
		val states = statesArr
		for (i in states.indices) {
			if (states[i] == FULL) {
				action(valuesArr[i] as V)
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

	private fun insertAt(index: Int, key: Int, value: V) {
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

	/**
	 * Fibonacci hashing: multiply by the 32-bit golden ratio and keep the high bits.
	 *
	 * One multiply and one shift, and consecutive keys land in distinct, well spread slots, which
	 * matters because most keys here are dense ids. A stronger mixer such as the MurmurHash3
	 * finalizer used by [KTLongObjectMap] costs more and buys nothing on this key shape.
	 */
	private fun hashIndex(key: Int): Int = (key * GOLDEN_RATIO) ushr shift

	private fun rehash(newCapacity: Int) {
		val oldKeys = keysArr
		val oldValues = valuesArr
		val oldStates = statesArr

		keysArr = IntArray(newCapacity)
		valuesArr = arrayOfNulls(newCapacity)
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

/**
 * Trove style iterator: `while (it.hasNext()) { it.advance(); it.key(); it.value() }`.
 * Fails fast when the map is structurally modified while iterating.
 */
class KTIntObjectIterator<V : Any> internal constructor(private val map: KTIntObjectMap<V>) {

	private val expectedModCount = map.modCount
	private var cursor = 0
	private var current = -1

	fun hasNext(): Boolean {
		val states = map.statesArr
		while (cursor < states.size && states[cursor] != KTIntObjectMap.FULL) {
			cursor++
		}
		return cursor < states.size
	}

	fun advance() {
		check(map.modCount == expectedModCount) { "Map was modified during iteration" }
		if (!hasNext()) {
			throw NoSuchElementException()
		}
		current = cursor
		cursor++
	}

	fun key(): Int {
		check(current >= 0) { "advance() must be called first" }
		return map.keysArr[current]
	}

	@Suppress("UNCHECKED_CAST")
	fun value(): V {
		check(current >= 0) { "advance() must be called first" }
		return map.valuesArr[current] as V
	}
}
