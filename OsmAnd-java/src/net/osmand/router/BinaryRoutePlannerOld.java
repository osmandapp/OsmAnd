package net.osmand.router;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataObject;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class BinaryRoutePlannerOld {
	
	public static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = true;
	private final int REVERSE_WAY_RESTRICTION_ONLY = 1024;
	private final int STANDARD_ROAD_IN_QUEUE_OVERHEAD = 900;
	
	protected static final Log log = PlatformUtil.getLog(BinaryRoutePlannerOld.class);
	
	private static final int ROUTE_POINTS = 11;
	
	
	private static double squareRootDist(int x1, int y1, int x2, int y2) {
		// translate into meters 
		double dy = MapUtils.convert31YToMeters(y1, y2);
		double dx = MapUtils.convert31XToMeters(x1, x2);
		return Math.sqrt(dx * dx + dy * dy);
//		return measuredDist(x1, y1, x2, y2);
	}
	
	
	/**
	 * Calculate route between start.segmentEnd and end.segmentStart (using A* algorithm)
	 * return list of segments
	 */
	void searchRouteInternal(final RoutingContext ctx, RouteSegment start, RouteSegment end) throws IOException, InterruptedException {
		// measure time
		ctx.timeToLoad = 0;
		ctx.visitedSegments = 0;
		ctx.timeToCalculate = System.nanoTime();
		if(ctx.config.initialDirection != null) {
			ctx.firstRoadId = (start.getRoad().id << ROUTE_POINTS) + start.getSegmentStart();
			double plusDir = start.getRoad().directionRoute(start.getSegmentStart(), true);
			double diff	 = plusDir - ctx.config.initialDirection;
			if(Math.abs(MapUtils.alignAngleDifference(diff)) <= Math.PI / 3) {
				ctx.firstRoadDirection = 1;
			} else if(Math.abs(MapUtils.alignAngleDifference(diff - Math.PI)) <= Math.PI / 3) {
				ctx.firstRoadDirection = -1;
			}
			
		}

		// Initializing priority queue to visit way segments 
		Comparator<RouteSegment> segmentsComparator = new Comparator<RouteSegment>(){
			@Override
			public int compare(RouteSegment o1, RouteSegment o2) {
				return ctx.roadPriorityComparator(o1.distanceFromStart, o1.distanceToEnd, o2.distanceFromStart, o2.distanceToEnd);
			}
		};
		
		Comparator<RouteSegment> nonHeuristicSegmentsComparator = new Comparator<RouteSegment>(){
			@Override
			public int compare(RouteSegment o1, RouteSegment o2) {
				return roadPriorityComparator(o1.distanceFromStart, o1.distanceToEnd, o2.distanceFromStart, o2.distanceToEnd, 0.5);
			}
		};

		PriorityQueue<RouteSegment> graphDirectSegments = new PriorityQueue<RouteSegment>(50, segmentsComparator);
		PriorityQueue<RouteSegment> graphReverseSegments = new PriorityQueue<RouteSegment>(50, segmentsComparator);
		
		// Set to not visit one segment twice (stores road.id << X + segmentStart)
		TLongObjectHashMap<RouteSegment> visitedDirectSegments = new TLongObjectHashMap<RouteSegment>();
		TLongObjectHashMap<RouteSegment> visitedOppositeSegments = new TLongObjectHashMap<RouteSegment>();
		
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
						long t = (rr.getObject().getId() << ROUTE_POINTS) + segment.getSegmentStart();
						visitedOppositeSegments.put(t, segment);
					}
					previous = segment;
				}
				end = previous;
			}
		}
		
		// for start : f(start) = g(start) + h(start) = 0 + h(start) = h(start)
		int targetEndX = end.road.getPoint31XTile(end.getSegmentStart());
		int targetEndY = end.road.getPoint31YTile(end.getSegmentStart());
		int startX = start.road.getPoint31XTile(start.getSegmentStart());
		int startY = start.road.getPoint31YTile(start.getSegmentStart());
		float estimatedDistance = (float) h(ctx, targetEndX, targetEndY, startX, startY);
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
		while (!graphSegments.isEmpty()) {
			RouteSegment segment = graphSegments.poll();
			ctx.visitedSegments++;
			// for debug purposes
			if (ctx.visitor != null) {
//				ctx.visitor.visitSegment(segment, true);
			}
			updateCalculationProgress(ctx, graphDirectSegments, graphReverseSegments);
			boolean routeFound = false;
			if (!inverse) {
				routeFound = processRouteSegment(ctx, false, graphDirectSegments, visitedDirectSegments, targetEndX, targetEndY,
						segment, visitedOppositeSegments);
			} else {
				routeFound = processRouteSegment(ctx, true, graphReverseSegments, visitedOppositeSegments, startX, startY, segment,
						visitedDirectSegments);
			}
			if (graphReverseSegments.isEmpty() || graphDirectSegments.isEmpty() || routeFound) {
				break;
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
		println("Result is found");
		printDebugMemoryInformation(ctx, graphDirectSegments, graphReverseSegments, visitedDirectSegments, visitedOppositeSegments);
	}
	
	private void updateCalculationProgress(final RoutingContext ctx, PriorityQueue<RouteSegment> graphDirectSegments,
			PriorityQueue<RouteSegment> graphReverseSegments) {
		if(ctx.calculationProgress != null) {
			ctx.calculationProgress.reverseSegmentQueueSize = graphReverseSegments.size();
			ctx.calculationProgress.directSegmentQueueSize = graphDirectSegments.size();
			RouteSegment dirPeek = graphDirectSegments.peek();
			if(dirPeek != null) {
				ctx.calculationProgress.distanceFromBegin =
						Math.max(dirPeek.distanceFromStart, ctx.calculationProgress.distanceFromBegin);
			}
			RouteSegment revPeek = graphReverseSegments.peek();
			if(revPeek != null) {
				ctx.calculationProgress.distanceFromEnd = 
						Math.max(revPeek.distanceFromStart, ctx.calculationProgress.distanceFromEnd);
			}
		}
	}
	
	
	private double h(final RoutingContext ctx, int targetEndX, int targetEndY,
			int startX, int startY) {
		double distance = squareRootDist(startX, startY, targetEndX, targetEndY);
		return distance / ctx.getRouter().getMaxDefaultSpeed();
	}
	
	protected static double h(RoutingContext ctx, double distToFinalPoint, RouteSegment next) {
		double result = distToFinalPoint / ctx.getRouter().getMaxDefaultSpeed();
		return result; 
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
		printInfo("Visited roads, " + ctx.visitedSegments + ", relaxed roads " + ctx.relaxedSegments);
		if (graphDirectSegments != null && graphReverseSegments != null) {
			printInfo("Priority queues sizes : " + graphDirectSegments.size() + "/" + graphReverseSegments.size());
		}
		if (visitedDirectSegments != null && visitedOppositeSegments != null) {
			printInfo("Visited segments sizes: " + visitedDirectSegments.size() + "/" + visitedOppositeSegments.size());
		}

	}
	

	private boolean processRouteSegment(final RoutingContext ctx, boolean reverseWaySearch,
			PriorityQueue<RouteSegment> graphSegments, TLongObjectHashMap<RouteSegment> visitedSegments, int targetEndX, int targetEndY,
            RouteSegment segment, TLongObjectHashMap<RouteSegment> oppositeSegments) throws IOException {
		// Always start from segmentStart (!), not from segmentEnd
		// It makes difference only for the first start segment
		// Middle point will always be skipped from observation considering already visited
		final RouteDataObject road = segment.road;
		final int middle = segment.getSegmentStart();
		float obstaclePlusTime = 0;
		float obstacleMinusTime = 0;
		
		
		// This is correct way of checking but it has problem with relaxing strategy
//			long ntf = (segment.road.getId() << ROUTE_POINTS) + segment.segmentStart;
//			visitedSegments.put(ntf, segment);
//			if (oppositeSegments.contains(ntf) && oppositeSegments.get(ntf) != null) {
//				RouteSegment opposite = oppositeSegments.get(ntf);
//				if (opposite.segmentStart == segment.segmentStart) {
					// if (reverseWaySearch) {
					// reverse : segment.parentSegmentEnd - segment.parentRoute
					// } else {
					// reverse : opposite.parentSegmentEnd - oppositie.parentRoute
					// }
//					return true;
//				}
//			}

		// 0. mark route segment as visited
		long nt = (road.getId() << ROUTE_POINTS) + middle;
		// avoid empty segments to connect but mark the point as visited
		visitedSegments.put(nt, null);

		int oneway = ctx.getRouter().isOneWay(road);
		boolean minusAllowed;
		boolean plusAllowed; 
		if(ctx.firstRoadId == nt) {
			if(ctx.firstRoadDirection < 0) {
				obstaclePlusTime += 500;
			} else if(ctx.firstRoadDirection > 0) {
				obstacleMinusTime += 500;
			}
		} 
		if (!reverseWaySearch) {
			minusAllowed = oneway <= 0;
			plusAllowed = oneway >= 0;
		} else {
			minusAllowed = oneway >= 0;
			plusAllowed = oneway <= 0;
		}

		// +/- diff from middle point
		int d = plusAllowed ? 1 : -1;
		if(segment.parentRoute != null) {
			if(plusAllowed && middle < segment.getRoad().getPointsLength() - 1) {
				obstaclePlusTime = (float) ctx.getRouter().calculateTurnTime(segment, segment.getRoad().getPointsLength() - 1,  
					segment.parentRoute, segment.parentSegmentEnd);
			}
			if(minusAllowed && middle > 0) {
				obstacleMinusTime = (float) ctx.getRouter().calculateTurnTime(segment, 0, 
						segment.parentRoute, segment.parentSegmentEnd);
			}
		}
		// Go through all point of the way and find ways to continue
		// ! Actually there is small bug when there is restriction to move forward on way (it doesn't take into account)
		float posSegmentDist = 0;
		float negSegmentDist = 0;
		while (minusAllowed || plusAllowed) {
			// 1. calculate point not equal to middle
			// (algorithm should visit all point on way if it is not oneway)
			int segmentEnd = middle + d;
			boolean positive = d > 0;
			if (!minusAllowed && d > 0) {
				d++;
			} else if (!plusAllowed && d < 0) {
				d--;
			} else {
				if (d <= 0) {
					d = -d + 1;
				} else {
					d = -d;
				}
			}
			if (segmentEnd < 0) {
				minusAllowed = false;
				continue;
			}
			if (segmentEnd >= road.getPointsLength()) {
				plusAllowed = false;
				continue;
			}
			// if we found end point break cycle
			long nts = (road.getId() << ROUTE_POINTS) + segmentEnd;
			visitedSegments.put(nts, segment);

			// 2. calculate point and try to load neighbor ways if they are not loaded
			int x = road.getPoint31XTile(segmentEnd);
			int y = road.getPoint31YTile(segmentEnd);
			if(positive) {
				posSegmentDist += squareRootDist(x, y, 
						road.getPoint31XTile(segmentEnd - 1), road.getPoint31YTile(segmentEnd - 1));
			} else {
				negSegmentDist += squareRootDist(x, y, 
						road.getPoint31XTile(segmentEnd + 1), road.getPoint31YTile(segmentEnd + 1));
			}
			
			// 2.1 calculate possible obstacle plus time
			if(positive){
				double obstacle = ctx.getRouter().defineRoutingObstacle(road, segmentEnd);
				if (obstacle < 0) {
					plusAllowed = false;
					continue;
				}
				obstaclePlusTime +=  obstacle;
			} else {
				double obstacle = ctx.getRouter().defineRoutingObstacle(road, segmentEnd);
				if (obstacle < 0) {
					minusAllowed = false;
					continue;
				}
				obstacleMinusTime +=  obstacle;
			}
//			int overhead = 0;
			// could be expensive calculation
			int overhead = (ctx.visitedSegments - ctx.relaxedSegments ) *
					STANDARD_ROAD_IN_QUEUE_OVERHEAD;
			if(overhead > ctx.config.memoryLimitation * 0.95){
				throw new OutOfMemoryError("There is no enough memory " + ctx.config.memoryLimitation/(1<<20) + " Mb");
			}
			RouteSegment next = ctx.loadRouteSegment(x, y, ctx.config.memoryLimitation - overhead);
			// 3. get intersected ways
			if (next != null) {
				// TO-DO U-Turn
				if((next == segment || next.road.id == road.id) && next.next == null) {
					// simplification if there is no real intersection
					continue;
				}
				// Using A* routing algorithm
				// g(x) - calculate distance to that point and calculate time
				
				float priority = ctx.getRouter().defineSpeedPriority(road);
				float speed = ctx.getRouter().defineSpeed(road) * priority;
				if (speed == 0) {
					speed = ctx.getRouter().getMinDefaultSpeed() * priority;
				}
				float distOnRoadToPass = positive? posSegmentDist : negSegmentDist;
				float distStartObstacles = segment.distanceFromStart + ( positive ? obstaclePlusTime : obstacleMinusTime) +
						 distOnRoadToPass / speed;
				
				float distToFinalPoint = (float) squareRootDist(x, y, targetEndX, targetEndY);
				boolean routeFound = processIntersections(ctx, graphSegments, visitedSegments, oppositeSegments,
						distStartObstacles, distToFinalPoint, segment, segmentEnd, next, reverseWaySearch);
				if(routeFound){
					return routeFound;
				}
				
			}
		}
		return false;
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
	
	


	private boolean processIntersections(RoutingContext ctx, PriorityQueue<RouteSegment> graphSegments,
			TLongObjectHashMap<RouteSegment> visitedSegments, TLongObjectHashMap<RouteSegment> oppositeSegments,  
			float distFromStart, float distToFinalPoint, 
			RouteSegment segment, int segmentEnd, RouteSegment inputNext,
			boolean reverseWay) {

		boolean thereAreRestrictions = proccessRestrictions(ctx, segment.road, inputNext, reverseWay);
		Iterator<RouteSegment> nextIterator = null;
		if (thereAreRestrictions) {
			nextIterator = ctx.segmentsToVisitPrescripted.iterator();
		}
		// Calculate possible ways to put into priority queue
		RouteSegment next = inputNext;
		boolean hasNext = nextIterator == null || nextIterator.hasNext();
		while (hasNext) {
			if (nextIterator != null) {
				next = nextIterator.next();
			}
			long nts = (next.road.getId() << ROUTE_POINTS) + next.getSegmentStart();

			// 1. Check if opposite segment found so we can stop calculations
			if (oppositeSegments.contains(nts) && oppositeSegments.get(nts) != null) {
				// restrictions checked
				RouteSegment opposite = oppositeSegments.get(nts);
				// additional check if opposite way not the same as current one
				if (next.getSegmentStart() != segmentEnd || 
						opposite.getRoad().getId() != segment.getRoad().getId()) {
					FinalRouteSegment frs = new FinalRouteSegment(segment.getRoad(), segment.getSegmentStart());
					float distStartObstacles = segment.distanceFromStart;
					frs.setParentRoute(segment.getParentRoute());
					frs.setParentSegmentEnd(segment.getParentSegmentEnd());
					frs.reverseWaySearch = reverseWay;
					frs.distanceFromStart = opposite.distanceFromStart + distStartObstacles;
					RouteSegment op = new RouteSegment(segment.getRoad(), segmentEnd);
					op.setParentRoute(opposite);
					op.setParentSegmentEnd(next.getSegmentStart());
					frs.distanceToEnd = 0;
					frs.opposite = op;
					ctx.finalRouteSegment = frs;
					return true;
				}
			}
			// road.id could be equal on roundabout, but we should accept them
			boolean alreadyVisited = visitedSegments.contains(nts);
			if (!alreadyVisited) {
				float distanceToEnd = (float) h(ctx, distToFinalPoint, next);
				if (next.parentRoute == null
						|| ctx.roadPriorityComparator(next.distanceFromStart, next.distanceToEnd, distFromStart, distanceToEnd) > 0) {
					if (next.parentRoute != null) {
						// already in queue remove it
						if (!graphSegments.remove(next)) {
							// exist in different queue!
							RouteSegment cpy = new RouteSegment(next.getRoad(), next.getSegmentStart());
							next = cpy;
						}
					}
					next.distanceFromStart = distFromStart;
					next.distanceToEnd = distanceToEnd;
					// put additional information to recover whole route after
					next.setParentRoute(segment);
					next.setParentSegmentEnd(segmentEnd);
					graphSegments.add(next);
				}
				if (ctx.visitor != null) {
//					ctx.visitor.visitSegment(next, false);
				}
			} else {
				// the segment was already visited! We need to follow better route if it exists
				// that is very strange situation and almost exception (it can happen when we underestimate distnceToEnd)
				if (distFromStart < next.distanceFromStart && next.road.id != segment.road.id) {
					// That code is incorrect (when segment is processed itself,
					// then it tries to make wrong u-turn) -
					// this situation should be very carefully checked in future (seems to be fixed)
					// System.out.println(segment.getRoad().getName() + " " + next.getRoad().getName());
					// System.out.println(next.distanceFromStart + " ! " + distFromStart);
					next.distanceFromStart = distFromStart;
					next.setParentRoute(segment);
					next.setParentSegmentEnd(segmentEnd);
					if (ctx.visitor != null) {
//						ctx.visitor.visitSegment(next, next.getSegmentStart(), false);
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
		return false;
	}
	
	
	
	/*public */static int roadPriorityComparator(double o1DistanceFromStart, double o1DistanceToEnd, 
			double o2DistanceFromStart, double o2DistanceToEnd, double heuristicCoefficient ) {
		// f(x) = g(x) + h(x)  --- g(x) - distanceFromStart, h(x) - distanceToEnd (not exact)
		return Double.compare(o1DistanceFromStart + heuristicCoefficient * o1DistanceToEnd, 
				o2DistanceFromStart + heuristicCoefficient *  o2DistanceToEnd);
	}

}
