package net.osmand.shared.routing

import net.osmand.shared.data.KDataTileManager
import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KPriorityQueue
import net.osmand.shared.util.collections.KTIntObjectMap
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.jvm.JvmField

/**
 * The hub graph of the selected HH files loaded into memory, and the state of the search over it.
 *
 * The graph is loaded once per set of files ([initialized]) and kept across routes when the
 * config asks for it ([HHRoutingConfig.cacheCtx]); the vertices' `rt` fields, the queues and the
 * visited lists are one route's and cleared between routes ([clearVisited]). [boundaries] holds
 * the id of every vertex, so a detailed search on the roads stops where it reaches the hub graph.
 *
 * A copy of `HHRouteDataStructure.HHRoutingContext` in OsmAnd-java, which stays there for android
 * and tools; this copy is for iOS. Java's `T extends NetworkDBPoint` is [NetworkDBPoint] here (the
 * subclasses are the tools'), and its `boundaries` map of nulls is a set of ids.
 */
class HHRoutingContext {

	// Initial data structure
	@JvmField
	var rctx: RoutingContext? = null

	@JvmField
	var filterRoutingParameters: MutableMap<String, String> = LinkedHashMap()

	@JvmField
	var pointsById: KTLongObjectMap<NetworkDBPoint> = KTLongObjectMap()

	@JvmField
	var pointsByGeo: KTLongObjectMap<NetworkDBPoint> = KTLongObjectMap()

	@JvmField
	var clusterInPoints: KTIntObjectMap<MutableList<NetworkDBPoint>> = KTIntObjectMap()

	@JvmField
	var clusterOutPoints: KTIntObjectMap<MutableList<NetworkDBPoint>> = KTIntObjectMap()

	@JvmField
	var pointsRect: KDataTileManager<NetworkDBPoint> = KDataTileManager(11) // 20km tile

	@JvmField
	var boundaries: KTLongHashSet = KTLongHashSet()

	@JvmField
	var initialized: Boolean = false

	// Route specific details
	@JvmField
	var stats: RoutingStats = RoutingStats()

	@JvmField
	var config: HHRoutingConfig? = null

	@JvmField
	var startX: Int = 0

	@JvmField
	var startY: Int = 0

	@JvmField
	var endX: Int = 0

	@JvmField
	var endY: Int = 0

	// Route runtime vars
	@JvmField
	var queueAdded: MutableList<NetworkDBPoint> = ArrayList()

	@JvmField
	var visited: MutableList<NetworkDBPoint> = ArrayList()

	@JvmField
	var visitedRev: MutableList<NetworkDBPoint> = ArrayList()

	@JvmField
	var queue: KPriorityQueue<NetworkDBPointCost> = createQueue()

	@JvmField
	var queuePos: KPriorityQueue<NetworkDBPointCost> = createQueue()

	@JvmField
	var queueRev: KPriorityQueue<NetworkDBPointCost> = createQueue()

	/**
	 * The road segments the route actually starts and ends on, as chosen by the last-mile search
	 * (they can differ from the nearest ones after a reiteration). Kept because the alternatives
	 * of a short route are searched on the detailed graph - see HHAlternativeRoutes.
	 */
	@JvmField
	var startSegment: RouteSegmentPoint? = null

	@JvmField
	var endSegment: RouteSegmentPoint? = null

	private fun createQueue(): KPriorityQueue<NetworkDBPointCost> {
		return KPriorityQueue(11) { o1, o2 -> o1.cost.compareTo(o2.cost) }
	}

	fun requireContext(): RoutingContext = rctx ?: throw IllegalStateException("HH context has no routing context")

	fun requireConfig(): HHRoutingConfig = config ?: throw IllegalStateException("HH context has no config")

	fun clearAll(stPoints: KTLongObjectMap<NetworkDBPoint>?, endPoints: KTLongObjectMap<NetworkDBPoint>?) {
		clearVisited()
		stPoints?.forEachValue { p ->
			p.clearRouting()
		}
		endPoints?.forEachValue { p ->
			p.clearRouting()
		}
	}

	fun clearSegments() {
		pointsById.forEachValue { p ->
			p.markSegmentsNotLoaded()
		}
	}

	fun clearVisited() {
		queue(false).clear()
		queue(true).clear()
		for (p in queueAdded) {
			p.clearRouting()
		}
		queueAdded.clear()
		visited.clear()
		visitedRev.clear()
	}

	fun getIncomingPoints(point: NetworkDBPoint): MutableList<NetworkDBPoint> {
		return clusterInPoints[point.clusterId]
			?: throw IllegalStateException("No cluster ${point.clusterId} for $point")
	}

	fun getOutgoingPoints(point: NetworkDBPoint): MutableList<NetworkDBPoint> {
		val dual = point.dualPoint ?: throw IllegalStateException("No dual point for $point")
		return clusterOutPoints[dual.clusterId]
			?: throw IllegalStateException("No cluster ${dual.clusterId} for $dual")
	}

	fun clearVisited(stPoints: KTLongObjectMap<NetworkDBPoint>, endPoints: KTLongObjectMap<NetworkDBPoint>) {
		queue(false).clear()
		queue(true).clear()
		for (p in queueAdded) {
			val pos = p.rt(false).rtDetailedRoute
			val rev = p.rt(true).rtDetailedRoute
			p.clearRouting()
			if (pos != null && stPoints.containsKey(p.index.toLong())) {
				p.setDistanceToEnd(false, distanceToEnd(false, p))
				p.setDetailedParentRt(false, pos)
			}
			if (rev != null && endPoints.containsKey(p.index.toLong())) {
				p.setDistanceToEnd(true, distanceToEnd(true, p))
				p.setDetailedParentRt(true, rev)
			}
		}
		queueAdded.clear()
		visited.clear()
		visitedRev.clear()
	}

	fun unloadAllConnections() {
		pointsById.forEachValue { p ->
			p.markSegmentsNotLoaded()
		}
	}

	fun setStartEnd(start: KLatLon?, end: KLatLon?) {
		if (start != null) {
			startY = KMapUtils.get31TileNumberY(start.latitude)
			startX = KMapUtils.get31TileNumberX(start.longitude)
		}
		if (end != null) {
			endY = KMapUtils.get31TileNumberY(end.latitude)
			endX = KMapUtils.get31TileNumberX(end.longitude)
		}
	}

	fun queue(rev: Boolean): KPriorityQueue<NetworkDBPointCost> {
		return if (USE_GLOBAL_QUEUE) queue else (if (rev) queueRev else queuePos)
	}

	fun distanceToEnd(reverse: Boolean, nextPoint: NetworkDBPoint): Double {
		val config = requireConfig()
		if (config.HEURISTIC_COEFFICIENT > 0) {
			var distanceToEnd = nextPoint.rt(reverse).rtDistanceToEnd
			if (distanceToEnd == 0.0) {
				val dist = KMapUtils.squareRootDist31(
					if (reverse) startX else endX, if (reverse) startY else endY,
					nextPoint.midX(), nextPoint.midY()
				)
				distanceToEnd = config.HEURISTIC_COEFFICIENT * dist / requireContext().getRouter().getMaxSpeed()
				nextPoint.setDistanceToEnd(reverse, distanceToEnd)
			}
			return distanceToEnd
		}
		return 0.0
	}

	companion object {
		// faster when roads are in 1 global network but doesn't make sense for isolated islands
		@JvmField
		var USE_GLOBAL_QUEUE: Boolean = false
	}
}
