package net.osmand.plus.development;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidNetworkUtils.OnFilesUploadCallback;
import net.osmand.AndroidNetworkUtils.OnRequestResultListener;
import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.ProgressImplementation;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBackupActivity extends OsmandActionBarActivity {

	// TODO pass actual sub order id!
	private static final String TEST_ORDER_ID = ""; 

	private OsmandApplication app;
	private OsmandSettings settings;

	private ProgressBar progressBar;
	private View buttonRegister;
	private View buttonVerify;
	private View buttonBackup;
	private View buttonRestore;
	private EditText emailEditText;
	private EditText tokenEditText;
	private TextView infoView;

	public interface OnResultListener {
		void onResult(boolean success, @Nullable String result);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		settings = app.getSettings();
		final WeakReference<TestBackupActivity> activityRef = new WeakReference<>(this);

		boolean nightMode = !app.getSettings().isLightContent();
		int themeId = nightMode ? R.style.OsmandDarkTheme_NoActionbar : R.style.OsmandLightTheme_NoActionbar;
		setTheme(themeId);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_backup_layout);
		Toolbar tb = findViewById(R.id.toolbar);
		tb.setTitle("Backup test");

		tb.setClickable(true);
		Drawable icBack = ((OsmandApplication) getApplication()).getUIUtilities().getIcon(AndroidUtils.getNavigationIconResId(app));
		tb.setNavigationIcon(icBack);
		tb.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		tb.setBackgroundColor(getResources().getColor(resolveResourceId(this, R.attr.pstsTabBackground)));
		tb.setTitleTextColor(getResources().getColor(resolveResourceId(this, R.attr.pstsTextColor)));
		tb.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(final View v) {
				finish();
			}
		});

		buttonRegister = findViewById(R.id.btn_register);
		UiUtilities.setupDialogButton(nightMode, buttonRegister, DialogButtonType.PRIMARY, "Register");
		buttonVerify = findViewById(R.id.btn_verify);
		UiUtilities.setupDialogButton(nightMode, buttonVerify, DialogButtonType.PRIMARY, "Verify");
		buttonBackup = findViewById(R.id.btn_backup);
		UiUtilities.setupDialogButton(nightMode, buttonBackup, DialogButtonType.PRIMARY, "Backup");
		buttonRestore = findViewById(R.id.btn_restore);
		UiUtilities.setupDialogButton(nightMode, buttonRestore, DialogButtonType.PRIMARY, "Restore");

		tokenEditText = findViewById(R.id.edit_token);
		infoView = findViewById(R.id.text_info);
		progressBar = findViewById(R.id.progress_bar);

		buttonVerify.setEnabled(false);
		emailEditText = findViewById(R.id.edit_email);
		String email = settings.BACKUP_USER_EMAIL.get();
		if (!Algorithms.isEmpty(email)) {
			emailEditText.setText(email);
		}
		buttonRegister.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = emailEditText.getText().toString();
				if (isEmailValid(email)) {
					buttonRegister.setEnabled(false);
					settings.BACKUP_USER_EMAIL.set(email);
					progressBar.setVisibility(View.VISIBLE);
					registerUser(email, new OnResultListener() {
						@Override
						public void onResult(boolean success, @Nullable String result) {
							TestBackupActivity a = activityRef.get();
							if (AndroidUtils.isActivityNotDestroyed(a)) {
								a.progressBar.setVisibility(View.GONE);
								a.buttonRegister.setEnabled(!success);
								a.buttonVerify.setEnabled(success);
								a.tokenEditText.requestFocus();
							}
						}
					});
				} else {
					emailEditText.requestFocus();
					emailEditText.setError(getString(R.string.osm_live_enter_email));
				}
			}
		});
		buttonVerify.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String token = tokenEditText.getText().toString();
				if (isTokenValid(token)) {
					buttonVerify.setEnabled(false);
					progressBar.setVisibility(View.VISIBLE);
					registerDevice(token, new OnResultListener() {
						@Override
						public void onResult(boolean success, @Nullable String result) {
							TestBackupActivity a = activityRef.get();
							if (AndroidUtils.isActivityNotDestroyed(a)) {
								a.progressBar.setVisibility(View.GONE);
								a.buttonVerify.setEnabled(!success);
								a.loadBackupInfo();
							}
						}
					});
				} else {
					tokenEditText.requestFocus();
					tokenEditText.setError("Token is not valid");
					buttonVerify.setEnabled(true);
				}
			}
		});
		buttonBackup.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				uploadFiles();
			}
		});
		buttonRestore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			}
		});

		loadBackupInfo();
	}

	private void loadBackupInfo() {
		if (!Algorithms.isEmpty(getDeviceId()) && !Algorithms.isEmpty(getAccessToken())) {
			final WeakReference<TestBackupActivity> activityRef = new WeakReference<>(this);
			progressBar.setVisibility(View.VISIBLE);
			loadBackupInfo(new OnResultListener() {
				@Override
				public void onResult(boolean success, @Nullable String result) {
					TestBackupActivity a = activityRef.get();
					if (AndroidUtils.isActivityNotDestroyed(a)) {
						a.progressBar.setVisibility(View.GONE);
						a.infoView.setText(result);
						a.infoView.requestFocus();
					}
				}
			});
		}
	}

	private boolean isEmailValid(CharSequence target) {
		return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
	}

	private String getOrderId() {
		return TEST_ORDER_ID;
	}

	private String getDeviceId() {
		return settings.BACKUP_DEVICE_ID.get();
	}

	private String getAccessToken() {
		return settings.BACKUP_ACCESS_TOKEN.get();
	}

	private void registerUser(@NonNull String email, @Nullable final OnResultListener listener) {
		Map<String, String> params = new HashMap<>();
		params.put("email", email);
		params.put("orderid", getOrderId());
		params.put("deviceid", app.getUserAndroidId());
		AndroidNetworkUtils.sendRequestAsync(app, "https://osmand.net/userdata/user-register", params, "Register user", true, true, new OnRequestResultListener() {
			@Override
			public void onResult(String resultJson) {
				boolean success = false;
				if (!Algorithms.isEmpty(resultJson)) {
					try {
						// {"status":"ok"}
						JSONObject result = new JSONObject(resultJson);
						String status = result.getString("status");
						success = status.equals("ok");
						app.showToastMessage(success
								? "You have been registered successfully. Please check for email with activation code."
								: "User registration error: " + status);
					} catch (JSONException e) {
						app.showToastMessage("User registration error: json parsing");
					}
				} else {
					app.showToastMessage("User registration error: empty response");
				}
				if (listener != null) {
					listener.onResult(success, resultJson);
				}
			}
		});
	}

	private void registerDevice(String token, @Nullable final OnResultListener listener) {
		Map<String, String> params = new HashMap<>();
		params.put("email", settings.BACKUP_USER_EMAIL.get());
		params.put("orderid", getOrderId());
		params.put("deviceid", app.getUserAndroidId());
		params.put("token", token);
		AndroidNetworkUtils.sendRequestAsync(app, "https://osmand.net/userdata/device-register", params, "Register device", true, true, new OnRequestResultListener() {
			@Override
			public void onResult(String resultJson) {
				boolean success = false;
				if (!Algorithms.isEmpty(resultJson)) {
					try {
						/*
						{
						  "id": 1034,
						  "userid": 1033,
						  "deviceid": "2fa8080d2985a777",
						  "orderid": "460000687003939",
						  "accesstoken": "4bc0a61f-397a-4c3e-9ffc-db382ec00372",
						  "udpatetime": "Apr 11, 2021, 11:32:20 AM"
						}
						*/
						JSONObject result = new JSONObject(resultJson);
						settings.BACKUP_DEVICE_ID.set(result.getString("id"));
						settings.BACKUP_USER_ID.set(result.getString("userid"));
						settings.BACKUP_NATIVE_DEVICE_ID.set(result.getString("deviceid"));
						settings.BACKUP_ACCESS_TOKEN.set(result.getString("accesstoken"));
						settings.BACKUP_ACCESS_TOKEN_UPDATE_TIME.set(result.getString("udpatetime"));
						success = true;
						app.showToastMessage("Device have been registered successfully");
					} catch (JSONException e) {
						app.showToastMessage("Device registration error: json parsing");
					}
				} else {
					app.showToastMessage("Device registration error: empty response");
				}
				if (listener != null) {
					listener.onResult(success, resultJson);
				}
			}
		});
	}

	private void uploadFiles() {
		LoadGpxTask loadGpxTask = new LoadGpxTask(this, new LoadGpxTask.OnLoadGpxListener() {
			@Override
			public void onLoadGpxDone(@NonNull List<GpxInfo> result) {
				uploadFiles(result);
			}
		});
		loadGpxTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this);
	}

	private void uploadFiles(List<GpxInfo> gpxFiles) {
		//{"status":"ok"}
		final WeakReference<TestBackupActivity> activityRef = new WeakReference<>(this);

		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		Map<String, String> headers = new HashMap<>();
		headers.put("Accept-Encoding", "deflate, gzip");

		final Map<File, GpxInfo> gpxInfos = new HashMap<>();
		for (GpxInfo gpxFile : gpxFiles) {
			gpxInfos.put(gpxFile.file, gpxFile);
		}
		final List<File> files = new ArrayList<>(gpxInfos.keySet());
		File favoritesFile = app.getFavorites().getExternalFile();
		files.add(favoritesFile);

		final ProgressImplementation progress = ProgressImplementation.createProgressDialog(this,
				"Create backup", "Uploading " + files.size() + " file(s) to server", ProgressDialog.STYLE_HORIZONTAL);

		AndroidNetworkUtils.uploadFilesAsync("https://osmand.net/userdata/upload-file", files, true, params, headers, new OnFilesUploadCallback() {
			@Nullable
			@Override
			public Map<String, String> getAdditionalParams(@NonNull File file) {
				GpxInfo gpxInfo = gpxInfos.get(file);
				Map<String, String> additionaParams = new HashMap<>();
				additionaParams.put("name", gpxInfo == null ? file.getName() : gpxInfo.getFileName(true));
				additionaParams.put("type", Algorithms.getFileExtension(file));
				return additionaParams;
			}

			@Override
			public void onFileUploadProgress(@NonNull File file, int percent) {
				Activity a = activityRef.get();
				if (AndroidUtils.isActivityNotDestroyed(a)) {
					if (percent < 100) {
						progress.startTask(file.getName(), percent);
					} else {
						progress.finishTask();
					}
				}
			}

			@Override
			public void onFilesUploadDone(@NonNull Map<File, String> errors) {
				Activity a = activityRef.get();
				if (AndroidUtils.isActivityNotDestroyed(a)) {
					app.runInUIThread(new Runnable() {
						@Override
						public void run() {
							try {
								if (progress.getDialog().isShowing()) {
									progress.getDialog().dismiss();
								}
							} catch (Exception e) {
								//ignored
							}
						}
					}, 300);
					app.showToastMessage("Uploaded " + (files.size() - errors.size() + " files" +
							(errors.size() > 0 ? ". Errors: " + errors.size() : "")));
					loadBackupInfo();
				}
			}
		});
	}

	private void loadBackupInfo(@Nullable final OnResultListener listener) {
		Map<String, String> params = new HashMap<>();
		params.put("deviceid", getDeviceId());
		params.put("accessToken", getAccessToken());
		AndroidNetworkUtils.sendRequestAsync(app, "https://osmand.net/userdata/list-files", params, "Get backup info", true, false, new OnRequestResultListener() {
			@Override
			public void onResult(String resultJson) {
				boolean success = false;
				StringBuilder resultString = new StringBuilder();
				if (!Algorithms.isEmpty(resultJson)) {
					try {
						/*
						{
							"totalZipSize": 21792,
								"totalFileSize": 185920,
								"totalFiles": 1,
								"totalFileVersions": 2,
								"uniqueFiles": [
									{
										"userid": 1033,
											"id": 7,
											"deviceid": 1034,
											"filesize": 92960,
											"type": "gpx",
											"name": "test/Day 2.gpx",
											"updatetime": "Apr 11, 2021, 1:49:01 PM",
											"updatetimems": 1618141741822,
											"zipSize": 10896
									}
								],
							"deviceid": 1034
						}
						*/
						JSONObject result = new JSONObject(resultJson);
						String totalZipSize = result.getString("totalZipSize");
						String totalFiles = result.getString("totalFiles");
						String totalFileVersions = result.getString("totalFileVersions");
						JSONArray files = result.getJSONArray("uniqueFiles");
						resultString.append("Total files: ").append(totalFiles).append("\n");
						resultString.append("Total zip size: ").append(AndroidUtils.formatSize(app, Long.parseLong(totalZipSize))).append("\n");
						resultString.append("Total file versions: ").append(totalFileVersions);

						success = true;
					} catch (JSONException e) {
					}
				}
				if (listener != null) {
					listener.onResult(success, resultString.toString());
				}
			}
		});
	}

	private boolean isTokenValid(String token) {
		return token.matches("[0-9]+");
	}

	private int resolveResourceId(final Activity activity, final int attr) {
		final TypedValue typedvalueattr = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}

	private static class LoadGpxTask extends AsyncTask<Activity, GpxInfo, List<GpxInfo>> {

		private final OsmandApplication app;
		private final OnLoadGpxListener listener;
		private final WeakReference<Activity> activityRef;
		private List<GpxInfo> result;
		private ProgressImplementation progress;

		interface OnLoadGpxListener {
			void onLoadGpxDone(@NonNull List<GpxInfo> result);
		}

		LoadGpxTask(@NonNull Activity activity, @Nullable OnLoadGpxListener listener) {
			this.activityRef = new WeakReference<>(activity);
			this.app = (OsmandApplication) activity.getApplication();
			this.listener = listener;
		}

		public List<GpxInfo> getResult() {
			return result;
		}

		@NonNull
		@Override
		protected List<GpxInfo> doInBackground(Activity... params) {
			List<GpxInfo> result = new ArrayList<>();
			loadGPXData(app.getAppPath(IndexConstants.GPX_INDEX_DIR), result, this);
			return result;
		}

		public void loadFile(GpxInfo... loaded) {
			publishProgress(loaded);
		}

		@Override
		protected void onPreExecute() {
			Activity a = activityRef.get();
			if (AndroidUtils.isActivityNotDestroyed(a)) {
				progress = ProgressImplementation.createProgressDialog(a,
						"Create backup", "Collecting gpx files...", ProgressDialog.STYLE_HORIZONTAL);
			}
		}

		@Override
		protected void onProgressUpdate(GpxInfo... values) {
			Activity a = activityRef.get();
			if (AndroidUtils.isActivityNotDestroyed(a)) {
				progress.startTask(values[0].getFileName(true), -1);
			}
		}

		@Override
		protected void onPostExecute(@NonNull List<GpxInfo> result) {
			this.result = result;
			if (listener != null) {
				listener.onLoadGpxDone(result);
			}
			Activity a = activityRef.get();
			if (AndroidUtils.isActivityNotDestroyed(a)) {
				progress.finishTask();
				app.runInUIThread(new Runnable() {
					@Override
					public void run() {
						try {
							if (progress.getDialog().isShowing()) {
								progress.getDialog().dismiss();
							}
						} catch (Exception e) {
							//ignored
						}
					}
				}, 300);
			}
		}

		private void loadGPXData(File mapPath, List<GpxInfo> result, LoadGpxTask loadTask) {
			if (mapPath.canRead()) {
				List<GpxInfo> progress = new ArrayList<>();
				loadGPXFolder(mapPath, result, loadTask, progress, "");
				if (!progress.isEmpty()) {
					loadTask.loadFile(progress.toArray(new GpxInfo[0]));
				}
			}
		}

		private void loadGPXFolder(File mapPath, List<GpxInfo> result, LoadGpxTask loadTask, List<GpxInfo> progress,
								   String gpxSubfolder) {
			File[] listFiles = mapPath.listFiles();
			if (listFiles != null) {
				for (File gpxFile : listFiles) {
					if (gpxFile.isDirectory()) {
						String sub = gpxSubfolder.length() == 0 ? gpxFile.getName() : gpxSubfolder + "/"
								+ gpxFile.getName();
						loadGPXFolder(gpxFile, result, loadTask, progress, sub);
					} else if (gpxFile.isFile() && gpxFile.getName().toLowerCase().endsWith(IndexConstants.GPX_FILE_EXT)) {
						GpxInfo info = new GpxInfo();
						info.subfolder = gpxSubfolder;
						info.file = gpxFile;
						result.add(info);
						progress.add(info);
						if (progress.size() > 7) {
							loadTask.loadFile(progress.toArray(new GpxInfo[0]));
							progress.clear();
						}
					}
				}
			}
		}
	}

	private static class GpxInfo {
		public File file;
		public String subfolder;

		private String name = null;
		private int sz = -1;
		private String fileName = null;

		public String getName() {
			if (name == null) {
				name = formatName(file.getName());
			}
			return name;
		}

		private String formatName(String name) {
			int ext = name.lastIndexOf('.');
			if (ext != -1) {
				name = name.substring(0, ext);
			}
			return name.replace('_', ' ');
		}

		// Usage: AndroidUtils.formatSize(v.getContext(), getSize() * 1024l);
		public int getSize() {
			if (sz == -1) {
				if (file == null) {
					return -1;
				}
				sz = (int) ((file.length() + 512) >> 10);
			}
			return sz;
		}

		public long getFileDate() {
			if (file == null) {
				return 0;
			}
			return file.lastModified();
		}

		public String getFileName(boolean includeSubfolder) {
			String result;
			if (fileName != null) {
				result = fileName;
			} else {
				if (file == null) {
					result = "";
				} else {
					result = fileName = file.getName();
				}
			}
			if (includeSubfolder && !Algorithms.isEmpty(subfolder)) {
				result = subfolder + "/" + result;
			}
			return result;
		}
	}
}
