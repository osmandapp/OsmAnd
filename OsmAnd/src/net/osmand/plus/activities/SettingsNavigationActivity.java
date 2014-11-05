package net.osmand.plus.activities;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.DeviceAdminRecv;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.AutoZoomMap;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class SettingsNavigationActivity extends SettingsBaseActivity {

	private Preference avoidRouting;
	private Preference preferRouting;
	private Preference showAlarms;
	private Preference speakAlarms;
	private ListPreference routerServicePreference;
	private ListPreference autoZoomMapPreference;
	private ListPreference speedLimitExceed;
	
	private ComponentName mDeviceAdmin;
	private static final int DEVICE_ADMIN_REQUEST = 5;
	
	private List<RoutingParameter> avoidParameters = new ArrayList<RoutingParameter>();
	private List<RoutingParameter> preferParameters = new ArrayList<RoutingParameter>();
	
	public SettingsNavigationActivity() {
		super(true);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.routing_settings);
	
		createUI();
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == DEVICE_ADMIN_REQUEST) {
			if (resultCode == RESULT_OK) {
//				Log.d("DeviceAdmin", "Lock screen permission approved.");
			} else {
				settings.WAKE_ON_VOICE_INT.set(0);
//				Log.d("DeviceAdmin", "Lock screen permission refused.");
			}
			return;
		}
	}

	private void requestLockScreenAdmin() {
		mDeviceAdmin = new ComponentName(getApplicationContext(),
				DeviceAdminRecv.class);

		DevicePolicyManager mDevicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);

		if (!mDevicePolicyManager.isAdminActive(mDeviceAdmin)) {
			// request permission from user
			Intent intent = new Intent(
					DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
					mDeviceAdmin);
			intent.putExtra(
					DevicePolicyManager.EXTRA_ADD_EXPLANATION,
					getString(R.string.lock_screen_request_explanation,
							Version.getAppName(getMyApplication())));
			startActivityForResult(intent, DEVICE_ADMIN_REQUEST);
		}
	};
	
	private void createUI() {
		addPreferencesFromResource(R.xml.navigation_settings);
		PreferenceScreen screen = getPreferenceScreen();
		settings = getMyApplication().getSettings();
		routerServicePreference = (ListPreference) screen.findPreference(settings.ROUTER_SERVICE.getId());
		
		RouteService[] vls = RouteService.getAvailableRouters(getMyApplication());
		String[] entries = new String[vls.length];
		for(int i=0; i<entries.length; i++){
			entries[i] = vls[i].getName();
		}
		registerListPreference(settings.ROUTER_SERVICE, screen, entries, vls);
		
		
		registerBooleanPreference(settings.SNAP_TO_ROAD, screen);

		Integer[] intValues = new Integer[] { 0, 5, 10, 15, 20, 25, 30, 45, 60, 90};
		entries = new String[intValues.length];
		entries[0] = getString(R.string.auto_follow_route_never);
		for (int i = 1; i < intValues.length; i++) {
			entries[i] = (int) intValues[i] + " " + getString(R.string.int_seconds);
		}
		registerListPreference(settings.AUTO_FOLLOW_ROUTE, screen, entries, intValues);

		entries = new String[AutoZoomMap.values().length];
		for(int i=0; i<entries.length; i++){
			entries[i] = getString(AutoZoomMap.values()[i].name);
		}
		registerListPreference(settings.AUTO_ZOOM_MAP, screen, entries, AutoZoomMap.values());
		
        //keep informing option:
        Integer[] keepInformingValues = new Integer[]{0, 1, 2, 3, 5, 7, 10, 15, 20, 25, 30};
        String[] keepInformingNames = new String[keepInformingValues.length];
        keepInformingNames[0] = getString(R.string.keep_informing_never);
        for (int i = 1; i < keepInformingValues.length; i++)
        {
            keepInformingNames[i] = keepInformingValues[i] + " " + getString(R.string.int_min);
        }
        registerListPreference(settings.KEEP_INFORMING, screen, keepInformingNames, keepInformingValues);
        
		// screen power save option:
		Integer[] screenPowerSaveValues = new Integer[] { 0, 5, 10, 15, 20, 30, 45, 60 };
		String[] screenPowerSaveNames = new String[screenPowerSaveValues.length];
		screenPowerSaveNames[0] = getString(R.string.wake_on_voice_never);
		for (int i = 1; i < screenPowerSaveValues.length; i++) {
			screenPowerSaveNames[i] = screenPowerSaveValues[i] + " "
					+ getString(R.string.int_seconds);
		}
		registerListPreference(settings.WAKE_ON_VOICE_INT, screen, screenPowerSaveNames, screenPowerSaveValues);
        
        registerBooleanPreference(settings.SHOW_ZOOM_BUTTONS_NAVIGATION, screen);

		autoZoomMapPreference = (ListPreference) screen.findPreference(settings.AUTO_ZOOM_MAP.getId());
		autoZoomMapPreference.setOnPreferenceChangeListener(this);

		
		showAlarms = (Preference) screen.findPreference("show_routing_alarms");
		showAlarms.setOnPreferenceClickListener(this);
		
		speakAlarms = (Preference) screen.findPreference("speak_routing_alarms");
		speakAlarms.setOnPreferenceClickListener(this);
		
		Float[] arrivalValues = new Float[] {1.5f, 1f, 0.5f, 0.25f} ;
		String[] arrivalNames = new String[] {
				getString(R.string.arrival_distance_factor_early),
				getString(R.string.arrival_distance_factor_normally),
				getString(R.string.arrival_distance_factor_late),
				getString(R.string.arrival_distance_factor_at_last)
		};
		registerListPreference(settings.ARRIVAL_DISTANCE_FACTOR, screen, arrivalNames, arrivalValues);

		//array size should be equal!
		Float[] speedLimitsKm = new Float[]{0f, 5f, 7f, 10f, 15f, 20f};
		Float[] speedLimitsMiles = new Float[]{0f, 3f, 5f, 7f, 10f, 15f};
		if (settings.METRIC_SYSTEM.get() == OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS) {
			String[] speedNames = new String[speedLimitsKm.length];
			for (int i =0; i<speedLimitsKm.length;i++){
				speedNames[i] = speedLimitsKm[i] + " " + getString(R.string.km_h);
			}
			registerListPreference(settings.SPEED_LIMIT_EXCEED, screen, speedNames, speedLimitsKm);
		} else {
			String[] speedNames = new String[speedLimitsKm.length];
			for (int i =0; i<speedNames.length;i++){
				speedNames[i] = speedLimitsMiles[i] + " " + getString(R.string.mile_per_hour);
			}
			registerListPreference(settings.SPEED_LIMIT_EXCEED, screen, speedNames, speedLimitsKm);
		}

		PreferenceCategory category = (PreferenceCategory) screen.findPreference("guidance_preferences");
		speedLimitExceed = (ListPreference) category.findPreference("speed_limit_exceed");
		ApplicationMode mode = getMyApplication().getSettings().getApplicationMode();
		if (!mode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			category.removePreference(speedLimitExceed);
		}


		profileDialog();
	}
	
	


	private void prepareRoutingPrefs(PreferenceScreen screen) {
		PreferenceCategory cat = (PreferenceCategory) screen.findPreference("routing_preferences");
		cat.removeAll();
		CheckBoxPreference fastRoute = createCheckBoxPreference(settings.FAST_ROUTE_MODE, R.string.fast_route_mode, R.string.fast_route_mode_descr);
		if(settings.ROUTER_SERVICE.get() != RouteService.OSMAND) {
			cat.addPreference(fastRoute);
		} else {
			ApplicationMode am = settings.getApplicationMode();
			GeneralRouter router = getRouter(getMyApplication().getDefaultRoutingConfig(), am);
			clearParameters();
			if (router != null) {
				Map<String, RoutingParameter> parameters = router.getParameters();
				if(parameters.containsKey("short_way")) {
					cat.addPreference(fastRoute);
				}
				List<RoutingParameter> others = new ArrayList<GeneralRouter.RoutingParameter>();
				for(Map.Entry<String, RoutingParameter> e : parameters.entrySet()) {
					String param = e.getKey();
					if(param.startsWith("avoid_")) {
						avoidParameters.add(e.getValue());
					} else if(param.startsWith("prefer_")) {
						preferParameters.add(e.getValue());
					} else if(!param.equals("short_way")) {
						others.add(e.getValue());
					}
				}
				if (avoidParameters.size() > 0) {
					avoidRouting = new Preference(this);
					avoidRouting.setTitle(R.string.avoid_in_routing_title);
					avoidRouting.setSummary(R.string.avoid_in_routing_descr);
					avoidRouting.setOnPreferenceClickListener(this);
					cat.addPreference(avoidRouting);
				}
				if (preferParameters.size() > 0) {
					preferRouting = new Preference(this);
					preferRouting.setTitle(R.string.prefer_in_routing_title);
					preferRouting.setSummary(R.string.prefer_in_routing_descr);
					preferRouting.setOnPreferenceClickListener(this);
					cat.addPreference(preferRouting);
				}
				for(RoutingParameter p : others) {
					Preference basePref;
					if(p.getType() == RoutingParameterType.BOOLEAN) {
						basePref = createCheckBoxPreference(settings.getCustomRoutingBooleanProperty(p.getId()));
					} else {
						Object[] vls = p.getPossibleValues();
						String[] svlss = new String[vls.length];
						int i = 0;
						for(Object o : vls) {
							svlss[i++] = o.toString();
						}
						basePref = createListPreference(settings.getCustomRoutingProperty(p.getId()), 
								p.getPossibleValueDescriptions(), svlss, SettingsBaseActivity.getRoutingStringPropertyName(this, p.getId(), p.getName()), SettingsBaseActivity.getRoutingStringPropertyDescription(this, p.getId(), p.getDescription()));
					}
					basePref.setTitle(SettingsBaseActivity.getRoutingStringPropertyName(this, p.getId(), p.getName()));
					basePref.setSummary(SettingsBaseActivity.getRoutingStringPropertyDescription(this, p.getId(), p.getDescription()));
					cat.addPreference(basePref);
				}
			}
			ApplicationMode mode = getMyApplication().getSettings().getApplicationMode();
			if (mode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
				PreferenceCategory category = (PreferenceCategory) screen.findPreference("guidance_preferences");
				category.addPreference(speedLimitExceed);
			} else {
				PreferenceCategory category = (PreferenceCategory) screen.findPreference("guidance_preferences");
				category.removePreference(speedLimitExceed);
			}
		}
	}


	private void clearParameters() {
		preferParameters.clear();
		avoidParameters.clear();
	}


	public static GeneralRouter getRouter(net.osmand.router.RoutingConfiguration.Builder builder, ApplicationMode am) {
		GeneralRouter router = builder.getRouter(am.getStringKey());
		if(router == null && am.getParent() != null) {
			router = builder.getRouter(am.getParent().getStringKey());
		}
		return router;
	}


	


	public void updateAllSettings() {	
		prepareRoutingPrefs(getPreferenceScreen());
		super.updateAllSettings();
		routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  [" + settings.ROUTER_SERVICE.get() + "]");
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String id = preference.getKey();
		super.onPreferenceChange(preference, newValue);
		if (id.equals(settings.ROUTER_SERVICE.getId())) {
			routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  ["
					+ settings.ROUTER_SERVICE.get() + "]");
			prepareRoutingPrefs(getPreferenceScreen());
			super.updateAllSettings();
		} else if (id.equals(settings.WAKE_ON_VOICE_INT.getId())) {
			Integer value;
			try {
				value = Integer.parseInt(newValue.toString());
			} catch (NumberFormatException e) {
				value = 0;
			}
			if (value > 0) {
				requestLockScreenAdmin();
			}
		}
		return true;
	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == avoidRouting || preference == preferRouting) {
			List<RoutingParameter> prms = preference == avoidRouting ? avoidParameters : preferParameters; 
			String[] vals = new String[prms.size()];
			OsmandPreference[] bls = new OsmandPreference[prms.size()];
			for(int i = 0; i < prms.size(); i++) {
				RoutingParameter p =  prms.get(i);
				vals[i] = SettingsBaseActivity.getRoutingStringPropertyName(this, p.getId(), p.getName());
				bls[i] = settings.getCustomRoutingBooleanProperty(p.getId());
			}
			showBooleanSettings(vals, bls, preference.getTitle());
			return true;
		} else if (preference == showAlarms) {
			showBooleanSettings(new String[] { getString(R.string.show_traffic_warnings), getString(R.string.show_cameras), 
					getString(R.string.show_lanes) }, new OsmandPreference[] { settings.SHOW_TRAFFIC_WARNINGS, 
					settings.SHOW_CAMERAS, settings.SHOW_LANES }, preference.getTitle());
			return true;
		} else if (preference == speakAlarms) {
			showBooleanSettings(new String[] { getString(R.string.speak_street_names),  getString(R.string.speak_traffic_warnings), 
					getString(R.string.speak_speed_limit), getString(R.string.speak_cameras),
					getString(R.string.speak_favorites),
					getString(R.string.speak_poi),
					getString(R.string.announce_gpx_waypoints)}, 
					new OsmandPreference[] { settings.SPEAK_STREET_NAMES, settings.SPEAK_TRAFFIC_WARNINGS, 
					settings.SPEAK_SPEED_LIMIT, settings.SPEAK_SPEED_CAMERA, 
					settings.ANNOUNCE_NEARBY_FAVORITES, settings.ANNOUNCE_NEARBY_POI, settings.ANNOUNCE_WPT}, preference.getTitle());
			return true;
		}
		return false;
	}

	public void showBooleanSettings(String[] vals, final OsmandPreference<Boolean>[] prefs, final CharSequence title) {
		Builder bld = new AlertDialog.Builder(this);
		boolean[] checkedItems = new boolean[prefs.length];
		for (int i = 0; i < prefs.length; i++) {
			checkedItems[i] = prefs[i].get();
		}
		
		final boolean[] tempPrefs = new boolean[prefs.length];
		for (int i = 0; i < prefs.length; i++) {
			tempPrefs[i] = prefs[i].get();
		}
		
		bld.setMultiChoiceItems(vals, checkedItems, new OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				tempPrefs[which] = isChecked;
			}
		});
		
		bld.setTitle(title);
		
		bld.setNegativeButton(R.string.default_buttons_cancel, null);
		
		bld.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
				for (int i = 0; i < prefs.length; i++) {
					prefs[i].set(tempPrefs[i]);
				}
		    }
		});
		
		bld.show();
	}

	
}
