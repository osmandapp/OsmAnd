package net.osmand.router;

import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;

public class DistanceProvider implements IDistanceProvider {
    @Override
    public double getDistance(LatLon latLon1, LatLon latLon2) {
        return MapUtils.getDistance(latLon1, latLon2);
    }

    @Override
    public double getDistance(LatLon latLon, double lat, double lon) {
        return MapUtils.getDistance(latLon, lat, lon);
    }
}
