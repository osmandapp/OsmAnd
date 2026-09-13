package net.osmand.shared.routing

import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KMapUtils
import kotlin.math.abs

/**
 * The manoeuvre for a roundabout: which exit to take, and at what angle the driver leaves it.
 *
 * A roundabout is not one turn but a stretch of road with several ways off it, so the exit has to be
 * counted by walking the segments and looking at what is attached to each point. A mini roundabout
 * is the same idea without the geometry - a single point tagged `highway=mini_roundabout` on both
 * the road arrived on and the road left on - and the exit is counted from the roads attached there.
 *
 * The turn angle is worked out twice and the sharper answer wins. The straightforward one is the
 * difference between the road left on and the road arrived on; it breaks down near 180 degrees,
 * where it cannot tell which way round the circle was driven, so beyond 120 the angle is taken
 * across the circle instead - antinormal at the entrance, normal at the exit.
 */
class RoundaboutTurn(
	private val routeSegmentResults: List<RouteSegmentResult>,
	private val iteration: Int,
	private val leftSide: Boolean
) {

	private val current: RouteSegmentResult? =
		if (routeSegmentResults.size > iteration) routeSegmentResults[iteration] else null
	private val prev: RouteSegmentResult? =
		if (iteration > 0 && routeSegmentResults.size > iteration) routeSegmentResults[iteration - 1] else null
	private val roundabout: Boolean = current != null && current.getObject().roundabout()
	private val prevRoundabout: Boolean = prev != null && prev.getObject().roundabout()
	private val miniRoundabout: Boolean = isMiniRoundabout(prev, current)

	fun isRoundaboutExist(): Boolean = roundabout || miniRoundabout || prevRoundabout

	fun getRoundaboutType(): TurnType? {
		if (prev == null || current == null) {
			return null
		}
		if (prevRoundabout) {
			// already analyzed!
			return null
		}
		if (roundabout) {
			return processRoundaboutTurn()
		}
		if (miniRoundabout) {
			return processMiniRoundaboutTurn()
		}
		return null
	}

	private fun isMiniRoundabout(prev: RouteSegmentResult?, current: RouteSegmentResult?): Boolean {
		if (prev == null || current == null) {
			return false
		}
		val prevTypes = prev.getObject().getPointTypes(prev.getEndPointIndex())
		val currentTypes = current.getObject().getPointTypes(current.getStartPointIndex())
		if (prevTypes != null && currentTypes != null) {
			val miniType = prev.getObject().region!!.searchRouteEncodingRule("highway", "mini_roundabout")
			if (miniType < 0) {
				return false
			}
			var p = false
			var c = false
			for (t in prevTypes) {
				if (t == miniType) {
					p = true
					break
				}
			}
			for (t in currentTypes) {
				if (t == miniType) {
					c = true
					break
				}
			}
			return p && c
		}
		return false
	}

	private fun processRoundaboutTurn(): TurnType {
		var exit = 1
		var last: RouteSegmentResult = current!!
		val firstRoundabout: RouteSegmentResult = current
		var lastRoundabout: RouteSegmentResult = current
		val turnAngles = ArrayList<Float>()
		for (j in iteration until routeSegmentResults.size) {
			val rnext = routeSegmentResults[j]
			last = rnext
			if (rnext.getObject().roundabout()) {
				lastRoundabout = rnext
				val plus = rnext.getStartPointIndex() < rnext.getEndPointIndex()
				var k = rnext.getStartPointIndex()
				// first exit could be immediately after roundabout enter, so k is not advanced here
				while (k != rnext.getEndPointIndex()) {
					val attachedRoads = rnext.getAttachedRoutes(k).size
					if (attachedRoads > 0) {
						exit++
						turnAngles.add(calculateRoundaboutTurnAngle(rnext, firstRoundabout, rnext, k))
					}
					k = if (plus) k + 1 else k - 1
				}
			} else {
				break
			}
		}
		// combine all roundabouts
		val t = TurnType.getExitTurn(exit, 0f, leftSide)
		t.turnAngle = calculateRoundaboutTurnAngle(last, firstRoundabout, lastRoundabout, -1)
		t.otherTurnAngles = turnAngles
		return t
	}

	private fun calculateRoundaboutTurnAngle(
		last: RouteSegmentResult,
		firstRoundabout: RouteSegmentResult,
		lastRoundabout: RouteSegmentResult,
		ind: Int
	): Float {
		// usually covers more than expected
		val turnAngleBasedOnOutRoads = KMapUtils.degreesDiff(
			(if (ind < 0) last.getBearingBegin() else last.getBearingBegin(ind, RouteSegmentResult.DIST_BEARING_DETECT)).toDouble(),
			prev!!.getBearingEnd().toDouble()
		).toFloat()
		// Angle based on circle method tries
		// 1. to calculate antinormal to roundabout circle on roundabout entrance and
		// 2. normal to roundabout circle on roundabout exit
		// 3. calculate angle difference
		// This method doesn't work if you go from S to N touching only 1 point of roundabout,
		// but it is very important to identify very sharp or very large angle to understand did you
		// pass whole roundabout or small entrance
		val turnAngleBasedOnCircle = (-KMapUtils.degreesDiff(
			firstRoundabout.getBearingBegin().toDouble(),
			(if (ind < 0) last.getBearingEnd() else lastRoundabout.getBearingEnd(ind, RouteSegmentResult.DIST_BEARING_DETECT) + 180).toDouble()
		)).toFloat()
		return if (abs(turnAngleBasedOnOutRoads) > 120) {
			// correctly identify if angle is +- 180, so we approach from left or right side
			turnAngleBasedOnCircle
		} else {
			turnAngleBasedOnOutRoads
		}
	}

	private fun processMiniRoundaboutTurn(): TurnType? {
		val attachedRoutes = current!!.getAttachedRoutes(current.getStartPointIndex())
		val clockwise = current.getObject().isClockwise(leftSide)
		if (!KAlgorithms.isEmpty(attachedRoutes)) {
			val rs = calculateSimpleRoadSplitStructure(attachedRoutes)
			val rightAttaches = rs.roadsOnRight
			val leftAttaches = rs.roadsOnLeft
			var exit = 1
			exit += if (clockwise) leftAttaches else rightAttaches
			val t = TurnType.getExitTurn(exit, 0f, leftSide)
			val turnAngleBasedOnOutRoads =
				KMapUtils.degreesDiff(current.getBearingBegin().toDouble(), prev!!.getBearingEnd().toDouble()).toFloat()
			val turnAngleBasedOnCircle =
				(-KMapUtils.degreesDiff(current.getBearingBegin().toDouble(), (prev.getBearingEnd() + 180).toDouble())).toFloat()
			t.turnAngle = if (abs(turnAngleBasedOnOutRoads) > 120) turnAngleBasedOnCircle else turnAngleBasedOnOutRoads
			return t
		}
		return null
	}

	private fun calculateSimpleRoadSplitStructure(attachedRoutes: List<RouteSegmentResult>): RoadSplitStructure {
		// java computed these in float and widened, because normalizeDegrees360 takes a float
		val prevAngle = KMapUtils.normalizeDegrees360(prev!!.getBearingBegin() - 180).toDouble()
		val currentAngle = KMapUtils.normalizeDegrees360(current!!.getBearingBegin()).toDouble()
		val rs = RoadSplitStructure()
		for (attached in attachedRoutes) {
			val attachedAngle = KMapUtils.normalizeDegrees360(attached.getBearingBegin()).toDouble()
			val rightSide: Boolean
			if (prevAngle > currentAngle) {
				rightSide = attachedAngle > currentAngle && attachedAngle < prevAngle
			} else {
				val leftSide = attachedAngle > prevAngle && attachedAngle < currentAngle
				rightSide = !leftSide
			}

			if (rightSide) {
				rs.roadsOnRight++
			} else {
				rs.roadsOnLeft++
			}
		}
		return rs
	}
}
