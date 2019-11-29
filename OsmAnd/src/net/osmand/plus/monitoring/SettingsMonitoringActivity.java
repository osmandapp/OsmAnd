package net.osmand.plus.monitoring;


import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndTaskManager.OsmAndTaskRunnable;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.SettingsBaseActivity;

import java.util.Map;

import static net.osmand.plus.OsmandSettings.DAILY_DIRECTORY;
import static net.osmand.plus.OsmandSettings.MONTHLY_DIRECTORY;
import static net.osmand.plus.OsmandSettings.REC_DIRECTORY;
import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;


public class SettingsMonitoringActivity extends SettingsBaseActivity {
	private CheckBoxPreference routeServiceEnabled;
	private BroadcastReceiver broadcastReceiver;

	public static final int[] BG_SECONDS = new int[]{0, 30, 60, 90};
	public static final int[] BG_MINUTES = new int[]{2, 3, 5, 10, 15, 30, 60, 90};
	private static final int[] SECONDS = OsmandMonitoringPlugin.SECONDS;
	private static final int[] MINUTES = OsmandMonitoringPlugin.MINUTES;
	private static final int[] MAX_INTERVAL_TO_SEND_MINUTES = OsmandMonitoringPlugin.MAX_INTERVAL_TO_SEND_MINUTES;

	public SettingsMonitoringActivity() {
		super(true);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		super.onCreate(savedInstanceState);
		setProgressVisibility(false);
		getToolbar().setTitle(R.string.monitoring_settings);
		PreferenceScreen grp = getPreferenceScreen();

		createLoggingSection(grp);
		createLiveSection(grp);
		createNotificationSection(grp);

		Intent intent = getIntent();
		if (intent != null && intent.hasExtra(PROFILE_STRING_KEY)) {
			String modeName = intent.getStringExtra(PROFILE_STRING_KEY);
			selectedAppMode = ApplicationMode.valueOfStringKey(modeName, ApplicationMode.CAR);
		} else {
			selectAppModeDialog().show();
		}
	}


	private void createLoggingSection(PreferenceScreen grp) {
		PreferenceCategory cat = new PreferenceCategory(this);
		cat.setTitle(R.string.save_track_to_gpx_globally);
		grp.addPreference(cat);

		Preference globalrecord = new Preference(this);
		globalrecord.setTitle(R.string.save_track_to_gpx_globally_headline);
		globalrecord.setSummary(R.string.save_track_to_gpx_globally_descr);
		globalrecord.setSelectable(false);
		//setEnabled(false) creates bad readability on some devices
		//globalrecord.setEnabled(false);
		cat.addPreference(globalrecord);

		if(settings.SAVE_GLOBAL_TRACK_REMEMBER.get()) {
			cat.addPreference(createTimeListPreference(settings.SAVE_GLOBAL_TRACK_INTERVAL, SECONDS,
					MINUTES, 1000, settings.SAVE_GLOBAL_TRACK_REMEMBER,  R.string.save_global_track_interval, R.string.save_global_track_interval_descr));
		}

		Preference pref = new Preference(this);
		pref.setTitle(R.string.save_current_track);
		pref.setSummary(getMyApplication().getString(R.string.save_current_track_descr)
				+ " (" + OsmAndFormatter.getFormattedDistance(getMyApplication().getSavingTrackHelper().getDistance(), getMyApplication()) + ")");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				SavingTrackHelper helper = getMyApplication().getSavingTrackHelper();
				if (helper.hasDataToSave()) {
					saveCurrentTracks(helper);
				} else {
					helper.close();
				}
				return true;
			}
		});
		cat.addPreference(pref);

		cat.addPreference(createCheckBoxPreference(settings.SAVE_TRACK_TO_GPX, R.string.save_track_to_gpx,
				R.string.save_track_to_gpx_descrp));
		cat.addPreference(createTimeListPreference(settings.SAVE_TRACK_INTERVAL, SECONDS,
				MINUTES, 1000, R.string.save_track_interval, R.string.save_track_interval_descr));
		String[] names;
		Float[] floatValues;
		floatValues = new Float[] {0.f, 2.0f, 5.0f, 10.0f, 20.0f, 30.0f, 50.0f};
		names = new String[floatValues.length];
		names[0] = getString(R.string.shared_string_not_selected);
		for(int i = 1; i < floatValues.length; i++) {
			names[i] = floatValues[i].intValue() + " " + getString(R.string.m);
		}
		cat.addPreference(createListPreference(settings.SAVE_TRACK_MIN_DISTANCE, names, floatValues,
				R.string.save_track_min_distance, R.string.save_track_min_distance_descr));
		floatValues = new Float[] {0.f, 1.0f, 2.0f, 5.0f, 10.0f, 15.0f, 20.0f, 50.0f, 100.0f};
		names = new String[floatValues.length];
		names[0] = getString(R.string.shared_string_not_selected);
		for(int i = 1; i < floatValues.length; i++) {
			names[i] = floatValues[i].intValue() + " " + getString(R.string.m) + "  (" + Math.round(floatValues[i]/0.3048f) + " " + getString(R.string.foot) + ")";
		}
		cat.addPreference(createListPreference(settings.SAVE_TRACK_PRECISION, names, floatValues,
				R.string.save_track_precision, R.string.save_track_precision_descr));
		floatValues = new Float[] {0.f, 0.000001f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f};
		names = new String[floatValues.length];
		names[0] = getString(R.string.shared_string_not_selected);
		names[1] = "> 0"; // This option for the GPS chipset motion detection
		for(int i = 2; i < floatValues.length; i++) {
			names[i] = floatValues[i].intValue() + " " + getString(R.string.km_h);
			floatValues[i] = floatValues[i] / 3.6f;
		}
		cat.addPreference(createListPreference(settings.SAVE_TRACK_MIN_SPEED, names, floatValues,
				R.string.save_track_min_speed, R.string.save_track_min_speed_descr));
		cat.addPreference(createCheckBoxPreference(settings.AUTO_SPLIT_RECORDING, R.string.auto_split_recording_title,
				R.string.auto_split_recording_descr));
		cat.addPreference(createCheckBoxPreference(settings.DISABLE_RECORDING_ONCE_APP_KILLED, R.string.disable_recording_once_app_killed,
				R.string.disable_recording_once_app_killed_descrp));
		cat.addPreference(createCheckBoxPreference(settings.SAVE_HEADING_TO_GPX, R.string.save_heading,
				R.string.save_heading_descr));

		Integer[] intValues = new Integer[]{REC_DIRECTORY, MONTHLY_DIRECTORY, DAILY_DIRECTORY};
		names = new String[intValues.length];
		names[0] = getString(R.string.store_tracks_in_rec_directory);
		names[1] = getString(R.string.store_tracks_in_monthly_directories);
		names[2] = getString(R.string.store_tracks_in_daily_directories);
		cat.addPreference(createListPreference(settings.TRACK_STORAGE_DIRECTORY, names, intValues,
				R.string.track_storage_directory, R.string.track_storage_directory_descrp));
	}


	private void createLiveSection(PreferenceScreen grp) {
		PreferenceCategory cat;
		cat = new PreferenceCategory(this);
		cat.setTitle(R.string.live_monitoring_m);
		grp.addPreference(cat);

		cat.addPreference(createEditTextPreference(settings.LIVE_MONITORING_URL, R.string.live_monitoring_url,
				R.string.live_monitoring_url_descr));
		final CheckBoxPreference liveMonitoring = createCheckBoxPreference(settings.LIVE_MONITORING, R.string.live_monitoring_m,
				R.string.live_monitoring_m_descr);
		cat.addPreference(liveMonitoring);
		cat.addPreference(createTimeListPreference(settings.LIVE_MONITORING_INTERVAL, SECONDS,
				MINUTES, 1000, R.string.live_monitoring_interval, R.string.live_monitoring_interval_descr));
		cat.addPreference(createTimeListPreference(settings.LIVE_MONITORING_MAX_INTERVAL_TO_SEND, null,
				MAX_INTERVAL_TO_SEND_MINUTES, 1000, R.string.live_monitoring_max_interval_to_send, R.string.live_monitoring_max_interval_to_send_desrc));
	}

	private void createNotificationSection(PreferenceScreen grp) {
		PreferenceCategory cat;
		cat = new PreferenceCategory(this);
		cat.setTitle(R.string.shared_string_notifications);
		grp.addPreference(cat);

		final CheckBoxPreference tripRecording = createCheckBoxPreference(settings.SHOW_TRIP_REC_NOTIFICATION, R.string.trip_rec_notification_settings,
				R.string.trip_rec_notification_settings_desc);
		cat.addPreference(tripRecording);
	}

	public void updateAllSettings() {
		super.updateAllSettings();

		if(routeServiceEnabled != null) {
			routeServiceEnabled.setChecked(getMyApplication().getNavigationService() != null);
		}
	}

	private void saveCurrentTracks(final SavingTrackHelper helper) {
		setProgressVisibility(true);
		getMyApplication().getTaskManager().runInBackground(new OsmAndTaskRunnable<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				SavingTrackHelper helper = getMyApplication().getSavingTrackHelper();
				helper.saveDataToGpx(getMyApplication().getAppCustomization().getTracksDir());
				helper.close();
				return null;
			}
			@Override
			protected void onPostExecute(Void result) {
				setProgressVisibility(false);
			}

		}, (Void) null);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(broadcastReceiver != null) {
			unregisterReceiver(broadcastReceiver);
			broadcastReceiver = null;
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String prefId = preference.getKey();
		if (preference instanceof ListPreference) {
			int ind = ((ListPreference) preference).findIndexOfValue((String) newValue);
			CharSequence entry = ((ListPreference) preference).getEntries()[ind];
			Map<String, ?> map = getListPrefValues().get(prefId);
			if (map != null) {
				newValue = map.get(entry);
			}
		}
		showConfirmDialog(prefId, newValue);
		return false;
	}

	protected void showConfirmDialog(final String prefId, final Object newValue) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		String appModeName = selectedAppMode.toHumanString(this);
		String currentModeText = getString(R.string.apply_to_current_profile, appModeName);
		int start = currentModeText.indexOf(appModeName);

		SpannableString[] strings = new SpannableString[2];
		strings[0] = new SpannableString(getString(R.string.apply_to_all_profiles));
		strings[1] = new SpannableString(currentModeText);
		strings[1].setSpan(new StyleSpan(Typeface.BOLD), start, start + appModeName.length(), 0);

		final int[] icons = new int[2];
		icons[0] = R.drawable.ic_action_copy;
		icons[1] = selectedAppMode.getIconRes();
		
		final boolean nightMode = !settings.isLightContent();
		final OsmandApplication app = getMyApplication();
		final LayoutInflater themedInflater = UiUtilities.getInflater(this, nightMode);

		//set up dialog title
		View dialogTitle = themedInflater.inflate(R.layout.bottom_sheet_item_simple, null);
		dialogTitle.findViewById(R.id.icon).setVisibility(View.GONE);
		TextView tvTitle = dialogTitle.findViewById(R.id.title);
		tvTitle.setText(R.string.change_default_settings);
		int textSize = (int) app.getResources().getDimension(R.dimen.dialog_header_text_size);
		tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		builder.setCustomTitle(dialogTitle);
		
		final ArrayAdapter<SpannableString> singleChoiceAdapter = new ArrayAdapter<SpannableString>(this, R.layout.bottom_sheet_item_simple, R.id.title, strings) {
			@NonNull
			@Override
			public View getView(int position, View convertView, @NonNull ViewGroup parent) {
				View v = convertView;
				if (v == null) {
					v = themedInflater.inflate(R.layout.bottom_sheet_item_simple, parent, false);
				}
				int activeColor = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
				Drawable icon = app.getUIUtilities().getIcon(icons[position], activeColor);
				((TextView) v.findViewById(R.id.title)).setText(getItem(position));
				((ImageView) v.findViewById(R.id.icon)).setImageDrawable(icon);
				return v;
			}
		};

		builder.setAdapter(singleChoiceAdapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					settings.setSharedGeneralPreference(prefId, newValue);
				} else {
					settings.setPreference(prefId, newValue);
				}
				updateAllSettings();
			}
		});

		builder.setNegativeButton(R.string.discard_changes, null);
		AlertDialog dialog = builder.create();
		dialog.getListView().setDividerHeight(0);
		dialog.show();
	}
}