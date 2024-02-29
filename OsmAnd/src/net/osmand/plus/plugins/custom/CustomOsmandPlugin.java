package net.osmand.plus.plugins.custom;

import static net.osmand.IndexConstants.SQLITE_EXT;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.ITileSource;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.avoidroads.AvoidRoadInfo;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.backup.SettingsHelper.CollectListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper.ImportListener;
import net.osmand.plus.settings.backend.backup.items.AvoidRoadsSettingsItem;
import net.osmand.plus.settings.backend.backup.items.MapSourcesSettingsItem;
import net.osmand.plus.settings.backend.backup.items.PluginSettingsItem;
import net.osmand.plus.settings.backend.backup.items.PoiUiFiltersSettingsItem;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.QuickActionsSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.JsonUtils;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomOsmandPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(CustomOsmandPlugin.class);

	private final int version;
	private final String pluginId;
	private String resourceDirName;
	private Map<String, String> names = new HashMap<>();
	private Map<String, String> descriptions = new HashMap<>();
	private Map<String, String> iconNames = new HashMap<>();
	private Map<String, String> imageNames = new HashMap<>();

	private Drawable icon;
	private Drawable image;

	private List<String> rendererNames = new ArrayList<>();
	private List<String> routerNames = new ArrayList<>();
	private List<SuggestedDownloadItem> suggestedDownloadItems = new ArrayList<>();
	private List<WorldRegion> customRegions = new ArrayList<>();

	public CustomOsmandPlugin(@NonNull OsmandApplication app, @NonNull JSONObject json) throws JSONException {
		super(app);
		pluginId = json.getString("pluginId");
		version = json.optInt("version", -1);
		readAdditionalDataFromJson(json);
		readDependentFilesFromJson(json);
		loadResources();
	}

	@Override
	public String getId() {
		return pluginId;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public String getName() {
		return JsonUtils.getLocalizedResFromMap(app, names, app.getString(R.string.custom_osmand_plugin));
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		String description = JsonUtils.getLocalizedResFromMap(app, descriptions, null);
		return description != null ? Html.fromHtml(description) : null;
	}

	public String getResourceDirName() {
		return resourceDirName;
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		super.init(app, activity);
		if (activity != null) {
			// called from UI
			File pluginItemsFile = getPluginItemsFile();
			if (pluginItemsFile.exists()) {
				addPluginItemsFromFile(pluginItemsFile, activity);
			}
		}
		return true;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		super.disable(app);
		removePluginItems(null);
	}

	public File getPluginDir() {
		return app.getAppPath(IndexConstants.PLUGINS_DIR + pluginId);
	}

	public File getPluginItemsFile() {
		return new File(getPluginDir(), "items" + IndexConstants.OSMAND_SETTINGS_FILE_EXT);
	}

	public File getPluginResDir() {
		File pluginDir = getPluginDir();
		if (!Algorithms.isEmpty(resourceDirName)) {
			return new File(pluginDir, resourceDirName);
		}
		return pluginDir;
	}

	@Override
	public List<String> getRendererNames() {
		return rendererNames;
	}

	@Override
	public List<String> getRouterNames() {
		return routerNames;
	}

	@Nullable
	private Drawable getIconForFile(String path, Map<String, String> fileNames) {
		for (Map.Entry<String, String> entry : fileNames.entrySet()) {
			String value = entry.getValue();
			if (value.startsWith("@")) {
				value = value.substring(1);
			}
			if (path.endsWith(value)) {
				return BitmapDrawable.createFromPath(path);
			}
		}
		return null;
	}

	@NonNull
	@Override
	public Drawable getLogoResource() {
		return icon != null ? icon : super.getLogoResource();
	}

	@Override
	public Drawable getAssetResourceImage() {
		return image;
	}

	@Override
	public List<WorldRegion> getDownloadMaps() {
		return customRegions;
	}

	@Override
	public List<IndexItem> getSuggestedMaps() {
		List<IndexItem> suggestedMaps = new ArrayList<>();

		DownloadIndexesThread downloadThread = app.getDownloadThread();
		if (!downloadThread.getIndexes().isDownloadedFromInternet && app.getSettings().isInternetConnectionAvailable()) {
			downloadThread.runReloadIndexFiles();
		}

		if (!downloadThread.shouldDownloadIndexes()) {
			for (SuggestedDownloadItem item : suggestedDownloadItems) {
				DownloadActivityType type = DownloadActivityType.getIndexType(item.scopeId);
				if (type != null) {
					List<IndexItem> foundMaps = new ArrayList<>();
					String searchType = item.getSearchType();
					if ("latlon".equalsIgnoreCase(searchType)) {
						LatLon latLon = app.getMapViewTrackingUtilities().getMapLocation();
						foundMaps.addAll(getMapsForType(latLon, type));
					} else if ("worldregion".equalsIgnoreCase(searchType)) {
						LatLon latLon = app.getMapViewTrackingUtilities().getMapLocation();
						foundMaps.addAll(getMapsForType(latLon, type));
					}
					if (!Algorithms.isEmpty(item.getNames())) {
						foundMaps.addAll(getMapsForType(item.getNames(), type, item.getLimit()));
					}
					suggestedMaps.addAll(foundMaps);
				}
			}
		}

		return suggestedMaps;
	}

	public void setResourceDirName(String resourceDirName) {
		this.resourceDirName = resourceDirName;
	}

	private void addPluginItemsFromFile(File file, Activity activity) {
		ProgressDialog progress = new ProgressDialog(activity);
		progress.setTitle(app.getString(R.string.loading_smth, ""));
		progress.setMessage(app.getString(R.string.loading_data));
		progress.setIndeterminate(true);
		progress.setCancelable(false);

		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			progress.show();
		}

		ImportListener importListener = new ImportListener() {
			@Override
			public void onImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items) {
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					progress.dismiss();
				}
			}
		};

		app.getFileSettingsHelper().collectSettings(file, "", 1, new CollectListener() {
			@Override
			public void onCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items) {
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
						} else if (!(item instanceof PluginSettingsItem)) {
							item.setShouldReplace(true);
						}
					}
					app.getFileSettingsHelper().importSettings(file, items, "", 1, importListener);
				}
			}
		});
	}

	public void removePluginItems(PluginItemsListener itemsListener) {
		File pluginItemsFile = getPluginItemsFile();
		if (pluginItemsFile.exists()) {
			removePluginItemsFromFile(pluginItemsFile, itemsListener);
		}
	}

	private void removePluginItemsFromFile(File file, PluginItemsListener itemsListener) {
		app.getFileSettingsHelper().collectSettings(file, "", 1, (succeed, empty, items) -> {
			if (succeed && !items.isEmpty()) {
				for (SettingsItem item : items) {
					if (item instanceof QuickActionsSettingsItem) {
						QuickActionsSettingsItem settingsItem = (QuickActionsSettingsItem) item;

						MapButtonsHelper mapButtonsHelper = app.getMapButtonsHelper();
						QuickActionButtonState buttonState = settingsItem.getButtonState();
						QuickActionButtonState state = mapButtonsHelper.getButtonStateById(buttonState.getId());
						if (state != null) {
							for (QuickAction action : buttonState.getQuickActions()) {
								QuickAction savedAction = state.getQuickAction(action.getType(), action.getName(app), action.getParams());
								if (savedAction != null) {
									mapButtonsHelper.deleteQuickAction(state, savedAction);
								}
							}
						}
					} else if (item instanceof MapSourcesSettingsItem) {
						MapSourcesSettingsItem mapSourcesSettingsItem = (MapSourcesSettingsItem) item;
						List<ITileSource> mapSources = mapSourcesSettingsItem.getItems();

						for (ITileSource tileSource : mapSources) {
							String tileSourceName = tileSource.getName();
							if (tileSource instanceof SQLiteTileSource) {
								tileSourceName += SQLITE_EXT;
							}

							ITileSource savedTileSource = app.getSettings().getTileSourceByName(tileSourceName, false);
							if (savedTileSource != null) {
								if (savedTileSource instanceof SQLiteTileSource) {
									SQLiteTileSource sqLiteTileSource = ((SQLiteTileSource) savedTileSource);
									sqLiteTileSource.closeDB();
								}

								File tPath = app.getAppPath(IndexConstants.TILES_INDEX_DIR);
								File dir = new File(tPath, tileSourceName);
								Algorithms.removeAllFiles(dir);
							}
						}
					} else if (item instanceof PoiUiFiltersSettingsItem) {
						PoiUiFiltersSettingsItem poiUiFiltersSettingsItem = (PoiUiFiltersSettingsItem) item;
						List<PoiUIFilter> poiUIFilters = poiUiFiltersSettingsItem.getItems();
						for (PoiUIFilter filter : poiUIFilters) {
							app.getPoiFilters().removePoiFilter(filter);
						}
						app.getPoiFilters().reloadAllPoiFilters();
						app.getPoiFilters().loadSelectedPoiFilters();
						app.getSearchUICore().refreshCustomPoiFilters();
					} else if (item instanceof AvoidRoadsSettingsItem) {
						AvoidRoadsSettingsItem avoidRoadsSettingsItem = (AvoidRoadsSettingsItem) item;
						List<AvoidRoadInfo> avoidRoadInfos = avoidRoadsSettingsItem.getItems();
						for (AvoidRoadInfo avoidRoad : avoidRoadInfos) {
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
			if (itemsListener != null) {
				itemsListener.onItemsRemoved();
			}
		});
	}

	public void readAdditionalDataFromJson(JSONObject json) throws JSONException {
		iconNames = JsonUtils.getLocalizedMapFromJson("icon", json);
		imageNames = JsonUtils.getLocalizedMapFromJson("image", json);
		names = JsonUtils.getLocalizedMapFromJson("name", json);
		descriptions = JsonUtils.getLocalizedMapFromJson("description", json);

		JSONArray regionsJson = json.optJSONArray("regionsJson");
		if (regionsJson != null) {
			customRegions.addAll(collectRegionsFromJson(app, regionsJson));
		}
	}

	public void writeAdditionalDataToJson(JSONObject json) throws JSONException {
		JsonUtils.writeLocalizedMapToJson("icon", json, iconNames);
		JsonUtils.writeLocalizedMapToJson("image", json, imageNames);
		JsonUtils.writeLocalizedMapToJson("name", json, names);
		JsonUtils.writeLocalizedMapToJson("description", json, descriptions);

		JSONArray regionsJson = new JSONArray();
		for (WorldRegion region : getFlatCustomRegions()) {
			if (region instanceof CustomRegion) {
				regionsJson.put(((CustomRegion) region).toJson());
			}
		}
		json.put("regionsJson", regionsJson);
	}

	private List<WorldRegion> getFlatCustomRegions() {
		List<WorldRegion> l = new ArrayList<>(customRegions);
		for (WorldRegion region : customRegions) {
			collectCustomSubregionsFromRegion(region, l);
		}
		return l;
	}

	private void collectCustomSubregionsFromRegion(WorldRegion region, List<WorldRegion> items) {
		items.addAll(region.getSubregions());
		for (WorldRegion subregion : region.getSubregions()) {
			collectCustomSubregionsFromRegion(subregion, items);
		}
	}

	public void readDependentFilesFromJson(JSONObject json) throws JSONException {
		rendererNames = JsonUtils.jsonArrayToList("rendererNames", json);
		routerNames = JsonUtils.jsonArrayToList("routerNames", json);
		resourceDirName = json.optString("pluginResDir");
	}

	public void writeDependentFilesJson(JSONObject json) throws JSONException {
		JsonUtils.writeStringListToJson("rendererNames", json, rendererNames);
		JsonUtils.writeStringListToJson("routerNames", json, routerNames);

		json.put("pluginResDir", resourceDirName);
	}

	public static List<CustomRegion> collectRegionsFromJson(@NonNull Context ctx, JSONArray jsonArray) throws JSONException {
		List<CustomRegion> customRegions = new ArrayList<>();
		Map<String, CustomRegion> flatRegions = new LinkedHashMap<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject regionJson = jsonArray.getJSONObject(i);
			CustomRegion region = CustomRegion.fromJson(ctx, regionJson);
			flatRegions.put(region.getPath(), region);
		}
		for (CustomRegion region : flatRegions.values()) {
			if (!Algorithms.isEmpty(region.getParentPath())) {
				CustomRegion parentReg = flatRegions.get(region.getParentPath());
				if (parentReg != null) {
					parentReg.addSubregion(region);
				}
			} else {
				customRegions.add(region);
			}
		}
		return customRegions;
	}

	public void addRouter(String fileName) {
		String routerName = Algorithms.getFileWithoutDirs(fileName);
		if (!routerNames.contains(routerName)) {
			routerNames.add(routerName);
		}
	}

	public void addRenderer(String fileName) {
		String rendererName = Algorithms.getFileWithoutDirs(fileName);
		if (!rendererNames.contains(rendererName)) {
			rendererNames.add(rendererName);
		}
	}

	public void loadResources() {
		File pluginResDir = getPluginResDir();
		if (pluginResDir.exists() && pluginResDir.isDirectory()) {
			List<File> files = FileUtils.collectDirFiles(pluginResDir);
			for (File file : files) {
				String path = file.getAbsolutePath();
				if (icon == null) {
					icon = getIconForFile(path, iconNames);
				}
				if (image == null) {
					image = getIconForFile(path, imageNames);
				}
			}
		}
		for (WorldRegion region : customRegions) {
			loadSubregionIndexItems(region);
		}
	}

	private void loadSubregionIndexItems(WorldRegion region) {
		if (region instanceof CustomRegion) {
			((CustomRegion) region).loadDynamicIndexItems(app);
		}
		for (WorldRegion subregion : region.getSubregions()) {
			loadSubregionIndexItems(subregion);
		}
	}

	public void updateSuggestedDownloads(List<SuggestedDownloadItem> items) {
		suggestedDownloadItems = new ArrayList<>(items);
	}

	public void updateDownloadItems(List<WorldRegion> items) {
		customRegions = new ArrayList<>(items);
	}

	private List<IndexItem> getMapsForType(List<String> names, DownloadActivityType type, int limit) {
		return DownloadResources.findIndexItemsAt(app, names, type, false, limit);
	}

	public interface PluginItemsListener {

		void onItemsRemoved();

	}

	public static class SuggestedDownloadItem {

		private final String scopeId;
		private final String searchType;
		private final List<String> names;
		private final int limit;

		public SuggestedDownloadItem(String scopeId, String searchType, List<String> names, int limit) {
			this.scopeId = scopeId;
			this.limit = limit;
			this.searchType = searchType;
			this.names = names;
		}

		public String getScopeId() {
			return scopeId;
		}

		public String getSearchType() {
			return searchType;
		}

		public List<String> getNames() {
			return names;
		}

		public int getLimit() {
			return limit;
		}
	}
}