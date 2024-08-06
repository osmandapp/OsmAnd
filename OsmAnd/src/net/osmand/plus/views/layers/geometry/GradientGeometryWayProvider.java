package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.views.layers.geometry.GeometryWay.GeometryWayProvider;
import net.osmand.shared.routing.RouteColorize;
import net.osmand.shared.routing.RouteColorize.RouteColorizationPoint;

import java.util.List;

class GradientGeometryWayProvider implements GeometryWayProvider {

	private final RouteColorize routeColorize;
	private final List<RouteColorizationPoint> points;
	private final List<Float> heights;

	public GradientGeometryWayProvider(@Nullable RouteColorize routeColorize,
	                                   @NonNull List<RouteColorizationPoint> points,
	                                   @Nullable List<Float> heights) {
		this.routeColorize = routeColorize;
		this.points = points;
		this.heights = heights;
	}

	@Nullable
	public List<RouteColorize.RouteColorizationPoint> simplify(int zoom) {
		return routeColorize != null ? routeColorize.simplify(zoom) : null;
	}

	public int getColor(int index) {
		return points.get(index).getPrimaryColor();
	}

	@Override
	public double getLatitude(int index) {
		return points.get(index).getLat();
	}

	@Override
	public double getLongitude(int index) {
		return points.get(index).getLon();
	}

	@Override
	public int getSize() {
		return points.size();
	}

	@Override
	public float getHeight(int index) {
		return heights == null ? 0 : heights.get(index);
	}
}
