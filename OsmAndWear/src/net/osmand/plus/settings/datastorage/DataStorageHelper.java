package net.osmand.plus.settings.datastorage;

import android.app.ProgressDialog;
import android.os.Environment;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IProgress;
import net.osmand.data.ValueHolder;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.resources.ResourceManager.ReloadIndexesListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.item.StorageItem;
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

	private final OsmandApplication app;
	private final List<StorageItem> storageItems = new ArrayList<>();

	private StorageItem currentDataStorage;
	private StorageItem manuallySpecified;

	private int currentStorageType;
	private String currentStoragePath;

	public DataStorageHelper(@NonNull OsmandApplication app) {
		this.app = app;
		prepareData();
	}

	@NonNull
	public List<StorageItem> getStorageItems() {
		return storageItems;
	}

	public StorageItem getCurrentStorage() {
		return currentDataStorage;
	}

	private void prepareData() {
		initStorageItems();
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
		if (key != null) {
			for (StorageItem storageItem : storageItems) {
				if (key.equals(storageItem.getKey())) {
					return storageItem;
				}
			}
		}
		return null;
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
			public void reloadIndexesFinished(@NonNull List<String> warnings) {
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
}
