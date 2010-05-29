package com.osmand.activities;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
	
	private CheckBoxPreference showPoiOnMap;
	private CheckBoxPreference useInternetToDownloadTiles;
	private ListPreference tileSourcePreference;
	private CheckBoxPreference rotateMapToBearing;
	private CheckBoxPreference showViewAngle;
	private ListPreference positionOnMap;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_pref);
		PreferenceScreen screen = getPreferenceScreen();
		useInternetToDownloadTiles = (CheckBoxPreference) screen.findPreference(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES);
		useInternetToDownloadTiles.setOnPreferenceChangeListener(this);
		showPoiOnMap =(CheckBoxPreference) screen.findPreference(OsmandSettings.SHOW_POI_OVER_MAP);
		showPoiOnMap.setOnPreferenceChangeListener(this);
		rotateMapToBearing =(CheckBoxPreference) screen.findPreference(OsmandSettings.ROTATE_MAP_TO_BEARING);
		rotateMapToBearing.setOnPreferenceChangeListener(this);
		showViewAngle =(CheckBoxPreference) screen.findPreference(OsmandSettings.SHOW_VIEW_ANGLE);
		showViewAngle.setOnPreferenceChangeListener(this);
		
		positionOnMap =(ListPreference) screen.findPreference(OsmandSettings.POSITION_ON_MAP);
		positionOnMap.setOnPreferenceChangeListener(this);
		tileSourcePreference =(ListPreference) screen.findPreference(OsmandSettings.MAP_TILE_SOURCES);
		tileSourcePreference.setOnPreferenceChangeListener(this);
		
		
    }
    
    @Override
    protected void onResume() {
		super.onResume();
		useInternetToDownloadTiles.setChecked(OsmandSettings.isUsingInternetToDownloadTiles(this));
		showPoiOnMap.setChecked(OsmandSettings.isShowingPoiOverMap(this));
		rotateMapToBearing.setChecked(OsmandSettings.isRotateMapToBearing(this));
		showViewAngle.setChecked(OsmandSettings.isShowingViewAngle(this));
		String[] e = new String[] { "Center", "Bottom" };
		positionOnMap.setEntryValues(e);
		positionOnMap.setEntries(e);
		positionOnMap.setValueIndex(OsmandSettings.getPositionOnMap(this));
		

		List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
		String[] entries = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			entries[i] = list.get(i).getName();
		}

		tileSourcePreference.setEntries(entries);
		tileSourcePreference.setEntryValues(entries);
		tileSourcePreference.setValue(OsmandSettings.getMapTileSourceName(this));
		String mapName = " " +OsmandSettings.getMapTileSourceName(this);
		tileSourcePreference.setSummary(tileSourcePreference.getSummary() + mapName);
	}
    

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		if(preference == showPoiOnMap){
			edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, (Boolean) newValue);
			edit.commit();
		} else if(preference == useInternetToDownloadTiles){
			edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, (Boolean) newValue);
			edit.commit();
		} else if(preference == rotateMapToBearing){
			edit.putBoolean(OsmandSettings.ROTATE_MAP_TO_BEARING, (Boolean) newValue);
			edit.commit();
		} else if(preference == showViewAngle){
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, (Boolean) newValue);
			edit.commit();
		} else if(preference == positionOnMap){
			edit.putInt(OsmandSettings.POSITION_ON_MAP, positionOnMap.findIndexOfValue((String) newValue));
			edit.commit();
		} else if (preference == tileSourcePreference) {
			edit.putString(OsmandSettings.MAP_TILE_SOURCES, (String) newValue);
			edit.commit();
			String summary = tileSourcePreference.getSummary().toString();
			if (summary.lastIndexOf(':') != -1) {
				summary = summary.substring(0, summary.lastIndexOf(':') + 1);
			} 
			summary += " " + OsmandSettings.getMapTileSourceName(this);
			tileSourcePreference.setSummary(summary);
			
		}
		return true;
	}

}
