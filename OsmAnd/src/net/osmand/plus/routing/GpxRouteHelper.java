package net.osmand.plus.routing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.RouteSegmentResult;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class GpxRouteHelper {
    private final RouteProvider provider;
    private static final int MIN_INTERMEDIATE_DIST = 10;
    private static final int ADDITIONAL_DISTANCE_FOR_START_POINT = 300;
    private static final int NEAREST_POINT_EXTRA_SEARCH_DISTANCE = 300;
    private static final int MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT = 60;

    private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(GpxRouteHelper.class);

    protected GpxRouteHelper(RouteProvider provider) {
        this.provider = provider;
    }

    protected RouteCalculationResult calculateGpxRoute(@NonNull RouteCalculationParams routeParams) throws IOException {
        GPXRouteParams gpxParams = routeParams.gpxRoute;
        boolean calcWholeRoute = gpxParams.passWholeRoute &&
                (routeParams.previousToRecalculate == null || !routeParams.onlyStartPointChanged);
        boolean calculateOsmAndRouteParts = gpxParams.calculateOsmAndRouteParts;

        boolean followOsmAndRoute = gpxParams.hasOsmAndRoute();
        List<RouteSegmentResult> gpxRouteResult = gpxParams.route;

        if (gpxParams.reverse && followOsmAndRoute) {
            switch (gpxParams.reverseStrategy) {
                case USE_ORIGINAL_GPX -> {
                    followOsmAndRoute = false;
                }
                case RECALCULATE_ALL_ROUTE_POINTS -> {
                    gpxRouteResult = recalculateByRoutePoints(routeParams, false);
                }
                case RECALCULATE_FROM_CLOSEST_ROUTE_POINT -> {
                    gpxRouteResult = recalculateByRoutePoints(routeParams, true);
                }
            }
        }

        if (followOsmAndRoute) {
            if (!gpxParams.calculatedRouteTimeSpeed) {
                calculateGpxRouteTimeSpeed(routeParams, gpxRouteResult);
            }
            if ((calcWholeRoute && !calculateOsmAndRouteParts)
                    || routeParams.mode.getRouteService() == RouteService.ONLINE) {
                return new RouteCalculationResult(gpxRouteResult, routeParams, null, gpxParams.wpt, true);
            }
            RouteCalculationResult result = new RouteCalculationResult(gpxRouteResult, routeParams, null,
                    gpxParams.wpt, false);
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
            if (gpxRoute != null) {
                newGpxRoute.addAll(gpxRoute);
            }
            if (lastSegmentRoute != null && !lastSegmentRoute.isEmpty()) {
                newGpxRoute.addAll(lastSegmentRoute);
            }

            if (routeParams.recheckRouteNearestPoint()) {
                newGpxRoute = checkNearestSegmentOnRecalculate(
                        routeParams.previousToRecalculate, newGpxRoute, routeParams.start);
            }

            return new RouteCalculationResult(newGpxRoute, routeParams, null, gpxParams.wpt, true);
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
            gpxRoute = provider.findStartAndEndLocationsFromRoute(gpxParams.points,
                    routeParams.start, routeParams.end, startI, endI);
        }
        List<RouteDirectionInfo> inputDirections = gpxParams.directions;
        List<RouteDirectionInfo> gpxDirections = provider.calcDirections(startI[0], endI[0], inputDirections);
        insertIntermediateSegments(routeParams, gpxRoute, gpxDirections, gpxParams.segmentEndpoints, calculateOsmAndRouteParts);
        insertInitialSegment(routeParams, gpxRoute, gpxDirections, calculateOsmAndRouteParts);
        insertFinalSegment(routeParams, gpxRoute, gpxDirections, calculateOsmAndRouteParts);

        if (routeParams.recheckRouteNearestPoint()) {
            int index = findNearestPointIndexOnRecalculate(routeParams.previousToRecalculate, gpxRoute, routeParams.start);
            if (index > 0) {
                gpxDirections = provider.calcDirections(index, gpxRoute.size(), gpxDirections);
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
        RoutingEnvironment env = provider.calculateRoutingEnvironment(params, false, true);
        if (env != null) {
            RouteResultPreparation.calculateTimeSpeed(env.getCtx(), gpxRouteResult);
        }
    }

    @NonNull
    private RouteCalculationParams copyRouteCalculationParams(@NonNull RouteCalculationParams routeParams) {
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
        rp.extraIntermediates = routeParams.extraIntermediates;
        rp.intermediates = routeParams.intermediates;
        return rp;
    }

    @NonNull
    private RouteCalculationResult calculateOsmAndRouteWithIntermediatePoints(@NonNull RouteCalculationParams routeParams,
                                                                              @Nullable List<Location> intermediates,
                                                                              boolean connectPointsStraightly) throws IOException {
        RouteCalculationParams rp = copyRouteCalculationParams(routeParams);

        if (Algorithms.isEmpty(rp.intermediates) && intermediates != null) {
            rp.intermediates = new ArrayList<>();
            rp.extraIntermediates = true;

            int closest = findClosestIntermediate(routeParams, intermediates);
            for (int i = closest; i < intermediates.size(); i++) {
                Location w = intermediates.get(i);
                rp.intermediates.add(new LatLon(w.getLatitude(), w.getLongitude()));
            }
        }

        RouteService routeService = routeParams.mode.getRouteService();
        if (routeService == RouteService.BROUTER) {
            try {
                return provider.findBROUTERRoute(rp);
            } catch (ParserConfigurationException | SAXException e) {
                throw new IOException(e);
            }
        } else if (routeService == RouteService.STRAIGHT || routeService == RouteService.DIRECT_TO || connectPointsStraightly) {
            return provider.findStraightRoute(rp);
        }
        return provider.findVectorMapsRoute(rp, false);
    }

    private int findClosestIntermediate(@NonNull RouteCalculationParams params, @NonNull List<Location> intermediates) {
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

    @NonNull
    private List<RouteSegmentResult> recalculateByRoutePoints(@NonNull RouteCalculationParams routeParams,
                                                              boolean optimizeUsingClosestRoutePoint)
            throws IOException {
        List<Location> locations = new ArrayList<>();
        for (WptPt wpt : routeParams.gpxRoute.routePoints) {
            locations.add(new Location("", wpt.getLat(), wpt.getLon()));
        }

        if (locations.size() > 1) {
            int closest = optimizeUsingClosestRoutePoint ? findClosestIntermediate(routeParams, locations) : 0;

            Location start = locations.get(closest);

            Location last = locations.get(locations.size() - 1);
            LatLon end = new LatLon(last.getLatitude(), last.getLongitude());

            List<LatLon> intermediates = new ArrayList<>();
            // catch intermediates between the closest and the last
            for (int i = closest + 1; i < locations.size() - 1; i++) {
                Location intermediate = locations.get(i);
                intermediates.add(new LatLon(intermediate.getLatitude(), intermediate.getLongitude()));
            }

            RouteCalculationParams rp = copyRouteCalculationParams(routeParams);
            rp.start = start;
            rp.end = end;
            rp.intermediates = intermediates;

            RouteCalculationResult route = calculateOsmAndRouteWithIntermediatePoints(rp, null, false);
            return route.getImmutableAllSegments();
        }

        return new ArrayList<>();
    }

    private List<RouteSegmentResult> findRouteWithIntermediateSegments(RouteCalculationParams routeParams,
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

    private void insertFinalSegment(RouteCalculationParams routeParams, List<Location> points,
                                    List<RouteDirectionInfo> directions, boolean calculateOsmAndRouteParts) {
        if(points.size() > 0) {
            Location routeEnd = points.get(points.size() - 1);
            LatLon e = routeEnd == null ? null : new LatLon(routeEnd.getLatitude(), routeEnd.getLongitude());
            LatLon finalEnd = routeParams.end;
            if (finalEnd != null && e != null && MapUtils.getDistance(finalEnd, e) > MIN_DISTANCE_FOR_INSERTING_ROUTE_SEGMENT) {
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

    private void insertIntermediateSegments(RouteCalculationParams routeParams,
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

                checkedDistance += (float) MapUtils.getDistance(prevRoutePoint, nextRoutePoint);
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

    protected void insertInitialSegment(RouteCalculationParams routeParams, List<Location> points,
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
                newRes = provider.findVectorMapsRoute(newParams, false);
            } else if (routeService == RouteService.BROUTER) {
                newRes = provider.findBROUTERRoute(newParams);
            } else if (routeService == RouteService.STRAIGHT || routeService == RouteService.DIRECT_TO) {
                newRes = provider.findStraightRoute(newParams);
            }
        } catch (IOException | SAXException | ParserConfigurationException e) {
            log.info("FindOfflineRouteSegment error", e);
        }
        return newRes;
    }
}
