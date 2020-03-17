package net.osmand.plus;

import net.osmand.map.ITileSource;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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

		return json.toString();
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