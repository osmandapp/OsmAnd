package net.osmand.router;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class BinaryRoutePlanner {
	
	private static final int TEST_ID = 235108903;
	private static final boolean TEST_SPECIFIC = false;
	
	private static final int REVERSE_WAY_RESTRICTION_ONLY = 1024;
	/*private*/ static final int STANDARD_ROAD_IN_QUEUE_OVERHEAD = 220;
	/*private*/ static final int STANDARD_ROAD_VISITED_OVERHEAD = 150;
	
	protected static final Log log = PlatformUtil.getLog(BinaryRoutePlanner.class);
	
	private static final int ROUTE_POINTS = 11;
	private static final boolean TRACE_ROUTING = false;
	
	
	public static double squareRootDist(int x1, int y1, int x2, int y2) {
		// translate into meters 
		
		double dy = MapUtils.convert31YToMeters(y1, y2);
		double dx = MapUtils.convert31XToMeters(x1, x2);
		return Math.sqrt(dx * dx + dy * dy);
//		return measuredDist(x1, y1, x2, y2);
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
	@SuppressWarnings("unused")
	FinalRouteSegment searchRouteInternal(final RoutingContext ctx, RouteSegment start, RouteSegment end) throws InterruptedException, IOException {
		// measure time
		ctx.timeToLoad = 0;
		ctx.visitedSegments = 0;
		ctx.memoryOverhead  = 1000;
		ctx.timeToCalculate = System.nanoTime();
		// Initializing priority queue to visit way segments 
		Comparator<RouteSegment> nonHeuristicSegmentsComparator = new NonHeuristicSegmentsComparator();
		PriorityQueue<RouteSegment> graphDirectSegments = new PriorityQueue<RouteSegment>(50, new SegmentsComparator(ctx));
		PriorityQueue<RouteSegment> graphReverseSegments = new PriorityQueue<RouteSegment>(50, new SegmentsComparator(ctx));
		
		// Set to not visit one segment twice (stores road.id << X + segmentStart)
		TLongObjectHashMap<RouteSegment> visitedDirectSegments = new TLongObjectHashMap<RouteSegment>();
		TLongObjectHashMap<RouteSegment> visitedOppositeSegments = new TLongObjectHashMap<RouteSegment>();
		
		
		initQueuesWithStartEnd(ctx, start, end, graphDirectSegments, graphReverseSegments);
		
		// Extract & analyze segment with min(f(x)) from queue while final segment is not found
		boolean forwardSearch = true;
		
		PriorityQueue<RouteSegment>  graphSegments = graphDirectSegments;
		
		FinalRouteSegment finalSegment = null;
		boolean onlyBackward = ctx.getPlanRoadDirection() < 0;
		boolean onlyForward = ctx.getPlanRoadDirection() > 0;
		while (!graphSegments.isEmpty()) {
			RouteSegment segment = graphSegments.poll();
			// use accumulative approach
			ctx.memoryOverhead = (visitedDirectSegments.size() + visitedOppositeSegments.size()) * STANDARD_ROAD_VISITED_OVERHEAD + 
					(graphDirectSegments.size() +
					graphReverseSegments.size()) * STANDARD_ROAD_IN_QUEUE_OVERHEAD;
			
			if(TRACE_ROUTING){
				printRoad(">", segment, !forwardSearch);
			}
			if(segment instanceof FinalRouteSegment) {
				if(RoutingContext.SHOW_GC_SIZE){
					log.warn("Estimated overhead " + (ctx.memoryOverhead / (1<<20))+ " mb");
					printMemoryConsumption("Memory occupied after calculation : ");
				}
				finalSegment = (FinalRouteSegment) segment;
				if(TRACE_ROUTING){
					println("Final segment found");
				}
				break;
			}
			if (ctx.memoryOverhead > ctx.config.memoryLimitation * 0.95 && RoutingContext.SHOW_GC_SIZE) {
				printMemoryConsumption("Memory occupied before exception : ");
			}
			if(ctx.memoryOverhead > ctx.config.memoryLimitation * 0.95) {
				throw new IllegalStateException("There is no enough memory " + ctx.config.memoryLimitation/(1<<20) + " Mb");
			}
			ctx.visitedSegments++;
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
			if(ctx.getPlanRoadDirection() <= 0 && graphReverseSegments.isEmpty()){
				throw new IllegalArgumentException("Route is not found to selected target point.");
			}
			if(ctx.getPlanRoadDirection() >= 0 && graphDirectSegments.isEmpty()){
				throw new IllegalArgumentException("Route is not found from selected start point.");
			}
			if (ctx.planRouteIn2Directions()) {
				forwardSearch = (nonHeuristicSegmentsComparator.compare(graphDirectSegments.peek(), graphReverseSegments.peek()) < 0);
//				if (graphDirectSegments.size() * 2 > graphReverseSegments.size()) {
//					forwardSearch = false;
//				} else if (graphDirectSegments.size() < 2 * graphReverseSegments.size()) {
//					forwardSearch = true;
//				}
			} else {
				// different strategy : use onedirectional graph
				forwardSearch = onlyForward;
				if(onlyBackward && !graphDirectSegments.isEmpty()) {
					forwardSearch = true;
				}
				if(onlyForward && !graphReverseSegments.isEmpty()) {
					forwardSearch = false;
				}
			}
			
			if (forwardSearch) {
				graphSegments = graphDirectSegments;
			} else {
				graphSegments = graphReverseSegments;
			}
			// check if interrupted
			if(ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				throw new InterruptedException("Route calculation interrupted");
			}
		}
		printDebugMemoryInformation(ctx, graphDirectSegments, graphReverseSegments, visitedDirectSegments, visitedOppositeSegments);
		return finalSegment;
	}


	private void initQueuesWithStartEnd(final RoutingContext ctx, RouteSegment start, RouteSegment end,
			PriorityQueue<RouteSegment> graphDirectSegments, PriorityQueue<RouteSegment> graphReverseSegments) {
		RouteSegment startPos = start.initRouteSegment(true);
		RouteSegment startNeg = start.initRouteSegment(false);
		RouteSegment endPos = end.initRouteSegment(true);
		RouteSegment endNeg = end.initRouteSegment(false);
		// for start : f(start) = g(start) + h(start) = 0 + h(start) = h(start)
		if(ctx.config.initialDirection != null) {
			// mark here as positive for further check
			double plusDir = start.getRoad().directionRoute(start.getSegmentStart(), true);
			double diff	 = plusDir - ctx.config.initialDirection;
			if(Math.abs(MapUtils.alignAngleDifference(diff)) <= Math.PI / 3) {
				if(startNeg != null) {
					startNeg.distanceFromStart += 500;
				}
			} else if(Math.abs(MapUtils.alignAngleDifference(diff - Math.PI)) <= Math.PI / 3) {
				if(startPos != null) {
					startPos.distanceFromStart += 500;
				}
			}
		}
		float estimatedDistance = (float) estimatedDistance(ctx, ctx.targetX, ctx.targetY, ctx.startX, ctx.startY);
		if(startPos != null) {
			startPos.distanceToEnd = estimatedDistance;
			graphDirectSegments.add(startPos);
		}
		if(startNeg != null) {
			startNeg.distanceToEnd = estimatedDistance;
			graphDirectSegments.add(startNeg);
		}
		if(endPos != null) {
			endPos.distanceToEnd = estimatedDistance;
			graphReverseSegments.add(endPos);
		}
		if(endNeg != null) {
			endNeg.distanceToEnd = estimatedDistance;
			graphReverseSegments.add(endNeg);
		}
	}


	private void printMemoryConsumption( String string) {
		long h1 = RoutingContext.runGCUsedMemory();
		float mb = (1 << 20);
		log.warn(string + h1 / mb);		
	}


	private void updateCalculationProgress(final RoutingContext ctx, PriorityQueue<RouteSegment> graphDirectSegments,
			PriorityQueue<RouteSegment> graphReverseSegments) {
		if(ctx.calculationProgress != null) {
			ctx.calculationProgress.reverseSegmentQueueSize = graphReverseSegments.size();
			ctx.calculationProgress.directSegmentQueueSize = graphDirectSegments.size();
			if(graphDirectSegments.size() > 0 && ctx.getPlanRoadDirection() >= 0) {
				RouteSegment peek = graphDirectSegments.peek();
				ctx.calculationProgress.distanceFromBegin = Math.max(peek.distanceFromStart, 
						ctx.calculationProgress.distanceFromBegin);
				ctx.calculationProgress.directDistance = peek.distanceFromStart + peek.distanceToEnd;
			}
			if(graphReverseSegments.size() > 0 && ctx.getPlanRoadDirection() <= 0) {
				RouteSegment peek = graphReverseSegments.peek();
				ctx.calculationProgress.distanceFromEnd = Math.max(peek.distanceFromStart + peek.distanceToEnd,
							ctx.calculationProgress.distanceFromEnd);
				ctx.calculationProgress.reverseDistance = peek.distanceFromStart + peek.distanceToEnd;
			}
		}
	}


	private void printRoad(String prefix, RouteSegment segment, Boolean reverseWaySearch) {
		String pr;
		if(segment.parentRoute != null){
			pr = " pend="+segment.parentSegmentEnd +" parent=" + segment.parentRoute.road;
		} else {
			pr = "";
		}
		String p = "";
		if(reverseWaySearch != null) {
			p = (reverseWaySearch?"B" : "F");
		}
		println(p+prefix  +"" + segment.road + " dir="+segment.getDirectionAssigned()+" ind=" + segment.getSegmentStart() + 
				" ds=" + ((float)segment.distanceFromStart) + " es="+((float)segment.distanceToEnd) + pr);
	}

	private float estimatedDistance(final RoutingContext ctx, int targetEndX, int targetEndY,
			int startX, int startY) {
		double distance = squareRootDist(startX, startY, targetEndX, targetEndY);
		return (float) (distance / ctx.getRouter().getMaxDefaultSpeed());
	}
	
	protected static float h(RoutingContext ctx, int begX, int begY, int endX, int endY) {
		double distToFinalPoint = squareRootDist(begX, begY,  endX, endY);
		double result = distToFinalPoint / ctx.getRouter().getMaxDefaultSpeed();
		if(ctx.precalculatedRouteDirection != null){
			float te = ctx.precalculatedRouteDirection.timeEstimate(begX, begY,  endX, endY);
			if(te > 0) {
				return te;
			}
		}
		return (float) result; 
	}
	
	
	
	private static void println(String logMsg) {
//		log.info(logMsg);
		System.out.println(logMsg);
	}
	
	private static void printInfo(String logMsg) {
		log.warn(logMsg);
	}
	
	public void printDebugMemoryInformation(RoutingContext ctx, PriorityQueue<RouteSegment> graphDirectSegments, PriorityQueue<RouteSegment> graphReverseSegments, 
			TLongObjectHashMap<RouteSegment> visitedDirectSegments,TLongObjectHashMap<RouteSegment> visitedOppositeSegments) {
		printInfo("Time to calculate : " + (System.nanoTime() - ctx.timeToCalculate) / 1e6 + 
				", time to load : " + ctx.timeToLoad / 1e6 + ", time to load headers : " + ctx.timeToLoadHeaders / 1e6 + 
				", time to calc dev : " + ctx.timeNanoToCalcDeviation/ 1e6);
		int maxLoadedTiles = Math.max(ctx.maxLoadedTiles, ctx.getCurrentlyLoadedTiles());
		printInfo("Current loaded tiles : " + ctx.getCurrentlyLoadedTiles() + ", maximum loaded tiles " + maxLoadedTiles);
		printInfo("Loaded tiles " + ctx.loadedTiles + " (distinct "+ctx.distinctLoadedTiles+ "), unloaded tiles " + ctx.unloadedTiles + 
				", loaded more than once same tiles "
				+ ctx.loadedPrevUnloadedTiles );
		printInfo("Visited roads " + ctx.visitedSegments + ", relaxed roads " + ctx.relaxedSegments);
		if (graphDirectSegments != null && graphReverseSegments != null) {
			printInfo("Priority queues sizes : " + graphDirectSegments.size() + "/" + graphReverseSegments.size());
		}
		if (visitedDirectSegments != null && visitedOppositeSegments != null) {
			printInfo("Visited interval sizes: " + visitedDirectSegments.size() + "/" + visitedOppositeSegments.size());
		}
		
	}
	
	
	@SuppressWarnings("unused")
	private void processRouteSegment(final RoutingContext ctx, boolean reverseWaySearch,
			PriorityQueue<RouteSegment> graphSegments, TLongObjectHashMap<RouteSegment> visitedSegments, 
            RouteSegment segment, TLongObjectHashMap<RouteSegment> oppositeSegments, boolean doNotAddIntersections) throws IOException {
		final RouteDataObject road = segment.road;
		boolean initDirectionAllowed = checkIfInitialMovementAllowedOnSegment(ctx, reverseWaySearch, visitedSegments, segment, road);
		if(TEST_SPECIFIC && road.getId() == TEST_ID ) {
			printRoad(" ! " + +segment.distanceFromStart + " ", segment, reverseWaySearch);
		}
		boolean directionAllowed = initDirectionAllowed;
		if(!directionAllowed) {
			if(TRACE_ROUTING) {
				println("  >> Already visited");
			}
			return;
		}
		// Go through all point of the way and find ways to continue
		// ! Actually there is small bug when there is restriction to move forward on the way (it doesn't take into account)
		float obstaclesTime = 0;
		float segmentDist = 0;
		// +/- diff from middle point
		short segmentPoint = segment.getSegmentStart();
		boolean[] processFurther = new boolean[1];
		RouteSegment previous = segment;
		boolean dir = segment.isPositive();
		while (directionAllowed) {
			// mark previous interval as visited and move to next intersection
			short prevInd = segmentPoint;
			if(dir) {
				segmentPoint ++;
			} else {
				segmentPoint --;
			}
			if (segmentPoint < 0 || segmentPoint >= road.getPointsLength()) {
				directionAllowed = false;
				continue;
			}
			// store <segment> in order to not have unique <segment, direction> in visitedSegments 
			visitedSegments.put(calculateRoutePointId(segment.getRoad(), segment.isPositive() ? segmentPoint - 1 : segmentPoint, 
					segment.isPositive()), previous != null ? previous : segment);
			final int x = road.getPoint31XTile(segmentPoint);
			final int y = road.getPoint31YTile(segmentPoint);
			final int prevx = road.getPoint31XTile(prevInd);
			final int prevy = road.getPoint31YTile(prevInd);
			if(x == prevx && y == prevy) {
				continue;
			}
			
			// 2. calculate point and try to load neighbor ways if they are not loaded
			segmentDist  += squareRootDist(x, y,  prevx, prevy);
			
			// 2.1 calculate possible obstacle plus time
			double obstacle = ctx.getRouter().defineRoutingObstacle(road, segmentPoint);
			if (obstacle < 0) {
				directionAllowed = false;
				continue;
			}
			obstaclesTime += obstacle;
			
			boolean alreadyVisited = checkIfOppositieSegmentWasVisited(ctx, reverseWaySearch, graphSegments, segment, oppositeSegments, 
					segmentPoint, segmentDist, obstaclesTime);
			if (alreadyVisited) {
				directionAllowed = false;
				continue;
			}
			// correct way of handling precalculatedRouteDirection 
			if(ctx.precalculatedRouteDirection != null) {
//				long nt = System.nanoTime();
//				float devDistance = ctx.precalculatedRouteDirection.getDeviationDistance(x, y);
//				// 1. linear method
//				 segmentDist = segmentDist * (1 + devDistance / ctx.config.DEVIATION_RADIUS);
//				// 2. exponential method
//				segmentDist = segmentDist * (float) Math.pow(1.5, devDistance / 500);
				// 3. next by method
//				segmentDist = devDistance ;
//				ctx.timeNanoToCalcDeviation += (System.nanoTime() - nt);
			}
			// could be expensive calculation
			// 3. get intersected ways
			final RouteSegment roadNext = ctx.loadRouteSegment(x, y, ctx.config.memoryLimitation - ctx.memoryOverhead);
			float distStartObstacles = segment.distanceFromStart + calculateTimeWithObstacles(ctx, road, segmentDist , obstaclesTime);
			if(ctx.precalculatedRouteDirection != null && ctx.precalculatedRouteDirection.isFollowNext()) {
				// reset to f
//				distStartObstacles = 0;
				// more precise but slower
				distStartObstacles = ctx.precalculatedRouteDirection.getDeviationDistance(x, y) / ctx.getRouter().getMaxDefaultSpeed();
			}
			
			// We don't check if there are outgoing connections
			previous = processIntersections(ctx, graphSegments, visitedSegments, distStartObstacles,
					segment, segmentPoint, roadNext, reverseWaySearch, doNotAddIntersections, processFurther);
			if (!processFurther[0]) {
				directionAllowed = false;
				continue;
			}
		}
		if(initDirectionAllowed && ctx.visitor != null){
			ctx.visitor.visitSegment(segment, segmentPoint, true);
		}
	}

	private boolean checkIfInitialMovementAllowedOnSegment(final RoutingContext ctx, boolean reverseWaySearch,
			TLongObjectHashMap<RouteSegment> visitedSegments, RouteSegment segment, final RouteDataObject road) {
		boolean directionAllowed;
		int oneway = ctx.getRouter().isOneWay(road);
		// use positive direction as agreed
		if (!reverseWaySearch) {
			if(segment.isPositive()){
				directionAllowed = oneway >= 0;
			} else {
				directionAllowed = oneway <= 0;
			}
		} else {
			if(segment.isPositive()){
				directionAllowed = oneway <= 0;
			} else {
				directionAllowed = oneway >= 0;
			}
		}
		
		if(directionAllowed && visitedSegments.containsKey(calculateRoutePointId(segment, segment.isPositive()))) {
			directionAllowed = false;
		}
		return directionAllowed;
	}

	private boolean checkIfOppositieSegmentWasVisited(final RoutingContext ctx, boolean reverseWaySearch,
			PriorityQueue<RouteSegment> graphSegments, RouteSegment segment, TLongObjectHashMap<RouteSegment> oppositeSegments,
			int segmentPoint, float segmentDist, float obstaclesTime) {
		RouteDataObject road = segment.getRoad();
		long opp = calculateRoutePointId(road, segment.isPositive() ? segmentPoint - 1 : segmentPoint, !segment.isPositive());
		if (oppositeSegments.containsKey(opp)) {
			RouteSegment opposite = oppositeSegments.get(opp);
			FinalRouteSegment frs = new FinalRouteSegment(road, segmentPoint);
			float distStartObstacles = segment.distanceFromStart
					+ calculateTimeWithObstacles(ctx, road, segmentDist, obstaclesTime);
			frs.setParentRoute(segment);
			frs.setParentSegmentEnd(segmentPoint);
			frs.reverseWaySearch = reverseWaySearch;
			frs.distanceFromStart = opposite.distanceFromStart + distStartObstacles;
			frs.distanceToEnd = 0;
			frs.opposite = opposite;
			graphSegments.add(frs);
			if(TRACE_ROUTING){
				printRoad("  >> Final segment : ", frs, reverseWaySearch);
			}
			return true;
		}
		return false;
	}
	

	private float calculateTimeWithObstacles(RoutingContext ctx, RouteDataObject road, float distOnRoadToPass, float obstaclesTime) {
		float priority = ctx.getRouter().defineSpeedPriority(road);
		float speed = (ctx.getRouter().defineRoutingSpeed(road) * priority);
		if (speed == 0) {
			speed = (ctx.getRouter().getMinDefaultSpeed() * priority);
		}
		// speed can not exceed max default speed according to A*
		if(speed > ctx.getRouter().getMaxDefaultSpeed()) {
			speed = ctx.getRouter().getMaxDefaultSpeed();
		}
		return obstaclesTime + distOnRoadToPass / speed;
	}

	private long calculateRoutePointId(final RouteDataObject road, int intervalId, boolean positive) {
		if(intervalId < 0) {
			// should be assert
			throw new IllegalStateException("Assert failed");
		}
		return (road.getId() << ROUTE_POINTS) + (intervalId << 1) + (positive ? 1 : 0);
	}
	
	private long calculateRoutePointId(RouteSegment segm, boolean direction) {
		if(segm.getSegmentStart() == 0 && !direction) {
			throw new IllegalStateException("Assert failed");	
		}
		if(segm.getSegmentStart() == segm.getRoad().getPointsLength() - 1 && direction) {
			throw new IllegalStateException("Assert failed");	
		}
		return calculateRoutePointId(segm.getRoad(),
				direction ? segm.getSegmentStart() : segm.getSegmentStart() - 1, direction);
	}


	private boolean proccessRestrictions(RoutingContext ctx, RouteDataObject road, RouteSegment inputNext, boolean reverseWay) {
		ctx.segmentsToVisitPrescripted.clear();
		ctx.segmentsToVisitNotForbidden.clear();
		boolean exclusiveRestriction = false;
		RouteSegment next = inputNext;
		if (!reverseWay && road.getRestrictionLength() == 0) {
			return false;
		}
		if(!ctx.getRouter().restrictionsAware()) {
			return false;
		}
		while (next != null) {
			int type = -1;
			if (!reverseWay) {
				for (int i = 0; i < road.getRestrictionLength(); i++) {
					if (road.getRestrictionId(i) == next.road.id) {
						type = road.getRestrictionType(i);
						break;
					}
				}
			} else {
				for (int i = 0; i < next.road.getRestrictionLength(); i++) {
					int rt = next.road.getRestrictionType(i);
					long restrictedTo = next.road.getRestrictionId(i);
					if (restrictedTo == road.id) {
						type = rt;
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
			} else if (type == -1) {
				// case no restriction
				ctx.segmentsToVisitNotForbidden.add(next);
			} else {
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
			next = next.next;
		}
		ctx.segmentsToVisitPrescripted.addAll(ctx.segmentsToVisitNotForbidden);
		return true;
	}
	
	


	private RouteSegment processIntersections(RoutingContext ctx, PriorityQueue<RouteSegment> graphSegments,
			TLongObjectHashMap<RouteSegment> visitedSegments,  float distFromStart, RouteSegment segment,
			short segmentPoint, RouteSegment inputNext, boolean reverseWaySearch, boolean doNotAddIntersections, 
			boolean[] processFurther) {
		boolean thereAreRestrictions ;
		RouteSegment itself = null;
		processFurther[0] = true;
		Iterator<RouteSegment> nextIterator = null;
		if(inputNext != null && inputNext.getRoad().getId() == segment.getRoad().getId() && inputNext.next == null) {
			thereAreRestrictions = false;
		} else {
			thereAreRestrictions = proccessRestrictions(ctx, segment.road, inputNext, reverseWaySearch);
			if (thereAreRestrictions) {
				nextIterator = ctx.segmentsToVisitPrescripted.iterator();
				if (TRACE_ROUTING) {
					println("  >> There are restrictions");
				}
			}
		}
		int targetEndX = reverseWaySearch ? ctx.startX : ctx.targetX;
		int targetEndY = reverseWaySearch ? ctx.startY : ctx.targetY;
		float distanceToEnd = h(ctx, segment.getRoad().getPoint31XTile(segmentPoint), segment.getRoad()
				.getPoint31YTile(segmentPoint), targetEndX, targetEndY);
		// Calculate possible ways to put into priority queue
		RouteSegment next = inputNext;
		boolean hasNext = nextIterator == null || nextIterator.hasNext();
		while (hasNext) {
			if (nextIterator != null) {
				next = nextIterator.next();
			}
			if (next.getSegmentStart() == segmentPoint && next.getRoad().getId() == segment.getRoad().id) {
				// find segment itself  
				// (and process it as other with small exception that we don't add to graph segments and process immediately)
				itself = next.initRouteSegment(segment.isPositive());
				if(itself == null) {
					// do nothing
				} else if (itself.getParentRoute() == null
						|| ctx.roadPriorityComparator(itself.distanceFromStart, itself.distanceToEnd, distFromStart,
								distanceToEnd) > 0) {
					itself.distanceFromStart = distFromStart;
					itself.distanceToEnd = distanceToEnd;
					itself.setParentRoute(segment);
					itself.setParentSegmentEnd(segmentPoint);
				} else {
					// we already processed that segment earlier or it is in graph segments
					// and we had better results (so we shouldn't process)
					processFurther[0] = false;
				}
			} else if(!doNotAddIntersections) {
				RouteSegment nextPos = next.initRouteSegment(true);
				RouteSegment nextNeg = next.initRouteSegment(false);
				processOneRoadIntersection(ctx, graphSegments, visitedSegments, distFromStart, distanceToEnd, segment, segmentPoint,
						nextPos);
				processOneRoadIntersection(ctx, graphSegments, visitedSegments, distFromStart, distanceToEnd, segment, segmentPoint,
						nextNeg);

			}
			// iterate to next road
			if (nextIterator == null) {
				next = next.next;
				hasNext = next != null;
			} else {
				hasNext = nextIterator.hasNext();
			}
		}
		return itself;
	}


	@SuppressWarnings("unused")
	private void processOneRoadIntersection(RoutingContext ctx, PriorityQueue<RouteSegment> graphSegments,
			TLongObjectHashMap<RouteSegment> visitedSegments, float distFromStart, float distanceToEnd,  RouteSegment segment,
			int segmentPoint, RouteSegment next) {
		if (next != null) {
			float obstaclesTime = (float) ctx.getRouter().calculateTurnTime(next, next.isPositive()? 
					next.getRoad().getPointsLength() - 1 : 0,  
					segment, segmentPoint);
			distFromStart += obstaclesTime;
			if(TEST_SPECIFIC && next.road.getId() == TEST_ID) {
				printRoad(" !? distFromStart=" + +distFromStart + " from " + segment.getRoad().getId() + 
						" dir=" + segment.getDirectionAssigned() + 
						" distToEnd=" + distanceToEnd +
						" segmentPoint="+ segmentPoint + " -- ", next, true);
			}
			if (!visitedSegments.containsKey(calculateRoutePointId(next, next.isPositive()))) {
				if (next.getParentRoute() == null
						|| ctx.roadPriorityComparator(next.distanceFromStart, next.distanceToEnd,
								distFromStart, distanceToEnd) > 0) {
					next.distanceFromStart = distFromStart;
					next.distanceToEnd = distanceToEnd;
					if (TRACE_ROUTING) {
						printRoad("  >>", next, null);
					}
					// put additional information to recover whole route after
					next.setParentRoute(segment);
					next.setParentSegmentEnd(segmentPoint);
					graphSegments.add(next);
				}
			} else {
				// the segment was already visited! We need to follow better route if it exists
				// that is very exceptional situation and almost exception, it can happen 
				// 1. when we underestimate distnceToEnd - wrong h()
				// 2. because we process not small segments but the whole road, it could be that 
				// deviation from the road is faster than following the whole road itself!
				if (distFromStart < next.distanceFromStart) {
					if (ctx.config.heuristicCoefficient <= 1) {
						System.err.println("! Alert distance from start " + distFromStart + " < "
								+ next.distanceFromStart + " id="+next.road.id);
					}
					// A: we can't change parent route just here, because we need to update visitedSegments
					// presumably we can do visitedSegments.put(calculateRoutePointId(next), next);
//					next.distanceFromStart = distFromStart;
//					next.setParentRoute(segment);
//					next.setParentSegmentEnd(segmentPoint);
					if (ctx.visitor != null) {
						// ctx.visitor.visitSegment(next, false);
					}
				}
			}
		}
	}
	

	/*public */static int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, 
			double o2DistanceFromStart, double o2DistanceToEnd, double heuristicCoefficient ) {
		// f(x) = g(x) + h(x)  --- g(x) - distanceFromStart, h(x) - distanceToEnd (not exact)
		return Double.compare(o1DistanceFromStart + heuristicCoefficient * o1DistanceToEnd, 
				o2DistanceFromStart + heuristicCoefficient *  o2DistanceToEnd);
	}

	
	public interface RouteSegmentVisitor {
		
		public void visitSegment(RouteSegment segment, int segmentEnd, boolean poll);
	}
	
	public static class RouteSegment {
		final short segStart;
		final RouteDataObject road;
		// needed to store intersection of routes
		RouteSegment next = null;
		RouteSegment oppositeDirection = null;
		
		// search context (needed for searching route)
		// Initially it should be null (!) because it checks was it segment visited before
		RouteSegment parentRoute = null;
		short parentSegmentEnd = 0;
		// 1 - positive , -1 - negative, 0 not assigned
		byte directionAssgn = 0;
		
		// distance measured in time (seconds)
		float distanceFromStart = 0;
		float distanceToEnd = 0;
		
		public RouteSegment(RouteDataObject road, int segmentStart) {
			this.road = road;
			this.segStart = (short) segmentStart;
		}
		
		public RouteSegment initRouteSegment(boolean positiveDirection) {
			if(segStart == 0 && !positiveDirection) {
				return null;
			}
			if(segStart == road.getPointsLength() - 1 && positiveDirection) {
				return null;
			}
			RouteSegment rs = this;
			if(directionAssgn == 0) {
				rs.directionAssgn = (byte) (positiveDirection ? 1 : -1);
			} else {
				if(positiveDirection != (directionAssgn == 1)) {
					if(oppositeDirection == null) {
						oppositeDirection = new RouteSegment(road, segStart);
						oppositeDirection.directionAssgn = (byte) (positiveDirection ? 1 : -1);
					}
					if ((oppositeDirection.directionAssgn == 1) != positiveDirection) {
						throw new IllegalStateException();
					}
					rs = oppositeDirection;
				}
			}
			return rs;
		}
		
		public byte getDirectionAssigned(){
			return directionAssgn;
		}
		
		public RouteSegment getParentRoute() {
			return parentRoute;
		}
		
		public boolean isPositive() {
			return directionAssgn == 1;
		}
		
		public void setParentRoute(RouteSegment parentRoute) {
			this.parentRoute = parentRoute;
		}
		
		public void assignDirection(byte b) {
			directionAssgn = b;
		}
		
		public void setParentSegmentEnd(int parentSegmentEnd) {
			this.parentSegmentEnd = (short) parentSegmentEnd;
		}
		
		public int getParentSegmentEnd() {
			return parentSegmentEnd;
		}
		
		public RouteSegment getNext() {
			return next;
		}
		
		public short getSegmentStart() {
			return segStart;
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
		
		public String getTestName(){
			return MessageFormat.format("s{0,number,#.##} e{1,number,#.##}", ((float)distanceFromStart), ((float)distanceToEnd));
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
					if(next != null) {
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

		public FinalRouteSegment(RouteDataObject road, int segmentStart) {
			super(road, segmentStart);
		}
		
	}
	
}
