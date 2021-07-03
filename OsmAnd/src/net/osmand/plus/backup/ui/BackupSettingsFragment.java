package net.osmand.plus.backup.ui;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.DeleteAllDataConfirmationBottomSheet.OnConfirmDeletionListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BackupSettingsFragment extends BaseOsmAndFragment implements OnDeleteFilesListener,
		OnConfirmDeletionListener, OnPrepareBackupListener {

	private final static Log log = PlatformUtil.getLog(BackupSettingsFragment.class);

	private OsmandApplication app;
	private BackupHelper backupHelper;

	private List<RemoteFile> oldRemoteFiles = new ArrayList<>();

	private ProgressBar progressBar;

	private boolean nightMode;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@ColorRes
	private int getActiveColorRes() {
		return nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
	}

	@ColorInt
	private int getActiveColor() {
		return ContextCompat.getColor(app, getActiveColorRes());
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		backupHelper = app.getBackupHelper();
		nightMode = !app.getSettings().isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_backup_settings, container, false);
		progressBar = view.findViewById(R.id.progress_bar);

		setupAccount(view);
		setupBackupTypes(view);
		setupDeleteAllData(view);
		setupRemoveOldData(view);
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

	private void setupBackupTypes(View view) {
		View container = view.findViewById(R.id.select_types_container);
		TextView title = container.findViewById(android.R.id.title);
		TextView summary = container.findViewById(android.R.id.summary);

		String text = getString(R.string.backup_data);
		Typeface typeface = FontCache.getRobotoMedium(app);
		title.setText(UiUtilities.createCustomFontSpannable(typeface, text, text));
		summary.setText(R.string.select_backup_data_descr);
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					BackupTypesFragment.showInstance(activity.getSupportFragmentManager());
				}
			}
		});
		setupSelectableBackground(container);
		AndroidUiHelper.updateVisibility(container.findViewById(android.R.id.icon), false);
	}

	private void setupAccount(View view) {
		TextView userName = view.findViewById(R.id.user_name);
		userName.setText(backupHelper.getEmail());

		View container = view.findViewById(R.id.logout_container);

		TextView title = container.findViewById(android.R.id.title);
		title.setText(R.string.shared_string_logout);
		title.setTextColor(ContextCompat.getColor(app, R.color.color_osm_edit_delete));

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_logout, R.color.color_osm_edit_delete));

		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					backupHelper.logout();
					activity.onBackPressed();
				}
			}
		});
		setupSelectableBackground(container);
	}

	private void setupVersionHistory(View view) {
		View container = view.findViewById(R.id.version_history);

		TextView title = container.findViewById(android.R.id.title);
		title.setText(R.string.backup_storage_taken);

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_storage));

		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					VersionHistoryFragment.showInstance(activity.getSupportFragmentManager());
				}
			}
		});
		setupSelectableBackground(container);

		TextView summary = container.findViewById(android.R.id.summary);
		if (!Algorithms.isEmpty(oldRemoteFiles)) {
			int filesSize = 0;
			for (RemoteFile remoteFile : oldRemoteFiles) {
				filesSize += remoteFile.getFilesize();
			}
			summary.setText(AndroidUtils.formatSize(app, filesSize));
			AndroidUiHelper.updateVisibility(summary, true);
		} else {
			AndroidUiHelper.updateVisibility(summary, false);
		}
	}

	private void setupDeleteAllData(View view) {
		View container = view.findViewById(R.id.delete_all_container);

		TextView title = container.findViewById(android.R.id.title);
		title.setText(UiUtilities.createCustomFontSpannable(FontCache.getRobotoMedium(app), getString(R.string.backup_delete_all_data), getString(R.string.backup_delete_all_data)));
		title.setTextColor(ContextCompat.getColor(app, R.color.color_osm_edit_delete));

		TextView summary = container.findViewById(android.R.id.summary);
		summary.setText(R.string.backup_delete_all_data_descr);

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_file_delete, R.color.color_osm_edit_delete));

		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getFragmentManager();
				if (fragmentManager != null) {
					DeleteAllDataBottomSheet.showInstance(fragmentManager, BackupSettingsFragment.this);
				}
			}
		});
		setupSelectableBackground(container);
	}

	private void setupRemoveOldData(View view) {
		View container = view.findViewById(R.id.delete_old_container);
		TextView title = container.findViewById(android.R.id.title);
		title.setText(UiUtilities.createCustomFontSpannable(FontCache.getRobotoMedium(app), getString(R.string.backup_delete_old_data), getString(R.string.backup_delete_old_data)));
		title.setTextColor(ContextCompat.getColor(app, R.color.color_osm_edit_delete));

		TextView summary = container.findViewById(android.R.id.summary);
		summary.setText(R.string.backup_delete_old_data_descr);

		ImageView icon = container.findViewById(android.R.id.icon);
		icon.setImageDrawable(getIcon(R.drawable.ic_action_history_delete, R.color.color_osm_edit_delete));
		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getFragmentManager();
				if (fragmentManager != null) {
					RemoveOldVersionsBottomSheet.showInstance(fragmentManager, BackupSettingsFragment.this);
				}
			}
		});
		setupSelectableBackground(container);
	}

	private void setupSelectableBackground(View view) {
		View selectableView = view.findViewById(R.id.selectable_list_item);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f);
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
			View view = getView();
			if (view != null) {
				setupVersionHistory(view);
			}
		}
	}

	private void deleteAllFiles() {
		try {
			updateProgressVisibility(true);
			backupHelper.deleteAllFiles(Arrays.asList(ExportSettingsType.values()));
		} catch (UserNotRegisteredException e) {
			updateProgressVisibility(false);
			log.error(e);
		}
	}

	protected void deleteOldFiles() {
		try {
			updateProgressVisibility(true);
			backupHelper.deleteOldFiles(Arrays.asList(ExportSettingsType.values()));
		} catch (UserNotRegisteredException e) {
			updateProgressVisibility(false);
			log.error(e);
		}
	}

	private void updateProgressVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(progressBar, visible);
	}

	@Override
	public void onFileDeleteProgress(@NonNull RemoteFile file) {

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
}