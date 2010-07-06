package com.osmand.activities;

import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

import com.osmand.OsmandSettings;
import com.osmand.PoiFiltersHelper;
import com.osmand.ProgressDialogImplementation;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.OsmandSettings.ApplicationMode;
import com.osmand.activities.RouteProvider.RouteService;
import com.osmand.map.TileSourceManager;
import com.osmand.map.TileSourceManager.TileSourceTemplate;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {
	
	private CheckBoxPreference showPoiOnMap;
	private CheckBoxPreference useInternetToDownloadTiles;
	private ListPreference tileSourcePreference;
	private CheckBoxPreference rotateMapToBearing;
	private CheckBoxPreference showViewAngle;
	private ListPreference positionOnMap;
	private CheckBoxPreference useEnglishNames;
	private CheckBoxPreference showOsmBugs;
	private EditTextPreference userName;
	private CheckBoxPreference saveTrackToGpx;
	private ListPreference saveTrackInterval;
	private Preference saveCurrentTrack;
	private ListPreference applicationMode;
	private CheckBoxPreference autoZoom;
	private EditTextPreference userPassword;
	private Preference reloadIndexes;
	private Preference downloadIndexes;
	private ListPreference routerPreference;
	private ListPreference maxLevelToDownload;
	private CheckBoxPreference showTransport;
	private ListPreference mapScreenOrientation;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_pref);
		PreferenceScreen screen = getPreferenceScreen();
		
		applicationMode =(ListPreference) screen.findPreference(OsmandSettings.APPLICATION_MODE);
		applicationMode.setOnPreferenceChangeListener(this);
		
		useInternetToDownloadTiles = (CheckBoxPreference) screen.findPreference(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES);
		useInternetToDownloadTiles.setOnPreferenceChangeListener(this);
		showPoiOnMap =(CheckBoxPreference) screen.findPreference(OsmandSettings.SHOW_POI_OVER_MAP);
		showPoiOnMap.setOnPreferenceChangeListener(this);
		rotateMapToBearing =(CheckBoxPreference) screen.findPreference(OsmandSettings.ROTATE_MAP_TO_BEARING);
		rotateMapToBearing.setOnPreferenceChangeListener(this);
		showViewAngle =(CheckBoxPreference) screen.findPreference(OsmandSettings.SHOW_VIEW_ANGLE);
		showViewAngle.setOnPreferenceChangeListener(this);
		autoZoom =(CheckBoxPreference) screen.findPreference(OsmandSettings.AUTO_ZOOM_MAP);
		autoZoom.setOnPreferenceChangeListener(this);
		showOsmBugs =(CheckBoxPreference) screen.findPreference(OsmandSettings.SHOW_OSM_BUGS);
		showOsmBugs.setOnPreferenceChangeListener(this);
		showTransport =(CheckBoxPreference) screen.findPreference(OsmandSettings.SHOW_TRANSPORT_OVER_MAP);
		showTransport.setOnPreferenceChangeListener(this);
		
		useEnglishNames =(CheckBoxPreference) screen.findPreference(OsmandSettings.USE_ENGLISH_NAMES);
		useEnglishNames.setOnPreferenceChangeListener(this);
		reloadIndexes =(Preference) screen.findPreference(OsmandSettings.RELOAD_INDEXES);
		reloadIndexes.setOnPreferenceClickListener(this);
		downloadIndexes =(Preference) screen.findPreference(OsmandSettings.DOWNLOAD_INDEXES);
		downloadIndexes.setOnPreferenceClickListener(this);
		
		userName = (EditTextPreference) screen.findPreference(OsmandSettings.USER_NAME);
		userName.setOnPreferenceChangeListener(this);
		userPassword = (EditTextPreference) screen.findPreference(OsmandSettings.USER_PASSWORD);
		userPassword.setOnPreferenceChangeListener(this);
		
		saveTrackToGpx =(CheckBoxPreference) screen.findPreference(OsmandSettings.SAVE_TRACK_TO_GPX);
		saveTrackToGpx.setOnPreferenceChangeListener(this);
		saveTrackInterval =(ListPreference) screen.findPreference(OsmandSettings.SAVE_TRACK_INTERVAL);
		saveTrackInterval.setOnPreferenceChangeListener(this);
		saveCurrentTrack =(Preference) screen.findPreference(OsmandSettings.SAVE_CURRENT_TRACK);
		saveCurrentTrack.setOnPreferenceClickListener(this);
		
		
		positionOnMap =(ListPreference) screen.findPreference(OsmandSettings.POSITION_ON_MAP);
		positionOnMap.setOnPreferenceChangeListener(this);
		mapScreenOrientation =(ListPreference) screen.findPreference(OsmandSettings.MAP_SCREEN_ORIENTATION);
		mapScreenOrientation.setOnPreferenceChangeListener(this);
		maxLevelToDownload =(ListPreference) screen.findPreference(OsmandSettings.MAX_LEVEL_TO_DOWNLOAD_TILE);
		maxLevelToDownload.setOnPreferenceChangeListener(this);
		tileSourcePreference =(ListPreference) screen.findPreference(OsmandSettings.MAP_TILE_SOURCES);
		tileSourcePreference.setOnPreferenceChangeListener(this);
		routerPreference =(ListPreference) screen.findPreference(OsmandSettings.ROUTER_SERVICE);
		routerPreference.setOnPreferenceChangeListener(this);
		
    }
    
    @Override
    protected void onResume() {
		super.onResume();
		updateAllSettings();
	}
    
    public void updateAllSettings(){
    	useInternetToDownloadTiles.setChecked(OsmandSettings.isUsingInternetToDownloadTiles(this));
		showPoiOnMap.setChecked(OsmandSettings.isShowingPoiOverMap(this));
		rotateMapToBearing.setChecked(OsmandSettings.isRotateMapToBearing(this));
		showViewAngle.setChecked(OsmandSettings.isShowingViewAngle(this));
		showOsmBugs.setChecked(OsmandSettings.isShowingOsmBugs(this));
		showTransport.setChecked(OsmandSettings.isShowingTransportOverMap(this));
		saveTrackToGpx.setChecked(OsmandSettings.isSavingTrackToGpx(this));
		useEnglishNames.setChecked(OsmandSettings.usingEnglishNames(this));
		autoZoom.setChecked(OsmandSettings.isAutoZoomEnabled(this));
		userName.setText(OsmandSettings.getUserName(this));
		userPassword.setText(OsmandSettings.getUserPassword(this));
		
		Resources resources = this.getResources();
		String[] e = new String[] {resources.getString(R.string.position_on_map_center), 
				resources.getString(R.string.position_on_map_bottom)};
		positionOnMap.setEntryValues(e);
		positionOnMap.setEntries(e);
		positionOnMap.setValueIndex(OsmandSettings.getPositionOnMap(this));
		
		
		saveTrackInterval.setEntries(new String[]{
				resources.getString(R.string.interval_1_second),
				resources.getString(R.string.interval_2_seconds),
				resources.getString(R.string.interval_5_seconds),
				resources.getString(R.string.interval_15_seconds),
				resources.getString(R.string.interval_30_seconds),
				resources.getString(R.string.interval_1_minute),
				resources.getString(R.string.interval_5_minutes)});				
		saveTrackInterval.setEntryValues(new String[]{"1", "2", "5", "15", "30", "60", "300"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$  //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		saveTrackInterval.setValue(OsmandSettings.getSavingTrackInterval(this)+""); //$NON-NLS-1$
		
		

		mapScreenOrientation.setEntries(new String[]{
				resources.getString(R.string.map_orientation_portrait),
				resources.getString(R.string.map_orientation_landscape),
				resources.getString(R.string.map_orientation_default),
				});				
		mapScreenOrientation.setEntryValues(new String[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT+"", //$NON-NLS-1$
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE+"", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED+""}); //$NON-NLS-1$ //$NON-NLS-2$
		mapScreenOrientation.setValue(OsmandSettings.getMapOrientation(this)+""); //$NON-NLS-1$
		
		ApplicationMode[] presets = ApplicationMode.values(); 
		String[] values = new String[presets.length];
		String[] valueEntries = new String[presets.length];
		for(int i=0; i<presets.length; i++){
			values[i] = ApplicationMode.toHumanString(presets[i], this);
			valueEntries[i] = presets[i].name();
		}
		applicationMode.setEntries(values);
		applicationMode.setEntryValues(valueEntries);
		applicationMode.setValue(OsmandSettings.getApplicationMode(this).name());

		
		String[] entries = new String[RouteService.values().length];
		String entry = OsmandSettings.getRouterService(this).getName();
		for(int i=0; i<entries.length; i++){
			entries[i] = RouteService.values()[i].getName();
		}
		routerPreference.setEntries(entries);
		routerPreference.setEntryValues(entries);
		routerPreference.setValue(entry);
		
		int startZoom = 12;
		int endZoom = 19;
		entries = new String[endZoom - startZoom + 1];
		for (int i = startZoom; i <= endZoom; i++) {
			entries[i - startZoom] = i + ""; //$NON-NLS-1$
		}
		maxLevelToDownload.setEntries(entries);
		maxLevelToDownload.setEntryValues(entries);
		maxLevelToDownload.setValue(OsmandSettings.getMaximumLevelToDownloadTile(this)+""); //$NON-NLS-1$
		

		List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
		entries = new String[list.size()];
		for (int i = 0; i < list.size(); i++) {
			entries[i] = list.get(i).getName();
		}

		tileSourcePreference.setEntries(entries);
		tileSourcePreference.setEntryValues(entries);
		tileSourcePreference.setValue(OsmandSettings.getMapTileSourceName(this));
		String mapName = " " +OsmandSettings.getMapTileSourceName(this); //$NON-NLS-1$
		String summary = tileSourcePreference.getSummary().toString();
		if (summary.lastIndexOf(':') != -1) {
			summary = summary.substring(0, summary.lastIndexOf(':') + 1);
		}
		tileSourcePreference.setSummary(summary + mapName);
    }
    

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		SharedPreferences prefs = getSharedPreferences(OsmandSettings.SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		if(preference == applicationMode){
			edit.putString(OsmandSettings.APPLICATION_MODE, (String) newValue);
			setAppMode(ApplicationMode.valueOf(newValue.toString()), edit);
			edit.commit();
			updateAllSettings();
		} else if(preference == autoZoom){
			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, (Boolean) newValue);
			edit.commit();
		} else if(preference == showTransport){
			edit.putBoolean(OsmandSettings.SHOW_TRANSPORT_OVER_MAP, (Boolean) newValue);
			edit.commit();
		} else if(preference == showPoiOnMap){
			edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, (Boolean) newValue);
			if((Boolean)newValue){
				edit.putString(OsmandSettings.SELECTED_POI_FILTER_FOR_MAP, PoiFiltersHelper.getOsmDefinedFilterId(null));
			}
			edit.commit();
		} else if(preference == useInternetToDownloadTiles){
			edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, (Boolean) newValue);
			edit.commit();
		} else if(preference == rotateMapToBearing){
			edit.putBoolean(OsmandSettings.ROTATE_MAP_TO_BEARING, (Boolean) newValue);
			edit.commit();
		} else if(preference == useEnglishNames){
			edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, (Boolean) newValue);
			edit.commit();
		} else if(preference == saveTrackToGpx){
			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, (Boolean) newValue);
			edit.commit();
		} else if(preference == mapScreenOrientation){
			edit.putInt(OsmandSettings.MAP_SCREEN_ORIENTATION, Integer.parseInt(newValue.toString()));
			edit.commit();
		} else if(preference == saveTrackInterval){
			edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, Integer.parseInt(newValue.toString()));
			edit.commit();
		} else if(preference == userPassword){
			edit.putString(OsmandSettings.USER_PASSWORD, (String) newValue);
			edit.commit();
		} else if(preference == userName){
			edit.putString(OsmandSettings.USER_NAME, (String) newValue);
			edit.commit();
		} else if(preference == showViewAngle){
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, (Boolean) newValue);
			edit.commit();
		} else if(preference == showOsmBugs){
			edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, (Boolean) newValue);
			edit.commit();
		} else if(preference == positionOnMap){
			edit.putInt(OsmandSettings.POSITION_ON_MAP, positionOnMap.findIndexOfValue((String) newValue));
			edit.commit();
		} else if (preference == maxLevelToDownload) {
			edit.putInt(OsmandSettings.MAX_LEVEL_TO_DOWNLOAD_TILE, Integer.parseInt((String) newValue));
			edit.commit();
		} else if (preference == routerPreference) {
			RouteService s = null;
			for(RouteService r : RouteService.values()){
				if(r.getName().equals(newValue)){
					s = r;
					break;
				}
			}
			if(s != null){
				edit.putInt(OsmandSettings.ROUTER_SERVICE, s.ordinal());
			}
			edit.commit();
		} else if (preference == tileSourcePreference) {
			edit.putString(OsmandSettings.MAP_TILE_SOURCES, (String) newValue);
			edit.commit();
			String summary = tileSourcePreference.getSummary().toString();
			if (summary.lastIndexOf(':') != -1) {
				summary = summary.substring(0, summary.lastIndexOf(':') + 1);
			} 
			summary += " " + OsmandSettings.getMapTileSourceName(this); //$NON-NLS-1$
			tileSourcePreference.setSummary(summary);
			
		}
		return true;
	}
	
	public void reloadIndexes(){
		final ProgressDialog dlg = ProgressDialog.show(this, getString(R.string.loading_data), getString(R.string.reading_indexes), true);
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(dlg);
		impl.setRunnable("Initializing app", new Runnable(){ //$NON-NLS-1$
			@Override
			public void run() {
				try {
					showWarnings(ResourceManager.getResourceManager().reloadIndexes(impl));
				} finally {
					dlg.dismiss();
				}
			}
		});
		impl.run();

	}
	protected void showWarnings(List<String> warnings) {
		if (!warnings.isEmpty()) {
			final StringBuilder b = new StringBuilder();
			boolean f = true;
			for (String w : warnings) {
				if(f){
					f = false;
				} else {
					b.append('\n');
				}
				b.append(w);
			}
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(SettingsActivity.this, b.toString(), Toast.LENGTH_LONG).show();

				}
			});
		}
	}
		
	public void setAppMode(ApplicationMode preset, Editor edit){
		if(preset == ApplicationMode.CAR){
			edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, true);
//			edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, _);
			edit.putBoolean(OsmandSettings.ROTATE_MAP_TO_BEARING, true);
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, false);
			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, true);
			edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, false);
//			edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, true);
			edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, 15);
			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.BOTTOM_CONSTANT);
//			edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);
			
		} else if(preset == ApplicationMode.BICYCLE){
//			edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, _);
//			edit.putBoolean(OsmandSettings.USE_INTERNET_TO_CALCULATE_ROUTE, _);
			edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, true);
			edit.putBoolean(OsmandSettings.ROTATE_MAP_TO_BEARING, true);
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, false);
			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, false);
//			edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, _);
//			edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, true);
			edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, 30);
			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.BOTTOM_CONSTANT);
//			edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);
			
		} else if(preset == ApplicationMode.PEDESTRIAN){
//			edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, _);
			edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, true);
			edit.putBoolean(OsmandSettings.ROTATE_MAP_TO_BEARING, false);
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, true);
			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, false);
//			if(useInternetToDownloadTiles.isChecked()){
//				edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, true);
//			}
//			edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, false);
//			edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, _);
			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.CENTER_CONSTANT);
//			edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);
			
		} else if(preset == ApplicationMode.DEFAULT){
//			edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, _);
			edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, true);
			edit.putBoolean(OsmandSettings.ROTATE_MAP_TO_BEARING, false);
			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, false);
			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, false);
//			edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, _);
//			edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
//			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, _);
//			edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, _);
			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.CENTER_CONSTANT);
//			edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);
			
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(preference == downloadIndexes){
			startActivity(new Intent(this, DownloadIndexActivity.class));
			return true;
		} else if(preference == reloadIndexes){
			reloadIndexes();
			return true;
		} else if(preference == saveCurrentTrack){
			SavingTrackHelper helper = new SavingTrackHelper(this);
			helper.saveDataToGpx();
			helper.close();
			return true;
		}
		return false;
	}

}
