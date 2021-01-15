package net.osmand.plus.onlinerouting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.onlinerouting.engine.EngineType;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OnlineRoutingHelper {

	private static final Log LOG = PlatformUtil.getLog(OnlineRoutingHelper.class);

	private static final String ITEMS = "items";
	private static final String TYPE = "type";
	private static final String PARAMS = "params";

	private OsmandApplication app;
	private OsmandSettings settings;
	private Map<String, OnlineRoutingEngine> cachedEngines;

	public OnlineRoutingHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.cachedEngines = loadSavedEngines();
	}

	@NonNull
	public List<OnlineRoutingEngine> getEngines() {
		return new ArrayList<>(cachedEngines.values());
	}

	@NonNull
	public List<OnlineRoutingEngine> getEnginesExceptMentioned(@Nullable String ... excludeKeys) {
		List<OnlineRoutingEngine> engines = getEngines();
		if (excludeKeys != null) {
			for (String key : excludeKeys) {
				OnlineRoutingEngine engine = getEngineByKey(key);
				engines.remove(engine);
			}
		}
		return engines;
	}

	@Nullable
	public OnlineRoutingEngine getEngineByKey(@Nullable String stringKey) {
		return cachedEngines.get(stringKey);
	}

	@NonNull
	public List<LatLon> calculateRouteOnline(@NonNull OnlineRoutingEngine engine,
	                                         @NonNull List<LatLon> path) throws IOException, JSONException {
		String url = engine.getFullUrl(path);
		String content = makeRequest(url);
		return engine.parseServerResponse(content);
	}

	@NonNull
	public String makeRequest(@NonNull String url) throws IOException {
		HttpURLConnection connection = NetworkUtils.getHttpURLConnection(url);
		connection.setRequestProperty("User-Agent", Version.getFullVersion(app));
		StringBuilder content = new StringBuilder();
		BufferedReader reader;
		if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		} else {
			reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
		}
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
		deleteInaccessibleParameters(engine);
		String key = createEngineKeyIfNeeded(engine);
		cachedEngines.put(key, engine);
		saveCacheToSettings();
	}

	public void deleteEngine(@NonNull OnlineRoutingEngine engine) {
		String stringKey = engine.getStringKey();
		deleteEngine(stringKey);
	}

	public void deleteEngine(@Nullable String stringKey) {
		if (stringKey != null) {
			cachedEngines.remove(stringKey);
			saveCacheToSettings();
		}
	}

	private void deleteInaccessibleParameters(@NonNull OnlineRoutingEngine engine) {
		for (EngineParameter key : EngineParameter.values()) {
			if (!engine.isParameterAllowed(key)) {
				engine.remove(key);
			}
		}
	}

	@NonNull
	private String createEngineKeyIfNeeded(@NonNull OnlineRoutingEngine engine) {
		String key = engine.get(EngineParameter.KEY);
		if (Algorithms.isEmpty(key)) {
			key = OnlineRoutingEngine.generateKey();
			engine.put(EngineParameter.KEY, key);
		}
		return key;
	}

	@NonNull
	private Map<String, OnlineRoutingEngine> loadSavedEngines() {
		Map<String, OnlineRoutingEngine> cachedEngines = new LinkedHashMap<>();
		for (OnlineRoutingEngine engine : readFromSettings()) {
			cachedEngines.put(engine.getStringKey(), engine);
		}
		return cachedEngines;
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

	private void saveCacheToSettings() {
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

	public static void readFromJson(@NonNull JSONObject json,
	                                @NonNull List<OnlineRoutingEngine> engines) throws JSONException {
		if (!json.has("items")) {
			return;
		}
		Gson gson = new Gson();
		Type typeToken = new TypeToken<HashMap<String, String>>() {
		}.getType();
		JSONArray itemsJson = json.getJSONArray(ITEMS);
		for (int i = 0; i < itemsJson.length(); i++) {
			JSONObject object = itemsJson.getJSONObject(i);
			if (object.has(TYPE) && object.has(PARAMS)) {
				EngineType type = EngineType.getTypeByName(object.getString(TYPE));
				String paramsString = object.getString(PARAMS);
				HashMap<String, String> params = gson.fromJson(paramsString, typeToken);
				OnlineRoutingEngine engine = OnlineRoutingFactory.createEngine(type, params);
				if (!Algorithms.isEmpty(engine.getStringKey())) {
					engines.add(engine);
				}
			}
		}
	}

	public static void writeToJson(@NonNull JSONObject json,
	                               @NonNull List<OnlineRoutingEngine> engines) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		Gson gson = new Gson();
		Type type = new TypeToken<HashMap<String, String>>() {
		}.getType();
		for (OnlineRoutingEngine engine : engines) {
			if (Algorithms.isEmpty(engine.getStringKey())) {
				continue;
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.put(TYPE, engine.getType().name());
			jsonObject.put(PARAMS, gson.toJson(engine.getParams(), type));
			jsonArray.put(jsonObject);
		}
		json.put(ITEMS, jsonArray);
	}
}
