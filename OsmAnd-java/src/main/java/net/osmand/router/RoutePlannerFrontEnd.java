package net.osmand.router;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;

import net.osmand.LocationsHolder;
import net.osmand.NativeLibrary;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.router.BinaryRoutePlanner.RouteSegment;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.GeneralRouter.GeneralRouterProfile;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GpxRoutingApproximation.GpxApproximationContext;
import net.osmand.router.GpxRoutingApproximation.GpxPoint;
import net.osmand.router.HHRouteDataStructure.HHNetworkRouteRes;
import net.osmand.router.HHRouteDataStructure.HHRoutingConfig;
import net.osmand.router.HHRouteDataStructure.NetworkDBPoint;
import net.osmand.router.RouteCalculationProgress.HHIteration;
import net.osmand.router.RouteResultPreparation.RouteCalcResult;
import net.osmand.util.MapUtils;

public class RoutePlannerFrontEnd {

	protected static final Log log = PlatformUtil.getLog(RoutePlannerFrontEnd.class);
	// Check issue #8649
	protected static final double GPS_POSSIBLE_ERROR = 7;
	static boolean TRACE_ROUTING = false;

	private boolean useSmartRouteRecalculation = true;
	private boolean useNativeApproximation = true;
	private boolean useOnlyHHRouting = false;
	private HHRoutingConfig hhRoutingConfig = null;
	private HHRoutingType hhRoutingType = HHRoutingType.JAVA;

	public RoutePlannerFrontEnd() {
	}
	
	public static HHRoutingConfig defaultHHConfig() {
		return HHRoutingConfig.astar(0).calcDetailed(HHRoutingConfig.CALCULATE_ALL_DETAILED);
	}

	public enum RouteCalculationMode {
		BASE, NORMAL, COMPLEX
	}

	private enum HHRoutingType {
		JAVA, CPP
	}

	public void setHHRouteCpp(boolean cpp) {
		if (cpp) {
			hhRoutingType = HHRoutingType.CPP;
		} else {
			hhRoutingType = HHRoutingType.JAVA;
		}
	}

	

	

	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary,
			BinaryMapIndexReader[] map, RouteCalculationMode rm) {
		if (rm == null) {
			rm = config.router.getProfile() == GeneralRouterProfile.CAR ? RouteCalculationMode.COMPLEX
					: RouteCalculationMode.NORMAL;
		}
		return new RoutingContext(config, nativeLibrary, map, rm);
	}

	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map) {
		return buildRoutingContext(config, nativeLibrary, map, null);
	}

	static double squareDist(int x1, int y1, int x2, int y2) {
		return MapUtils.squareDist31TileMetric(x1, y1, x2, y2);
	}

	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list)
			throws IOException {
		return findRouteSegment(lat, lon, ctx, list, false);
	}

	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list,
			boolean transportStop) throws IOException {
		return findRouteSegment(lat, lon, ctx, list, false, false);
	}

	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list,
			boolean transportStop, boolean allowDuplications) throws IOException {
		long now = System.nanoTime();
		int px = MapUtils.get31TileNumberX(lon);
		int py = MapUtils.get31TileNumberY(lat);
		ArrayList<RouteDataObject> dataObjects = new ArrayList<RouteDataObject>();
		ctx.loadTileData(px, py, 17, dataObjects, allowDuplications);
		if (dataObjects.isEmpty()) {
			ctx.loadTileData(px, py, 15, dataObjects, allowDuplications);
		}
		if (dataObjects.isEmpty()) {
			ctx.loadTileData(px, py, 14, dataObjects, allowDuplications);
		}
		if (list == null) {
			list = new ArrayList<BinaryRoutePlanner.RouteSegmentPoint>();
		}
		for (RouteDataObject r : dataObjects) {
			if (r.getPointsLength() > 1) {
				RouteSegmentPoint road = null;
				for (int j = 1; j < r.getPointsLength(); j++) {
					QuadPointDouble pr = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(j - 1),
							r.getPoint31YTile(j - 1), r.getPoint31XTile(j), r.getPoint31YTile(j));
					double currentsDistSquare = squareDist((int) pr.x, (int) pr.y, px, py);
					if (road == null || currentsDistSquare < road.distToProj) {
						RouteDataObject ro = new RouteDataObject(r);
						road = new RouteSegmentPoint(ro, j - 1, j, currentsDistSquare);
						road.preciseX = (int) pr.x;
						road.preciseY = (int) pr.y;
					}
				}
				if (road != null) {
					if (!transportStop) {
						float prio = ctx.getRouter().defineDestinationPriority(road.road);
						if (prio > 0) {
							road.distToProj = (road.distToProj + GPS_POSSIBLE_ERROR * GPS_POSSIBLE_ERROR)
									/ (prio * prio);
							list.add(road);
						}
					} else {
						list.add(road);
					}

				}
			}
		}
		Collections.sort(list, new Comparator<RouteSegmentPoint>() {

			@Override
			public int compare(RouteSegmentPoint o1, RouteSegmentPoint o2) {
				return Double.compare(o1.distToProj, o2.distToProj);
			}
		});
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.timeToFindInitialSegments += (System.nanoTime() - now);
		}
		if (list.size() > 0) {
			RouteSegmentPoint ps = null;
			if (ctx.publicTransport) {
				for (RouteSegmentPoint p : list) {
					if (transportStop && p.distToProj > GPS_POSSIBLE_ERROR * GPS_POSSIBLE_ERROR) {
						break;
					}
					boolean platform = p.road.platform();
					if (transportStop && platform) {
						ps = p;
						break;
					}
					if (!transportStop && !platform) {
						ps = p;
						break;
					}
				}
			}
			if (ps == null) {
				ps = list.get(0);
			}
			list.remove(ps); // remove cyclic link to itself to avoid memory leaks (C++ backport)
			ps.others = list;
			return ps;
		}
		return null;
	}

	public RouteCalcResult searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates)
			throws IOException, InterruptedException {
		return searchRoute(ctx, start, end, intermediates, null);
	}

	public RoutePlannerFrontEnd setUseFastRecalculation(boolean use) {
		useSmartRouteRecalculation = use;
		return this;
	}

	public RoutePlannerFrontEnd setHHRoutingConfig(HHRoutingConfig hhRoutingConfig) {
		// null means don't use hh
		this.hhRoutingConfig = hhRoutingConfig;
		return this;
	}

	public RoutePlannerFrontEnd disableHHRoutingConfig() {
		this.hhRoutingConfig = null;
		return this;
	}

	public boolean isHHRoutingConfigured() {
		return this.hhRoutingConfig != null;
	}

	public void setDefaultHHRoutingConfig() {
		this.hhRoutingConfig = defaultHHConfig();
	}

	public RoutePlannerFrontEnd setUseOnlyHHRouting(boolean useOnlyHHRouting) {
		this.useOnlyHHRouting = useOnlyHHRouting;
		if (useOnlyHHRouting && hhRoutingConfig == null) {
			this.hhRoutingConfig = defaultHHConfig();
		}
		return this;
	}

	public RoutePlannerFrontEnd setUseNativeApproximation(boolean useNativeApproximation) {
		this.useNativeApproximation = useNativeApproximation;
		return this;
	}

	public boolean isUseNativeApproximation() {
		return useNativeApproximation;
	}

	public GpxApproximationContext searchGpxRoute(GpxApproximationContext gctx, List<GpxPoint> gpxPoints,
			ResultMatcher<GpxApproximationContext> resultMatcher) throws IOException, InterruptedException {
		return new GpxRoutingApproximation(this).searchGpxRoute(gctx, gpxPoints, resultMatcher);
	}
	
	public List<GpxPoint> generateGpxPoints(GpxApproximationContext gctx, LocationsHolder locationsHolder) {
		List<GpxPoint> gpxPoints = new ArrayList<>(locationsHolder.getSize());
		GpxPoint prev = null;
		for (int i = 0; i < locationsHolder.getSize(); i++) {
			GpxPoint p = new GpxPoint();
			p.ind = i;
			p.loc = locationsHolder.getLatLon(i);
			if (prev != null) {
				p.cumDist = MapUtils.getDistance(p.loc, prev.loc) + prev.cumDist;
			}
			gpxPoints.add(p);
			gctx.routeDistance = (int) p.cumDist;
			prev = p;
		}
		return gpxPoints;
	}
	
	private boolean needRequestPrivateAccessRouting(RoutingContext ctx, List<LatLon> points) throws IOException {
		boolean res = false;
		if (ctx.nativeLib != null) {
			int size = points.size();
			int[] y31Coordinates = new int[size];
			int[] x31Coordinates = new int[size];
			for (int i = 0; i < size; i++) {
				y31Coordinates[i] = MapUtils.get31TileNumberY(points.get(i).getLatitude());
				x31Coordinates[i] = MapUtils.get31TileNumberX(points.get(i).getLongitude());
			}
			res = ctx.nativeLib.needRequestPrivateAccessRouting(ctx, x31Coordinates, y31Coordinates);
		} else {
			GeneralRouter router = (GeneralRouter) ctx.getRouter();
			if (router == null) {
				return false;
			}
			Map<String, RoutingParameter> parameters = router.getParameters();
			String allowPrivateKey = null;
			if (parameters.containsKey(GeneralRouter.ALLOW_PRIVATE)) {
				allowPrivateKey = GeneralRouter.ALLOW_PRIVATE;
			} else if (parameters.containsKey(GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK)) {
				allowPrivateKey = GeneralRouter.ALLOW_PRIVATE;
			}
			if (!router.isAllowPrivate() && allowPrivateKey != null) {
				ctx.unloadAllData();
				LinkedHashMap<String, String> mp = new LinkedHashMap<String, String>();
				mp.put(allowPrivateKey, "true");
				mp.put(GeneralRouter.CHECK_ALLOW_PRIVATE_NEEDED, "true");
				ctx.setRouter(new GeneralRouter(router.getProfile(), mp));
				for (LatLon latLon : points) {
					RouteSegmentPoint rp = findRouteSegment(latLon.getLatitude(), latLon.getLongitude(), ctx, null);
					if (rp != null && rp.road != null) {
						if (rp.road.hasPrivateAccess(ctx.config.router.getProfile())) {
							res = true;
							break;
						}
					}
				}
				ctx.unloadAllData();
				ctx.setRouter(router);
			}
		}
		return res;
	}

	public RouteCalcResult searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates,
			PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		RouteCalcResult res = null;
		long timeToCalculate = System.nanoTime();
		if (ctx.calculationProgress == null) {
			ctx.calculationProgress = new RouteCalculationProgress();
		}
		boolean intermediatesEmpty = intermediates == null || intermediates.isEmpty();
		List<LatLon> targets = new ArrayList<>();
		if (!intermediatesEmpty) {
			targets.addAll(intermediates);
		}
		targets.add(end);
		if (needRequestPrivateAccessRouting(ctx, targets)) {
			ctx.calculationProgress.requestPrivateAccessRouting = true;
		}
		if (hhRoutingConfig != null && ctx.calculationMode != RouteCalculationMode.BASE) {
			ctx.calculationProgress.nextIteration();
			if (hhRoutingType == HHRoutingType.CPP && ctx.nativeLib != null) {
				ctx.initLatLonStartEndPoints(start, end, intermediates);
				RouteSegmentResult[] nr = runNativeRouting(ctx, hhRoutingConfig);
				if (nr.length > 0) {
					res = new HHNetworkRouteRes();
					res.detailed.addAll(Arrays.asList(nr));
				}
			} else {
				res = runJavaHHRoute(ctx, start, targets);
			}
		}
		if ((res == null || !res.isCorrect()) && !useOnlyHHRouting) {
			double maxDistance = MapUtils.getDistance(start, end);
			if (!intermediatesEmpty) {
				LatLon prev = start;
				for (LatLon next : intermediates) {
					maxDistance = Math.max(MapUtils.getDistance(prev, next), maxDistance);
					prev = next;
				}
			}
			if (ctx.calculationMode == RouteCalculationMode.COMPLEX && routeDirection == null
					&& maxDistance > RoutingConfiguration.DEVIATION_RADIUS * 6) {
				ctx.calculationProgress.totalIterations++;
				RoutingContext nctx = buildRoutingContext(ctx.config, ctx.nativeLib, ctx.getMaps(),
						RouteCalculationMode.BASE);
				nctx.calculationProgress = ctx.calculationProgress;
				RouteCalcResult baseRes = searchRoute(nctx, start, end, intermediates); // recursion for base routing
				if (baseRes == null || !baseRes.isCorrect()) {
					return baseRes;
				}
				routeDirection = PrecalculatedRouteDirection.build(baseRes.detailed,
						RoutingConfiguration.DEVIATION_RADIUS, ctx.getRouter().getMaxSpeed());
				ctx.calculationProgressFirstPhase = RouteCalculationProgress.capture(ctx.calculationProgress);
			}
			ctx.calculationProgress.nextIteration();
			if (!useSmartRouteRecalculation && intermediatesEmpty) {
				ctx.previouslyCalculatedRoute = null;
			}
			targets.add(0, start); // insert as 1st
			if (ctx.previouslyCalculatedRoute == null || intermediatesEmpty) {
				List<RouteSegmentPoint> points = new ArrayList<>();
				for (int i = 0; i < targets.size() - 1; i++) {
					RouteCalcResult lr = searchRouteAndPrepareTurns(ctx, targets.get(i), null, targets.get(i + 1), null, points, i, routeDirection);
					if (lr == null || !lr.isCorrect()) {
						return lr;
					} 
					makeStartEndPointsPrecise(lr.detailed, targets.get(i), targets.get(i + 1));
					if (res == null) {
						res = lr;
					} else {
						res.detailed.addAll(lr.detailed);
					}
				}
			} else {
				res = searchRouteWithInterSmartRecalc(ctx, targets, routeDirection);
			}
			
		}
		if (res != null && res.isCorrect()) {
			res = new RouteResultPreparation().prepareResult(ctx, res.detailed);
			if (RouteResultPreparation.PRINT_TO_CONSOLE_ROUTE_INFORMATION) {
				RouteResultPreparation.printResults(ctx, start, end, res.detailed);
			}
		}
		ctx.calculationProgress.timeToCalculate = (System.nanoTime() - timeToCalculate);
		return res;
	}

	private HHNetworkRouteRes runJavaHHRoute(RoutingContext ctx, LatLon start, List<LatLon> targets)
			throws IOException, InterruptedException {
		HHNetworkRouteRes r = null;
		HHRoutePlanner<NetworkDBPoint> routePlanner = HHRoutePlanner.create(ctx);
		Double dir = ctx.config.initialDirection;
		for (int i = 0; i < targets.size(); i++) {
			double initialPenalty = ctx.config.penaltyForReverseDirection;
			if (i > 0) {
				ctx.config.penaltyForReverseDirection /= 2; // relax reverse-penalty (only for inter-points)
			}
			ctx.calculationProgress.hhTargetsProgress(i, targets.size());
			HHNetworkRouteRes res = calculateHHRoute(routePlanner, ctx, i == 0 ? start : targets.get(i - 1),
					targets.get(i), dir);
			ctx.config.penaltyForReverseDirection = initialPenalty;
			if (r == null) {
				r = res;
			} else {
				r.append(res);
			}
			if (r == null || !r.isCorrect()) {
				break;
			}
			if (r.detailed.size() > 0) {
				dir = (r.detailed.get(r.detailed.size() - 1).getBearingEnd() / 180.0) * Math.PI;
			}
		}
		ctx.routingTime = r != null ? (float) r.getHHRoutingDetailed() : 0;
		return r;
	}

	private HHNetworkRouteRes calculateHHRoute(HHRoutePlanner<NetworkDBPoint> routePlanner, RoutingContext ctx,
			LatLon start, LatLon end, Double dir) throws InterruptedException, IOException {
		NativeLibrary nativeLib = ctx.nativeLib;
		ctx.nativeLib = null; // keep null to interfere with detailed
		try {
			HHRoutingConfig cfg = hhRoutingConfig;
			cfg.INITIAL_DIRECTION = dir;
			HHNetworkRouteRes res = routePlanner.runRouting(start, end, cfg);
			if (res != null && res.error == null) {
				ctx.calculationProgress.hhIteration(HHIteration.DONE);
				makeStartEndPointsPrecise(res.detailed, start, end);
				return res;
			}
			ctx.calculationProgress.hhIteration(HHIteration.HH_NOT_STARTED);
		} catch (SQLException e) {
			throw new IOException(e.getMessage(), e);
		} catch (IOException | RuntimeException e) {
			e.printStackTrace();
			if (useOnlyHHRouting) {
				return new HHNetworkRouteRes("Error during routing calculation : " + e.getMessage());
			}
		} finally {
			ctx.nativeLib = nativeLib;
		}
		return null;
	}

	protected void makeStartEndPointsPrecise(List<RouteSegmentResult> res, LatLon start, LatLon end) {
		if (res.size() > 0) {
			makeSegmentPointPrecise(res.get(0), start, true);
			makeSegmentPointPrecise(res.get(res.size() - 1), end, false);
		}
	}

	protected double projectDistance(List<RouteSegmentResult> res, int k, int px, int py) {
		RouteSegmentResult sr = res.get(k);
		RouteDataObject r = sr.getObject();
		QuadPointDouble pp = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(sr.getStartPointIndex()),
				r.getPoint31YTile(sr.getStartPointIndex()), r.getPoint31XTile(sr.getEndPointIndex()),
				r.getPoint31YTile(sr.getEndPointIndex()));
		double currentsDist = squareDist((int) pp.x, (int) pp.y, px, py);
		return currentsDist;
	}

	void makeSegmentPointPrecise(RouteSegmentResult routeSegmentResult, LatLon point, boolean st) {
		int px = MapUtils.get31TileNumberX(point.getLongitude());
		int py = MapUtils.get31TileNumberY(point.getLatitude());
		int pind = st ? routeSegmentResult.getStartPointIndex() : routeSegmentResult.getEndPointIndex();

		RouteDataObject r = new RouteDataObject(routeSegmentResult.getObject());
		routeSegmentResult.setObject(r);
		QuadPointDouble before = null;
		QuadPointDouble after = null;
		if (pind > 0) {
			before = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(pind - 1), r.getPoint31YTile(pind - 1),
					r.getPoint31XTile(pind), r.getPoint31YTile(pind));
		}
		if (pind < r.getPointsLength() - 1) {
			after = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(pind + 1), r.getPoint31YTile(pind + 1),
					r.getPoint31XTile(pind), r.getPoint31YTile(pind));
		}
		int insert = 0;
		double dd = MapUtils.getDistance(point, MapUtils.get31LatitudeY(r.getPoint31YTile(pind)),
				MapUtils.get31LongitudeX(r.getPoint31XTile(pind)));
		double ddBefore = Double.POSITIVE_INFINITY;
		double ddAfter = Double.POSITIVE_INFINITY;
		QuadPointDouble i = null;
		if (before != null) {
			ddBefore = MapUtils.getDistance(point, MapUtils.get31LatitudeY((int) before.y),
					MapUtils.get31LongitudeX((int) before.x));
			if (ddBefore < dd) {
				insert = -1;
				i = before;
			}
		}

		if (after != null) {
			ddAfter = MapUtils.getDistance(point, MapUtils.get31LatitudeY((int) after.y),
					MapUtils.get31LongitudeX((int) after.x));
			if (ddAfter < dd && ddAfter < ddBefore) {
				insert = 1;
				i = after;
			}
		}

		if (insert != 0) {
			if (st && routeSegmentResult.getStartPointIndex() < routeSegmentResult.getEndPointIndex()) {
				routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1);
			}
			if (!st && routeSegmentResult.getStartPointIndex() > routeSegmentResult.getEndPointIndex()) {
				routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1);
			}
			if (insert > 0) {
				r.insert(pind + 1, (int) i.x, (int) i.y);
				if (st) {
					routeSegmentResult.setStartPointIndex(routeSegmentResult.getStartPointIndex() + 1);
				}
				if (!st) {
					routeSegmentResult.setEndPointIndex(routeSegmentResult.getEndPointIndex() + 1);
				}
			} else {
				r.insert(pind, (int) i.x, (int) i.y);
			}

		}
	}

	private RouteCalcResult initSegmentPnt(final RoutingContext ctx, LatLon pnt, int i, List<RouteSegmentPoint> st)
			throws IOException {
		while (i >= st.size()) {
			st.add(null);
		}
		if (st.get(i) == null) {
			RouteSegmentPoint s = findRouteSegment(pnt.getLatitude(), pnt.getLongitude(), ctx, null,
					ctx.startTransportStop);
			if (s == null) {
				ctx.calculationProgress.segmentNotFound = i;
				return new RouteCalcResult((i == 0 ? "Start" : "Target") + " point is not located: " + pnt);
			}
			st.set(i, s);
		}
		return null; // success
	}

	RouteCalcResult searchRouteAndPrepareTurns(final RoutingContext ctx, RouteSegmentPoint s,
			RouteSegmentPoint e, PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		List<RouteSegmentPoint> points = new ArrayList<>();
		points.add(s);
		points.add(e);
		return searchRouteAndPrepareTurns(ctx, s.getPreciseLatLon(), s, e.getPreciseLatLon(), e, points, 0, routeDirection);
	}

	private RouteCalcResult searchRouteAndPrepareTurns(final RoutingContext ctx, LatLon start, RouteSegmentPoint s,
			LatLon end, RouteSegmentPoint e, List<RouteSegmentPoint> points, int i,
			PrecalculatedRouteDirection routeDirection)
			throws IOException, InterruptedException {
		RouteSegmentPoint recalculationEnd = getRecalculationEnd(ctx);
		if (recalculationEnd != null) {
			e = recalculationEnd;
		}
		if (ctx.nativeLib != null) {
			if (s != null && e != null) {
				ctx.initPreciseStartEndPoints(s, e);
			} else { 
				ctx.initLatLonStartEndPoints(start, end, null);
			}
		} else {
			RouteCalcResult err = initSegmentPnt(ctx, start, i, points);
			if (err == null) {
				s = points.get(i);
			} else {
				return err;
			}
			if (e == null) {
				err = initSegmentPnt(ctx, end, i + 1, points);
				if (err == null) {
					e = points.get(i + 1);
				} else {
					return err;
				}
			}
			ctx.initPreciseStartEndPoints(s, e);
		}
		if (routeDirection != null) {
			ctx.precalculatedRouteDirection = routeDirection.adopt(ctx);
		}
		List<RouteSegmentResult> result;
		if (ctx.nativeLib != null) {
			RouteSegmentResult[] res = runNativeRouting(ctx, null);
			result = new ArrayList<>(Arrays.asList(res));
		} else {
			refreshProgressDistance(ctx);
			RoutingContext local = new RoutingContext(ctx);
			ctx.finalRouteSegment = new BinaryRoutePlanner().searchRouteInternal(local, s, e, null);
			result = RouteResultPreparation.convertFinalSegmentToResults(ctx, ctx.finalRouteSegment);
			addPrecalculatedToResult(recalculationEnd, result);
		}
		return new RouteCalcResult(result); 
	}

	public RouteSegmentPoint getRecalculationEnd(final RoutingContext ctx) {
		RouteSegmentPoint recalculationEnd = null;
		boolean runRecalculation = ctx.previouslyCalculatedRoute != null && !ctx.previouslyCalculatedRoute.isEmpty()
				&& ctx.config.recalculateDistance != 0;
		if (runRecalculation) {
			List<RouteSegmentResult> rlist = new ArrayList<>();
			float distanceThreshold = ctx.config.recalculateDistance;
			float threshold = 0;
			for (RouteSegmentResult rr : ctx.previouslyCalculatedRoute) {
				threshold += rr.getDistance();
				if (threshold > distanceThreshold) {
					rlist.add(rr);
				}
			}
			if (!rlist.isEmpty()) {
				RouteSegment previous = null;
				for (int i = 0; i < rlist.size(); i++) {
					RouteSegmentResult rr = rlist.get(i);
					if (previous != null) {
						RouteSegment segment = new RouteSegment(rr.getObject(), rr.getStartPointIndex(),
								rr.getEndPointIndex());
						previous.setParentRoute(segment);
						previous = segment;
					} else {
						recalculationEnd = new RouteSegmentPoint(rr.getObject(), rr.getStartPointIndex(), 0);
						if (Math.abs(rr.getEndPointIndex() - rr.getStartPointIndex()) > 1) {
							RouteSegment segment = new RouteSegment(rr.getObject(), recalculationEnd.segEnd,
									rr.getEndPointIndex());
							recalculationEnd.setParentRoute(segment);
							previous = segment;
						} else {
							previous = recalculationEnd;
						}
					}
				}
			}
		}
		return recalculationEnd;
	}

	private void refreshProgressDistance(RoutingContext ctx) {
		if (ctx.calculationProgress != null) {
			ctx.calculationProgress.distanceFromBegin = 0;
			ctx.calculationProgress.distanceFromEnd = 0;
			ctx.calculationProgress.reverseSegmentQueueSize = 0;
			ctx.calculationProgress.directSegmentQueueSize = 0;
			float rd = (float) MapUtils.squareRootDist31(ctx.startX, ctx.startY, ctx.targetX, ctx.targetY);
			float speed = 0.9f * ctx.config.router.getMaxSpeed();
			ctx.calculationProgress.totalEstimatedDistance = (float) (rd / speed);
		}

	}

	private RouteSegmentResult[] runNativeRouting(final RoutingContext ctx, HHRoutingConfig hhConfig)
			throws IOException {
		refreshProgressDistance(ctx);
		RouteRegion[] regions = ctx.reverseMap.keySet().toArray(new RouteRegion[0]);
		// long time = System.currentTimeMillis();
		if (ctx.intermediatesX == null || ctx.intermediatesY == null) {
			ctx.intermediatesX = new int[0];
			ctx.intermediatesY = new int[0];
		}
		RouteSegmentResult[] res = ctx.nativeLib.runNativeRouting(ctx, hhConfig, regions,
				ctx.calculationMode == RouteCalculationMode.BASE);
		if (TRACE_ROUTING) {
			log.info("Native routing result!");
			for (RouteSegmentResult r : res) {
				log.info(
						"Road = " + r.getObject().id / 64 + " " + r.getStartPointIndex() + "->" + r.getEndPointIndex());
			}
		}
		// log.info("Native routing took " + (System.currentTimeMillis() - time) / 1000f
		// + " seconds");
		ctx.routingTime += ctx.calculationProgress.routingCalculatedTime;
		return res;
	}

	private void addPrecalculatedToResult(RouteSegment recalculationEnd, List<RouteSegmentResult> result) {
		if (recalculationEnd != null) {
			RouteSegment current = recalculationEnd;
			if (!hasSegment(result, current)) {
				if (TRACE_ROUTING) {
					log.info("Add recalculationEnd to result = " + current.getRoad() + " " + current.getSegmentStart()
							+ "->" + current.getSegmentEnd());
				}
				result.add(
						new RouteSegmentResult(current.getRoad(), current.getSegmentStart(), current.getSegmentEnd()));
			}
			while (current.getParentRoute() != null) {
				RouteSegment pr = current.getParentRoute();
				result.add(new RouteSegmentResult(pr.getRoad(), pr.getSegmentStart(), pr.getSegmentEnd()));
				if (TRACE_ROUTING) {
					log.info("Road = " + pr.getRoad() + " " + pr.getSegmentStart() + "->" + pr.getSegmentEnd());
				}
				current = pr;
			}
		}
	}

	private boolean hasSegment(List<RouteSegmentResult> result, RouteSegment current) {
		for (RouteSegmentResult r : result) {
			long currentId = r.getObject().id;
			if (currentId == current.getRoad().id && r.getStartPointIndex() == current.getSegmentStart()
					&& r.getEndPointIndex() == current.getSegmentEnd()) {
				return true;
			}
		}
		return false;
	}

	private RouteCalcResult searchRouteWithInterSmartRecalc(final RoutingContext ctx, List<LatLon> ps,
			PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		assert ps.size() > 2 && ctx.previouslyCalculatedRoute != null;
		List<RouteSegmentPoint> pnts = new ArrayList<RouteSegmentPoint>();
		for (int i = 0; i < ps.size(); i++) {
			RouteCalcResult err = initSegmentPnt(ctx, ps.get(i), i, pnts);
			if (err != null) {
				return err;
			}
		}
		List<RouteSegmentResult> firstPartRecalculatedRoute = null;
		List<RouteSegmentResult> restPartRecalculatedRoute = null;
		List<RouteSegmentResult> prev = ctx.previouslyCalculatedRoute;
		RouteSegmentPoint first = pnts.get(1);
		for (int i = 0; i < prev.size(); i++) {
			RouteSegmentResult rsr = prev.get(i);
			if (first.getRoad().id == rsr.getObject().getId()) {
				if (MapUtils.measuredDist31(rsr.getEndPointX(), rsr.getEndPointY(), first.getStartPointX(), first.getStartPointY()) < 50) {
					firstPartRecalculatedRoute = new ArrayList<RouteSegmentResult>(i + 1);
					restPartRecalculatedRoute = new ArrayList<RouteSegmentResult>(prev.size() - i);
					for (int k = 0; k < prev.size(); k++) {
						if (k <= i) {
							firstPartRecalculatedRoute.add(prev.get(k));
						} else {
							restPartRecalculatedRoute.add(prev.get(k));
						}
					}
					System.out.println("Recalculate only first part of the route");
					break;
				}
			}
		}
		RouteCalcResult results = new RouteCalcResult(new ArrayList<RouteSegmentResult>());
		for (int i = 0; i < pnts.size() - 1; i++) {
			RoutingContext local = new RoutingContext(ctx);
			if (i == 0 && ctx.nativeLib == null) {
				if (useSmartRouteRecalculation) {
					local.previouslyCalculatedRoute = firstPartRecalculatedRoute;
				}
			}
			RouteCalcResult res = searchRouteAndPrepareTurns(local, pnts.get(i), pnts.get(i + 1), routeDirection);
			if (res != null && res.detailed != null) {
				makeStartEndPointsPrecise(res.detailed, pnts.get(i).getPreciseLatLon(), pnts.get(i + 1).getPreciseLatLon());
			}
			results.detailed.addAll(res.detailed);
			ctx.routingTime += local.routingTime;
//			local.unloadAllData(ctx);
			if (restPartRecalculatedRoute != null) {
				results.detailed.addAll(restPartRecalculatedRoute);
				break;
			}
		}
		ctx.unloadAllData();
		return results;

	}

	protected void pringGC(final RoutingContext ctx, boolean before) {
		if (RoutingContext.SHOW_GC_SIZE && before) {
			long h1 = RoutingContext.runGCUsedMemory();
			float mb = (1 << 20);
			log.warn("Used before routing " + h1 / mb + " actual");
		} else if (RoutingContext.SHOW_GC_SIZE && !before) {
			int sz = ctx.global.size;
			log.warn("Subregion size " + ctx.subregionTiles.size() + " " + " tiles " + ctx.indexedSubregions.size());
			long h1 = RoutingContext.runGCUsedMemory();
			ctx.unloadAllData();
			long h2 = RoutingContext.runGCUsedMemory();
			float mb = (1 << 20);
			log.warn("Unload context :  estimated " + sz / mb + " ?= " + (h1 - h2) / mb + " actual");
		}
	}

}
