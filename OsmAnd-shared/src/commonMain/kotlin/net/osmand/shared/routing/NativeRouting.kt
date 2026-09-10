package net.osmand.shared.routing

/**
 * The C++ router, as the rest of the code is allowed to see it.
 *
 * There is one implementation of the routing itself - the C++ in OsmAnd-core-legacy - and two ways
 * of reaching it. Android and the jvm go through JNI, where `NativeLibrary` implements this and the
 * arguments cross the boundary as java objects. iOS links the same C++ and calls it directly, so
 * its implementation converts instead of marshalling. Everything in these signatures is a shared
 * type, which is what lets the caller stop caring which of the two it got.
 *
 * This is not the java planner. That one stays in `net.osmand.router` as the fallback for when
 * there is no native library at all - the server and the tools, and SAFE_MODE on android - and it
 * is not behind this interface because nothing outside java calls it.
 */
interface NativeRouting {

	/**
	 * Calculates a route and returns its segments, raw: the turns are worked out afterwards.
	 *
	 * [regions] are the route regions of the files to search, which the core matches back to its
	 * own by file pointer and length so it can attach them to the roads it returns; it opens the
	 * files itself. [basemap] asks for the coarse network a basemap carries. A null [hhConfig]
	 * means the plain A* rather than the hierarchical search.
	 *
	 * Returns null when the route could not be calculated.
	 */
	fun runNativeRouting(
		request: RoutingRequest,
		hhConfig: HHRoutingConfig?,
		regions: Array<RouteRegion>,
		basemap: Boolean
	): Array<RouteSegmentResult>?

	/**
	 * Whether any of the given points can only be reached across a road the profile treats as
	 * private, so the caller can offer to allow them for this route.
	 */
	fun needRequestPrivateAccessRouting(request: RoutingRequest, x31: IntArray, y31: IntArray): Boolean

	/**
	 * Releases the C++ side of a calculation. The handle is the one the core left in
	 * [RoutingRequest.nativeRoutingContext]; zero means there is nothing to release.
	 */
	fun releaseNativeRoutingContext(handle: Long)
}
