package net.osmand.shared.util.collections

import kotlin.jvm.JvmOverloads

/**
 * Primitive `long` -> object map without boxing of keys.
 *
 * Minimal replacement for `gnu.trove.map.hash.TLongObjectHashMap`, covering the API actually used by
 * the routing code: [get], [put], [containsKey], [remove], [size], [clear], [keys], [values] and
 * iteration.
 *
 * Implementation notes:
 * - open addressing with linear probing over power-of-two tables (cache friendly, no per-entry
 *   allocation, unlike a bucket-per-entry hash map);
 * - keys are mixed with the MurmurHash3 64-bit finalizer, because raw OsmAnd keys (road ids,
 *   `x31 << 31 | y31` tile keys) are highly structured and would cluster badly under linear probing;
 * - deletions leave tombstones, which are purged on the next rehash. Routing removes entries very
 *   rarely, so this keeps [remove] simple and iteration-safe.
 *
 * Values may not be null: a null result from [get] always means "no such key".
 * The map is not thread safe.
 */
class KTLongObjectMap<V : Any> @JvmOverloads constructor(
	initialCapacity: Int = DEFAULT_CAPACITY
) {

	@PublishedApi
	internal var keysArr: LongArray

	@PublishedApi
	internal var valuesArr: Array<Any?>

	/** Per slot state, one of [FREE], [FULL], [REMOVED]. */
	@PublishedApi
	internal var statesArr: ByteArray

	private var mask: Int
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
		keysArr = LongArray(capacity)
		valuesArr = arrayOfNulls(capacity)
		statesArr = ByteArray(capacity)
		mask = capacity - 1
		threshold = capacity / 2
	}

	fun isEmpty(): Boolean = size == 0

	fun isNotEmpty(): Boolean = size != 0

	@Suppress("UNCHECKED_CAST")
	operator fun get(key: Long): V? {
		val index = indexOf(key)
		return if (index < 0) null else valuesArr[index] as V
	}

	fun containsKey(key: Long): Boolean = indexOf(key) >= 0

	/** Returns the value previously stored under [key], or null. */
	@Suppress("UNCHECKED_CAST")
	fun put(key: Long, value: V): V? {
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
	fun putIfAbsent(key: Long, value: V): V {
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
	fun getOrPut(key: Long, defaultValue: () -> V): V {
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
	fun remove(key: Long): V? {
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
	fun keys(): LongArray {
		val result = LongArray(size)
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

	fun iterator(): KTLongObjectIterator<V> = KTLongObjectIterator(this)

	/** Allocation free iteration over live entries. */
	@Suppress("UNCHECKED_CAST")
	inline fun forEach(action: (key: Long, value: V) -> Unit) {
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

	private fun insertAt(index: Int, key: Long, value: V) {
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
	private fun indexOf(key: Long): Int {
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
	private fun insertionIndex(key: Long): Int {
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

	private fun hashIndex(key: Long): Int {
		var h = key
		h = h xor (h ushr 33)
		h *= MURMUR_C1
		h = h xor (h ushr 33)
		h *= MURMUR_C2
		h = h xor (h ushr 33)
		return h.toInt() and mask
	}

	private fun rehash(newCapacity: Int) {
		val oldKeys = keysArr
		val oldValues = valuesArr
		val oldStates = statesArr

		keysArr = LongArray(newCapacity)
		valuesArr = arrayOfNulls(newCapacity)
		statesArr = ByteArray(newCapacity)
		mask = newCapacity - 1
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

		private const val MURMUR_C1 = -0xae502812aa7333L
		private const val MURMUR_C2 = -0x3b314601e57a13adL
	}
}

/**
 * Trove style iterator: `while (it.hasNext()) { it.advance(); it.key(); it.value() }`.
 * Fails fast when the map is structurally modified while iterating.
 */
class KTLongObjectIterator<V : Any> internal constructor(private val map: KTLongObjectMap<V>) {

	private val expectedModCount = map.modCount
	private var cursor = 0
	private var current = -1

	fun hasNext(): Boolean {
		val states = map.statesArr
		while (cursor < states.size && states[cursor] != KTLongObjectMap.FULL) {
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

	fun key(): Long {
		check(current >= 0) { "advance() must be called first" }
		return map.keysArr[current]
	}

	@Suppress("UNCHECKED_CAST")
	fun value(): V {
		check(current >= 0) { "advance() must be called first" }
		return map.valuesArr[current] as V
	}
}

/** Smallest power of two table that keeps [entries] entries at or below a 0.5 load factor. */
internal fun tableSizeFor(entries: Int): Int {
	var capacity = 8
	val needed = if (entries < 1) 1 else entries
	while (capacity / 2 < needed) {
		capacity = capacity shl 1
		check(capacity > 0) { "Capacity overflow for $entries entries" }
	}
	return capacity
}
