package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.views.layers.geometry.GeometryWay.GeometryWayProvider;
import net.osmand.router.RouteColorize.ColorizationType;

import java.util.List;

public class Geometry3DWayProvider implements GeometryWayProvider {

	private final List<WptPt> points;
	private final ColorizationType colorizationType;
	private final ColorizationType outlineColorizationType;

	public Geometry3DWayProvider(@NonNull List<WptPt> points,
	                             @Nullable ColorizationType colorizationType,
	                             @Nullable ColorizationType outlineColorizationType) {
		this.points = points;
		this.colorizationType = colorizationType;
		this.outlineColorizationType = outlineColorizationType;
	}

	public int getColor(int index) {
		return points.get(index).getColor(colorizationType);
	}

	public int getOutlineColor(int index) {
		return points.get(index).getColor(outlineColorizationType);
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
		return (float) points.get(index).ele;
	}

	@Override
	public int getSize() {
		return points.size();
	}
}