package net.osmand.plus.backup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnFileUploadCallback;
import net.osmand.AndroidNetworkUtils.OnFilesDownloadCallback;
import net.osmand.AndroidNetworkUtils.OnFilesUploadCallback;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.AndroidNetworkUtils.OnSendRequestsListener;
import net.osmand.AndroidNetworkUtils.Request;
import net.osmand.AndroidNetworkUtils.RequestResponse;
import net.osmand.AndroidUtils;
import net.osmand.OperationLog;
import net.osmand.PlatformUtil;
import net.osmand.StreamWriter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.plus.api.SQLiteAPI.SQLiteConnection;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BackupHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final BackupDbHelper dbHelper;

	public static final Log LOG = PlatformUtil.getLog(BackupHelper.class);
	private static final boolean DEBUG = true;

	public final static String INFO_EXT = ".info";

	private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 0L,
			TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

	private static final String SERVER_URL = "https://osmand.net";

	private static final String USER_REGISTER_URL = SERVER_URL + "/userdata/user-register";
	private static final String DEVICE_REGISTER_URL = SERVER_URL + "/userdata/device-register";
	private static final String UPDATE_ORDER_ID_URL = SERVER_URL + "/userdata/user-update-orderid";
	private static final String UPLOAD_FILE_URL = SERVER_URL + "/userdata/upload-file";
	private static final String LIST_FILES_URL = SERVER_URL + "/userdata/list-files";
	private static final String DOWNLOAD_FILE_URL = SERVER_URL + "/userdata/download-file";
	private static final String DELETE_FILE_URL = SERVER_URL + "/userdata/delete-file";
	private static final String DELETE_FILE_VERSION_URL = SERVER_URL + "/userdata/delete-file-version";

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

	public interface OnRegisterUserListener {
		void onRegisterUser(int status, @Nullable String message, @Nullable String error);
	}

	public interface OnRegisterDeviceListener {
		void onRegisterDevice(int status, @Nullable String message, @Nullable String error);
	}

	public interface OnUpdateOrderIdListener {
		void onUpdateOrderId(int status, @Nullable String message, @Nullable String error);
	}

	public interface OnDownloadFileListListener {
		void onDownloadFileList(int status, @Nullable String message, @NonNull List<RemoteFile> remoteFiles);
	}

	public interface OnCollectLocalFilesListener {
		void onFileCollected(@NonNull LocalFile localFile);
		void onFilesCollected(@NonNull List<LocalFile> localFiles);
	}

	public interface OnGenerateBackupInfoListener {
		void onBackupInfoGenerated(@Nullable BackupInfo backupInfo, @Nullable String error);
	}

	public interface OnUploadFileListener {
		void onFileUploadStarted(@NonNull String type, @NonNull String fileName, int work);
		void onFileUploadProgress(@NonNull String type, @NonNull String fileName, int progress, int deltaWork);
		void onFileUploadDone(@NonNull String type, @NonNull String fileName, long uploadTime, @Nullable String error);
	}

	public interface OnUploadFilesListener {
		void onFileUploadProgress(@NonNull File file, int progress);
		void onFileUploadDone(@NonNull File file);
		void onFilesUploadDone(@NonNull Map<File, String> errors);
	}

	public interface OnDeleteFilesListener {
		void onFileDeleteProgress(@NonNull RemoteFile file);
		void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors);
		void onFilesDeleteError(int status, @NonNull String message);
	}

	public interface OnDownloadFileListener {
		void onFileDownloadProgress(@NonNull RemoteFile remoteFile, int progress);

		@WorkerThread
		void onFileDownloadedAsync(@NonNull File file);
		void onFileDownloaded(@NonNull File file);
		void onFilesDownloadDone(@NonNull Map<File, String> errors);
	}

	public BackupHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.dbHelper = new BackupDbHelper(app);
	}

	@NonNull
	public OsmandApplication getApp() {
		return app;
	}

	@NonNull
	public BackupDbHelper getDbHelper() {
		return dbHelper;
	}

	public PrepareBackupResult getBackup() {
		return backup;
	}

	void setBackup(PrepareBackupResult backup) {
		this.backup = backup;
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
	private String getAndroidId() {
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

	public void registerUser(@NonNull final String email, @Nullable String promoCode, boolean login,
							 @Nullable final OnRegisterUserListener listener) {
		Map<String, String> params = new HashMap<>();
		params.put("email", email);
		params.put("login", String.valueOf(login));
		final String orderId = Algorithms.isEmpty(promoCode) ? getOrderId() : promoCode;
		if (!Algorithms.isEmpty(orderId)) {
			params.put("orderid", orderId);
		}
		final String deviceId = app.getUserAndroidId();
		params.put("deviceid", deviceId);
		OperationLog operationLog = new OperationLog("registerUser", DEBUG);
		AndroidNetworkUtils.sendRequestAsync(app, USER_REGISTER_URL, params, "Register user", false, true, (resultJson, error) -> {
			int status;
			String message;
			if (!Algorithms.isEmpty(error)) {
				message = "User registration error: " + parseServerError(error) + "\nEmail=" + email + "\nOrderId=" + orderId + "\nDeviceId=" + deviceId;
				status = STATUS_SERVER_ERROR;
			} else if (!Algorithms.isEmpty(resultJson)) {
				try {
					JSONObject result = new JSONObject(resultJson);
					if (result.has("status") && "ok".equals(result.getString("status"))) {
						message = "You have been registered successfully. Please check for email with activation code.";
						status = STATUS_SUCCESS;
					} else {
						message = "User registration error: unknown";
						status = STATUS_SERVER_ERROR;
					}
				} catch (JSONException e) {
					message = "User registration error: json parsing";
					status = STATUS_PARSE_JSON_ERROR;
				}
			} else {
				message = "User registration error: empty response";
				status = STATUS_EMPTY_RESPONSE_ERROR;
			}
			if (listener != null) {
				listener.onRegisterUser(status, message, error);
			}
			operationLog.finishOperation(status + " " + message);
		}, EXECUTOR);
	}

	public void registerDevice(String token, @Nullable final OnRegisterDeviceListener listener) {
		Map<String, String> params = new HashMap<>();
		params.put("email", getEmail());
		String orderId = getOrderId();
		if (orderId != null) {
			params.put("orderid", orderId);
		}
		String androidId = getAndroidId();
		if (!Algorithms.isEmpty(androidId)) {
			params.put("deviceid", androidId);
		}
		params.put("token", token);
		OperationLog operationLog = new OperationLog("registerDevice", DEBUG);
		AndroidNetworkUtils.sendRequestAsync(app, DEVICE_REGISTER_URL, params, "Register device", false, true, (resultJson, error) -> {
			int status;
			String message;
			if (!Algorithms.isEmpty(error)) {
				message = "Device registration error: " + parseServerError(error);
				status = STATUS_SERVER_ERROR;
			} else if (!Algorithms.isEmpty(resultJson)) {
				try {
					JSONObject result = new JSONObject(resultJson);
					settings.BACKUP_DEVICE_ID.set(result.getString("id"));
					settings.BACKUP_USER_ID.set(result.getString("userid"));
					settings.BACKUP_NATIVE_DEVICE_ID.set(result.getString("deviceid"));
					settings.BACKUP_ACCESS_TOKEN.set(result.getString("accesstoken"));
					settings.BACKUP_ACCESS_TOKEN_UPDATE_TIME.set(result.getString("udpatetime"));

					message = "Device have been registered successfully";
					status = STATUS_SUCCESS;
				} catch (JSONException e) {
					message = "Device registration error: json parsing";
					status = STATUS_PARSE_JSON_ERROR;
				}
			} else {
				message = "Device registration error: empty response";
				status = STATUS_EMPTY_RESPONSE_ERROR;
			}
			if (listener != null) {
				listener.onRegisterDevice(status, message, error);
			}
			operationLog.finishOperation(status + " " + message);
		}, EXECUTOR);
	}

	public void updateOrderIdSync(@Nullable final OnUpdateOrderIdListener listener) {
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
				message = "Update order id error: " + parseServerError(error);
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

	private void prepareFileUpload(@NonNull String fileName, @NonNull String type, long uploadTime,
								   @NonNull Map<String, String> params, @NonNull Map<String, String> headers) {
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("name", fileName);
		params.put("type", type);
		params.put("clienttime", String.valueOf(uploadTime));

		headers.put("Accept-Encoding", "deflate, gzip");
	}

	public String uploadFileSync(@NonNull String fileName, @NonNull String type,
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

		OperationLog operationLog = new OperationLog("uploadFileSync", DEBUG);
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
		operationLog.finishOperation(type + " " + fileName + (error != null ? " Error: " + parseServerError(error) : " OK"));
		return error;
	}

	public void uploadFile(@NonNull String fileName, @NonNull String type, @NonNull StreamWriter streamWriter,
						   @Nullable final OnUploadFileListener listener) throws UserNotRegisteredException {
		checkRegistered();

		final long uploadTime = System.currentTimeMillis();
		Map<String, String> params = new HashMap<>();
		Map<String, String> headers = new HashMap<>();
		prepareFileUpload(fileName, type, uploadTime, params, headers);

		OperationLog operationLog = new OperationLog("uploadFile", DEBUG);
		AndroidNetworkUtils.uploadFileAsync(UPLOAD_FILE_URL, streamWriter, fileName, true, params, headers, new OnFileUploadCallback() {

			@Override
			public void onFileUploadStarted() {
				if (listener != null) {
					listener.onFileUploadStarted(type, fileName, 1);
				}
			}

			@Override
			public void onFileUploadProgress(int progress) {
				if (listener != null) {
					listener.onFileUploadProgress(type, fileName, progress, 0);
				}
			}

			@Override
			public void onFileUploadDone(@Nullable String error) {
				operationLog.finishOperation(type + " " + fileName + (error != null ? " Error: " + parseServerError(error) : " OK"));
				if (error == null) {
					updateFileUploadTime(type, fileName, uploadTime);
				}
				if (listener != null) {
					listener.onFileUploadDone(type, fileName, uploadTime, error);
				}
			}
		}, EXECUTOR);
	}

	public void uploadFiles(@NonNull List<LocalFile> localFiles, @Nullable final OnUploadFilesListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept-Encoding", "deflate, gzip");

		final Map<File, LocalFile> localFileMap = new HashMap<>();
		for (LocalFile localFile : localFiles) {
			localFileMap.put(localFile.file, localFile);
		}
		AndroidNetworkUtils.uploadFilesAsync(UPLOAD_FILE_URL, new ArrayList<>(localFileMap.keySet()), true, params, headers, new OnFilesUploadCallback() {
			OperationLog operationLog;
			@Nullable
			@Override
			public Map<String, String> getAdditionalParams(@NonNull File file) {
				Map<String, String> additionaParams = new HashMap<>();
				LocalFile localFile = localFileMap.get(file);
				if (localFile != null) {
					additionaParams.put("name", localFile.getFileName(true));
					additionaParams.put("type", localFile.item.getType().name());
					localFile.uploadTime = System.currentTimeMillis();
					additionaParams.put("clienttime", String.valueOf(localFile.uploadTime));
				}
				return additionaParams;
			}

			@Override
			public void onFileUploadProgress(@NonNull File file, int progress) {
				if (progress == 0) {
					operationLog = new OperationLog("uploadFile", DEBUG);
				}
				if (listener != null) {
					listener.onFileUploadProgress(file, progress);
				}
			}

			@Override
			public void onFileUploadDone(@NonNull File file) {
				if (operationLog != null) {
					operationLog.finishOperation(file.getName());
				}
				LocalFile localFile = localFileMap.get(file);
				if (localFile != null) {
					updateFileUploadTime(localFile.item.getType().name(), localFile.getFileName(true), localFile.uploadTime);
				}
				if (listener != null) {
					listener.onFileUploadDone(file);
				}
			}

			@Override
			public void onFilesUploadDone(@NonNull Map<File, String> errors) {
				if (errors.isEmpty()) {
					settings.BACKUP_LAST_UPLOADED_TIME.set(System.currentTimeMillis() + 1);
				}
				if (listener != null) {
					listener.onFilesUploadDone(errors);
				}
			}
		}, EXECUTOR);
	}

	public void deleteFiles(@NonNull List<RemoteFile> remoteFiles, @Nullable final OnDeleteFilesListener listener) throws UserNotRegisteredException {
		deleteFiles(remoteFiles, false, listener);
	}

	public void deleteFiles(@NonNull List<RemoteFile> remoteFiles, boolean byVersion, @Nullable final OnDeleteFilesListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> commonParameters = new HashMap<>();
		commonParameters.put("deviceid", getDeviceId());
		commonParameters.put("accessToken", getAccessToken());

		final List<Request> requests = new ArrayList<>();
		final Map<Request, RemoteFile> filesMap = new HashMap<>();
		for (RemoteFile remoteFile : remoteFiles) {
			Map<String, String> parameters = new HashMap<>(commonParameters);
			parameters.put("name", remoteFile.getName());
			parameters.put("type", remoteFile.getType());
			if (byVersion) {
				parameters.put("updatetime", String.valueOf(remoteFile.getUpdatetimems()));
			}
			Request r = new Request(byVersion ? DELETE_FILE_VERSION_URL : DELETE_FILE_URL, parameters, null, false, true);
			requests.add(r);
			filesMap.put(r, remoteFile);
		}
		AndroidNetworkUtils.sendRequestsAsync(null, requests, new OnSendRequestsListener() {
			OperationLog operationLog;
			@Override
			public void onRequestSending(@NonNull Request request) {
				operationLog = new OperationLog("deleteFile", DEBUG);
			}

			@Override
			public void onRequestSent(@NonNull RequestResponse response) {
				if (listener != null) {
					RemoteFile remoteFile = filesMap.get(response.getRequest());
					if (remoteFile != null) {
						if (operationLog != null) {
							operationLog.finishOperation(remoteFile.getName());
						}
						listener.onFileDeleteProgress(remoteFile);
					}
				}
			}

			@Override
			public void onRequestsSent(@NonNull List<RequestResponse> results) {
				if (listener != null) {
					Map<RemoteFile, String> errors = new HashMap<>();
					for (RequestResponse response : results) {
						RemoteFile remoteFile = filesMap.get(response.getRequest());
						if (remoteFile != null) {
							boolean success;
							String message = null;
							String errorStr = response.getError();
							if (!Algorithms.isEmpty(errorStr)) {
								message = parseServerError(errorStr);
								success = false;
							} else {
								String responseStr = response.getResponse();
								try {
									JSONObject result = new JSONObject(responseStr);
									if (result.has("status") && "ok".equals(result.getString("status"))) {
										success = true;
									} else {
										message = "Unknown error";
										success = false;
									}
								} catch (JSONException e) {
									message = "Json parsing error";
									success = false;
								}
							}
							if (!success) {
								errors.put(remoteFile, message);
							}
						}
					}
					listener.onFilesDeleteDone(errors);
				}
			}
		}, EXECUTOR);
	}

	public void downloadFileListSync(@Nullable final OnDownloadFileListListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("allVersions", "true");
		AndroidNetworkUtils.sendRequest(app, LIST_FILES_URL, params, "Download file list", false, false,
				getDownloadFileListListener(listener));
	}

	public void downloadFileList(@Nullable final OnDownloadFileListListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("allVersions", "true");
		AndroidNetworkUtils.sendRequestAsync(app, LIST_FILES_URL, params, "Download file list", false, false,
				getDownloadFileListListener(listener), EXECUTOR);
	}

	private OnRequestResultListener getDownloadFileListListener(@Nullable OnDownloadFileListListener listener) {
		return new OnRequestResultListener() {
			final OperationLog operationLog = new OperationLog("downloadFileList", DEBUG);
			@Override
			public void onResult(@Nullable String resultJson, @Nullable String error) {
				int status;
				String message;
				List<RemoteFile> remoteFiles = new ArrayList<>();
				if (!Algorithms.isEmpty(error)) {
					status = STATUS_SERVER_ERROR;
					message = "Download file list error: " + parseServerError(error);
				} else if (!Algorithms.isEmpty(resultJson)) {
					try {
						JSONObject result = new JSONObject(resultJson);
						String totalZipSize = result.getString("totalZipSize");
						String totalFiles = result.getString("totalFiles");
						String totalFileVersions = result.getString("totalFileVersions");
						JSONArray allFiles = result.getJSONArray("allFiles");
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
		};
	}

	public void deleteAllFiles(@Nullable final OnDeleteFilesListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("allVersions", "true");
		AndroidNetworkUtils.sendRequestAsync(app, LIST_FILES_URL, params, "Delete all files", false, false,
				getDeleteAllFilesListener(listener), EXECUTOR);
	}

	private OnRequestResultListener getDeleteAllFilesListener(@Nullable OnDeleteFilesListener listener) {
		return (resultJson, error) -> {
			int status;
			String message;
			List<RemoteFile> remoteFiles = new ArrayList<>();
			if (!Algorithms.isEmpty(error)) {
				status = STATUS_SERVER_ERROR;
				message = "Download file list error: " + parseServerError(error);
			} else if (!Algorithms.isEmpty(resultJson)) {
				try {
					JSONObject result = new JSONObject(resultJson);
					JSONArray files = result.getJSONArray("allFiles");
					for (int i = 0; i < files.length(); i++) {
						remoteFiles.add(new RemoteFile(files.getJSONObject(i)));
					}
					status = STATUS_SUCCESS;
					message = "OK";
				} catch (JSONException | ParseException e) {
					status = STATUS_PARSE_JSON_ERROR;
					message = "Download file list error: json parsing";
				}
			} else {
				status = STATUS_EMPTY_RESPONSE_ERROR;
				message = "Download file list error: empty response";
			}
			if (status != STATUS_SUCCESS) {
				if (listener != null) {
					listener.onFilesDeleteError(status, message);
				}
			} else {
				try {
					if (!remoteFiles.isEmpty()) {
						deleteFiles(remoteFiles, true, listener);
					} else {
						if (listener != null) {
							listener.onFilesDeleteDone(Collections.emptyMap());
						}
					}
				} catch (UserNotRegisteredException e) {
					if (listener != null) {
						listener.onFilesDeleteError(STATUS_SERVER_ERROR, "User not registered");
					}
				}
			}
		};
	}

	public void deleteOldFiles(@Nullable final OnDeleteFilesListener listener, List<ExportSettingsType> types) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("allVersions", "true");
		AndroidNetworkUtils.sendRequestAsync(app, LIST_FILES_URL, params, "Delete old files", false, false,
				getDeleteOldFilesListener(listener, types), EXECUTOR);
	}

	private OnRequestResultListener getDeleteOldFilesListener(@Nullable OnDeleteFilesListener listener, List<ExportSettingsType> types) {
		return (resultJson, error) -> {
			int status;
			String message;
			List<RemoteFile> remoteFiles = new ArrayList<>();
			if (!Algorithms.isEmpty(error)) {
				status = STATUS_SERVER_ERROR;
				message = "Download file list error: " + parseServerError(error);
			} else if (!Algorithms.isEmpty(resultJson)) {
				try {
					JSONObject result = new JSONObject(resultJson);
					JSONArray allFiles = result.getJSONArray("allFiles");
					for (int i = 0; i < allFiles.length(); i++) {
						remoteFiles.add(new RemoteFile(allFiles.getJSONObject(i)));
					}
					JSONArray uniqueFiles = result.getJSONArray("uniqueFiles");
					for (int i = 0; i < uniqueFiles.length(); i++) {
						remoteFiles.remove(new RemoteFile(uniqueFiles.getJSONObject(i)));
					}
					status = STATUS_SUCCESS;
					message = "OK";
				} catch (JSONException | ParseException e) {
					status = STATUS_PARSE_JSON_ERROR;
					message = "Download file list error: json parsing";
				}
			} else {
				status = STATUS_EMPTY_RESPONSE_ERROR;
				message = "Download file list error: empty response";
			}
			if (status != STATUS_SUCCESS) {
				if (listener != null) {
					listener.onFilesDeleteError(status, message);
				}
			} else {
				try {
					List<RemoteFile> filesToDelete = new ArrayList<>();
					for (RemoteFile file : remoteFiles) {
						ExportSettingsType exportType = ExportSettingsType.getExportSettingsTypeForRemoteFile(file);
						if (types.contains(exportType)) {
							filesToDelete.add(file);
						}
					}
					if (!filesToDelete.isEmpty()) {
						deleteFiles(filesToDelete, true, listener);
					} else {
						if (listener != null) {
							listener.onFilesDeleteDone(Collections.emptyMap());
						}
					}
				} catch (UserNotRegisteredException e) {
					if (listener != null) {
						listener.onFilesDeleteError(STATUS_SERVER_ERROR, "User not registered");
					}
				}
			}
		};
	}

	@NonNull
	public Map<File, String> downloadFilesSync(@NonNull final Map<File, RemoteFile> filesMap,
											   @Nullable final OnDownloadFileListener listener) throws UserNotRegisteredException {
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
						if (listener != null) {
							listener.onFileDownloadProgress(filesMap.get(file), percent);
						}
					}

					@Override
					public void onFileDownloadDone(@NonNull File file) {
						if (operationLog != null) {
							operationLog.finishOperation(file.getName());
						}
						if (listener != null) {
							listener.onFileDownloaded(file);
						}
					}

					@Override
					public void onFileDownloadedAsync(@NonNull File file) {
						if (listener != null) {
							listener.onFileDownloadedAsync(file);
						}
					}

					@Override
					public void onFilesDownloadDone(@NonNull Map<File, String> errors) {
						res.putAll(errors);
						if (listener != null) {
							listener.onFilesDownloadDone(errors);
						}
					}
				});
		return res;
	}

	public void downloadFiles(@NonNull final Map<File, RemoteFile> filesMap,
							  @Nullable final OnDownloadFileListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		AndroidNetworkUtils.downloadFilesAsync(DOWNLOAD_FILE_URL,
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
						if (listener != null) {
							listener.onFileDownloadProgress(filesMap.get(file), percent);
						}
					}

					@Override
					public void onFileDownloadDone(@NonNull File file) {
						if (operationLog != null) {
							operationLog.finishOperation(file.getName());
						}
						if (listener != null) {
							listener.onFileDownloaded(file);
						}
					}

					@Override
					public void onFileDownloadedAsync(@NonNull File file) {
						if (listener != null) {
							listener.onFileDownloadedAsync(file);
						}
					}

					@Override
					public void onFilesDownloadDone(@NonNull Map<File, String> errors) {
						if (listener != null) {
							listener.onFilesDownloadDone(errors);
						}
					}
				}, EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
	public void collectLocalFiles(@Nullable final OnCollectLocalFilesListener listener) {
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
		task.executeOnExecutor(EXECUTOR);
	}

	@NonNull
	public static String parseServerError(@NonNull String error) {
		try {
			JSONObject resultError = new JSONObject(error);
			if (resultError.has("error")) {
				JSONObject errorObj = resultError.getJSONObject("error");
				return errorObj.getInt("errorCode") + " (" + errorObj.getString("message") + ")";
			}
		} catch (JSONException e) {
			// ignore
		}
		return error;
	}

	public static int getErrorCode(@Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			try {
				JSONObject resultError = new JSONObject(error);
				if (resultError.has("error")) {
					JSONObject errorObj = resultError.getJSONObject("error");
					return errorObj.getInt("errorCode");
				}
			} catch (JSONException e) {
				// ignore
			}
		}
		return -1;
	}

	@SuppressLint("StaticFieldLeak")
	public void generateBackupInfo(@NonNull final List<LocalFile> localFiles,
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
		task.executeOnExecutor(EXECUTOR);
	}
}
