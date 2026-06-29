/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.router.RouteSegmentResult;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Walks a calculated route and proposes rest stops at mode-specific intervals.
 * Port of driver_break_finder.c and hiking/cycling stop placement in driver_break.c.
 */
public class RestStopFinder {

	private static final Log LOG = PlatformUtil.getLog(RestStopFinder.class);

	/** Max POI references attached to each rest stop marker. */
	static final int MAX_POIS_PER_STOP = 5;

	private final OsmandApplication app;
	private final DriverBreakSettings settings;
	private final PoiDiscovery poiDiscovery;
	private final ExecutorService executor;

	public interface RestStopListener {
		void onRestStopsFound(@NonNull List<RestStop> stops);
	}

	public RestStopFinder(@NonNull OsmandApplication app, @NonNull DriverBreakSettings settings,
			@NonNull PoiDiscovery poiDiscovery, @NonNull ExecutorService executor) {
		this.app = app;
		this.settings = settings;
		this.poiDiscovery = poiDiscovery;
		this.executor = executor;
	}

	/**
	 * Analyze route and find rest stop candidates on a background thread.
	 */
	public void findRestStops(@NonNull RouteCalculationResult route, @NonNull TravelMode mode,
			@NonNull RestStopListener listener) {
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, List<RestStop>>() {
			@Override
			protected List<RestStop> doInBackground(Void... voids) {
				return computeStops(route, mode);
			}

			@Override
			protected void onPostExecute(List<RestStop> restStops) {
				app.runInUIThread(() -> listener.onRestStopsFound(
						restStops != null ? restStops : Collections.emptyList()));
			}
		}, executor);
	}

	@NonNull
	List<RestStop> computeStops(@NonNull RouteCalculationResult route, @NonNull TravelMode mode) {
		List<RouteSegmentResult> segments = route.getOriginalRoute();
		if (segments == null || segments.isEmpty()) {
			return Collections.emptyList();
		}
		double totalDistanceM = 0.0;
		for (RouteSegmentResult segment : segments) {
			totalDistanceM += segment.getDistance();
		}
		List<Double> breakDistancesM = intervalDistancesM(mode, segments, totalDistanceM);
		List<Location> routeLocations = route.getImmutableAllLocations();
		PoiDiscovery.RoutePoiIndex routePois = poiDiscovery.buildRouteIndex(routeLocations, mode);
		List<RestStop> stops = new ArrayList<>();
		for (Double targetM : breakDistancesM) {
			LatLon coord = coordinateAtDistance(segments, targetM);
			if (coord == null) {
				continue;
			}
			stops.add(restStopWithNearbyPois(coord, mode, routePois));
		}
		if (!stops.isEmpty()) {
			int withPois = 0;
			for (RestStop stop : stops) {
				if (!stop.getPois().isEmpty()) {
					withPois++;
				}
			}
			LOG.info("DriverBreak: " + stops.size() + " rest stops, " + withPois + " with nearby POIs");
		}
		return stops;
	}

	@NonNull
	private RestStop restStopWithNearbyPois(@NonNull LatLon coord, @NonNull TravelMode mode,
			@NonNull PoiDiscovery.RoutePoiIndex routePois) {
		List<RestStop.NearbyPoi> pois = limitPois(
				poiDiscovery.poisForStop(routePois, coord.getLatitude(), coord.getLongitude(), mode),
				MAX_POIS_PER_STOP);
		double nearestPoiDistM = pois.isEmpty() ? 0.0 : pois.get(0).getDistanceM();
		return new RestStop(coord, mode, pois, nearestPoiDistM);
	}

	@NonNull
	static List<RestStop.NearbyPoi> limitPois(@NonNull List<RestStop.NearbyPoi> pois, int maxCount) {
		if (pois.size() <= maxCount) {
			return pois;
		}
		return new ArrayList<>(pois.subList(0, maxCount));
	}

	@NonNull
	private List<Double> intervalDistancesM(@NonNull TravelMode mode,
			@NonNull List<RouteSegmentResult> segments, double totalDistanceM) {
		switch (mode) {
			case HIKING:
				return RestStopIntervalHelper.hikingOrCyclingDistancesM(totalDistanceM,
						settings.getHikingMainDistKm(), settings.getHikingAltDistKm(),
						settings.isHikingAltEnabledSync(), settings.getHikingMaxDailyKm());
			case CYCLING:
				return RestStopIntervalHelper.hikingOrCyclingDistancesM(totalDistanceM,
						settings.getCyclingMainDistKm(), settings.getCyclingAltDistKm(),
						settings.isCyclingAltEnabledSync(), settings.getCyclingMaxDailyKm());
			case TRUCK:
				return RestStopIntervalHelper.drivingBreakDistancesM(segments,
						settings.getTruckMandatoryBreakHours() * 3600.0,
						settings.getTruckMaxDailyHours() * 3600.0);
			case MOTORCYCLE:
				return RestStopIntervalHelper.drivingBreakDistancesM(segments,
						settings.getMotorcycleMandatoryBreakMinutes() * 60.0,
						settings.getMotorcycleMaxDailyHours() * 3600.0);
			case CAR:
			default:
				return RestStopIntervalHelper.drivingBreakDistancesM(segments,
						settings.getCarBreakIntervalHours() * 3600.0,
						settings.getCarMaxLimitHours() * 3600.0);
		}
	}

	@Nullable
	private LatLon coordinateAtDistance(@NonNull List<RouteSegmentResult> segments, double targetDistanceM) {
		double accumulated = 0.0;
		for (RouteSegmentResult segment : segments) {
			double segLen = segment.getDistance();
			if (accumulated + segLen >= targetDistanceM) {
				double fraction = (targetDistanceM - accumulated) / segLen;
				LatLon start = segment.getStartPoint();
				LatLon end = segment.getEndPoint();
				if (start == null || end == null) {
					return null;
				}
				double lat = start.getLatitude() + fraction * (end.getLatitude() - start.getLatitude());
				double lon = start.getLongitude() + fraction * (end.getLongitude() - start.getLongitude());
				return new LatLon(lat, lon);
			}
			accumulated += segLen;
		}
		return null;
	}

}
