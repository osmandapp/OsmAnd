package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.router.RoadSplitStructure.AttachedRoadInfo;
import net.osmand.util.Algorithms;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class RouteResultPreparation {

	public static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION = true;
	public static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = false;
	public static String PRINT_TO_GPX_FILE = null;
	private static final float TURN_DEGREE_MIN = 45;
	private static final float UNMATCHED_TURN_DEGREE_MINIMUM = 45;
	private static final float SPLIT_TURN_DEGREE_NOT_STRAIGHT = 100;
	private static final float TURN_SLIGHT_DEGREE = 5;
	public static final int SHIFT_ID = 6;
	protected static final Log LOG = PlatformUtil.getLog(RouteResultPreparation.class);
	public static final String UNMATCHED_HIGHWAY_TYPE = "unmatched";
	
	private static class CombineAreaRoutePoint {
		int x31;
		int y31;
		int originalIndex;
	}
	
	public static class RouteCalcResult {
		List<RouteSegmentResult> detailed = new ArrayList<RouteSegmentResult>();
		String error = null;
		
		public RouteCalcResult(List<RouteSegmentResult> list) {
			if(list == null) {
				error = "Result is empty";
			} else {
				this.detailed = list;
			}
		}
		
		public RouteCalcResult(String error) {
			this.error = error;
		}
		
		public List<RouteSegmentResult> getList() {
			return detailed;
		}
		
		public String getError() {
			return error;
		}

		public boolean isCorrect() {
			return error == null && !detailed.isEmpty();
		}
	}

	private void combineWayPointsForAreaRouting(RoutingContext ctx, List<RouteSegmentResult> result) {
		for(int i = 0; i < result.size(); i++) {
			RouteSegmentResult rsr = result.get(i);
			RouteDataObject obj = rsr.getObject();
			boolean area = false;
			if(obj.getPoint31XTile(0) == obj.getPoint31XTile(obj.getPointsLength() - 1) &&
					obj.getPoint31YTile(0) == obj.getPoint31YTile(obj.getPointsLength() - 1)) {
				area = true;
			}
			if(!area || !ctx.getRouter().isArea(obj)) {
				continue;
			}
			List<CombineAreaRoutePoint> originalWay = new ArrayList<CombineAreaRoutePoint>();
			List<CombineAreaRoutePoint> routeWay = new ArrayList<CombineAreaRoutePoint>();
			for(int j = 0;  j < obj.getPointsLength(); j++) {
				CombineAreaRoutePoint pnt = new CombineAreaRoutePoint();
				pnt.x31 = obj.getPoint31XTile(j);
				pnt.y31 = obj.getPoint31YTile(j);
				pnt.originalIndex = j;
				
				originalWay.add(pnt);
				if(j >= rsr.getStartPointIndex() && j <= rsr.getEndPointIndex()) {
					routeWay.add(pnt);
				} else if(j <= rsr.getStartPointIndex() && j >= rsr.getEndPointIndex()) {
					routeWay.add(0, pnt);
				}
			}
			int originalSize = routeWay.size();
			simplifyAreaRouteWay(routeWay, originalWay);
			int newsize = routeWay.size();
			if (routeWay.size() != originalSize) {
				RouteDataObject nobj = new RouteDataObject(obj);
				nobj.pointsX = new int[newsize];
				nobj.pointsY = new int[newsize];
				for (int k = 0; k < newsize; k++) {
					nobj.pointsX[k] = routeWay.get(k).x31;
					nobj.pointsY[k] = routeWay.get(k).y31;
				}
				// in future point names might be used
				nobj.restrictions = null;
				nobj.restrictionsVia = null;
				nobj.pointTypes = null;
				nobj.pointNames = null;
				nobj.pointNameTypes = null;
				RouteSegmentResult nrsr = new RouteSegmentResult(nobj, 0, newsize - 1);
				result.set(i, nrsr);
			}
		}
	}

	private void simplifyAreaRouteWay(List<CombineAreaRoutePoint> routeWay, List<CombineAreaRoutePoint> originalWay) {
		boolean changed = true;
		while (changed) {
			changed = false;
			int connectStart = -1;
			int connectLen = 0;
			double dist = 0;
			int length = routeWay.size() - 1;
			while (length > 0 && connectLen == 0) {
				for (int i = 0; i < routeWay.size() - length; i++) {
					CombineAreaRoutePoint p = routeWay.get(i);
					CombineAreaRoutePoint n = routeWay.get(i + length);
					if (segmentLineBelongsToPolygon(p, n, originalWay)) {
						double ndist = BinaryRoutePlanner.squareRootDist(p.x31, p.y31, n.x31, n.y31);
						if (ndist > dist) {
							ndist = dist;
							connectStart = i;
							connectLen = length;
						}
					}
				}
				length--;
			}
			while (connectLen > 1) {
				routeWay.remove(connectStart + 1);
				connectLen--;
				changed = true;
			}
		}
		
	}

	private boolean segmentLineBelongsToPolygon(CombineAreaRoutePoint p, CombineAreaRoutePoint n,
			List<CombineAreaRoutePoint> originalWay) {
		int intersections = 0;
		int mx = p.x31 / 2 + n.x31 / 2;
		int my = p.y31 / 2 + n.y31 / 2;
		for(int i = 1; i < originalWay.size(); i++) {
			CombineAreaRoutePoint p2 = originalWay.get(i -1);
			CombineAreaRoutePoint n2 = originalWay.get(i);
			if(p.originalIndex != i && p.originalIndex != i - 1) {
				if(n.originalIndex != i && n.originalIndex != i - 1) {
					if(MapAlgorithms.linesIntersect(p.x31, p.y31, n.x31, n.y31, p2.x31, p2.y31, n2.x31, n2.y31)) {
						return false;
					}
				}
			}
			int fx = MapAlgorithms.ray_intersect_x(p2.x31, p2.y31, n2.x31, n2.y31, my);
			if (Integer.MIN_VALUE != fx && mx >= fx) {
				intersections++;
			}
		}
		return intersections % 2 == 1;
	}

	public RouteCalcResult prepareResult(RoutingContext ctx, List<RouteSegmentResult> result) throws IOException {
		for (int i = 0; i < result.size(); i++) {
			RouteDataObject road = result.get(i).getObject();
			checkAndInitRouteRegion(ctx, road);
			// "osmand_dp" using for backward compatibility from native lib RoutingConfiguration directionPoints
			if (road.region != null) {
				road.region.findOrCreateRouteType(RoutingConfiguration.DirectionPoint.TAG, RoutingConfiguration.DirectionPoint.DELETE_TYPE);
			}
		}
		combineWayPointsForAreaRouting(ctx, result);
		validateAllPointsConnected(result);
		splitRoadsAndAttachRoadSegments(ctx, result);
		for (int i = 0; i < result.size(); i++) {
			filterMinorStops(result.get(i));
		}
		calculateTimeSpeed(ctx, result);
		prepareTurnResults(ctx, result);
		RouteCalcResult res = new RouteCalcResult(result);
		return res;
	}
	
	public RouteSegmentResult filterMinorStops(RouteSegmentResult seg) {
		List<Integer> stops = null;
		boolean plus = seg.getStartPointIndex() < seg.getEndPointIndex();
		int next;

		for (int i = seg.getStartPointIndex(); i != seg.getEndPointIndex(); i = next) {
			next = plus ? i + 1 : i - 1;
			int[] pointTypes = seg.getObject().getPointTypes(i);
			if (pointTypes != null) {
				for (int j = 0; j < pointTypes.length; j++) {
					if (pointTypes[j] == seg.getObject().region.stopMinor) {
						if (stops == null) {
							stops = new ArrayList<>();
						}
						stops.add(i);
					}
				}
			}
		}

		if (stops != null) {
			for (int stop : stops) {
				List<RouteSegmentResult> attachedRoutes = seg.getAttachedRoutes(stop);
				for (RouteSegmentResult attached : attachedRoutes) {
					int attStopPriority = highwaySpeakPriority(attached.getObject().getHighway());
					int segStopPriority = highwaySpeakPriority(seg.getObject().getHighway());
					if (segStopPriority < attStopPriority) {
						seg.getObject().removePointType(stop, seg.getObject().region.stopSign);
						break;
					}
				}
			}
		}
		return seg;
	}

	public void prepareTurnResults(RoutingContext ctx, List<RouteSegmentResult> result) {
		for (int i = 0; i < result.size(); i ++) {
			TurnType turnType = getTurnInfo(result, i, ctx.leftSideNavigation);
			result.get(i).setTurnType(turnType);
		}
		
		determineTurnsToMerge(ctx.leftSideNavigation, result);
		ignorePrecedingStraightsOnSameIntersection(ctx.leftSideNavigation, result);
		justifyUTurns(ctx.leftSideNavigation, result);
		avoidKeepForThroughMoving(result);
		muteAndRemoveTurns(result, ctx);
		addTurnInfoDescriptions(result);
	}

	protected void ignorePrecedingStraightsOnSameIntersection(boolean leftside, List<RouteSegmentResult> result) {
		//Issue 2571: Ignore TurnType.C if immediately followed by another turn in non-motorway cases, as these likely belong to the very same intersection
		RouteSegmentResult nextSegment = null;
		double distanceToNextTurn = 999999;
		for (int i = result.size() - 1; i >= 0; i--) {
			// Mark next "real" turn
			if (nextSegment != null && nextSegment.getTurnType() != null &&
					nextSegment.getTurnType().getValue() != TurnType.C &&
					!isMotorway(nextSegment)) {
				if (distanceToNextTurn == 999999) {
					distanceToNextTurn = 0;
				}
			}
			RouteSegmentResult currentSegment = result.get(i);
			// Identify preceding goStraights within distance limit and suppress
			if (currentSegment != null) {
				distanceToNextTurn += currentSegment.getDistance();
				if (currentSegment.getTurnType() != null &&
						currentSegment.getTurnType().getValue() == TurnType.C &&
						distanceToNextTurn <= 200) {
					currentSegment.getTurnType().setSkipToSpeak(true);
				} else {
					nextSegment = currentSegment;
					if (currentSegment.getTurnType() != null) {
						distanceToNextTurn = 999999;
					}
				}
			}
		}
	}

	private void justifyUTurns(boolean leftSide, List<RouteSegmentResult> result) {
		int next;
		for (int i = 1; i < result.size() - 1; i = next) {
			next = i + 1;
			TurnType t = result.get(i).getTurnType();
			// justify turn
			if (t != null) {
				TurnType jt = justifyUTurn(leftSide, result, i, t);
				if (jt != null) {
					result.get(i).setTurnType(jt);
					next = i + 2;
				}
			}
		}
	}

	// decrease speed proportionally from 15ms (50kmh)
	private static final double SLOW_DOWN_SPEED_THRESHOLD = 15;
	// reference speed 30ms (108kmh) - 2ms (7kmh)
	private static final double SLOW_DOWN_SPEED = 2;
	
	public static void calculateTimeSpeed(RoutingContext ctx, List<RouteSegmentResult> result) {
		for (int i = 0; i < result.size(); i++) {
			RouteSegmentResult rr = result.get(i);
			calculateTimeSpeed(ctx, rr);
		}
	}

	public static void calculateTimeSpeed(RoutingContext ctx, RouteSegmentResult rr) {
		// Naismith's/Scarf rules add additional travel time when moving uphill
		boolean useNaismithRule = false;
		double scarfSeconds = 0; // Additional time as per Naismith/Scarf
		GeneralRouter currentRouter = (GeneralRouter) ctx.getRouter();
		if (currentRouter.getProfile() == GeneralRouterProfile.PEDESTRIAN) {
			// PEDESTRIAN 1:7.92 based on https://en.wikipedia.org/wiki/Naismith%27s_rule (Scarf rule)
			scarfSeconds = 7.92f / currentRouter.getDefaultSpeed();
			useNaismithRule = true;
		} else if (currentRouter.getProfile() == GeneralRouterProfile.BICYCLE) {
			// BICYCLE 1:8.2 based on https://pubmed.ncbi.nlm.nih.gov/17454539/ (Scarf's article)
			scarfSeconds = 8.2f / currentRouter.getDefaultSpeed();
			useNaismithRule = true;
		}
		
		RouteDataObject road = rr.getObject();
		double distOnRoadToPass = 0;
		double speed = ctx.getRouter().defineVehicleSpeed(road, rr.isForwardDirection());
		if (speed == 0) {
			speed = ctx.getRouter().getDefaultSpeed();
		} else {
			if (speed > SLOW_DOWN_SPEED_THRESHOLD) {
				speed = speed - (speed / SLOW_DOWN_SPEED_THRESHOLD - 1) * SLOW_DOWN_SPEED;
			}
		}
		boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
		int next;
		double distance = 0;

		//for Naismith/Scarf
		float[] heightDistanceArray = null;
		if (useNaismithRule) {
			road.calculateHeightArray();
			heightDistanceArray = road.heightDistanceArray;
		}

		for (int j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
			next = plus ? j + 1 : j - 1;
			double d = measuredDist(road.getPoint31XTile(j), road.getPoint31YTile(j), road.getPoint31XTile(next),
					road.getPoint31YTile(next));
			distance += d;
			double obstacle = ctx.getRouter().defineObstacle(road, j, plus);
			if (obstacle < 0) {
				obstacle = 0;
			}
			distOnRoadToPass += d / speed + obstacle;  //this is time in seconds

			//for Naismith/Scarf
			if (useNaismithRule) {
				int heightIndex = 2 * j + 1;
				int nextHeightIndex = 2 * next + 1;
				if (heightDistanceArray != null && heightIndex < heightDistanceArray.length && nextHeightIndex < heightDistanceArray.length) {
					float heightDiff = heightDistanceArray[nextHeightIndex] - heightDistanceArray[heightIndex];
					if (heightDiff > 0) { // ascent only
						// Naismith/Scarf rule: An ascent adds 7.92 times the hiking time its vertical elevation gain takes to cover horizontally
						//   (- Naismith original: Add 1 hour per vertical 2000ft (600m) at assumed horizontal speed 3mph)
						//   (- Swiss Alpine Club: Uses conservative 1 hour per 400m at 4km/h)
						distOnRoadToPass += heightDiff * scarfSeconds;
					}
				}
			}
		}

		// last point turn time can be added
		// if(i + 1 < result.size()) { distOnRoadToPass += ctx.getRouter().calculateTurnTime(); }
		rr.setDistance((float) distance);
		rr.setSegmentTime((float) distOnRoadToPass);
		if (distOnRoadToPass != 0) {
			rr.setSegmentSpeed((float) (distance / distOnRoadToPass));  //effective segment speed incl. obstacle and height effects
		} else {
			rr.setSegmentSpeed((float) speed);
		}
	}

	public static void recalculateTimeDistance(List<RouteSegmentResult> result) {
		for (int i = 0; i < result.size(); i++) {
			RouteSegmentResult rr = result.get(i);
			RouteDataObject road = rr.getObject();
			double distOnRoadToPass = 0;
			double speed = rr.getSegmentSpeed();
			if (speed == 0) {
				continue;
			}
			boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
			int next;
			double distance = 0;
			for (int j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
				next = plus ? j + 1 : j - 1;
				double d = measuredDist(road.getPoint31XTile(j), road.getPoint31YTile(j), road.getPoint31XTile(next),
						road.getPoint31YTile(next));
				distance += d;
				distOnRoadToPass += d / speed;  //this is time in seconds
			}
			rr.setSegmentTime((float) distOnRoadToPass);
			rr.setSegmentSpeed((float) speed);
			rr.setDistance((float) distance);
		}
	}

	private void splitRoadsAndAttachRoadSegments(RoutingContext ctx, List<RouteSegmentResult> result) throws IOException {
		for (int i = 0; i < result.size(); i++) {
			if (ctx.checkIfMemoryLimitCritical(ctx.config.memoryLimitation)) {
				ctx.unloadUnusedTiles(ctx.config.memoryLimitation);
			}
			RouteSegmentResult rr = result.get(i);
			boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
			int next;
			boolean unmatched = UNMATCHED_HIGHWAY_TYPE.equals(rr.getObject().getHighway());
			for (int j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
				next = plus ? j + 1 : j - 1;
				if (j == rr.getStartPointIndex()) {
					attachRoadSegments(ctx, result, i, j, plus);
				}
				if (next != rr.getEndPointIndex()) {
					attachRoadSegments(ctx, result, i, next, plus);
				}
				List<RouteSegmentResult> attachedRoutes = rr.getAttachedRoutes(next);
				boolean tryToSplit = next != rr.getEndPointIndex() && !rr.getObject().roundabout() && attachedRoutes != null;
				if (rr.getDistance(next, plus) == 0) {
					// same point will be processed next step
					tryToSplit = false;
				}
				if (tryToSplit) {
					float distBearing = unmatched ? RouteSegmentResult.DIST_BEARING_DETECT_UNMATCHED : RouteSegmentResult.DIST_BEARING_DETECT;
					// avoid small zigzags
					float before = rr.getBearingEnd(next, distBearing);
					float after = rr.getBearingBegin(next, distBearing);
					if (rr.getDistance(next, plus) < distBearing / 2) {
						after = before;
					} else if (rr.getDistance(next, !plus) < distBearing / 2) {
						before = after;
					}
					double contAngle = Math.abs(MapUtils.degreesDiff(before, after));
					boolean straight = contAngle < TURN_DEGREE_MIN;
					boolean isSplit = false;
					
					if (unmatched && Math.abs(contAngle) >= UNMATCHED_TURN_DEGREE_MINIMUM) {
						isSplit = true;
					}
					// split if needed
					for (RouteSegmentResult rs : attachedRoutes) {
						double diff = MapUtils.degreesDiff(before, rs.getBearingBegin());
						if (Math.abs(diff) <= TURN_DEGREE_MIN) {
							isSplit = true;
						} else if (!straight && Math.abs(diff) < SPLIT_TURN_DEGREE_NOT_STRAIGHT) {
							isSplit = true;
						}
					}
					if (isSplit) {
						int endPointIndex = rr.getEndPointIndex();
						RouteSegmentResult split = new RouteSegmentResult(rr.getObject(), next, endPointIndex);
						split.copyPreattachedRoutes(rr, Math.abs(next - rr.getStartPointIndex()));
						rr.setEndPointIndex(next);
						result.add(i + 1, split);
						i++;
						// switch current segment to the splitted
						rr = split;
					}
				}
			}
		}
	}

	private void checkAndInitRouteRegion(RoutingContext ctx, RouteDataObject road) throws IOException {
		BinaryMapIndexReader reader = ctx.reverseMap.get(road.region);
		if (reader != null) {
			reader.initRouteRegion(road.region);
		}
	}

	public void validateAllPointsConnected(List<RouteSegmentResult> result) {
		for (int i = 1; i < result.size(); i++) {
			RouteSegmentResult rr = result.get(i);
			RouteSegmentResult pr = result.get(i - 1);
			double d = MapUtils.getDistance(pr.getPoint(pr.getEndPointIndex()), rr.getPoint(rr.getStartPointIndex()));
			if (d > 0) {
				System.out.printf("Points are not connected: %d-%d of %d %s (%d) -> %s (%d) by %.2f meters\n",
						i - 1, i, result.size() - 1, pr.getObject(), pr.getEndPointIndex(),
						rr.getObject(), rr.getStartPointIndex(), d);
			}
		}
	}

	public List<RouteSegmentResult> convertFinalSegmentToResults(RoutingContext ctx, FinalRouteSegment finalSegment) {
		List<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>();
		if (finalSegment != null) {
			ctx.routingTime += finalSegment.distanceFromStart;
			float correctionTime = finalSegment.opposite == null ? 0 :
				finalSegment.distanceFromStart - distanceFromStart(finalSegment.opposite) - distanceFromStart(finalSegment.parentRoute);
			// println("Routing calculated time distance " + finalSegment.distanceFromStart);
			// Get results from opposite direction roads
			RouteSegment thisSegment =  finalSegment.opposite == null ? finalSegment : finalSegment.parentRoute; // for dijkstra
			RouteSegment segment = finalSegment.reverseWaySearch ? thisSegment : finalSegment.opposite;
			while (segment != null) {
				RouteSegmentResult res = new RouteSegmentResult(segment.road, segment.getSegmentEnd(), segment.getSegmentStart());
				float parentRoutingTime = segment.getParentRoute() != null ? segment.getParentRoute().distanceFromStart : 0;
				res.setRoutingTime(segment.distanceFromStart - parentRoutingTime + correctionTime);
				correctionTime = 0;
				segment = segment.getParentRoute();
				addRouteSegmentToResult(ctx, result, res, false);
				
			}
			// reverse it just to attach good direction roads
			Collections.reverse(result);
			segment = finalSegment.reverseWaySearch ? finalSegment.opposite : thisSegment;
			while (segment != null) {
				RouteSegmentResult res = new RouteSegmentResult(segment.road, segment.getSegmentStart(), segment.getSegmentEnd());
				float parentRoutingTime = segment.getParentRoute() != null ? segment.getParentRoute().distanceFromStart : 0;
				res.setRoutingTime(segment.distanceFromStart - parentRoutingTime + correctionTime);
				correctionTime = 0;
				segment = segment.getParentRoute();
				// happens in smart recalculation
				addRouteSegmentToResult(ctx, result, res, true);
			}
			Collections.reverse(result);
			checkTotalRoutingTime(result, finalSegment.distanceFromStart);
		}
		return result;
	}

	private float distanceFromStart(RouteSegment s) {
		return s == null ? 0 : s.distanceFromStart;
	}

	protected void checkTotalRoutingTime(List<RouteSegmentResult> result, float cmp) {
		float totalRoutingTime = 0;
		for (RouteSegmentResult r : result) {
			totalRoutingTime += r.getRoutingTime();
		}
		if (Math.abs(totalRoutingTime - cmp) > 0.1) {
			println("Total sum routing time ! " + totalRoutingTime + " == " + cmp);
		}
	}
	
	private void addRouteSegmentToResult(RoutingContext ctx, List<RouteSegmentResult> result, RouteSegmentResult res, boolean reverse) {
		if (res.getStartPointIndex() != res.getEndPointIndex()) {
			if (result.size() > 0) {
				RouteSegmentResult last = result.get(result.size() - 1);
				if (last.getObject().id == res.getObject().id && ctx.calculationMode != RouteCalculationMode.BASE) {
					if (combineTwoSegmentResult(res, last, reverse)) {
						return;
					}
				}
			}
			result.add(res);
		}
	}
	
	private boolean combineTwoSegmentResult(RouteSegmentResult toAdd, RouteSegmentResult previous, 
			boolean reverse) {
		boolean ld = previous.getEndPointIndex() > previous.getStartPointIndex();
		boolean rd = toAdd.getEndPointIndex() > toAdd.getStartPointIndex();
		if (rd == ld) {
			if (toAdd.getStartPointIndex() == previous.getEndPointIndex() && !reverse) {
				previous.setEndPointIndex(toAdd.getEndPointIndex());
				previous.setRoutingTime(previous.getRoutingTime() + toAdd.getRoutingTime());
				return true;
			} else if (toAdd.getEndPointIndex() == previous.getStartPointIndex() && reverse) {
				previous.setStartPointIndex(toAdd.getStartPointIndex());
				previous.setRoutingTime(previous.getRoutingTime() + toAdd.getRoutingTime());
				return true;
			}
		}
		return false;
	}
	
	public static void printResults(RoutingContext ctx, LatLon start, LatLon end, List<RouteSegmentResult> result) {
		Map<String, Object> info =  new LinkedHashMap<String, Object>();
		Map<String, Object> route =  new LinkedHashMap<String, Object>();
		info.put("route", route);
		
		route.put("routing_time", String.format("%.1f", ctx.routingTime));
		route.put("vehicle", ctx.config.routerName);
		route.put("base", ctx.calculationMode == RouteCalculationMode.BASE);
		route.put("start_lat", String.format("%.5f", start.getLatitude()));
		route.put("start_lon", String.format("%.5f", start.getLongitude()));
		route.put("target_lat", String.format("%.5f", end.getLatitude()));
		route.put("target_lon", String.format("%.5f", end.getLongitude()));
		if (result != null) {
			float completeTime = 0;
			float completeDistance = 0;
			for (RouteSegmentResult r : result) {
				completeTime += r.getSegmentTime();
				completeDistance += r.getDistance();
			}
			route.put("complete_distance", String.format("%.1f", completeDistance));
			route.put("complete_time", String.format("%.1f", completeTime));
			
		}
		route.put("native", ctx.nativeLib != null);
		
		if (ctx.calculationProgress != null && ctx.calculationProgress.timeToCalculate > 0) {
			info.putAll(ctx.calculationProgress.getInfo(ctx.calculationProgressFirstPhase));
		}
		
		String alerts = String.format("Alerts during routing: %d fastRoads, %d slowSegmentsEearlier",
				ctx.alertFasterRoadToVisitedSegments, ctx.alertSlowerSegmentedWasVisitedEarlier);
		if (ctx.alertFasterRoadToVisitedSegments + ctx.alertSlowerSegmentedWasVisitedEarlier == 0) {
			alerts = "No alerts";
		}
		println("ROUTE. " + alerts);
		List<String> routeInfo = new ArrayList<String>();
		StringBuilder extraInfo = buildRouteMessagesFromInfo(info, routeInfo);
		if (PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST && result != null) {
			println(String.format("<test %s>",extraInfo.toString()));
			printRouteInfoSegments(result);
			println("</test>");
			// duplicate base info
			if (ctx.calculationProgressFirstPhase != null) {
				println("<<<1st Phase>>>>");
				List<String> baseRouteInfo = new ArrayList<String>();
				buildRouteMessagesFromInfo(ctx.calculationProgressFirstPhase.getInfo(null), baseRouteInfo);
				for (String msg : baseRouteInfo) {
					println(msg);
				}
				println("<<<2nd Phase>>>>");
			}
		}
		for (String msg : routeInfo) {
			println(msg);
		}
//		calculateStatistics(result);
	}

	private static StringBuilder buildRouteMessagesFromInfo(Map<String, Object> info, List<String> routeMessages) {
		StringBuilder extraInfo = new StringBuilder(); 
		for (String key : info.keySet()) {
			// // GeneralRouter.TIMER = 0;
			if (info.get(key) instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> mp = (Map<String, Object>) info.get(key);
				StringBuilder msg = new StringBuilder("Route <" + key + ">");
				int i = 0;
				for (String mkey : mp.keySet()) {
					msg.append((i++ == 0) ? ": " : ", ");
					Object obj = mp.get(mkey);
					String valueString = obj.toString();
					if (obj instanceof Double || obj instanceof Float) {
						valueString = String.format("%.1f", ((Number) obj).doubleValue());
					}
					msg.append(mkey).append("=").append(valueString);
					extraInfo.append(" ").append(key + "_" + mkey).append("=\"").append(valueString).append("\"");
				}
				if (routeMessages != null) {
					routeMessages.add(msg.toString());
				}
			}
		}
		return extraInfo;
	}

	private static void printRouteInfoSegments(List<RouteSegmentResult> result) {
		org.xmlpull.v1.XmlSerializer serializer = null;
		if (PRINT_TO_GPX_FILE != null) {
			serializer = PlatformUtil.newSerializer();
			try {
				serializer.setOutput(new FileWriter(PRINT_TO_GPX_FILE));
				serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
				// indentation as 3 spaces
				// serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-indentation", " ");
				// // also set the line separator
				// serializer.setProperty("http://xmlpull.org/v1/doc/properties.html#serializer-line-separator",
				// "\n");
				serializer.startDocument("UTF-8", true);
				serializer.startTag("", "gpx");
				serializer.attribute("", "version", "1.1");
				serializer.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");
				serializer.attribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
				serializer.attribute("", "xmlns:schemaLocation",
						"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");
				serializer.startTag("", "trk");
				serializer.startTag("", "trkseg");
			} catch (IOException e) {
				e.printStackTrace();
				serializer = null;
			}
		}
				
		double lastHeight = -180;		
		for (RouteSegmentResult res : result) {
			String name = res.getObject().getName();
			String ref = res.getObject().getRef("", false, res.isForwardDirection());
			if (name == null) {
				name = "";
			}
			if (ref != null) {
				name += " (" + ref + ") ";
			}
			StringBuilder additional = new StringBuilder();
			additional.append("time = \"").append(((int)res.getSegmentTime()*100)/100.0f).append("\" ");
			if (res.getRoutingTime() > 0) {
//					additional.append("rspeed = \"")
//							.append((int) Math.round(res.getDistance() / res.getRoutingTime() * 3.6)).append("\" ");
				additional.append(String.format("rtime = \"%.1f\" ", res.getRoutingTime()));
			}
			
//				additional.append("rtime = \"").append(res.getRoutingTime()).append("\" ");
			additional.append("name = \"").append(name).append("\" ");
//				float ms = res.getSegmentSpeed();
			float ms = res.getObject().getMaximumSpeed(res.isForwardDirection());
			if(ms > 0) {
				additional.append("maxspeed = \"").append((int) Math.round(ms * 3.6f)).append("\" ");
			}
			additional.append("distance = \"").append(((int)res.getDistance()*100)/100.0f).append("\" ");
			additional.append(res.getObject().getHighway()).append(" ");
			if (res.getTurnType() != null) {
				additional.append("turn = \"").append(res.getTurnType()).append("\" ");
				additional.append("turn_angle = \"").append(res.getTurnType().getTurnAngle()).append("\" ");
				if (res.getTurnType().getLanes() != null) {
					additional.append("lanes = \"").append(Arrays.toString(res.getTurnType().getLanes())).append("\" ");
				}
			}
			additional.append("start_bearing = \"").append(res.getBearingBegin()).append("\" ");
			additional.append("end_bearing = \"").append(res.getBearingEnd()).append("\" ");
			additional.append("height = \"").append(Arrays.toString(res.getHeightValues())).append("\" ");
			additional.append("description = \"").append(res.getDescription(false)).append("\" ");
			println(MessageFormat.format("\t<segment id=\"{0}\" oid=\"{1}\" start=\"{2}\" end=\"{3}\" {4}/>",
					(res.getObject().getId() >> (SHIFT_ID )) + "", res.getObject().getId() + "", 
					res.getStartPointIndex() + "", res.getEndPointIndex() + "", additional.toString()));
			int inc = res.getStartPointIndex() < res.getEndPointIndex() ? 1 : -1;
			int indexnext = res.getStartPointIndex();
			LatLon prev = null;
			for (int index = res.getStartPointIndex() ; index != res.getEndPointIndex(); ) {
				index = indexnext;
				indexnext += inc; 
				if (serializer != null) {
					try {
						LatLon l = res.getPoint(index);
						serializer.startTag("","trkpt");
						serializer.attribute("", "lat",  l.getLatitude() + "");
						serializer.attribute("", "lon",  l.getLongitude() + "");
						float[] vls = res.getObject().heightDistanceArray;
						double dist = prev == null ? 0 : MapUtils.getDistance(prev, l);
						if(index * 2 + 1 < vls.length) {
							double h = vls[2*index + 1];
							serializer.startTag("","ele");
							serializer.text(h +"");
							serializer.endTag("","ele");
							if(lastHeight != -180 && dist > 0) {
								serializer.startTag("","cmt");
								serializer.text((float) ((h -lastHeight)/ dist*100) + "% " +
								" degree " + (float) Math.atan(((h -lastHeight)/ dist)) / Math.PI * 180 +  
								" asc " + (float) (h -lastHeight) + " dist "
										+ (float) dist);
								serializer.endTag("","cmt");
								serializer.startTag("","slope");
								serializer.text((h -lastHeight)/ dist*100 + "");
								serializer.endTag("","slope");
							}
							serializer.startTag("","desc");
							serializer.text((res.getObject().getId() >> (SHIFT_ID )) + " " + index);
							serializer.endTag("","desc");
							lastHeight = h;
						} else if(lastHeight != -180){
//								serializer.startTag("","ele");
//								serializer.text(lastHeight +"");
//								serializer.endTag("","ele");
						}
						serializer.endTag("", "trkpt");
						prev = l;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			printAdditionalPointInfo(res);
		}
		if (serializer != null) {
			try {
				serializer.endTag("", "trkseg");
				serializer.endTag("", "trk");
				serializer.endTag("", "gpx");
				serializer.endDocument();
				serializer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void calculateStatistics(List<RouteSegmentResult> result) {
		InputStream is = RenderingRulesStorage.class.getResourceAsStream("default.render.xml");
		final Map<String, String> renderingConstants = new LinkedHashMap<String, String>();
		try {
			InputStream pis = RenderingRulesStorage.class.getResourceAsStream("default.render.xml");
			try {
				XmlPullParser parser = PlatformUtil.newXMLPullParser();
				parser.setInput(pis, "UTF-8");
				int tok;
				while ((tok = parser.next()) != XmlPullParser.END_DOCUMENT) {
					if (tok == XmlPullParser.START_TAG) {
						String tagName = parser.getName();
						if (tagName.equals("renderingConstant")) {
							if (!renderingConstants.containsKey(parser.getAttributeValue("", "name"))) {
								renderingConstants.put(parser.getAttributeValue("", "name"), 
										parser.getAttributeValue("", "value"));
							}
						}
					}
				}
			} finally {
				pis.close();
			}
			RenderingRulesStorage rrs = new RenderingRulesStorage("default", renderingConstants);
			rrs.parseRulesFromXmlInputStream(is, new RenderingRulesStorageResolver() {
				
				@Override
				public RenderingRulesStorage resolve(String name, RenderingRulesStorageResolver ref)
						throws XmlPullParserException, IOException {
					throw new UnsupportedOperationException();
				}
			}, false);
			RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
			List<RouteStatistics> rsr = RouteStatisticsHelper.calculateRouteStatistic(result, null, rrs, null, req);
			for(RouteStatistics r : rsr) {
				System.out.println(r);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		
	}

	private static void printAdditionalPointInfo(RouteSegmentResult res) {
		boolean plus = res.getStartPointIndex() < res.getEndPointIndex();
		StringBuilder bld = new StringBuilder();
		for (int k = res.getStartPointIndex(); k != res.getEndPointIndex();) {
			int[] tp = res.getObject().getPointTypes(k);
			String[] pointNames = res.getObject().getPointNames(k);
			int[] pointNameTypes = res.getObject().getPointNameTypes(k);
			if (tp != null || pointNameTypes != null) {
				bld.append("<point " + (k));
				if (tp != null) {
					for (int t = 0; t < tp.length; t++) {
						RouteTypeRule rr = res.getObject().region.quickGetEncodingRule(tp[t]);
						bld.append(" " + rr.getTag() + "=\"" + rr.getValue() + "\"");
					}
				}
				if (pointNameTypes != null) {
					for (int t = 0; t < pointNameTypes.length; t++) {
						RouteTypeRule rr = res.getObject().region.quickGetEncodingRule(pointNameTypes[t]);
						bld.append(" " + rr.getTag() + "=\"" + pointNames[t] + "\"");
					}
				}
				bld.append("/> ");
			}
			if (plus) {
				k++;
			} else {
				k--;
			}
		}
		if (bld.length() > 0) {
			println("\t" + bld.toString());
		}
	}


	public void addTurnInfoDescriptions(List<RouteSegmentResult> result) {
		int prevSegment = -1;
		float dist = 0;
		for (int i = 0; i <= result.size(); i++) {
			if (i == result.size() || result.get(i).getTurnType() != null) {
				if (prevSegment >= 0) {
					RouteSegmentResult turnInfo = result.get(prevSegment);
					String turn = turnInfo.getTurnType().toString();
					String mute = turnInfo.getTurnType().isSkipToSpeak() ? "[MUTE] " : "";
					String streetName = "";
					if (prevSegment < result.size() - 1) {
						String nm = result.get(prevSegment + 1).getStreetName("", false, result, prevSegment + 1);
						if (nm == null) {
							nm = "";
						}
						String ref = result.get(prevSegment + 1).getRef("", false);
						if (ref == null) {
							ref = "";
						}
						if (!Algorithms.isEmpty(nm) || !Algorithms.isEmpty(ref)) {
							streetName = String.format("onto %s %s " , nm, ref);
						}
						String to = result.get(prevSegment + 1).getDestinationName("", false, result, prevSegment + 1, true);
						if(!Algorithms.isEmpty(to)) {
							streetName = "to " + to; 
						}
					}
					turnInfo.setDescription(String.format("%s %s and go %.1f km", mute, turn, dist / 1000.0),
							String.format("%s %s %s and go %.1f km", mute, turn, streetName, dist / 1000.0));
				}
				prevSegment = i;
				dist = 0;
			}
			if (i < result.size()) {
				dist += result.get(i).getDistance();
			}
		}
	}

	protected TurnType justifyUTurn(boolean leftside, List<RouteSegmentResult> result, int i, TurnType t) {
		boolean tl = TurnType.isLeftTurnNoUTurn(t.getValue());
		boolean tr = TurnType.isRightTurnNoUTurn(t.getValue());
		if(tl || tr) {
			TurnType tnext = result.get(i + 1).getTurnType();
			if (tnext != null && result.get(i).getDistance() < 50) { //
				boolean ut = true;
				if (i > 0) {
					double uTurn = MapUtils.degreesDiff(result.get(i - 1).getBearingEnd(), 
							result.get(i + 1).getBearingBegin());
					if (Math.abs(uTurn) < 120) {
						ut = false;
					}
				}
//				String highway = result.get(i).getObject().getHighway();
//				if(highway == null || highway.endsWith("track") || highway.endsWith("services") || highway.endsWith("service")
//						|| highway.endsWith("path")) {
//					ut = false;
//				}
				if (result.get(i - 1).getObject().getOneway() == 0 || result.get(i + 1).getObject().getOneway() == 0) {
					ut = false;
				}
				if (!Algorithms.objectEquals(getStreetName(result, i - 1, false), 
						getStreetName(result, i + 1, true))) {
					ut = false;
				}
				if (ut) {
					tnext.setSkipToSpeak(true);
					if (tl && TurnType.isLeftTurnNoUTurn(tnext.getValue())) {
						TurnType tt = TurnType.valueOf(TurnType.TU, false);
						tt.setLanes(t.getLanes());
						return tt;
					} else if (tr && TurnType.isRightTurnNoUTurn(tnext.getValue())) {
						TurnType tt = TurnType.valueOf(TurnType.TU, true);
						tt.setLanes(t.getLanes());
						return tt;
					}
				}
			}
		}
		return null;
	}

	private String getStreetName(List<RouteSegmentResult> result, int i, boolean dir) {
		String nm = result.get(i).getObject().getName();
		if (Algorithms.isEmpty(nm)) {
			if (!dir) {
				if (i > 0) {
					nm = result.get(i - 1).getObject().getName();
				}
			} else {
				if(i < result.size() - 1) {
					nm = result.get(i + 1).getObject().getName();
				}
			}
		}
		
		return nm;
	}

	private void determineTurnsToMerge(boolean leftside, List<RouteSegmentResult> result) {
		RouteSegmentResult nextSegment = null;
		double dist = 0;
		for (int i = result.size() - 1; i >= 0; i--) {
			RouteSegmentResult currentSegment = result.get(i);
			TurnType currentTurn = currentSegment.getTurnType();
			dist += currentSegment.getDistance();
			if (currentTurn == null || currentTurn.getLanes() == null) {
				// skip
			} else {
				boolean merged = false;
				if (nextSegment != null) {
					String hw = currentSegment.getObject().getHighway();
					double mergeDistance = 200;
					if (hw != null && (hw.startsWith("trunk") || hw.startsWith("motorway"))) {
						mergeDistance = 400;
					}
					if (dist < mergeDistance) {
						mergeTurnLanes(leftside, currentSegment, nextSegment);
						inferCommonActiveLane(currentSegment.getTurnType(), nextSegment.getTurnType());
						merged = true;
						replaceConfusingKeepTurnsWithLaneTurn(currentSegment, leftside);
					}
				}
				if (!merged) {
					TurnType tt = currentSegment.getTurnType();
					inferActiveTurnLanesFromTurn(tt, tt.getValue());
				}
				nextSegment = currentSegment;
				dist = 0;
			}
		}
	}

	private void replaceConfusingKeepTurnsWithLaneTurn(RouteSegmentResult currentSegment, boolean leftSide) {
		if (currentSegment.getTurnType() == null) {
			return;
		}
		int currentTurn = currentSegment.getTurnType().getValue();
		int activeTurn = currentSegment.getTurnType().getActiveCommonLaneTurn();
		boolean changeToActive = false;
		if (TurnType.isKeepDirectionTurn(currentTurn) && !TurnType.isKeepDirectionTurn(activeTurn)) {
			if (TurnType.isLeftTurn(currentTurn) && !TurnType.isLeftTurn(activeTurn)) {
				changeToActive = true;
			}
			if (TurnType.isRightTurn(currentTurn) && !TurnType.isRightTurn(activeTurn)) {
				changeToActive = true;
			}
		}
		if (changeToActive) {
			TurnType turn = TurnType.valueOf(activeTurn, leftSide);
			turn.setLanes(currentSegment.getTurnType().getLanes());
			currentSegment.setTurnType(turn);
		}
	}

	private void inferActiveTurnLanesFromTurn(TurnType tt, int type) {
		boolean found = false;
		if (tt.getValue() == type && tt.getLanes() != null) {
			for (int it = 0; it < tt.getLanes().length; it++) {
				int turn = tt.getLanes()[it];
				if (TurnType.getPrimaryTurn(turn) == type ||
						TurnType.getSecondaryTurn(turn) == type ||
						TurnType.getTertiaryTurn(turn) == type) {
					found = true;
					break;
				}
			}
		}
		if (found) {
			for (int it = 0; it < tt.getLanes().length; it++) {
				int turn = tt.getLanes()[it];
				if (TurnType.getPrimaryTurn(turn) != type) {
					if(TurnType.getSecondaryTurn(turn) == type) {
						int st = TurnType.getSecondaryTurn(turn);
						TurnType.setSecondaryTurn(tt.getLanes(), it, TurnType.getPrimaryTurn(turn));
						TurnType.setPrimaryTurn(tt.getLanes(), it, st);
					} else if(TurnType.getTertiaryTurn(turn) == type) {
						int st = TurnType.getTertiaryTurn(turn);
						TurnType.setTertiaryTurn(tt.getLanes(), it, TurnType.getPrimaryTurn(turn));
						TurnType.setPrimaryTurn(tt.getLanes(), it, st);
					} else {
						tt.getLanes()[it] = turn & (~1);
					}
				} else {
					tt.getLanes()[it] = turn | 1;
				}
			}
		}
	}
	
	private class MergeTurnLaneTurn {
		TurnType turn;
		int[] originalLanes;
		int[] disabledLanes;
		int activeStartIndex = -1;
		int activeEndIndex = -1;
		int activeLen = 0;
		
		public MergeTurnLaneTurn(RouteSegmentResult segment) {
			this.turn = segment.getTurnType();
			if(turn != null) {
				originalLanes = turn.getLanes();
			}
			if(originalLanes != null) {
				disabledLanes = new int[originalLanes.length];
				for (int i = 0; i < originalLanes.length; i++) {
					int ln = originalLanes[i];
					disabledLanes[i] = ln & ~1;
					if ((ln & 1) > 0) {
						if (activeStartIndex == -1) {
							activeStartIndex = i;
						}
						activeEndIndex = i;
						activeLen++;
					}
				}
			}
		}
		
		public boolean isActiveTurnMostLeft() {
			return activeStartIndex == 0;
		}
		public boolean isActiveTurnMostRight() {
			return activeEndIndex == originalLanes.length - 1;
		}
	}
	
	private boolean mergeTurnLanes(boolean leftSide, RouteSegmentResult currentSegment, RouteSegmentResult nextSegment) {
		MergeTurnLaneTurn active = new MergeTurnLaneTurn(currentSegment);
		MergeTurnLaneTurn target = new MergeTurnLaneTurn(nextSegment);
		if (active.activeLen < 2) {
			return false;
		}
		if (target.activeStartIndex == -1) {
			return false;
		}
		boolean changed = false;
		if (target.isActiveTurnMostLeft()) {
			// let only the most left lanes be enabled
			if (target.activeLen < active.activeLen) {
				active.activeEndIndex -= (active.activeLen - target.activeLen);
				changed = true;
			}
		} else if (target.isActiveTurnMostRight()) {
			// next turn is right
			// let only the most right lanes be enabled
			if (target.activeLen < active.activeLen ) {
				active.activeStartIndex += (active.activeLen - target.activeLen);
				changed = true;
			}
		} else {
			// next turn is get through (take out the left and the right turn)
			if (target.activeLen < active.activeLen) {
				if(target.originalLanes.length == active.activeLen) {
					active.activeEndIndex = active.activeStartIndex + target.activeEndIndex;
					active.activeStartIndex = active.activeStartIndex + target.activeStartIndex;
					changed = true;
				} else {
					int straightActiveLen = 0;
					int straightActiveBegin = -1;
					for(int i = active.activeStartIndex; i <= active.activeEndIndex; i++) {
						if(TurnType.hasAnyTurnLane(active.originalLanes[i], TurnType.C)) {
							straightActiveLen++;
							if(straightActiveBegin == -1) {
								straightActiveBegin = i;
							}
						}
					}
					if(straightActiveBegin != -1 && straightActiveLen <= target.activeLen) {
						active.activeStartIndex = straightActiveBegin;
						active.activeEndIndex = straightActiveBegin + straightActiveLen - 1;
						changed = true;
					} else {
						// cause the next-turn goes forward exclude left most and right most lane
						if (active.activeStartIndex == 0) {
							active.activeStartIndex++;
							active.activeLen--;
						}
						if (active.activeEndIndex == active.originalLanes.length - 1) {
							active.activeEndIndex--;
							active.activeLen--;
						}
						float ratio = (active.activeLen - target.activeLen) / 2f;
						if (ratio > 0) {
							active.activeEndIndex = (int) Math.ceil(active.activeEndIndex - ratio);
							active.activeStartIndex = (int) Math.floor(active.activeStartIndex + ratio);
						}
						changed = true;
					}
				}
			}
		}
		if (!changed) {
			return false;
		}

		// set the allowed lane bit
		for (int i = 0; i < active.disabledLanes.length; i++) {
			if (i >= active.activeStartIndex && i <= active.activeEndIndex && 
					active.originalLanes[i] % 2 == 1) {
				active.disabledLanes[i] |= 1;
			}
		}
		TurnType currentTurn = currentSegment.getTurnType();
		currentTurn.setLanes(active.disabledLanes);
		return true;
	}
	
	private void inferCommonActiveLane(TurnType currentTurn, TurnType nextTurn) {
		int[] lanes = currentTurn.getLanes();
		TIntHashSet turnSet = new TIntHashSet();
		for (int i = 0; i < lanes.length; i++) {
			if (lanes[i] % 2 == 1) {
				int singleTurn = TurnType.getPrimaryTurn(lanes[i]);
				turnSet.add(singleTurn);
				if (TurnType.getSecondaryTurn(lanes[i]) != 0) {
					turnSet.add(TurnType.getSecondaryTurn(lanes[i]));
				}
				if (TurnType.getTertiaryTurn(lanes[i]) != 0) {
					turnSet.add(TurnType.getTertiaryTurn(lanes[i]));
				}
			}
		}
		int singleTurn = 0;
		if (turnSet.size() == 1) {
			singleTurn = turnSet.iterator().next();
		} else if((currentTurn.goAhead() || currentTurn.keepLeft() || currentTurn.keepRight())  
				&& turnSet.contains(nextTurn.getValue())) {
			int nextTurnLane = nextTurn.getActiveCommonLaneTurn();
			if (currentTurn.isPossibleLeftTurn() && TurnType.isLeftTurn(nextTurn.getValue())) {
				singleTurn = nextTurn.getValue();
			} else if (currentTurn.isPossibleLeftTurn() && TurnType.isLeftTurn(nextTurnLane)) {
				singleTurn = nextTurnLane;
			} else if (currentTurn.isPossibleRightTurn() && TurnType.isRightTurn(nextTurn.getValue())) {
				singleTurn = nextTurn.getValue();
			} else if (currentTurn.isPossibleRightTurn() && TurnType.isRightTurn(nextTurnLane)) {
				singleTurn = nextTurnLane;
			} else if ((currentTurn.goAhead() || currentTurn.keepLeft() || currentTurn.keepRight())
					&& TurnType.isKeepDirectionTurn(nextTurnLane)) {
				singleTurn = nextTurnLane;
			}
		}
		if (singleTurn == 0) {
			singleTurn = currentTurn.getValue();
			if (singleTurn == TurnType.KL || singleTurn == TurnType.KR) {
				return;
			}
		}
		for (int i = 0; i < lanes.length; i++) {
			if (lanes[i] % 2 == 1 && TurnType.getPrimaryTurn(lanes[i]) != singleTurn) {
				if (TurnType.getSecondaryTurn(lanes[i]) == singleTurn) {
					TurnType.setSecondaryTurn(lanes, i, TurnType.getPrimaryTurn(lanes[i]));
					TurnType.setPrimaryTurn(lanes, i, singleTurn);
				} else if (TurnType.getTertiaryTurn(lanes[i]) == singleTurn) {
					TurnType.setTertiaryTurn(lanes, i, TurnType.getPrimaryTurn(lanes[i]));
					TurnType.setPrimaryTurn(lanes, i, singleTurn);
				} else {
					if (lanes.length == 1) {
						return;
					}
					// disable lane
					lanes[i] = lanes[i] - 1;
				}
			}
		}
		
	}

	private static final int MAX_SPEAK_PRIORITY = 5;
	private int highwaySpeakPriority(String highway) {
		if(highway == null || highway.endsWith("track") || highway.endsWith("services") || highway.endsWith("service")
				|| highway.endsWith("path")) {
			return MAX_SPEAK_PRIORITY;
		}
		if (highway.endsWith("_link")  || highway.endsWith("unclassified") || highway.endsWith("road") 
				|| highway.endsWith("living_street") || highway.endsWith("residential"))  {
			return 3;
		}
		if (highway.endsWith("tertiary")) {
			return 2;
		}
		if (highway.endsWith("secondary")) {
			return 1;
		}
		return 0;
	}


	private TurnType getTurnInfo(List<RouteSegmentResult> result, int i, boolean leftSide) {
		if (i == 0) {
			return TurnType.valueOf(TurnType.C, false);
		}
		RoundaboutTurn roundaboutTurn = new RoundaboutTurn(result, i, leftSide);
		if (roundaboutTurn.isRoundaboutExist()) {
			return roundaboutTurn.getRoundaboutType();
		}
		RouteSegmentResult prev = result.get(i - 1) ;
		RouteSegmentResult rr = result.get(i);
		TurnType t = null;
		if (prev != null) {
			// avoid small zigzags is covered at (search for "zigzags")
			float bearingDist = RouteSegmentResult.DIST_BEARING_DETECT;
			if (UNMATCHED_HIGHWAY_TYPE.equals(rr.getObject().getHighway())) {
				bearingDist = RouteSegmentResult.DIST_BEARING_DETECT_UNMATCHED;
			}
			double mpi = MapUtils.degreesDiff(prev.getBearingEnd(prev.getEndPointIndex(), Math.min(prev.getDistance(), bearingDist)), 
					rr.getBearingBegin(rr.getStartPointIndex(), Math.min(rr.getDistance(), bearingDist)));

			String turnTag = getTurnString(rr);
			boolean twiceRoadPresent = twiceRoadPresent(result, i);
			if (turnTag != null) {
				int fromTag = TurnType.convertType(turnTag);
				if (!TurnType.isSlightTurn(fromTag)) {
					t = TurnType.valueOf(fromTag, leftSide);
					int[] lanes = getTurnLanesInfo(prev, rr, t.getValue());
					t = getActiveTurnType(lanes, leftSide, t);
					t.setLanes(lanes);
				} else if (fromTag != TurnType.C) {
					t = attachKeepLeftInfoAndLanes(leftSide, prev, rr, twiceRoadPresent);
					if (t != null) {
						TurnType mainTurnType = TurnType.valueOf(fromTag, leftSide);
						int[] lanes = t.getLanes();
						t = getActiveTurnType(t.getLanes(), leftSide, mainTurnType);
						t.setLanes(lanes);
					}
				}
				if (t != null) {
					t.setTurnAngle((float) - mpi);
					return t;
				}
			}

			if (mpi >= TURN_DEGREE_MIN) {
				if (mpi < TURN_DEGREE_MIN) {
					// Slight turn detection here causes many false positives where drivers would expect a "normal" TL. Best use limit-angle=TURN_DEGREE_MIN, this reduces TSL to the turn-lanes cases.
					t = TurnType.valueOf(TurnType.TSLL, leftSide);
				} else if (mpi < 120) {
					t = TurnType.valueOf(TurnType.TL, leftSide);
				} else if (mpi < 150 || leftSide) {
					t = TurnType.valueOf(TurnType.TSHL, leftSide);
				} else {
					t = TurnType.valueOf(TurnType.TU, leftSide);
				}
				int[] lanes = getTurnLanesInfo(prev, rr, t.getValue());
				t = getActiveTurnType(lanes, leftSide, t);
				t.setLanes(lanes);
			} else if (mpi < -TURN_DEGREE_MIN) {
				if (mpi > -TURN_DEGREE_MIN) {
					t = TurnType.valueOf(TurnType.TSLR, leftSide);
				} else if (mpi > -120) {
					t = TurnType.valueOf(TurnType.TR, leftSide);
				} else if (mpi > -150 || !leftSide) {
					t = TurnType.valueOf(TurnType.TSHR, leftSide);
				} else {
					t = TurnType.valueOf(TurnType.TRU, leftSide);
				}
				int[] lanes = getTurnLanesInfo(prev, rr, t.getValue());
				t = getActiveTurnType(lanes, leftSide, t);
				t.setLanes(lanes);
			} else {
				t = attachKeepLeftInfoAndLanes(leftSide, prev, rr, twiceRoadPresent);
			}
			if (t != null) {
				t.setTurnAngle((float) - mpi);
			}
		}
		return t;
	}

	private int[] getTurnLanesInfo(RouteSegmentResult prevSegm, RouteSegmentResult currentSegm, int mainTurnType) {
		String turnLanes = getTurnLanesString(prevSegm);
		int[] lanesArray;
		if (turnLanes == null) {
			if (prevSegm.getTurnType() != null && prevSegm.getTurnType().getLanes() != null
					&& prevSegm.getDistance() < 60) { // calculate for short segment junctions with missing turn:lanes
				int[] lns = prevSegm.getTurnType().getLanes();
				TIntArrayList lst = new TIntArrayList();
				for (int i = 0; i < lns.length; i++) {
					if (lns[i] % 2 == 1) {
						lst.add((lns[i] >> 1) << 1);
					}
				}
				if (lst.isEmpty()) {
					return null;
				}
				lanesArray = lst.toArray();
			} else {
				return null;
			}
		} else {
			lanesArray = calculateRawTurnLanes(turnLanes, mainTurnType);
		}

		boolean isSet = false;
		int[] act = findActiveIndex(prevSegm, currentSegm, lanesArray, null, turnLanes);
		int startIndex = act[0];
		int endIndex = act[1];
		if (startIndex != -1 && endIndex != -1) {
			if (hasAllowedLanes(mainTurnType, lanesArray, startIndex, endIndex)) {
				for (int k = startIndex; k <= endIndex; k++) {
					int[] oneActiveLane = {lanesArray[k]};
					if (hasAllowedLanes(mainTurnType, oneActiveLane, 0, 0)) {
						lanesArray[k] |= 1;
					}
				}
				isSet = true;
			}
		}
		if (!isSet) {
			// Manually set the allowed lanes.
			isSet = setAllowedLanes(mainTurnType, lanesArray);
		}
		return lanesArray;
	}

	protected boolean setAllowedLanes(int mainTurnType, int[] lanesArray) {
		boolean turnSet = false;
		for (int i = 0; i < lanesArray.length; i++) {
			if (TurnType.getPrimaryTurn(lanesArray[i]) == mainTurnType) {
				lanesArray[i] |= 1;
				turnSet = true;
			}
		}
		return turnSet;
	}

	private TurnType attachKeepLeftInfoAndLanes(boolean leftSide, RouteSegmentResult prevSegm, RouteSegmentResult currentSegm, boolean twiceRoadPresent) {
		List<RouteSegmentResult> attachedRoutes = currentSegm.getAttachedRoutes(currentSegm.getStartPointIndex());
		if(attachedRoutes == null || attachedRoutes.isEmpty()) {
			return null;
		}
		String turnLanesPrevSegm = twiceRoadPresent ? null : getTurnLanesString(prevSegm);
		// keep left/right
		RoadSplitStructure rs = calculateRoadSplitStructure(prevSegm, currentSegm, attachedRoutes, turnLanesPrevSegm);
		if (rs.roadsOnLeft + rs.roadsOnRight == 0) {
			return null;
		}

		// turn lanes exist
		if (turnLanesPrevSegm != null) {
			return createKeepLeftRightTurnBasedOnTurnTypes(rs, prevSegm, currentSegm, turnLanesPrevSegm, leftSide);
		}

		// turn lanes don't exist
		if (rs.keepLeft || rs.keepRight) {
			return createSimpleKeepLeftRightTurn(leftSide, prevSegm, currentSegm, rs);
			
		}
		return null;
	}

	protected TurnType createKeepLeftRightTurnBasedOnTurnTypes(RoadSplitStructure rs, RouteSegmentResult prevSegm,
			RouteSegmentResult currentSegm, String turnLanes, boolean leftSide) {
		// Maybe going straight at a 90-degree intersection
		TurnType t = TurnType.valueOf(TurnType.C, leftSide);
		int[] rawLanes = calculateRawTurnLanes(turnLanes, TurnType.C);
		boolean possiblyLeftTurn = rs.roadsOnLeft == 0;
		boolean possiblyRightTurn = rs.roadsOnRight == 0;
		for (int k = 0; k < rawLanes.length; k++) {
			int turn = TurnType.getPrimaryTurn(rawLanes[k]);
			int sturn = TurnType.getSecondaryTurn(rawLanes[k]);
			int tturn = TurnType.getTertiaryTurn(rawLanes[k]);
			if (turn == TurnType.TU || sturn == TurnType.TU || tturn == TurnType.TU) {
				possiblyLeftTurn = true;
			}
			if (turn == TurnType.TRU || sturn == TurnType.TRU || tturn == TurnType.TRU) {
				possiblyRightTurn = true;
			}
		}

		int[] act = findActiveIndex(prevSegm, currentSegm, rawLanes, rs, turnLanes);
		int activeBeginIndex = act[0];
		int activeEndIndex = act[1];
		int activeTurn = act[2];
		if (activeBeginIndex == -1 || activeEndIndex == -1 || activeBeginIndex > activeEndIndex) {
			// something went wrong
			return createSimpleKeepLeftRightTurn(leftSide, prevSegm, currentSegm, rs);
		}
		boolean leftOrRightKeep = (rs.keepLeft && !rs.keepRight) || (!rs.keepLeft && rs.keepRight);
		if (leftOrRightKeep) {
			setActiveLanesRange(rawLanes, activeBeginIndex, activeEndIndex, activeTurn);
			int tp = inferSlightTurnFromActiveLanes(rawLanes, rs.keepLeft, rs.keepRight);
			// Checking to see that there is only one unique turn
			if (tp != 0) {
				// add extra lanes with same turn
				for(int i = 0; i < rawLanes.length; i++) {
					if (TurnType.getSecondaryTurn(rawLanes[i]) == tp) {
						TurnType.setSecondaryToPrimary(rawLanes, i);
						rawLanes[i] |= 1;
					} else if(TurnType.getPrimaryTurn(rawLanes[i]) == tp) {
						rawLanes[i] |= 1;
					}
				}
			}
			if (tp != t.getValue() && tp != 0) {
				t = TurnType.valueOf(tp, leftSide);
			} else {
				//use keepRight and keepLeft turns when attached road doesn't have lanes
				//or prev segment has more then 1 turn to the active lane
				if (rs.keepRight && !rs.keepLeft) {
					t = getTurnByCurrentTurns(rs.leftLanesInfo, rawLanes, TurnType.KR, leftSide);
				} else if (rs.keepLeft && !rs.keepRight) {
					t = getTurnByCurrentTurns(rs.rightLanesInfo, rawLanes, TurnType.KL, leftSide);
				}
			}
		} else {
			setActiveLanesRange(rawLanes, activeBeginIndex, activeEndIndex, activeTurn);
			t = getActiveTurnType(rawLanes, leftSide, t);
		}
		t.setLanes(rawLanes);
		t.setPossibleLeftTurn(possiblyLeftTurn);
		t.setPossibleRightTurn(possiblyRightTurn);
		return t;
	}

	private void setActiveLanesRange(int[] rawLanes, int activeBeginIndex, int activeEndIndex, int activeTurn) {
		Set<Integer> possibleTurns = new TreeSet<>();
		Set<Integer> upossibleTurns = new TreeSet<>();
		for (int k = activeBeginIndex; k < rawLanes.length && k <= activeEndIndex; k++) {
			rawLanes[k] |= 1;
			upossibleTurns.clear();
			upossibleTurns.add(TurnType.getPrimaryTurn(rawLanes[k]));
			if (TurnType.getSecondaryTurn(rawLanes[k]) != 0) {
				upossibleTurns.add(TurnType.getSecondaryTurn(rawLanes[k]));
			}
			if (TurnType.getTertiaryTurn(rawLanes[k]) != 0) {
				upossibleTurns.add(TurnType.getTertiaryTurn(rawLanes[k]));
			}
			if (k == activeBeginIndex) {
				possibleTurns.addAll(upossibleTurns);
			} else {
				possibleTurns.retainAll(upossibleTurns);
			}
		}
		for (int k = activeBeginIndex; k < rawLanes.length && k <= activeEndIndex; k++) {
			if(TurnType.getPrimaryTurn(rawLanes[k]) != activeTurn) {
				if(TurnType.getSecondaryTurn(rawLanes[k]) == activeTurn) {
					TurnType.setSecondaryToPrimary(rawLanes, k);
				} else if(TurnType.getTertiaryTurn(rawLanes[k]) == activeTurn) {
					TurnType.setTertiaryToPrimary(rawLanes, k);
				}
			}
		}
		
	}

	private TurnType getTurnByCurrentTurns(List<AttachedRoadInfo> otherSideLanesInfo, int[] rawLanes, int keepTurnType, boolean leftSide) {
		LinkedHashSet<Integer> otherSideTurns = new LinkedHashSet<>();
		if (otherSideLanesInfo != null) {
			for (AttachedRoadInfo li : otherSideLanesInfo) {
				if (li.parsedLanes != null) {
					for (int i : li.parsedLanes) {
						TurnType.collectTurnTypes(i, otherSideTurns);
					}
				}
			}
		}
		LinkedHashSet<Integer> currentTurns = new LinkedHashSet<>();
		for (int ln : rawLanes) {
			TurnType.collectTurnTypes(ln, currentTurns);
		}
		LinkedList<Integer> analyzedList = new LinkedList<>(currentTurns);
		if (analyzedList.size() > 1) {
			if (keepTurnType == TurnType.KL) {
				// no need analyze turns in left side (current direction)
				analyzedList.remove(0);
			} else if (keepTurnType == TurnType.KR) {
				// no need analyze turns in right side (current direction)
				analyzedList.remove(analyzedList.size() - 1);
			}
			// Here we detect single case when turn lane continues on 1 road / single sign and all other lane turns continue on the other side roads
			if (analyzedList.containsAll(otherSideTurns)) {
				currentTurns.removeAll(otherSideTurns);
				if (currentTurns.size() == 1) {
					return TurnType.valueOf(currentTurns.iterator().next(), leftSide);
				}
			} else {
				// Avoid "keep" instruction if active side contains only "through" moving
				analyzedList = new LinkedList<>(currentTurns);
				if ((keepTurnType == TurnType.KL && analyzedList.get(0) == TurnType.C)) {
					return TurnType.valueOf(TurnType.C, leftSide);
				}
				if (keepTurnType == TurnType.KR && analyzedList.get(analyzedList.size() - 1) == TurnType.C) {
					return TurnType.valueOf(TurnType.C, leftSide);
				}
			}
		}

		
		return TurnType.valueOf(keepTurnType, leftSide);
	}

	protected RoadSplitStructure calculateRoadSplitStructure(RouteSegmentResult prevSegm, RouteSegmentResult currentSegm,
			List<RouteSegmentResult> attachedRoutes, String turnLanesPrevSegm) {
		RoadSplitStructure rs = new RoadSplitStructure();
		int speakPriority = Math.max(highwaySpeakPriority(prevSegm.getObject().getHighway()), highwaySpeakPriority(currentSegm.getObject().getHighway()));
		double currentAngle = MapUtils.normalizeDegrees360(currentSegm.getBearingBegin());
		double prevAngle = MapUtils.normalizeDegrees360(prevSegm.getBearingBegin() - 180);
		boolean hasSharpOrReverseLane = hasSharpOrReverseTurnLane(turnLanesPrevSegm);
		boolean hasSameTurnLanes = hasSameTurnLanes(prevSegm, currentSegm);
		for (RouteSegmentResult attached : attachedRoutes) {
			boolean restricted = false;
			for (int k = 0; k < prevSegm.getObject().getRestrictionLength(); k++) {
				if (prevSegm.getObject().getRestrictionId(k) == attached.getObject().getId() &&
						prevSegm.getObject().getRestrictionType(k) <= MapRenderingTypes.RESTRICTION_NO_STRAIGHT_ON) {
					restricted = true;
					break;
				}
			}
			if (restricted) {
				continue;
			}
			double ex = MapUtils.degreesDiff(attached.getBearingBegin(), currentSegm.getBearingBegin());
			double deviation = MapUtils.degreesDiff(prevSegm.getBearingEnd(), attached.getBearingBegin());
			double mpi = Math.abs(deviation);
			int lanes = countLanesMinOne(attached);
			boolean smallStraightVariation = mpi < TURN_DEGREE_MIN;
			boolean smallTargetVariation = Math.abs(ex) < TURN_DEGREE_MIN;
			boolean verySharpTurn = Math.abs(ex) > 150;
			AttachedRoadInfo ai = new AttachedRoadInfo();
			ai.speakPriority = highwaySpeakPriority(attached.getObject().getHighway()); 
			ai.attachedOnTheRight = ex >= 0;
			ai.attachedAngle = deviation;
			ai.parsedLanes = parseTurnLanes(attached.getObject(), attached.getBearingBegin() * Math.PI / 180);
			ai.lanes = lanes;

			if (!verySharpTurn || hasSharpOrReverseLane) {
				double attachedAngle = MapUtils.normalizeDegrees360(attached.getBearingBegin());
				boolean rightSide;
				if (prevAngle > currentAngle) {
					// left side angle range contains 0 degree transition
					rightSide = attachedAngle > currentAngle && attachedAngle < prevAngle;
				} else {
					// right side angle range contains 0 degree transition
					boolean leftSide = attachedAngle > prevAngle && attachedAngle < currentAngle;
					rightSide = !leftSide;
				}

				//check if need ignore right or left attached road
				if (hasSameTurnLanes && !smallTargetVariation && !smallStraightVariation) {
					if (rightSide && !hasTurn(turnLanesPrevSegm, TurnType.TR)) {
						//restricted
						continue;
					} else if (!hasTurn(turnLanesPrevSegm, TurnType.TL)) {
						//restricted
						continue;
					}
				}

				if (rightSide) {
					rs.roadsOnRight++;
				} else {
					rs.roadsOnLeft++;
				}
			}

			if (turnLanesPrevSegm != null || ai.speakPriority != MAX_SPEAK_PRIORITY || speakPriority == MAX_SPEAK_PRIORITY) {
				if (smallTargetVariation || smallStraightVariation) {
					if (ai.attachedOnTheRight) {
						rs.keepLeft = true;
						rs.rightLanes += lanes;
						rs.rightMaxPrio = Math.max(rs.rightMaxPrio, highwaySpeakPriority(attached.getObject().getHighway()));
						rs.rightLanesInfo.add(ai);
					} else {
						rs.keepRight = true;
						rs.leftLanes += lanes;
						rs.leftMaxPrio = Math.max(rs.leftMaxPrio, highwaySpeakPriority(attached.getObject().getHighway()));
						rs.leftLanesInfo.add(ai);
					}
					rs.speak = rs.speak || ai.speakPriority <= speakPriority;
				}
			}
			
		}
		return rs;
	}
	
	protected TurnType createSimpleKeepLeftRightTurn(boolean leftSide, RouteSegmentResult prevSegm,
	                                                 RouteSegmentResult currentSegm, RoadSplitStructure rs) {
		double deviation = MapUtils.degreesDiff(prevSegm.getBearingEnd(), currentSegm.getBearingBegin());
		boolean makeSlightTurn = Math.abs(deviation) > TURN_SLIGHT_DEGREE;

		TurnType t = null;
		int mainLaneType = TurnType.C;

		if (rs.keepLeft || rs.keepRight) {
			if (deviation < -TURN_SLIGHT_DEGREE && makeSlightTurn) {
				t = TurnType.valueOf(TurnType.TSLR, leftSide);
				mainLaneType = TurnType.TSLR;
			} else if (deviation > TURN_SLIGHT_DEGREE && makeSlightTurn) {
				t = TurnType.valueOf(TurnType.TSLL, leftSide);
				mainLaneType = TurnType.TSLL;
			} else if (rs.keepLeft && rs.keepRight) {
				t = TurnType.valueOf(TurnType.C, leftSide);
			} else {
				t = TurnType.valueOf(rs.keepLeft ? TurnType.KL : TurnType.KR, leftSide);
			}
		} else {
			return null;
		}

		int currentLanesCount = countLanesMinOne(currentSegm);
		int prevLanesCount = countLanesMinOne(prevSegm);
		boolean oneLane = currentLanesCount == 1 && prevLanesCount == 1;
		int[] lanes;

		if (oneLane) {
			lanes = createCombinedTurnTypeForSingleLane(rs, deviation);
			t.setLanes(lanes);
			int active = t.getActiveCommonLaneTurn();
			if (active > 0 && (!TurnType.isKeepDirectionTurn(active) || !TurnType.isKeepDirectionTurn(t.getValue()))) {
				t = TurnType.valueOf(active, leftSide);
			}
		} else {
			lanes = new int[prevLanesCount];
			boolean ltr = rs.leftLanes < rs.rightLanes;
			List<AttachedRoadInfo> roads = new ArrayList<>();
			AttachedRoadInfo mainType = new AttachedRoadInfo();
			mainType.lanes = currentLanesCount;
			mainType.speakPriority = highwaySpeakPriority(currentSegm.getObject().getHighway());
			mainType.turnType = mainLaneType;
			roads.add(mainType);
			
			synteticAssignTurnTypes(rs, mainLaneType, roads, true);
			synteticAssignTurnTypes(rs, mainLaneType, roads, false);
			// sort important last
			Collections.sort(roads, new Comparator<AttachedRoadInfo>() {
				@Override
				public int compare(AttachedRoadInfo o1, AttachedRoadInfo o2) {
					if (o1.speakPriority == o2.speakPriority) {
						return -Integer.compare(o1.lanes, o2.lanes);
					}
					return -Integer.compare(o1.speakPriority, o2.speakPriority);
				}
			});
			for (AttachedRoadInfo i : roads) {
				int sumLanes = 0;
				for (AttachedRoadInfo l : roads) {
					sumLanes += l.lanes;
				}
				if (sumLanes < 2 * lanes.length) {
					// max 2 attached per lane is enough
					break;
				}
				i.lanes = 1; // if not enough reset to 1 lane
			}
								
			// active lanes
			int startActive = Math.max(0, ltr ? 0 : lanes.length - mainType.lanes);
			int endActive = Math.min(lanes.length, startActive + mainType.lanes) - 1;
			for (int i = startActive; i <= endActive; i++) {
				lanes[i] = (mainType.turnType << 1) + 1;
			}
			int ind = 0;
			for (AttachedRoadInfo i : rs.leftLanesInfo) {
				for (int k = 0; k < i.lanes && ind <= startActive; k++, ind++) {
					if (lanes[ind] == 0) {
						lanes[ind] = i.turnType << 1;
					} else if (TurnType.getSecondaryTurn(lanes[ind]) == 0) {
						TurnType.setSecondaryTurn(lanes, ind, i.turnType);
					} else {
						TurnType.setTertiaryTurn(lanes, ind, i.turnType);
					}
				}
			}
			ind = lanes.length - 1;
			for (AttachedRoadInfo i : rs.rightLanesInfo) {
				for (int k = 0; k < i.lanes && ind >= endActive; k++, ind--) {
					if (lanes[ind] == 0) {
						lanes[ind] = i.turnType << 1;
					} else if (TurnType.getSecondaryTurn(lanes[ind]) == 0) {
						TurnType.setSecondaryTurn(lanes, ind, i.turnType);
					} else {
						TurnType.setTertiaryTurn(lanes, ind, i.turnType);
					}
				}
			}
			// Fill All left empty slots with inactive C
			for (int i = 0; i < lanes.length; i++) {
				if (lanes[i] == 0) {
					lanes[i] = TurnType.C << 1;
				}
			}

		}

		// Set properties for the TurnType object
		t.setSkipToSpeak(!rs.speak);
		t.setLanes(lanes);
		return t;
	}

	private void synteticAssignTurnTypes(RoadSplitStructure rs, int mainLaneType, List<AttachedRoadInfo> roads, boolean left) {
		Comparator<AttachedRoadInfo> comparatorByAngle = new Comparator<AttachedRoadInfo>() {
			@Override
			public int compare(AttachedRoadInfo o1, AttachedRoadInfo o2) {
				return (left? 1 : -1) * Double.compare(o1.attachedAngle, o2.attachedAngle);
			}
		};
		List<AttachedRoadInfo> col = left ? rs.leftLanesInfo : rs.rightLanesInfo;
		Collections.sort(col, comparatorByAngle);
		int type = mainLaneType;
		for (int i = col.size() - 1; i >= 0; i--) {
			AttachedRoadInfo info = col.get(i);
			int turnByAngle = getTurnByAngle(info.attachedAngle);
			if (left && turnByAngle >= type) {
				type = TurnType.getPrev(type);
			} else if (!left && turnByAngle <= type) {
				type = TurnType.getNext(type);
			} else {
				type = turnByAngle;
			}
			info.turnType = type;
			roads.add(info);
		}
	}


	private int[] createCombinedTurnTypeForSingleLane(RoadSplitStructure rs, double currentDeviation) {
		List<Double> attachedAngles = new ArrayList<>();
		attachedAngles.add(currentDeviation);
		for (AttachedRoadInfo l : rs.leftLanesInfo) {
			attachedAngles.add(l.attachedAngle);
		}
		for (AttachedRoadInfo l : rs.rightLanesInfo) {
			attachedAngles.add(l.attachedAngle);
		}
		Collections.sort(attachedAngles, new Comparator<Double>() {
			@Override
			public int compare(Double c1, Double c2) {
				return Double.compare(c2, c1);
			}
		});

		int size = attachedAngles.size();
		boolean allStraight = rs.allAreStraight();
		int[] lanes = new int[1];
		int extraLanes = 0;
		double prevAngle = Double.NaN;
		// iterate from left to right turns
		int prevTurn = 0;
		for (int i = 0; i < size; i++) {
			double angle = attachedAngles.get(i);
			if (!Double.isNaN(prevAngle) && angle == prevAngle) {
				continue;
			}
			prevAngle = angle;
			int turn;
			if (allStraight) {
				// create fork intersection
				if (i == 0) {
					turn = TurnType.KL;
				} else if (i == size - 1) {
					turn = TurnType.KR;
				} else {
					turn = TurnType.C;
				}
			} else {
				turn = getTurnByAngle(angle);
				if (prevTurn > 0 && prevTurn == turn) {
					turn = TurnType.getNext(turn);
				}
			}
			prevTurn = turn;
			if (angle == currentDeviation) {
				TurnType.setPrimaryTurn(lanes, 0, turn);
			} else {
				if (extraLanes++ == 0) {
					TurnType.setSecondaryTurn(lanes, 0, turn);
				} else {
					TurnType.setTertiaryTurn(lanes, 0, turn);
					// if (extraLanes > 2): we don't have enough space to display
				}
			}
		}
		lanes[0] |= 1;
		return lanes;
	}

	private int getTurnByAngle(double angle) {
		int turnType = TurnType.C;
		if (angle < -150) {
			turnType = TurnType.TRU;
		} else if (angle < -120) {
			turnType = TurnType.TSHR;
		} else if (angle < -TURN_DEGREE_MIN) {
			turnType = TurnType.TR;
		} else if (angle <= -TURN_SLIGHT_DEGREE) {
			turnType = TurnType.TSLR;
		} else if (angle > -TURN_SLIGHT_DEGREE && angle < TURN_SLIGHT_DEGREE) {
			turnType = TurnType.C;
		} else if (angle < TURN_DEGREE_MIN) {
			turnType = TurnType.TSLL;
		} else if (angle < 120) {
			turnType = TurnType.TL;
		} else if (angle < 150) {
			turnType = TurnType.TSHL;
		} else {
			turnType = TurnType.TU;
		}
		return turnType;
	}
	
	protected int countLanesMinOne(RouteSegmentResult attached) {
		final boolean oneway = attached.getObject().getOneway() != 0;
		int lns = attached.getObject().getLanes();
		if (lns == 0) {
			String tls = getTurnLanesString(attached);
			if(tls != null) {
				return Math.max(1, countOccurrences(tls, '|'));
			}
		}
		if (oneway) {
			return Math.max(1, lns);
		}
		try {
			if (attached.isForwardDirection() && attached.getObject().getValue("lanes:forward") != null) {
				return Integer.parseInt(attached.getObject().getValue("lanes:forward"));
			} else if (!attached.isForwardDirection() && attached.getObject().getValue("lanes:backward") != null) {
				return Integer.parseInt(attached.getObject().getValue("lanes:backward"));
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return Math.max(1, (lns + 1) / 2);
	}

	protected static String getTurnLanesString(RouteSegmentResult segment) {
		if (segment.getObject().getOneway() == 0) {
			if (segment.isForwardDirection()) {
				return segment.getObject().getValue("turn:lanes:forward");
			} else {
				return segment.getObject().getValue("turn:lanes:backward");
			}
		} else {
			return segment.getObject().getValue("turn:lanes");
		}
	}

	private String getTurnString(RouteSegmentResult segment) {
		return segment.getObject().getValue("turn");
	}

	

	private int countOccurrences(String haystack, char needle) {
	    int count = 0;
		for (int i = 0; i < haystack.length(); i++) {
			if (haystack.charAt(i) == needle) {
				count++;
			}
		}
		return count;
	}

	public static int[] parseTurnLanes(RouteDataObject ro, double dirToNorthEastPi) {
		String turnLanes = null;
		if (ro.getOneway() == 0) {
			// we should get direction to detect forward or backward
			double cmp = ro.directionRoute(0, true);
			if(Math.abs(MapUtils.alignAngleDifference(dirToNorthEastPi -cmp)) < Math.PI / 2) {
				turnLanes = ro.getValue("turn:lanes:forward");
			} else {
				turnLanes = ro.getValue("turn:lanes:backward");
			}
		} else {
			turnLanes = ro.getValue("turn:lanes");
		}
		if(turnLanes == null) {
			return null;
		}
		return calculateRawTurnLanes(turnLanes, 0);
	}
	
	public static int[] parseLanes(RouteDataObject ro, double dirToNorthEastPi) {
		int lns = 0;
		try {
			if (ro.getOneway() == 0) {
				// we should get direction to detect forward or backward
				double cmp = ro.directionRoute(0, true);
				
				if(Math.abs(MapUtils.alignAngleDifference(dirToNorthEastPi -cmp)) < Math.PI / 2) {
					if(ro.getValue("lanes:forward") != null) {
						lns = Integer.parseInt(ro.getValue("lanes:forward"));
					}
				} else {
					if(ro.getValue("lanes:backward") != null) {
					lns = Integer.parseInt(ro.getValue("lanes:backward"));
					}
				}
				if (lns == 0 && ro.getValue("lanes") != null) {
					lns = Integer.parseInt(ro.getValue("lanes")) / 2;
				}
			} else {
				lns = Integer.parseInt(ro.getValue("lanes"));
			}
			if(lns > 0 ) {
				return new int[lns];
			}
		} catch (NumberFormatException e) {
		}
		return null;
	}
	
	public static int[] calculateRawTurnLanes(String turnLanes, int calcTurnType) {
		String[] splitLaneOptions = turnLanes.split("\\|", -1);
		int[] lanes = new int[splitLaneOptions.length];
		for (int i = 0; i < splitLaneOptions.length; i++) {
			String[] laneOptions = splitLaneOptions[i].split(";");
			for (int j = 0; j < laneOptions.length; j++) {
				int turn = TurnType.convertType(laneOptions[j]);
				final int primary = TurnType.getPrimaryTurn(lanes[i]);
				if (primary == 0) {
					TurnType.setPrimaryTurnAndReset(lanes, i, turn);
				} else {
                    if (turn == calcTurnType || 
                    	(TurnType.isRightTurn(calcTurnType) && TurnType.isRightTurn(turn)) || 
                    	(TurnType.isLeftTurn(calcTurnType) && TurnType.isLeftTurn(turn)) 
                    	) {
                    	TurnType.setPrimaryTurnShiftOthers(lanes, i, turn);
                    } else if (TurnType.getSecondaryTurn(lanes[i]) == 0) {
                    	TurnType.setSecondaryTurn(lanes, i, turn);
                    } else if (TurnType.getTertiaryTurn(lanes[i]) == 0) {
						TurnType.setTertiaryTurn(lanes, i, turn);
                    } else {
                    	// ignore
                    }
				}
			}
		}
		return lanes;
	}

	
	private int inferSlightTurnFromActiveLanes(int[] oLanes, boolean mostLeft, boolean mostRight) {
		Integer[] possibleTurns = getPossibleTurns(oLanes, false, false);
		if (possibleTurns.length == 0) {
			// No common turns, so can't determine anything.
			return 0;
		}
		int infer = 0;
		if (possibleTurns.length == 1) {
			infer = possibleTurns[0];
		} else if (possibleTurns.length == 2) {
			// this method could be adapted for 3+ turns 
			if (mostLeft && !mostRight) {
				infer = possibleTurns[0];
			} else if (mostRight && !mostLeft) {
				infer = possibleTurns[possibleTurns.length - 1];
			} else {
				infer = possibleTurns[1];
				// infer = TurnType.C;
			}
		}
		return infer;
	}
	
	private Integer[] getPossibleTurns(int[] oLanes, boolean onlyPrimary, boolean uniqueFromActive) {
		Set<Integer> possibleTurns = new LinkedHashSet<>();
		Set<Integer> upossibleTurns = new LinkedHashSet<>();
		for (int i = 0; i < oLanes.length; i++) {
			// Nothing is in the list to compare to, so add the first elements
			upossibleTurns.clear();
			upossibleTurns.add(TurnType.getPrimaryTurn(oLanes[i]));
			if (!onlyPrimary && TurnType.getSecondaryTurn(oLanes[i]) != 0) {
				upossibleTurns.add(TurnType.getSecondaryTurn(oLanes[i]));
			}
			if (!onlyPrimary && TurnType.getTertiaryTurn(oLanes[i]) != 0) {
				upossibleTurns.add(TurnType.getTertiaryTurn(oLanes[i]));
			}
			if (!uniqueFromActive) {
				possibleTurns.addAll(upossibleTurns);
//				if (!possibleTurns.isEmpty()) {
//					possibleTurns.retainAll(upossibleTurns);
//					if(possibleTurns.isEmpty()) {
//						break;
//					}
//				} else {
//					possibleTurns.addAll(upossibleTurns);
//				}
			} else if ((oLanes[i] & 1) == 1) {
				if (!possibleTurns.isEmpty()) {
					possibleTurns.retainAll(upossibleTurns);
					if(possibleTurns.isEmpty()) {
						break;
					}
				} else {
					possibleTurns.addAll(upossibleTurns);
				}
			}
		}
		// Remove all turns from lanes not selected...because those aren't it
		if (uniqueFromActive) {
			for (int i = 0; i < oLanes.length; i++) {
				if ((oLanes[i] & 1) == 0) {
					possibleTurns.remove((Integer) TurnType.getPrimaryTurn(oLanes[i]));
					if (TurnType.getSecondaryTurn(oLanes[i]) != 0) {
						possibleTurns.remove((Integer) TurnType.getSecondaryTurn(oLanes[i]));
					}
					if (TurnType.getTertiaryTurn(oLanes[i]) != 0) {
						possibleTurns.remove((Integer) TurnType.getTertiaryTurn(oLanes[i]));
					}
				}
			}
		}
		Integer[] array = possibleTurns.toArray(new Integer[0]);
		Arrays.sort(array, new Comparator<Integer>() {

			@Override
			public int compare(Integer o1, Integer o2) {
				return Integer.compare(TurnType.orderFromLeftToRight(o1), TurnType.orderFromLeftToRight(o2));
			}
		});
		return array;
	}

	private boolean isMotorway(RouteSegmentResult s){
		String h = s.getObject().getHighway();
		return "motorway".equals(h) || "motorway_link".equals(h)  ||
				"trunk".equals(h) || "trunk_link".equals(h);
		
	}

	
	private void attachRoadSegments(RoutingContext ctx, List<RouteSegmentResult> result, int routeInd, int pointInd, boolean plus) throws IOException {
		RouteSegmentResult rr = result.get(routeInd);
		RouteDataObject road = rr.getObject();
		long nextL = pointInd < road.getPointsLength() - 1 ? getPoint(road, pointInd + 1) : 0;
		long prevL = pointInd > 0 ? getPoint(road, pointInd - 1) : 0;
		
		// attach additional roads to represent more information about the route
		RouteSegmentResult previousResult = null;
		
		// by default make same as this road id
		long previousRoadId = road.getId();
		if (pointInd == rr.getStartPointIndex() && routeInd > 0) {
			previousResult = result.get(routeInd - 1);
			previousRoadId = previousResult.getObject().getId();
			if (previousRoadId != road.getId()) {
				if (previousResult.getStartPointIndex() < previousResult.getEndPointIndex()
						&& previousResult.getEndPointIndex() < previousResult.getObject().getPointsLength() - 1) {
					rr.attachRoute(pointInd, new RouteSegmentResult(previousResult.getObject(), previousResult.getEndPointIndex(),
							previousResult.getObject().getPointsLength() - 1));
				} else if (previousResult.getStartPointIndex() > previousResult.getEndPointIndex() 
						&& previousResult.getEndPointIndex() > 0) {
					rr.attachRoute(pointInd, new RouteSegmentResult(previousResult.getObject(), previousResult.getEndPointIndex(), 0));
				}
			}
		}
		Iterator<RouteSegment> it;
		if (rr.getPreAttachedRoutes(pointInd) != null) {
			final RouteSegmentResult[] list = rr.getPreAttachedRoutes(pointInd);
			it = new Iterator<BinaryRoutePlanner.RouteSegment>() {
				int i = 0;
				@Override
				public boolean hasNext() {
					return i < list.length;
				}

				@Override
				public RouteSegment next() {
					RouteSegmentResult r = list[i++];
					return new RouteSegment(r.getObject(), r.getStartPointIndex(), r.getEndPointIndex());
				}

				@Override
				public void remove() {
				}
			};	
		} else if (ctx.nativeLib == null) {
			RouteSegment rt = ctx.loadRouteSegment(road.getPoint31XTile(pointInd), road.getPoint31YTile(pointInd), ctx.config.memoryLimitation);
			it = rt == null ? null : rt.getIterator();
		} else {
			// Here we assume that all segments should be attached by native
			it = null;
		}
		// try to attach all segments except with current id
		while (it != null && it.hasNext()) {
			RouteSegment routeSegment = it.next();
			if (routeSegment.road.getId() != road.getId() && routeSegment.road.getId() != previousRoadId) {
				RouteDataObject addRoad = routeSegment.road;
				checkAndInitRouteRegion(ctx, addRoad);
				// Future: restrictions can be considered as well
				int oneWay = ctx.getRouter().isOneWay(addRoad);
				if (oneWay >= 0 && routeSegment.getSegmentStart() < addRoad.getPointsLength() - 1) {
					long pointL = getPoint(addRoad, routeSegment.getSegmentStart() + 1);
					if(pointL != nextL && pointL != prevL) {
						// if way contains same segment (nodes) as different way (do not attach it)
						rr.attachRoute(pointInd, new RouteSegmentResult(addRoad, routeSegment.getSegmentStart(), addRoad.getPointsLength() - 1));
					}
				}
				if (oneWay <= 0 && routeSegment.getSegmentStart() > 0) {
					long pointL = getPoint(addRoad, routeSegment.getSegmentStart() - 1);
					// if way contains same segment (nodes) as different way (do not attach it)
					if(pointL != nextL && pointL != prevL) {
						rr.attachRoute(pointInd, new RouteSegmentResult(addRoad, routeSegment.getSegmentStart(), 0));
					}
				}
			}
		}
	}
	
	private static void println(String logMsg) {
//		log.info(logMsg);
		System.out.println(logMsg);
	}
	
	private long getPoint(RouteDataObject road, int pointInd) {
		return (((long) road.getPoint31XTile(pointInd)) << 31) + (long) road.getPoint31YTile(pointInd);
	}
	
	private static double measuredDist(int x1, int y1, int x2, int y2) {
		return MapUtils.getDistance(MapUtils.get31LatitudeY(y1), MapUtils.get31LongitudeX(x1), 
				MapUtils.get31LatitudeY(y2), MapUtils.get31LongitudeX(x2));
	}

	private void avoidKeepForThroughMoving(List<RouteSegmentResult> result) {
		for (int i = 1; i < result.size(); i++) {
			RouteSegmentResult curr = result.get(i);
			TurnType turnType = curr.getTurnType();
			if (turnType == null) {
				continue;
			}
			if (!turnType.keepLeft() && !turnType.keepRight()) {
				continue;
			}
			if (isSwitchToLink(curr, result.get(i - 1))) {
				continue;
			}
			if (isKeepTurn(turnType) && isHighSpeakPriority(curr)) {
				continue;
			}
			if (isForkByLanes(curr, result.get(i - 1))) {
				continue;
			}
			int cnt = turnType.countTurnTypeDirections(TurnType.C, true);
			int cntAll = turnType.countTurnTypeDirections(TurnType.C, false);
			if(cnt > 0 && cnt == cntAll) {
				TurnType newTurnType = new TurnType(TurnType.C, turnType.getExitOut(), turnType.getTurnAngle(),
						turnType.isSkipToSpeak(), turnType.getLanes(),
						turnType.isPossibleLeftTurn(), turnType.isPossibleRightTurn());
				curr.setTurnType(newTurnType);
			}
		}
	}
	
	private void muteAndRemoveTurns(List<RouteSegmentResult> result, RoutingContext ctx) {
		for (int i = 0; i < result.size(); i++) {
			RouteSegmentResult curr = result.get(i);
			TurnType turnType = curr.getTurnType();
			if (turnType == null || turnType.getLanes() == null) {
				continue;
			}
			int active = turnType.getActiveCommonLaneTurn();
			if (TurnType.isKeepDirectionTurn(active)) {
				if (i > 0 && isSwitchToLink(curr, result.get(i - 1))) {
					continue;
				}
				if (isKeepTurn(turnType) && isHighSpeakPriority(curr)) {
					continue;
				}
				turnType.setSkipToSpeak(true);
				if (ctx.config.showMinorTurns) {
					continue;
				}
				if (turnType.goAhead()) {
					int uniqDirections = turnType.countDirections();
					if (uniqDirections >= 3) {
						continue;
					}
					int cnt = turnType.countTurnTypeDirections(TurnType.C, true);
					int cntAll = turnType.countTurnTypeDirections(TurnType.C, false);
					int lanesCnt = turnType.getLanes().length;
					if (cnt == cntAll && cnt >= 2 && (lanesCnt - cnt) <= 1) {
						curr.setTurnType(null);
					}
				}
			}
		}
	}

	private int[] getUniqTurnTypes(String turnLanes) {
		LinkedHashSet<Integer> turnTypes = new LinkedHashSet<>();
		String[] splitLaneOptions = turnLanes.split("\\|", -1);
		for (int i = 0; i < splitLaneOptions.length; i++) {
			String[] laneOptions = splitLaneOptions[i].split(";");
			for (int j = 0; j < laneOptions.length; j++) {
				int turn = TurnType.convertType(laneOptions[j]);
				turnTypes.add(turn);
			}
		}
		Iterator<Integer> it = turnTypes.iterator();
		int[] r = new int[turnTypes.size()];
		int i = 0;
		while (it.hasNext()) {
			r[i++] = it.next();
		}
		return r;
	}

	private int[] findActiveIndex(RouteSegmentResult prevSegm, RouteSegmentResult currentSegm, int[] rawLanes, RoadSplitStructure rs, String turnLanes) {
		int[] pair = {-1, -1, 0};
		if (turnLanes == null) {
			return pair;
		}
		if (rs == null) {
			List<RouteSegmentResult> attachedRoutes = currentSegm.getAttachedRoutes(currentSegm.getStartPointIndex());
			if(!Algorithms.isEmpty(attachedRoutes)) {
				rs = calculateRoadSplitStructure(prevSegm, currentSegm, attachedRoutes, turnLanes);
			}
		}
		if (rs == null) {
			return pair;
		}
		int[] directions = getUniqTurnTypes(turnLanes);
		if (rs.roadsOnLeft + rs.roadsOnRight < directions.length) {
			int startDirection = directions[rs.roadsOnLeft];
			int endDirection = directions[directions.length - rs.roadsOnRight - 1];
			for (int i = 0; i < rawLanes.length; i++) {
				int p = TurnType.getPrimaryTurn(rawLanes[i]);
				int s = TurnType.getSecondaryTurn(rawLanes[i]);
				int t = TurnType.getTertiaryTurn(rawLanes[i]);
				if (p == startDirection || s == startDirection || t == startDirection) {
					pair[0] = i;
					pair[2] = startDirection;
					break;
				}
			}
			for (int i = rawLanes.length - 1; i >= 0; i--) {
				int p = TurnType.getPrimaryTurn(rawLanes[i]);
				int s = TurnType.getSecondaryTurn(rawLanes[i]);
				int t = TurnType.getTertiaryTurn(rawLanes[i]);
				if (p == endDirection || s == endDirection || t == endDirection) {
					pair[1] = i;
					break;
				}
			}
		}
		return pair;
	}

	private boolean hasTurn(String turnLanes, int turnType) {
		if (turnLanes == null) {
			return false;
		}
		int[] uniqTurnTypes = getUniqTurnTypes(turnLanes);
		for (int lane : uniqTurnTypes) {
			if (lane == turnType) {
				return true;
			}
		}
		return false;
	}

	private boolean hasSharpOrReverseTurnLane(String turnLanesPrevSegm) {
		if (turnLanesPrevSegm == null) {
			return false;
		}
		int[] uniqTurnTypes = getUniqTurnTypes(turnLanesPrevSegm);
		for (int lane : uniqTurnTypes) {
			if (TurnType.isSharpOrReverse(lane)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasSameTurnLanes(RouteSegmentResult prevSegm, RouteSegmentResult currentSegm) {
		String turnLanesPrevSegm = getTurnLanesString(prevSegm);
		String turnLanesCurrSegm = getTurnLanesString(currentSegm);
		if (turnLanesPrevSegm == null || turnLanesCurrSegm == null) {
			return false;
		}
		int[] uniqPrev = getUniqTurnTypes(turnLanesPrevSegm);
		int[] uniqCurr = getUniqTurnTypes(turnLanesCurrSegm);
		if (uniqPrev.length != uniqCurr.length) {
			return false;
		}
		for (int i = 0; i < uniqCurr.length; i++) {
			if (uniqPrev[i] != uniqCurr[i]) {
				return false;
			}
		}
		return true;
	}

	private boolean hasAllowedLanes(int mainTurnType, int[] lanesArray, int startActiveIndex, int endActiveIndex) {
		if (lanesArray.length == 0 || startActiveIndex > endActiveIndex) {
			return false;
		}
		int[] activeLines = new int[endActiveIndex - startActiveIndex + 1];
		for (int i = startActiveIndex, j = 0; i <= endActiveIndex; i++, j++) {
			activeLines[j] = lanesArray[i];
		}
		boolean possibleSharpLeftOrUTurn = startActiveIndex == 0;
		boolean possibleSharpRightOrUTurn = endActiveIndex == lanesArray.length - 1;
		for (int i = 0; i < activeLines.length; i++) {
			int turnType = TurnType.getPrimaryTurn(activeLines[i]);
			if (turnType == mainTurnType) {
				return true;
			}
			if (TurnType.isLeftTurnNoUTurn(mainTurnType) && TurnType.isLeftTurnNoUTurn(turnType)) {
				return true;
			}
			if (TurnType.isRightTurnNoUTurn(mainTurnType) && TurnType.isRightTurnNoUTurn(turnType)) {
				return true;
			}
			if (mainTurnType == TurnType.C && TurnType.isSlightTurn(turnType)) {
				return true;
			}
			if (possibleSharpLeftOrUTurn && TurnType.isSharpLeftOrUTurn(mainTurnType) && TurnType.isSharpLeftOrUTurn(turnType)) {
				return true;
			}
			if (possibleSharpRightOrUTurn && TurnType.isSharpRightOrUTurn(mainTurnType) && TurnType.isSharpRightOrUTurn(turnType)) {
				return true;
			}
		}
		return false;
	}

	private TurnType getActiveTurnType(int[] lanes, boolean leftSide, TurnType oldTurnType) {
		if (lanes == null || lanes.length == 0) {
			return oldTurnType;
		}
		int tp = oldTurnType.getValue();
		int cnt = 0;
		for (int k = 0; k < lanes.length; k++) {
			int ln = lanes[k];
			if ((ln & 1) > 0) {
				int[] oneActiveLane = {lanes[k]};
				if (hasAllowedLanes(oldTurnType.getValue(), oneActiveLane, 0, 0)) {
					tp = TurnType.getPrimaryTurn(lanes[k]);
				}
				cnt++;
			}
		}
		TurnType t = TurnType.valueOf(tp, leftSide);
		// mute when most lanes have a straight/slight direction
		if (cnt >= 3 && TurnType.isSlightTurn(t.getValue())) {
			t.setSkipToSpeak(true);
		}
		return t;
	}

	private boolean isHighSpeakPriority(RouteSegmentResult curr) {
		List<RouteSegmentResult> attachedRoutes = curr.getAttachedRoutes(curr.getStartPointIndex());
		String h = curr.getObject().getHighway();
		for (RouteSegmentResult attach : attachedRoutes) {
			String c = attach.getObject().getHighway();
			if( highwaySpeakPriority(h) >= highwaySpeakPriority(c)) {
				return true;
			}
		}
		return false;
	}

	private boolean isForkByLanes(RouteSegmentResult curr, RouteSegmentResult prev) {
		//check for Y-intersections with many lanes
		if (countLanesMinOne(curr) < countLanesMinOne(prev)) {
			List<RouteSegmentResult> attachedRoutes = curr.getAttachedRoutes(curr.getStartPointIndex());
			if (attachedRoutes.size() == 1) {
				return countLanesMinOne(attachedRoutes.get(0)) >= 2;
			}
		}
		return false;
	}

	private boolean isKeepTurn(TurnType t) {
		return t.keepRight() || t.keepLeft();
	}

	private boolean isSwitchToLink(RouteSegmentResult curr, RouteSegmentResult prev) {
		String c = curr.getObject().getHighway();
		String p = prev.getObject().getHighway();
		return c != null && c.contains("_link") && 
				p != null && !p.contains("_link");
	}

	private boolean twiceRoadPresent(List<RouteSegmentResult> result, int i) {
		if (i > 0 && i < result.size() - 1) {
			RouteSegmentResult prev = result.get(i - 1);
			String turnLanes = getTurnLanesString(prev);
			if (turnLanes == null) {
				return false;
			}
			RouteSegmentResult curr = result.get(i);
			RouteSegmentResult next = result.get(i + 1);
			if (prev.getObject().getId() == curr.getObject().getId()) {
				List<RouteSegmentResult> attachedRoutes = next.getAttachedRoutes(next.getStartPointIndex());
				//check if turn lanes allowed for next segment
				return !Algorithms.isEmpty(attachedRoutes);
			} else {
				List<RouteSegmentResult> attachedRoutes = curr.getAttachedRoutes(curr.getStartPointIndex());
				for (RouteSegmentResult attach : attachedRoutes) {
					if (attach.getObject().getId() == prev.getObject().getId()) {
						//check if road the continue in attached roads
						return true;
					}
				}
			}
		}
		return false;
	}

}
