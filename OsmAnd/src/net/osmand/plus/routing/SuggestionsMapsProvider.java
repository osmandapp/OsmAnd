package net.osmand.plus.routing;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.onlinerouting.EngineParameter;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.onlinerouting.engine.EngineType;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

public class SuggestionsMapsProvider {

	private static final int EARTH_RADIUS = 6371000;
	private static final int MINIMAL_DISTANCE = 20000;
	private static boolean isPointOnWater;

	public static boolean isPointOnWater() {
		return isPointOnWater;
	}

	public static boolean checkIfObjectDownloaded(String downloadName, OsmandApplication app) {
		ResourceManager rm = app.getResourceManager();
		final String regionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		final String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName) + ".road"
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		return rm.getIndexFileNames().containsKey(regionName) || rm.getIndexFileNames().containsKey(roadsRegionName);
	}

	public static List<Location> getLocationBasedOnDistanceInterval(List<Location> points) {
		List<Location> mapsBasedOnPoints = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			int nextIndex = i + 1 < points.size() ? i + 1 : i;
			mapsBasedOnPoints.add(0, points.get(i));
			mapsBasedOnPoints.add(mapsBasedOnPoints.size(), points.get(nextIndex));
			while (mapsBasedOnPoints.get(0).distanceTo(mapsBasedOnPoints.get(mapsBasedOnPoints.size() - 1)) > MINIMAL_DISTANCE) {
				float bearing = mapsBasedOnPoints.get(0).bearingTo(mapsBasedOnPoints.get(mapsBasedOnPoints.size() - 1));
				LatLon latLon = findPointAtDistanceFrom(mapsBasedOnPoints.get(0).getLatitude(), mapsBasedOnPoints.get(0).getLongitude(), MINIMAL_DISTANCE, bearing);
				Location location = new Location("", latLon.getLatitude(), latLon.getLongitude());
				mapsBasedOnPoints.add(0, location);
			}
		}
		return mapsBasedOnPoints;
	}

	public static LatLon findPointAtDistanceFrom(double latitude, double longitude, double distanceInMetres, double bearing) {
		double bearingRad = toRadians(bearing);
		double latRad = toRadians(latitude);
		double lonRad = toRadians(longitude);
		double distFraction = distanceInMetres / EARTH_RADIUS;

		double latitudeResult = asin(sin(latRad) * cos(distFraction) + cos(latRad) * sin(distFraction) * cos(bearingRad));
		double a = atan2(sin(bearingRad) * sin(distFraction) * cos(latRad), cos(distFraction) - sin(latRad) * sin(latitudeResult));
		double longitudeResult = (lonRad + a + 3 * PI) % (2 * PI) - PI;
		return new LatLon(toDegrees(latitudeResult), toDegrees(longitudeResult));
	}

	public static LinkedList<Location> getStartFinishIntermediatesPoints(RouteCalculationParams params, String locationProvider) {
		LinkedList<Location> points = new LinkedList<>();
		points.add(new Location(locationProvider, params.start.getLatitude(), params.start.getLongitude()));
		if (params.intermediates != null) {
			for (LatLon l : params.intermediates) {
				points.add(new Location(locationProvider, l.getLatitude(), l.getLongitude()));
			}
		}
		points.add(new Location(locationProvider, params.end.getLatitude(), params.end.getLongitude()));
		return points;
	}

	public static LinkedList<Location> findOnlineRoutePoints(RouteCalculationParams params) throws IOException, JSONException {
		List<LatLon> points = new ArrayList<>();
		List<Location> route;
		LinkedList<Location> routeLocation = new LinkedList<>();
		OsmandApplication app = params.ctx;
		OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
		LinkedList<Location> location = getStartFinishIntermediatesPoints(params, "");
		for (Location e : location) {
			points.add(new LatLon(e.getLongitude(), e.getLatitude()));
		}
		String engineKey = params.mode.getRoutingProfile();
		OnlineRoutingEngine.OnlineRoutingResponse response =
				helper.calculateRouteOnline(engineKey, points, params.leftSide);

		if (response != null) {
			route = response.getRoute();
			routeLocation.addAll(route);
		}

		return routeLocation;
	}

	public static OnlineRoutingEngine createInitStateEngine(RouteCalculationParams params) {
		Map<String, String> paramsOnlineRouting = new HashMap<>();
		paramsOnlineRouting.put(EngineParameter.VEHICLE_KEY.name(), params.mode.getRoutingProfile());
		return EngineType.OSRM_TYPE.newInstance(paramsOnlineRouting);
	}

	public static List<WorldRegion> getSuggestedMaps(List<Location> points, OsmandApplication app) throws IOException {
		LinkedHashSet<WorldRegion> suggestedMaps = new LinkedHashSet<>();
		for (int i = 0; i < points.size(); i++) {
			final Location o = points.get(i);
			LatLon latLonPoint = new LatLon(o.getLatitude(), o.getLongitude());
			List<WorldRegion> downloadRegions = app.getRegions().getWorldRegionsAt(latLonPoint);

			if (downloadRegions.isEmpty()) {
				isPointOnWater = true;
			}

			List<Boolean> mapsDownloadedList = new ArrayList<>();
			List<WorldRegion> maps = new ArrayList<>();
			for (WorldRegion downloadRegion : downloadRegions) {
				String mapName = downloadRegion.getRegionDownloadName();
				String countryMapName = downloadRegion.getSuperregion().getRegionDownloadName();
				List<WorldRegion> subregions = downloadRegion.getSubregions();
				boolean isSuggestedMapsNeeded = !checkIfObjectDownloaded(mapName, app);
				boolean isCountry = Algorithms.isEmpty(countryMapName) && !subregions.isEmpty();
				if (isSuggestedMapsNeeded) {
					if (!isCountry) {
						maps.add(downloadRegion);
					}
					mapsDownloadedList.add(true);
				} else {
					mapsDownloadedList.add(false);
				}
			}
			if (!mapsDownloadedList.contains(false)) {
				suggestedMaps.addAll(maps);
			}
		}

		return new ArrayList<>(suggestedMaps);
	}
}
