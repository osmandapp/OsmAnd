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
import java.text.DecimalFormat;
import java.util.ArrayList;

import static net.osmand.plus.settings.DataStorageMemoryItem.Directory;
import static net.osmand.plus.settings.DataStorageMemoryItem.EXTENSIONS;
import static net.osmand.plus.settings.DataStorageMemoryItem.PREFIX;

public class DataStorageHelper implements Parcelable {
	public final static String INTERNAL_STORAGE = "internal_storage";
	public final static String EXTERNAL_STORAGE = "external_storage";
	public final static String SHARED_STORAGE = "shared_storage";
	public final static String MULTIUSER_STORAGE = "multiuser_storage";
	public final static String MANUALLY_SPECIFIED = "manually_specified";

	public final static String MAPS_MEMORY = "maps_memory_used";
	public final static String SRTM_AND_HILLSHADE_MEMORY = "contour_lines_and_hillshade_memory";
	public final static String TRACKS_MEMORY = "tracks_memory_used";
	public final static String NOTES_MEMORY = "notes_memory_used";
	public final static String TILES_MEMORY = "tiles_memory_used";
	public final static String OTHER_MEMORY = "other_memory_used";

	private ArrayList<DataStorageMenuItem> menuItems = new ArrayList<>();
	private DataStorageMenuItem currentDataStorage;
	private DataStorageMenuItem manuallySpecified;

	private ArrayList<DataStorageMemoryItem> memoryItems = new ArrayList<>();
	private DataStorageMemoryItem mapsMemory;
	private DataStorageMemoryItem srtmAndHillshadeMemory;
	private DataStorageMemoryItem tracksMemory;
	private DataStorageMemoryItem notesMemory;
	private DataStorageMemoryItem tilesMemory;
	private DataStorageMemoryItem otherMemory;

	private int currentStorageType;
	private String currentStoragePath;

	private DataStorageHelper(OsmandApplication app) {
		prepareData(app);
	}

	public static DataStorageHelper refreshInfo(OsmandApplication app) {
		return new DataStorageHelper(app);
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
				.setExtensions(IndexConstants.BINARY_MAP_INDEX_EXT)
				.setDirectories(
						new Directory(app.getAppPath(IndexConstants.MAPS_PATH).getAbsolutePath(), false, EXTENSIONS, false),
						new Directory(app.getAppPath(IndexConstants.ROADS_INDEX_DIR).getAbsolutePath(), true, EXTENSIONS, false),
						new Directory(app.getAppPath(IndexConstants.WIKI_INDEX_DIR).getAbsolutePath(), true, EXTENSIONS, false),
						new Directory(app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).getAbsolutePath(), true, EXTENSIONS, false),
						new Directory(app.getAppPath(IndexConstants.BACKUP_INDEX_DIR).getAbsolutePath(), true, EXTENSIONS, false))
				.createItem();
		memoryItems.add(mapsMemory);

		srtmAndHillshadeMemory = DataStorageMemoryItem.builder()
				.setKey(SRTM_AND_HILLSHADE_MEMORY)
				.setExtensions(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT)
				.setDirectories(
						new Directory(app.getAppPath(IndexConstants.SRTM_INDEX_DIR).getAbsolutePath(), true, EXTENSIONS, false),
						new Directory(app.getAppPath(IndexConstants.TILES_INDEX_DIR).getAbsolutePath(), false, PREFIX, true))
				.setPrefixes("Hillshade")
				.createItem();
		memoryItems.add(srtmAndHillshadeMemory);

		tracksMemory = DataStorageMemoryItem.builder()
				.setKey(TRACKS_MEMORY)
//				.setExtensions(".gpx", ".gpx.bz2")
				.setDirectories(
						new Directory(app.getAppPath(IndexConstants.GPX_INDEX_DIR).getAbsolutePath(), true, EXTENSIONS, false))
				.createItem();
		memoryItems.add(tracksMemory);

		notesMemory = DataStorageMemoryItem.builder()
				.setKey(NOTES_MEMORY)
//				.setExtensions("")
				.setDirectories(
						new Directory(app.getAppPath(IndexConstants.AV_INDEX_DIR).getAbsolutePath(), true, EXTENSIONS, false))
				.createItem();
		memoryItems.add(notesMemory);

		tilesMemory = DataStorageMemoryItem.builder()
				.setKey(TILES_MEMORY)
//				.setExtensions("")
				.setDirectories(
						new Directory(app.getAppPath(IndexConstants.TILES_INDEX_DIR).getAbsolutePath(), true, EXTENSIONS, false))
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
		RefreshMemoryUsedInfo task = new RefreshMemoryUsedInfo(listener, otherMemory, rootDir, null, null);
		task.execute(mapsMemory, srtmAndHillshadeMemory, tracksMemory, notesMemory);
		return task;
	}

	public RefreshMemoryUsedInfo calculateTilesMemoryUsed(UpdateMemoryInfoUIAdapter listener) {
		File rootDir = new File(tilesMemory.getDirectories()[0].getAbsolutePath());
		RefreshMemoryUsedInfo task = new RefreshMemoryUsedInfo(listener, otherMemory, rootDir, null, srtmAndHillshadeMemory.getPrefixes());
		task.execute(tilesMemory);
		return task;
	}

	public static class RefreshMemoryUsedInfo extends AsyncTask<DataStorageMemoryItem, Void, Void> {
		private UpdateMemoryInfoUIAdapter listener;
		private File rootDir;
		private DataStorageMemoryItem otherMemory;
		private String[] directoriesToAvoid;
		private String[] prefixesToAvoid;

		public RefreshMemoryUsedInfo(UpdateMemoryInfoUIAdapter listener, DataStorageMemoryItem otherMemory, File rootDir, String[] directoriesToAvoid, String[] prefixesToAvoid) {
			this.listener = listener;
			this.otherMemory = otherMemory;
			this.rootDir = rootDir;
			this.directoriesToAvoid = directoriesToAvoid;
			this.prefixesToAvoid = prefixesToAvoid;
		}

		@Override
		protected Void doInBackground(DataStorageMemoryItem... items) {
			if (rootDir.canRead()) {
				calculateMultiTypes(rootDir, items);
			}
			return null;
		}

		private void calculateMultiTypes(File rootDir, DataStorageMemoryItem... items) {
			File[] subFiles = rootDir.listFiles();

			for (File file : subFiles) {
				if (isCancelled()) {
					break;
				}
				nextFile : {
					if (file.isDirectory()) {
						//check current directory should be avoid
						if (directoriesToAvoid != null) {
							for (String directoryToAvoid : directoriesToAvoid) {
								if (file.getAbsolutePath().equals(directoryToAvoid)) {
									break nextFile;
								}
							}
						}
						//check current directory matched items type
						for (DataStorageMemoryItem item : items) {
							Directory[] directories = item.getDirectories();
							if (directories == null) {
								continue;
							}
							for (Directory dir : directories) {
								if (file.getAbsolutePath().equals(dir.getAbsolutePath())
										|| (file.getAbsolutePath().startsWith(dir.getAbsolutePath()) && dir.isGoDeeper())) {
									calculateMultiTypes(file, items);
									break nextFile;
								}
							}
						}
						//current directory did not match to any type
						otherMemory.addBytes(getDirectorySize(file));
					} else if (file.isFile()) {
						//check current file should be avoid
						if (prefixesToAvoid != null) {
							for (String prefixToAvoid : prefixesToAvoid) {
								if (file.getName().toLowerCase().startsWith(prefixToAvoid.toLowerCase())) {
									break nextFile;
								}
							}
						}
						//check current file matched items type
						for (DataStorageMemoryItem item : items) {
							Directory[] directories = item.getDirectories();
							if (directories == null) {
								continue;
							}
							for (Directory dir : directories) {
								if (rootDir.getAbsolutePath().equals(dir.getAbsolutePath())
										|| (rootDir.getAbsolutePath().startsWith(dir.getAbsolutePath()) && dir.isGoDeeper())) {
									int checkingType = dir.getCheckingType();
									switch (checkingType) {
										case EXTENSIONS : {
											String[] extensions = item.getExtensions();
											if (extensions != null) {
												for (String extension : extensions) {
													if (file.getAbsolutePath().endsWith(extension)) {
														item.addBytes(file.length());
														break nextFile;
													}
												}
											} else {
												item.addBytes(file.length());
												break nextFile;
											}
											break ;
										}
										case PREFIX : {
											String[] prefixes = item.getPrefixes();
											if (prefixes != null) {
												for (String prefix : prefixes) {
													if (file.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
														item.addBytes(file.length());
														break nextFile;
													}
												}
											} else {
												item.addBytes(file.length());
												break nextFile;
											}
											break ;
										}
									}
									if (dir.isSkipOther()) {
										break nextFile;
									}
								}
							}
						}
						//current file did not match any type
						otherMemory.addBytes(file.length());
					}
				}
				publishProgress();
			}
		}

		private long getDirectorySize(File dir) {
			long bytes = 0;
			if (dir.isDirectory()) {
				File[] files = dir.listFiles();
				for (File file : files) {
					if (isCancelled()) {
						break;
					}
					if (file.isDirectory()) {
						bytes += getDirectorySize(file);
					} else if (file.isFile()) {
						bytes += file.length();
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

	public long getTotalUsedBytes() {
		long total = 0;
		if (memoryItems != null && memoryItems.size() > 0) {
			for (DataStorageMemoryItem mi : memoryItems) {
				total += mi.getUsedMemoryBytes();
			}
			return total;
		}
		return -1;
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

	public interface UpdateMemoryInfoUIAdapter {

		void onMemoryInfoUpdate();

	}

	@Override
	public int describeContents() {
		return 0;
	}

	private DataStorageHelper(Parcel in) {
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

	public static final Parcelable.Creator<DataStorageHelper> CREATOR = new Parcelable.Creator<DataStorageHelper>() {

		@Override
		public DataStorageHelper createFromParcel(Parcel source) {
			return new DataStorageHelper(source);
		}

		@Override
		public DataStorageHelper[] newArray(int size) {
			return new DataStorageHelper[size];
		}
	};
}