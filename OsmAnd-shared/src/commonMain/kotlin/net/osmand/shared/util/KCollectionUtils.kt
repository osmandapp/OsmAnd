package net.osmand.shared.util

object KCollectionUtils {
	fun <T> addToList(original: Collection<T>, element: T): MutableList<T> {
		val copy: MutableList<T> = original.toMutableList()
		copy.add(element)
		return copy
	}

}