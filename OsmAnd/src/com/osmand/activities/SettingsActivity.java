package com.osmand.activities;

import java.util.List;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;

import com.osmand.OsmandSettings;
import com.osmand.R;
import com.osmand.map.TileSourceManager;
import com.osmand.map.TileSourceManager.TileSourceTemplate;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	private static final String use_internet_to_download_tiles = "use_internet_to_download_tiles";
	private static final String show_gps_location_text = "show_gps_location_text";
	private static final String map_tile_sources = "map_tile_sources";
	private static final String show_poi_over_map = "show_poi_over_map";
	
	private CheckBoxPreference showGpsLocation;
	private CheckBoxPreference showPoiOnMap;
	private CheckBoxPreference useInternetToDownloadTiles;
	private ListPreference tileSourcePreference;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_pref);
		PreferenceScreen screen = getPreferenceScreen();
		showGpsLocation =(CheckBoxPreference) screen.findPreference(show_gps_location_text);
		showGpsLocation.setOnPreferenceChangeListener(this);
		useInternetToDownloadTiles =(CheckBoxPreference) screen.findPreference(use_internet_to_download_tiles);
		useInternetToDownloadTiles.setOnPreferenceChangeListener(this);
		showPoiOnMap =(CheckBoxPreference) screen.findPreference(show_poi_over_map);
		showPoiOnMap.setOnPreferenceChangeListener(this);
		
		tileSourcePreference =(ListPreference) screen.findPreference(map_tile_sources);
		tileSourcePreference.setOnPreferenceChangeListener(this);
		
		
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	useInternetToDownloadTiles.setChecked(OsmandSettings.useInternetToDownloadTiles);
    	showGpsLocation.setChecked(OsmandSettings.showGPSLocationOnMap);
    	showPoiOnMap.setChecked(OsmandSettings.showPoiOverMap);
    	
    	List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
    	String[] entries = new String[list.size()];
    	for(int i=0; i<list.size(); i++){
    		entries[i] = list.get(i).getName();
    	}
    	tileSourcePreference.setEntries(entries);
    	tileSourcePreference.setEntryValues(entries);
    	tileSourcePreference.setValue(OsmandSettings.tileSource.getName());
    	
    }
    

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference == showGpsLocation){
			OsmandSettings.showGPSLocationOnMap = (Boolean) newValue;
		} else if(preference == showPoiOnMap){
			OsmandSettings.showPoiOverMap = (Boolean) newValue;
		} else if(preference == useInternetToDownloadTiles){
			OsmandSettings.useInternetToDownloadTiles = (Boolean) newValue;
		} else if (preference == tileSourcePreference) {
			String newTile = newValue.toString();
			for (TileSourceTemplate t : TileSourceManager.getKnownSourceTemplates()) {
				if (t.getName().equals(newTile)) {
					OsmandSettings.tileSource = t;
					break;
				}
			}
		}
		return true;
	}

}
