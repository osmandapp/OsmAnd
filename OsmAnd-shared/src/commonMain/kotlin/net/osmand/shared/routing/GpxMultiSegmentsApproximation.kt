package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.format
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KPriorityQueue
import net.osmand.shared.util.collections.KTLongHashSet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The geometry-based approximation: from a road segment the first track point lies on, walks the
 * road graph along the track, one road segment at a time, taking at every junction the segments
 * that stay within `minPointApproximation` of the track; a priority queue orders the open ends by
 * how far they strayed, so the walk that follows the track best is extended first, and a walk
 * that loses the track is abandoned unless it got further than the best so far. Where no road
 * continues, the route found is wrapped up and the walk starts again from the next track point a
 * road can be found for.
 *
 * A copy of `GpxMultiSegmentsApproximation` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS.
 */
// TEST missing roads, performance, start-end points (precise)
class GpxMultiSegmentsApproximation(
	private val frontEnd: RoutePlannerFrontEnd, private val gctx: GpxRouteApproximation, private val gpxPoints: List<GpxPoint>
) {

	private val metricsComparator = Comparator<RouteSegmentAppr> { o1, o2 -> o1.metric().compareTo(o2.metric()) }

	/// Evaluation variables
	private val minPointApproximation: Float = gctx.ctx.config.minPointApproximation
	private val initDist: Float = minPointApproximation / 2
	private val queue = KPriorityQueue(11, metricsComparator)
	private var visited = KTLongHashSet()

	private class RouteSegmentAppr {
		val segment: RouteSegment
		val parent: RouteSegmentAppr?

		val gpxStart: Int
		var gpxLen = 0
		var maxDistToGpx = 0.0

		fun gpxNext(): Int = gpxStart + gpxLen + 1

		constructor(start: Int, pnt: RouteSegmentPoint) {
			this.parent = null
			this.segment = pnt
			this.gpxStart = start
		}

		constructor(parent: RouteSegmentAppr, segment: RouteSegment) {
			this.parent = parent
			this.segment = segment
			this.gpxStart = parent.gpxStart + parent.gpxLen
		}

		fun metric(): Double = maxDistToGpx / sqrt((gpxLen + 1).toDouble()) // heuristics for eager algorithm

		override fun toString(): String = "%d -> %d  ( %s ) %.2f".format(gpxStart, gpxNext(), segment, maxDistToGpx)
	}

	private fun loadConnections(last: RouteSegmentAppr, connected: MutableList<RouteSegmentAppr>) {
		connected.clear()
		if (last.parent == null) {
			val pnt = last.segment as RouteSegmentPoint
			addSegmentInternal(last, pnt, connected)
			val others = pnt.others
			if (others != null) {
				for (o in others) {
					addSegmentInternal(last, o, connected)
				}
			}
		} else {
			var sg = gctx.ctx.loadRouteSegment(last.segment.getEndPointX(), last.segment.getEndPointY(), gctx.ctx.config.memoryLimitation)
			while (sg != null) {
				addSegment(last, sg.initRouteSegment(!sg.isPositive()), connected)
				addSegment(last, sg, connected)
				sg = sg.getNext()
			}
		}
	}

	private fun addSegmentInternal(last: RouteSegmentAppr, sg: RouteSegment, connected: MutableList<RouteSegmentAppr>) {
		val accept = approximateSegment(last, sg, connected)
		if (DEBUG && !accept) {
			log.debug("** $sg - not accepted")
		}
	}

	private fun addSegment(last: RouteSegmentAppr, sg: RouteSegment?, connected: MutableList<RouteSegmentAppr>) {
		if (sg == null) {
			return
		}
		val oneway = gctx.ctx.getRouter().isOneWay(sg.getRoad())
		if ((sg.isPositive() && oneway < 0) || (!sg.isPositive() && oneway > 0)) {
			// don't allow passing wrong way
			return
		}
		// Disable loops:
		// min(sg.getSegmentStart(), sg.getSegmentEnd()) != min(last.segment.getSegmentStart(), last.segment.getSegmentEnd())
		if (sg.getRoad().id != last.segment.getRoad().id
			|| sg.getSegmentStart() != last.segment.getSegmentStart()
		) {
			addSegmentInternal(last, sg, connected)
		}
	}

	private fun visit(r: RouteSegmentAppr) {
		visited.add(calculateRoutePointId(r))
	}

	private fun isVisited(r: RouteSegmentAppr): Boolean = visited.contains(calculateRoutePointId(r))

	fun gpxApproximation(): GpxRouteApproximation {
		val timeToCalculate = nanoTime()
		initGpxPointsXY31(gpxPoints)

		val currentPoint = findNextRoutablePoint(0) ?: return gctx
		var last = RouteSegmentAppr(0, currentPoint.pnt!!)
		val connected = ArrayList<RouteSegmentAppr>()
		var bestRoute: RouteSegmentAppr? = null
		while (last.gpxNext() < gpxPoints.size) {
			if (gctx.ctx.calculationProgress?.isCancelled == true) {
				break
			}
			var bestNext: RouteSegmentAppr? = null
			if (!isVisited(last)) {
				visit(last)
				loadConnections(last, connected)
				if (connected.size > 0) {
					if (EAGER_ALGORITHM) {
						connected.sortWith(metricsComparator)
						bestNext = connected[0]
						for (i in 1 until connected.size) {
							queue.add(connected[i])
						}
					} else if (PRIORITY_ALGORITHM) {
						for (c in connected) {
							queue.add(c)
						}
					}
				}
			}
			bestNext = peakMinFromQueue(bestRoute, bestNext) // try to revert to MAX_DEPTH_ROLLBACK if bestNext null
			if (bestNext != null) {
				if (DEBUG) {
					log.debug(bestNext.toString() + " " + gpxPoints[bestNext.gpxStart + bestNext.gpxLen].loc)
				}
				if (bestRoute == null || bestRoute.gpxNext() < last.gpxNext()) {
					bestRoute = last
				}
				last = bestNext
			} else {
				if (bestRoute != null) {
					wrapupRoute(gpxPoints, bestRoute)
				}
				val pnt = findNextRoutablePoint(bestRoute?.gpxNext() ?: last.gpxNext())
				visited = KTLongHashSet()
				if (pnt == null) {
					debugln("------------------")
					break
				} else {
					debugln("\n!!! " + pnt.ind + " " + pnt.loc + " " + pnt.pnt)
					last = RouteSegmentAppr(pnt.ind, pnt.pnt!!)
					bestRoute = null
				}
			}
		}
		val progress = gctx.ctx.calculationProgress
		if (progress != null) {
			progress.timeToCalculate = nanoTime() - timeToCalculate
			if (progress.isCancelled) {
				log.info("Approximation cancelled")
				return gctx
			}
		}
		if (bestRoute == null || bestRoute.gpxNext() < last.gpxNext()) {
			bestRoute = last // prefer the farthest end-of-the-route
		}
		wrapupRoute(gpxPoints, bestRoute)
		if (RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION) {
			log.info(
				"Approximation took %.2f seconds (%d route points searched)".format(
					(nanoTime() - timeToCalculate) / 1.0e9, gctx.routePointsSearched
				)
			)
		}
		return gctx
	}

	private fun peakMinFromQueue(bestRoute: RouteSegmentAppr?, bestNextArg: RouteSegmentAppr?): RouteSegmentAppr? {
		var bestNext = bestNextArg
		while (!queue.isEmpty() && bestNext == null) {
			bestNext = queue.poll()!!
			if (bestRoute != null && gpxDist(bestRoute.gpxNext(), bestNext.gpxNext()) > MAX_DEPTH_ROLLBACK) {
				bestNext = null
			}
		}
		return bestNext
	}

	private fun debugln(string: String) {
		if (DEBUG) {
			log.debug(string)
		}
	}

	private fun gpxDist(gpxL1: Int, gpxL2: Int): Double {
		return gpxPoints[minOf(gpxL1, gpxPoints.size - 1)].cumDist - gpxPoints[minOf(gpxL2, gpxPoints.size - 1)].cumDist
	}

	private fun approximateSegment(parent: RouteSegmentAppr, sg: RouteSegment, connected: MutableList<RouteSegmentAppr>): Boolean {
		val c = RouteSegmentAppr(parent, sg)
		var added = false
		for (pointInd in c.gpxStart + 1 until gpxPoints.size) {
			val p = gpxPoints[pointInd]
			if (p.x31 == c.segment.getEndPointX() && p.y31 == c.segment.getEndPointY()) {
				c.gpxLen++
				continue
			}
			val pp = KMapUtils.getProjectionPoint31(
				p.x31, p.y31, c.segment.getStartPointX(), c.segment.getStartPointY(), c.segment.getEndPointX(), c.segment.getEndPointY()
			)
			val beforeStart = (pp.x == c.segment.getStartPointX().toDouble() && pp.y == c.segment.getStartPointY().toDouble())
			val farEnd = (pp.x == c.segment.getEndPointX().toDouble() && pp.y == c.segment.getEndPointY().toDouble())
			if (farEnd) {
				break
			}
			val dist = BinaryRoutePlanner.squareRootDist(pp.x.toInt(), pp.y.toInt(), p.x31, p.y31)
			if (dist > minPointApproximation) {
				if (beforeStart || c.gpxLen > 0) {
					break
				}
				return added
			}
			if (dist > MIN_BRANCHING_DIST && dist > c.maxDistToGpx && c.gpxLen > 0) {
				val altShortBranch = RouteSegmentAppr(parent, sg)
				altShortBranch.maxDistToGpx = c.maxDistToGpx
				altShortBranch.gpxLen = c.gpxLen
				added = addConnected(parent, altShortBranch, connected) || added
			}
			c.maxDistToGpx = max(c.maxDistToGpx, dist)
			c.gpxLen++
		}
		added = addConnected(parent, c, connected) || added
		return added
	}

	private fun addConnected(parent: RouteSegmentAppr, c: RouteSegmentAppr, connected: MutableList<RouteSegmentAppr>): Boolean {
		if (isVisited(c)) {
			return false
		}
		val pointInd = c.gpxNext()
		// calculate dist for last segment (end point is exactly in between prev gpx / next gpx)
		// because next gpx point doesn't project onto segment
		if (pointInd < gpxPoints.size) {
			val pp = KMapUtils.getProjectionPoint31(
				c.segment.getEndPointX(), c.segment.getEndPointY(),
				gpxPoints[pointInd - 1].x31, gpxPoints[pointInd - 1].y31, gpxPoints[pointInd].x31, gpxPoints[pointInd].y31
			)
			val dist = BinaryRoutePlanner.squareRootDist(pp.x.toInt(), pp.y.toInt(), c.segment.getEndPointX(), c.segment.getEndPointY())
			c.maxDistToGpx = max(c.maxDistToGpx, dist)
			if (dist > minPointApproximation) {
				if (DEBUG) {
					log.debug("** $c - ignore $dist")
				}
				return false
			}
		}
		connected.add(c)
		if (DEBUG) {
			log.debug("** $c - accept")
		}
		return true
	}

	private fun findNextRoutablePoint(searchStart: Int): GpxPoint? {
		for (i in searchStart until gpxPoints.size) {
			if (initRoutingPoint(gpxPoints[i], initDist.toDouble())) {
				return gpxPoints[i]
			}
		}
		return null
	}

	private fun initRoutingPoint(start: GpxPoint?, distThreshold: Double): Boolean {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++
			val gpxDir = start.track!!.directionRoute(start.ind, true)
			val rsp = frontEnd.findRouteSegment(start.loc.latitude, start.loc.longitude, gctx.ctx, null, false)
			if (rsp == null || KMapUtils.getDistance(rsp.getPreciseLatLon(), start.loc) > distThreshold) {
				return false
			}
			val pnt = initStartPoint(start, gpxDir, rsp)
			start.pnt = pnt
			val others = rsp.others
			if (others != null) {
				val pntOthers = ArrayList<RouteSegmentPoint>()
				pnt.others = pntOthers
				for (o in others) {
					if (KMapUtils.getDistance(o.getPreciseLatLon(), start.loc) < distThreshold) {
						pntOthers.add(initStartPoint(start, gpxDir, o))
					}
				}
			}
			return true
		}
		return false
	}

	@Suppress("UNUSED_PARAMETER")
	private fun initStartPoint(start: GpxPoint, gpxDir: Double, rsp: RouteSegmentPoint): RouteSegmentPoint {
		val dirc = rsp.getRoad().directionRoute(rsp.getSegmentStart().toInt(), rsp.isPositive())
		val direct = abs(KMapUtils.alignAngleDifference(gpxDir - dirc)) < PI / 2
		return RouteSegmentPoint(
			rsp.getRoad(), if (direct) rsp.getSegmentStart().toInt() else rsp.getSegmentEnd().toInt(),
			if (direct) rsp.getSegmentEnd().toInt() else rsp.getSegmentStart().toInt(), rsp.distToProj
		)
	}

	private fun initGpxPointsXY31(gpxPoints: List<GpxPoint>) {
		for (p in gpxPoints) {
			if (TEST_SHIFT_GPX_POINTS) {
				val shift = 0.00015 // shift ~15 meters to check attached geometry visually
				p.loc = KLatLon(p.loc.latitude - shift, p.loc.longitude + shift)
			}
			p.x31 = KMapUtils.get31TileNumberX(p.loc.longitude)
			p.y31 = KMapUtils.get31TileNumberY(p.loc.latitude)
		}
	}

	private fun wrapupRoute(gpxPoints: List<GpxPoint>, bestRouteArg: RouteSegmentAppr) {
		if (bestRouteArg.parent == null) {
			return
		}
		var bestRoute: RouteSegmentAppr? = bestRouteArg
		val res = ArrayList<RouteSegmentResult>()
		var startInd = 0
		val last = minOf(bestRouteArg.gpxNext(), gpxPoints.size - 1)
		// combining segments doesn't seem to have any effect on tests
		var lastRes: RouteSegmentResult? = null
		while (bestRoute != null && bestRoute.parent != null) {
			startInd = bestRoute.gpxStart
			var end = bestRoute.segment.getSegmentEnd().toInt()
			if (lastRes != null && bestRoute.segment.getRoad().id == lastRes.getObject().id) {
				if (lastRes.getStartPointIndex() == bestRoute.segment.getSegmentEnd().toInt()
					&& lastRes.isForwardDirection() == bestRoute.segment.isPositive()
				) {
					end = lastRes.getEndPointIndex()
					res.removeAt(res.size - 1)
				}
			}
			val routeRes = RouteSegmentResult(bestRoute.segment.getRoad(), bestRoute.segment.getSegmentStart().toInt(), end)
			res.add(routeRes)
			bestRoute = bestRoute.parent
			lastRes = routeRes
		}
		res.reverse()
		if (DEBUG) {
			log.debug("ROUTE $startInd -> $last :")
			for (r in res) {
				log.debug(" $r")
			}
		}
		for (r in res) {
			r.setGpxPointIndex(startInd) // required for reconstructFinalPointsFromFullRoute()
		}
		gpxPoints[startInd].routeToTarget = res
		gpxPoints[startInd].targetInd = last // keep straight line
	}

	companion object {
		// ALGORITHM CONSTANTS //
		private const val MAX_DEPTH_ROLLBACK = 500 // 500 m rollback
		private const val MIN_BRANCHING_DIST = 10.0 // 5 m for branching
		private const val EAGER_ALGORITHM = false
		private const val PRIORITY_ALGORITHM = !EAGER_ALGORITHM
		private const val TEST_SHIFT_GPX_POINTS = false
		private const val DEBUG = false
		/////////////////////////

		private const val ROUTE_POINTS = 12
		private const val GPX_MAX = 30 // 1M

		private val log = LoggerFactory.getLogger("GpxMultiSegmentsApproximation")

		private fun calculateRoutePointId(segm: RouteSegmentAppr): Long {
			var segId: Long = 0
			if (segm.parent != null) {
				val positive = segm.segment.isPositive()
				segId = (segm.segment.getRoad().id shl ROUTE_POINTS) + (segm.segment.getSegmentStart().toInt() shl 1) + (if (positive) 1 else 0)
			}
			return (segId shl GPX_MAX) + (segm.gpxStart + segm.gpxLen)
		}
	}
}
