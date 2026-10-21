package net.osmand.shared.util.collections

/**
 * A set in the order of [comparator], as java's `TreeSet`: elements the comparator finds equal are
 * one element, and the first one added stays.
 */
class KTreeSet<T>(private val comparator: Comparator<in T>) : AbstractMutableSet<T>() {

	private val elements = ArrayList<T>()

	override val size: Int
		get() = elements.size

	override fun add(element: T): Boolean {
		val i = elements.binarySearch(element, comparator)
		if (i >= 0) {
			return false
		}
		elements.add(-(i + 1), element)
		return true
	}

	override fun contains(element: T): Boolean = elements.binarySearch(element, comparator) >= 0

	override fun remove(element: T): Boolean {
		val i = elements.binarySearch(element, comparator)
		if (i < 0) {
			return false
		}
		elements.removeAt(i)
		return true
	}

	override fun clear() {
		elements.clear()
	}

	override fun iterator(): MutableIterator<T> = elements.iterator()
}
