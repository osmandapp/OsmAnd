package net.osmand.plus.background;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class OsmandBackgroundServicePlugin extends OsmandPlugin {
	private static final String ID = "osmand.backgroundservice";
	private OsmandSettings settings;
	private OsmandApplication app;
	private BroadcastReceiver broadcastReceiver;
	private CheckBoxPreference routeServiceEnabled;
	private SettingsActivity activity;
	
	public OsmandBackgroundServicePlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
		return true;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_background_plugin_description);
	}
	@Override
	public String getName() {
		return app.getString(R.string.osmand_service);
	}
	@Override
	public void registerLayers(MapActivity activity) {
	}
	
	@Override
	public void settingsActivityDestroy(final SettingsActivity activity){
		unregisterReceiver(activity);
	}

	private void unregisterReceiver(final SettingsActivity activity) {
		if (activity != null && this.activity == activity && broadcastReceiver != null) {
			activity.unregisterReceiver(broadcastReceiver);
			broadcastReceiver = null;
		}
	}
	
	@Override
	public void settingsActivityUpdate(final SettingsActivity activity){
		if(routeServiceEnabled != null) {
			routeServiceEnabled.setChecked(app.getNavigationService() != null);
		}
	}
	

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		PreferenceScreen grp = screen.getPreferenceManager().createPreferenceScreen(activity);
		grp.setTitle(R.string.osmand_service);
		grp.setSummary(R.string.osmand_service_descr);
		((PreferenceCategory) screen.findPreference("global_settings")).addPreference(grp);

		//unregister old service. Note, the order of calls of Create/Destroy is not guaranteed!!
		unregisterReceiver(this.activity);
		
		routeServiceEnabled = new CheckBoxPreference(activity);
		this.activity = activity;
		broadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				routeServiceEnabled.setChecked(false);
			}
			
		};
		activity.registerReceiver(broadcastReceiver, new IntentFilter(NavigationService.OSMAND_STOP_SERVICE_ACTION));
		routeServiceEnabled.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				Intent serviceIntent = new Intent(activity, NavigationService.class);
				if ((Boolean) newValue) {
					ComponentName name = activity.startService(serviceIntent);
					if (name == null) {
						routeServiceEnabled.setChecked(app.getNavigationService() != null);
					}
				} else {
					if(!activity.stopService(serviceIntent)){
						routeServiceEnabled.setChecked(app.getNavigationService() != null);
					}
				}
				return true;
			}
		});
		routeServiceEnabled.setTitle(R.string.background_router_service);
		routeServiceEnabled.setSummary(R.string.background_router_service_descr);
		routeServiceEnabled.setKey("service_off_enabled");
		grp.addPreference(routeServiceEnabled);
		
		String[] entries = new String[]{app.getString(R.string.gps_provider), app.getString(R.string.network_provider)};
		String[] entrieValues = new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};
		grp.addPreference(activity.createListPreference(settings.SERVICE_OFF_PROVIDER, entries, entrieValues, 
				R.string.background_service_provider, R.string.background_service_provider_descr));
		
		grp.addPreference(activity.createTimeListPreference(settings.SERVICE_OFF_INTERVAL,new int[]{0, 30, 45, 60}, new int[]{2, 3, 5, 10, 15, 30, 45, 60, 90}, 1000,
				R.string.background_service_int, R.string.background_service_int_descr));
		grp.addPreference(activity.createTimeListPreference(settings.SERVICE_OFF_WAIT_INTERVAL,new int[]{15, 30, 45, 60, 90}, new int[]{2, 3, 5, 10}, 1000,
				R.string.background_service_wait_int, R.string.background_service_wait_int_descr));
	}
	
}
