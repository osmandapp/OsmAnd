package net.osmand.shared.util.collections

import kotlin.math.min

/**
 * The order in which trove's `TIntObjectHashMap` walks its keys, for the places where what java
 * builds depends on it. [KTIntObjectMap] walks its keys in an order of its own.
 *
 * This is the trove of `gnu-trove-osmand.jar`, which OsmAnd-java ships. Each key has a slot,
 * `(key * 31 & 0x7fffffff) % capacity`. When that slot is taken, the probe steps back by
 * `1 + hash % (capacity - 2)` until it finds a free one. A new map has 23 slots and grows to the
 * next capacity below once it holds more than half of them. On growth the old slots are
 * re-inserted from the last to the first, and the iterator walks the slots in the same direction.
 */
object KTroveHashOrder {

	/** The capacities a map made with the default constructor goes through, from trove's `PrimeFinder`. */
	private val CAPACITIES = intArrayOf(
		23, 47, 97, 197, 397, 797, 1597, 3203, 6421, 12853, 25717, 51437, 102877, 205759, 411527,
		823117, 1646237, 3292489, 6584983, 13169977, 26339969, 52679969, 105359939, 210719881,
		421439783, 842879579
	)

	/** The keys of a map that was given [keys] in this order, as its iterator hands them back. */
	fun intObjectMapKeys(keys: KTIntArrayList): IntArray {
		var step = 0
		var set = IntArray(CAPACITIES[step])
		var full = BooleanArray(CAPACITIES[step])
		var size = 0
		for (i in 0 until keys.size()) {
			val key = keys[i]
			val index = insertionIndex(set, full, key)
			if (index < 0) {
				continue
			}
			set[index] = key
			full[index] = true
			size++
			if (size > maxSize(set.size)) {
				val oldSet = set
				val oldFull = full
				step++
				set = IntArray(CAPACITIES[step])
				full = BooleanArray(CAPACITIES[step])
				for (j in oldSet.indices.reversed()) {
					if (oldFull[j]) {
						val moved = insertionIndex(set, full, oldSet[j])
						set[moved] = oldSet[j]
						full[moved] = true
					}
				}
			}
		}
		val result = IntArray(size)
		var n = 0
		for (j in set.indices.reversed()) {
			if (full[j]) {
				result[n++] = set[j]
			}
		}
		return result
	}

	private fun maxSize(capacity: Int): Int = min(capacity - 1, (capacity * 0.5f).toInt())

	/** The slot for [key], or `-slot - 1` when [key] is already there, as in `TIntHash.insertionIndex`. */
	private fun insertionIndex(set: IntArray, full: BooleanArray, key: Int): Int {
		val length = set.size
		val hash = (key * 31) and 0x7fffffff
		var index = hash % length
		if (!full[index]) {
			return index
		}
		if (set[index] == key) {
			return -index - 1
		}
		val probe = 1 + hash % (length - 2)
		do {
			index -= probe
			if (index < 0) {
				index += length
			}
		} while (full[index] && set[index] != key)
		return if (full[index]) -index - 1 else index
	}
}
