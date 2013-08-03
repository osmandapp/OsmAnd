package net.osmand.plus.activities;


import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.routing.RouteProvider.RouteService;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

public class SettingsNavigationActivity extends SettingsBaseActivity {

	private Preference avoidRouting;
	private Preference preferRouting;
	private Preference showAlarms;
	private Preference speakAlarms;
	private ListPreference routerServicePreference;
	public static final String MORE_VALUE = "MORE_VALUE";
	
	public SettingsNavigationActivity() {
		super(true);
	}
	

	private Set<String> getVoiceFiles() {
		// read available voice data
		File extStorage = getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR);
		Set<String> setFiles = new LinkedHashSet<String>();
		if (extStorage.exists()) {
			for (File f : extStorage.listFiles()) {
				if (f.isDirectory()) {
					setFiles.add(f.getName());
				}
			}
		}
		return setFiles;
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
		
		registerBooleanPreference(settings.AUTO_ZOOM_MAP, screen);
		registerBooleanPreference(settings.FAST_ROUTE_MODE, screen);
		registerBooleanPreference(settings.PRECISE_ROUTING_MODE, screen);
		registerBooleanPreference(settings.SNAP_TO_ROAD, screen);
		registerBooleanPreference(settings.USE_COMPASS_IN_NAVIGATION, screen);
		
		Integer[] intValues = new Integer[] { 0, 5, 10, 15, 20, 25, 30, 45, 60, 90};
		String[] entries = new String[intValues.length];
		entries[0] = getString(R.string.auto_follow_route_never);
		for (int i = 1; i < intValues.length; i++) {
			entries[i] = (int) intValues[i] + " " + getString(R.string.int_seconds);
		}
		registerListPreference(settings.AUTO_FOLLOW_ROUTE, screen, entries, intValues);
		
		entries = new String[RouteService.values().length];
		for(int i=0; i<entries.length; i++){
			entries[i] = RouteService.values()[i].getName();
		}
		registerListPreference(settings.ROUTER_SERVICE, screen, entries, RouteService.values());
		
		routerServicePreference = (ListPreference) screen.findPreference(settings.ROUTER_SERVICE.getId());
		routerServicePreference.setOnPreferenceChangeListener(this);

		
		avoidRouting = (Preference) screen.findPreference("avoid_in_routing");
		avoidRouting.setOnPreferenceClickListener(this);
		preferRouting = (Preference) screen.findPreference("prefer_in_routing");
		preferRouting.setOnPreferenceClickListener(this);
		
		showAlarms = (Preference) screen.findPreference("show_routing_alarms");
		showAlarms.setOnPreferenceClickListener(this);
		
		speakAlarms = (Preference) screen.findPreference("speak_routing_alarms");
		speakAlarms.setOnPreferenceClickListener(this);
		
		reloadVoiceListPreference(screen);
		
		profileDialog();
	}


	private void reloadVoiceListPreference(PreferenceScreen screen) {
		String[] entries;
		String[] entrieValues;
		Set<String> voiceFiles = getVoiceFiles();
		entries = new String[voiceFiles.size() + 2];
		entrieValues = new String[voiceFiles.size() + 2];
		int k = 0;
		// entries[k++] = getString(R.string.voice_not_specified);
		entrieValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k++] = getString(R.string.voice_not_use);
		for (String s : voiceFiles) {
			entries[k] = s;
			entrieValues[k] = s;
			k++;
		}
		entrieValues[k] = MORE_VALUE;
		entries[k] = getString(R.string.install_more);
		registerListPreference(settings.VOICE_PROVIDER, screen, entries, entrieValues);
	}


	public void updateAllSettings() {
		reloadVoiceListPreference(getPreferenceScreen());
		super.updateAllSettings();
		routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  [" + settings.ROUTER_SERVICE.get() + "]");
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String id = preference.getKey();
		if (id.equals(settings.VOICE_PROVIDER.getId())) {
			if (MORE_VALUE.equals(newValue)) {
				// listPref.set(oldValue); // revert the change..
				final Intent intent = new Intent(this, DownloadIndexActivity.class);
				intent.putExtra(DownloadIndexActivity.FILTER_KEY, "voice");
				startActivity(intent);
			} else {
				super.onPreferenceChange(preference, newValue);
				getMyApplication().showDialogInitializingCommandPlayer(this, false);
			}
			return true;
		}
		boolean changed = super.onPreferenceChange(preference, newValue);
		
		if (id.equals(settings.ROUTER_SERVICE.getId())) {
			routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  ["
					+ settings.ROUTER_SERVICE.get() + "]");
		}
		return true;
	}


	@SuppressWarnings("unchecked")
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == avoidRouting) {
			showBooleanSettings(new String[] { getString(R.string.avoid_toll_roads), getString(R.string.avoid_ferries),
					getString(R.string.avoid_unpaved), getString(R.string.avoid_motorway)
					}, new OsmandPreference[] { settings.AVOID_TOLL_ROADS,
					settings.AVOID_FERRIES, settings.AVOID_UNPAVED_ROADS, settings.AVOID_MOTORWAY });
			return true;
		} else if (preference == preferRouting) {
			showBooleanSettings(new String[] { getString(R.string.prefer_motorways)}, 
					new OsmandPreference[] { settings.PREFER_MOTORWAYS});
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
