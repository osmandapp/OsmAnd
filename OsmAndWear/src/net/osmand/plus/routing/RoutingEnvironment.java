package net.osmand.plus.routing;

import net.osmand.router.PrecalculatedRouteDirection;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingContext;

public class RoutingEnvironment {

	private final RoutingContext ctx;
	private final RoutingContext complexCtx;
	private final RoutePlannerFrontEnd router;
	private final PrecalculatedRouteDirection precalculated;

	public RoutingEnvironment(RoutePlannerFrontEnd router, RoutingContext ctx, RoutingContext complexCtx, PrecalculatedRouteDirection precalculated) {
		this.router = router;
		this.ctx = ctx;
		this.complexCtx = complexCtx;
		this.precalculated = precalculated;
	}

	public RoutePlannerFrontEnd getRouter() {
		return router;
	}

	public RoutingContext getCtx() {
		return ctx;
	}

	public RoutingContext getComplexCtx() {
		return complexCtx;
	}

	public PrecalculatedRouteDirection getPrecalculated() {
		return precalculated;
	}
}
