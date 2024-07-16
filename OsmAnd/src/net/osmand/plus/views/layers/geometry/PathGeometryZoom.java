package net.osmand.plus.views.layers.geometry;

import static net.osmand.plus.views.layers.geometry.GeometryWayPathAlgorithms.cullRamerDouglasPeucker;

import androidx.annotation.NonNull;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.layers.geometry.GeometryWay.GeometryWayProvider;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TByteArrayList;

class PathGeometryZoom {

	private static final float EPSILON_IN_DPI = 2;
	private final TByteArrayList simplifyPoints;
	private final List<Double> distances;
	private final List<Double> angles;

	public PathGeometryZoom(@NonNull GeometryWayProvider provider, @NonNull RotatedTileBox tb,
	                        boolean simplify, @NonNull List<Integer> forceIncludedIndexes) {
		tb = new RotatedTileBox(tb);
		tb.setZoomAndAnimation(tb.getZoom(), 0, tb.getZoomFloatPart());
		int size = provider.getSize();
		simplifyPoints = new TByteArrayList(size);
		distances = new ArrayList<>(size);
		angles = new ArrayList<>(size);
		if (simplify) {
			simplifyPoints.fill(0, size, (byte) 0);
			simplify(tb, provider, simplifyPoints);
		} else {
			simplifyPoints.fill(0, size, (byte) 1);
		}
		int previousIndex = -1;
		for (int i = 0; i < size; i++) {
			double d = 0;
			double angle = 0;
			if (simplifyPoints.get(i) > 0 || forceIncludedIndexes.contains(i)) {
				if (previousIndex > -1) {
					float x = tb.getPixXFromLatLon(provider.getLatitude(i), provider.getLongitude(i));
					float y = tb.getPixYFromLatLon(provider.getLatitude(i), provider.getLongitude(i));
					float px = tb.getPixXFromLatLon(provider.getLatitude(previousIndex), provider.getLongitude(previousIndex));
					float py = tb.getPixYFromLatLon(provider.getLatitude(previousIndex), provider.getLongitude(previousIndex));
					d = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
					if (px != x || py != y) {
						double angleRad = Math.atan2(y - py, x - px);
						angle = (angleRad * 180 / Math.PI) + 90f;
					}
				}
				previousIndex = i;
			}
			distances.add(d);
			angles.add(angle);
		}
	}

	public void simplify(RotatedTileBox tb, GeometryWayProvider locationProvider, TByteArrayList simplifyPoints) {
		int size = locationProvider.getSize();
		if (size > 0) {
			simplifyPoints.set(0, (byte) 1);
		}
		double distInPix = (tb.getDistance(0, 0, tb.getPixWidth(), 0) / tb.getPixWidth());
		double cullDistance = (distInPix * (EPSILON_IN_DPI * Math.max(1, tb.getDensity())));
		cullRamerDouglasPeucker(simplifyPoints, locationProvider, 0, size - 1, cullDistance);
	}

	@NonNull
	public List<Double> getDistances() {
		return distances;
	}

	@NonNull
	public List<Double> getAngles() {
		return angles;
	}

	@NonNull
	public TByteArrayList getSimplifyPoints() {
		return simplifyPoints;
	}
}