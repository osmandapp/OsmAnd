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
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.Calendar;

public class LiveUpdatesSettingsDialogFragment extends DialogFragment {
	public static final String LOCAL_INDEX_INFO = "local_index_info";

	private static final String LOCAL_INDEX = "local_index";
	private static final int UPDATE_HOURLY = 0;
	private static final int UPDATE_DAILY = 1;
	private static final int UPDATE_WEEKLY = 2;
	public static final String UPDATE_TIMES_POSTFIX = "_update_times";
	private static final String TIME_OF_DAY_TO_UPDATE_POSTFIX = "_time_of_day_to_update";
	private static final int MORNING_UPDATE_TIME = 8;
	private static final int NIGHT_UPDATE_TIME = 21;
	private static final int SHIFT = 1000;
	public static final String DOWNLOAD_VIA_WIFI_POSTFIX = "_download_via_wifi";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final LocalIndexInfo localIndexInfo = getArguments().getParcelable(LOCAL_INDEX);

		View view = LayoutInflater.from(getActivity())
				.inflate(R.layout.dialog_live_updates_item_settings, null);
		final SwitchCompat liveUpdatesSwitch = (SwitchCompat) view.findViewById(R.id.liveUpdatesSwitch);
		final SwitchCompat downloadOverWiFiSwitch = (SwitchCompat) view.findViewById(R.id.downloadOverWiFiSwitch);
		final Spinner updateFrequencySpinner = (Spinner) view.findViewById(R.id.updateFrequencySpinner);
		final Spinner updateTimesOfDaySpinner = (Spinner) view.findViewById(R.id.updateTimesOfDaySpinner);

		final OsmandSettings.CommonPreference<Boolean> liveUpdatePreference =
				preferenceForLocalIndex(localIndexInfo);
		final OsmandSettings.CommonPreference<Boolean> downloadViaWiFiPreference =
				preferenceDownloadViaWiFi(localIndexInfo);
		final OsmandSettings.CommonPreference<Integer> updateFrequencePreference =
				preferenceUpdateTimes(localIndexInfo);
		final OsmandSettings.CommonPreference<Integer> timeOfDayPreference =
				preferenceTimeOfDayToUpdate(localIndexInfo);
		liveUpdatesSwitch.setChecked(liveUpdatePreference.get());
		downloadOverWiFiSwitch.setChecked(downloadViaWiFiPreference.get());

		builder.setView(view)
				.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final int updateFrequencyInt = updateFrequencySpinner.getSelectedItemPosition();
						updateFrequencePreference.set(updateFrequencyInt);
						UpdateFrequencies updateFrequency = UpdateFrequencies.values()[updateFrequencyInt];

						AlarmManager alarmMgr = (AlarmManager) getActivity()
								.getSystemService(Context.ALARM_SERVICE);
						Intent intent = new Intent(getActivity(), LiveUpdatesAlarmReceiver.class);
						final File file = new File(localIndexInfo.getFileName());
						final String fileName = Algorithms.getFileNameWithoutExtension(file);
						intent.putExtra(LOCAL_INDEX_INFO, localIndexInfo);
						intent.setAction(fileName);
						PendingIntent alarmIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);

						final int timeOfDayInt = updateTimesOfDaySpinner.getSelectedItemPosition();
						timeOfDayPreference.set(timeOfDayInt);
						TimesOfDay timeOfDayToUpdate = TimesOfDay.values()[timeOfDayInt];
						long timeOfFirstUpdate;
						long updateInterval;
						switch (updateFrequency) {
							case HOURLY:
								timeOfFirstUpdate = System.currentTimeMillis() + SHIFT;
								updateInterval = AlarmManager.INTERVAL_HOUR;
								break;
							case DAILY:
								timeOfFirstUpdate = getNextUpdateTime(timeOfDayToUpdate);
								updateInterval = AlarmManager.INTERVAL_DAY;
								break;
							case WEEKLY:
								timeOfFirstUpdate = getNextUpdateTime(timeOfDayToUpdate);
								updateInterval = AlarmManager.INTERVAL_DAY * 7;
								break;
							default:
								throw new IllegalStateException("Unexpected update frequency:"
										+ updateFrequency);
						}

						liveUpdatePreference.set(liveUpdatesSwitch.isChecked());
						downloadViaWiFiPreference.set(downloadOverWiFiSwitch.isChecked());
						alarmMgr.cancel(alarmIntent);
						if (liveUpdatesSwitch.isChecked()) {
							alarmMgr.setInexactRepeating(AlarmManager.RTC,
									timeOfFirstUpdate, updateInterval, alarmIntent);
						}
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

		updateFrequencySpinner.setSelection(updateFrequencePreference.get());
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

	private long getNextUpdateTime(TimesOfDay timeOfDayToUpdate) {
		Calendar calendar = Calendar.getInstance();
		if (timeOfDayToUpdate == TimesOfDay.MORNING) {
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, MORNING_UPDATE_TIME);
		} else if (timeOfDayToUpdate == TimesOfDay.NIGHT) {
			calendar.add(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, NIGHT_UPDATE_TIME);
		}
		return calendar.getTimeInMillis();
	}

	private LiveUpdatesFragment getLiveUpdatesFragment() {
		return (LiveUpdatesFragment) getParentFragment();
	}

	private OsmandSettings.CommonPreference<Boolean> preferenceForLocalIndex(LocalIndexInfo item) {
		final String settingId = item.getFileName() + LiveUpdatesFragment.LIVE_UPDATES_ON_POSTFIX;
		return getSettings().registerBooleanPreference(settingId, false);
	}

	private OsmandSettings.CommonPreference<Boolean> preferenceDownloadViaWiFi(LocalIndexInfo item) {
		final String settingId = item.getFileName() + DOWNLOAD_VIA_WIFI_POSTFIX;
		return getSettings().registerBooleanPreference(settingId, false);
	}

	private OsmandSettings.CommonPreference<Integer> preferenceUpdateTimes(LocalIndexInfo item) {
		final String settingId = item.getFileName() + UPDATE_TIMES_POSTFIX;
		return getSettings().registerIntPreference(settingId, UpdateFrequencies.HOURLY.ordinal());
	}

	private OsmandSettings.CommonPreference<Integer> preferenceTimeOfDayToUpdate(LocalIndexInfo item) {
		final String settingId = item.getFileName() + TIME_OF_DAY_TO_UPDATE_POSTFIX;
		return getSettings().registerIntPreference(settingId, TimesOfDay.NIGHT.ordinal());
	}

	private OsmandSettings getSettings() {
		return getMyApplication().getSettings();
	}

	private OsmandApplication getMyApplication() {
		return ((OsmandActionBarActivity) this.getActivity()).getMyApplication();
	}

	public static LiveUpdatesSettingsDialogFragment createInstance(LocalIndexInfo localIndexInfo) {
		LiveUpdatesSettingsDialogFragment fragment = new LiveUpdatesSettingsDialogFragment();
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
