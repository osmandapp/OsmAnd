package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;

import net.osmand.plus.views.layers.geometry.GeometryWay.GeometryWayProvider;
import net.osmand.shared.routing.RouteColorize;

import java.util.List;

public class Geometry3DWayProvider implements GeometryWayProvider {

	private final List<RouteColorize.RouteColorizationPoint> points;

	public Geometry3DWayProvider(@NonNull List<RouteColorize.RouteColorizationPoint> points) {
		this.points = points;
	}

	public int getColor(int index) {
		return points.get(index).getPrimaryColor();
	}

	public int getOutlineColor(int index) {
		return points.get(index).getSecondaryColor();
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
	public float getHeight(int index) {
		return (float) points.get(index).getValue();
	}

	@Override
	public int getSize() {
		return points.size();
	}
}