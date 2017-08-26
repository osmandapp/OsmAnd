package net.osmand.plus.routing;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper.RouteCalculationProgressCallback;
import net.osmand.router.RouteCalculationProgress;
import net.osmand.router.RoutingContext;

import java.util.List;

public class RouteCalculationParams {

	public Location start;
	public LatLon end;
	public List<LatLon> intermediates;


	public OsmandApplication ctx;
	public RoutingContext cachedRoutingContext;
	public ApplicationMode mode;
	public RouteService type;
	public GPXRouteParams gpxRoute;
	public RouteCalculationResult previousToRecalculate;
	public boolean onlyStartPointChanged;
	public boolean fast;
	public boolean leftSide;
	public boolean inSnapToRoadMode;
	public RouteCalculationProgress calculationProgress;
	public RouteCalculationProgressCallback calculationProgressCallback;
	public RouteCalculationResultListener resultListener;

	public interface RouteCalculationResultListener {
		void onRouteCalculated(List<Location> locations);
	}
}
