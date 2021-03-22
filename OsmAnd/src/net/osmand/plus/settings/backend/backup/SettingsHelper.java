package net.osmand.plus.settings.backend.backup;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
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
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.itinerary.ItineraryGroup;
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
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
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

import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.plus.activities.LocalIndexHelper.LocalIndexType;
import static net.osmand.plus.settings.backend.backup.FileSettingsItem.FileSubtype;

/*
	Usage:

	SettingsHelper helper = app.getSettingsHelper();
	File file = new File(app.getAppPath(null), "settings.zip");

	List<SettingsItem> items = new ArrayList<>();
	items.add(new GlobalSettingsItem(app.getSettings()));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.DEFAULT));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.CAR));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.PEDESTRIAN));
	items.add(new ProfileSettingsItem(app.getSettings(), ApplicationMode.BICYCLE));
	items.add(new FileSettingsItem(app, new File(app.getAppPath(GPX_INDEX_DIR), "Day 2.gpx")));
	items.add(new FileSettingsItem(app, new File(app.getAppPath(GPX_INDEX_DIR), "Day 3.gpx")));
	items.add(new FileSettingsItem(app, new File(app.getAppPath(RENDERERS_DIR), "default.render.xml")));
	items.add(new DataSettingsItem(new byte[] {'t', 'e', 's', 't', '1'}, "data1"));
	items.add(new DataSettingsItem(new byte[] {'t', 'e', 's', 't', '2'}, "data2"));

	helper.exportSettings(file, items);

	helper.importSettings(file);
 */

public class SettingsHelper {

	public static final int VERSION = 1;

	public static final String SETTINGS_TYPE_LIST_KEY = "settings_type_list_key";
	public static final String REPLACE_KEY = "replace";
	public static final String SILENT_IMPORT_KEY = "silent_import";
	public static final String SETTINGS_LATEST_CHANGES_KEY = "settings_latest_changes";
	public static final String SETTINGS_VERSION_KEY = "settings_version";

	public static final int BUFFER = 1024;

	protected static final Log LOG = PlatformUtil.getLog(SettingsHelper.class);

	private OsmandApplication app;

	private ImportAsyncTask importTask;
	private Map<File, ExportAsyncTask> exportAsyncTasks = new HashMap<>();

	public interface SettingsImportListener {
		void onSettingsImportFinished(boolean succeed, boolean needRestart, @NonNull List<SettingsItem> items);
	}

	public interface SettingsCollectListener {
		void onSettingsCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items);
	}

	public interface CheckDuplicatesListener {
		void onDuplicatesChecked(@NonNull List<Object> duplicates, List<SettingsItem> items);
	}

	public interface SettingsExportListener {
		void onSettingsExportFinished(@NonNull File file, boolean succeed);

		void onSettingsExportProgressUpdate(int value);
	}

	public enum ImportType {
		COLLECT,
		CHECK_DUPLICATES,
		IMPORT
	}

	public SettingsHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Nullable
	public ImportAsyncTask getImportTask() {
		return importTask;
	}

	@Nullable
	public ImportType getImportTaskType() {
		ImportAsyncTask importTask = this.importTask;
		return importTask != null ? importTask.getImportType() : null;
	}

	public boolean isImportDone() {
		ImportAsyncTask importTask = this.importTask;
		return importTask == null || importTask.isImportDone();
	}

	public boolean cancelExportForFile(File file) {
		ExportAsyncTask exportTask = exportAsyncTasks.get(file);
		if (exportTask != null && (exportTask.getStatus() == AsyncTask.Status.RUNNING)) {
			return exportTask.cancel(true);
		}
		return false;
	}

	public boolean isFileExporting(File file) {
		return exportAsyncTasks.containsKey(file);
	}

	public void updateExportListener(File file, SettingsExportListener listener) {
		ExportAsyncTask exportAsyncTask = exportAsyncTasks.get(file);
		if (exportAsyncTask != null) {
			exportAsyncTask.listener = listener;
		}
	}

	private void finishImport(@Nullable SettingsImportListener listener, boolean success, @NonNull List<SettingsItem> items, boolean needRestart) {
		importTask = null;
		List<String> warnings = new ArrayList<>();
		for (SettingsItem item : items) {
			warnings.addAll(item.getWarnings());
		}
		if (!warnings.isEmpty()) {
			app.showToastMessage(AndroidUtils.formatWarnings(warnings).toString());
		}
		if (listener != null) {
			listener.onSettingsImportFinished(success, needRestart, items);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class ImportItemsAsyncTask extends AsyncTask<Void, Void, Boolean> {

		private final SettingsImporter importer;
		private final File file;
		private final SettingsImportListener listener;
		private final List<SettingsItem> items;
		private final StateChangedListener<String> localeListener;
		private boolean needRestart = false;

		ImportItemsAsyncTask(@NonNull File file,
							 @Nullable SettingsImportListener listener,
							 @NonNull List<SettingsItem> items) {
			importer = new SettingsImporter(app);
			this.file = file;
			this.listener = listener;
			this.items = items;
			localeListener = new StateChangedListener<String>() {
				@Override
				public void stateChanged(String change) {
					needRestart = true;
				}
			};
		}

		@Override
		protected void onPreExecute() {
			app.getSettings().PREFERRED_LOCALE.addListener(localeListener);
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				importer.importItems(file, items);
				return true;
			} catch (IllegalArgumentException e) {
				LOG.error("Failed to import items from: " + file.getName(), e);
			} catch (IOException e) {
				LOG.error("Failed to import items from: " + file.getName(), e);
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			app.getSettings().PREFERRED_LOCALE.removeListener(localeListener);
			finishImport(listener, success, items, needRestart);
		}
	}

	public interface ExportProgressListener {
		void updateProgress(int value);
	}

	@SuppressLint("StaticFieldLeak")
	public class ExportAsyncTask extends AsyncTask<Void, Integer, Boolean> {

		private File file;
		private SettingsExporter exporter;
		private SettingsExportListener listener;

		ExportAsyncTask(@NonNull File settingsFile,
						@Nullable SettingsExportListener listener,
						@NonNull List<SettingsItem> items, boolean exportItemsFiles) {
			this.file = settingsFile;
			this.listener = listener;
			this.exporter = new SettingsExporter(getProgressListener(), exportItemsFiles);
			for (SettingsItem item : items) {
				exporter.addSettingsItem(item);
			}
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			try {
				exporter.exportSettings(file);
				return true;
			} catch (JSONException e) {
				LOG.error("Failed to export items to: " + file.getName(), e);
			} catch (IOException e) {
				LOG.error("Failed to export items to: " + file.getName(), e);
			}
			return false;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			if (listener != null) {
				listener.onSettingsExportProgressUpdate(values[0]);
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {
			exportAsyncTasks.remove(file);
			if (listener != null) {
				listener.onSettingsExportFinished(file, success);
			}
		}

		@Override
		protected void onCancelled() {
			Algorithms.removeAllFiles(file);
		}

		private ExportProgressListener getProgressListener() {
			return new ExportProgressListener() {
				@Override
				public void updateProgress(int value) {
					exporter.setCancelled(isCancelled());
					publishProgress(value);
				}
			};
		}
	}

	@SuppressLint("StaticFieldLeak")
	public class ImportAsyncTask extends AsyncTask<Void, Void, List<SettingsItem>> {

		private File file;
		private String latestChanges;
		private int version;

		private SettingsImportListener importListener;
		private SettingsCollectListener collectListener;
		private CheckDuplicatesListener duplicatesListener;
		private SettingsImporter importer;

		private List<SettingsItem> items = new ArrayList<>();
		private List<SettingsItem> selectedItems = new ArrayList<>();
		private List<Object> duplicates;

		private ImportType importType;
		private boolean importDone;

		ImportAsyncTask(@NonNull File file, String latestChanges, int version, @Nullable SettingsCollectListener collectListener) {
			this.file = file;
			this.collectListener = collectListener;
			this.latestChanges = latestChanges;
			this.version = version;
			importer = new SettingsImporter(app);
			importType = ImportType.COLLECT;
		}

		ImportAsyncTask(@NonNull File file, @NonNull List<SettingsItem> items, String latestChanges, int version, @Nullable SettingsImportListener importListener) {
			this.file = file;
			this.importListener = importListener;
			this.items = items;
			this.latestChanges = latestChanges;
			this.version = version;
			importer = new SettingsImporter(app);
			importType = ImportType.IMPORT;
		}

		ImportAsyncTask(@NonNull File file, @NonNull List<SettingsItem> items, @NonNull List<SettingsItem> selectedItems, @Nullable CheckDuplicatesListener duplicatesListener) {
			this.file = file;
			this.items = items;
			this.duplicatesListener = duplicatesListener;
			this.selectedItems = selectedItems;
			importer = new SettingsImporter(app);
			importType = ImportType.CHECK_DUPLICATES;
		}

		@Override
		protected void onPreExecute() {
			ImportAsyncTask importTask = SettingsHelper.this.importTask;
			if (importTask != null && !importTask.importDone) {
				finishImport(importListener, false, items, false);
			}
			SettingsHelper.this.importTask = this;
		}

		@Override
		protected List<SettingsItem> doInBackground(Void... voids) {
			switch (importType) {
				case COLLECT:
					try {
						return importer.collectItems(file);
					} catch (IllegalArgumentException e) {
						LOG.error("Failed to collect items from: " + file.getName(), e);
					} catch (IOException e) {
						LOG.error("Failed to collect items from: " + file.getName(), e);
					}
					break;
				case CHECK_DUPLICATES:
					this.duplicates = getDuplicatesData(selectedItems);
					return selectedItems;
				case IMPORT:
					return items;
			}
			return null;
		}

		@Override
		protected void onPostExecute(@Nullable List<SettingsItem> items) {
			if (items != null && importType != ImportType.CHECK_DUPLICATES) {
				this.items = items;
			} else {
				selectedItems = items;
			}
			switch (importType) {
				case COLLECT:
					importDone = true;
					collectListener.onSettingsCollectFinished(true, false, this.items);
					break;
				case CHECK_DUPLICATES:
					importDone = true;
					if (duplicatesListener != null) {
						duplicatesListener.onDuplicatesChecked(duplicates, selectedItems);
					}
					break;
				case IMPORT:
					if (items != null && items.size() > 0) {
						for (SettingsItem item : items) {
							item.apply();
						}
						new ImportItemsAsyncTask(file, importListener, items).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					}
					break;
			}
		}

		public List<SettingsItem> getItems() {
			return items;
		}

		public File getFile() {
			return file;
		}

		public void setImportListener(SettingsImportListener importListener) {
			this.importListener = importListener;
		}

		public void setDuplicatesListener(CheckDuplicatesListener duplicatesListener) {
			this.duplicatesListener = duplicatesListener;
		}

		ImportType getImportType() {
			return importType;
		}

		boolean isImportDone() {
			return importDone;
		}

		public List<Object> getDuplicates() {
			return duplicates;
		}

		public List<SettingsItem> getSelectedItems() {
			return selectedItems;
		}

		private List<Object> getDuplicatesData(List<SettingsItem> items) {
			List<Object> duplicateItems = new ArrayList<>();
			for (SettingsItem item : items) {
				if (item instanceof ProfileSettingsItem) {
					if (item.exists()) {
						duplicateItems.add(((ProfileSettingsItem) item).getModeBean());
					}
				} else if (item instanceof CollectionSettingsItem<?>) {
					CollectionSettingsItem settingsItem = (CollectionSettingsItem) item;
					List<?> duplicates = settingsItem.processDuplicateItems();
					if (!duplicates.isEmpty() && settingsItem.shouldShowDuplicates()) {
						duplicateItems.addAll(duplicates);
					}
				} else if (item instanceof FileSettingsItem) {
					if (item.exists()) {
						duplicateItems.add(((FileSettingsItem) item).getFile());
					}
				}
			}
			return duplicateItems;
		}
	}

	public void collectSettings(@NonNull File settingsFile, String latestChanges, int version,
								@Nullable SettingsCollectListener listener) {
		new ImportAsyncTask(settingsFile, latestChanges, version, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void checkDuplicates(@NonNull File file, @NonNull List<SettingsItem> items, @NonNull List<SettingsItem> selectedItems, CheckDuplicatesListener listener) {
		new ImportAsyncTask(file, items, selectedItems, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void importSettings(@NonNull File settingsFile, @NonNull List<SettingsItem> items, String latestChanges, int version, @Nullable SettingsImportListener listener) {
		new ImportAsyncTask(settingsFile, items, latestChanges, version, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@NonNull File fileDir, @NonNull String fileName, @Nullable SettingsExportListener listener, @NonNull List<SettingsItem> items, boolean exportItemsFiles) {
		File file = new File(fileDir, fileName + OSMAND_SETTINGS_FILE_EXT);
		ExportAsyncTask exportAsyncTask = new ExportAsyncTask(file, listener, items, exportItemsFiles);
		exportAsyncTasks.put(file, exportAsyncTask);
		exportAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public void exportSettings(@NonNull File fileDir, @NonNull String fileName, @Nullable SettingsExportListener listener,
							   boolean exportItemsFiles, @NonNull SettingsItem... items) {
		exportSettings(fileDir, fileName, listener, new ArrayList<>(Arrays.asList(items)), exportItemsFiles);
	}

	public List<SettingsItem> getFilteredSettingsItems(List<ExportSettingsType> settingsTypes, boolean addProfiles, boolean export) {
		Map<ExportSettingsType, List<?>> typesMap = new HashMap<>();
		typesMap.putAll(getSettingsItems(addProfiles));
		typesMap.putAll(getMyPlacesItems());
		typesMap.putAll(getResourcesItems());

		return getFilteredSettingsItems(typesMap, settingsTypes, Collections.<SettingsItem>emptyList(), export);
	}

	public List<SettingsItem> getFilteredSettingsItems(
			Map<ExportSettingsType, List<?>> allSettingsMap, List<ExportSettingsType> settingsTypes,
			@NonNull List<SettingsItem> settingsItems, boolean export
	) {
		List<SettingsItem> filteredSettingsItems = new ArrayList<>();
		for (ExportSettingsType settingsType : settingsTypes) {
			List<?> settingsDataObjects = allSettingsMap.get(settingsType);
			if (settingsDataObjects != null) {
				filteredSettingsItems.addAll(prepareSettingsItems(settingsDataObjects, settingsItems, export));
			}
		}
		return filteredSettingsItems;
	}

	public Map<ExportSettingsCategory, SettingsCategoryItems> getSettingsByCategory(boolean addProfiles) {
		Map<ExportSettingsCategory, SettingsCategoryItems> dataList = new LinkedHashMap<>();

		Map<ExportSettingsType, List<?>> settingsItems = getSettingsItems(addProfiles);
		Map<ExportSettingsType, List<?>> myPlacesItems = getMyPlacesItems();
		Map<ExportSettingsType, List<?>> resourcesItems = getResourcesItems();

		if (!settingsItems.isEmpty()) {
			dataList.put(ExportSettingsCategory.SETTINGS, new SettingsCategoryItems(settingsItems));
		}
		if (!myPlacesItems.isEmpty()) {
			dataList.put(ExportSettingsCategory.MY_PLACES, new SettingsCategoryItems(myPlacesItems));
		}
		if (!resourcesItems.isEmpty()) {
			dataList.put(ExportSettingsCategory.RESOURCES, new SettingsCategoryItems(resourcesItems));
		}

		return dataList;
	}

	private Map<ExportSettingsType, List<?>> getSettingsItems(boolean addProfiles) {
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
		if (!actionsList.isEmpty()) {
			settingsItems.put(ExportSettingsType.QUICK_ACTIONS, actionsList);
		}
		List<PoiUIFilter> poiList = app.getPoiFilters().getUserDefinedPoiFilters(false);
		if (!poiList.isEmpty()) {
			settingsItems.put(ExportSettingsType.POI_TYPES, poiList);
		}
		Map<LatLon, AvoidRoadInfo> impassableRoads = app.getAvoidSpecificRoads().getImpassableRoads();
		if (!impassableRoads.isEmpty()) {
			settingsItems.put(ExportSettingsType.AVOID_ROADS, new ArrayList<>(impassableRoads.values()));
		}
		return settingsItems;
	}

	private Map<ExportSettingsType, List<?>> getMyPlacesItems() {
		Map<ExportSettingsType, List<?>> myPlacesItems = new LinkedHashMap<>();

		List<FavoriteGroup> favoriteGroups = app.getFavorites().getFavoriteGroups();
		if (!favoriteGroups.isEmpty()) {
			myPlacesItems.put(ExportSettingsType.FAVORITES, favoriteGroups);
		}
		File gpxDir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		List<GPXInfo> gpxInfoList = GpxUiHelper.getSortedGPXFilesInfo(gpxDir, null, true);
		if (!gpxInfoList.isEmpty()) {
			List<File> files = new ArrayList<>();
			for (GPXInfo gpxInfo : gpxInfoList) {
				File file = new File(gpxInfo.getFileName());
				if (file.exists()) {
					files.add(file);
				}
			}
			if (!files.isEmpty()) {
				myPlacesItems.put(ExportSettingsType.TRACKS, files);
			}
		}
		OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			List<OsmNotesPoint> notesPointList = osmEditingPlugin.getDBBug().getOsmbugsPoints();
			if (!notesPointList.isEmpty()) {
				myPlacesItems.put(ExportSettingsType.OSM_NOTES, notesPointList);
			}
			List<OpenstreetmapPoint> editsPointList = osmEditingPlugin.getDBPOI().getOpenstreetmapPoints();
			if (!editsPointList.isEmpty()) {
				myPlacesItems.put(ExportSettingsType.OSM_EDITS, editsPointList);
			}
		}
		AudioVideoNotesPlugin plugin = OsmandPlugin.getPlugin(AudioVideoNotesPlugin.class);
		if (plugin != null) {
			List<File> files = new ArrayList<>();
			for (Recording rec : plugin.getAllRecordings()) {
				File file = rec.getFile();
				if (file != null && file.exists()) {
					files.add(file);
				}
			}
			if (!files.isEmpty()) {
				myPlacesItems.put(ExportSettingsType.MULTIMEDIA_NOTES, files);
			}
		}
		List<MapMarker> mapMarkers = app.getItineraryHelper().getMapMarkersFromDefaultGroups(false);
		if (!mapMarkers.isEmpty()) {
			String name = app.getString(R.string.map_markers);
			String groupId = ExportSettingsType.ACTIVE_MARKERS.name();
			ItineraryGroup markersGroup = new ItineraryGroup(groupId, name, ItineraryGroup.ANY_TYPE);
			markersGroup.setMarkers(mapMarkers);
			myPlacesItems.put(ExportSettingsType.ACTIVE_MARKERS, Collections.singletonList(markersGroup));
		}
		List<MapMarker> markersHistory = app.getItineraryHelper().getMapMarkersFromDefaultGroups(true);
		if (!markersHistory.isEmpty()) {
			String name = app.getString(R.string.shared_string_history);
			String groupId = ExportSettingsType.HISTORY_MARKERS.name();
			ItineraryGroup markersGroup = new ItineraryGroup(groupId, name, ItineraryGroup.ANY_TYPE);
			markersGroup.setMarkers(markersHistory);
			myPlacesItems.put(ExportSettingsType.HISTORY_MARKERS, Collections.singletonList(markersGroup));
		}
		List<HistoryEntry> historyEntries = SearchHistoryHelper.getInstance(app).getHistoryEntries(false);
		if (!historyEntries.isEmpty()) {
			myPlacesItems.put(ExportSettingsType.SEARCH_HISTORY, historyEntries);
		}
		return myPlacesItems;
	}

	private Map<ExportSettingsType, List<?>> getResourcesItems() {
		Map<ExportSettingsType, List<?>> resourcesItems = new LinkedHashMap<>();

		Map<String, File> externalRenderers = app.getRendererRegistry().getExternalRenderers();
		if (!externalRenderers.isEmpty()) {
			resourcesItems.put(ExportSettingsType.CUSTOM_RENDER_STYLE, new ArrayList<>(externalRenderers.values()));
		}
		File routingProfilesFolder = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
		if (routingProfilesFolder.exists() && routingProfilesFolder.isDirectory()) {
			File[] fl = routingProfilesFolder.listFiles();
			if (fl != null && fl.length > 0) {
				resourcesItems.put(ExportSettingsType.CUSTOM_ROUTING, Arrays.asList(fl));
			}
		}
		List<OnlineRoutingEngine> onlineRoutingEngines = app.getOnlineRoutingHelper().getEngines();
		if (!Algorithms.isEmpty(onlineRoutingEngines)) {
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
		if (!iTileSources.isEmpty()) {
			resourcesItems.put(ExportSettingsType.MAP_SOURCES, iTileSources);
		}
		List<LocalIndexInfo> localIndexInfoList = getLocalIndexData();
		List<File> files = getFilesByType(localIndexInfoList, LocalIndexType.MAP_DATA, LocalIndexType.TILES_DATA,
				LocalIndexType.SRTM_DATA, LocalIndexType.WIKI_DATA);
		if (!files.isEmpty()) {
			sortLocalFiles(files);
			resourcesItems.put(ExportSettingsType.OFFLINE_MAPS, files);
		}
		files = getFilesByType(localIndexInfoList, LocalIndexType.TTS_VOICE_DATA);
		if (!files.isEmpty()) {
			resourcesItems.put(ExportSettingsType.TTS_VOICE, files);
		}
		files = getFilesByType(localIndexInfoList, LocalIndexType.VOICE_DATA);
		if (!files.isEmpty()) {
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
		List<ItineraryGroup> markersGroups = new ArrayList<>();
		List<ItineraryGroup> markersHistoryGroups = new ArrayList<>();
		List<HistoryEntry> historyEntries = new ArrayList<>();
		List<OnlineRoutingEngine> onlineRoutingEngines = new ArrayList<>();

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
			} else if (object instanceof ItineraryGroup) {
				ItineraryGroup markersGroup = (ItineraryGroup) object;
				if (ExportSettingsType.ACTIVE_MARKERS.name().equals(markersGroup.getId())) {
					markersGroups.add((ItineraryGroup) object);
				} else if (ExportSettingsType.HISTORY_MARKERS.name().equals(markersGroup.getId())) {
					markersHistoryGroups.add((ItineraryGroup) object);
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
			for (ItineraryGroup group : markersGroups) {
				mapMarkers.addAll(group.getMarkers());
			}
			MarkersSettingsItem baseItem = getBaseItem(SettingsItemType.ACTIVE_MARKERS, MarkersSettingsItem.class, settingsItems);
			result.add(new MarkersSettingsItem(app, baseItem, mapMarkers));
		}
		if (!markersHistoryGroups.isEmpty()) {
			List<MapMarker> mapMarkers = new ArrayList<>();
			for (ItineraryGroup group : markersHistoryGroups) {
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

	public static Map<ExportSettingsCategory, SettingsCategoryItems> getSettingsToOperateByCategory(List<SettingsItem> items, boolean importComplete) {
		Map<ExportSettingsCategory, SettingsCategoryItems> exportMap = new LinkedHashMap<>();
		Map<ExportSettingsType, List<?>> settingsToOperate = getSettingsToOperate(items, importComplete);

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
		if (!settingsItems.isEmpty()) {
			exportMap.put(ExportSettingsCategory.SETTINGS, new SettingsCategoryItems(settingsItems));
		}
		if (!myPlacesItems.isEmpty()) {
			exportMap.put(ExportSettingsCategory.MY_PLACES, new SettingsCategoryItems(myPlacesItems));
		}
		if (!resourcesItems.isEmpty()) {
			exportMap.put(ExportSettingsCategory.RESOURCES, new SettingsCategoryItems(resourcesItems));
		}

		return exportMap;
	}

	public static Map<ExportSettingsType, List<?>> getSettingsToOperate(List<SettingsItem> settingsItems, boolean importComplete) {
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
		List<ItineraryGroup> markersGroups = new ArrayList<>();
		List<ItineraryGroup> markersHistoryGroups = new ArrayList<>();
		List<HistoryEntry> historyEntries = new ArrayList<>();
		List<OnlineRoutingEngine> onlineRoutingEngines = new ArrayList<>();

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
					markersHistoryGroups.add(historyMarkersSettingsItem.getMarkersGroup());
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
				default:
					break;
			}
		}

		if (!profiles.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.PROFILE, profiles);
		}
		if (!quickActions.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.QUICK_ACTIONS, quickActions);
		}
		if (!poiUIFilters.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.POI_TYPES, poiUIFilters);
		}
		if (!tileSourceTemplates.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.MAP_SOURCES, tileSourceTemplates);
		}
		if (!renderFilesList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.CUSTOM_RENDER_STYLE, renderFilesList);
		}
		if (!routingFilesList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.CUSTOM_ROUTING, routingFilesList);
		}
		if (!avoidRoads.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.AVOID_ROADS, avoidRoads);
		}
		if (!multimediaFilesList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.MULTIMEDIA_NOTES, multimediaFilesList);
		}
		if (!tracksFilesList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.TRACKS, tracksFilesList);
		}
		if (!globalSettingsItems.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.GLOBAL, globalSettingsItems);
		}
		if (!notesPointList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.OSM_NOTES, notesPointList);
		}
		if (!editsPointList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.OSM_EDITS, editsPointList);
		}
		if (!mapFilesList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.OFFLINE_MAPS, mapFilesList);
		}
		if (!favoriteGroups.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.FAVORITES, favoriteGroups);
		}
		if (!ttsVoiceFilesList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.TTS_VOICE, ttsVoiceFilesList);
		}
		if (!voiceFilesList.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.VOICE, voiceFilesList);
		}
		if (!markersGroups.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.ACTIVE_MARKERS, markersGroups);
		}
		if (!markersHistoryGroups.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.HISTORY_MARKERS, markersHistoryGroups);
		}
		if (!historyEntries.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.SEARCH_HISTORY, historyEntries);
		}
		if (!onlineRoutingEngines.isEmpty()) {
			settingsToOperate.put(ExportSettingsType.ONLINE_ROUTING_ENGINES, onlineRoutingEngines);
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