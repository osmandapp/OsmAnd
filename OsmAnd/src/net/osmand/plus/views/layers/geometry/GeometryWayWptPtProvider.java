package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;

import net.osmand.plus.views.layers.geometry.GeometryWay.GeometryWayProvider;
import net.osmand.shared.gpx.primitives.WptPt;

import java.util.List;

class GeometryWayWptPtProvider implements GeometryWayProvider {

	private final List<WptPt> points;

	public GeometryWayWptPtProvider(@NonNull List<WptPt> points) {
		this.points = points;
	}

	@Override
	public double getLatitude(int index) {
		return points.get(index).getLatitude();
	}

	@Override
	public double getLongitude(int index) {
		return points.get(index).getLongitude();
	}

	@Override
	public float getHeight(int index) {
		return (float) points.get(index).getEle();
	}

	@Override
	public int getSize() {
		return points.size();
	}
}
