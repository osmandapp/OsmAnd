package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryHHRouteReaderAdapter
import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.HHRouteRegion
import net.osmand.shared.data.KDataTileManager
import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.collections.KPriorityQueue
import net.osmand.shared.util.collections.KTIntObjectMap
import net.osmand.shared.util.collections.KTLongHashSet
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * The hub graph of the selected HH files loaded into memory, and the state of the search over it.
 *
 * The graph is loaded once per set of files ([initialized]) and kept across routes when the
 * config asks for it ([HHRoutingConfig.cacheCtx]); the vertices' `rt` fields, the queues and the
 * visited lists are one route's and cleared between routes ([clearVisited]). [boundaries] holds
 * the id of every vertex, so a detailed search on the roads stops where it reaches the hub graph.
 * The graph comes from the [regions], one per HH section of the selected files.
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
	var regions: MutableList<HHRouteRegionPointsCtx> = ArrayList()

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

	/**
	 * Reads the vertices of every region, keyed by index. A vertex on the edge of a region is
	 * stored [NetworkDBPoint.incomplete] in the file that cuts it and complete in the one that
	 * holds its cluster; the complete one wins.
	 */
	fun loadNetworkPoints(): KTLongObjectMap<NetworkDBPoint> {
		val points = KTLongObjectMap<NetworkDBPoint>()
		for (r in regions) {
			val pnts = r.file.initHHPoints(r.fileRegion, r.id)
			pnts.forEach { key, pnt ->
				if (!pnt.incomplete || !points.containsKey(key)) {
					points.put(key, pnt)
				}
			}
		}
		return points
	}

	/** Java loads all edges at once only from the tools' database; a file loads them per vertex. */
	fun loadNetworkSegments(valueCollection: Collection<NetworkDBPoint>): Int {
		for (r in regions) {
			throw UnsupportedOperationException("Edges are loaded per vertex from ${r.file.getFile().name()}")
		}
		return 0
	}

	/** Java loads an edge's geometry only from the tools' database, so from files it is never there. */
	fun loadGeometry(segment: NetworkDBSegment, reload: Boolean): Boolean {
		if (segment.getGeometry().isNotEmpty() && !reload) {
			return true
		}
		return false
	}

	fun loadNetworkSegmentPoint(point: NetworkDBPoint, reverse: Boolean): Int {
		val mapId = point.mapId
		val r = regions[mapId.toInt()]
		return r.file.loadNetworkSegmentPoint(this, r, point, reverse)
	}

	fun getRoutingInfo(): String {
		val b = StringBuilder()
		for (r in regions) {
			if (b.isNotEmpty()) {
				b.append(", ")
			}
			b.append(r.file.getFile().name()).append(' ').append(r.fileRegion.profile)
				.append(" [").append(r.fileRegion.profileParams[r.routingProfile]).append(']')
		}
		return b.toString()
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

		/**
		 * The loaded graph of [src] for another routing context over the same files: the vertices
		 * are copied, with their edges unloaded and their routing state cleared, so two searches can
		 * run at once over one set of read points. Null when [src] is not loaded or [rctx] does not
		 * have its files open.
		 */
		@JvmStatic
		fun copy(src: HHRoutingContext, rctx: RoutingContext): HHRoutingContext? {
			if (!src.initialized) {
				return null
			}
			val c = HHRoutingContext()
			c.rctx = rctx
			for (r in src.regions) {
				var fileRegion: HHRouteRegion? = null
				var file: BinaryMapIndexReader? = null
				for (reader in rctx.map.keys) {
					for (h in reader.getHHRoutingIndexes()) {
						if (reader.getFile() == r.file.getFile() && h.getFilePointer() == r.fileRegion.getFilePointer()) {
							fileRegion = h
							file = reader
						}
					}
				}
				if (fileRegion == null || file == null) {
					return null
				}
				BinaryHHRouteReaderAdapter.copySegmentHeaders(r.fileRegion, fileRegion)
				c.regions.add(HHRouteRegionPointsCtx(r.id, fileRegion, file, r.routingProfile))
			}
			var maxIndex = 0
			src.pointsById.forEachValue { p ->
				maxIndex = maxOf(maxIndex, p.index)
			}
			// index is a dense global id
			val byIndex = arrayOfNulls<NetworkDBPoint>(maxIndex + 1)
			c.pointsById = KTLongObjectMap(src.pointsById.size)
			src.pointsById.forEach { key, s ->
				val p = NetworkDBPoint()
				p.tagValues = s.tagValues
				p.index = s.index
				p.clusterId = s.clusterId
				p.fileId = s.fileId
				p.mapId = s.mapId
				p.incomplete = s.incomplete
				p.roadId = s.roadId
				p.start = s.start
				p.end = s.end
				p.startX = s.startX
				p.startY = s.startY
				p.endX = s.endX
				p.endY = s.endY
				p.rtExclude = s.rtExclude
				p.markSegmentsNotLoaded()
				byIndex[p.index] = p
				c.pointsById.put(key, p)
			}
			src.pointsById.forEachValue { s ->
				val dual = s.dualPoint
				if (dual != null) {
					byIndex[s.index]!!.dualPoint = byIndex[dual.index]
				}
			}
			c.pointsByGeo = KTLongObjectMap(src.pointsByGeo.size)
			c.boundaries = KTLongHashSet(src.pointsByGeo.size)
			src.pointsByGeo.forEach { key, value ->
				c.pointsByGeo.put(key, byIndex[value.index]!!)
				// not copied from src.boundaries: it holds the start and end of a route running there
				c.boundaries.add(key)
			}
			c.clusterInPoints = copyClusters(src.clusterInPoints, byIndex)
			c.clusterOutPoints = copyClusters(src.clusterOutPoints, byIndex)
			c.pointsById.forEachValue { p ->
				val latlon = p.getPoint()
				c.pointsRect.registerObject(latlon.latitude, latlon.longitude, p)
			}
			for (i in src.regions.indices) {
				src.regions[i].pntsByFileId.forEach { key, value ->
					c.regions[i].pntsByFileId.put(key, byIndex[value.index]!!)
				}
			}
			c.filterRoutingParameters = LinkedHashMap(src.filterRoutingParameters)
			rctx.hhHasUnsupportedParameters = src.requireContext().hhHasUnsupportedParameters
			c.initialized = true
			return c
		}

		private fun copyClusters(
			src: KTIntObjectMap<MutableList<NetworkDBPoint>>, byIndex: Array<NetworkDBPoint?>
		): KTIntObjectMap<MutableList<NetworkDBPoint>> {
			val res = KTIntObjectMap<MutableList<NetworkDBPoint>>(src.size)
			src.forEach { key, value ->
				val l = ArrayList<NetworkDBPoint>(value.size)
				for (p in value) {
					l.add(byIndex[p.index]!!)
				}
				res.put(key, l)
			}
			return res
		}
	}
}
