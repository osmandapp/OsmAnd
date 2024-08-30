package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.views.layers.geometry.GeometryWay.GeometryWayProvider;
import net.osmand.shared.routing.RouteColorize.RouteColorizationPoint;
//import net.osmand.router.RouteColorize.RouteColorizationPoint;

import java.util.List;

import gnu.trove.list.array.TByteArrayList;

class GradientPathGeometryZoom extends PathGeometryZoom {

	public GradientPathGeometryZoom(GeometryWayProvider locationProvider, RotatedTileBox tb, boolean simplify,
	                                @NonNull List<Integer> forceIncludedIndexes) {
		super(locationProvider, tb, simplify, forceIncludedIndexes);
	}

	@Override
	public void simplify(RotatedTileBox tb, GeometryWayProvider locationProvider, TByteArrayList simplifyPoints) {
		if (locationProvider instanceof GradientGeometryWayProvider) {
			GradientGeometryWayProvider provider = (GradientGeometryWayProvider) locationProvider;
			List<RouteColorizationPoint> simplified = provider.simplify(tb.getZoom());
			if (simplified != null) {
				for (RouteColorizationPoint location : simplified) {
					simplifyPoints.set(location.getId(), (byte) 1);
				}
			}
		}
	}
}
