package net.osmand.plus.settings;

import android.os.Build;

import net.osmand.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import java.io.File;
import java.util.ArrayList;

public class DataStorageItemsHolder {
	public final static String INTERNAL_STORAGE = "internal_storage";
	public final static String EXTERNAL_STORAGE = "external_storage";
	public final static String SHARED_STORAGE = "shared_storage";
	public final static String MULTIUSER_STORAGE = "multiuser_storage";
	public final static String MANUALLY_SPECIFIED = "manually_specified";

	private ArrayList<DataStorageMenuItem> menuItems;
	private DataStorageMenuItem currentDataStorage;
	private DataStorageMenuItem manuallySpecified;

	private int currentStorageType;
	private String currentStoragePath;

	private OsmandApplication app;
	private OsmandSettings settings;

	private DataStorageItemsHolder(OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		prepareData();
	}

	public static DataStorageItemsHolder refreshInfo(OsmandApplication app) {
		return new DataStorageItemsHolder(app);
	}

	private void prepareData() {

		if (app == null) {
			return;
		}
		
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

		menuItems = new ArrayList<>();

		String path;
		File dir;
		int iconId;

		//internal storage
		path = settings.getInternalAppPath().getAbsolutePath();
		dir = new File(path);
		iconId = R.drawable.ic_action_phone;

		DataStorageMenuItem internalStorageItem = DataStorageMenuItem.builder()
				.buildKey(INTERNAL_STORAGE)
				.buildTitle(getString(R.string.storage_directory_internal_app))
				.buildDirectory(path)
				.buildDescription(getString(R.string.internal_app_storage_description))
				.buildType(OsmandSettings.EXTERNAL_STORAGE_TYPE_INTERNAL_FILE)
				.buildIconResId(iconId)
				.build();
		addItem(internalStorageItem);

		//shared_storage
		dir = settings.getDefaultInternalStorage();
		path = dir.getAbsolutePath();
		iconId = R.drawable.ic_action_phone;

		DataStorageMenuItem sharedStorageItem = DataStorageMenuItem.builder()
				.buildKey(SHARED_STORAGE)
				.buildTitle(getString(R.string.storage_directory_shared))
				.buildDirectory(path)
				.buildType(OsmandSettings.EXTERNAL_STORAGE_TYPE_DEFAULT)
				.buildIconResId(iconId)
				.build();
		addItem(sharedStorageItem);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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
								.buildKey(EXTERNAL_STORAGE + i)
								.buildTitle(getString(R.string.storage_directory_external) + " " + i)
								.buildDirectory(path)
								.buildType(OsmandSettings.EXTERNAL_STORAGE_TYPE_EXTERNAL_FILE)
								.buildIconResId(iconId)
								.build();
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
								.buildKey(MULTIUSER_STORAGE + i)
								.buildTitle(getString(R.string.storage_directory_multiuser) + " " + i)
								.buildDirectory(path)
								.buildType(OsmandSettings.EXTERNAL_STORAGE_TYPE_OBB)
								.buildIconResId(iconId)
								.build();
						addItem(multiuserStorageItem);
					}
				}
			}
		}

		//manually specified storage
		manuallySpecified = DataStorageMenuItem.builder()
				.buildKey(MANUALLY_SPECIFIED)
				.buildTitle(getString(R.string.storage_directory_manual))
				.buildDirectory(currentStoragePath)
				.buildType(OsmandSettings.EXTERNAL_STORAGE_TYPE_SPECIFIED)
				.buildIconResId(R.drawable.ic_action_folder)
				.build();
		menuItems.add(manuallySpecified);

		if (currentDataStorage == null) {
			currentDataStorage = manuallySpecified;
		}
	}

	private String getString(int resId) {
		return app.getString(resId);
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
}
