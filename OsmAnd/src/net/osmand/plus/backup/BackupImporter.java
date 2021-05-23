package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupHelper.OnDownloadFileListListener;
import net.osmand.plus.settings.backend.backup.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsHelper.SettingsCollectListener;
import net.osmand.plus.settings.backend.backup.SettingsItem;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemsFactory;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static net.osmand.IndexConstants.OSMAND_SETTINGS_FILE_EXT;

class BackupImporter {

	private static final Log LOG = PlatformUtil.getLog(BackupImporter.class);

	private final BackupHelper backupHelper;

	BackupImporter(@NonNull BackupHelper backupHelper) {
		this.backupHelper = backupHelper;
	}

	void collectItems(final @NonNull SettingsCollectListener collectListener) throws IllegalArgumentException {
		try {
			backupHelper.downloadFileList(new OnDownloadFileListListener() {
				@Override
				public void onDownloadFileList(int status, @Nullable String message, @NonNull List<RemoteFile> remoteFiles) {
					if (status == BackupHelper.STATUS_SUCCESS) {
						List<SettingsItem> items = getItemsFromFiles(remoteFiles);
						collectListener.onSettingsCollectFinished(true, items.isEmpty(), items);
					} else {
						collectListener.onSettingsCollectFinished(false, true, Collections.emptyList());
						onError(!Algorithms.isEmpty(message) ? message : "Download file list error: " + status);
					}
				}
			});
		} catch (UserNotRegisteredException e) {
			collectListener.onSettingsCollectFinished(false, true, Collections.emptyList());
		}
	}

	void importItems(@NonNull File file, @NonNull List<SettingsItem> items) throws IllegalArgumentException, IOException {
		processItems(file, items);
	}

	@NonNull
	private List<SettingsItem> getItemsFromFiles(@NonNull List<RemoteFile> remoteFiles) {
		List<SettingsItem> items = new ArrayList<>();
		try {
			JSONObject json = new JSONObject();
			JSONArray itemsJson = new JSONArray();
			json.put("items", itemsJson);
			for (RemoteFile remoteFile : remoteFiles) {

			}

			SettingsItemsFactory itemsFactory = new SettingsItemsFactory(backupHelper.getApp(), json);
			List<SettingsItem> settingsItemList = itemsFactory.getItems();
			updateFilesInfo(file, settingsItemList);
			items.addAll(settingsItemList);
		} catch (IllegalArgumentException e) {
			SettingsHelper.LOG.error("Error parsing items: " + itemsJson, e);
			throw new IllegalArgumentException("No items");
		} catch (JSONException e) {
			SettingsHelper.LOG.error("Error parsing items: " + itemsJson, e);
			throw new IllegalArgumentException("No items");
		}
		return items;
	}

	private void updateFilesInfo(@NonNull File file, List<SettingsItem> settingsItemList) throws IOException {
		ZipFile zipfile = new ZipFile(file.getPath());
		Enumeration<? extends ZipEntry> zipEnum = zipfile.entries();
		while (zipEnum.hasMoreElements()) {
			ZipEntry zipEntry = zipEnum.nextElement();
			long size = zipEntry.getSize();
			for (SettingsItem settingsItem : settingsItemList) {
				if (settingsItem instanceof FileSettingsItem
						&& zipEntry.getName().equals(settingsItem.getFileName())) {
					FileSettingsItem fileSettingsItem = (FileSettingsItem) settingsItem;
					fileSettingsItem.setSize(size);
					fileSettingsItem.setLastModified(zipEntry.getTime());
					break;
				}
			}
		}
	}

	private List<SettingsItem> processItems(@NonNull File file, @Nullable List<SettingsItem> items) throws IllegalArgumentException, IOException {
		if (Algorithms.isEmpty(items)) {
			throw new IllegalArgumentException("No items");
		}
		ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
		InputStream ois = new BufferedInputStream(zis);
		try {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				String fileName = checkEntryName(entry.getName());
				SettingsItem item = null;
				for (SettingsItem settingsItem : items) {
					if (settingsItem != null && settingsItem.applyFileName(fileName)) {
						item = settingsItem;
						break;
					}
				}
				if (item != null) {
					try {
						SettingsItemReader<? extends SettingsItem> reader = item.getReader();
						if (reader != null) {
							reader.readFromStream(ois, fileName);
						}
						item.applyAdditionalParams();
					} catch (IllegalArgumentException e) {
						item.warnings.add(app.getString(R.string.settings_item_read_error, item.getName()));
						SettingsHelper.LOG.error("Error reading item data: " + item.getName(), e);
					} catch (IOException e) {
						item.warnings.add(app.getString(R.string.settings_item_read_error, item.getName()));
						SettingsHelper.LOG.error("Error reading item data: " + item.getName(), e);
					} finally {
						zis.closeEntry();
					}
				}
			}
		} catch (IOException ex) {
			SettingsHelper.LOG.error("Failed to read next entry", ex);
		} finally {
			Algorithms.closeStream(ois);
			Algorithms.closeStream(zis);
		}
		return items;
	}

	private String checkEntryName(String entryName) {
		String fileExt = OSMAND_SETTINGS_FILE_EXT + "/";
		int index = entryName.indexOf(fileExt);
		if (index != -1) {
			entryName = entryName.substring(index + fileExt.length());
		}
		return entryName;
	}
}
