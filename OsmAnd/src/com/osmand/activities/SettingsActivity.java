package com.osmand.activities;

import java.util.List;

import com.osmand.DefaultLauncherConstants;
import com.osmand.R;
import com.osmand.R.xml;
import com.osmand.map.TileSourceManager;
import com.osmand.map.TileSourceManager.TileSourceTemplate;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {
	private static final String use_internet_to_download_tiles = "use_internet_to_download_tiles";
	private static final String show_gps_location_text = "show_gps_location_text";
	private static final String map_tile_sources = "map_tile_sources";
	private CheckBoxPreference showGpsLocation;
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
		
		tileSourcePreference =(ListPreference) screen.findPreference(map_tile_sources);
		tileSourcePreference.setOnPreferenceChangeListener(this);
		
		
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	useInternetToDownloadTiles.setChecked(DefaultLauncherConstants.loadMissingImages);
    	showGpsLocation.setChecked(DefaultLauncherConstants.showGPSCoordinates);
    	List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
    	String[] entries = new String[list.size()];
    	for(int i=0; i<list.size(); i++){
    		entries[i] = list.get(i).getName();
    	}
    	tileSourcePreference.setEntries(entries);
    	tileSourcePreference.setEntryValues(entries);
    	tileSourcePreference.setValue(DefaultLauncherConstants.MAP_defaultTileSource.getName());
    }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference == showGpsLocation){
			DefaultLauncherConstants.showGPSCoordinates = (Boolean) newValue;
		} else if(preference == useInternetToDownloadTiles){
			DefaultLauncherConstants.loadMissingImages = (Boolean) newValue;
		} else if(preference == tileSourcePreference){
			String newTile = newValue.toString();
	    	for(TileSourceTemplate t : TileSourceManager.getKnownSourceTemplates()){
	    		if(t.getName().equals(newTile)){
	    			DefaultLauncherConstants.MAP_defaultTileSource = t;
	    			break;
	    		}
	    	}	
		}
		return true;
	}

}
