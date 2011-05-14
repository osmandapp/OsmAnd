package net.osmand.plus.activities;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.PoiFiltersHelper;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.SQLiteTileSource;
import net.osmand.plus.OsmandSettings.DayNightMode;
import net.osmand.plus.OsmandSettings.MetricsConstants;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.activities.RouteProvider.RouteService;
import net.osmand.plus.render.BaseOsmandRender;
import net.osmand.plus.render.RendererRegistry;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.location.LocationManager;
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

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener, OnPreferenceClickListener {
	private final static String VECTOR_MAP = "#VECTOR_MAP"; //$NON-NLS-1$
	
	private EditTextPreference userPassword;
	private EditTextPreference userName;
	private EditTextPreference applicationDir;
	
	private Preference saveCurrentTrack;
	private Preference reloadIndexes;
	private Preference downloadIndexes;

	private ListPreference applicationMode;
	private ListPreference saveTrackInterval;
	private ListPreference tileSourcePreference;
	private ListPreference rendererPreference;
	private ListPreference routeServiceInterval;
	private ListPreference routeServiceWaitInterval;
	
	private CheckBoxPreference routeServiceEnabled;
	
	private ProgressDialog progressDlg;
	
	private BroadcastReceiver broadcastReceiver;
	private OsmandSettings osmandSettings;
	
	private Map<String, Preference> screenPreferences = new LinkedHashMap<String, Preference>();
	private Map<String, OsmandPreference<Boolean>> booleanPreferences = new LinkedHashMap<String, OsmandPreference<Boolean>>();
	private Map<String, OsmandPreference<?>> listPreferences = new LinkedHashMap<String, OsmandPreference<?>>();
	private Map<String, Map<String, ?>> listPrefValues = new LinkedHashMap<String, Map<String, ?>>();
	
	
	
	private void registerBooleanPreference(OsmandPreference<Boolean> b, PreferenceScreen screen){
		CheckBoxPreference p = (CheckBoxPreference) screen.findPreference(b.getId());
		p.setOnPreferenceChangeListener(this);
		screenPreferences.put(b.getId(), p);
		booleanPreferences.put(b.getId(), b);
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
	    registerBooleanPreference(osmandSettings.SAVE_TRACK_TO_GPX,screen); 
	    registerBooleanPreference(osmandSettings.DEBUG_RENDERING_INFO,screen); 
	    registerBooleanPreference(osmandSettings.USE_STEP_BY_STEP_RENDERING,screen); 
	    registerBooleanPreference(osmandSettings.FAST_ROUTE_MODE,screen);
	    registerBooleanPreference(osmandSettings.USE_OSMAND_ROUTING_SERVICE_ALWAYS,screen); 
	    registerBooleanPreference(osmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES,screen);
	    
		
		reloadIndexes =(Preference) screen.findPreference(OsmandSettings.RELOAD_INDEXES);
		reloadIndexes.setOnPreferenceClickListener(this);
		downloadIndexes =(Preference) screen.findPreference(OsmandSettings.DOWNLOAD_INDEXES);
		downloadIndexes.setOnPreferenceClickListener(this);
		saveCurrentTrack =(Preference) screen.findPreference(OsmandSettings.SAVE_CURRENT_TRACK);
		saveCurrentTrack.setOnPreferenceClickListener(this);
		routeServiceEnabled =(CheckBoxPreference) screen.findPreference(OsmandSettings.SERVICE_OFF_ENABLED);
		routeServiceEnabled.setOnPreferenceChangeListener(this);
		
		userName = (EditTextPreference) screen.findPreference(osmandSettings.USER_NAME.getId());
		userName.setOnPreferenceChangeListener(this);
		userPassword = (EditTextPreference) screen.findPreference(osmandSettings.USER_PASSWORD.getId());
		userPassword.setOnPreferenceChangeListener(this);
		applicationDir = (EditTextPreference) screen.findPreference(OsmandSettings.EXTERNAL_STORAGE_DIR);
		applicationDir.setOnPreferenceChangeListener(this);
		
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
		entries = new String[] { "", "en", "cs", "de", "es", "fr", "hu", "it", "pt", "ru", "sk" };
		entrieValues = new String[entries.length];
		entrieValues[0] = getString(R.string.system_locale);
		for (int i=1; i< entries.length; i++) {
			entrieValues[i] = entries[i];
		}
		registerListPreference(osmandSettings.PREFERRED_LOCALE, screen, entries, entries);
		
		Set<String> voiceFiles = getVoiceFiles();
		entries = new String[voiceFiles.size() + 1];
		entrieValues = new String[voiceFiles.size() + 1];
		int k = 0;
		entries[k++] = getString(R.string.voice_not_use);
		for (String s : voiceFiles) {
			entries[k] = s;
			entrieValues[k] = s;
			k++;
		}
		registerListPreference(osmandSettings.VOICE_PROVIDER, screen, entries, entries);
		
		int startZoom = 12;
		int endZoom = 19;
		entries = new String[endZoom - startZoom + 1];
		Integer[] intValues = new Integer[endZoom - startZoom + 1];
		for (int i = startZoom; i <= endZoom; i++) {
			entries[i - startZoom] = i + ""; //$NON-NLS-1$
			intValues[i - startZoom] = i ;
		}
		registerListPreference(osmandSettings.MAX_LEVEL_TO_DOWNLOAD_TILE, screen, entries, intValues);
		
		entries = new String[RouteService.values().length];
		for(int i=0; i<entries.length; i++){
			entries[i] = RouteService.values()[i].getName();
		}
		registerListPreference(osmandSettings.ROUTER_SERVICE, screen, entries, RouteService.values());
		
		
		entries = new String[]{getString(R.string.gps_provider), getString(R.string.network_provider)};
		entrieValues = new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};
		registerListPreference(osmandSettings.SERVICE_OFF_PROVIDER, screen, entries, entries);
		
		saveTrackInterval =(ListPreference) screen.findPreference(osmandSettings.SAVE_TRACK_INTERVAL.getId());
		saveTrackInterval.setOnPreferenceChangeListener(this);
		routeServiceInterval =(ListPreference) screen.findPreference(osmandSettings.SERVICE_OFF_INTERVAL.getId());
		routeServiceInterval.setOnPreferenceChangeListener(this);
		routeServiceWaitInterval =(ListPreference) screen.findPreference(osmandSettings.SERVICE_OFF_WAIT_INTERVAL.getId());
		routeServiceWaitInterval.setOnPreferenceChangeListener(this);
		
		
		applicationMode =(ListPreference) screen.findPreference(OsmandSettings.APPLICATION_MODE);
		applicationMode.setOnPreferenceChangeListener(this);
		tileSourcePreference = (ListPreference) screen.findPreference(OsmandSettings.MAP_TILE_SOURCES);
		tileSourcePreference.setOnPreferenceChangeListener(this);
		
		
		
		
		rendererPreference =(ListPreference) screen.findPreference(osmandSettings.RENDERER.getId());
		rendererPreference.setOnPreferenceChangeListener(this);
		
		
		
		broadcastReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				routeServiceEnabled.setChecked(false);
			}
			
		};
		registerReceiver(broadcastReceiver, new IntentFilter(NavigationService.OSMAND_STOP_SERVICE_ACTION));
    }

	private void updateApplicationDirSummary() {
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
    
    
    private void fillTime(ListPreference component, int[] seconds, int[] minutes, int currentSeconds){
    	int minutesLength = minutes == null? 0 : minutes.length;
    	int secondsLength = seconds == null? 0 : seconds.length;
    	
    	
    	String[] ints = new String[secondsLength + minutesLength];
		String[] intDescriptions = new String[ints.length];
		for (int i = 0; i < secondsLength; i++) {
			ints[i] = seconds[i] + "";
			intDescriptions[i] = ints[i] + " " + getString(R.string.int_seconds); //$NON-NLS-1$
		}
		for (int i = 0; i < minutesLength; i++) {
			ints[secondsLength + i] = (minutes[i] * 60) + "";
			intDescriptions[secondsLength + i] = minutes[i] + " " + getString(R.string.int_min); //$NON-NLS-1$
		}
		fill(component, intDescriptions, ints, currentSeconds+"");
    }
    
    public void updateAllSettings(){
    	for(OsmandPreference<Boolean> b : booleanPreferences.values()){
    		CheckBoxPreference pref = (CheckBoxPreference) screenPreferences.get(b.getId());
    		pref.setChecked(b.get());
    	}
    	
    	
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
    	
    	
		userName.setText(OsmandSettings.getUserName(prefs));
		userPassword.setText(OsmandSettings.getUserPassword(prefs));
		applicationDir.setText(OsmandSettings.getExternalStorageDirectory(prefs).getAbsolutePath());
		
		Resources resources = this.getResources();
		
		fillTime(saveTrackInterval, new int[]{1, 2, 3, 5, 15, 20, 30}, new int[]{1, 2, 3, 5}, OsmandSettings.getSavingTrackInterval(prefs)); //$NON-NLS-1$
		
		fillTime(routeServiceInterval, new int[]{0, 30, 45, 60}, new int[]{2, 3, 5, 8, 10, 15, 20, 30, 40, 50, 70, 90}, OsmandSettings.getServiceOffInterval(prefs)/1000); //$NON-NLS-1$
		
		fillTime(routeServiceWaitInterval, new int[]{15, 30, 45, 60, 90}, new int[]{2, 3, 5, 10}, OsmandSettings.getServiceOffWaitInterval(prefs)/1000);
		
		
		
		routeServiceEnabled.setChecked(getMyApplication().getNavigationService() != null);

		
		
		ApplicationMode[] presets = ApplicationMode.values(); 
		String[] names = new String[presets.length];
		String[] values = new String[presets.length];
		for(int i=0; i<presets.length; i++){
			names[i] = ApplicationMode.toHumanString(presets[i], this);
			values[i] = presets[i].name();
		}
		fill(applicationMode, names, values, OsmandSettings.getApplicationMode(prefs).name());

		
		
		
		String vectorRenderer = OsmandSettings.getVectorRenderer(prefs);
		Collection<String> rendererNames = RendererRegistry.getRegistry().getRendererNames();
		entries = (String[]) rendererNames.toArray(new String[rendererNames.size()]);
		rendererPreference.setEntries(entries);
		rendererPreference.setEntryValues(entries);
		if(rendererNames.contains(vectorRenderer)){
			rendererPreference.setValue(vectorRenderer);
		} else {
			rendererPreference.setValueIndex(0);
		}
		
		

		Map<String, String> entriesMap = getTileSourceEntries(this);
		entries = new String[entriesMap.size() + 1];
		values = new String[entriesMap.size() + 1];
		values[0] = VECTOR_MAP;
		entries[0] = getString(R.string.vector_data);
		int ki = 1;
		for(Map.Entry<String, String> es : entriesMap.entrySet()){
			entries[ki] = es.getValue();
			values[ki] = es.getKey();
			ki++;
		}
		String value = OsmandSettings.isUsingMapVectorData(prefs)? VECTOR_MAP : OsmandSettings.getMapTileSourceName(prefs);
		fill(tileSourcePreference, entries, values, value);

		String mapName = " " + (OsmandSettings.isUsingMapVectorData(prefs) ? getString(R.string.vector_data) : //$NON-NLS-1$
				OsmandSettings.getMapTileSourceName(prefs));
		String summary = tileSourcePreference.getSummary().toString();
		if (summary.lastIndexOf(':') != -1) {
			summary = summary.substring(0, summary.lastIndexOf(':') + 1);
		}
		tileSourcePreference.setSummary(summary + mapName);
		
		updateApplicationDirSummary();
    }
    
	private void fill(ListPreference component, String[] list, String[] values, String selected) {
		component.setEntries(list);
		component.setEntryValues(values);
		component.setValue(selected);
	}
    
    public static Map<String, String> getTileSourceEntries(Context ctx){
		Map<String, String> map = new LinkedHashMap<String, String>();
		File dir = OsmandSettings.extendOsmandPath(ctx, ResourceManager.TILES_PATH);
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			Arrays.sort(files, new Comparator<File>(){
				@Override
				public int compare(File object1, File object2) {
					if(object1.lastModified() > object2.lastModified()){
						return -1;
					} else if(object1.lastModified() == object2.lastModified()){
						return 0;
					}
					return 1;
				}
				
			});
			if (files != null) {
				for (File f : files) {
					if (f.getName().endsWith(SQLiteTileSource.EXT)) {
						String n = f.getName();
						map.put(f.getName(), n.substring(0, n.lastIndexOf('.')));
					} else if (f.isDirectory() && !f.getName().equals(ResourceManager.TEMP_SOURCE_TO_LOAD)) {
						map.put(f.getName(), f.getName());
					}
				}
			}
		}
		for(TileSourceTemplate l : TileSourceManager.getKnownSourceTemplates()){
			map.put(l.getName(), l.getName());
		}
		return map;
		
    }
    
    private void editBoolean(String id, boolean value){
    	OsmandSettings.getWriteableEditor(this).putBoolean(id, value).commit();
    }
    
    private void editString(String id, String value){
    	OsmandSettings.getWriteableEditor(this).putString(id, value).commit();
    }
    
    private void editInt(String id, int value){
    	OsmandSettings.getWriteableEditor(this).putInt(id, value).commit();
    }

	@SuppressWarnings("unchecked")
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// handle boolean prefences
		OsmandPreference<Boolean> boolPref = booleanPreferences.get(preference.getKey());
		OsmandPreference<Object> listPref = (OsmandPreference<Object>) listPreferences.get(preference.getKey());
		if(boolPref != null){
			boolPref.set((Boolean)newValue);
		} else if (listPref != null) {
			CharSequence entry = ((ListPreference) preference).getEntry();
			Map<String, ?> map = listPrefValues.get(preference.getKey());
			Object obj = map.get(entry);
			listPref.set(obj);
			
			if(listPref.getId().equals(osmandSettings.DAYNIGHT_MODE.getId())){
				getMyApplication().getDaynightHelper().setDayNightMode(osmandSettings.DAYNIGHT_MODE.get());
			} else if(listPref.getId().equals(osmandSettings.VOICE_PROVIDER.getId())){
				getMyApplication().initCommandPlayer();
			} else if(listPref.getId().equals(osmandSettings.PREFERRED_LOCALE.getId())){
				// restart activity
				getMyApplication().checkPrefferedLocale();
				Intent intent = getIntent();
				finish();
				startActivity(intent);
			}
		} else if(preference == applicationMode){
			boolean changed = ApplicationMode.setAppMode(ApplicationMode.valueOf(newValue.toString()), getMyApplication());
			if(changed){
				updateAllSettings();
			}
		} else if(preference == saveTrackInterval){
			editInt(OsmandSettings.SAVE_TRACK_INTERVAL, Integer.parseInt(newValue.toString()));
		} else if (preference == routeServiceInterval) {
			editInt(OsmandSettings.SERVICE_OFF_INTERVAL, Integer.parseInt((String) newValue) * 1000);
		} else if (preference == routeServiceWaitInterval) {
			editInt(OsmandSettings.SERVICE_OFF_WAIT_INTERVAL, Integer.parseInt((String) newValue) * 1000);
		} else if(preference == userPassword){
			editString(OsmandSettings.USER_PASSWORD, (String) newValue);
		} else if(preference == userName){
			editString(OsmandSettings.USER_NAME, (String) newValue);
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
		} else if (preference == rendererPreference) {
			BaseOsmandRender loaded = RendererRegistry.getRegistry().getRenderer((String) newValue);
			if(loaded == null){
				Toast.makeText(this, R.string.renderer_load_exception, Toast.LENGTH_SHORT).show();
			} else {
				RendererRegistry.getRegistry().setCurrentSelectedRender(loaded);
				editString(OsmandSettings.RENDERER, (String) newValue);
				Toast.makeText(this, R.string.renderer_load_sucess, Toast.LENGTH_SHORT).show();
				getMyApplication().getResourceManager().getRenderer().clearCache();
			}
		} else if (preference == tileSourcePreference) {
			if(VECTOR_MAP.equals((String) newValue)){
				editBoolean(OsmandSettings.MAP_VECTOR_DATA, true);
			} else {
				editString(OsmandSettings.MAP_TILE_SOURCES, (String) newValue);
				editBoolean(OsmandSettings.MAP_VECTOR_DATA, false);
			}
			String summary = tileSourcePreference.getSummary().toString();
			if (summary.lastIndexOf(':') != -1) {
				summary = summary.substring(0, summary.lastIndexOf(':') + 1);
			}
			SharedPreferences prefs = OsmandSettings.getPrefs(this);
			summary += " " + (OsmandSettings.isUsingMapVectorData(prefs) ? getString(R.string.vector_data) : //$NON-NLS-1$
				OsmandSettings.getMapTileSourceName(prefs));
			tileSourcePreference.setSummary(summary);
			
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
				editString(OsmandSettings.EXTERNAL_STORAGE_DIR, newDir);
				getMyApplication().getResourceManager().resetStoreDirectory();
				reloadIndexes();
				updateApplicationDirSummary();
			}
		});
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.show();
	}

	public void reloadIndexes(){
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
		if(preference == downloadIndexes){
			startActivity(new Intent(this, DownloadIndexActivity.class));
			return true;
		} else if(preference == reloadIndexes){
			reloadIndexes();
			return true;
		} else if(preference == saveCurrentTrack){
			SavingTrackHelper helper = new SavingTrackHelper(this);
			if (helper.hasDataToSave()) {
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
			} else {
				helper.close();
			}
			return true;
		}
		return false;
	}
}
