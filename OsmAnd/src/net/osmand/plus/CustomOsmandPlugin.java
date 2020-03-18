package net.osmand.plus;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class CustomOsmandPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(CustomOsmandPlugin.class);

	public String pluginId;
	public String name;
	public String description;

	public List<String> rendererNames = new ArrayList<>();
	public List<String> routerNames = new ArrayList<>();
	public List<ApplicationMode> appModes = new ArrayList<>();
	public List<QuickAction> quickActions = new ArrayList<>();
	public List<PoiUIFilter> poiUIFilters = new ArrayList<>();
	public List<ITileSource> mapSources = new ArrayList<>();
	public List<AvoidSpecificRoads.AvoidRoadInfo> avoidRoadInfos = new ArrayList<>();

	public CustomOsmandPlugin(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app);
		pluginId = json.getString("pluginId");
		name = json.getString("name");
		description = json.getString("Description");
	}

//	Prepare ".opr" desert-package manually + add all resources inside (extend json to describe package).
//
//Desert package
//1. Add to Plugins list
//1.1 Description / image / icon / name
//1.2 Enable description bottom sheet on Install
//2. Add custom rendering style to list Configure Map
//3. Include Special profile for navigation with selected style
//4. Add custom navigation icon (as example to use another car)
//
//P.S.: Functionality similar to Nautical / Ski Maps plugin,
// so we could remove all code for Nautical / Ski Maps from OsmAnd
// and put to separate "skimaps.opr", "nautical.opr" in future

	@Override
	public String getId() {
		return pluginId;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public int getAssetResourceName() {
		return R.drawable.contour_lines;
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_skiing;
	}

	public String toJson() throws JSONException {
		JSONObject json = new JSONObject();

		json.put("type", SettingsHelper.SettingsItemType.PLUGIN.name());
		json.put("pluginId", getId());
		json.put("name", getName());
		json.put("Description", getDescription());

		saveAdditionalItemsToJson(json);

		return json.toString();
	}

	public void loadAdditionalItemsFromJson(JSONObject json) throws JSONException {
		if (json.has("appModes")) {
			String appModesStr = json.getString("appModes");
			if (!Algorithms.isEmpty(appModesStr)) {
				JSONArray appModesJson = new JSONArray(appModesStr);
				for (int i = 0; i < appModesJson.length(); i++) {
					String str = appModesJson.getString(i);
					ApplicationMode mode = ApplicationMode.valueOfStringKey(str, null);
					if (mode != null) {
						appModes.add(mode);
					}
				}
			}
		}
		if (json.has("rendererNames")) {
			String rendererNamesStr = json.getString("rendererNames");
			if (!Algorithms.isEmpty(rendererNamesStr)) {
				JSONArray rendererNamesJson = new JSONArray(rendererNamesStr);
				for (int i = 0; i < rendererNamesJson.length(); i++) {
					String str = rendererNamesJson.getString(i);
					rendererNames.add(str);
				}
			}
		}
		if (json.has("routerNames")) {
			String routerNamesStr = json.getString("routerNames");
			if (!Algorithms.isEmpty(routerNamesStr)) {
				JSONArray routerNamesJson = new JSONArray(routerNamesStr);
				for (int i = 0; i < routerNamesJson.length(); i++) {
					String str = routerNamesJson.getString(i);
					routerNames.add(str);
				}
			}
		}

		readPoiUIFiltersFromJson(json);
		readMapSourcesFromJson(json);
		readQuickActionsFromJson(json);
		readAvoidRoadsFromJson(json);
	}

	public void saveAdditionalItemsToJson(JSONObject json) throws JSONException {
		if (!appModes.isEmpty()) {
			List<String> appModesKeys = new ArrayList<>();
			for (ApplicationMode mode : appModes) {
				appModesKeys.add(mode.getStringKey());
			}
			JSONArray appModesJson = new JSONArray(appModesKeys);
			json.put("appModes", appModesJson);
		}
		if (!rendererNames.isEmpty()) {
			JSONArray rendererNamesJson = new JSONArray(rendererNames);
			json.put("rendererNames", rendererNamesJson);
		}
		if (!routerNames.isEmpty()) {
			JSONArray rendererNamesJson = new JSONArray(routerNames);
			json.put("routerNames", rendererNamesJson);
		}

		savePoiUIFiltersToJson(json);
		saveMapSourcesToJson(json);
		saveQuickActionsToJson(json);
		saveAvoidRoadsToJson(json);
	}

	private void savePoiUIFiltersToJson(JSONObject poiUIFiltersJson) throws JSONException {
		if (!poiUIFilters.isEmpty()) {
			JSONObject json = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			Gson gson = new Gson();
			Type type = new TypeToken<HashMap<PoiCategory, LinkedHashSet<String>>>() {
			}.getType();
			try {
				for (PoiUIFilter filter : poiUIFilters) {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("name", filter.getName());
					jsonObject.put("filterId", filter.getFilterId());
					jsonObject.put("acceptedTypes", gson.toJson(filter.getAcceptedTypes(), type));
					jsonArray.put(jsonObject);
				}
				json.put("items", jsonArray);
			} catch (JSONException e) {
				LOG.error("Failed write to json", e);
			}
			poiUIFiltersJson.put("poiUIFilters", json);
		}
	}

	private void saveMapSourcesToJson(JSONObject mapSourcesJson) throws JSONException {
		if (!mapSources.isEmpty()) {
			JSONObject json = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			if (!mapSources.isEmpty()) {
				try {
					for (ITileSource template : mapSources) {
						JSONObject jsonObject = new JSONObject();
						boolean sql = template instanceof SQLiteTileSource;
						jsonObject.put("sql", sql);
						jsonObject.put("name", template.getName());
						jsonObject.put("minZoom", template.getMinimumZoomSupported());
						jsonObject.put("maxZoom", template.getMaximumZoomSupported());
						jsonObject.put("url", template.getUrlTemplate());
						jsonObject.put("randoms", template.getRandoms());
						jsonObject.put("ellipsoid", template.isEllipticYTile());
						jsonObject.put("inverted_y", template.isInvertedYTile());
						jsonObject.put("referer", template.getReferer());
						jsonObject.put("timesupported", template.isTimeSupported());
						jsonObject.put("expire", template.getExpirationTimeMillis());
						jsonObject.put("inversiveZoom", template.getInversiveZoom());
						jsonObject.put("ext", template.getTileFormat());
						jsonObject.put("tileSize", template.getTileSize());
						jsonObject.put("bitDensity", template.getBitDensity());
						jsonObject.put("avgSize", template.getAvgSize());
						jsonObject.put("rule", template.getRule());
						jsonArray.put(jsonObject);
					}
					json.put("items", jsonArray);
				} catch (JSONException e) {
					LOG.error("Failed write to json", e);
				}
			}
			mapSourcesJson.put("mapSources", json);
		}
	}

	private void saveAvoidRoadsToJson(JSONObject avoidRoadInfosJson) throws JSONException {
		if (!avoidRoadInfos.isEmpty()) {
			JSONObject json = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			if (!avoidRoadInfos.isEmpty()) {
				try {
					for (AvoidSpecificRoads.AvoidRoadInfo avoidRoad : avoidRoadInfos) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("latitude", avoidRoad.latitude);
						jsonObject.put("longitude", avoidRoad.longitude);
						jsonObject.put("name", avoidRoad.name);
						jsonObject.put("appModeKey", avoidRoad.appModeKey);
						jsonArray.put(jsonObject);
					}
					json.put("items", jsonArray);
				} catch (JSONException e) {
					LOG.error("Failed write to json", e);
				}
			}
			avoidRoadInfosJson.put("avoidRoadInfos", json);
		}
	}

	private void saveQuickActionsToJson(JSONObject quickActionsJson) throws JSONException {
		if (!quickActions.isEmpty()) {
			JSONObject json = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			Gson gson = new Gson();
			Type type = new TypeToken<HashMap<String, String>>() {
			}.getType();
			if (!quickActions.isEmpty()) {
				try {
					for (QuickAction action : quickActions) {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("name", action.hasCustomName(app)
								? action.getName(app) : "");
						jsonObject.put("type", action.getType());
						jsonObject.put("params", gson.toJson(action.getParams(), type));
						jsonArray.put(jsonObject);
					}
					json.put("items", jsonArray);
				} catch (JSONException e) {
					LOG.error("Failed write to json", e);
				}
			}
			quickActionsJson.put("quickActions", json);
		}
	}

	private void readMapSourcesFromJson(JSONObject json) {
		if (json.has("mapSources")) {
			try {
				String mapSourcesStr = json.getString("mapSources");
				if (!Algorithms.isEmpty(mapSourcesStr)) {
					json = new JSONObject(mapSourcesStr);
					JSONArray jsonArray = json.getJSONArray("items");
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject object = jsonArray.getJSONObject(i);
						boolean sql = object.optBoolean("sql");
						String name = object.optString("name");
						int minZoom = object.optInt("minZoom");
						int maxZoom = object.optInt("maxZoom");
						String url = object.optString("url");
						String randoms = object.optString("randoms");
						boolean ellipsoid = object.optBoolean("ellipsoid", false);
						boolean invertedY = object.optBoolean("inverted_y", false);
						String referer = object.optString("referer");
						boolean timesupported = object.optBoolean("timesupported", false);
						long expire = object.optLong("expire");
						boolean inversiveZoom = object.optBoolean("inversiveZoom", false);
						String ext = object.optString("ext");
						int tileSize = object.optInt("tileSize");
						int bitDensity = object.optInt("bitDensity");
						int avgSize = object.optInt("avgSize");
						String rule = object.optString("rule");

						ITileSource template;
						if (!sql) {
							template = new TileSourceManager.TileSourceTemplate(name, url, ext, maxZoom, minZoom, tileSize, bitDensity, avgSize);
						} else {
							template = new SQLiteTileSource(app, name, minZoom, maxZoom, url, randoms, ellipsoid, invertedY, referer, timesupported, expire, inversiveZoom);
						}
						mapSources.add(template);
					}
				}
			} catch (JSONException e) {
				throw new IllegalArgumentException("Json parse error", e);
			}
		}
	}

	private void readQuickActionsFromJson(JSONObject json) {
		if (json.has("quickActions")) {
			try {
				String quickActionsStr = json.getString("quickActions");
				if (!Algorithms.isEmpty(quickActionsStr)) {
					Gson gson = new Gson();
					Type type = new TypeToken<HashMap<String, String>>() {
					}.getType();
					JSONObject quickActionsJson = new JSONObject(quickActionsStr);
					JSONArray itemsJson = quickActionsJson.getJSONArray("items");
					for (int i = 0; i < itemsJson.length(); i++) {
						JSONObject object = itemsJson.getJSONObject(i);
						String name = object.getString("name");
						int actionType = object.getInt("type");
						String paramsString = object.getString("params");
						HashMap<String, String> params = gson.fromJson(paramsString, type);
						QuickAction quickAction = new QuickAction(actionType);
						if (!name.isEmpty()) {
							quickAction.setName(name);
						}
						quickAction.setParams(params);
						quickActions.add(quickAction);
					}
				}
			} catch (JSONException e) {
				throw new IllegalArgumentException("Json parse error", e);
			}
		}
	}

	private void readAvoidRoadsFromJson(JSONObject json) {
		if (json.has("avoidRoadInfos")) {
			try {
				String avoidRoadInfosStr = json.getString("avoidRoadInfos");
				if (!Algorithms.isEmpty(avoidRoadInfosStr)) {
					JSONObject avoidRoadInfosJson = new JSONObject(avoidRoadInfosStr);
					JSONArray jsonArray = avoidRoadInfosJson.getJSONArray("items");
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject object = jsonArray.getJSONObject(i);
						double latitude = object.optDouble("latitude");
						double longitude = object.optDouble("longitude");
						String name = object.optString("name");
						String appModeKey = object.optString("appModeKey");
						AvoidSpecificRoads.AvoidRoadInfo roadInfo = new AvoidSpecificRoads.AvoidRoadInfo();
						roadInfo.id = 0;
						roadInfo.latitude = latitude;
						roadInfo.longitude = longitude;
						roadInfo.name = name;
						if (ApplicationMode.valueOfStringKey(appModeKey, null) != null) {
							roadInfo.appModeKey = appModeKey;
						} else {
							roadInfo.appModeKey = app.getRoutingHelper().getAppMode().getStringKey();
						}
						avoidRoadInfos.add(roadInfo);
					}
				}
			} catch (JSONException e) {
				throw new IllegalArgumentException("Json parse error", e);
			}
		}
	}

	private void readPoiUIFiltersFromJson(JSONObject json) {
		if (json.has("poiUIFilters")) {
			try {
				String poiUIFiltersStr = json.getString("poiUIFilters");
				if (!Algorithms.isEmpty(poiUIFiltersStr)) {
					JSONObject poiUIFiltersJson = new JSONObject(poiUIFiltersStr);
					JSONArray jsonArray = poiUIFiltersJson.getJSONArray("items");
					Gson gson = new Gson();
					Type type = new TypeToken<HashMap<String, LinkedHashSet<String>>>() {
					}.getType();
					MapPoiTypes poiTypes = app.getPoiTypes();
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject object = jsonArray.getJSONObject(i);
						String name = object.getString("name");
						String filterId = object.getString("filterId");
						String acceptedTypesString = object.getString("acceptedTypes");
						HashMap<String, LinkedHashSet<String>> acceptedTypes = gson.fromJson(acceptedTypesString, type);
						Map<PoiCategory, LinkedHashSet<String>> acceptedTypesDone = new HashMap<>();
						for (Map.Entry<String, LinkedHashSet<String>> mapItem : acceptedTypes.entrySet()) {
							final PoiCategory a = poiTypes.getPoiCategoryByName(mapItem.getKey());
							acceptedTypesDone.put(a, mapItem.getValue());
						}
						PoiUIFilter filter = new PoiUIFilter(name, filterId, acceptedTypesDone, app);
						poiUIFilters.add(filter);
					}
				}
			} catch (JSONException e) {
				throw new IllegalArgumentException("Json parse error", e);
			}
		}
	}

	@Override
	public List<String> getRendererNames() {
		return rendererNames;
	}

	@Override
	public List<String> getRouterNames() {
		return routerNames;
	}

	@Override
	public List<ApplicationMode> getAddedAppModes() {
		return appModes;
	}

	@Override
	public List<QuickAction> getQuickActions() {
		return quickActions;
	}

	@Override
	public List<PoiUIFilter> getPoiUIFilters() {
		return poiUIFilters;
	}

	@Override
	public List<ITileSource> getMapSources() {
		return mapSources;
	}

	@Override
	public List<AvoidSpecificRoads.AvoidRoadInfo> getAvoidRoadInfos() {
		return avoidRoadInfos;
	}
}