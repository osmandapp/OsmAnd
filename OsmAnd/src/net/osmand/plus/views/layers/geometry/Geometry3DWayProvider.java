package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;

import net.osmand.plus.views.layers.geometry.GeometryWay.GeometryWayProvider;
import net.osmand.router.RouteColorize.RouteColorizationPoint;

import java.util.List;

public class Geometry3DWayProvider implements GeometryWayProvider {

	private final List<RouteColorizationPoint> points;

	public Geometry3DWayProvider(@NonNull List<RouteColorizationPoint> points) {
		this.points = points;
	}

	public int getColor(int index) {
		return points.get(index).primaryColor;
	}

	public int getOutlineColor(int index) {
		return points.get(index).secondaryColor;
	}

	@Override
	public double getLatitude(int index) {
		return points.get(index).lat;
	}

	@Override
	public double getLongitude(int index) {
		return points.get(index).lon;
	}

	@Override
	public float getHeight(int index) {
		return (float) points.get(index).val;
	}

	@Override
	public int getSize() {
		return points.size();
	}
}