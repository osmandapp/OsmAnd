package net.osmand.plus.backup;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnFilesDownloadCallback;
import net.osmand.AndroidNetworkUtils.OnFilesUploadCallback;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.AndroidNetworkUtils.OnSendRequestsListener;
import net.osmand.AndroidNetworkUtils.Request;
import net.osmand.AndroidNetworkUtils.RequestResponse;
import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.inapp.InAppPurchases.InAppSubscription;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
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

	private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 0L,
			TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

	private static final String SERVER_URL = "https://osmand.net";

	private static final String USER_REGISTER_URL = SERVER_URL + "/userdata/user-register";
	private static final String DEVICE_REGISTER_URL = SERVER_URL + "/userdata/device-register";
	private static final String UPLOAD_FILE_URL = SERVER_URL + "/userdata/upload-file";
	private static final String LIST_FILES_URL = SERVER_URL + "/userdata/list-files";
	private static final String DOWNLOAD_FILE_URL = SERVER_URL + "/userdata/download-file";
	private static final String DELETE_FILE_URL = SERVER_URL + "/userdata/delete-file";

	public final static int STATUS_SUCCESS = 0;
	public final static int STATUS_PARSE_JSON_ERROR = 1;
	public final static int STATUS_EMPTY_RESPONSE_ERROR = 2;
	public final static int STATUS_SERVER_ERROR = 3;

	public interface OnRegisterUserListener {
		void onRegisterUser(int status, @Nullable String message);
	}

	public interface OnRegisterDeviceListener {
		void onRegisterDevice(int status, @Nullable String message);
	}

	public interface OnDownloadFileListListener {
		void onDownloadFileList(int status, @Nullable String message, @NonNull List<UserFile> userFiles);
	}

	public interface OnCollectLocalFilesListener {
		void onFileCollected(@NonNull GpxFileInfo fileInfo);
		void onFilesCollected(@NonNull List<GpxFileInfo> fileInfos);
	}

	public interface OnGenerateBackupInfoListener {
		void onBackupInfoGenerated(@Nullable BackupInfo backupInfo, @Nullable String error);
	}

	public interface OnUploadFilesListener {
		void onFileUploadProgress(@NonNull File file, int progress);
		void onFileUploadDone(@NonNull File file);
		void onFilesUploadDone(@NonNull Map<File, String> errors);
	}

	public interface OnDeleteFilesListener {
		void onFileDeleteProgress(@NonNull UserFile file);
		void onFilesDeleteDone(@NonNull Map<UserFile, String> errors);
	}

	public interface OnDownloadFileListener {
		void onFileDownloadProgress(@NonNull UserFile userFile, int progress);

		@WorkerThread
		void onFileDownloadedAsync(@NonNull File file);
		void onFileDownloaded(@NonNull File file);
		void onFilesDownloadDone(@NonNull Map<File, String> errors);
	}

	public static class BackupInfo {
		public List<UserFile> filesToDownload = new ArrayList<>();
		public List<GpxFileInfo> filesToUpload = new ArrayList<>();
		public List<UserFile> filesToDelete = new ArrayList<>();
		public List<Pair<GpxFileInfo, UserFile>> filesToMerge = new ArrayList<>();
	}

	public BackupHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.favouritesHelper = app.getFavorites();
		this.gpxHelper = app.getGpxDbHelper();
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
					message = "User registration error: " + parseServerError(error) + "\nEmail=" + email + "\nOrderId=" + orderId + "\nDeviceId=" + deviceId + "\n\n";
					List<InAppSubscription> subscriptions = app.getInAppPurchaseHelper().getLiveUpdates().getAllSubscriptions();
					for (InAppSubscription s : subscriptions) {
						message += s.getSku() + " " + s.getOrderId() + " " + s.getState().getStateStr() + " " + s.getPurchaseState().name() + " " + s.getExpireTime() + "\n";
					}
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
					listener.onRegisterUser(status, message);
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
					listener.onRegisterDevice(status, message);
				}
			}
		}, EXECUTOR);
	}

	public void uploadFiles(@NonNull List<GpxFileInfo> gpxFiles, @Nullable final OnUploadFilesListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept-Encoding", "deflate, gzip");

		final Map<File, GpxFileInfo> gpxInfos = new HashMap<>();
		for (GpxFileInfo gpxFile : gpxFiles) {
			gpxInfos.put(gpxFile.file, gpxFile);
		}
		final File favoritesFile = favouritesHelper.getExternalFile();
		AndroidNetworkUtils.uploadFilesAsync(UPLOAD_FILE_URL, new ArrayList<>(gpxInfos.keySet()), true, params, headers, new OnFilesUploadCallback() {
			@Nullable
			@Override
			public Map<String, String> getAdditionalParams(@NonNull File file) {
				Map<String, String> additionaParams = new HashMap<>();
				GpxFileInfo gpxFileInfo = gpxInfos.get(file);
				if (gpxFileInfo != null) {
					additionaParams.put("name", gpxFileInfo.getFileName(true));
					additionaParams.put("type", Algorithms.getFileExtension(file));
					gpxFileInfo.uploadTime = System.currentTimeMillis();
					additionaParams.put("clienttime", String.valueOf(gpxFileInfo.uploadTime));
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
					GpxFileInfo gpxFileInfo = gpxInfos.get(file);
					if (gpxFileInfo != null) {
						if (file.equals(favoritesFile)) {
							favouritesHelper.setLastUploadedTime(gpxFileInfo.uploadTime);
						} else {
							GpxDataItem gpxItem = gpxHelper.getItem(file);
							if (gpxItem != null) {
								gpxHelper.updateLastUploadedTime(gpxItem, gpxFileInfo.uploadTime);
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

	public void deleteFiles(@NonNull List<UserFile> userFiles, @Nullable final OnDeleteFilesListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> commonParameters = new HashMap<>();
		commonParameters.put("deviceid", getDeviceId());
		commonParameters.put("accessToken", getAccessToken());

		final List<Request> requests = new ArrayList<>();
		final Map<Request, UserFile> filesMap = new HashMap<>();
		for (UserFile userFile : userFiles) {
			Map<String, String> parameters = new HashMap<>(commonParameters);
			parameters.put("name", userFile.getName());
			parameters.put("type", userFile.getType());
			Request r = new Request(DELETE_FILE_URL, parameters, null, false, true);
			requests.add(r);
			filesMap.put(r, userFile);
		}
		AndroidNetworkUtils.sendRequestsAsync(null, requests, new OnSendRequestsListener() {
			@Override
			public void onRequestSent(@NonNull RequestResponse response) {
				if (listener != null) {
					UserFile userFile = filesMap.get(response.getRequest());
					if (userFile != null) {
						listener.onFileDeleteProgress(userFile);
					}
				}
			}

			@Override
			public void onRequestsSent(@NonNull List<RequestResponse> results) {
				if (listener != null) {
					Map<UserFile, String> errors = new HashMap<>();
					for (RequestResponse response : results) {
						UserFile userFile = filesMap.get(response.getRequest());
						if (userFile != null) {
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
								errors.put(userFile, message);
							}
						}
					}
					listener.onFilesDeleteDone(errors);
				}
			}
		}, EXECUTOR);
	}

	public void downloadFileList(@Nullable final OnDownloadFileListListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		AndroidNetworkUtils.sendRequestAsync(app, LIST_FILES_URL, params, "Download file list", false, false, new OnRequestResultListener() {
			@Override
			public void onResult(@Nullable String resultJson, @Nullable String error) {
				int status;
				String message;
				List<UserFile> userFiles = new ArrayList<>();
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
							userFiles.add(new UserFile(files.getJSONObject(i)));
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
					listener.onDownloadFileList(status, message, userFiles);
				}
			}
		}, EXECUTOR);
	}

	public void downloadFiles(@NonNull final Map<File, UserFile> filesMap, @Nullable final OnDownloadFileListener listener) throws UserNotRegisteredException {
		checkRegistered();

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		AndroidNetworkUtils.downloadFilesAsync(DOWNLOAD_FILE_URL,
				new ArrayList<>(filesMap.keySet()), params, new OnFilesDownloadCallback() {
					@Nullable
					@Override
					public Map<String, String> getAdditionalParams(@NonNull File file) {
						UserFile userFile = filesMap.get(file);
						Map<String, String> additionaParams = new HashMap<>();
						additionaParams.put("name", userFile.getName());
						additionaParams.put("type", userFile.getType());
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
		AsyncTask<Void, GpxFileInfo, List<GpxFileInfo>> task = new AsyncTask<Void, GpxFileInfo, List<GpxFileInfo>>() {

			private final OnCollectLocalFilesListener internalListener = new OnCollectLocalFilesListener() {
				@Override
				public void onFileCollected(@NonNull GpxFileInfo fileInfo) {
					publishProgress(fileInfo);
				}

				@Override
				public void onFilesCollected(@NonNull List<GpxFileInfo> fileInfos) {
				}
			};

			private void loadGPXData(@NonNull File mapPath, @NonNull List<GpxFileInfo> result,
									 @Nullable OnCollectLocalFilesListener listener) {
				if (mapPath.canRead()) {
					loadGPXFolder(mapPath, result, "", listener);
				}
			}

			private void loadGPXFolder(@NonNull File mapPath, @NonNull List<GpxFileInfo> result,
									   @NonNull String gpxSubfolder, @Nullable OnCollectLocalFilesListener listener) {
				File[] listFiles = mapPath.listFiles();
				if (listFiles != null) {
					for (File gpxFile : listFiles) {
						if (gpxFile.isDirectory()) {
							String sub = gpxSubfolder.length() == 0 ? gpxFile.getName() : gpxSubfolder + "/"
									+ gpxFile.getName();
							loadGPXFolder(gpxFile, result, sub, listener);
						} else if (gpxFile.isFile() && gpxFile.getName().toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
							GpxFileInfo info = new GpxFileInfo();
							info.subfolder = gpxSubfolder;
							info.file = gpxFile;
							GpxDataItem gpxItem = gpxHelper.getItem(gpxFile);
							if (gpxItem != null) {
								info.uploadTime = gpxItem.getFileLastUploadedTime();
							}
							result.add(info);
							if (listener != null) {
								listener.onFileCollected(info);
							}
						}
					}
				}
			}

			@Override
			protected List<GpxFileInfo> doInBackground(Void... voids) {
				List<GpxFileInfo> result = new ArrayList<>();

				GpxFileInfo favInfo = new GpxFileInfo();
				favInfo.subfolder = "";
				favInfo.file = favouritesHelper.getExternalFile();
				favInfo.uploadTime = favouritesHelper.getLastUploadedTime();
				result.add(favInfo);
				if (listener != null) {
					listener.onFileCollected(favInfo);
				}

				loadGPXData(app.getAppPath(IndexConstants.GPX_INDEX_DIR), result, internalListener);
				return result;
			}

			@Override
			protected void onProgressUpdate(GpxFileInfo... fileInfos) {
				if (listener != null) {
					listener.onFileCollected(fileInfos[0]);
				}
			}

			@Override
			protected void onPostExecute(List<GpxFileInfo> fileInfos) {
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
			String errorStr = fileError.getValue();
			try {
				JSONObject errorJson = new JSONObject(errorStr);
				JSONObject error = errorJson.getJSONObject("error");
				errorStr = "Error " + error.getInt("errorCode") + " (" + error.getString("message") + ")";
			} catch (JSONException e) {
				// ignore
			}
			resolvedErrors.put(file, errorStr);
		}
		return resolvedErrors;
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

	@SuppressLint("StaticFieldLeak")
	public void generateBackupInfo(@NonNull final List<GpxFileInfo> localFiles, @NonNull final List<UserFile> remoteFiles,
								   @Nullable final OnGenerateBackupInfoListener listener) {

		final long backupLastUploadedTime = settings.BACKUP_LAST_UPLOADED_TIME.get();

		AsyncTask<Void, Void, BackupInfo> task = new AsyncTask<Void, Void, BackupInfo>() {
			@Override
			protected BackupInfo doInBackground(Void... voids) {
				BackupInfo info = new BackupInfo();
				for (UserFile remoteFile : remoteFiles) {
					boolean hasLocalFile = false;
					for (GpxFileInfo localFile : localFiles) {
						if (remoteFile.getName().equals(localFile.getFileName(true))) {
							hasLocalFile = true;
							long remoteUploadTime = remoteFile.getClienttimems();
							long localUploadTime = localFile.uploadTime;
							long localModifiedTime = localFile.file.lastModified();
							if (remoteUploadTime == localUploadTime) {
								if (localUploadTime < localModifiedTime) {
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
				for (GpxFileInfo localFile : localFiles) {
					boolean hasRemoteFile = false;
					for (UserFile remoteFile : remoteFiles) {
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
