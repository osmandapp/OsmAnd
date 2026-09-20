package net.osmand.shared.binary

/**
 * Where a search hands its results as it finds them, and how it learns it should stop.
 *
 * A copy of `net.osmand.ResultMatcher`, which stays in OsmAnd-java.
 */
interface ResultMatcher<T> {

	/** @return true if the [obj] is accepted, false otherwise. */
	fun publish(obj: T): Boolean

	/** @return true to stop the search. */
	fun isCancelled(): Boolean
}
