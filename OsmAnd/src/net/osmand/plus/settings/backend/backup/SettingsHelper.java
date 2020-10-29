package net.osmand.plus.settings.backend.backup;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.audionotes.AudioVideoNotesPlugin.Recording;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.helpers.AvoidSpecificRoads.AvoidRoadInfo;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.osmedit.OpenstreetmapPoint;
import net.osmand.plus.osmedit.OsmEditingPlugin;
import net.osmand.plus.osmedit.OsmNotesPoint;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.ApplicationMode.ApplicationModeBean;
import net.osmand.plus.settings.backend.ExportSettingsType;

import org.apache.commons.logging.Log;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;
import static net.osmand.plus.activities.LocalIndexHelper.*;

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
	public static final String SETTINGS_LATEST_CHANGES_KEY = "settings_latest_changes";
	public static final String SETTINGS_VERSION_KEY = "settings_version";

	public static final int BUFFER = 1024;

	protected static final Log LOG = PlatformUtil.getLog(SettingsHelper.class);

	private OsmandApplication app;

	private ImportAsyncTask importTask;
	private Map<File, ExportAsyncTask> exportAsyncTasks = new HashMap<>();

	public interface SettingsImportListener {
		void onSettingsImportFinished(boolean succeed, @NonNull List<SettingsItem> items);
	}

	public interface SettingsCollectListener {
		void onSettingsCollectFinished(boolean succeed, boolean empty, @NonNull List<SettingsItem> items);
	}

	public interface CheckDuplicatesListener {
		void onDuplicatesChecked(@NonNull List<Object> duplicates, List<SettingsItem> items);
	}

	public interface SettingsExportListener {
		void onSettingsExportFinished(@NonNull File file, boolean succeed);
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

	public boolean isFileExporting(File file) {
		return exportAsyncTasks.containsKey(file);
	}

	public void updateExportListener(File file, SettingsExportListener listener) {
		ExportAsyncTask exportAsyncTask = exportAsyncTasks.get(file);
		if (exportAsyncTask != null) {
			exportAsyncTask.listener = listener;
		}
	}

	private void finishImport(@Nullable SettingsImportListener listener, boolean success, @NonNull List<SettingsItem> items) {
		importTask = null;
		List<String> warnings = new ArrayList<>();
		for (SettingsItem item : items) {
			warnings.addAll(item.getWarnings());
		}
		if (!warnings.isEmpty()) {
			app.showToastMessage(AndroidUtils.formatWarnings(warnings).toString());
		}
		if (listener != null) {
			listener.onSettingsImportFinished(success, items);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class ImportItemsAsyncTask extends AsyncTask<Void, Void, Boolean> {

		private SettingsImporter importer;
		private File file;
		private SettingsImportListener listener;
		private List<SettingsItem> items;

		ImportItemsAsyncTask(@NonNull File file,
							 @Nullable SettingsImportListener listener,
							 @NonNull List<SettingsItem> items) {
			importer = new SettingsImporter(app);
			this.file = file;
			this.listener = listener;
			this.items = items;
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
			finishImport(listener, success, items);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class ExportAsyncTask extends AsyncTask<Void, Void, Boolean> {

		private SettingsExporter exporter;
		private File file;
		private SettingsExportListener listener;

		ExportAsyncTask(@NonNull File settingsFile,
						@Nullable SettingsExportListener listener,
						@NonNull List<SettingsItem> items, boolean exportItemsFiles) {
			this.file = settingsFile;
			this.listener = listener;
			this.exporter = new SettingsExporter(exportItemsFiles);
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
		protected void onPostExecute(Boolean success) {
			exportAsyncTasks.remove(file);
			if (listener != null) {
				listener.onSettingsExportFinished(file, success);
			}
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
				finishImport(importListener, false, items);
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
					List<?> duplicates = ((CollectionSettingsItem<?>) item).processDuplicateItems();
					if (!duplicates.isEmpty()) {
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

	public List<SettingsItem> getFilteredSettingsItems(Map<ExportSettingsType, List<?>> additionalData,
													   List<ExportSettingsType> settingsTypes) {
		List<SettingsItem> settingsItems = new ArrayList<>();
		for (ExportSettingsType settingsType : settingsTypes) {
			List<?> settingsDataObjects = additionalData.get(settingsType);
			if (settingsDataObjects != null) {
				for (Object object : settingsDataObjects) {
					if (object instanceof ApplicationModeBean) {
						settingsItems.add(new ProfileSettingsItem(app, null, (ApplicationModeBean) object));
					}
				}
				settingsItems.addAll(prepareAdditionalSettingsItems(new ArrayList<>(settingsDataObjects)));
			}
		}
		return settingsItems;
	}

	public Map<ExportSettingsType, List<?>> getAdditionalData(boolean globalExport) {
		Map<ExportSettingsType, List<?>> dataList = new HashMap<>();

		QuickActionRegistry registry = app.getQuickActionRegistry();
		List<QuickAction> actionsList = registry.getQuickActions();
		if (!actionsList.isEmpty()) {
			dataList.put(ExportSettingsType.QUICK_ACTIONS, actionsList);
		}

		List<PoiUIFilter> poiList = app.getPoiFilters().getUserDefinedPoiFilters(false);
		if (!poiList.isEmpty()) {
			dataList.put(ExportSettingsType.POI_TYPES, poiList);
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
			dataList.put(ExportSettingsType.MAP_SOURCES, iTileSources);
		}

		Map<String, File> externalRenderers = app.getRendererRegistry().getExternalRenderers();
		if (!externalRenderers.isEmpty()) {
			dataList.put(ExportSettingsType.CUSTOM_RENDER_STYLE, new ArrayList<>(externalRenderers.values()));
		}

		File routingProfilesFolder = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
		if (routingProfilesFolder.exists() && routingProfilesFolder.isDirectory()) {
			File[] fl = routingProfilesFolder.listFiles();
			if (fl != null && fl.length > 0) {
				dataList.put(ExportSettingsType.CUSTOM_ROUTING, Arrays.asList(fl));
			}
		}

		Map<LatLon, AvoidRoadInfo> impassableRoads = app.getAvoidSpecificRoads().getImpassableRoads();
		if (!impassableRoads.isEmpty()) {
			dataList.put(ExportSettingsType.AVOID_ROADS, new ArrayList<>(impassableRoads.values()));
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
				dataList.put(ExportSettingsType.MULTIMEDIA_NOTES, files);
			}
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
				dataList.put(ExportSettingsType.TRACKS, files);
			}
		}
		if (globalExport) {
			List<ApplicationModeBean> appModeBeans = new ArrayList<>();
			for (ApplicationMode mode : ApplicationMode.allPossibleValues()) {
				appModeBeans.add(mode.toModeBean());
			}
			dataList.put(ExportSettingsType.PROFILE, appModeBeans);
		}
		OsmEditingPlugin osmEditingPlugin = OsmandPlugin.getPlugin(OsmEditingPlugin.class);
		if (osmEditingPlugin != null) {
			List<OsmNotesPoint> notesPointList = osmEditingPlugin.getDBBug().getOsmbugsPoints();
			if (!notesPointList.isEmpty()) {
				dataList.put(ExportSettingsType.OSM_NOTES, notesPointList);
			}
			List<OpenstreetmapPoint> editsPointList = osmEditingPlugin.getDBPOI().getOpenstreetmapPoints();
			if (!editsPointList.isEmpty()) {
				dataList.put(ExportSettingsType.OSM_EDITS, editsPointList);
			}
		}
		List<File> files = getLocalMapFiles();
		if (!files.isEmpty()) {
			dataList.put(ExportSettingsType.OFFLINE_MAPS, files);
		}
		return dataList;
	}

	private List<File> getLocalMapFiles() {
		List<File> files = new ArrayList<>();
		LocalIndexHelper helper = new LocalIndexHelper(app);
		List<LocalIndexInfo> localMapFileList = helper.getLocalIndexData(new AbstractLoadLocalIndexTask() {
			@Override
			public void loadFile(LocalIndexInfo... loaded) {
			}
		});
		for (LocalIndexInfo map : localMapFileList) {
			File file = new File(map.getPathToData());
			if (file != null && file.exists() && map.getType() != LocalIndexType.TTS_VOICE_DATA) {
				files.add(file);
			}
		}
		return files;
	}

	public List<SettingsItem> prepareAdditionalSettingsItems(List<? super Object> data) {
		List<SettingsItem> settingsItems = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();
		List<ApplicationModeBean> appModeBeans = new ArrayList<>();
		List<OsmNotesPoint> osmNotesPointList = new ArrayList<>();
		List<OpenstreetmapPoint> osmEditsPointList = new ArrayList<>();

		for (Object object : data) {
			if (object instanceof QuickAction) {
				quickActions.add((QuickAction) object);
			} else if (object instanceof PoiUIFilter) {
				poiUIFilters.add((PoiUIFilter) object);
			} else if (object instanceof TileSourceTemplate || object instanceof SQLiteTileSource) {
				tileSourceTemplates.add((ITileSource) object);
			} else if (object instanceof File) {
				try {
					settingsItems.add(new FileSettingsItem(app, (File) object));
				} catch (IllegalArgumentException e) {
					LOG.warn("Trying to export unsuported file type", e);
				}
			} else if (object instanceof AvoidRoadInfo) {
				avoidRoads.add((AvoidRoadInfo) object);
			} else if (object instanceof ApplicationModeBean) {
				appModeBeans.add((ApplicationModeBean) object);
			} else if (object instanceof OsmNotesPoint) {
				osmNotesPointList.add((OsmNotesPoint) object);
			} else if (object instanceof OpenstreetmapPoint) {
				osmEditsPointList.add((OpenstreetmapPoint) object);
			}
		}
		if (!quickActions.isEmpty()) {
			settingsItems.add(new QuickActionsSettingsItem(app, quickActions));
		}
		if (!poiUIFilters.isEmpty()) {
			settingsItems.add(new PoiUiFiltersSettingsItem(app, poiUIFilters));
		}
		if (!tileSourceTemplates.isEmpty()) {
			settingsItems.add(new MapSourcesSettingsItem(app, tileSourceTemplates));
		}
		if (!avoidRoads.isEmpty()) {
			settingsItems.add(new AvoidRoadsSettingsItem(app, avoidRoads));
		}
		if (!appModeBeans.isEmpty()) {
			for (ApplicationModeBean modeBean : appModeBeans) {
				ApplicationMode mode = ApplicationMode.valueOfStringKey(modeBean.stringKey, null);
				if (mode != null) {
					settingsItems.add(new ProfileSettingsItem(app, mode));
				}
			}
		}
		if (!osmNotesPointList.isEmpty()) {
			settingsItems.add(new OsmNotesSettingsItem(app, osmNotesPointList));
		}
		if (!osmEditsPointList.isEmpty()) {
			settingsItems.add(new OsmEditsSettingsItem(app, osmEditsPointList));
		}
		return settingsItems;
	}

	public static Map<ExportSettingsType, List<?>> getSettingsToOperate(List<SettingsItem> settingsItems, boolean importComplete) {
		Map<ExportSettingsType, List<?>> settingsToOperate = new HashMap<>();
		List<ApplicationModeBean> profiles = new ArrayList<>();
		List<QuickAction> quickActions = new ArrayList<>();
		List<PoiUIFilter> poiUIFilters = new ArrayList<>();
		List<ITileSource> tileSourceTemplates = new ArrayList<>();
		List<File> routingFilesList = new ArrayList<>();
		List<File> renderFilesList = new ArrayList<>();
		List<File> multimediaFilesList = new ArrayList<>();
		List<File> tracksFilesList = new ArrayList<>();
		List<FileSettingsItem> mapFilesList = new ArrayList<>();
		List<AvoidRoadInfo> avoidRoads = new ArrayList<>();
		List<GlobalSettingsItem> globalSettingsItems = new ArrayList<>();
		List<OsmNotesPoint> notesPointList = new ArrayList<>();
		List<OpenstreetmapPoint> editsPointList = new ArrayList<>();
		for (SettingsItem item : settingsItems) {
			switch (item.getType()) {
				case PROFILE:
					profiles.add(((ProfileSettingsItem) item).getModeBean());
					break;
				case FILE:
					FileSettingsItem fileItem = (FileSettingsItem) item;
					if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.RENDERING_STYLE) {
						renderFilesList.add(fileItem.getFile());
					} else if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.ROUTING_CONFIG) {
						routingFilesList.add(fileItem.getFile());
					} else if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.MULTIMEDIA_NOTES) {
						multimediaFilesList.add(fileItem.getFile());
					} else if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.GPX) {
						tracksFilesList.add(fileItem.getFile());
					} else if (fileItem.getSubtype() == FileSettingsItem.FileSubtype.OBF_MAP
							|| fileItem.getSubtype() == FileSettingsItem.FileSubtype.WIKI_MAP
							|| fileItem.getSubtype() == FileSettingsItem.FileSubtype.SRTM_MAP
							|| fileItem.getSubtype() == FileSettingsItem.FileSubtype.TILES_MAP) {
						mapFilesList.add(fileItem);
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
		return settingsToOperate;
	}
}