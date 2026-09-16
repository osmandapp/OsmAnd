package net.osmand.shared.routing

import kotlin.jvm.JvmField

/**
 * What the HH search came back with: the hub-graph edges the route follows, each resolved to
 * detailed road segments, and the alternatives if they were asked for.
 *
 * A copy of `HHRouteDataStructure.HHNetworkRouteRes` in OsmAnd-java, which stays there for
 * android and tools; this copy is for iOS.
 */
class HHNetworkRouteRes : RouteCalcResult {

	@JvmField
	var stats: RoutingStats? = null

	@JvmField
	var segments: MutableList<HHNetworkSegmentRes> = ArrayList()

	@JvmField
	var altRoutes: MutableList<HHNetworkRouteRes> = ArrayList()

	constructor() : super(ArrayList<RouteSegmentResult>())

	constructor(error: String) : super(error)

	fun getHHRoutingTime(): Double {
		var d = 0.0
		for (r in segments) {
			d += r.rtTimeHHSegments
		}
		return d
	}

	override fun getAlternatives(): List<List<RouteSegmentResult>> {
		// altRoutes is the storage - this is the same list seen through the generic result
		val alts = ArrayList<List<RouteSegmentResult>>(altRoutes.size)
		for (alt in altRoutes) {
			alts.add(alt.detailed)
		}
		return alts
	}

	fun getHHRoutingDetailed(): Double {
		var d = 0.0
		for (r in segments) {
			d += r.rtTimeDetailed
		}
		return d
	}

	fun append(res: HHNetworkRouteRes?) {
		if (res == null || res.error != null) {
			this.error = "Can't build a route with intermediate point"
		} else {
			detailed.addAll(res.detailed)
			segments.addAll(res.segments)
			altRoutes.clear() // not supported with intermediate points
		}
	}
}

/**
 * One hub-graph edge of a route with the detailed road segments it resolved to; [segment] is null
 * for the first and last entries, the detailed routes from the start and to the end.
 *
 * A copy of `HHRouteDataStructure.HHNetworkSegmentRes` in OsmAnd-java.
 */
class HHNetworkSegmentRes(@JvmField var segment: NetworkDBSegment?) {

	@JvmField
	var list: MutableList<RouteSegmentResult>? = null

	@JvmField
	var rtTimeDetailed: Double = 0.0

	@JvmField
	var rtTimeHHSegments: Double = 0.0
}
