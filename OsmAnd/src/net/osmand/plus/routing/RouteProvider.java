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
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXUtilities.Route;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.avoidroads.AvoidRoadsHelper;
import net.osmand.plus.avoidroads.DirectionPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
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
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import net.osmand.router.GpxRouteApproximation;
import net.osmand.router.MissingMapsCalculationResult;
import net.osmand.router.PrecalculatedRouteDirection;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RouteExporter;
import net.osmand.router.RouteImporter;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.RouteCalculationMode;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.router.RoutingConfiguration.RoutingMemoryLimits;
import net.osmand.router.RoutingContext;
import net.osmand.router.TurnType;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.MapUtils;

import org.json.JSONException;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import btools.routingapp.IBRouterService;


public class RouteProvider {

	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(RouteProvider.class);
	private static final int MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT = 60;
	private static final int ADDITIONAL_DISTANCE_FOR_START_POINT = 300;
	private static final int MIN_STRAIGHT_DIST = 50000;
	private static final int MIN_INTERMEDIATE_DIST = 10;
	private static final int NEAREST_POINT_EXTRA_SEARCH_DISTANCE = 300;

	public static Location createLocation(WptPt pt){
		Location loc = new Location("OsmandRouteProvider");
		loc.setLatitude(pt.lat);
		loc.setLongitude(pt.lon);
		loc.setSpeed((float) pt.speed);
		if(!Double.isNaN(pt.ele)) {
			loc.setAltitude(pt.ele);
		}
		loc.setTime(pt.time);
		if(!Double.isNaN(pt.hdop)) {
			loc.setAccuracy((float) pt.hdop);
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

	public RouteCalculationResult calculateRouteImpl(RouteCalculationParams params) {
		long time = System.currentTimeMillis();
		if (params.start != null && params.end != null) {
			params.calculationProgress.routeCalculationStartTime = time;
			if (log.isInfoEnabled()) {
				log.info("Start finding route from " + params.start + " to " + params.end + " using " +
						params.mode.getRouteService().getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			try {
				RouteCalculationResult res;
				boolean calcGPXRoute = params.gpxRoute != null && (!params.gpxRoute.points.isEmpty()
						|| (params.gpxRoute.reverse && !params.gpxRoute.routePoints.isEmpty()));
				if (calcGPXRoute && !params.gpxRoute.calculateOsmAndRoute) {
					res = calculateGpxRoute(params);
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

	public RouteCalculationResult recalculatePartOfflineRoute(RouteCalculationResult res, RouteCalculationParams params) {
		RouteCalculationResult rcr = params.previousToRecalculate;
		List<Location> locs = new ArrayList<Location>(rcr.getRouteLocations());
		try {
			int[] startI = {0};
			int[] endI = {locs.size()};
			locs = findStartAndEndLocationsFromRoute(locs, params.start, params.end, startI, endI);
			List<RouteDirectionInfo> directions = calcDirections(startI[0], endI[0], rcr.getRouteDirections());
			insertInitialSegment(params, locs, directions, true);
			res = new RouteCalculationResult(locs, directions, params, null, true);
		} catch (RuntimeException e) {
			log.error(e.getMessage(), e);
		}
		return res;
	}

	private RouteCalculationResult calculateGpxRoute(RouteCalculationParams routeParams) throws IOException {
		GPXRouteParams gpxParams = routeParams.gpxRoute;
		boolean calcWholeRoute = routeParams.gpxRoute.passWholeRoute && (routeParams.previousToRecalculate == null || !routeParams.onlyStartPointChanged);
		boolean calculateOsmAndRouteParts = gpxParams.calculateOsmAndRouteParts;
		List<RouteSegmentResult> gpxRouteResult = routeParams.gpxRoute.route;

		if (!Algorithms.isEmpty(gpxRouteResult)) {
			if (!gpxParams.calculatedRouteTimeSpeed) {
				calculateGpxRouteTimeSpeed(routeParams, gpxRouteResult);
			}
			if ((calcWholeRoute && !calculateOsmAndRouteParts)
					|| routeParams.mode.getRouteService() == RouteService.ONLINE) {
				return new RouteCalculationResult(gpxRouteResult, routeParams.start, routeParams.end,
						routeParams.intermediates, routeParams.ctx, routeParams.leftSide, null,
						gpxParams.wpt, routeParams.mode, true, routeParams.initialCalculation);
			}
			RouteCalculationResult result = new RouteCalculationResult(gpxRouteResult,
					routeParams.start, routeParams.end, routeParams.intermediates, routeParams.ctx,
					routeParams.leftSide, null, gpxParams.wpt, routeParams.mode, false, routeParams.initialCalculation);
			List<Location> gpxRouteLocations = result.getImmutableAllLocations();
			int nearestGpxPointInd = calcWholeRoute ? 0 : findNearestGpxPointIndexFromRoute(gpxRouteLocations, routeParams.start, calculateOsmAndRouteParts);
			Location nearestGpxLocation = null;
			Location gpxLastLocation = !gpxRouteLocations.isEmpty() ? gpxRouteLocations.get(gpxRouteLocations.size() - 1) : null;
			List<RouteSegmentResult> firstSegmentRoute = null;
			List<RouteSegmentResult> lastSegmentRoute = null;
			List<RouteSegmentResult> gpxRoute;

			if (nearestGpxPointInd > 0) {
				nearestGpxLocation = gpxRouteLocations.get(nearestGpxPointInd);

			} else if (!gpxRouteLocations.isEmpty()) {
				nearestGpxLocation = gpxRouteLocations.get(0);
			}
			if (calculateOsmAndRouteParts && !Algorithms.isEmpty(gpxParams.segmentEndpoints)) {
				gpxRoute = findRouteWithIntermediateSegments(routeParams, result, gpxRouteLocations, gpxParams.segmentEndpoints, nearestGpxPointInd);
			} else {
				if (nearestGpxPointInd > 0) {
					gpxRoute = result.getOriginalRoute(nearestGpxPointInd, false);
					if (!Algorithms.isEmpty(gpxRoute)) {
						LatLon startPoint = gpxRoute.get(0).getStartPoint();
						nearestGpxLocation = new Location("", startPoint.getLatitude(), startPoint.getLongitude());
					} else {
						nearestGpxLocation = new Location("", routeParams.end.getLatitude(), routeParams.end.getLongitude());
					}
				} else {
					gpxRoute = result.getOriginalRoute();
				}
			}

			if (calculateOsmAndRouteParts
					&& routeParams.start != null && nearestGpxLocation != null
					&& nearestGpxLocation.distanceTo(routeParams.start) > MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT) {
				RouteCalculationResult firstSegmentResult = findOfflineRouteSegment(
						routeParams, routeParams.start, new LatLon(nearestGpxLocation.getLatitude(), nearestGpxLocation.getLongitude()));
				firstSegmentRoute = firstSegmentResult.getOriginalRoute();
			}
			if (calculateOsmAndRouteParts
					&& routeParams.end != null && gpxLastLocation != null
					&& MapUtils.getDistance(gpxLastLocation.getLatitude(), gpxLastLocation.getLongitude(),
					routeParams.end.getLatitude(), routeParams.end.getLongitude()) > MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT) {
				RouteCalculationResult lastSegmentResult = findOfflineRouteSegment(routeParams, gpxLastLocation, routeParams.end);
				lastSegmentRoute = lastSegmentResult.getOriginalRoute();
			}
			List<RouteSegmentResult> newGpxRoute = new ArrayList<>();
			if (firstSegmentRoute != null && !firstSegmentRoute.isEmpty()) {
				newGpxRoute.addAll(firstSegmentRoute);
			}
			newGpxRoute.addAll(gpxRoute);
			if (lastSegmentRoute != null && !lastSegmentRoute.isEmpty()) {
				newGpxRoute.addAll(lastSegmentRoute);
			}

			if (routeParams.recheckRouteNearestPoint()) {
				newGpxRoute = checkNearestSegmentOnRecalculate(
						routeParams.previousToRecalculate, newGpxRoute, routeParams.start);
			}

			return new RouteCalculationResult(newGpxRoute, routeParams.start, routeParams.end,
					routeParams.intermediates, routeParams.ctx, routeParams.leftSide, null,
					gpxParams.wpt, routeParams.mode, true, routeParams.initialCalculation);
		}

		if (routeParams.gpxRoute.useIntermediatePointsRTE) {
			return calculateOsmAndRouteWithIntermediatePoints(routeParams, gpxParams.points,
					gpxParams.connectPointsStraightly);
		}

		List<Location> gpxRoute;
		int[] startI = {0};
		int[] endI = {gpxParams.points.size()};
		if (calcWholeRoute) {
			gpxRoute = gpxParams.points;
		} else {
			gpxRoute = findStartAndEndLocationsFromRoute(gpxParams.points,
					routeParams.start, routeParams.end, startI, endI);
		}
		List<RouteDirectionInfo> inputDirections = gpxParams.directions;
		List<RouteDirectionInfo> gpxDirections = calcDirections(startI[0], endI[0], inputDirections);
		insertIntermediateSegments(routeParams, gpxRoute, gpxDirections, gpxParams.segmentEndpoints, calculateOsmAndRouteParts);
		insertInitialSegment(routeParams, gpxRoute, gpxDirections, calculateOsmAndRouteParts);
		insertFinalSegment(routeParams, gpxRoute, gpxDirections, calculateOsmAndRouteParts);

		if (routeParams.recheckRouteNearestPoint()) {
			int index = findNearestPointIndexOnRecalculate(routeParams.previousToRecalculate, gpxRoute, routeParams.start);
			if (index > 0) {
				gpxDirections = calcDirections(index, gpxRoute.size(), gpxDirections);
				gpxRoute = gpxRoute.subList(index, gpxRoute.size());
			}
		}

		for (RouteDirectionInfo info : gpxDirections) {
			// recalculate
			info.distance = 0;
			info.afterLeftTime = 0;
		}

		return new RouteCalculationResult(gpxRoute, gpxDirections, routeParams,
				gpxParams.wpt, routeParams.gpxRoute.addMissingTurns);
	}

	private void calculateGpxRouteTimeSpeed(RouteCalculationParams params, List<RouteSegmentResult> gpxRouteResult) throws IOException {
		RoutingEnvironment env = calculateRoutingEnvironment(params, false, true);
		if (env != null) {
			RouteResultPreparation.calculateTimeSpeed(env.getCtx(), gpxRouteResult);
		}
	}

	private RouteCalculationResult calculateOsmAndRouteWithIntermediatePoints(RouteCalculationParams routeParams,
																			  List<Location> intermediates,
																			  boolean connectPointsStraightly) throws IOException {
		RouteCalculationParams rp = new RouteCalculationParams();
		rp.calculationProgress = routeParams.calculationProgress;
		rp.ctx = routeParams.ctx;
		rp.mode = routeParams.mode;
		rp.start = routeParams.start;
		rp.end = routeParams.end;
		rp.leftSide = routeParams.leftSide;
		rp.fast = routeParams.fast;
		rp.onlyStartPointChanged = routeParams.onlyStartPointChanged;
		rp.previousToRecalculate = routeParams.previousToRecalculate;
		rp.extraIntermediates = true;
		rp.intermediates = new ArrayList<>();

		int closest = findClosestIntermediate(routeParams, intermediates);
		for (int i = closest; i < intermediates.size(); i++) {
			Location w = intermediates.get(i);
			rp.intermediates.add(new LatLon(w.getLatitude(), w.getLongitude()));
		}
		RouteService routeService = routeParams.mode.getRouteService();
		if (routeService == RouteService.BROUTER) {
			try {
				return findBROUTERRoute(rp);
			} catch (ParserConfigurationException | SAXException e) {
				throw new IOException(e);
			}
		} else if (routeService == RouteService.STRAIGHT || routeService == RouteService.DIRECT_TO || connectPointsStraightly) {
			return findStraightRoute(rp);
		}
		return findVectorMapsRoute(rp, false);
	}

	private int findClosestIntermediate(RouteCalculationParams params, List<Location> intermediates) {
		int closest = 0;
		if (!params.gpxRoute.passWholeRoute) {
			double maxDist = Double.POSITIVE_INFINITY;
			for (int i = 0; i < intermediates.size(); i++) {
				Location loc = intermediates.get(i);
				double dist = MapUtils.getDistance(loc.getLatitude(), loc.getLongitude(),
						params.start.getLatitude(), params.start.getLongitude());
				if (dist <= MIN_INTERMEDIATE_DIST) {
					return i;
				} else if (dist < maxDist) {
					closest = i;
					maxDist = dist;
				}
			}
		}
		return closest;
	}

	private List<RouteDirectionInfo> calcDirections(int startIndex, int endIndex,
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

	private void insertFinalSegment(RouteCalculationParams routeParams, List<Location> points,
			List<RouteDirectionInfo> directions, boolean calculateOsmAndRouteParts) {
		if(points.size() > 0) {
			Location routeEnd = points.get(points.size() - 1);
			LatLon e = routeEnd == null ? null : new LatLon(routeEnd.getLatitude(), routeEnd.getLongitude());
			LatLon finalEnd = routeParams.end;
			if (finalEnd != null && MapUtils.getDistance(finalEnd, e) > MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT) {
				RouteCalculationResult newRes = null;
				if (calculateOsmAndRouteParts) {
					newRes = findOfflineRouteSegment(routeParams, routeEnd, finalEnd);
				}
				List<Location> loct;
				List<RouteDirectionInfo> dt;
				if (newRes != null && newRes.isCalculated()) {
					loct = newRes.getImmutableAllLocations();
					dt = newRes.getImmutableAllDirections();
				} else {
					loct = new ArrayList<Location>();
					Location l = new Location("");
					l.setLatitude(finalEnd.getLatitude());
					l.setLongitude(finalEnd.getLongitude());
					loct.add(l);
					dt = new ArrayList<RouteDirectionInfo>();
				}
				for (RouteDirectionInfo i : dt) {
					i.routePointOffset += points.size();
				}
				points.addAll(loct);
				directions.addAll(dt);
			}
		}
	}

	public void insertInitialSegment(RouteCalculationParams routeParams, List<Location> points,
			List<RouteDirectionInfo> directions, boolean calculateOsmAndRouteParts) {
		Location realStart = routeParams.start;
		if (realStart != null && points.size() > 0 && realStart.distanceTo(points.get(0)) > MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT) {
			Location trackStart = points.get(0);
			RouteCalculationResult newRes = null;
			if (calculateOsmAndRouteParts) {
				LatLon end = new LatLon(trackStart.getLatitude(), trackStart.getLongitude());
				newRes = findOfflineRouteSegment(routeParams, realStart, end);
			}
			List<Location> loct;
			List<RouteDirectionInfo> dt;
			if (newRes != null && newRes.isCalculated()) {
				loct = newRes.getImmutableAllLocations();
				dt = newRes.getImmutableAllDirections();
			} else {
				loct = new ArrayList<Location>();
				loct.add(realStart);
				dt = new ArrayList<RouteDirectionInfo>();
			}
			points.addAll(0, loct);
			directions.addAll(0, dt);
			for (int i = dt.size(); i < directions.size(); i++) {
				directions.get(i).routePointOffset += loct.size();
			}
		}
	}

	public void insertIntermediateSegments(RouteCalculationParams routeParams,
	                                       List<Location> points,
	                                       List<RouteDirectionInfo> directions,
	                                       List<Location> segmentEndpoints,
	                                       boolean calculateOsmAndRouteParts) {
		for (int i = 0; i < segmentEndpoints.size() - 1; i += 2) {
			Location prevSegmentPoint = segmentEndpoints.get(i);
			Location newSegmentPoint = segmentEndpoints.get(i + 1);

			if (prevSegmentPoint.distanceTo(newSegmentPoint) <= MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT) {
				continue;
			}
			int index = points.indexOf(newSegmentPoint);
			if (calculateOsmAndRouteParts && index != -1 && points.contains(prevSegmentPoint)) {
				LatLon end = new LatLon(newSegmentPoint.getLatitude(), newSegmentPoint.getLongitude());
				RouteCalculationResult newRes = findOfflineRouteSegment(routeParams, prevSegmentPoint, end);

				if (newRes != null && newRes.isCalculated()) {
					List<Location> locations = newRes.getImmutableAllLocations();
					List<RouteDirectionInfo> dt = newRes.getImmutableAllDirections();

					for (RouteDirectionInfo directionInfo : dt) {
						directionInfo.routePointOffset += points.size();
					}
					points.addAll(index, locations);
					directions.addAll(dt);
				}
			}
		}
	}

	public List<RouteSegmentResult> findRouteWithIntermediateSegments(RouteCalculationParams routeParams,
	                                                                  RouteCalculationResult result,
	                                                                  List<Location> gpxRouteLocations,
	                                                                  List<Location> segmentEndpoints,
	                                                                  int nearestGpxPointInd) {
		List<RouteSegmentResult> newGpxRoute = new ArrayList<>();

		int lastIndex = nearestGpxPointInd;
		for (int i = 0; i < segmentEndpoints.size() - 1; i += 2) {
			Location prevSegmentPoint = segmentEndpoints.get(i);
			Location newSegmentPoint = segmentEndpoints.get(i + 1);

			if (prevSegmentPoint.distanceTo(newSegmentPoint) <= MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT) {
				continue;
			}
			int indexNew = findNearestGpxPointIndexFromRoute(gpxRouteLocations, newSegmentPoint, false);
			int indexPrev = findNearestGpxPointIndexFromRoute(gpxRouteLocations, prevSegmentPoint, false);
			if (indexPrev != -1 && indexPrev > nearestGpxPointInd && indexNew != -1) {
				List<RouteSegmentResult> route = result.getOriginalRoute(lastIndex, indexPrev, true);
				if (!Algorithms.isEmpty(route)) {
					newGpxRoute.addAll(route);
				}
				lastIndex = indexNew;

				LatLon end = new LatLon(newSegmentPoint.getLatitude(), newSegmentPoint.getLongitude());
				RouteCalculationResult newRes = findOfflineRouteSegment(routeParams, prevSegmentPoint, end);
				List<RouteSegmentResult> segmentResults = newRes.getOriginalRoute();
				if (!Algorithms.isEmpty(segmentResults)) {
					newGpxRoute.addAll(segmentResults);
				}
			}
		}

		List<RouteSegmentResult> route = result.getOriginalRoute(lastIndex);
		if (!Algorithms.isEmpty(route)) {
			newGpxRoute.addAll(route);
		}

		return newGpxRoute;
	}

	private RouteCalculationResult findOfflineRouteSegment(RouteCalculationParams params, Location start, LatLon end) {
		RouteCalculationParams newParams = new RouteCalculationParams();
		newParams.start = start;
		newParams.end = end;
		newParams.ctx = params.ctx;
		newParams.calculationProgress = params.calculationProgress;
		newParams.mode = params.mode;
		newParams.leftSide = params.leftSide;
		RouteCalculationResult newRes = null;
		try {
			RouteService routeService = params.mode.getRouteService();
			if (routeService == RouteService.OSMAND) {
				newRes = findVectorMapsRoute(newParams, false);
			} else if (routeService == RouteService.BROUTER) {
				newRes = findBROUTERRoute(newParams);
			} else if (routeService == RouteService.STRAIGHT || routeService == RouteService.DIRECT_TO) {
				newRes = findStraightRoute(newParams);
			}
		} catch (IOException | SAXException | ParserConfigurationException e) {
			log.info("FindOfflineRouteSegment error", e);
		}
		return newRes;
	}

	private ArrayList<Location> findStartAndEndLocationsFromRoute(List<Location> route, Location startLoc, LatLon endLoc, int[] startI, int[] endI) {
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
		} else {
			startLoc = route.get(0);
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

	private int findNearestGpxPointIndexFromRoute(List<Location> route, Location startLoc, boolean calculateOsmAndRouteParts) {
		float minDist = Integer.MAX_VALUE;
		int nearestPointIndex = 0;
		if (startLoc != null) {
			for (int i = 0; i < route.size(); i++) {
				float d = route.get(i).distanceTo(startLoc);
				if (d < minDist) {
					nearestPointIndex = i;
					minDist = d;
				}
			}
		}
		if (nearestPointIndex > 0 && calculateOsmAndRouteParts) {
			Location nearestLocation = route.get(nearestPointIndex);
			for (int i = nearestPointIndex + 1; i < route.size(); i++) {
				Location nextLocation = route.get(i);
				if (nextLocation.distanceTo(nearestLocation) >= ADDITIONAL_DISTANCE_FOR_START_POINT) {
					return i;
				}
			}
		}
		return nearestPointIndex;
	}

	@NonNull
	private List<RouteSegmentResult> checkNearestSegmentOnRecalculate(@NonNull RouteCalculationResult previousRoute,
	                                                                  @NonNull List<RouteSegmentResult> segments,
	                                                                  @NonNull Location startLocation) {
		float previousDistanceToFinish = previousRoute.getRouteDistanceToFinish(0);
		float searchDistance = previousDistanceToFinish + NEAREST_POINT_EXTRA_SEARCH_DISTANCE;

		float minDistance = Float.POSITIVE_INFINITY;
		float checkedDistance = 0;

		int nearestSegmentIndex = 0;

		for (int segmentIndex = segments.size() - 1; segmentIndex >= 0 && checkedDistance < searchDistance; segmentIndex--) {
			RouteSegmentResult segment = segments.get(segmentIndex);
			int step = segment.isForwardDirection() ? -1 : 1;
			int startIndex = segment.getEndPointIndex() + step;
			int endIndex = segment.getStartPointIndex() + step;

			for (int index = startIndex; index != endIndex && checkedDistance < searchDistance; index += step) {
				LatLon prevRoutePoint = segment.getPoint(index);
				LatLon nextRoutePoint = segment.getPoint(index - step);
				double distance = MapUtils.getOrthogonalDistance(
						startLocation.getLatitude(), startLocation.getLongitude(),
						prevRoutePoint.getLatitude(), prevRoutePoint.getLongitude(),
						nextRoutePoint.getLatitude(), nextRoutePoint.getLongitude());

				if (distance < Math.min(minDistance, MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT)) {
					minDistance = (float) distance;
					nearestSegmentIndex = segmentIndex;
				}

				checkedDistance += MapUtils.getDistance(prevRoutePoint, nextRoutePoint);
			}
		}

		return nearestSegmentIndex > 0 ? segments.subList(nearestSegmentIndex, segments.size()) : segments;
	}

	private int findNearestPointIndexOnRecalculate(@NonNull RouteCalculationResult previousRoute,
	                                               @NonNull List<Location> routeLocations,
	                                               @NonNull Location startLocation) {
		float prevDistanceToFinish = previousRoute.getRouteDistanceToFinish(0);
		float searchDistance = prevDistanceToFinish + NEAREST_POINT_EXTRA_SEARCH_DISTANCE;
		float checkedDistance = 0;
		int newStartIndex = 0;
		float minDistance = Float.POSITIVE_INFINITY;

		for (int i = routeLocations.size() - 2; i >= 0 && checkedDistance < searchDistance; i--) {
			Location prevRoutePoint = routeLocations.get(i);
			Location nextRoutePoint = routeLocations.get(i + 1);
			float distance = (float) MapUtils.getOrthogonalDistance(
					startLocation.getLatitude(), startLocation.getLongitude(),
					prevRoutePoint.getLatitude(), prevRoutePoint.getLongitude(),
					nextRoutePoint.getLatitude(), nextRoutePoint.getLongitude());

			if (distance < Math.min(minDistance, MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT)) {
				minDistance = distance;
				newStartIndex = i + 1;
			}

			checkedDistance += prevRoutePoint.distanceTo(nextRoutePoint);
		}

		return newStartIndex;
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

		router.CALCULATE_MISSING_MAPS = !settings.IGNORE_MISSING_MAPS;

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

	private RoutingConfiguration initOsmAndRoutingConfig(Builder config, RouteCalculationParams params, OsmandSettings settings,
	                                                     GeneralRouter generalRouter) throws IOException, FileNotFoundException {
		Map<String, String> paramsR = new LinkedHashMap<String, String>();
		for (Map.Entry<String, RoutingParameter> e : RoutingHelperUtils.getParametersForDerivedProfile(params.mode, generalRouter).entrySet()) {
			String key = e.getKey();
			RoutingParameter pr = e.getValue();
			String vl;
			if (key.equals(GeneralRouter.USE_SHORTEST_WAY)) {
				Boolean bool = !settings.FAST_ROUTE_MODE.getModeValue(params.mode);
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
		config.setDirectionPoints(helper.getDirectionPoints(params.mode));

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
		RoutingConfiguration cf = config.build(routingProfile, params.start.hasBearing() ?
						params.start.getBearing() / 180d * Math.PI : null,
				memoryLimits, paramsR);
		if (settings.ENABLE_TIME_CONDITIONAL_ROUTING.getModeValue(params.mode)) {
			cf.routeCalculationTime = System.currentTimeMillis();
		}
		return cf;
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
				RouteCalculationResult res = new RouteCalculationResult(result.getList(), params.start, params.end,
						params.intermediates, params.ctx, params.leftSide, ctx, params.gpxRoute  == null? null: params.gpxRoute.wpt,
								params.mode, true, params.initialCalculation);
				return res;
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
	public static List<RouteSegmentResult> parseOsmAndGPXRoute(List<Location> points, GPXFile gpxFile,
	                                                           List<Location> segmentEndpoints,
	                                                           int selectedSegment) {
		return parseOsmAndGPXRoute(points, gpxFile, segmentEndpoints, selectedSegment, false);
	}

	@NonNull
	public static List<RouteSegmentResult> parseOsmAndGPXRoute(List<Location> points, GPXFile gpxFile,
	                                                           List<Location> segmentEndpoints,
	                                                           int selectedSegment, boolean leftSide) {
		List<TrkSegment> segments = gpxFile.getNonEmptyTrkSegments(false);
		if (selectedSegment != -1 && segments.size() > selectedSegment) {
			TrkSegment segment = segments.get(selectedSegment);
			points.addAll(locationsFromWpts(segment.points));
			RouteImporter routeImporter = new RouteImporter(segment, gpxFile.getRoutePoints(selectedSegment));
			return routeImporter.importRoute();
		} else {
			collectPointsFromSegments(segments, points, segmentEndpoints);
			RouteImporter routeImporter = new RouteImporter(gpxFile, leftSide);
			return routeImporter.importRoute();
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

	protected static List<RouteDirectionInfo> parseOsmAndGPXRoute(List<Location> points, GPXFile gpxFile,
																  List<Location> segmentEndpoints,
																  boolean osmandRouter, boolean leftSide,
																  float defSpeed, int selectedSegment) {
		List<RouteDirectionInfo> directions = null;
		if (!osmandRouter) {
			for (WptPt pt : gpxFile.getPoints()) {
				points.add(createLocation(pt));
			}
		} else {
			collectSegmentPointsFromGpx(gpxFile, points, segmentEndpoints, selectedSegment);
		}
		float[] distanceToEnd = new float[points.size()];
		for (int i = points.size() - 2; i >= 0; i--) {
			distanceToEnd[i] = distanceToEnd[i + 1] + points.get(i).distanceTo(points.get(i + 1));
		}

		Route route = null;
		if (gpxFile.routes.size() > 0) {
			route = gpxFile.routes.get(0);
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

	public GPXFile createOsmandRouterGPX(RouteCalculationResult route, OsmandApplication ctx, String name) {
		TargetPointsHelper helper = ctx.getTargetPointsHelper();
		List<WptPt> points = new ArrayList<>();
		List<TargetPoint> ps = helper.getIntermediatePointsWithTarget();
		for (int k = 0; k < ps.size(); k++) {
			WptPt pt = new WptPt();
			pt.lat = ps.get(k).getLatitude();
			pt.lon = ps.get(k).getLongitude();
			if (k < ps.size()) {
				pt.name = ps.get(k).getOnlyName() + "";
				if (k == ps.size() - 1) {
					String target = ctx.getString(R.string.destination_point, "");
					if (pt.name.startsWith(target)) {
						pt.name = ctx.getString(R.string.destination_point, pt.name);
					}
				} else {
					String prefix = (k + 1) +". ";
					if(Algorithms.isEmpty(pt.name)) {
						pt.name = ctx.getString(R.string.target_point, pt.name);
					}
					if (pt.name.startsWith(prefix)) {
						pt.name = prefix + pt.name;
					}
				}
				pt.desc = pt.name;
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
				helper.calculateRouteOnline(engineKey, getPathFromParams(params),
						params.start.hasBearing() ? params.start.getBearing() : null,
						params.leftSide, params.initialCalculation, params.calculationProgress);

		if (response != null) {
			if (response.getGpxFile() != null) {
				GPXRouteParamsBuilder builder = new GPXRouteParamsBuilder(response.getGpxFile(), settings);
				builder.setCalculatedRouteTimeSpeed(response.hasCalculatedTimeSpeed());
				params.gpxRoute = builder.build(app);
				return calculateGpxRoute(params);
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
				gpxStream = new ByteArrayInputStream(gpxMessage.getBytes("UTF-8"));
			}
			GPXFile gpxFile = GPXUtilities.loadGPXFile(gpxStream);
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

	private RouteCalculationResult findStraightRoute(@NonNull RouteCalculationParams params) {
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
				if (lastAdded.getProvider().equals("pnt")) {
					RouteDirectionInfo previousInfo = new RouteDirectionInfo(speed, TurnType.straight());
					previousInfo.routePointOffset = segments.size();
					previousInfo.setDescriptionRoute(params.ctx.getString(R.string.route_head));
					computeDirections.add(previousInfo);
				}
				segments.add(lastAdded);
			} else {
				Location mp = MapUtils.calculateMidPoint(lastAdded, pl);
				points.add(0, mp);
			}
		}
		return new RouteCalculationResult(segments, computeDirections, params, null, params.extraIntermediates);
	}
}
