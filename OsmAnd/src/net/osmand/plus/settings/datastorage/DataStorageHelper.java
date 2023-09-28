package net.osmand.plus.settings.datastorage;

import static net.osmand.IndexConstants.AV_INDEX_DIR;
import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.IndexConstants.GEOTIFF_DIR;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.HEIGHTMAP_INDEX_DIR;
import static net.osmand.IndexConstants.LIVE_INDEX_DIR;
import static net.osmand.IndexConstants.MAPS_PATH;
import static net.osmand.IndexConstants.NAUTICAL_INDEX_DIR;
import static net.osmand.IndexConstants.ROADS_INDEX_DIR;
import static net.osmand.IndexConstants.SRTM_INDEX_DIR;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;
import static net.osmand.IndexConstants.WEATHER_FORECAST_DIR;
import static net.osmand.IndexConstants.WEATHER_INDEX_DIR;
import static net.osmand.IndexConstants.WIKI_INDEX_DIR;
import static net.osmand.IndexConstants.WIKIVOYAGE_INDEX_DIR;
import static net.osmand.plus.settings.datastorage.item.DirectoryItem.CheckingType.EXTENSIONS;
import static net.osmand.plus.settings.datastorage.item.DirectoryItem.CheckingType.PREFIX;

import android.app.ProgressDialog;
import android.os.Environment;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.data.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.resources.ResourceManager.ReloadIndexesListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.item.DirectoryItem;
import net.osmand.plus.settings.datastorage.item.DirectoryItem.CheckingType;
import net.osmand.plus.settings.datastorage.item.MemoryItem;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.datastorage.task.RefreshUsedMemoryTask;
import net.osmand.plus.utils.FileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DataStorageHelper {
	public static final String INTERNAL_STORAGE = "internal_storage";
	public static final String EXTERNAL_STORAGE = "external_storage";
	public static final String SHARED_STORAGE = "shared_storage";
	public static final String MULTIUSER_STORAGE = "multiuser_storage";
	public static final String MANUALLY_SPECIFIED = "manually_specified";

	private static final String MAPS_STORAGE_SIZE = "maps_storage_size";
	public static final String TILES_STORAGE_SIZE = "tiles_storage_size";
	private static final String CONTOUR_LINES_STORAGE_SIZE = "contour_lines_storage_size";
	private static final String HILLSHADE_SLOPE_STORAGE_SIZE = "hillshade_slope_storage_size";
	private static final String TERRAIN3D_STORAGE_SIZE = "terrain3d_storage_size";
	private static final String ROADSONLY_STORAGE_SIZE = "roadsonly_storage_size";
	private static final String NAUTICAL_STORAGE_SIZE = "nautical_storage_size";
	private static final String LIVE_STORAGE_SIZE = "live_storage_size";
	private static final String WIKI_STORAGE_SIZE = "wiki_storage_size";
	private static final String TRAVEL_STORAGE_SIZE = "travel_storage_size";
	private static final String WEATHER_STORAGE_SIZE = "weather_storage_size";
	private static final String TRACKS_STORAGE_SIZE = "tracks_storage_size";
	private static final String NOTES_STORAGE_SIZE = "notes_storage_size";
	private static final String ARCHIVE_STORAGE_SIZE = "archive_storage_size";
	public static final String OTHER_STORAGE_SIZE = "other_storage_size";

	private final OsmandApplication app;
	private final ArrayList<StorageItem> storageItems = new ArrayList<>();
	private StorageItem currentDataStorage;
	private StorageItem manuallySpecified;

	private final ArrayList<MemoryItem> memoryItems = new ArrayList<>();
	private MemoryItem mapsStorageSize;
	private MemoryItem tilesStorageSize;
	private MemoryItem contourLinesStorageSize;
	private MemoryItem hillshadeSlopeStorageSize;
	private MemoryItem terrain3dStorageSize;
	private MemoryItem roadsonlyStorageSize;
	private MemoryItem nauticalStorageSize;
	private MemoryItem liveStorageSize;
	private MemoryItem wikiStorageSize;
	private MemoryItem travelStorageSize;
	private MemoryItem weatherStorageSize;
	private MemoryItem tracksStorageSize;
	private MemoryItem notesStorageSize;
	private MemoryItem archiveStorageSize;
	private MemoryItem otherStorageSize;

	private int currentStorageType;
	private String currentStoragePath;

	public DataStorageHelper(@NonNull OsmandApplication app) {
		this.app = app;
		prepareData();
	}

	private void prepareData() {
		initStorageItems();
		initStorageSizeItems();
	}

	private void initStorageItems() {
		OsmandSettings settings = app.getSettings();
		if (settings.getExternalStorageDirectoryTypeV19() >= 0) {
			currentStorageType = settings.getExternalStorageDirectoryTypeV19();
		} else {
			ValueHolder<Integer> vh = new ValueHolder<>();
			if (vh.value != null && vh.value >= 0) {
				currentStorageType = vh.value;
			} else {
				currentStorageType = 0;
			}
		}
		currentStoragePath = settings.getExternalStorageDirectory().getAbsolutePath();

		//internal storage
		StorageItem internalStorageItem = StorageItem.builder()
				.setKey(INTERNAL_STORAGE)
				.setTitle(app.getString(R.string.storage_directory_internal_app))
				.setDirectory(settings.getInternalAppPath().getAbsolutePath())
				.setDescription(app.getString(R.string.internal_app_storage_description))
				.setType(OsmandSettings.EXTERNAL_STORAGE_TYPE_INTERNAL_FILE)
				.setIconResIds(R.drawable.ic_action_phone, R.drawable.ic_action_phone_filled)
				.createItem();
		addItem(internalStorageItem);

		//shared storage
		StorageItem sharedStorageItem = StorageItem.builder()
				.setKey(SHARED_STORAGE)
				.setTitle(app.getString(R.string.storage_directory_shared))
				.setDirectory(settings.getDefaultInternalStorage().getAbsolutePath())
				.setDescription(app.getString(R.string.shared_app_storage_description))
				.setType(OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT)
				.setIconResId(R.drawable.ic_action_device_alert)
				.createItem();
		addItem(sharedStorageItem);

		//external storage
		File[] externals = app.getExternalFilesDirs(null);
		if (externals != null) {
			listExternalOrObbStorages(externals, EXTERNAL_STORAGE, R.string.storage_directory_external,
					OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE);
		}

		//multi user storage
		File[] obbDirs = app.getObbDirs();
		if (obbDirs != null) {
			listExternalOrObbStorages(obbDirs, MULTIUSER_STORAGE, R.string.storage_directory_multiuser,
					OsmandSettings.EXTERNAL_STORAGE_TYPE_OBB);
		}

		//manually specified storage
		manuallySpecified = StorageItem.builder()
				.setKey(MANUALLY_SPECIFIED)
				.setTitle(app.getString(R.string.storage_directory_manual))
				.setDirectory(currentStoragePath)
				.setType(OsmandSettings.EXTERNAL_STORAGE_TYPE_SPECIFIED)
				.setIconResId(R.drawable.ic_action_folder)
				.createItem();
		storageItems.add(manuallySpecified);

		if (currentDataStorage == null) {
			currentDataStorage = manuallySpecified;
		}
	}

	private void listExternalOrObbStorages(File[] dirs, String keyPrefix, int titleRes, int storageType) {
		int visibleIndex = 0;
		for (File dir : dirs) {
			if (dir != null) {
				visibleIndex++;
				int notSelectedIcon = getIconResForExternalStorage(dir, false);
				int selectedIcon = getIconResForExternalStorage(dir, true);
				StorageItem item = StorageItem.builder()
						.setKey(keyPrefix + visibleIndex)
						.setTitle(app.getString(titleRes) + " " + visibleIndex)
						.setDirectory(dir.getAbsolutePath())
						.setType(storageType)
						.setIconResIds(notSelectedIcon, selectedIcon)
						.createItem();
				addItem(item);
			}
		}
	}

	@DrawableRes
	private int getIconResForExternalStorage(File dir, boolean selected) {
		return Environment.isExternalStorageRemovable(dir)
				? selected ? R.drawable.ic_actiond_sdcard_filled : R.drawable.ic_sdcard
				: R.drawable.ic_action_folder;
	}

	private void initStorageSizeItems() {
		mapsStorageSize = MemoryItem.builder()
				.setKey(MAPS_STORAGE_SIZE)
				.setExtensions(IndexConstants.BINARY_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(MAPS_PATH, false, EXTENSIONS, true))
				.createItem();
		memoryItems.add(mapsStorageSize);

		tilesStorageSize = MemoryItem.builder()
				.setKey(TILES_STORAGE_SIZE)
//				.setExtensions("")
				.setDirectories(
						createDirectory(TILES_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(tilesStorageSize);

		contourLinesStorageSize = MemoryItem.builder()
				.setKey(CONTOUR_LINES_STORAGE_SIZE)
//				.setExtensions(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT, IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(SRTM_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(contourLinesStorageSize);

		hillshadeSlopeStorageSize = MemoryItem.builder()
				.setKey(HILLSHADE_SLOPE_STORAGE_SIZE)
				.setDirectories(
						createDirectory(TILES_INDEX_DIR, false, PREFIX, false))
				.setPrefixes("Hillshade", "Slope")
				.createItem();
		memoryItems.add(hillshadeSlopeStorageSize);

		terrain3dStorageSize = MemoryItem.builder()
				.setKey(TERRAIN3D_STORAGE_SIZE)
//				.setExtensions(IndexConstants.TIF_EXT)
				.setDirectories(
						createDirectory(HEIGHTMAP_INDEX_DIR, true, EXTENSIONS, true),
						createDirectory(GEOTIFF_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(terrain3dStorageSize);

		roadsonlyStorageSize = MemoryItem.builder()
				.setKey(ROADSONLY_STORAGE_SIZE)
//				.setExtensions(IndexConstants.BINARY_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(ROADS_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(roadsonlyStorageSize);

		nauticalStorageSize = MemoryItem.builder()
				.setKey(NAUTICAL_STORAGE_SIZE)
//				.setExtensions(IndexConstants.BINARY_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(NAUTICAL_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(nauticalStorageSize);

		liveStorageSize = MemoryItem.builder()
				.setKey(LIVE_STORAGE_SIZE)
//				.setExtensions(IndexConstants.BINARY_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(LIVE_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(liveStorageSize);

		archiveStorageSize = MemoryItem.builder()
				.setKey(ARCHIVE_STORAGE_SIZE)
//				.setExtensions(IndexConstants.BINARY_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(BACKUP_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(archiveStorageSize);

		wikiStorageSize = MemoryItem.builder()
				.setKey(WIKI_STORAGE_SIZE)
//				.setExtensions(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(WIKI_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(wikiStorageSize);

		travelStorageSize = MemoryItem.builder()
				.setKey(TRAVEL_STORAGE_SIZE)
//				.setExtensions(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(WIKIVOYAGE_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(travelStorageSize);

		weatherStorageSize = MemoryItem.builder()
				.setKey(WEATHER_STORAGE_SIZE)
//				.setExtensions(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(WEATHER_FORECAST_DIR, true, EXTENSIONS, true),
						createDirectory(WEATHER_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(weatherStorageSize);

		tracksStorageSize = MemoryItem.builder()
				.setKey(TRACKS_STORAGE_SIZE)
//				.setExtensions(IndexConstants.GPX_FILE_EXT, ".gpx.bz2")
				.setDirectories(
						createDirectory(GPX_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(tracksStorageSize);

		notesStorageSize = MemoryItem.builder()
				.setKey(NOTES_STORAGE_SIZE)
//				.setExtensions("")
				.setDirectories(
						createDirectory(AV_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(notesStorageSize);

		otherStorageSize = MemoryItem.builder()
				.setKey(OTHER_STORAGE_SIZE)
				.createItem();
		memoryItems.add(otherStorageSize);
	}

	public ArrayList<StorageItem> getStorageItems() {
		return storageItems;
	}

	public StorageItem getCurrentStorage() {
		return currentDataStorage;
	}

	private void addItem(StorageItem item) {
		if (currentStorageType == item.getType() && currentStoragePath.equals(item.getDirectory())) {
			currentDataStorage = item;
		}
		storageItems.add(item);
	}

	public StorageItem getManuallySpecified() {
		return manuallySpecified;
	}

	public StorageItem getStorage(String key) {
		if (storageItems != null && key != null) {
			for (StorageItem storageItem : storageItems) {
				if (key.equals(storageItem.getKey())) {
					return storageItem;
				}
			}
		}
		return null;
	}

	public ArrayList<MemoryItem> getMemoryInfoItems() {
		return memoryItems;
	}

	public RefreshUsedMemoryTask calculateMemoryUsedInfo(UpdateMemoryInfoUIAdapter uiAdapter) {
		File rootDir = new File(currentStoragePath);
		RefreshUsedMemoryTask task = new RefreshUsedMemoryTask(uiAdapter, otherStorageSize, rootDir, null, null, OTHER_STORAGE_SIZE);
		task.execute(mapsStorageSize, contourLinesStorageSize, hillshadeSlopeStorageSize, terrain3dStorageSize, tilesStorageSize, roadsonlyStorageSize, nauticalStorageSize, liveStorageSize, 
				wikiStorageSize, travelStorageSize, weatherStorageSize, tracksStorageSize, notesStorageSize, archiveStorageSize, otherStorageSize);
		return task;
	}

	public RefreshUsedMemoryTask calculateTilesMemoryUsed(UpdateMemoryInfoUIAdapter listener) {
		File rootDir = new File(tilesStorageSize.getDirectories()[0].getAbsolutePath());
		RefreshUsedMemoryTask task = new RefreshUsedMemoryTask(listener, otherStorageSize, rootDir, null, hillshadeSlopeStorageSize.getPrefixes(), TILES_STORAGE_SIZE);
		task.execute(tilesStorageSize);
		return task;
	}

	public long getTotalUsedBytes() {
		long total = 0;
		if (memoryItems != null && memoryItems.size() > 0) {
			for (MemoryItem mi : memoryItems) {
				total += mi.getUsedMemoryBytes();
			}
			return total;
		}
		return -1;
	}

	public DirectoryItem createDirectory(@NonNull String relativePath,
	                                     boolean processInternalDirectories,
	                                     CheckingType checkingType,
	                                     boolean addUnmatchedToOtherMemory) {
		String path = app.getAppPath(relativePath).getAbsolutePath();
		return new DirectoryItem(path, processInternalDirectories, checkingType, addUnmatchedToOtherMemory);
	}

	public static String getFormattedMemoryInfo(long bytes, String[] formatStrings) {
		int type = 0;
		double memory = (double) bytes / 1024;
		while (memory > 1024 && type < formatStrings.length) {
			++type;
			memory = memory / 1024;
		}
		String formattedUsed = new DecimalFormat("#.##").format(memory);
		return String.format(formatStrings[type], formattedUsed);
	}

	public static boolean isCurrentStorageShared(@NonNull OsmandApplication app) {
		DataStorageHelper dataStorageHelper = new DataStorageHelper(app);
		return SHARED_STORAGE.equals(dataStorageHelper.getCurrentStorage().getKey());
	}

	public static boolean saveFilesLocation(@NonNull OsmandApplication app, @NonNull FragmentActivity activity, int type, @NonNull File selectedFile) {
		boolean writable = FileUtils.isWritable(selectedFile);
		if (writable) {
			app.setExternalStorageDirectory(type, selectedFile.getAbsolutePath());
			reloadData(app, activity);
		} else {
			app.showToastMessage(R.string.specified_directiory_not_writeable);
		}
		return writable;
	}

	public static void checkAssetsAsync(@NonNull OsmandApplication app) {
		app.getResourceManager().checkAssetsAsync(IProgress.EMPTY_PROGRESS, true, false, null);
	}

	public static void updateDownloadIndexes(@NonNull OsmandApplication app) {
		app.getDownloadThread().runReloadIndexFilesSilent();
	}

	public static void reloadData(@NonNull OsmandApplication app, @NonNull FragmentActivity activity) {
		WeakReference<FragmentActivity> activityRef = new WeakReference<>(activity);
		app.getResourceManager().reloadIndexesAsync(IProgress.EMPTY_PROGRESS, new ReloadIndexesListener() {

			private ProgressImplementation progress;

			@Override
			public void reloadIndexesStarted() {
				FragmentActivity activity = activityRef.get();
				if (activity != null) {
					progress = ProgressImplementation.createProgressDialog(activity, app.getString(R.string.loading_data),
							app.getString(R.string.loading_data), ProgressDialog.STYLE_HORIZONTAL);
				}
			}

			@Override
			public void reloadIndexesFinished(List<String> warnings) {
				try {
					if (progress != null && progress.getDialog().isShowing()) {
						progress.getDialog().dismiss();
					}
				} catch (Exception e) {
					//ignored
				}
			}
		});
	}

	public interface UpdateMemoryInfoUIAdapter {

		void onMemoryInfoUpdate();

		void onFinishUpdating(String tag);

	}
}
