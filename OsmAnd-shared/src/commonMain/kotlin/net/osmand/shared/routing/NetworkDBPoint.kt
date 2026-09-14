package net.osmand.shared.routing

import net.osmand.shared.binary.TagValuePair
import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.format
import net.osmand.shared.util.KMapUtils
import kotlin.jvm.JvmField

/**
 * A vertex of the HH hub graph: one directed road segment (a road, a point on it and the next
 * point) chosen by the graph builder as an entry or exit of a cluster of roads, with the edges
 * to the other vertices of its clusters loaded on demand from the obf file.
 *
 * The routing fields prefixed `rt` are the state of one search, one set per direction ([rt]),
 * cleared by [clearRouting] between routes. [index] is a dense global id; [fileId] the id inside
 * the file's HH section and [mapId] which of the selected files that is.
 *
 * A copy of `HHRouteDataStructure.NetworkDBPoint` in OsmAnd-java, which stays there for android
 * and tools; this copy is for iOS. Java's `NetworkDBPointCh` and `NetworkDBPointMid` subclasses
 * belong to the tools' contraction and midpoint experiments and are not copied, so [chInd] and
 * [midPntDepth] here always answer 0.
 */
open class NetworkDBPoint {

	@JvmField
	var tagValues: MutableList<TagValuePair>? = null

	@JvmField
	var dualPoint: NetworkDBPoint? = null

	@JvmField
	var index: Int = 0

	@JvmField
	var clusterId: Int = 0

	@JvmField
	var fileId: Int = 0

	@JvmField
	var mapId: Short = 0

	@JvmField
	var incomplete: Boolean = false

	@JvmField
	var roadId: Long = 0

	@JvmField
	var start: Short = 0

	@JvmField
	var end: Short = 0

	@JvmField
	var startX: Int = 0

	@JvmField
	var startY: Int = 0

	@JvmField
	var endX: Int = 0

	@JvmField
	var endY: Int = 0

	@JvmField
	internal var rtExclude: Boolean = false

	internal var rtRev: NetworkDBPointRouteInfo? = null

	internal var rtPos: NetworkDBPointRouteInfo? = null

	/** Edges out of this vertex, null while not loaded. */
	internal var connected: MutableList<NetworkDBSegment>? = ArrayList()

	/** Edges into this vertex, null while not loaded. */
	internal var connectedReverse: MutableList<NetworkDBSegment>? = ArrayList()

	fun midX(): Int = startX / 2 + endX / 2

	fun midY(): Int = startY / 2 + endY / 2

	fun rt(rev: Boolean): NetworkDBPointRouteInfo {
		if (rev) {
			var r = rtRev
			if (r == null) {
				r = NetworkDBPointRouteInfo()
				rtRev = r
			}
			return r
		} else {
			var r = rtPos
			if (r == null) {
				r = NetworkDBPointRouteInfo()
				rtPos = r
			}
			return r
		}
	}

	fun connected(rev: Boolean): MutableList<NetworkDBSegment>? = if (rev) connectedReverse else connected

	fun setDistanceToEnd(rev: Boolean, segmentDist: Double) {
		rt(rev).rtDistanceToEnd = segmentDist
	}

	fun markVisited(rev: Boolean) {
		rt(rev).rtVisited = true
	}

	fun connectedSet(rev: Boolean, l: MutableList<NetworkDBSegment>?) {
		if (rev) {
			connectedReverse = l
		} else {
			connected = l
		}
	}

	fun setCostParentRt(reverse: Boolean, cost: Double, point: NetworkDBPoint?, segmentDist: Double) {
		rt(reverse).setCostParentRt(reverse, cost, point, segmentDist)
	}

	fun setDetailedParentRt(reverse: Boolean, r: FinalRouteSegment) {
		rt(reverse).setDetailedParentRt(r)
	}

	fun markSegmentsNotLoaded() {
		connected = null
		connectedReverse = null
	}

	override fun toString(): String = "Point $index (${roadId / 64} $start-$end)"

	fun getPoint(): KLatLon {
		return KLatLon(
			KMapUtils.get31LatitudeY(this.startY / 2 + this.endY / 2),
			KMapUtils.get31LongitudeX(this.startX / 2 + this.endX / 2)
		)
	}

	fun getSegment(target: NetworkDBPoint, dir: Boolean): NetworkDBSegment? {
		val l = (if (dir) connected else connectedReverse) ?: return null
		for (s in l) {
			if (dir && s.end === target) {
				return s
			} else if (!dir && s.start === target) {
				return s
			}
		}
		return null
	}

	fun clearRouting() {
		rtExclude = false
		rtPos = null
		rtRev = null
	}

	open fun chInd(): Int = 0

	open fun midPntDepth(): Int = 0

	fun getGeoPntId(): Long = HHRouteDataStructure.calculateRoutePointInternalId(roadId, start.toInt(), end.toInt())
}

/**
 * An edge of the hub graph from [start] to [end] costing [dist] seconds; [shortcut] edges are the
 * tools' contraction shortcuts, never read from an obf file.
 *
 * A copy of `HHRouteDataStructure.NetworkDBSegment` in OsmAnd-java.
 */
class NetworkDBSegment(
	@JvmField val start: NetworkDBPoint,
	@JvmField val end: NetworkDBPoint,
	@JvmField var dist: Double,
	@JvmField val direction: Boolean,
	@JvmField val shortcut: Boolean
) {

	private var geom: MutableList<KLatLon>? = null

	fun getGeometry(): MutableList<KLatLon> {
		var g = geom
		if (g == null) {
			g = ArrayList()
			geom = g
		}
		return g
	}

	override fun toString(): String =
		"Segment %s -> %s [%.2f] %s".format(start, end, dist, if (shortcut) "sh" else "bs")
}

/**
 * What one direction of the hub-graph search knows about a vertex: how it was reached and at what
 * cost, or, for the vertices next to the start and the end, the detailed road route to them.
 *
 * A copy of `HHRouteDataStructure.NetworkDBPointRouteInfo` in OsmAnd-java.
 */
class NetworkDBPointRouteInfo {

	@JvmField
	var rtRouteToPoint: NetworkDBPoint? = null

	@JvmField
	var rtVisited: Boolean = false

	@JvmField
	var rtDistanceFromStart: Double = 0.0

	@JvmField
	var rtDepth: Int = -1 // possibly not needed (used 1)

	@JvmField
	var rtDistanceToEnd: Double = 0.0 // possibly not needed (used 1)

	@JvmField
	var rtCost: Double = 0.0

	@JvmField
	var rtDetailedRoute: FinalRouteSegment? = null

	fun getDepth(rev: Boolean): Int {
		if (rtDepth > 0) {
			return rtDepth
		}
		val to = rtRouteToPoint
		if (to != null) {
			rtDepth = to.rt(rev).getDepth(rev) + 1
			return rtDepth
		}
		return 0
	}

	fun setDetailedParentRt(r: FinalRouteSegment) {
		val segmentDist = r.getDistanceFromStart().toDouble()
		rtRouteToPoint = null
		rtCost = rtDistanceToEnd + segmentDist
		rtDetailedRoute = r
		rtDistanceFromStart = segmentDist
	}

	fun setCostParentRt(rev: Boolean, cost: Double, point: NetworkDBPoint?, segmentDist: Double) {
		rtCost = cost
		rtRouteToPoint = point
		rtDistanceFromStart = (point?.rt(rev)?.rtDistanceFromStart ?: 0.0) + segmentDist
	}
}

/**
 * An entry of the search queue: a vertex, the cost it is queued at and the direction it was queued by.
 *
 * A copy of `HHRouteDataStructure.NetworkDBPointCost` in OsmAnd-java.
 */
class NetworkDBPointCost(
	@JvmField val point: NetworkDBPoint,
	@JvmField val cost: Double,
	@JvmField val rev: Boolean
)
