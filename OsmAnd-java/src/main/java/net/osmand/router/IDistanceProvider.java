package net.osmand.router;

import net.osmand.data.LatLon;

public interface IDistanceProvider {
    double getDistance(LatLon latLon1, LatLon latLon2);
    double getDistance(LatLon latLon, double lat, double lon);
}
