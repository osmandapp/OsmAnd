package net.osmand.plus.settings.backend.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.FavouritesDbHelper.FavoriteGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.mapmarkers.ItineraryType;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.onlinerouting.engine.OnlineRoutingEngine;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ApplicationMode.ApplicationModeBean;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.items.AvoidRoadsSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FavoritesSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.GlobalSettingsItem;
import net.osmand.plus.settings.backend.backup.items.GpxSettingsItem;
import net.osmand.plus.settings.backend.backup.items.HistoryMarkersSettingsItem;
import net.osmand.plus.settings.backend.backup.items.ItinerarySettingsItem;
import net.osmand.plus.settings.backend.backup.items.MapSourcesSettingsItem;
import net.osmand.plus.settings.backend.backup.items.MarkersSettingsItem;
import net.osmand.plus.settings.backend.backup.items.OnlineRoutingSettingsItem;
import net.osmand.plus.settings.backend.backup.items.OsmEditsSettingsItem;
import net.osmand.plus.settings.backend.backup.items.OsmNotesSettingsItem;
import net.osmand.plus.settings.backend.backup.items.PoiUiFiltersSettingsItem;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.QuickActionsSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SearchHistorySettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import static net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;

public abstract class SettingsHelper {

	public static final int VERSION = 1;

	public static final String SETTINGS_TYPE_LIST_KEY = "settings_type_list_key";
	public static final String REPLACE_KEY = "replace";
	public static final String SILENT_IMPORT_KEY = "silent_import";
	public static final String SETTINGS_LATEST_CHANGES_KEY = "settings_latest_changes";
	public static final String SETTINGS_VERSION_KEY = "settings_version";

	public static final int BUFFER = 1024;

	public static final Log LOG = PlatformUtil.getLog(SettingsHelper.class);

	private final OsmandApplication app;

	public interface CollectListener {
		void onCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items);
	}

	public interface ImportListener {
		void onImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items);
	}

	public interface CheckDuplicatesListener {
		void onDuplicatesChecked(@NonNull List<Object> duplicates, List<SettingsItem> items);
	}

	public interface ExportProgressListener {
		void updateProgress(int value);
	}

	public enum ImportType {
		COLLECT,
		COLLECT_AND_READ,
		CHECK_DUPLICATES,
		IMPORT,
		IMPORT_FORCE_READ,
	}

	public SettingsHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public OsmandApplication getApp() {
		return app;
	}

	public List<SettingsItem> getFilteredSettingsItems(List<ExportSettingsType> settingsTypes, boolean addProfiles, boolean export, boolean addEmptyItems) {
		Map<ExportSettingsType, List<?>> typesMap = new HashMap<>();
		typesMap.putAll(getSettingsItems(addProfiles, addEmptyItems));
		typesMap.putAll(getMyPlacesItems(addEmptyItems));
		typesMap.putAll(getResourcesItems(addEmptyItems));

		return getFilteredSettingsItems(typesMap, settingsTypes, Collections.emptyList(), export);
	}

	public List<SettingsItem> getFilteredSettingsItems(@NonNull Map<ExportSettingsType, List<?>> allSettingsMap,
													   @NonNull List<ExportSettingsType> settingsTypes,
													   @NonNull List<SettingsItem> settingsItems,
													   boolean export) {
		List<SettingsItem> filteredSettingsItems = new ArrayList<>();
		for (ExportSettingsType settingsType : settingsTypes) {
			List<?> settingsDataObjects = allSettingsMap.get(settingsType);
			if (settingsDataObjects != null) {
				filteredSettingsItems.addAll(prepareSettingsItems(settingsDataObjects, settingsItems, export));
			}
		}
		return filteredSettingsItems;
	}

	public Map<ExportSettingsCategory, SettingsCategoryItems> getSettingsByCategory(boolean addProfiles, boolean addEmptyItems) {
		Map<ExportSettingsCategory, SettingsCategoryItems> dataList = new LinkedHashMap<>();

		Map<ExportSettingsType, List<?>> settingsItems = getSettingsItems(addProfiles, addEmptyItems);
		Map<ExportSettingsType, List<?>> myPlacesItems = getMyPlacesItems(addEmptyItems);
		Map<ExportSettingsType, List<?>> resourcesItems = getResourcesItems(addEmptyItems);

		if (!settingsItems.isEmpty() || addEmptyItems) {
			dataList.put(ExportSettingsCategory.SETTINGS, new SettingsCategoryItems(settingsItems));
		}
		if (!myPlacesItems.isEmpty() || addEmptyItems) {
			dataList.put(ExportSettingsCategory.MY_PLACES, new SettingsCategoryItems(myPlacesItems));
		}
		if (!resourcesItems.isEmpty() || addEmptyItems) {
			dataList.put(ExportSettingsCategory.RESOURCES, new SettingsCategoryItems(resourcesItems));
		}

		return dataList;
	}

	private Map<ExportSettingsType, List<?>> getSettingsItems(boolean addProfiles, boolean addEmptyItems) {
		Map<ExportSettingsType, List<?>> settingsItems = new LinkedHashMap<>();

		if (addProfiles) {
			List<ApplicationModeBean> appModeBeans = new ArrayList<>();
			for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
				appModeBeans.add(mode.toModeBean());
			}
			settingsItems.put(ExportSettingsType.PROFILE, appModeBeans);
		}
		settingsItems.put(ExportSettingsType.GLOBAL, Collections.singletonList(new GlobalSettingsItem(app.getSettings())));

		QuickActionRegistry registry = app.getQuickActionRegistry();
		List<QuickAction> actionsList = registry.getQuickActions();
		if (!actionsList.isEmpty() || addEmptyItems) {
			settingsItems.put(ExportSettingsType.QUICK_ACTIONS, actionsList);
		}
		List<PoiUIFilter> poiList = app.getPoiFilters().getUserDefinedPoiFilters(false);
		if (!poiList.isEmpty() || addEmptyItems) {
			settingsItems.put(ExportSettingsType.POI_TYPES, poiList);
		}
		Map<LatLon, AvoidRoadInfo> impassableRoads = app.getAvoidSpecificRoads().getImpassableRoads();
		if (!impassableRoads.isEmpty() || addEmptyItems) {
			settingsItems.put(ExportSettingsType.AVOID_ROADS, new ArrayList<>(impassableRoads.values()));
		}
		return settingsItems;
	}

	private Map<ExportSettingsType, List<?>> getMyPlacesItems(boolean addEmptyItems) {
		Map<ExportSettingsType, List<?>> myPlacesItems = new LinkedHashMap<>();

		List<FavoriteGroup> favoriteGroups = app.getFavorites().getFavoriteGroups();
		if (!favoriteGroups.isEmpty() || addEmptyItems) {
			myPlacesItems.put(ExportSettingsType.FAVORITES, favoriteGroups);
		}
		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> gpxInfoList = GpxUiHelper.getSortedGPXFilesInfo(gpxDir, null, true);
		if (!gpxInfoList.isEmpty() || addEmptyItems) {
			List<File> files = new ArrayList<>();
			for (GPXInfo gpxInfo : gpxInfoList) {
				File file = new File(gpxInfo.getFileName());
				if (file.exists()) {
					files.add(file);
				}
			}
			if (!files.isEmpty() || addEmptyItems) {
				myPlacesItems.put(ExportSettingsType.TRACKS, files);
			}
		}
		OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getEnabledPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			List<OsmNotesPoint> notesPointList = osmEditingPlugin.getDBBug().getOsmbugsPoints();
			if (!notesPointList.isEmpty() || addEmptyItems) {
				myPlacesItems.put(ExportSettingsType.OSM_NOTES, notesPointList);
			}
			List<OpenstreetmapPoint> editsPointList = osmEditingPlugin.getDBPOI().getOpenstreetmapPoints();
			if (!editsPointList.isEmpty() || addEmptyItems) {
				myPlacesItems.put(ExportSettingsType.OSM_EDITS, editsPointList);
			}
		}
		AudioVideoNotesPlugin plugin = OsmandPlugin.getEnabledPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			List<File> files = new ArrayList<>();
			for (Recording rec : plugin.getAllRecordings()) {
				File file = rec.getFile();
				if (file != null && file.exists()) {
					files.add(file);
				}
			}
			if (!files.isEmpty() || addEmptyItems) {
				myPlacesItems.put(ExportSettingsType.MULTIMEDIA_NOTES, files);
			}
		}
		List<MapMarker> mapMarkers = app.getMapMarkersHelper().getMapMarkers();
		if (!mapMarkers.isEmpty() || addEmptyItems) {
			String name = app.getString(R.string.map_markers);
			String groupId = ExportSettingsType.ACTIVE_MARKERS.name();
			MapMarkersGroup markersGroup = new MapMarkersGroup(groupId, name, ItineraryType.MARKERS);
			markersGroup.setMarkers(mapMarkers);
			myPlacesItems.put(ExportSettingsType.ACTIVE_MARKERS, Collections.singletonList(markersGroup));
		}
		List<MapMarker> markersHistory = app.getMapMarkersHelper().getMapMarkersHistory();
		if (!markersHistory.isEmpty() || addEmptyItems) {
			String name = app.getString(R.string.shared_string_history);
			String groupId = ExportSettingsType.HISTORY_MARKERS.name();
			MapMarkersGroup markersGroup = new MapMarkersGroup(groupId, name, ItineraryType.MARKERS);
			markersGroup.setMarkers(markersHistory);
			myPlacesItems.put(ExportSettingsType.HISTORY_MARKERS, Collections.singletonList(markersGroup));
		}
		List<HistoryEntry> historyEntries = SearchHistoryHelper.getInstance(app).getHistoryEntries(false);
		if (!historyEntries.isEmpty() || addEmptyItems) {
			myPlacesItems.put(ExportSettingsType.SEARCH_HISTORY, historyEntries);
		}
		List<MapMarkersGroup> markersGroups = app.getMapMarkersHelper().getVisibleMapMarkersGroups();
		if (!markersGroups.isEmpty() || addEmptyItems) {
			myPlacesItems.put(ExportSettingsType.ITINERARY_GROUPS, markersGroups);
		}
		return myPlacesItems;
	}

	private Map<ExportSettingsType, List<?>> getResourcesItems(boolean addEmptyItems) {
		Map<ExportSettingsType, List<?>> resourcesItems = new LinkedHashMap<>();

		Map<String, File> externalRenderers = app.getRendererRegistry().getExternalRenderers();
		if (!externalRenderers.isEmpty() || addEmptyItems) {
			resourcesItems.put(ExportSettingsType.CUSTOM_RENDER_STYLE, new ArrayList<>(externalRenderers.values()));
		}
		List<File> routingProfiles = new ArrayList<>();
		File routingProfilesFolder = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
		if (routingProfilesFolder.exists() && routingProfilesFolder.isDirectory()) {
			File[] fl = routingProfilesFolder.listFiles();
			if (fl != null && fl.length > 0) {
				routingProfiles.addAll(Arrays.asList(fl));
			}
		}
		if (!Algorithms.isEmpty(routingProfiles) || addEmptyItems) {
			resourcesItems.put(ExportSettingsType.CUSTOM_ROUTING, routingProfiles);
		}
		List<OnlineRoutingEngine> onlineRoutingEngines = app.getOnlineRoutingHelper().getOnlyCustomEngines();
		if (!Algorithms.isEmpty(onlineRoutingEngines) || addEmptyItems) {
			resourcesItems.put(ExportSettingsType.ONLINE_ROUTING_ENGINES, onlineRoutingEngines);
		}
		List<ITileSource> iTileSources = new ArrayList<>();
		Set<String> tileSourceNames = app.getSettings().getTileSourceEntries(true).keySet();
		for (String name : tileSourceNames) {
			File f = app.getAppPath(IndexConstants.TILES_INDEX_DIR + name);
			if (f != null) {
				ITileSource template;
				if (f.getName().endsWith(SQLiteTileSource.EXT)) {
					template = new SQLiteTileSource(app, f, TileSourceManager.getKnownSourceTemplates());
				} else {
					template = TileSourceManager.createTileSourceTemplate(f);
				}
				if (template.getUrlTemplate() != null) {
					iTileSources.add(template);
				}
			}
		}
		if (!iTileSources.isEmpty() || addEmptyItems) {
			resourcesItems.put(ExportSettingsType.MAP_SOURCES, iTileSources);
		}
		List<LocalIndexInfo> localIndexInfoList = getLocalIndexData();
		List<File> files = getFilesByType(localIndexInfoList, LocalIndexType.MAP_DATA, LocalIndexType.TILES_DATA,
				LocalIndexType.SRTM_DATA, LocalIndexType.WIKI_DATA);
		if (!files.isEmpty() || addEmptyItems) {
			sortLocalFiles(files);
			resourcesItems.put(ExportSettingsType.OFFLINE_MAPS, files);
		}
		files = getFilesByType(localIndexInfoList, LocalIndexType.TTS_VOICE_DATA);
		if (!files.isEmpty() || addEmptyItems) {
			resourcesItems.put(ExportSettingsType.TTS_VOICE, files);
		}
		files = getFilesByType(localIndexInfoList, LocalIndexType.VOICE_DATA);
		if (!files.isEmpty() || addEmptyItems) {
			resourcesItems.put(ExportSettingsType.VOICE, files);
		}

		return resourcesItems;
	}

	private List<LocalIndexInfo> getLocalIndexData() {
		return new LocalIndexHelper(app).getLocalIndexData(new AbstractLoadLocalIndexTask() {
			@Override
			public void loadFile(LocalIndexInfo... loaded) {
			}
		});
	}

	private List<File> getFilesByType(List<LocalIndexInfo> localVoiceFileList, LocalIndexType... localIndexType) {
		List<File> files = new ArrayList<>();
		for (LocalIndexInfo map : localVoiceFileList) {
			File file = new File(map.getPathToData());
			boolean filtered = false;
			for (LocalIndexType type : localIndexType) {
				if (map.getType() == type) {
					filtered = true;
					break;
				}
			}
			if (file.exists() && filtered) {
				files.add(file);
			}
		}
		return files;
	}

	public List<SettingsItem> prepareSettingsItems(List<?> data, List<SettingsItem> settingsItems, boolean export) {
		List<SettingsItem> result = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();
		List<ApplicationModeBean> appModeBeans = new ArrayList<>();
		List<FavoriteGroup> favoriteGroups = new ArrayList<>();
		List<OsmNotesPoint> osmNotesPointList = new ArrayList<>();
		List<OpenstreetmapPoint> osmEditsPointList = new ArrayList<>();
		List<MapMarkersGroup> markersGroups = new ArrayList<>();
		List<MapMarkersGroup> markersHistoryGroups = new ArrayList<>();
		List<HistoryEntry> historyEntries = new ArrayList<>();
		List<OnlineRoutingEngine> onlineRoutingEngines = new ArrayList<>();
		List<MapMarkersGroup> itineraryGroups = new ArrayList<>();

		for (Object object : data) {
			if (object instanceof QuickAction) {
				quickActions.add((QuickAction) object);
			} else if (object instanceof PoiUIFilter) {
				poiUIFilters.add((PoiUIFilter) object);
			} else if (object instanceof TileSourceTemplate || object instanceof SQLiteTileSource) {
				tileSourceTemplates.add((ITileSource) object);
			} else if (object instanceof File) {
				try {
					File file = (File) object;
					if (file.getName().endsWith(IndexConstants.GPX_FILE_EXT)) {
						result.add(new GpxSettingsItem(app, file));
					} else {
						result.add(new FileSettingsItem(app, file));
					}
				} catch (IllegalArgumentException e) {
					LOG.warn("Trying to export unsuported file type", e);
				}
			} else if (object instanceof FileSettingsItem) {
				result.add((FileSettingsItem) object);
			} else if (object instanceof AvoidRoadInfo) {
				avoidRoads.add((AvoidRoadInfo) object);
			} else if (object instanceof ApplicationModeBean) {
				appModeBeans.add((ApplicationModeBean) object);
			} else if (object instanceof OsmNotesPoint) {
				osmNotesPointList.add((OsmNotesPoint) object);
			} else if (object instanceof OpenstreetmapPoint) {
				osmEditsPointList.add((OpenstreetmapPoint) object);
			} else if (object instanceof FavoriteGroup) {
				favoriteGroups.add((FavoriteGroup) object);
			} else if (object instanceof MapMarkersGroup) {
				MapMarkersGroup markersGroup = (MapMarkersGroup) object;
				if (ExportSettingsType.ACTIVE_MARKERS.name().equals(markersGroup.getId())) {
					markersGroups.add((MapMarkersGroup) object);
				} else if (ExportSettingsType.HISTORY_MARKERS.name().equals(markersGroup.getId())) {
					markersHistoryGroups.add((MapMarkersGroup) object);
				} else {
					itineraryGroups.add((MapMarkersGroup) object);
				}
			} else if (object instanceof HistoryEntry) {
				historyEntries.add((HistoryEntry) object);
			} else if (object instanceof GlobalSettingsItem) {
				result.add((GlobalSettingsItem) object);
			} else if (object instanceof OnlineRoutingEngine) {
				onlineRoutingEngines.add((OnlineRoutingEngine) object);
			}
		}
		if (!quickActions.isEmpty()) {
			QuickActionsSettingsItem baseItem = getBaseItem(SettingsItemType.QUICK_ACTIONS, QuickActionsSettingsItem.class, settingsItems);
			result.add(new QuickActionsSettingsItem(app, baseItem, quickActions));
		}
		if (!poiUIFilters.isEmpty()) {
			PoiUiFiltersSettingsItem baseItem = getBaseItem(SettingsItemType.POI_UI_FILTERS, PoiUiFiltersSettingsItem.class, settingsItems);
			result.add(new PoiUiFiltersSettingsItem(app, baseItem, poiUIFilters));
		}
		if (!tileSourceTemplates.isEmpty()) {
			MapSourcesSettingsItem baseItem = getBaseItem(SettingsItemType.MAP_SOURCES, MapSourcesSettingsItem.class, settingsItems);
			result.add(new MapSourcesSettingsItem(app, baseItem, tileSourceTemplates));
		}
		if (!avoidRoads.isEmpty()) {
			AvoidRoadsSettingsItem baseItem = getBaseItem(SettingsItemType.AVOID_ROADS, AvoidRoadsSettingsItem.class, settingsItems);
			result.add(new AvoidRoadsSettingsItem(app, baseItem, avoidRoads));
		}
		if (!appModeBeans.isEmpty()) {
			for (ApplicationModeBean modeBean : appModeBeans) {
				if (export) {
					ApplicationMode mode = ApplicationMode.valueOfStringKey(modeBean.stringKey, null);
					if (mode != null) {
						result.add(new ProfileSettingsItem(app, mode));
					}
				} else {
					result.add(new ProfileSettingsItem(app, getBaseProfileSettingsItem(modeBean, settingsItems), modeBean));
				}
			}
		}
		if (!osmNotesPointList.isEmpty()) {
			OsmNotesSettingsItem baseItem = getBaseItem(SettingsItemType.OSM_NOTES, OsmNotesSettingsItem.class, settingsItems);
			result.add(new OsmNotesSettingsItem(app, baseItem, osmNotesPointList));
		}
		if (!osmEditsPointList.isEmpty()) {
			OsmEditsSettingsItem baseItem = getBaseItem(SettingsItemType.OSM_EDITS, OsmEditsSettingsItem.class, settingsItems);
			result.add(new OsmEditsSettingsItem(app, baseItem, osmEditsPointList));
		}
		if (!favoriteGroups.isEmpty()) {
			FavoritesSettingsItem baseItem = getBaseItem(SettingsItemType.FAVOURITES, FavoritesSettingsItem.class, settingsItems);
			result.add(new FavoritesSettingsItem(app, baseItem, favoriteGroups));
		}
		if (!markersGroups.isEmpty()) {
			List<MapMarker> mapMarkers = new ArrayList<>();
			for (MapMarkersGroup group : markersGroups) {
				mapMarkers.addAll(group.getMarkers());
			}
			MarkersSettingsItem baseItem = getBaseItem(SettingsItemType.ACTIVE_MARKERS, MarkersSettingsItem.class, settingsItems);
			result.add(new MarkersSettingsItem(app, baseItem, mapMarkers));
		}
		if (!markersHistoryGroups.isEmpty()) {
			List<MapMarker> mapMarkers = new ArrayList<>();
			for (MapMarkersGroup group : markersHistoryGroups) {
				mapMarkers.addAll(group.getMarkers());
			}
			HistoryMarkersSettingsItem baseItem = getBaseItem(SettingsItemType.HISTORY_MARKERS, HistoryMarkersSettingsItem.class, settingsItems);
			result.add(new HistoryMarkersSettingsItem(app, baseItem, mapMarkers));
		}
		if (!historyEntries.isEmpty()) {
			SearchHistorySettingsItem baseItem = getBaseItem(SettingsItemType.SEARCH_HISTORY, SearchHistorySettingsItem.class, settingsItems);
			result.add(new SearchHistorySettingsItem(app, baseItem, historyEntries));
		}
		if (!onlineRoutingEngines.isEmpty()) {
			OnlineRoutingSettingsItem baseItem = getBaseItem(SettingsItemType.ONLINE_ROUTING_ENGINES, OnlineRoutingSettingsItem.class, settingsItems);
			result.add(new OnlineRoutingSettingsItem(app, baseItem, onlineRoutingEngines));
		}
		if (!itineraryGroups.isEmpty()) {
			ItinerarySettingsItem baseItem = getBaseItem(SettingsItemType.ITINERARY_GROUPS, ItinerarySettingsItem.class, settingsItems);
			result.add(new ItinerarySettingsItem(app, baseItem, itineraryGroups));
		}
		return result;
	}

	@Nullable
	private ProfileSettingsItem getBaseProfileSettingsItem(ApplicationModeBean modeBean, List<SettingsItem> settingsItems) {
		for (SettingsItem settingsItem : settingsItems) {
			if (settingsItem.getType() == SettingsItemType.PROFILE) {
				ProfileSettingsItem profileItem = (ProfileSettingsItem) settingsItem;
				ApplicationModeBean bean = profileItem.getModeBean();
				if (Algorithms.objectEquals(bean.stringKey, modeBean.stringKey) && Algorithms.objectEquals(bean.userProfileName, modeBean.userProfileName)) {
					return profileItem;
				}
			}
		}
		return null;
	}

	@Nullable
	private <T> T getBaseItem(SettingsItemType settingsItemType, Class<T> clazz, List<SettingsItem> settingsItems) {
		for (SettingsItem settingsItem : settingsItems) {
			if (settingsItem.getType() == settingsItemType && clazz.isInstance(settingsItem)) {
				return clazz.cast(settingsItem);
			}
		}
		return null;
	}

	public static Map<ExportSettingsCategory, SettingsCategoryItems> getSettingsToOperateByCategory(List<SettingsItem> items, boolean importComplete, boolean addEmptyItems) {
		Map<ExportSettingsType, List<?>> settingsToOperate = getSettingsToOperate(items, importComplete, addEmptyItems);
		return getSettingsToOperateByCategory(settingsToOperate, addEmptyItems);
	}

	public static Map<ExportSettingsCategory, SettingsCategoryItems> getSettingsToOperateByCategory(Map<ExportSettingsType, List<?>> settingsToOperate, boolean addEmptyItems) {
		Map<ExportSettingsType, List<?>> settingsItems = new LinkedHashMap<>();
		Map<ExportSettingsType, List<?>> myPlacesItems = new LinkedHashMap<>();
		Map<ExportSettingsType, List<?>> resourcesItems = new LinkedHashMap<>();

		for (Map.Entry<ExportSettingsType, List<?>> entry : settingsToOperate.entrySet()) {
			ExportSettingsType type = entry.getKey();
			if (type.isSettingsCategory()) {
				settingsItems.put(type, entry.getValue());
			} else if (type.isMyPlacesCategory()) {
				myPlacesItems.put(type, entry.getValue());
			} else if (type.isResourcesCategory()) {
				resourcesItems.put(type, entry.getValue());
			}
		}
		Map<ExportSettingsCategory, SettingsCategoryItems> exportMap = new LinkedHashMap<>();
		if (!settingsItems.isEmpty() || addEmptyItems) {
			exportMap.put(ExportSettingsCategory.SETTINGS, new SettingsCategoryItems(settingsItems));
		}
		if (!myPlacesItems.isEmpty() || addEmptyItems) {
			exportMap.put(ExportSettingsCategory.MY_PLACES, new SettingsCategoryItems(myPlacesItems));
		}
		if (!resourcesItems.isEmpty() || addEmptyItems) {
			exportMap.put(ExportSettingsCategory.RESOURCES, new SettingsCategoryItems(resourcesItems));
		}

		return exportMap;
	}

	public static Map<ExportSettingsType, List<?>> getSettingsToOperate(List<SettingsItem> settingsItems, boolean importComplete, boolean addEmptyItems) {
		Map<ExportSettingsType, List<?>> settingsToOperate = new EnumMap<>(ExportSettingsType.class);
		List<ApplicationModeBean> profiles = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		List<File> routingFilesList = new ArrayList<>();
		List<File> renderFilesList = new ArrayList<>();
		List<File> ttsVoiceFilesList = new ArrayList<>();
		List<File> voiceFilesList = new ArrayList<>();
		List<FileSettingsItem> mapFilesList = new ArrayList<>();
		List<FileSettingsItem> tracksFilesList = new ArrayList<>();
		List<FileSettingsItem> multimediaFilesList = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();
		List<GlobalSettingsItem> globalSettingsItems = new ArrayList<>();
		List<OsmNotesPoint> notesPointList = new ArrayList<>();
		List<OpenstreetmapPoint> editsPointList = new ArrayList<>();
		List<FavoriteGroup> favoriteGroups = new ArrayList<>();
		List<MapMarkersGroup> markersGroups = new ArrayList<>();
		List<HistoryEntry> historyEntries = new ArrayList<>();
		List<OnlineRoutingEngine> onlineRoutingEngines = new ArrayList<>();
		List<MapMarkersGroup> itineraryGroups = new ArrayList<>();

		for (SettingsItem item : settingsItems) {
			switch (item.getType()) {
				case PROFILE:
					profiles.add(((ProfileSettingsItem) item).getModeBean());
					break;
				case FILE:
					FileSettingsItem fileItem = (FileSettingsItem) item;
					if (fileItem.getSubtype() == FileSubtype.RENDERING_STYLE) {
						renderFilesList.add(fileItem.getFile());
					} else if (fileItem.getSubtype() == FileSubtype.ROUTING_CONFIG) {
						routingFilesList.add(fileItem.getFile());
					} else if (fileItem.getSubtype() == FileSubtype.MULTIMEDIA_NOTES) {
						multimediaFilesList.add(fileItem);
					} else if (fileItem.getSubtype() == FileSubtype.GPX) {
						tracksFilesList.add(fileItem);
					} else if (fileItem.getSubtype().isMap()) {
						mapFilesList.add(fileItem);
					} else if (fileItem.getSubtype() == FileSubtype.TTS_VOICE) {
						ttsVoiceFilesList.add(fileItem.getFile());
					} else if (fileItem.getSubtype() == FileSubtype.VOICE) {
						voiceFilesList.add(fileItem.getFile());
					}
					break;
				case QUICK_ACTIONS:
					QuickActionsSettingsItem quickActionsItem = (QuickActionsSettingsItem) item;
					if (importComplete) {
						quickActions.addAll(quickActionsItem.getAppliedItems());
					} else {
						quickActions.addAll(quickActionsItem.getItems());
					}
					break;
				case POI_UI_FILTERS:
					PoiUiFiltersSettingsItem poiUiFilterItem = (PoiUiFiltersSettingsItem) item;
					if (importComplete) {
						poiUIFilters.addAll(poiUiFilterItem.getAppliedItems());
					} else {
						poiUIFilters.addAll(poiUiFilterItem.getItems());
					}
					break;
				case MAP_SOURCES:
					MapSourcesSettingsItem mapSourcesItem = (MapSourcesSettingsItem) item;
					if (importComplete) {
						tileSourceTemplates.addAll(mapSourcesItem.getAppliedItems());
					} else {
						tileSourceTemplates.addAll(mapSourcesItem.getItems());
					}
					break;
				case AVOID_ROADS:
					AvoidRoadsSettingsItem avoidRoadsItem = (AvoidRoadsSettingsItem) item;
					if (importComplete) {
						avoidRoads.addAll(avoidRoadsItem.getAppliedItems());
					} else {
						avoidRoads.addAll(avoidRoadsItem.getItems());
					}
					break;
				case GLOBAL:
					globalSettingsItems.add((GlobalSettingsItem) item);
					break;
				case OSM_NOTES:
					OsmNotesSettingsItem osmNotesSettingsItem = (OsmNotesSettingsItem) item;
					if (importComplete) {
						notesPointList.addAll(osmNotesSettingsItem.getAppliedItems());
					} else {
						notesPointList.addAll(osmNotesSettingsItem.getItems());
					}
					break;
				case OSM_EDITS:
					OsmEditsSettingsItem osmEditsSettingsItem = (OsmEditsSettingsItem) item;
					if (importComplete) {
						editsPointList.addAll(osmEditsSettingsItem.getAppliedItems());
					} else {
						editsPointList.addAll(osmEditsSettingsItem.getItems());
					}
					break;
				case FAVOURITES:
					FavoritesSettingsItem favoritesSettingsItem = (FavoritesSettingsItem) item;
					favoriteGroups.addAll(favoritesSettingsItem.getItems());
					break;
				case ACTIVE_MARKERS:
					MarkersSettingsItem markersSettingsItem = (MarkersSettingsItem) item;
					markersGroups.add(markersSettingsItem.getMarkersGroup());
					break;
				case HISTORY_MARKERS:
					HistoryMarkersSettingsItem historyMarkersSettingsItem = (HistoryMarkersSettingsItem) item;
					markersGroups.add(historyMarkersSettingsItem.getMarkersGroup());
					break;
				case SEARCH_HISTORY:
					SearchHistorySettingsItem searchHistorySettingsItem = (SearchHistorySettingsItem) item;
					historyEntries.addAll(searchHistorySettingsItem.getItems());
					break;
				case GPX:
					tracksFilesList.add((GpxSettingsItem) item);
					break;
				case ONLINE_ROUTING_ENGINES:
					OnlineRoutingSettingsItem onlineRoutingSettingsItem = (OnlineRoutingSettingsItem) item;
					onlineRoutingEngines.addAll(onlineRoutingSettingsItem.getItems());
					break;
				case ITINERARY_GROUPS:
					ItinerarySettingsItem itinerarySettingsItem = (ItinerarySettingsItem) item;
					itineraryGroups.addAll(itinerarySettingsItem.getItems());
					break;
				default:
					break;
			}
		}
		for (SettingsItem item : settingsItems) {
			switch (item.getType()) {
				case PROFILE:
					if (!profiles.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.PROFILE, profiles);
					}
					break;
				case FILE:
					FileSettingsItem fileItem = (FileSettingsItem) item;
					if (fileItem.getSubtype() == FileSubtype.RENDERING_STYLE) {
						if (!renderFilesList.isEmpty() || addEmptyItems) {
							settingsToOperate.put(ExportSettingsType.CUSTOM_RENDER_STYLE, renderFilesList);
						}
					} else if (fileItem.getSubtype() == FileSubtype.ROUTING_CONFIG) {
						if (!routingFilesList.isEmpty() || addEmptyItems) {
							settingsToOperate.put(ExportSettingsType.CUSTOM_ROUTING, routingFilesList);
						}
					} else if (fileItem.getSubtype() == FileSubtype.MULTIMEDIA_NOTES) {
						if (!multimediaFilesList.isEmpty() || addEmptyItems) {
							settingsToOperate.put(ExportSettingsType.MULTIMEDIA_NOTES, multimediaFilesList);
						}
					} else if (fileItem.getSubtype() == FileSubtype.GPX) {
						if (!tracksFilesList.isEmpty() || addEmptyItems) {
							settingsToOperate.put(ExportSettingsType.TRACKS, tracksFilesList);
						}
					} else if (fileItem.getSubtype().isMap()) {
						if (!mapFilesList.isEmpty() || addEmptyItems) {
							settingsToOperate.put(ExportSettingsType.OFFLINE_MAPS, mapFilesList);
						}
					} else if (fileItem.getSubtype() == FileSubtype.TTS_VOICE) {
						if (!ttsVoiceFilesList.isEmpty() || addEmptyItems) {
							settingsToOperate.put(ExportSettingsType.TTS_VOICE, ttsVoiceFilesList);
						}
					} else if (fileItem.getSubtype() == FileSubtype.VOICE) {
						if (!voiceFilesList.isEmpty() || addEmptyItems) {
							settingsToOperate.put(ExportSettingsType.VOICE, voiceFilesList);
						}
					}
					break;
				case QUICK_ACTIONS:
					if (!quickActions.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.QUICK_ACTIONS, quickActions);
					}
					break;
				case POI_UI_FILTERS:
					if (!poiUIFilters.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.POI_TYPES, poiUIFilters);
					}
					break;
				case MAP_SOURCES:
					if (!tileSourceTemplates.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.MAP_SOURCES, tileSourceTemplates);
					}
					break;
				case AVOID_ROADS:
					if (!avoidRoads.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.AVOID_ROADS, avoidRoads);
					}
					break;
				case GLOBAL:
					if (!globalSettingsItems.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.GLOBAL, globalSettingsItems);
					}
					break;
				case OSM_NOTES:
					if (!notesPointList.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.OSM_NOTES, notesPointList);
					}
					break;
				case OSM_EDITS:
					if (!editsPointList.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.OSM_EDITS, editsPointList);
					}
					break;
				case FAVOURITES:
					if (!favoriteGroups.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.FAVORITES, favoriteGroups);
					}
					break;
				case ACTIVE_MARKERS:
				case HISTORY_MARKERS:
					if (!markersGroups.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.ACTIVE_MARKERS, markersGroups);
					}
					break;
				case SEARCH_HISTORY:
					if (!historyEntries.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.SEARCH_HISTORY, historyEntries);
					}
					break;
				case GPX:
					if (!tracksFilesList.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.TRACKS, tracksFilesList);
					}
					break;
				case ONLINE_ROUTING_ENGINES:
					if (!onlineRoutingEngines.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.ONLINE_ROUTING_ENGINES, onlineRoutingEngines);
					}
					break;
				case ITINERARY_GROUPS:
					if (!itineraryGroups.isEmpty() || addEmptyItems) {
						settingsToOperate.put(ExportSettingsType.ITINERARY_GROUPS, itineraryGroups);
					}
					break;
				default:
					break;
			}
		}
		return settingsToOperate;
	}

	private void sortLocalFiles(List<File> files) {
		final Collator collator = OsmAndCollator.primaryCollator();
		Collections.sort(files, new Comparator<File>() {
			@Override
			public int compare(File lhs, File rhs) {
				return collator.compare(getNameToDisplay(lhs), getNameToDisplay(rhs));
			}

			private String getNameToDisplay(File item) {
				return FileNameTranslationHelper.getFileNameWithRegion(app, item.getName());
			}
		});
	}
}