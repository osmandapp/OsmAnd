package net.osmand.shared.routing

import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.format
import net.osmand.shared.extensions.nanoTime
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory

/**
 * The older geometry-based approximation: from the road segment a track point lies on, picks
 * for each of the next few track points the closest road point a few segments ahead, takes the
 * best of those as the next stretch of the route, and continues from the roads that meet at its
 * end. [GpxRouteApproximation.GPX_SEGMENT_ALGORITHM] selects it; the default is
 * [GpxMultiSegmentsApproximation].
 *
 * A copy of `GpxPointsMatchApproximation` in OsmAnd-java, which stays there for android and
 * tools; this copy is for iOS.
 */
// TO-THINK ? fix minor "Points are not connected" (~0.01m)
// TO-THINK ? think about "bearing" in addition to LOOKUP_AHEAD to keep sharp/loop-shaped gpx parts
// TO-THINK ? makePrecise for start / end segments (just check how correctly they are calculated)
class GpxPointsMatchApproximation {

	fun gpxApproximation(frontEnd: RoutePlannerFrontEnd, gctx: GpxRouteApproximation, gpxPoints: List<GpxPoint>): GpxRouteApproximation {
		val timeToCalculate = nanoTime()

		initGpxPointsXY31(gpxPoints)

		val minPointApproximation = gctx.ctx.config.minPointApproximation
		var currentPoint = findNextRoutablePoint(frontEnd, gctx, minPointApproximation.toDouble(), gpxPoints, 0)

		while (currentPoint != null && currentPoint.pnt != null) {
			var minDistSqrSegment = 0.0
			var fres: RouteSegmentResult? = null
			var minNextInd = -1
			for (j in currentPoint.ind + 1 until minOf(currentPoint.ind + LOOKUP_AHEAD, gpxPoints.size)) {
				val res = arrayOfNulls<RouteSegmentResult>(1)
				var minDistSqr = Double.POSITIVE_INFINITY
				val ps = gpxPoints[j]
				val currentPnt = currentPoint.pnt!!
				minDistSqr = minDistResult(res, minDistSqr, currentPnt, ps)
				val others = currentPnt.others
				if (others != null) {
					for (oth in others) {
						minDistSqr = minDistResult(res, minDistSqr, oth, ps)
					}
				}
				if (fres == null || minDistSqr <= minDistSqrSegment) {
					fres = res[0]
					minDistSqrSegment = minDistSqr
					minNextInd = j
				}
				if (KMapUtils.getDistance(currentPoint.loc, gpxPoints[j].loc) > minPointApproximation) {
					break // avoid shortcutting of loops
				}
			}
			if (minNextInd < 0) {
				break
			}
			if (minDistSqrSegment > minPointApproximation * minPointApproximation) {
				val nextIndex = currentPoint.ind + 1
				currentPoint = findNextRoutablePoint(frontEnd, gctx, minPointApproximation.toDouble(), gpxPoints, nextIndex)
				continue
			}
			val routeToTarget = ArrayList<RouteSegmentResult>()
			currentPoint.routeToTarget = routeToTarget
			fres!!.setGpxPointIndex(currentPoint.ind)
			routeToTarget.add(fres)
			currentPoint.targetInd = minNextInd

			currentPoint = gpxPoints[minNextInd] // next point

			var sg = gctx.ctx.loadRouteSegment(fres.getEndPointX(), fres.getEndPointY(), gctx.ctx.config.memoryLimitation)

			while (sg != null) {
				if (sg.getRoad().id != fres.getObject().id || sg.getSegmentEnd().toInt() != fres.getEndPointIndex()) {
					val p = RouteSegmentPoint(sg.getRoad(), sg.getSegmentStart().toInt(), sg.getSegmentEnd().toInt(), 0.0)
					val currentPnt = currentPoint.pnt
					if (currentPnt == null) {
						currentPoint.pnt = p
					} else {
						var others = currentPnt.others
						if (others == null) {
							others = ArrayList()
							currentPnt.others = others
						}
						others.add(p)
					}
				}
				sg = sg.getNext()
			}
		}
		val progress = gctx.ctx.calculationProgress
		if (progress != null) {
			progress.timeToCalculate = nanoTime() - timeToCalculate
		}
		if (RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION) {
			log.info(
				"Approximation took %.2f seconds (%d route points searched)".format(
					(nanoTime() - timeToCalculate) / 1.0e9, gctx.routePointsSearched
				)
			)
		}
		return gctx
	}

	private fun initRoutingPoint(frontEnd: RoutePlannerFrontEnd, gctx: GpxRouteApproximation, start: GpxPoint?, distThreshold: Double): Boolean {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++
			val rsp = frontEnd.findRouteSegment(start.loc.latitude, start.loc.longitude, gctx.ctx, null, false)
			if (rsp != null) {
				if (KMapUtils.getDistance(rsp.getPreciseLatLon(), start.loc) < distThreshold) {
					start.pnt = rsp
				}
			}
		}
		return start != null && start.pnt != null
	}

	private fun minDistResult(res: Array<RouteSegmentResult?>, minDistSqrArg: Double, pnt: RouteSegmentPoint, loc: GpxPoint): Double {
		var minDistSqr = minDistSqrArg
		var segmentEnd = -1
		var dist = 0.0
		val start = maxOf(0, pnt.getSegmentStart() - LOOKUP_AHEAD)
		val end = minOf(pnt.getRoad().getPointsLength(), pnt.getSegmentStart() + LOOKUP_AHEAD)
		for (i in start until end) {
			if (i == pnt.getSegmentStart().toInt()) {
				continue
			}
			val d = KMapUtils.squareDist31TileMetric(loc.x31, loc.y31, pnt.getRoad().getPoint31XTile(i), pnt.getRoad().getPoint31YTile(i))
			if (segmentEnd < 0 || d < dist) {
				segmentEnd = i
				dist = d
			}
		}
		dist += pnt.distToProj // distToProj > 0 is only for pnt(s) after findRouteSegment

		// Sometimes, more than 1 segment from (pnt+others) to next-gpx-point might have the same distance.
		// To make difference, a small fraction (1/1000) of real-segment-distance is added as "dilution" value.
		// Such a small dilution prevents from interfering with main searching of minimal distance to gpx-point.
		// https://test.osmand.net/map/?start=52.481439,13.386036&end=52.483094,13.386060&profile=rescuetrack&params=rescuetrack,geoapproximation#18/52.48234/13.38672
		dist += sumPntDistanceSqr(pnt, pnt.getSegmentStart().toInt(), segmentEnd) * DILUTE_BY_SEGMENT_DISTANCE

		if ((res[0] == null || dist < minDistSqr) && segmentEnd >= 0) {
			minDistSqr = dist
			res[0] = RouteSegmentResult(pnt.getRoad(), pnt.getSegmentStart().toInt(), segmentEnd)
		}
		return minDistSqr
	}

	private fun sumPntDistanceSqr(pnt: RouteSegmentPoint, startArg: Int, endArg: Int): Double {
		if (startArg == endArg) return 0.0
		var start = startArg
		var end = endArg
		if (start > end) {
			val swap = start
			start = end
			end = swap
		}
		var dist = 0.0
		for (i in start until end) {
			dist += KMapUtils.squareRootDist31(
				pnt.getRoad().getPoint31XTile(i), pnt.getRoad().getPoint31YTile(i),
				pnt.getRoad().getPoint31XTile(i + 1), pnt.getRoad().getPoint31YTile(i + 1)
			)
		}
		return dist * dist
	}

	private fun findNextRoutablePoint(
		frontEnd: RoutePlannerFrontEnd, gctx: GpxRouteApproximation, distThreshold: Double, gpxPoints: List<GpxPoint>, searchStart: Int
	): GpxPoint? {
		for (i in searchStart until gpxPoints.size) {
			if (initRoutingPoint(frontEnd, gctx, gpxPoints[i], distThreshold)) {
				return gpxPoints[i]
			}
		}
		return null
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

	companion object {
		private const val LOOKUP_AHEAD = 10
		private const val TEST_SHIFT_GPX_POINTS = false
		private const val DILUTE_BY_SEGMENT_DISTANCE = 0.001 // add a fraction of seg dist to pnt-to-gpx dist (0.001)

		// if (DEBUG_IDS.indexOf((int)(pnt.getRoad().getId() / 64)) >= 0) { ... }
		// private List<Integer> DEBUG_IDS = Arrays.asList(499257893, 126338247, 237816930); // good, wrong, turn

		private val log = LoggerFactory.getLogger("GpxPointsMatchApproximation")
	}
}
