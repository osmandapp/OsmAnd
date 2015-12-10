package net.osmand.plus.liveupdates;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class SettingsDialogFragment extends DialogFragment {
	public static final String LOCAL_INDEX = "local_index";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		View view = LayoutInflater.from(getActivity())
				.inflate(R.layout.dialog_live_updates_item_settings, null);
		final LocalIndexInfo localIndexInfo = getArguments().getParcelable(LOCAL_INDEX);
		builder.setView(view)
				.setPositiveButton("SAVE", null)
				.setNegativeButton("CANCEL", null)
				.setNeutralButton("UPDATE NOW", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getLiveUpdatesFragment().runLiveUpdate(localIndexInfo);
					}
				});
		return builder.create();
	}

	private LiveUpdatesFragment getLiveUpdatesFragment() {
		return (LiveUpdatesFragment) getParentFragment();
	}

	private void initSwitch(ToggleButton toggleButton, String idPostfix, LocalIndexInfo item) {
		final OsmandApplication myApplication = ((OsmandActionBarActivity) this.getActivity()).getMyApplication();
		final OsmandSettings settings = myApplication.getSettings();
		final String settingId = item.getFileName() + idPostfix;
		final OsmandSettings.CommonPreference<Boolean> preference =
				settings.registerBooleanPreference(settingId, false);
		boolean initialValue = preference.get();
		toggleButton.setChecked(initialValue);
		toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				preference.set(isChecked);
			}
		});
	}

	public static SettingsDialogFragment createInstance(LocalIndexInfo localIndexInfo) {
		SettingsDialogFragment fragment = new SettingsDialogFragment();
		Bundle args = new Bundle();
		args.putParcelable(LOCAL_INDEX, localIndexInfo);
		fragment.setArguments(args);
		return fragment;
	}
}
