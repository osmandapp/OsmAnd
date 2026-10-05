package net.osmand.shared.routing

/**
 * The outcome of the missing maps check, as far as [RouteCalculationProgress] is concerned.
 *
 * Working the outcome out needs the region index and a whole routing context, neither of which is
 * here yet, so the calculation stays in OsmAnd-java and only its answer crosses over. The progress
 * itself just carries the reference; the one thing it is asked for is the message below.
 */
interface MissingMapsResult {

	/** Why the route cannot be calculated, for the routing log and for the failed result. */
	fun getErrorMessage(): String
}
