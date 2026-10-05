package net.osmand.shared.routing

import net.osmand.shared.binary.ResultMatcher
import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.format
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KTIntArrayList
import kotlin.jvm.JvmField
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Attaches a track to the roads of the map: the result of one approximation, and the two ways of
 * getting it. The routing-based way runs the A* search between track points a few kilometres
 * apart, steps back from every found route so the next search starts on the road, and accepts a
 * route only if it stays close to the track. The geometry-based way ([GpxMultiSegmentsApproximation],
 * or the older [GpxPointsMatchApproximation]) walks the road graph along the track without a
 * search. Either way the parts of the track no road matches are kept as straight lines, and the
 * turns are prepared over the whole.
 *
 * A copy of `GpxRouteApproximation` in OsmAnd-java, which stays there for android and tools; this
 * copy is for iOS. The path through the C++ library (`runNativeSearchGpxRoute`) is not copied.
 */
class GpxRouteApproximation(@JvmField val ctx: RoutingContext) {

	@JvmField
	var finalPoints: MutableList<GpxPoint> = ArrayList()

	@JvmField
	var fullRoute: MutableList<RouteSegmentResult> = ArrayList()

	private lateinit var router: RoutePlannerFrontEnd
	private var routeCalculations = 0

	@JvmField
	var routePointsSearched = 0
	private var routeDistCalculations = 0

	@JvmField
	var routeDistance = 0
	private var routeDistanceUnmatched = 0

	override fun toString(): String {
		return ">> GPX approximation (%d of %d m route calcs, %d route points searched) for %d m: %d m unmatched".format(
			routeCalculations, routeDistCalculations, routePointsSearched, routeDistance, routeDistanceUnmatched
		)
	}

	internal fun searchGpxRouteInternal(
		router: RoutePlannerFrontEnd, gpxPoints: List<GpxPoint>, resultMatcher: ResultMatcher<GpxRouteApproximation?>?,
		useExternalTimestamps: Boolean
	): GpxRouteApproximation {
		this.router = router
		val result = if (router.isUseGeometryBasedApproximation()) {
			searchGpxSegments(this, gpxPoints)
		} else {
			searchGpxRouteByRouting(this, gpxPoints)
		}
		result.reconstructFinalPointsFromFullRoute()
		if (useExternalTimestamps) {
			result.applyExternalTimestamps(gpxPoints)
		}
		if (resultMatcher != null) {
			resultMatcher.publish(if (this.ctx.calculationProgress?.isCancelled == true) null else this)
		}
		return result
	}

	fun collectFinalPointsAsRoute(): List<RouteSegmentResult> {
		val route = ArrayList<RouteSegmentResult>()
		for (gp in finalPoints) {
			gp.routeToTarget?.let { route.addAll(it) }
		}
		return route
	}

	private fun distFromLastPoint(pnt: KLatLon): Double {
		if (fullRoute.size > 0) {
			return KMapUtils.getDistance(getLastPoint()!!, pnt)
		}
		return 0.0
	}

	private fun getLastPoint(): KLatLon? {
		if (fullRoute.size > 0) {
			return fullRoute[fullRoute.size - 1].getEndPoint()
		}
		return null
	}

	private fun applyExternalTimestamps(sourcePoints: List<GpxPoint>) {
		if (!validateExternalTimestamps(sourcePoints)) {
			log.warn("applyExternalTimestamps() got invalid sourcePoints")
			return
		}
		for (gp in finalPoints) {
			val route = gp.routeToTarget ?: continue
			for (seg in route) {
				seg.setSegmentSpeed(calcSegmentSpeedByExternalTimestamps(gp, seg, sourcePoints))
			}
			TurnPreparation.recalculateTimeDistance(route)
		}
	}

	private fun calcSegmentSpeedByExternalTimestamps(gp: GpxPoint, seg: RouteSegmentResult, sourcePoints: List<GpxPoint>): Float {
		var speed = seg.getSegmentSpeed()
		val indexStart = gp.ind
		var indexEnd = gp.targetInd

		if (indexEnd == -1 && indexStart >= 0 && indexStart + 1 < sourcePoints.size) {
			indexEnd = indexStart + 1 // this is straight line
		}

		if (indexStart >= 0 && indexEnd > 0 && indexStart < indexEnd) {
			val time = sourcePoints[indexEnd].time - sourcePoints[indexStart].time
			if (time > 0) {
				var distance = 0.0
				for (i in indexStart until indexEnd) {
					distance += KMapUtils.getDistance(sourcePoints[i].loc, sourcePoints[i + 1].loc)
				}
				if (distance > 0) {
					speed = distance.toFloat() / (time.toFloat() / 1000) // update based on external timestamps
				}
			}
		}

		return speed
	}

	private fun validateExternalTimestamps(points: List<GpxPoint>?): Boolean {
		if (points == null || points.isEmpty()) {
			return false
		}
		var last: Long = 0
		for (p in points) {
			if (p.time == 0L || p.time < last) {
				return false
			}
			last = p.time
		}
		return true
	}

	private fun reconstructFinalPointsFromFullRoute() {
		// create gpx-to-final index map, clear routeToTarget(s)
		val gpxIndexFinalIndex = HashMap<Int, Int>()
		for (i in finalPoints.indices) {
			gpxIndexFinalIndex[finalPoints[i].ind] = i
			finalPoints[i].routeToTarget!!.clear()
		}

		// reconstruct routeToTarget from scratch
		var lastIndex = 0
		for (seg in fullRoute) {
			var index = seg.getGpxPointIndex()
			if (index == -1) {
				index = lastIndex
			} else {
				lastIndex = index
			}
			finalPoints[gpxIndexFinalIndex[index]!!].routeToTarget!!.add(seg)
		}

		// finally remove finalPoints with empty route
		val emptyFinalPoints = ArrayList<GpxPoint>()
		for (gp in finalPoints) {
			val route = gp.routeToTarget
			if (route != null) {
				if (route.size == 0) {
					emptyFinalPoints.add(gp)
				}
			}
		}
		if (emptyFinalPoints.size > 0) {
			finalPoints.removeAll(emptyFinalPoints)
		}
	}

	private fun searchGpxSegments(gctx: GpxRouteApproximation, gpxPoints: List<GpxPoint>): GpxRouteApproximation {
		val progress = gctx.ctx.calculationProgress ?: RouteCalculationProgress().also { gctx.ctx.calculationProgress = it }
		if (GPX_SEGMENT_ALGORITHM == GPX_OSM_POINTS_MATCH_ALGORITHM) {
			val app = GpxPointsMatchApproximation()
			app.gpxApproximation(router, gctx, gpxPoints)
		} else if (GPX_SEGMENT_ALGORITHM == GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM) {
			val app = GpxMultiSegmentsApproximation(router, gctx, gpxPoints)
			app.gpxApproximation()
		}
		calculateGpxRouteResult(gctx, gpxPoints)
		if (gctx.fullRoute.isNotEmpty() && !progress.isCancelled) {
			if (RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION) {
				log.info(gctx.toString())
			}
		}
		return gctx
	}

	private fun searchGpxRouteByRouting(gctx: GpxRouteApproximation, gpxPoints: List<GpxPoint>): GpxRouteApproximation {
		val timeToCalculate = nanoTime()
		val progress = gctx.ctx.calculationProgress ?: RouteCalculationProgress().also { gctx.ctx.calculationProgress = it }
		var start: GpxPoint? = null
		var prev: GpxPoint? = null
		if (gpxPoints.isNotEmpty()) {
			progress.totalApproximateDistance = gpxPoints[gpxPoints.size - 1].cumDist.toFloat()
			start = gpxPoints[0]
		}
		val minPointApproximation = gctx.ctx.config.minPointApproximation
		while (start != null && !progress.isCancelled) {
			var routeDist = gctx.ctx.config.maxStepApproximation.toDouble()
			var next = findNextGpxPointWithin(gpxPoints, start, routeDist)
			var routeFound = false
			if (next != null && initRoutingPoint(start, gctx, minPointApproximation.toDouble())) {
				while (routeDist >= gctx.ctx.config.minStepApproximation && !routeFound) {
					routeFound = initRoutingPoint(next, gctx, minPointApproximation.toDouble())
					if (routeFound) {
						routeFound = findGpxRouteSegment(gctx, gpxPoints, start, next!!, prev != null)
						if (routeFound) {
							routeFound = isRouteCloseToGpxPoints(minPointApproximation, gpxPoints, start, next)
							if (!routeFound) {
								start.routeToTarget = null
							}
						}
						if (routeFound && next.ind < gpxPoints.size - 1) {
							// route is found - cut the end of the route and move to next iteration
							val stepBack = stepBackAndFindPrevPointInRoute(gctx, gpxPoints, start, next)
							if (!stepBack) {
								// not supported case (workaround increase routing.xml maxStepApproximation)
								log.info("Consider to increase routing.xml maxStepApproximation to: " + routeDist * 2)
								start.routeToTarget = null
								routeFound = false
							} else {
								gctx.ctx.getVisitor()?.visitApproximatedSegments(start.routeToTarget!!, start, next)
							}
						}
					}
					if (!routeFound) {
						// route is not found move next point closer to start point (distance / 2)
						routeDist /= 2
						if (routeDist < gctx.ctx.config.minStepApproximation
							&& routeDist > gctx.ctx.config.minStepApproximation / 2 + 1
						) {
							routeDist = gctx.ctx.config.minStepApproximation.toDouble()
						}
						next = findNextGpxPointWithin(gpxPoints, start, routeDist)
						if (next != null) {
							routeDist = min(next.cumDist - start.cumDist, routeDist)
						}
					}
				}
			}
			// route is not found skip segment and keep it as straight line on display
			if (!routeFound && next != null) {
				// route is not found, move start point by
				next = findNextGpxPointWithin(gpxPoints, start, gctx.ctx.config.minStepApproximation.toDouble())
				if (prev != null) {
					prev.routeToTarget!!.addAll(prev.stepBackRoute!!)
					if (next != null) {
						log.warn("NOT found route from: " + start.pnt!!.getRoad() + " at " + start.pnt!!.getSegmentStart())
					}
				}
				prev = null
			} else {
				prev = start
			}
			start = next
			if (start != null) {
				progress.approximatedDistance = start.cumDist.toFloat()
			}
		}
		progress.timeToCalculate = nanoTime() - timeToCalculate
		calculateGpxRouteResult(gctx, gpxPoints)
		if (gctx.fullRoute.isNotEmpty() && !progress.isCancelled) {
			if (RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION) {
				log.info(gctx.toString())
			}
		}
		return gctx
	}

	private fun stepBackAndFindPrevPointInRoute(gctx: GpxRouteApproximation, gpxPoints: List<GpxPoint>, start: GpxPoint, next: GpxPoint): Boolean {
		// step back to find to be sure
		// 1) route point is behind GpxPoint - minPointApproximation (end route point could slightly ahead)
		// 2) we don't miss correct turn i.e. points could be attached to multiple routes
		// 3) to make sure that we perfectly connect to RoadDataObject points
		val stepBackDist = max(gctx.ctx.config.minPointApproximation, gctx.ctx.config.minStepApproximation).toDouble()
		var d = 0.0
		val routeToTarget = start.routeToTarget!!
		var segmendInd = routeToTarget.size - 1
		var search = true
		val stepBackRoute = ArrayList<RouteSegmentResult>()
		start.stepBackRoute = stepBackRoute
		mainLoop@ while (segmendInd >= 0 && search) {
			val rr = routeToTarget[segmendInd]
			val minus = rr.getStartPointIndex() < rr.getEndPointIndex()
			var j = rr.getEndPointIndex()
			while (j != rr.getStartPointIndex()) {
				val nextInd = if (minus) j - 1 else j + 1
				d += KMapUtils.getDistance(rr.getPoint(j), rr.getPoint(nextInd))
				if (d > stepBackDist) {
					if (nextInd == rr.getStartPointIndex()) {
						segmendInd--
					} else {
						val seg = RouteSegmentResult(rr.getObject(), nextInd, rr.getEndPointIndex())
						seg.setGpxPointIndex(start.ind)
						stepBackRoute.add(seg)
						rr.setEndPointIndex(nextInd)
					}
					search = false
					break@mainLoop
				}
				j = nextInd
			}
			segmendInd--
		}
		if (segmendInd == -1) {
			// here all route segments - 1 is longer than needed distance to step back
			return false
		}

		while (routeToTarget.size > segmendInd + 1) {
			val removed = routeToTarget.removeAt(segmendInd + 1)
			stepBackRoute.add(removed)
		}
		val res = routeToTarget[segmendInd]
		val end = res.getEndPointIndex()
		val beforeEnd = if (res.isForwardDirection()) end - 1 else end + 1
		val pnt = RouteSegmentPoint(res.getObject(), beforeEnd, end, 0.0)
		// use start point as it overlaps
		// as we step back we can't use precise coordinates
		pnt.preciseX = pnt.getEndPointX()
		pnt.preciseY = pnt.getEndPointY()
		next.pnt = pnt
		return true
	}

	private fun calculateGpxRouteResult(gctx: GpxRouteApproximation, gpxPoints: List<GpxPoint>) {
		val reg = RouteRegion()
		reg.initRouteEncodingRule(0, "highway", TurnPreparation.UNMATCHED_HIGHWAY_TYPE)
		var lastStraightLine: MutableList<KLatLon?>? = null
		var straightPointStart: GpxPoint? = null
		val progress = gctx.ctx.calculationProgress!!

		var i = 0
		while (i < gpxPoints.size && !progress.isCancelled) {
			val pnt = gpxPoints[i]
			val routeToTarget = pnt.routeToTarget
			if (routeToTarget != null && routeToTarget.isNotEmpty()) {
				var startPoint = pnt.getFirstRouteRes()!!.getStartPoint()
				if (lastStraightLine != null) {
					router.makeSegmentPointPrecise(gctx.ctx, pnt.getFirstRouteRes()!!, pnt.loc, true)
					startPoint = pnt.getFirstRouteRes()!!.getStartPoint()
					lastStraightLine.add(startPoint)
					addStraightLine(gctx, lastStraightLine, straightPointStart!!, reg)
					lastStraightLine = null
				}
				if (gctx.distFromLastPoint(startPoint) > 1 && RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION) {
					log.info(
						"?? gap of route point = %f, gap of actual gpxPoint = %f, %s ".format(
							gctx.distFromLastPoint(startPoint), gctx.distFromLastPoint(pnt.loc), pnt.loc
						)
					)
				}
				gctx.finalPoints.add(pnt)
				gctx.fullRoute.addAll(routeToTarget)
				i = pnt.targetInd
			} else {
				// add straight line from i -> i+1
				if (lastStraightLine == null) {
					lastStraightLine = ArrayList()
					if (gctx.getLastPoint() != null && gctx.finalPoints.size > 0) {
						val prev = gctx.finalPoints[gctx.finalPoints.size - 1]
						router.makeSegmentPointPrecise(gctx.ctx, prev.getLastRouteRes()!!, pnt.loc, false)
						lastStraightLine.add(gctx.getLastPoint())
					}
					straightPointStart = pnt
				}
				lastStraightLine.add(pnt.loc)
				i++
			}
		}

		if (lastStraightLine != null) {
			addStraightLine(gctx, lastStraightLine, straightPointStart!!, reg)
		}
		if (router.isUseGeometryBasedApproximation()) {
			RouteResultPreparation.prepareResult(gctx.ctx, gctx.fullRoute) // routing-based already did it
		} else {
			cleanDoubleJoints(gctx)
		}
		// clean turns to recalculate them
		cleanupResultAndAddTurns(gctx)
	}

	private fun cleanupResultAndAddTurns(gctx: GpxRouteApproximation) {
		RouteResultPreparation.validateAllPointsConnected(gctx.fullRoute)
		for (r in gctx.fullRoute) {
			r.setTurnType(null)
			r.clearDescription()
		}
		if (gctx.ctx.calculationProgress?.isCancelled != true) {
			TurnPreparation.prepareTurnResults(gctx.ctx, gctx.fullRoute)
		}
		for (r in gctx.fullRoute) {
			r.clearAttachedRoutes()
			r.clearPreattachedRoutes()
		}
	}

	private fun cleanDoubleJoints(gctx: GpxRouteApproximation) {
		val lookAhead = 4
		val progress = gctx.ctx.calculationProgress!!
		var i = 0
		while (i < gctx.fullRoute.size && !progress.isCancelled) {
			val s = gctx.fullRoute[i]
			var j = i + 2
			while (j <= i + lookAhead && j < gctx.fullRoute.size) {
				val e = gctx.fullRoute[j]
				if (e.getStartPoint() == s.getEndPoint()) {
					while ((--j) != i) {
						gctx.fullRoute.removeAt(j)
					}
					break
				}
				j++
			}
			i++
		}
	}

	private fun addStraightLine(gctx: GpxRouteApproximation, lastStraightLine: MutableList<KLatLon?>, strPnt: GpxPoint, reg: RouteRegion) {
		val rdo = RouteDataObject(reg)
		if (gctx.ctx.config.smoothenPointsNoRoute > 0) {
			simplifyDouglasPeucker(lastStraightLine, gctx.ctx.config.smoothenPointsNoRoute.toDouble(), 0, lastStraightLine.size - 1)
		}
		val s = lastStraightLine.size
		val x = KTIntArrayList(s)
		val y = KTIntArrayList(s)
		for (i in 0 until s) {
			val l = lastStraightLine[i]
			if (l != null) {
				val t = x.size() - 1
				x.add(KMapUtils.get31TileNumberX(l.longitude))
				y.add(KMapUtils.get31TileNumberY(l.latitude))
				if (t >= 0) {
					val dist = KMapUtils.squareRootDist31(x[t], y[t], x[t + 1], y[t + 1])
					gctx.routeDistanceUnmatched += dist.toInt()
				}
			}
		}
		rdo.pointsX = x.toArray()
		rdo.pointsY = y.toArray()
		rdo.types = intArrayOf(0)
		rdo.id = -1
		val routeToTarget = ArrayList<RouteSegmentResult>()
		strPnt.routeToTarget = routeToTarget
		strPnt.straightLine = true
		val line = RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1)
		line.setGpxPointIndex(strPnt.ind)
		routeToTarget.add(line)
		RouteResultPreparation.prepareResult(gctx.ctx, routeToTarget)

		// VIEW: comment to see road without straight connections
		gctx.finalPoints.add(strPnt)
		gctx.fullRoute.addAll(routeToTarget)
	}

	private fun simplifyDouglasPeucker(l: MutableList<KLatLon?>, eps: Double, start: Int, end: Int) {
		var dmax = -1.0
		var index = -1
		val s = l[start]!!
		val e = l[end]!!
		for (i in start + 1..end - 1) {
			val ip = l[i]!!
			val dist = KMapUtils.getOrthogonalDistance(
				ip.latitude, ip.longitude, s.latitude, s.longitude, e.latitude, e.longitude
			)
			if (dist > dmax) {
				dmax = dist
				index = i
			}
		}
		if (dmax >= eps) {
			simplifyDouglasPeucker(l, eps, start, index)
			simplifyDouglasPeucker(l, eps, index, end)
		} else {
			for (i in start + 1 until end) {
				l[i] = null
			}
		}
	}

	private fun initRoutingPoint(start: GpxPoint?, gctx: GpxRouteApproximation, distThreshold: Double): Boolean {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++
			val rsp = router.findRouteSegment(start.loc.latitude, start.loc.longitude, gctx.ctx, null, false)
			if (rsp != null) {
				if (KMapUtils.getDistance(rsp.getPreciseLatLon(), start.loc) < distThreshold) {
					start.pnt = rsp
				}
			}
		}
		return start != null && start.pnt != null
	}

	private fun findNextGpxPointWithin(gpxPoints: List<GpxPoint>, start: GpxPoint, dist: Double): GpxPoint? {
		// returns first point with that has slightly more than dist or last point
		val plus = if (dist > 0) 1 else -1
		var targetInd = start.ind + plus
		var target: GpxPoint? = null
		while (targetInd < gpxPoints.size && targetInd >= 0) {
			target = gpxPoints[targetInd]
			if (abs(target.cumDist - start.cumDist) > abs(dist)) {
				break
			}
			targetInd += plus
		}
		return target
	}

	private fun findGpxRouteSegment(
		gctx: GpxRouteApproximation, gpxPoints: List<GpxPoint>, start: GpxPoint, target: GpxPoint, prevRouteCalculated: Boolean
	): Boolean {
		var routeIsCorrect = false
		val startPnt = start.pnt
		val targetPnt = target.pnt
		if (startPnt != null && targetPnt != null) {
			val startCopy = RouteSegmentPoint(startPnt)
			val targetCopy = RouteSegmentPoint(targetPnt)
			start.pnt = startCopy
			target.pnt = targetCopy
			gctx.routeDistCalculations += (target.cumDist - start.cumDist).toInt()
			gctx.routeCalculations++
			val local = RoutingContext(gctx.ctx)
			val res = router.searchRouteInternalPrepare(local, startCopy, targetCopy, null)
			routeIsCorrect = res.isCorrect()
			var k = start.ind + 1
			while (routeIsCorrect && k < target.ind) {
				val ipoint = gpxPoints[k]
				if (!pointCloseEnough(gctx, ipoint, res)) {
					routeIsCorrect = false
				}
				k++
			}
			if (routeIsCorrect) {
				val firstSegment = res.detailed[0]
				// correct start point though don't change end point
				if (prevRouteCalculated) {
					if (firstSegment.getObject().id == startCopy.getRoad().id) {
						// start point is end point of prev route
						firstSegment.setStartPointIndex(startCopy.getSegmentEnd().toInt())
						if (firstSegment.getObject().getPointsLength() != startCopy.getRoad().getPointsLength()) {
							firstSegment.setObject(startCopy.getRoad())
						}
						if (firstSegment.getStartPointIndex() == firstSegment.getEndPointIndex()) {
							res.detailed.removeAt(0)
						}
					} else {
						// for native routing this is possible when point lies on intersection of 2 lines
						// solution here could be to pass to native routing id of the route
						// though it should not create any issue
						log.info("??? not found " + startCopy.getRoad().id + " instead " + firstSegment.getObject().id)
					}
				}
				for (seg in res.detailed) {
					seg.setGpxPointIndex(start.ind)
				}
				start.routeToTarget = res.detailed
				start.targetInd = target.ind
			}
			gctx.ctx.getVisitor()?.visitApproximatedSegments(res.detailed, start, target)
		}
		return routeIsCorrect
	}

	private fun isRouteCloseToGpxPoints(minPointApproximation: Float, gpxPoints: List<GpxPoint>, start: GpxPoint, next: GpxPoint): Boolean {
		var routeIsClose = true
		for (r in start.routeToTarget!!) {
			var st = r.getStartPointIndex()
			val end = r.getEndPointIndex()
			while (st != end) {
				val point = r.getPoint(st)
				var pointIsClosed = false
				val delta = 5
				val startInd = max(0, start.ind - delta)
				val nextInd = min(gpxPoints.size - 1, next.ind + delta)
				var k = startInd
				while (!pointIsClosed && k < nextInd) {
					pointIsClosed = pointCloseEnough(minPointApproximation, point, gpxPoints[k], gpxPoints[k + 1])
					k++
				}
				if (!pointIsClosed) {
					routeIsClose = false
					break
				}
				st += if (st < end) 1 else -1
			}
		}
		return routeIsClose
	}

	private fun pointCloseEnough(minPointApproximation: Float, point: KLatLon, gpxPoint: GpxPoint, gpxPointNext: GpxPoint): Boolean {
		val gpxPointLL = gpxPoint.pnt?.getPreciseLatLon() ?: gpxPoint.loc
		val gpxPointNextLL = gpxPointNext.pnt?.getPreciseLatLon() ?: gpxPointNext.loc
		val orthogonalDistance = KMapUtils.getOrthogonalDistance(
			point.latitude, point.longitude,
			gpxPointLL.latitude, gpxPointLL.longitude,
			gpxPointNextLL.latitude, gpxPointNextLL.longitude
		)
		return orthogonalDistance <= minPointApproximation
	}

	private fun pointCloseEnough(gctx: GpxRouteApproximation, ipoint: GpxPoint, res: RouteCalcResult): Boolean {
		val px = KMapUtils.get31TileNumberX(ipoint.loc.longitude)
		val py = KMapUtils.get31TileNumberY(ipoint.loc.latitude)
		var sqr = gctx.ctx.config.minPointApproximation.toDouble()
		sqr *= sqr
		for (sr in res.detailed) {
			var start = sr.getStartPointIndex()
			var end = sr.getEndPointIndex()
			if (sr.getStartPointIndex() > sr.getEndPointIndex()) {
				start = sr.getEndPointIndex()
				end = sr.getStartPointIndex()
			}
			for (i in start until end) {
				val r = sr.getObject()
				val pp = KMapUtils.getProjectionPoint31(
					px, py, r.getPoint31XTile(i), r.getPoint31YTile(i), r.getPoint31XTile(i + 1), r.getPoint31YTile(i + 1)
				)
				val currentsDist = RoutePlannerFrontEnd.squareDist(pp.x.toInt(), pp.y.toInt(), px, py)
				if (currentsDist <= sqr) {
					return true
				}
			}
		}
		return false
	}

	companion object {
		const val GPX_OSM_POINTS_MATCH_ALGORITHM = 1
		const val GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM = 2

		/** Which geometry-based algorithm [RoutePlannerFrontEnd.searchGpxRoute] runs when asked for one. */
		@JvmField
		var GPX_SEGMENT_ALGORITHM = GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM

		private val log = LoggerFactory.getLogger("GpxRouteApproximation")
	}
}
