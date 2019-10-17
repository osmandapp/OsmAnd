package net.osmand.plus.settings;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import net.osmand.IndexConstants;
import net.osmand.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import java.io.File;
import java.util.ArrayList;

public class DataStorageItemsHolder implements Parcelable {
	public final static String INTERNAL_STORAGE = "internal_storage";
	public final static String EXTERNAL_STORAGE = "external_storage";
	public final static String SHARED_STORAGE = "shared_storage";
	public final static String MULTIUSER_STORAGE = "multiuser_storage";
	public final static String MANUALLY_SPECIFIED = "manually_specified";

	public final static String MAPS_MEMORY = "maps_memory_used";
	public final static String TRACKS_MEMORY = "tracks_memory_used";
	public final static String NOTES_MEMORY = "notes_memory_used";
	public final static String TILES_MEMORY = "tiles_memory_used";
	public final static String OTHER_MEMORY = "other_memory_used";

	private ArrayList<DataStorageMenuItem> menuItems = new ArrayList<>();
	private DataStorageMenuItem currentDataStorage;
	private DataStorageMenuItem manuallySpecified;

	private ArrayList<DataStorageMemoryItem> memoryItems = new ArrayList<>();
	private DataStorageMemoryItem mapsMemory;
	private DataStorageMemoryItem tracksMemory;
	private DataStorageMemoryItem notesMemory;
	private DataStorageMemoryItem tilesMemory;
	private DataStorageMemoryItem otherMemory;

	private int currentStorageType;
	private String currentStoragePath;

	private DataStorageItemsHolder(OsmandApplication app) {
		prepareData(app);
	}

	public static DataStorageItemsHolder refreshInfo(OsmandApplication app) {
		return new DataStorageItemsHolder(app);
	}

	private void prepareData(OsmandApplication app) {

		if (app == null) {
			return;
		}

		OsmandSettings settings = app.getSettings();

		if (settings.getExternalStorageDirectoryTypeV19() >= 0) {
			currentStorageType = settings.getExternalStorageDirectoryTypeV19();
		} else {
			ValueHolder<Integer> vh = new ValueHolder<Integer>();
			if (vh.value != null && vh.value >= 0) {
				currentStorageType = vh.value;
			} else {
				currentStorageType = 0;
			}
		}
		currentStoragePath = settings.getExternalStorageDirectory().getAbsolutePath();

		String path;
		File dir;
		int iconId;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

			//internal storage
			path = settings.getInternalAppPath().getAbsolutePath();
			dir = new File(path);
			iconId = R.drawable.ic_action_phone;

			DataStorageMenuItem internalStorageItem = DataStorageMenuItem.builder()
					.setKey(INTERNAL_STORAGE)
					.setTitle(app.getString(R.string.storage_directory_internal_app))
					.setDirectory(path)
					.setDescription(app.getString(R.string.internal_app_storage_description))
					.setType(OsmandSettings.EXTERNAL_STORAGE_TYPE_INTERNAL_FILE)
					.setIconResId(iconId)
					.createItem();
			addItem(internalStorageItem);

			//shared_storage
			dir = settings.getDefaultInternalStorage();
			path = dir.getAbsolutePath();
			iconId = R.drawable.ic_action_phone;

			DataStorageMenuItem sharedStorageItem = DataStorageMenuItem.builder()
					.setKey(SHARED_STORAGE)
					.setTitle(app.getString(R.string.storage_directory_shared))
					.setDirectory(path)
					.setType(OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT)
					.setIconResId(iconId)
					.createItem();
			addItem(sharedStorageItem);

			//external storage
			File[] externals = app.getExternalFilesDirs(null);
			if (externals != null) {
				int i = 0;
				for (File external : externals) {
					if (external != null) {
						++i;
						dir = external;
						path = dir.getAbsolutePath();
						iconId = getIconForStorageType(dir);
						DataStorageMenuItem externalStorageItem = DataStorageMenuItem.builder()
								.setKey(EXTERNAL_STORAGE + i)
								.setTitle(app.getString(R.string.storage_directory_external) + " " + i)
								.setDirectory(path)
								.setType(OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE)
								.setIconResId(iconId)
								.createItem();
						addItem(externalStorageItem);
					}
				}
			}

			//multi user storage
			File[] obbDirs = app.getObbDirs();
			if (obbDirs != null) {
				int i = 0;
				for (File obb : obbDirs) {
					if (obb != null) {
						++i;
						dir = obb;
						path = dir.getAbsolutePath();
						iconId = getIconForStorageType(dir);
						DataStorageMenuItem multiuserStorageItem = DataStorageMenuItem.builder()
								.setKey(MULTIUSER_STORAGE + i)
								.setTitle(app.getString(R.string.storage_directory_multiuser) + " " + i)
								.setDirectory(path)
								.setType(OsmandSettings.EXTERNAL_STORAGE_TYPE_OBB)
								.setIconResId(iconId)
								.createItem();
						addItem(multiuserStorageItem);
					}
				}
			}
		}

		//manually specified storage
		manuallySpecified = DataStorageMenuItem.builder()
				.setKey(MANUALLY_SPECIFIED)
				.setTitle(app.getString(R.string.storage_directory_manual))
				.setDirectory(currentStoragePath)
				.setType(OsmandSettings.EXTERNAL_STORAGE_TYPE_SPECIFIED)
				.setIconResId(R.drawable.ic_action_folder)
				.createItem();
		menuItems.add(manuallySpecified);

		if (currentDataStorage == null) {
			currentDataStorage = manuallySpecified;
		}

		initMemoryUsed(app);
	}

	private void initMemoryUsed(OsmandApplication app) {
		mapsMemory = DataStorageMemoryItem.builder()
				.setKey(MAPS_MEMORY)
				.setExtensions(IndexConstants.BINARY_MAP_INDEX_EXT, IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)
				.setDirectories(
						app.getAppPath(IndexConstants.MAPS_PATH).getAbsolutePath(),
						app.getAppPath(IndexConstants.ROADS_INDEX_DIR).getAbsolutePath(),
						app.getAppPath(IndexConstants.SRTM_INDEX_DIR).getAbsolutePath(),
						app.getAppPath(IndexConstants.WIKI_INDEX_DIR).getAbsolutePath(),
						app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).getAbsolutePath(),
						app.getAppPath(IndexConstants.BACKUP_INDEX_DIR).getAbsolutePath())
				.createItem();
		memoryItems.add(mapsMemory);

		tracksMemory = DataStorageMemoryItem.builder()
				.setKey(TRACKS_MEMORY)
//				.setExtensions(".gpx", ".gpx.bz2")
				.setDirectories(app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath())
				.createItem();
		memoryItems.add(tracksMemory);

		notesMemory = DataStorageMemoryItem.builder()
				.setKey(NOTES_MEMORY)
//				.setExtensions("")
				.setDirectories(app.getAppPath(IndexConstants.AV_INDEX_DIR).getAbsolutePath())
				.createItem();
		memoryItems.add(notesMemory);

		tilesMemory = DataStorageMemoryItem.builder()
				.setKey(TILES_MEMORY)
//				.setExtensions("")
				.setDirectories(app.getAppPath(IndexConstants.TILES_INDEX_DIR).getAbsolutePath())
				.createItem();
		memoryItems.add(tilesMemory);

		otherMemory = DataStorageMemoryItem.builder()
				.setKey(OTHER_MEMORY)
				.createItem();
		memoryItems.add(otherMemory);
	}

	public ArrayList<DataStorageMenuItem> getStorageItems() {
		return menuItems;
	}

	private int getIconForStorageType(File dir) {
		return R.drawable.ic_action_folder;
	}

	public DataStorageMenuItem getCurrentStorage() {
		return currentDataStorage;
	}

	private void addItem(DataStorageMenuItem item) {
		if (currentStorageType == item.getType() && currentStoragePath.equals(item.getDirectory())) {
			currentDataStorage = item;
		}
		menuItems.add(item);
	}

	public DataStorageMenuItem getManuallySpecified() {
		return manuallySpecified;
	}

	public DataStorageMenuItem getStorage(String key) {
		if (menuItems != null && key != null) {
			for (DataStorageMenuItem menuItem : menuItems) {
				if (key.equals(menuItem.getKey())) {
					return menuItem;
				}
			}
		}
		return null;
	}

	public int getCurrentType() {
		return currentStorageType;
	}

	public String getCurrentPath() {
		return currentStoragePath;
	}

	public ArrayList<DataStorageMemoryItem> getMemoryInfoItems() {
		return memoryItems;
	}

	public RefreshMemoryUsedInfo calculateMemoryUsedInfo(UpdateMemoryInfoUIAdapter listener) {
		File rootDir = new File(currentStoragePath);
		RefreshMemoryUsedInfo task = new RefreshMemoryUsedInfo(listener, otherMemory, rootDir, tilesMemory.getDirectories());
		task.execute(mapsMemory, tracksMemory, notesMemory);
		return task;
	}

	public RefreshMemoryUsedInfo calculateTilesMemoryUsed(UpdateMemoryInfoUIAdapter listener) {
		File rootDir = new File(tilesMemory.getDirectories()[0]);
		RefreshMemoryUsedInfo task = new RefreshMemoryUsedInfo(listener, otherMemory, rootDir);
		task.execute(tilesMemory);
		return task;
	}

	public static class RefreshMemoryUsedInfo extends AsyncTask<DataStorageMemoryItem, Void, Void> {
		private UpdateMemoryInfoUIAdapter listener;
		private File rootDir;
		private DataStorageMemoryItem otherMemory;
		private String[] directoriesToAvoid;

		public RefreshMemoryUsedInfo(UpdateMemoryInfoUIAdapter listener, DataStorageMemoryItem otherMemory, File rootDir, String... directoriesToAvoid) {
			this.listener = listener;
			this.otherMemory = otherMemory;
			this.rootDir = rootDir;
			this.directoriesToAvoid = directoriesToAvoid;
		}

		@Override
		protected Void doInBackground(DataStorageMemoryItem... items) {
			if (items.length == 1) {
				DataStorageMemoryItem item = items[0];
				item.addBytes(getDirectorySize(rootDir, item.getExtensions()));
			} else {
				calculateMultiTypes(items);
			}
			return null;
		}

		private void calculateMultiTypes(DataStorageMemoryItem[] items) {
			File[] files = rootDir.listFiles();

			for (File f : files) {
				boolean matched = false;
				if (f.isDirectory()) {
					boolean avoid = false;
					for (String directoryToAvoid : directoriesToAvoid) {
						if (f.getAbsolutePath().equals(directoryToAvoid)) {
							avoid = true;
							break;
						}
					}
					if (!avoid) {
						for (DataStorageMemoryItem item : items) {
							String[] directories = item.getDirectories();
							if (directories != null) {
								for (String directory : directories) {
									if (f.getAbsolutePath().equals(directory)) {
										item.addBytes(getDirectorySize(f, item.getExtensions()));
										matched = true;
										break;
									}
								}
							}
						}
						if (!matched) {
							otherMemory.addBytes(getDirectorySize(f, null));
						}
					}
				} else if (f.isFile()) {
					for (DataStorageMemoryItem item : items) {
						String[] extensions = item.getExtensions();
						if (extensions != null) {
							for (String extension : extensions) {
								if (f.getAbsolutePath().endsWith(extension)) {
									item.addBytes(f.length());
									matched = true;
									break;
								}
							}
						}
					}
					if (!matched) {
						otherMemory.addBytes(f.length());
					}
					publishProgress();
				}
			}
		}

		private long getDirectorySize(File dir, String[] extensions) {
			long bytes = 0;
			if (dir.isDirectory()) {
				File[] files = dir.listFiles();
				for (File file : files) {
					if (isCancelled()) {
						break;
					}
					if (file.isDirectory()) {
						bytes += getDirectorySize(file, extensions);
					} else if (file.isFile()) {
						//check file extension
						boolean matched = false;
						if (extensions != null) {
							for (String extension : extensions) {
								if (file.getName().endsWith(extension)) {
									matched = true;
									break;
								}
							}
						} else {
							matched = true;
						}
						if (matched) {
							bytes += file.length();
						} else {
							otherMemory.addBytes(file.length());
						}
						publishProgress();
					}
				}
			}
			return bytes;
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
			if (listener != null) {
				listener.onMemoryInfoUpdate();
			}
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			if (listener != null) {
				listener.onMemoryInfoUpdate();
			}
		}
	}

	public interface UpdateMemoryInfoUIAdapter {

		void onMemoryInfoUpdate();

	}

	@Override
	public int describeContents() {
		return 0;
	}

	private DataStorageItemsHolder(Parcel in) {
		menuItems = in.readArrayList(DataStorageMenuItem.class.getClassLoader());
		currentDataStorage = in.readParcelable(DataStorageMenuItem.class.getClassLoader());
		memoryItems = in.readArrayList(DataStorageMemoryItem.class.getClassLoader());
		currentStorageType = in.readInt();
		currentStoragePath = in.readString();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeArray(menuItems.toArray());
		dest.writeParcelable(currentDataStorage, flags);
		dest.writeArray(memoryItems.toArray());
		dest.writeInt(currentStorageType);
		dest.writeString(currentStoragePath);
	}

	public static final Parcelable.Creator<DataStorageItemsHolder> CREATOR = new Parcelable.Creator<DataStorageItemsHolder>() {

		@Override
		public DataStorageItemsHolder createFromParcel(Parcel source) {
			return new DataStorageItemsHolder(source);
		}

		@Override
		public DataStorageItemsHolder[] newArray(int size) {
			return new DataStorageItemsHolder[size];
		}
	};
}