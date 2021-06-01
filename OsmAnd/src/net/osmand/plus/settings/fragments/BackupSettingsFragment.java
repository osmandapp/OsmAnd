package net.osmand.plus.settings.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupHelper.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;

import org.apache.commons.logging.Log;

import java.util.Map;

public class BackupSettingsFragment extends BaseOsmAndFragment {

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

		TextView userName = view.findViewById(R.id.user_name);
		userName.setText(backupHelper.getEmail());

		View logout = view.findViewById(R.id.logout_container);
		AndroidUtils.setBackground(logout, UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f));
		logout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					backupHelper.logout();
					activity.onBackPressed();
				}
			}
		});

		View deleteAll = view.findViewById(R.id.delete_all_container);
		AndroidUtils.setBackground(deleteAll, UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f));
		deleteAll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					deleteAllData();
				}
			}
		});

		return view;
	}

	private void deleteAllData() {
		try {
			AndroidUiHelper.setVisibility(View.VISIBLE, progressBar);
			backupHelper.deleteAllFiles(new OnDeleteFilesListener() {
				@Override
				public void onFileDeleteProgress(@NonNull RemoteFile file) {

				}

				@Override
				public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {
					AndroidUiHelper.setVisibility(View.INVISIBLE, progressBar);
				}

				@Override
				public void onFilesDeleteError(int status, @NonNull String message) {

				}
			});
		} catch (UserNotRegisteredException e) {
			log.error(e);
		}
	}
}