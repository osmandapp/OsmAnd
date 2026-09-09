package net.osmand.shared.routing

/**
 * How much of the road network a route calculation is allowed to see.
 *
 * BASE is the coarse network a basemap carries, NORMAL the detailed one, and COMPLEX means both:
 * a base route first, which then guides the detailed search as a [PrecalculatedRouteDirection].
 *
 * The C++ router in core-legacy reads this off the routing context and calls `ordinal()` on it, so
 * **the order of the constants is the contract** - `RouteCalculationMode { BASE, NORMAL, COMPLEX }`
 * in `native/src/routingContext.h` has to keep the same one.
 */
enum class RouteCalculationMode {
	BASE,
	NORMAL,
	COMPLEX
}
