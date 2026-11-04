package net.osmand.shared.util.collections

/**
 * The read side of a map keyed by `long`: is a key there, and what does it hold.
 *
 * The A* planner takes its boundaries through this, so a search can be limited by a
 * [KTLongObjectMap], by a [KTLongHashSet] - a set of keys is a map whose every value is null,
 * which is how the HH planner marks the network points a detailed search must stop at - or by
 * an [ExcludeKTLongObjectMap] that hides a few keys of either. Java's `TLongObjectMap` interface
 * plays this part in OsmAnd-java.
 */
interface KTLongObjectLookup<out V : Any> {

	fun containsKey(key: Long): Boolean

	/** The value stored under [key], or null when it is absent or stored with no value. */
	operator fun get(key: Long): V?
}
