package net.osmand.plus.routing;

import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;

public enum RouteService {
	OSMAND("OsmAnd (offline)"),
	BROUTER("BRouter (offline)"),
	STRAIGHT("Straight line"),
	DIRECT_TO("Direct To"),
	ONLINE("Online engine");

	private final String name;

	RouteService(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isOnline() {
		return this != OSMAND && this != BROUTER;
	}

	boolean isAvailable(OsmandApplication ctx) {
		if (this == BROUTER) {
			return ctx.getBRouterService() != null;
		}
		return true;
	}

	public static RouteService[] getAvailableRouters(OsmandApplication ctx) {
		List<RouteService> list = new ArrayList<>();
		for (RouteService r : values()) {
			if (r.isAvailable(ctx)) {
				list.add(r);
			}
		}
		return list.toArray(new RouteService[0]);
	}
}
