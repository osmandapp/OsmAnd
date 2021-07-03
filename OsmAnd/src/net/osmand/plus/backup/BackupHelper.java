package net.osmand.plus.backup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnFilesDownloadCallback;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.AndroidUtils;
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
import net.osmand.plus.backup.BackupListeners.OnGenerateBackupInfoListener;
import net.osmand.plus.backup.BackupListeners.OnUpdateOrderIdListener;
import net.osmand.plus.backup.BackupListeners.OnUploadFileListener;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.commands.DeleteAllFilesCommand;
import net.osmand.plus.backup.commands.DeleteFilesCommand;
import net.osmand.plus.backup.commands.DeleteOldFilesCommand;
import net.osmand.plus.backup.commands.RegisterDeviceCommand;
import net.osmand.plus.backup.commands.RegisterUserCommand;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.plus.settings.backend.backup.items.CollectionSettingsItem;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.GpxSettingsItem;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final BackupDbHelper dbHelper;

	public static final Log LOG = PlatformUtil.getLog(BackupHelper.class);
	public static final boolean DEBUG = true;

	public static final int THREAD_POOL_SIZE = 4;
	private final BackupExecutor executor;

	public final static String INFO_EXT = ".info";

	public static final String SERVER_URL = "https://osmand.net";

	public static final String USER_REGISTER_URL = SERVER_URL + "/userdata/user-register";
	public static final String DEVICE_REGISTER_URL = SERVER_URL + "/userdata/device-register";
	public static final String UPDATE_ORDER_ID_URL = SERVER_URL + "/userdata/user-update-orderid";
	public static final String UPLOAD_FILE_URL = SERVER_URL + "/userdata/upload-file";
	public static final String LIST_FILES_URL = SERVER_URL + "/userdata/list-files";
	public static final String DOWNLOAD_FILE_URL = SERVER_URL + "/userdata/download-file";
	public static final String DELETE_FILE_URL = SERVER_URL + "/userdata/delete-file";
	public static final String DELETE_FILE_VERSION_URL = SERVER_URL + "/userdata/delete-file-version";

	private static final String BACKUP_TYPE_PREFIX = "backup_type_";
	private static final String VERSION_HISTORY_PREFIX = "save_version_history_";

	public final static int STATUS_SUCCESS = 0;
	public final static int STATUS_PARSE_JSON_ERROR = 1;
	public final static int STATUS_EMPTY_RESPONSE_ERROR = 2;
	public final static int STATUS_SERVER_ERROR = 3;
	public final static int STATUS_NO_ORDER_ID_ERROR = 4;

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

	public PrepareBackupResult getBackup() {
		return backup;
	}

	void setBackup(PrepareBackupResult backup) {
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

	@SuppressLint("HardwareIds")
	public String getAndroidId() {
		try {
			return Settings.Secure.getString(app.getContentResolver(), Settings.Secure.ANDROID_ID);
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean isTokenValid(@NonNull String token) {
		return token.matches("[0-9]+");
	}

	@Nullable
	public String getOrderId() {
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		InAppSubscription purchasedSubscription = purchaseHelper.getAnyPurchasedSubscription();
		return purchasedSubscription != null ? purchasedSubscription.getOrderId() : null;
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

	public void updateBackupUploadTime() {
		settings.BACKUP_LAST_UPLOADED_TIME.set(System.currentTimeMillis() + 1);
	}

	public void logout() {
		settings.BACKUP_DEVICE_ID.resetToDefault();
		settings.BACKUP_ACCESS_TOKEN.resetToDefault();
	}

	public CommonPreference<Boolean> getBackupTypePref(@NonNull ExportSettingsType type) {
		return app.getSettings().registerBooleanPreference(BACKUP_TYPE_PREFIX + type.name(), true).makeGlobal().makeShared();
	}

	public CommonPreference<Boolean> getVersionHistoryTypePref(@NonNull ExportSettingsType type) {
		return app.getSettings().registerBooleanPreference(VERSION_HISTORY_PREFIX + type.name(), true).makeGlobal().makeShared();
	}

	public static boolean applyItem(@NonNull SettingsItem item, @NonNull String type, @NonNull String name) {
		String itemFileName = getItemFileName(item);
		if (item.getType().name().equals(type)) {
			if (name.equals(itemFileName)) {
				return true;
			} else if (item instanceof FileSettingsItem) {
				FileSettingsItem fileItem = (FileSettingsItem) item;
				return name.startsWith(fileItem.getSubtype().getSubtypeFolder()) && name.startsWith(itemFileName);
			}
		}
		return false;
	}

	@NonNull
	public static String getItemFileName(@NonNull SettingsItem item) {
		String fileName;
		if (item instanceof FileSettingsItem) {
			FileSettingsItem fileItem = (FileSettingsItem) item;
			fileName = BackupHelper.getFileItemName(fileItem);
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

	public void registerUser(@NonNull final String email, @Nullable String promoCode, boolean login) {
		executor.runCommand(new RegisterUserCommand(this, login, email, promoCode));
	}

	public void registerDevice(@NonNull String token) {
		executor.runCommand(new RegisterDeviceCommand(this, token));
	}

	void updateOrderId(@Nullable final OnUpdateOrderIdListener listener) {
		Map<String, String> params = new HashMap<>();
		params.put("email", getEmail());
		String orderId = getOrderId();
		if (Algorithms.isEmpty(orderId)) {
			if (listener != null) {
				listener.onUpdateOrderId(STATUS_NO_ORDER_ID_ERROR, "Order id is empty", null);
			}
			return;
		} else {
			params.put("orderid", orderId);
		}
		String androidId = getAndroidId();
		if (!Algorithms.isEmpty(androidId)) {
			params.put("deviceid", androidId);
		}
		OperationLog operationLog = new OperationLog("updateOrderId", DEBUG);
		AndroidNetworkUtils.sendRequest(app, UPDATE_ORDER_ID_URL, params, "Update order id", false, true, (resultJson, error) -> {
			int status;
			String message;
			if (!Algorithms.isEmpty(error)) {
				message = "Update order id error: " + new ServerError(error);
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
				listener.onUpdateOrderId(status, message, error);
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
				BackupHelper.this.prepareBackupTask = null;
				for (OnPrepareBackupListener listener : prepareBackupListeners) {
					listener.onBackupPrepared(backupResult);
				}
			}
		});
		this.prepareBackupTask = prepareBackupTask;
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

	String uploadFile(@NonNull String fileName, @NonNull String type,
					  @NonNull StreamWriter streamWriter, final long uploadTime,
					  @Nullable final OnUploadFileListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("name", fileName);
		params.put("type", type);
		params.put("clienttime", String.valueOf(uploadTime));

		Map<String, String> headers = new HashMap<>();
		headers.put("Accept-Encoding", "deflate, gzip");

		OperationLog operationLog = new OperationLog("uploadFile", DEBUG);
		operationLog.startOperation(type + " " + fileName);
		String error = AndroidNetworkUtils.uploadFile(UPLOAD_FILE_URL, streamWriter, fileName, true, params, headers,
				new AbstractProgress() {

					private int work = 0;
					private int progress = 0;
					private int deltaProgress = 0;

					@Override
					public void startWork(int work) {
						if (listener != null) {
							this.work = work > 0 ? work : 1;
							listener.onFileUploadStarted(type, fileName, work);
						}
					}

					@Override
					public void progress(int deltaWork) {
						if (listener != null) {
							deltaProgress += deltaWork;
							if ((deltaProgress > (work / 100)) || ((progress + deltaProgress) >= work)) {
								progress += deltaProgress;
								deltaProgress = 0;
								listener.onFileUploadProgress(type, fileName, progress, deltaWork);
							}
						}
					}
				});
		if (error == null) {
			updateFileUploadTime(type, fileName, uploadTime);
		}
		if (listener != null) {
			listener.onFileUploadDone(type, fileName, uploadTime, error);
		}
		operationLog.finishOperation(type + " " + fileName + (error != null ? " Error: " + new ServerError(error) : " OK"));
		return error;
	}

	void deleteFiles(@NonNull List<RemoteFile> remoteFiles,
					 @Nullable final OnDeleteFilesListener listener) throws UserNotRegisteredException {
		deleteFiles(remoteFiles, false, listener);
	}

	void deleteFiles(@NonNull List<RemoteFile> remoteFiles, boolean byVersion,
					 @Nullable final OnDeleteFilesListener listener) throws UserNotRegisteredException {
		checkRegistered();
		executor.runCommand(new DeleteFilesCommand(this, remoteFiles, byVersion, listener));
	}

	void downloadFileList(@Nullable final OnDownloadFileListListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("allVersions", "true");
		AndroidNetworkUtils.sendRequest(app, LIST_FILES_URL, params, "Download file list", false, false,
				new OnRequestResultListener() {
					final OperationLog operationLog = new OperationLog("downloadFileList", DEBUG);

					@Override
					public void onResult(@Nullable String resultJson, @Nullable String error) {
						int status;
						String message;
						List<RemoteFile> remoteFiles = new ArrayList<>();
						if (!Algorithms.isEmpty(error)) {
							status = STATUS_SERVER_ERROR;
							message = "Download file list error: " + new ServerError(error);
						} else if (!Algorithms.isEmpty(resultJson)) {
							try {
								JSONObject result1 = new JSONObject(resultJson);
								String totalZipSize = result1.getString("totalZipSize");
								String totalFiles = result1.getString("totalFiles");
								String totalFileVersions = result1.getString("totalFileVersions");
								JSONArray allFiles = result1.getJSONArray("allFiles");
								for (int i = 0; i < allFiles.length(); i++) {
									remoteFiles.add(new RemoteFile(allFiles.getJSONObject(i)));
								}
								status = STATUS_SUCCESS;
								message = "Total files: " + totalFiles + " " +
										"Total zip size: " + AndroidUtils.formatSize(app, Long.parseLong(totalZipSize)) + " " +
										"Total file versions: " + totalFileVersions;
							} catch (JSONException | ParseException e) {
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
					}
				});
	}

	public void deleteAllFiles(@NonNull List<ExportSettingsType> types) throws UserNotRegisteredException {
		checkRegistered();
		executor.runCommand(new DeleteAllFilesCommand(this, types));
	}

	public void deleteOldFiles(@NonNull List<ExportSettingsType> types) throws UserNotRegisteredException {
		checkRegistered();
		executor.runCommand(new DeleteOldFilesCommand(this, types));
	}

	@NonNull
	Map<File, String> downloadFiles(@NonNull final Map<File, RemoteFile> filesMap) throws UserNotRegisteredException {
		checkRegistered();

		Map<File, String> res = new HashMap<>();
		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		AndroidNetworkUtils.downloadFiles(DOWNLOAD_FILE_URL,
				new ArrayList<>(filesMap.keySet()), params, new OnFilesDownloadCallback() {
					OperationLog operationLog;

					@Nullable
					@Override
					public Map<String, String> getAdditionalParams(@NonNull File file) {
						RemoteFile remoteFile = filesMap.get(file);
						Map<String, String> additionaParams = new HashMap<>();
						additionaParams.put("name", remoteFile.getName());
						additionaParams.put("type", remoteFile.getType());
						return additionaParams;
					}

					@Override
					public void onFileDownloadProgress(@NonNull File file, int percent) {
						if (percent == 0) {
							operationLog = new OperationLog("downloadFile", DEBUG);
						}
					}

					@Override
					public void onFileDownloadDone(@NonNull File file) {
						if (operationLog != null) {
							operationLog.finishOperation(file.getName());
						}
					}

					@Override
					public void onFileDownloadedAsync(@NonNull File file) {
					}

					@Override
					public void onFilesDownloadDone(@NonNull Map<File, String> errors) {
						res.putAll(errors);
					}
				});
		return res;
	}

	@SuppressLint("StaticFieldLeak")
	void collectLocalFiles(@Nullable final OnCollectLocalFilesListener listener) {
		OperationLog operationLog = new OperationLog("collectLocalFiles", DEBUG);
		AsyncTask<Void, LocalFile, List<LocalFile>> task = new AsyncTask<Void, LocalFile, List<LocalFile>>() {

			BackupDbHelper dbHelper;
			SQLiteConnection db;

			@Override
			protected void onPreExecute() {
				dbHelper = app.getBackupHelper().getDbHelper();
				db = dbHelper.openConnection(true);
			}

			@Override
			protected List<LocalFile> doInBackground(Void... voids) {
				List<LocalFile> result = new ArrayList<>();
				List<SettingsItem> localItems = getLocalItems();
				for (SettingsItem item : localItems) {
					String fileName = BackupHelper.getItemFileName(item);
					if (item instanceof FileSettingsItem) {
						File file = ((FileSettingsItem) item).getFile();
						if (file.isDirectory()) {
							List<File> dirs = new ArrayList<>();
							dirs.add(file);
							Algorithms.collectDirs(file, dirs);
							for (File dir : dirs) {
								File[] files = dir.listFiles();
								if (files != null && files.length > 0) {
									for (File f : files) {
										fileName = f.getPath().replace(app.getAppPath(null).getPath() + "/", "");
										createLocalFile(result, item, fileName, f.lastModified());
									}
								}
							}
						} else {
							createLocalFile(result, item, fileName, file.lastModified());
						}
					} else {
						createLocalFile(result, item, fileName, item.getLastModifiedTime());
					}
				}
				return result;
			}

			private void createLocalFile(List<LocalFile> result, SettingsItem item, String fileName, long lastModifiedTime) {
				LocalFile localFile = new LocalFile();
				localFile.item = item;
				localFile.subfolder = "";
				localFile.fileName = fileName;
				localFile.localModifiedTime = lastModifiedTime;
				if (db != null) {
					UploadedFileInfo info = dbHelper.getUploadedFileInfo(db, item.getType().name(), fileName);
					if (info != null) {
						localFile.uploadTime = info.getUploadTime();
					}
				}
				result.add(localFile);
				publishProgress(localFile);
			}

			private List<SettingsItem> getLocalItems() {
				List<ExportSettingsType> types = ExportSettingsType.getEnabledTypes();
				return app.getFileSettingsHelper().getFilteredSettingsItems(types, true, true, true);
			}

			@Override
			protected void onProgressUpdate(LocalFile... localFiles) {
				if (listener != null) {
					listener.onFileCollected(localFiles[0]);
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
		task.executeOnExecutor(executor);
	}

	@SuppressLint("StaticFieldLeak")
	void generateBackupInfo(@NonNull final List<LocalFile> localFiles,
							@NonNull final List<RemoteFile> uniqueRemoteFiles,
							@NonNull final List<RemoteFile> deletedRemoteFiles,
							@Nullable final OnGenerateBackupInfoListener listener) {

		OperationLog operationLog = new OperationLog("generateBackupInfo", DEBUG, 200);
		final long backupLastUploadedTime = settings.BACKUP_LAST_UPLOADED_TIME.get();

		AsyncTask<Void, Void, BackupInfo> task = new AsyncTask<Void, Void, BackupInfo>() {
			@Override
			protected BackupInfo doInBackground(Void... voids) {
				BackupInfo info = new BackupInfo();
				List<RemoteFile> remoteFiles = new ArrayList<>(uniqueRemoteFiles);
				remoteFiles.addAll(deletedRemoteFiles);
				for (RemoteFile remoteFile : remoteFiles) {
					ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForRemoteFile(remoteFile);
					if (exportType == null || !ExportSettingsType.isTypeEnabled(exportType)) {
						continue;
					}
					boolean hasLocalFile = false;
					for (LocalFile localFile : localFiles) {
						if (remoteFile.getName().equals(localFile.getFileName(true))) {
							hasLocalFile = true;
							long remoteUploadTime = remoteFile.getClienttimems();
							long localUploadTime = localFile.uploadTime;
							if (remoteFile.isDeleted()) {
								info.localFilesToDelete.add(localFile);
							} else if (remoteUploadTime == localUploadTime) {
								if (localUploadTime < localFile.localModifiedTime) {
									info.filesToUpload.add(localFile);
								}
							} else {
								info.filesToMerge.add(new Pair<>(localFile, remoteFile));
							}
							break;
						}
					}
					if (!hasLocalFile && !remoteFile.isDeleted()) {
						if (backupLastUploadedTime > 0 && backupLastUploadedTime >= remoteFile.getClienttimems()) {
							info.filesToDelete.add(remoteFile);
						} else {
							info.filesToDownload.add(remoteFile);
						}
					}
				}
				for (LocalFile localFile : localFiles) {
					ExportSettingsType exportType = localFile.item != null
							? ExportSettingsType.getExportSettingsTypeForItem(localFile.item) : null;
					if (exportType == null || !ExportSettingsType.isTypeEnabled(exportType)) {
						continue;
					}
					boolean hasRemoteFile = false;
					for (RemoteFile remoteFile : uniqueRemoteFiles) {
						if (localFile.getFileName(true).equals(remoteFile.getName())) {
							hasRemoteFile = true;
							break;
						}
					}
					if (!hasRemoteFile) {
						boolean isEmpty = localFile.item instanceof CollectionSettingsItem<?> && ((CollectionSettingsItem<?>) localFile.item).isEmpty();
						if (!isEmpty) {
							info.filesToUpload.add(localFile);
						}
					}
				}
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
}
