package net.osmand.plus.plugins.aistracker;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.QuadRect;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.shared.aistracker.AisObject;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Fetches aircraft positions from a plane {@link AisUrlSource} and turns them into
 * {@link AisObject} instances (message type 9 = airborne position report), so aircraft reuse the
 * existing AIS rendering and tap-menu pipeline alongside vessels.
 * <p>
 * Two response layouts are recognised, picked by what the server actually returns:
 * <ul>
 *     <li>OpenSky Network - {@code {"states": [[icao24, callsign, .., lon, lat, ..], ..]}},
 *     speed in m/s and altitude in metres;</li>
 *     <li>ADS-B Exchange and the community services serving the same payload (adsb.lol under
 *     {@code "ac"}, adsb.fi under {@code "aircraft"}) - {@code [{"hex": .., "lat": .., "gs": ..}]},
 *     speed in knots and altitude in feet.</li>
 * </ul>
 * Runs synchronously - callers are expected to invoke this from a background thread.
 */
public class AisPlaneDataFetcher {

	/** Shared with AisTrackerPlugin so the whole plane pipeline reads as one log: adb logcat -s AisPlanes */
	public static final String TAG = "AisPlanes";

	private static final int LOGGED_BODY_LIMIT = 700;

	// net.osmand.shared.aistracker.AisObjectConstants values, duplicated here to avoid
	// Kotlin-object interop from Java for a handful of constants.
	private static final int INVALID_ALTITUDE = 4095;
	private static final double INVALID_COG = 360.0;
	private static final double INVALID_SOG = 1023.0;
	private static final int MSG_TYPE_AIRCRAFT_POSITION = 9;

	private static final String API_KEY_PLACEHOLDER = "{API_KEY}";
	private static final String RAPIDAPI_HOST_SUFFIX = ".p.rapidapi.com";
	private static final int CONNECT_TIMEOUT = 15000;
	private static final int READ_TIMEOUT = 30000;
	private static final double FEET_TO_METERS = 0.3048;
	private static final double MS_TO_KNOTS = 3600.0 / 1852.0;
	private static final double METERS_PER_NM = 1852.0;
	private static final double MIN_RADIUS_NM = 5;
	// the ADS-B services cap the radius at 250 nm
	private static final double MAX_RADIUS_NM = 250;

	private static final String NUMBER = "-?\\d+(?:\\.\\d+)?";
	private static final Pattern PLACEHOLDER =
			Pattern.compile("\\{(LAT|LON|DIST|LAMIN|LOMIN|LAMAX|LOMAX)}");
	private static final Pattern LAT_LON_DIST_PATH =
			Pattern.compile("/lat/" + NUMBER + "/lon/" + NUMBER + "/dist/" + NUMBER);
	private static final Pattern POINT_PATH =
			Pattern.compile("/point/" + NUMBER + "/" + NUMBER + "/" + NUMBER);

	private AisPlaneDataFetcher() {
	}

	/**
	 * Placeholders filled in from the visible map area, so a source follows the map instead of
	 * being pinned to whatever coordinates were typed when it was created. Bounding-box services
	 * (OpenSky) and centre+radius services (the ADS-B family) each take the set they need.
	 */
	@NonNull
	public static String applyMapArea(@NonNull String url, @NonNull QuadRect latLonBounds) {
		double north = latLonBounds.top;
		double south = latLonBounds.bottom;
		double west = latLonBounds.left;
		double east = latLonBounds.right;
		double centerLat = (north + south) / 2;
		double centerLon = (west + east) / 2;
		// radius covering the viewport corner, which is what centre+radius services ask for
		double distNm = Math.min(MAX_RADIUS_NM, Math.max(MIN_RADIUS_NM,
				MapUtils.getDistance(centerLat, centerLon, north, east) / METERS_PER_NM));
		String dist = String.valueOf(Math.round(distNm));

		if (PLACEHOLDER.matcher(url).find()) {
			return url.replace("{LAMIN}", format(south))
					.replace("{LAMAX}", format(north))
					.replace("{LOMIN}", format(west))
					.replace("{LOMAX}", format(east))
					.replace("{LAT}", format(centerLat))
					.replace("{LON}", format(centerLon))
					.replace("{DIST}", dist);
		}
		return replaceLiteralCoordinates(url, format(centerLat), format(centerLon), dist,
				format(south), format(west), format(north), format(east));
	}

	/**
	 * A URL without placeholders - typed by hand or saved before placeholders existed - still has
	 * to follow the map, so coordinates written in the shapes these services use are rewritten to
	 * the current area. Anything unrecognised is left alone.
	 */
	@NonNull
	private static String replaceLiteralCoordinates(@NonNull String url, @NonNull String lat,
	                                                 @NonNull String lon, @NonNull String dist,
	                                                 @NonNull String lamin, @NonNull String lomin,
	                                                 @NonNull String lamax, @NonNull String lomax) {
		String result = url;
		// /lat/50.03/lon/8.56/dist/50 - adsb.lol, adsb.fi, ADS-B Exchange
		result = LAT_LON_DIST_PATH.matcher(result)
				.replaceAll("/lat/" + lat + "/lon/" + lon + "/dist/" + dist);
		// /point/50.03/8.56/50 - airplanes.live
		result = POINT_PATH.matcher(result).replaceAll("/point/" + lat + "/" + lon + "/" + dist);
		// lamin=45&lomin=5&lamax=55&lomax=20 - OpenSky, in whatever order they appear
		result = replaceQueryValue(result, "lamin", lamin);
		result = replaceQueryValue(result, "lomin", lomin);
		result = replaceQueryValue(result, "lamax", lamax);
		result = replaceQueryValue(result, "lomax", lomax);
		if (!result.equals(url)) {
			Log.d(TAG, "rewrote fixed coordinates to the current map area: " + result);
		}
		return result;
	}

	@NonNull
	private static String replaceQueryValue(@NonNull String url, @NonNull String param,
	                                         @NonNull String value) {
		return Pattern.compile("([?&]" + param + "=)" + NUMBER)
				.matcher(url).replaceAll("$1" + value);
	}

	@NonNull
	private static String format(double value) {
		return String.format(Locale.US, "%.4f", value);
	}

	/**
	 * @return the aircraft in view, empty when the service legitimately reports none, or null when
	 * the request failed - callers must not treat a failure as "everything is gone".
	 */
	@Nullable
	public static List<AisObject> fetch(@NonNull OsmandApplication app, @NonNull AisUrlSource source,
	                                     @NonNull QuadRect latLonBounds) {
		String response = request(app, source, latLonBounds);
		if (Algorithms.isEmpty(response)) {
			Log.d(TAG, "'" + source.name + "': empty response, nothing to parse");
			return null;
		}
		try {
			JSONObject root = new JSONObject(response);
			JSONArray states = root.optJSONArray("states");
			if (states != null) {
				List<AisObject> parsed = parseOpenSky(states);
				Log.d(TAG, "'" + source.name + "': OpenSky format, " + states.length()
						+ " states -> " + parsed.size() + " aircraft with a position");
				return parsed;
			}
			// same payload, different wrapper key depending on the service
			JSONArray aircraft = root.optJSONArray("ac");
			if (aircraft == null) {
				aircraft = root.optJSONArray("aircraft");
			}
			if (aircraft != null) {
				List<AisObject> parsed = parseAdsb(aircraft);
				Log.d(TAG, "'" + source.name + "': ADS-B format, " + aircraft.length()
						+ " entries -> " + parsed.size() + " aircraft with a position");
				return parsed;
			}
			Log.w(TAG, "'" + source.name + "': unrecognised response, no 'states'/'ac'/'aircraft' key."
					+ " Keys: " + root.keys() + ", body: " + shorten(response));
		} catch (JSONException e) {
			Log.w(TAG, "'" + source.name + "': response is not JSON: " + shorten(response), e);
		}
		return null;
	}

	@NonNull
	private static String shorten(@NonNull String body) {
		return body.length() <= LOGGED_BODY_LIMIT ? body
				: body.substring(0, LOGGED_BODY_LIMIT) + "... (" + body.length() + " chars)";
	}

	/** Never log the credential itself. */
	@NonNull
	private static String hideKey(@NonNull String url, @NonNull String apiKey) {
		return Algorithms.isEmpty(apiKey) ? url : url.replace(apiKey, "***");
	}

	@Nullable
	private static String request(@NonNull OsmandApplication app, @NonNull AisUrlSource source,
	                               @NonNull QuadRect latLonBounds) {
		String url = applyMapArea(source.url, latLonBounds);
		if (url.contains(API_KEY_PLACEHOLDER)) {
			url = url.replace(API_KEY_PLACEHOLDER, source.apiKey);
		}
		String loggedUrl = hideKey(url, source.apiKey);
		HttpURLConnection connection = null;
		long started = System.currentTimeMillis();
		try {
			connection = NetworkUtils.getHttpURLConnection(url);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			connection.setRequestProperty("User-Agent", Version.getFullVersion(app));
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			// RapidAPI-hosted services (ADS-B Exchange among them) expect the key as a header
			// rather than a query parameter.
			String rapidApiHost = getRapidApiHost(url);
			boolean rapidApiAuth = rapidApiHost != null && !Algorithms.isEmpty(source.apiKey);
			if (rapidApiAuth) {
				connection.setRequestProperty("X-RapidAPI-Key", source.apiKey);
				connection.setRequestProperty("X-RapidAPI-Host", rapidApiHost);
			}
			Log.d(TAG, "GET '" + source.name + "' " + loggedUrl
					+ " (key " + (Algorithms.isEmpty(source.apiKey) ? "not set" : "set")
					+ (rapidApiAuth ? ", sent as RapidAPI header" : "") + ")");
			connection.connect();
			int responseCode = connection.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				String error = connection.getErrorStream() != null
						? shorten(readStream(connection.getErrorStream())) : "";
				Log.w(TAG, "'" + source.name + "' responded " + responseCode + " "
						+ connection.getResponseMessage() + " " + error);
				return null;
			}
			String body = readStream(connection.getInputStream());
			Log.d(TAG, "'" + source.name + "' responded " + responseCode + ", " + body.length()
					+ " chars in " + (System.currentTimeMillis() - started) + " ms: " + shorten(body));
			return body;
		} catch (IOException e) {
			Log.w(TAG, "'" + source.name + "' request failed: " + loggedUrl, e);
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	@NonNull
	private static String readStream(@NonNull InputStream stream) throws IOException {
		StringBuilder builder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
		}
		return builder.toString();
	}

	@Nullable
	private static String getRapidApiHost(@NonNull String url) {
		try {
			String host = new URI(url).getHost();
			return host != null && host.endsWith(RAPIDAPI_HOST_SUFFIX) ? host : null;
		} catch (URISyntaxException e) {
			return null;
		}
	}

	@NonNull
	private static List<AisObject> parseOpenSky(@NonNull JSONArray states) {
		List<AisObject> result = new ArrayList<>();
		for (int i = 0; i < states.length(); i++) {
			JSONArray row = states.optJSONArray(i);
			if (row == null || row.length() < 11 || row.isNull(5) || row.isNull(6)) {
				continue;
			}
			Integer mmsi = parseIcao24(row.optString(0, ""));
			if (mmsi == null) {
				continue;
			}
			double lon = row.optDouble(5, Double.NaN);
			double lat = row.optDouble(6, Double.NaN);
			if (Double.isNaN(lon) || Double.isNaN(lat)) {
				continue;
			}
			double altitudeMeters = row.length() > 13 && !row.isNull(13)
					? row.optDouble(13, Double.NaN) : row.optDouble(7, Double.NaN);
			double velocityMs = row.isNull(9) ? Double.NaN : row.optDouble(9, Double.NaN);
			double cog = row.isNull(10) ? INVALID_COG : row.optDouble(10, INVALID_COG);
			result.add(createAisObject(mmsi, lat, lon, altitudeMeters,
					Double.isNaN(velocityMs) ? Double.NaN : velocityMs * MS_TO_KNOTS, cog));
		}
		return result;
	}

	@NonNull
	private static List<AisObject> parseAdsb(@NonNull JSONArray aircraft) {
		List<AisObject> result = new ArrayList<>();
		for (int i = 0; i < aircraft.length(); i++) {
			JSONObject plane = aircraft.optJSONObject(i);
			if (plane == null || plane.isNull("lat") || plane.isNull("lon")) {
				continue;
			}
			Integer mmsi = parseIcao24(plane.optString("hex", ""));
			if (mmsi == null) {
				continue;
			}
			double lat = plane.optDouble("lat", Double.NaN);
			double lon = plane.optDouble("lon", Double.NaN);
			if (Double.isNaN(lat) || Double.isNaN(lon)) {
				continue;
			}
			// alt_baro is "ground" for aircraft on the ground, hence optDouble with a NaN default
			double altitudeFeet = plane.isNull("alt_geom")
					? plane.optDouble("alt_baro", Double.NaN) : plane.optDouble("alt_geom", Double.NaN);
			double altitudeMeters = Double.isNaN(altitudeFeet) ? Double.NaN : altitudeFeet * FEET_TO_METERS;
			// ground speed is already in knots here, unlike OpenSky
			double sog = plane.isNull("gs") ? Double.NaN : plane.optDouble("gs", Double.NaN);
			double cog = plane.isNull("track") ? INVALID_COG : plane.optDouble("track", INVALID_COG);
			result.add(createAisObject(mmsi, lat, lon, altitudeMeters, sog, cog));
		}
		return result;
	}

	@NonNull
	private static AisObject createAisObject(int mmsi, double lat, double lon,
	                                          double altitudeMeters, double sogKnots, double cog) {
		int altitude = Double.isNaN(altitudeMeters) ? INVALID_ALTITUDE : (int) Math.round(altitudeMeters);
		double sog = Double.isNaN(sogKnots) ? INVALID_SOG : sogKnots;
		return new AisObject(mmsi, MSG_TYPE_AIRCRAFT_POSITION, 0, altitude, cog, sog, lat, lon);
	}

	/**
	 * ICAO 24-bit address doubles as the MMSI for aircraft: it tops out at 0xFFFFFF, well below
	 * the 9-digit range real vessel MMSIs live in, so the two cannot collide in the shared index.
	 */
	@Nullable
	private static Integer parseIcao24(@NonNull String icao24) {
		// ADS-B Exchange prefixes non-ICAO (e.g. TIS-B) addresses with "~"
		String hex = icao24.trim().replace("~", "");
		if (Algorithms.isEmpty(hex)) {
			return null;
		}
		try {
			return Integer.parseInt(hex, 16);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
