package net.osmand.shared.routing

import net.osmand.shared.util.KMapUtils
import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.min

/**
 * Turning a route that has been found into the manoeuvres a driver is told about, and into the times
 * and speeds each segment is driven at.
 *
 * This runs over segments that are already decided - whichever planner produced them - so it is what
 * makes two platforms describe the same route the same way. The C++ router carries its own copy of
 * all of it in `routeResultPreparation.cpp`; while both exist, android is told by the java side and
 * iOS by the C++ one, and they can disagree about a manoeuvre on the same road.
 *
 * A copy of that preparation from `RouteResultPreparation` in OsmAnd-java, which stays there whole
 * for android and tools; this copy is for iOS. It carries the part that decides manoeuvres, lanes and
 * times, and leaves out the orchestration around it - the tile machinery, the area routing, the
 * debug printing and `convertFinalSegmentToResults`, which walks the java planner's own segments.
 */
object TurnPreparation {

	const val UNMATCHED_HIGHWAY_TYPE = "highway_unmatched"

	private const val TURN_DEGREE_MIN = 45.0

	// decrease speed proportionally from 15ms (50kmh)
	private const val SLOW_DOWN_SPEED_THRESHOLD = 15.0

	// reference speed 30ms (108kmh) - 2ms (7kmh)
	private const val SLOW_DOWN_SPEED = 2.0

	// ---- the manoeuvres ----

	/**
	 * Gives every segment the manoeuvre that leads onto it, then reconciles them with each other:
	 * lanes merged across junctions that are close together, straights that belong to the same
	 * intersection silenced, pairs of turns that are really a u turn joined up, and keeps that say
	 * nothing dropped.
	 */
	@JvmStatic
	fun prepareTurnResults(request: RoutingRequest, result: MutableList<RouteSegmentResult>) {
		for (i in result.indices) {
			val turnType = getTurnInfo(result, i, request.leftSideNavigation)
			result[i].setTurnType(turnType)
		}

		determineTurnsToMerge(request.leftSideNavigation, result)
		ignorePrecedingStraightsOnSameIntersection(request.leftSideNavigation, result)
		justifyUTurns(request.leftSideNavigation, result)
		avoidKeepForThroughMoving(result)
		muteAndRemoveTurns(result, request)
	}

	/** The manoeuvre for arriving on segment [i], from the angle, the tags and the lanes. */
	private fun getTurnInfo(result: List<RouteSegmentResult>, i: Int, leftSide: Boolean): TurnType? {
		if (i == 0) {
			return TurnType.valueOf(TurnType.C, false)
		}
		val roundaboutTurn = RoundaboutTurn(result, i, leftSide)
		if (roundaboutTurn.isRoundaboutExist()) {
			return roundaboutTurn.getRoundaboutType()
		}
		val prev = result[i - 1]
		val rr = result[i]
		var t: TurnType? = null
		// avoid small zigzags is covered at (search for "zigzags")
		var bearingDist = RouteSegmentResult.DIST_BEARING_DETECT
		if (UNMATCHED_HIGHWAY_TYPE == rr.getObject().getHighway()) {
			bearingDist = RouteSegmentResult.DIST_BEARING_DETECT_UNMATCHED
		}
		val mpi = KMapUtils.degreesDiff(
			prev.getBearingEnd(prev.getEndPointIndex(), min(prev.getDistance(), bearingDist)).toDouble(),
			rr.getBearingBegin(rr.getStartPointIndex(), min(rr.getDistance(), bearingDist)).toDouble()
		)

		val turnTag = getTurnString(rr)
		val twiceRoadPresent = twiceRoadPresent(result, i)
		if (turnTag != null) {
			val fromTag = TurnType.convertType(turnTag)
			if (!TurnType.isSlightTurn(fromTag)) {
				t = TurnType.valueOf(fromTag, leftSide)
				val lanes = TurnLanes.getTurnLanesInfo(prev, rr, t.value)
				t = TurnLanes.getActiveTurnType(lanes, leftSide, t)
				t.lanes = lanes
			} else if (fromTag != TurnType.C) {
				t = TurnLanes.attachKeepLeftInfoAndLanes(leftSide, prev, rr, twiceRoadPresent)
				if (t != null) {
					val mainTurnType = TurnType.valueOf(fromTag, leftSide)
					val lanes = t.lanes
					t = TurnLanes.getActiveTurnType(t.lanes, leftSide, mainTurnType)
					t.lanes = lanes
				}
			}
			if (t != null) {
				t.turnAngle = (-mpi).toFloat()
				return t
			}
		}

		if (mpi >= TURN_DEGREE_MIN) {
			// Slight turn detection here causes many false positives where drivers would expect a
			// "normal" TL. Best use limit-angle=TURN_DEGREE_MIN, this reduces TSL to the turn-lanes cases.
			t = if (mpi < TURN_DEGREE_MIN) {
				TurnType.valueOf(TurnType.TSLL, leftSide)
			} else if (mpi < 120) {
				TurnType.valueOf(TurnType.TL, leftSide)
			} else if (mpi < 150 || leftSide) {
				TurnType.valueOf(TurnType.TSHL, leftSide)
			} else {
				TurnType.valueOf(TurnType.TU, leftSide)
			}
			val lanes = TurnLanes.getTurnLanesInfo(prev, rr, t.value)
			t = TurnLanes.getActiveTurnType(lanes, leftSide, t)
			t.lanes = lanes
		} else if (mpi < -TURN_DEGREE_MIN) {
			t = if (mpi > -TURN_DEGREE_MIN) {
				TurnType.valueOf(TurnType.TSLR, leftSide)
			} else if (mpi > -120) {
				TurnType.valueOf(TurnType.TR, leftSide)
			} else if (mpi > -150 || !leftSide) {
				TurnType.valueOf(TurnType.TSHR, leftSide)
			} else {
				TurnType.valueOf(TurnType.TRU, leftSide)
			}
			val lanes = TurnLanes.getTurnLanesInfo(prev, rr, t.value)
			t = TurnLanes.getActiveTurnType(lanes, leftSide, t)
			t.lanes = lanes
		} else {
			t = TurnLanes.attachKeepLeftInfoAndLanes(leftSide, prev, rr, twiceRoadPresent)
		}
		if (t != null) {
			t.turnAngle = (-mpi).toFloat()
		}
		return t
	}

	private fun getTurnString(segment: RouteSegmentResult): String? = segment.getObject().getValue("turn")

	/**
	 * Merges the lanes of manoeuvres that follow each other closely enough to be one decision, so
	 * the driver is not told to be in a lane the next manoeuvre then leaves. Trunk and motorway get
	 * twice the distance, because the decision has to be made further out.
	 */
	private fun determineTurnsToMerge(leftside: Boolean, result: List<RouteSegmentResult>) {
		var nextSegment: RouteSegmentResult? = null
		var dist = 0.0
		for (i in result.indices.reversed()) {
			val currentSegment = result[i]
			val currentTurn = currentSegment.getTurnType()
			dist += currentSegment.getDistance()
			if (currentTurn == null || currentTurn.lanes == null) {
				// skip
			} else {
				var merged = false
				if (nextSegment != null) {
					val hw = currentSegment.getObject().getHighway()
					var mergeDistance = 200.0
					if (hw != null && (hw.startsWith("trunk") || hw.startsWith("motorway"))) {
						mergeDistance = 400.0
					}
					if (dist < mergeDistance) {
						TurnLanes.mergeTurnLanes(leftside, currentSegment, nextSegment)
						TurnLanes.inferCommonActiveLane(currentSegment.getTurnType()!!, nextSegment.getTurnType()!!)
						merged = true
						TurnLanes.replaceConfusingKeepTurnsWithLaneTurn(currentSegment, leftside)
					}
				}
				if (!merged) {
					val tt = currentSegment.getTurnType()!!
					TurnLanes.inferActiveTurnLanesFromTurn(tt, tt.value)
				}
				nextSegment = currentSegment
				dist = 0.0
			}
		}
	}

	/**
	 * Issue 2571: a "go straight" immediately before a real turn on a non-motorway is almost always
	 * the same intersection, so it is silenced rather than announced twice.
	 */
	private fun ignorePrecedingStraightsOnSameIntersection(leftside: Boolean, result: List<RouteSegmentResult>) {
		var nextSegment: RouteSegmentResult? = null
		var distanceToNextTurn = 999999.0
		for (i in result.indices.reversed()) {
			// Mark next "real" turn
			val next = nextSegment
			if (next?.getTurnType() != null && next.getTurnType()!!.value != TurnType.C && !isMotorway(next)) {
				if (distanceToNextTurn == 999999.0) {
					distanceToNextTurn = 0.0
				}
			}
			val currentSegment = result[i]
			// Identify preceding goStraights within distance limit and suppress
			distanceToNextTurn += currentSegment.getDistance()
			val turn = currentSegment.getTurnType()
			if (turn != null && turn.value == TurnType.C && distanceToNextTurn <= 200) {
				turn.isSkipToSpeak = true
			} else {
				nextSegment = currentSegment
				if (turn != null) {
					distanceToNextTurn = 999999.0
				}
			}
		}
	}

	/** Joins a left then left, or right then right, over a very short segment into one u turn. */
	private fun justifyUTurns(leftSide: Boolean, result: MutableList<RouteSegmentResult>) {
		var next: Int
		var i = 1
		while (i < result.size - 1) {
			next = i + 1
			val t = result[i].getTurnType()
			// justify turn
			if (t != null) {
				val jt = justifyUTurn(leftSide, result, i, t)
				if (jt != null) {
					result[i].setTurnType(jt)
					next = i + 2
				}
			}
			i = next
		}
	}

	private fun justifyUTurn(leftside: Boolean, result: List<RouteSegmentResult>, i: Int, t: TurnType): TurnType? {
		val tl = TurnType.isLeftTurnNoUTurn(t.value)
		val tr = TurnType.isRightTurnNoUTurn(t.value)
		if (tl || tr) {
			val tnext = result[i + 1].getTurnType()
			if (tnext != null && result[i].getDistance() < 50) {
				var ut = true
				if (i > 0) {
					val uTurn = KMapUtils.degreesDiff(
						result[i - 1].getBearingEnd().toDouble(),
						result[i + 1].getBearingBegin().toDouble()
					)
					if (abs(uTurn) < 120) {
						ut = false
					}
				}
				if (result[i - 1].getObject().getOneway() == 0 || result[i + 1].getObject().getOneway() == 0) {
					ut = false
				}
				if (getStreetName(result, i - 1, false) != getStreetName(result, i + 1, true)) {
					ut = false
				}
				if (ut) {
					tnext.isSkipToSpeak = true
					if (tl && TurnType.isLeftTurnNoUTurn(tnext.value)) {
						val tt = TurnType.valueOf(TurnType.TU, false)
						tt.lanes = t.lanes
						return tt
					} else if (tr && TurnType.isRightTurnNoUTurn(tnext.value)) {
						val tt = TurnType.valueOf(TurnType.TU, true)
						tt.lanes = t.lanes
						return tt
					}
				}
			}
		}
		return null
	}

	/** The road's name, falling back to the neighbour in the given direction when it has none. */
	private fun getStreetName(result: List<RouteSegmentResult>, i: Int, dir: Boolean): String? {
		var nm = result[i].getObject().getName()
		if (nm.isNullOrEmpty()) {
			if (!dir) {
				if (i > 0) {
					nm = result[i - 1].getObject().getName()
				}
			} else {
				if (i < result.size - 1) {
					nm = result[i + 1].getObject().getName()
				}
			}
		}
		return nm
	}

	/**
	 * Turns a "keep" into a plain "go straight" when every lane it offers goes straight anyway -
	 * there is nothing to keep away from, so there is nothing to say.
	 */
	private fun avoidKeepForThroughMoving(result: List<RouteSegmentResult>) {
		for (i in 1 until result.size) {
			val curr = result[i]
			val turnType = curr.getTurnType() ?: continue
			if (!turnType.keepLeft() && !turnType.keepRight()) {
				continue
			}
			if (isSwitchToLink(curr, result[i - 1])) {
				continue
			}
			if (isKeepTurn(turnType) && isHighSpeakPriority(curr)) {
				continue
			}
			if (TurnLanes.isForkByLanes(curr, result[i - 1])) {
				continue
			}
			val cnt = turnType.countTurnTypeDirections(TurnType.C, true)
			val cntAll = turnType.countTurnTypeDirections(TurnType.C, false)
			if (cnt > 0 && cnt == cntAll) {
				val newTurnType = TurnType(
					TurnType.C, turnType.exitOut, turnType.turnAngle,
					turnType.isSkipToSpeak, turnType.lanes,
					turnType.isPossibleLeftTurn, turnType.isPossibleRightTurn
				)
				curr.setTurnType(newTurnType)
			}
		}
	}

	/**
	 * Silences a manoeuvre whose active lanes all keep going, and drops it altogether when the road
	 * simply carries on - unless the profile asked for minor turns to be kept.
	 */
	private fun muteAndRemoveTurns(result: List<RouteSegmentResult>, request: RoutingRequest) {
		for (i in result.indices) {
			val curr = result[i]
			val turnType = curr.getTurnType()
			if (turnType?.lanes == null) {
				continue
			}
			val active = turnType.getActiveCommonLaneTurn()
			if (TurnType.isKeepDirectionTurn(active)) {
				if (i > 0 && isSwitchToLink(curr, result[i - 1])) {
					continue
				}
				if (isKeepTurn(turnType) && isHighSpeakPriority(curr)) {
					continue
				}
				turnType.isSkipToSpeak = true
				if (request.config.showMinorTurns) {
					continue
				}
				if (turnType.goAhead()) {
					val uniqDirections = turnType.countDirections()
					if (uniqDirections >= 3) {
						continue
					}
					val cnt = turnType.countTurnTypeDirections(TurnType.C, true)
					val cntAll = turnType.countTurnTypeDirections(TurnType.C, false)
					val lanesCnt = turnType.lanes!!.size
					if (cnt == cntAll && cnt >= 2 && (lanesCnt - cnt) <= 1) {
						curr.setTurnType(null)
					}
				}
			}
		}
	}

	private fun isMotorway(s: RouteSegmentResult): Boolean {
		val h = s.getObject().getHighway()
		return "motorway" == h || "motorway_link" == h || "trunk" == h || "trunk_link" == h
	}

	private fun isKeepTurn(t: TurnType): Boolean = t.keepRight() || t.keepLeft()

	private fun isSwitchToLink(curr: RouteSegmentResult, prev: RouteSegmentResult): Boolean {
		val c = curr.getObject().getHighway()
		val p = prev.getObject().getHighway()
		return c != null && c.contains("_link") && p != null && !p.contains("_link")
	}

	/** Whether this road is at least as important as something attached to it, so worth announcing. */
	private fun isHighSpeakPriority(curr: RouteSegmentResult): Boolean {
		val attachedRoutes = curr.getAttachedRoutes(curr.getStartPointIndex())
		val h = curr.getObject().getHighway()
		for (attach in attachedRoutes) {
			val c = attach.getObject().getHighway()
			if (TurnLanes.highwaySpeakPriority(h) >= TurnLanes.highwaySpeakPriority(c)) {
				return true
			}
		}
		return false
	}

	/** Whether the same road appears twice around this segment, which changes what the lanes mean. */
	private fun twiceRoadPresent(result: List<RouteSegmentResult>, i: Int): Boolean {
		if (i > 0 && i < result.size - 1) {
			val prev = result[i - 1]
			TurnLanes.getTurnLanesString(prev) ?: return false
			val curr = result[i]
			val next = result[i + 1]
			if (prev.getObject().getId() == curr.getObject().getId()) {
				// check if turn lanes allowed for next segment
				return next.getAttachedRoutes(next.getStartPointIndex()).isNotEmpty()
			} else {
				for (attach in curr.getAttachedRoutes(curr.getStartPointIndex())) {
					if (attach.getObject().getId() == prev.getObject().getId()) {
						// check if road the continue in attached roads
						return true
					}
				}
			}
		}
		return false
	}

	// ---- how long each segment takes ----

	/**
	 * The time and the effective speed of every segment, from the profile's speed for the road, the
	 * obstacles along it, and - for walking and cycling - the extra time an ascent costs.
	 */
	@JvmStatic
	fun calculateTimeSpeed(request: RoutingRequest, result: List<RouteSegmentResult>) {
		for (i in result.indices) {
			calculateTimeSpeed(request, result[i])
		}
	}

	@JvmStatic
	fun calculateTimeSpeed(request: RoutingRequest, rr: RouteSegmentResult) {
		// Naismith's/Scarf rules add additional travel time when moving uphill
		var useNaismithRule = false
		var scarfSeconds = 0.0 // Additional time as per Naismith/Scarf
		val currentRouter = request.getRouter() as GeneralRouter
		if (currentRouter.getProfile() == GeneralRouterProfile.PEDESTRIAN) {
			// PEDESTRIAN 1:7.92 based on https://en.wikipedia.org/wiki/Naismith%27s_rule (Scarf rule)
			scarfSeconds = (7.92f / currentRouter.getDefaultSpeed()).toDouble()
			useNaismithRule = true
		} else if (currentRouter.getProfile() == GeneralRouterProfile.BICYCLE) {
			// BICYCLE 1:8.2 based on https://pubmed.ncbi.nlm.nih.gov/17454539/ (Scarf's article)
			scarfSeconds = (8.2f / currentRouter.getDefaultSpeed()).toDouble()
			useNaismithRule = true
		}

		val road = rr.getObject()
		var distOnRoadToPass = 0.0
		var speed = request.getRouter().defineVehicleSpeed(road, rr.isForwardDirection()).toDouble()
		if (speed == 0.0) {
			speed = request.getRouter().getDefaultSpeed().toDouble()
		} else {
			if (speed > SLOW_DOWN_SPEED_THRESHOLD) {
				speed -= (speed / SLOW_DOWN_SPEED_THRESHOLD - 1) * SLOW_DOWN_SPEED
			}
		}
		val plus = rr.getStartPointIndex() < rr.getEndPointIndex()
		var next: Int
		var distance = 0.0

		// for Naismith/Scarf
		var heightDistanceArray: FloatArray? = null
		if (useNaismithRule) {
			road.calculateHeightArray()
			heightDistanceArray = road.heightDistanceArray
		}

		var j = rr.getStartPointIndex()
		while (j != rr.getEndPointIndex()) {
			next = if (plus) j + 1 else j - 1
			val d = measuredDist(
				road.getPoint31XTile(j), road.getPoint31YTile(j),
				road.getPoint31XTile(next), road.getPoint31YTile(next)
			)
			distance += d
			var obstacle = request.getRouter().defineObstacle(road, j, !plus).toDouble()
			if (obstacle < 0) {
				obstacle = 0.0
			}
			distOnRoadToPass += d / speed + obstacle // this is time in seconds

			// for Naismith/Scarf
			if (useNaismithRule) {
				val heightIndex = 2 * j + 1
				val nextHeightIndex = 2 * next + 1
				val heights = heightDistanceArray
				if (heights != null && heightIndex < heights.size && nextHeightIndex < heights.size) {
					val heightDiff = heights[nextHeightIndex] - heights[heightIndex]
					if (heightDiff > 0) { // ascent only
						// Naismith/Scarf rule: An ascent adds 7.92 times the hiking time its vertical
						// elevation gain takes to cover horizontally
						distOnRoadToPass += heightDiff * scarfSeconds
					}
				}
			}
			j = next
		}

		// last point turn time can be added
		rr.setDistance(distance.toFloat())
		rr.setSegmentTime(distOnRoadToPass.toFloat())
		if (distOnRoadToPass != 0.0) {
			// effective segment speed incl. obstacle and height effects
			rr.setSegmentSpeed((distance / distOnRoadToPass).toFloat())
		} else {
			rr.setSegmentSpeed(speed.toFloat())
		}
	}

	/** Recomputes the time and distance of segments whose speed is already known. */
	@JvmStatic
	fun recalculateTimeDistance(result: List<RouteSegmentResult>) {
		for (i in result.indices) {
			val rr = result[i]
			val road = rr.getObject()
			var distOnRoadToPass = 0.0
			val speed = rr.getSegmentSpeed().toDouble()
			if (speed == 0.0) {
				continue
			}
			val plus = rr.getStartPointIndex() < rr.getEndPointIndex()
			var next: Int
			var distance = 0.0
			var j = rr.getStartPointIndex()
			while (j != rr.getEndPointIndex()) {
				next = if (plus) j + 1 else j - 1
				val d = measuredDist(
					road.getPoint31XTile(j), road.getPoint31YTile(j),
					road.getPoint31XTile(next), road.getPoint31YTile(next)
				)
				distance += d
				distOnRoadToPass += d / speed // this is time in seconds
				j = next
			}
			rr.setSegmentTime(distOnRoadToPass.toFloat())
			rr.setSegmentSpeed(speed.toFloat())
			rr.setDistance(distance.toFloat())
		}
	}

	private fun measuredDist(x1: Int, y1: Int, x2: Int, y2: Int): Double =
		KMapUtils.getDistance(
			KMapUtils.get31LatitudeY(y1), KMapUtils.get31LongitudeX(x1),
			KMapUtils.get31LatitudeY(y2), KMapUtils.get31LongitudeX(x2)
		)

	// ---- tidying the list of segments ----

	/** Drops the stop signs of a minor road where the road being driven has right of way. */
	@JvmStatic
	fun filterMinorStops(seg: RouteSegmentResult): RouteSegmentResult {
		var stops: MutableList<Int>? = null
		val plus = seg.getStartPointIndex() < seg.getEndPointIndex()
		var next: Int

		var i = seg.getStartPointIndex()
		while (i != seg.getEndPointIndex()) {
			next = if (plus) i + 1 else i - 1
			val pointTypes = seg.getObject().getPointTypes(i)
			if (pointTypes != null) {
				for (j in pointTypes.indices) {
					if (pointTypes[j] == seg.getObject().region!!.stopMinor) {
						if (stops == null) {
							stops = ArrayList()
						}
						stops.add(i)
					}
				}
			}
			i = next
		}

		if (stops != null) {
			for (stop in stops) {
				for (attached in seg.getAttachedRoutes(stop)) {
					val attStopPriority = TurnLanes.highwaySpeakPriority(attached.getObject().getHighway())
					val segStopPriority = TurnLanes.highwaySpeakPriority(seg.getObject().getHighway())
					if (segStopPriority < attStopPriority) {
						seg.getObject().removePointType(stop, seg.getObject().region!!.stopSign)
						break
					}
				}
			}
		}
		return seg
	}

	/** Adds a segment to the route, joining it onto the previous one when it is the same road. */
	@JvmStatic
	fun addRouteSegmentToResult(
		request: RoutingRequest,
		result: MutableList<RouteSegmentResult>,
		res: RouteSegmentResult,
		reverse: Boolean
	) {
		if (res.getStartPointIndex() != res.getEndPointIndex()) {
			if (result.size > 0) {
				val last = result[result.size - 1]
				if (last.getObject().id == res.getObject().id && request.calculationMode != RouteCalculationMode.BASE) {
					if (combineTwoSegmentResult(res, last, reverse)) {
						return
					}
				}
			}
			result.add(res)
		}
	}

	private fun combineTwoSegmentResult(toAdd: RouteSegmentResult, previous: RouteSegmentResult, reverse: Boolean): Boolean {
		val ld = previous.getEndPointIndex() > previous.getStartPointIndex()
		val rd = toAdd.getEndPointIndex() > toAdd.getStartPointIndex()
		if (rd == ld) {
			if (toAdd.getStartPointIndex() == previous.getEndPointIndex() && !reverse) {
				previous.setEndPointIndex(toAdd.getEndPointIndex())
				previous.setRoutingTime(previous.getRoutingTime() + toAdd.getRoutingTime())
				return true
			} else if (toAdd.getEndPointIndex() == previous.getStartPointIndex() && reverse) {
				previous.setStartPointIndex(toAdd.getStartPointIndex())
				previous.setRoutingTime(previous.getRoutingTime() + toAdd.getRoutingTime())
				return true
			}
		}
		return false
	}
}
