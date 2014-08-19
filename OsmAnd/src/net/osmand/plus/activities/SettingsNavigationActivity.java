package net.osmand.plus.activities;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings.AutoZoomMap;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
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
								p.getPossibleValueDescriptions(), svlss);
					}
					basePref.setTitle(SettingsBaseActivity.getRoutingStringPropertyName(this, p.getId(), p.getName()));
					basePref.setSummary(SettingsBaseActivity.getRoutingStringPropertyDescription(this, p.getId(), p.getDescription()));
					cat.addPreference(basePref);
				}
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
			showBooleanSettings(vals, bls);
			return true;
		} else if (preference == showAlarms) {
			showBooleanSettings(new String[] { getString(R.string.show_traffic_warnings), getString(R.string.show_cameras), 
					getString(R.string.show_lanes) }, new OsmandPreference[] { settings.SHOW_TRAFFIC_WARNINGS, 
					settings.SHOW_CAMERAS, settings.SHOW_LANES });
			return true;
		} else if (preference == speakAlarms) {
			showBooleanSettings(new String[] { getString(R.string.speak_street_names),  getString(R.string.speak_traffic_warnings), getString(R.string.speak_cameras), 
					getString(R.string.speak_speed_limit) }, new OsmandPreference[] { settings.SPEAK_STREET_NAMES, settings.SPEAK_TRAFFIC_WARNINGS, 
					settings.SPEAK_SPEED_CAMERA , settings.SPEAK_SPEED_LIMIT});
			return true;
		}
		return false;
	}

	public void showBooleanSettings(String[] vals, final OsmandPreference<Boolean>[] prefs) {
		Builder bld = new AlertDialog.Builder(this);
		boolean[] checkedItems = new boolean[prefs.length];
		for (int i = 0; i < prefs.length; i++) {
			checkedItems[i] = prefs[i].get();
		}
		bld.setMultiChoiceItems(vals, checkedItems, new OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				prefs[which].set(isChecked);
			}
		});
		bld.show();
	}

	
}
