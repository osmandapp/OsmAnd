package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.FileUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupHelper.OnDownloadFileListListener;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemsFactory;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static net.osmand.plus.backup.BackupHelper.INFO_EXT;

class BackupImporter {

	private static final Log LOG = PlatformUtil.getLog(BackupImporter.class);

	private final BackupHelper backupHelper;

	BackupImporter(@NonNull BackupHelper backupHelper) {
		this.backupHelper = backupHelper;
	}

	List<SettingsItem> collectItems() throws IllegalArgumentException, IOException {
		List<SettingsItem> result = new ArrayList<>();
		StringBuilder error = new StringBuilder();
		try {
			backupHelper.downloadFileListSync(new OnDownloadFileListListener() {
				@Override
				public void onDownloadFileList(int status, @Nullable String message, @NonNull List<RemoteFile> remoteFiles) {
					if (status == BackupHelper.STATUS_SUCCESS) {
						try {
							backupHelper.setRemoteFiles(remoteFiles);
							List<SettingsItem> items = getRemoteItems(remoteFiles);
							result.addAll(items);
						} catch (IOException e) {
							error.append(e.getMessage());
						}
					} else {
						error.append(message);
					}
				}
			});
		} catch (UserNotRegisteredException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		if (!Algorithms.isEmpty(error)) {
			throw new IOException(error.toString());
		}
		return result;
	}

	void importItems(@NonNull List<SettingsItem> items) throws IllegalArgumentException, IOException {
		if (Algorithms.isEmpty(items)) {
			throw new IllegalArgumentException("No items");
		}
		List<RemoteFile> remoteFiles = backupHelper.getRemoteFiles();
		if (Algorithms.isEmpty(remoteFiles)) {
			throw new IllegalArgumentException("No remote files");
		}
		OsmandApplication app = backupHelper.getApp();
		File tempDir = FileUtils.getTempDir(app);
		for (RemoteFile remoteFile : remoteFiles) {
			String fileName = remoteFile.getName();
			SettingsItem item = null;
			for (SettingsItem settingsItem : items) {
				if (settingsItem != null && settingsItem.applyFileName(fileName)) {
					item = settingsItem;
					break;
				}
			}
			if (item != null) {
				FileInputStream is = null;
				try {
					SettingsItemReader<? extends SettingsItem> reader = item.getReader();
					if (reader != null) {
						File tempFile = new File(tempDir, fileName);
						Map<File, RemoteFile> map = new HashMap<>();
						map.put(tempFile, remoteFile);
						Map<File, String> errors = backupHelper.downloadFilesSync(map, null);
						if (errors.isEmpty()) {
							is = new FileInputStream(tempFile);
							reader.readFromStream(is, fileName);
						} else {
							throw new IOException("Error reading temp item file " + fileName + ": " +
									errors.values().iterator().next());
						}
					}
					item.applyAdditionalParams();
				} catch (IllegalArgumentException e) {
					item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
					LOG.error("Error reading item data: " + item.getName(), e);
				} catch (IOException e) {
					item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
					LOG.error("Error reading item data: " + item.getName(), e);
				} catch (UserNotRegisteredException e) {
					item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
					LOG.error("Error reading item data: " + item.getName(), e);
				} finally {
					Algorithms.closeStream(is);
				}
			}
		}
		backupHelper.setRemoteFiles(null);
	}

	@NonNull
	private List<SettingsItem> getRemoteItems(@NonNull List<RemoteFile> remoteFiles) throws IllegalArgumentException, IOException {
		List<SettingsItem> items = new ArrayList<>();
		try {
			JSONObject json = new JSONObject();
			JSONArray itemsJson = new JSONArray();
			json.put("items", itemsJson);
			Map<File, RemoteFile> remoteInfoFiles = new HashMap<>();
			Map<String, RemoteFile> remoteItemFiles = new HashMap<>();
			OsmandApplication app = backupHelper.getApp();
			File tempDir = FileUtils.getTempDir(app);
			for (RemoteFile remoteFile : remoteFiles) {
				String fileName = remoteFile.getName();
				if (Algorithms.getFileNameExtension(fileName).equals(INFO_EXT)) {
					remoteInfoFiles.put(new File(tempDir, fileName), remoteFile);
				} else {
					remoteItemFiles.put(fileName, remoteFile);
				}
			}
			if (!remoteInfoFiles.isEmpty()) {
				downloadInfoFiles(itemsJson, remoteInfoFiles);

				SettingsItemsFactory itemsFactory = new SettingsItemsFactory(app, json);
				List<SettingsItem> settingsItemList = itemsFactory.getItems();
				updateFilesInfo(remoteItemFiles, settingsItemList);
				items.addAll(settingsItemList);

				Map<RemoteFile, SettingsItemReader<? extends SettingsItem>> remoteFilesForRead = new HashMap<>();
				for (SettingsItem item : settingsItemList) {
					String fileName = item.getFileName();
					RemoteFile remoteFile = remoteItemFiles.get(fileName);
					if (remoteFile != null && item.shouldReadOnCollecting()) {
						SettingsItemReader<? extends SettingsItem> reader = item.getReader();
						if (reader != null) {
							remoteFilesForRead.put(remoteFile, reader);
						}
					}
				}
				Map<File, RemoteFile> remoteFilesForDownload = new HashMap<>();
				for (RemoteFile remoteFile : remoteFilesForRead.keySet()) {
					String fileName = remoteFile.getName();
					remoteFilesForDownload.put(new File(tempDir, fileName), remoteFile);
				}
				downloadAndReadItemFiles(remoteFilesForRead, remoteFilesForDownload);
			}
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Error reading items", e);
		} catch (JSONException e) {
			throw new IllegalArgumentException("Error parsing items", e);
		} catch (UserNotRegisteredException e) {
			throw new IllegalArgumentException("User is not registered", e);
		} catch (IOException e) {
			throw new IOException(e);
		}
		return items;
	}

	private void downloadInfoFiles(JSONArray itemsJson, Map<File, RemoteFile> remoteInfoFiles) throws UserNotRegisteredException, JSONException, IOException {
		Map<File, String> errors = backupHelper.downloadFilesSync(remoteInfoFiles, null);
		if (errors.isEmpty()) {
			for (File file : remoteInfoFiles.keySet()) {
				String jsonStr = Algorithms.getFileAsString(file);
				if (!Algorithms.isEmpty(jsonStr)) {
					itemsJson.put(new JSONObject(jsonStr));
				} else {
					throw new IOException("Error reading item info: " + file.getName());
				}
			}
		} else {
			throw new IOException("Error downloading items info");
		}
	}

	private void downloadAndReadItemFiles(@NonNull Map<RemoteFile, SettingsItemReader<? extends SettingsItem>> remoteFilesForRead,
										  @NonNull Map<File, RemoteFile> remoteFilesForDownload) throws UserNotRegisteredException, IOException {
		OsmandApplication app = backupHelper.getApp();
		Map<File, String> errors = backupHelper.downloadFilesSync(remoteFilesForDownload, null);
		if (errors.isEmpty()) {
			for (Entry<File, RemoteFile> entry : remoteFilesForDownload.entrySet()) {
				File tempFile = entry.getKey();
				RemoteFile remoteFile = entry.getValue();
				if (tempFile.exists()) {
					SettingsItemReader<? extends SettingsItem> reader = remoteFilesForRead.get(remoteFile);
					if (reader != null) {
						SettingsItem item = reader.getItem();
						FileInputStream is = new FileInputStream(tempFile);
						try {
							reader.readFromStream(is, item.getFileName());
							item.applyAdditionalParams();
						} catch (IllegalArgumentException e) {
							item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
							SettingsHelper.LOG.error("Error reading item data: " + item.getName(), e);
						} catch (IOException e) {
							item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
							SettingsHelper.LOG.error("Error reading item data: " + item.getName(), e);
						} finally {
							Algorithms.closeStream(is);
						}
					} else {
						throw new IOException("No reader for: " + tempFile.getName());
					}
				} else {
					throw new IOException("No temp item file: " + tempFile.getName());
				}
			}
		} else {
			throw new IOException("Error downloading temp item files");
		}
	}

	private void updateFilesInfo(@NonNull Map<String, RemoteFile> remoteFiles, List<SettingsItem> settingsItemList) throws IOException {
		for (SettingsItem settingsItem : settingsItemList) {
			if (settingsItem instanceof FileSettingsItem) {
				RemoteFile remoteFile = remoteFiles.get(settingsItem.getFileName());
				if (remoteFile != null) {
					FileSettingsItem fileSettingsItem = (FileSettingsItem) settingsItem;
					fileSettingsItem.setSize(remoteFile.getFilesize());
					fileSettingsItem.setLastModified(remoteFile.getClienttimems());
				}
			}
		}
	}
}
