package net.osmand.plus.palette.utils

import java.util.concurrent.atomic.AtomicLong

/**
 * Helper class to map String keys to stable unique Long IDs.
 *
 * This is particularly useful for RecyclerView adapters with setHasStableIds(true).
 * It ensures that the same String key always corresponds to the same Long ID during the lifecycle of the mapper,
 * preventing UI glitches and enabling correct animations when items move or update.
 */
class IdMapper {

	private val idMap = HashMap<String, Long>()

	// Uses AtomicLong for a thread-safe counter mechanism.
	// Starts from 0 to provide safe unique IDs.
	private val nextId = AtomicLong(0)

	/**
	 * Returns a stable ID for the given string key.
	 *
	 * If the key has been seen before, the previously generated ID is returned.
	 * If the key is new, a new unique ID is generated, stored, and returned.
	 *
	 * @param key The unique string identifier of the item (e.g., file path or name).
	 * @return A unique Long ID associated with the key.
	 */
	fun getSafeId(key: String): Long {
		return idMap.getOrPut(key) {
			nextId.getAndIncrement()
		}
	}

	/**
	 * Clears the internal mapping and resets the counter.
	 *
	 * Use this method only when the data set is completely refreshed/invalidated
	 * and preserving the connection between old keys and IDs is no longer necessary
	 * (e.g., when the adapter is detached or the parent component is destroyed).
	 */
	fun clear() {
		idMap.clear()
		nextId.set(0)
	}
}