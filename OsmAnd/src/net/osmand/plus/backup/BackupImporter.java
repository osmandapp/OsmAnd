package net.osmand.plus.backup;

import static net.osmand.plus.backup.BackupHelper.INFO_EXT;
import static net.osmand.plus.backup.BackupUtils.getRemoteFilesSettingsItems;
import static net.osmand.plus.backup.ExportBackupTask.APPROXIMATE_FILE_SIZE_BYTES;
import static net.osmand.plus.settings.backend.backup.SettingsItemType.QUICK_ACTIONS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.OperationLog;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupListeners.OnDownloadFileListener;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.backup.SettingsItemReader;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.SettingsItemsFactory;
import net.osmand.plus.settings.backend.backup.items.CollectionSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.GpxSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

class BackupImporter {

	private static final Log LOG = PlatformUtil.getLog(BackupImporter.class);

	private final BackupHelper backupHelper;
	private final NetworkImportProgressListener listener;

	private boolean cancelled;

	private final AtomicInteger dataProgress = new AtomicInteger(0);
	private final AtomicInteger itemsProgress = new AtomicInteger(0);

	public static class CollectItemsResult {
		public List<SettingsItem> items;
		public List<RemoteFile> remoteFiles;
	}

	public interface NetworkImportProgressListener {
		void itemExportStarted(@NonNull String type, @NonNull String fileName, int work);

		void updateItemProgress(@NonNull String type, @NonNull String fileName, int progress);

		void itemExportDone(@NonNull String type, @NonNull String fileName);

		void updateGeneralProgress(int downloadedItems, int uploadedKb);
	}

	BackupImporter(@NonNull BackupHelper backupHelper, @Nullable NetworkImportProgressListener listener) {
		this.listener = listener;
		this.backupHelper = backupHelper;
	}

	@NonNull
	CollectItemsResult collectItems(@Nullable List<SettingsItem> settingsItems, boolean readItems, boolean restoreDeleted) throws IllegalArgumentException, IOException {
		CollectItemsResult result = new CollectItemsResult();
		StringBuilder error = new StringBuilder();
		OperationLog operationLog = new OperationLog("collectRemoteItems", BackupHelper.DEBUG);
		operationLog.startOperation();
		try {
			backupHelper.downloadFileList((status, message, remoteFiles) -> {
				if (status == BackupHelper.STATUS_SUCCESS) {
					if (settingsItems != null) {
						Map<RemoteFile, SettingsItem> items = getRemoteFilesSettingsItems(settingsItems, remoteFiles, true);
						remoteFiles = new ArrayList<>(items.keySet());
					}
					result.remoteFiles = remoteFiles;
					try {
						result.items = getRemoteItems(remoteFiles, readItems, restoreDeleted);
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

	void importItems(@NonNull List<SettingsItem> items, @NonNull Collection<RemoteFile> remoteFiles,
	                 boolean forceReadData, boolean restoreDeleted) throws IllegalArgumentException {
		if (Algorithms.isEmpty(items)) {
			throw new IllegalArgumentException("No items");
		}
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
			if (item != null) {
				if (forceReadData) {
					tasks.add(new ItemFileImportTask(remoteFile, item, true));
				} else {
					backupHelper.updateFileUploadTime(remoteFile.getType(), remoteFile.getName(), remoteFile.getClienttimems());
				}
			}
		}
		ThreadPoolTaskExecutor<ItemFileImportTask> executor = createExecutor();
		executor.run(tasks);

		if (!restoreDeleted) {
			for (Entry<RemoteFile, SettingsItem> fileItem : remoteFileItems.entrySet()) {
				fileItem.getValue().setLocalModifiedTime(fileItem.getKey().getClienttimems());
				fileItem.getValue().setLastModifiedTime(fileItem.getKey().getClienttimems());
			}
		}
		if (!isCancelled()) {
			backupHelper.updateBackupDownloadTime();
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
				String errorStr = backupHelper.downloadFile(tempFile, remoteFile, getOnDownloadItemFileListener(item));
				boolean error = !Algorithms.isEmpty(errorStr);
				if (PluginsHelper.isDevelopment()) {
					LOG.debug("Temp file downloaded " + errorStr + " " + tempFile.getAbsolutePath());
				}
				if (!error) {
					is = new FileInputStream(tempFile);
					File file = reader.readFromStream(is, tempFile, remoteFile.getName());
					if (forceReadData) {
						if (item instanceof CollectionSettingsItem<?>) {
							((CollectionSettingsItem<?>) item).processDuplicateItems();
						}
						item.apply();
					}
					updateFileM5Digest(remoteFile, item, file);
					updateFileUploadTime(remoteFile, item);
					if (PluginsHelper.isDevelopment()) {
						UploadedFileInfo info = backupHelper.getDbHelper().getUploadedFileInfo(remoteFile.getType(), remoteFile.getName());
						LOG.debug(" importItemFile file info " + info);
					}
				}
				if (tempFile.exists()) {
					tempFile.delete();
				}
				if (error) {
					throw new IOException("Error reading temp item file " + fileName + ": " + errorStr);
				}
			}
			item.applyAdditionalParams(reader);
		} catch (IllegalArgumentException | IOException | UserNotRegisteredException e) {
			item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
			LOG.error("Error reading item data: " + item.getName(), e);
		} catch (Throwable err) {
			LOG.error("Error reading item: " + item.getName(), err);
		} finally {
			Algorithms.closeStream(is);
		}
	}

	private void updateFileM5Digest(@NonNull RemoteFile remoteFile, @NonNull SettingsItem item, @Nullable File file) {
		if (file != null && item instanceof FileSettingsItem fileItem && fileItem.needMd5Digest()) {
			BackupDbHelper dbHelper = backupHelper.getDbHelper();
			UploadedFileInfo fileInfo = dbHelper.getUploadedFileInfo(remoteFile.getType(), remoteFile.getName());
			String lastMd5 = fileInfo != null ? fileInfo.getMd5Digest() : null;

			if (Algorithms.isEmpty(lastMd5) && file != null) {
				FileInputStream is = null;
				try {
					is = new FileInputStream(file);
					String md5Digest = new String(Hex.encodeHex(DigestUtils.md5(is)));
					if (!Algorithms.isEmpty(md5Digest)) {
						backupHelper.updateFileMd5Digest(item.getType().name(), remoteFile.getName(), md5Digest);
					}
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				} finally {
					Algorithms.closeStream(is);
				}
			}
		}
	}

	private void updateFileUploadTime(@NonNull RemoteFile remoteFile, @NonNull SettingsItem item) {
		long time = remoteFile.getUpdatetimems();
		backupHelper.updateFileUploadTime(remoteFile.getType(), remoteFile.getName(), time);
		if (item instanceof FileSettingsItem) {
			OsmandApplication app = backupHelper.getApp();
			String itemFileName = BackupUtils.getFileItemName((FileSettingsItem) item);
			if (app.getAppPath(itemFileName).isDirectory()) {
				backupHelper.updateFileUploadTime(item.getType().name(), itemFileName, time);
			}
		}
	}

	@NonNull
	private List<SettingsItem> getRemoteItems(@NonNull List<RemoteFile> remoteFiles, boolean readItems, boolean restoreDeleted) throws IllegalArgumentException, IOException {
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

			List<RemoteFile> uniqueRemoteFiles = new ArrayList<>();
			Map<String, RemoteFile> deletedRemoteFilesMap = new HashMap<>();
			collectUniqueAndDeletedRemoteFiles(remoteFiles, uniqueRemoteFiles, deletedRemoteFilesMap);
			operationLog.log("build uniqueRemoteFiles");

			Set<String> remoteInfoNames = new HashSet<>();
			List<RemoteFile> remoteInfoFiles = new ArrayList<>();
			Map<File, RemoteFile> remoteInfoFilesMap = new HashMap<>();
			Map<String, RemoteFile> remoteItemFilesMap = new HashMap<>();

			processUniqueRemoteFiles(uniqueRemoteFiles, remoteItemFilesMap, remoteInfoFilesMap, remoteInfoNames, remoteInfoFiles, readItems);
			operationLog.log("build maps");

			List<RemoteFile> noInfoRemoteItemFiles = new ArrayList<>();
			collectNoInfoRemoteItemFiles(noInfoRemoteItemFiles, remoteItemFilesMap, remoteInfoNames);
			operationLog.log("build noInfoRemoteItemFiles");

			if (readItems || !Algorithms.isEmpty(remoteInfoFilesMap)) {
				generateItemsJson(itemsJson, remoteInfoFilesMap, remoteInfoFiles);
			}
			if (!readItems) {
				generateItemsJson(itemsJson, remoteInfoFiles);
			}
			addRemoteFilesToJson(itemsJson, noInfoRemoteItemFiles);
			operationLog.log("generateItemsJson");

			SettingsItemsFactory itemsFactory = new SettingsItemsFactory(backupHelper.getApp(), json);
			operationLog.log("create setting items");
			List<SettingsItem> settingsItemList = itemsFactory.getItems();
			if (settingsItemList.isEmpty()) {
				return Collections.emptyList();
			}
			updateFilesInfo(remoteItemFilesMap, settingsItemList, restoreDeleted);
			updateFilesInfo(deletedRemoteFilesMap, settingsItemList, restoreDeleted);
			items.addAll(settingsItemList);
			operationLog.log("updateFilesInfo");
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

	private void collectUniqueAndDeletedRemoteFiles(@NonNull List<RemoteFile> remoteFiles,
			@NonNull List<RemoteFile> uniqueRemoteFiles,
			@NonNull Map<String, RemoteFile> deletedRemoteFiles) {
		Set<String> uniqueFileIds = new TreeSet<>();
		for (RemoteFile rf : remoteFiles) {
			String fileId = rf.getTypeNamePath();
			if (rf.isDeleted()) {
				String fileName = rf.getTypeNamePath();
				if (!fileName.endsWith(INFO_EXT) && !deletedRemoteFiles.containsKey(fileName)) {
					deletedRemoteFiles.put(fileName, rf);
				}
			} else if (uniqueFileIds.add(fileId)) {
				uniqueRemoteFiles.add(rf);
			}
		}
	}

	private void processUniqueRemoteFiles(@NonNull List<RemoteFile> uniqueRemoteFiles,
	                                      @NonNull Map<String, RemoteFile> remoteItemFilesMap,
	                                      @NonNull Map<File, RemoteFile> remoteInfoFilesMap,
	                                      @NonNull Set<String> remoteInfoNames,
	                                      @NonNull List<RemoteFile> remoteInfoFiles,
	                                      boolean readItems) {
		File tempDir = FileUtils.getTempDir(backupHelper.getApp());
		Map<String, UploadedFileInfo> infoMap = backupHelper.getDbHelper().getUploadedFileInfoMap();
		BackupInfo backupInfo = backupHelper.getBackup().getBackupInfo();
		List<RemoteFile> filesToDelete = backupInfo != null ? backupInfo.filesToDelete : Collections.emptyList();
		for (RemoteFile remoteFile : uniqueRemoteFiles) {
			String fileName = remoteFile.getTypeNamePath();
			if (fileName.endsWith(INFO_EXT)) {
				boolean delete = false;
				String origFileName = remoteFile.getName().substring(0, remoteFile.getName().length() - INFO_EXT.length());
				for (RemoteFile file : filesToDelete) {
					if (file.getName().equals(origFileName)) {
						delete = true;
						break;
					}
				}
				UploadedFileInfo fileInfo = infoMap.get(remoteFile.getType() + "___" + origFileName);
				long uploadTime = fileInfo != null ? fileInfo.getUploadTime() : 0;
				if (shouldDownloadOnCollecting(remoteFile, readItems)
						&& (uploadTime != remoteFile.getUpdatetimems() || delete)) {
					remoteInfoFilesMap.put(new File(tempDir, fileName), remoteFile);
				}
				String itemFileName = fileName.substring(0, fileName.length() - INFO_EXT.length());
				remoteInfoNames.add(itemFileName);
				remoteInfoFiles.add(remoteFile);
			} else if (!remoteItemFilesMap.containsKey(fileName)) {
				remoteItemFilesMap.put(fileName, remoteFile);
			}
		}
	}

	private void collectNoInfoRemoteItemFiles(@NonNull List<RemoteFile> noInfoRemoteItemFiles,
	                                          @NonNull Map<String, RemoteFile> remoteItemFilesMap,
	                                          @NonNull Set<String> remoteInfoNames) {
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
	}

	@NonNull
	private List<RemoteFile> getItemRemoteFiles(@NonNull SettingsItem item, @NonNull Map<String, RemoteFile> remoteFiles) {
		List<RemoteFile> res = new ArrayList<>();
		String fileName = item.getFileName();
		if (!Algorithms.isEmpty(fileName)) {
			if (fileName.charAt(0) != '/') {
				fileName = "/" + fileName;
			}
			if (item instanceof GpxSettingsItem gpxItem) {
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

	private void generateItemsJson(@NonNull JSONArray itemsJson, @NonNull List<RemoteFile> remoteInfoFiles) throws JSONException {
		for (RemoteFile remoteFile : remoteInfoFiles) {
			itemsJson.put(generateItemJson(remoteFile));
		}
	}

	@NonNull
	private JSONObject generateItemJson(@NonNull RemoteFile remoteFile) throws JSONException {
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
		return itemJson;
	}

	private void generateItemsJson(@NonNull JSONArray itemsJson,
	                               @NonNull Map<File, RemoteFile> remoteInfoFilesMap,
	                               @NonNull List<RemoteFile> remoteInfoFiles) throws JSONException, IOException {
		List<FileDownloadTask> tasks = new ArrayList<>();
		for (Entry<File, RemoteFile> fileEntry : remoteInfoFilesMap.entrySet()) {
			RemoteFile remoteFile = fileEntry.getValue();
			tasks.add(new FileDownloadTask(fileEntry.getKey(), remoteFile));
			remoteInfoFiles.remove(remoteFile);
		}
		ThreadPoolTaskExecutor<FileDownloadTask> executor = createExecutor();
		executor.run(tasks);

		for (FileDownloadTask task : tasks) {
			if (Algorithms.isEmpty(task.error)) {
				String jsonStr = Algorithms.getFileAsString(task.file);
				if (!Algorithms.isEmpty(jsonStr)) {
					itemsJson.put(new JSONObject(jsonStr));
				} else {
					throw new IOException("Error reading item info: " + task.file.getName());
				}
			} else {
				LOG.error("Error reading item info: " + task.file.getName() + " error " + task.error);
			}
		}
	}

	private boolean shouldDownloadOnCollecting(@NonNull RemoteFile remoteFile, boolean defValue) {
		String type = remoteFile.getType();
		return defValue || CollectionUtils.equalsToAny(type, QUICK_ACTIONS.name());
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
		}
		for (Entry<File, RemoteFile> entry : remoteFilesForDownload.entrySet()) {
			File tempFile = entry.getKey();
			if (tempFile.exists()) {
				tempFile.delete();
			}
		}
		if (hasDownloadErrors) {
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
			reader.readFromStream(is, tempFile, item.getFileName());
			item.applyAdditionalParams(reader);
		} catch (IllegalArgumentException | IOException e) {
			item.getWarnings().add(app.getString(R.string.settings_item_read_error, item.getName()));
			LOG.error("Error reading item data: " + item.getName(), e);
		} finally {
			Algorithms.closeStream(is);
		}
	}

	private void updateFilesInfo(@NonNull Map<String, RemoteFile> remoteFiles,
	                             @NonNull List<SettingsItem> settingsItemList,
	                             boolean restoreDeleted) {
		Map<String, RemoteFile> remoteFilesMap = new HashMap<>(remoteFiles);
		for (SettingsItem settingsItem : settingsItemList) {
			List<RemoteFile> foundRemoteFiles = getItemRemoteFiles(settingsItem, remoteFilesMap);
			for (RemoteFile remoteFile : foundRemoteFiles) {
				remoteFile.item = settingsItem;

				if (!remoteFile.isDeleted()) {
					if (!restoreDeleted) {
						settingsItem.setLastModifiedTime(remoteFile.getClienttimems());
					}
					if (settingsItem instanceof FileSettingsItem item) {
						item.setSize(remoteFile.getFilesize());
					}
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
				if (listener != null) {
					listener.itemExportStarted(type, fileName, work);
				}
			}

			@Override
			public void onFileDownloadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork) {
				int p = dataProgress.addAndGet(deltaWork);
				if (listener != null) {
					listener.updateItemProgress(type, fileName, progress);
					listener.updateGeneralProgress(itemsProgress.get(), p);
				}
			}

			@Override
			public void onFileDownloadDone(@NonNull String type, @NonNull String fileName, @Nullable String error) {
				itemsProgress.addAndGet(1);
				dataProgress.addAndGet(APPROXIMATE_FILE_SIZE_BYTES / 1024);
				if (listener != null) {
					listener.itemExportDone(type, fileName);
					listener.updateGeneralProgress(itemsProgress.get(), dataProgress.get());
				}
			}

			@Override
			public boolean isDownloadCancelled() {
				return isCancelled();
			}
		};
	}

	private OnDownloadFileListener getOnDownloadItemFileListener(@NonNull SettingsItem item) {
		String itemFileName = BackupUtils.getItemFileName(item);
		return new OnDownloadFileListener() {
			@Override
			public void onFileDownloadStarted(@NonNull String type, @NonNull String fileName, int work) {
				if (listener != null) {
					listener.itemExportStarted(type, itemFileName, work);
				}
			}

			@Override
			public void onFileDownloadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork) {
				int p = dataProgress.addAndGet(deltaWork);
				if (listener != null) {
					listener.updateItemProgress(type, itemFileName, progress);
					listener.updateGeneralProgress(itemsProgress.get(), p);
				}
			}

			@Override
			public void onFileDownloadDone(@NonNull String type, @NonNull String fileName, @Nullable String error) {
				itemsProgress.addAndGet(1);
				if (listener != null) {
					listener.itemExportDone(type, itemFileName);
					listener.updateGeneralProgress(itemsProgress.get(), dataProgress.get());
				}
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

		@NonNull
		@Override
		public String toString() {
			return "FileDownloadTask{" +
					"file=" + file.getAbsolutePath() +
					", remoteFile=" + remoteFile +
					", error=" + error +
					" }";
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
