package net.osmand.plus.background;

import java.util.EnumSet;

import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.ImageViewControl;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

public class OsmandBackgroundServicePlugin extends OsmandPlugin {
	private static final int[] MINUTES = new int[]{2, 3, 5, 10, 15, 30, 45, 60, 90};
	private static final int[] SECONDS = new int[]{0, 30, 45, 60};
	private static final String ID = "osmand.backgroundservice";
	private OsmandSettings settings;
	private OsmandApplication app;
	private BroadcastReceiver broadcastReceiver;
	private CheckBoxPreference routeServiceEnabled;
	private SettingsActivity activity;
	
	private static boolean isScreenLocked = false;
	private OsmandBackgroundServiceLayer backgroundServiceLayer;
	
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
		backgroundServiceLayer = new OsmandBackgroundServiceLayer(activity);
		MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
		ImageViewControl lockView = createBgServiceView( activity.getMapView(), activity);
		// TODO icon
		layer.getMapInfoControls().registerTopWidget(lockView, R.drawable.monitoring_rec_big, R.string.map_widget_lock_screen, "lock_view", true, EnumSet.allOf(ApplicationMode.class), 10);
	}
	
	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if(backgroundServiceLayer == null) {
			registerLayers(activity);
		}
		if (isScreenLocked) {
			mapView.addLayer(backgroundServiceLayer, mapView.getLayers().size());
		} else {
			mapView.removeLayer(backgroundServiceLayer); 
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
		routeServiceEnabled.setKey(OsmandSettings.SERVICE_OFF_ENABLED);
		grp.addPreference(routeServiceEnabled);
		
		String[] entries = new String[]{app.getString(R.string.gps_provider), app.getString(R.string.network_provider)};
		String[] entrieValues = new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};
		grp.addPreference(activity.createListPreference(settings.SERVICE_OFF_PROVIDER, entries, entrieValues, 
				R.string.background_service_provider, R.string.background_service_provider_descr));
		
		grp.addPreference(activity.createTimeListPreference(settings.SERVICE_OFF_INTERVAL, SECONDS, MINUTES, 1000,
				R.string.background_service_int, R.string.background_service_int_descr));
		grp.addPreference(activity.createTimeListPreference(settings.SERVICE_OFF_WAIT_INTERVAL,new int[]{15, 30, 45, 60, 90}, new int[]{2, 3, 5, 10}, 1000,
				R.string.background_service_wait_int, R.string.background_service_wait_int_descr));
	}

	/**
	 * 
	 */
	public ImageViewControl createBgServiceView(final OsmandMapTileView view, final MapActivity map) {
		// TODO Lock icons
		final Drawable lock = view.getResources().getDrawable(R.drawable.monitoring_rec_big);
		final Drawable unLock = view.getResources().getDrawable(R.drawable.monitoring_rec_inactive);
		final ImageViewControl lockView = new ImageViewControl(view.getContext()) {
			@Override
			public boolean updateInfo() {
				return false;
			}
		};
		
		if (isScreenLocked) {
			lockView.setBackgroundDrawable(lock);
		} else {
			lockView.setBackgroundDrawable(unLock);
		}
		lockView.setOnClickListener(new View.OnClickListener() {				
			@Override
			public void onClick(View v) {
				showBgServiceDialog(view, map);
				if (isScreenLocked) {
					lockView.setBackgroundDrawable(lock);
				} else {
					lockView.setBackgroundDrawable(unLock);
				}
//				TODO refresh View
				lockView.invalidate();
			}
			
		});
		return lockView;
	}

	/**
	 * 
	 * @param mapActivity
	 */
	public void showBgServiceDialog(final OsmandMapTileView view, final MapActivity mapActivity) {
		final View bgServiceView = mapActivity.getLayoutInflater().inflate(R.layout.background_service, null);
		Builder dialog = new AlertDialog.Builder(mapActivity);
		dialog.setView(bgServiceView);
		dialog.setTitle("Background Service");			
		
		CheckBox lockCheck = (CheckBox) bgServiceView.findViewById(R.id.screen_lock_check);
		lockCheck.setChecked(isScreenLocked);
		lockCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {		
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				isScreenLocked = isChecked;
				updateLayers(view, mapActivity);
			}
		});
		
		CheckBox  bgCheck = (CheckBox) bgServiceView.findViewById(R.id.sleep_mode_check);
		bgCheck.setChecked(app.getNavigationService() != null);
		bgCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {		
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Context applicationContext = app.getApplicationContext();
				Intent serviceIntent = new Intent(applicationContext, NavigationService.class);
				if (isChecked) {
					applicationContext.startService(serviceIntent);
				} else {
					applicationContext.stopService(serviceIntent);
				}
			}
		});
		
		Spinner intSpinner = (Spinner) bgServiceView.findViewById(R.id.wake_up_int_spinner);
		final int secondsLength = SECONDS.length;
    	final int minutesLength = MINUTES.length;
    	final int coef = 1000;
    	int selection = 0;
    	Integer interval = settings.SERVICE_OFF_INTERVAL.get();
    	String[] intDescriptions = new String[minutesLength + secondsLength];
		for (int i = 0; i < secondsLength; i++) {
			intDescriptions[i] = SECONDS[i] + " " + mapActivity.getString(R.string.int_seconds);
			if (interval == SECONDS[i] * coef) {
				selection = i;
			}
		}
		for (int i = 0; i < minutesLength; i++) {
			intDescriptions[secondsLength + i] = MINUTES[i] + " " + mapActivity.getString(R.string.int_min);
			if (interval == MINUTES[i] * 60 * coef) {
				selection = secondsLength + i;
			}
		}
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(mapActivity, R.layout.my_spinner_text, intDescriptions);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		intSpinner.setAdapter(adapter);
		intSpinner.setSelection(selection);
		intSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position < secondsLength) {
					settings.SERVICE_OFF_INTERVAL.set(SECONDS[position] * coef);
					return;
				} else {
					settings.SERVICE_OFF_INTERVAL.set(MINUTES[position - secondsLength] * 60 * coef);
					return;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		dialog.setNeutralButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		
		dialog.show();

	}
	
}
