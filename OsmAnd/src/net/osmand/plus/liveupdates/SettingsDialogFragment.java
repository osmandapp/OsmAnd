package net.osmand.plus.liveupdates;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;

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
		final LocalIndexInfo localIndexInfo = getArguments().getParcelable(LOCAL_INDEX);

		View view = LayoutInflater.from(getActivity())
				.inflate(R.layout.dialog_live_updates_item_settings, null);
		final SwitchCompat liveUpdatesSwitch = (SwitchCompat) view.findViewById(R.id.liveUpdatesSwitch);

		final OsmandSettings.CommonPreference<Boolean> liveUpdatePreference =
				preferenceForLocalIndex(LiveUpdatesFragment.LIVE_UPDATES_ON_POSTFIX, localIndexInfo);
		liveUpdatesSwitch.setChecked(liveUpdatePreference.get());

		builder.setView(view)
				.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						liveUpdatePreference.set(liveUpdatesSwitch.isChecked());
						getLiveUpdatesFragment().notifyLiveUpdatesChanged();
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setNeutralButton(R.string.update_now, new DialogInterface.OnClickListener() {
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

	private OsmandSettings.CommonPreference<Boolean> preferenceForLocalIndex(String idPostfix,
																			 LocalIndexInfo item) {
		final OsmandApplication myApplication = ((OsmandActionBarActivity) this.getActivity()).getMyApplication();
		final OsmandSettings settings = myApplication.getSettings();
		final String settingId = item.getFileName() + idPostfix;
		return settings.registerBooleanPreference(settingId, false);
	}

	public static SettingsDialogFragment createInstance(LocalIndexInfo localIndexInfo) {
		SettingsDialogFragment fragment = new SettingsDialogFragment();
		Bundle args = new Bundle();
		args.putParcelable(LOCAL_INDEX, localIndexInfo);
		fragment.setArguments(args);
		return fragment;
	}
}
