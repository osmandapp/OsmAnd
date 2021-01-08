package net.osmand.plus.onlinerouting;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnlineRoutingHelper {

	private static final Log LOG = PlatformUtil.getLog(OnlineRoutingHelper.class);

	private OsmandApplication app;
	private OsmandSettings settings;
	private List<OnlineRoutingEngine> cachedEngines;
	private Map<String, OnlineRoutingEngine> cachedEnginesMap;

	public OnlineRoutingHelper(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		loadFromSettings();
	}

	@NonNull
	public List<OnlineRoutingEngine> getEngines() {
		return cachedEngines;
	}

	public OnlineRoutingEngine getEngineByKey(String stringKey) {
		return cachedEnginesMap.get(stringKey);
	}

	public void saveEngine(@NonNull OnlineRoutingEngine engine) {
		String stringKey = engine.getStringKey();
		OnlineRoutingEngine existedEngine = cachedEnginesMap.get(stringKey);
		if (existedEngine != null) {
			int index = cachedEngines.indexOf(existedEngine);
			cachedEngines.set(index, engine);
		} else {
			cachedEngines.add(engine);
		}
		cachedEnginesMap.put(stringKey, engine);
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
		if (cachedEnginesMap.containsKey(stringKey)) {
			OnlineRoutingEngine existedEngine = cachedEnginesMap.remove(stringKey);
			cachedEngines.remove(existedEngine);
			saveToSettings();
		}
	}

	private void loadFromSettings() {
		cachedEngines = readFromSettings();
		cachedEnginesMap = new HashMap<>();
		for (OnlineRoutingEngine engine : cachedEngines) {
			cachedEnginesMap.put(engine.getStringKey(), engine);
		}
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
				writeToJson(json, cachedEngines);
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
			ServerType serverType = ServerType.valueOf(object.getString("serverType"));
			String paramsString = object.getString("params");
			HashMap<String, String> params = gson.fromJson(paramsString, type);
			engines.add(new OnlineRoutingEngine(key, serverType, vehicleKey, params));
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
			jsonObject.put("serverType", engine.getServerType().name());
			jsonObject.put("vehicle", engine.getVehicleKey());
			jsonObject.put("params", gson.toJson(engine.getParams(), type));
			jsonArray.put(jsonObject);
		}
		json.put("items", jsonArray);
	}
}
