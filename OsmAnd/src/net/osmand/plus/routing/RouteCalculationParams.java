package net.osmand.plus.routing;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.router.RouteCalculationProgress;

import java.util.List;

public class RouteCalculationParams {

	public Location start;
	public LatLon end;
	public List<LatLon> intermediates;
	public Location currentLocation;

	public OsmandApplication ctx;
	public ApplicationMode mode;
	public GPXRouteParams gpxRoute;
	public RouteCalculationResult previousToRecalculate;
	public boolean onlyStartPointChanged;
	public boolean fast;
	public boolean leftSide;
	public boolean inSnapToRoadMode;
	public boolean inPublicTransportMode;
	public boolean startTransportStop;
	public boolean targetTransportStop;
	public RouteCalculationProgress calculationProgress;
	public RouteCalculationProgressCallback calculationProgressCallback;
	public RouteCalculationResultListener resultListener;

	public interface RouteCalculationResultListener {
		void onRouteCalculated(RouteCalculationResult route);
	}
}
