package net.osmand.shared.binary

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * What a routing search asks the reader for: a bounding box in 31 coordinates, and where the
 * counters of what was read go.
 *
 * The routing part of `BinaryMapIndexReader.SearchRequest` in OsmAnd-java, which is one generic
 * class for every kind of search. Routing uses only the box and the four counters, so that is
 * all this carries; the search kinds that need a name query or a poi filter will extend it when
 * they are copied.
 */
class SearchRequest {

	// 31 zoom tiles
	@JvmField
	var left: Int = 0

	@JvmField
	var right: Int = 0

	@JvmField
	var top: Int = 0

	@JvmField
	var bottom: Int = 0

	@JvmField
	var numberOfVisitedObjects: Int = 0

	@JvmField
	var numberOfAcceptedObjects: Int = 0

	@JvmField
	var numberOfReadSubtrees: Int = 0

	@JvmField
	var numberOfAcceptedSubtrees: Int = 0

	fun intersects(l: Int, t: Int, r: Int, b: Int): Boolean =
		r >= left && l <= right && t <= bottom && b >= top

	fun contains(l: Int, t: Int, r: Int, b: Int): Boolean =
		r <= right && l >= left && b <= bottom && t >= top

	companion object {
		@JvmStatic
		fun buildSearchRouteRequest(sleft: Int, sright: Int, stop: Int, sbottom: Int): SearchRequest {
			val request = SearchRequest()
			request.left = sleft
			request.right = sright
			request.top = stop
			request.bottom = sbottom
			return request
		}
	}
}
