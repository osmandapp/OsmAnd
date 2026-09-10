package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.ObfConstants;
import net.osmand.shared.routing.RouteTypeRule;
import net.osmand.shared.routing.RouteDataObject;
import net.osmand.shared.routing.DirectionPoint;
import net.osmand.shared.routing.RoutingConfiguration;
import net.osmand.data.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.render.RenderingRulesStorage.RenderingRulesStorageResolver;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.shared.routing.RouteCalculationMode;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.shared.routing.RoadSplitStructure;
import net.osmand.shared.routing.RoadSplitStructure.AttachedRoadInfo;
import net.osmand.shared.routing.TurnLanes;
import net.osmand.shared.routing.TurnPreparation;
import net.osmand.shared.routing.TurnType;
import net.osmand.shared.routing.GeneralRouterProfile;
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
import net.osmand.shared.routing.RouteSegmentResult;
import net.osmand.shared.util.KMapUtils;
import net.osmand.shared.routing.GeneralRouter;

public class RouteResultPreparation {

	public static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION = true;
	public static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = false;
	public static String PRINT_TO_GPX_FILE = null;
	private static final float TURN_DEGREE_MIN = 45;
	private static final float UNMATCHED_TURN_DEGREE_MINIMUM = 45;
	private static final float SPLIT_TURN_DEGREE_NOT_STRAIGHT = 100;
	private static final float TURN_SLIGHT_DEGREE = 5;
	private static final int MAX_SPEAK_PRIORITY = 5;
	
	protected static final Log LOG = PlatformUtil.getLog(RouteResultPreparation.class);
	public static final String UNMATCHED_HIGHWAY_TYPE = TurnPreparation.UNMATCHED_HIGHWAY_TYPE;
	
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

		/** routes with the same start / end, empty unless they were requested and found */
		public List<List<RouteSegmentResult>> getAlternatives() {
			return Collections.emptyList();
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
		if (ctx.requestNativePrepareResult) {
			return new RouteCalcResult(result);
		}
		for (int i = 0; i < result.size(); i++) {
			RouteDataObject road = result.get(i).getObject();
			checkAndInitRouteRegion(ctx, road);
			// "osmand_dp" using for backward compatibility from native lib RoutingConfiguration directionPoints
			if (road.region != null) {
				road.region.findOrCreateRouteType(DirectionPoint.TAG, DirectionPoint.DELETE_TYPE);
			}
		}
		combineWayPointsForAreaRouting(ctx, result);
		validateAllPointsConnected(result);
		splitRoadsAndAttachRoadSegments(ctx, result);
		for (int i = 0; i < result.size(); i++) {
			TurnPreparation.filterMinorStops(result.get(i));
		}
		TurnPreparation.calculateTimeSpeed(ctx, result);
		TurnPreparation.prepareTurnResults(ctx, result);
		RouteCalcResult res = new RouteCalcResult(result);
		return res;
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
			double d = KMapUtils.INSTANCE.getDistance(pr.getPoint(pr.getEndPointIndex()), rr.getPoint(rr.getStartPointIndex()));
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
				TurnPreparation.addRouteSegmentToResult(ctx, result, res, false);
				
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
				TurnPreparation.addRouteSegmentToResult(ctx, result, res, true);
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
					(ObfConstants.getOsmObjectId(res.getObject())) + "", res.getObject().getId() + "", 
					res.getStartPointIndex() + "", res.getEndPointIndex() + "", additional.toString()));
			int inc = res.getStartPointIndex() < res.getEndPointIndex() ? 1 : -1;
			int indexnext = res.getStartPointIndex();
			LatLon prev = null;
			for (int index = res.getStartPointIndex() ; index != res.getEndPointIndex(); ) {
				index = indexnext;
				indexnext += inc; 
				if (serializer != null) {
					try {
						LatLon l = LatLon.of(res.getPoint(index));
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
							serializer.text((ObfConstants.getOsmObjectId(res.getObject())) + " " + index);
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
	}
