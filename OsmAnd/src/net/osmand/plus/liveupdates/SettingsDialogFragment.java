package net.osmand.plus.liveupdates;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class SettingsDialogFragment extends DialogFragment {
	public static final String LOCAL_INDEX = "local_index";
	public static final int UPDATE_HOURLY = 0;
	public static final int UPDATE_DAILY = 1;
	public static final int UPDATE_WEEKLY = 2;
	public static final String UPDATE_TIMES = "_update_times";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final LocalIndexInfo localIndexInfo = getArguments().getParcelable(LOCAL_INDEX);

		View view = LayoutInflater.from(getActivity())
				.inflate(R.layout.dialog_live_updates_item_settings, null);
		final SwitchCompat liveUpdatesSwitch = (SwitchCompat) view.findViewById(R.id.liveUpdatesSwitch);
		final Spinner updateFrequencySpinner = (Spinner) view.findViewById(R.id.updateFrequencySpinner);
		final Spinner updateTimesOfDaySpinner = (Spinner) view.findViewById(R.id.updateTimesOfDaySpinner);

		final OsmandSettings.CommonPreference<Boolean> liveUpdatePreference =
				preferenceForLocalIndex(localIndexInfo);
		final OsmandSettings.CommonPreference<Integer> updateFrequencies =
				preferenceUpdateTimes(localIndexInfo);
		liveUpdatesSwitch.setChecked(liveUpdatePreference.get());

		builder.setView(view)
				.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final int updateFrequencyInt = updateFrequencySpinner.getSelectedItemPosition();
						updateFrequencies.set(updateFrequencyInt);
						AlarmManager alarmMgr = (AlarmManager) getActivity()
								.getSystemService(Context.ALARM_SERVICE);

						Intent intent = new Intent(getActivity(), LiveUpdatesAlarmReceiver.class);
						PendingIntent alarmIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);

						UpdateFrequencies updateFrequency = UpdateFrequencies.values()[updateFrequencyInt];
						switch (updateFrequency) {
							case HOURLY:
								alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
										1000, 60 * 60 * 1000, alarmIntent);
								break;
							case DAILY:
							case WEEKLY:
								updateTimesOfDaySpinner.setVisibility(View.VISIBLE);
								break;
						}
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

		updateFrequencySpinner.setSelection(updateFrequencies.get());
		updateFrequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				UpdateFrequencies updateFrequency = UpdateFrequencies.values()[position];
				switch (updateFrequency) {
					case HOURLY:
						updateTimesOfDaySpinner.setVisibility(View.GONE);
						break;
					case DAILY:
					case WEEKLY:
						updateTimesOfDaySpinner.setVisibility(View.VISIBLE);
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		return builder.create();
	}

	private LiveUpdatesFragment getLiveUpdatesFragment() {
		return (LiveUpdatesFragment) getParentFragment();
	}

	private OsmandSettings.CommonPreference<Boolean> preferenceForLocalIndex(LocalIndexInfo item) {
		final String settingId = item.getFileName() + LiveUpdatesFragment.LIVE_UPDATES_ON_POSTFIX;
		return getSettings().registerBooleanPreference(settingId, false);
	}

	private OsmandSettings.CommonPreference<Integer> preferenceUpdateTimes(LocalIndexInfo item) {
		final String settingId = item.getFileName() + UPDATE_TIMES;
		return getSettings().registerIntPreference(settingId, UpdateFrequencies.HOURLY.ordinal());
	}

	private OsmandSettings getSettings() {
		return getMyApplication().getSettings();
	}

	private OsmandApplication getMyApplication() {
		return ((OsmandActionBarActivity) this.getActivity()).getMyApplication();
	}

	public static SettingsDialogFragment createInstance(LocalIndexInfo localIndexInfo) {
		SettingsDialogFragment fragment = new SettingsDialogFragment();
		Bundle args = new Bundle();
		args.putParcelable(LOCAL_INDEX, localIndexInfo);
		fragment.setArguments(args);
		return fragment;
	}

	public static enum UpdateFrequencies {
		HOURLY,
		DAILY,
		WEEKLY
	}

	public static enum TimesOfDay {
		MORNING,
		NIGHT
	}
}
