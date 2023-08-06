package net.osmand.router;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import static net.osmand.router.RoutePlannerFrontEnd.*;

public class BinaryRoutePlanner {


	private static final int REVERSE_WAY_RESTRICTION_ONLY = 1024;
	/*private*/ static final int STANDARD_ROAD_IN_QUEUE_OVERHEAD = 220;
	/*private*/ static final int STANDARD_ROAD_VISITED_OVERHEAD = 150;

	protected static final Log log = PlatformUtil.getLog(BinaryRoutePlanner.class);

	private static final int ROUTE_POINTS = 11;
	private static final boolean ASSERT_CHECKS = true;
	private static final boolean TRACE_ROUTING = false;
	private static final int TEST_ID = 50725;
	private static final boolean TEST_SPECIFIC = true;


	public static double squareRootDist(int x1, int y1, int x2, int y2) {
		return MapUtils.squareRootDist31(x1, y1, x2, y2);
//		return MapUtils.measuredDist31(x1, y1, x2, y2);
	}


	private static class SegmentsComparator implements Comparator<RouteSegment> {
		final RoutingContext ctx;

		public SegmentsComparator(RoutingContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public int compare(RouteSegment o1, RouteSegment o2) {
			return ctx.roadPriorityComparator(o1.distanceFromStart, o1.distanceToEnd, o2.distanceFromStart, o2.distanceToEnd);
		}
	}

	private static class NonHeuristicSegmentsComparator implements Comparator<RouteSegment> {
		public NonHeuristicSegmentsComparator() {
		}

		@Override
		public int compare(RouteSegment o1, RouteSegment o2) {
			return roadPriorityComparator(o1.distanceFromStart, o1.distanceToEnd, o2.distanceFromStart, o2.distanceToEnd, 0.5);
		}
	}

	/**
	 * Calculate route between start.segmentEnd and end.segmentStart (using A* algorithm)
	 * return list of segments
	 */
	FinalRouteSegment searchRouteInternal(final RoutingContext ctx, RouteSegmentPoint start, RouteSegmentPoint end,
			RouteSegment recalculationEnd ) throws InterruptedException, IOException {
		// measure time
		ctx.memoryOverhead = 1000;

		// Initializing priority queue to visit way segments 
		Comparator<RouteSegment> nonHeuristicSegmentsComparator = new NonHeuristicSegmentsComparator();
		PriorityQueue<RouteSegment> graphDirectSegments = new PriorityQueue<RouteSegment>(50, new SegmentsComparator(ctx));
		PriorityQueue<RouteSegment> graphReverseSegments = new PriorityQueue<RouteSegment>(50, new SegmentsComparator(ctx));

		// Set to not visit one segment twice (stores road.id << X + segmentStart)
		TLongObjectHashMap<RouteSegment> visitedDirectSegments = new TLongObjectHashMap<RouteSegment>();
		TLongObjectHashMap<RouteSegment> visitedOppositeSegments = new TLongObjectHashMap<RouteSegment>();

		initQueuesWithStartEnd(ctx, start, end, recalculationEnd, graphDirectSegments, graphReverseSegments, 
				visitedDirectSegments, visitedOppositeSegments);



		FinalRouteSegment finalSegment = null;
		boolean onlyBackward = ctx.getPlanRoadDirection() < 0;
		boolean onlyForward = ctx.getPlanRoadDirection() > 0;
		// Extract & analyze segment with min(f(x)) from queue while final segment is not found
		PriorityQueue<RouteSegment> graphSegments = onlyForward ? graphReverseSegments : graphDirectSegments;
		boolean forwardSearch = !onlyForward;
		while (!graphSegments.isEmpty()) {
			RouteSegment segment = graphSegments.poll();
			// use accumulative approach
			ctx.memoryOverhead = (visitedDirectSegments.size() + visitedOppositeSegments.size()) * STANDARD_ROAD_VISITED_OVERHEAD +
					(graphDirectSegments.size() +
					graphReverseSegments.size()) * STANDARD_ROAD_IN_QUEUE_OVERHEAD;
			
			if (TRACE_ROUTING) {
				printRoad(">", segment, !forwardSearch);
			}
			if (segment instanceof FinalRouteSegment) {
				if (RoutingContext.SHOW_GC_SIZE) {
					log.warn("Estimated overhead " + (ctx.memoryOverhead / (1 << 20)) + " mb");
					printMemoryConsumption("Memory occupied after calculation : ");
				}
				finalSegment = (FinalRouteSegment) segment;
				if (TRACE_ROUTING) {
					println("Final segment found");
				}
				break;
			}
			if (ctx.memoryOverhead > ctx.config.memoryLimitation * 0.95 && RoutingContext.SHOW_GC_SIZE) {
				printMemoryConsumption("Memory occupied before exception : ");
			}
			if (ctx.memoryOverhead > ctx.config.memoryLimitation * 0.95) {
				throw new IllegalStateException("There is not enough memory " + ctx.config.memoryLimitation / (1 << 20) + " Mb");
			}
			if (ctx.calculationProgress != null) {
				ctx.calculationProgress.visitedSegments++;
			}
			if (forwardSearch) {
				boolean doNotAddIntersections = onlyBackward;
				processRouteSegment(ctx, false, graphDirectSegments, visitedDirectSegments,
						segment, visitedOppositeSegments, doNotAddIntersections);
			} else {
				boolean doNotAddIntersections = onlyForward;
				processRouteSegment(ctx, true, graphReverseSegments, visitedOppositeSegments, segment,
						visitedDirectSegments, doNotAddIntersections);
			}
			updateCalculationProgress(ctx, graphDirectSegments, graphReverseSegments);

			checkIfGraphIsEmpty(ctx, ctx.getPlanRoadDirection() <= 0, true, graphReverseSegments, end, visitedOppositeSegments,
					"Route is not found to selected target point.");
			checkIfGraphIsEmpty(ctx, ctx.getPlanRoadDirection() >= 0, false, graphDirectSegments, start, visitedDirectSegments,
					"Route is not found from selected start point.");
			if (ctx.planRouteIn2Directions()) {
				if (graphDirectSegments.isEmpty() || graphReverseSegments.isEmpty()) {
					// can't proceed - so no route
					break;
				} else {
					forwardSearch = nonHeuristicSegmentsComparator.compare(graphDirectSegments.peek(), graphReverseSegments.peek()) <= 0;
				}
//				if (graphDirectSegments.size() * 2 > graphReverseSegments.size()) {
//					forwardSearch = false;
//				} else if (graphDirectSegments.size() < 2 * graphReverseSegments.size()) {
//					forwardSearch = true;
//				}
			} else {
				// different strategy : use onedirectional graph
				forwardSearch = onlyForward;
				if (onlyBackward && !graphDirectSegments.isEmpty()) {
					forwardSearch = true;
				}
				if (onlyForward && !graphReverseSegments.isEmpty()) {
					forwardSearch = false;
				}
			}

			if (forwardSearch) {
				graphSegments = graphDirectSegments;
			} else {
				graphSegments = graphReverseSegments;
			}
			// check if interrupted
			if (ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				throw new InterruptedException("Route calculation interrupted");
			}
		}
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.visitedDirectSegments += visitedDirectSegments.size();
			ctx.calculationProgress.visitedOppositeSegments += visitedOppositeSegments.size();
			ctx.calculationProgress.directQueueSize += graphDirectSegments.size(); // Math.max(ctx.directQueueSize,
																					// graphDirectSegments.size());
			ctx.calculationProgress.oppositeQueueSize += graphReverseSegments.size();
		}
		return finalSegment;
	}

	protected void checkIfGraphIsEmpty(final RoutingContext ctx, boolean allowDirection,
			boolean reverseWaySearch, PriorityQueue<RouteSegment> graphSegments, RouteSegmentPoint pnt, TLongObjectHashMap<RouteSegment> visited,
			String msg) {
		if (allowDirection && graphSegments.isEmpty()) {
			if (pnt.others != null) {
				Iterator<RouteSegmentPoint> pntIterator = pnt.others.iterator();
				while (pntIterator.hasNext()) {
					RouteSegmentPoint next = pntIterator.next();
					pntIterator.remove();
					float estimatedDistance = (float) estimatedDistance(ctx, ctx.targetX, ctx.targetY, ctx.startX,
							ctx.startY);
					RouteSegment pos = next.initRouteSegment(true);
					if (pos != null && !visited.containsKey(calculateRoutePointId(pos)) &&
							checkMovementAllowed(ctx, reverseWaySearch, pos)) {
						pos.setParentRoute(null);
						pos.distanceFromStart = 0;
						pos.distanceToEnd = estimatedDistance;
						graphSegments.add(pos);
					}
					RouteSegment neg = next.initRouteSegment(false);
					if (neg != null && !visited.containsKey(calculateRoutePointId(neg)) && 
							checkMovementAllowed(ctx, reverseWaySearch, neg)) {
						neg.setParentRoute(null);
						neg.distanceFromStart = 0;
						neg.distanceToEnd = estimatedDistance;
						graphSegments.add(neg);
					}
					if (!graphSegments.isEmpty()) {
						println("Reiterate point with new " + (!reverseWaySearch ? "start " : "destination ")
								+ next.getRoad());
						break;
					}
				}
				if (graphSegments.isEmpty()) {
					throw new IllegalArgumentException(msg);
				}
			}
		}
	}

	public RouteSegment initRouteSegment(final RoutingContext ctx, RouteSegment segment, boolean positiveDirection, boolean reverseSearchWay) {
		if (segment.getSegmentStart() == 0 && !positiveDirection && segment.getRoad().getPointsLength() > 0) {
			segment = loadSameSegment(ctx, segment, 1, reverseSearchWay);
//		} else if (segment.getSegmentStart() == segment.getRoad().getPointsLength() - 1 && positiveDirection && segment.getSegmentStart() > 0) {
		// asymmetric cause we calculate initial point differently (segmentStart means that point is between ]segmentStart-1, segmentStart]
		} else if (segment.getSegmentStart() > 0 && positiveDirection) {
			segment = loadSameSegment(ctx, segment, segment.getSegmentStart() - 1, reverseSearchWay);
		}
		RouteSegment initSegment = null;
		if (segment != null) {
			initSegment = segment.initRouteSegment(positiveDirection);
		}
		if (initSegment != null) {
			initSegment.setParentRoute(RouteSegment.NULL);
			// compensate first segment difference
			initSegment.distanceFromStart += initDistFromStart(ctx, initSegment, reverseSearchWay);
		}
		return initSegment;
	}
	
	private double initDistFromStart(RoutingContext ctx, RouteSegment initSegment, boolean reverseSearchWay) {
		int prevX = initSegment.road.getPoint31XTile(initSegment.getSegmentStart());
		int prevY = initSegment.road.getPoint31YTile(initSegment.getSegmentStart());
		int x = initSegment.road.getPoint31XTile(initSegment.getSegmentEnd());
		int y = initSegment.road.getPoint31YTile(initSegment.getSegmentEnd());
		
		float priority = ctx.getRouter().defineSpeedPriority(initSegment.road);
		float speed = (ctx.getRouter().defineRoutingSpeed(initSegment.road) * priority);
		
		if (speed == 0) {
			speed = (ctx.getRouter().getDefaultSpeed() * priority);
		}
		// speed can not exceed max default speed according to A*
		if (speed > ctx.getRouter().getMaxSpeed()) {
			speed = ctx.getRouter().getMaxSpeed();
		}
		double fullDist = squareRootDist(prevX, prevY, x, y);
		double distFromStart = squareRootDist(x, y, !reverseSearchWay ? ctx.startX : ctx.targetX, !reverseSearchWay ? ctx.startY : ctx.targetY);
		
		return (distFromStart - fullDist) / speed;
	}


	protected RouteSegment loadSameSegment(final RoutingContext ctx, RouteSegment segment, int ind, boolean reverseSearchWay) {
		int x31 = segment.getRoad().getPoint31XTile(ind);
		int y31 = segment.getRoad().getPoint31YTile(ind);
		RouteSegment s = ctx.loadRouteSegment(x31, y31, 0, reverseSearchWay);
		while (s != null) {
			if (s.getRoad().getId() == segment.getRoad().getId()) {
				segment = s;
				break;
			}
			s = s.getNext();
		}
		return segment;
	}


	private void initQueuesWithStartEnd(final RoutingContext ctx, RouteSegmentPoint start, RouteSegmentPoint end,
			RouteSegment recalculationEnd, PriorityQueue<RouteSegment> graphDirectSegments, PriorityQueue<RouteSegment> graphReverseSegments, 
			TLongObjectHashMap<RouteSegment> visitedDirectSegments, TLongObjectHashMap<RouteSegment> visitedOppositeSegments) {
		RouteSegment startPos = initRouteSegment(ctx, start, true, false);
		RouteSegment startNeg = initRouteSegment(ctx, start, false, false);
		RouteSegment endPos = initRouteSegment(ctx, end, true, true);
		RouteSegment endNeg = initRouteSegment(ctx, end, false, true);
		// for start : f(start) = g(start) + h(start) = 0 + h(start) = h(start)
		if (ctx.config.initialDirection != null) {
			// mark here as positive for further check
			double plusDir = start.getRoad().directionRoute(start.getSegmentStart(), true);
			double diff = plusDir - ctx.config.initialDirection;
			if (Math.abs(MapUtils.alignAngleDifference(diff)) <= Math.PI / 3) {
				if (startNeg != null) {
					startNeg.distanceFromStart += 500;
				}
			} else if (Math.abs(MapUtils.alignAngleDifference(diff - Math.PI)) <= Math.PI / 3) {
				if (startPos != null) {
					startPos.distanceFromStart += 500;
				}
			}
		}
		if (recalculationEnd != null) {
			ctx.targetX = recalculationEnd.getRoad().getPoint31XTile(recalculationEnd.getSegmentStart());
			ctx.targetY = recalculationEnd.getRoad().getPoint31YTile(recalculationEnd.getSegmentStart());
		}
		float estimatedDistance = (float) estimatedDistance(ctx, ctx.targetX, ctx.targetY, ctx.startX, ctx.startY);
		if (startPos != null && checkMovementAllowed(ctx, false, startPos)) {
			startPos.distanceToEnd = estimatedDistance;
			graphDirectSegments.add(startPos);
		}
		if (startNeg != null && checkMovementAllowed(ctx, false, startNeg)) {
			startNeg.distanceToEnd = estimatedDistance;
			graphDirectSegments.add(startNeg);
		}
		if (recalculationEnd != null) {
			graphReverseSegments.add(recalculationEnd);
		} else {
			if (endPos != null && checkMovementAllowed(ctx, true, endPos)) {
				endPos.distanceToEnd = estimatedDistance;
				graphReverseSegments.add(endPos);
			}
			if (endNeg != null && checkMovementAllowed(ctx, true, endNeg)) {
				endNeg.distanceToEnd = estimatedDistance;
				graphReverseSegments.add(endNeg);
			}
		}
		if (TRACE_ROUTING) {
			printRoad("Initial segment start positive: ", startPos, false);
			printRoad("Initial segment start negative: ", startNeg, false);
			printRoad("Initial segment end positive: ", endPos, false);
			printRoad("Initial segment end negative: ", endNeg, false);
		}
	}


	private void printMemoryConsumption(String string) {
		long h1 = RoutingContext.runGCUsedMemory();
		float mb = (1 << 20);
		log.warn(string + h1 / mb);
	}


	private void updateCalculationProgress(final RoutingContext ctx, PriorityQueue<RouteSegment> graphDirectSegments,
			PriorityQueue<RouteSegment> graphReverseSegments) {
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.reverseSegmentQueueSize = graphReverseSegments.size();
			ctx.calculationProgress.directSegmentQueueSize = graphDirectSegments.size();
			if (graphDirectSegments.size() > 0 && ctx.getPlanRoadDirection() >= 0) {
				RouteSegment peek = graphDirectSegments.peek();
				ctx.calculationProgress.distanceFromBegin = Math.max(peek.distanceFromStart,
						ctx.calculationProgress.distanceFromBegin);
				ctx.calculationProgress.directDistance = peek.distanceFromStart + peek.distanceToEnd;
			}
			if (graphReverseSegments.size() > 0 && ctx.getPlanRoadDirection() <= 0) {
				RouteSegment peek = graphReverseSegments.peek();
				ctx.calculationProgress.distanceFromEnd = Math.max(peek.distanceFromStart + peek.distanceToEnd,
							ctx.calculationProgress.distanceFromEnd);
				ctx.calculationProgress.reverseDistance = peek.distanceFromStart + peek.distanceToEnd;
			}
		}
	}

	private void printRoad(String prefix, RouteSegment segment, Boolean reverseWaySearch) {
		String p = "";
		if (reverseWaySearch != null) {
			p = (reverseWaySearch ? "B" : "F");
		}
		if (segment == null) {
			println(p + prefix + " Segment=null");
		} else {
			String pr;
			if (segment.parentRoute != null) {
				pr = " pend=" + segment.parentRoute.segEnd + " parent=" + segment.parentRoute.road;
			} else {
				pr = "";
			}
			println(p + prefix + "" + segment.road + " ind=" + segment.getSegmentStart() + "->" + segment.getSegmentEnd() +
					" ds=" + ((float) segment.distanceFromStart) + " es=" + ((float) segment.distanceToEnd) + pr);
		}
	}

	private float estimatedDistance(final RoutingContext ctx, int targetEndX, int targetEndY,
			int startX, int startY) {
		double distance = squareRootDist(startX, startY, targetEndX, targetEndY);
		return (float) (distance / ctx.getRouter().getMaxSpeed());
	}

	protected static float h(RoutingContext ctx, int begX, int begY, int endX, int endY) {
		double distToFinalPoint = squareRootDist(begX, begY, endX, endY);
		double result = distToFinalPoint / ctx.getRouter().getMaxSpeed();
		if (ctx.precalculatedRouteDirection != null) {
			float te = ctx.precalculatedRouteDirection.timeEstimate(begX, begY, endX, endY);
			if (te > 0) {
				return te;
			}
		}
		return (float) result;
	}


	private static void println(String logMsg) {
//		log.info(logMsg);
		System.out.println(logMsg);
	}

	private double calculateRouteSegmentTime(RoutingContext ctx, boolean reverseWaySearch, RouteSegment segment) {
		final RouteDataObject road = segment.road;
		// store <segment> in order to not have unique <segment, direction> in visitedSegments
		short segmentInd = reverseWaySearch ? segment.getSegmentStart() : segment.getSegmentEnd();
		short prevSegmentInd = !reverseWaySearch ? segment.getSegmentStart() : segment.getSegmentEnd();
		final int x = road.getPoint31XTile(segmentInd);
		final int y = road.getPoint31YTile(segmentInd);
		final int prevX = road.getPoint31XTile(prevSegmentInd);
		final int prevY = road.getPoint31YTile(prevSegmentInd);

		// calculate point and try to load neighbor ways if they are not loaded
		double distOnRoadToPass = squareRootDist(x, y, prevX, prevY);
		float priority = ctx.getRouter().defineSpeedPriority(road);
		float speed = (ctx.getRouter().defineRoutingSpeed(road) * priority);
		if (speed == 0) {
			speed = (ctx.getRouter().getDefaultSpeed() * priority);
		}
		// speed can not exceed max default speed according to A*
		if (speed > ctx.getRouter().getMaxSpeed()) {
			speed = ctx.getRouter().getMaxSpeed();
		}
		
		// calculate possible obstacle plus time
		double obstacle = ctx.getRouter().defineRoutingObstacle(road, segmentInd, prevSegmentInd > segmentInd);
		if (obstacle < 0) {
			return -1;
		}
		double heightObstacle = ctx.getRouter().defineHeightObstacle(road, segmentInd, prevSegmentInd);
		if (heightObstacle < 0) {
			return -1;
		}
		return obstacle + heightObstacle + distOnRoadToPass / speed;

	}


	@SuppressWarnings("unused")
	private void processRouteSegment(final RoutingContext ctx, boolean reverseWaySearch,
			PriorityQueue<RouteSegment> graphSegments, TLongObjectHashMap<RouteSegment> visitedSegments, 
            RouteSegment startSegment, TLongObjectHashMap<RouteSegment> oppositeSegments, boolean doNotAddIntersections) {
		if (ASSERT_CHECKS && !checkMovementAllowed(ctx, reverseWaySearch, startSegment)) {
			throw new IllegalStateException();
		}
		final RouteDataObject road = startSegment.getRoad();
		if (TEST_SPECIFIC && road.getId() >> 6 == TEST_ID) {
			printRoad(" ! "  + startSegment.distanceFromStart + " ", startSegment, reverseWaySearch);
		}
		boolean directionAllowed = true;
		// Go through all point of the way and find ways to continue
		// ! Actually there is small bug when there is restriction to move forward on the way (it doesn't take into account)
		// +/- diff from middle point
		RouteSegment nextCurrentSegment = startSegment;
		RouteSegment currentSegment = null;
		while (nextCurrentSegment != null) {
			currentSegment = nextCurrentSegment;
			nextCurrentSegment = null;
			
			// 1. calculate obstacle for passing this segment 
			float segmentAndObstaclesTime = (float) calculateRouteSegmentTime(ctx, reverseWaySearch, currentSegment);
			if (segmentAndObstaclesTime < 0) {
				directionAllowed = false;
				break;
			}
			// calculate new start segment time as we're going to assign to put to visited segments
			float distFromStartPlusSegmentTime = currentSegment.distanceFromStart + segmentAndObstaclesTime;
			// 2. check if segment was already visited in opposite direction
			// We check before we calculate segmentTime (to not calculate it twice with opposite and calculate turns
			// onto each segment).
			boolean alreadyVisited = checkIfOppositeSegmentWasVisited(reverseWaySearch, graphSegments, currentSegment,
					oppositeSegments);
 			if (alreadyVisited) {
 				// we don't stop here in order to allow improve found *potential* final segment - test case on short route
				// directionAllowed = false;
				// break;
			}
 			
			// 3. upload segment itself to visited segments
			long nextPntId = calculateRoutePointId(currentSegment);
			RouteSegment existingSegment = visitedSegments.put(nextPntId, currentSegment);
			if (existingSegment != null) {
				if (distFromStartPlusSegmentTime > existingSegment.distanceFromStart) {
					// insert back original segment (test case with large area way)
					visitedSegments.put(nextPntId, existingSegment);
					directionAllowed = false;
					
					if (TRACE_ROUTING) {
						println("  >> Already visited");
					}
					break;
				} else {
					if (ctx.config.heuristicCoefficient <= 1) {
						if (RoutingContext.PRINT_ROUTING_ALERTS) {
							System.err.println("! ALERT slower segment was visited earlier " + distFromStartPlusSegmentTime + " > "
								+ existingSegment.distanceFromStart + ": " + currentSegment + " - " + existingSegment);
						} else {
							ctx.alertSlowerSegmentedWasVisitedEarlier++;
						}
					}
				}
			}
						
			// reassign @distanceFromStart to make it correct for visited segment
			currentSegment.distanceFromStart = distFromStartPlusSegmentTime;
			
			// 4. load road connections at the end of segment    
			nextCurrentSegment = processIntersections(ctx, graphSegments, visitedSegments, currentSegment, reverseWaySearch, doNotAddIntersections);
		}
		if (ctx.visitor != null) {
			ctx.visitor.visitSegment(startSegment, currentSegment.getSegmentEnd(), true);
		}
	}
	
	private boolean checkMovementAllowed(final RoutingContext ctx, boolean reverseWaySearch, RouteSegment segment) {
		boolean directionAllowed;
		int oneway = ctx.getRouter().isOneWay(segment.getRoad());
		// use positive direction as agreed
		if (!reverseWaySearch) {
			if (segment.isPositive()) {
				directionAllowed = oneway >= 0;
			} else {
				directionAllowed = oneway <= 0;
			}
		} else {
			if (segment.isPositive()) {
				directionAllowed = oneway <= 0;
			} else {
				directionAllowed = oneway >= 0;
			}
		}
		return directionAllowed;
	}



	private boolean checkViaRestrictions(RouteSegment from, RouteSegment to) {
		if (from != null && to != null) {
			long fid = to.getRoad().getId();
			for (int i = 0; i < from.getRoad().getRestrictionLength(); i++) {
				long id = from.getRoad().getRestrictionId(i);
				int tp = from.getRoad().getRestrictionType(i);
				if (fid == id) {
					if (tp == MapRenderingTypes.RESTRICTION_NO_LEFT_TURN
							|| tp == MapRenderingTypes.RESTRICTION_NO_RIGHT_TURN
							|| tp == MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON
							|| tp == MapRenderingTypes.RESTRICTION_NO_U_TURN) {
						return false;
					}
					break;
				}
				if (tp == MapRenderingTypes.RESTRICTION_ONLY_STRAIGHT_ON) {
					return false;
				}
			}
		}
		return true;
	}

	private RouteSegment getParentDiffId(RouteSegment s) {
		while (s.getParentRoute() != null && s.getParentRoute().getRoad().getId() == s.getRoad().getId()) {
			s = s.getParentRoute();
		}
		return s.getParentRoute();
	}

	private boolean checkIfOppositeSegmentWasVisited(boolean reverseWaySearch,
			PriorityQueue<RouteSegment> graphSegments, RouteSegment currentSegment, TLongObjectHashMap<RouteSegment> oppositeSegments) {
		// check inverse direction for opposite
		long currPoint = calculateRoutePointInternalId(currentSegment.getRoad(), 
				currentSegment.getSegmentEnd(), currentSegment.getSegmentStart());
		if (oppositeSegments.containsKey(currPoint)) {
			RouteSegment opposite = oppositeSegments.get(currPoint);
			RouteSegment curParent = getParentDiffId(currentSegment);
			RouteSegment oppParent = getParentDiffId(opposite);
			RouteSegment to = reverseWaySearch ? curParent : oppParent;
			RouteSegment from = !reverseWaySearch ? curParent : oppParent;
			if (checkViaRestrictions(from, to)) {
				FinalRouteSegment frs = new FinalRouteSegment(currentSegment.getRoad(),
						currentSegment.getSegmentStart(), currentSegment.getSegmentEnd());
				frs.setParentRoute(currentSegment.getParentRoute());
				frs.reverseWaySearch = reverseWaySearch;
				frs.distanceFromStart = opposite.distanceFromStart + currentSegment.distanceFromStart;
				frs.distanceToEnd = 0;
				frs.opposite = opposite;
				graphSegments.add(frs);
				if (TRACE_ROUTING) {
					printRoad("  >> Final segment : ", frs, reverseWaySearch);
				}
				return true;
			}
		}
		return false;
	}

	private long calculateRoutePointInternalId(final RouteDataObject road, int pntId, int nextPntId) {
		int positive = nextPntId - pntId;
		int pntLen = road.getPointsLength();
		if (pntId < 0 || nextPntId < 0 || pntId >= pntLen || nextPntId >= pntLen || (positive != -1 && positive != 1)) {
			// should be assert
			throw new IllegalStateException("Assert failed");
		}
		return (road.getId() << ROUTE_POINTS) + (pntId << 1) + (positive > 0 ? 1 : 0);
	}

	private long calculateRoutePointId(RouteSegment segm) {
		return calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentStart(), 
				segm.isPositive() ? segm.getSegmentStart() + 1 : segm.getSegmentStart() - 1);
		// return calculateRoutePointInternalId(segm.getRoad(), segm.getSegmentStart(), segm.getSegmentEnd()); 
	}


	private boolean proccessRestrictions(RoutingContext ctx, RouteSegment segment, RouteSegment inputNext, boolean reverseWay) {
		if (!ctx.getRouter().restrictionsAware()) {
			return false;
		}
		RouteDataObject road = segment.getRoad();
		RouteSegment parent = getParentDiffId(segment);
		if (!reverseWay && road.getRestrictionLength() == 0 &&
				(parent == null || parent.getRoad().getRestrictionLength() == 0)) {
			return false;
		}
		ctx.segmentsToVisitPrescripted.clear();
		ctx.segmentsToVisitNotForbidden.clear();
		processRestriction(ctx, inputNext, reverseWay, 0, road);
		if (parent != null) {
			processRestriction(ctx, inputNext, reverseWay, road.id, parent.getRoad());
		}
		return true;
	}


	protected void processRestriction(RoutingContext ctx, RouteSegment inputNext, boolean reverseWay, long viaId,
			RouteDataObject road) {
		boolean via = viaId != 0;
		RouteSegment next = inputNext;
		boolean exclusiveRestriction = false;
		while (next != null) {
			int type = -1;
			if (!reverseWay) {
				for (int i = 0; i < road.getRestrictionLength(); i++) {
					int rt = road.getRestrictionType(i);
					long rv = road.getRestrictionVia(i);
					if (road.getRestrictionId(i) == next.road.id) {
						if (!via || rv == viaId) {
							type = rt;
							break;
						}
					}
					if (rv == viaId && via && rt == MapRenderingTypes.RESTRICTION_ONLY_STRAIGHT_ON) {
						type = MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON;
						break;
					}
				}
			} else {
				for (int i = 0; i < next.road.getRestrictionLength(); i++) {
					int rt = next.road.getRestrictionType(i);
					long rv = next.road.getRestrictionVia(i);
					long restrictedTo = next.road.getRestrictionId(i);
					if (restrictedTo == road.id) {
						if (!via || rv == viaId) {
							type = rt;
							break;
						}
					}

					if (rv == viaId && via && rt == MapRenderingTypes.RESTRICTION_ONLY_STRAIGHT_ON) {
						type = MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON;
						break;
					}

					// Check if there is restriction only to the other than current road
					if (rt == MapRenderingTypes.RESTRICTION_ONLY_RIGHT_TURN || rt == MapRenderingTypes.RESTRICTION_ONLY_LEFT_TURN
							|| rt == MapRenderingTypes.RESTRICTION_ONLY_STRAIGHT_ON) {
						// check if that restriction applies to considered junk
						RouteSegment foundNext = inputNext;
						while (foundNext != null) {
							if (foundNext.getRoad().id == restrictedTo) {
								break;
							}
							foundNext = foundNext.next;
						}
						if (foundNext != null) {
							type = REVERSE_WAY_RESTRICTION_ONLY; // special constant
						}
					}
				}
			}
			if (type == REVERSE_WAY_RESTRICTION_ONLY) {
				// next = next.next; continue;
			} else if (type == -1 && exclusiveRestriction) {
				// next = next.next; continue;
			} else if (type == MapRenderingTypes.RESTRICTION_NO_LEFT_TURN || type == MapRenderingTypes.RESTRICTION_NO_RIGHT_TURN
					|| type == MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON || type == MapRenderingTypes.RESTRICTION_NO_U_TURN) {
				// next = next.next; continue;
				if (via) {
					ctx.segmentsToVisitPrescripted.remove(next);
				}
			} else if (type == -1) {
				// case no restriction
				ctx.segmentsToVisitNotForbidden.add(next);
			} else {
				if (!via) {
					// case exclusive restriction (only_right, only_straight, ...)
					// 1. in case we are going backward we should not consider only_restriction
					// as exclusive because we have many "in" roads and one "out"
					// 2. in case we are going forward we have one "in" and many "out"
					if (!reverseWay) {
						exclusiveRestriction = true;
						ctx.segmentsToVisitNotForbidden.clear();
						ctx.segmentsToVisitPrescripted.add(next);
					} else {
						ctx.segmentsToVisitNotForbidden.add(next);
					}
				}
			}
			next = next.next;
		}
		if (!via) {
			ctx.segmentsToVisitPrescripted.addAll(ctx.segmentsToVisitNotForbidden);
		}
	}

	private RouteSegment processIntersections(RoutingContext ctx, PriorityQueue<RouteSegment> graphSegments,
			TLongObjectHashMap<RouteSegment> visitedSegments,  RouteSegment currentSegment,
			boolean reverseWaySearch, boolean doNotAddIntersections) {
		RouteSegment nextCurrentSegment = null;
		int targetEndX = reverseWaySearch ? ctx.startX : ctx.targetX;
		int targetEndY = reverseWaySearch ? ctx.startY : ctx.targetY;
		final int x = currentSegment.getRoad().getPoint31XTile(currentSegment.getSegmentEnd());
		final int y = currentSegment.getRoad().getPoint31YTile(currentSegment.getSegmentEnd());
		float distanceToEnd = h(ctx, x, y, targetEndX, targetEndY);
		// reassign @distanceToEnd to make it correct for visited segment
		currentSegment.distanceToEnd = distanceToEnd; 
		
		final RouteSegment connectedNextSegment = ctx.loadRouteSegment(x, y, ctx.config.memoryLimitation - ctx.memoryOverhead, reverseWaySearch);
		RouteSegment roadIter = connectedNextSegment;
		boolean directionAllowed = true;
		boolean singleRoad = true;
		while (roadIter != null) {
			if (currentSegment.getSegmentEnd() == roadIter.getSegmentStart() && roadIter.road.getId() == currentSegment.getRoad().getId() ) {
				nextCurrentSegment = roadIter.initRouteSegment(currentSegment.isPositive());
				if (nextCurrentSegment == null) { 
					// end of route (-1 or length + 1)
					directionAllowed = false;
				} else {
					if (nextCurrentSegment.isSegmentAttachedToStart()) {
						directionAllowed = processOneRoadIntersection(ctx, reverseWaySearch, null, 
								visitedSegments, currentSegment, nextCurrentSegment);
					} else {
						nextCurrentSegment.setParentRoute(currentSegment);
						nextCurrentSegment.distanceFromStart = currentSegment.distanceFromStart;
						nextCurrentSegment.distanceToEnd = distanceToEnd;
						final int nx = nextCurrentSegment.getRoad().getPoint31XTile(nextCurrentSegment.getSegmentEnd());
						final int ny = nextCurrentSegment.getRoad().getPoint31YTile(nextCurrentSegment.getSegmentEnd());
						if (nx == x && ny == y) {
							// don't process other intersections (let process further segment)
							return nextCurrentSegment;
						}
					}
				}
			} else {
				singleRoad = false;
			}
			roadIter = roadIter.getNext();
		}
		
		if (singleRoad) {
			return nextCurrentSegment;
		}
		
		// find restrictions and iterator
		Iterator<RouteSegment> nextIterator = null;
		boolean thereAreRestrictions = proccessRestrictions(ctx, currentSegment, connectedNextSegment, reverseWaySearch);
		if (thereAreRestrictions) {
			nextIterator = ctx.segmentsToVisitPrescripted.iterator();
			if (TRACE_ROUTING) {
				println("  >> There are restrictions");
			}
		}
		
		// Calculate possible turns to put into priority queue
		RouteSegment next = connectedNextSegment;
		boolean hasNext = nextIterator != null ? nextIterator.hasNext() : next != null;
		while (hasNext) {
			if (nextIterator != null) {
				next = nextIterator.next();
			}
			if (next.getSegmentStart() == currentSegment.getSegmentEnd() && 
					next.getRoad().getId() == currentSegment.getRoad().getId()) {
				// skip itself
			} else if (!doNotAddIntersections) {
				RouteSegment nextPos = next.initRouteSegment(true);
				processOneRoadIntersection(ctx, reverseWaySearch, graphSegments, visitedSegments, currentSegment, nextPos);
				RouteSegment nextNeg = next.initRouteSegment(false);
				processOneRoadIntersection(ctx, reverseWaySearch, graphSegments, visitedSegments, currentSegment, nextNeg);
			}
			// iterate to next road
			if (nextIterator == null) {
				next = next.next;
				hasNext = next != null;
			} else {
				hasNext = nextIterator.hasNext();
			}
		}
		
		if (nextCurrentSegment == null && directionAllowed) {
			if (ctx.calculationMode != RouteCalculationMode.BASE) {
				// exception as it should not occur 
				// To do: happens anyway during approximation (should be investigated)
//				throw new IllegalStateException();
			} else {
				//  Issue #13284: we know that bug in data (how we simplify base data and connect between regions), so we workaround it
				int newEnd = currentSegment.getSegmentEnd() + (currentSegment.isPositive() ? +1 :-1);
				if (newEnd >= 0 && newEnd < currentSegment.getRoad().getPointsLength() - 1) {
					nextCurrentSegment = new RouteSegment(currentSegment.getRoad(), currentSegment.getSegmentEnd(),
							newEnd);
					nextCurrentSegment.setParentRoute(currentSegment);
					nextCurrentSegment.distanceFromStart = currentSegment.distanceFromStart;
					nextCurrentSegment.distanceToEnd = distanceToEnd;
				}
			}
		}
		return nextCurrentSegment;
	}

	private boolean processOneRoadIntersection(RoutingContext ctx, boolean reverseWaySearch, PriorityQueue<RouteSegment> graphSegments,
			TLongObjectHashMap<RouteSegment> visitedSegments, RouteSegment segment, RouteSegment next) {
		if (next != null) {
			if (!checkMovementAllowed(ctx, reverseWaySearch, next)) {
				return false;
			}
			float obstaclesTime = (float) ctx.getRouter().calculateTurnTime(next, 
					next.isPositive() ? next.getRoad().getPointsLength() - 1 : 0,    
					segment, segment.getSegmentEnd());
			if (obstaclesTime < 0) {
				return false;
			}
			float distFromStart = obstaclesTime + segment.distanceFromStart;
			if (TEST_SPECIFIC && next.road.getId() >> 6 == TEST_ID) {
				printRoad(" !? distFromStart=" + distFromStart + " from " + segment.getRoad().getId() +
						" distToEnd=" + segment.distanceFromStart +
						" segmentPoint=" + segment.getSegmentEnd() + " -- ", next, null);
			}
			RouteSegment visIt = visitedSegments.get(calculateRoutePointId(next));
			boolean toAdd = true;
			if (visIt != null) {
				if (TRACE_ROUTING) {
					printRoad("  >?", visitedSegments.get(calculateRoutePointId(next)), null);
				}
				toAdd = false;
				// The segment was already visited! We can try to follow new route if it's shorter.
				// That is very exceptional situation and almost exception, it can happen
				// 1. We underestimate distanceToEnd - wrong h() of A* (heuristic > 1)
				// 2. We don't process small segments 1 by 1 from the queue but the whole road, 
				//  and it could be that deviation from the road is faster than following the whole road itself!
				if (distFromStart < visIt.distanceFromStart) {
					double routeSegmentTime = calculateRouteSegmentTime(ctx, reverseWaySearch, visIt);
					// we need to properly compare @distanceFromStart VISITED and NON-VISITED segment
					if (distFromStart + routeSegmentTime < visIt.distanceFromStart) {
						// Here it's not very legitimate action cause in theory we need to go up to the final segment in the queue & decrease final time
						// But it's compensated by chain reaction cause next.distanceFromStart < finalSegment.distanceFromStart and revisiting all segments
						
						// We don't check ```next.getParentRoute() == null``` cause segment could be unloaded
						// so we need to add segment back to the queue & reassign the parent (same as for next.getParentRoute() == null)
						toAdd = true;
						if (ctx.config.heuristicCoefficient <= 1) {
							if (RoutingContext.PRINT_ROUTING_ALERTS) {
								System.err.println("! ALERT new faster path to a visited segment: "
										+ (distFromStart + routeSegmentTime) + " < " + visIt.distanceFromStart + ": " + next + " - " + visIt);
							} else {
								ctx.alertFasterRoadToVisitedSegments++;
							}
						}
						// ??? It's not clear whether this block is needed or not ???
						// All Test cases work with and without it
						// visIt.setParentRoute(segment);
						// visIt.distanceFromStart = (float) (distFromStart + routeSegmentTime);
						// visIt.distanceToEnd = segment.distanceToEnd;
						// ???
					}
				}
				
			}
			if (toAdd && (!next.isSegmentAttachedToStart() || ctx.roadPriorityComparator(next.distanceFromStart,
					next.distanceToEnd, distFromStart, segment.distanceToEnd) > 0)) {
				next.distanceFromStart = distFromStart;
				next.distanceToEnd = segment.distanceToEnd;
				if (TRACE_ROUTING) {
					printRoad(" " + segment.getSegmentEnd() + ">>", next, null);
				}
				// put additional information to recover whole route after
				next.setParentRoute(segment);
				if (graphSegments != null) {
					graphSegments.add(next);
				}
				return true;
			}
		}
		return false;
	}
	

	/*public */static int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, 
			double o2DistanceFromStart, double o2DistanceToEnd, double heuristicCoefficient ) {
		// f(x) = g(x) + h(x)  --- g(x) - distanceFromStart, h(x) - distanceToEnd (not exact)
		return Double.compare(o1DistanceFromStart + heuristicCoefficient * o1DistanceToEnd, 
				o2DistanceFromStart + heuristicCoefficient *  o2DistanceToEnd);
	}

	
	public interface RouteSegmentVisitor {
		
		public void visitSegment(RouteSegment segment, int segmentEnd, boolean poll);
		public void visitApproximatedSegments(List<RouteSegmentResult> segment, GpxPoint start, GpxPoint target);
	}
	
	public static class RouteSegmentPoint extends RouteSegment {
		
		public RouteSegmentPoint(RouteDataObject road, int segmentStart, double distSquare) {
			super(road, segmentStart);
			this.distSquare = distSquare;
			this.preciseX = road.getPoint31XTile(segmentStart);
			this.preciseY = road.getPoint31YTile(segmentStart);
		}
		
		public RouteSegmentPoint(RouteSegmentPoint pnt) {
			super(pnt.road, pnt.segStart, pnt.segEnd);
			this.distSquare = pnt.distSquare;
			this.preciseX = pnt.preciseX;
			this.preciseY = pnt.preciseY;
		}

		public double distSquare;
		public int preciseX;
		public int preciseY;
		public List<RouteSegmentPoint> others;
		
		public LatLon getPreciseLatLon() {
			return new LatLon(MapUtils.get31LatitudeY(preciseY), MapUtils.get31LongitudeX(preciseX));
			
		}
		
		@Override
		public String toString() {
			return String.format("%d (%s): %s", segStart, getPreciseLatLon(), road);
		}
		
	}

	// Route segment represents part (segment) of the road. 
	// In our current data it's always length of 1: [X, X + 1] or [X - 1, X] 
	public static class RouteSegment {
		
		// # Represents parent segment for Start & End segment 
		public static final RouteSegment NULL = new RouteSegment(null, 0, 1);
		
		// # Final fields that store objects 
		final short segStart;
		final short segEnd;
		final RouteDataObject road;
		
		// # Represents cheap-storage of LinkedList connected segments
		// All the road segments from map data connected to the same end point   
		RouteSegment nextLoaded = null;
		// Segments only allowed for Navigation connected to the same end point 
		RouteSegment next = null;
		
		// # Caches of similar segments to speed up routing calculation 
		// Segment of opposite direction i.e. for [4 -> 5], opposite [5 -> 4]
		RouteSegment oppositeDirection = null;
		// Same Road/ same Segment but used for opposite A* search (important to have different cause #parentRoute is different)
		// Note: if we use 1-direction A* then this is field is not needed
		RouteSegment reverseSearch = null;

		// # Important for A*-search to distinguish whether segment was visited or not
		// Initially all segments null and startSegment/endSegment.parentRoute = RouteSegment.NULL;
		// After iteration stores previous segment i.e. how it was reached from startSegment
		RouteSegment parentRoute = null;

		// # A* routing - Distance measured in time (seconds)
		// There is a small (important!!!) difference how it's calculated for visited (parentRoute != null) and non-visited
		// NON-VISITED: time from Start [End for reverse A*] to @segStart of @this, including turn time from previous segment (@parentRoute)
		// VISITED: time from Start [End for reverse A*] to @segEnd of @this, 
		//          including turn time from previous segment (@parentRoute) and obstacle / distance time between @segStart-@segEnd on @this 
		float distanceFromStart = 0;
		// NON-VISITED: Approximated (h(x)) time from @segStart of @this route segment to End [Start for reverse A*] 
		// VISITED: Approximated (h(x)) time from @segEnd of @this route segment to End [Start for reverse A*]
		float distanceToEnd = 0;

		public RouteSegment(RouteDataObject road, int segmentStart, int segmentEnd) {
			this.road = road;
			this.segStart = (short) segmentStart;
			this.segEnd = (short) segmentEnd;
		}
		
		public RouteSegment(RouteDataObject road, int segmentStart) {
			this(road, segmentStart, segmentStart < road.getPointsLength() - 1 ? segmentStart + 1 : segmentStart - 1);
		}

		public RouteSegment initRouteSegment(boolean positiveDirection) {
			if (segStart == 0 && !positiveDirection) {
				return null;
			}
			if (segStart == road.getPointsLength() - 1 && positiveDirection) {
				return null;
			}
			if (segStart == segEnd) {
				throw new IllegalArgumentException();
			} else {
				if (positiveDirection == (segEnd > segStart)) {
					return this;
				} else {
					if (oppositeDirection == null) {
						oppositeDirection = new RouteSegment(road, segStart,
								segEnd > segStart ? (segStart - 1) : (segStart + 1));
						oppositeDirection.oppositeDirection = this;
					}
					return oppositeDirection;
				}
			}
		}
		
		public boolean isSegmentAttachedToStart() {
			return parentRoute != null;
		}

		public RouteSegment getParentRoute() {
			return parentRoute == NULL ? null : parentRoute;
		}

		public boolean isPositive() {
			return segEnd > segStart;
		}

		public void setParentRoute(RouteSegment parentRoute) {
			this.parentRoute = parentRoute;
		}

		public RouteSegment getNext() {
			return next;
		}

		public short getSegmentStart() {
			return segStart;
		}
		
		public short getSegmentEnd() {
			return segEnd;
		}

		public float getDistanceFromStart() {
			return distanceFromStart;
		}

		public void setDistanceFromStart(float distanceFromStart) {
			this.distanceFromStart = distanceFromStart;
		}

		public RouteDataObject getRoad() {
			return road;
		}

		public String getTestName() {
			return MessageFormat.format("s{0,number,#.##} e{1,number,#.##}", ((float) distanceFromStart), ((float) distanceToEnd));
		}
		
		@Override
		public String toString() {
			int x = road.getPoint31XTile(segStart);
			int y = road.getPoint31YTile(segStart);
			int xe = road.getPoint31XTile(segEnd);
			int ye = road.getPoint31YTile(segEnd);
			float dst = ((int)(Math.sqrt(MapUtils.squareDist31TileMetric(x, y, xe, ye)) * 10)) / 10.0f;
			return (road == null ? "NULL" : road.toString()) + " [" + segStart +"-" +segEnd+"] " + dst + " m";
		}


		public Iterator<RouteSegment> getIterator() {
			return new Iterator<BinaryRoutePlanner.RouteSegment>() {
				RouteSegment next = RouteSegment.this;

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

				@Override
				public RouteSegment next() {
					RouteSegment c = next;
					if (next != null) {
						next = next.next;
					}
					return c;
				}

				@Override
				public boolean hasNext() {
					return next != null;
				}
			};
		}
	}

	static class FinalRouteSegment extends RouteSegment {

		boolean reverseWaySearch;
		RouteSegment opposite;

		public FinalRouteSegment(RouteDataObject road, int segmentStart, int segmentEnd) {
			super(road, segmentStart, segmentEnd);
		}

	}

}
