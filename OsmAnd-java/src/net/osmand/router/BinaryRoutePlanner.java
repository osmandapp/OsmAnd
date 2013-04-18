package net.osmand.router;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataBorderLinePoint;
import net.osmand.binary.RouteDataObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class BinaryRoutePlanner {
	
	private final int REVERSE_WAY_RESTRICTION_ONLY = 1024;
	/*private*/ final int STANDARD_ROAD_IN_QUEUE_OVERHEAD = 220;
	/*private */final int STANDARD_ROAD_VISITED_OVERHEAD = 150;
	
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
		if(ctx.config.initialDirection != null) {
			// mark here as positive for further check
			ctx.firstRoadId = calculateRoutePointId(start.getRoad(), start.getSegmentStart(), true);
			double plusDir = start.getRoad().directionRoute(start.getSegmentStart(), true);
			double diff	 = plusDir - ctx.config.initialDirection;
			if(Math.abs(MapUtils.alignAngleDifference(diff)) <= Math.PI / 3) {
				ctx.firstRoadDirection = 1;
			} else if(Math.abs(MapUtils.alignAngleDifference(diff - Math.PI)) <= Math.PI / 3) {
				ctx.firstRoadDirection = -1;
			}
			
		}

		// Initializing priority queue to visit way segments 
		Comparator<RouteSegment> nonHeuristicSegmentsComparator = new NonHeuristicSegmentsComparator();
		PriorityQueue<RouteSegment> graphDirectSegments = new PriorityQueue<RouteSegment>(50, new SegmentsComparator(ctx));
		PriorityQueue<RouteSegment> graphReverseSegments = new PriorityQueue<RouteSegment>(50, new SegmentsComparator(ctx));
		
		// Set to not visit one segment twice (stores road.id << X + segmentStart)
		TLongObjectHashMap<RouteSegment> visitedDirectSegments = new TLongObjectHashMap<RouteSegment>();
		TLongObjectHashMap<RouteSegment> visitedOppositeSegments = new TLongObjectHashMap<RouteSegment>();
		
		RouteSegment recalcEndSegment = smartRecalculationEnabled(ctx, visitedOppositeSegments);
		boolean runRecalculation = false;
		if(recalcEndSegment != null) {
			runRecalculation = true;
			end = recalcEndSegment;
		}
		
		// for start : f(start) = g(start) + h(start) = 0 + h(start) = h(start)
		float estimatedDistance = (float) estimatedDistance(ctx, ctx.targetX, ctx.targetY, ctx.startX, ctx.startY);
		end.distanceToEnd = start.distanceToEnd	= estimatedDistance;
		
		graphDirectSegments.add(start);
		graphReverseSegments.add(end);
		
		// Extract & analyze segment with min(f(x)) from queue while final segment is not found
		boolean inverse = false;
		boolean init = false;
		
		PriorityQueue<RouteSegment>  graphSegments;
		if(inverse) {
			graphSegments = graphReverseSegments;
		} else {
			graphSegments = graphDirectSegments;
		}
		ctx.loadBorderPoints();
		
		FinalRouteSegment finalSegment = null;
		while (!graphSegments.isEmpty()) {
			RouteSegment segment = graphSegments.poll();
			// use accumulative approach
			ctx.memoryOverhead = (visitedDirectSegments.size() + visitedOppositeSegments.size()) * STANDARD_ROAD_VISITED_OVERHEAD + 
					(graphDirectSegments.size() +
					graphReverseSegments.size()) * STANDARD_ROAD_IN_QUEUE_OVERHEAD;
			
			if(TRACE_ROUTING){
				printRoad(">", segment);
			}
			if(segment instanceof FinalRouteSegment) {
				if(RoutingContext.SHOW_GC_SIZE){
					log.warn("Estimated overhead " + (ctx.memoryOverhead / (1<<20))+ " mb");
					printMemoryConsumption("Memory occupied after calculation : ");
				}
				finalSegment = (FinalRouteSegment) segment;
				break;
			}
			if (ctx.memoryOverhead > ctx.config.memoryLimitation * 0.95 && RoutingContext.SHOW_GC_SIZE) {
				printMemoryConsumption("Memory occupied before exception : ");
			}
			if(ctx.memoryOverhead > ctx.config.memoryLimitation * 0.95) {
				throw new IllegalStateException("There is no enough memory " + ctx.config.memoryLimitation/(1<<20) + " Mb");
			}
			ctx.visitedSegments++;
			if (!inverse) {
				processRouteSegment(ctx, false, graphDirectSegments, visitedDirectSegments, 
						segment, visitedOppositeSegments, true);
				processRouteSegment(ctx, false, graphDirectSegments, visitedDirectSegments, 
						segment, visitedOppositeSegments, false);
			} else {
				processRouteSegment(ctx, true, graphReverseSegments, visitedOppositeSegments, segment,
						visitedDirectSegments, true);
				processRouteSegment(ctx, true, graphReverseSegments, visitedOppositeSegments,segment,
						visitedDirectSegments, false);
			}
			updateCalculationProgress(ctx, graphDirectSegments, graphReverseSegments);
			if(graphReverseSegments.isEmpty()){
				throw new IllegalArgumentException("Route is not found to selected target point.");
			}
			if(graphDirectSegments.isEmpty()){
				throw new IllegalArgumentException("Route is not found from selected start point.");
			}
			if(runRecalculation) {
				// nothing to do
				inverse = false;
			} else if (!init) {
				inverse = !inverse;
				init = true;
			} else if (ctx.planRouteIn2Directions()) {
				inverse = nonHeuristicSegmentsComparator.compare(graphDirectSegments.peek(), graphReverseSegments.peek()) > 0;
				if (graphDirectSegments.size() * 1.3 > graphReverseSegments.size()) {
					inverse = true;
				} else if (graphDirectSegments.size() < 1.3 * graphReverseSegments.size()) {
					inverse = false;
				}
			} else {
				// different strategy : use onedirectional graph
				inverse = ctx.getPlanRoadDirection() < 0;
			}
			if (inverse) {
				graphSegments = graphReverseSegments;
			} else {
				graphSegments = graphDirectSegments;
			}

			// check if interrupted
			if(ctx.calculationProgress != null && ctx.calculationProgress.isCancelled) {
				throw new InterruptedException("Route calculation interrupted");
			}
		}
		printDebugMemoryInformation(ctx, graphDirectSegments, graphReverseSegments, visitedDirectSegments, visitedOppositeSegments);
		return finalSegment;
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
			if(graphDirectSegments.size() > 0) {
				ctx.calculationProgress.distanceFromBegin =
						Math.max(graphDirectSegments.peek().distanceFromStart, ctx.calculationProgress.distanceFromBegin);
			}
			if(graphDirectSegments.size() > 0) {
				ctx.calculationProgress.distanceFromEnd = 
						Math.max(graphReverseSegments.peek().distanceFromStart, ctx.calculationProgress.distanceFromBegin);
			}
		}
	}


	private RouteSegment smartRecalculationEnabled(final RoutingContext ctx, TLongObjectHashMap<RouteSegment> visitedOppositeSegments) {
		boolean runRecalculation = ctx.previouslyCalculatedRoute != null && ctx.previouslyCalculatedRoute.size() > 0
				&& ctx.config.recalculateDistance != 0;
		if (runRecalculation) {
			RouteSegment previous = null;
			List<RouteSegmentResult> rlist = new ArrayList<RouteSegmentResult>();
			float distanceThreshold = ctx.config.recalculateDistance;
			float threshold = 0;
			for(RouteSegmentResult rr : ctx.previouslyCalculatedRoute) {
				threshold += rr.getDistance();
				if(threshold > distanceThreshold) {
					rlist.add(rr);
				}
			}
			runRecalculation = rlist.size() > 0;
			if (rlist.size() > 0) {
				for (RouteSegmentResult rr : rlist) {
					RouteSegment segment = new RouteSegment(rr.getObject(), rr.getEndPointIndex());
					if (previous != null) {
						previous.setParentRoute(segment);
						previous.setParentSegmentEnd(rr.getStartPointIndex());
						boolean positive = rr.getStartPointIndex() < rr.getEndPointIndex();
						long t = calculateRoutePointId(rr.getObject(), positive ? rr.getEndPointIndex() - 1 : rr.getEndPointIndex(),
								positive);
						visitedOppositeSegments.put(t, segment);
					}
					previous = segment;
				}
				return previous;
			}
		}
		return null;
	}
	
	

	private void printRoad(String prefix, RouteSegment segment) {
		String pr;
		if(segment.parentRoute != null){
			pr = " pend="+segment.parentSegmentEnd +" parent=" + segment.parentRoute.road;
		} else {
			pr = "";
		}
		println(prefix  +"" + segment.road + " ind=" + segment.getSegmentStart() + 
				" ds=" + ((float)segment.distanceFromStart) + " es="+((float)segment.distanceToEnd) + pr);
	}

	private float estimatedDistance(final RoutingContext ctx, int targetEndX, int targetEndY,
			int startX, int startY) {
		double distance = squareRootDist(startX, startY, targetEndX, targetEndY);
		return (float) (distance / ctx.getRouter().getMaxDefaultSpeed());
	}
	
	protected static float h(RoutingContext ctx, int begX, int begY, int endX, int endY, 
			RouteSegment next) {
		double distToFinalPoint = squareRootDist(begX, begY,  endX, endY);
		if (RoutingContext.USE_BORDER_LINES) {
			int begBorder = ctx.searchBorderLineIndex(begY);
			int endBorder = ctx.searchBorderLineIndex(endY);
			if (begBorder != endBorder) {
				double res = 0;
				boolean plus = begBorder < endBorder;
				boolean beginEqStart = begX == ctx.startX && begY == ctx.startY;
				boolean beginEqTarget = begX == ctx.targetX && begY == ctx.targetY;
				boolean endEqStart = endX == ctx.startX && endY == ctx.startY;
				boolean endEqTarget = endX == ctx.targetX && endY == ctx.targetY;
				if(endEqStart || endEqTarget) {
					// we start from intermediate point and end in target or start
					if (begX > ctx.leftBorderBoundary && begX < ctx.rightBorderBoundary) {
						List<RouteDataBorderLinePoint> pnts = ctx.borderLines[plus ? begBorder : begBorder - 1].borderPoints;
						for (RouteDataBorderLinePoint p : pnts) {
							double f = (endEqTarget ? p.distanceToEndPoint : p.distanceToStartPoint) + squareRootDist(p.x, p.y, begX, begY);
							if (res > f || res <= 0) {
								res = f;
							}
						}
					}
				} else if(beginEqStart || beginEqTarget) {
					if (endX > ctx.leftBorderBoundary && endX < ctx.rightBorderBoundary) {
						List<RouteDataBorderLinePoint> pnts = ctx.borderLines[plus ? endBorder - 1 : endBorder].borderPoints;
						for (RouteDataBorderLinePoint p : pnts) {
							double f = (beginEqTarget ? p.distanceToEndPoint : p.distanceToStartPoint)
									+ squareRootDist(p.x, p.y, endX, endY);
							if (res > f || res <= 0) {
								res = f;
							}
						}
					}
				} else { 
					throw new IllegalStateException();
				}
				if(res > 0) {
					if(res < distToFinalPoint - 0.01) {
						throw new IllegalStateException("Estimated distance " + res + " > " + distToFinalPoint);
					}
//					if(endEqStart && res - distToFinalPoint > 13000) {
//						System.out.println(" Res="+res + " dist=" +distToFinalPoint);
//					}
					distToFinalPoint = res;
					
				} else {
					// FIXME put penalty
//					distToFinalPoint = distToFinalPoint;
				}
			}
		}
		
		double result = distToFinalPoint / ctx.getRouter().getMaxDefaultSpeed();
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
		printInfo("Time to calculate : " + (System.nanoTime() - ctx.timeToCalculate) / 1e6 + ", time to load : " + ctx.timeToLoad / 1e6 + ", time to load headers : " + ctx.timeToLoadHeaders / 1e6);
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
		
		for(int k=0; k<ctx.borderLines.length; k++) {
			System.out.println("Line " + (ctx.borderLineCoordinates[k] >> 17) + " points " + ctx.borderLines[k].borderPoints.size());
		}

	}
	
	
	private void processRouteSegment(final RoutingContext ctx, boolean reverseWaySearch,
			PriorityQueue<RouteSegment> graphSegments, TLongObjectHashMap<RouteSegment> visitedSegments, 
            RouteSegment segment, TLongObjectHashMap<RouteSegment> oppositeSegments, boolean direction) throws IOException {
		final RouteDataObject road = segment.road;
		boolean initDirectionAllowed = checkIfInitialMovementAllowedOnSegment(ctx, reverseWaySearch, visitedSegments, segment, direction, road);
		boolean directionAllowed = initDirectionAllowed; 
		// Go through all point of the way and find ways to continue
		// ! Actually there is small bug when there is restriction to move forward on the way (it doesn't take into account)
		float obstaclesTime = 0;
		if(segment.getParentRoute() != null && directionAllowed) {
			obstaclesTime = (float) ctx.getRouter().calculateTurnTime(segment, direction? segment.getRoad().getPointsLength() - 1 : 0,  
					segment.getParentRoute(), segment.getParentSegmentEnd());
		}
		if(ctx.firstRoadId == calculateRoutePointId(road, segment.getSegmentStart(), true) ) {
			if(direction && ctx.firstRoadDirection < 0) {
				obstaclesTime += 500;
			} else if(!direction && ctx.firstRoadDirection > 0) {
				obstaclesTime += 500;
			}
		}
		float segmentDist = 0;
		// +/- diff from middle point
		int segmentEnd = segment.getSegmentStart();
		while (directionAllowed) {
			int prevInd = segmentEnd;
			if(direction) {
				segmentEnd ++;
			} else {
				segmentEnd --;
			}
			if (segmentEnd < 0 || segmentEnd >= road.getPointsLength()) {
				directionAllowed = false;
				continue;
			}
			final int intervalId = direction ? segmentEnd - 1 : segmentEnd;
			visitedSegments.put(calculateRoutePointId(road, intervalId, direction), segment);
			final int x = road.getPoint31XTile(segmentEnd);
			final int y = road.getPoint31YTile(segmentEnd);
			final int prevx = road.getPoint31XTile(prevInd);
			final int prevy = road.getPoint31YTile(prevInd);
			if(x == prevx && y == prevy) {
				continue;
			}
			if(RoutingContext.USE_BORDER_LINES) {
				int st = ctx.searchBorderLineIndex(y);
				int tt = ctx.searchBorderLineIndex(prevy);
				if(st != tt){
//					System.out.print(" " + st + " != " + tt + " " + road.id + " ? ");
					for(int i = Math.min(st, tt); i < Math.max(st, tt) & i < ctx.borderLines.length ; i++) {
						Iterator<RouteDataBorderLinePoint> pnts = ctx.borderLines[i].borderPoints.iterator();
						boolean changed = false;
						while(pnts.hasNext()) {
							RouteDataBorderLinePoint o = pnts.next();
							if(o.id == road.id) {
//								System.out.println("Point removed !");
								pnts.remove();
								changed = true;
							}
						}
						if(changed){
							ctx.updateDistanceForBorderPoints(ctx.startX, ctx.startY, true);
							ctx.updateDistanceForBorderPoints(ctx.targetX, ctx.targetY, false);
						}
					}
				}
			}
			// 2. calculate point and try to load neighbor ways if they are not loaded
			segmentDist  += squareRootDist(x, y,  prevx, prevy);
			
			// 2.1 calculate possible obstacle plus time
			double obstacle = ctx.getRouter().defineRoutingObstacle(road, segmentEnd);
			if (obstacle < 0) {
				directionAllowed = false;
				continue;
			}
			obstaclesTime += obstacle;
			
			boolean alreadyVisited = checkIfOppositieSegmentWasVisited(ctx, reverseWaySearch, graphSegments, segment, oppositeSegments, road,
					segmentEnd, direction, intervalId, segmentDist, obstaclesTime);
			if (alreadyVisited) {
				directionAllowed = false;
				continue;
			}
			
			// could be expensive calculation
			// 3. get intersected ways
			final RouteSegment roadNext = ctx.loadRouteSegment(x, y, ctx.config.memoryLimitation - ctx.memoryOverhead);
			if(roadNext != null && 
					!((roadNext == segment || roadNext.road.id == road.id) && roadNext.next == null)) {
				// check if there are outgoing connections in that case we need to stop processing
				boolean outgoingConnections = false;
				RouteSegment r = roadNext;
				while(r != null && !outgoingConnections) {
					if(r.road.id != road.id || r.getSegmentStart() != 0 || r.road.getOneway() != 1){
						outgoingConnections = true;
					}
					r = r.next;
				}
				if (outgoingConnections) {
					directionAllowed = false;
				}
				
				float distStartObstacles = segment.distanceFromStart + calculateTimeWithObstacles(ctx, road, segmentDist , obstaclesTime);
				processIntersections(ctx, graphSegments, visitedSegments, 
						distStartObstacles, segment, segmentEnd, 
						roadNext, reverseWaySearch, outgoingConnections);
			}
		}
		if(initDirectionAllowed && ctx.visitor != null){
			ctx.visitor.visitSegment(segment, segmentEnd, true);
		}
	}

	private boolean checkIfInitialMovementAllowedOnSegment(final RoutingContext ctx, boolean reverseWaySearch,
			TLongObjectHashMap<RouteSegment> visitedSegments, RouteSegment segment, boolean direction, final RouteDataObject road
			) {
		boolean directionAllowed;
		final int middle = segment.getSegmentStart();
		int oneway = ctx.getRouter().isOneWay(road);
		// use positive direction as agreed
		if (!reverseWaySearch) {
			if(direction){
				directionAllowed = oneway >= 0;
			} else {
				directionAllowed = oneway <= 0;
			}
		} else {
			if(direction){
				directionAllowed = oneway <= 0;
			} else {
				directionAllowed = oneway >= 0;
			}
		}
		if(direction) {
			if(middle == road.getPointsLength() - 1 ||
					visitedSegments.containsKey(calculateRoutePointId(road, middle, true)) || 
					segment.getAllowedDirection() == -1) {
				directionAllowed = false;
			}	
		} else {
			if(middle == 0 || visitedSegments.containsKey(calculateRoutePointId(road, middle - 1, false)) || 
					segment.getAllowedDirection() == 1) {
				directionAllowed = false;
			}	
		}
		return directionAllowed;
	}

	private boolean checkIfOppositieSegmentWasVisited(final RoutingContext ctx, boolean reverseWaySearch,
			PriorityQueue<RouteSegment> graphSegments, RouteSegment segment, TLongObjectHashMap<RouteSegment> oppositeSegments,
			final RouteDataObject road, int segmentEnd, boolean positive, int intervalId, float segmentDist, float obstaclesTime) {
		long opp = calculateRoutePointId(road, intervalId, !positive);
		if (oppositeSegments.containsKey(opp)) {
			RouteSegment opposite = oppositeSegments.get(opp);
			if (opposite.getSegmentStart() == segmentEnd) {
				FinalRouteSegment frs = new FinalRouteSegment(road, segment.getSegmentStart());
				float distStartObstacles = segment.distanceFromStart + calculateTimeWithObstacles(ctx, road, segmentDist , obstaclesTime);
				frs.setParentRoute(segment.getParentRoute());
				frs.setParentSegmentEnd(segment.getParentSegmentEnd());
				frs.reverseWaySearch = reverseWaySearch;
				frs.distanceFromStart = opposite.distanceFromStart + distStartObstacles;
				frs.distanceToEnd = 0;
				frs.opposite = opposite;
				graphSegments.add(frs);
				return true;
			}
		}
		return false;
	}
	

	private float calculateTimeWithObstacles(RoutingContext ctx, RouteDataObject road, float distOnRoadToPass, float obstaclesTime) {
		float priority = ctx.getRouter().defineSpeedPriority(road);
		float speed = (ctx.getRouter().defineSpeed(road) * priority);
		if (speed == 0) {
			speed = (ctx.getRouter().getMinDefaultSpeed() * priority);
		}
		// speed can not exceed max default speed according to A*
		if(speed > ctx.getRouter().getMaxDefaultSpeed()) {
			speed = ctx.getRouter().getMaxDefaultSpeed();
		}
		float distStartObstacles = obstaclesTime  +
				 distOnRoadToPass / speed;
		return distStartObstacles;
		
	}

	private long calculateRoutePointId(final RouteDataObject road, int intervalId, boolean positive) {
		return (road.getId() << ROUTE_POINTS) + (intervalId << 1) + (positive ? 1 : 0);
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
	
	


	private void processIntersections(RoutingContext ctx, PriorityQueue<RouteSegment> graphSegments,
			TLongObjectHashMap<RouteSegment> visitedSegments,  float  distFromStart,  
			RouteSegment segment, int segmentEnd,  
			RouteSegment inputNext, boolean reverseWaySearch, 
			boolean addSameRoadFutureDirection) {
		byte searchDirection = reverseWaySearch ? (byte)-1 : (byte)1;
		boolean thereAreRestrictions = proccessRestrictions(ctx, segment.road, inputNext, reverseWaySearch);
		Iterator<RouteSegment> nextIterator = null;
		if (thereAreRestrictions) {
			nextIterator = ctx.segmentsToVisitPrescripted.iterator();
			if(TRACE_ROUTING){
				println("  >> There are restrictions");
			}
		}
		// Calculate possible ways to put into priority queue
		RouteSegment next = inputNext;
		boolean hasNext = nextIterator == null || nextIterator.hasNext();
		while (hasNext) {
			if (nextIterator != null) {
				next = nextIterator.next();
			}
			boolean nextPlusNotAllowed = (next.getSegmentStart() == next.road.getPointsLength() - 1) ||
					visitedSegments.containsKey(calculateRoutePointId(next.road, next.getSegmentStart(), true));
			boolean nextMinusNotAllowed = (next.getSegmentStart() == 0) ||
					visitedSegments.containsKey(calculateRoutePointId(next.road, next.getSegmentStart() - 1, false));
			boolean sameRoadFutureDirection = next.road.id == segment.road.id && next.getSegmentStart() == segmentEnd;
			// road.id could be equal on roundabout, but we should accept them
			boolean alreadyVisited = nextPlusNotAllowed && nextMinusNotAllowed;
			boolean skipRoad = sameRoadFutureDirection && !addSameRoadFutureDirection;
			if (!alreadyVisited && !skipRoad) {
				int targetEndX = reverseWaySearch? ctx.startX : ctx.targetX;
				int targetEndY = reverseWaySearch? ctx.startY : ctx.targetY;
				float distanceToEnd = h(ctx, segment.getRoad().getPoint31XTile(segmentEnd),
						segment.getRoad().getPoint31YTile(segmentEnd), targetEndX, targetEndY, next);
				// assigned to wrong direction
				if(next.getDirectionAssigned() == -searchDirection){
					next = new RouteSegment(next.getRoad(), next.getSegmentStart());
				}
				 
				if (next.getParentRoute() == null
						|| ctx.roadPriorityComparator(next.distanceFromStart, next.distanceToEnd, distFromStart, distanceToEnd) > 0) {
					if (next.getParentRoute() != null) {
						if (!graphSegments.remove(next)) {
							throw new IllegalStateException("Should be handled by direction flag");
						} 
					} 
					next.assignDirection(searchDirection);
					next.distanceFromStart = distFromStart;
					next.distanceToEnd = distanceToEnd;
					if(sameRoadFutureDirection) {
						next.setAllowedDirection((byte) (segment.getSegmentStart() < next.getSegmentStart() ? 1 : - 1));
					}
					if(TRACE_ROUTING) {
						printRoad("  >>", next);
					}
					// put additional information to recover whole route after
					next.setParentRoute(segment);
					next.setParentSegmentEnd(segmentEnd);
					
					graphSegments.add(next);
				}
				if (ctx.visitor != null) {
//					ctx.visitor.visitSegment(next, false);
				}
			} else if(!sameRoadFutureDirection){
				// the segment was already visited! We need to follow better route if it exists
				// that is very strange situation and almost exception (it can happen when we underestimate distnceToEnd)
				if (next.getDirectionAssigned() == searchDirection && 
						distFromStart < next.distanceFromStart && next.road.id != segment.road.id) {
					if(ctx.config.heuristicCoefficient <= 1) {
						throw new IllegalStateException("distance from start " + distFromStart + " < " +next.distanceFromStart); 
					}
					// That code is incorrect (when segment is processed itself,
					// then it tries to make wrong u-turn) -
					// this situation should be very carefully checked in future (seems to be fixed)
					next.distanceFromStart = distFromStart;
					next.setParentRoute(segment);
					next.setParentSegmentEnd(segmentEnd);
					if (ctx.visitor != null) {
//						ctx.visitor.visitSegment(next, false);
					}
				}
			}

			// iterate to next road
			if (nextIterator == null) {
				next = next.next;
				hasNext = next != null;
			} else {
				hasNext = nextIterator.hasNext();
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
		
		// search context (needed for searching route)
		// Initially it should be null (!) because it checks was it segment visited before
		RouteSegment parentRoute = null;
		short parentSegmentEnd = 0;
		// 1 - positive , -1 - negative, 0 not assigned
		byte directionAssgn = 0;
		// 1 - only positive allowed, -1 - only negative allowed
		byte allowedDirection = 0;
		
		// distance measured in time (seconds)
		float distanceFromStart = 0;
		float distanceToEnd = 0;
		
		public RouteSegment(RouteDataObject road, int segmentStart) {
			this.road = road;
			this.segStart = (short) segmentStart;
		}
		
		public byte getDirectionAssigned(){
			return directionAssgn;
		}
		
		public RouteSegment getParentRoute() {
			return parentRoute;
		}
		
		public void setParentRoute(RouteSegment parentRoute) {
			this.parentRoute = parentRoute;
		}
		
		public void assignDirection(byte b) {
			directionAssgn = b;
		}
		
		public byte getAllowedDirection() {
			return allowedDirection;
		}
		
		public void setAllowedDirection(byte allowedDirection) {
			this.allowedDirection = allowedDirection;
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
		
		public int getSegmentStart() {
			return segStart;
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
