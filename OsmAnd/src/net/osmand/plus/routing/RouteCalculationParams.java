package net.osmand.plus.routing;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RouteProvider.SnapToRoadParams;
import net.osmand.router.RouteCalculationProgress;

import java.util.List;

public class RouteCalculationParams {

	public Location start;
	public LatLon end;
	public List<LatLon> intermediates;
	
	
	public OsmandApplication ctx;
	public ApplicationMode mode;
	public RouteService type;
	public GPXRouteParams gpxRoute;
	public SnapToRoadParams snapToRoadParams;
	public RouteCalculationResult previousToRecalculate;
	public boolean onlyStartPointChanged;
	public boolean fast;
	public boolean leftSide;
	public RouteCalculationProgress calculationProgress;
}
