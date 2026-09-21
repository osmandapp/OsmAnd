package net.osmand.shared.gpx.primitives

import net.osmand.shared.util.KLock
import net.osmand.shared.util.synchronized

/**
 * Tag names of gpx extensions come from a small vocabulary, so one instance of every name is shared
 * by all the points instead of one per point.
 */
object GpxExtensionTagNames {

	private val lock = KLock()
	private var shared: Map<String, String> = emptyMap()

	fun intern(name: String): String {
		shared[name]?.let { return it }
		return synchronized(lock) {
			shared[name] ?: run {
				shared = HashMap(shared).also { it[name] = name }
				name
			}
		}
	}

	fun size(): Int = shared.size
}

/**
 * Mutable view over the extensions of one [GpxExtensions], which keeps them as a single array of
 * `[key, value, key, value …]` sorted by key and looked up with a binary search. A recorded point
 * carries 4-8 extensions and a `LinkedHashMap` spends about 40 bytes on every one of them plus its
 * own table; here a point pays one array and one reference per key and per value.
 *
 * The view itself holds no data, so it is created per call and collected right away.
 */
internal class GpxExtensionsMap(
	private val owner: GpxExtensions,
	private val deferred: Boolean
) : AbstractMutableMap<String, String>() {

	private var data: Array<String?>
		get() = (if (deferred) owner.deferredArray else owner.extensionsArray) ?: EMPTY
		set(value) {
			val array = if (value.isEmpty()) null else value
			if (deferred) owner.deferredArray = array else owner.extensionsArray = array
		}

	override val size: Int
		get() = data.size / 2

	override fun isEmpty(): Boolean = data.isEmpty()

	override fun containsKey(key: String): Boolean = indexOf(data, key) >= 0

	override fun get(key: String): String? {
		val array = data
		val index = indexOf(array, key)
		return if (index >= 0) array[index + 1] else null
	}

	override fun put(key: String, value: String): String? {
		val array = data
		val index = indexOf(array, key)
		if (index >= 0) {
			val previous = array[index + 1]
			array[index + 1] = value
			return previous
		}
		val insert = -(index + 1)
		val updated = arrayOfNulls<String>(array.size + 2)
		array.copyInto(updated, 0, 0, insert)
		updated[insert] = GpxExtensionTagNames.intern(key)
		updated[insert + 1] = value
		array.copyInto(updated, insert + 2, insert, array.size)
		data = updated
		return null
	}

	override fun remove(key: String): String? {
		val array = data
		val index = indexOf(array, key)
		if (index < 0) {
			return null
		}
		val previous = array[index + 1]
		val updated = arrayOfNulls<String>(array.size - 2)
		array.copyInto(updated, 0, 0, index)
		array.copyInto(updated, index, index + 2, array.size)
		data = updated
		return previous
	}

	override fun clear() {
		data = EMPTY
	}

	override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
		get() = EntrySet()

	private inner class EntrySet : AbstractMutableSet<MutableMap.MutableEntry<String, String>>() {

		override val size: Int
			get() = this@GpxExtensionsMap.size

		override fun add(element: MutableMap.MutableEntry<String, String>): Boolean =
			put(element.key, element.value) != element.value

		override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, String>> =
			object : MutableIterator<MutableMap.MutableEntry<String, String>> {

				private var index = 0

				override fun hasNext(): Boolean = index < data.size

				override fun next(): MutableMap.MutableEntry<String, String> {
					val array = data
					val entry = Entry(array[index]!!, array[index + 1]!!)
					index += 2
					return entry
				}

				override fun remove() {
					index -= 2
					this@GpxExtensionsMap.remove(data[index]!!)
				}
			}
	}

	private inner class Entry(
		override val key: String,
		override var value: String
	) : MutableMap.MutableEntry<String, String> {

		override fun setValue(newValue: String): String {
			val previous = value
			value = newValue
			put(key, newValue)
			return previous
		}

		override fun equals(other: Any?): Boolean =
			other is Map.Entry<*, *> && other.key == key && other.value == value

		override fun hashCode(): Int = key.hashCode() xor value.hashCode()
	}

	companion object {

		private val EMPTY = arrayOfNulls<String>(0)

		/** Index of the key inside the array, or `-(insertion point) - 1` when it is not there. */
		private fun indexOf(array: Array<String?>, key: String): Int {
			var low = 0
			var high = array.size / 2 - 1
			while (low <= high) {
				val mid = (low + high) ushr 1
				val compare = array[mid * 2]!!.compareTo(key)
				when {
					compare < 0 -> low = mid + 1
					compare > 0 -> high = mid - 1
					else -> return mid * 2
				}
			}
			return -(low * 2) - 1
		}

		fun toArray(values: Map<String, String>?): Array<String?>? {
			if (values.isNullOrEmpty()) {
				return null
			}
			val array = arrayOfNulls<String>(values.size * 2)
			var index = 0
			for (key in values.keys.sorted()) {
				array[index] = GpxExtensionTagNames.intern(key)
				array[index + 1] = values[key]
				index += 2
			}
			return array
		}
	}
}
