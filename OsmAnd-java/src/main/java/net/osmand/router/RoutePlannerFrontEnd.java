package net.osmand.router;


import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.map.OsmandRegions;
import org.apache.commons.logging.Log;

import gnu.trove.list.array.TIntArrayList;
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
	public static boolean CALCULATE_MISSING_MAPS = true;
	static boolean TRACE_ROUTING = false;
	private boolean useSmartRouteRecalculation = true;
	private boolean useGeometryBasedApproximation = false;
	private boolean useNativeApproximation = true;
	private boolean useOnlyHHRouting = false;
	private HHRoutingConfig hhRoutingConfig = null;
	private HHRoutingType hhRoutingType = HHRoutingType.JAVA;


	public RoutePlannerFrontEnd() {
	}
	
	public static HHRoutingConfig defaultHHConfig() {
		return HHRoutingConfig.astar(0).calcDetailed(HHRoutingConfig.CALCULATE_ALL_DETAILED)
				.applyCalculateMissingMaps(RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS);
	}
	
	public enum RouteCalculationMode {
		BASE,
		NORMAL,
		COMPLEX
	}

	private enum HHRoutingType {
		JAVA,
		CPP
	}

	public void setHHRouteCpp(boolean cpp) {
		if (cpp) {
			hhRoutingType = HHRoutingType.CPP;
		} else {
			hhRoutingType = HHRoutingType.JAVA;
		}
	}

	public static class GpxPoint {
		public int ind;
		public LatLon loc;
		public int x31, y31;
		public long time = 0;
		public double cumDist;
		public RouteSegmentPoint pnt;
		public List<RouteSegmentResult> routeToTarget;
		public List<RouteSegmentResult> stepBackRoute;
		public int targetInd = -1;
		public boolean straightLine = false;
		public RouteDataObject object;

		public GpxPoint() {
		}

		public RouteSegmentResult getFirstRouteRes() {
			if (routeToTarget == null || routeToTarget.isEmpty()) {
				return null;
			}
			return routeToTarget.get(0);
		}
		
		public RouteSegmentResult getLastRouteRes() {
			if (routeToTarget == null || routeToTarget.isEmpty()) {
				return null;
			}
			return routeToTarget.get(routeToTarget.size() - 1);
		}
		
		public GpxPoint(GpxPoint point) {
			this.ind = point.ind;
			this.loc = point.loc;
			this.object = point.object;
			this.cumDist = point.cumDist;
		}
	}

	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map, RouteCalculationMode rm) {
		if (rm == null) {
			rm = config.router.getProfile() == GeneralRouterProfile.CAR ? RouteCalculationMode.COMPLEX
					: RouteCalculationMode.NORMAL;
		}
		return new RoutingContext(config, nativeLibrary, map, rm);
	}

	public RoutingContext buildRoutingContext(RoutingConfiguration config, NativeLibrary nativeLibrary, BinaryMapIndexReader[] map) {
		return buildRoutingContext(config, nativeLibrary, map, null);
	}


	public static double squareDist(int x1, int y1, int x2, int y2) {
		return MapUtils.squareDist31TileMetric(x1, y1, x2, y2);
	}

	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list) throws IOException {
		return findRouteSegment(lat, lon, ctx, list, false);
	}

	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list, boolean transportStop) throws IOException {
		return findRouteSegment(lat, lon, ctx, list, false, false);
	}
	
	public RouteSegmentPoint findRouteSegment(double lat, double lon, RoutingContext ctx, List<RouteSegmentPoint> list, boolean transportStop, 
			boolean allowDuplications) throws IOException {
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

	public RouteCalcResult searchRoute(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates) throws IOException, InterruptedException {
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

	public RoutePlannerFrontEnd setUseGeometryBasedApproximation(boolean enabled) {
		this.useGeometryBasedApproximation = enabled;
		return this;
	}

	public boolean isUseNativeApproximation() {
		return useNativeApproximation;
	}

	public boolean isUseGeometryBasedApproximation() {
		return useGeometryBasedApproximation;
	}

	public GpxRouteApproximation searchGpxRoute(GpxRouteApproximation gctx, List<GpxPoint> gpxPoints,
	                                            ResultMatcher<GpxRouteApproximation> resultMatcher,
	                                            boolean useExternalTimestamps) throws IOException, InterruptedException {
		if (!isUseNativeApproximation()) {
			gctx.ctx.nativeLib = null; // rare case of C++ routing (setup) -> Online routing -> Java approximation
		}
		return gctx.searchGpxRouteInternal(this, gpxPoints, resultMatcher, useExternalTimestamps);
	}

	public static RouteSegmentResult generateStraightLineSegment(float averageSpeed, List<LatLon> points) {
		RouteRegion reg = new RouteRegion();
		reg.initRouteEncodingRule(0, "highway", RouteResultPreparation.UNMATCHED_HIGHWAY_TYPE);
		RouteDataObject rdo = new RouteDataObject(reg);
		int size = points.size();
		TIntArrayList x = new TIntArrayList(size);
		TIntArrayList y = new TIntArrayList(size);
		double distance = 0;
		double distOnRoadToPass = 0;
		LatLon prev = null;
		for (int i = 0; i < size; i++) {
			LatLon l = points.get(i);
			if (l != null) {
				x.add(MapUtils.get31TileNumberX(l.getLongitude()));
				y.add(MapUtils.get31TileNumberY(l.getLatitude()));
				if (prev != null) {
					double d = MapUtils.getDistance(l, prev);
					distance += d;
					distOnRoadToPass += d / averageSpeed;
				}
			}
			prev = l;
		}
		rdo.pointsX = x.toArray();
		rdo.pointsY = y.toArray();
		rdo.types = new int[] { 0 } ;
		rdo.id = -1;
		RouteSegmentResult segment = new RouteSegmentResult(rdo, 0, rdo.getPointsLength() - 1);
		segment.setSegmentTime((float) distOnRoadToPass);
		segment.setSegmentSpeed(averageSpeed);
		segment.setDistance((float) distance);
		segment.setTurnType(TurnType.straight());
		return segment;
	}

	public List<GpxPoint> generateGpxPoints(GpxRouteApproximation gctx, LocationsHolder locationsHolder) {
		List<GpxPoint> gpxPoints = new ArrayList<>(locationsHolder.getSize());
		GpxPoint prev = null;
		List<LatLon> points = new ArrayList<>();
		for (int i = 0; i < locationsHolder.getSize(); i++) {
			points.add(locationsHolder.getLatLon(i));
		}
		RouteDataObject o = generateStraightLineSegment(0, points).getObject();
		for (int i = 0; i < points.size(); i++) {
			GpxPoint p = new GpxPoint();
			p.ind = i;
			p.time = locationsHolder.getTime(i);
			p.loc = locationsHolder.getLatLon(i);
			p.object = o;
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
		OsmandRegions osmandRegions = PlatformUtil.getOsmandRegions();
		if (CALCULATE_MISSING_MAPS) {
			MissingMapsCalculator calculator = new MissingMapsCalculator(osmandRegions);
			if (calculator.checkIfThereAreMissingMaps(ctx, start, targets, hhRoutingConfig != null)) {
				return new RouteCalcResult(ctx.calculationProgress.missingMapsCalculationResult.getErrorMessage());
			}
		}
		if (needRequestPrivateAccessRouting(ctx, targets)) {
			ctx.calculationProgress.requestPrivateAccessRouting = true;
		}
		if (hhRoutingConfig != null && ctx.calculationMode != RouteCalculationMode.BASE) {
			calculateRegionsWithAllRoutePoints(ctx, osmandRegions, start, targets);
			if (ctx.nativeLib == null || hhRoutingType == HHRoutingType.JAVA) {
				HHNetworkRouteRes r = runHHRoute(ctx, start, targets);
				if ((r != null && r.isCorrect()) || useOnlyHHRouting) {
					return r;
				}
			} else {
				setStartEndToCtx(ctx, start, end, intermediates);
				ctx.calculationProgress.nextIteration();
				RouteCalcResult r = runNativeRouting(ctx, null, hhRoutingConfig);
				ctx.calculationProgress.timeToCalculate = (System.nanoTime() - timeToCalculate);
				RouteResultPreparation.printResults(ctx, start, end, r.detailed);
				if ((!r.detailed.isEmpty() && r.isCorrect()) || useOnlyHHRouting) {
					makeStartEndPointsPrecise(ctx, r, start, end, intermediates);
					return r;
				}
			}
		}

		double maxDistance = MapUtils.getDistance(start, end);
		if (!intermediatesEmpty) {
			LatLon b = start;
			for (LatLon l : intermediates) {
				maxDistance = Math.max(MapUtils.getDistance(b, l), maxDistance);
				b = l;
			}
		}
		if (ctx.calculationMode == RouteCalculationMode.COMPLEX && routeDirection == null
				&& maxDistance > RoutingConfiguration.DEVIATION_RADIUS * 6) {
			ctx.calculationProgress.totalIterations++;
			RoutingContext nctx = buildRoutingContext(ctx.config, ctx.nativeLib, ctx.getMaps(), RouteCalculationMode.BASE);
			nctx.calculationProgress = ctx.calculationProgress;
			RouteCalcResult baseRes = searchRoute(nctx, start, end, intermediates);
			if (baseRes == null || !baseRes.isCorrect()) {
				return baseRes;
			}
			routeDirection = PrecalculatedRouteDirection.build(baseRes.detailed, RoutingConfiguration.DEVIATION_RADIUS, ctx.getRouter().getMaxSpeed());
			ctx.calculationProgressFirstPhase = RouteCalculationProgress.capture(ctx.calculationProgress);
		}
		RouteCalcResult res ;
		if (intermediatesEmpty && ctx.nativeLib != null) {
			setStartEndToCtx(ctx, start, end, intermediates);
			RouteSegmentPoint recalculationEnd = getRecalculationEnd(ctx);
			if (recalculationEnd != null) {
				ctx.initTargetPoint(recalculationEnd);
			}
			if (routeDirection != null) {
				ctx.precalculatedRouteDirection = routeDirection.adopt(ctx);
			} else {
				ctx.precalculatedRouteDirection = null;
			}
			ctx.calculationProgress.nextIteration();
			res = runNativeRouting(ctx, recalculationEnd, null);
			makeStartEndPointsPrecise(ctx, res, start, end, intermediates);
		} else {
			int indexNotFound = 0;
			List<RouteSegmentPoint> points = new ArrayList<RouteSegmentPoint>();
			if (!addSegment(start, ctx, indexNotFound++, points, ctx.startTransportStop)) {
				return new RouteCalcResult("Start point is not located");
			}
			if (intermediates != null) {
				for (LatLon l : intermediates) {
					if (!addSegment(l, ctx, indexNotFound++, points, false)) {
						System.out.println(points.get(points.size() - 1).getRoad().toString());
						return new RouteCalcResult("Intermediate point is not located");
					}
				}
			}
			if (!addSegment(end, ctx, indexNotFound++, points, ctx.targetTransportStop)) {
				return new RouteCalcResult("End point is not located");
			}
			ctx.calculationProgress.nextIteration();
			res = searchRouteImpl(ctx, points, routeDirection);
		}
		ctx.calculationProgress.timeToCalculate = (System.nanoTime() - timeToCalculate);
		RouteResultPreparation.printResults(ctx, start, end, res.detailed);
		return res;
	}

	private void calculateRegionsWithAllRoutePoints(RoutingContext ctx, OsmandRegions osmandRegions,
	                                                LatLon start, List<LatLon> targets) throws IOException {
		Map<String, Integer> regionCounter = new LinkedHashMap<>();

		getRegionsOfPoint(start, regionCounter, osmandRegions);
		for (LatLon target : targets) {
			getRegionsOfPoint(target, regionCounter, osmandRegions);
		}

		int allPoints = 1 + targets.size();
		List<String> result = new ArrayList<>();

		for (String region : regionCounter.keySet()) {
			if (regionCounter.get(region) == allPoints) {
				result.add(region);
			}
		}

		ctx.regionsCoveringStartAndTargets = result.toArray(new String[0]);
	}

	private void getRegionsOfPoint(LatLon ll, Map<String, Integer> regionCounter, OsmandRegions or) throws IOException {
		List<BinaryMapDataObject> foundRegions = or.getRegionsToDownload(ll.getLatitude(), ll.getLongitude());
		for (BinaryMapDataObject region : foundRegions) {
			String name = or.getDownloadName(region);
			regionCounter.put(name, regionCounter.getOrDefault(name, 0) + 1);
		}
	}

	private void setStartEndToCtx(final RoutingContext ctx, LatLon start, LatLon end, List<LatLon> intermediates) {
		boolean intermediatesEmpty = intermediates == null || intermediates.isEmpty();
		ctx.startX = MapUtils.get31TileNumberX(start.getLongitude());
		ctx.startY = MapUtils.get31TileNumberY(start.getLatitude());
		ctx.targetX = MapUtils.get31TileNumberX(end.getLongitude());
		ctx.targetY = MapUtils.get31TileNumberY(end.getLatitude());
		if (!intermediatesEmpty) {
			ctx.intermediatesX = new int[intermediates.size()];
			ctx.intermediatesY = new int[intermediates.size()];
			for (int i = 0; i < intermediates.size(); i++) {
				LatLon l = intermediates.get(i);
				ctx.intermediatesX[i] = MapUtils.get31TileNumberX(l.getLongitude());
				ctx.intermediatesY[i] = MapUtils.get31TileNumberY(l.getLatitude());
			}
		} else {
			ctx.intermediatesX = new int[0];
			ctx.intermediatesY = new int[0];
		}
	}

	private HHNetworkRouteRes runHHRoute(RoutingContext ctx, LatLon start, List<LatLon> targets)
			throws IOException, InterruptedException {
		HHRoutePlanner<NetworkDBPoint> routePlanner = HHRoutePlanner.create(ctx);
		HHNetworkRouteRes r = null;
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
		ctx.unloadAllData(); // clean indexedSubregions is required for BRP-fallback
		ctx.routingTime = r != null ? (float) r.getHHRoutingDetailed() : 0;
		return r;
	}

	private HHNetworkRouteRes calculateHHRoute(HHRoutePlanner<NetworkDBPoint> routePlanner, RoutingContext ctx,
			LatLon start, LatLon end, Double dir) throws InterruptedException, IOException {
		NativeLibrary nativeLib = ctx.nativeLib;
		ctx.nativeLib = null; // keep null to interfere with detailed 
		try {
			HHRoutingConfig cfg = HHRoutePlanner.prepareDefaultRoutingConfig(hhRoutingConfig);
			cfg.INITIAL_DIRECTION = dir;
			HHNetworkRouteRes res = routePlanner.runRouting(start, end, cfg);
			if (res != null && res.error == null) {
				ctx.calculationProgress.hhIteration(HHIteration.DONE);
				makeStartEndPointsPrecise(ctx, res, start, end, new ArrayList<LatLon>());
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

	protected void makeStartEndPointsPrecise(RoutingContext ctx, RouteCalcResult res, LatLon start, LatLon end, List<LatLon> intermediates) {
		if (res.detailed.size() > 0) {
			makeSegmentPointPrecise(ctx, res.detailed.get(0), start, true);
			makeSegmentPointPrecise(ctx, res.detailed.get(res.detailed.size() - 1), end, false);
		}
	}

	protected double projectDistance(List<RouteSegmentResult> res, int k, int px, int py) {
		RouteSegmentResult sr = res.get(k);
		RouteDataObject r = sr.getObject();
		QuadPointDouble pp = MapUtils.getProjectionPoint31(px, py,
				r.getPoint31XTile(sr.getStartPointIndex()), r.getPoint31YTile(sr.getStartPointIndex()),
				r.getPoint31XTile(sr.getEndPointIndex()), r.getPoint31YTile(sr.getEndPointIndex()));
		double currentsDist = squareDist((int) pp.x, (int) pp.y, px, py);
		return currentsDist;
	}

	public void makeSegmentPointPrecise(RoutingContext ctx, RouteSegmentResult routeSegmentResult, LatLon point, boolean st) {
		int px = MapUtils.get31TileNumberX(point.getLongitude());
		int py = MapUtils.get31TileNumberY(point.getLatitude());
		int pind = st ? routeSegmentResult.getStartPointIndex() : routeSegmentResult.getEndPointIndex();

		RouteDataObject r = new RouteDataObject(routeSegmentResult.getObject());
		routeSegmentResult.setObject(r);
		QuadPointDouble before = null;
		QuadPointDouble after = null;
		if (pind > 0) {
			before = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(pind - 1),
					r.getPoint31YTile(pind - 1), r.getPoint31XTile(pind), r.getPoint31YTile(pind));
		}
		if (pind < r.getPointsLength() - 1) {
			after = MapUtils.getProjectionPoint31(px, py, r.getPoint31XTile(pind + 1),
					r.getPoint31YTile(pind + 1), r.getPoint31XTile(pind), r.getPoint31YTile(pind));
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
			// correct distance
			RouteResultPreparation.calculateTimeSpeed(ctx, routeSegmentResult);
		}
	}

	private boolean addSegment(LatLon s, RoutingContext ctx, int indexNotFound, List<RouteSegmentPoint> res, boolean transportStop) throws IOException {
		RouteSegmentPoint f = findRouteSegment(s.getLatitude(), s.getLongitude(), ctx, null, transportStop);
		if (f == null) {
			ctx.calculationProgress.segmentNotFound = indexNotFound;
			return false;
		} else {
			log.info("Route segment found " + f.road);
			res.add(f);
			return true;
		}
	}

	RouteCalcResult searchRouteInternalPrepare(final RoutingContext ctx, RouteSegmentPoint start, RouteSegmentPoint end,
	                                                  PrecalculatedRouteDirection routeDirection) throws IOException, InterruptedException {
		RouteSegmentPoint recalculationEnd = getRecalculationEnd(ctx);
		if (recalculationEnd != null) {
			ctx.initStartAndTargetPoints(start, recalculationEnd);
		} else {
			ctx.initStartAndTargetPoints(start, end);
		}
		if (routeDirection != null) {
			ctx.precalculatedRouteDirection = routeDirection.adopt(ctx);
		} else {
			ctx.precalculatedRouteDirection = null;
		}
		if (ctx.nativeLib != null) {
			ctx.startX = start.preciseX;
			ctx.startY = start.preciseY;
			ctx.startRoadId = start.road.id;
			ctx.startSegmentInd  = start.segStart;
			ctx.targetX = end.preciseX;
			ctx.targetY = end.preciseY;
			ctx.targetRoadId = end.road.id;
			ctx.targetSegmentInd  = end.segStart;
			return runNativeRouting(ctx, recalculationEnd, null);
		} else {
			refreshProgressDistance(ctx);
			// Split into 2 methods to let GC work in between
			ctx.finalRouteSegment = new BinaryRoutePlanner().searchRouteInternal(ctx, start, recalculationEnd != null ? recalculationEnd : end, null);
			RouteResultPreparation rrp = new RouteResultPreparation();
			// 4. Route is found : collect all segments and prepare result
			List<RouteSegmentResult> result  = rrp.convertFinalSegmentToResults(ctx, ctx.finalRouteSegment);
			addPrecalculatedToResult(recalculationEnd, result);
			return rrp.prepareResult(ctx, result);
		}
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
						RouteSegment segment = new RouteSegment(rr.getObject(), rr.getStartPointIndex(),rr.getEndPointIndex());
						previous.setParentRoute(segment);
						previous = segment;
					} else {
						recalculationEnd = new RouteSegmentPoint(rr.getObject(), rr.getStartPointIndex(), 0);
						if (Math.abs(rr.getEndPointIndex() - rr.getStartPointIndex()) > 1) {
							RouteSegment segment = new RouteSegment(rr.getObject(), recalculationEnd.segEnd, rr.getEndPointIndex());
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

	private RouteCalcResult runNativeRouting(final RoutingContext ctx, RouteSegment recalculationEnd, HHRoutingConfig hhConfig) throws IOException {
		refreshProgressDistance(ctx);
		if (recalculationEnd != null) {
			if (TRACE_ROUTING) {
				log.info("RecalculationEnd = " + recalculationEnd.road + " ind=" + recalculationEnd.getSegmentStart() + "->" + recalculationEnd.getSegmentEnd());
			}
		}
		RouteRegion[] regions = ctx.reverseMap.keySet().toArray(new RouteRegion[0]);
		// long time = System.currentTimeMillis();
		if (ctx.intermediatesX == null || ctx.intermediatesY == null) {
			ctx.intermediatesX = new int[0];
			ctx.intermediatesY = new int[0];
		}
		RouteSegmentResult[] res = ctx.nativeLib.runNativeRouting(ctx, hhConfig, regions, ctx.calculationMode == RouteCalculationMode.BASE);
		if (TRACE_ROUTING) {
			log.info("Native routing result!");
			for (RouteSegmentResult r : res) {
				log.info("Road = " + r.getObject().id / 64 + " " + r.getStartPointIndex() + "->" + r.getEndPointIndex());
			}
		}
		//	log.info("Native routing took " + (System.currentTimeMillis() - time) / 1000f + " seconds");
		List<RouteSegmentResult> result = new ArrayList<>(Arrays.asList(res));
		if (TRACE_ROUTING) {
			log.info("RecalculationEnd result!");
		}
		addPrecalculatedToResult(recalculationEnd, result);
		ctx.routingTime += ctx.calculationProgress.routingCalculatedTime;
		return new RouteResultPreparation().prepareResult(ctx, result);
	}

	private void addPrecalculatedToResult(RouteSegment recalculationEnd, List<RouteSegmentResult> result) {
		if (recalculationEnd != null) {
			log.info("Native routing use precalculated route");
			RouteSegment current = recalculationEnd;
			if (!hasSegment(result, current)) {
				if (TRACE_ROUTING) {
					log.info("Add recalculationEnd to result = " + current.getRoad() + " " + current.getSegmentStart() + "->" + current.getSegmentEnd());
				}
				result.add(new RouteSegmentResult(current.getRoad(), current.getSegmentStart(), current.getSegmentEnd()));
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

	private RouteCalcResult searchRouteImpl(final RoutingContext ctx, List<RouteSegmentPoint> points, PrecalculatedRouteDirection routeDirection)
			throws IOException, InterruptedException {
		if (points.size() <= 2) {
			// simple case 2 points only
			if (!useSmartRouteRecalculation) {
				ctx.previouslyCalculatedRoute = null;
			}
			pringGC(ctx, true);
			RouteCalcResult res = searchRouteInternalPrepare(ctx, points.get(0), points.get(1), routeDirection);
			pringGC(ctx, false);
			makeStartEndPointsPrecise(ctx, res, points.get(0).getPreciseLatLon(), points.get(1).getPreciseLatLon(), null);
			return res;
		}

		ArrayList<RouteSegmentResult> firstPartRecalculatedRoute = null;
		ArrayList<RouteSegmentResult> restPartRecalculatedRoute = null;
		if (ctx.previouslyCalculatedRoute != null) {
			List<RouteSegmentResult> prev = ctx.previouslyCalculatedRoute;
			long id = points.get(1).getRoad().id;
			int ss = points.get(1).getSegmentStart();
			int px = points.get(1).getRoad().getPoint31XTile(ss);
			int py = points.get(1).getRoad().getPoint31YTile(ss);
			for (int i = 0; i < prev.size(); i++) {
				RouteSegmentResult rsr = prev.get(i);
				if (id == rsr.getObject().getId()) {
					if (MapUtils.getDistance(rsr.getPoint(rsr.getEndPointIndex()), MapUtils.get31LatitudeY(py),
							MapUtils.get31LongitudeX(px)) < 50) {
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
		}
		RouteCalcResult results = new RouteCalcResult(new ArrayList<RouteSegmentResult>());
		for (int i = 0; i < points.size() - 1; i++) {
			RoutingContext local = new RoutingContext(ctx);
			if (i == 0 && ctx.nativeLib == null) {
				if (useSmartRouteRecalculation) {
					local.previouslyCalculatedRoute = firstPartRecalculatedRoute;
				}
			}
			RouteCalcResult res = searchRouteInternalPrepare(local, points.get(i), points.get(i + 1), routeDirection);
			makeStartEndPointsPrecise(local, res, points.get(i).getPreciseLatLon(), points.get(i + 1).getPreciseLatLon(), null);
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

	private void pringGC(final RoutingContext ctx, boolean before) {
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
