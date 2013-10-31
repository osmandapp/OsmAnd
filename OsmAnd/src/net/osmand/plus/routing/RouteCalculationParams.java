package net.osmand.plus.routing;

import java.util.List;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ClientContext;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.router.RouteCalculationProgress;

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
	public boolean optimal;
	public boolean leftSide;
	public RouteCalculationProgress calculationProgress;
	public boolean preciseRouting;
}
