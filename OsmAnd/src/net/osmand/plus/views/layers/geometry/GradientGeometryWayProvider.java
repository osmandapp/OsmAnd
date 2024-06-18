package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.views.layers.geometry.GeometryWay.GeometryWayProvider;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.RouteColorizationPoint;

import java.util.List;

class GradientGeometryWayProvider implements GeometryWayProvider {

	private final RouteColorize routeColorize;
	private final List<RouteColorizationPoint> locations;
	private final List<Float> pointHeights;

	public GradientGeometryWayProvider(@Nullable RouteColorize routeColorize,
	                                   @NonNull List<RouteColorizationPoint> locations,
	                                   @Nullable List<Float> pointHeights) {
		this.routeColorize = routeColorize;
		this.locations = locations;
		this.pointHeights = pointHeights;
	}

	@Nullable
	public List<RouteColorizationPoint> simplify(int zoom) {
		return routeColorize != null ? routeColorize.simplify(zoom) : null;
	}

	public int getColor(int index) {
		return locations.get(index).color;
	}

	@Override
	public double getLatitude(int index) {
		return locations.get(index).lat;
	}

	@Override
	public double getLongitude(int index) {
		return locations.get(index).lon;
	}

	@Override
	public int getSize() {
		return locations.size();
	}

	@Override
	public float getHeight(int index) {
		return pointHeights == null ? 0 : pointHeights.get(index);
	}
}
