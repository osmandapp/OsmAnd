package net.osmand.plus;

import android.app.Activity;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.SettingsHelper.AvoidRoadsSettingsItem;
import net.osmand.plus.SettingsHelper.MapSourcesSettingsItem;
import net.osmand.plus.SettingsHelper.PoiUiFilterSettingsItem;
import net.osmand.plus.SettingsHelper.QuickActionsSettingsItem;
import net.osmand.plus.SettingsHelper.SettingsCollectListener;
import net.osmand.plus.SettingsHelper.SettingsItem;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CustomOsmandPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(CustomOsmandPlugin.class);

	public String pluginId;
	public Map<String, String> names = new HashMap<>();
	public Map<String, String> descriptions = new HashMap<>();

	public List<String> rendererNames = new ArrayList<>();
	public List<String> routerNames = new ArrayList<>();

	public CustomOsmandPlugin(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app);
		pluginId = json.getString("pluginId");
		readAdditionalDataFromJson(json);
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
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		super.init(app, activity);
		if (activity != null) {
			// called from UI
			File pluginItemsFile = getPluginItemsFile();
			if (pluginItemsFile.exists()) {
				addPluginItemsFromFile(pluginItemsFile);
			}
		}
		return true;
	}

	private void addPluginItemsFromFile(final File file) {
		app.getSettingsHelper().collectSettings(file, "", 1, new SettingsCollectListener() {
			@Override
			public void onSettingsCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items) {
				if (succeed && !items.isEmpty()) {

				}
			}
		});
	}

	private void removePluginItemsFromFile(final File file) {
		app.getSettingsHelper().collectSettings(file, "", 1, new SettingsCollectListener() {
			@Override
			public void onSettingsCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items) {
				if (succeed && !items.isEmpty()) {
					for (SettingsItem item : items) {
						if (item instanceof QuickActionsSettingsItem) {
							QuickActionsSettingsItem quickActionsSettingsItem = (QuickActionsSettingsItem) item;
							List<QuickAction> quickActions = quickActionsSettingsItem.getItems();
							for (QuickAction action : quickActions) {
								QuickAction savedAction = app.getQuickActionRegistry().getQuickAction(app, action.getType(), action.getName(app), action.getParams());
								if (savedAction != null) {
									app.getQuickActionRegistry().deleteQuickAction(savedAction);
								}
							}
						}
						if (item instanceof MapSourcesSettingsItem) {
							MapSourcesSettingsItem mapSourcesSettingsItem = (MapSourcesSettingsItem) item;
							List<ITileSource> mapSources = mapSourcesSettingsItem.getItems();

							for (ITileSource tileSource : mapSources) {
								if (tileSource instanceof TileSourceManager.TileSourceTemplate) {
//									app.getSettings().installTileSource((TileSourceManager.TileSourceTemplate) tileSource);
								} else if (tileSource instanceof SQLiteTileSource) {
//									((SQLiteTileSource) tileSource).createDataBase();
								}
							}
						}
						if (item instanceof PoiUiFilterSettingsItem) {
							PoiUiFilterSettingsItem poiUiFilterSettingsItem = (PoiUiFilterSettingsItem) item;
							List<PoiUIFilter> poiUIFilters = poiUiFilterSettingsItem.getItems();
							for (PoiUIFilter filter : poiUIFilters) {
								app.getPoiFilters().removePoiFilter(filter);
							}
							app.getSearchUICore().refreshCustomPoiFilters();
						}
						if (item instanceof AvoidRoadsSettingsItem) {
							AvoidRoadsSettingsItem avoidRoadsSettingsItem = (AvoidRoadsSettingsItem) item;
							List<AvoidSpecificRoads.AvoidRoadInfo> avoidRoadInfos = avoidRoadsSettingsItem.getItems();
							for (AvoidSpecificRoads.AvoidRoadInfo avoidRoad : avoidRoadInfos) {
								app.getAvoidSpecificRoads().removeImpassableRoad(avoidRoad);
							}
						}
					}
				}
			}
		});
	}

	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		File pluginItemsFile = getPluginItemsFile();
		if (pluginItemsFile.exists()) {
			removePluginItemsFromFile(pluginItemsFile);
		}
	}

	private File getPluginItemsFile() {
		File pluginDir = new File(app.getAppPath(null), IndexConstants.PLUGINS_DIR + pluginId);
		return new File(pluginDir, "items" + IndexConstants.OSMAND_SETTINGS_FILE_EXT);
	}

	@Override
	public String getId() {
		return pluginId;
	}

	@Override
	public String getName() {
		Configuration config = app.getResources().getConfiguration();
		String lang = config.locale.getLanguage();
		String name = names.get(lang);
		if (Algorithms.isEmpty(name)) {
			name = names.get("");
		}
		if (Algorithms.isEmpty(name)) {
			name = app.getString(R.string.custom_osmand_plugin);
		}
		return name;
	}

	@Override
	public String getDescription() {
		Configuration config = app.getResources().getConfiguration();
		String lang = config.locale.getLanguage();
		String description = descriptions.get(lang);
		if (Algorithms.isEmpty(description)) {
			description = descriptions.get("");
		}
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

	public void readAdditionalDataFromJson(JSONObject json) throws JSONException {
		JSONObject nameJson = json.has("name") ? json.getJSONObject("name") : null;
		if (nameJson != null) {
			for (Iterator<String> it = nameJson.keys(); it.hasNext(); ) {
				String localeKey = it.next();
				String name = nameJson.getString(localeKey);
				names.put(localeKey, name);
			}
		}
		JSONObject descriptionJson = json.has("description") ? json.getJSONObject("description") : null;
		if (descriptionJson != null) {
			for (Iterator<String> it = descriptionJson.keys(); it.hasNext(); ) {
				String localeKey = it.next();
				String name = descriptionJson.getString(localeKey);
				descriptions.put(localeKey, name);
			}
		}
	}

	public void writeAdditionalDataToJson(JSONObject json) throws JSONException {
		JSONObject nameJson = new JSONObject();
		for (Map.Entry<String, String> entry : names.entrySet()) {
			nameJson.put(entry.getKey(), entry.getValue());
		}
		json.put("name", nameJson);

		JSONObject descriptionJson = new JSONObject();
		for (Map.Entry<String, String> entry : descriptions.entrySet()) {
			descriptionJson.put(entry.getKey(), entry.getValue());
		}
		json.put("description", descriptionJson);
	}

	@Override
	public List<String> getRendererNames() {
		return rendererNames;
	}

	@Override
	public List<String> getRouterNames() {
		return routerNames;
	}
}