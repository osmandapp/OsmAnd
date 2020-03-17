package net.osmand.plus;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.map.ITileSource;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

public class CustomOsmandPlugin extends OsmandPlugin {

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

	public CustomOsmandPlugin(OsmandApplication app) {
		super(app);
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

	private void savePoiUIFiltersToJson(JSONObject json) throws JSONException {
		if (!poiUIFilters.isEmpty()) {
			JSONArray jsonArray = new JSONArray();
			Gson gson = new Gson();
			Type type = new TypeToken<HashMap<PoiCategory, LinkedHashSet<String>>>() {
			}.getType();
			for (PoiUIFilter filter : poiUIFilters) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("name", filter.getName());
				jsonObject.put("filterId", filter.getFilterId());
				jsonObject.put("acceptedTypes", gson.toJson(filter.getAcceptedTypes(), type));
				jsonArray.put(jsonObject);
			}
			json.put("poiUIFilters", jsonArray);
		}
	}

	private void saveMapSourcesToJson(JSONObject json) throws JSONException {
		if (!mapSources.isEmpty()) {
			JSONArray jsonArray = new JSONArray();
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
			json.put("mapSources", jsonArray);
		}
	}

	private void saveAvoidRoadsToJson(JSONObject json) throws JSONException {
		if (!avoidRoadInfos.isEmpty()) {
			JSONArray jsonArray = new JSONArray();
			for (AvoidSpecificRoads.AvoidRoadInfo avoidRoad : avoidRoadInfos) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("latitude", avoidRoad.latitude);
				jsonObject.put("longitude", avoidRoad.longitude);
				jsonObject.put("name", avoidRoad.name);
				jsonObject.put("appModeKey", avoidRoad.appModeKey);
				jsonArray.put(jsonObject);
			}
			json.put("avoidRoadInfos", jsonArray);
		}
	}

	private void saveQuickActionsToJson(JSONObject json) throws JSONException {
		if (!quickActions.isEmpty()) {
			JSONArray jsonArray = new JSONArray();
			Gson gson = new Gson();
			Type type = new TypeToken<HashMap<String, String>>() {
			}.getType();

			for (QuickAction action : quickActions) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("name", action.hasCustomName(app) ? action.getName(app) : "");
				jsonObject.put("type", action.getType());
				jsonObject.put("params", gson.toJson(action.getParams(), type));
				jsonArray.put(jsonObject);
			}
			json.put("quickActions", jsonArray);
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