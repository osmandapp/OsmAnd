package net.osmand.plus.backup;

import android.annotation.SuppressLint;
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
import net.osmand.PlatformUtil;
import net.osmand.StreamWriter;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupDbHelper.UploadedFileInfo;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.AbstractProgress;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.backend.backup.items.GlobalSettingsItem;
import net.osmand.plus.settings.backend.backup.items.ProfileSettingsItem;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BackupHelper {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final FavouritesDbHelper favouritesHelper;
	private final GpxDbHelper gpxHelper;
	private final BackupDbHelper dbHelper;

	public static final Log LOG = PlatformUtil.getLog(BackupHelper.class);

	public final static String INFO_EXT = ".info";

	private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 0L,
			TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

	private static final String SERVER_URL = "https://osmand.net";

	private static final String USER_REGISTER_URL = SERVER_URL + "/userdata/user-register";
	private static final String DEVICE_REGISTER_URL = SERVER_URL + "/userdata/device-register";
	private static final String UPLOAD_FILE_URL = SERVER_URL + "/userdata/upload-file";
	private static final String LIST_FILES_URL = SERVER_URL + "/userdata/list-files";
	private static final String DOWNLOAD_FILE_URL = SERVER_URL + "/userdata/download-file";
	private static final String DELETE_FILE_URL = SERVER_URL + "/userdata/delete-file";
	private static final String DELETE_FILE_VERSION_URL = SERVER_URL + "/userdata/delete-file-version";

	public final static int STATUS_SUCCESS = 0;
	public final static int STATUS_PARSE_JSON_ERROR = 1;
	public final static int STATUS_EMPTY_RESPONSE_ERROR = 2;
	public final static int STATUS_SERVER_ERROR = 3;

	public static final int SERVER_ERROR_CODE_EMAIL_IS_INVALID = 101;
	public static final int SERVER_ERROR_CODE_NO_VALID_SUBSCRIPTION = 102;
	public static final int SERVER_ERROR_CODE_USER_IS_NOT_REGISTERED = 103;
	public static final int SERVER_ERROR_CODE_TOKEN_IS_NOT_VALID_OR_EXPIRED = 104;
	public static final int SERVER_ERROR_CODE_PROVIDED_TOKEN_IS_NOT_VALID = 105;
	public static final int SERVER_ERROR_CODE_FILE_NOT_AVAILABLE = 106;
	public static final int SERVER_ERROR_CODE_GZIP_ONLY_SUPPORTED_UPLOAD = 107;
	public static final int SERVER_ERROR_CODE_SIZE_OF_SUPPORTED_BOX_IS_EXCEEDED = 108;

	private List<RemoteFile> remoteFiles;

	public interface OnRegisterUserListener {
		void onRegisterUser(int status, @Nullable String message, @Nullable String error);
	}

	public interface OnRegisterDeviceListener {
		void onRegisterDevice(int status, @Nullable String message, @Nullable String error);
	}

	public interface OnDownloadFileListListener {
		void onDownloadFileList(int status, @Nullable String message, @NonNull List<RemoteFile> remoteFiles);
	}

	public interface OnCollectLocalFilesListener {
		void onFileCollected(@NonNull LocalFile fileInfo);
		void onFilesCollected(@NonNull List<LocalFile> fileInfos);
	}

	public interface OnGenerateBackupInfoListener {
		void onBackupInfoGenerated(@Nullable BackupInfo backupInfo, @Nullable String error);
	}

	public interface OnUploadFileListener {
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

	public static class BackupInfo {
		public List<RemoteFile> filesToDownload = new ArrayList<>();
		public List<LocalFile> filesToUpload = new ArrayList<>();
		public List<RemoteFile> filesToDelete = new ArrayList<>();
		public List<Pair<LocalFile, RemoteFile>> filesToMerge = new ArrayList<>();
	}

	public BackupHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.favouritesHelper = app.getFavorites();
		this.gpxHelper = app.getGpxDbHelper();
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

	public List<RemoteFile> getRemoteFiles() {
		return remoteFiles;
	}

	void setRemoteFiles(List<RemoteFile> remoteFiles) {
		this.remoteFiles = remoteFiles;
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
		BackupDbHelper dbHelper = getDbHelper();
		UploadedFileInfo info = dbHelper.getUploadedFileInfo(type, fileName);
		if (info != null) {
			info.setUploadTime(updateTime);
			dbHelper.updateUploadedFileInfo(info);
		} else {
			info = new UploadedFileInfo(type, fileName, updateTime);
			dbHelper.addUploadedFileInfo(info);
		}
	}

	public void updateBackupUploadTime() {
		settings.BACKUP_LAST_UPLOADED_TIME.set(System.currentTimeMillis() + 1);
	}

	public void logout() {
		settings.BACKUP_DEVICE_ID.resetToDefault();
		settings.BACKUP_ACCESS_TOKEN.resetToDefault();
	}

	public void registerUser(@NonNull final String email, @Nullable final OnRegisterUserListener listener) {
		Map<String, String> params = new HashMap<>();
		params.put("email", email);
		final String orderId = getOrderId();
		if (!Algorithms.isEmpty(orderId)) {
			params.put("orderid", orderId);
		}
		final String deviceId = app.getUserAndroidId();
		params.put("deviceid", deviceId);
		AndroidNetworkUtils.sendRequestAsync(app, USER_REGISTER_URL, params, "Register user", false, true, new OnRequestResultListener() {
			@Override
			public void onResult(@Nullable String resultJson, @Nullable String error) {
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
			}
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
		AndroidNetworkUtils.sendRequestAsync(app, DEVICE_REGISTER_URL, params, "Register device", false, true, new OnRequestResultListener() {
			@Override
			public void onResult(@Nullable String resultJson, @Nullable String error) {
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
			}
		}, EXECUTOR);
	}

	public void prepareBackupInfo(OnPrepareBackupListener listener) {
		new PrepareBackupTask(app, listener).prepare();
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

	public String uploadFileSync(@NonNull String fileName, @NonNull String type, @NonNull StreamWriter streamWriter,
								 @Nullable final OnUploadFileListener listener) throws UserNotRegisteredException {
		checkRegistered();

		final long uploadTime = System.currentTimeMillis();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("name", fileName);
		params.put("type", type);
		params.put("clienttime", String.valueOf(uploadTime));

		Map<String, String> headers = new HashMap<>();
		headers.put("Accept-Encoding", "deflate, gzip");

		String error = AndroidNetworkUtils.uploadFile(UPLOAD_FILE_URL, streamWriter, fileName, true, params, headers,
				new AbstractProgress() {
					int progress = 0;

					@Override
					public void progress(int deltaWork) {
						if (listener != null) {
							progress += deltaWork;
							listener.onFileUploadProgress(type, fileName, progress, deltaWork);
						}
					}
				});
		error = error != null ? resolveServerError(error) : null;
		if (listener != null) {
			listener.onFileUploadDone(type, fileName, uploadTime, error);
		}
		return error;
	}

	public void uploadFile(@NonNull String fileName, @NonNull String type, @NonNull StreamWriter streamWriter,
						   @Nullable final OnUploadFileListener listener) throws UserNotRegisteredException {
		checkRegistered();

		final long uploadTime = System.currentTimeMillis();
		Map<String, String> params = new HashMap<>();
		Map<String, String> headers = new HashMap<>();
		prepareFileUpload(fileName, type, uploadTime, params, headers);

		AndroidNetworkUtils.uploadFileAsync(UPLOAD_FILE_URL, streamWriter, fileName, true, params, headers, new OnFileUploadCallback() {

			@Override
			public void onFileUploadProgress(int progress) {
				if (listener != null) {
					listener.onFileUploadProgress(type, fileName, progress, 0);
				}
			}

			@Override
			public void onFileUploadDone(@Nullable String error) {
				if (listener != null) {
					listener.onFileUploadDone(type, fileName, uploadTime, error != null ? resolveServerError(error) : null);
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
		final File favoritesFile = favouritesHelper.getExternalFile();
		AndroidNetworkUtils.uploadFilesAsync(UPLOAD_FILE_URL, new ArrayList<>(localFileMap.keySet()), true, params, headers, new OnFilesUploadCallback() {
			@Nullable
			@Override
			public Map<String, String> getAdditionalParams(@NonNull File file) {
				Map<String, String> additionaParams = new HashMap<>();
				LocalFile localFile = localFileMap.get(file);
				if (localFile != null) {
					additionaParams.put("name", localFile.getFileName(true));
					additionaParams.put("type", Algorithms.getFileExtension(file));
					localFile.uploadTime = System.currentTimeMillis();
					additionaParams.put("clienttime", String.valueOf(localFile.uploadTime));
				}
				return additionaParams;
			}

			@Override
			public void onFileUploadProgress(@NonNull File file, int progress) {
				if (listener != null) {
					listener.onFileUploadProgress(file, progress);
				}
			}

			@Override
			public void onFileUploadDone(@NonNull File file) {
				if (listener != null) {
					LocalFile localFile = localFileMap.get(file);
					if (localFile != null) {
						if (file.equals(favoritesFile)) {
							favouritesHelper.setLastUploadedTime(localFile.uploadTime);
						} else {
							GpxDataItem gpxItem = gpxHelper.getItem(file);
							if (gpxItem != null) {
								gpxHelper.updateLastUploadedTime(gpxItem, localFile.uploadTime);
							}
						}
					}
					listener.onFileUploadDone(file);
				}
			}

			@Override
			public void onFilesUploadDone(@NonNull Map<File, String> errors) {
				if (errors.isEmpty()) {
					settings.BACKUP_LAST_UPLOADED_TIME.set(System.currentTimeMillis() + 1);
				}
				if (listener != null) {
					listener.onFilesUploadDone(resolveServerErrors(errors));
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
			@Override
			public void onRequestSent(@NonNull RequestResponse response) {
				if (listener != null) {
					RemoteFile remoteFile = filesMap.get(response.getRequest());
					if (remoteFile != null) {
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
		AndroidNetworkUtils.sendRequest(app, LIST_FILES_URL, params, "Download file list", false, false,
				getDownloadFileListListener(listener));
	}

	public void downloadFileList(@Nullable final OnDownloadFileListListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		AndroidNetworkUtils.sendRequestAsync(app, LIST_FILES_URL, params, "Download file list", false, false,
				getDownloadFileListListener(listener), EXECUTOR);
	}

	private OnRequestResultListener getDownloadFileListListener(@Nullable OnDownloadFileListListener listener) {
		return new OnRequestResultListener() {
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
						JSONArray files = result.getJSONArray("uniqueFiles");
						for (int i = 0; i < files.length(); i++) {
							remoteFiles.add(new RemoteFile(files.getJSONObject(i)));
						}

						status = STATUS_SUCCESS;
						message = "Total files: " + totalFiles + "\n" +
								"Total zip size: " + AndroidUtils.formatSize(app, Long.parseLong(totalZipSize)) + "\n" +
								"Total file versions: " + totalFileVersions;
					} catch (JSONException | ParseException e) {
						status = STATUS_PARSE_JSON_ERROR;
						message = "Download file list error: json parsing";
					}
				} else {
					status = STATUS_EMPTY_RESPONSE_ERROR;
					message = "Download file list error: empty response";
				}
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
		return new OnRequestResultListener() {
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
			}
		};
	}

	public void deleteOldFiles(@Nullable final OnDeleteFilesListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		params.put("allVersions", "true");
		AndroidNetworkUtils.sendRequestAsync(app, LIST_FILES_URL, params, "Delete old files", false, false,
				getDeleteOldFilesListener(listener), EXECUTOR);
	}

	private OnRequestResultListener getDeleteOldFilesListener(@Nullable OnDeleteFilesListener listener) {
		return new OnRequestResultListener() {
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
						if (listener != null) {
							listener.onFileDownloadProgress(filesMap.get(file), percent);
						}
					}

					@Override
					public void onFileDownloadDone(@NonNull File file) {
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
						Map<File, String> errMap = resolveServerErrors(errors);
						res.putAll(errMap);
						if (listener != null) {
							listener.onFilesDownloadDone(errMap);
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
						if (listener != null) {
							listener.onFileDownloadProgress(filesMap.get(file), percent);
						}
					}

					@Override
					public void onFileDownloadDone(@NonNull File file) {
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
							listener.onFilesDownloadDone(resolveServerErrors(errors));
						}
					}
				}, EXECUTOR);
	}

	@SuppressLint("StaticFieldLeak")
	public void collectLocalFiles(@Nullable final OnCollectLocalFilesListener listener) {
		AsyncTask<Void, LocalFile, List<LocalFile>> task = new AsyncTask<Void, LocalFile, List<LocalFile>>() {

			@Override
			protected List<LocalFile> doInBackground(Void... voids) {
				List<LocalFile> result = new ArrayList<>();

				List<ExportSettingsType> settingsTypes = new ArrayList<>(Arrays.asList(ExportSettingsType.values()));
				List<SettingsItem> localItems = app.getFileSettingsHelper().getFilteredSettingsItems(settingsTypes, true, true);

				for (SettingsItem item : localItems) {
					String fileName = item.getFileName();
					if (fileName == null) {
						fileName = item.getDefaultFileName();
					}
					LocalFile localFile = new LocalFile();
					localFile.item = item;
					localFile.subfolder = "";
					if (item instanceof FileSettingsItem) {
						localFile.file = ((FileSettingsItem) item).getFile();
						localFile.localModifiedTime = localFile.file.lastModified();
					} else {
						localFile.fileName = fileName;
						if (localFile.item instanceof ProfileSettingsItem) {
							ProfileSettingsItem settingsItem = (ProfileSettingsItem) localFile.item;
							localFile.localModifiedTime = app.getSettings().getLastModePreferencesEditTime(settingsItem.getAppMode());
						} else if (localFile.item instanceof GlobalSettingsItem) {
							localFile.localModifiedTime = app.getSettings().getLastGlobalPreferencesEditTime();
						}
					}

					UploadedFileInfo info = app.getBackupHelper().getDbHelper().getUploadedFileInfo(item.getType().name(), fileName);
					if (info != null) {
						localFile.uploadTime = info.getUploadTime();
					}

					result.add(localFile);
					if (listener != null) {
						listener.onFileCollected(localFile);
					}
				}
				return result;
			}

			@Override
			protected void onProgressUpdate(LocalFile... fileInfos) {
				if (listener != null) {
					listener.onFileCollected(fileInfos[0]);
				}
			}

			@Override
			protected void onPostExecute(List<LocalFile> fileInfos) {
				if (listener != null) {
					listener.onFilesCollected(fileInfos);
				}
			}
		};
		task.executeOnExecutor(EXECUTOR);
	}

	private Map<File, String> resolveServerErrors(@NonNull Map<File, String> errors) {
		Map<File, String> resolvedErrors = new HashMap<>();
		for (Entry<File, String> fileError : errors.entrySet()) {
			File file = fileError.getKey();
			String errorStr = resolveServerError(fileError.getValue());
			resolvedErrors.put(file, errorStr);
		}
		return resolvedErrors;
	}

	@NonNull
	private String resolveServerError(@NonNull String fileError) {
		String errorStr = fileError;
		try {
			JSONObject errorJson = new JSONObject(errorStr);
			JSONObject error = errorJson.getJSONObject("error");
			errorStr = "Error " + error.getInt("errorCode") + " (" + error.getString("message") + ")";
		} catch (JSONException e) {
			// ignore
		}
		return errorStr;
	}

	private String parseServerError(@NonNull String error) {
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
	public void generateBackupInfo(@NonNull final List<LocalFile> localFiles, @NonNull final List<RemoteFile> remoteFiles,
								   @Nullable final OnGenerateBackupInfoListener listener) {

		final long backupLastUploadedTime = settings.BACKUP_LAST_UPLOADED_TIME.get();

		AsyncTask<Void, Void, BackupInfo> task = new AsyncTask<Void, Void, BackupInfo>() {
			@Override
			protected BackupInfo doInBackground(Void... voids) {
				BackupInfo info = new BackupInfo();
				for (RemoteFile remoteFile : remoteFiles) {
					boolean hasLocalFile = false;
					for (LocalFile localFile : localFiles) {
						if (remoteFile.getName().equals(localFile.getFileName(true))) {
							hasLocalFile = true;
							long remoteUploadTime = remoteFile.getClienttimems();
							long localUploadTime = localFile.uploadTime;
							if (remoteUploadTime == localUploadTime) {
								if (localUploadTime < localFile.localModifiedTime) {
									info.filesToUpload.add(localFile);
								}
							} else {
								info.filesToMerge.add(new Pair<>(localFile, remoteFile));
							}
							break;
						}
					}
					if (!hasLocalFile) {
						if (backupLastUploadedTime > 0 && backupLastUploadedTime >= remoteFile.getClienttimems()) {
							info.filesToDelete.add(remoteFile);
						} else {
							info.filesToDownload.add(remoteFile);
						}
					}
				}
				for (LocalFile localFile : localFiles) {
					boolean hasRemoteFile = false;
					for (RemoteFile remoteFile : remoteFiles) {
						if (localFile.getFileName(true).equals(remoteFile.getName())) {
							hasRemoteFile = true;
							break;
						}
					}
					if (!hasRemoteFile) {
						info.filesToUpload.add(localFile);
					}
				}
				return info;
			}

			@Override
			protected void onPostExecute(BackupInfo backupInfo) {
				if (listener != null) {
					listener.onBackupInfoGenerated(backupInfo, null);
				}
			}
		};
		task.executeOnExecutor(EXECUTOR);
	}
}
