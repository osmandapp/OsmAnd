package net.osmand.plus.routing;

import net.osmand.LocationsHolder;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteProvider.RoutingEnvironment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.router.RoutePlannerFrontEnd.GpxPoint;
import net.osmand.router.RoutePlannerFrontEnd.GpxRouteApproximation;

import java.io.IOException;
import java.util.List;

public class GpxApproximator {

	private OsmandApplication ctx;
	private RoutingHelper routingHelper;

	private RoutingEnvironment env;
	private GpxRouteApproximation gctx;
	private ApplicationMode mode;
	private LocationsHolder locationsHolder;
	private List<GpxPoint> points;
	private LatLon start;
	private LatLon end;

	public GpxApproximator(OsmandApplication ctx, LocationsHolder locationsHolder) throws IOException {
		this.ctx = ctx;
		this.locationsHolder = locationsHolder;
		this.routingHelper = ctx.getRoutingHelper();
		this.mode = ApplicationMode.CAR;
		if (locationsHolder.getSize() > 1) {
			start = locationsHolder.getLatLon(0);
			end = locationsHolder.getLatLon(locationsHolder.getSize() - 1);
			prepareEnvironment(ctx, mode);
			this.points = routingHelper.generateGpxPoints(env, gctx, locationsHolder);
		}
	}

	public GpxApproximator(OsmandApplication ctx, ApplicationMode mode, double pointApproximation, LocationsHolder locationsHolder) throws IOException {
		this.ctx = ctx;
		this.routingHelper = ctx.getRoutingHelper();
		this.mode = mode;
		if (locationsHolder.getSize() > 1) {
			start = locationsHolder.getLatLon(0);
			end = locationsHolder.getLatLon(locationsHolder.getSize() - 1);
			prepareEnvironment(ctx, mode);
			gctx.MINIMUM_POINT_APPROXIMATION = pointApproximation;
			this.points = routingHelper.generateGpxPoints(env, gctx, locationsHolder);
		}
	}

	private void prepareEnvironment(OsmandApplication ctx, ApplicationMode mode) throws IOException {
		this.env = routingHelper.getRoutingEnvironment(ctx, mode, start, end);
		this.gctx = new GpxRouteApproximation(env.getCtx());
	}

	public ApplicationMode getMode() {
		return mode;
	}

	public void setMode(ApplicationMode mode) throws IOException {
		boolean changed = this.mode != mode;
		this.mode = mode;
		if (changed) {
			prepareEnvironment(ctx, mode);
		}
	}

	public double getPointApproximation() {
		return gctx.MINIMUM_POINT_APPROXIMATION;
	}

	public void setPointApproximation(double pointApproximation) {
		gctx.MINIMUM_POINT_APPROXIMATION = pointApproximation;
	}

	public LocationsHolder getLocationsHolder() {
		return locationsHolder;
	}

	public GpxRouteApproximation calculateGpxApproximation() throws IOException, InterruptedException {
		return routingHelper.calculateGpxApproximation(env, gctx, points);
	}
}
