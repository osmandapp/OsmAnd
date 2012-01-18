package net.osmand.plus.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.osmand.ResultMatcher;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.DayNightMode;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.views.SeekBarPreference;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {
	
	public static final String INTENT_KEY_SETTINGS_SCREEN = "INTENT_KEY_SETTINGS_SCREEN";
	public static final int SCREEN_GENERAL_SETTINGS = 1;
	public static final int SCREEN_NAVIGATION_SETTINGS = 2;
	public static final int SCREEN_MONITORING_SETTINGS = 3;
	
	private static final String MORE_VALUE = "MORE_VALUE";
	
	private Preference saveCurrentTrack;
	private Preference testVoiceCommands;

	private EditTextPreference applicationDir;
	private ListPreference applicationModePreference;
	private ListPreference tileSourcePreference;
	private ListPreference overlayPreference;
	private ListPreference underlayPreference;

	private ListPreference dayNightModePreference;
	private ListPreference routerServicePreference;

	private CheckBoxPreference routeServiceEnabled;
	private BroadcastReceiver broadcastReceiver;
	
	private ProgressDialog progressDlg;
	
	private OsmandSettings osmandSettings;
	
	private Map<String, Preference> screenPreferences = new LinkedHashMap<String, Preference>();
	private Map<String, OsmandPreference<Boolean>> booleanPreferences = new LinkedHashMap<String, OsmandPreference<Boolean>>();
	private Map<String, OsmandPreference<?>> listPreferences = new LinkedHashMap<String, OsmandPreference<?>>();
	private Map<String, OsmandPreference<String>> editTextPreferences = new LinkedHashMap<String, OsmandPreference<String>>();
	private Map<String, OsmandPreference<Integer>> seekBarPreferences = new LinkedHashMap<String, OsmandPreference<Integer>>();
	
	private Map<String, Map<String, ?>> listPrefValues = new LinkedHashMap<String, Map<String, ?>>();
	
	private CheckBoxPreference registerBooleanPreference(OsmandPreference<Boolean> b, PreferenceScreen screen){
		CheckBoxPreference p = (CheckBoxPreference) screen.findPreference(b.getId());
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		booleanPreferences.put(b.getId(), b);
		return p;
	}
	
	private void registerSeekBarPreference(OsmandPreference<Integer> b, PreferenceScreen screen){
		SeekBarPreference p = (SeekBarPreference) screen.findPreference(b.getId());
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		seekBarPreferences.put(b.getId(), b);
	}
	
	private <T> void registerListPreference(OsmandPreference<T> b, PreferenceScreen screen, String[] names, T[] values){
		ListPreference p = (ListPreference) screen.findPreference(b.getId());
		p.setOnPreferenceChangeListener(this);
		LinkedHashMap<String, Object> vals = new LinkedHashMap<String, Object>();
		screenPreferences.put(b.getId(), p);
		listPreferences.put(b.getId(), b);
		listPrefValues.put(b.getId(), vals);
		assert names.length == values.length;
		for(int i=0; i<names.length; i++){
			vals.put(names[i], values[i]);
		}
	}
	
	private void registerEditTextPreference(OsmandPreference<String> b, PreferenceScreen screen){
		EditTextPreference p = (EditTextPreference) screen.findPreference(b.getId());
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		editTextPreferences.put(b.getId(), b);
	}
	
	private  void registerTimeListPreference(OsmandPreference<Integer> b, PreferenceScreen screen, int[] seconds, int[] minutes, int coeff){
		int minutesLength = minutes == null? 0 : minutes.length;
    	int secondsLength = seconds == null? 0 : seconds.length;
    	Integer[] ints = new Integer[secondsLength + minutesLength];
		String[] intDescriptions = new String[ints.length];
		for (int i = 0; i < secondsLength; i++) {
			ints[i] = seconds[i] * coeff;
			intDescriptions[i] = seconds[i] + " " + getString(R.string.int_seconds); //$NON-NLS-1$
		}
		for (int i = 0; i < minutesLength; i++) {
			ints[secondsLength + i] = (minutes[i] * 60) * coeff;
			intDescriptions[secondsLength + i] = minutes[i] + " " + getString(R.string.int_min); //$NON-NLS-1$
		}
		registerListPreference(b, screen, intDescriptions, ints);
	}
	
	private Set<String> getVoiceFiles(){
		// read available voice data
		File extStorage = osmandSettings.extendOsmandPath(ResourceManager.VOICE_PATH);
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
		addPreferencesFromResource(R.xml.settings_pref);
		String[] entries;
		String[] entrieValues;
		PreferenceScreen screen = getPreferenceScreen();
		osmandSettings = OsmandSettings.getOsmandSettings(this);
		
		registerBooleanPreference(osmandSettings.SHOW_VIEW_ANGLE,screen); 
	    registerBooleanPreference(osmandSettings.USE_TRACKBALL_FOR_MOVEMENTS,screen); 
	    registerBooleanPreference(osmandSettings.USE_HIGH_RES_MAPS,screen); 
	    registerBooleanPreference(osmandSettings.USE_ENGLISH_NAMES,screen); 
	    registerBooleanPreference(osmandSettings.AUTO_ZOOM_MAP,screen); 
	    registerBooleanPreference(osmandSettings.AUTO_FOLLOW_ROUTE_NAV,screen);
	    registerBooleanPreference(osmandSettings.SAVE_TRACK_TO_GPX,screen); 
	    registerBooleanPreference(osmandSettings.DEBUG_RENDERING_INFO,screen); 
	    registerBooleanPreference(osmandSettings.FAST_ROUTE_MODE,screen);
	    registerBooleanPreference(osmandSettings.USE_OSMAND_ROUTING_SERVICE_ALWAYS,screen); 
	    registerBooleanPreference(osmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES,screen);
	    registerBooleanPreference(osmandSettings.MAP_VECTOR_DATA,screen);
	    registerBooleanPreference(osmandSettings.TRANSPARENT_MAP_THEME,screen);
	    registerBooleanPreference(osmandSettings.TEST_ANIMATE_ROUTING,screen);
	    registerBooleanPreference(osmandSettings.SHOW_ALTITUDE_INFO,screen);
	    registerBooleanPreference(osmandSettings.SHOW_ZOOM_LEVEL,screen);
	    CheckBoxPreference nativeCheckbox = registerBooleanPreference(osmandSettings.NATIVE_RENDERING,screen);
	    //disable the checkbox if the library cannot be used
	    if (NativeOsmandLibrary.isLoaded() && !NativeOsmandLibrary.isSupported()) {
	    	nativeCheckbox.setEnabled(false);
	    }
	    
		registerEditTextPreference(osmandSettings.USER_NAME, screen);
		registerEditTextPreference(osmandSettings.USER_PASSWORD, screen);
		
		
		registerSeekBarPreference(osmandSettings.MAP_OVERLAY_TRANSPARENCY, screen);
		registerSeekBarPreference(osmandSettings.MAP_TRANSPARENCY, screen);
		
		// List preferences
		registerListPreference(osmandSettings.ROTATE_MAP, screen, 
				new String[]{getString(R.string.rotate_map_none_opt), getString(R.string.rotate_map_bearing_opt), getString(R.string.rotate_map_compass_opt)},
				new Integer[]{OsmandSettings.ROTATE_MAP_NONE, OsmandSettings.ROTATE_MAP_BEARING, OsmandSettings.ROTATE_MAP_COMPASS});
		
		registerListPreference(osmandSettings.MAP_SCREEN_ORIENTATION, screen, 
				new String[] {getString(R.string.map_orientation_portrait), getString(R.string.map_orientation_landscape), getString(R.string.map_orientation_default)},
				new Integer[] {ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED});
		
		registerListPreference(osmandSettings.POSITION_ON_MAP, screen,
				new String[] {getString(R.string.position_on_map_center), getString(R.string.position_on_map_bottom)},
				new Integer[] {OsmandSettings.CENTER_CONSTANT, OsmandSettings.BOTTOM_CONSTANT});
		
		registerListPreference(osmandSettings.AUDIO_STREAM_GUIDANCE, screen,
				new String[] {getString(R.string.voice_stream_music), getString(R.string.voice_stream_notification),
				getString(R.string.voice_stream_voice_call)},
				new Integer[] {AudioManager.STREAM_MUSIC, AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_VOICE_CALL});
		
		entries = new String[DayNightMode.values().length];
		for(int i=0; i<entries.length; i++){
			entries[i] = DayNightMode.values()[i].toHumanString(this);
		}
		registerListPreference(osmandSettings.DAYNIGHT_MODE, screen, entries,DayNightMode.values());
		
		entries = new String[MetricsConstants.values().length];
		for(int i=0; i<entries.length; i++){
			entries[i] = MetricsConstants.values()[i].toHumanString(this);
		}
		registerListPreference(osmandSettings.METRIC_SYSTEM, screen, entries, MetricsConstants.values());
		
		//getResources().getAssets().getLocales();
		entrieValues = new String[] { "", "en", "cs", "de", "es", "jp", "fr", "hu", "it", "pl", "pt", "ru", "sk", "vi" };
		entries = new String[entrieValues.length];
		entries[0] = getString(R.string.system_locale);
		for (int i = 1; i < entries.length; i++) {
			entries[i] = entrieValues[i];
		}
		registerListPreference(osmandSettings.PREFERRED_LOCALE, screen, entries, entrieValues);
		
		int startZoom = 12;
		int endZoom = 19;
		entries = new String[endZoom - startZoom + 1];
		Integer[] intValues = new Integer[endZoom - startZoom + 1];
		for (int i = startZoom; i <= endZoom; i++) {
			entries[i - startZoom] = i + ""; //$NON-NLS-1$
			intValues[i - startZoom] = i ;
		}
		// try without, Issue 823:
		// registerListPreference(osmandSettings.MAX_LEVEL_TO_DOWNLOAD_TILE, screen, entries, intValues);
		
		
		intValues = new Integer[] { 0, 5, 10, 15, 20, 25, 30, 45, 60, 90};
		entries = new String[intValues.length];
		entries[0] = getString(R.string.auto_follow_route_never);
		for (int i = 1; i < intValues.length; i++) {
			entries[i] = (int) intValues[i] + " " + getString(R.string.int_seconds);
		}
		registerListPreference(osmandSettings.AUTO_FOLLOW_ROUTE, screen, entries, intValues);
		
		Float[] floatValues = new Float[] {0.6f, 0.8f, 1.0f, 1.2f, 1.5f};
		entries = new String[floatValues.length];
		for (int i = 0; i < floatValues.length; i++) {
			entries[i] = (int) (floatValues[i] * 100) +" %";
		}
		registerListPreference(osmandSettings.MAP_TEXT_SIZE, screen, entries, floatValues);
		
		startZoom = 1;
		endZoom = 18;
		entries = new String[endZoom - startZoom + 1];
		intValues = new Integer[endZoom - startZoom + 1];
		for (int i = startZoom; i <= endZoom; i++) {
			entries[i - startZoom] = i + ""; //$NON-NLS-1$
			intValues[i - startZoom] = i ;
		}
		registerListPreference(osmandSettings.LEVEL_TO_SWITCH_VECTOR_RASTER, screen, entries, intValues);
		
		entries = new String[RouteService.values().length];
		for(int i=0; i<entries.length; i++){
			entries[i] = RouteService.values()[i].getName();
		}
		registerListPreference(osmandSettings.ROUTER_SERVICE, screen, entries, RouteService.values());
		
		
		
		
		entries = new String[]{getString(R.string.gps_provider), getString(R.string.network_provider)};
		entrieValues = new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};
		registerListPreference(osmandSettings.SERVICE_OFF_PROVIDER, screen, entries, entrieValues);
		
		registerTimeListPreference(osmandSettings.SAVE_TRACK_INTERVAL, screen, new int[]{1, 2, 3, 5, 10, 15, 20, 30}, new int[]{1, 2, 3, 5}, 1);
		registerTimeListPreference(osmandSettings.SERVICE_OFF_INTERVAL, screen, 
				new int[]{0, 30, 45, 60}, new int[]{2, 3, 5, 10, 15, 30, 45, 60, 90}, 1000);
		registerTimeListPreference(osmandSettings.SERVICE_OFF_WAIT_INTERVAL, screen, 
				new int[]{15, 30, 45, 60, 90}, new int[]{2, 3, 5, 10}, 1000);
		
		
		entries = new String[ApplicationMode.values().length];
		for(int i=0; i<entries.length; i++){
			entries[i] = ApplicationMode.toHumanString(ApplicationMode.values()[i], this);
		}
		registerListPreference(osmandSettings.APPLICATION_MODE, screen, entries, ApplicationMode.values());
		
		Collection<String> rendererNames = getMyApplication().getRendererRegistry().getRendererNames();
		entries = (String[]) rendererNames.toArray(new String[rendererNames.size()]);
		registerListPreference(osmandSettings.RENDERER, screen, entries, entries);
		
		createCustomRenderingProperties(false);
		
		applicationModePreference = (ListPreference) screen.findPreference(osmandSettings.APPLICATION_MODE.getId());
		applicationModePreference.setOnPreferenceChangeListener(this);

		tileSourcePreference = (ListPreference) screen.findPreference(osmandSettings.MAP_TILE_SOURCES.getId());
		tileSourcePreference.setOnPreferenceChangeListener(this);
		overlayPreference = (ListPreference) screen.findPreference(osmandSettings.MAP_OVERLAY.getId());
		overlayPreference.setOnPreferenceChangeListener(this);
		underlayPreference = (ListPreference) screen.findPreference(osmandSettings.MAP_UNDERLAY.getId());
		underlayPreference.setOnPreferenceChangeListener(this);

		dayNightModePreference = (ListPreference) screen.findPreference(osmandSettings.DAYNIGHT_MODE.getId());
		dayNightModePreference.setOnPreferenceChangeListener(this);
		routerServicePreference = (ListPreference) screen.findPreference(osmandSettings.ROUTER_SERVICE.getId());
		routerServicePreference.setOnPreferenceChangeListener(this);

		Preference localIndexes =(Preference) screen.findPreference(OsmandSettings.LOCAL_INDEXES);
		localIndexes.setOnPreferenceClickListener(this);
		saveCurrentTrack =(Preference) screen.findPreference(OsmandSettings.SAVE_CURRENT_TRACK);
		saveCurrentTrack.setOnPreferenceClickListener(this);
		testVoiceCommands =(Preference) screen.findPreference("test_voice_commands");
		testVoiceCommands.setOnPreferenceClickListener(this);
		routeServiceEnabled =(CheckBoxPreference) screen.findPreference(OsmandSettings.SERVICE_OFF_ENABLED);
		routeServiceEnabled.setOnPreferenceChangeListener(this);
		applicationDir = (EditTextPreference) screen.findPreference(OsmandSettings.EXTERNAL_STORAGE_DIR);
		applicationDir.setOnPreferenceChangeListener(this);
		
		
		
		broadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				routeServiceEnabled.setChecked(false);
			}
			
		};
		registerReceiver(broadcastReceiver, new IntentFilter(NavigationService.OSMAND_STOP_SERVICE_ACTION));
		
		
		Intent intent = getIntent();
		if(intent != null && intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0) != 0){
			int s = intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0);
			String pref = null;
			if(s == SCREEN_GENERAL_SETTINGS){
				pref = "general_settings";
			} else if(s == SCREEN_NAVIGATION_SETTINGS){
				pref = "routing_settings";
			} else if(s == SCREEN_MONITORING_SETTINGS){
				pref = "monitor_settings";
			} 
			if(pref != null){
				Preference toOpen = screen.findPreference(pref);
				if(toOpen instanceof PreferenceScreen){
					setPreferenceScreen((PreferenceScreen) toOpen);
				}
			}
		}
    }

	private void createCustomRenderingProperties(boolean update) {
		RenderingRulesStorage renderer = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
		PreferenceCategory cat = (PreferenceCategory) findPreference("custom_vector_rendering");
		cat.removeAll();
		if(renderer != null){
			for(RenderingRuleProperty p : renderer.PROPS.getCustomRules()){
				CommonPreference<String> custom = getMyApplication().getSettings().getCustomRenderProperty(p.getAttrName());
				ListPreference lp = new ListPreference(this);
				lp.setOnPreferenceChangeListener(this);
				lp.setKey(custom.getId());
				lp.setTitle(p.getName());
				lp.setSummary(p.getDescription());
				cat.addPreference(lp);
				
				LinkedHashMap<String, Object> vals = new LinkedHashMap<String, Object>();
				screenPreferences.put(custom.getId(), lp);
				listPreferences.put(custom.getId(), custom);
				listPrefValues.put(custom.getId(), vals);
				String[] names = p.getPossibleValues();
				for(int i=0; i<names.length; i++){
					vals.put(names[i], names[i]);
				}
				
			}
			if(update) {
				updateAllSettings();
			}
		}
		
	}

	private void reloadVoiceListPreference(PreferenceScreen screen) {
		String[] entries;
		String[] entrieValues;
		Set<String> voiceFiles = getVoiceFiles();
		entries = new String[voiceFiles.size() + 2];
		entrieValues = new String[voiceFiles.size() + 2];
		int k = 0;
//		entries[k++] = getString(R.string.voice_not_specified);
		entrieValues[k] = OsmandSettings.VOICE_PROVIDER_NOT_USE;
		entries[k++] = getString(R.string.voice_not_use);
		for (String s : voiceFiles) {
			entries[k] = s;
			entrieValues[k] = s;
			k++;
		}
		entrieValues[k] = MORE_VALUE;
		entries[k] = getString(R.string.install_more);
		registerListPreference(osmandSettings.VOICE_PROVIDER, screen, entries, entrieValues);
	}

	private void updateApplicationDirTextAndSummary() {
		String storageDir = osmandSettings.getExternalStorageDirectory().getAbsolutePath();
		applicationDir.setText(storageDir);
		applicationDir.setSummary(storageDir);
	}
    
    @Override
    protected void onResume() {
		super.onResume();
		updateAllSettings();
	}
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	unregisterReceiver(broadcastReceiver);
    }
    
    public void updateAllSettings(){
    	for(OsmandPreference<Boolean> b : booleanPreferences.values()){
    		CheckBoxPreference pref = (CheckBoxPreference) screenPreferences.get(b.getId());
    		pref.setChecked(b.get());
    	}
    	
    	for(OsmandPreference<Integer> b : seekBarPreferences.values()){
    		SeekBarPreference pref = (SeekBarPreference) screenPreferences.get(b.getId());
    		pref.setValue(b.get());
    	}
    	
    	reloadVoiceListPreference(getPreferenceScreen());
    	
    	for(OsmandPreference<?> p : listPreferences.values()){
    		ListPreference listPref = (ListPreference) screenPreferences.get(p.getId());
    		Map<String, ?> prefValues = listPrefValues.get(p.getId());
    		String[] entryValues = new String[prefValues.size()];
    		String[] entries = new String[prefValues.size()];
    		int i = 0;
    		for(Entry<String, ?> e : prefValues.entrySet()){
    			entries[i] = e.getKey();
				entryValues[i] = e.getValue() + ""; // case of null
    			i++;
    		}
    		listPref.setEntries(entries);
    		listPref.setEntryValues(entryValues);
			listPref.setValue(p.get() + "");
    	}
    	
    	for(OsmandPreference<String> s : editTextPreferences.values()){
    		EditTextPreference pref = (EditTextPreference) screenPreferences.get(s.getId());
    		pref.setText(s.get());
    	}
    	
    	// Specific properties
		routeServiceEnabled.setChecked(getMyApplication().getNavigationService() != null);
		
		updateTileSourceSummary();
		
		updateApplicationDirTextAndSummary();

		applicationModePreference.setTitle(getString(R.string.settings_preset) + "  [" + ApplicationMode.toHumanString(osmandSettings.APPLICATION_MODE.get(), this) + "]");
		dayNightModePreference.setSummary(getString(R.string.daynight_descr) + "  [" + osmandSettings.DAYNIGHT_MODE.get() + "]");
		routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  [" + osmandSettings.ROUTER_SERVICE.get() + "]");
    }

	private void updateTileSourceSummary() {
		fillTileSourcesToPreference(tileSourcePreference, osmandSettings.MAP_TILE_SOURCES.get(), false);
		fillTileSourcesToPreference(overlayPreference, osmandSettings.MAP_OVERLAY.get(), true);
		fillTileSourcesToPreference(underlayPreference, osmandSettings.MAP_UNDERLAY.get(), true);

//		String mapName = " " + osmandSettings.MAP_TILE_SOURCES.get(); //$NON-NLS-1$
//		String summary = tileSourcePreference.getSummary().toString();
//		if (summary.lastIndexOf(':') != -1) {
//			summary = summary.substring(0, summary.lastIndexOf(':') + 1);
//		}
//		tileSourcePreference.setSummary(summary + mapName);
		tileSourcePreference.setSummary(getString(R.string.map_tile_source_descr) + "  [" + osmandSettings.MAP_TILE_SOURCES.get() + "]");
		overlayPreference.setSummary(getString(R.string.map_overlay_descr) + "  [" + osmandSettings.MAP_OVERLAY.get() + "]");
		underlayPreference.setSummary(getString(R.string.map_underlay_descr) + "  [" + osmandSettings.MAP_UNDERLAY.get() + "]");
	}

	private void fillTileSourcesToPreference(ListPreference tileSourcePreference, String value, boolean addNone) {
		Map<String, String> entriesMap = osmandSettings.getTileSourceEntries();
		int add = addNone ? 1 : 0;
		String[] entries = new String[entriesMap.size() + 1 + add];
		String[] values = new String[entriesMap.size() + 1 + add];
		int ki = 0;
		if(addNone){
			entries[ki] = getString(R.string.default_none);
			values[ki] = "";
			ki++;
		}
		if (value == null) {
			value = "";
		}
		
		for(Map.Entry<String, String> es : entriesMap.entrySet()){
			entries[ki] = es.getValue();
			values[ki] = es.getKey();
			ki++;
		}
		entries[ki] = getString(R.string.install_more);
		values[ki] = MORE_VALUE;
		fill(tileSourcePreference, entries, values, value);
	}

  
	private void fill(ListPreference component, String[] list, String[] values, String selected) {
		component.setEntries(list);
		component.setEntryValues(values);
		component.setValue(selected);
	}
    
    
	@SuppressWarnings("unchecked")
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// handle boolean prefences
		OsmandPreference<Boolean> boolPref = booleanPreferences.get(preference.getKey());
		OsmandPreference<Integer> seekPref = seekBarPreferences.get(preference.getKey());
		OsmandPreference<Object> listPref = (OsmandPreference<Object>) listPreferences.get(preference.getKey());
		OsmandPreference<String> editPref = editTextPreferences.get(preference.getKey());
		if(boolPref != null){
			boolPref.set((Boolean)newValue);
			if (boolPref.getId().equals(osmandSettings.MAP_VECTOR_DATA.getId())) {
				MapRenderRepositories r = ((OsmandApplication)getApplication()).getResourceManager().getRenderer();
				if(r.isEmpty()){
					Toast.makeText(this, getString(R.string.no_vector_map_loaded), Toast.LENGTH_LONG).show();
					return false;
				}
			}
			if (boolPref.getId().equals(osmandSettings.NATIVE_RENDERING.getId())) {
				if(((Boolean)newValue).booleanValue()) {
					loadNativeLibrary();
				}
			}
		} else if (seekPref != null) {
			seekPref.set((Integer) newValue);
		} else if (editPref != null) {
			editPref.set((String) newValue);
		} else if (listPref != null) {
			int ind = ((ListPreference) preference).findIndexOfValue((String) newValue);
			CharSequence entry = ((ListPreference) preference).getEntries()[ind];
			Map<String, ?> map = listPrefValues.get(preference.getKey());
			Object obj = map.get(entry);
			final Object oldValue = listPref.get();
			boolean changed = listPref.set(obj);
			
			// Specific actions after list preference changed
			if (changed) {
				if (listPref.getId().equals(osmandSettings.VOICE_PROVIDER.getId())) {
					if (MORE_VALUE.equals(newValue)) {
						listPref.set(oldValue); //revert the change..
						final Intent intent = new Intent(this, DownloadIndexActivity.class);
						intent.putExtra(DownloadIndexActivity.FILTER_KEY, "voice");
						startActivity(intent);
					} else {
						getMyApplication().showDialogInitializingCommandPlayer(this, false);
					}
				} else if (listPref.getId().equals(osmandSettings.APPLICATION_MODE.getId())) {
					updateAllSettings();
				} else if (listPref.getId().equals(osmandSettings.PREFERRED_LOCALE.getId())) {
					// restart application to update locale
					getMyApplication().checkPrefferedLocale();
					Intent intent = getIntent();
					finish();
					startActivity(intent);
				} else if (listPref.getId().equals(osmandSettings.DAYNIGHT_MODE.getId())) {
					dayNightModePreference.setSummary(getString(R.string.daynight_descr) + "  [" + osmandSettings.DAYNIGHT_MODE.get() + "]");
				} else if (listPref.getId().equals(osmandSettings.ROUTER_SERVICE.getId())) {
					routerServicePreference.setSummary(getString(R.string.router_service_descr) + "  [" + osmandSettings.ROUTER_SERVICE.get() + "]");
				}
			}
			if (listPref.getId().equals(osmandSettings.RENDERER.getId())) {
				if(changed){
					Toast.makeText(this, R.string.renderer_load_sucess, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
				}
				createCustomRenderingProperties(true);
			}
		} else if(preference == applicationDir){
			warnAboutChangingStorage((String) newValue);
			return false;
		} else if (preference == routeServiceEnabled) {
			Intent serviceIntent = new Intent(this, NavigationService.class);
			if ((Boolean) newValue) {
				ComponentName name = startService(serviceIntent);
				if (name == null) {
					routeServiceEnabled.setChecked(getMyApplication().getNavigationService() != null);
				}
			} else {
				if(!stopService(serviceIntent)){
					routeServiceEnabled.setChecked(getMyApplication().getNavigationService() != null);
				}
			}
		} else if (preference == tileSourcePreference  || preference == overlayPreference 
				|| preference == underlayPreference) {
			if(MORE_VALUE.equals(newValue)){
				SettingsActivity.installMapLayers(this, new ResultMatcher<TileSourceTemplate>() {
					@Override
					public boolean isCancelled() { return false;}

					@Override
					public boolean publish(TileSourceTemplate object) {
						if(object == null){
							updateTileSourceSummary();
						}
						return true;
					}
				});
			} else if(preference == tileSourcePreference){
				osmandSettings.MAP_TILE_SOURCES.set((String) newValue);
				updateTileSourceSummary();
			} else {
				if(((String) newValue).length() == 0){
					newValue = null;
				}
				if(preference == underlayPreference){
					osmandSettings.MAP_UNDERLAY.set(((String) newValue));
					underlayPreference.setSummary(getString(R.string.map_underlay_descr) + "  [" + osmandSettings.MAP_UNDERLAY.get() + "]");
				} else if(preference == overlayPreference){
					osmandSettings.MAP_OVERLAY.set(((String) newValue));
					overlayPreference.setSummary(getString(R.string.map_overlay_descr) + "  [" + osmandSettings.MAP_OVERLAY.get() + "]");
				}
			}
		}
		return true;
	}
	
	

	private void warnAboutChangingStorage(final String newValue) {
		final String newDir = newValue != null ? newValue.trim(): newValue;
		File path = new File(newDir);
		path.mkdirs();
		if(!path.canRead() || !path.exists()){
			Toast.makeText(this, R.string.specified_dir_doesnt_exist, Toast.LENGTH_LONG).show()	;
			return;
		}
		
		Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.application_dir_change_warning));
		builder.setPositiveButton(R.string.default_buttons_yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//edit the preference
				osmandSettings.setExternalStorageDirectory(newDir);
				getMyApplication().getResourceManager().resetStoreDirectory();
				reloadIndexes();
				updateApplicationDirTextAndSummary();
			}
		});
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.show();
	}

	public void reloadIndexes(){
		reloadVoiceListPreference(getPreferenceScreen());
		progressDlg = ProgressDialog.show(this, getString(R.string.loading_data), getString(R.string.reading_indexes), true);
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(progressDlg);
		impl.setRunnable("Initializing app", new Runnable(){ //$NON-NLS-1$
			@Override
			public void run() {
				try {
					showWarnings(getMyApplication().getResourceManager().reloadIndexes(impl));
				} finally {
					if(progressDlg !=null){
						progressDlg.dismiss();
						progressDlg = null;
					}
				}
			}
		});
		impl.run();
	}
	
	public void loadNativeLibrary(){
		if (!NativeOsmandLibrary.isLoaded()) {
			final RenderingRulesStorage storage = getMyApplication().getRendererRegistry().getCurrentSelectedRenderer();
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected void onPreExecute() {
					progressDlg = ProgressDialog.show(SettingsActivity.this, getString(R.string.loading_data),
							getString(R.string.init_native_library), true);
				};

				@Override
				protected Void doInBackground(Void... params) {
					NativeOsmandLibrary.getLibrary(storage);
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					progressDlg.dismiss();
					if (!NativeOsmandLibrary.isNativeSupported(storage)) {
						Toast.makeText(SettingsActivity.this, R.string.native_library_not_supported, Toast.LENGTH_LONG).show();
					}
				};
			}.execute();
		}
	}
	
	private OsmandApplication getMyApplication() {
		return (OsmandApplication)getApplication();
	}
	
	@Override
	protected void onStop() {
		if(progressDlg !=null){
			progressDlg.dismiss();
			progressDlg = null;
		}
		super.onStop();
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
		
	
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference == applicationDir) {
			return true;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if(preference.getKey().equals(OsmandSettings.LOCAL_INDEXES)){
			startActivity(new Intent(this, LocalIndexesActivity.class));
			return true;
		} else if(preference == testVoiceCommands){
			startActivity(new Intent(this, TestVoiceActivity.class));
			return true;
		} else if(preference == saveCurrentTrack){
			SavingTrackHelper helper = new SavingTrackHelper(this);
			if (helper.hasDataToSave()) {
				saveCurrentTracks();
			} else {
				helper.close();
			}
			return true;
		}
		return false;
	}

	private void saveCurrentTracks() {
		progressDlg = ProgressDialog.show(this, getString(R.string.saving_gpx_tracks), getString(R.string.saving_gpx_tracks), true);
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(progressDlg);
		impl.setRunnable("SavingGPX", new Runnable() { //$NON-NLS-1$
					@Override
					public void run() {
						try {
							SavingTrackHelper helper = new SavingTrackHelper(SettingsActivity.this);
							helper.saveDataToGpx();
							helper.close();
						} finally {
							if (progressDlg != null) {
								progressDlg.dismiss();
								progressDlg = null;
							}
						}
					}
				});
		impl.run();
	}
	
	public static void installMapLayers(final Activity activity, final ResultMatcher<TileSourceTemplate> result){
		final OsmandSettings settings = ((OsmandApplication) activity.getApplication()).getSettings();
		final Map<String, String> entriesMap = settings.getTileSourceEntries();
		if(!settings.isInternetConnectionAvailable(true)){
			Toast.makeText(activity, R.string.internet_not_available, Toast.LENGTH_LONG).show();
			return;
		}
		final List<TileSourceTemplate> downloaded = TileSourceManager.downloadTileSourceTemplates();
		if(downloaded == null || downloaded.isEmpty()){
			Toast.makeText(activity, R.string.error_io_error, Toast.LENGTH_SHORT).show();
			return;
		}
		Builder builder = new AlertDialog.Builder(activity);
		String[] names = new String[downloaded.size()];
		for(int i=0; i<names.length; i++){
			names[i] = downloaded.get(i).getName();
		}
		final boolean[] selected = new boolean[downloaded.size()];
		builder.setMultiChoiceItems(names, selected, new DialogInterface.OnMultiChoiceClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				selected[which] = isChecked;
				if(entriesMap.containsKey(downloaded.get(which).getName()) && isChecked){
					Toast.makeText(activity, R.string.tile_source_already_installed, Toast.LENGTH_SHORT).show();
				}
			}
		});
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setTitle(R.string.select_tile_source_to_install);
		builder.setPositiveButton(R.string.default_buttons_apply, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				List<TileSourceTemplate> toInstall = new ArrayList<TileSourceTemplate>();
				for(int i=0; i<selected.length; i++){
					if(selected[i]){
						toInstall.add(downloaded.get(i));
					}
				}
				for(TileSourceTemplate ts : toInstall){
					if(settings.installTileSource(ts)){
						if(result != null){
							result.publish(ts);
						}
					}
				}
				// at the end publish null to show end of process
				if (!toInstall.isEmpty() && result != null) {
					result.publish(null);
				}
			}
		});
		
		builder.show();
	}
}
