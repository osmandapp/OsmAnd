package net.osmand.plus.routing;


import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.LocationPoint;
import net.osmand.plus.GPXFile;
import net.osmand.plus.Route;
import net.osmand.plus.Track;
import net.osmand.plus.TrkSegment;
import net.osmand.plus.WptPt;
import net.osmand.plus.routing.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RouteDirectionInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GPXRouteParams {
    List<Location> points = new ArrayList<Location>();
    List<RouteDirectionInfo> directions;
    boolean calculateOsmAndRoute;
    boolean passWholeRoute;
    boolean calculateOsmAndRouteParts;
    boolean useIntermediatePointsRTE;
    private List<LocationPoint> wpt;

    public List<LocationPoint> getWpt() { return wpt; }

    public List<Location> getPoints() {
        return points;
    }

    public Location getStartPointForRoute(){
        if(!points.isEmpty()){
            return points.get(0);
        }
        return null;
    }

    public Location getEndPointForRoute(){
        if(!points.isEmpty()){
            return points.get(points.size());
        }
        return null;
    }

    public LatLon getLastPoint() {
        if(!points.isEmpty()){
            Location l = points.get(points.size() - 1);
            LatLon point = new LatLon(l.getLatitude(), l.getLongitude());
            return point;
        }
        return null;
    }

    public GPXRouteParams prepareGPXFile(GPXRouteParamsBuilder builder){
        GPXFile file = builder.getFile();
        boolean reverse = builder.isReverse();
        passWholeRoute = builder.isPassWholeRoute();
        calculateOsmAndRouteParts = builder.isCalculateOsmAndRouteParts();
        useIntermediatePointsRTE = builder.isUseIntermediatePointsRTE();
        builder.calculateOsmAndRoute = false; // Disabled temporary builder.calculateOsmAndRoute;
        if(!file.points.isEmpty()) {
            wpt = new ArrayList<LocationPoint>(file.points );
        }
        if(file.isCloudmadeRouteFile() || RouteProvider.OSMAND_ROUTER.equals(file.author)){
            directions =  new RouteProvider().parseOsmAndGPXRoute(points, file, RouteProvider.OSMAND_ROUTER.equals(file.author), builder.isLeftSide(), 10);
            if(reverse){
                // clear directions all turns should be recalculated
                directions = null;
                Collections.reverse(points);
            }
        } else {
            // first of all check tracks
            if (!useIntermediatePointsRTE) {
                for (Track tr : file.tracks) {
                    for (TrkSegment tkSeg : tr.segments) {
                        for (WptPt pt : tkSeg.points) {
                            points.add(new RouteProvider().createLocation(pt));
                        }
                    }
                }
            }
            if (points.isEmpty()) {
                for (Route rte : file.routes) {
                    for (WptPt pt : rte.points) {
                        points.add(new RouteProvider().createLocation(pt));
                    }
                }
            }
            if (reverse) {
                Collections.reverse(points);
            }
        }
        return this;
    }

}