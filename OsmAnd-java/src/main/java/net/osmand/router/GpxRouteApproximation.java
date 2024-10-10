package net.osmand.router;

import gnu.trove.list.array.TIntArrayList;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.util.*;

public class GpxRouteApproximation {
	
	public static final int GPX_OSM_POINTS_MATCH_ALGORITHM = 1;
	public static final int GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM = 2;
	public static int GPX_SEGMENT_ALGORITHM = GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM;
	
	public List<RoutePlannerFrontEnd.GpxPoint> finalPoints = new ArrayList<>();
	public List<RouteSegmentResult> fullRoute = new ArrayList<>();

	public final RoutingContext ctx;
	private RoutePlannerFrontEnd router;
	private int routeCalculations = 0;
	public int routePointsSearched = 0;
	private int routeDistCalculations = 0;
	public int routeDistance;
	// private int routeGapDistance; // never used - remove
	private int routeDistanceUnmatched;
	private final Log log = PlatformUtil.getLog(RoutePlannerFrontEnd.class);

	public GpxRouteApproximation(RoutingContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String toString() {
		return String.format(">> GPX approximation (%d of %d m route calcs, %d route points searched) for %d m: %d m unmatched",
				routeCalculations, routeDistCalculations, routePointsSearched, routeDistance, routeDistanceUnmatched);
	}

	public GpxRouteApproximation searchGpxRouteInternal(RoutePlannerFrontEnd router,
			List<RoutePlannerFrontEnd.GpxPoint> gpxPoints, ResultMatcher<GpxRouteApproximation> resultMatcher,
			boolean useExternalTimestamps) throws IOException, InterruptedException {
		this.router = router;
		GpxRouteApproximation result;
		if (router.isUseGeometryBasedApproximation()) {
			result = searchGpxSegments(this, gpxPoints);
		} else {
			result = searchGpxRouteByRouting(this, gpxPoints);
		}
		result.reconstructFinalPointsFromFullRoute();
		if (useExternalTimestamps) {
			result.applyExternalTimestamps(gpxPoints);
		}
		if (resultMatcher != null) {
			resultMatcher.publish(this.ctx.calculationProgress.isCancelled ? null : this);
		}
		return result;
	}

	public List<RouteSegmentResult> collectFinalPointsAsRoute() {
		List<RouteSegmentResult> route = new ArrayList<RouteSegmentResult>();
		for (RoutePlannerFrontEnd.GpxPoint gp : finalPoints) {
			route.addAll(gp.routeToTarget);
		}
		return Collections.unmodifiableList(route);
	}

	private double distFromLastPoint(LatLon pnt) {
		if (fullRoute.size() > 0) {
			return MapUtils.getDistance(getLastPoint(), pnt);
		}
		return 0;
	}

	private LatLon getLastPoint() {
		if (fullRoute.size() > 0) {
			return fullRoute.get(fullRoute.size() - 1).getEndPoint();
		}
		return null;
	}

	private void applyExternalTimestamps(List<RoutePlannerFrontEnd.GpxPoint> sourcePoints) {
		if (!validateExternalTimestamps(sourcePoints)) {
			log.warn("applyExternalTimestamps() got invalid sourcePoints");
			return;
		}
		for (RoutePlannerFrontEnd.GpxPoint gp : finalPoints) {
			for (RouteSegmentResult seg : gp.routeToTarget) {
				seg.setSegmentSpeed(calcSegmentSpeedByExternalTimestamps(gp, seg, sourcePoints));
			}
			RouteResultPreparation.recalculateTimeDistance(gp.routeToTarget);
		}
	}

	private float calcSegmentSpeedByExternalTimestamps(RoutePlannerFrontEnd.GpxPoint gp, RouteSegmentResult seg,
	                                                   List<RoutePlannerFrontEnd.GpxPoint> sourcePoints) {
		float speed = seg.getSegmentSpeed();
		int indexStart = gp.ind, indexEnd = gp.targetInd;

		if (indexEnd == -1 && indexStart >= 0 && indexStart + 1 < sourcePoints.size()) {
			indexEnd = indexStart + 1; // this is straight line
		}

		if (indexStart >=0 && indexEnd > 0 && indexStart < indexEnd) {
			long time = sourcePoints.get(indexEnd).time - sourcePoints.get(indexStart).time;
			if (time > 0) {
				double distance = 0;
				for (int i = indexStart; i < indexEnd; i++) {
					distance += MapUtils.getDistance(sourcePoints.get(i).loc, sourcePoints.get(i + 1).loc);
				}
				if (distance > 0) {
					speed = (float) distance / ((float) time / 1000); // update based on external timestamps
				}
			}
		}

		return speed;
	}

	private boolean validateExternalTimestamps(List<RoutePlannerFrontEnd.GpxPoint> points) {
		if (points == null || points.isEmpty()) {
			return false;
		}
		long last = 0;
		for (RoutePlannerFrontEnd.GpxPoint p : points) {
			if (p.time == 0 || p.time < last) {
				return false;
			}
			last = p.time;
		}
		return true;
	}

	private void reconstructFinalPointsFromFullRoute() {
		// create gpx-to-final index map, clear routeToTarget(s)
		Map<Integer, Integer> gpxIndexFinalIndex = new HashMap<>();
		for (int i = 0; i < finalPoints.size(); i++) {
			gpxIndexFinalIndex.put(finalPoints.get(i).ind, i);
			finalPoints.get(i).routeToTarget.clear();
		}

		// reconstruct routeToTarget from scratch
		int lastIndex = 0;
		for (RouteSegmentResult seg : fullRoute) {
			int index = seg.getGpxPointIndex();
			if (index == -1) {
				index = lastIndex;
			} else {
				lastIndex = index;
			}
			finalPoints.get(gpxIndexFinalIndex.get(index)).routeToTarget.add(seg);
		}

		// finally remove finalPoints with empty route
		List<RoutePlannerFrontEnd.GpxPoint> emptyFinalPoints = new ArrayList<>();
		for (RoutePlannerFrontEnd.GpxPoint gp : finalPoints) {
			if (gp.routeToTarget != null) {
				if (gp.routeToTarget.size() == 0) {
					emptyFinalPoints.add(gp);
				}
			}
		}
		if (emptyFinalPoints.size() > 0) {
			finalPoints.removeAll(emptyFinalPoints);
		}
	}

	private GpxRouteApproximation searchGpxSegments(GpxRouteApproximation gctx, List<RoutePlannerFrontEnd.GpxPoint> gpxPoints) throws IOException, InterruptedException {
		NativeLibrary nativeLib = gctx.ctx.nativeLib;
		if (nativeLib != null && router.isUseNativeApproximation()) {
			gctx = nativeLib.runNativeSearchGpxRoute(gctx, gpxPoints, true);
		} else {
			if (gctx.ctx.calculationProgress == null) {
				gctx.ctx.calculationProgress = new RouteCalculationProgress();
			}
			if (GPX_SEGMENT_ALGORITHM == GPX_OSM_POINTS_MATCH_ALGORITHM) {
				GpxPointsMatchApproximation app = new GpxPointsMatchApproximation();
				app.gpxApproximation(router, gctx, gpxPoints);
			} else if (GPX_SEGMENT_ALGORITHM == GPX_OSM_MULTISEGMENT_SCAN_ALGORITHM) {
				GpxMultiSegmentsApproximation app = new GpxMultiSegmentsApproximation(router, gctx, gpxPoints);
				app.gpxApproximation();
			}
			calculateGpxRouteResult(gctx, gpxPoints);
			if (!gctx.fullRoute.isEmpty() && !gctx.ctx.calculationProgress.isCancelled) {
				RouteResultPreparation.printResults(gctx.ctx, gpxPoints.get(0).loc,
						gpxPoints.get(gpxPoints.size() - 1).loc, gctx.fullRoute);
				log.info(gctx);
			}
		}
		return gctx;
	}

	private GpxRouteApproximation searchGpxRouteByRouting(GpxRouteApproximation gctx, List<RoutePlannerFrontEnd.GpxPoint> gpxPoints) throws IOException, InterruptedException {
		long timeToCalculate = System.nanoTime();
		NativeLibrary nativeLib = gctx.ctx.nativeLib;
		if (nativeLib != null && router.isUseNativeApproximation()) {
			gctx = nativeLib.runNativeSearchGpxRoute(gctx, gpxPoints, false);
		} else {
			gctx.ctx.keepNativeRoutingContext = true;
			if (gctx.ctx.calculationProgress == null) {
				gctx.ctx.calculationProgress = new RouteCalculationProgress();
			}
			RoutePlannerFrontEnd.GpxPoint start = null;
			RoutePlannerFrontEnd.GpxPoint prev = null;
			if (gpxPoints.size() > 0) {
				gctx.ctx.calculationProgress.totalApproximateDistance = (float) gpxPoints.get(gpxPoints.size() - 1).cumDist;
				start = gpxPoints.get(0);
			}
			float minPointApproximation = gctx.ctx.config.minPointApproximation;
			while (start != null && !gctx.ctx.calculationProgress.isCancelled) {
				double routeDist = gctx.ctx.config.maxStepApproximation;
				RoutePlannerFrontEnd.GpxPoint next = findNextGpxPointWithin(gpxPoints, start, routeDist);
				boolean routeFound = false;
				if (next != null && initRoutingPoint(start, gctx, minPointApproximation)) {
					while (routeDist >= gctx.ctx.config.minStepApproximation && !routeFound) {
						routeFound = initRoutingPoint(next, gctx, minPointApproximation);
						if (routeFound) {
							routeFound = findGpxRouteSegment(gctx, gpxPoints, start, next, prev != null);
							if (routeFound) {
								routeFound = isRouteCloseToGpxPoints(minPointApproximation, gpxPoints, start, next);
								if (!routeFound) {
									start.routeToTarget = null;
								}
							}
							if (routeFound && next.ind < gpxPoints.size() - 1) {
								// route is found - cut the end of the route and move to next iteration
								// start.stepBackRoute = new ArrayList<RouteSegmentResult>();
								// boolean stepBack = true;
								boolean stepBack = stepBackAndFindPrevPointInRoute(gctx, gpxPoints, start, next);
								if (!stepBack) {
									// not supported case (workaround increase routing.xml maxStepApproximation)
									log.info("Consider to increase routing.xml maxStepApproximation to: " + routeDist * 2);
									start.routeToTarget = null;
									routeFound = false;
								} else {
									if (gctx.ctx.getVisitor() != null) {
										gctx.ctx.getVisitor().visitApproximatedSegments(start.routeToTarget, start,
												next);
									}
								}
							}
						}
						if (!routeFound) {
							// route is not found move next point closer to start point (distance / 2)
							routeDist = routeDist / 2;
							if (routeDist < gctx.ctx.config.minStepApproximation
									&& routeDist > gctx.ctx.config.minStepApproximation / 2 + 1) {
								routeDist = gctx.ctx.config.minStepApproximation;
							}
							next = findNextGpxPointWithin(gpxPoints, start, routeDist);
							if (next != null) {
								routeDist = Math.min(next.cumDist - start.cumDist, routeDist);
							}
						}
					}
				}
				// route is not found skip segment and keep it as straight line on display
				if (!routeFound && next != null) {
					// route is not found, move start point by
					next = findNextGpxPointWithin(gpxPoints, start, gctx.ctx.config.minStepApproximation);
					if (prev != null) {
						prev.routeToTarget.addAll(prev.stepBackRoute);
						if (next != null) {
							log.warn("NOT found route from: " + start.pnt.getRoad() + " at " + start.pnt.getSegmentStart());
						}
					}
					prev = null;
				} else {
					prev = start;
				}
				start = next;
				if (gctx.ctx.calculationProgress != null && start != null) {
					gctx.ctx.calculationProgress.approximatedDistance = (float) start.cumDist;
				}
			}
			if (gctx.ctx.calculationProgress != null) {
				gctx.ctx.calculationProgress.timeToCalculate = System.nanoTime() - timeToCalculate;
			}
			gctx.ctx.deleteNativeRoutingContext();
			calculateGpxRouteResult(gctx, gpxPoints);
			if (!gctx.fullRoute.isEmpty() && !gctx.ctx.calculationProgress.isCancelled) {
				RouteResultPreparation.printResults(gctx.ctx, gpxPoints.get(0).loc,
						gpxPoints.get(gpxPoints.size() - 1).loc, gctx.fullRoute);
				log.info(gctx);
			}
		}
		return gctx;
	}

	private boolean stepBackAndFindPrevPointInRoute(GpxRouteApproximation gctx, List<RoutePlannerFrontEnd.GpxPoint> gpxPoints,
	                                                RoutePlannerFrontEnd.GpxPoint start, RoutePlannerFrontEnd.GpxPoint next) throws IOException {
		// step back to find to be sure
		// 1) route point is behind GpxPoint - minPointApproximation (end route point could slightly ahead)
		// 2) we don't miss correct turn i.e. points could be attached to multiple routes
		// 3) to make sure that we perfectly connect to RoadDataObject points
		double STEP_BACK_DIST = Math.max(gctx.ctx.config.minPointApproximation, gctx.ctx.config.minStepApproximation);
		double d = 0;
		int segmendInd = start.routeToTarget.size() - 1;
		boolean search = true;
		start.stepBackRoute = new ArrayList<RouteSegmentResult>();
		mainLoop: for (; segmendInd >= 0 && search; segmendInd--) {
			RouteSegmentResult rr = start.routeToTarget.get(segmendInd);
			boolean minus = rr.getStartPointIndex() < rr.getEndPointIndex();
			int nextInd;
			for (int j = rr.getEndPointIndex(); j != rr.getStartPointIndex(); j = nextInd) {
				nextInd = minus ? j - 1 : j + 1;
				d += MapUtils.getDistance(rr.getPoint(j), rr.getPoint(nextInd));
				if (d > STEP_BACK_DIST) {
					if (nextInd == rr.getStartPointIndex()) {
						segmendInd--;
					} else {
						RouteSegmentResult seg = new RouteSegmentResult(rr.getObject(), nextInd, rr.getEndPointIndex());
						seg.setGpxPointIndex(start.ind);
						start.stepBackRoute.add(seg);
						rr.setEndPointIndex(nextInd);
					}
					search = false;
					break mainLoop;
				}
			}
		}
		if (segmendInd == -1) {
			// here all route segments - 1 is longer than needed distance to step back
			return false;
		}

		while (start.routeToTarget.size() > segmendInd + 1) {
			RouteSegmentResult removed = start.routeToTarget.remove(segmendInd + 1);
			start.stepBackRoute.add(removed);
		}
		RouteSegmentResult res = start.routeToTarget.get(segmendInd);
		int end = res.getEndPointIndex();
		int beforeEnd = res.isForwardDirection() ? end - 1 : end + 1;
//		res.setEndPointIndex(beforeEnd);
		next.pnt = new BinaryRoutePlanner.RouteSegmentPoint(res.getObject(), beforeEnd, end, 0);
		// use start point as it overlaps
		// as we step back we can't use precise coordinates
//		next.pnt.preciseX = MapUtils.get31TileNumberX(next.loc.getLongitude());
//		next.pnt.preciseY = MapUtils.get31TileNumberY(next.loc.getLatitude());
		next.pnt.preciseX = next.pnt.getEndPointX();
		next.pnt.preciseY = next.pnt.getEndPointY();
		return true;
	}

	private void calculateGpxRouteResult(GpxRouteApproximation gctx, List<RoutePlannerFrontEnd.GpxPoint> gpxPoints) throws IOException {
		BinaryMapRouteReaderAdapter.RouteRegion reg = new BinaryMapRouteReaderAdapter.RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);
		List<LatLon> lastStraightLine = null;
		RoutePlannerFrontEnd.GpxPoint straightPointStart = null;

		for (int i = 0; i < gpxPoints.size() && !gctx.ctx.calculationProgress.isCancelled; ) {
			RoutePlannerFrontEnd.GpxPoint pnt = gpxPoints.get(i);
			if (pnt.routeToTarget != null && !pnt.routeToTarget.isEmpty()) {
				LatLon startPoint = pnt.getFirstRouteRes().getStartPoint();
				if (lastStraightLine != null) {
					router.makeSegmentPointPrecise(gctx.ctx, pnt.getFirstRouteRes(), pnt.loc, true);
					startPoint = pnt.getFirstRouteRes().getStartPoint();
					lastStraightLine.add(startPoint);
					addStraightLine(gctx, lastStraightLine, straightPointStart, reg);
					lastStraightLine = null;
				}
				if (gctx.distFromLastPoint(startPoint) > 1) {
					// gctx.routeGapDistance += gctx.distFromLastPoint(startPoint);
					System.out.println(String.format("?? gap of route point = %f, gap of actual gpxPoint = %f, %s ",
							gctx.distFromLastPoint(startPoint), gctx.distFromLastPoint(pnt.loc), pnt.loc));
				}
				gctx.finalPoints.add(pnt);
				gctx.fullRoute.addAll(pnt.routeToTarget);
				i = pnt.targetInd;
			} else {
				// add straight line from i -> i+1
				if (lastStraightLine == null) {
					lastStraightLine = new ArrayList<LatLon>();
					if (gctx.getLastPoint() != null && gctx.finalPoints.size() > 0) {
						RoutePlannerFrontEnd.GpxPoint prev = gctx.finalPoints.get(gctx.finalPoints.size() - 1);
						router.makeSegmentPointPrecise(gctx.ctx, prev.getLastRouteRes(), pnt.loc, false);
						lastStraightLine.add(gctx.getLastPoint());
					}
					straightPointStart = pnt;
				}
				lastStraightLine.add(pnt.loc);
				i++;
			}
		}

		if (lastStraightLine != null) {
			addStraightLine(gctx, lastStraightLine, straightPointStart, reg);
			lastStraightLine = null;
		}
		if (router.isUseGeometryBasedApproximation()) {
			new RouteResultPreparation().prepareResult(gctx.ctx, gctx.fullRoute); // routing-based already did it
		} else {
			cleanDoubleJoints(gctx);
		}
		// clean turns to recalculate them
		cleanupResultAndAddTurns(gctx);
	}

	private void cleanupResultAndAddTurns(GpxRouteApproximation gctx) {
		RouteResultPreparation preparation = new RouteResultPreparation();
		preparation.validateAllPointsConnected(gctx.fullRoute);
		for (RouteSegmentResult r : gctx.fullRoute) {
			r.setTurnType(null);
			r.clearDescription();
		}
		if (!gctx.ctx.calculationProgress.isCancelled) {
			preparation.prepareTurnResults(gctx.ctx, gctx.fullRoute);
		}
		for (RouteSegmentResult r : gctx.fullRoute) {
			r.clearAttachedRoutes();
			r.clearPreattachedRoutes();
		}
	}

	private void cleanDoubleJoints(GpxRouteApproximation gctx) {
		int LOOK_AHEAD = 4;
		for (int i = 0; i < gctx.fullRoute.size() && !gctx.ctx.calculationProgress.isCancelled; i++) {
			RouteSegmentResult s = gctx.fullRoute.get(i);
			for (int j = i + 2; j <= i + LOOK_AHEAD && j < gctx.fullRoute.size(); j++) {
				RouteSegmentResult e = gctx.fullRoute.get(j);
				if (e.getStartPoint().equals(s.getEndPoint())) {
					while ((--j) != i) {
						gctx.fullRoute.remove(j);
					}
					break;
				}
			}
		}
	}

	private void addStraightLine(GpxRouteApproximation gctx, List<LatLon> lastStraightLine, RoutePlannerFrontEnd.GpxPoint strPnt, BinaryMapRouteReaderAdapter.RouteRegion reg) {
		RouteDataObject rdo = new RouteDataObject(reg);
		if (gctx.ctx.config.smoothenPointsNoRoute > 0) {
			simplifyDouglasPeucker(lastStraightLine, gctx.ctx.config.smoothenPointsNoRoute,
					0, lastStraightLine.size() - 1);
		}
		int s = lastStraightLine.size();
		TIntArrayList x = new TIntArrayList(s);
		TIntArrayList y = new TIntArrayList(s);
		for (int i = 0; i < s; i++) {
			if (lastStraightLine.get(i) != null) {
				LatLon l = lastStraightLine.get(i);
				int t = x.size() - 1;
				x.add(MapUtils.get31TileNumberX(l.getLongitude()));
				y.add(MapUtils.get31TileNumberY(l.getLatitude()));
				if (t >= 0) {
					double dist = MapUtils.squareRootDist31(x.get(t), y.get(t), x.get(t + 1), y.get(t + 1));
					gctx.routeDistanceUnmatched += dist;
				}
			}
		}
		rdo.pointsX = x.toArray();
		rdo.pointsY = y.toArray();
		rdo.types = new int[] { 0 } ;
		rdo.id = -1;
		strPnt.routeToTarget = new ArrayList<>();
		strPnt.straightLine = true;
		RouteSegmentResult line = new RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1);
		line.setGpxPointIndex(strPnt.ind);
		strPnt.routeToTarget.add(line);
		RouteResultPreparation preparation = new RouteResultPreparation();
		try {
			preparation.prepareResult(gctx.ctx, strPnt.routeToTarget);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		// VIEW: comment to see road without straight connections
		gctx.finalPoints.add(strPnt);
		gctx.fullRoute.addAll(strPnt.routeToTarget);
	}

	private void simplifyDouglasPeucker(List<LatLon> l, double eps, int start, int end) {
		double dmax = -1;
		int index = -1;
		LatLon s = l.get(start);
		LatLon e = l.get(end);
		for (int i = start + 1; i <= end - 1; i++) {
			LatLon ip = l.get(i);
			double dist = MapUtils.getOrthogonalDistance(ip.getLatitude(), ip.getLongitude(), s.getLatitude(), s.getLongitude(),
					e.getLatitude(), e.getLongitude());
			if (dist > dmax) {
				dmax = dist;
				index = i;
			}
		}
		if (dmax >= eps) {
			simplifyDouglasPeucker(l, eps, start, index);
			simplifyDouglasPeucker(l, eps, index, end);
		} else {
			for(int i = start + 1; i < end; i++ ) {
				l.set(i, null);
			}
		}
	}

	private boolean initRoutingPoint(RoutePlannerFrontEnd.GpxPoint start, GpxRouteApproximation gctx, double distThreshold) throws IOException {
		if (start != null && start.pnt == null) {
			gctx.routePointsSearched++;
			BinaryRoutePlanner.RouteSegmentPoint rsp = router.findRouteSegment(start.loc.getLatitude(), start.loc.getLongitude(), gctx.ctx, null, false);
			if (rsp != null) {
				if (MapUtils.getDistance(rsp.getPreciseLatLon(), start.loc) < distThreshold) {
					start.pnt = rsp;
				}
			}
		}
		if (start != null && start.pnt != null) {
			return true;
		}
		return false;
	}

	private RoutePlannerFrontEnd.GpxPoint findNextGpxPointWithin(List<RoutePlannerFrontEnd.GpxPoint> gpxPoints, RoutePlannerFrontEnd.GpxPoint start, double dist) {
		// returns first point with that has slightly more than dist or last point
		int plus = dist > 0 ? 1 : -1;
		int targetInd = start.ind + plus;
		RoutePlannerFrontEnd.GpxPoint target = null;
		while (targetInd < gpxPoints.size() && targetInd >= 0) {
			target = gpxPoints.get(targetInd);
			if (Math.abs(target.cumDist - start.cumDist) > Math.abs(dist)) {
				break;
			}
			targetInd = targetInd + plus;
		}
		return target;
	}

	private boolean findGpxRouteSegment(GpxRouteApproximation gctx, List<RoutePlannerFrontEnd.GpxPoint> gpxPoints,
	                                    RoutePlannerFrontEnd.GpxPoint start, RoutePlannerFrontEnd.GpxPoint target, boolean prevRouteCalculated) throws IOException, InterruptedException {
		RouteResultPreparation.RouteCalcResult res = null;
		boolean routeIsCorrect = false;
		if (start.pnt != null && target.pnt != null) {
			start.pnt = new BinaryRoutePlanner.RouteSegmentPoint(start.pnt);
			target.pnt = new BinaryRoutePlanner.RouteSegmentPoint(target.pnt);
			gctx.routeDistCalculations += (target.cumDist - start.cumDist);
			gctx.routeCalculations++;
			RoutingContext local = new RoutingContext(gctx.ctx);
			res = router.searchRouteInternalPrepare(local, start.pnt, target.pnt, null);
			//BinaryRoutePlanner.printDebugMemoryInformation(gctx.ctx);
			routeIsCorrect = res != null && res.isCorrect();
			for (int k = start.ind + 1; routeIsCorrect && k < target.ind; k++) {
				RoutePlannerFrontEnd.GpxPoint ipoint = gpxPoints.get(k);
				if (!pointCloseEnough(gctx, ipoint, res)) {
					routeIsCorrect = false;
				}
			}
			if (routeIsCorrect) {
				RouteSegmentResult firstSegment = res.detailed.get(0);
				// correct start point though don't change end point
				if (prevRouteCalculated) {
					if (firstSegment.getObject().getId() == start.pnt.getRoad().getId()) {
						// start point is end point of prev route
						firstSegment.setStartPointIndex(start.pnt.getSegmentEnd());
						if (firstSegment.getObject().getPointsLength() != start.pnt.getRoad().getPointsLength()) {
							firstSegment.setObject(start.pnt.road);
						}
						if (firstSegment.getStartPointIndex() == firstSegment.getEndPointIndex()) {
							res.detailed.remove(0);
						}
					} else {
						// for native routing this is possible when point lies on intersection of 2 lines
						// solution here could be to pass to native routing id of the route
						// though it should not create any issue
						System.out.println("??? not found " + start.pnt.getRoad().getId() + " instead "
								+ firstSegment.getObject().getId());
					}
				}
				for (RouteSegmentResult seg : res.detailed) {
					seg.setGpxPointIndex(start.ind);
				}
				start.routeToTarget = res.detailed;
				start.targetInd = target.ind;
			}
			if (gctx.ctx.getVisitor() != null) {
				gctx.ctx.getVisitor().visitApproximatedSegments(res.detailed, start, target);
			}
		}
		return routeIsCorrect;
	}

	private boolean isRouteCloseToGpxPoints(float minPointApproximation, List<RoutePlannerFrontEnd.GpxPoint> gpxPoints,
	                                        RoutePlannerFrontEnd.GpxPoint start, RoutePlannerFrontEnd.GpxPoint next) {
		boolean routeIsClose = true;
		for (RouteSegmentResult r : start.routeToTarget) {
			int st = r.getStartPointIndex();
			int end = r.getEndPointIndex();
			while (st != end) {
				LatLon point = r.getPoint(st);
				boolean pointIsClosed = false;
				int delta = 5, startInd = Math.max(0, start.ind - delta),
						nextInd = Math.min(gpxPoints.size() - 1, next.ind + delta);
				for (int k = startInd; !pointIsClosed && k < nextInd; k++) {
					pointIsClosed = pointCloseEnough(minPointApproximation, point, gpxPoints.get(k), gpxPoints.get(k + 1));
				}
				if (!pointIsClosed) {
					routeIsClose = false;
					break;
				}
				st += ((st < end) ? 1 : -1);
			}
		}
		return routeIsClose;
	}

	private boolean pointCloseEnough(float minPointApproximation, LatLon point, RoutePlannerFrontEnd.GpxPoint gpxPoint, RoutePlannerFrontEnd.GpxPoint gpxPointNext) {
		LatLon gpxPointLL = gpxPoint.pnt != null ? gpxPoint.pnt.getPreciseLatLon() : gpxPoint.loc;
		LatLon gpxPointNextLL = gpxPointNext.pnt != null ? gpxPointNext.pnt.getPreciseLatLon() : gpxPointNext.loc;
		double orthogonalDistance = MapUtils.getOrthogonalDistance(point.getLatitude(), point.getLongitude(),
				gpxPointLL.getLatitude(), gpxPointLL.getLongitude(),
				gpxPointNextLL.getLatitude(), gpxPointNextLL.getLongitude());
		return orthogonalDistance <= minPointApproximation;
	}

	private boolean pointCloseEnough(GpxRouteApproximation gctx, RoutePlannerFrontEnd.GpxPoint ipoint, RouteResultPreparation.RouteCalcResult res) {
		int px = MapUtils.get31TileNumberX(ipoint.loc.getLongitude());
		int py = MapUtils.get31TileNumberY(ipoint.loc.getLatitude());
		double SQR = gctx.ctx.config.minPointApproximation;
		SQR = SQR * SQR;
		for (RouteSegmentResult sr : res.detailed) {
			int start = sr.getStartPointIndex();
			int end = sr.getEndPointIndex();
			if (sr.getStartPointIndex() > sr.getEndPointIndex()) {
				start = sr.getEndPointIndex();
				end = sr.getStartPointIndex();
			}
			for (int i = start; i < end; i++) {
				RouteDataObject r = sr.getObject();
				QuadPointDouble pp = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(i), r.getPoint31YTile(i),
						r.getPoint31XTile(i + 1), r.getPoint31YTile(i + 1));
				double currentsDist = RoutePlannerFrontEnd.squareDist((int) pp.x, (int) pp.y, px, py);
				if (currentsDist <= SQR) {
					return true;
				}
			}
		}
		return false;
	}

}
