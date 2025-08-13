package net.osmand.plus.routing;


import static net.osmand.plus.settings.enums.RoutingType.A_STAR_2_PHASE;
import static net.osmand.plus.settings.enums.RoutingType.HH_CPP;
import static net.osmand.plus.settings.enums.RoutingType.HH_JAVA;

import android.os.Bundle;
import android.util.Base64;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.LocationsHolder;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.avoidroads.AvoidRoadsHelper;
import net.osmand.plus.avoidroads.DirectionPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.measurementtool.GpxApproximationHelper;
import net.osmand.plus.measurementtool.GpxApproximationParams;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine.OnlineRoutingResponse;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.enums.ApproximationType;
import net.osmand.plus.settings.enums.RoutingType;
import net.osmand.router.*;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingContext;
import net.osmand.router.TurnType;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.gpx.GPXUtilities.Route;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import org.json.JSONException;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import btools.routingapp.IBRouterService;


public class RouteProvider {

	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(RouteProvider.class);
	private static final int MIN_STRAIGHT_DIST = 50000;

	private final GpxRouteHelper gpxRouteHelper = new GpxRouteHelper(this);

	public static Location createLocation(@NonNull WptPt pt) {
		Location loc = new Location("OsmandRouteProvider");
		loc.setLatitude(pt.lat);
		loc.setLongitude(pt.lon);
		loc.setSpeed((float) pt.speed);
		if (!Double.isNaN(pt.ele)) {
			loc.setAltitude(pt.ele);
		}
		loc.setTime(pt.time);
		if (!Double.isNaN(pt.hdop)) {
			loc.setAccuracy((float) pt.hdop);
		}
		return loc;
	}

	public static Location createLocation(net.osmand.shared.gpx.primitives.WptPt pt){
		Location loc = new Location("OsmandRouteProvider");
		loc.setLatitude(pt.getLatitude());
		loc.setLongitude(pt.getLongitude());
		loc.setSpeed((float) pt.getSpeed());
		if(!Double.isNaN(pt.getEle())) {
			loc.setAltitude(pt.getEle());
		}
		loc.setTime(pt.getTime());
		if(!Double.isNaN(pt.getHdop())) {
			loc.setAccuracy((float) pt.getHdop());
		}
		return loc;
	}

	public static List<Location> locationsFromWpts(List<WptPt> wpts) {
		List<Location> locations = new ArrayList<>(wpts.size());
		for (WptPt pt : wpts) {
			locations.add(createLocation(pt));
		}
		return locations;
	}

	public static List<Location> locationsFromSharedWpts(List<net.osmand.shared.gpx.primitives.WptPt> wpts) {
		List<Location> locations = new ArrayList<>(wpts.size());
		for (net.osmand.shared.gpx.primitives.WptPt pt : wpts) {
			locations.add(createLocation(pt));
		}
		return locations;
	}

	public RouteCalculationResult calculateRouteImpl(@NonNull RouteCalculationParams params) {
		long time = System.currentTimeMillis();
		if (params.start != null && params.end != null) {
			params.calculationProgress.routeCalculationStartTime = time;
			if (log.isInfoEnabled()) {
				log.info("Start finding route from " + params.start + " to " + params.end + " using " +
						params.mode.getRouteService().getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			try {
				RouteCalculationResult res;
				boolean calcGPXRoute = shouldCalculateGpxRoute(params);
				if (calcGPXRoute && !params.gpxRoute.calculateOsmAndRoute) {
					res = gpxRouteHelper.calculateGpxRoute(params);
				} else if (params.mode.getRouteService() == RouteService.OSMAND) {
					res = findVectorMapsRoute(params, calcGPXRoute);
					if (params.calculationProgress.missingMapsCalculationResult != null) {
						res.setMissingMapsCalculationResult(params.calculationProgress.missingMapsCalculationResult);
					}
				} else if (params.mode.getRouteService() == RouteService.BROUTER) {
					res = findBROUTERRoute(params);
				} else if (params.mode.getRouteService() == RouteService.ONLINE) {
					boolean useFallbackRouting = false;
					try {
						res = findOnlineRoute(params);
					} catch (IOException | JSONException e) {
						res = new RouteCalculationResult(null);
						params.initialCalculation = false;
						useFallbackRouting = true;
					}
					if (useFallbackRouting || !res.isCalculated()) {
						OnlineRoutingHelper helper = params.ctx.getOnlineRoutingHelper();
						String engineKey = params.mode.getRoutingProfile();
						OnlineRoutingEngine engine = helper.getEngineByKey(engineKey);
						if (engine != null && engine.useRoutingFallback()) {
							res = findVectorMapsRoute(params, calcGPXRoute);
						}
					}
				} else if (params.mode.getRouteService() == RouteService.STRAIGHT ||
						params.mode.getRouteService() == RouteService.DIRECT_TO) {
					res = findStraightRoute(params);
				} else {
					res = new RouteCalculationResult("Selected route service is not available");
				}
				if (log.isInfoEnabled()) {
					log.info("Finding route contained " + res.getImmutableAllLocations().size() + " points for " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				return res;
			} catch (IOException | ParserConfigurationException | SAXException e) {
				log.error("Failed to find route ", e);
			}
		}
		return new RouteCalculationResult(null);
	}

	private boolean shouldCalculateGpxRoute(@NonNull RouteCalculationParams params) {
		if (params.gpxRoute != null) {
			GpxApproximationParams approximationParams = params.gpxRoute.approximationParams;
			if (approximationParams != null && !params.gpxRoute.gpxFile.isAttachedToRoads()) {
				GpxFile gpxFile = GpxApproximationHelper.approximateGpxSync(params.ctx, params.gpxRoute.gpxFile, approximationParams);
				if (gpxFile.getError() == null && gpxFile.isAttachedToRoads()) {
					params.gpxRoute = new GPXRouteParamsBuilder(gpxFile, params.gpxRoute).build(params.ctx, params.end);
				}
			}
			return params.gpxRoute != null && (!params.gpxRoute.points.isEmpty()
					|| (params.gpxRoute.reverse && !params.gpxRoute.routePoints.isEmpty()));
		}
		return false;
	}

	public RouteCalculationResult recalculatePartOfflineRoute(RouteCalculationResult res, RouteCalculationParams params) {
		RouteCalculationResult rcr = params.previousToRecalculate;
		List<Location> locs = new ArrayList<Location>(rcr.getRouteLocations());
		try {
			int[] startI = {0};
			int[] endI = {locs.size()};
			locs = findStartAndEndLocationsFromRoute(locs, params.start, params.end, startI, endI);
			List<RouteDirectionInfo> directions = calcDirections(startI[0], endI[0], rcr.getRouteDirections());
			gpxRouteHelper.insertInitialSegment(params, locs, directions, true);
			res = new RouteCalculationResult(locs, directions, params, null, true);
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
		}
		return res;
	}


	protected List<RouteDirectionInfo> calcDirections(int startIndex, int endIndex,
                                                      List<RouteDirectionInfo> inputDirections) {
		List<RouteDirectionInfo> directions = new ArrayList<RouteDirectionInfo>();
		if (inputDirections != null) {
			for (RouteDirectionInfo info : inputDirections) {
				if (info.routePointOffset >= startIndex && info.routePointOffset < endIndex) {
					RouteDirectionInfo ch = new RouteDirectionInfo(info.getAverageSpeed(), info.getTurnType());
					ch.routePointOffset = info.routePointOffset - startIndex;
					if(info.routeEndPointOffset != 0) {
						ch.routeEndPointOffset = info.routeEndPointOffset - startIndex;
					}
					ch.setDescriptionRoute(info.getDescriptionRoutePart());
					ch.setRouteDataObject(info.getRouteDataObject());
					// Issue #2894
					if (info.getRef() != null && !"null".equals(info.getRef())) {
						ch.setRef(info.getRef());
					}
					if (info.getStreetName() != null && !"null".equals(info.getStreetName())) {
						ch.setStreetName(info.getStreetName());
					}
					if (info.getDestinationName() != null && !"null".equals(info.getDestinationName())) {
						ch.setDestinationName(info.getDestinationName());
					}

					directions.add(ch);
				}
			}
		}
		return directions;
	}

	protected ArrayList<Location> findStartAndEndLocationsFromRoute(List<Location> route, Location startLoc, LatLon endLoc, int[] startI, int[] endI) {
		float minDist = Integer.MAX_VALUE;
		int start = 0;
		int end = route.size();
		if (startLoc != null) {
			for (int i = 0; i < route.size(); i++) {
				float d = route.get(i).distanceTo(startLoc);
				if (d < minDist) {
					start = i;
					minDist = d;
				}
			}
//		} else {
//			startLoc = route.get(0); // no more used
		}
		Location l = new Location("temp"); //$NON-NLS-1$
		l.setLatitude(endLoc.getLatitude());
		l.setLongitude(endLoc.getLongitude());
		minDist = Integer.MAX_VALUE;
		// get in reverse order taking into account ways with cycle
		for (int i = route.size() - 1; i >= start; i--) {
			float d = route.get(i).distanceTo(l);
			if (d < minDist) {
				end = i + 1;
				// slightly modify to allow last point to be added
				minDist = d - 40;
			}
		}
		ArrayList<Location> sublist = new ArrayList<Location>(route.subList(start, end));
		if(startI != null) {
			startI[0] = start;
		}
		if(endI != null) {
			endI[0] = end;
		}
		return sublist;
	}

	public RoutingEnvironment getRoutingEnvironment(OsmandApplication ctx, ApplicationMode mode, LatLon start, LatLon end) throws IOException {
		RouteCalculationParams params = new RouteCalculationParams();
		params.ctx = ctx;
		params.mode = mode;
		params.start = new Location("", start.getLatitude(), start.getLongitude());
		params.end = end;
		return calculateRoutingEnvironment(params, false, true);
	}

	public List<GpxPoint> generateGpxPoints(RoutingEnvironment env, GpxRouteApproximation gctx, LocationsHolder locationsHolder) {
		return env.getRouter().generateGpxPoints(gctx, locationsHolder);
	}

	public GpxRouteApproximation calculateGpxPointsApproximation(RoutingEnvironment env, GpxRouteApproximation gctx, List<GpxPoint> points, ResultMatcher<GpxRouteApproximation> resultMatcher, boolean useExternalTimestamps) throws IOException, InterruptedException {
		return env.getRouter().searchGpxRoute(gctx, points, resultMatcher, useExternalTimestamps);
	}

	protected RoutingEnvironment calculateRoutingEnvironment(RouteCalculationParams params, boolean calcGPXRoute, boolean skipComplex) throws IOException {
		BinaryMapIndexReader[] files = params.ctx.getResourceManager().getRoutingMapFiles();
		RoutePlannerFrontEnd router = new RoutePlannerFrontEnd();

		OsmandSettings settings = params.ctx.getSettings();

		RoutePlannerFrontEnd.CALCULATE_MISSING_MAPS = !OsmandSettings.IGNORE_MISSING_MAPS;

		RoutingType routingType = settings.ROUTING_TYPE.getModeValue(params.mode);
		if (routingType.isHHRouting()) {
			router.setDefaultHHRoutingConfig();
			router.setHHRouteCpp(routingType == HH_CPP);
		} else {
			router.setHHRoutingConfig(null);
		}

		ApproximationType approximationType = settings.APPROXIMATION_TYPE.getModeValue(params.mode);
		router.setUseNativeApproximation(approximationType.isNativeApproximation());
		router.setUseGeometryBasedApproximation(approximationType.isGeoApproximation());

		RoutingConfiguration.Builder config = params.ctx.getRoutingConfigForMode(params.mode);
		GeneralRouter generalRouter = params.ctx.getRouter(config, params.mode);
		if (generalRouter == null) {
			return null;
		}
		RoutingConfiguration cf = initOsmAndRoutingConfig(config, params, settings, generalRouter);
		if (cf == null) {
			return null;
		}
		PrecalculatedRouteDirection precalculated = null;
		if (calcGPXRoute) {
			ArrayList<Location> sublist = findStartAndEndLocationsFromRoute(params.gpxRoute.points,
					params.start, params.end, null, null);
			LatLon[] latLon = new LatLon[sublist.size()];
			for (int k = 0; k < latLon.length; k++) {
				latLon[k] = new LatLon(sublist.get(k).getLatitude(), sublist.get(k).getLongitude());
			}
			precalculated = PrecalculatedRouteDirection.build(latLon, generalRouter.getMaxSpeed());
			precalculated.setFollowNext(true);
			//cf.planRoadDirection = 1;
		}
		// BUILD context
		NativeOsmandLibrary lib = settings.SAFE_MODE.get() ? null : NativeOsmandLibrary.getLoadedLibrary();
		// check loaded files
		int leftX = MapUtils.get31TileNumberX(params.start.getLongitude());
		int rightX = leftX;
		int bottomY = MapUtils.get31TileNumberY(params.start.getLatitude());
		int topY = bottomY;
		if (params.intermediates != null) {
			for (LatLon l : params.intermediates) {
				leftX = Math.min(MapUtils.get31TileNumberX(l.getLongitude()), leftX);
				rightX = Math.max(MapUtils.get31TileNumberX(l.getLongitude()), rightX);
				bottomY = Math.max(MapUtils.get31TileNumberY(l.getLatitude()), bottomY);
				topY = Math.min(MapUtils.get31TileNumberY(l.getLatitude()), topY);
			}
		}
		LatLon l = params.end;
		leftX = Math.min(MapUtils.get31TileNumberX(l.getLongitude()), leftX);
		rightX = Math.max(MapUtils.get31TileNumberX(l.getLongitude()), rightX);
		bottomY = Math.max(MapUtils.get31TileNumberY(l.getLatitude()), bottomY);
		topY = Math.min(MapUtils.get31TileNumberY(l.getLatitude()), topY);

		params.ctx.getResourceManager().getRenderer().checkInitialized(15, lib, leftX, rightX, bottomY, topY);

		RoutingContext ctx = router.buildRoutingContext(cf, lib, files, RouteCalculationMode.NORMAL);
		ctx.leftSideNavigation = params.leftSide;
		ctx.calculationProgress = params.calculationProgress;
		ctx.publicTransport = params.inPublicTransportMode;
		ctx.startTransportStop = params.startTransportStop;
		ctx.targetTransportStop = params.targetTransportStop;
		if (params.previousToRecalculate != null && params.onlyStartPointChanged) {
			int currentRoute = params.previousToRecalculate.getCurrentRoute();
			List<RouteSegmentResult> originalRoute = params.previousToRecalculate.getOriginalRoute();
			if (originalRoute != null && currentRoute < originalRoute.size()) {
				ctx.previouslyCalculatedRoute = originalRoute.subList(currentRoute, originalRoute.size());
			}
		}
		boolean complex = !skipComplex && params.mode.isDerivedRoutingFrom(ApplicationMode.CAR)
				&& (routingType == A_STAR_2_PHASE || routingType == HH_JAVA || routingType == HH_CPP)
				&& precalculated == null && router.getRecalculationEnd(ctx) == null;

		RoutingContext complexCtx = null;
		if (complex) {
			complexCtx = router.buildRoutingContext(cf, lib, files, RouteCalculationMode.COMPLEX);
			complexCtx.calculationProgress = params.calculationProgress;
			complexCtx.leftSideNavigation = params.leftSide;
			complexCtx.previouslyCalculatedRoute = ctx.previouslyCalculatedRoute;
		}
		return new RoutingEnvironment(router, ctx, complexCtx, precalculated);
	}

	protected RouteCalculationResult findVectorMapsRoute(RouteCalculationParams params, boolean calcGPXRoute) throws IOException {
		RoutingEnvironment env = calculateRoutingEnvironment(params, calcGPXRoute, false);
		if (env == null) {
			return applicationModeNotSupported(params);
		}
		LatLon st = new LatLon(params.start.getLatitude(), params.start.getLongitude());
		LatLon en = new LatLon(params.end.getLatitude(), params.end.getLongitude());
		List<LatLon> inters = new ArrayList<>();
		if (params.intermediates != null) {
			inters = new ArrayList<>(params.intermediates);
		}
		return calcOfflineRouteImpl(params, env.getRouter(), env.getCtx(), env.getComplexCtx(), st, en, inters, env.getPrecalculated());
	}

	private RoutingConfiguration initOsmAndRoutingConfig(Builder builder, RouteCalculationParams params, OsmandSettings settings,
	                                                     GeneralRouter generalRouter) {
		Map<String, String> paramsR = new LinkedHashMap<String, String>();
		for (Map.Entry<String, RoutingParameter> e : RoutingHelperUtils.getParametersForDerivedProfile(params.mode, generalRouter).entrySet()) {
			String key = e.getKey();
			RoutingParameter pr = e.getValue();
			String vl;
			if (key.equals(GeneralRouter.USE_SHORTEST_WAY)) {
				boolean bool = !settings.FAST_ROUTE_MODE.getModeValue(params.mode);
				vl = bool ? "true" : null;
			} else if (pr.getType() == RoutingParameterType.BOOLEAN) {
				CommonPreference<Boolean> pref = settings.getCustomRoutingBooleanProperty(key, pr.getDefaultBoolean());
				Boolean bool = pref.getModeValue(params.mode);
				vl = bool ? "true" : null;
			} else {
				vl = settings.getCustomRoutingProperty(key, pr.getDefaultString()).getModeValue(params.mode);
			}
			if (vl != null && vl.length() > 0) {
				paramsR.put(key, vl);
			}
		}
		Float defaultSpeed = params.mode.getDefaultSpeed();
		if (defaultSpeed > 0) {
			paramsR.put(GeneralRouter.DEFAULT_SPEED, String.valueOf(defaultSpeed));
		}
		Float minSpeed = params.mode.getMinSpeed();
		if (minSpeed > 0) {
			paramsR.put(GeneralRouter.MIN_SPEED, String.valueOf(minSpeed));
		}
		Float maxSpeed = params.mode.getMaxSpeed();
		if (maxSpeed > 0) {
			paramsR.put(GeneralRouter.MAX_SPEED, String.valueOf(maxSpeed));
		}
		OsmandApplication app = settings.getContext();
		DirectionPointsHelper helper = app.getAvoidSpecificRoads().getPointsHelper();
		builder.setDirectionPoints(helper.getDirectionPoints(params.mode));

		float mb = (1 << 20);
		Runtime rt = Runtime.getRuntime();
		// make visible
		int memoryLimitMb = (int) (0.95 * ((rt.maxMemory() - rt.totalMemory()) + rt.freeMemory()) / mb);
		int nativeMemoryLimitMb = settings.MEMORY_ALLOCATED_FOR_ROUTING.get();
		RoutingMemoryLimits memoryLimits = new RoutingMemoryLimits(memoryLimitMb, nativeMemoryLimitMb);
		log.warn("Use " + memoryLimitMb + " MB Free " + rt.freeMemory() / mb + " of " + rt.totalMemory() / mb + " max " + rt.maxMemory() / mb);
		log.warn("Use " + nativeMemoryLimitMb + " MB of native memory ");
		String derivedProfile = params.mode.getDerivedProfile();
		String routingProfile = "default".equals(derivedProfile) ? params.mode.getRoutingProfile() : derivedProfile;
		Double direction = params.start.hasBearing() ? params.start.getBearing() / 180d * Math.PI : null;

		RoutingConfiguration configuration = builder.build(routingProfile, direction, memoryLimits, paramsR);
		if (settings.ENABLE_TIME_CONDITIONAL_ROUTING.getModeValue(params.mode)) {
			configuration.routeCalculationTime = System.currentTimeMillis();
		}
		configuration.showMinorTurns = settings.SHOW_MINOR_TURNS.getModeValue(params.mode);

		return configuration;
	}

	private RouteCalculationResult calcOfflineRouteImpl(RouteCalculationParams params,
	                                                    RoutePlannerFrontEnd router, RoutingContext ctx, RoutingContext complexCtx, LatLon st, LatLon en,
	                                                    List<LatLon> inters, PrecalculatedRouteDirection precalculated) throws IOException {
		try {
			RouteResultPreparation.RouteCalcResult result = null;
			if (complexCtx != null) {
				try {
					result = router.searchRoute(complexCtx, st, en, inters, precalculated);
					// discard ctx and replace with calculated
					ctx = complexCtx;
				} catch(RuntimeException e) {
					params.ctx.runInUIThread(() -> {
						log.error("Runtime error: " + e.getMessage(), e);
						params.ctx.showToastMessage(R.string.complex_route_calculation_failed, e.getMessage());
					});
				}
			}
			if (result == null) {
				result = router.searchRoute(ctx, st, en, inters);
			}

			if (result == null || result.getList().isEmpty()) {
				if(ctx.calculationProgress.segmentNotFound == 0) {
					return new RouteCalculationResult(params.ctx.getString(R.string.starting_point_too_far));
				} else if(ctx.calculationProgress.segmentNotFound == inters.size() + 1) {
					return new RouteCalculationResult(params.ctx.getString(R.string.ending_point_too_far));
				} else if(ctx.calculationProgress.segmentNotFound > 0) {
					return new RouteCalculationResult(params.ctx.getString(R.string.intermediate_point_too_far, "'" + ctx.calculationProgress.segmentNotFound + "'"));
				} else if (ctx.calculationProgress.directSegmentQueueSize == 0) {
					return new RouteCalculationResult("Route can not be found from start point (" + ctx.calculationProgress.distanceFromBegin / 1000f + " km)");
				} else if (ctx.calculationProgress.reverseSegmentQueueSize == 0) {
					return new RouteCalculationResult("Route can not be found from end point (" + ctx.calculationProgress.distanceFromEnd / 1000f + " km)");
				} else if (ctx.calculationProgress.isCancelled) {
					return interrupted();
				} else if(result != null && !Algorithms.isEmpty(result.getError())) {
					return new RouteCalculationResult(result.getError());
				}
				// something really strange better to see that message on the scren
				return emptyResult();
			} else {
				return new RouteCalculationResult(result.getList(), params, ctx,
						params.gpxRoute == null ? null : params.gpxRoute.wpt, true);
			}
		} catch (RuntimeException e) {
			log.error("Runtime error: " + e.getMessage(), e);
			return new RouteCalculationResult(e.getMessage() );
		} catch (InterruptedException e) {
			log.error("Interrupted: " + e.getMessage(), e);
			return interrupted();
		} catch (OutOfMemoryError e) {
//			ActivityManager activityManager = (ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
//			ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
//			activityManager.getMemoryInfo(memoryInfo);
//			int avl = (int) (memoryInfo.availMem / (1 << 20));
			int max = (int) (Runtime.getRuntime().maxMemory() / (1 << 20));
			int avl = (int) (Runtime.getRuntime().freeMemory() / (1 << 20));
			String s = " (" + avl + " MB available of " + max  + ") ";
			return new RouteCalculationResult("Not enough process memory "+ s);
		}
	}

	private RouteCalculationResult applicationModeNotSupported(RouteCalculationParams params) {
		return new RouteCalculationResult("Application mode '"+ params.mode.toHumanString()+ "' is not supported.");
	}

	private RouteCalculationResult interrupted() {
		return new RouteCalculationResult("Route calculation was interrupted");
	}

	private RouteCalculationResult emptyResult() {
		return new RouteCalculationResult("Empty result");
	}

	@NonNull
	public static List<RouteSegmentResult> parseOsmAndGPXRoute(List<Location> points, GpxFile gpxFile,
	                                                           List<Location> segmentEndpoints,
	                                                           int selectedSegment) {
		return parseOsmAndGPXRoute(points, gpxFile, segmentEndpoints, selectedSegment, false);
	}

	@NonNull
	public static List<RouteSegmentResult> parseOsmAndGPXRoute(List<Location> points, GpxFile gpxFile,
	                                                           List<Location> segmentEndpoints,
	                                                           int selectedSegment, boolean leftSide) {
		GPXFile javaGpx = SharedUtil.jGpxFile(gpxFile);
		List<TrkSegment> segments = javaGpx.getNonEmptyTrkSegments(false);
		if (selectedSegment != -1 && segments.size() > selectedSegment) {
			TrkSegment segment = segments.get(selectedSegment);
			points.addAll(locationsFromWpts(segment.points));
			RouteImporter routeImporter = new RouteImporter(segment, javaGpx.getRoutePoints(selectedSegment));
			return routeImporter.importRoute();
		} else {
			collectPointsFromSegments(segments, points, segmentEndpoints);
			RouteImporter routeImporter = new RouteImporter(javaGpx, leftSide);
			return routeImporter.importRoute();
		}
	}

	protected static void collectSegmentPointsFromGpx(GpxFile gpxFile, List<Location> points,
													  List<Location> segmentEndpoints, int selectedSegment) {
		List<net.osmand.shared.gpx.primitives.TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
		if (selectedSegment != -1 && segments.size() > selectedSegment) {
			net.osmand.shared.gpx.primitives.TrkSegment segment = segments.get(selectedSegment);
			points.addAll(locationsFromSharedWpts(segment.getPoints()));
		} else {
			collectPointsFromSharedSegments(segments, points, segmentEndpoints);
		}
	}

	protected static void collectSegmentPointsFromGpx(GPXFile gpxFile, List<Location> points,
													  List<Location> segmentEndpoints, int selectedSegment) {
		List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
		if (selectedSegment != -1 && segments.size() > selectedSegment) {
			TrkSegment segment = segments.get(selectedSegment);
			points.addAll(locationsFromWpts(segment.points));
		} else {
			collectPointsFromSegments(segments, points, segmentEndpoints);
		}
	}

	protected static void collectPointsFromSegments(List<TrkSegment> segments, List<Location> points, List<Location> segmentEndpoints) {
		Location lastPoint = null;
		for (int i = 0; i < segments.size(); i++) {
			TrkSegment segment = segments.get(i);
			points.addAll(locationsFromWpts(segment.points));
			if (i <= segments.size() - 1 && lastPoint != null) {
				segmentEndpoints.add(lastPoint);
				segmentEndpoints.add(points.get((points.size() - segment.points.size())));
			}
			lastPoint = points.get(points.size() - 1);
		}
	}

	protected static void collectPointsFromSharedSegments(List<net.osmand.shared.gpx.primitives.TrkSegment> segments, List<Location> points, List<Location> segmentEndpoints) {
		Location lastPoint = null;
		for (int i = 0; i < segments.size(); i++) {
			net.osmand.shared.gpx.primitives.TrkSegment segment = segments.get(i);
			points.addAll(locationsFromSharedWpts(segment.getPoints()));
			if (i <= segments.size() - 1 && lastPoint != null) {
				segmentEndpoints.add(lastPoint);
				segmentEndpoints.add(points.get((points.size() - segment.getPoints().size())));
			}
			lastPoint = points.get(points.size() - 1);
		}
	}

	protected static List<RouteDirectionInfo> parseOsmAndGPXRoute(List<Location> points, GpxFile gpxFile,
																  List<Location> segmentEndpoints,
																  boolean osmandRouter, boolean leftSide,
																  float defSpeed, int selectedSegment) {
		GPXFile javaGpx = SharedUtil.jGpxFile(gpxFile);
		List<RouteDirectionInfo> directions = null;
		if (!osmandRouter) {
			for (WptPt pt : javaGpx.getPoints()) {
				points.add(createLocation(pt));
			}
		} else {
			collectSegmentPointsFromGpx(javaGpx, points, segmentEndpoints, selectedSegment);
		}
		float[] distanceToEnd = new float[points.size()];
		for (int i = points.size() - 2; i >= 0; i--) {
			distanceToEnd[i] = distanceToEnd[i + 1] + points.get(i).distanceTo(points.get(i + 1));
		}

		Route route = null;
		if (javaGpx.routes.size() > 0) {
			route = javaGpx.routes.get(0);
		}
		RouteDirectionInfo previous = null;
		if (route != null && route.points.size() > 0) {
			directions = new ArrayList<RouteDirectionInfo>();
			Iterator<WptPt> iterator = route.points.iterator();
			float lasttime = 0;
			while(iterator.hasNext()){
				WptPt item = iterator.next();
				try {
					String stime = item.getExtensionsToRead().get("time");
					int time  = 0;
					if (stime != null) {
						time = Integer.parseInt(stime);
					}
					int offset = Integer.parseInt(item.getExtensionsToRead().get("offset")); //$NON-NLS-1$
					if(directions.size() > 0) {
						RouteDirectionInfo last = directions.get(directions.size() - 1);
						// update speed using time and idstance
						if (distanceToEnd.length > last.routePointOffset && distanceToEnd.length > offset) {
							float lastDistanceToEnd = distanceToEnd[last.routePointOffset];
							float currentDistanceToEnd = distanceToEnd[offset];
							if (lasttime != 0) {
								last.setAverageSpeed((lastDistanceToEnd - currentDistanceToEnd) / lasttime);
							}
							last.distance = Math.round(lastDistanceToEnd - currentDistanceToEnd);
						}
					}
					// save time as a speed because we don't know distance of the route segment
					lasttime = time;
					float avgSpeed = defSpeed;
					if (!iterator.hasNext() && time > 0 && distanceToEnd.length > offset) {
						avgSpeed = distanceToEnd[offset] / time;
					}
					String stype = item.getExtensionsToRead().get("turn"); //$NON-NLS-1$
					TurnType turnType;
					if (stype != null) {
						turnType = TurnType.fromString(stype.toUpperCase(), leftSide);
					} else {
						turnType = TurnType.straight();
					}
					String sturn = item.getExtensionsToRead().get("turn-angle"); //$NON-NLS-1$
					if (sturn != null) {
						turnType.setTurnAngle((float) Double.parseDouble(sturn));
					}
					String slanes = item.getExtensionsToRead().get("lanes");
					if (slanes != null) {
						try {
							int[] lanes = CollectionUtils.stringToArray(slanes);
							if (lanes != null && lanes.length > 0) {
								turnType.setLanes(lanes);
							}
						} catch (NumberFormatException e) {
							// ignore
						}
					}
					RouteDirectionInfo dirInfo = new RouteDirectionInfo(avgSpeed, turnType);
					dirInfo.setDescriptionRoute(item.desc); //$NON-NLS-1$
					dirInfo.routePointOffset = offset;

					// Issue #2894
					String sref = item.getExtensionsToRead().get("ref"); //$NON-NLS-1$
					if (sref != null && !"null".equals(sref)) {
						dirInfo.setRef(sref); //$NON-NLS-1$
					}
					String sstreetname = item.getExtensionsToRead().get("street-name"); //$NON-NLS-1$
					if (sstreetname != null && !"null".equals(sstreetname)) {
						dirInfo.setStreetName(sstreetname); //$NON-NLS-1$
					}
					String sdest = item.getExtensionsToRead().get("dest"); //$NON-NLS-1$
					if (sdest != null && !"null".equals(sdest)) {
						dirInfo.setDestinationName(sdest); //$NON-NLS-1$
					}

					if (previous != null && TurnType.C != previous.getTurnType().getValue() &&
							!osmandRouter) {
						// calculate angle
						if (previous.routePointOffset > 0) {
							float paz = points.get(previous.routePointOffset - 1).bearingTo(points.get(previous.routePointOffset));
							float caz;
							if (previous.getTurnType().isRoundAbout() && dirInfo.routePointOffset < points.size() - 1) {
								caz = points.get(dirInfo.routePointOffset).bearingTo(points.get(dirInfo.routePointOffset + 1));
							} else {
								caz = points.get(dirInfo.routePointOffset - 1).bearingTo(points.get(dirInfo.routePointOffset));
							}
							float angle = caz - paz;
							if (angle < 0) {
								angle += 360;
							} else if (angle > 360) {
								angle -= 360;
							}
							// that magic number helps to fix some errors for turn
							angle += 75;

							if (previous.getTurnType().getTurnAngle() < 0.5f) {
								previous.getTurnType().setTurnAngle(angle);
							}
						}
					}
					directions.add(dirInfo);

					previous = dirInfo;
				} catch (IllegalArgumentException e) {
					log.info("Exception", e);
				}
			}
		}
		if (previous != null && TurnType.C != previous.getTurnType().getValue()) {
			// calculate angle
			if (previous.routePointOffset > 0 && previous.routePointOffset < points.size() - 1) {
				float paz = points.get(previous.routePointOffset - 1).bearingTo(points.get(previous.routePointOffset));
				float caz = points.get(previous.routePointOffset).bearingTo(points.get(points.size() - 1));
				float angle = caz - paz;
				if (angle < 0) {
					angle += 360;
				}
				if (previous.getTurnType().getTurnAngle() < 0.5f) {
					previous.getTurnType().setTurnAngle(angle);
				}
			}
		}
		return directions;
	}

	public GpxFile createOsmandRouterGPX(RouteCalculationResult route, OsmandApplication ctx, String name) {
		TargetPointsHelper helper = ctx.getTargetPointsHelper();
		List<net.osmand.shared.gpx.primitives.WptPt> points = new ArrayList<>();
		List<TargetPoint> ps = helper.getIntermediatePointsWithTarget();
		for (int k = 0; k < ps.size(); k++) {
			net.osmand.shared.gpx.primitives.WptPt pt = new net.osmand.shared.gpx.primitives.WptPt();
			pt.setLat(ps.get(k).getLatitude());
			pt.setLon(ps.get(k).getLongitude());
			if (k < ps.size()) {
				pt.setName(ps.get(k).getOnlyName());
				if (k == ps.size() - 1) {
					String target = ctx.getString(R.string.destination_point, "");
					if (pt.getName() != null && pt.getName().startsWith(target)) {
						pt.setName(ctx.getString(R.string.destination_point, pt.getName()));
					}
				} else {
					String prefix = (k + 1) +". ";
					if(Algorithms.isEmpty(pt.getName())) {
						pt.setName(ctx.getString(R.string.target_point, pt.getName()));
					}
					if (pt.getName().startsWith(prefix)) {
						pt.setName(prefix + pt.getName());
					}
				}
				pt.setDesc(pt.getName());
			}
			points.add(pt);
		}

		List<Location> locations = route.getImmutableAllLocations();
		List<RouteSegmentResult> originalRoute = route.getOriginalRoute();
		RouteExporter exporter = new RouteExporter(name, originalRoute, locations, null, points);

		return exporter.exportRoute();
	}

	private RouteCalculationResult findOnlineRoute(RouteCalculationParams params) throws IOException, JSONException {
		OsmandApplication app = params.ctx;
		OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
		OsmandSettings settings = app.getSettings();
		String engineKey = params.mode.getRoutingProfile();
		OnlineRoutingResponse response =
				helper.calculateRouteOnline(engineKey, getPathFromParams(params), params);

		if (response != null) {
			if (response.getGpxFile() != null) {
				GPXRouteParamsBuilder builder = new GPXRouteParamsBuilder(response.getGpxFile(), settings);
				builder.setCalculatedRouteTimeSpeed(response.hasCalculatedTimeSpeed());
				params.gpxFile = response.getGpxFile();
				params.gpxRoute = builder.build(app);
				return gpxRouteHelper.calculateGpxRoute(params);
			}
			List<Location> route = response.getRoute();
			List<RouteDirectionInfo> directions = response.getDirections();
			if (!Algorithms.isEmpty(route) && !Algorithms.isEmpty(directions)) {
				params.intermediates = null;
				return new RouteCalculationResult(route, directions, params, null, false);
			}
		} else {
			params.initialCalculation = false;
		}

		return new RouteCalculationResult("Route is empty");
	}

	private static List<LatLon> getPathFromParams(RouteCalculationParams params) {
		List<LatLon> points = new ArrayList<>();
		points.add(new LatLon(params.start.getLatitude(), params.start.getLongitude()));
		if (!Algorithms.isEmpty(params.intermediates)) {
			points.addAll(params.intermediates);
		}
		points.add(params.end);
		return points;
	}

	@NonNull
	protected RouteCalculationResult findBROUTERRoute(@NonNull RouteCalculationParams params) throws
			IOException, ParserConfigurationException, FactoryConfigurationError, SAXException {
		boolean addMissingTurns = true;
		Bundle brouterParams = getBRouterParams(params);

		OsmandApplication app = params.ctx;
		List<Location> res = new ArrayList<>();
		List<RouteDirectionInfo> infos = new ArrayList<>();
		List<Location> segmentEndpoints = new ArrayList<>();

		IBRouterService brouterService = app.getBRouterService();
		if (brouterService == null) {
			brouterService = app.reconnectToBRouter();
			if (brouterService == null) {
				return new RouteCalculationResult("BRouter service is not available");
			}
		}
		try {
			String gpxMessage = brouterService.getTrackFromParams(brouterParams);
			if (gpxMessage == null) {
				gpxMessage = "no result from brouter";
			}
			boolean isZ64Encoded = gpxMessage.startsWith("ejY0"); // base-64 version of "z64"
			if (!(isZ64Encoded || gpxMessage.startsWith("<"))) {
				return new RouteCalculationResult(gpxMessage);
			}
			InputStream gpxStream;
			if (isZ64Encoded) {
				ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(gpxMessage, Base64.DEFAULT));
				bais.read(new byte[3]); // skip prefix
				gpxStream = new GZIPInputStream(bais);
			} else {
				gpxStream = new ByteArrayInputStream(gpxMessage.getBytes(StandardCharsets.UTF_8));
			}
			GpxFile gpxFile = SharedUtil.loadGpxFile(gpxStream);
			infos = parseOsmAndGPXRoute(res, gpxFile, segmentEndpoints, true, params.leftSide, params.mode.getDefaultSpeed(), -1);
			if (infos != null) {
				addMissingTurns = false;
			}
		} catch (Exception e) {
			return new RouteCalculationResult("Exception calling BRouter: " + e); //$NON-NLS-1$
		}
		return new RouteCalculationResult(res, infos, params, null, addMissingTurns);
	}

	@NonNull
	private Bundle getBRouterParams(@NonNull RouteCalculationParams params) {
		int numpoints = 2 + (params.intermediates != null ? params.intermediates.size() : 0);
		double[] lats = new double[numpoints];
		double[] lons = new double[numpoints];
		int index = 0;
		String mode;
		lats[index] = params.start.getLatitude();
		lons[index] = params.start.getLongitude();
		index++;
		if (params.intermediates != null && params.intermediates.size() > 0) {
			for (LatLon il : params.intermediates) {
				lats[index] = il.getLatitude();
				lons[index] = il.getLongitude();
				index++;
			}
		}
		lats[index] = params.end.getLatitude();
		lons[index] = params.end.getLongitude();

		AvoidRoadsHelper avoidRoadsHelper = params.ctx.getAvoidSpecificRoads();
		Set<LatLon> impassableRoads = avoidRoadsHelper.getImpassableRoadsCoordinates();
		double[] nogoLats = new double[impassableRoads.size()];
		double[] nogoLons = new double[impassableRoads.size()];
		double[] nogoRadi = new double[impassableRoads.size()];

		if (impassableRoads.size() != 0) {
			int nogoindex = 0;
			for (LatLon nogos : impassableRoads) {
				nogoLats[nogoindex] = nogos.getLatitude();
				nogoLons[nogoindex] = nogos.getLongitude();
				nogoRadi[nogoindex] = 10;
				nogoindex++;
			}
		}
		if (params.mode.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
			mode = "foot"; //$NON-NLS-1$
		} else if (params.mode.isDerivedRoutingFrom(ApplicationMode.BICYCLE)) {
			mode = "bicycle"; //$NON-NLS-1$
		} else {
			mode = "motorcar"; //$NON-NLS-1$
		}
		Bundle bundle = new Bundle();
		bundle.putDoubleArray("lats", lats);
		bundle.putDoubleArray("lons", lons);
		bundle.putDoubleArray("nogoLats", nogoLats);
		bundle.putDoubleArray("nogoLons", nogoLons);
		bundle.putDoubleArray("nogoRadi", nogoRadi);
		bundle.putString("fast", params.fast ? "1" : "0");
		bundle.putString("v", mode);
		bundle.putString("trackFormat", "gpx");
		bundle.putString("turnInstructionFormat", "osmand");
		bundle.putString("acceptCompressedResult", "true");

		String osmandProfileName = params.mode.getUserProfileName();
		if (osmandProfileName.indexOf("Brouter") == 0) {
			if (osmandProfileName.contains("[") && osmandProfileName.contains("]")) {
				String brouterProfileName = osmandProfileName.substring(osmandProfileName.indexOf("[") + 1, osmandProfileName.indexOf("]"));

				// log.info (" BROUTER_PROFILE_NAME = " + brouterProfileName );
				if (brouterProfileName.length() > 0) {
					//  set the profile-name in the new parameter "profile" to transmit the profile-name to the brouter
					bundle.putString("profile", brouterProfileName);
				}
			}
		}
		return bundle;
	}

	protected RouteCalculationResult findStraightRoute(@NonNull RouteCalculationParams params) {
		LinkedList<Location> points = new LinkedList<>();
		List<Location> segments = new ArrayList<>();
		points.add(new Location("pnt", params.start.getLatitude(), params.start.getLongitude()));
		if (params.intermediates != null) {
			for (LatLon l : params.intermediates) {
				points.add(new Location(params.extraIntermediates ? "" : "pnt", l.getLatitude(), l.getLongitude()));
			}
			if (params.extraIntermediates) {
				params.intermediates = null;
			}
		}
		points.add(new Location("", params.end.getLatitude(), params.end.getLongitude()));
		Location lastAdded = null;
		float speed = params.mode.getDefaultSpeed();
		List<RouteDirectionInfo> computeDirections = new ArrayList<>();
		while (!points.isEmpty()) {
			Location pl = points.peek();
			if (lastAdded == null || lastAdded.distanceTo(pl) < MIN_STRAIGHT_DIST) {
				lastAdded = points.poll();
				if (lastAdded != null && lastAdded.getProvider().equals("pnt")) {
					RouteDirectionInfo previousInfo = new RouteDirectionInfo(speed, TurnType.straight());
					previousInfo.routePointOffset = segments.size();
					previousInfo.setDescriptionRoute(params.ctx.getString(R.string.route_head));
					computeDirections.add(previousInfo);
				}
				segments.add(lastAdded);
			} else {
				if (pl != null) {
					Location mp = MapUtils.calculateMidPoint(lastAdded, pl);
					points.add(0, mp);
				}
			}
		}
		return new RouteCalculationResult(segments, computeDirections, params, null, params.extraIntermediates);
	}
}
