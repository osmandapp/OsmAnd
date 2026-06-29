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

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates rest-stop coordinates against building and glacier proximity using Overpass.
 * Port of driver_break_finder.c / driver_break_glacier.c checks.
 */
public class PoiLocationValidator {

	private static final Log LOG = PlatformUtil.getLog(PoiLocationValidator.class);

	private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

	/** Initial Overpass retry delay in ms (429/504 backoff). */
	private static final long OVERPASS_RETRY_INITIAL_MS = 2000L;

	/** Backoff multiplier per retry attempt. */
	private static final double OVERPASS_RETRY_MULTIPLIER = 2.0;

	private static final int OVERPASS_MAX_ATTEMPTS = 3;

	/** Single attempt for proximity validation; fail open when Overpass is unavailable. */
	private static final int OVERPASS_VALIDATION_ATTEMPTS = 1;

	/** Minimum spacing between Overpass requests to reduce rate limiting. */
	private static final long OVERPASS_MIN_INTERVAL_MS = 1500L;

	/** Grid size in degrees (~110 m) for building-proximity cache keys. */
	private static final double BUILDING_CACHE_GRID_DEG = 0.001;

	private static final Map<String, Boolean> buildingProximityCache = new ConcurrentHashMap<>();

	private static final Object OVERPASS_LOCK = new Object();
	private static long lastOverpassRequestMs;

	private PoiLocationValidator() {
	}

	/**
	 * @return true when the coordinate is at least {@code minDistBuildingsM} from any building
	 */
	public static boolean isFarEnoughFromBuildings(double lat, double lon, int minDistBuildingsM) {
		if (minDistBuildingsM <= 0) {
			return true;
		}
		String cacheKey = buildingCacheKey(lat, lon, minDistBuildingsM);
		Boolean cached = buildingProximityCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}
		String query = String.format(Locale.US,
				"[out:json][timeout:25];(way[\"building\"](around:%d,%.6f,%.6f););out center;",
				minDistBuildingsM, lat, lon);
		boolean farEnough = countOverpassElements(query, OVERPASS_VALIDATION_ATTEMPTS) == 0;
		buildingProximityCache.put(cacheKey, farEnough);
		return farEnough;
	}

	/**
	 * @return true when no glacier is within {@code minDistGlaciersM} (unless camping building present)
	 */
	public static boolean isFarEnoughFromGlaciers(double lat, double lon, int minDistGlaciersM,
			boolean hasCampingBuilding) {
		if (hasCampingBuilding || minDistGlaciersM <= 0) {
			return true;
		}
		String query = String.format(Locale.US,
				"[out:json][timeout:25];(way[\"natural\"=\"glacier\"](around:%d,%.6f,%.6f););out center;",
				minDistGlaciersM, lat, lon);
		return countOverpassElements(query, OVERPASS_VALIDATION_ATTEMPTS) == 0;
	}

	@NonNull
	static String buildingCacheKey(double lat, double lon, int minDistBuildingsM) {
		long gridLat = Math.round(lat / BUILDING_CACHE_GRID_DEG);
		long gridLon = Math.round(lon / BUILDING_CACHE_GRID_DEG);
		return gridLat + "," + gridLon + "," + minDistBuildingsM;
	}

	private static int countOverpassElements(@NonNull String query) {
		return countOverpassElements(query, OVERPASS_MAX_ATTEMPTS);
	}

	private static int countOverpassElements(@NonNull String query, int maxAttempts) {
		try {
			String response = executeOverpassWithRetry(query, maxAttempts);
			if (Algorithms.isEmpty(response)) {
				return 0;
			}
			JSONObject root = new JSONObject(response);
			JSONArray elements = root.optJSONArray("elements");
			return elements != null ? elements.length() : 0;
		} catch (JSONException e) {
			LOG.warn("DriverBreak: building/glacier Overpass parse failed: " + e.getMessage());
			return 0;
		}
	}

	@NonNull
	static String executeOverpassWithRetry(@NonNull String query) {
		return executeOverpassWithRetry(query, OVERPASS_MAX_ATTEMPTS);
	}

	@NonNull
	static String executeOverpassWithRetry(@NonNull String query, int maxAttempts) {
		long delayMs = OVERPASS_RETRY_INITIAL_MS;
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			try {
				waitForOverpassSlot();
				HttpURLConnection connection = NetworkUtils.getHttpURLConnection(OVERPASS_URL);
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				connection.setConnectTimeout(30000);
				connection.setReadTimeout(120000);
				byte[] body = query.getBytes();
				connection.getOutputStream().write(body);
				int code = connection.getResponseCode();
				if (code == HttpURLConnection.HTTP_OK) {
					return Algorithms.readFromInputStream(connection.getInputStream()).toString();
				}
				if (code == 429 || code == 504) {
					LOG.warn("DriverBreak: Overpass HTTP " + code + ", retry in " + delayMs + " ms");
					Thread.sleep(delayMs);
					delayMs = (long) (delayMs * OVERPASS_RETRY_MULTIPLIER);
					continue;
				}
				LOG.warn("DriverBreak: Overpass HTTP " + code);
				return "";
			} catch (IOException e) {
				LOG.warn("DriverBreak: Overpass IO error: " + e.getMessage());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return "";
			}
		}
		return "";
	}

	private static void waitForOverpassSlot() throws InterruptedException {
		synchronized (OVERPASS_LOCK) {
			long now = System.currentTimeMillis();
			long waitMs = lastOverpassRequestMs + OVERPASS_MIN_INTERVAL_MS - now;
			if (waitMs > 0) {
				Thread.sleep(waitMs);
			}
			lastOverpassRequestMs = System.currentTimeMillis();
		}
	}
}
