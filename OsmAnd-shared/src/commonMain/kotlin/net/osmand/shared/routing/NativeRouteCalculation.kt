package net.osmand.shared.routing

import net.osmand.shared.util.KMapUtils

/**
 * Asking the C++ router for a route, from code that does not know which platform it is on.
 *
 * Everything here works on a [RoutingRequest] and a [NativeRouting], so android, the jvm and iOS
 * take the same path into the same C++ and get the same segments back. What comes back is raw: the
 * turns are worked out afterwards, and that part is still two implementations, one per platform.
 *
 * The java planner is not reachable from here and does not need to be. It is the fallback for when
 * there is no native library at all, it lives in `net.osmand.router`, and only java calls it.
 */
object NativeRouteCalculation {

	/**
	 * Resets what the progress reports about distance and queues, and sets the estimate the
	 * progress bar is drawn from: straight line distance over nine tenths of the profile's top
	 * speed. Both routers start from this, so the java planner calls it too.
	 */
	fun refreshProgressDistance(request: RoutingRequest) {
		val progress = request.calculationProgress ?: return
		progress.distanceFromBegin = 0f
		progress.distanceFromEnd = 0f
		progress.reverseSegmentQueueSize = 0
		progress.directSegmentQueueSize = 0
		val rd = KMapUtils.squareRootDist31(request.startX, request.startY, request.targetX, request.targetY).toFloat()
		val speed = 0.9f * request.config.router.getMaxSpeed()
		progress.totalEstimatedDistance = rd / speed
	}

	/**
	 * Calculates a route and returns its segments in the order they are driven.
	 *
	 * [regions] are the route regions of the files to search: the core opens the files itself and
	 * matches its own regions back to these by file pointer and length, so it can attach them to
	 * the roads it returns. A null [hhConfig] asks for the plain A* rather than the hierarchical
	 * search.
	 *
	 * The list is the caller's to modify - a recalculated route has the piece already driven
	 * spliced back onto the front of it.
	 *
	 * @throws IllegalStateException when the request carries no native router. The caller decides
	 * whether to fall back, and only java has anything to fall back to.
	 */
	fun calculate(
		request: RoutingRequest,
		hhConfig: HHRoutingConfig?,
		regions: Array<RouteRegion>
	): MutableList<RouteSegmentResult> {
		val nativeLib = request.nativeLib
			?: throw IllegalStateException("There is no native router to calculate this route with")

		refreshProgressDistance(request)
		if (request.intermediatesX == null || request.intermediatesY == null) {
			request.intermediatesX = IntArray(0)
			request.intermediatesY = IntArray(0)
		}

		val basemap = request.calculationMode == RouteCalculationMode.BASE
		// the core answers with an empty array when it finds nothing; null would be a broken
		// implementation, and an empty route says the same thing to every caller here
		val segments = nativeLib.runNativeRouting(request, hhConfig, regions, basemap) ?: return mutableListOf()
		return segments.toMutableList()
	}
}
