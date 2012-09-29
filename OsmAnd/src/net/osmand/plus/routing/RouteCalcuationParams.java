package net.osmand.plus.routing;

import java.util.List;

import net.osmand.osm.LatLon;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.router.Interruptable;
import android.content.Context;
import android.location.Location;

public class RouteCalcuationParams {

	public Location start;
	public LatLon end;
	public List<LatLon> intermediates;
	
	public Context ctx;
	public ApplicationMode mode;
	public RouteService type;
	public GPXRouteParams gpxRoute;
	public RouteCalculationResult previousToRecalculate;
	public boolean fast;
	public boolean optimal;
	public boolean leftSide;
	public Interruptable interruptable;
}
