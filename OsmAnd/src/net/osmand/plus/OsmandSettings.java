package net.osmand.plus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.osmand.LogUtil;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.osm.LatLon;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.OsmandApplication;
import net.osmand.plus.activities.RouteProvider.RouteService;
import net.osmand.plus.activities.search.SearchHistoryHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;

public class OsmandSettings {
	// GLOBAL instance - make instance global for application
	// if some problems appear it can be unique for Application (ApplicationContext)
	private static OsmandSettings INSTANCE;
	
	public static OsmandSettings getOsmandSettings(Context ctx) {
		if (INSTANCE == null) {
			synchronized (ctx.getApplicationContext()) {
				if (INSTANCE == null) {
					INSTANCE = new OsmandSettings(ctx.getApplicationContext());
				}
			}
		}
		return INSTANCE;
	}
	
	public interface OsmandPreference<T> {
		T get();
		
		boolean set(T obj);
		
		String getId();
	}
	
	// These settings are stored in SharedPreferences
	private static final String SHARED_PREFERENCES_NAME = "net.osmand.settings"; //$NON-NLS-1$
	
	/// Settings variables
	private Context ctx;
	private SharedPreferences globalPreferences;
	private SharedPreferences profilePreferences;
	private ApplicationMode currentMode;
	
	// cache variables
	private long lastTimeInternetConnectionChecked = 0;
	private boolean internetConnectionAvailable = true;
	
	//TODO make all layers profile preferenced????
	private OsmandSettings(Context ctx){
		this.ctx = ctx;
		globalPreferences = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		// start from default settings
		currentMode = ApplicationMode.DEFAULT;
		updateProfilePreferences();
	}
	
	private void updateProfilePreferences(){
		profilePreferences = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME + "." + currentMode.name().toLowerCase(), Context.MODE_WORLD_READABLE);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String APPLICATION_MODE = "application_mode"; //$NON-NLS-1$
	
	public ApplicationMode getApplicationMode() {
		return currentMode;
	}

	protected ApplicationMode readApplicationMode() {
		String s = globalPreferences.getString(APPLICATION_MODE, ApplicationMode.DEFAULT.name());
		try {
			return ApplicationMode.valueOf(s);
		} catch (IllegalArgumentException e) {
			return ApplicationMode.DEFAULT;
		}
	}

	public boolean setApplicationMode(ApplicationMode p, OsmandApplication app) {
		ApplicationMode oldMode = currentMode;
		boolean changed = globalPreferences.edit().putString(APPLICATION_MODE, p.name()).commit();
		if(changed){
			currentMode = p;
			updateProfilePreferences();
			switchApplicationMode(oldMode);
		}
		return changed;
	}
	
	protected void switchApplicationMode(ApplicationMode oldMode){
		// TODO
		// change some global settings
		// for car
		if(currentMode == ApplicationMode.CAR){
			SHOW_TRANSPORT_OVER_MAP.set(false);
			SHOW_OSM_BUGS.set(false);
		}
		// TODO clear preferences ???
		
		
//		ApplicationMode old = OsmandSettings.getApplicationMode(OsmandSettings.getPrefs(app));
//		if(preset == old){
//			return false;
//		}
//		Editor edit = OsmandSettings.getWriteableEditor(app);
//		edit.putString(OsmandSettings.APPLICATION_MODE, preset.toString());
//		if (preset == ApplicationMode.CAR) {
//			OsmandSettings.setUseInternetToDownloadTiles(true, edit);
//			// edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, _);
//			edit.putBoolean(OsmandSettings.SHOW_TRANSPORT_OVER_MAP, false);
//			edit.putInt(OsmandSettings.ROTATE_MAP, OsmandSettings.ROTATE_MAP_BEARING);
//			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, false);
//			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, true);
//			edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, false);
//			edit.putBoolean(OsmandSettings.USE_STEP_BY_STEP_RENDERING, true);
//			// edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
//			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, true);
//			edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, 5);
//			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.BOTTOM_CONSTANT);
//			// edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);
//
//		} else if (preset == ApplicationMode.BICYCLE) {
//			// edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, _);
//			// edit.putBoolean(OsmandSettings.USE_INTERNET_TO_CALCULATE_ROUTE, _);
//			// edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, true);
//			edit.putInt(OsmandSettings.ROTATE_MAP, OsmandSettings.ROTATE_MAP_BEARING);
//			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, true);
//			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, false);
//			// edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, _);
//			// edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
//			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, true);
//			edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, 30);
//			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.BOTTOM_CONSTANT);
//			// edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);
//
//		} else if (preset == ApplicationMode.PEDESTRIAN) {
//			// edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, _);
//			// edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, true);
//			edit.putInt(OsmandSettings.ROTATE_MAP, OsmandSettings.ROTATE_MAP_COMPASS);
//			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, true);
//			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, false);
//			edit.putBoolean(OsmandSettings.USE_STEP_BY_STEP_RENDERING, false);
//			// if(useInternetToDownloadTiles.isChecked()){
//			// edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, true);
//			// }
//			// edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
//			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, false);
//			// edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, _);
//			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.CENTER_CONSTANT);
//			// edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);
//
//		} else if (preset == ApplicationMode.DEFAULT) {
//			// edit.putBoolean(OsmandSettings.USE_INTERNET_TO_DOWNLOAD_TILES, _);
//			// edit.putBoolean(OsmandSettings.SHOW_POI_OVER_MAP, true);
//			edit.putInt(OsmandSettings.ROTATE_MAP, OsmandSettings.ROTATE_MAP_NONE);
//			edit.putBoolean(OsmandSettings.SHOW_VIEW_ANGLE, false);
//			edit.putBoolean(OsmandSettings.AUTO_ZOOM_MAP, false);
//			edit.putBoolean(OsmandSettings.USE_STEP_BY_STEP_RENDERING, true);
//			// edit.putBoolean(OsmandSettings.SHOW_OSM_BUGS, _);
//			// edit.putBoolean(OsmandSettings.USE_ENGLISH_NAMES, _);
//			edit.putBoolean(OsmandSettings.SAVE_TRACK_TO_GPX, false);
//			// edit.putInt(OsmandSettings.SAVE_TRACK_INTERVAL, _);
//			edit.putInt(OsmandSettings.POSITION_ON_MAP, OsmandSettings.CENTER_CONSTANT);
//			// edit.putString(OsmandSettings.MAP_TILE_SOURCES, _);
//
//		}
//
//		BaseOsmandRender current = RendererRegistry.getRegistry().getCurrentSelectedRenderer();
//		BaseOsmandRender defaultRender = RendererRegistry.getRegistry().defaultRender();
//		BaseOsmandRender newRenderer;
//		if (preset == ApplicationMode.CAR) {
//			newRenderer = RendererRegistry.getRegistry().carRender();
//		} else if (preset == ApplicationMode.BICYCLE) {
//			newRenderer = RendererRegistry.getRegistry().bicycleRender();
//		} else if (preset == ApplicationMode.PEDESTRIAN) {
//			newRenderer = RendererRegistry.getRegistry().pedestrianRender();
//		} else {
//			newRenderer = defaultRender;
//		}
//		if (newRenderer != current) {
//			RendererRegistry.getRegistry().setCurrentSelectedRender(newRenderer);
//			app.getResourceManager().getRenderer().clearCache();
//		}
//		return edit.commit();
	}
	

	// Check internet connection available every 15 seconds
	public boolean isInternetConnectionAvailable(){
		long delta = System.currentTimeMillis() - lastTimeInternetConnectionChecked;
		if(delta < 0 || delta > 15000){
			ConnectivityManager mgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo active = mgr.getActiveNetworkInfo();
			if(active == null){
				internetConnectionAvailable = false;
			} else {
				NetworkInfo.State state = active.getState();
				internetConnectionAvailable = state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING;
			}
		}
		return internetConnectionAvailable;
	}
	
	/////////////// PREFERENCES classes ////////////////
	
	private abstract class CommonPreference<T> implements OsmandPreference<T> {
		private final String id;
		private final boolean global;
		private T cachedValue;
		private boolean cache;
		
		public CommonPreference(String id, boolean global){
			this.id = id;
			this.global = global;
		}
		
		public CommonPreference(String id, boolean global, boolean cache){
			this.id = id;
			this.global = global;
			this.cache = cache; 
		}
		
		protected SharedPreferences getPreferences(){
			return global ? globalPreferences : profilePreferences;
		}
		
		protected abstract T getValue();
		
		protected abstract boolean setValue(T val);

		@Override
		public T get() {
			if(cache && cachedValue != null){
				return cachedValue;
			}
			cachedValue = getValue();
			return cachedValue;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public boolean set(T obj) {
			if(setValue(obj)){
				cachedValue = obj;
				return true;
			}
			return false;
		}
		
	}
	
	private class BooleanPreference extends CommonPreference<Boolean> {

		private final boolean defValue;

		private BooleanPreference(String id, boolean defaultValue, boolean global) {
			super(id, global);
			this.defValue = defaultValue;
		}
		
		private BooleanPreference(String id, boolean defaultValue, boolean global, boolean cache) {
			super(id, global, cache);
			this.defValue = defaultValue;
		}
		
		protected boolean getDefaultValue(){
			return defValue;
		}
		@Override
		protected Boolean getValue() {
			return getPreferences().getBoolean(getId(), getDefaultValue());
		}

		@Override
		protected boolean setValue(Boolean val) {
			return getPreferences().edit().putBoolean(getId(), val).commit();
		}

	}
	private class IntPreference extends CommonPreference<Integer> {

		private final int defValue;

		private IntPreference(String id, int defaultValue, boolean global) {
			super(id, global);
			this.defValue = defaultValue;
		}
		
		private IntPreference(String id, int defaultValue, boolean global, boolean cache) {
			super(id, global, cache);
			this.defValue = defaultValue;
		}
		
		protected int getDefValue() {
			return defValue;
		}

		@Override
		protected Integer getValue() {
			return getPreferences().getInt(getId(), getDefValue());
		}

		@Override
		protected boolean setValue(Integer val) {
			return getPreferences().edit().putInt(getId(), val).commit();
		}

	}
	
	private class StringPreference extends CommonPreference<String> {

		private final String defValue;

		private StringPreference(String id, String defaultValue, boolean global) {
			super(id, global);
			this.defValue = defaultValue;
		}

		@Override
		protected String getValue() {
			return getPreferences().getString(getId(), defValue);
		}

		@Override
		protected boolean setValue(String val) {
			return getPreferences().edit().putString(getId(), val).commit();
		}

	}
	
	private class EnumIntPreference<E extends Enum<E>> extends CommonPreference<E> {

		private final E defValue;
		private final E[] values;

		private EnumIntPreference(String id, E defaultValue, boolean global, boolean cache,
				E[] values) {
			super(id, global, cache);
			this.defValue = defaultValue;
			this.values = values;
		}
		
		private EnumIntPreference(String id, E defaultValue, boolean global, E[] values) {
			super(id, global);
			this.defValue = defaultValue;
			this.values = values;
		}


		@Override
		protected E getValue() {
			int i = getPreferences().getInt(getId(), -1);
			if(i < 0 || i >= values.length){
				return defValue;
			}
			return values[i];
		}
		
		@Override
		protected boolean setValue(E val) {
			return getPreferences().edit().putInt(getId(), val.ordinal()).commit();
		}

	}
	/////////////// PREFERENCES classes ////////////////
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_INTERNET_TO_DOWNLOAD_TILES =
		new BooleanPreference("use_internet_to_download_tiles", true, true, true);

	
	// this value string is synchronized with settings_pref.xml preference name
	// cache of metrics constants as they are used very often
	public final OsmandPreference<MetricsConstants> METRIC_SYSTEM = new EnumIntPreference<MetricsConstants>(
			"default_metric_system", MetricsConstants.KILOMETERS_AND_METERS, true, true, MetricsConstants.values());

	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_TRACKBALL_FOR_MOVEMENTS =
		new BooleanPreference("use_trackball_for_movements", true, true);
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_HIGH_RES_MAPS =
		new BooleanPreference("use_high_res_maps", false, false, true);
	

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_POI_OVER_MAP =
		new BooleanPreference("show_poi_over_map", false, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_TRANSPORT_OVER_MAP = 
		new BooleanPreference("show_transport_over_map", false, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> PREFERRED_LOCALE = 
		new StringPreference("preferred_locale", "", true);

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> USER_NAME = 
		new StringPreference("user_name", "NoName", true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> USER_OSM_BUG_NAME = 
		new StringPreference("user_osm_bug_name", "NoName/Osmand", true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> USER_PASSWORD = 
		new StringPreference("user_password", "", true);

	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<DayNightMode> DAYNIGHT_MODE = 
		new EnumIntPreference<DayNightMode>("daynight_mode", DayNightMode.AUTO, false, DayNightMode.values());
		
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<RouteService> ROUTER_SERVICE = 
		new EnumIntPreference<RouteService>("router_service", RouteService.OSMAND, false, RouteService.values());


	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_CURRENT_TRACK = "save_current_track"; //$NON-NLS-1$
	public static final String RELOAD_INDEXES = "reload_indexes"; //$NON-NLS-1$
	public static final String DOWNLOAD_INDEXES = "download_indexes"; //$NON-NLS-1$

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SAVE_TRACK_TO_GPX = new 
		BooleanPreference("save_track_to_gpx", false, false){
		protected boolean getDefaultValue() {
			boolean defaultValue = false;
			if (currentMode == ApplicationMode.CAR || currentMode == ApplicationMode.BICYCLE) {
				defaultValue = true;
			}
			return defaultValue;
		};
	};
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> FAST_ROUTE_MODE = new 
		BooleanPreference("fast_route_mode", true, false);

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> SAVE_TRACK_INTERVAL = new 
				IntPreference("save_track_interval", 5, false){
		
		protected int getDefValue() {
			int defValue = 5;
			if(currentMode == ApplicationMode.BICYCLE){
				defValue = 15;
			}
			return defValue;
		}
	};

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_OSMAND_ROUTING_SERVICE_ALWAYS = 
		new BooleanPreference("use_osmand_routing_service", true, false);

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_OSM_BUGS = new BooleanPreference("show_osm_bugs", false, true);	
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> DEBUG_RENDERING_INFO = new BooleanPreference("debug_rendering", false, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_YANDEX_TRAFFIC = new BooleanPreference("show_yandex_traffic", false, false);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_FAVORITES = new BooleanPreference("show_favorites", false, false);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> MAP_SCREEN_ORIENTATION = 
		new IntPreference("map_screen_orientation", ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, true);
	
	// TODO switch modes
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> SHOW_VIEW_ANGLE = 
		new BooleanPreference("show_view_angle", false, true);

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> AUTO_ZOOM_MAP = 
		new BooleanPreference("auto_zoom_map", false, true);

	// this value string is synchronized with settings_pref.xml preference name
	public static final int ROTATE_MAP_TO_BEARING_DEF = 0;
	public static final int ROTATE_MAP_NONE = 0;
	public static final int ROTATE_MAP_BEARING = 1;
	public static final int ROTATE_MAP_COMPASS = 2;
	public final OsmandPreference<Integer> ROTATE_MAP = 
			new IntPreference("rotate_map", ROTATE_MAP_TO_BEARING_DEF, false);

	// this value string is synchronized with settings_pref.xml preference name
	public static final int CENTER_CONSTANT = 0;
	public static final int BOTTOM_CONSTANT = 1;
	public final OsmandPreference<Integer> POSITION_ON_MAP = new IntPreference("position_on_map", CENTER_CONSTANT, false);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> MAX_LEVEL_TO_DOWNLOAD_TILE = new IntPreference("max_level_download_tile", 18, false, true);

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> MAP_VIEW_3D = new BooleanPreference("map_view_3d", false, false);

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_ENGLISH_NAMES = new BooleanPreference("use_english_names", false, true);
	
	public boolean usingEnglishNames(){
		return USE_ENGLISH_NAMES.get();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_STEP_BY_STEP_RENDERING = new BooleanPreference("use_step_by_step_rendering",
			true, false);

	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAP_VECTOR_DATA = "map_vector_data"; //$NON-NLS-1$
	public static final String MAP_TILE_SOURCES = "map_tile_sources"; //$NON-NLS-1$
	
	// TODO profile preferences ???
	public boolean isUsingMapVectorData(){
		return globalPreferences.getBoolean(MAP_VECTOR_DATA, false);
	}
	
	public boolean setUsingMapVectorData(boolean val){
		return globalPreferences.edit().putBoolean(MAP_VECTOR_DATA, val).commit();
	}
	
	public boolean setMapTileSource(String tileSource){
		return globalPreferences.edit().putString(MAP_TILE_SOURCES, tileSource).commit();
	}
	
	public String getMapTileSourceName(){
		return globalPreferences.getString(MAP_TILE_SOURCES, TileSourceManager.getMapnikSource().getName());
	}
	
	public ITileSource getMapTileSource() {
		String tileName = globalPreferences.getString(MAP_TILE_SOURCES, null);
		if (tileName != null) {
			
			List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
			for (TileSourceTemplate l : list) {
				if (l.getName().equals(tileName)) {
					return l;
				}
			}
			File tPath = extendOsmandPath(ResourceManager.TILES_PATH);
			File dir = new File(tPath, tileName);
			if(dir.exists()){
				if(tileName.endsWith(SQLiteTileSource.EXT)){
					return new SQLiteTileSource(dir);
				} else if (dir.isDirectory()) {
					String url = null;
					File readUrl = new File(dir, "url"); //$NON-NLS-1$
					try {
						if (readUrl.exists()) {
							BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(readUrl), "UTF-8")); //$NON-NLS-1$
							url = reader.readLine();
							url = url.replaceAll(Pattern.quote("{$z}"), "{0}"); //$NON-NLS-1$ //$NON-NLS-2$
							url = url.replaceAll(Pattern.quote("{$x}"), "{1}"); //$NON-NLS-1$//$NON-NLS-2$
							url = url.replaceAll(Pattern.quote("{$y}"), "{2}"); //$NON-NLS-1$ //$NON-NLS-2$
							reader.close();
						}
					} catch (IOException e) {
						Log.d(LogUtil.TAG, "Error reading url " + dir.getName(), e); //$NON-NLS-1$
					}
					return new TileSourceManager.TileSourceTemplate(dir, dir.getName(), url);
				}
			}
				
		}
		return TileSourceManager.getMapnikSource();
	}
	
	public Map<String, String> getTileSourceEntries(){
		Map<String, String> map = new LinkedHashMap<String, String>();
		File dir = extendOsmandPath(ResourceManager.TILES_PATH);
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

	public static final String EXTERNAL_STORAGE_DIR = "external_storage_dir"; //$NON-NLS-1$
	
	public File getExternalStorageDirectory() {
		return new File(globalPreferences.getString(EXTERNAL_STORAGE_DIR, Environment.getExternalStorageDirectory().getAbsolutePath()));
	}
	
	public boolean setExternalStorageDirectory(String externalStorageDir) {
		return globalPreferences.edit().putString(EXTERNAL_STORAGE_DIR, externalStorageDir).commit();
	}
	
	public File extendOsmandPath(String path) {
		return new File(getExternalStorageDirectory(), path);
	}


	// This value is a key for saving last known location shown on the map
	public static final String LAST_KNOWN_MAP_LAT = "last_known_map_lat"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_LON = "last_known_map_lon"; //$NON-NLS-1$
	public static final String IS_MAP_SYNC_TO_GPS_LOCATION = "is_map_sync_to_gps_location"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_ZOOM = "last_known_map_zoom"; //$NON-NLS-1$
	
	public static final String MAP_LAT_TO_SHOW = "map_lat_to_show"; //$NON-NLS-1$
	public static final String MAP_LON_TO_SHOW = "map_lon_to_show"; //$NON-NLS-1$
	public static final String MAP_ZOOM_TO_SHOW = "map_zoom_to_show"; //$NON-NLS-1$

	public LatLon getLastKnownMapLocation() {
		float lat = globalPreferences.getFloat(LAST_KNOWN_MAP_LAT, 0);
		float lon = globalPreferences.getFloat(LAST_KNOWN_MAP_LON, 0);
		return new LatLon(lat, lon);
	}
	
	public boolean isLastKnownMapLocation(){
		return globalPreferences.contains(LAST_KNOWN_MAP_LAT);
	}

	public void setMapLocationToShow(double latitude, double longitude) {
		setMapLocationToShow(latitude, longitude, getLastKnownMapZoom(), null);
	}
	
	public void setMapLocationToShow(double latitude, double longitude, int zoom) {
		setMapLocationToShow(latitude, longitude, null);
	}
	
	public LatLon getAndClearMapLocationToShow(){
		if(!globalPreferences.contains(MAP_LAT_TO_SHOW)){
			return null;
		}
		float lat = globalPreferences.getFloat(MAP_LAT_TO_SHOW, 0);
		float lon = globalPreferences.getFloat(MAP_LON_TO_SHOW, 0);
		globalPreferences.edit().remove(MAP_LAT_TO_SHOW).commit();
		return new LatLon(lat, lon);
	}
	
	public int getMapZoomToShow() {
		return globalPreferences.getInt(MAP_ZOOM_TO_SHOW, 5);
	}
	
	public void setMapLocationToShow(double latitude, double longitude, int zoom, String historyDescription) {
		Editor edit = globalPreferences.edit();
		edit.putFloat(MAP_LAT_TO_SHOW, (float) latitude);
		edit.putFloat(MAP_LON_TO_SHOW, (float) longitude);
		edit.putInt(MAP_ZOOM_TO_SHOW, zoom);
		edit.putBoolean(IS_MAP_SYNC_TO_GPS_LOCATION, false);
		edit.commit();
		if(historyDescription != null){
			SearchHistoryHelper.getInstance().addNewItemToHistory(latitude, longitude, historyDescription, ctx);
		}
	}
	
	public void setMapLocationToShow(double latitude, double longitude, String historyDescription) {
		setMapLocationToShow(latitude, longitude, getLastKnownMapZoom(), historyDescription);
	}

	// Do not use that method if you want to show point on map. Use setMapLocationToShow
	public void setLastKnownMapLocation(double latitude, double longitude) {
		Editor edit = globalPreferences.edit();
		edit.putFloat(LAST_KNOWN_MAP_LAT, (float) latitude);
		edit.putFloat(LAST_KNOWN_MAP_LON, (float) longitude);
		edit.commit();
	}

	public boolean setSyncMapToGpsLocation(boolean value) {
		return globalPreferences.edit().putBoolean(IS_MAP_SYNC_TO_GPS_LOCATION, value).commit();
	}

	public boolean isMapSyncToGpsLocation() {
		return globalPreferences.getBoolean(IS_MAP_SYNC_TO_GPS_LOCATION, true);
	}

	public int getLastKnownMapZoom() {
		return globalPreferences.getInt(LAST_KNOWN_MAP_ZOOM, 5);
	}

	public void setLastKnownMapZoom(int zoom) {
		globalPreferences.edit().putInt(LAST_KNOWN_MAP_ZOOM, zoom).commit();
	}

	public final static String POINT_NAVIGATE_LAT = "point_navigate_lat"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_LON = "point_navigate_lon"; //$NON-NLS-1$

	public LatLon getPointToNavigate() {
		float lat = globalPreferences.getFloat(POINT_NAVIGATE_LAT, 0);
		float lon = globalPreferences.getFloat(POINT_NAVIGATE_LON, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}

	public boolean clearPointToNavigate() {
		return globalPreferences.edit().remove(POINT_NAVIGATE_LAT).remove(POINT_NAVIGATE_LON).commit();
	}

	public boolean setPointToNavigate(double latitude, double longitude) {
		return globalPreferences.edit().putFloat(POINT_NAVIGATE_LAT, (float) latitude).putFloat(POINT_NAVIGATE_LON, (float) longitude).commit();
	}

	public static final String LAST_SEARCHED_REGION = "last_searched_region"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_CITY = "last_searched_city"; //$NON-NLS-1$
	public static final String lAST_SEARCHED_POSTCODE= "last_searched_postcode"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_STREET = "last_searched_street"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_BUILDING = "last_searched_building"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_INTERSECTED_STREET = "last_searched_intersected_street"; //$NON-NLS-1$

	public String getLastSearchedRegion() {
		return globalPreferences.getString(LAST_SEARCHED_REGION, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedRegion(String region) {
		Editor edit = globalPreferences.edit().putString(LAST_SEARCHED_REGION, region).putLong(LAST_SEARCHED_CITY, -1).putString(LAST_SEARCHED_STREET,
				"").putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (globalPreferences.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}
	
	public String getLastSearchedPostcode(){
		return globalPreferences.getString(lAST_SEARCHED_POSTCODE, null);	
	}
	
	public boolean setLastSearchedPostcode(String postcode){
		Editor edit = globalPreferences.edit().putLong(LAST_SEARCHED_CITY, -1).putString(LAST_SEARCHED_STREET, "").putString( //$NON-NLS-1$
				LAST_SEARCHED_BUILDING, "").putString(lAST_SEARCHED_POSTCODE, postcode); //$NON-NLS-1$
		if(globalPreferences.contains(LAST_SEARCHED_INTERSECTED_STREET)){
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}

	public Long getLastSearchedCity() {
		return globalPreferences.getLong(LAST_SEARCHED_CITY, -1);
	}

	public boolean setLastSearchedCity(Long cityId) {
		Editor edit = globalPreferences.edit().putLong(LAST_SEARCHED_CITY, cityId).putString(LAST_SEARCHED_STREET, "").putString( //$NON-NLS-1$
				LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
		edit.remove(lAST_SEARCHED_POSTCODE);
		if(globalPreferences.contains(LAST_SEARCHED_INTERSECTED_STREET)){
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}

	public String getLastSearchedStreet() {
		return globalPreferences.getString(LAST_SEARCHED_STREET, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedStreet(String street) {
		Editor edit = globalPreferences.edit().putString(LAST_SEARCHED_STREET, street).putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
		if (globalPreferences.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}

	public String getLastSearchedBuilding() {
		return globalPreferences.getString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedBuilding(String building) {
		return globalPreferences.edit().putString(LAST_SEARCHED_BUILDING, building).remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
	}

	public String getLastSearchedIntersectedStreet() {
		if (!globalPreferences.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			return null;
		}
		return globalPreferences.getString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
	}

	public boolean setLastSearchedIntersectedStreet(String street) {
		return globalPreferences.edit().putString(LAST_SEARCHED_INTERSECTED_STREET, street).commit();
	}

	public boolean removeLastSearchedIntersectedStreet() {
		return globalPreferences.edit().remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
	}

	public static final String SELECTED_POI_FILTER_FOR_MAP = "selected_poi_filter_for_map"; //$NON-NLS-1$

	public boolean setPoiFilterForMap(String filterId) {
		return globalPreferences.edit().putString(SELECTED_POI_FILTER_FOR_MAP, filterId).commit();
	}

	public PoiFilter getPoiFilterForMap(OsmandApplication application) {
		String filterId = globalPreferences.getString(SELECTED_POI_FILTER_FOR_MAP, null);
		PoiFilter filter = application.getPoiFilters().getFilterById(filterId);
		if (filter != null) {
			return filter;
		}
		return new PoiFilter(null, application);
	}
	

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> VOICE_PROVIDER = new StringPreference("voice_provider", null, false);
	
	// this value string is synchronized with settings_pref.xml preference name
	// TODO init default value !!!
	public final OsmandPreference<String> RENDERER = new StringPreference("renderer", null, false);
	
	public final OsmandPreference<Boolean> VOICE_MUTE = new BooleanPreference("voice_mute", false, true);
	
	// for background service
	public final OsmandPreference<Boolean> MAP_ACTIVITY_ENABLED = new BooleanPreference("map_activity_enabled", false, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_ENABLED = "service_off_enabled"; //$NON-NLS-1$
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<String> SERVICE_OFF_PROVIDER = new StringPreference("service_off_provider", LocationManager.GPS_PROVIDER, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> SERVICE_OFF_INTERVAL = new IntPreference("service_off_interval", 
			5 * 60 * 1000, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> SERVICE_OFF_WAIT_INTERVAL = new IntPreference("service_off_wait_interval", 
			90 * 1000, true);
	
	public final OsmandPreference<String> CONTRIBUTION_INSTALL_APP_DATE = new StringPreference("CONTRIBUTION_INSTALL_APP_DATE", null, true);
	
	
	public final OsmandPreference<Boolean> FOLLOW_TO_THE_ROUTE = new BooleanPreference("follow_to_route", false, true);
	
	public final OsmandPreference<Boolean> SHOW_ARRIVAL_TIME_OTHERWISE_EXPECTED_TIME = 
		new BooleanPreference("show_arrival_time", true, true);
	
	public enum DayNightMode {
		AUTO(R.string.daynight_mode_auto), 
		DAY(R.string.daynight_mode_day), 
		NIGHT(R.string.daynight_mode_night),
		SENSOR(R.string.daynight_mode_sensor);

		private final int key;
		
		DayNightMode(int key) {
			this.key = key;
		}
		
		public  String toHumanString(Context ctx){
			return ctx.getResources().getString(key);
		}

		public boolean isSensor() {
			return this == SENSOR;
		}

		public boolean isAuto() {
			return this == AUTO;
		}

		public boolean isDay() {
			return this == DAY;
		}

		public boolean isNight() {
			return this == NIGHT;
		}
		
		public static DayNightMode[] possibleValues(Context context) {
	         SensorManager mSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);         
	         Sensor mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
	         if (mLight != null) {
	        	 return DayNightMode.values();
	         } else {
	        	 return new DayNightMode[] { AUTO, DAY, NIGHT };
	         }
		}

	}
	
	public enum MetricsConstants {
	    KILOMETERS_AND_METERS(R.string.si_km_m),
		MILES_AND_YARDS(R.string.si_mi_yard),
		MILES_AND_FOOTS(R.string.si_mi_foots);
		
		private final int key;
		MetricsConstants(int key) {
			this.key = key;
		}
		
		public String toHumanString(Context ctx){
			return ctx.getResources().getString(key);
		}
		
	}
	
}
