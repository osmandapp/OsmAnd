package net.osmand.plus.views.layers.geometry;

import static net.osmand.plus.routing.RouteCalculationResult.FIRST_LAST_LOCATION_PROVIDER;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.plus.views.layers.geometry.GeometryWay.GeometryWayProvider;

import java.util.List;

class GeometryWayLocationProvider implements GeometryWayProvider {

	private final List<Location> locations;

	public GeometryWayLocationProvider(@NonNull List<Location> locations) {
		this.locations = locations;
	}

	@Override
	public double getLatitude(int index) {
		return locations.get(index).getLatitude();
	}

	@Override
	public double getLongitude(int index) {
		return locations.get(index).getLongitude();
	}

	@Override
	public int getSize() {
		return locations.size();
	}

	@Override
	public float getHeight(int index) {
		return 0;
	}

	@Override
	public boolean isFirstLastLocation(int index) {
		return FIRST_LAST_LOCATION_PROVIDER.equals(locations.get(index).getProvider());
	}
}
