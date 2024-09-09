package net.osmand.shared.util

object KCollectionUtils {

	/********************* Collection Operations **************************/

	fun <T> asOneList(vararg collections: Collection<T>): List<T> {
		val result = mutableListOf<T>()
		collections.forEach { collection -> result.addAll(collection) }
		return result
	}

	fun <T> addToList(original: Collection<T>, element: T): List<T> {
		val copy = original.toMutableList()
		copy.add(element)
		return copy
	}

	fun <T> addAllToList(original: Collection<T>, elements: Collection<T>): List<T> {
		val copy = original.toMutableList()
		copy.addAll(elements)
		return copy
	}

	fun <T> setInList(original: Collection<T>, position: Int, element: T): List<T> {
		val copy = original.toMutableList()
		copy[position] = element
		return copy
	}

	fun <T> removeFromList(original: Collection<T>, element: T): List<T> {
		val copy = original.toMutableList()
		copy.remove(element)
		return copy
	}

	fun <T> removeAllFromList(original: Collection<T>, elements: Collection<T>): List<T> {
		val copy = original.toMutableList()
		copy.removeAll(elements)
		return copy
	}

	fun <T> addAllIfNotContains(collection: MutableCollection<T>, elements: Collection<T>) {
		elements.forEach { element -> addIfNotContains(collection, element) }
	}

	fun <T> addIfNotContains(element: T, vararg collections: MutableCollection<T>) {
		collections.forEach { collection -> addIfNotContains(collection, element) }
	}

	fun <T> addIfNotContains(collection: MutableCollection<T>, element: T) {
		if (!collection.contains(element)) {
			collection.add(element)
		}
	}

	fun <T> searchElementWithCondition(collection: Collection<T>, condition: (T) -> Boolean): T? {
		return collection.firstOrNull(condition)
	}

	fun <T> filterElementsWithCondition(collection: Collection<T>, condition: (T) -> Boolean): List<T> {
		return collection.filter(condition)
	}

	/********************* Inclusion Checks *******************************/

	fun <T> containsAny(collection: Collection<T>, vararg objects: T): Boolean {
		return objects.any { collection.contains(it) }
	}

	fun startsWithAny(s: String?, vararg args: String): Boolean {
		return !s.isNullOrEmpty() && args.any { s.startsWith(it) }
	}

	fun containsAny(s: String?, vararg args: String): Boolean {
		return !s.isNullOrEmpty() && args.any { s.contains(it) }
	}

	fun endsWithAny(s: String?, vararg args: String): Boolean {
		return !s.isNullOrEmpty() && args.any { s.endsWith(it) }
	}

	fun equalsToAny(o: Any?, vararg args: Any?): Boolean {
		return args.any { it == o }
	}

	fun anyIsNull(vararg args: Any?): Boolean {
		return args.any { it == null }
	}

	/********************* Array Manipulations ****************************/

	fun <T> reverseArray(array: Array<T>) {
		array.reverse()
	}

	fun containsInArrayL(array: LongArray, value: Long): Boolean {
		return array.contains(value)
	}

	fun addToArrayL(array: LongArray?, value: Long, skipIfExists: Boolean): LongArray {
		return when {
			array == null -> longArrayOf(value)
			skipIfExists && array.contains(value) -> array
			else -> (array + value).sortedArray()
		}
	}

	fun removeFromArrayL(array: LongArray?, value: Long): LongArray? {
		if (array == null) return null
		val index = array.indexOf(value)
		return if (index >= 0) {
			array.sliceArray(0 until index) + array.sliceArray(index + 1 until array.size)
		} else {
			array
		}
	}

	fun arrayToString(a: IntArray?): String? {
		return a?.joinToString(",")
	}

	fun stringToArray(array: String?): IntArray? {
		return array?.split(",")?.map { it.toInt() }?.toIntArray()
	}
}