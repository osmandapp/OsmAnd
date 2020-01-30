package net.osmand.plus.liveupdates;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.TimeOfDay;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.DEFAULT_LAST_CHECK;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.formatDateTime;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getNameToDisplay;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getPendingIntent;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceDownloadViaWiFi;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceForLocalIndex;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastCheck;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLiveUpdatesOn;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceTimeOfDayToUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.runLiveUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.setAlarmForPendingIntent;

public class LiveUpdatesSettingsDialogFragment extends DialogFragment {
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesSettingsDialogFragment.class);
	private static final String LOCAL_INDEX_FILE_NAME = "local_index_file_name";

	private TextView sizeTextView;
	
	private String fileName;
	private String fileNameWithoutExtension;
	
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		fileName = getArguments().getString(LOCAL_INDEX_FILE_NAME);
		assert fileName != null;

		View view = LayoutInflater.from(getActivity())
				.inflate(R.layout.dialog_live_updates_item_settings, null);
		final TextView regionNameTextView = (TextView) view.findViewById(R.id.regionNameTextView);
		final TextView lastMapChangeTextView = (TextView) view.findViewById(R.id.lastMapChangeTextView);
		final TextView lastUpdateTextView = (TextView) view.findViewById(R.id.lastUpdateTextView);
		final SwitchCompat liveUpdatesSwitch = (SwitchCompat) view.findViewById(R.id.toggle_item);
		final CheckBox downloadOverWiFiCheckBox = (CheckBox) view.findViewById(R.id.downloadOverWiFiSwitch);
		final Spinner updateFrequencySpinner = (Spinner) view.findViewById(R.id.updateFrequencySpinner);
		final Spinner updateTimesOfDaySpinner = (Spinner) view.findViewById(R.id.updateTimesOfDaySpinner);
		final View updateTimesOfDayLayout = view.findViewById(R.id.updateTimesOfDayLayout);
		sizeTextView = (TextView) view.findViewById(R.id.sizeTextView);

		regionNameTextView.setText(getNameToDisplay(fileName, getMyActivity()));
		fileNameWithoutExtension = Algorithms.getFileNameWithoutExtension(new File(fileName));
		final IncrementalChangesManager changesManager = getMyApplication().getResourceManager().getChangesManager();
		final long timestamp = changesManager.getTimestamp(fileNameWithoutExtension);
		String lastUpdateDate = formatDateTime(getActivity(), timestamp);
		lastMapChangeTextView.setText(getString(R.string.last_map_change, lastUpdateDate));
		final long lastCheck = preferenceLastCheck(fileName, getSettings()).get();


		OsmandSettings.CommonPreference<Boolean> preference = preferenceLiveUpdatesOn(fileName,
				getSettings());
		if (preference.get() && lastCheck != DEFAULT_LAST_CHECK) {
			String lastCheckString = formatDateTime(getActivity(), lastCheck);
			lastUpdateTextView.setText(getString(R.string.last_update, lastCheckString));
		} else {
			lastUpdateTextView.setVisibility(View.GONE);
		}

		final OsmandSettings.CommonPreference<Boolean> liveUpdatePreference =
				preferenceForLocalIndex(fileName, getSettings());
		final OsmandSettings.CommonPreference<Boolean> downloadViaWiFiPreference =
				preferenceDownloadViaWiFi(fileName, getSettings());
		final OsmandSettings.CommonPreference<Integer> updateFrequencyPreference =
				preferenceUpdateFrequency(fileName, getSettings());
		final OsmandSettings.CommonPreference<Integer> timeOfDayPreference =
				preferenceTimeOfDayToUpdate(fileName, getSettings());

		downloadOverWiFiCheckBox.setChecked(!liveUpdatePreference.get() || downloadViaWiFiPreference.get());

		sizeTextView.setText(getUpdatesSize(getMyActivity(), fileNameWithoutExtension, changesManager));

		TimeOfDay[] timeOfDays = TimeOfDay.values();
		String[] timeOfDaysStrings = new String[timeOfDays.length];
		for (int i = 0; i < timeOfDays.length; i++) {
			timeOfDaysStrings[i] = getString(timeOfDays[i].getLocalizedId());
		}
		updateTimesOfDaySpinner.setAdapter(new ArrayAdapter<>(getActivity(),
				R.layout.action_spinner_item, timeOfDaysStrings));
		updateTimesOfDaySpinner.setSelection(timeOfDayPreference.get());

		UpdateFrequency[] updateFrequencies = UpdateFrequency.values();
		String[] updateFrequenciesStrings = new String[updateFrequencies.length];
		for (int i = 0; i < updateFrequencies.length; i++) {
			updateFrequenciesStrings[i] = getString(updateFrequencies[i].getLocalizedId());
		}

		refreshTimeOfDayLayout(UpdateFrequency.values()[updateFrequencyPreference.get()],
				updateTimesOfDayLayout);
		updateFrequencySpinner.setAdapter(new ArrayAdapter<>(getActivity(),
				R.layout.action_spinner_item, updateFrequenciesStrings));
		updateFrequencySpinner.setSelection(updateFrequencyPreference.get());
		updateFrequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				refreshTimeOfDayLayout(UpdateFrequency.values()[position], updateTimesOfDayLayout);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		builder.setView(view)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (liveUpdatePreference.get() != liveUpdatesSwitch.isChecked()) {
							liveUpdatePreference.set(liveUpdatesSwitch.isChecked());
							if (!liveUpdatesSwitch.isChecked()) {
								long updatesSize = changesManager.getUpdatesSize(fileNameWithoutExtension);
								if (updatesSize != 0) {
									ClearUpdatesDialogFragment.createInstance(fileName)
											.show(getParentFragment().getChildFragmentManager(), null);
								}
							}
						}
						downloadViaWiFiPreference.set(downloadOverWiFiCheckBox.isChecked());

						final int updateFrequencyInt = updateFrequencySpinner.getSelectedItemPosition();
						updateFrequencyPreference.set(updateFrequencyInt);

						AlarmManager alarmMgr = (AlarmManager) getActivity()
								.getSystemService(Context.ALARM_SERVICE);
						PendingIntent alarmIntent = getPendingIntent(getActivity(), fileName);

						final int timeOfDayInt = updateTimesOfDaySpinner.getSelectedItemPosition();
						timeOfDayPreference.set(timeOfDayInt);

						if (liveUpdatesSwitch.isChecked() && getSettings().IS_LIVE_UPDATES_ON.get()) {
							runLiveUpdate(getActivity(), fileName, false);
							UpdateFrequency updateFrequency = UpdateFrequency.values()[updateFrequencyInt];
							TimeOfDay timeOfDayToUpdate = TimeOfDay.values()[timeOfDayInt];
							setAlarmForPendingIntent(alarmIntent, alarmMgr, updateFrequency, timeOfDayToUpdate);
						} else {
							alarmMgr.cancel(alarmIntent);
						}
						getLiveUpdatesFragment().notifyLiveUpdatesChanged();
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setNeutralButton(R.string.update_now, null);
		return builder.create();
	}

	@Override
	public void onResume() {
		super.onResume();
		final AlertDialog dialog = (AlertDialog) getDialog();
		if (dialog != null) {
			Button neutralButton = (Button) dialog.getButton(Dialog.BUTTON_NEUTRAL);
			neutralButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!getSettings().isInternetConnectionAvailable()) {
						getMyApplication().showShortToastMessage(R.string.no_internet_connection);
					} else {
						runLiveUpdate(getActivity(), fileName, true);
						final IncrementalChangesManager changesManager = getMyApplication().getResourceManager().getChangesManager();
						sizeTextView.setText(getUpdatesSize(getMyActivity(), fileNameWithoutExtension, changesManager));
						dialog.dismiss();
					}
				}
			});
		}
	}

	private void refreshTimeOfDayLayout(UpdateFrequency updateFrequency, View updateTimesOfDayLayout) {
		switch (updateFrequency) {
			case HOURLY:
				updateTimesOfDayLayout.setVisibility(View.GONE);
				break;
			case DAILY:
			case WEEKLY:
				updateTimesOfDayLayout.setVisibility(View.VISIBLE);
				break;
		}
	}

	private static String getUpdatesSize(Context ctx, String fileNameWithoutExtension,
										 IncrementalChangesManager changesManager) {
		long updatesSize = changesManager.getUpdatesSize(fileNameWithoutExtension);
		return AndroidUtils.formatSize(ctx, updatesSize);
	}

	private LiveUpdatesFragment getLiveUpdatesFragment() {
		return (LiveUpdatesFragment) getParentFragment();
	}

	private OsmandSettings getSettings() {
		return getMyApplication().getSettings();
	}

	private OsmandApplication getMyApplication() {
		return getMyActivity().getMyApplication();
	}

	private AbstractDownloadActivity getMyActivity() {
		return (AbstractDownloadActivity) this.getActivity();
	}

	public static LiveUpdatesSettingsDialogFragment createInstance(String fileName) {
		LiveUpdatesSettingsDialogFragment fragment = new LiveUpdatesSettingsDialogFragment();
		Bundle args = new Bundle();
		args.putString(LOCAL_INDEX_FILE_NAME, fileName);
		fragment.setArguments(args);
		return fragment;
	}

	public static class ClearUpdatesDialogFragment extends DialogFragment {
		@NonNull
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			final String localIndexInfo = getArguments().getString(LOCAL_INDEX_FILE_NAME);
			assert localIndexInfo != null;

			final IncrementalChangesManager changesManager =
					getMyApplication().getResourceManager().getChangesManager();
			final String fileNameWithoutExtension =
					Algorithms.getFileNameWithoutExtension(new File(localIndexInfo));
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setMessage(
					getString(R.string.ltr_or_rtl_combine_via_space,
							getString(R.string.clear_updates_proposition_message),
							getUpdatesSize(getContext(), fileNameWithoutExtension, changesManager)))
					.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							changesManager.deleteUpdates(fileNameWithoutExtension);
							preferenceLastCheck(localIndexInfo, getMyApplication().getSettings()).resetToDefault();
						}
					})
					.setNegativeButton(R.string.shared_string_cancel, null);
			return builder.create();
		}

		private OsmandApplication getMyApplication() {
			return (OsmandApplication) getActivity().getApplication();
		}

		public static ClearUpdatesDialogFragment createInstance(String fileName) {
			ClearUpdatesDialogFragment fragment = new ClearUpdatesDialogFragment();
			Bundle args = new Bundle();
			args.putString(LOCAL_INDEX_FILE_NAME, fileName);
			fragment.setArguments(args);
			return fragment;
		}
	}
}
