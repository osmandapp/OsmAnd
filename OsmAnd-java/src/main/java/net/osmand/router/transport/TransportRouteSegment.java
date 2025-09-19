package net.osmand.router.transport;

import static net.osmand.router.transport.TransportRoutePlanner.formatTransportTime;

import net.osmand.data.LatLon;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;

public class TransportRouteSegment {

    final int segStart;
    final TransportRoute road;
    final int departureTime;
    private static final int SHIFT = 10; // assume less than 1024 stops
    private static final int SHIFT_DEPTIME = 14; // assume less than 1024 stops

    TransportRouteSegment parentRoute = null;
    int parentStop; // last stop to exit for parent route
    double parentTravelTime; // travel time for parent route
    double parentTravelDist; // travel distance for parent route (inaccurate)
    // walk distance to start route location (or finish in case last segment)
    public double walkDist = 0;
    // main field accumulated all time spent from beginning of journey
    double totalTravelTime = 0;

    public TransportRouteSegment(TransportRoute road, int stopIndex) {
        this.road = road;
        this.segStart = (short) stopIndex;
        this.departureTime = -1;
    }

    public TransportRouteSegment(TransportRoute road, int stopIndex, int depTime) {
        this.road = road;
        this.segStart = (short) stopIndex;
        this.departureTime = depTime;
    }

    public TransportRouteSegment(TransportRouteSegment c) {
        this.road = c.road;
        this.segStart = c.segStart;
        this.departureTime = c.departureTime;
    }

    public boolean wasVisited(TransportRouteSegment rrs) {
        if (rrs.road.getId().longValue() == road.getId().longValue() &&
                rrs.departureTime == departureTime) {
            return true;
        }
        if(parentRoute != null) {
            return parentRoute.wasVisited(rrs);
        }
        return false;
    }

    public TransportStop getStop(int i) {
        return road.getForwardStops().get(i);
    }

    public LatLon getLocation() {
        return road.getForwardStops().get(segStart).getLocation();
    }

    public int getLength() {
        return road.getForwardStops().size();
    }

    public TransportRoute getRoute() {
        return road;
    }

    public long getId() {
        long l = road.getId();

        l = l << SHIFT_DEPTIME;
        if(departureTime >= (1 << SHIFT_DEPTIME)) {
            throw new IllegalStateException("too long dep time" + departureTime);
        }
        l += (departureTime + 1);

        l = l << SHIFT;
        if (segStart >= (1 << SHIFT)) {
            throw new IllegalStateException("too many stops " + road.getId() + " " + segStart);
        }
        l += segStart;

        if(l < 0 ) {
            throw new IllegalStateException("too long id " + road.getId());
        }
        return l  ;
    }

    public int getDepth() {
        if(parentRoute != null) {
            return parentRoute.getDepth() + 1;
        }
        return 1;
    }

    @Override
    public String toString() {
        return String.format("Route: %s, stop: %s %s", road.getName(), road.getForwardStops().get(segStart).getName(),
                departureTime == -1 ? "" : formatTransportTime(departureTime) );
    }
}
