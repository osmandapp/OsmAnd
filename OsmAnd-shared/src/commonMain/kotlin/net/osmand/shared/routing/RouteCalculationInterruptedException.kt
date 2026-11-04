package net.osmand.shared.routing

/**
 * Thrown out of the search when [RouteCalculationProgress.isCancelled] was set while it ran.
 *
 * Java throws `InterruptedException` there, which common Kotlin does not have; this is the one
 * exception the planner throws on purpose, so a caller that cancelled knows the result is not a
 * failure.
 */
class RouteCalculationInterruptedException(message: String) : RuntimeException(message)
