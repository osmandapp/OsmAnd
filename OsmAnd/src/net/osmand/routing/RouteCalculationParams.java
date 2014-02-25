package net.osmand.routing;

import java.util.List;

import net.osmand.ApplicationMode;
import net.osmand.ClientContext;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.routing.RouteProvider.GPXRouteParams;
import net.osmand.routing.RouteProvider.RouteService;

public class RouteCalculationParams {

	public Location start;
	public LatLon end;
	public List<LatLon> intermediates;
	
	public ClientContext ctx;
	public ApplicationMode mode;
	public RouteService type;
	public GPXRouteParams gpxRoute;
	public RouteCalculationResult previousToRecalculate;
	public boolean fast;
	public boolean leftSide;
	public RouteCalculationProgress calculationProgress;
}
