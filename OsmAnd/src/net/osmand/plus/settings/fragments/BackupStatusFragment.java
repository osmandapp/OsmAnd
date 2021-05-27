package net.osmand.plus.settings.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.backend.OsmandSettings;

public class BackupStatusFragment extends BaseOsmAndFragment {

	private OsmandApplication app;
	private OsmandSettings settings;
	private BackupHelper backupHelper;

	private boolean nightMode;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		backupHelper = app.getBackupHelper();
		nightMode = !settings.isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_backup_status, container, false);

		return view;
	}
}