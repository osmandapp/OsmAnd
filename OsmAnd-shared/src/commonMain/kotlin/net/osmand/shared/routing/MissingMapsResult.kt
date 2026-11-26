package net.osmand.shared.routing

/**
 * The outcome of the missing maps check, as far as [RouteCalculationProgress] is concerned.
 *
 * The missing maps calculation (`MissingMapsCalculator` in OsmAnd-java) is not implemented in
 * shared. This interface lets a caller supply its result to [RouteCalculationProgress], which
 * carries the reference. Consumers can read the error message through this interface.
 */
interface MissingMapsResult {

	/** Why the route cannot be calculated, for the routing log and for the failed result. */
	fun getErrorMessage(): String
}
