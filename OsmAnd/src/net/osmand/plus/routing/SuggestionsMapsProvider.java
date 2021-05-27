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
import net.osmand.util.Algorithms;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static net.osmand.util.MapUtils.movePointToDistance;

public class SuggestionsMapsProvider {

	private static final int DISTANCE = 20000;
	RouteCalculationParams params;
	private boolean pointOnWater;

	public SuggestionsMapsProvider(RouteCalculationParams params) {
		this.params = params;
	}

	public boolean isPointOnWater() {
		return pointOnWater;
	}

	public boolean checkIfObjectDownloaded(String downloadName) {
		final String regionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		final String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName) + ".road"
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		return params.ctx.getResourceManager().getIndexFileNames().containsKey(regionName) || params.ctx.getResourceManager().getIndexFileNames().containsKey(roadsRegionName);
	}

	public List<Location> getLocationBasedOnDistanceInterval(List<Location> points) {
		List<Location> mapsBasedOnPoints = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			int nextIndex = i + 1 < points.size() ? i + 1 : i;
			mapsBasedOnPoints.add(0, points.get(i));
			mapsBasedOnPoints.add(mapsBasedOnPoints.size(), points.get(nextIndex));
			while (mapsBasedOnPoints.get(0).distanceTo(mapsBasedOnPoints.get(mapsBasedOnPoints.size() - 1)) > DISTANCE) {
				float bearing = mapsBasedOnPoints.get(0).bearingTo(mapsBasedOnPoints.get(mapsBasedOnPoints.size() - 1));
				Location location = movePointToDistance(mapsBasedOnPoints.get(0).getLatitude(), mapsBasedOnPoints.get(0).getLongitude(), DISTANCE, bearing);
				mapsBasedOnPoints.add(0, location);
			}
		}
		return mapsBasedOnPoints;
	}

	public LinkedList<Location> getStartFinishIntermediatesPoints(String locationProvider) {
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

	public List<Location> findOnlineRoutePoints() throws IOException, JSONException {
		List<LatLon> points = new ArrayList<>();
		List<Location> route;
		List<Location> routeLocation = new ArrayList<>();
		OsmandApplication app = params.ctx;
		OnlineRoutingHelper helper = app.getOnlineRoutingHelper();
		LinkedList<Location> location = getStartFinishIntermediatesPoints("");
		for (Location e : location) {
			points.add(new LatLon(e.getLatitude(), e.getLongitude()));
		}
		OnlineRoutingEngine.OnlineRoutingResponse response =
				helper.calculateRouteOnline(createInitStateEngine(), points, params.leftSide);
		if (response != null) {
			route = response.getRoute();
			routeLocation.addAll(route);
		}
		List<Location> mapsBasedOnPoints = new ArrayList<>();
		for (int i = 0; i < routeLocation.size(); i++) {
			for (int j = i + 1; j < routeLocation.size(); j++) {
				if (routeLocation.get(i).distanceTo(routeLocation.get(j)) > DISTANCE) {
					mapsBasedOnPoints.add(routeLocation.get(j));
					i = j;
				}
			}
		}
		return mapsBasedOnPoints;
	}

	private OnlineRoutingEngine createInitStateEngine() {
		Map<String, String> paramsOnlineRouting = new HashMap<>();
		paramsOnlineRouting.put(EngineParameter.VEHICLE_KEY.name(), params.mode.getRoutingProfile());
		return EngineType.OSRM_TYPE.newInstance(paramsOnlineRouting);
	}

	public List<WorldRegion> getMissingMaps(List<Location> points) throws IOException {
		LinkedHashSet<WorldRegion> suggestedMaps = new LinkedHashSet<>();
		for (int i = 0; i < points.size(); i++) {
			final Location o = points.get(i);
			LatLon latLonPoint = new LatLon(o.getLatitude(), o.getLongitude());
			List<WorldRegion> downloadRegions = params.ctx.getRegions().getWorldRegionsAt(latLonPoint);

			if (downloadRegions.isEmpty()) {
				pointOnWater = true;
			}

			boolean addMaps = true;
			List<WorldRegion> maps = new ArrayList<>();
			for (WorldRegion downloadRegion : downloadRegions) {
				String mapName = downloadRegion.getRegionDownloadName();
				String countryMapName = downloadRegion.getSuperregion().getRegionDownloadName();
				List<WorldRegion> subregions = downloadRegion.getSubregions();
				boolean isSuggestedMapsNeeded = !checkIfObjectDownloaded(mapName);
				boolean isCountry = Algorithms.isEmpty(countryMapName) && !subregions.isEmpty();
				if (isSuggestedMapsNeeded) {
					if (!isCountry) {
						maps.add(downloadRegion);
					}
				} else {
					addMaps = false;
				}
			}
			if (addMaps) {
				suggestedMaps.addAll(maps);
			}
		}
		return new ArrayList<>(suggestedMaps);
	}
}
