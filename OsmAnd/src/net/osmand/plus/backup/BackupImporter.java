package net.osmand.plus.backup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.FileUtils;
import net.osmand.OperationLog;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupListeners.OnDownloadFileListener;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import static net.osmand.plus.backup.BackupHelper.INFO_EXT;

class BackupImporter {

	private static final Log LOG = PlatformUtil.getLog(BackupImporter.class);

	private final BackupHelper backupHelper;

	private boolean cancelled;

	public static class CollectItemsResult {
		public List<SettingsItem> items;
		public List<RemoteFile> remoteFiles;
	}

	BackupImporter(@NonNull BackupHelper backupHelper) {
		this.backupHelper = backupHelper;
	}

	@NonNull
	CollectItemsResult collectItems(boolean readItems) throws IllegalArgumentException, IOException {
		CollectItemsResult result = new CollectItemsResult();
		StringBuilder error = new StringBuilder();
		OperationLog operationLog = new OperationLog("collectRemoteItems", BackupHelper.DEBUG);
		operationLog.startOperation();
		try {
			backupHelper.downloadFileList((status, message, remoteFiles) -> {
				if (status == BackupHelper.STATUS_SUCCESS) {
					result.remoteFiles = remoteFiles;
					try {
						result.items = getRemoteItems(remoteFiles, readItems);
					} catch (IOException e) {
						error.append(e.getMessage());
					}
				} else {
					error.append(message);
				}
			});
		} catch (UserNotRegisteredException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		operationLog.finishOperation();
		if (!Algorithms.isEmpty(error)) {
			throw new IOException(error.toString());
		}
		return result;
	}

	void importItems(@NonNull List<SettingsItem> items, boolean forceReadData) throws IllegalArgumentException {
		if (Algorithms.isEmpty(items)) {
			throw new IllegalArgumentException("No items");
		}
		Collection<RemoteFile> remoteFiles = backupHelper.getBackup().getRemoteFiles(RemoteFilesType.UNIQUE).values();
		if (Algorithms.isEmpty(remoteFiles)) {
			throw new IllegalArgumentException("No remote files");
		}
		OperationLog operationLog = new OperationLog("importItems", BackupHelper.DEBUG);
		operationLog.startOperation();
		List<ItemFileImportTask> tasks = new ArrayList<>();
		Map<RemoteFile, SettingsItem> remoteFileItems = new HashMap<>();
		for (RemoteFile remoteFile : remoteFiles) {
			SettingsItem item = null;
			for (SettingsItem settingsItem : items) {
				String fileName = remoteFile.item != null ? remoteFile.item.getFileName() : null;
				if (fileName != null && settingsItem.applyFileName(fileName)) {
					item = settingsItem;
					remoteFileItems.put(remoteFile, item);
					break;
				}
			}
			if (item != null && (!item.shouldReadOnCollecting() || forceReadData)) {
				tasks.add(new ItemFileImportTask(remoteFile, item, forceReadData));
			}
		}
		ThreadPoolTaskExecutor<ItemFileImportTask> executor = createExecutor();
		executor.run(tasks);

		for (Entry<RemoteFile, SettingsItem> fileItem : remoteFileItems.entrySet()) {
			fileItem.getValue().setLocalModifiedTime(fileItem.getKey().getClienttimems());
		}
		operationLog.finishOperation();
	}

	private void importItemFile(@NonNull RemoteFile remoteFile, @NonNull SettingsItem item, boolean forceReadData) {
		OsmandApplication app = backupHelper.getApp();
		File tempDir = FileUtils.getTempDir(app);
		FileInputStream is = null;
		try {
			SettingsItemReader<? extends SettingsItem> reader = item.getReader();
			if (reader != null) {
				String fileName = remoteFile.getTypeNamePath();
				File tempFile = new File(tempDir, fileName);
				String error = backupHelper.downloadFile(tempFile, remoteFile, getOnDownloadFileListener());
				if (Algorithms.isEmpty(error)) {
					is = new FileInputStream(tempFile);
					reader.readFromStream(is, remoteFile.getName());
					if (forceReadData) {
						item.apply();
					}
					backupHelper.updateFileUploadTime(remoteFile.getType(), remoteFile.getName(), remoteFile.getClienttimems());
					if (item instanceof FileSettingsItem) {
						String itemFileName = BackupHelper.getFileItemName((FileSettingsItem) item);
						if (app.getAppPath(itemFileName).isDirectory()) {
							backupHelper.updateFileUploadTime(item.getType().name(), itemFileName,
									remoteFile.getClienttimems());
						}
					}
				} else {
					throw new IOException("Error reading temp item file " + fileName + ": " + error);
				}
			}
			item.applyAdditionalParams(reader);
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

	@NonNull
	private List<SettingsItem> getRemoteItems(@NonNull List<RemoteFile> remoteFiles, boolean readItems) throws IllegalArgumentException, IOException {
		if (remoteFiles.isEmpty()) {
			return Collections.emptyList();
		}
		List<SettingsItem> items = new ArrayList<>();
		try {
			OperationLog operationLog = new OperationLog("getRemoteItems", BackupHelper.DEBUG);
			operationLog.startOperation();
			JSONObject json = new JSONObject();
			JSONArray itemsJson = new JSONArray();
			json.put("items", itemsJson);
			Map<File, RemoteFile> remoteInfoFilesMap = new HashMap<>();
			Map<String, RemoteFile> remoteItemFilesMap = new HashMap<>();
			List<RemoteFile> remoteInfoFiles = new ArrayList<>();
			Set<String> remoteInfoNames = new HashSet<>();
			List<RemoteFile> noInfoRemoteItemFiles = new ArrayList<>();
			OsmandApplication app = backupHelper.getApp();
			File tempDir = FileUtils.getTempDir(app);

			List<RemoteFile> uniqueRemoteFiles = new ArrayList<>();
			Set<String> uniqueFileIds = new TreeSet<>();
			for (RemoteFile rf : remoteFiles) {
				String fileId = rf.getTypeNamePath();
				if (uniqueFileIds.add(fileId) && !rf.isDeleted()) {
					uniqueRemoteFiles.add(rf);
				}
			}
			operationLog.log("build uniqueRemoteFiles");

			Map<String, Long> infoMap = backupHelper.getDbHelper().getUploadedFileInfoMap();
			for (RemoteFile remoteFile : uniqueRemoteFiles) {
				String fileName = remoteFile.getTypeNamePath();
				if (fileName.endsWith(INFO_EXT)) {
					Long uploadTime = infoMap.get(remoteFile.getType() + "___"
							+ remoteFile.getName().substring(0, remoteFile.getName().length() - INFO_EXT.length()));
					if (readItems && (uploadTime == null || uploadTime != remoteFile.getClienttimems())) {
						remoteInfoFilesMap.put(new File(tempDir, fileName), remoteFile);
					}
					String itemFileName = fileName.substring(0, fileName.length() - INFO_EXT.length());
					remoteInfoNames.add(itemFileName);
					remoteInfoFiles.add(remoteFile);
				} else if (!remoteItemFilesMap.containsKey(fileName)) {
					remoteItemFilesMap.put(fileName, remoteFile);
				}
			}
			operationLog.log("build maps");

			for (Entry<String, RemoteFile> remoteFileEntry : remoteItemFilesMap.entrySet()) {
				String itemFileName = remoteFileEntry.getKey();
				RemoteFile remoteFile = remoteFileEntry.getValue();
				boolean hasInfo = false;
				for (String remoteInfoName : remoteInfoNames) {
					if (itemFileName.equals(remoteInfoName) || itemFileName.startsWith(remoteInfoName + "/")) {
						hasInfo = true;
						break;
					}
				}
				if (!hasInfo && !remoteFile.isRecordedVoiceFile()) {
					noInfoRemoteItemFiles.add(remoteFile);
				}
			}
			operationLog.log("build noInfoRemoteItemFiles");

			if (readItems) {
				generateItemsJson(itemsJson, remoteInfoFilesMap, noInfoRemoteItemFiles);
			} else {
				generateItemsJson(itemsJson, remoteInfoFiles, noInfoRemoteItemFiles);
			}
			operationLog.log("generateItemsJson");

			SettingsItemsFactory itemsFactory = new SettingsItemsFactory(app, json);
			operationLog.log("create setting items");
			List<SettingsItem> settingsItemList = itemsFactory.getItems();
			if (settingsItemList.isEmpty()) {
				return Collections.emptyList();
			}
			updateFilesInfo(remoteItemFilesMap, settingsItemList);
			items.addAll(settingsItemList);
			operationLog.log("updateFilesInfo");

			if (readItems) {
				Map<RemoteFile, SettingsItemReader<? extends SettingsItem>> remoteFilesForRead = new HashMap<>();
				for (SettingsItem item : settingsItemList) {
					if (item.shouldReadOnCollecting()) {
						List<RemoteFile> foundRemoteFiles = getItemRemoteFiles(item, remoteItemFilesMap);
						for (RemoteFile remoteFile : foundRemoteFiles) {
							SettingsItemReader<? extends SettingsItem> reader = item.getReader();
							if (reader != null) {
								remoteFilesForRead.put(remoteFile, reader);
							}
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
				operationLog.log("readItems");
			}
			operationLog.finishOperation();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Error reading items", e);
		} catch (JSONException e) {
			throw new IllegalArgumentException("Error parsing items", e);
		} catch (IOException e) {
			throw new IOException(e);
		}
		return items;
	}

	@NonNull
	private List<RemoteFile> getItemRemoteFiles(@NonNull SettingsItem item, @NonNull Map<String, RemoteFile> remoteFiles) {
		List<RemoteFile> res = new ArrayList<>();
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
			String typeFileName = item.getType().name() + fileName;
			RemoteFile remoteFile = remoteFiles.remove(typeFileName);
			if (remoteFile != null) {
				res.add(remoteFile);
			}
			Iterator<Entry<String, RemoteFile>> it = remoteFiles.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, RemoteFile> fileEntry = it.next();
				String remoteFileName = fileEntry.getKey();
				if (remoteFileName.startsWith(typeFileName + "/")) {
					res.add(fileEntry.getValue());
					it.remove();
				}
			}
		}
		return res;
	}

	private void generateItemsJson(@NonNull JSONArray itemsJson,
								   @NonNull List<RemoteFile> remoteInfoFiles,
								   @NonNull List<RemoteFile> noInfoRemoteItemFiles) throws JSONException {
		for (RemoteFile remoteFile : remoteInfoFiles) {
			String fileName = remoteFile.getName();
			fileName = fileName.substring(0, fileName.length() - INFO_EXT.length());
			String type = remoteFile.getType();
			JSONObject itemJson = new JSONObject();
			itemJson.put("type", type);
			if (SettingsItemType.GPX.name().equals(type)) {
				fileName = FileSubtype.GPX.getSubtypeFolder() + fileName;
			}
			if (SettingsItemType.PROFILE.name().equals(type)) {
				JSONObject appMode = new JSONObject();
				String name = fileName.replaceFirst("profile_", "");
				if (name.endsWith(".json")) {
					name = name.substring(0, name.length() - 5);
				}
				appMode.put("stringKey", name);
				itemJson.put("appMode", appMode);
			}
			itemJson.put("file", fileName);
			itemsJson.put(itemJson);
		}
		addRemoteFilesToJson(itemsJson, noInfoRemoteItemFiles);
	}

	private void generateItemsJson(@NonNull JSONArray itemsJson,
								   @NonNull Map<File, RemoteFile> remoteInfoFiles,
								   @NonNull List<RemoteFile> noInfoRemoteItemFiles) throws JSONException, IOException {
		List<FileDownloadTask> tasks = new ArrayList<>();
		for (Entry<File, RemoteFile> fileEntry : remoteInfoFiles.entrySet()) {
			tasks.add(new FileDownloadTask(fileEntry.getKey(), fileEntry.getValue()));
		}
		ThreadPoolTaskExecutor<FileDownloadTask> executor = createExecutor();
		executor.run(tasks);

		boolean hasDownloadErrors = hasDownloadErrors(tasks);
		if (!hasDownloadErrors) {
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
		Set<String> fileItems = new HashSet<>();
		for (RemoteFile remoteFile : noInfoRemoteItemFiles) {
			String type = remoteFile.getType();
			String fileName = remoteFile.getName();
			if (type.equals(SettingsItemType.FILE.name()) && fileName.startsWith(FileSubtype.VOICE.getSubtypeFolder())) {
				FileSubtype subtype = FileSubtype.getSubtypeByFileName(fileName);
				int lastSeparatorIndex = fileName.lastIndexOf('/');
				if (lastSeparatorIndex > 0) {
					fileName = fileName.substring(0, lastSeparatorIndex);
				}
				String typeName = subtype + "___" + fileName;
				if (!fileItems.contains(typeName)) {
					fileItems.add(typeName);
					JSONObject itemJson = new JSONObject();
					itemJson.put("type", type);
					itemJson.put("file", fileName);
					itemJson.put("subtype", subtype);
					itemsJson.put(itemJson);
				}
			} else {
				JSONObject itemJson = new JSONObject();
				itemJson.put("type", type);
				itemJson.put("file", fileName);
				itemsJson.put(itemJson);
			}
		}
	}

	private void downloadAndReadItemFiles(@NonNull Map<RemoteFile, SettingsItemReader<? extends SettingsItem>> remoteFilesForRead,
										  @NonNull Map<File, RemoteFile> remoteFilesForDownload) throws IOException {
		OsmandApplication app = backupHelper.getApp();
		List<FileDownloadTask> fileDownloadTasks = new ArrayList<>();
		for (Entry<File, RemoteFile> fileEntry : remoteFilesForDownload.entrySet()) {
			fileDownloadTasks.add(new FileDownloadTask(fileEntry.getKey(), fileEntry.getValue()));
		}
		ThreadPoolTaskExecutor<FileDownloadTask> filesDownloadExecutor = createExecutor();
		filesDownloadExecutor.run(fileDownloadTasks);

		boolean hasDownloadErrors = hasDownloadErrors(fileDownloadTasks);
		if (!hasDownloadErrors) {
			List<ItemFileDownloadTask> itemFileDownloadTasks = new ArrayList<>();
			for (Entry<File, RemoteFile> entry : remoteFilesForDownload.entrySet()) {
				File tempFile = entry.getKey();
				RemoteFile remoteFile = entry.getValue();
				if (tempFile.exists()) {
					SettingsItemReader<? extends SettingsItem> reader = remoteFilesForRead.get(remoteFile);
					if (reader != null) {
						itemFileDownloadTasks.add(new ItemFileDownloadTask(app, tempFile, reader));
					} else {
						throw new IOException("No reader for: " + tempFile.getName());
					}
				} else {
					throw new IOException("No temp item file: " + tempFile.getName());
				}
			}
			ThreadPoolTaskExecutor<ItemFileDownloadTask> itemFilesDownloadExecutor = createExecutor();
			itemFilesDownloadExecutor.run(itemFileDownloadTasks);
		} else {
			throw new IOException("Error downloading temp item files");
		}
	}

	private boolean hasDownloadErrors(@NonNull List<FileDownloadTask> tasks) {
		boolean hasError = false;
		for (FileDownloadTask task : tasks) {
			if (!Algorithms.isEmpty(task.error)) {
				hasError = true;
				break;
			}
		}
		return hasError;
	}

	private void downloadItemFile(@NonNull OsmandApplication app, @NonNull File tempFile,
								  @NonNull SettingsItemReader<? extends SettingsItem> reader) {
		SettingsItem item = reader.getItem();
		FileInputStream is = null;
		try {
			is = new FileInputStream(tempFile);
			reader.readFromStream(is, item.getFileName());
			item.applyAdditionalParams(reader);
		} catch (IllegalArgumentException e) {
			item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
			LOG.error("Error reading item data: " + item.getName(), e);
		} catch (IOException e) {
			item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
			LOG.error("Error reading item data: " + item.getName(), e);
		} finally {
			Algorithms.closeStream(is);
		}
	}

	private void updateFilesInfo(@NonNull Map<String, RemoteFile> remoteFiles,
								 @NonNull List<SettingsItem> settingsItemList) {
		Map<String, RemoteFile> remoteFilesMap = new HashMap<>(remoteFiles);
		for (SettingsItem settingsItem : settingsItemList) {
			List<RemoteFile> foundRemoteFiles = getItemRemoteFiles(settingsItem, remoteFilesMap);
			for (RemoteFile remoteFile : foundRemoteFiles) {
				settingsItem.setLastModifiedTime(remoteFile.getClienttimems());
				remoteFile.item = settingsItem;
				if (settingsItem instanceof FileSettingsItem) {
					FileSettingsItem fileSettingsItem = (FileSettingsItem) settingsItem;
					fileSettingsItem.setSize(remoteFile.getFilesize());
				}
			}
		}
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void cancel() {
		this.cancelled = true;
	}

	private <T extends ThreadPoolTaskExecutor.Task> ThreadPoolTaskExecutor<T> createExecutor() {
		ThreadPoolTaskExecutor<T> executor = new ThreadPoolTaskExecutor<>(null);
		executor.setInterruptOnError(true);
		return executor;
	}

	private OnDownloadFileListener getOnDownloadFileListener() {
		return new OnDownloadFileListener() {
			@Override
			public void onFileDownloadStarted(@NonNull String type, @NonNull String fileName, int work) {
			}

			@Override
			public void onFileDownloadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork) {
			}

			@Override
			public void onFileDownloadDone(@NonNull String type, @NonNull String fileName, long uploadTime, @Nullable String error) {
			}

			@Override
			public boolean isDownloadCancelled() {
				return isCancelled();
			}
		};
	}

	private class FileDownloadTask extends ThreadPoolTaskExecutor.Task {

		private final File file;
		private final RemoteFile remoteFile;
		private String error;

		public FileDownloadTask(@NonNull File file, @NonNull RemoteFile remoteFile) {
			this.file = file;
			this.remoteFile = remoteFile;
		}

		public String getError() {
			return error;
		}

		@Override
		public Void call() throws Exception {
			error = backupHelper.downloadFile(file, remoteFile, getOnDownloadFileListener());
			return null;
		}
	}

	private class ItemFileDownloadTask extends ThreadPoolTaskExecutor.Task {

		private final OsmandApplication app;
		private final File file;
		private final SettingsItemReader<? extends SettingsItem> reader;

		public ItemFileDownloadTask(@NonNull OsmandApplication app, @NonNull File file,
									@NonNull SettingsItemReader<? extends SettingsItem> reader) {
			this.app = app;
			this.file = file;
			this.reader = reader;
		}

		@Override
		public Void call() {
			downloadItemFile(app, file, reader);
			return null;
		}
	}

	private class ItemFileImportTask extends ThreadPoolTaskExecutor.Task {

		private final RemoteFile remoteFile;
		private final SettingsItem item;
		private final boolean forceReadData;

		public ItemFileImportTask(@NonNull RemoteFile remoteFile, @NonNull SettingsItem item, boolean forceReadData) {
			this.remoteFile = remoteFile;
			this.item = item;
			this.forceReadData = forceReadData;
		}

		@Override
		public Void call() {
			importItemFile(remoteFile, item, forceReadData);
			return null;
		}
	}
}