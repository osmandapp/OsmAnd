package net.osmand.shared.db

class CollectionUtils {
	companion object {
		fun <T> containsAny(collection: Collection<T>, vararg objects: T): Boolean {
			for (`object` in objects) {
				if (collection.contains(`object`)) {
					return true
				}
			}
			return false
		}
	}
}