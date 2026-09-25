package net.osmand.shared.util.collections

/**
 * A view of a [KTLongObjectLookup] with a few keys hidden: [containsKey] and [get] answer as if
 * [keys] were not in the base. The HH planner limits a detailed search by every network point
 * except the two it runs between.
 *
 * A copy of `net.osmand.router.ExcludeTLongObjectMap` in OsmAnd-java, which stays there for
 * android and tools; this copy is for iOS. The java class implements the whole `TLongObjectMap`
 * and throws on everything but these two; here the lookup interface is all the planner asks for.
 */
class ExcludeKTLongObjectMap<V : Any>(
	private val map: KTLongObjectLookup<V>,
	private vararg val keys: Long
) : KTLongObjectLookup<V> {

	override fun containsKey(key: Long): Boolean {
		if (checkException(key)) {
			return false
		}
		return map.containsKey(key)
	}

	private fun checkException(key: Long): Boolean {
		for (k in keys) {
			if (key == k) {
				return true
			}
		}
		return false
	}

	override fun get(key: Long): V? {
		if (checkException(key)) {
			return null
		}
		return map[key]
	}
}
