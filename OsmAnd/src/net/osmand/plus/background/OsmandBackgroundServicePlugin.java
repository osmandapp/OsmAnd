package net.osmand.plus.background;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MonitoringInfoControl.MonitoringInfoControlServices;
import net.osmand.plus.views.MonitoringInfoControl.ValueHolder;
import net.osmand.plus.views.MonitoringInfoControl;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnClickListener;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.View;

public class OsmandBackgroundServicePlugin extends OsmandPlugin implements MonitoringInfoControlServices {
	public static final int[] SECONDS = new int[]{0, 30, 60, 90};
	public static final int[] MINUTES = new int[]{2, 3, 5, 10, 15, 30, 60, 90};
	private final static boolean REGISTER_BG_SETTINGS = false;
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
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		MapInfoLayer li = activity.getMapLayers().getMapInfoLayer();
		MonitoringInfoControl lock = li.getMonitoringInfoControl();
		if(lock != null && !lock.getMonitorActions().contains(this)) {
			lock.getMonitorActions().add(this);
		}
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
		if(REGISTER_BG_SETTINGS) {
			registerBackgroundSettings(activity, screen);
		}
	}

	private void registerBackgroundSettings(final SettingsActivity activity, PreferenceScreen screen) {
		PreferenceCategory cat = new PreferenceCategory(activity);
		cat.setTitle(R.string.osmand_service);
		((PreferenceScreen) screen.findPreference(SettingsActivity.SCREEN_ID_GENERAL_SETTINGS)).addPreference(cat);

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
		routeServiceEnabled.setKey(OsmandSettings.SERVICE_OFF_ENABLED);
		cat.addPreference(routeServiceEnabled);
		
		cat.addPreference(activity.createTimeListPreference(settings.SERVICE_OFF_INTERVAL, SECONDS, MINUTES, 1000,
				R.string.background_service_int, R.string.background_service_int_descr));
	}

	@Override
	public void addMonitorActions(final QuickAction qa, final MonitoringInfoControl li, final OsmandMapTileView view) {
		
		final ActionItem bgServiceAction = new ActionItem();
		final boolean off = view.getApplication().getNavigationService() == null;
		bgServiceAction.setTitle(view.getResources().getString(!off? R.string.bg_service_sleep_mode_on : R.string.bg_service_sleep_mode_off));
		bgServiceAction.setIcon(view.getResources().getDrawable(!off? R.drawable.monitoring_rec_big : R.drawable.monitoring_rec_inactive));
		bgServiceAction.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent serviceIntent = new Intent(view.getContext(), NavigationService.class);
				if (view.getApplication().getNavigationService() == null) {
					final ValueHolder<Integer> vs = new ValueHolder<Integer>();
					vs.value = view.getSettings().SERVICE_OFF_INTERVAL.get();
					li.showIntervalChooseDialog(view, view.getContext().getString(R.string.gps_wakeup_interval), 
							view.getContext().getString(R.string.background_router_service), OsmandBackgroundServicePlugin.SECONDS, OsmandBackgroundServicePlugin.MINUTES,
							vs, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							view.getSettings().SERVICE_OFF_INTERVAL.set(vs.value);
							view.getContext().startService(serviceIntent);	
						}
					});
				} else {
					view.getContext().stopService(serviceIntent);
				}
				qa.dismiss();
			}
		});
		qa.addActionItem(bgServiceAction);
		
	}
}
