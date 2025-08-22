package net.osmand.plus.backup.ui;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.DeleteAllDataConfirmationBottomSheet.OnConfirmDeletionListener;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupSettingsFragment extends BaseFullScreenFragment implements OnDeleteFilesListener,
		OnConfirmDeletionListener, OnPrepareBackupListener {

	public static final String TAG = BackupSettingsFragment.class.getSimpleName();

	private static final Log log = PlatformUtil.getLog(BackupSettingsFragment.class);

	private BackupHelper backupHelper;
	private NetworkSettingsHelper settingsHelper;

	private Map<String, RemoteFile> oldRemoteFiles = new HashMap<>();
	private Map<String, RemoteFile> uniqueRemoteFiles = new HashMap<>();

	private ProgressBar progressBar;


	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		backupHelper = app.getBackupHelper();
		settingsHelper = app.getNetworkSettingsHelper();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_backup_settings, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		progressBar = view.findViewById(R.id.progress_bar);

		setupToolbar(view);
		setupAccount(view);
		setupBackupTypes(view);
		setupDeleteAllData(view);
		setupRemoveOldData(view);
		setupDeleteAccountData(view);
		setupVersionHistory(view);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		backupHelper.addPrepareBackupListener(this);
		if (!backupHelper.isBackupPreparing()) {
			onBackupPrepared(backupHelper.getBackup());
		}
		backupHelper.getBackupListeners().addDeleteFilesListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		backupHelper.removePrepareBackupListener(this);
		backupHelper.getBackupListeners().removeDeleteFilesListener(this);
	}

	protected void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.shared_string_settings);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	private void setupBackupTypes(@NonNull View view) {
		View container = view.findViewById(R.id.select_types_container);
		TextView title = container.findViewById(android.R.id.title);
		title.setText(R.string.backup_storage_taken);

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_storage));

		container.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				BackupDataController.showScreen(activity);
			}
		});
		setupSelectableBackground(container);

		TextView summary = container.findViewById(android.R.id.summary);
		setupSizeSummary(summary, uniqueRemoteFiles);
	}

	private void setupAccount(@NonNull View view) {
		TextView userName = view.findViewById(R.id.user_name);
		userName.setText(backupHelper.getEmail());

		View container = view.findViewById(R.id.logout_container);

		TextView title = container.findViewById(android.R.id.title);
		title.setText(R.string.shared_string_logout);
		title.setTextColor(ContextCompat.getColor(app, R.color.color_osm_edit_delete));

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_logout, R.color.color_osm_edit_delete));

		container.setOnClickListener(v -> {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				LogoutBottomSheet.showInstance(fragmentManager, this);
			}
		});
		setupSelectableBackground(container);
	}

	protected void logout() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			backupHelper.logout();
			((MapActivity) activity).getFragmentsHelper().dismissFragment(BackupCloudFragment.TAG);
			BackupAuthorizationFragment.showInstance(getActivity().getSupportFragmentManager());
		}
	}

	private void setupVersionHistory(@NonNull View view) {
		View container = view.findViewById(R.id.version_history);

		TextView title = container.findViewById(android.R.id.title);
		title.setText(R.string.backup_storage_taken);

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_storage));

		container.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				VersionHistoryController.showScreen(activity);
			}
		});
		setupSelectableBackground(container);

		TextView summary = container.findViewById(android.R.id.summary);
		setupSizeSummary(summary, oldRemoteFiles);
	}

	private void setupSizeSummary(@NonNull TextView summary, @NonNull Map<String, RemoteFile> remoteFiles) {
		if (!Algorithms.isEmpty(remoteFiles)) {
			int filesSize = 0;
			for (RemoteFile remoteFile : remoteFiles.values()) {
				filesSize += remoteFile.getZipSize();
			}
			summary.setText(AndroidUtils.formatSize(app, filesSize));
			AndroidUiHelper.updateVisibility(summary, true);
		} else {
			AndroidUiHelper.updateVisibility(summary, false);
		}
	}

	private void setupDeleteAllData(@NonNull View view) {
		View container = view.findViewById(R.id.delete_all_container);

		TextView title = container.findViewById(android.R.id.title);
		title.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), getString(R.string.backup_delete_all_data), getString(R.string.backup_delete_all_data)));
		title.setTextColor(ContextCompat.getColor(app, R.color.deletion_color_warning));

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_file_delete, R.color.deletion_color_warning));

		container.setOnClickListener(v -> {
			if (!settingsHelper.isBackupExporting()) {
				if (!Algorithms.isEmpty(backupHelper.getBackup().getRemoteFiles())) {
					FragmentManager fragmentManager = getFragmentManager();
					if (fragmentManager != null) {
						DeleteAllDataBottomSheet.showInstance(fragmentManager, BackupSettingsFragment.this);
					}
				} else {
					app.showShortToastMessage(R.string.backup_data_removed);
				}
			}
		});
		setupSelectableBackground(container);
	}

	private void setupDeleteAccountData(@NonNull View view) {
		View container = view.findViewById(R.id.delete_account_container);

		String deleteAccount = getString(R.string.delete_account);
		TextView title = container.findViewById(android.R.id.title);
		title.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), deleteAccount, deleteAccount));
		title.setTextColor(ContextCompat.getColor(app, R.color.deletion_color_warning));

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_user_account_delete, R.color.deletion_color_warning));

		container.setOnClickListener(v -> {
			if (!settingsHelper.isBackupExporting()) {
				FragmentManager fragmentManager = getFragmentManager();
				if (fragmentManager != null) {
					AuthorizeFragment.showInstance(fragmentManager, LoginDialogType.DELETE_ACCOUNT);
				}
			}
		});
		setupSelectableBackground(container);

		TextView summary = container.findViewById(android.R.id.summary);
		AndroidUiHelper.updateVisibility(summary, false);
	}

	private void setupRemoveOldData(@NonNull View view) {
		View container = view.findViewById(R.id.delete_old_container);
		TextView title = container.findViewById(android.R.id.title);
		title.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), getString(R.string.backup_delete_old_data), getString(R.string.backup_delete_old_data)));
		title.setTextColor(ContextCompat.getColor(app, R.color.deletion_color_warning));

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_history_delete, R.color.deletion_color_warning));
		container.setOnClickListener(v -> {
			if (!settingsHelper.isBackupExporting()) {
				if (!Algorithms.isEmpty(backupHelper.getBackup().getRemoteFiles(RemoteFilesType.OLD))) {
					FragmentManager fragmentManager = getFragmentManager();
					if (fragmentManager != null) {
						RemoveOldVersionsBottomSheet.showInstance(fragmentManager, BackupSettingsFragment.this);
					}
				} else {
					app.showShortToastMessage(R.string.backup_version_history_removed);
				}
			}
		});
		setupSelectableBackground(container);
	}

	private void setupSelectableBackground(@NonNull View view) {
		View selectableView = view.findViewById(R.id.selectable_list_item);
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		AndroidUtils.setBackground(selectableView, drawable);
	}

	@Override
	public void onBackupPreparing() {
		updateProgressVisibility(true);
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		updateProgressVisibility(false);
		if (backupResult != null && Algorithms.isEmpty(backupResult.getError())) {
			oldRemoteFiles = backupResult.getRemoteFiles(RemoteFilesType.OLD);
			uniqueRemoteFiles = backupResult.getRemoteFiles(RemoteFilesType.UNIQUE);
			View view = getView();
			if (view != null) {
				setupBackupTypes(view);
				setupVersionHistory(view);
			}
		}
	}

	private void deleteAllFiles() {
		try {
			updateProgressVisibility(true);
			backupHelper.deleteAllFiles(null);
		} catch (UserNotRegisteredException e) {
			updateProgressVisibility(false);
			log.error(e);
		}
	}

	protected void deleteOldFiles() {
		try {
			updateProgressVisibility(true);
			backupHelper.deleteOldFiles(null);
		} catch (UserNotRegisteredException e) {
			updateProgressVisibility(false);
			log.error(e);
		}
	}

	private void updateProgressVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(progressBar, visible);
	}

	@Override
	public void onFilesDeleteStarted(@NonNull List<RemoteFile> files) {
		if (!settingsHelper.isBackupExporting()) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				DeleteProgressBottomSheet.showInstance(activity.getSupportFragmentManager(), files.size());
			}
		}
	}

	@Override
	public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {
		updateProgressVisibility(false);
		backupHelper.prepareBackup();
	}

	@Override
	public void onFilesDeleteError(int status, @NonNull String message) {
		updateProgressVisibility(false);
		backupHelper.prepareBackup();
	}

	@Override
	public void onDeletionConfirmed() {
		deleteAllFiles();
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			BackupSettingsFragment fragment = new BackupSettingsFragment();
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}