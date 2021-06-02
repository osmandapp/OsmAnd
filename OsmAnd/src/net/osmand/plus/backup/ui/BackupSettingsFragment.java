package net.osmand.plus.backup.ui;

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
import net.osmand.plus.backup.BackupHelper.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.DeleteAllDataConfirmationBottomSheet.OnConfirmDeletionListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;

import org.apache.commons.logging.Log;

import java.util.Map;

public class BackupSettingsFragment extends BaseOsmAndFragment implements OnDeleteFilesListener, OnConfirmDeletionListener {

	private final static Log log = PlatformUtil.getLog(BackupSettingsFragment.class);

	private OsmandApplication app;
	private BackupHelper backupHelper;

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
		setupDeleteAllData(view);
		setupRemoveOldData(view);

		return view;
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
		AndroidUtils.setBackground(container, UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f));
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
		AndroidUtils.setBackground(container, UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f));
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
		AndroidUtils.setBackground(container, UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f));
	}

	private void deleteAllFiles() {
		try {
			updateProgressVisibility(true);
			backupHelper.deleteAllFiles(BackupSettingsFragment.this);
		} catch (UserNotRegisteredException e) {
			updateProgressVisibility(false);
			log.error(e);
		}
	}

	protected void deleteOldFiles() {
		try {
			updateProgressVisibility(true);
			backupHelper.deleteOldFiles(BackupSettingsFragment.this);
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
	}

	@Override
	public void onFilesDeleteError(int status, @NonNull String message) {
		updateProgressVisibility(false);
	}

	@Override
	public void onDeletionConfirmed() {
		deleteAllFiles();
	}
}