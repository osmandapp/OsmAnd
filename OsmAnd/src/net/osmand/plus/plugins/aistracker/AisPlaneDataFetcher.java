package net.osmand.plus.plugins.aistracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.shared.aistracker.AisObject;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fetches aircraft positions from an OpenSky-Network-compatible JSON URL
 * ({@code {"states": [[icao24, callsign, origin_country, time_position, last_contact,
 * longitude, latitude, baro_altitude, on_ground, velocity, true_track, ...], ...]}}) and turns
 * them into {@link AisObject} instances (message type 9 = airborne SAR/aircraft position report)
 * so they can be fed straight into the existing AIS rendering/tap-menu pipeline alongside ships.
 * <p>
 * Runs synchronously - callers are expected to invoke this from a background thread.
 */
public class AisPlaneDataFetcher {

	private static final Log LOG = PlatformUtil.getLog(AisPlaneDataFetcher.class);

	// net.osmand.shared.aistracker.AisObjectConstants values, duplicated here to avoid
	// Kotlin-object interop from Java for a handful of constants.
	private static final int INVALID_ALTITUDE = 4095;
	private static final double INVALID_COG = 360.0;
	private static final double INVALID_SOG = 1023.0;
	private static final int MSG_TYPE_AIRCRAFT_POSITION = 9;

	private AisPlaneDataFetcher() {
	}

	@NonNull
	public static List<AisObject> fetchOpenSkyCompatible(@NonNull OsmandApplication app, @NonNull String url) {
		List<AisObject> result = new ArrayList<>();
		String json = AndroidNetworkUtils.sendRequest(app, url, null, "AIS planes source", false, false);
		if (Algorithms.isEmpty(json)) {
			return result;
		}
		try {
			JSONObject root = new JSONObject(json);
			JSONArray states = root.optJSONArray("states");
			if (states == null) {
				return result;
			}
			for (int i = 0; i < states.length(); i++) {
				JSONArray row = states.optJSONArray(i);
				if (row == null || row.length() < 11) {
					continue;
				}
				AisObject ais = parseState(row);
				if (ais != null) {
					result.add(ais);
				}
			}
		} catch (JSONException e) {
			LOG.warn("Failed to parse AIS planes source response", e);
		}
		return result;
	}

	@Nullable
	private static AisObject parseState(@NonNull JSONArray row) {
		String icao24 = row.optString(0, "");
		if (Algorithms.isEmpty(icao24) || row.isNull(5) || row.isNull(6)) {
			return null;
		}
		int mmsi;
		try {
			mmsi = Integer.parseInt(icao24.trim(), 16);
		} catch (NumberFormatException e) {
			return null;
		}
		double lon = row.optDouble(5, Double.NaN);
		double lat = row.optDouble(6, Double.NaN);
		if (Double.isNaN(lon) || Double.isNaN(lat)) {
			return null;
		}
		double altitudeMeters = row.length() > 13 && !row.isNull(13)
				? row.optDouble(13, Double.NaN) : row.optDouble(7, Double.NaN);
		int altitude = Double.isNaN(altitudeMeters) ? INVALID_ALTITUDE : (int) Math.round(altitudeMeters);
		double velocityMs = row.isNull(9) ? Double.NaN : row.optDouble(9, Double.NaN);
		// AisObject stores speed-over-ground in knots (see AisObject.getAisLocation()).
		double sog = Double.isNaN(velocityMs) ? INVALID_SOG : velocityMs * 3600.0 / 1852.0;
		double cog = row.isNull(10) ? INVALID_COG : row.optDouble(10, INVALID_COG);
		return new AisObject(mmsi, MSG_TYPE_AIRCRAFT_POSITION, 0, altitude, cog, sog, lat, lon);
	}
}
