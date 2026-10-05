package net.osmand.shared.routing

import net.osmand.shared.util.KMapAlgorithms
import net.osmand.shared.util.KMapUtils
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic
import kotlin.math.abs

/**
 * Turns what the planner found into the route the apps show: the segments in order, joined where
 * they run along one road, split where a turn is made in the middle of one, with the roads that
 * leave every junction attached, and then the manoeuvres and times from [TurnPreparation].
 *
 * A copy of the orchestration in `net.osmand.router.RouteResultPreparation`, which stays in
 * OsmAnd-java for android and tools; this copy is for iOS. The manoeuvres, lanes and times were
 * copied earlier into [TurnPreparation]; this is the part around them that needs the planner's
 * own segments and the routing context's tiles: `convertFinalSegmentToResults`, the area routing,
 * the split with the attached roads, and `prepareResult` that runs them in order. The debug
 * printing stays in java.
 */
object RouteResultPreparation {

	private const val TURN_DEGREE_MIN = 45f
	private const val UNMATCHED_TURN_DEGREE_MINIMUM = 45f
	private const val SPLIT_TURN_DEGREE_NOT_STRAIGHT = 100f

	/** Whether [validateAllPointsConnected] reports a gap; java's `PRINT_TO_CONSOLE_ROUTE_INFORMATION`. */
	@JvmField
	var PRINT_TO_CONSOLE_ROUTE_INFORMATION = true

	private class CombineAreaRoutePoint {
		var x31 = 0
		var y31 = 0
		var originalIndex = 0
	}

	private fun combineWayPointsForAreaRouting(ctx: RoutingContext, result: MutableList<RouteSegmentResult>) {
		for (i in result.indices) {
			val rsr = result[i]
			val obj = rsr.getObject()
			var area = false
			if (obj.getPoint31XTile(0) == obj.getPoint31XTile(obj.getPointsLength() - 1) &&
				obj.getPoint31YTile(0) == obj.getPoint31YTile(obj.getPointsLength() - 1)
			) {
				area = true
			}
			if (!area || !ctx.getRouter().isArea(obj)) {
				continue
			}
			val originalWay = ArrayList<CombineAreaRoutePoint>()
			val routeWay = ArrayList<CombineAreaRoutePoint>()
			for (j in 0 until obj.getPointsLength()) {
				val pnt = CombineAreaRoutePoint()
				pnt.x31 = obj.getPoint31XTile(j)
				pnt.y31 = obj.getPoint31YTile(j)
				pnt.originalIndex = j

				originalWay.add(pnt)
				if (j >= rsr.getStartPointIndex() && j <= rsr.getEndPointIndex()) {
					routeWay.add(pnt)
				} else if (j <= rsr.getStartPointIndex() && j >= rsr.getEndPointIndex()) {
					routeWay.add(0, pnt)
				}
			}
			val originalSize = routeWay.size
			simplifyAreaRouteWay(routeWay, originalWay)
			val newsize = routeWay.size
			if (routeWay.size != originalSize) {
				val nobj = RouteDataObject(obj)
				val pointsX = IntArray(newsize)
				val pointsY = IntArray(newsize)
				for (k in 0 until newsize) {
					pointsX[k] = routeWay[k].x31
					pointsY[k] = routeWay[k].y31
				}
				nobj.pointsX = pointsX
				nobj.pointsY = pointsY
				// in future point names might be used
				nobj.restrictions = null
				nobj.restrictionsVia = null
				nobj.pointTypes = null
				nobj.pointNames = null
				nobj.pointNameTypes = null
				val nrsr = RouteSegmentResult(nobj, 0, newsize - 1)
				result[i] = nrsr
			}
		}
	}

	private fun simplifyAreaRouteWay(routeWay: MutableList<CombineAreaRoutePoint>, originalWay: List<CombineAreaRoutePoint>) {
		var changed = true
		while (changed) {
			changed = false
			var connectStart = -1
			var connectLen = 0
			val dist = 0.0
			var length = routeWay.size - 1
			while (length > 0 && connectLen == 0) {
				for (i in 0 until routeWay.size - length) {
					val p = routeWay[i]
					val n = routeWay[i + length]
					if (segmentLineBelongsToPolygon(p, n, originalWay)) {
						@Suppress("UNUSED_VALUE")
						var ndist = BinaryRoutePlanner.squareRootDist(p.x31, p.y31, n.x31, n.y31)
						if (ndist > dist) {
							// as in java: the first pair found at this length wins, dist stays 0
							ndist = dist
							connectStart = i
							connectLen = length
						}
					}
				}
				length--
			}
			while (connectLen > 1) {
				routeWay.removeAt(connectStart + 1)
				connectLen--
				changed = true
			}
		}
	}

	private fun segmentLineBelongsToPolygon(p: CombineAreaRoutePoint, n: CombineAreaRoutePoint, originalWay: List<CombineAreaRoutePoint>): Boolean {
		var intersections = 0
		val mx = p.x31 / 2 + n.x31 / 2
		val my = p.y31 / 2 + n.y31 / 2
		for (i in 1 until originalWay.size) {
			val p2 = originalWay[i - 1]
			val n2 = originalWay[i]
			if (p.originalIndex != i && p.originalIndex != i - 1) {
				if (n.originalIndex != i && n.originalIndex != i - 1) {
					if (KMapAlgorithms.linesIntersect(
							p.x31.toDouble(), p.y31.toDouble(), n.x31.toDouble(), n.y31.toDouble(),
							p2.x31.toDouble(), p2.y31.toDouble(), n2.x31.toDouble(), n2.y31.toDouble()
						)
					) {
						return false
					}
				}
			}
			val fx = KMapAlgorithms.rayIntersectX(p2.x31, p2.y31, n2.x31, n2.y31, my)
			if (Int.MIN_VALUE != fx && mx >= fx) {
				intersections++
			}
		}
		return intersections % 2 == 1
	}

	/** The whole preparation, in the order java runs it. */
	@JvmStatic
	fun prepareResult(ctx: RoutingContext, result: MutableList<RouteSegmentResult>): RouteCalcResult {
		for (i in result.indices) {
			val road = result[i].getObject()
			checkAndInitRouteRegion(ctx, road)
			// "osmand_dp" using for backward compatibility from native lib RoutingConfiguration directionPoints
			road.region?.findOrCreateRouteType(DirectionPoint.TAG, DirectionPoint.DELETE_TYPE)
		}
		combineWayPointsForAreaRouting(ctx, result)
		validateAllPointsConnected(result)
		splitRoadsAndAttachRoadSegments(ctx, result)
		for (i in result.indices) {
			TurnPreparation.filterMinorStops(result[i])
		}
		TurnPreparation.calculateTimeSpeed(ctx, result)
		TurnPreparation.prepareTurnResults(ctx, result)
		return RouteCalcResult(result)
	}

	private fun splitRoadsAndAttachRoadSegments(ctx: RoutingContext, result: MutableList<RouteSegmentResult>) {
		var i = 0
		while (i < result.size) {
			if (ctx.checkIfMemoryLimitCritical(ctx.config.memoryLimitation)) {
				ctx.unloadUnusedTiles(ctx.config.memoryLimitation)
			}
			var rr = result[i]
			val plus = rr.getStartPointIndex() < rr.getEndPointIndex()
			var next: Int
			val unmatched = TurnPreparation.UNMATCHED_HIGHWAY_TYPE == rr.getObject().getHighway()
			var j = rr.getStartPointIndex()
			while (j != rr.getEndPointIndex()) {
				next = if (plus) j + 1 else j - 1
				if (j == rr.getStartPointIndex()) {
					attachRoadSegments(ctx, result, i, j, plus)
				}
				if (next != rr.getEndPointIndex()) {
					attachRoadSegments(ctx, result, i, next, plus)
				}
				val attachedRoutes = rr.getAttachedRoutes(next)
				var tryToSplit = next != rr.getEndPointIndex() && !rr.getObject().roundabout()
				if (rr.getDistance(next, plus) == 0f) {
					// same point will be processed next step
					tryToSplit = false
				}
				if (tryToSplit) {
					val distBearing = if (unmatched) RouteSegmentResult.DIST_BEARING_DETECT_UNMATCHED else RouteSegmentResult.DIST_BEARING_DETECT
					// avoid small zigzags
					var before = rr.getBearingEnd(next, distBearing)
					var after = rr.getBearingBegin(next, distBearing)
					if (rr.getDistance(next, plus) < distBearing / 2) {
						after = before
					} else if (rr.getDistance(next, !plus) < distBearing / 2) {
						before = after
					}
					val contAngle = abs(KMapUtils.degreesDiff(before.toDouble(), after.toDouble()))
					val straight = contAngle < TURN_DEGREE_MIN
					var isSplit = false

					if (unmatched && abs(contAngle) >= UNMATCHED_TURN_DEGREE_MINIMUM) {
						isSplit = true
					}
					// split if needed
					for (rs in attachedRoutes) {
						val diff = KMapUtils.degreesDiff(before.toDouble(), rs.getBearingBegin().toDouble())
						if (abs(diff) <= TURN_DEGREE_MIN) {
							isSplit = true
						} else if (!straight && abs(diff) < SPLIT_TURN_DEGREE_NOT_STRAIGHT) {
							isSplit = true
						}
					}
					if (isSplit) {
						val endPointIndex = rr.getEndPointIndex()
						val split = RouteSegmentResult(rr.getObject(), next, endPointIndex)
						split.copyPreattachedRoutes(rr, abs(next - rr.getStartPointIndex()))
						rr.setEndPointIndex(next)
						result.add(i + 1, split)
						i++
						// switch current segment to the splitted
						rr = split
					}
				}
				j = next
			}
			i++
		}
	}

	private fun checkAndInitRouteRegion(ctx: RoutingContext, road: RouteDataObject) {
		val region = road.region ?: return
		val reader = ctx.reverseMap[region]
		reader?.initRouteRegion(region)
	}

	@JvmStatic
	fun validateAllPointsConnected(result: List<RouteSegmentResult>) {
		for (i in 1 until result.size) {
			val rr = result[i]
			val pr = result[i - 1]
			val d = KMapUtils.getDistance(pr.getPoint(pr.getEndPointIndex()), rr.getPoint(rr.getStartPointIndex()))
			if (d > 0 && PRINT_TO_CONSOLE_ROUTE_INFORMATION) {
				println(
					"Points are not connected: " + (i - 1) + "-" + i + " of " + (result.size - 1) + " " + pr.getObject() + " (" +
							pr.getEndPointIndex() + ") -> " + rr.getObject() + " (" + rr.getStartPointIndex() + ") by " + d + " meters"
				)
			}
		}
	}

	/** Walks the two search trees back from where they met, into the segments of the route in order. */
	@JvmStatic
	fun convertFinalSegmentToResults(ctx: RoutingContext, finalSegment: FinalRouteSegment?): MutableList<RouteSegmentResult> {
		val result = ArrayList<RouteSegmentResult>()
		if (finalSegment != null) {
			ctx.routingTime += finalSegment.distanceFromStart
			val opposite = finalSegment.opposite
			var correctionTime = if (opposite == null) 0f else
				finalSegment.distanceFromStart - distanceFromStart(opposite) - distanceFromStart(finalSegment.parentRoute)
			// Get results from opposite direction roads
			val thisSegment = if (opposite == null) finalSegment else finalSegment.parentRoute // for dijkstra
			var segment = if (finalSegment.reverseWaySearch) thisSegment else opposite
			while (segment != null) {
				val res = RouteSegmentResult(segment.getRoad(), segment.getSegmentEnd().toInt(), segment.getSegmentStart().toInt())
				val parentRoutingTime = segment.getParentRoute()?.distanceFromStart ?: 0f
				res.setRoutingTime(segment.distanceFromStart - parentRoutingTime + correctionTime)
				correctionTime = 0f
				segment = segment.getParentRoute()
				TurnPreparation.addRouteSegmentToResult(ctx, result, res, false)
			}
			// reverse it just to attach good direction roads
			result.reverse()
			segment = if (finalSegment.reverseWaySearch) opposite else thisSegment
			while (segment != null) {
				val res = RouteSegmentResult(segment.getRoad(), segment.getSegmentStart().toInt(), segment.getSegmentEnd().toInt())
				val parentRoutingTime = segment.getParentRoute()?.distanceFromStart ?: 0f
				res.setRoutingTime(segment.distanceFromStart - parentRoutingTime + correctionTime)
				correctionTime = 0f
				segment = segment.getParentRoute()
				// happens in smart recalculation
				TurnPreparation.addRouteSegmentToResult(ctx, result, res, true)
			}
			result.reverse()
			checkTotalRoutingTime(result, finalSegment.distanceFromStart)
		}
		return result
	}

	private fun distanceFromStart(s: RouteSegment?): Float = s?.distanceFromStart ?: 0f

	private fun checkTotalRoutingTime(result: List<RouteSegmentResult>, cmp: Float) {
		var totalRoutingTime = 0f
		for (r in result) {
			totalRoutingTime += r.getRoutingTime()
		}
		if (abs(totalRoutingTime - cmp) > 0.1) {
			if (PRINT_TO_CONSOLE_ROUTE_INFORMATION) {
				println("Total sum routing time ! $totalRoutingTime == $cmp")
			}
		}
	}

	private fun attachRoadSegments(ctx: RoutingContext, result: List<RouteSegmentResult>, routeInd: Int, pointInd: Int, plus: Boolean) {
		val rr = result[routeInd]
		val road = rr.getObject()
		val nextL = if (pointInd < road.getPointsLength() - 1) getPoint(road, pointInd + 1) else 0L
		val prevL = if (pointInd > 0) getPoint(road, pointInd - 1) else 0L

		// attach additional roads to represent more information about the route
		var previousResult: RouteSegmentResult? = null

		// by default make same as this road id
		var previousRoadId = road.getId()
		if (pointInd == rr.getStartPointIndex() && routeInd > 0) {
			previousResult = result[routeInd - 1]
			previousRoadId = previousResult.getObject().getId()
			if (previousRoadId != road.getId()) {
				if (previousResult.getStartPointIndex() < previousResult.getEndPointIndex()
					&& previousResult.getEndPointIndex() < previousResult.getObject().getPointsLength() - 1
				) {
					rr.attachRoute(
						pointInd, RouteSegmentResult(
							previousResult.getObject(), previousResult.getEndPointIndex(),
							previousResult.getObject().getPointsLength() - 1
						)
					)
				} else if (previousResult.getStartPointIndex() > previousResult.getEndPointIndex()
					&& previousResult.getEndPointIndex() > 0
				) {
					rr.attachRoute(pointInd, RouteSegmentResult(previousResult.getObject(), previousResult.getEndPointIndex(), 0))
				}
			}
		}
		val it: Iterator<RouteSegment>?
		val preAttached = rr.getPreAttachedRoutes(pointInd)
		if (preAttached != null) {
			it = object : Iterator<RouteSegment> {
				var i = 0

				override fun hasNext(): Boolean = i < preAttached.size

				override fun next(): RouteSegment {
					val r = preAttached[i++]
					return RouteSegment(r.getObject(), r.getStartPointIndex(), r.getEndPointIndex())
				}
			}
		} else {
			val rt = ctx.loadRouteSegment(road.getPoint31XTile(pointInd), road.getPoint31YTile(pointInd), ctx.config.memoryLimitation)
			it = rt?.getIterator()
		}
		// try to attach all segments except with current id
		while (it != null && it.hasNext()) {
			val routeSegment = it.next()
			val addRoad = routeSegment.getRoad()
			if (addRoad.getId() != road.getId() && addRoad.getId() != previousRoadId) {
				checkAndInitRouteRegion(ctx, addRoad)
				// Future: restrictions can be considered as well
				val oneWay = ctx.getRouter().isOneWay(addRoad)
				if (oneWay >= 0 && routeSegment.getSegmentStart() < addRoad.getPointsLength() - 1) {
					val pointL = getPoint(addRoad, routeSegment.getSegmentStart() + 1)
					if (pointL != nextL && pointL != prevL) {
						// if way contains same segment (nodes) as different way (do not attach it)
						rr.attachRoute(pointInd, RouteSegmentResult(addRoad, routeSegment.getSegmentStart().toInt(), addRoad.getPointsLength() - 1))
					}
				}
				if (oneWay <= 0 && routeSegment.getSegmentStart() > 0) {
					val pointL = getPoint(addRoad, routeSegment.getSegmentStart() - 1)
					// if way contains same segment (nodes) as different way (do not attach it)
					if (pointL != nextL && pointL != prevL) {
						rr.attachRoute(pointInd, RouteSegmentResult(addRoad, routeSegment.getSegmentStart().toInt(), 0))
					}
				}
			}
		}
	}

	private fun getPoint(road: RouteDataObject, pointInd: Int): Long {
		return (road.getPoint31XTile(pointInd).toLong() shl 31) + road.getPoint31YTile(pointInd).toLong()
	}
}
