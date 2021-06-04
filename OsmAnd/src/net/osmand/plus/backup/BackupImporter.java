package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.FileUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupHelper.OnDownloadFileListListener;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemsFactory;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.GpxSettingsItem;
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
import java.util.Collection;
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

	@NonNull
	List<SettingsItem> collectItems(boolean readItems) throws IllegalArgumentException, IOException {
		List<SettingsItem> result = new ArrayList<>();
		StringBuilder error = new StringBuilder();
		try {
			backupHelper.downloadFileListSync(new OnDownloadFileListListener() {
				@Override
				public void onDownloadFileList(int status, @Nullable String message, @NonNull List<RemoteFile> remoteFiles) {
					if (status == BackupHelper.STATUS_SUCCESS) {
						try {
							backupHelper.setRemoteFiles(remoteFiles);
							List<SettingsItem> items = getRemoteItems(remoteFiles, readItems);
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

	void importItems(@NonNull List<SettingsItem> items) throws IllegalArgumentException {
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
			SettingsItem item = null;
			for (SettingsItem settingsItem : items) {
				if (settingsItem.equals(remoteFile.item)) {
					item = settingsItem;
					break;
				}
			}
			if (item != null && !item.shouldReadOnCollecting()) {
				FileInputStream is = null;
				try {
					SettingsItemReader<? extends SettingsItem> reader = item.getReader();
					if (reader != null) {
						String fileName = remoteFile.getTypeNamePath();
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
	private List<SettingsItem> getRemoteItems(@NonNull List<RemoteFile> remoteFiles, boolean readItems) throws IllegalArgumentException, IOException {
		List<SettingsItem> items = new ArrayList<>();
		if (remoteFiles.isEmpty()) {
			return items;
		}
		try {
			JSONObject json = new JSONObject();
			JSONArray itemsJson = new JSONArray();
			json.put("items", itemsJson);
			Map<File, RemoteFile> remoteInfoFilesMap = new HashMap<>();
			Map<String, RemoteFile> remoteItemFilesMap = new HashMap<>();
			List<RemoteFile> remoteInfoFiles = new ArrayList<>();
			List<String> remoteInfoNames = new ArrayList<>();
			List<RemoteFile> noInfoRemoteItemFiles = new ArrayList<>();
			OsmandApplication app = backupHelper.getApp();
			File tempDir = FileUtils.getTempDir(app);
			for (RemoteFile remoteFile : remoteFiles) {
				String fileName = remoteFile.getTypeNamePath();
				if (fileName.endsWith(INFO_EXT)) {
					if (readItems) {
						remoteInfoFilesMap.put(new File(tempDir, fileName), remoteFile);
					}
					String itemFileName = fileName.substring(0, fileName.length() - INFO_EXT.length());
					remoteInfoNames.add(itemFileName);
					remoteInfoFiles.add(remoteFile);
				} else {
					remoteItemFilesMap.put(fileName, remoteFile);
				}
			}
			for (Entry<String, RemoteFile> remoteFileEntry : remoteItemFilesMap.entrySet()) {
				String itemFileName = remoteFileEntry.getKey();
				boolean hasInfo = false;
				for (String remoteInfoName : remoteInfoNames) {
					if (itemFileName.equals(remoteInfoName) || itemFileName.startsWith(remoteInfoName + "/")) {
						hasInfo = true;
						break;
					}
				}
				if (!hasInfo) {
					noInfoRemoteItemFiles.add(remoteFileEntry.getValue());
				}
			}
			if (readItems) {
				generateItemsJson(itemsJson, remoteInfoFilesMap, noInfoRemoteItemFiles);
			} else {
				generateItemsJson(itemsJson, remoteInfoFiles, noInfoRemoteItemFiles);
			}

			SettingsItemsFactory itemsFactory = new SettingsItemsFactory(app, json);
			List<SettingsItem> settingsItemList = itemsFactory.getItems();
			updateFilesInfo(remoteItemFilesMap, settingsItemList);
			items.addAll(settingsItemList);

			if (readItems) {
				Map<RemoteFile, SettingsItemReader<? extends SettingsItem>> remoteFilesForRead = new HashMap<>();
				for (SettingsItem item : settingsItemList) {
					RemoteFile remoteFile = getItemRemoteFile(item, remoteItemFilesMap.values());
					if (remoteFile != null && item.shouldReadOnCollecting()) {
						SettingsItemReader<? extends SettingsItem> reader = item.getReader();
						if (reader != null) {
							remoteFilesForRead.put(remoteFile, reader);
						}
					}
				}
				Map<File, RemoteFile> remoteFilesForDownload = new HashMap<>();
				for (RemoteFile remoteFile : remoteFilesForRead.keySet()) {
					String fileName = remoteFile.getTypeNamePath();
					remoteFilesForDownload.put(new File(tempDir, fileName), remoteFile);
				}
				if (!remoteFilesForDownload.isEmpty()) {
					downloadAndReadItemFiles(remoteFilesForRead, remoteFilesForDownload);
				}
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

	@Nullable
	private RemoteFile getItemRemoteFile(@NonNull SettingsItem item, @NonNull Collection<RemoteFile> remoteFiles) {
		String fileName = item.getFileName();
		if (!Algorithms.isEmpty(fileName)) {
			if (fileName.charAt(0) != '/') {
				fileName = "/" + fileName;
			}
			if (item instanceof GpxSettingsItem) {
				GpxSettingsItem gpxItem = (GpxSettingsItem) item;
				String folder = gpxItem.getSubtype().getSubtypeFolder();
				if (!Algorithms.isEmpty(folder) && folder.charAt(0) != '/') {
					folder = "/" + folder;
				}
				if (fileName.startsWith(folder)) {
					fileName = fileName.substring(folder.length() - 1);
				}
			}
			for (RemoteFile remoteFile : remoteFiles) {
				String remoteFileName = remoteFile.getTypeNamePath();
				String typeFileName = remoteFile.getType() + fileName;
				if (remoteFileName.equals(typeFileName) || remoteFileName.startsWith(typeFileName + "/")) {
					return remoteFile;
				}
			}
		}
		return null;
	}

	private void generateItemsJson(@NonNull JSONArray itemsJson,
								   @NonNull List<RemoteFile> remoteInfoFiles,
								   @NonNull List<RemoteFile> noInfoRemoteItemFiles) throws JSONException {
		for (RemoteFile remoteFile : remoteInfoFiles) {
			String fileName = remoteFile.getName();
			fileName = fileName.substring(0, fileName.length() - INFO_EXT.length());
			String type = remoteFile.getType();
			if (SettingsItemType.GPX.name().equals(type)) {
				fileName = FileSubtype.GPX.getSubtypeFolder() + fileName;
			}
			JSONObject itemJson = new JSONObject();
			itemJson.put("type", type);
			itemJson.put("file", fileName);
			itemsJson.put(itemJson);
		}
		addRemoteFilesToJson(itemsJson, noInfoRemoteItemFiles);
	}

	private void generateItemsJson(@NonNull JSONArray itemsJson,
								   @NonNull Map<File, RemoteFile> remoteInfoFiles,
								   @NonNull List<RemoteFile> noInfoRemoteItemFiles) throws UserNotRegisteredException, JSONException, IOException {
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
		addRemoteFilesToJson(itemsJson, noInfoRemoteItemFiles);
	}

	private void addRemoteFilesToJson(@NonNull JSONArray itemsJson, @NonNull List<RemoteFile> noInfoRemoteItemFiles) throws JSONException {
		for (RemoteFile remoteFile : noInfoRemoteItemFiles) {
			String type = remoteFile.getType();
			String fileName = remoteFile.getName();
			JSONObject itemJson = new JSONObject();
			itemJson.put("type", type);
			itemJson.put("file", fileName);
			itemsJson.put(itemJson);
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
							LOG.error("Error reading item data: " + item.getName(), e);
						} catch (IOException e) {
							item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
							LOG.error("Error reading item data: " + item.getName(), e);
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

	private void updateFilesInfo(@NonNull Map<String, RemoteFile> remoteFiles, List<SettingsItem> settingsItemList) {
		for (SettingsItem settingsItem : settingsItemList) {
			RemoteFile remoteFile = getItemRemoteFile(settingsItem, remoteFiles.values());
			if (remoteFile != null) {
				settingsItem.setLastModifiedTime(remoteFile.getClienttimems());
				remoteFile.item = settingsItem;
				if (settingsItem instanceof FileSettingsItem) {
					FileSettingsItem fileSettingsItem = (FileSettingsItem) settingsItem;
					fileSettingsItem.setSize(remoteFile.getFilesize());
				}
			}
		}
	}
}