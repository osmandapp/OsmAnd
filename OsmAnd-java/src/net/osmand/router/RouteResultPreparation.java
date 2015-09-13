package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteTypeRule;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.router.BinaryRoutePlanner.FinalRouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

public class RouteResultPreparation {

	public static boolean PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST = false;
	private static final float TURN_DEGREE_MIN = 45;
	private Log log = PlatformUtil.getLog(RouteResultPreparation.class);
	/**
	 * Helper method to prepare final result 
	 */
	List<RouteSegmentResult> prepareResult(RoutingContext ctx, FinalRouteSegment finalSegment) throws IOException {
		List<RouteSegmentResult> result  = convertFinalSegmentToResults(ctx, finalSegment);
		prepareResult(ctx, result);
		return result;
	}

	List<RouteSegmentResult> prepareResult(RoutingContext ctx, List<RouteSegmentResult> result) throws IOException {
		validateAllPointsConnected(result);
		splitRoadsAndAttachRoadSegments(ctx, result);
		calculateTimeSpeed(ctx, result);
		
		for (int i = 0; i < result.size(); i ++) {
			TurnType turnType = getTurnInfo(result, i, ctx.leftSideNavigation);
			result.get(i).setTurnType(turnType);
		}
		
		determineTurnsToMerge(ctx.leftSideNavigation, result);
		justifyUTurns(ctx.leftSideNavigation, result);
		addTurnInfoDescriptions(result);
		return result;
	}

	private void justifyUTurns(boolean leftSide, List<RouteSegmentResult> result) {
		int next;
		for (int i = 0; i < result.size() - 1; i = next) {
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

	private void calculateTimeSpeed(RoutingContext ctx, List<RouteSegmentResult> result) throws IOException {
		for (int i = 0; i < result.size(); i++) {
			RouteSegmentResult rr = result.get(i);
			RouteDataObject road = rr.getObject();
			double distOnRoadToPass = 0;
			double speed = ctx.getRouter().defineVehicleSpeed(road);
			if (speed == 0) {
				speed = ctx.getRouter().getMinDefaultSpeed();
			} else {
				if(speed > 15) {
					// decrease speed proportionally from 15ms=50kmh - 
					// reference speed 30ms=108kmh - 7kmh
					speed = speed - ((speed - 15f) / (30f - 15f) * 2f);
				}
			}
			boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
			int next;
			double distance = 0;
			for (int j = rr.getStartPointIndex(); j != rr.getEndPointIndex(); j = next) {
				next = plus ? j + 1 : j - 1;
				double d = measuredDist(road.getPoint31XTile(j), road.getPoint31YTile(j), road.getPoint31XTile(next),
						road.getPoint31YTile(next));
				distance += d;
				double obstacle = ctx.getRouter().defineObstacle(road, j);
				if (obstacle < 0) {
					obstacle = 0;
				}
				distOnRoadToPass += d / speed + obstacle;

			}
			// last point turn time can be added
			// if(i + 1 < result.size()) { distOnRoadToPass += ctx.getRouter().calculateTurnTime(); }
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
			RouteDataObject road = rr.getObject();
			checkAndInitRouteRegion(ctx, road);
			boolean plus = rr.getStartPointIndex() < rr.getEndPointIndex();
			int next;
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
				if(rr.getDistance(next, plus ) == 0) {
					// same point will be processed next step
					tryToSplit = false;
				}
				if (tryToSplit) {
					// avoid small zigzags
					float before = rr.getBearing(next, !plus);
					float after = rr.getBearing(next, plus);
					if(rr.getDistance(next, plus ) < 5) {
						after = before + 180;
					} else if(rr.getDistance(next, !plus ) < 5) {
						before = after - 180;
					}
					boolean straight = Math.abs(MapUtils.degreesDiff(before + 180, after)) < TURN_DEGREE_MIN;
					boolean isSplit = false;
					// split if needed
					for (RouteSegmentResult rs : attachedRoutes) {
						double diff = MapUtils.degreesDiff(before + 180, rs.getBearingBegin());
						if (Math.abs(diff) <= TURN_DEGREE_MIN) {
							isSplit = true;
						} else if (!straight && Math.abs(diff) < 100) {
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
		if(reader != null) {
			reader.initRouteRegion(road.region);
		}
	}

	private void validateAllPointsConnected(List<RouteSegmentResult> result) {
		for (int i = 1; i < result.size(); i++) {
			RouteSegmentResult rr = result.get(i);
			RouteSegmentResult pr = result.get(i - 1);
			double d = MapUtils.getDistance(pr.getPoint(pr.getEndPointIndex()), rr.getPoint(rr.getStartPointIndex()));
			if (d > 0) {
				System.err.println("Points are not connected : " + pr.getObject() + "(" + pr.getEndPointIndex() + ") -> " + rr.getObject()
						+ "(" + rr.getStartPointIndex() + ") " + d + " meters");
			}
		}
	}

	private List<RouteSegmentResult> convertFinalSegmentToResults(RoutingContext ctx, FinalRouteSegment finalSegment) {
		List<RouteSegmentResult> result = new ArrayList<RouteSegmentResult>();
		if (finalSegment != null) {
			ctx.routingTime = finalSegment.distanceFromStart;
			println("Routing calculated time distance " + finalSegment.distanceFromStart);
			// Get results from opposite direction roads
			RouteSegment segment = finalSegment.reverseWaySearch ? finalSegment : 
				finalSegment.opposite.getParentRoute();
			int parentSegmentStart = finalSegment.reverseWaySearch ? finalSegment.opposite.getSegmentStart() : 
				finalSegment.opposite.getParentSegmentEnd();
			float parentRoutingTime = -1;
			while (segment != null) {
				RouteSegmentResult res = new RouteSegmentResult(segment.road, parentSegmentStart, segment.getSegmentStart());
				parentRoutingTime = calcRoutingTime(parentRoutingTime, finalSegment, segment, res);
				parentSegmentStart = segment.getParentSegmentEnd();
				segment = segment.getParentRoute();
				addRouteSegmentToResult(ctx, result, res, false);
			}
			// reverse it just to attach good direction roads
			Collections.reverse(result);

			segment = finalSegment.reverseWaySearch ? finalSegment.opposite.getParentRoute() : finalSegment;
			int parentSegmentEnd = finalSegment.reverseWaySearch ? finalSegment.opposite.getParentSegmentEnd() : finalSegment.opposite.getSegmentStart();
			parentRoutingTime = -1;
			while (segment != null) {
				RouteSegmentResult res = new RouteSegmentResult(segment.road, segment.getSegmentStart(), parentSegmentEnd);
				parentRoutingTime = calcRoutingTime(parentRoutingTime, finalSegment, segment, res);
				parentSegmentEnd = segment.getParentSegmentEnd();
				segment = segment.getParentRoute();
				// happens in smart recalculation
				addRouteSegmentToResult(ctx, result, res, true);
			}
			Collections.reverse(result);
			// checkTotalRoutingTime(result);
		}
		return result;
	}

	protected void checkTotalRoutingTime(List<RouteSegmentResult> result) {
		float totalRoutingTime = 0;
		for(RouteSegmentResult r : result) {
			totalRoutingTime += r.getRoutingTime();
		}
		println("Total routing time ! " + totalRoutingTime);
	}

	private float calcRoutingTime(float parentRoutingTime, RouteSegment finalSegment, RouteSegment segment,
			RouteSegmentResult res) {
		if(segment != finalSegment) {
			if(parentRoutingTime != -1) {
				res.setRoutingTime(parentRoutingTime - segment.distanceFromStart);
			}
			parentRoutingTime = segment.distanceFromStart;
		}
		return parentRoutingTime;
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
	
	void printResults(RoutingContext ctx, LatLon start, LatLon end, List<RouteSegmentResult> result) {
		float completeTime = 0;
		float completeDistance = 0;
		for(RouteSegmentResult r : result) {
			completeTime += r.getSegmentTime();
			completeDistance += r.getDistance();
		}

		println("ROUTE : ");
		double startLat = start.getLatitude();
		double startLon = start.getLongitude();
		double endLat = end.getLatitude();
		double endLon = end.getLongitude();
		String msg = MessageFormat.format("<test regions=\"\" description=\"\" best_percent=\"\" vehicle=\"{4}\" \n"
				+ "    start_lat=\"{0}\" start_lon=\"{1}\" target_lat=\"{2}\" target_lon=\"{3}\" {5} >", 
				startLat + "", startLon + "", endLat + "", endLon + "", ctx.config.routerName, 
				"loadedTiles = \"" + ctx.loadedTiles + "\" " + "visitedSegments = \"" + ctx.visitedSegments + "\" " +
				"complete_distance = \"" + completeDistance + "\" " + "complete_time = \"" + completeTime + "\" " +
				"routing_time = \"" + ctx.routingTime + "\" ");
		log.info(msg);
        println(msg);
		if (PRINT_TO_CONSOLE_ROUTE_INFORMATION_TO_TEST) {
			for (RouteSegmentResult res : result) {
				String name = res.getObject().getName();
				String ref = res.getObject().getRef();
				if (name == null) {
					name = "";
				}
				if (ref != null) {
					name += " (" + ref + ") ";
				}
				StringBuilder additional = new StringBuilder();
				additional.append("time = \"").append(res.getSegmentTime()).append("\" ");
				additional.append("rtime = \"").append(res.getRoutingTime()).append("\" ");
				additional.append("name = \"").append(name).append("\" ");
//				float ms = res.getSegmentSpeed();
				float ms = res.getObject().getMaximumSpeed(res.isForwardDirection());
				if(ms > 0) {
					additional.append("maxspeed = \"").append(ms * 3.6f).append("\" ").append(res.getObject().getHighway()).append(" ");
				}
				additional.append("distance = \"").append(res.getDistance()).append("\" ");
				if (res.getTurnType() != null) {
					additional.append("turn = \"").append(res.getTurnType()).append("\" ");
					additional.append("turn_angle = \"").append(res.getTurnType().getTurnAngle()).append("\" ");
					if (res.getTurnType().getLanes() != null) {
						additional.append("lanes = \"").append(Arrays.toString(res.getTurnType().getLanes())).append("\" ");
					}
				}
				additional.append("start_bearing = \"").append(res.getBearingBegin()).append("\" ");
				additional.append("end_bearing = \"").append(res.getBearingEnd()).append("\" ");
				additional.append("description = \"").append(res.getDescription()).append("\" ");
				println(MessageFormat.format("\t<segment id=\"{0}\" start=\"{1}\" end=\"{2}\" {3}/>", (res.getObject().getId()) + "",
						res.getStartPointIndex() + "", res.getEndPointIndex() + "", additional.toString()));
				printAdditionalPointInfo(res);
			}
		}
		println("</test>");
	}

	private void printAdditionalPointInfo(RouteSegmentResult res) {
		boolean plus = res.getStartPointIndex() < res.getEndPointIndex();
		for(int k = res.getStartPointIndex(); k != res.getEndPointIndex(); ) {
			int[] tp = res.getObject().getPointTypes(k);
			String[] pointNames = res.getObject().getPointNames(k);
			int[] pointNameTypes = res.getObject().getPointNameTypes(k);
			if (tp != null || pointNameTypes != null) {
				StringBuilder bld = new StringBuilder();
				bld.append("<point ");
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
				bld.append("/>");
				println("\t"+bld.toString());
			}
			if(plus) {
				k++;
			} else {
				k--;
			}
		}
	}


	protected void addTurnInfoDescriptions(List<RouteSegmentResult> result) {
		int prevSegment = -1;
		float dist = 0;
		for (int i = 0; i <= result.size(); i++) {
			if (i == result.size() || result.get(i).getTurnType() != null) {
				if (prevSegment >= 0) {
					String turn = result.get(prevSegment).getTurnType().toString();
					final int[] lns = result.get(prevSegment).getTurnType().getLanes();
					if (lns != null) {
						String s = "[ ";
						for (int h = 0; h < lns.length; h++) {
							if (h > 0) {
								s += ", ";
							}
							if (lns[h] % 2 == 1) {
								s += "+";
							}
							int pt = TurnType.getPrimaryTurn(lns[h]);
							if (pt == 0) {
								pt = 1;
							}
							s += TurnType.valueOf(pt, false).toXmlString();
							int st = TurnType.getSecondaryTurn(lns[h]);
							if (st != 0) {
								s += ";" + TurnType.valueOf(st, false).toXmlString();
							}
							
						}
						s += "]";
						turn += s;
					}
					result.get(prevSegment).setDescription(
							turn + MessageFormat.format(" and go {0,number,#.##} meters", dist));
					if (result.get(prevSegment).getTurnType().isSkipToSpeak()) {
						result.get(prevSegment).setDescription("-*" + result.get(prevSegment).getDescription());
					}
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
		boolean tl = TurnType.TL == t.getValue();
		boolean tr = TurnType.TR == t.getValue();
		if(tl || tr) {
			TurnType tnext = result.get(i + 1).getTurnType();
			if (tnext != null && result.get(i).getDistance() < 35) { //
				boolean ut = true;
				if (i > 0) {
					double uTurn = MapUtils.degreesDiff(result.get(i - 1).getBearingEnd(), result
							.get(i + 1).getBearingBegin());
					if (Math.abs(uTurn) < 120) {
						ut = false;
					}
				}
				String highway = result.get(i).getObject().getHighway();
				if(highway == null || highway.endsWith("track") || highway.endsWith("services") || highway.endsWith("service")
						|| highway.endsWith("path")) {
					ut = false;
				}
				if (ut) {
					tnext.setSkipToSpeak(true);
					if (tl && TurnType.TL == tnext.getValue()) {
						return TurnType.valueOf(TurnType.TU, false);
					} else if (tr && TurnType.TR == tnext.getValue()) {
						return TurnType.valueOf(TurnType.TU, true);
					}
				}
			}
		}
		return null;
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
				if (nextSegment != null) {
					String hw = currentSegment.getObject().getHighway();
					double mergeDistance = 200;
					if (hw != null && (hw.startsWith("trunk") || hw.startsWith("motorway"))) {
						mergeDistance = 400;
					}
					if (dist < mergeDistance) {
						mergeTurnLanes(leftside, currentSegment, nextSegment);
					}
				}
				nextSegment = currentSegment;
				dist = 0;
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
	
	private void mergeTurnLanes(boolean leftSide, RouteSegmentResult currentSegment, RouteSegmentResult nextSegment) {
		MergeTurnLaneTurn active = new MergeTurnLaneTurn(currentSegment);
		MergeTurnLaneTurn target = new MergeTurnLaneTurn(nextSegment);
		if (active.activeLen < 2) {
			return;
		}
		if (target.activeStartIndex == -1) {
			return;
		}
		boolean changed = false;
		if (target.isActiveTurnMostLeft()) {
			// let only the most left lanes be enabled
			if (target.activeLen <= active.activeLen) {
				active.activeEndIndex -= (active.activeLen - target.activeLen);
				changed = true;
			}
		} else if (target.isActiveTurnMostRight()) {
			// next turn is right
			// let only the most right lanes be enabled
			if (target.activeLen < active.activeLen) {
				active.activeStartIndex += (active.activeLen - target.activeLen);
				changed = true;
			}
		} else {
			// next turn is get through (take out the left and the right turn)
			if (target.activeLen < active.activeLen) {
				float ratio = (active.activeLen - target.activeLen) / 2f;
				active.activeEndIndex = (int) Math.ceil(active.activeEndIndex - ratio);
				active.activeStartIndex = (int) Math.floor(active.activeStartIndex + ratio);
				changed = true;
			}
		}
		if (!changed) {
			return;
		}

		// set the allowed lane bit
		for (int i = 0; i < active.disabledLanes.length; i++) {
			if (i >= active.activeStartIndex && i <= active.activeEndIndex) {
				active.disabledLanes[i] |= 1;
			}
		}
		TurnType currentTurn = currentSegment.getTurnType();
		currentTurn.setLanes(active.disabledLanes);
		int turn = inferTurnFromLanes(active.disabledLanes);
		if (turn != 0 && turn != currentTurn.getValue()) {
			TurnType newTurnType = TurnType.valueOf(turn, leftSide);
			newTurnType.setLanes(active.disabledLanes);
			newTurnType.setSkipToSpeak(currentTurn.isSkipToSpeak());
			currentSegment.setTurnType(newTurnType);
		}
	}
	
	private static final int MAX_SPEAK_PRIORITY = 5;
	private int highwaySpeakPriority(String highway) {
		if(highway == null || highway.endsWith("track") || highway.endsWith("services") || highway.endsWith("service")
				|| highway.endsWith("path")) {
			return MAX_SPEAK_PRIORITY;
		}
		if (highway.endsWith("_link")  || highway.endsWith("unclassified") || highway.endsWith("road") 
				|| highway.endsWith("living_street") || highway.endsWith("residential") )  {
			return 1;
		}
		return 0;
	}


	private TurnType getTurnInfo(List<RouteSegmentResult> result, int i, boolean leftSide) {
		if (i == 0) {
			return TurnType.valueOf(TurnType.C, false);
		}
		RouteSegmentResult prev = result.get(i - 1) ;
		if(prev.getObject().roundabout()) {
			// already analyzed!
			return null;
		}
		RouteSegmentResult rr = result.get(i);
		if (rr.getObject().roundabout()) {
			return processRoundaboutTurn(result, i, leftSide, prev, rr);
		}
		TurnType t = null;
		if (prev != null) {
			boolean noAttachedRoads = rr.getAttachedRoutes(rr.getStartPointIndex()).size() == 0;
			// add description about turn
			double mpi = MapUtils.degreesDiff(prev.getBearingEnd(), rr.getBearingBegin());
			if(noAttachedRoads){
				// TODO VICTOR : look at the comment inside direction route
				// ? avoid small zigzags is covered at (search for "zigzags") 
//				double begin = rr.getObject().directionRoute(rr.getStartPointIndex(), rr.getStartPointIndex() < 
//						rr.getEndPointIndex(), 25);
//				mpi = MapUtils.degreesDiff(prev.getBearingEnd(), begin);
			}
			if (mpi >= TURN_DEGREE_MIN) {
				if (mpi < 45) {
					// Slight turn detection here causes many false positives where drivers would expect a "normal" TL. Best use limit-angle=TURN_DEGREE_MIN, this reduces TSL to the turn-lanes cases.
					t = TurnType.valueOf(TurnType.TSLL, leftSide);
				} else if (mpi < 120) {
					t = TurnType.valueOf(TurnType.TL, leftSide);
				} else if (mpi < 150 || leftSide) {
					t = TurnType.valueOf(TurnType.TSHL, leftSide);
				} else {
					t = TurnType.valueOf(TurnType.TU, leftSide);
				}
				int[] lanes = getTurnLanesInfo(prev, t.getValue());
				t.setLanes(lanes);
			} else if (mpi < -TURN_DEGREE_MIN) {
				if (mpi > -45) {
					t = TurnType.valueOf(TurnType.TSLR, leftSide);
				} else if (mpi > -120) {
					t = TurnType.valueOf(TurnType.TR, leftSide);
				} else if (mpi > -150 || !leftSide) {
					t = TurnType.valueOf(TurnType.TSHR, leftSide);
				} else {
					t = TurnType.valueOf(TurnType.TRU, leftSide);
				}
				int[] lanes = getTurnLanesInfo(prev, t.getValue());
				t.setLanes(lanes);
			} else {
				t = attachKeepLeftInfoAndLanes(leftSide, prev, rr);
			}
			if (t != null) {
				t.setTurnAngle((float) -mpi);
			}
		}
		return t;
	}

	private int[] getTurnLanesInfo(RouteSegmentResult prevSegm, int mainTurnType) {
		String turnLanes = getTurnLanesString(prevSegm);
		if (turnLanes == null) {
			return null;
		}
		int[] lanesArray = calculateRawTurnLanes(turnLanes, mainTurnType);
		// Manually set the allowed lanes.
		boolean isSet = setAllowedLanes(mainTurnType, lanesArray);
		if(!isSet && lanesArray.length > 0) {
			// In some cases (at least in the US), the rightmost lane might not have a right turn indicated as per turn:lanes,
			// but is allowed and being used here. This section adds in that indicator.  The same applies for where leftSide is true.
			boolean leftTurn = TurnType.isLeftTurn(mainTurnType);
			int ind = leftTurn? 0 : lanesArray.length - 1;
			final int tt = TurnType.getPrimaryTurn(lanesArray[ind]);
			if (leftTurn) {
				if (!TurnType.isLeftTurn(tt)) {
					// This was just to make sure that there's no bad data.
					TurnType.setPrimaryTurnAndReset(lanesArray, ind, TurnType.TL);
					TurnType.setSecondaryTurn(lanesArray, ind, tt);
					lanesArray[ind] |= 1;
				}
			} else {
				if (!TurnType.isRightTurn(tt)) {
					// This was just to make sure that there's no bad data.
					TurnType.setPrimaryTurnAndReset(lanesArray, ind, TurnType.TR);
					TurnType.setSecondaryTurn(lanesArray, ind, tt);
					lanesArray[ind] |= 1;
				}
			}
			setAllowedLanes(tt, lanesArray);
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

	private TurnType processRoundaboutTurn(List<RouteSegmentResult> result, int i, boolean leftSide, RouteSegmentResult prev,
			RouteSegmentResult rr) {
		int exit = 1;
		RouteSegmentResult last = rr;
		for (int j = i; j < result.size(); j++) {
			RouteSegmentResult rnext = result.get(j);
			last = rnext;
			if (rnext.getObject().roundabout()) {
				boolean plus = rnext.getStartPointIndex() < rnext.getEndPointIndex();
				int k = rnext.getStartPointIndex();
				if (j == i) {
					// first exit could be immediately after roundabout enter
//					k = plus ? k + 1 : k - 1;
				}
				while (k != rnext.getEndPointIndex()) {
					int attachedRoads = rnext.getAttachedRoutes(k).size();
					if(attachedRoads > 0) {
						exit++;
					}
					k = plus ? k + 1 : k - 1;
				}
			} else {
				break;
			}
		}
		// combine all roundabouts
		TurnType t = TurnType.getExitTurn(exit, 0, leftSide);
		t.setTurnAngle((float) MapUtils.degreesDiff(last.getBearingBegin(), prev.getBearingEnd())) ;
		return t;
	}
	
	private class RoadSplitStructure {
		boolean keepLeft = false;
		boolean keepRight = false;
		boolean speak = false;
		int leftLanes = 0;
		int rightLanes = 0;
		int roadsOnLeft = 0;
		int addRoadsOnLeft = 0;
		int roadsOnRight = 0;
		int addRoadsOnRight = 0;
	}


	private TurnType attachKeepLeftInfoAndLanes(boolean leftSide, RouteSegmentResult prevSegm, RouteSegmentResult currentSegm) {
		List<RouteSegmentResult> attachedRoutes = currentSegm.getAttachedRoutes(currentSegm.getStartPointIndex());
		if(attachedRoutes == null || attachedRoutes.size() == 0) {
			return null;
		}
		// keep left/right
		RoadSplitStructure rs = calculateRoadSplitStructure(prevSegm, currentSegm, attachedRoutes);
		if(rs.roadsOnLeft  + rs.roadsOnRight == 0) {
			return null;
		}
		
		// turn lanes exist
		String turnLanes = getTurnLanesString(prevSegm);
		if (turnLanes != null) {
			return createKeepLeftRightTurnBasedOnTurnTypes(rs, prevSegm, currentSegm, turnLanes, leftSide);
		}

		// turn lanes don't exist
		if (rs.keepLeft || rs.keepRight) {
			return createSimpleKeepLeftRightTurn(leftSide, prevSegm, currentSegm, rs);
			
		}
		return null;
	}

	protected TurnType createKeepLeftRightTurnBasedOnTurnTypes(RoadSplitStructure rs, RouteSegmentResult prevSegm,
			RouteSegmentResult currentSegm, String turnLanes,boolean leftSide) {
		// Maybe going straight at a 90-degree intersection
		TurnType t = TurnType.valueOf(TurnType.C, leftSide);
		int[] rawLanes = calculateRawTurnLanes(turnLanes, TurnType.C);
		if (rs.keepLeft || rs.keepRight) {
			String[] splitLaneOptions = turnLanes.split("\\|", -1);
			int activeBeginIndex = findActiveIndex(rawLanes, splitLaneOptions, rs.leftLanes, true, 
					rs.roadsOnLeft, rs.addRoadsOnLeft);
			int activeEndIndex = findActiveIndex(rawLanes, splitLaneOptions, rs.rightLanes, false, 
					rs.roadsOnRight, rs.addRoadsOnRight);
			if (activeBeginIndex == -1 || activeEndIndex == -1 || activeBeginIndex > activeEndIndex) {
				// something went wrong
				return createSimpleKeepLeftRightTurn(leftSide, prevSegm, currentSegm, rs);
			}
			for (int k = 0; k < rawLanes.length; k++) {
				if (k >= activeBeginIndex && k <= activeEndIndex) {
					rawLanes[k] |= 1;
				}
			}
			int tp = inferTurnFromLanes(rawLanes);
			if (tp != t.getValue() && tp != 0) {
				t = TurnType.valueOf(tp, leftSide);
			}
		} else {
			for (int k = 0; k < rawLanes.length; k++) {
				int turn = rawLanes[k];
				boolean active = false;
				if (TurnType.getPrimaryTurn(turn) == TurnType.C) {
					active = true;
				} else if (TurnType.isRightTurn(turn) && rs.roadsOnRight == 0) {
					// some turns go through many segments (to turn right or left)
					// so on one first segment the lane could be available and may be only 1 possible
					// all undesired lanes will be disabled through the 2nd pass
					active = true;
				} else if (TurnType.isLeftTurn(turn) && rs.roadsOnLeft == 0) {
					active = true;
				}
				if (active) {
					rawLanes[k] |= 1;
				}
			}
		}
		t.setSkipToSpeak(!rs.speak);
		t.setLanes(rawLanes);
		return t;
	}

	protected int findActiveIndex(int[] rawLanes, String[] splitLaneOptions, int lanes, boolean left, 
			int roads, int addRoads) {
		int activeStartIndex = -1;
		boolean lookupSlightTurn = addRoads > 0;
		for (int i = 0; i < rawLanes.length; i++) {
			int ind = left ? i : (rawLanes.length - i - 1);
			if (!lookupSlightTurn ||
					TurnType.isSlightTurn(TurnType.getPrimaryTurn(rawLanes[ind]))
					|| TurnType.isSlightTurn(TurnType.getSecondaryTurn(rawLanes[ind]))) {
				int cnt = countOccurrences(splitLaneOptions[ind], ';') + 1;
				if(cnt > 1) {
					// sometimes slight right turn goes to the road with 2 lanes 
					// the better situation to group all the lanes and 
					// when ';' we know for sure the lane combines 2 group
					roads --;
				}
				lanes -= cnt;
				// we already found slight turn others are turn in different direction
				lookupSlightTurn = false;
			}
			if (lanes < 0 || roads < 0) {
				activeStartIndex = ind;
				break;
			}
		}
		return activeStartIndex;
	}

	protected RoadSplitStructure calculateRoadSplitStructure(RouteSegmentResult prevSegm, RouteSegmentResult currentSegm,
			List<RouteSegmentResult> attachedRoutes) {
		RoadSplitStructure rs = new RoadSplitStructure();
		int speakPriority = Math.max(highwaySpeakPriority(prevSegm.getObject().getHighway()), highwaySpeakPriority(currentSegm.getObject().getHighway()));
		for (RouteSegmentResult attached : attachedRoutes) {
			double ex = MapUtils.degreesDiff(attached.getBearingBegin(), currentSegm.getBearingBegin());
			double mpi = Math.abs(MapUtils.degreesDiff(prevSegm.getBearingEnd(), attached.getBearingBegin()));
			int rsSpeakPriority = highwaySpeakPriority(attached.getObject().getHighway());
			if (rsSpeakPriority != MAX_SPEAK_PRIORITY || speakPriority == MAX_SPEAK_PRIORITY) {
				int lanes = countLanesMinOne(attached);
				boolean smallStraightVariation = mpi < TURN_DEGREE_MIN;
				boolean smallTargetVariation = Math.abs(ex) < TURN_DEGREE_MIN;
				boolean attachedOnTheRight = ex >= 0;
				if (attachedOnTheRight) {
					rs.roadsOnRight++;
				} else {
					rs.roadsOnLeft++;
				}
				if (smallTargetVariation || smallStraightVariation) {
					if (attachedOnTheRight) {
						rs.keepLeft = true;
						rs.rightLanes += lanes;
					} else {
						rs.keepRight = true;
						rs.leftLanes += lanes;
					}
					rs.speak = rs.speak || rsSpeakPriority <= speakPriority;
				} else {
					if (attachedOnTheRight) {
						rs.addRoadsOnRight++;
					} else {
						rs.addRoadsOnLeft++;
					}
				}
			}
		}
		return rs;
	}

	protected TurnType createSimpleKeepLeftRightTurn(boolean leftSide, RouteSegmentResult prevSegm,
			RouteSegmentResult currentSegm, RoadSplitStructure rs) {
		int current = countLanesMinOne(currentSegm);
		int ls = current + rs.leftLanes + rs.rightLanes;
		int[] lanes = new int[ls];
		for (int it = 0; it < ls; it++) {
			if (it < rs.leftLanes || it >= rs.leftLanes + current) {
				lanes[it] = 0;
			} else {
				lanes[it] = 1;
			}
		}
		// sometimes links are
		if ((current <= rs.leftLanes + rs.rightLanes) && (rs.leftLanes > 1 || rs.rightLanes > 1)) {
			rs.speak = true;
		}
		double devation = Math.abs(MapUtils.degreesDiff(prevSegm.getBearingEnd(), currentSegm.getBearingBegin()));
		boolean makeSlightTurn = devation > 5 && (!isMotorway(prevSegm) || !isMotorway(currentSegm));
		TurnType t = null;
		if (rs.keepLeft && rs.keepRight) {
			t = TurnType.valueOf(TurnType.C, leftSide);
		} else if (rs.keepLeft) {
			t = TurnType.valueOf(makeSlightTurn ? TurnType.TSLL : TurnType.KL, leftSide);
		} else if (rs.keepRight) {
			t = TurnType.valueOf(makeSlightTurn ? TurnType.TSLR : TurnType.KR, leftSide);
		} else {
			return t;
		}
		t.setSkipToSpeak(!rs.speak);
		t.setLanes(lanes);
		return t;
	}

	
	protected int countLanesMinOne(RouteSegmentResult attached) {
		final boolean oneway = attached.getObject().getOneway() != 0;
		int lns = attached.getObject().getLanes();
		if(lns == 0) {
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
		} catch(NumberFormatException e) {
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
	
	private static int[] calculateRawTurnLanes(String turnLanes, int calcTurnType) {
		String[] splitLaneOptions = turnLanes.split("\\|", -1);
		int[] lanes = new int[splitLaneOptions.length];
		for (int i = 0; i < splitLaneOptions.length; i++) {
			String[] laneOptions = splitLaneOptions[i].split(";");

			for (int j = 0; j < laneOptions.length; j++) {
				int turn;
				if (laneOptions[j].equals("none") || laneOptions[j].equals("through")) {
					turn = TurnType.C;
				} else if (laneOptions[j].equals("slight_right")) {
					turn = TurnType.TSLR;
				} else if (laneOptions[j].equals("slight_left")) {
					turn = TurnType.TSLL;
				} else if (laneOptions[j].equals("right")) {
					turn = TurnType.TR;
				} else if (laneOptions[j].equals("left")) {
					turn = TurnType.TL;
				} else if (laneOptions[j].equals("sharp_right")) {
					turn = TurnType.TSHR;
				} else if (laneOptions[j].equals("sharp_left")) {
					turn = TurnType.TSHL;
				} else if (laneOptions[j].equals("reverse")) {
					turn = TurnType.TU;
				} else {
					// Unknown string
					continue;
				}

				final int primary = TurnType.getPrimaryTurn(lanes[i]);
				if (primary == 0) {
					TurnType.setPrimaryTurnAndReset(lanes, i, turn);
				} else {
                    if (turn == calcTurnType || 
                    	(TurnType.isRightTurn(calcTurnType) && TurnType.isRightTurn(turn)) || 
                    	(TurnType.isLeftTurn(calcTurnType) && TurnType.isLeftTurn(turn)) 
                    	) {
                    	TurnType.setPrimaryTurnAndReset(lanes, i, turn);
                        TurnType.setSecondaryTurn(lanes, i, primary);
                    } else {
                        TurnType.setSecondaryTurn(lanes, i, turn);
                    }
					break; // Move on to the next lane
				}
			}
		}
		return lanes;
	}

	private int inferTurnFromLanes(int[] oLanes) {
		TIntHashSet possibleTurns = new TIntHashSet();
		for (int i = 0; i < oLanes.length; i++) {
			if ((oLanes[i] & 1) == 0) {
				continue;
			}
			if (possibleTurns.isEmpty()) {
				// Nothing is in the list to compare to, so add the first elements
				possibleTurns.add(TurnType.getPrimaryTurn(oLanes[i]));
				if (TurnType.getSecondaryTurn(oLanes[i]) != 0) {
					possibleTurns.add(TurnType.getSecondaryTurn(oLanes[i]));
				}
			} else {
				TIntArrayList laneTurns = new TIntArrayList();
				laneTurns.add(TurnType.getPrimaryTurn(oLanes[i]));
				if (TurnType.getSecondaryTurn(oLanes[i]) != 0) {
					laneTurns.add(TurnType.getSecondaryTurn(oLanes[i]));
				}
				possibleTurns.retainAll(laneTurns);
				if (possibleTurns.isEmpty()) {
					// No common turns, so can't determine anything.
					return 0;
				}
			}
		}

		// Remove all turns from lanes not selected...because those aren't it
		for (int i = 0; i < oLanes.length; i++) {
			if ((oLanes[i] & 1) == 0 && !possibleTurns.isEmpty()) {
				possibleTurns.remove((Integer) TurnType.getPrimaryTurn(oLanes[i]));
				if (TurnType.getSecondaryTurn(oLanes[i]) != 0) {
					possibleTurns.remove((Integer) TurnType.getSecondaryTurn(oLanes[i]));
				}
			}
		}

		// Checking to see that there is only one unique turn
		if (possibleTurns.size() == 1) {
			int infer = possibleTurns.iterator().next();
			for(int i = 0; i < oLanes.length; i++) {
				if(TurnType.getSecondaryTurn(oLanes[i]) == infer) {
					int pt = TurnType.getPrimaryTurn(oLanes[i]);
					int en = oLanes[i] & 1;
					TurnType.setPrimaryTurnAndReset(oLanes, i, infer);
					oLanes[i] |= en;
					TurnType.setSecondaryTurn(oLanes, i, pt);
				}
				
			}
			return infer;
		}

		return 0;
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
		if(rr.getPreAttachedRoutes(pointInd) != null) {
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
					return new RouteSegment(r.getObject(), r.getStartPointIndex());
				}

				@Override
				public void remove() {
				}
			};	
		} else {
			RouteSegment rt = ctx.loadRouteSegment(road.getPoint31XTile(pointInd), road.getPoint31YTile(pointInd), ctx.config.memoryLimitation);
			it = rt == null ? null : rt.getIterator();
		}
		// try to attach all segments except with current id
		while (it != null && it.hasNext()) {
			RouteSegment routeSegment = it.next();
			if (routeSegment.road.getId() != road.getId() && routeSegment.road.getId() != previousRoadId) {
				RouteDataObject addRoad = routeSegment.road;
				checkAndInitRouteRegion(ctx, addRoad);
				// TODO restrictions can be considered as well
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
}
