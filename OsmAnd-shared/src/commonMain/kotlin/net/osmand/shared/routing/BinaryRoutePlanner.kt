package net.osmand.shared.routing

import net.osmand.shared.extensions.format
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.collections.KPriorityQueue
import net.osmand.shared.util.collections.KTLongObjectMap
import net.osmand.shared.util.collections.KTLongObjectLookup
import kotlin.experimental.ExperimentalObjCName
import kotlin.jvm.JvmField
import kotlin.native.ObjCName
import kotlin.jvm.JvmStatic
import kotlin.math.PI
import kotlin.math.abs

/**
 * The A* search over the road graph: two open sets, one growing from the start and one from the
 * target, settled in turn until a segment is reached from both sides.
 *
 * Everything it knows about the roads comes through [RoutingContext.loadRouteSegment], which hands
 * it the segments of every road at a point, and through the vehicle router, which prices them. A
 * segment's cost is time in seconds; the heuristic is the straight line at the profile's maximum
 * speed, or the time along a precalculated route when the context has one.
 *
 * A copy of `net.osmand.router.BinaryRoutePlanner`, which stays in OsmAnd-java for android and
 * tools; this copy is for iOS, where it is to replace the C++ router. The open sets are
 * [KPriorityQueue], which orders equal costs the way `java.util.PriorityQueue` does, so that the two
 * planners find the same route where two are equally good.
 */
class BinaryRoutePlanner {

	private class SegmentsComparator : Comparator<RouteSegmentCost> {
		override fun compare(o1: RouteSegmentCost, o2: RouteSegmentCost): Int {
			return o1.cost.toDouble().compareTo(o2.cost.toDouble())
		}
	}

	internal class RouteSegmentCost(@JvmField val segment: RouteSegment, ctx: RoutingContext) {
		@JvmField
		val cost: Float = cost(segment.distanceFromStart, segment.distanceToEnd, ctx)

		override fun toString(): String = "%.2f %s".format(cost, segment)
	}

	/**
	 * Calculate route between start.segmentEnd and end.segmentStart (using A* algorithm)
	 * return list of segments
	 */
	fun searchRouteInternal(
		ctx: RoutingContext, start: RouteSegmentPoint?, end: RouteSegmentPoint?,
		boundaries: KTLongObjectLookup<RouteSegment>?
	): FinalRouteSegment? {
		return searchRouteInternal(ctx, start, end, boundaries, null, null)
	}

	/**
	 * @param visitedDirectOut  when given, it is used as the visited set of the forward search, so the
	 *                          caller keeps the search tree after the call (see HHAlternativeRoutes)
	 * @param visitedOppositeOut the same for the backward search
	 */
	fun searchRouteInternal(
		ctx: RoutingContext, start: RouteSegmentPoint?, end: RouteSegmentPoint?,
		boundaries: KTLongObjectLookup<RouteSegment>?, visitedDirectOut: KTLongObjectMap<RouteSegment>?,
		visitedOppositeOut: KTLongObjectMap<RouteSegment>?
	): FinalRouteSegment? {
		// measure time
		ctx.memoryOverhead = 1000
		// Initializing priority queue to visit way segments
		val graphDirectSegments = KPriorityQueue<RouteSegmentCost>(50, SegmentsComparator())
		val graphReverseSegments = KPriorityQueue<RouteSegmentCost>(50, SegmentsComparator())
		// Set to not visit one segment twice (stores road.id << X + segmentStart)
		val visitedDirectSegments = visitedDirectOut ?: KTLongObjectMap()
		val visitedOppositeSegments = visitedOppositeOut ?: KTLongObjectMap()
		initQueuesWithStartEnd(ctx, start, end, graphDirectSegments, graphReverseSegments)

		val onlyBackward = ctx.getPlanRoadDirection() < 0
		val onlyForward = ctx.getPlanRoadDirection() > 0
		// Extract & analyze segment with min(f(x)) from queue while final segment is not found
		var forwardSearch = !onlyForward

		var finalSegment: FinalRouteSegment? = null
		ctx.dijkstraMode = if (end == null) 1 else (if (start == null) -1 else 0)
		if (ctx.dijkstraMode == 1) {
			start!!.others = null
			forwardSearch = true
		} else if (ctx.dijkstraMode == -1) {
			end!!.others = null
			forwardSearch = false
		}
		var graphSegments = if (forwardSearch) graphDirectSegments else graphReverseSegments
		var minCost = floatArrayOf(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
		while (!graphSegments.isEmpty()) {
			val cst = graphSegments.poll()!!
			val segment = cst.segment
			val visitedCnt = (if (start != null) visitedDirectSegments.size else 0) + (if (end != null) visitedOppositeSegments.size else 0)
			// use accumulative approach
			ctx.memoryOverhead = visitedCnt * STANDARD_ROAD_VISITED_OVERHEAD +
					(graphDirectSegments.size + graphReverseSegments.size) * STANDARD_ROAD_IN_QUEUE_OVERHEAD
			if (TRACE_ROUTING) {
				printRoad(">", segment, !forwardSearch)
			}
			if (ctx.config.MAX_VISITED > 0 && visitedCnt > ctx.config.MAX_VISITED) {
				if (finalSegment != null) {
					// we can mark incomplete
				}
				break
			}
			var skipSegment = false
			if (segment is FinalRouteSegment) {
				if (TRACE_ROUTING) {
					println(" >>FINAL segment: $segment")
				}

				if (ctx.dijkstraMode != 0) {
					if (finalSegment == null) {
						finalSegment = MultiFinalRouteSegment(segment)
					}
					(finalSegment as MultiFinalRouteSegment).all.add(segment)
					val visitedSegments = if (forwardSearch) visitedDirectSegments else visitedOppositeSegments
					if (!visitedSegments.containsKey(calculateRoutePointId(segment))) {
						visitedSegments.put(calculateRoutePointId(segment), segment)
					}
					skipSegment = true
				} else if (ctx.config.altHorizon > 0) {
					// alternative routes are read off the two trees, so the search must not stop at the
					// first meeting point - it keeps the cheapest one and settles on (see altHorizon)
					if (finalSegment == null || segment.distanceFromStart < finalSegment.distanceFromStart) {
						finalSegment = segment
					}
					skipSegment = true
				} else {
					finalSegment = segment
					break
				}
			}

			if (ctx.memoryOverhead > ctx.config.memoryLimitation * 0.9) {
				ctx.throwNotEnoughMemory()
			}
			ctx.calculationProgress?.let { it.visitedSegments++ }
			val visited = (if (forwardSearch) visitedDirectSegments else visitedOppositeSegments)
				.containsKey(calculateRoutePointId(segment))
			if (visited) {
				if (TRACE_ROUTING) {
					println("  " + segment.segEnd + ">> Already visited by minimum")
				}
				skipSegment = true
			} else if (cst.cost + 5.0 < minCost[if (forwardSearch) 1 else 0] && ASSERT_CHECKS && ctx.calculationMode != RouteCalculationMode.COMPLEX) {
				// squareRootDist doesn't follow Triangle-inequality and it breaks A* algorithm. Maximum error on the optimal route could be constant (5.0)
				if (ctx.config.heuristicCoefficient <= 1) {
					throw IllegalStateException(cst.cost.toString() + " < ???  " + minCost[if (forwardSearch) 1 else 0])
				}
			} else {
				minCost[if (forwardSearch) 1 else 0] = cst.cost
			}
			if (!skipSegment) {
				if (forwardSearch) {
					val doNotAddIntersections = onlyBackward
					processRouteSegment(
						ctx, false, graphDirectSegments, visitedDirectSegments, segment,
						visitedOppositeSegments, boundaries, doNotAddIntersections
					)
				} else {
					val doNotAddIntersections = onlyForward
					processRouteSegment(
						ctx, true, graphReverseSegments, visitedOppositeSegments, segment,
						visitedDirectSegments, boundaries, doNotAddIntersections
					)
				}
			}
			updateCalculationProgress(ctx, graphDirectSegments, graphReverseSegments)

			var reiterate = false
			reiterate = reiterate or checkIfGraphIsEmpty(
				ctx, ctx.getPlanRoadDirection() <= 0, true, graphReverseSegments, end,
				visitedOppositeSegments, "Route is not found to selected target point."
			)
			reiterate = reiterate or checkIfGraphIsEmpty(
				ctx, ctx.getPlanRoadDirection() >= 0, false, graphDirectSegments, start,
				visitedDirectSegments, "Route is not found from selected start point."
			)
			if (reiterate) {
				minCost = floatArrayOf(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
			}
			if (ctx.planRouteIn2Directions()) {
				// initial iteration make in 2 directions
				if (visitedDirectSegments.isEmpty() && !graphDirectSegments.isEmpty()) {
					forwardSearch = true
				} else if (visitedOppositeSegments.isEmpty() && !graphReverseSegments.isEmpty()) {
					forwardSearch = false
				} else if (graphDirectSegments.isEmpty() || graphReverseSegments.isEmpty()) {
					// can't proceed any more - check if final already exist
					graphSegments = if (graphDirectSegments.isEmpty()) graphReverseSegments else graphDirectSegments
					if (finalSegment == null) {
						while (!graphSegments.isEmpty()) {
							val pc = graphSegments.poll()!!
							if (pc.segment is FinalRouteSegment) {
								finalSegment = pc.segment
								break
							}
						}
					}
					return finalSegment
				} else {
					val fw = graphDirectSegments.peek()!!.segment
					val bw = graphReverseSegments.peek()!!.segment
					forwardSearch = cost(fw.distanceFromStart, fw.distanceToEnd, ctx).toDouble()
						.compareTo(cost(bw.distanceFromStart, bw.distanceToEnd, ctx).toDouble()) <= 0
				}
			} else {
				// different strategy : use one directional graph
				forwardSearch = onlyForward
				if (onlyBackward && !graphDirectSegments.isEmpty()) {
					forwardSearch = true
				}
				if (onlyForward && !graphReverseSegments.isEmpty()) {
					forwardSearch = false
				}
			}

			graphSegments = if (forwardSearch) graphDirectSegments else graphReverseSegments
			// check if interrupted
			val progress = ctx.calculationProgress
			if (progress != null && progress.isCancelled) {
				throw RouteCalculationInterruptedException("Route calculation interrupted")
			}
			if (ctx.config.altHorizon > 0 && finalSegment != null && leftAltHorizon(ctx, finalSegment, graphDirectSegments, graphReverseSegments)) {
				break
			}
		}
		val progress = ctx.calculationProgress
		if (progress != null) {
			progress.visitedDirectSegments += visitedDirectSegments.size
			progress.visitedOppositeSegments += visitedOppositeSegments.size
			progress.directQueueSize += graphDirectSegments.size // Math.max(ctx.directQueueSize, graphDirectSegments.size());
			progress.oppositeQueueSize += graphReverseSegments.size
		}
		return finalSegment
	}

	/**
	 * Everything still in the queues costs more than the band the alternatives may live in, so no
	 * route through a node settled from here on could be proposed anyway.
	 */
	private fun leftAltHorizon(
		ctx: RoutingContext, finalSegment: FinalRouteSegment,
		direct: KPriorityQueue<RouteSegmentCost>, reverse: KPriorityQueue<RouteSegmentCost>
	): Boolean {
		if (direct.isEmpty() || reverse.isEmpty()) {
			return true
		}
		return direct.peek()!!.cost + reverse.peek()!!.cost > finalSegment.distanceFromStart * (1 + ctx.config.altHorizon)
	}

	private fun checkIfGraphIsEmpty(
		ctx: RoutingContext, allowDirection: Boolean, reverseWaySearch: Boolean,
		graphSegments: KPriorityQueue<RouteSegmentCost>, pnt: RouteSegmentPoint?, visited: KTLongObjectMap<RouteSegment>,
		msg: String
	): Boolean {
		if (allowDirection && graphSegments.isEmpty()) {
			val others = pnt!!.others
			if (others != null) {
				val pntIterator = others.iterator()
				while (pntIterator.hasNext()) {
					val next = pntIterator.next()
					pntIterator.remove()
					initEdgeSegment(ctx, next, true, graphSegments, visited, reverseWaySearch)
					initEdgeSegment(ctx, next, false, graphSegments, visited, reverseWaySearch)
					if (!graphSegments.isEmpty()) {
						println("Reiterate point with new " + (if (!reverseWaySearch) "start " else "destination ") + next.getRoad())
						return true
					}
				}
				if (graphSegments.isEmpty()) {
					throw IllegalArgumentException(msg)
				}
			}
		}
		return false
	}

	internal fun initEdgeSegment(
		ctx: RoutingContext, pnt: RouteSegmentPoint?, originalDir: Boolean,
		graphSegments: KPriorityQueue<RouteSegmentCost>, visited: KTLongObjectMap<RouteSegment>?, reverseSearchWay: Boolean
	): RouteSegment? {
		if (pnt == null) {
			return null
		}
		// originalDir = true: start points & end points equal to pnt
		var seg = ctx.loadRouteSegment(
			if (originalDir) pnt.getStartPointX() else pnt.getEndPointX(),
			if (originalDir) pnt.getStartPointY() else pnt.getEndPointY(), 0, reverseSearchWay
		)
		while (seg != null) {
			if (seg.getRoad().getId() == pnt.getRoad().getId() &&
				(seg.getSegmentStart() == (if (originalDir) pnt.getSegmentStart() else pnt.getSegmentEnd()))
			) {
				break
			}
			seg = seg.getNext()
		}
		if (seg!!.getSegmentStart() != (if (originalDir) pnt.getSegmentStart() else pnt.getSegmentEnd())
			|| seg.getSegmentEnd() != (if (originalDir) pnt.getSegmentEnd() else pnt.getSegmentStart())
		) {
			seg = seg.initRouteSegment(!seg.isPositive())
		}
		if (originalDir && (seg!!.getSegmentStart() != pnt.getSegmentStart() || seg.getSegmentEnd() != pnt.getSegmentEnd())) {
			throw IllegalStateException()
		}
		if (!originalDir && (seg!!.getSegmentStart() != pnt.getSegmentEnd() || seg.getSegmentEnd() != pnt.getSegmentStart())) {
			throw IllegalStateException()
		}
		if (!originalDir && ctx.config.initialDirection == null && ctx.config.penaltyForReverseDirection < 0) {
			// special case for single side spread point-dijkstra
			return null
		}
		seg!!.setParentRoute(RouteSegment.NULL)
		val dist = -calculatePreciseStartTime(ctx, pnt.preciseX, pnt.preciseY, seg)
		// full segment length will be added on first visit
		seg.distanceFromStart = dist

		val initialDirection = ctx.config.initialDirection
		val targetDirection = ctx.config.targetDirection
		if ((!reverseSearchWay && initialDirection != null) || (reverseSearchWay && targetDirection != null)) {
			// for start : f(start) = g(start) + h(start) = 0 + h(start) = h(start)
			// mark here as positive for further check
			val plusDir = seg.getRoad().directionRoute(seg.getSegmentStart().toInt(), seg.isPositive())
			val diff = plusDir - (if (reverseSearchWay) targetDirection!! else initialDirection!!)
			if (abs(KMapUtils.alignAngleDifference(diff - PI)) <= PI / 3) {
				seg.distanceFromStart += ctx.config.penaltyForReverseDirection.toFloat()
			}
		}
		if (checkMovementAllowed(ctx, reverseSearchWay, seg)) {
			if (visited == null || !visited.containsKey(calculateRoutePointId(seg))) {
				seg.distanceToEnd = estimatedDistance(seg, reverseSearchWay, ctx)
				graphSegments.add(RouteSegmentCost(seg, ctx))
				return seg
			}
		}
		return null
	}

	fun calculatePreciseStartTime(ctx: RoutingContext, projX: Int, projY: Int, seg: RouteSegment): Float {
		// compensate first segment difference to mid point (length) https://github.com/osmandapp/OsmAnd/issues/14148
		val fullTime = calcRoutingSegmentTimeOnlyDist(ctx.getRouter(), seg).toDouble()
		val full = squareRootDist(seg.getStartPointX(), seg.getStartPointY(), seg.getEndPointX(), seg.getEndPointY()) + 0.01 // avoid div 0
		val fromStart = squareRootDist(projX, projY, seg.getStartPointX(), seg.getStartPointY())
		return (fromStart / full * fullTime).toFloat()
	}

	private fun initQueuesWithStartEnd(
		ctx: RoutingContext, start: RouteSegmentPoint?, end: RouteSegmentPoint?,
		graphDirectSegments: KPriorityQueue<RouteSegmentCost>, graphReverseSegments: KPriorityQueue<RouteSegmentCost>
	) {
		ctx.precalculatedRouteDirection?.updatePreciseStartEnd(
			start?.preciseX ?: 0, start?.preciseY ?: 0,
			end?.preciseX ?: 0, end?.preciseY ?: 0
		)
		if (start != null) {
			ctx.startX = start.preciseX
			ctx.startY = start.preciseY
		}
		if (end != null) {
			ctx.targetX = end.preciseX
			ctx.targetY = end.preciseY
		}
		val startPos = initEdgeSegment(ctx, start, true, graphDirectSegments, null, false)
		val startNeg = initEdgeSegment(ctx, start, false, graphDirectSegments, null, false)
		val endPos = initEdgeSegment(ctx, end, true, graphReverseSegments, null, true)
		val endNeg = initEdgeSegment(ctx, end, false, graphReverseSegments, null, true)
		if (TRACE_ROUTING) {
			printRoad("Initial segment start positive: ", startPos, false)
			printRoad("Initial segment start negative: ", startNeg, false)
			printRoad("Initial segment end positive: ", endPos, false)
			printRoad("Initial segment end negative: ", endNeg, false)
		}
	}

	private fun updateCalculationProgress(
		ctx: RoutingContext, graphDirectSegments: KPriorityQueue<RouteSegmentCost>,
		graphReverseSegments: KPriorityQueue<RouteSegmentCost>
	) {
		val progress = ctx.calculationProgress ?: return
		progress.reverseSegmentQueueSize = graphReverseSegments.size
		progress.directSegmentQueueSize = graphDirectSegments.size
		if (!graphDirectSegments.isEmpty() && ctx.getPlanRoadDirection() >= 0) {
			val peek = graphDirectSegments.peek()!!.segment
			progress.distanceFromBegin = kotlin.math.max(peek.distanceFromStart, progress.distanceFromBegin)
			progress.directDistance = peek.distanceFromStart + peek.distanceToEnd
		}
		if (!graphReverseSegments.isEmpty() && ctx.getPlanRoadDirection() <= 0) {
			val peek = graphReverseSegments.peek()!!.segment
			progress.distanceFromEnd = kotlin.math.max(peek.distanceFromStart + peek.distanceToEnd, progress.distanceFromEnd)
			progress.reverseDistance = peek.distanceFromStart + peek.distanceToEnd
		}
	}

	private fun printRoad(prefix: String, segment: RouteSegment?, reverseWaySearch: Boolean?) {
		var p = ""
		if (reverseWaySearch != null) {
			p = if (reverseWaySearch) "B" else "F"
		}
		if (segment == null) {
			println("$p$prefix Segment=null")
		} else {
			val parent = segment.parentRoute
			val pr = if (parent != null) " pend=" + parent.segEnd + " parent=" + parent.road else ""
			println(
				p + prefix + "" + segment.road + " ind=" + segment.getSegmentStart() + "->" + segment.getSegmentEnd() +
						" ds=" + segment.distanceFromStart + " es=" + segment.distanceToEnd + pr
			)
		}
	}

	private fun estimatedDistance(seg: RouteSegment, rev: Boolean, ctx: RoutingContext): Float {
		val x = seg.getStartPointX() / 2 + seg.getEndPointX() / 2
		val y = seg.getStartPointY() / 2 + seg.getEndPointY() / 2
		val distance = squareRootDist(x, y, if (rev) ctx.startX else ctx.targetX, if (rev) ctx.startY else ctx.targetY)
		return (distance / ctx.getRouter().getMaxSpeed()).toFloat()
	}

	private fun calculateRouteSegmentTime(ctx: RoutingContext, reverseWaySearch: Boolean, segment: RouteSegment): Double {
		val road = segment.getRoad()
		// store <segment> in order to not have unique <segment, direction> in visitedSegments
		val segmentInd = if (reverseWaySearch) segment.getSegmentStart() else segment.getSegmentEnd()
		val prevSegmentInd = if (!reverseWaySearch) segment.getSegmentStart() else segment.getSegmentEnd()

		val distTimeOnRoadToPass = calcRoutingSegmentTimeOnlyDist(ctx.getRouter(), segment).toDouble()
		// calculate possible obstacle plus time
		var obstacle = 0.0
		if (segment.distanceFromStart >= 0 || !reverseWaySearch) { // ignore last point for reverse
			obstacle = ctx.getRouter().defineRoutingObstacle(road, segmentInd.toInt(), prevSegmentInd > segmentInd).toDouble()
		}
		if (obstacle < 0) {
			return -1.0
		}
		val heightObstacle = ctx.getRouter().defineHeightObstacle(road, segmentInd, prevSegmentInd)
		if (heightObstacle < 0) {
			return -1.0
		}
		return obstacle + heightObstacle + distTimeOnRoadToPass
	}

	fun calcRoutingSegmentTimeOnlyDist(router: VehicleRouter, segment: RouteSegment): Float {
		val road = segment.getRoad()
		val prevX = road.getPoint31XTile(segment.getSegmentStart().toInt())
		val prevY = road.getPoint31YTile(segment.getSegmentStart().toInt())
		val x = road.getPoint31XTile(segment.getSegmentEnd().toInt())
		val y = road.getPoint31YTile(segment.getSegmentEnd().toInt())
		val priority = router.defineSpeedPriority(road, segment.isPositive())
		var speed = router.defineRoutingSpeed(road, segment.isPositive()) * priority
		if (speed == 0f) {
			speed = router.getDefaultSpeed() * priority
		}
		// speed can not exceed max default speed according to A*
		if (speed > router.getMaxSpeed()) {
			speed = router.getMaxSpeed()
		}
		val distOnRoadToPass = squareRootDist(prevX, prevY, x, y).toFloat()
		return distOnRoadToPass / speed
	}

	private fun processRouteSegment(
		ctx: RoutingContext, reverseWaySearch: Boolean,
		graphSegments: KPriorityQueue<RouteSegmentCost>, visitedSegments: KTLongObjectMap<RouteSegment>,
		startSegment: RouteSegment, oppositeSegments: KTLongObjectMap<RouteSegment>,
		boundaries: KTLongObjectLookup<RouteSegment>?, doNotAddIntersections: Boolean
	) {
		if (ASSERT_CHECKS && !checkMovementAllowed(ctx, reverseWaySearch, startSegment)) {
			throw IllegalStateException()
		}
		val road = startSegment.getRoad()
		if (TEST_SPECIFIC && road.getId() shr 6 == TEST_ID.toLong()) {
			printRoad(" ! " + startSegment.distanceFromStart + " ", startSegment, reverseWaySearch)
		}
		// Go through all point of the way and find ways to continue
		// ! Actually there is small bug when there is restriction to move forward on the way (it doesn't take into account)
		// +/- diff from middle point
		var nextCurrentSegment: RouteSegment? = startSegment
		var currentSegment: RouteSegment? = null
		while (nextCurrentSegment != null) {
			currentSegment = nextCurrentSegment
			nextCurrentSegment = null

			// 1. check if segment was already visited in opposite direction
			// We check before we calculate segmentTime (to not calculate it twice with opposite and calculate turns onto each segment).
			val bothDirVisited = checkIfOppositeSegmentWasVisited(ctx, reverseWaySearch, graphSegments, currentSegment, oppositeSegments, boundaries)

			// 2. calculate obstacle for passing this segment (after  visiting cause obstacle is at the end of the segment)
			val segmentAndObstaclesTime = calculateRouteSegmentTime(ctx, reverseWaySearch, currentSegment).toFloat()
			if (segmentAndObstaclesTime < 0) {
				break
			}
			// calculate new start segment time as we're going to assign to put to visited segments
			val distFromStartPlusSegmentTime = currentSegment.distanceFromStart + segmentAndObstaclesTime

			// 3. upload segment itself to visited segments
			val nextPntId = calculateRoutePointId(currentSegment)
			val existingSegment = visitedSegments.put(nextPntId, currentSegment)
			if (existingSegment != null) {
				if (distFromStartPlusSegmentTime > existingSegment.distanceFromStart) {
					// insert back original segment (test case with large area way)
					visitedSegments.put(nextPntId, existingSegment)
					if (TRACE_ROUTING) {
						println("  " + currentSegment.segEnd + ">> Already visited")
					}
					break
				} else {
					if (ctx.config.heuristicCoefficient <= 1) {
						if (RoutingContext.PRINT_ROUTING_ALERTS) {
							println(
								"! ALERT slower segment was visited earlier " + distFromStartPlusSegmentTime + " > "
										+ existingSegment.distanceFromStart + ": " + currentSegment + " - " + existingSegment
							)
						} else {
							ctx.alertSlowerSegmentedWasVisitedEarlier++
						}
					}
				}
			}

			// reassign @distanceFromStart to make it correct for visited segment
			currentSegment.distanceFromStart = distFromStartPlusSegmentTime

			if (bothDirVisited && ctx.config.altHorizon <= 0) {
				// We stop here for shortcut creation (we can't improve the neighbors if they're already visited cause the opposite is min - prove by contradiction)
				// Alternatives need the opposite: the two trees have to grow through each other, or the
				// only road points settled by both are the handful on the frontier (see altHorizon).
				if (TRACE_ROUTING) {
					println("  " + currentSegment.segEnd + ">> 2 dir visited")
				}
				break
			}

			// 4. load road connections at the end of segment
			nextCurrentSegment = processIntersections(ctx, graphSegments, visitedSegments, currentSegment, reverseWaySearch, doNotAddIntersections)

			// Theoretically we should process each step separately but we don't have any issues with it.
			// a) final segment is always in queue & double checked b) using osm segment almost always is shorter routing than other connected
			if (DEBUG_BREAK_EACH_SEGMENT && nextCurrentSegment != null) {
				if (!doNotAddIntersections) {
					graphSegments.add(RouteSegmentCost(nextCurrentSegment, ctx))
				}
				break
			}
			if (doNotAddIntersections) {
				break
			}
		}

		ctx.visitor?.visitSegment(startSegment, currentSegment!!.getSegmentEnd().toInt(), true)
	}

	private fun checkMovementAllowed(ctx: RoutingContext, reverseWaySearch: Boolean, segment: RouteSegment): Boolean {
		val directionAllowed: Boolean
		val oneway = ctx.getRouter().isOneWay(segment.getRoad())
		// use positive direction as agreed
		if (!reverseWaySearch) {
			directionAllowed = if (segment.isPositive()) {
				oneway >= 0
			} else {
				oneway <= 0
			}
		} else {
			directionAllowed = if (segment.isPositive()) {
				oneway <= 0
			} else {
				oneway >= 0
			}
		}
		return directionAllowed
	}

	private fun checkViaRestrictions(from: RouteSegment?, to: RouteSegment?): Boolean {
		if (from != null && to != null) {
			val fid = to.getRoad().getId()
			for (i in 0 until from.getRoad().getRestrictionLength()) {
				val id = from.getRoad().getRestrictionId(i)
				val tp = from.getRoad().getRestrictionType(i)
				if (fid == id) {
					if (tp == RouteDataObject.RESTRICTION_NO_LEFT_TURN
						|| tp == RouteDataObject.RESTRICTION_NO_RIGHT_TURN
						|| tp == RouteDataObject.RESTRICTION_NO_STRAIGHT_ON
						|| tp == RouteDataObject.RESTRICTION_NO_U_TURN
					) {
						return false
					}
					break
				}
				if (tp == RouteDataObject.RESTRICTION_ONLY_STRAIGHT_ON) {
					return false
				}
			}
		}
		return true
	}

	private fun getParentDiffId(segment: RouteSegment?): RouteSegment? {
		var s = segment ?: return null
		while (s.getParentRoute() != null && s.getParentRoute()!!.getRoad().getId() == s.getRoad().getId()) {
			s = s.getParentRoute()!!
		}
		return s.getParentRoute()
	}

	private fun checkIfOppositeSegmentWasVisited(
		ctx: RoutingContext, reverseWaySearch: Boolean,
		graphSegments: KPriorityQueue<RouteSegmentCost>, currentSegment: RouteSegment,
		oppositeSegmentsArg: KTLongObjectMap<RouteSegment>, boundaries: KTLongObjectLookup<RouteSegment>?
	): Boolean {
		var oppositeSegments: KTLongObjectLookup<RouteSegment> = oppositeSegmentsArg
		// check inverse direction for opposite
		val currPoint = calculateRoutePointInternalId(
			currentSegment.getRoad(),
			currentSegment.getSegmentEnd().toInt(), currentSegment.getSegmentStart().toInt()
		)
		if (boundaries != null && ctx.dijkstraMode != 0) {
			// limit by boundaries for dijkstra mode
			oppositeSegments = boundaries
		}
		if (oppositeSegments.containsKey(currPoint)) {
			val opposite = oppositeSegments[currPoint]
			val curParent = getParentDiffId(currentSegment)
			val oppParent = getParentDiffId(opposite)
			val to = if (reverseWaySearch) curParent else oppParent
			val from = if (!reverseWaySearch) curParent else oppParent
			if (checkViaRestrictions(from, to)) {
				val frs = FinalRouteSegment(
					currentSegment.getRoad(),
					currentSegment.getSegmentStart().toInt(), currentSegment.getSegmentEnd().toInt()
				)
				frs.setParentRoute(currentSegment.getParentRoute())
				frs.reverseWaySearch = reverseWaySearch
				val oppTime = opposite?.distanceFromStart ?: 0f
				frs.distanceFromStart = oppTime + currentSegment.distanceFromStart
				frs.distanceToEnd = 0f
				frs.opposite = opposite
				if (frs.distanceFromStart < 0) {
					// impossible route (when start/point on same segment but different dir) don't add to queue
					return true
				}
				graphSegments.add(RouteSegmentCost(frs, ctx))
				if (TRACE_ROUTING) {
					printRoad("  " + currentSegment.segEnd + ">> Final segment : ", frs, reverseWaySearch)
				}
				ctx.calculationProgress?.let { it.finalSegmentsFound++ }
				return true
			}
		}
		if (boundaries != null && ctx.dijkstraMode == 0 && boundaries.containsKey(currPoint)) {
			// limit search by boundaries
			return true
		}
		return false
	}

	private fun calculateRoutePointInternalId(road: RouteDataObject, pntId: Int, nextPntId: Int): Long {
		val positive = nextPntId - pntId
		val pntLen = road.getPointsLength()
		if (pntId < 0 || nextPntId < 0 || pntId >= pntLen || nextPntId >= pntLen || (positive != -1 && positive != 1)) {
			// should be assert
			throw IllegalStateException("Assert failed")
		}
		return (road.getId() shl ROUTE_POINTS) + (pntId shl 1) + (if (positive > 0) 1 else 0)
	}

	private fun calculateRoutePointId(segm: RouteSegment): Long {
		return calculateRoutePointInternalId(
			segm.getRoad(), segm.getSegmentStart().toInt(),
			if (segm.isPositive()) segm.getSegmentStart() + 1 else segm.getSegmentStart() - 1
		)
		// return calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentStart(), segm.getSegmentEnd());
	}

	private fun proccessRestrictions(ctx: RoutingContext, segment: RouteSegment, inputNext: RouteSegment?, reverseWay: Boolean): Boolean {
		if (!ctx.getRouter().restrictionsAware()) {
			return false
		}
		val road = segment.getRoad()
		val parent = getParentDiffId(segment)
		if (!reverseWay && road.getRestrictionLength() == 0 &&
			(parent == null || parent.getRoad().getRestrictionLength() == 0)
		) {
			return false
		}
		ctx.segmentsToVisitPrescripted.clear()
		ctx.segmentsToVisitNotForbidden.clear()
		processRestriction(ctx, inputNext, reverseWay, 0, road)
		if (parent != null) {
			processRestriction(ctx, inputNext, reverseWay, road.id, parent.getRoad())
		}
		return true
	}

	private fun processRestriction(ctx: RoutingContext, inputNext: RouteSegment?, reverseWay: Boolean, viaId: Long, road: RouteDataObject) {
		val via = viaId != 0L
		var next = inputNext
		var exclusiveRestriction = false
		while (next != null) {
			var type = -1
			if (!reverseWay) {
				for (i in 0 until road.getRestrictionLength()) {
					val rt = road.getRestrictionType(i)
					val rv = road.getRestrictionVia(i)
					if (road.getRestrictionId(i) == next.getRoad().id) {
						if (!via || rv == viaId) {
							type = rt
							break
						}
					}
					if (rv == viaId && via && rt == RouteDataObject.RESTRICTION_ONLY_STRAIGHT_ON) {
						type = RouteDataObject.RESTRICTION_NO_STRAIGHT_ON
						break
					}
				}
			} else {
				val nextRoad = next.getRoad()
				for (i in 0 until nextRoad.getRestrictionLength()) {
					val rt = nextRoad.getRestrictionType(i)
					val rv = nextRoad.getRestrictionVia(i)
					val restrictedTo = nextRoad.getRestrictionId(i)
					if (restrictedTo == road.id) {
						if (!via || rv == viaId) {
							type = rt
							break
						}
					}

					if (rv == viaId && via && rt == RouteDataObject.RESTRICTION_ONLY_STRAIGHT_ON) {
						type = RouteDataObject.RESTRICTION_NO_STRAIGHT_ON
						break
					}

					// Check if there is restriction only to the other than current road
					if (rt == RouteDataObject.RESTRICTION_ONLY_RIGHT_TURN || rt == RouteDataObject.RESTRICTION_ONLY_LEFT_TURN
						|| rt == RouteDataObject.RESTRICTION_ONLY_STRAIGHT_ON
					) {
						// check if that restriction applies to considered junk
						var foundNext = inputNext
						while (foundNext != null) {
							if (foundNext.getRoad().id == restrictedTo) {
								break
							}
							foundNext = foundNext.next
						}
						if (foundNext != null) {
							type = REVERSE_WAY_RESTRICTION_ONLY // special constant
						}
					}
				}
			}
			if (type == REVERSE_WAY_RESTRICTION_ONLY) {
				// next = next.next; continue;
			} else if (type == -1 && exclusiveRestriction) {
				// next = next.next; continue;
			} else if (type == RouteDataObject.RESTRICTION_NO_LEFT_TURN || type == RouteDataObject.RESTRICTION_NO_RIGHT_TURN
				|| type == RouteDataObject.RESTRICTION_NO_STRAIGHT_ON || type == RouteDataObject.RESTRICTION_NO_U_TURN
			) {
				// next = next.next; continue;
				if (via) {
					ctx.segmentsToVisitPrescripted.remove(next)
				}
			} else if (type == -1) {
				// case no restriction
				ctx.segmentsToVisitNotForbidden.add(next)
			} else {
				if (!via) {
					// case exclusive restriction (only_right, only_straight, ...)
					// 1. in case we are going backward we should not consider only_restriction
					// as exclusive because we have many "in" roads and one "out"
					// 2. in case we are going forward we have one "in" and many "out"
					if (!reverseWay) {
						exclusiveRestriction = true
						ctx.segmentsToVisitNotForbidden.clear()
						ctx.segmentsToVisitPrescripted.add(next)
					} else {
						ctx.segmentsToVisitNotForbidden.add(next)
					}
				}
			}
			next = next.next
		}
		if (!via) {
			ctx.segmentsToVisitPrescripted.addAll(ctx.segmentsToVisitNotForbidden)
		}
	}

	private fun processIntersections(
		ctx: RoutingContext, graphSegments: KPriorityQueue<RouteSegmentCost>,
		visitedSegments: KTLongObjectMap<RouteSegment>, currentSegment: RouteSegment,
		reverseWaySearch: Boolean, doNotAddIntersections: Boolean
	): RouteSegment? {
		var nextCurrentSegment: RouteSegment? = null
		val targetEndX = if (reverseWaySearch) ctx.startX else ctx.targetX
		val targetEndY = if (reverseWaySearch) ctx.startY else ctx.targetY
		val x = currentSegment.getRoad().getPoint31XTile(currentSegment.getSegmentEnd().toInt())
		val y = currentSegment.getRoad().getPoint31YTile(currentSegment.getSegmentEnd().toInt())
		val distanceToEnd = h(ctx, x, y, targetEndX, targetEndY)
		// reassign @distanceToEnd to make it correct for visited segment
		currentSegment.distanceToEnd = distanceToEnd

		val connectedNextSegment = ctx.loadRouteSegment(x, y, ctx.config.memoryLimitation - ctx.memoryOverhead, reverseWaySearch)
		var roadIter = connectedNextSegment
		var directionAllowed = true
		var singleRoad = true
		while (roadIter != null) {
			if (currentSegment.getSegmentEnd() == roadIter.getSegmentStart() && roadIter.getRoad().getId() == currentSegment.getRoad().getId()) {
				nextCurrentSegment = roadIter.initRouteSegment(currentSegment.isPositive())
				if (nextCurrentSegment == null) {
					// end of route (-1 or length + 1)
					directionAllowed = false
				} else {
					if (nextCurrentSegment.isSegmentAttachedToStart()) {
						directionAllowed = processOneRoadIntersection(
							ctx, reverseWaySearch, null,
							visitedSegments, currentSegment, nextCurrentSegment
						)
						if (!directionAllowed) {
							nextCurrentSegment = null
						}
					} else {
						nextCurrentSegment.setParentRoute(currentSegment)
						nextCurrentSegment.distanceFromStart = currentSegment.distanceFromStart
						nextCurrentSegment.distanceToEnd = distanceToEnd
						val nx = nextCurrentSegment.getRoad().getPoint31XTile(nextCurrentSegment.getSegmentEnd().toInt())
						val ny = nextCurrentSegment.getRoad().getPoint31YTile(nextCurrentSegment.getSegmentEnd().toInt())
						if (nx == x && ny == y) {
							// don't process other intersections (let process further segment)
							return nextCurrentSegment
						}
					}
				}
			} else {
				singleRoad = false
			}
			roadIter = roadIter.getNext()
		}

		if (singleRoad) {
			return nextCurrentSegment
		}

		// find restrictions and iterator
		var nextIterator: MutableIterator<RouteSegment>? = null
		val thereAreRestrictions = proccessRestrictions(ctx, currentSegment, connectedNextSegment, reverseWaySearch)
		if (thereAreRestrictions) {
			nextIterator = ctx.segmentsToVisitPrescripted.iterator()
			if (TRACE_ROUTING) {
				println("  " + currentSegment.segEnd + ">> There are restrictions ")
			}
		}

		// Calculate possible turns to put into priority queue
		var next = connectedNextSegment
		var hasNext = if (nextIterator != null) nextIterator.hasNext() else next != null
		while (hasNext) {
			if (nextIterator != null) {
				next = nextIterator.next()
			}
			if (next!!.getSegmentStart() == currentSegment.getSegmentEnd() &&
				next.getRoad().getId() == currentSegment.getRoad().getId()
			) {
				// skip itself
			} else if (!doNotAddIntersections) {
				val nextPos = next.initRouteSegment(true)
				processOneRoadIntersection(ctx, reverseWaySearch, graphSegments, visitedSegments, currentSegment, nextPos)
				val nextNeg = next.initRouteSegment(false)
				processOneRoadIntersection(ctx, reverseWaySearch, graphSegments, visitedSegments, currentSegment, nextNeg)
			}
			// iterate to next road
			if (nextIterator == null) {
				next = next.next
				hasNext = next != null
			} else {
				hasNext = nextIterator.hasNext()
			}
		}

		if (nextCurrentSegment == null && directionAllowed) {
			if (ctx.calculationMode != RouteCalculationMode.BASE) {
				// exception as it should not occur, if happens during approximation - should be investigated
				if (ASSERT_CHECKS) {
					throw IllegalStateException()
				}
			} else {
				//  Issue #13284: we know that bug in data (how we simplify base data and connect between regions), so we workaround it
				val newEnd = currentSegment.getSegmentEnd() + (if (currentSegment.isPositive()) +1 else -1)
				if (newEnd >= 0 && newEnd < currentSegment.getRoad().getPointsLength() - 1) {
					nextCurrentSegment = RouteSegment(currentSegment.getRoad(), currentSegment.getSegmentEnd().toInt(), newEnd)
					nextCurrentSegment.setParentRoute(currentSegment)
					nextCurrentSegment.distanceFromStart = currentSegment.distanceFromStart
					nextCurrentSegment.distanceToEnd = distanceToEnd
				}
			}
		}
		return nextCurrentSegment
	}

	private fun processOneRoadIntersection(
		ctx: RoutingContext, reverseWaySearch: Boolean, graphSegments: KPriorityQueue<RouteSegmentCost>?,
		visitedSegments: KTLongObjectMap<RouteSegment>, segment: RouteSegment, next: RouteSegment?
	): Boolean {
		if (next != null) {
			if (!checkMovementAllowed(ctx, reverseWaySearch, next)) {
				return false
			}
			var obstaclesTime = 0f
			if (next.getRoad().getId() != segment.getRoad().getId()) {
				obstaclesTime = ctx.getRouter().calculateTurnTime(next, segment).toFloat()
			}
			if (obstaclesTime < 0) {
				return false
			}
			val distFromStart = obstaclesTime + segment.distanceFromStart
			if (TEST_SPECIFIC && next.getRoad().getId() shr 6 == TEST_ID.toLong()) {
				printRoad(
					" !? distFromStart=" + distFromStart + " from " + segment.getRoad().getId() +
							" distToEnd=" + segment.distanceFromStart +
							" segmentPoint=" + segment.getSegmentEnd() + " -- ", next, null
				)
			}
			val visIt = visitedSegments[calculateRoutePointId(next)]
			if (visIt != null) {
				if (TRACE_ROUTING) {
					printRoad("  " + segment.segEnd + ">?", visitedSegments[calculateRoutePointId(next)], null)
				}
				// The segment was already visited! We can try to follow new route if it's shorter.
				// That is very exceptional situation and almost exception, it can happen
				// 1. We underestimate distanceToEnd - wrong h() of A* (heuristic > 1)
				// 2. We don't process small segments 1 by 1 as we should by Dijkstra
				if (distFromStart < visIt.distanceFromStart) {
					val routeSegmentTime = calculateRouteSegmentTime(ctx, reverseWaySearch, visIt)
					// we need to properly compare @distanceFromStart VISITED and NON-VISITED segment
					if (visIt.distanceFromStart - (distFromStart + routeSegmentTime) > 0.01) { // cause we do double -> float we can get into infinite loop here
						// Here it's not very legitimate action cause in theory we need to go up to the final segment in the queue & decrease final time
						// But it's compensated by chain reaction cause next.distanceFromStart < finalSegment.distanceFromStart and revisiting all segments
						// We don't check ```next.getParentRoute() == null``` cause segment could be unloaded
						// so we need to add segment back to the queue & reassign the parent (same as for next.getParentRoute() == null)
						if (ctx.config.heuristicCoefficient <= 1) {
							if (DEBUG_BREAK_EACH_SEGMENT && ASSERT_CHECKS) {
								throw IllegalStateException()
							}
							if (RoutingContext.PRINT_ROUTING_ALERTS) {
								println(
									"! ALERT new faster path to a visited segment: "
											+ (distFromStart + routeSegmentTime) + " < " + visIt.distanceFromStart + ": " + next + " - " + visIt
								)
							} else {
								ctx.alertFasterRoadToVisitedSegments++
							}
						}
						visitedSegments.remove(calculateRoutePointId(next))
					} else {
						return false
					}
				} else {
					return false
				}
			}
			if (!next.isSegmentAttachedToStart() || cost(next.distanceFromStart, next.distanceToEnd, ctx) > cost(distFromStart, segment.distanceToEnd, ctx)) {
				next.distanceFromStart = distFromStart
				next.distanceToEnd = segment.distanceToEnd
				if (TRACE_ROUTING) {
					printRoad(" " + (if (next.isSegmentAttachedToStart()) "*" else "") + segment.getSegmentEnd() + ">>", next, null)
				}
				// put additional information to recover whole route after
				next.setParentRoute(segment)
				graphSegments?.add(RouteSegmentCost(next, ctx))
				return true
			}
		}
		return false
	}

	companion object {
		private val log = LoggerFactory.getLogger("BinaryRoutePlanner")

		private const val REVERSE_WAY_RESTRICTION_ONLY = 1024
		internal const val STANDARD_ROAD_IN_QUEUE_OVERHEAD = 220
		internal const val STANDARD_ROAD_VISITED_OVERHEAD = 150

		private const val ROUTE_POINTS = 11

		@JvmField
		var ASSERT_CHECKS = true

		// exported to Objective-C under another name: a macro of the same name in the C++ core headers
		// would otherwise break every file that includes both
		@OptIn(ExperimentalObjCName::class)
		@ObjCName("traceRouting")
		@JvmField
		var TRACE_ROUTING = false

		@JvmField
		var TEST_ID = 194349150

		@JvmField
		var TEST_SPECIFIC = false

		@JvmField
		var DEBUG_PRECISE_DIST_MEASUREMENT = false

		@JvmField
		var DEBUG_BREAK_EACH_SEGMENT = false

		@JvmStatic
		fun squareRootDist(x1: Int, y1: Int, x2: Int, y2: Int): Double {
			if (DEBUG_PRECISE_DIST_MEASUREMENT) {
				return KMapUtils.measuredDist31(x1, y1, x2, y2)
			}
			return KMapUtils.squareRootDist31(x1, y1, x2, y2)
		}

		private fun cost(distanceFromStart: Float, distanceToEnd: Float, ctx: RoutingContext): Float {
			return ctx.config.heuristicCoefficient * distanceToEnd + distanceFromStart
		}

		@JvmStatic
		fun h(ctx: RoutingContext, begX: Int, begY: Int, endX: Int, endY: Int): Float {
			if (ctx.dijkstraMode != 0) {
				return 0f
			}
			val direction = ctx.precalculatedRouteDirection
			if (direction != null) {
				val te = direction.timeEstimate(begX, begY, endX, endY)
				if (te > 0) {
					return te
				}
			}
			val distToFinalPoint = squareRootDist(begX, begY, endX, endY)
			val result = distToFinalPoint / ctx.getRouter().getMaxSpeed()
			return result.toFloat()
		}
	}
}
