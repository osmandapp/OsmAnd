package net.osmand.plus.monitoring;


import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndTaskManager.OsmAndTaskRunnable;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.SettingsBaseActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.view.Window;

public class SettingsMonitoringActivity extends SettingsBaseActivity {
	private CheckBoxPreference routeServiceEnabled;
	private BroadcastReceiver broadcastReceiver;
	
	public static final int[] BG_SECONDS = new int[]{0, 30, 60, 90};
	public static final int[] BG_MINUTES = new int[]{2, 3, 5, 10, 15, 30, 60, 90};
	private final static boolean REGISTER_BG_SETTINGS = false;
	private static final int[] SECONDS = OsmandMonitoringPlugin.SECONDS;
	private static final int[] MINUTES = OsmandMonitoringPlugin.MINUTES;
	
	public SettingsMonitoringActivity() {
		super(true);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setSupportProgressBarIndeterminateVisibility(false);
		getSupportActionBar().setTitle(R.string.monitoring_settings);
		PreferenceScreen grp = getPreferenceScreen();
		
		createLoggingSection(grp);
		createLiveSection(grp);
		if(REGISTER_BG_SETTINGS) {
			registerBackgroundSettings();
		}
		profileDialog();
    }


	private void createLoggingSection(PreferenceScreen grp) {
		PreferenceCategory cat = new PreferenceCategory(this);
		cat.setTitle(R.string.save_track_to_gpx);
		grp.addPreference(cat);
		
		cat.addPreference(createCheckBoxPreference(settings.SAVE_TRACK_TO_GPX, R.string.save_track_to_gpx,
				R.string.save_track_to_gpx_descrp));
		cat.addPreference(createTimeListPreference(settings.SAVE_TRACK_INTERVAL, SECONDS,
				MINUTES, 1000, R.string.save_track_interval, R.string.save_track_interval_descr));
		Preference pref = new Preference(this);
		pref.setTitle(R.string.save_current_track);
		pref.setSummary(R.string.save_current_track_descr);
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
	}


	private void createLiveSection(PreferenceScreen grp) {
		PreferenceCategory cat;
		cat = new PreferenceCategory(this);
		cat.setTitle(R.string.live_monitoring);
		grp.addPreference(cat);
		
		cat.addPreference(createEditTextPreference(settings.LIVE_MONITORING_URL, R.string.live_monitoring_url,
				R.string.live_monitoring_url_descr));
		cat.addPreference(createCheckBoxPreference(settings.LIVE_MONITORING, R.string.live_monitoring,
				R.string.live_monitoring_descr));
		cat.addPreference(createTimeListPreference(settings.LIVE_MONITORING_INTERVAL, SECONDS,
				MINUTES, 1000, R.string.live_monitoring_interval, R.string.live_monitoring_interval_descr));
	}


	public void updateAllSettings() {
		super.updateAllSettings();
		
		if(routeServiceEnabled != null) {
			routeServiceEnabled.setChecked(getMyApplication().getNavigationService() != null);
		}
	}
	
	private void saveCurrentTracks(final SavingTrackHelper helper) {
		setSupportProgressBarIndeterminateVisibility(true);
		getMyApplication().getTaskManager().runInBackground(new OsmAndTaskRunnable<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				SavingTrackHelper helper = getMyApplication().getSavingTrackHelper();
				helper.saveDataToGpx();
				helper.close();
				return null;
			}
			@Override
			protected void onPostExecute(Void result) {
				setSupportProgressBarIndeterminateVisibility(false);
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
	
	private void registerBackgroundSettings() {
		PreferenceCategory cat = new PreferenceCategory(this);
		cat.setTitle(R.string.osmand_service);
		getPreferenceScreen().addPreference(cat);

		if(broadcastReceiver != null) {
			unregisterReceiver(broadcastReceiver);
			broadcastReceiver = null;
		}
		
		routeServiceEnabled = new CheckBoxPreference(this);
		broadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				routeServiceEnabled.setChecked(false);
			}
			
		};
		registerReceiver(broadcastReceiver, new IntentFilter(NavigationService.OSMAND_STOP_SERVICE_ACTION));
		routeServiceEnabled.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Intent serviceIntent = new Intent(SettingsMonitoringActivity.this, NavigationService.class);
				if ((Boolean) newValue) {
					ComponentName name = startService(serviceIntent);
					if (name == null) {
						routeServiceEnabled.setChecked(getMyApplication().getNavigationService() != null);
					}
				} else {
					if(!stopService(serviceIntent)){
						routeServiceEnabled.setChecked(getMyApplication().getNavigationService() != null);
					}
				}
				return true;
			}
		});
		routeServiceEnabled.setTitle(R.string.background_router_service);
		routeServiceEnabled.setSummary(R.string.background_router_service_descr);
		routeServiceEnabled.setKey(OsmandSettings.SERVICE_OFF_ENABLED);
		cat.addPreference(routeServiceEnabled);
		
		cat.addPreference(createTimeListPreference(settings.SERVICE_OFF_INTERVAL, BG_SECONDS, BG_MINUTES, 1000,
				R.string.background_service_int, R.string.background_service_int_descr));
	}
	
	
}
