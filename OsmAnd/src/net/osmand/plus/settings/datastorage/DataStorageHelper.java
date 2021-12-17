package net.osmand.plus.settings.datastorage;

import android.os.Environment;

import net.osmand.IndexConstants;
import net.osmand.data.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.item.DirectoryItem;
import net.osmand.plus.settings.datastorage.item.DirectoryItem.CheckingType;
import net.osmand.plus.settings.datastorage.item.MemoryItem;
import net.osmand.plus.settings.datastorage.item.StorageItem;
import net.osmand.plus.settings.datastorage.task.RefreshUsedMemoryTask;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import static net.osmand.IndexConstants.AV_INDEX_DIR;
import static net.osmand.IndexConstants.BACKUP_INDEX_DIR;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.MAPS_PATH;
import static net.osmand.IndexConstants.ROADS_INDEX_DIR;
import static net.osmand.IndexConstants.SRTM_INDEX_DIR;
import static net.osmand.IndexConstants.TILES_INDEX_DIR;
import static net.osmand.IndexConstants.WIKIVOYAGE_INDEX_DIR;
import static net.osmand.IndexConstants.WIKI_INDEX_DIR;
import static net.osmand.plus.settings.datastorage.item.DirectoryItem.CheckingType.EXTENSIONS;
import static net.osmand.plus.settings.datastorage.item.DirectoryItem.CheckingType.PREFIX;

public class DataStorageHelper {
	public final static String INTERNAL_STORAGE = "internal_storage";
	public final static String EXTERNAL_STORAGE = "external_storage";
	public final static String SHARED_STORAGE = "shared_storage";
	public final static String MULTIUSER_STORAGE = "multiuser_storage";
	public final static String MANUALLY_SPECIFIED = "manually_specified";

	public final static String MAPS_MEMORY = "maps_memory_used";
	public final static String TRAVEL_MEMORY = "travel_memory_used";
	public final static String TERRAIN_MEMORY = "terrain_memory_used";
	public final static String TRACKS_MEMORY = "tracks_memory_used";
	public final static String NOTES_MEMORY = "notes_memory_used";
	public final static String TILES_MEMORY = "tiles_memory_used";
	public final static String OTHER_MEMORY = "other_memory_used";

	private OsmandApplication app;
	private ArrayList<StorageItem> storageItems = new ArrayList<>();
	private StorageItem currentDataStorage;
	private StorageItem manuallySpecified;

	private ArrayList<MemoryItem> memoryItems = new ArrayList<>();
	private MemoryItem mapsMemory;
	private MemoryItem travelMemory;
	private MemoryItem terrainMemory;
	private MemoryItem tracksMemory;
	private MemoryItem notesMemory;
	private MemoryItem tilesMemory;
	private MemoryItem otherMemory;

	private int currentStorageType;
	private String currentStoragePath;

	public DataStorageHelper(@NonNull OsmandApplication app) {
		this.app = app;
		prepareData();
	}

	private void prepareData() {
		initStorageItems();
		initUsedMemoryItems();
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

	private void initUsedMemoryItems() {
		mapsMemory = MemoryItem.builder()
				.setKey(MAPS_MEMORY)
				.setExtensions(IndexConstants.BINARY_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(MAPS_PATH, false, EXTENSIONS, true),
						createDirectory(ROADS_INDEX_DIR, true, EXTENSIONS, true),
						createDirectory(WIKI_INDEX_DIR, true, EXTENSIONS, true),
						createDirectory(BACKUP_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(mapsMemory);

		travelMemory = MemoryItem.builder()
				.setKey(TRAVEL_MEMORY)
				.setExtensions(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(WIKIVOYAGE_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(travelMemory);

		terrainMemory = MemoryItem.builder()
				.setKey(TERRAIN_MEMORY)
				.setExtensions(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT, IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT)
				.setDirectories(
						createDirectory(SRTM_INDEX_DIR, true, EXTENSIONS, true),
						createDirectory(TILES_INDEX_DIR, false, PREFIX, false))
				.setPrefixes("Hillshade", "Slope")
				.createItem();
		memoryItems.add(terrainMemory);

		tracksMemory = MemoryItem.builder()
				.setKey(TRACKS_MEMORY)
//				.setExtensions(IndexConstants.GPX_FILE_EXT, ".gpx.bz2")
				.setDirectories(
						createDirectory(GPX_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(tracksMemory);

		notesMemory = MemoryItem.builder()
				.setKey(NOTES_MEMORY)
//				.setExtensions("")
				.setDirectories(
						createDirectory(AV_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(notesMemory);

		tilesMemory = MemoryItem.builder()
				.setKey(TILES_MEMORY)
//				.setExtensions("")
				.setDirectories(
						createDirectory(TILES_INDEX_DIR, true, EXTENSIONS, true))
				.createItem();
		memoryItems.add(tilesMemory);

		otherMemory = MemoryItem.builder()
				.setKey(OTHER_MEMORY)
				.createItem();
		memoryItems.add(otherMemory);
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
		RefreshUsedMemoryTask task = new RefreshUsedMemoryTask(uiAdapter, otherMemory, rootDir, null, null, OTHER_MEMORY);
		task.execute(mapsMemory, travelMemory, terrainMemory, tracksMemory, notesMemory);
		return task;
	}

	public RefreshUsedMemoryTask calculateTilesMemoryUsed(UpdateMemoryInfoUIAdapter listener) {
		File rootDir = new File(tilesMemory.getDirectories()[0].getAbsolutePath());
		RefreshUsedMemoryTask task = new RefreshUsedMemoryTask(listener, otherMemory, rootDir, null, terrainMemory.getPrefixes(), TILES_MEMORY);
		task.execute(tilesMemory);
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

	public interface UpdateMemoryInfoUIAdapter {

		void onMemoryInfoUpdate();

		void onFinishUpdating(String tag);

	}
}