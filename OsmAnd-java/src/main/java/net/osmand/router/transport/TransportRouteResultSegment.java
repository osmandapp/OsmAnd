package net.osmand.router.transport;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.Way;
import net.osmand.router.IDistanceProvider;
import net.osmand.router.TransportStopsRouteReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class TransportRouteResultSegment {

    private static final boolean DISPLAY_FULL_SEGMENT_ROUTE = false;
    private static final int DISPLAY_SEGMENT_IND = 0;
    private final TransportRoutePlanner transportRoutePlanner;
    private final IDistanceProvider distanceProvider;
    public TransportRoute route;
    public double walkTime;
    public double travelDistApproximate;
    public double travelTime;
    public int start;
    public int end;
    public double walkDist;
    public int depTime;

    public TransportRouteResultSegment(TransportRoutePlanner transportRoutePlanner, IDistanceProvider distanceProvider) {
        this.transportRoutePlanner = transportRoutePlanner;
        this.distanceProvider = distanceProvider;
    }

    public int getArrivalTime() {
        if (route.getSchedule() != null && depTime != -1) {
            int tm = depTime;
            TIntArrayList intervals = route.getSchedule().avgStopIntervals;
            for (int i = start; i <= end; i++) {
                if (i == end) {
                    return tm;
                }
                if (intervals.size() > i) {
                    tm += intervals.get(i);
                } else {
                    break;
                }
            }
        }
        return -1;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public TransportStop getStart() {
        return route.getForwardStops().get(start);
    }

    public TransportStop getEnd() {
        return route.getForwardStops().get(end);
    }

    public List<TransportStop> getTravelStops() {
        return route.getForwardStops().subList(start, end + 1);
    }

    public QuadRect getSegmentRect() {
        double left = 0, right = 0;
        double top = 0, bottom = 0;
        for (Node n : getNodes()) {
            if (left == 0 && right == 0) {
                left = n.getLongitude();
                right = n.getLongitude();
                top = n.getLatitude();
                bottom = n.getLatitude();
            } else {
                left = Math.min(left, n.getLongitude());
                right = Math.max(right, n.getLongitude());
                top = Math.max(top, n.getLatitude());
                bottom = Math.min(bottom, n.getLatitude());
            }
        }
        return left == 0 && right == 0 ? null : new QuadRect(left, top, right, bottom);
    }

    public List<Node> getNodes() {
        List<Node> nodes = new ArrayList<>();
        List<Way> ways = getGeometry();
        for (Way way : ways) {
            nodes.addAll(way.getNodes());
        }
        return nodes;
    }

    private class SearchNodeInd {
        int ind = -1;
        Way way = null;
        double dist = TransportRoutePlanner.MIN_DIST_STOP_TO_GEOMETRY;
    }

    public List<Way> getGeometry() {
        route.mergeForwardWays();
        if (DISPLAY_FULL_SEGMENT_ROUTE) {
            System.out.println("TOTAL SEGMENTS: " + route.getForwardWays().size());
            if (route.getForwardWays().size() > DISPLAY_SEGMENT_IND && DISPLAY_SEGMENT_IND != -1) {
                return Collections.singletonList(route.getForwardWays().get(DISPLAY_SEGMENT_IND));
            }
            return route.getForwardWays();
        }
        List<Way> ways = route.getForwardWays();

        final LatLon startLoc = getStart().getLocation();
        final LatLon endLoc = getEnd().getLocation();
        SearchNodeInd startInd = new SearchNodeInd();
        SearchNodeInd endInd = new SearchNodeInd();
        for (int i = 0; i < ways.size(); i++) {
            List<Node> nodes = ways.get(i).getNodes();
            for (int j = 0; j < nodes.size(); j++) {
                Node n = nodes.get(j);
                if (distanceProvider.getDistance(startLoc, n.getLatitude(), n.getLongitude()) < startInd.dist) {
                    startInd.dist = distanceProvider.getDistance(startLoc, n.getLatitude(), n.getLongitude());
                    startInd.ind = j;
                    startInd.way = ways.get(i);
                }
                if (distanceProvider.getDistance(endLoc, n.getLatitude(), n.getLongitude()) < endInd.dist) {
                    endInd.dist = distanceProvider.getDistance(endLoc, n.getLatitude(), n.getLongitude());
                    endInd.ind = j;
                    endInd.way = ways.get(i);
                }
            }
        }
        boolean validOneWay = startInd.way != null && startInd.way == endInd.way && startInd.ind <= endInd.ind;
        if (validOneWay) {
            Way way = new Way(TransportRoutePlanner.GEOMETRY_WAY_ID);
            for (int k = startInd.ind; k <= endInd.ind; k++) {
                way.addNode(startInd.way.getNodes().get(k));
            }
            return Collections.singletonList(way);
        }
        boolean validContinuation = startInd.way != null && endInd.way != null &&
                startInd.way != endInd.way;
        if (validContinuation) {
            Node ln = startInd.way.getLastNode();
            Node fn = endInd.way.getFirstNode();
            // HERE we need to check other ways for continuation
            if (ln != null && fn != null && distanceProvider.getDistance(ln.getLatLon(), fn.getLatLon()) < TransportStopsRouteReader.MISSING_STOP_SEARCH_RADIUS) {
                validContinuation = true;
            } else {
                validContinuation = false;
            }
        }
        if (validContinuation) {
            List<Way> two = new ArrayList<Way>();
            Way way = new Way(TransportRoutePlanner.GEOMETRY_WAY_ID);
            for (int k = startInd.ind; k < startInd.way.getNodes().size(); k++) {
                way.addNode(startInd.way.getNodes().get(k));
            }
            two.add(way);
            way = new Way(TransportRoutePlanner.GEOMETRY_WAY_ID);
            for (int k = 0; k <= endInd.ind; k++) {
                way.addNode(endInd.way.getNodes().get(k));
            }
            two.add(way);
            return two;
        }
        Way way = new Way(TransportRoutePlanner.STOPS_WAY_ID);
        for (int i = start; i <= end; i++) {
            LatLon l = getStop(i).getLocation();
            Node n = new Node(l.getLatitude(), l.getLongitude(), -1);
            way.addNode(n);
        }
        return Collections.singletonList(way);
    }

    public double getTravelDist() {
        double d = 0;
        for (int k = start; k < end; k++) {
            d += distanceProvider.getDistance(route.getForwardStops().get(k).getLocation(),
                    route.getForwardStops().get(k + 1).getLocation());
        }
        return d;
    }

    public TransportStop getStop(int i) {
        return route.getForwardStops().get(i);
    }
}
