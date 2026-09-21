package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import kotlin.jvm.JvmField

/**
 * One point of a track being attached to roads: where it is, how far along the track, the road
 * segment the planner found for it, and the roads that lead from it to the next attached point.
 *
 * A copy of `RoutePlannerFrontEnd.GpxPoint` in OsmAnd-java, which stays there; this copy is for
 * iOS. Java's `object`, the whole track drawn as one straight-line road, is [track] here, since
 * `object` is a Kotlin keyword.
 */
class GpxPoint() {

	@JvmField
	var ind: Int = 0

	lateinit var loc: KLatLon

	@JvmField
	var x31: Int = 0

	@JvmField
	var y31: Int = 0

	@JvmField
	var time: Long = 0

	@JvmField
	var cumDist: Double = 0.0

	@JvmField
	var pnt: RouteSegmentPoint? = null

	@JvmField
	var routeToTarget: MutableList<RouteSegmentResult>? = null

	@JvmField
	var stepBackRoute: MutableList<RouteSegmentResult>? = null

	@JvmField
	var targetInd: Int = -1

	@JvmField
	var straightLine: Boolean = false

	/** The track as one straight-line road, whose direction at a point is the track's bearing there. */
	@JvmField
	var track: RouteDataObject? = null

	constructor(point: GpxPoint) : this() {
		this.ind = point.ind
		this.loc = point.loc
		this.time = point.time
		this.track = point.track
		this.cumDist = point.cumDist
	}

	fun getFirstRouteRes(): RouteSegmentResult? {
		val route = routeToTarget
		if (route == null || route.isEmpty()) {
			return null
		}
		return route[0]
	}

	fun getLastRouteRes(): RouteSegmentResult? {
		val route = routeToTarget
		if (route == null || route.isEmpty()) {
			return null
		}
		return route[route.size - 1]
	}
}
