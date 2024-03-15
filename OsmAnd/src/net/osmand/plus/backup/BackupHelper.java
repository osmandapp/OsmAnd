package net.osmand.plus.backup;

import static net.osmand.plus.backup.ExportBackupTask.APPROXIMATE_FILE_SIZE_BYTES;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.OperationLog;
import net.osmand.PlatformUtil;
import net.osmand.StreamWriter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.BackupExecutor.BackupExecutorListener;
import net.osmand.plus.backup.BackupListeners.OnCollectLocalFilesListener;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.BackupListeners.OnDownloadFileListListener;
import net.osmand.plus.backup.BackupListeners.OnDownloadFileListener;
import net.osmand.plus.backup.BackupListeners.OnGenerateBackupInfoListener;
import net.osmand.plus.backup.BackupListeners.OnUpdateSubscriptionListener;
import net.osmand.plus.backup.BackupListeners.OnUploadFileListener;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.commands.CheckCodeCommand;
import net.osmand.plus.backup.commands.DeleteAccountCommand;
import net.osmand.plus.backup.commands.DeleteAllFilesCommand;
import net.osmand.plus.backup.commands.DeleteFilesCommand;
import net.osmand.plus.backup.commands.DeleteOldFilesCommand;
import net.osmand.plus.backup.commands.RegisterDeviceCommand;
import net.osmand.plus.backup.commands.RegisterUserCommand;
import net.osmand.plus.backup.commands.SendCodeCommand;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.plus.settings.backend.backup.SettingsItemType;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.CollectionSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem.FileSubtype;
import net.osmand.plus.settings.backend.backup.items.GpxSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.backend.backup.items.StreamSettingsItem;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidNetworkUtils.NetworkResult;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class BackupHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final BackupDbHelper dbHelper;

	public static final Log LOG = PlatformUtil.getLog(BackupHelper.class);
	public static boolean DEBUG;

	private final BackupExecutor executor;

	public static final String INFO_EXT = ".info";

	public static final String SERVER_URL = "https://osmand.net";

	public static final String USER_REGISTER_URL = SERVER_URL + "/userdata/user-register";
	public static final String DEVICE_REGISTER_URL = SERVER_URL + "/userdata/device-register";
	public static final String UPDATE_ORDER_ID_URL = SERVER_URL + "/userdata/user-update-orderid";
	public static final String UPLOAD_FILE_URL = SERVER_URL + "/userdata/upload-file";
	public static final String LIST_FILES_URL = SERVER_URL + "/userdata/list-files";
	public static final String DOWNLOAD_FILE_URL = SERVER_URL + "/userdata/download-file";
	public static final String DELETE_FILE_URL = SERVER_URL + "/userdata/delete-file";
	public static final String DELETE_FILE_VERSION_URL = SERVER_URL + "/userdata/delete-file-version";
	public static final String ACCOUNT_DELETE_URL = SERVER_URL + "/userdata/delete-account";
	public static final String SEND_CODE_URL = SERVER_URL + "/userdata/send-code";
	public static final String CHECK_CODE_URL = SERVER_URL + "/userdata/auth/confirm-code";

	private static final String BACKUP_TYPE_PREFIX = "backup_type_";
	private static final String VERSION_HISTORY_PREFIX = "save_version_history_";

	public static final int STATUS_SUCCESS = 0;
	public static final int STATUS_PARSE_JSON_ERROR = 1;
	public static final int STATUS_EMPTY_RESPONSE_ERROR = 2;
	public static final int STATUS_SERVER_ERROR = 3;
	public static final int STATUS_NO_ORDER_ID_ERROR = 4;
	public static final int STATUS_EXECUTION_ERROR = 5;
	public static final int STATUS_SERVER_TEMPORALLY_UNAVAILABLE_ERROR = 6;

	public static final int SERVER_ERROR_CODE_EMAIL_IS_INVALID = 101;
	public static final int SERVER_ERROR_CODE_NO_VALID_SUBSCRIPTION = 102;
	public static final int SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED = 103;
	public static final int SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED = 104;
	public static final int SERVER_ERROR_CODE_PROVIDED_TOKEN_IS_NOT_VALID = 105;
	public static final int SERVER_ERROR_CODE_FILE_NOT_AVAILABLE = 106;
	public static final int SERVER_ERROR_CODE_GZIP_ONLY_SUPPORTED_UPLOAD = 107;
	public static final int SERVER_ERROR_CODE_SIZE_OF_SUPPORTED_BOX_IS_EXCEEDED = 108;
	public static final int SERVER_ERROR_CODE_SUBSCRIPTION_WAS_USED_FOR_ANOTHER_ACCOUNT = 109;
	public static final int SERVER_ERROR_CODE_SUBSCRIPTION_WAS_EXPIRED_OR_NOT_PRESENT = 110;
	public static final int SERVER_ERROR_CODE_USER_IS_ALREADY_REGISTERED = 111;

	private PrepareBackupTask prepareBackupTask;
	private PrepareBackupResult backup = new PrepareBackupResult();
	private final List<OnPrepareBackupListener> prepareBackupListeners = new ArrayList<>();

	private final BackupListeners backupListeners = new BackupListeners();

	public BackupHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.executor = new BackupExecutor(app);
		this.settings = app.getSettings();
		this.dbHelper = new BackupDbHelper(app);
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public BackupExecutor getExecutor() {
		return executor;
	}

	@NonNull
	public BackupDbHelper getDbHelper() {
		return dbHelper;
	}

	@NonNull
	public BackupListeners getBackupListeners() {
		return backupListeners;
	}

	@NonNull
	public PrepareBackupResult getBackup() {
		return backup;
	}

	void setBackup(@NonNull PrepareBackupResult backup) {
		this.backup = backup;
	}

	public boolean isBusy() {
		return executor.getState() == BackupExecutor.State.BUSY;
	}

	public void addBackupListener(@NonNull BackupExecutorListener listener) {
		executor.addListener(listener);
	}

	public void removeBackupListener(@NonNull BackupExecutorListener listener) {
		executor.removeListener(listener);
	}

	public static void setLastModifiedTime(@NonNull Context ctx, @NonNull String name) {
		setLastModifiedTime(ctx, name, System.currentTimeMillis());
	}

	public static void setLastModifiedTime(@NonNull Context ctx, @NonNull String name, long lastModifiedTime) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		app.getBackupHelper().getDbHelper().setLastModifiedTime(name, lastModifiedTime);
	}

	public static long getLastModifiedTime(@NonNull Context ctx, @NonNull String name) {
		OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
		return app.getBackupHelper().getDbHelper().getLastModifiedTime(name);
	}

	public String getAndroidId() {
		return app.getUserAndroidId();
	}

	public static boolean isTokenValid(@NonNull String token) {
		return token.matches("[0-9]+");
	}

	@NonNull
	public static List<SettingsItem> getItemsForRestore(@Nullable BackupInfo info, @NonNull List<SettingsItem> settingsItems) {
		List<SettingsItem> itemsForRestore = new ArrayList<>();
		if (info != null) {
			Map<RemoteFile, SettingsItem> restoreItems = getRemoteFilesSettingsItems(settingsItems, info.filteredFilesToDownload, false);
			for (SettingsItem restoreItem : restoreItems.values()) {
				if (restoreItem instanceof CollectionSettingsItem) {
					CollectionSettingsItem<?> settingsItem = (CollectionSettingsItem<?>) restoreItem;
					settingsItem.processDuplicateItems();
					settingsItem.setShouldReplace(true);
				}
				itemsForRestore.add(restoreItem);
			}
		}
		return itemsForRestore;
	}

	@NonNull
	public static Map<RemoteFile, SettingsItem> getItemsMapForRestore(@Nullable BackupInfo info, @NonNull List<SettingsItem> settingsItems) {
		Map<RemoteFile, SettingsItem> itemsForRestore = new HashMap<>();
		if (info != null) {
			itemsForRestore.putAll(getRemoteFilesSettingsItems(settingsItems, info.filteredFilesToDownload, false));
		}
		return itemsForRestore;
	}

	@NonNull
	public static Map<RemoteFile, SettingsItem> getRemoteFilesSettingsItems(@NonNull List<SettingsItem> items,
	                                                                        @NonNull List<RemoteFile> remoteFiles,
	                                                                        boolean infoFiles) {
		Map<RemoteFile, SettingsItem> res = new HashMap<>();
		List<RemoteFile> files = new ArrayList<>(remoteFiles);
		for (SettingsItem item : items) {
			List<RemoteFile> processedFiles = new ArrayList<>();
			for (RemoteFile file : files) {
				String type = file.getType();
				String name = file.getName();
				if (infoFiles && name.endsWith(INFO_EXT)) {
					name = name.substring(0, name.length() - INFO_EXT.length());
				}
				if (applyItem(item, type, name)) {
					res.put(file, item);
					processedFiles.add(file);
				}
			}
			files.removeAll(processedFiles);
		}
		return res;
	}

	@Nullable
	public String getOrderId() {
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		InAppSubscription purchasedSubscription = purchaseHelper.getAnyPurchasedOsmAndProSubscription();
		return purchasedSubscription != null ? purchasedSubscription.getOrderId() : null;
	}

	@Nullable
	private Map<String, LocalFile> getPreparedLocalFiles() {
		if (isBackupPreparing()) {
			PrepareBackupResult backupResult = prepareBackupTask.getResult();
			return backupResult != null ? backupResult.getLocalFiles() : null;
		}
		return null;
	}

	public String getDeviceId() {
		return settings.BACKUP_DEVICE_ID.get();
	}

	public String getAccessToken() {
		return settings.BACKUP_ACCESS_TOKEN.get();
	}

	public String getEmail() {
		return settings.BACKUP_USER_EMAIL.get();
	}

	public boolean isRegistered() {
		return !Algorithms.isEmpty(getDeviceId()) && !Algorithms.isEmpty(getAccessToken());
	}

	private void checkRegistered() throws UserNotRegisteredException {
		if (Algorithms.isEmpty(getDeviceId()) || Algorithms.isEmpty(getAccessToken())) {
			throw new UserNotRegisteredException();
		}
	}

	public void updateFileUploadTime(@NonNull String type, @NonNull String fileName, long updateTime) {
		dbHelper.updateFileUploadTime(type, fileName, updateTime);
	}

	public void updateFileMd5Digest(@NonNull String type, @NonNull String fileName, @NonNull String md5Hex) {
		dbHelper.updateFileMd5Digest(type, fileName, md5Hex);
	}

	public void updateBackupUploadTime() {
		settings.BACKUP_LAST_UPLOADED_TIME.set(System.currentTimeMillis() + 1);
	}

	public void logout() {
		settings.BACKUP_PROMOCODE.resetToDefault();
		settings.BACKUP_DEVICE_ID.resetToDefault();
		settings.BACKUP_ACCESS_TOKEN.resetToDefault();
	}

	@NonNull
	private List<ExportType> getEnabledExportTypes() {
		List<ExportType> result = new ArrayList<>();
		for (ExportType exportType : ExportType.enabledValues()) {
			if (getBackupTypePref(exportType).get()) {
				result.add(exportType);
			}
		}
		return result;
	}

	public CommonPreference<Boolean> getBackupTypePref(@NonNull ExportType exportType) {
		return getBackupTypePref(app, exportType);
	}

	public CommonPreference<Boolean> getVersionHistoryTypePref(@NonNull ExportType exportType) {
		return getVersionHistoryTypePref(app, exportType);
	}

	public static CommonPreference<Boolean> getBackupTypePref(@NonNull OsmandApplication app, @NonNull ExportType type) {
		return app.getSettings().registerBooleanPreference(BACKUP_TYPE_PREFIX + type.name(), true).makeGlobal().makeShared();
	}

	public static CommonPreference<Boolean> getVersionHistoryTypePref(@NonNull OsmandApplication app, @NonNull ExportType exportType) {
		return app.getSettings().registerBooleanPreference(VERSION_HISTORY_PREFIX + exportType.name(), true).makeGlobal().makeShared();
	}

	public static boolean applyItem(@NonNull SettingsItem item, @NonNull String type, @NonNull String name) {
		String itemFileName = getItemFileName(item);
		if (item.getType().name().equals(type)) {
			if (name.equals(itemFileName)) {
				return true;
			} else if (item instanceof FileSettingsItem) {
				FileSettingsItem fileItem = (FileSettingsItem) item;
				if (name.startsWith(fileItem.getSubtype().getSubtypeFolder())) {
					if (fileItem.getFile().isDirectory() && !itemFileName.endsWith("/")) {
						return name.startsWith(itemFileName + "/");
					} else {
						return name.startsWith(itemFileName);
					}
				}
			}
		}
		return false;
	}

	@NonNull
	public static String getItemFileName(@NonNull SettingsItem item) {
		String fileName;
		if (item instanceof FileSettingsItem) {
			FileSettingsItem fileItem = (FileSettingsItem) item;
			fileName = getFileItemName(fileItem);
		} else {
			fileName = item.getFileName();
			if (Algorithms.isEmpty(fileName)) {
				fileName = item.getDefaultFileName();
			}
		}
		if (!Algorithms.isEmpty(fileName) && fileName.charAt(0) == '/') {
			fileName = fileName.substring(1);
		}
		return fileName;
	}

	@NonNull
	public static String getFileItemName(@NonNull FileSettingsItem fileSettingsItem) {
		return getFileItemName(null, fileSettingsItem);
	}

	@NonNull
	public static String getFileItemName(@Nullable File file, @NonNull FileSettingsItem fileSettingsItem) {
		String subtypeFolder = fileSettingsItem.getSubtype().getSubtypeFolder();
		String fileName;
		if (file == null) {
			file = fileSettingsItem.getFile();
		}
		if (Algorithms.isEmpty(subtypeFolder)) {
			fileName = file.getName();
		} else if (fileSettingsItem instanceof GpxSettingsItem) {
			fileName = file.getPath().substring(file.getPath().indexOf(subtypeFolder) + subtypeFolder.length());
		} else {
			fileName = file.getPath().substring(file.getPath().indexOf(subtypeFolder) - 1);
		}
		if (!Algorithms.isEmpty(fileName) && fileName.charAt(0) == '/') {
			fileName = fileName.substring(1);
		}
		return fileName;
	}

	public static boolean isLimitedFilesCollectionItem(@NonNull FileSettingsItem item) {
		return item.getSubtype() == FileSubtype.VOICE;
	}

	@NonNull
	public List<File> collectItemFilesForUpload(@NonNull FileSettingsItem item) {
		List<File> filesToUpload = new ArrayList<>();
		BackupInfo info = getBackup().getBackupInfo();
		if (!isLimitedFilesCollectionItem(item) && info != null &&
				(!Algorithms.isEmpty(info.filesToUpload)
						|| !Algorithms.isEmpty(info.filesToMerge)
						|| !Algorithms.isEmpty(info.filesToDownload))) {
			for (LocalFile localFile : info.filesToUpload) {
				File file = localFile.file;
				if (item.equals(localFile.item) && file != null) {
					filesToUpload.add(file);
				}
			}
			for (Pair<LocalFile, RemoteFile> pair : info.filesToMerge) {
				LocalFile localFile = pair.first;
				File file = localFile.file;
				if (item.equals(localFile.item) && file != null) {
					filesToUpload.add(file);
				}
			}
			for (RemoteFile remoteFile : info.filesToDownload) {
				if (remoteFile.item instanceof FileSettingsItem) {
					String fileName = remoteFile.item.getFileName();
					if (fileName != null && item.applyFileName(fileName)) {
						filesToUpload.add(((FileSettingsItem) remoteFile.item).getFile());
					}
				}
			}
		} else {
			FileUtils.collectDirFiles(item.getFile(), filesToUpload);
		}
		return filesToUpload;
	}

	public void registerUser(@NonNull String email, @Nullable String promoCode, boolean login) {
		executor.runCommand(new RegisterUserCommand(this, login, email, promoCode));
	}

	public void registerDevice(@NonNull String token) {
		executor.runCommand(new RegisterDeviceCommand(this, token));
	}

	void checkSubscriptions(@Nullable OnUpdateSubscriptionListener listener) {
		boolean subscriptionActive = false;
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		if (purchaseHelper != null) {
			OperationLog operationLog = new OperationLog("checkSubscriptions", DEBUG);
			String error = "";
			try {
				subscriptionActive = purchaseHelper.checkBackupSubscriptions();
			} catch (Exception e) {
				error = e.getMessage();
			}
			operationLog.finishOperation(subscriptionActive + " " + error);
		}
		if (subscriptionActive) {
			if (listener != null) {
				listener.onUpdateSubscription(STATUS_SUCCESS, "Subscriptions have been checked successfully", null);
			}
		} else {
			updateOrderId(listener);
		}
	}

	public void updateOrderId(@Nullable OnUpdateSubscriptionListener listener) {
		Map<String, String> params = new HashMap<>();
		String orderId = getOrderId();
		if (Algorithms.isEmpty(orderId)) {
			return;
		}
		params.put("email", getEmail());
		params.put("orderid", orderId);
		String androidId = getAndroidId();
		if (!Algorithms.isEmpty(androidId)) {
			params.put("deviceid", androidId);
		}
		OperationLog operationLog = new OperationLog("updateOrderId", DEBUG);
		AndroidNetworkUtils.sendRequest(app, UPDATE_ORDER_ID_URL, params, "Update order id", false, true, (resultJson, error, resultCode) -> {
			int status;
			String message;
			if (!Algorithms.isEmpty(error)) {
				message = "Update order id error: " + new BackupError(error);
				status = STATUS_SERVER_ERROR;
			} else if (!Algorithms.isEmpty(resultJson)) {
				try {
					JSONObject result = new JSONObject(resultJson);
					if (result.has("status") && "ok".equals(result.getString("status"))) {
						message = "Order id have been updated successfully";
						status = STATUS_SUCCESS;
					} else {
						message = "Update order id error: unknown";
						status = STATUS_SERVER_ERROR;
					}
				} catch (JSONException e) {
					message = "Update order id error: json parsing";
					status = STATUS_PARSE_JSON_ERROR;
				}
			} else {
				message = "Update order id error: empty response";
				status = STATUS_EMPTY_RESPONSE_ERROR;
			}
			if (listener != null) {
				listener.onUpdateSubscription(status, message, error);
			}
			operationLog.finishOperation(status + " " + message);
		});
	}

	public boolean isBackupPreparing() {
		return prepareBackupTask != null;
	}

	public boolean prepareBackup() {
		if (isBackupPreparing()) {
			return false;
		}
		PrepareBackupTask prepareBackupTask = new PrepareBackupTask(app, new OnPrepareBackupListener() {
			@Override
			public void onBackupPreparing() {
				for (OnPrepareBackupListener listener : prepareBackupListeners) {
					listener.onBackupPreparing();
				}
			}

			@Override
			public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
				setPrepareBackupTask(null);
				for (OnPrepareBackupListener listener : prepareBackupListeners) {
					listener.onBackupPrepared(backupResult);
				}
			}
		});
		setPrepareBackupTask(prepareBackupTask);
		prepareBackupTask.prepare();
		return true;
	}

	public void addPrepareBackupListener(@NonNull OnPrepareBackupListener listener) {
		prepareBackupListeners.add(listener);
		if (isBackupPreparing()) {
			listener.onBackupPreparing();
		}
	}

	public void removePrepareBackupListener(@NonNull OnPrepareBackupListener listener) {
		prepareBackupListeners.remove(listener);
	}

	private void setPrepareBackupTask(@Nullable PrepareBackupTask task) {
		this.prepareBackupTask = task;
	}

	@Nullable
	String uploadFile(@NonNull String fileName, @NonNull String type, long lastModifiedTime,
	                  @NonNull StreamWriter streamWriter, @Nullable OnUploadFileListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("name", fileName);
		params.put("type", type);
		params.put("clienttime", String.valueOf(lastModifiedTime));

		Map<String, String> headers = new HashMap<>();
		headers.put("Accept-Encoding", "deflate, gzip");

		OperationLog operationLog = new OperationLog("uploadFile", DEBUG);
		operationLog.startOperation(type + " " + fileName);
		NetworkResult networkResult = AndroidNetworkUtils.uploadFile(UPLOAD_FILE_URL, streamWriter, fileName, true, params, headers,
				new AbstractProgress() {

					private ProgressHelper progressHelper;

					@Override
					public void startWork(int work) {
						progressHelper = new ProgressHelper(() -> {
							if (listener != null) {
								int progress = progressHelper.getLastKnownProgress();
								int addedDeltaProgress = progressHelper.getLastAddedDeltaProgress();
								listener.onFileUploadProgress(type, fileName, progress, addedDeltaProgress);
							}
						});
						if (listener != null) {
							progressHelper.onStartWork(work);
							listener.onFileUploadStarted(type, fileName, progressHelper.getTotalWork());
						}
					}

					@Override
					public void progress(int deltaWork) {
						if (listener != null) {
							progressHelper.onProgress(deltaWork);
						}
					}

					@Override
					public boolean isInterrupted() {
						if (listener != null) {
							return listener.isUploadCancelled();
						}
						return super.isInterrupted();
					}
				});
		String status = "";
		long uploadTime = 0;
		String response = networkResult.getResponse();
		if (!Algorithms.isEmpty(response)) {
			try {
				JSONObject responseObj = new JSONObject(response);
				status = responseObj.optString("status", "");
				uploadTime = Long.parseLong(responseObj.optString("updatetime", "0"));
			} catch (JSONException | NumberFormatException e) {
				LOG.error("Cannot obtain updatetime after upload. Server response: " + response);
			}
		}
		String error = networkResult.getError();
		if (error == null && status.equals("ok")) {
			updateFileUploadTime(type, fileName, uploadTime);
		}
		if (listener != null) {
			listener.onFileUploadDone(type, fileName, uploadTime, error);
		}
		operationLog.finishOperation(type + " " + fileName + (error != null ? " Error: " + new BackupError(error) : " OK"));
		return error;
	}

	public void deleteFiles(@NonNull List<RemoteFile> remoteFiles, boolean byVersion,
	                        @Nullable OnDeleteFilesListener listener) throws UserNotRegisteredException {
		checkRegistered();
		executor.runCommand(new DeleteFilesCommand(this, remoteFiles, byVersion, listener));
	}

	void deleteFilesSync(@NonNull List<RemoteFile> remoteFiles, boolean byVersion,
	                     @Nullable Executor executor, @Nullable OnDeleteFilesListener listener) throws UserNotRegisteredException {
		checkRegistered();
		try {
			new DeleteFilesCommand(this, remoteFiles, byVersion, listener)
					.executeOnExecutor(executor == null ? this.executor : executor).get();
		} catch (ExecutionException | InterruptedException e) {
			if (listener != null) {
				app.runInUIThread(() -> listener.onFilesDeleteError(STATUS_EXECUTION_ERROR, "Execution error while deleting files"));
			}
		}
	}

	void downloadFileList(@Nullable OnDownloadFileListListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("allVersions", "true");
		OperationLog operationLog = new OperationLog("downloadFileList", DEBUG);
		operationLog.startOperation();
		AndroidNetworkUtils.sendRequest(app, LIST_FILES_URL, params, "Download file list", false, false,
				(resultJson, error, resultCode) -> {
					int status;
					String message;
					List<RemoteFile> remoteFiles = new ArrayList<>();
					if (!Algorithms.isEmpty(error)) {
						status = STATUS_SERVER_ERROR;
						message = "Download file list error: " + new BackupError(error);
					} else if (!Algorithms.isEmpty(resultJson)) {
						try {
							JSONObject res = new JSONObject(resultJson);
							String totalZipSize = res.getString("totalZipSize");
							String totalFiles = res.getString("totalFiles");
							String totalFileVersions = res.getString("totalFileVersions");
							JSONArray allFiles = res.getJSONArray("allFiles");
							for (int i = 0; i < allFiles.length(); i++) {
								remoteFiles.add(new RemoteFile(allFiles.getJSONObject(i)));
							}
							status = STATUS_SUCCESS;
							message = "Total files: " + totalFiles + " " +
									"Total zip size: " + AndroidUtils.formatSize(app, Long.parseLong(totalZipSize)) + " " +
									"Total file versions: " + totalFileVersions;
						} catch (JSONException e) {
							status = STATUS_PARSE_JSON_ERROR;
							message = "Download file list error: json parsing";
						}
					} else {
						status = STATUS_EMPTY_RESPONSE_ERROR;
						message = "Download file list error: empty response";
					}
					operationLog.finishOperation("(" + status + "): " + message);
					if (listener != null) {
						listener.onDownloadFileList(status, message, remoteFiles);
					}
				});
	}

	public void deleteAllFiles(@Nullable List<ExportType> types) throws UserNotRegisteredException {
		checkRegistered();
		executor.runCommand(new DeleteAllFilesCommand(this, types));
	}

	public void deleteOldFiles(@Nullable List<ExportType> types) throws UserNotRegisteredException {
		checkRegistered();
		executor.runCommand(new DeleteOldFilesCommand(this, types));
	}

	public void deleteAccount(@NonNull String email, @NonNull String token) throws UserNotRegisteredException {
		checkRegistered();
		executor.runCommand(new DeleteAccountCommand(this, email, token));
	}

	public void checkCode(@NonNull String email, @NonNull String token) throws UserNotRegisteredException {
		checkRegistered();
		executor.runCommand(new CheckCodeCommand(this, email, token));
	}

	public void sendCode(@NonNull String email, @NonNull String action) throws UserNotRegisteredException {
		checkRegistered();
		executor.runCommand(new SendCodeCommand(this, email, action));
	}

	public long calculateFileSize(@NonNull RemoteFile remoteFile) {
		long size = remoteFile.getFilesize();
		if (remoteFile.item != null && remoteFile.item.getType() == SettingsItemType.FILE) {
			FileSettingsItem fileItem = (FileSettingsItem) remoteFile.item;
			String fileName = fileItem.getFileName();
			if (fileItem.getSubtype() == FileSubtype.OBF_MAP && fileName != null) {
				File file = app.getResourceManager().getIndexFiles().get(fileName.toLowerCase());
				if (file != null) {
					size = file.length();
				}
			}
		}
		return size + APPROXIMATE_FILE_SIZE_BYTES;
	}

	@NonNull
	String downloadFile(@NonNull File file, @NonNull RemoteFile remoteFile,
	                    @Nullable OnDownloadFileListener listener) throws UserNotRegisteredException {
		checkRegistered();

		OperationLog operationLog = new OperationLog("downloadFile " + file.getName(), DEBUG);
		String error;
		String type = remoteFile.getType();
		String fileName = remoteFile.getName();
		try {
			Map<String, String> params = new HashMap<>();
			params.put("deviceid", getDeviceId());
			params.put("accessToken", getAccessToken());
			params.put("name", fileName);
			params.put("type", type);
			params.put("updatetime", String.valueOf(remoteFile.getUpdatetimems()));

			StringBuilder builder = new StringBuilder(DOWNLOAD_FILE_URL);
			boolean firstParam = true;
			for (Entry<String, String> entry : params.entrySet()) {
				builder.append(firstParam ? "?" : "&").append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				firstParam = false;
			}
			IProgress iProgress = new AbstractProgress() {

				private ProgressHelper progressHelper;

				@Override
				public void startWork(int work) {
					progressHelper = new ProgressHelper(() -> {
						if (listener != null) {
							int progress = progressHelper.getLastKnownProgress();
							int deltaProgress = progressHelper.getLastAddedDeltaProgress();
							listener.onFileDownloadProgress(type, fileName, progress, deltaProgress);
						}
					});
					if (listener != null) {
						progressHelper.onStartWork(work);
						listener.onFileDownloadStarted(type, fileName, progressHelper.getTotalWork());
					}
				}

				@Override
				public void progress(int deltaWork) {
					if (listener != null) {
						progressHelper.onProgress(deltaWork);
					}
				}

				@Override
				public boolean isInterrupted() {
					if (listener != null) {
						return listener.isDownloadCancelled();
					}
					return super.isInterrupted();
				}

				@Override
				public void finishTask() {
					int remainingProgress = progressHelper.getTotalWork() - progressHelper.getLastKnownProgress();
					if (remainingProgress > 0) {
						progress(remainingProgress);
					}
				}
			};
			iProgress.startWork(remoteFile.getFilesize() / 1024);
			error = AndroidNetworkUtils.downloadFile(builder.toString(), file, true, iProgress);
		} catch (UnsupportedEncodingException e) {
			error = "UnsupportedEncodingException";
		}
		if (listener != null) {
			listener.onFileDownloadDone(type, fileName, error);
		}
		operationLog.finishOperation(error);
		return error;
	}

	@SuppressLint("StaticFieldLeak")
	void collectLocalFiles(@Nullable OnCollectLocalFilesListener listener) {
		OperationLog operationLog = new OperationLog("collectLocalFiles", DEBUG);
		operationLog.startOperation();
		AsyncTask<Void, LocalFile, List<LocalFile>> task = new AsyncTask<Void, LocalFile, List<LocalFile>>() {

			BackupDbHelper dbHelper;
			SQLiteConnection db;
			Map<String, UploadedFileInfo> infos;

			@Override
			protected void onPreExecute() {
				dbHelper = app.getBackupHelper().getDbHelper();
				db = dbHelper.openConnection(true);
			}

			@Override
			protected List<LocalFile> doInBackground(Void... voids) {
				List<LocalFile> result = new ArrayList<>();
				infos = dbHelper.getUploadedFileInfoMap();
				List<SettingsItem> localItems = getLocalItems();
				operationLog.log("getLocalItems");
				for (SettingsItem item : localItems) {
					String fileName = getItemFileName(item);
					if (item instanceof FileSettingsItem) {
						FileSettingsItem fileItem = (FileSettingsItem) item;
						File file = fileItem.getFile();
						if (file.isDirectory()) {
							if (item instanceof GpxSettingsItem) {
								continue;
							} else if (fileItem.getSubtype() == FileSubtype.VOICE) {
								File jsFile = new File(file, file.getName() + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS);
								if (jsFile.exists()) {
									fileName = jsFile.getPath().replace(app.getAppPath(null).getPath() + "/", "");
									createLocalFile(result, item, fileName, jsFile, jsFile.lastModified());
									continue;
								}
							} else if (fileItem.getSubtype() == FileSubtype.TTS_VOICE) {
								String langName = file.getName().replace(IndexConstants.VOICE_PROVIDER_SUFFIX, "");
								File jsFile = new File(file, langName + "_" + IndexConstants.TTSVOICE_INDEX_EXT_JS);
								if (jsFile.exists()) {
									fileName = jsFile.getPath().replace(app.getAppPath(null).getPath() + "/", "");
									createLocalFile(result, item, fileName, jsFile, jsFile.lastModified());
									continue;
								}
							} else if (fileItem.getSubtype() == FileSubtype.TILES_MAP) {
								continue;
							}
							List<File> dirs = new ArrayList<>();
							dirs.add(file);
							Algorithms.collectDirs(file, dirs);
							operationLog.log("collectDirs " + file.getName() + " BEGIN");
							for (File dir : dirs) {
								File[] files = dir.listFiles();
								if (files != null && files.length > 0) {
									for (File f : files) {
										if (!f.isDirectory()) {
											fileName = f.getPath().replace(app.getAppPath(null).getPath() + "/", "");
											createLocalFile(result, item, fileName, f, f.lastModified());
										}
									}
								}
							}
							operationLog.log("collectDirs " + file.getName() + " END");
						} else if (fileItem.getSubtype() == FileSubtype.TILES_MAP) {
							if (file.getName().endsWith(SQLiteTileSource.EXT)) {
								createLocalFile(result, item, fileName, file, file.lastModified());
							}
						} else {
							createLocalFile(result, item, fileName, file, file.lastModified());
						}
					} else {
						createLocalFile(result, item, fileName, null, item.getLastModifiedTime());
					}
				}
				return result;
			}

			private void createLocalFile(@NonNull List<LocalFile> result, @NonNull SettingsItem item,
			                             @NonNull String fileName, @Nullable File file, long lastModifiedTime) {
				LocalFile localFile = new LocalFile();
				localFile.file = file;
				localFile.item = item;
				localFile.fileName = fileName;
				localFile.localModifiedTime = lastModifiedTime;
				if (infos != null) {
					UploadedFileInfo fileInfo = infos.get(item.getType().name() + "___" + fileName);
					if (fileInfo != null) {
						localFile.uploadTime = fileInfo.getUploadTime();
						String lastMd5 = fileInfo.getMd5Digest();
						boolean needM5Digest = item instanceof StreamSettingsItem
								&& ((StreamSettingsItem) item).needMd5Digest()
								&& localFile.uploadTime < lastModifiedTime
								&& !Algorithms.isEmpty(lastMd5);
						if (needM5Digest && file != null && file.exists()) {
							FileInputStream is = null;
							try {
								is = new FileInputStream(file);
								String md5 = new String(Hex.encodeHex(DigestUtils.md5(is)));
								if (md5.equals(lastMd5)) {
									item.setLocalModifiedTime(localFile.uploadTime);
									localFile.localModifiedTime = localFile.uploadTime;
								}
							} catch (IOException e) {
								LOG.error(e.getMessage(), e);
							} finally {
								Algorithms.closeStream(is);
							}
						}
					}
				}
				result.add(localFile);
				publishProgress(localFile);
			}

			private List<SettingsItem> getLocalItems() {
				List<ExportType> types = getEnabledExportTypes();
				return app.getFileSettingsHelper().getFilteredSettingsItems(types, true, true, false);
			}

			@Override
			protected void onProgressUpdate(LocalFile... localFiles) {
				for (LocalFile file : localFiles) {
					if (listener != null) {
						listener.onFileCollected(file);
					}
				}
			}

			@Override
			protected void onPostExecute(List<LocalFile> localFiles) {
				if (db != null) {
					db.close();
				}
				operationLog.finishOperation(" Files=" + localFiles.size());
				if (listener != null) {
					listener.onFilesCollected(localFiles);
				}
			}
		};
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
	void generateBackupInfo(@NonNull Map<String, LocalFile> localFiles,
	                        @NonNull Map<String, RemoteFile> uniqueRemoteFiles,
	                        @NonNull Map<String, RemoteFile> deletedRemoteFiles,
	                        @Nullable OnGenerateBackupInfoListener listener) {

		OperationLog operationLog = new OperationLog("generateBackupInfo", DEBUG, 200);
		operationLog.startOperation();

		AsyncTask<Void, Void, BackupInfo> task = new AsyncTask<Void, Void, BackupInfo>() {
			@Override
			protected BackupInfo doInBackground(Void... voids) {
				BackupInfo info = new BackupInfo();
				/*
				operationLog.log("=== localFiles ===");
				for (LocalFile localFile : localFiles.values()) {
					operationLog.log(localFile.toString());
				}
				operationLog.log("=== localFiles ===");
				operationLog.log("=== uniqueRemoteFiles ===");
				for (RemoteFile remoteFile : uniqueRemoteFiles.values()) {
					operationLog.log(remoteFile.toString());
				}
				operationLog.log("=== uniqueRemoteFiles ===");
				operationLog.log("=== deletedRemoteFiles ===");
				for (RemoteFile remoteFile : deletedRemoteFiles.values()) {
					operationLog.log(remoteFile.toString());
				}
				operationLog.log("=== deletedRemoteFiles ===");
				*/
				List<RemoteFile> remoteFiles = new ArrayList<>(uniqueRemoteFiles.values());
				remoteFiles.addAll(deletedRemoteFiles.values());
				for (RemoteFile remoteFile : remoteFiles) {
					ExportType exportType = ExportType.findBy(remoteFile);
					if (exportType == null || !exportType.isEnabled() || remoteFile.isRecordedVoiceFile()) {
						continue;
					}
					LocalFile localFile = localFiles.get(remoteFile.getTypeNamePath());
					if (localFile != null) {
						boolean fileChangedLocally = localFile.localModifiedTime > localFile.uploadTime;
						boolean fileChangedRemotely = remoteFile.getUpdatetimems() > localFile.uploadTime;
						if (fileChangedRemotely && fileChangedLocally) {
							info.filesToMerge.add(new Pair<>(localFile, remoteFile));
						} else if (fileChangedLocally) {
							info.filesToUpload.add(localFile);
						} else if (fileChangedRemotely) {
							if (remoteFile.isDeleted()) {
								info.localFilesToDelete.add(localFile);
							} else {
								info.filesToDownload.add(remoteFile);
							}
						}
						if (PluginsHelper.isDevelopment()) {
							if (fileChangedRemotely || fileChangedLocally) {
								LOG.debug("file to backup " + localFile + " " + remoteFile
										+ " " + fileChangedRemotely + " fileChangedRemotely " + fileChangedLocally + "fileChangedLocally");
							}
						}
					} else if (!remoteFile.isDeleted()) {
						UploadedFileInfo fileInfo = dbHelper.getUploadedFileInfo(remoteFile.getType(), remoteFile.getName());
						// suggest to remove only if file exists in db
						if (fileInfo != null && fileInfo.getUploadTime() >= remoteFile.getUpdatetimems()) {
							// conflicts not supported yet
							// info.filesToMerge.add(new Pair<>(null, remoteFile));
							info.filesToDelete.add(remoteFile);
						} else {
							info.filesToDownload.add(remoteFile);
						}
					}
				}
				for (LocalFile localFile : localFiles.values()) {
					ExportType exportType = ExportType.findBy(localFile.item);
					if (exportType == null || !exportType.isEnabled()) {
						continue;
					}
					boolean hasRemoteFile = uniqueRemoteFiles.containsKey(localFile.getTypeFileName());
					boolean toDelete = info.localFilesToDelete.contains(localFile);
					if (!hasRemoteFile && !toDelete) {
						boolean isEmpty = localFile.item instanceof CollectionSettingsItem<?> && ((CollectionSettingsItem<?>) localFile.item).isEmpty();
						if (!isEmpty) {
							info.filesToUpload.add(localFile);
						}
					}
				}
				info.createItemCollections(app);

				operationLog.log("=== filesToUpload ===");
				for (LocalFile localFile : info.filesToUpload) {
					operationLog.log(localFile.toString());
				}
				operationLog.log("=== filesToUpload ===");
				operationLog.log("=== filesToDownload ===");
				for (RemoteFile remoteFile : info.filesToDownload) {
					LocalFile localFile = localFiles.get(remoteFile.getTypeNamePath());
					if (localFile != null) {
						operationLog.log(remoteFile.toString() + " localUploadTime=" + localFile.uploadTime);
					} else {
						operationLog.log(remoteFile.toString());
					}
				}
				operationLog.log("=== filesToDownload ===");
				operationLog.log("=== filesToDelete ===");
				for (RemoteFile remoteFile : info.filesToDelete) {
					operationLog.log(remoteFile.toString());
				}
				operationLog.log("=== filesToDelete ===");
				operationLog.log("=== localFilesToDelete ===");
				for (LocalFile localFile : info.localFilesToDelete) {
					operationLog.log(localFile.toString());
				}
				operationLog.log("=== localFilesToDelete ===");
				operationLog.log("=== filesToMerge ===");
				for (Pair<LocalFile, RemoteFile> filePair : info.filesToMerge) {
					operationLog.log("LOCAL=" + filePair.first.toString() + " REMOTE=" + filePair.second.toString());
				}
				operationLog.log("=== filesToMerge ===");
				return info;
			}

			@Override
			protected void onPostExecute(BackupInfo backupInfo) {
				operationLog.finishOperation(backupInfo.toString());
				if (listener != null) {
					listener.onBackupInfoGenerated(backupInfo, null);
				}
			}
		};
		task.executeOnExecutor(executor);
	}

	public static boolean isDefaultObfMap(@NonNull OsmandApplication app,
	                                      @NonNull FileSettingsItem settingsItem,
	                                      @NonNull String fileName) {
		FileSubtype subtype = settingsItem.getSubtype();
		if (subtype.isMap()) {
			return isObfMapExistsOnServer(app, fileName);
		}
		return false;
	}

	private static boolean isObfMapExistsOnServer(@NonNull OsmandApplication app, @NonNull String name) {
		boolean[] exists = new boolean[1];

		Map<String, String> params = new HashMap<>();
		params.put("name", name);
		params.put("type", "file");

		OperationLog operationLog = new OperationLog("isObfMapExistsOnServer", DEBUG);
		operationLog.startOperation(name);

		AndroidNetworkUtils.sendRequest(app, "https://osmand.net/userdata/check-file-on-server",
				params, "Check obf map on server", false, false,
				(result, error, resultCode) -> {
					int status;
					String message;
					if (!Algorithms.isEmpty(error)) {
						status = STATUS_SERVER_ERROR;
						message = "Check obf map on server error: " + new BackupError(error);
					} else if (!Algorithms.isEmpty(result)) {
						try {
							JSONObject obj = new JSONObject(result);
							String fileStatus = obj.optString("status");
							exists[0] = Algorithms.stringsEqual(fileStatus, "present");

							status = STATUS_SUCCESS;
							message = name + " exists: " + exists[0];
						} catch (JSONException e) {
							status = STATUS_PARSE_JSON_ERROR;
							message = "Check obf map on server error: json parsing";
						}
					} else {
						status = STATUS_EMPTY_RESPONSE_ERROR;
						message = "Check obf map on server error: empty response";
					}
					operationLog.finishOperation("(" + status + "): " + message);
				});
		return exists[0];
	}
}
