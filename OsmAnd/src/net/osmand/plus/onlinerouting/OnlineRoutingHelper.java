package net.osmand.plus.onlinerouting;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.onlinerouting.OnlineRoutingEngine.EngineParameter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoPolylineParserUtil;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OnlineRoutingHelper {

	private static final Log LOG = PlatformUtil.getLog(OnlineRoutingHelper.class);

	private OsmandApplication app;
	private OsmandSettings settings;
	private Map<String, OnlineRoutingEngine> cachedEngines;

	public OnlineRoutingHelper(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		loadFromSettings();
	}

	@NonNull
	public List<OnlineRoutingEngine> getEngines() {
		return new ArrayList<>(cachedEngines.values());
	}

	public OnlineRoutingEngine getEngineByKey(String stringKey) {
		return cachedEngines.get(stringKey);
	}

	public List<LatLon> calculateRouteOnline(@NonNull OnlineRoutingEngine engine,
	                                         @NonNull List<LatLon> path) throws IOException, JSONException {
		String fullUrl = createFullUrl(engine, path);
		String content = makeRequest(fullUrl);
		return parseResponse(engine, content);
	}

	public String createFullUrl(OnlineRoutingEngine engine, List<LatLon> path) {
		StringBuilder sb = new StringBuilder(engine.getBaseUrl());
		String vehicle = engine.getVehicleKey();
		String apiKey = engine.getParameter(EngineParameter.API_KEY);
		switch (engine.getType()) {

			case GRAPHHOPER:
				sb.append("?");
				for (LatLon point : path) {
					sb.append("point=")
							.append(point.getLatitude())
							.append(',')
							.append(point.getLongitude())
							.append('&');
				}
				sb.append("vehicle=").append(vehicle);

				if (!Algorithms.isEmpty(apiKey)) {
					sb.append('&').append("key=").append(apiKey);
				}
				break;

			case OSRM:
				sb.append(vehicle).append('/');
				for (int i = 0; i < path.size(); i++) {
					LatLon point = path.get(i);
					sb.append(point.getLongitude()).append(',').append(point.getLatitude());
					if (i < path.size() - 1) {
						sb.append(';');
					}
				}
				break;

			case ORS:
				if (path.size() > 1) {
					sb.append("driving-car").append('?'); // todo only for testing
					if (!Algorithms.isEmpty(apiKey)) {
						sb.append("api_key=").append(apiKey);
					}
					LatLon start = path.get(0);
					LatLon end = path.get(path.size() - 1);
					sb.append('&').append("start=")
							.append(start.getLatitude()).append(',').append(start.getLongitude());
					sb.append('&').append("end=")
							.append(end.getLatitude()).append(',').append(end.getLongitude());
				}
				break;

		}
		return sb.toString();
	}

	private List<LatLon> parseResponse(OnlineRoutingEngine engine, String content) throws JSONException {
		JSONObject obj = new JSONObject(content);

		switch (engine.getType()) {
			case GRAPHHOPER:
				return GeoPolylineParserUtil.parse(
						obj.getJSONArray("paths").getJSONObject(0).getString("points"),
						GeoPolylineParserUtil.PRECISION_5);

			case OSRM:
				return GeoPolylineParserUtil.parse(
						obj.getJSONArray("routes").getJSONObject(0).getString("geometry"),
						GeoPolylineParserUtil.PRECISION_5);

			case ORS:
				JSONArray array = obj.getJSONArray("features").getJSONObject(0)
						.getJSONObject("geometry").getJSONArray("coordinates");
				List<LatLon> track = new ArrayList<>();
				for (int i = 0; i < array.length(); i++) {
					JSONArray point = array.getJSONArray(i);
					double lat = Double.parseDouble(point.getString(0));
					double lon = Double.parseDouble(point.getString(1));
					track.add(new LatLon(lat, lon));
				}
				return track;
		}
		return new ArrayList<>();
	}

	private String makeRequest(String url) throws IOException {
		URLConnection connection = NetworkUtils.getHttpURLConnection(url);
		connection.setRequestProperty("User-Agent", Version.getFullVersion(app));
		StringBuilder content = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String s;
		while ((s = reader.readLine()) != null) {
			content.append(s);
		}
		try {
			reader.close();
		} catch (IOException ignored) {
		}
		return content.toString();
	}

	public void saveEngine(@NonNull OnlineRoutingEngine engine) {
		String stringKey = engine.getStringKey();
		cachedEngines.put(stringKey, engine);
		saveToSettings();
	}

	public void deleteEngine(@NonNull String stringKey) {
		OnlineRoutingEngine engine = getEngineByKey(stringKey);
		if (engine != null) {
			deleteEngine(engine);
		}
	}

	public void deleteEngine(@NonNull OnlineRoutingEngine engine) {
		String stringKey = engine.getStringKey();
		if (cachedEngines.containsKey(stringKey)) {
			cachedEngines.remove(stringKey);
			saveToSettings();
		}
	}

	private void loadFromSettings() {
		Map<String, OnlineRoutingEngine> cachedEngines = new LinkedHashMap<>();
		for (OnlineRoutingEngine engine : readFromSettings()) {
			cachedEngines.put(engine.getStringKey(), engine);
		}
		this.cachedEngines = cachedEngines;
	}

	@NonNull
	private List<OnlineRoutingEngine> readFromSettings() {
		List<OnlineRoutingEngine> engines = new ArrayList<>();
		String jsonString = settings.ONLINE_ROUTING_ENGINES.get();
		if (!Algorithms.isEmpty(jsonString)) {
			try {
				JSONObject json = new JSONObject(jsonString);
				readFromJson(json, engines);
			} catch (JSONException e) {
				LOG.debug("Error when reading engines from JSON ", e);
			}
		}
		return engines;
	}

	private void saveToSettings() {
		if (!Algorithms.isEmpty(cachedEngines)) {
			try {
				JSONObject json = new JSONObject();
				writeToJson(json, getEngines());
				settings.ONLINE_ROUTING_ENGINES.set(json.toString());
			} catch (JSONException e) {
				LOG.debug("Error when writing engines to JSON ", e);
			}
		} else {
			settings.ONLINE_ROUTING_ENGINES.set(null);
		}
	}

	public static void readFromJson(JSONObject json, List<OnlineRoutingEngine> engines) throws JSONException {
		if (!json.has("items")) {
			return;
		}
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<String, String>>() {
		}.getType();
		JSONArray itemsJson = json.getJSONArray("items");
		for (int i = 0; i < itemsJson.length(); i++) {
			JSONObject object = itemsJson.getJSONObject(i);
			String key = object.getString("key");
			String vehicleKey = object.getString("vehicle");
			EngineType engineType = EngineType.valueOf(object.getString("type"));
			String paramsString = object.getString("params");
			HashMap<String, String> params = gson.fromJson(paramsString, type);
			engines.add(new OnlineRoutingEngine(key, engineType, vehicleKey, params));
		}
	}

	public static void writeToJson(JSONObject json, List<OnlineRoutingEngine> engines) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<String, String>>() {
		}.getType();
		for (OnlineRoutingEngine engine : engines) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("key", engine.getStringKey());
			jsonObject.put("type", engine.getType().name());
			jsonObject.put("vehicle", engine.getVehicleKey());
			jsonObject.put("params", gson.toJson(engine.getParams(), type));
			jsonArray.put(jsonObject);
		}
		json.put("items", jsonArray);
	}
}
