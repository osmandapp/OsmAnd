package net.osmand.plus.settings.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.backend.OsmandSettings;

public class BackupSettingsFragment extends BaseOsmAndFragment {

	private OsmandApplication app;
	private OsmandSettings settings;
	private BackupHelper backupHelper;

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
		settings = app.getSettings();
		backupHelper = app.getBackupHelper();
		nightMode = !settings.isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_backup_settings, container, false);

		TextView userName = view.findViewById(R.id.user_name);
		userName.setText(backupHelper.getEmail());

		View logout = view.findViewById(R.id.logout_container);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, getActiveColor(), 0.3f);
		AndroidUtils.setBackground(logout, drawable);
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

		return view;
	}
}