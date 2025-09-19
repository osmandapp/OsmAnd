package net.osmand.router.transport;

import net.osmand.data.*;
import net.osmand.router.*;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

public class TransportRoutePlannerTest {
    @Test
    public void buildRoute_noStartStops_returnsEmptyList() throws IOException, InterruptedException {
        // Arrange
        final var start = new LatLon(0, 0);
        final var end = new LatLon(1, 1);
        GeneralRouter generalRouter = mock(GeneralRouter.class);
        when(generalRouter.build(any())).thenReturn(generalRouter);
        when(generalRouter.getIntAttribute(anyString(), anyInt())).thenReturn(0);
        GeneralRouter.RouteAttributeContext attributeContext = mock(GeneralRouter.RouteAttributeContext.class);
        when(generalRouter.getObjContext(any())).thenReturn(attributeContext);
        final var config = new TransportRoutingConfiguration(generalRouter, new HashMap<>());
        final var context = mock(ITransportRoutingContext.class);
        when(context.getTransportStops(any(LatLon.class))).thenReturn(Collections.emptyList());
        when(context.getCfg()).thenReturn(config);
        when(context.getVisitedSegments()).thenReturn(new gnu.trove.map.hash.TLongObjectHashMap<>());
        final var distanceProvider = mock(DistanceProvider.class);

        // Act
        final var planner = new TransportRoutePlanner(distanceProvider);
        List<TransportRouteResult> results = planner.buildRoute(context, start, end);

        // Assert
        assertEquals(Collections.emptyList(), results);
    }

    @Test
    public void buildRoute_simpleDirectRouteNoWalking_returnsOneResult() throws IOException, InterruptedException {
        // Arrange
        final var start = new LatLon(0, 0);
        final var end = new LatLon(1, 1);

        // 1. Create real stops for start and end
        final var startStop = new TransportStop();
        startStop.setLocation(start.getLatitude(), start.getLatitude()); // same location as start point
        startStop.setName("start");
        final var endStop = new TransportStop();
        endStop.setLocation(end.getLatitude(), end.getLatitude()); // same location as end point
        startStop.setName("stop");

        // 2. Create a real route, give it a non-zero ID, and add both stops to it
        final var route = new TransportRoute();
        final var routeType = "someType";
        route.setType(routeType);
        final var routeId = 1L;
        route.setId(routeId); // Critical: A non-zero ID is needed for segment ID calculation
        final var routeName = "Route 61";
        route.setName(routeName);
        route.getForwardStops().add(startStop);
        route.getForwardStops().add(endStop);

        // 3. Create REAL segments. Their IDs will be calculated as sequential by the constructor.
        final var startSegment = new TransportRouteSegment(route, 0);
        final var endSegment = new TransportRouteSegment(route, 1);

        // 4. Configure the context to return the correct segment for each location
        final var context = mock(ITransportRoutingContext.class);
        when(context.getTransportStops(eq(start))).thenReturn(Collections.singletonList(startSegment));
        when(context.getTransportStops(eq(end))).thenReturn(Collections.singletonList(endSegment));

        // 5. Create a config that can be used by the planner
        ITransportRoutingConfiguration cfg = mock(TransportRoutingConfiguration.class);
        //  not sure that we need getWalkSpeed call in case if we have no need in walk
        when(cfg.getWalkSpeed()).thenReturn(1.0); // 1 (don't clear measure units)
        //  not sure that we need getWalkRadius call in case if we have no need in walk
        when(cfg.getWalkRadius()).thenReturn(1); // 1 (don't clear measure units)
        when(cfg.getSpeedByRouteType(eq(routeType))).thenReturn(10.0f); // 10(m/s? ), speed of route
        when(cfg.getMaxNumberOfChanges()).thenReturn(0); // Even without change on direct route we should find path
        // when(cfg.getFinishTimeSeconds()).thenReturn(15001); // We not use it in scope of method.

        when(context.getCfg()).thenReturn(cfg);
        when(context.getVisitedSegments()).thenReturn(new gnu.trove.map.hash.TLongObjectHashMap<>());
        when(context.getFinishTimeSeconds()).thenReturn(15001); // We will be in finish time in 15000 seconds see bellow

        // Configure the mock IDistanceProvider with specific, realistic values for each
        // call the planner will make. This is the key to satisfying the planner's
        // internal logic without using a real DistanceProvider object.
        final var distanceProvider = mock(DistanceProvider.class);
        final var planner = new TransportRoutePlanner(distanceProvider);
        when(distanceProvider.getDistance(eq(start), eq(end))).thenReturn(10.0); // start <-> end distance (meters?)

        // Act
        final var results = planner.buildRoute(context, start, end);

        // Assert
        assertEquals(1, results.size());
        final var result = results.get(0);
        assertNotNull(result);
        assertEquals(1.0, result.getRouteTime(), 0.0); // 1 = 10.0 (distance) / 10.0 (speed)

        // We have no walk as start and end in a bus stops
        assertEquals(0.0, result.getWalkDist(), 0.0);
        assertEquals(0.0, result.getWalkTime(), 0.0);

        // Assert segment
        final var firstSegment = result.segments.get(0);
        assertEquals(routeName, firstSegment.route.getName());
        assertEquals(routeType, firstSegment.route.getType());
        final long resultRouteId = firstSegment.route.getId();
        assertEquals(routeId, resultRouteId);
        assertEquals(startStop, firstSegment.getStop(firstSegment.start));
        assertEquals(endStop, firstSegment.getStop(firstSegment.end));
        assertEquals(0.0, firstSegment.walkDist, 0.0);
        assertEquals(0.0, firstSegment.walkTime, 0.0);
    }

    @Test
    public void buildRoute_simpleDirectRouteAndWalking_returnsOneResult() throws IOException, InterruptedException {
        // Arrange
        final var start = new LatLon(0, 0);
        final var end = new LatLon(0, 4);

        // 1. Create real stops for start and end
        final var startStop = new TransportStop();
        startStop.setLocation(0, 2); // same location as start point
        startStop.setName("start");
        final var endStop = new TransportStop();
        endStop.setLocation(0, 3); // same location as end point
        startStop.setName("stop");

        // 2. Create a real route, give it a non-zero ID, and add both stops to it
        final var route = new TransportRoute();
        final var routeType = "someType";
        route.setType(routeType);
        final var routeId = 1L;
        route.setId(routeId); // Critical: A non-zero ID is needed for segment ID calculation
        final var routeName = "Route 61";
        route.setName(routeName);
        route.getForwardStops().add(startStop);
        route.getForwardStops().add(endStop);

        // 3. Create REAL segments. Their IDs will be calculated as sequential by the constructor.
        final var startSegment = new TransportRouteSegment(route, 0);
        final var endSegment = new TransportRouteSegment(route, 1);

        // 4. Configure the context to return the correct segment for each location
        final var context = mock(ITransportRoutingContext.class);
        when(context.getTransportStops(eq(start))).thenReturn(Collections.singletonList(startSegment));
        when(context.getTransportStops(eq(end))).thenReturn(Collections.singletonList(endSegment));

        // 5. Create a config that can be used by the planner
        ITransportRoutingConfiguration cfg = mock(TransportRoutingConfiguration.class);
        when(cfg.getWalkSpeed()).thenReturn(1.0); // 1 (m/s? )
        when(cfg.getWalkRadius()).thenReturn(11); // 1 (unclear measure units) , bigger then distance from endStop to end point
        when(cfg.getSpeedByRouteType(eq(routeType))).thenReturn(10.0f); // 10(m/s? ), speed of route
        when(cfg.getMaxNumberOfChanges()).thenReturn(0); // Even without change on direct route we should find path
        // when(cfg.getFinishTimeSeconds()).thenReturn(15001); // We not use it in scope of method.

        when(context.getCfg()).thenReturn(cfg);
        when(context.getVisitedSegments()).thenReturn(new gnu.trove.map.hash.TLongObjectHashMap<>());
        when(context.getFinishTimeSeconds()).thenReturn(15001); // We will be in finish time in 15000 seconds see bellow

        // Configure the mock IDistanceProvider with specific, realistic values for each
        // call the planner will make. This is the key to satisfying the planner's
        // internal logic without using a real DistanceProvider object.
        final var distanceProvider = mock(DistanceProvider.class);
        final var planner = new TransportRoutePlanner(distanceProvider);
        when(distanceProvider.getDistance(eq(start), eq(startStop.getLocation()))).thenReturn(10.0); // start <-> end distance (meters?)
        when(distanceProvider.getDistance(eq(startStop.getLocation()), eq(endStop.getLocation()))).thenReturn(10.0); // start <-> end distance (meters?)
        when(distanceProvider.getDistance(eq(endStop.getLocation()), eq(end))).thenReturn(10.0); // start <-> end distance (meters?)

        // Act
        final var results = planner.buildRoute(context, start, end);

        // Assert
        assertEquals(1, results.size());
        final var result = results.get(0);
        assertNotNull(result);

        // Looks like current implementation is not correct
        // as we have no walk as start and end in a bus stops
        // RouteTime = (start [absent] + end transport time) + walking time
        // Transport time: 1 = 10.0 (distance) / 10.0 (speed)
        // Walking time: 10 = 10.0 (distance) / 1.0 (speed)
        // RouteTime: 1 + 10 = 11.0
        assertEquals(11.0, result.getRouteTime(), 0.0);
        // We have no walk as start and end in a bus stops
        assertEquals(10.0, result.getWalkDist(), 0.0);
        assertEquals(10.0, result.getWalkTime(), 0.0);

        // Assert segment
        final var firstSegment = result.segments.get(0);
        assertEquals(routeName, firstSegment.route.getName());
        assertEquals(routeType, firstSegment.route.getType());
        final long resultRouteId = firstSegment.route.getId();
        assertEquals(routeId, resultRouteId);
        assertEquals(startStop, firstSegment.getStop(firstSegment.start));
        assertEquals(endStop, firstSegment.getStop(firstSegment.end));
        assertEquals(0.0, firstSegment.walkDist, 0.0);
        assertEquals(0.0, firstSegment.walkTime, 0.0);
    }


    @Test
    public void buildRoute_TwoConcurrentRoutesAndWalking_returnsOneResult() throws IOException, InterruptedException {
        // Arrange
        final var routeType = "someType";
        final var start = new LatLon(1, 0);
        final var end = new LatLon(1, 4);

        // 1. Create real stops for start and end of route A
        final var startStopRouteA = new TransportStop();
        startStopRouteA.setLocation(0, 2); // same location as start point
        startStopRouteA.setName("start route A");
        final var endStopRouteA = new TransportStop();
        endStopRouteA.setLocation(0, 3); // same location as end point
        endStopRouteA.setName("stop route A");

        // 2. Create a route A, give it a non-zero ID, and add both stops to it
        final var routeA = new TransportRoute();
        routeA.setType(routeType);
        final var routeAId = 1L;
        routeA.setId(routeAId); // Critical: A non-zero ID is needed for segment ID calculation
        final var routeNameA = "Route A";
        routeA.setName(routeNameA);
        routeA.getForwardStops().add(startStopRouteA);
        routeA.getForwardStops().add(endStopRouteA);

        // 3. Create REAL segments of route A.
        // Their IDs will be calculated as sequential by the constructor.
        final var startSegmentA = new TransportRouteSegment(routeA, 0);
        final var endSegmentA = new TransportRouteSegment(routeA, 1);

        // 4. Create real stops for start and end of route B
        final var startStopRouteB = new TransportStop();
        startStopRouteB.setLocation(2, 2); // same location as start point
        startStopRouteB.setName("start route B");
        final var endStopRouteB = new TransportStop();
        endStopRouteB.setLocation(2, 3); // same location as end point
        endStopRouteB.setName("stop route B");

        // 5. Create a route B, give it a non-zero ID, and add both stops to it
        final var routeB = new TransportRoute();
        routeB.setType(routeType);
        final var routeBId = 2L;
        routeB.setId(routeBId); // Critical: A non-zero ID is needed for segment ID calculation
        final var routeNameB = "Route B";
        routeB.setName(routeNameB);
        routeB.getForwardStops().add(startStopRouteB);
        routeB.getForwardStops().add(endStopRouteB);

        // 6. Create REAL segments of route B.
        // Their IDs will be calculated as sequential by the constructor.
        final var startSegmentB = new TransportRouteSegment(routeB, 0);
        final var endSegmentB = new TransportRouteSegment(routeB, 1);

        // 7. Configure the context to return the correct segment for each location
        final var context = mock(ITransportRoutingContext.class);
        var startSegments = new ArrayList<TransportRouteSegment>();
        startSegments.add(startSegmentA);
        startSegments.add(startSegmentB);
        when(context.getTransportStops(eq(start))).thenReturn(startSegments);
        var endSegments = new ArrayList<TransportRouteSegment>();
        endSegments.add(endSegmentA);
        endSegments.add(endSegmentB);
        when(context.getTransportStops(eq(end))).thenReturn(endSegments);

        // 8. Create a config that can be used by the planner
        ITransportRoutingConfiguration cfg = mock(TransportRoutingConfiguration.class);
        when(cfg.getWalkSpeed()).thenReturn(1.0); // 1 (m/s? )
        when(cfg.getWalkRadius()).thenReturn(12); // 1 (unclear measure units) , bigger then distance from endStop to end point
        when(cfg.getSpeedByRouteType(eq(routeType))).thenReturn(10.0f); // 10(m/s? ), speed of route
        when(cfg.getMaxNumberOfChanges()).thenReturn(0); // Even without change on direct route we should find path
        // when(cfg.getFinishTimeSeconds()).thenReturn(15001); // We not use it in scope of method.

        when(context.getCfg()).thenReturn(cfg);
        when(context.getVisitedSegments()).thenReturn(new gnu.trove.map.hash.TLongObjectHashMap<>());
        when(context.getFinishTimeSeconds()).thenReturn(15001); // We will be in finish time in 15000 seconds see bellow

        // Configure the mock IDistanceProvider with specific, realistic values for each
        // call the planner will make. This is the key to satisfying the planner's
        // internal logic without using a real DistanceProvider object.
        final var distanceProvider = mock(DistanceProvider.class);
        final var planner = new TransportRoutePlanner(distanceProvider);
        when(distanceProvider.getDistance(eq(start), eq(end))).thenReturn(20.0); // start <-> end distance
        when(distanceProvider.getDistance(eq(start), eq(startStopRouteA.getLocation()))).thenReturn(10.0); // start <-> start route A (no one care?)
        when(distanceProvider.getDistance(eq(start), eq(startStopRouteB.getLocation()))).thenReturn(10.0); // start <-> start route B (no one care?)
        when(distanceProvider.getDistance(eq(startStopRouteA.getLocation()), eq(endStopRouteA.getLocation()))).thenReturn(11.0); // start route A <-> end route A distance (meters?)
        when(distanceProvider.getDistance(eq(startStopRouteB.getLocation()), eq(endStopRouteB.getLocation()))).thenReturn(10.0); // start route B <-> end route B distance (meters?)
        when(distanceProvider.getDistance(eq(endStopRouteA.getLocation()), eq(end))).thenReturn(10.0); // end route A <-> end distance (meters?)
        when(distanceProvider.getDistance(eq(endStopRouteB.getLocation()), eq(end))).thenReturn(11.0); // end route B <-> end distance (meters?)

        // Act
        final var results = planner.buildRoute(context, start, end);

        // Assert
        assertEquals(2, results.size());
        final var resultA = results.get(0);
        assertNotNull(resultA);

        // Looks like current implementation is not correct
        // as we have no walk as start and end in a bus stops
        // RouteTime = (start [absent] + end transport time) + walking time
        // Transport time: 1.1 = 11.0 (distance) / 10.0 (speed)
        // Walking time: 10 = 10.0 (distance) / 1.0 (speed)
        // RouteTime: 1.1 + 10 = 11.1
        assertEquals(11.1, resultA.getRouteTime(), 0.0);
        // We have no walk as start and end in a bus stops
        assertEquals(10.0, resultA.getWalkDist(), 0.0);
        assertEquals(10.0, resultA.getWalkTime(), 0.0);

        // Assert segment
        final var firstSegmentA = resultA.segments.get(0);
        assertEquals(routeNameA, firstSegmentA.route.getName());
        assertEquals(routeType, firstSegmentA.route.getType());
        final long resultRouteAId = firstSegmentA.route.getId();
        assertEquals(routeAId, resultRouteAId);

        assertEquals(startStopRouteA, firstSegmentA.getStop(firstSegmentA.start));
        assertEquals(endStopRouteA, firstSegmentA.getStop(firstSegmentA.end));
        assertEquals(0.0, firstSegmentA.walkDist, 0.0);
        assertEquals(0.0, firstSegmentA.walkTime, 0.0);


        final var resultB = results.get(1);
        // Looks like current implementation is not correct
        // as we have no walk as start and end in a bus stops
        // RouteTime = (start [absent] + end transport time) + walking time
        // Transport time: 1 = 10.0 (distance) / 10.0 (speed)
        // Walking time: 10 = 11.0 (distance) / 1.0 (speed)
        // RouteTime: 1 + 11 = 11.0
        assertEquals(12.0, resultB.getRouteTime(), 0.0);
        // We have no walk as start and end in a bus stops
        assertEquals(11.0, resultB.getWalkDist(), 0.0);
        assertEquals(11.0, resultB.getWalkTime(), 0.0);

        // Assert segment
        final var firstSegmentB = resultB.segments.get(0);
        assertEquals(routeNameB, firstSegmentB.route.getName());
        assertEquals(routeType, firstSegmentB.route.getType());
        final long resultRouteBId = firstSegmentB.route.getId();
        assertEquals(routeBId, resultRouteBId);

        assertEquals(startStopRouteA, firstSegmentB.getStop(firstSegmentB.start));
        assertEquals(endStopRouteB, firstSegmentB.getStop(firstSegmentB.end));
    }
}
