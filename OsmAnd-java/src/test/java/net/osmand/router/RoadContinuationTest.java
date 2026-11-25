package net.osmand.router;

import static org.junit.Assert.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.binary.RouteDataObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RoadContinuationTest {
    @Parameterized.Parameters(name = "{0}, reverse={1}")
    public static Object[][] parameters() {
        return new Object[][]{{"car", false}, {"car", true}, {"motorcycle", false}, {"motorcycle", true}};
    }

    private final String profile;
    private final boolean reverse;
    private Segment[] route;
    private Segment before;
    private Segment after;
    private Segment branch;
    private int junction;

    public RoadContinuationTest(String profile, boolean reverse) {
        this.profile = profile;
        this.reverse = reverse;
    }

    @Before
    public void loadJunction() throws Exception {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                "/routing/road-continuation.json")), StandardCharsets.UTF_8)) {
            route = new Gson().fromJson(reader, Fixture.class).routes[reverse ? 1 : 0];
        }
        junction = reverse ? 2 : 1;
        before = route[junction - 1];
        after = route[junction];
        branch = after.attached[0];
    }

    @Test public void followsB72PastServiceRoad() {
        assertNull(turn());
    }

    @Test public void allowsDifferentWayIdsAndStreetNamesWithSameRef() {
        after.id++;
        after.name = "New street name";
        assertNull(turn());
    }

    @Test public void followsUniqueNameWithoutRef() {
        after.id++;
        before.ref = after.ref = null;
        branch.name = "Driveway";
        assertNull(turn());
    }

    @Test public void followsSameRefAcrossWayBoundaryWithoutBranches() {
        after.id++;
        after.name = "New street name";
        after.attached = null;
        assertNull(turn());
    }

    @Test public void followsSameNameAcrossWayBoundaryWithoutBranches() {
        after.id++;
        before.ref = after.ref = null;
        after.attached = null;
        assertNull(turn());
    }

    @Test public void keepsTurnWhenRefChangesWithoutBranches() {
        after.id++;
        after.ref = "B73";
        after.attached = null;
        assertTurn();
    }

    @Test public void keepsTurnWithoutIdentityOrBranches() {
        after.id++;
        before.ref = after.ref = before.name = after.name = null;
        after.attached = null;
        assertTurn();
    }

    @Test public void keepsTurnWhenClassChangesWithoutBranches() {
        after.id++;
        after.highway = "secondary";
        after.attached = null;
        assertTurn();
    }

    @Test public void preservesMarkedTurnWithoutBranches() {
        after.id++;
        before.turnLanes = reverse ? "right" : "left";
        after.attached = null;
        assertTurn();
    }

    @Test public void followsLp114AcrossBothJunctionAndWayBoundary() throws Exception {
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(
                "/routing/road-continuation-lp114.json")), StandardCharsets.UTF_8)) {
            route = new Gson().fromJson(reader, Fixture.class).routes[reverse ? 1 : 0];
        }
        List<RouteSegmentResult> results = prepareTurns();
        for (int i = 1; i < results.size(); i++) {
            assertNull("Unexpected instruction on LP-114 segment " + i, results.get(i).getTurnType());
        }
    }

    @Test public void keepsTurnWhenNameIsNotUnique() {
        before.ref = after.ref = null;
        assertTurn();
    }

    @Test public void keepsTurnWhenRefIsNotUnique() {
        branch.ref = before.ref;
        assertTurn();
    }

    @Test public void keepsTurnWhenRefChangesDespiteMatchingName() {
        after.ref = "B73";
        assertTurn();
    }

    @Test public void keepsTurnWhenOneRefIsMissing() {
        after.ref = null;
        assertTurn();
    }

    @Test public void keepsTurnWithoutRoadIdentity() {
        before.ref = after.ref = before.name = after.name = null;
        assertTurn();
    }

    @Test public void keepsTurnWhenRoadClassChanges() {
        after.highway = "secondary";
        assertTurn();
    }

    @Test public void keepsTurnAtEqualClassJunction() {
        branch.highway = "primary";
        assertTurn();
    }

    @Test public void keepsTurnAtHigherClassJunction() {
        before.highway = after.highway = "secondary";
        branch.highway = "primary";
        assertTurn();
    }

    @Test public void keepsTurnWhenBranchClassIsUnknown() {
        branch.highway = null;
        assertTurn();
    }

    @Test public void keepsTurnWhenAnotherBranchMatchesRef() {
        Segment second = new Gson().fromJson(new Gson().toJson(branch), Segment.class);
        second.id++;
        second.ref = before.ref;
        after.attached = new Segment[]{branch, second};
        assertTurn();
    }

    @Test public void preservesMarkedTurnLanes() {
        before.turnLanes = reverse ? "right" : "left";
        assertTurn();
    }

    @Test public void preservesLinkJunctions() {
        branch.highway = "primary_link";
        assertTurn();
    }

    @Test public void preservesActualTurnOntoServiceRoad() {
        after.attached = null;
        branch.attached = new Segment[]{after};
        branch.distance = 100;
        route = new Segment[]{before, branch};
        junction = 1;
        TurnType t = turn();
        assertNotNull(t);
        assertEquals(reverse ? TurnType.TL : TurnType.TR, t.getValue());
        assertFalse(t.isSkipToSpeak());
    }

    private void assertTurn() {
        TurnType t = turn();
        assertNotNull(t);
        assertEquals(reverse ? TurnType.TR : TurnType.TL, t.getValue());
        assertFalse(t.isSkipToSpeak());
    }

    private TurnType turn() {
        return prepareTurns().get(junction).getTurnType();
    }

    private List<RouteSegmentResult> prepareTurns() {
        List<RouteSegmentResult> results = new ArrayList<>();
        for (Segment segment : route) {
            results.add(segment.result());
        }
        RoutingConfiguration config = RoutingConfiguration.getDefault().build(profile,
                new RoutingConfiguration.RoutingMemoryLimits(64, 64), new HashMap<>());
        RoutingContext ctx = new RoutePlannerFrontEnd().buildRoutingContext(config, null, new BinaryMapIndexReader[0]);
        new RouteResultPreparation().prepareTurnResults(ctx, results);
        return results;
    }

    private static class Fixture {
        Segment[][] routes;
    }

    private static class Segment {
        long id;
        int start, end;
        int[][] points;
        float distance;
        String highway, ref, name, turnLanes;
        Segment[] attached;

        RouteSegmentResult result() {
            RouteRegion region = new RouteRegion();
            region.initRouteEncodingRule(1, "name", null);
            region.initRouteEncodingRule(2, "ref", null);
            if (highway != null) region.initRouteEncodingRule(3, "highway", highway);
            if (turnLanes != null) region.initRouteEncodingRule(4, start < end ? "turn:lanes:forward" : "turn:lanes:backward", turnLanes);
            RouteDataObject road = new RouteDataObject(region, new int[]{1, 2}, new String[]{name, ref});
            road.id = id;
            road.types = highway == null ? new int[0] : turnLanes == null ? new int[]{3} : new int[]{3, 4};
            road.pointsX = new int[points.length];
            road.pointsY = new int[points.length];
            for (int i = 0; i < points.length; i++) {
                road.pointsX[i] = points[i][0];
                road.pointsY[i] = points[i][1];
            }
            RouteSegmentResult result = new RouteSegmentResult(road, start, end);
            result.setDistance(distance);
            if (attached != null) {
                for (Segment a : attached) {
                    result.attachRoute(start, a.result());
                }
            }
            return result;
        }
    }
}
