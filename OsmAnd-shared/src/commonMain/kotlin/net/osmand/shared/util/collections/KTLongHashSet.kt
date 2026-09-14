package net.osmand.shared.util.collections

import kotlin.jvm.JvmOverloads

/**
 * Primitive `long` set without boxing, a minimal replacement for `gnu.trove.set.hash.TLongHashSet`.
 *
 * Same open addressing scheme as [KTLongObjectMap]: power-of-two table, linear probing,
 * MurmurHash3 finalizer for key mixing, tombstones on removal.
 *
 * Not thread safe.
 */
class KTLongHashSet @JvmOverloads constructor(initialCapacity: Int = DEFAULT_CAPACITY) {

	@PublishedApi
	internal var keysArr: LongArray

	@PublishedApi
	internal var statesArr: ByteArray

	private var mask: Int
	private var threshold: Int
	private var occupied: Int = 0

	@PublishedApi
	internal var modCount: Int = 0

	var size: Int = 0
		private set

	init {
		val capacity = tableSizeFor(initialCapacity)
		keysArr = LongArray(capacity)
		statesArr = ByteArray(capacity)
		mask = capacity - 1
		threshold = capacity / 2
	}

	/**
	 * Trove compatible alias of [size].
	 *
	 * Kotlin callers read the [size] property; this exists so Java code migrating off
	 * `TLongHashSet` keeps calling `size()` rather than the property accessor `getSize()`.
	 */
	fun size(): Int = size

	fun isEmpty(): Boolean = size == 0

	fun isNotEmpty(): Boolean = size != 0

	operator fun contains(key: Long): Boolean = indexOf(key) >= 0

	/** Returns true when [key] was not in the set yet. */
	fun add(key: Long): Boolean {
		val index = insertionIndex(key)
		if (index < 0) {
			return false
		}
		val wasFree = statesArr[index] == FREE
		keysArr[index] = key
		statesArr[index] = FULL
		size++
		modCount++
		if (wasFree) {
			occupied++
			if (occupied > threshold) {
				rehash(tableSizeFor(size + 1))
			}
		}
		return true
	}

	fun addAll(keys: LongArray): Boolean {
		ensureCapacity(size + keys.size)
		var changed = false
		for (key in keys) {
			changed = add(key) || changed
		}
		return changed
	}

	fun addAll(other: KTLongHashSet): Boolean {
		ensureCapacity(size + other.size)
		var changed = false
		other.forEach { key ->
			changed = add(key) || changed
		}
		return changed
	}

	/** Returns true when [key] was present. */
	fun remove(key: Long): Boolean {
		val index = indexOf(key)
		if (index < 0) {
			return false
		}
		statesArr[index] = REMOVED
		size--
		modCount++
		return true
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

	fun ensureCapacity(entries: Int) {
		if (entries > threshold) {
			rehash(tableSizeFor(entries))
		}
	}

	/** All keys, in unspecified order. */
	fun toArray(): LongArray {
		val result = LongArray(size)
		var at = 0
		for (i in statesArr.indices) {
			if (statesArr[i] == FULL) {
				result[at++] = keysArr[i]
			}
		}
		return result
	}

	fun iterator(): KTLongIterator = KTLongIterator(this)

	/** Allocation free iteration. */
	inline fun forEach(action: (key: Long) -> Unit) {
		val expected = modCount
		val states = statesArr
		for (i in states.indices) {
			if (states[i] == FULL) {
				action(keysArr[i])
				check(modCount == expected) { "Set was modified during iteration" }
			}
		}
	}

	override fun toString(): String {
		val builder = StringBuilder("{")
		var first = true
		forEach { key ->
			if (!first) {
				builder.append(", ")
			}
			first = false
			builder.append(key)
		}
		return builder.append('}').toString()
	}

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
		val oldStates = statesArr

		keysArr = LongArray(newCapacity)
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

/** Trove style iterator: `while (it.hasNext()) { val key = it.next() }`. */
class KTLongIterator internal constructor(private val set: KTLongHashSet) {

	private val expectedModCount = set.modCount
	private var cursor = 0

	fun hasNext(): Boolean {
		val states = set.statesArr
		while (cursor < states.size && states[cursor] != KTLongHashSet.FULL) {
			cursor++
		}
		return cursor < states.size
	}

	fun next(): Long {
		check(set.modCount == expectedModCount) { "Set was modified during iteration" }
		if (!hasNext()) {
			throw NoSuchElementException()
		}
		return set.keysArr[cursor++]
	}
}
