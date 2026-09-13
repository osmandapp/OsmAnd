package net.osmand.shared.routing

/**
 * How much of the road network a route calculation is allowed to see.
 *
 * BASE is the coarse network a basemap carries, NORMAL the detailed one, and COMPLEX means both:
 * a base route first, which then guides the detailed search as a [PrecalculatedRouteDirection].
 *
 * A copy of `RoutePlannerFrontEnd.RouteCalculationMode`, which stays in OsmAnd-java; this copy is
 * for iOS. The C++ router reads the java enum by `ordinal()`, so keep the constants in the same
 * order here, if only so a mode can be handed between the two by number.
 */
enum class RouteCalculationMode {
	BASE,
	NORMAL,
	COMPLEX
}
