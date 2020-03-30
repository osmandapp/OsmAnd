package net.osmand.plus;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.SettingsHelper.AvoidRoadsSettingsItem;
import net.osmand.plus.SettingsHelper.MapSourcesSettingsItem;
import net.osmand.plus.SettingsHelper.PluginSettingsItem;
import net.osmand.plus.SettingsHelper.PoiUiFilterSettingsItem;
import net.osmand.plus.SettingsHelper.ProfileSettingsItem;
import net.osmand.plus.SettingsHelper.QuickActionsSettingsItem;
import net.osmand.plus.SettingsHelper.SettingsCollectListener;
import net.osmand.plus.SettingsHelper.SettingsItem;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static net.osmand.IndexConstants.SQLITE_EXT;

public class CustomOsmandPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(CustomOsmandPlugin.class);

	public String pluginId;
	public Map<String, String> names = new HashMap<>();
	public Map<String, String> descriptions = new HashMap<>();
	public Map<String, String> iconNames = new HashMap<>();
	public Map<String, String> imageNames = new HashMap<>();
	public Drawable icon;
	public Drawable image;

	public List<String> rendererNames = new ArrayList<>();
	public List<String> routerNames = new ArrayList<>();

	public CustomOsmandPlugin(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app);
		pluginId = json.getString("pluginId");
		readAdditionalDataFromJson(json);
		readDependentFilesFromJson(json);
	}

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
					for (Iterator<SettingsItem> iterator = items.iterator(); iterator.hasNext(); ) {
						SettingsItem item = iterator.next();
						if (item instanceof ProfileSettingsItem) {
							ProfileSettingsItem profileSettingsItem = (ProfileSettingsItem) item;
							ApplicationMode mode = profileSettingsItem.getAppMode();
							ApplicationMode savedMode = ApplicationMode.valueOfStringKey(mode.getStringKey(), null);
							if (savedMode != null) {
								ApplicationMode.changeProfileAvailability(savedMode, true, app);
							}
							iterator.remove();
						} else if (item instanceof PluginSettingsItem) {
							iterator.remove();
						} else {
							item.setShouldReplace(true);
						}
					}
					app.getSettingsHelper().importSettings(file, items, "", 1, null);
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
							QuickActionRegistry actionRegistry = app.getQuickActionRegistry();
							for (QuickAction action : quickActions) {
								QuickAction savedAction = actionRegistry.getQuickAction(app, action.getType(), action.getName(app), action.getParams());
								if (savedAction != null) {
									actionRegistry.deleteQuickAction(savedAction);
								}
							}
						} else if (item instanceof MapSourcesSettingsItem) {
							MapSourcesSettingsItem mapSourcesSettingsItem = (MapSourcesSettingsItem) item;
							List<ITileSource> mapSources = mapSourcesSettingsItem.getItems();

							for (ITileSource tileSource : mapSources) {
								if (tileSource instanceof TileSourceManager.TileSourceTemplate) {
									TileSourceManager.TileSourceTemplate sourceTemplate = (TileSourceManager.TileSourceTemplate) tileSource;
									File tPath = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
									File dir = new File(tPath, sourceTemplate.getName());
									Algorithms.removeAllFiles(dir);
								} else if (tileSource instanceof SQLiteTileSource) {
									SQLiteTileSource sqLiteTileSource = ((SQLiteTileSource) tileSource);
									sqLiteTileSource.closeDB();

									File tPath = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
									File dir = new File(tPath, sqLiteTileSource.getName() + SQLITE_EXT);
									Algorithms.removeAllFiles(dir);
								}
							}
						} else if (item instanceof PoiUiFilterSettingsItem) {
							PoiUiFilterSettingsItem poiUiFilterSettingsItem = (PoiUiFilterSettingsItem) item;
							List<PoiUIFilter> poiUIFilters = poiUiFilterSettingsItem.getItems();
							for (PoiUIFilter filter : poiUIFilters) {
								app.getPoiFilters().removePoiFilter(filter);
							}
							app.getPoiFilters().reloadAllPoiFilters();
							app.getPoiFilters().loadSelectedPoiFilters();
							app.getSearchUICore().refreshCustomPoiFilters();
						} else if (item instanceof AvoidRoadsSettingsItem) {
							AvoidRoadsSettingsItem avoidRoadsSettingsItem = (AvoidRoadsSettingsItem) item;
							List<AvoidSpecificRoads.AvoidRoadInfo> avoidRoadInfos = avoidRoadsSettingsItem.getItems();
							for (AvoidSpecificRoads.AvoidRoadInfo avoidRoad : avoidRoadInfos) {
								app.getAvoidSpecificRoads().removeImpassableRoad(avoidRoad);
							}
						} else if (item instanceof ProfileSettingsItem) {
							ProfileSettingsItem profileSettingsItem = (ProfileSettingsItem) item;
							ApplicationMode mode = profileSettingsItem.getAppMode();
							ApplicationMode savedMode = ApplicationMode.valueOfStringKey(mode.getStringKey(), null);
							if (savedMode != null) {
								ApplicationMode.changeProfileAvailability(savedMode, false, app);
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

	public void readAdditionalDataFromJson(JSONObject json) throws JSONException {
		JSONObject iconJson = json.has("icon") ? json.getJSONObject("icon") : null;
		if (iconJson != null) {
			for (Iterator<String> it = iconJson.keys(); it.hasNext(); ) {
				String iconKey = it.next();
				String name = iconJson.getString(iconKey);
				iconNames.put(iconKey, name);
			}
		}
		JSONObject imageJson = json.has("image") ? json.getJSONObject("image") : null;
		if (imageJson != null) {
			for (Iterator<String> it = imageJson.keys(); it.hasNext(); ) {
				String imageKey = it.next();
				String name = imageJson.getString(imageKey);
				imageNames.put(imageKey, name);
			}
		}
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
		JSONObject iconJson = new JSONObject();
		for (Map.Entry<String, String> entry : iconNames.entrySet()) {
			iconJson.put(entry.getKey(), entry.getValue());
		}
		json.put("icon", iconJson);

		JSONObject imageJson = new JSONObject();
		for (Map.Entry<String, String> entry : imageNames.entrySet()) {
			imageJson.put(entry.getKey(), entry.getValue());
		}
		json.put("image", imageJson);

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

	public void readDependentFilesFromJson(JSONObject json) throws JSONException {
		JSONArray rendererNamesJson = json.has("rendererNames") ? json.getJSONArray("rendererNames") : null;
		if (rendererNamesJson != null) {
			for (int i = 0; i < rendererNamesJson.length(); i++) {
				String renderer = rendererNamesJson.getString(i);
				rendererNames.add(renderer);
			}
		}
		JSONArray routerNamesJson = json.has("routerNames") ? json.getJSONArray("routerNames") : null;
		if (routerNamesJson != null) {
			for (int i = 0; i < routerNamesJson.length(); i++) {
				String renderer = routerNamesJson.getString(i);
				routerNames.add(renderer);
			}
		}
	}

	public void writeDependentFilesJson(JSONObject json) throws JSONException {
		JSONArray rendererNamesJson = new JSONArray();
		for (String render : rendererNames) {
			rendererNamesJson.put(render);
		}
		json.put("rendererNames", rendererNamesJson);

		JSONArray routerNamesJson = new JSONArray();
		for (String render : routerNames) {
			routerNamesJson.put(render);
		}
		json.put("routerNames", routerNamesJson);
	}

	@Override
	public List<String> getRendererNames() {
		return rendererNames;
	}

	@Override
	public List<String> getRouterNames() {
		return routerNames;
	}

	public void addRenderer(String fileName) {
		String renderer = RendererRegistry.formatRendererFileName(fileName);
		rendererNames.add(renderer.replace('_', ' ').replace('-', ' '));
	}

	public void updateCustomItems(List<SettingsItem> items) {
		for (SettingsItem item : items) {
			if (item instanceof SettingsHelper.ResourcesSettingsItem) {
				SettingsHelper.ResourcesSettingsItem resourcesSettingsItem = (SettingsHelper.ResourcesSettingsItem) item;
				File pluginDir = resourcesSettingsItem.getPluginPath();
				File pluginResDir = new File(pluginDir, resourcesSettingsItem.getFileName());
				if (pluginResDir.exists() && pluginResDir.isDirectory()) {
					File[] files = pluginResDir.listFiles();
					for (File resFile : files) {
						String path = resFile.getAbsolutePath();
						for (Map.Entry<String, String> entry : iconNames.entrySet()) {
							String value = entry.getValue();
							if (value.startsWith("@")) {
								value = value.substring(1);
								if (path.endsWith(value)) {
									icon = BitmapDrawable.createFromPath(path);
									break;
								}
							}
						}
						for (Map.Entry<String, String> entry : imageNames.entrySet()) {
							String value = entry.getValue();
							if (value.startsWith("@")) {
								value = value.substring(1);
								if (path.endsWith(value)) {
									image = BitmapDrawable.createFromPath(path);
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public int getLogoResourceId() {
		return super.getLogoResourceId();
	}

	@Override
	public Drawable getAssetResourceImage() {
		return image;
	}
}