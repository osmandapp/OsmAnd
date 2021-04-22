package net.osmand.plus.development;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupHelper.BackupInfo;
import net.osmand.plus.backup.BackupHelper.OnRegisterUserListener;
import net.osmand.plus.backup.BackupTask;
import net.osmand.plus.backup.BackupTask.OnBackupListener;
import net.osmand.plus.backup.GpxFileInfo;
import net.osmand.plus.backup.PrepareBackupTask;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.UserFile;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.OsmandTextFieldBoxes;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

public class TestBackupActivity extends OsmandActionBarActivity {

	private static final DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

	private OsmandApplication app;
	private OsmandSettings settings;
	private BackupHelper backupHelper;

	private ProgressBar progressBar;
	private View buttonRegister;
	private View buttonVerify;
	private View buttonRefresh;
	private View buttonBackup;
	private View buttonRestore;
	private EditText emailEditText;
	private OsmandTextFieldBoxes tokenEdit;
	private EditText tokenEditText;
	private TextView infoView;

	private BackupInfo backupInfo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		app = getMyApplication();
		settings = app.getSettings();
		backupHelper = app.getBackupHelper();
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
		buttonRefresh = findViewById(R.id.btn_refresh);
		UiUtilities.setupDialogButton(nightMode, buttonRefresh, DialogButtonType.PRIMARY, "Refresh");
		buttonBackup = findViewById(R.id.btn_backup);
		UiUtilities.setupDialogButton(nightMode, buttonBackup, DialogButtonType.PRIMARY, "Backup");
		buttonRestore = findViewById(R.id.btn_restore);
		UiUtilities.setupDialogButton(nightMode, buttonRestore, DialogButtonType.PRIMARY, "Restore");

		tokenEdit = findViewById(R.id.edit_token_label);
		tokenEditText = findViewById(R.id.edit_token);
		infoView = findViewById(R.id.text_info);
		progressBar = findViewById(R.id.progress_bar);

		buttonVerify.setEnabled(false);
		emailEditText = findViewById(R.id.edit_email);
		String email = settings.BACKUP_USER_EMAIL.get();
		if (!Algorithms.isEmpty(email)) {
			emailEditText.setText(email);
		}
		if (backupHelper.isRegistered()) {
			tokenEdit.setVisibility(View.GONE);
			buttonVerify.setVisibility(View.GONE);
		} else {
			tokenEdit.setVisibility(View.VISIBLE);
			buttonVerify.setVisibility(View.VISIBLE);
		}
		buttonRegister.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String email = emailEditText.getText().toString();
				if (AndroidUtils.isValidEmail(email)) {
					buttonRegister.setEnabled(false);
					settings.BACKUP_USER_EMAIL.set(email);
					progressBar.setVisibility(View.VISIBLE);
					backupHelper.registerUser(email, new OnRegisterUserListener() {
						@Override
						public void onRegisterUser(int status, @Nullable String message) {
							TestBackupActivity a = activityRef.get();
							if (AndroidUtils.isActivityNotDestroyed(a)) {
								a.progressBar.setVisibility(View.GONE);
								a.buttonRegister.setEnabled(status != BackupHelper.STATUS_SUCCESS);
								a.tokenEdit.setVisibility(View.VISIBLE);
								a.buttonVerify.setVisibility(View.VISIBLE);
								a.buttonVerify.setEnabled(status == BackupHelper.STATUS_SUCCESS);
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
				if (BackupHelper.isTokenValid(token)) {
					buttonVerify.setEnabled(false);
					progressBar.setVisibility(View.VISIBLE);
					backupHelper.registerDevice(token, new BackupHelper.OnRegisterDeviceListener() {

						@Override
						public void onRegisterDevice(int status, @Nullable String message) {
							TestBackupActivity a = activityRef.get();
							if (AndroidUtils.isActivityNotDestroyed(a)) {
								a.progressBar.setVisibility(View.GONE);
								a.buttonVerify.setEnabled(status != BackupHelper.STATUS_SUCCESS);
								if (status == BackupHelper.STATUS_SUCCESS) {
									tokenEdit.setVisibility(View.GONE);
									buttonVerify.setVisibility(View.GONE);
								}
								a.prepareBackup();
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
		buttonRefresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				prepareBackup();
			}
		});
		buttonBackup.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (backupInfo != null) {
					buttonBackup.setEnabled(false);
					BackupTask task = new BackupTask(backupInfo, TestBackupActivity.this, new OnBackupListener() {
						@Override
						public void onBackupDone(@Nullable Map<File, String> uploadErrors, @Nullable Map<File, String> downloadErrors,
												 @Nullable Map<UserFile, String> deleteErrors, @Nullable String error) {
							TestBackupActivity a = activityRef.get();
							if (AndroidUtils.isActivityNotDestroyed(a)) {
								String description;
								if (error != null) {
									description = error;
								} else if (uploadErrors == null && downloadErrors == null) {
									description = "No data";
								} else {
									description = getBackupErrorsDescription(uploadErrors, downloadErrors, deleteErrors, error);
								}
								a.infoView.setText(description);
								a.infoView.requestFocus();
								a.prepareBackup();
								a.buttonBackup.setEnabled(true);
							}
						}
					});
					task.runBackup();
				}
			}
		});
		buttonRestore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (backupInfo != null) {
					buttonRestore.setEnabled(false);
					BackupTask task = new BackupTask(backupInfo, TestBackupActivity.this, new OnBackupListener() {
						@Override
						public void onBackupDone(@Nullable Map<File, String> uploadErrors, @Nullable Map<File, String> downloadErrors,
												 @Nullable Map<UserFile, String> deleteErrors, @Nullable String error) {
							TestBackupActivity a = activityRef.get();
							if (AndroidUtils.isActivityNotDestroyed(a)) {
								String description;
								if (error != null) {
									description = error;
								} else if (uploadErrors == null && downloadErrors == null) {
									description = "No data";
								} else {
									description = getBackupErrorsDescription(uploadErrors, downloadErrors, deleteErrors, error);
								}
								a.infoView.setText(description);
								a.infoView.requestFocus();
								a.prepareBackup();
								a.buttonRestore.setEnabled(true);
							}
						}
					});
					task.runRestore();
				}
			}
		});

		prepareBackup();
	}

	private String getBackupErrorsDescription(@Nullable Map<File, String> uploadErrors, @Nullable Map<File, String> downloadErrors, @Nullable Map<UserFile, String> deleteErrors, @Nullable String error) {
		StringBuilder sb = new StringBuilder();
		if (!Algorithms.isEmpty(uploadErrors)) {
			sb.append("--- Upload errors ---").append("\n");
			for (Entry<File, String> uploadEntry : uploadErrors.entrySet()) {
				sb.append(uploadEntry.getKey().getName()).append(": ").append(uploadEntry.getValue()).append("\n");
			}
		}
		if (!Algorithms.isEmpty(downloadErrors)) {
			sb.append("--- Download errors ---").append("\n");
			for (Entry<File, String> downloadEntry : downloadErrors.entrySet()) {
				sb.append(downloadEntry.getKey().getName()).append(": ").append(downloadEntry.getValue()).append("\n");
			}
		}
		if (!Algorithms.isEmpty(deleteErrors)) {
			sb.append("--- Delete errors ---").append("\n");
			for (Entry<UserFile, String> deleteEntry : deleteErrors.entrySet()) {
				sb.append(deleteEntry.getKey().getName()).append(": ").append(deleteEntry.getValue()).append("\n");
			}
		}
		return sb.length() == 0 ? "OK" : sb.toString();
	}

	private String getBackupDescription(@NonNull BackupInfo backupInfo) {
		StringBuilder sb = new StringBuilder();
		if (!Algorithms.isEmpty(backupInfo.filesToUpload)) {
			sb.append("\n").append("--- Upload ---").append("\n");
			for (GpxFileInfo info : backupInfo.filesToUpload) {
				sb.append(info.getFileName(true))
						.append(" L: ").append(DF.format(new Date(info.getFileDate())))
						.append(" U: ").append(DF.format(new Date(info.uploadTime)))
						.append("\n");
			}
		}
		if (!Algorithms.isEmpty(backupInfo.filesToDownload)) {
			sb.append("\n").append("--- Download ---").append("\n");
			for (UserFile userFile : backupInfo.filesToDownload) {
				sb.append(userFile.getName())
						.append(" R: ").append(DF.format(new Date(userFile.getClienttimems())))
						.append("\n");
			}
		}
		if (!Algorithms.isEmpty(backupInfo.filesToDelete)) {
			sb.append("\n").append("--- Delete ---").append("\n");
			for (UserFile userFile : backupInfo.filesToDelete) {
				sb.append(userFile.getName())
						.append(" R: ").append(DF.format(new Date(userFile.getClienttimems())))
						.append("\n");
			}
		}
		if (!Algorithms.isEmpty(backupInfo.filesToMerge)) {
			sb.append("\n").append("--- Conflicts ---").append("\n");
			for (Pair<GpxFileInfo, UserFile> localRemote : backupInfo.filesToMerge) {
				GpxFileInfo local = localRemote.first;
				UserFile remote = localRemote.second;
				sb.append(local.getFileName(true))
						.append(" L: ").append(DF.format(new Date(local.getFileDate())))
						.append(" U: ").append(DF.format(new Date(local.uploadTime)))
						.append(" R: ").append(DF.format(new Date(remote.getClienttimems())))
						.append("\n");
			}
		}
		return sb.toString();
	}

	private void prepareBackup() {
		final WeakReference<TestBackupActivity> activityRef = new WeakReference<>(this);
		buttonRefresh.setEnabled(false);
		PrepareBackupTask prepareBackupTask = new PrepareBackupTask(this, new OnPrepareBackupListener() {
			@Override
			public void onBackupPrepared(@Nullable BackupInfo backupInfo, @Nullable String error) {
				TestBackupActivity.this.backupInfo = backupInfo;
				TestBackupActivity a = activityRef.get();
				if (AndroidUtils.isActivityNotDestroyed(a)) {
					String description = "Last uploaded: " + DF.format(new Date(settings.BACKUP_LAST_UPLOADED_TIME.get())) + "\n\n";
					if (error != null) {
						description += error;
					} else if (backupInfo == null) {
						description += "No data";
					} else {
						description += "Files to upload: " + backupInfo.filesToUpload.size()
								+ "\nFiles to download: " + backupInfo.filesToDownload.size()
								+ "\nFiles to delete: " + backupInfo.filesToDelete.size()
								+ "\nConflicts: " + backupInfo.filesToMerge.size()
								+ "\n" + getBackupDescription(backupInfo);
					}
					a.infoView.setText(description);
					a.infoView.requestFocus();
					a.buttonRefresh.setEnabled(true);
				}
			}
		});
		prepareBackupTask.prepare();
	}

	private int resolveResourceId(final Activity activity, final int attr) {
		final TypedValue typedvalueattr = new TypedValue();
		activity.getTheme().resolveAttribute(attr, typedvalueattr, true);
		return typedvalueattr.resourceId;
	}
}
