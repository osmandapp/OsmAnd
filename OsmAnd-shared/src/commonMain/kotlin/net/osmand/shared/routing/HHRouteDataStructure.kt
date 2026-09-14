package net.osmand.shared.routing

import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KTIntObjectMap
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * The pieces of the HH data model that are not a class of their own: the id a directed road
 * segment is known by throughout the hub graph, and the decoding of a vertex's edges from the
 * bytes the obf file stores them as.
 *
 * A copy of the statics of `HHRouteDataStructure` and `HHRoutePlanner` in OsmAnd-java, which stay
 * there for android and tools; this copy is for iOS. The id helpers sit on the planner in java;
 * they are here because a point computes its own id ([NetworkDBPoint.getGeoPntId]).
 */
object HHRouteDataStructure {

	private val LOG = LoggerFactory.getLogger("HHRouteDataStructure")

	const val ROUTE_POINTS = 11

	/**
	 * The id of the directed segment of [road] from [pntId] to [nextPntId], which must be
	 * neighbours; java's checks, including that the road has at most 2^11 points, are kept.
	 */
	@JvmStatic
	fun calculateRoutePointInternalId(road: RouteDataObject, pntId: Int, nextPntId: Int): Long {
		val positive = nextPntId - pntId
		val pntLen = road.getPointsLength()
		if (positive < 0) {
			throw IllegalStateException("Check only positive segments are in calculation")
		}
		if (pntId < 0 || nextPntId < 0 || pntId >= pntLen || nextPntId >= pntLen || (positive != -1 && positive != 1) ||
			pntLen > (1 shl ROUTE_POINTS)
		) {
			// should be assert
			throw IllegalStateException("Assert failed")
		}
		return (road.getId() shl ROUTE_POINTS) + (pntId shl 1) + (if (positive > 0) 1 else 0)
	}

	@JvmStatic
	fun calculateRoutePointInternalId(id: Long, pntId: Int, nextPntId: Int): Long {
		val positive = nextPntId - pntId
		return (id shl ROUTE_POINTS) + (pntId shl 1) + (if (positive > 0) 1 else 0)
	}

	@JvmStatic
	fun calcRPId(p: RouteSegment, pntId: Int, nextPntId: Int): Long {
		return calculateRoutePointInternalId(p.getRoad().getId(), pntId, nextPntId)
	}

	@JvmStatic
	fun calcUniDirRoutePointInternalId(segm: RouteSegment): Long {
		return if (segm.getSegmentStart() < segm.getSegmentEnd()) {
			calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentStart().toInt(), segm.getSegmentEnd().toInt())
		} else {
			calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentEnd().toInt(), segm.getSegmentStart().toInt())
		}
	}

	/**
	 * The vertices of every cluster, by cluster id, each list ordered by [NetworkDBPoint.index]:
	 * with [out] the clusters the points lead out of, otherwise the ones their dual points lead
	 * into. The order is what the edge bytes of a vertex are decoded against.
	 */
	@JvmStatic
	fun groupByClusters(pointsById: KTLongObjectMap<NetworkDBPoint>, out: Boolean): KTIntObjectMap<MutableList<NetworkDBPoint>> {
		val res = KTIntObjectMap<MutableList<NetworkDBPoint>>()
		pointsById.forEachValue { p ->
			val cid = if (out) p.clusterId else (p.dualPoint ?: throw IllegalStateException("No dual point for $p")).clusterId
			res.getOrPut(cid) { ArrayList() }.add(p)
		}
		res.forEachValue { l ->
			l.sortWith { o1, o2 -> o1.index.compareTo(o2.index) }
		}
		return res
	}

	/**
	 * Gives [point] its edges from the two byte arrays the file stores per vertex: [inBytes] holds
	 * one varint per point of the vertex's own cluster, [outBytes] one per point of its dual
	 * point's cluster, each the cost in tenths of a second or zero for no edge.
	 */
	@JvmStatic
	fun setSegments(ctx: HHRoutingContext, point: NetworkDBPoint, inBytes: ByteArray?, outBytes: ByteArray?) {
		point.connectedSet(true, parseSegments(inBytes, ctx.getIncomingPoints(point), point, false))
		point.connectedSet(false, parseSegments(outBytes, ctx.getOutgoingPoints(point), point, true))
	}

	internal fun parseSegments(
		bytes: ByteArray?, lst: List<NetworkDBPoint>, pnt: NetworkDBPoint, out: Boolean
	): MutableList<NetworkDBSegment> {
		val l = ArrayList<NetworkDBSegment>()
		if (bytes == null || bytes.isEmpty() || pnt.incomplete) {
			return l
		}
		val str = VarintReader(bytes)
		for (i in lst.indices) {
			val d = str.readRawVarint32()
			if (d <= 0) {
				continue
			}
			val dist = d / 10.0
			val start = if (out) pnt else lst[i]
			val end = if (out) lst[i] else pnt
			val seg = NetworkDBSegment(start, end, dist, out, false)
			l.add(seg)
		}
		if (str.available() > 0) {
			LOG.error("Error reading file: $pnt $out")
		}
		return l
	}

	/**
	 * Reads the varints of one byte array the way protobuf's `CodedInputStream.readRawVarint32`
	 * reads them off an `InputStream`, which is what the java code uses here.
	 */
	private class VarintReader(private val bytes: ByteArray) {

		private var pos = 0

		fun available(): Int = bytes.size - pos

		private fun read(): Int {
			if (pos >= bytes.size) {
				throw IllegalStateException("Truncated varint at $pos")
			}
			return bytes[pos++].toInt() and 0xff
		}

		fun readRawVarint32(): Int {
			val firstByte = read()
			if (firstByte and 0x80 == 0) {
				return firstByte
			}
			var result = firstByte and 0x7f
			var offset = 7
			while (offset < 32) {
				val b = read()
				result = result or ((b and 0x7f) shl offset)
				if (b and 0x80 == 0) {
					return result
				}
				offset += 7
			}
			// Keep reading up to 64 bits.
			while (offset < 64) {
				val b = read()
				if (b and 0x80 == 0) {
					return result
				}
				offset += 7
			}
			throw IllegalStateException("Malformed varint at $pos")
		}
	}
}

/**
 * Counters and timings of one HH search, for the log line at its end.
 *
 * A copy of `HHRouteDataStructure.RoutingStats` in OsmAnd-java.
 */
class RoutingStats {

	@JvmField
	var firstRouteVisitedVertices: Int = 0

	@JvmField
	var visitedVertices: Int = 0

	@JvmField
	var uniqueVisitedVertices: Int = 0

	@JvmField
	var addedVertices: Int = 0

	@JvmField
	var loadPointsTime: Double = 0.0

	@JvmField
	var loadEdgesCnt: Int = 0

	@JvmField
	var loadEdgesTime: Double = 0.0

	@JvmField
	var altRoutingTime: Double = 0.0

	@JvmField
	var routingTime: Double = 0.0

	@JvmField
	var searchPointsTime: Double = 0.0

	@JvmField
	var addQueueTime: Double = 0.0

	@JvmField
	var pollQueueTime: Double = 0.0

	@JvmField
	var prepTime: Double = 0.0
}
