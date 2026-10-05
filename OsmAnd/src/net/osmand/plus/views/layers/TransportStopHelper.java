package net.osmand.plus.views.layers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.*;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.views.MapLayers;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.List;

public class TransportStopHelper {

	private final OsmandApplication app;
	private final MapLayers mapLayers;

	public TransportStopHelper(@NonNull Context context) {
		app = (OsmandApplication) context.getApplicationContext();
		mapLayers = app.getOsmandMap().getMapLayers();
	}

	public static void sortTransportStopRoutes(@NonNull List<TransportStopRoute> routes) {
		routes.sort((o1, o2) -> {
//					int radEqual = 50;
//					int dist1 = o1.distance / radEqual;
//					int dist2 = o2.distance / radEqual;
//					if (dist1 != dist2) {
//						return Algorithms.compare(dist1, dist2);
//					}
			int i1 = Algorithms.extractFirstIntegerNumber(o1.route.getRef());
			int i2 = Algorithms.extractFirstIntegerNumber(o2.route.getRef());
			if (i1 != i2) {
				return Algorithms.compare(i1, i2);
			}
			return o1.desc.compareTo(o2.desc);
		});
	}

	@Nullable
	private static List<TransportStop> findTransportStopsAt(@NonNull OsmandApplication app,
			double latitude, double longitude, int radiusMeters) {
		QuadRect ll = MapUtils.calculateLatLonBbox(latitude, longitude, radiusMeters);
		try {
			return app.getResourceManager().searchTransportSync(ll.top, ll.left, ll.bottom, ll.right, null);
		} catch (IOException e) {
			return null;
		}
	}

	@Nullable
	public static TransportStop findBestTransportStopForAmenity(@NonNull OsmandApplication app, @NonNull Amenity amenity) {
		LatLon loc = amenity.getLocation();
		int radiusMeters = TransportStopMatcher.getSearchRadius(amenity);
		List<TransportStop> transportStops = findTransportStopsAt(app, loc.getLatitude(), loc.getLongitude(), radiusMeters);
		if (transportStops == null) {
			return null;
		}
		return TransportStopMatcher.findBestStopForAmenity(transportStops, amenity);
	}

	public static void processTransportStopAggregated(@NonNull OsmandApplication app, @NonNull TransportStop transportStop) {
		TransportStopAggregated stopAggregated = new TransportStopAggregated();
		transportStop.setTransportStopAggregated(stopAggregated);
		TransportStop localStop = null;
		LatLon loc = transportStop.getLocation();
		List<TransportStop> transportStops = findTransportStopsAt(app, loc.getLatitude(), loc.getLongitude(),
				TransportStopMatcher.SHOW_STOPS_RADIUS_METERS);
		if (transportStops != null) {
			for (TransportStop stop : transportStops) {
				if (localStop == null && transportStop.equals(stop)) {
					localStop = stop;
				} else {
					stopAggregated.addNearbyTransportStop(stop);
				}
			}
		}
		stopAggregated.addLocalTransportStop(localStop == null ? transportStop : localStop);
	}

	public static boolean checkSameRoute(@NonNull List<TransportStopRoute> stopRoutes, @NonNull TransportRoute route) {
		for (TransportStopRoute stopRoute : stopRoutes) {
			if (stopRoute.route.compareRoute(route)) {
				return true;
			}
		}
		return false;
	}
}