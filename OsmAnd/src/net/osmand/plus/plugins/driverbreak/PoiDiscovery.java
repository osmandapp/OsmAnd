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
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Map-based POI search with Overpass API fallback. Port of driver_break_poi.c / driver_break_poi_map.c.
 */
public class PoiDiscovery {

	private static final Log LOG = PlatformUtil.getLog(PoiDiscovery.class);

	private static final long OVERPASS_CACHE_MS = 10L * 60L * 1000L;

	/** Minimum map POI hits before skipping Overpass for a single-point lookup. */
	private static final int MIN_MAP_POIS_BEFORE_OVERPASS = 2;

	/** Max Overpass queries when the offline map index is empty. */
	private static final int MAX_OVERPASS_ROUTE_SAMPLES = 3;

	private static final String[] DNT_NETWORK_MARKERS = {
			"dnt", "stf", "dav", "sac", "oeav", "metsähallitus", "den norske turistforening",
			"svenska turistföreningen"
	};

	private final OsmandApplication app;
	private final DriverBreakSettings settings;
	private final ExecutorService downloadExecutor;
	private final Map<String, CachedOverpassResult> overpassCache = new HashMap<>();
	private final EnumMap<TravelMode, List<PoiUIFilter>> mapFilterCache = new EnumMap<>(TravelMode.class);

	public interface PoiResultListener {
		void onResult(@NonNull List<RestStop.NearbyPoi> pois);
	}

	/** POIs collected once along the route, then matched to each rest stop by distance. */
	static final class RoutePoiIndex {
		private final List<RestStop.NearbyPoi> pois;

		RoutePoiIndex(@NonNull List<RestStop.NearbyPoi> pois) {
			this.pois = pois;
		}

		boolean isEmpty() {
			return pois.isEmpty();
		}

		@NonNull
		List<RestStop.NearbyPoi> withinRadius(double lat, double lon, int radiusM, boolean dntPriority) {
			List<RestStop.NearbyPoi> hits = new ArrayList<>();
			for (RestStop.NearbyPoi poi : pois) {
				double dist = MapUtils.getDistance(lat, lon, poi.getLocation().getLatitude(),
						poi.getLocation().getLongitude());
				if (dist <= radiusM) {
					hits.add(new RestStop.NearbyPoi(poi.getLocation(), poi.getName(), poi.getCategory(), dist,
							poi.isNetworkPriority()));
				}
			}
			return sortPois(hits, lat, lon, dntPriority);
		}
	}

	public PoiDiscovery(@NonNull OsmandApplication app, @NonNull DriverBreakSettings settings) {
		this.app = app;
		this.settings = settings;
		this.downloadExecutor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "driver-break-poi");
			t.setDaemon(true);
			return t;
		});
	}

	/**
	 * Find nearby POIs for a rest-stop candidate.
	 */
	public void findNearby(double lat, double lon, @NonNull TravelMode mode, @NonNull PoiResultListener listener) {
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, List<RestStop.NearbyPoi>>() {
			@Override
			protected List<RestStop.NearbyPoi> doInBackground(Void... voids) {
				return findNearbySync(lat, lon, mode);
			}

			@Override
			protected void onPostExecute(List<RestStop.NearbyPoi> result) {
				app.runInUIThread(() -> listener.onResult(result != null ? result : Collections.emptyList()));
			}
		}, downloadExecutor);
	}

	/**
	 * Build a route-wide POI index from offline map data (one path search instead of per-stop queries).
	 */
	@NonNull
	RoutePoiIndex buildRouteIndex(@NonNull List<Location> routeLocations, @NonNull TravelMode mode) {
		long startedMs = System.currentTimeMillis();
		if (routeLocations.isEmpty()) {
			return new RoutePoiIndex(Collections.emptyList());
		}
		int radiusM = radiusForMode(mode);
		List<Location> path = sampleRouteLocations(routeLocations, Math.max(500, radiusM / 2));
		List<PoiUIFilter> filters = resolveMapFilters(mode);
		if (filters.isEmpty()) {
			LOG.warn("DriverBreak: no map POI filters resolved for mode " + mode.getConfigKey());
			return new RoutePoiIndex(Collections.emptyList());
		}
		PoiUIFilter searchFilter = filters.size() == 1
				? filters.get(0)
				: new PoiUIFilter(new HashSet<>(filters), app);
		List<Amenity> amenities;
		try {
			amenities = searchFilter.searchAmenitiesOnThePath(path, radiusM);
		} catch (RuntimeException e) {
			LOG.warn("DriverBreak: route POI search failed: " + e.getMessage());
			amenities = Collections.emptyList();
		}
		List<RestStop.NearbyPoi> pois = amenitiesToNearbyPois(amenities, mode, null, radiusM);
		if (pois.isEmpty()) {
			pois = searchOverpassAlongRoute(path, mode);
		}
		LOG.info("DriverBreak: route POI index " + pois.size() + " POIs, "
				+ path.size() + " path samples, " + filters.size() + " filters, "
				+ (System.currentTimeMillis() - startedMs) + " ms");
		return new RoutePoiIndex(pois);
	}

	/**
	 * POIs near a rest stop using a pre-built route index.
	 */
	@NonNull
	List<RestStop.NearbyPoi> poisForStop(@NonNull RoutePoiIndex index, double lat, double lon,
			@NonNull TravelMode mode) {
		return index.withinRadius(lat, lon, radiusForMode(mode), settings.isDntPrioritySync());
	}

	/**
	 * Synchronous POI lookup for a single coordinate. Call from a background thread only.
	 */
	@NonNull
	List<RestStop.NearbyPoi> findNearbySync(double lat, double lon, @NonNull TravelMode mode) {
		if (!isValidSearchLocation(lat, lon)) {
			return Collections.emptyList();
		}
		List<RestStop.NearbyPoi> mapResults = searchMap(lat, lon, mode);
		List<RestStop.NearbyPoi> merged;
		if (mapResults.size() >= MIN_MAP_POIS_BEFORE_OVERPASS) {
			merged = mapResults;
		} else {
			List<RestStop.NearbyPoi> overpass = searchOverpass(lat, lon, mode);
			merged = mergeDistinct(mapResults, overpass);
		}
		return sortPois(merged, lat, lon, settings.isDntPrioritySync());
	}

	private boolean isValidSearchLocation(double lat, double lon) {
		int minBuildingsM = settings.getMinDistBuildingsM();
		int minGlaciersM = settings.getMinDistGlaciersM();
		if (!PoiLocationValidator.isFarEnoughFromBuildings(lat, lon, minBuildingsM)) {
			LOG.debug("DriverBreak: POI search skipped — too close to buildings");
			return false;
		}
		if (!PoiLocationValidator.isFarEnoughFromGlaciers(lat, lon, minGlaciersM, false)) {
			LOG.debug("DriverBreak: POI search skipped — too close to glacier");
			return false;
		}
		return true;
	}

	@NonNull
	private List<RestStop.NearbyPoi> searchMap(double lat, double lon, @NonNull TravelMode mode) {
		int radiusM = radiusForMode(mode);
		QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, radiusM);
		List<PoiUIFilter> filters = resolveMapFilters(mode);
		List<RestStop.NearbyPoi> results = new ArrayList<>();
		Set<Long> seenIds = new HashSet<>();
		for (PoiUIFilter filter : filters) {
			List<Amenity> amenities;
			try {
				amenities = filter.searchAmenities(rect.top, rect.left, rect.bottom, rect.right, -1, null, true);
			} catch (RuntimeException e) {
				LOG.warn("DriverBreak: map POI search failed: " + e.getMessage());
				continue;
			}
			if (amenities == null) {
				continue;
			}
			results.addAll(amenitiesToNearbyPois(amenities, mode, new LatLon(lat, lon), radiusM, seenIds));
		}
		return results;
	}

	@NonNull
	private List<PoiUIFilter> resolveMapFilters(@NonNull TravelMode mode) {
		List<PoiUIFilter> cached = mapFilterCache.get(mode);
		if (cached != null) {
			return cached;
		}
		List<PoiUIFilter> resolved = new ArrayList<>();
		for (PoiUIFilter filter : app.getPoiFilters().getTopDefinedPoiFilters()) {
			if (isRelevantMapFilter(filter, mode)) {
				resolved.add(filter);
			}
		}
		mapFilterCache.put(mode, resolved);
		return resolved;
	}

	@NonNull
	private List<RestStop.NearbyPoi> amenitiesToNearbyPois(@NonNull List<Amenity> amenities,
			@NonNull TravelMode mode, @Nullable LatLon from, int radiusM) {
		return amenitiesToNearbyPois(amenities, mode, from, radiusM, new HashSet<>());
	}

	@NonNull
	private List<RestStop.NearbyPoi> amenitiesToNearbyPois(@NonNull List<Amenity> amenities,
			@NonNull TravelMode mode, @Nullable LatLon from, int radiusM, @NonNull Set<Long> seenIds) {
		List<RestStop.NearbyPoi> results = new ArrayList<>();
		for (Amenity amenity : amenities) {
			if (!matchesAmenityTags(amenity, mode)) {
				continue;
			}
			long id = amenity.getId();
			if (!seenIds.add(id)) {
				continue;
			}
			LatLon location = amenity.getLocation();
			double dist = from != null
					? MapUtils.getDistance(from.getLatitude(), from.getLongitude(),
					location.getLatitude(), location.getLongitude())
					: 0.0;
			if (from != null && dist > radiusM) {
				continue;
			}
			boolean networkPriority = isNetworkPriorityPoi(amenity);
			results.add(new RestStop.NearbyPoi(location, amenity.getName(), amenity.getSubType(), dist,
					networkPriority));
		}
		return results;
	}

	@NonNull
	private List<RestStop.NearbyPoi> searchOverpassAlongRoute(@NonNull List<Location> path,
			@NonNull TravelMode mode) {
		if (path.isEmpty()) {
			return Collections.emptyList();
		}
		List<RestStop.NearbyPoi> merged = new ArrayList<>();
		Set<String> keys = new HashSet<>();
		for (int index : overpassSampleIndices(path.size(), MAX_OVERPASS_ROUTE_SAMPLES)) {
			Location location = path.get(index);
			List<RestStop.NearbyPoi> batch = searchOverpass(location.getLatitude(), location.getLongitude(), mode);
			for (RestStop.NearbyPoi poi : batch) {
				if (keys.add(poiKey(poi))) {
					merged.add(poi);
				}
			}
		}
		return merged;
	}

	@NonNull
	private List<RestStop.NearbyPoi> searchOverpass(double lat, double lon, @NonNull TravelMode mode) {
		String cacheKey = String.format(Locale.US, "%.2f,%.2f,%s", lat, lon, mode.getConfigKey());
		CachedOverpassResult cached = overpassCache.get(cacheKey);
		long now = System.currentTimeMillis();
		if (cached != null && now - cached.timestampMs < OVERPASS_CACHE_MS) {
			return cached.pois;
		}
		String query = buildOverpassQuery(lat, lon, mode, radiusForMode(mode));
		try {
			String response = PoiLocationValidator.executeOverpassWithRetry(query);
			if (Algorithms.isEmpty(response)) {
				return Collections.emptyList();
			}
			List<RestStop.NearbyPoi> parsed = parseOverpassJson(response, lat, lon);
			overpassCache.put(cacheKey, new CachedOverpassResult(now, parsed));
			return parsed;
		} catch (JSONException e) {
			LOG.warn("DriverBreak: Overpass query failed: " + e.getMessage());
			return Collections.emptyList();
		}
	}

	@NonNull
	static String buildOverpassQuery(double lat, double lon, @NonNull TravelMode mode, int radiusM) {
		String[] tags = overpassTagsForMode(mode);
		StringBuilder query = new StringBuilder("[out:json][timeout:25];(");
		for (int i = 0; i < tags.length; i++) {
			if (i > 0) {
				query.append(';');
			}
			String[] kv = tags[i].split("=", 2);
			if (kv.length == 2) {
				query.append(String.format(Locale.US, "node[\"%s\"=\"%s\"](around:%d,%.6f,%.6f);",
						kv[0], kv[1], radiusM, lat, lon));
			}
		}
		query.append(");out body;");
		return query.toString();
	}

	@NonNull
	private static String[] overpassTagsForMode(@NonNull TravelMode mode) {
		switch (mode) {
			case HIKING:
				return new String[] {
						"amenity=drinking_water", "amenity=fountain", "natural=spring",
						"tourism=wilderness_hut", "tourism=alpine_hut", "tourism=hostel",
						"tourism=camp_site", "amenity=place_of_worship"
				};
			case CYCLING:
				return new String[] {
						"amenity=drinking_water", "shop=bicycle", "amenity=bicycle_repair_station",
						"amenity=charging_station", "tourism=alpine_hut", "amenity=fuel"
				};
			case MOTORCYCLE:
				return new String[] {
						"amenity=fuel", "amenity=cafe", "amenity=restaurant", "tourism=viewpoint"
				};
			case TRUCK:
			case CAR:
			default:
				return new String[] {
						"amenity=fuel", "amenity=cafe", "amenity=restaurant", "tourism=museum",
						"tourism=viewpoint"
				};
		}
	}

	@NonNull
	static List<RestStop.NearbyPoi> parseOverpassJson(@NonNull String json, double lat, double lon)
			throws JSONException {
		List<RestStop.NearbyPoi> pois = new ArrayList<>();
		JSONObject root = new JSONObject(json);
		JSONArray elements = root.optJSONArray("elements");
		if (elements == null) {
			return pois;
		}
		for (int i = 0; i < elements.length(); i++) {
			JSONObject node = elements.getJSONObject(i);
			if (!node.has("lat") || !node.has("lon")) {
				continue;
			}
			double nLat = node.getDouble("lat");
			double nLon = node.getDouble("lon");
			JSONObject tags = node.optJSONObject("tags");
			String name = tags != null ? tags.optString("name", "") : "";
			String category = categoryFromTags(tags);
			double dist = MapUtils.getDistance(lat, lon, nLat, nLon);
			boolean networkPriority = tags != null && isNetworkTag(tags.optString("operator"), tags.optString("network"));
			pois.add(new RestStop.NearbyPoi(new LatLon(nLat, nLon), name, category, dist, networkPriority));
		}
		return pois;
	}

	@Nullable
	private static String categoryFromTags(@Nullable JSONObject tags) {
		if (tags == null) {
			return "overpass";
		}
		if (tags.has("amenity")) {
			return tags.optString("amenity");
		}
		if (tags.has("tourism")) {
			return tags.optString("tourism");
		}
		if (tags.has("natural")) {
			return tags.optString("natural");
		}
		if (tags.has("shop")) {
			return tags.optString("shop");
		}
		return "overpass";
	}

	@NonNull
	static List<Location> sampleRouteLocations(@NonNull List<Location> routeLocations, double intervalM) {
		if (routeLocations.size() <= 2 || intervalM <= 0) {
			return new ArrayList<>(routeLocations);
		}
		List<Location> sampled = new ArrayList<>();
		sampled.add(routeLocations.get(0));
		double sinceLastM = 0.0;
		for (int i = 1; i < routeLocations.size(); i++) {
			Location previous = routeLocations.get(i - 1);
			Location current = routeLocations.get(i);
			sinceLastM += MapUtils.getDistance(previous.getLatitude(), previous.getLongitude(),
					current.getLatitude(), current.getLongitude());
			if (sinceLastM >= intervalM) {
				sampled.add(current);
				sinceLastM = 0.0;
			}
		}
		Location last = routeLocations.get(routeLocations.size() - 1);
		Location tail = sampled.get(sampled.size() - 1);
		if (tail.getLatitude() != last.getLatitude() || tail.getLongitude() != last.getLongitude()) {
			sampled.add(last);
		}
		return sampled;
	}

	@NonNull
	static int[] overpassSampleIndices(int pathSize, int maxSamples) {
		if (pathSize <= 0) {
			return new int[0];
		}
		if (pathSize == 1 || maxSamples <= 1) {
			return new int[] {0};
		}
		int samples = Math.min(maxSamples, pathSize);
		int[] indices = new int[samples];
		for (int i = 0; i < samples; i++) {
			indices[i] = (int) Math.round((double) i * (pathSize - 1) / (samples - 1));
		}
		return indices;
	}

	static boolean isRelevantMapFilter(@NonNull PoiUIFilter filter, @NonNull TravelMode mode) {
		if (filter.isRoutesFilter() || filter.isWikiFilter()
				|| filter.isRouteArticleFilter() || filter.isRouteArticlePointFilter()) {
			return false;
		}
		String filterId = filter.getFilterId().toLowerCase(Locale.US);
		if (filterId.contains("routes_") || filterId.contains("route_article")) {
			return false;
		}
		if (mode == TravelMode.HIKING && (filterId.contains("charging") || filterId.contains("bicycle"))) {
			return false;
		}
		Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = filter.getAcceptedTypes();
		if (acceptedTypes == null || acceptedTypes.isEmpty()) {
			return false;
		}
		for (Map.Entry<PoiCategory, LinkedHashSet<String>> entry : acceptedTypes.entrySet()) {
			LinkedHashSet<String> subTypes = entry.getValue();
			if (subTypes == null) {
				continue;
			}
			for (String subType : subTypes) {
				if (matchesAmenitySubType(subType, mode)) {
					return true;
				}
			}
		}
		return false;
	}

	static boolean matchesAmenitySubType(@Nullable String subType, @NonNull TravelMode mode) {
		if (subType == null) {
			return false;
		}
		switch (mode) {
			case HIKING:
				return "drinking_water".equals(subType) || "spring".equals(subType) || "fountain".equals(subType)
						|| "wilderness_hut".equals(subType) || "alpine_hut".equals(subType)
						|| "hostel".equals(subType) || "camp_site".equals(subType)
						|| "place_of_worship".equals(subType);
			case CYCLING:
				return "drinking_water".equals(subType) || "spring".equals(subType) || "bicycle".equals(subType)
						|| "bicycle_repair_station".equals(subType) || "charging_station".equals(subType)
						|| "alpine_hut".equals(subType) || "fuel".equals(subType);
			case MOTORCYCLE:
				return "fuel".equals(subType) || "cafe".equals(subType) || "restaurant".equals(subType)
						|| "viewpoint".equals(subType);
			case TRUCK:
			case CAR:
			default:
				return "fuel".equals(subType) || "cafe".equals(subType) || "restaurant".equals(subType)
						|| "viewpoint".equals(subType) || "museum".equals(subType);
		}
	}

	private int radiusForMode(@NonNull TravelMode mode) {
		switch (mode) {
			case HIKING:
				if (settings.isWaterPoisEnabledSync()) {
					return settings.getWaterRadiusM();
				}
				return settings.getCabinRadiusM();
			case CYCLING:
				return settings.getPoiRadiusM();
			default:
				return settings.getPoiRadiusM();
		}
	}

	private static boolean matchesAmenityTags(@NonNull Amenity amenity, @NonNull TravelMode mode) {
		return matchesAmenitySubType(amenity.getSubType(), mode);
	}

	static boolean isNetworkPriorityPoi(@NonNull Amenity amenity) {
		String operator = amenity.getAdditionalInfo(Amenity.OPERATOR);
		String network = amenity.getAdditionalInfo("network");
		if (isNetworkTag(operator, network)) {
			return true;
		}
		String name = amenity.getName();
		return name != null && isNetworkTag(name, null);
	}

	static boolean isNetworkTag(@Nullable String operator, @Nullable String network) {
		if (containsNetworkMarker(operator)) {
			return true;
		}
		return containsNetworkMarker(network);
	}

	private static boolean containsNetworkMarker(@Nullable String value) {
		if (Algorithms.isEmpty(value)) {
			return false;
		}
		String lower = value.toLowerCase(Locale.US);
		for (String marker : DNT_NETWORK_MARKERS) {
			if (lower.contains(marker)) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	private static List<RestStop.NearbyPoi> mergeDistinct(@NonNull List<RestStop.NearbyPoi> first,
			@NonNull List<RestStop.NearbyPoi> second) {
		List<RestStop.NearbyPoi> merged = new ArrayList<>(first);
		Set<String> keys = new HashSet<>();
		for (RestStop.NearbyPoi poi : first) {
			keys.add(poiKey(poi));
		}
		for (RestStop.NearbyPoi poi : second) {
			if (keys.add(poiKey(poi))) {
				merged.add(poi);
			}
		}
		return merged;
	}

	@NonNull
	private static String poiKey(@NonNull RestStop.NearbyPoi poi) {
		LatLon loc = poi.getLocation();
		return String.format(Locale.US, "%.4f,%.4f", loc.getLatitude(), loc.getLongitude());
	}

	@NonNull
	static List<RestStop.NearbyPoi> sortPois(@NonNull List<RestStop.NearbyPoi> pois, double lat, double lon,
			boolean dntPriority) {
		pois.sort((a, b) -> {
			if (dntPriority) {
				if (a.isNetworkPriority() && !b.isNetworkPriority()) {
					return -1;
				}
				if (!a.isNetworkPriority() && b.isNetworkPriority()) {
					return 1;
				}
			}
			double da = MapUtils.getDistance(lat, lon, a.getLocation().getLatitude(), a.getLocation().getLongitude());
			double db = MapUtils.getDistance(lat, lon, b.getLocation().getLatitude(), b.getLocation().getLongitude());
			return Double.compare(da, db);
		});
		return pois;
	}

	private static final class CachedOverpassResult {
		final long timestampMs;
		final List<RestStop.NearbyPoi> pois;

		CachedOverpassResult(long timestampMs, @NonNull List<RestStop.NearbyPoi> pois) {
			this.timestampMs = timestampMs;
			this.pois = pois;
		}
	}
}
