package net.osmand.plus;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.map.TileSourceManager.TileSourceTemplate;
import net.osmand.osm.LatLon;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.OsmandApplication;
import net.osmand.plus.activities.RouteProvider.RouteService;
import net.osmand.plus.activities.search.SearchHistoryHelper;
import net.osmand.plus.render.BaseOsmandRender;
import net.osmand.plus.render.RendererRegistry;
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
import android.widget.Toast;

public class OsmandSettings {
	// GLOBAL instance - make instance global for application
	// if some problems appear it can be unique for Application (ApplicationContext)
	private static OsmandSettings INSTANCE;
	
	public static OsmandSettings getOsmandSettings(Context ctx) {
		if (INSTANCE == null) {
			synchronized (ctx.getApplicationContext()) {
				if (INSTANCE == null) {
					INSTANCE = new OsmandSettings((OsmandApplication) ctx.getApplicationContext());
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
	private OsmandApplication ctx;
	private SharedPreferences globalPreferences;
	private SharedPreferences defaultProfilePreferences;
	private SharedPreferences profilePreferences;
	private ApplicationMode currentMode;
	
	// cache variables
	private long lastTimeInternetConnectionChecked = 0;
	private boolean internetConnectionAvailable = true;
	private List<TileSourceTemplate> internetAvailableSourceTemplates = null;
	
	// TODO make all layers profile preferenced????
	private OsmandSettings(OsmandApplication ctx){
		this.ctx = ctx;
		globalPreferences = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		// start from default settings
		currentMode = ApplicationMode.DEFAULT;
		
		defaultProfilePreferences = getProfilePreferences(ApplicationMode.DEFAULT);
		profilePreferences = defaultProfilePreferences;
//		if(FOLLOW_TO_THE_ROUTE.get()){
			currentMode = readApplicationMode();
			profilePreferences = getProfilePreferences(currentMode);
//		}
	}
	
	private SharedPreferences getProfilePreferences(ApplicationMode mode){
		return ctx.getSharedPreferences(SHARED_PREFERENCES_NAME + "." + mode.name().toLowerCase(), Context.MODE_WORLD_READABLE);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<ApplicationMode> APPLICATION_MODE = new OsmandPreference<ApplicationMode>(){
		public String getId() {
			return "application_mode";
		};
		
		@Override
		public ApplicationMode get() {
			return currentMode;
		}

		@Override
		public boolean set(ApplicationMode val) {
			ApplicationMode oldMode = currentMode;
			boolean changed = globalPreferences.edit().putString(getId(), val.name()).commit();
			if(changed){
				currentMode = val;
				profilePreferences = getProfilePreferences(currentMode);
				switchApplicationMode(oldMode);
			}
			return changed;
		}
	}; 
	
	public ApplicationMode getApplicationMode(){
		return APPLICATION_MODE.get();
	}
	
	protected ApplicationMode readApplicationMode() {
		String s = globalPreferences.getString(APPLICATION_MODE.getId(), ApplicationMode.DEFAULT.name());
		try {
			return ApplicationMode.valueOf(s);
		} catch (IllegalArgumentException e) {
			return ApplicationMode.DEFAULT;
		}
	}

	protected void switchApplicationMode(ApplicationMode oldMode){
		// change some global settings/ for car
		if(currentMode == ApplicationMode.CAR){
			SHOW_TRANSPORT_OVER_MAP.set(false);
			SHOW_OSM_BUGS.set(false);
		}
		// update vector renderer 
		RendererRegistry registry = ctx.getRendererRegistry();
		BaseOsmandRender newRenderer = registry.getRenderer(RENDERER.get());
		if (newRenderer == null) {
			newRenderer = registry.defaultRender();
		}
		if(registry.getCurrentSelectedRenderer() != newRenderer){
			registry.setCurrentSelectedRender(newRenderer);
			ctx.getResourceManager().getRenderer().clearCache();
		}
	}
	

	// Check internet connection available every 15 seconds
	public boolean isInternetConnectionAvailable(){
		return isInternetConnectionAvailable(false);
	}
	public boolean isInternetConnectionAvailable(boolean update){
		long delta = System.currentTimeMillis() - lastTimeInternetConnectionChecked;
		if(delta < 0 || delta > 15000 || update){
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
	
	public abstract class CommonPreference<T> implements OsmandPreference<T> {
		private final String id;
		private final boolean global;
		private T cachedValue;
		private SharedPreferences cachedPreference;
		private boolean cache;
		private Map<ApplicationMode, T> defaultValues;
		private T defaultValue;
		
		public CommonPreference(String id, boolean global, T defaultValue){
			this.id = id;
			this.global = global;
			this.defaultValue = defaultValue;
		}
		
		public CommonPreference(String id, boolean global, boolean cache, T defaultValue){
			this.id = id;
			this.global = global;
			this.cache = cache;
			this.defaultValue = defaultValue; 
		}
		
		protected SharedPreferences getPreferences(){
			return global ? globalPreferences : profilePreferences;
		}
		
		public void setModeDefaultValue(ApplicationMode mode, T defValue){
			if(defaultValues == null){
				defaultValues = new LinkedHashMap<ApplicationMode, T>();
			}
			defaultValues.put(mode, defValue);
		}
		
		protected T getDefaultValue(){
			if(global){
				return defaultValue;
			}
			if(defaultValues != null && defaultValues.containsKey(currentMode)){
				return defaultValues.get(currentMode);
			}
			if(defaultProfilePreferences.contains(getId())) {
				return getValue(defaultProfilePreferences, defaultValue);
			} else {
				return defaultValue;
			}
		}
		
		protected abstract T getValue(SharedPreferences prefs, T defaultValue);
		
		protected abstract boolean setValue(SharedPreferences prefs, T val);

		@Override
		public T get() {
			if(cache && cachedValue != null && cachedPreference == getPreferences()){
				return cachedValue;
			}
			cachedPreference = getPreferences();
			cachedValue = getValue(cachedPreference, getDefaultValue());
			return cachedValue;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public boolean set(T obj) {
			SharedPreferences prefs = getPreferences();
			if(setValue(prefs,obj)){
				cachedValue = obj;
				cachedPreference = prefs;
				return true;
			}
			return false;
		}
		
	}
	
	private class BooleanPreference extends CommonPreference<Boolean> {

		private BooleanPreference(String id, boolean defaultValue, boolean global) {
			super(id, global, defaultValue);
		}
		
		private BooleanPreference(String id, boolean defaultValue, boolean global, boolean cache) {
			super(id, global, cache);
		}
		
		@Override
		protected Boolean getValue(SharedPreferences prefs, Boolean defaultValue) {
			return prefs.getBoolean(getId(), defaultValue);
		}

		@Override
		protected boolean setValue(SharedPreferences prefs, Boolean val) {
			return prefs.edit().putBoolean(getId(), val).commit();
		}

	}
	private class IntPreference extends CommonPreference<Integer> {


		private IntPreference(String id, int defaultValue, boolean global) {
			super(id, global, defaultValue);
		}
		
		private IntPreference(String id, int defaultValue, boolean global, boolean cache) {
			super(id, global, cache, defaultValue);
		}
		
		@Override
		protected Integer getValue(SharedPreferences prefs, Integer defaultValue) {
			return prefs.getInt(getId(), defaultValue);
		}

		@Override
		protected boolean setValue(SharedPreferences prefs, Integer val) {
			return prefs.edit().putInt(getId(), val).commit();
		}

	}
	
	private class StringPreference extends CommonPreference<String> {

		private StringPreference(String id, String defaultValue, boolean global) {
			super(id, global, defaultValue);
		}

		@Override
		protected String getValue(SharedPreferences prefs, String defaultValue) {
			return prefs.getString(getId(), defaultValue);
		}

		@Override
		protected boolean setValue(SharedPreferences prefs, String val) {
			return prefs.edit().putString(getId(), val).commit();
		}

	}
	
	private class EnumIntPreference<E extends Enum<E>> extends CommonPreference<E> {

		private final E[] values;

		private EnumIntPreference(String id, E defaultValue, boolean global, boolean cache,
				E[] values) {
			super(id, global, cache, defaultValue);
			this.values = values;
		}
		
		private EnumIntPreference(String id, E defaultValue, boolean global, E[] values) {
			super(id, global, defaultValue);
			this.values = values;
		}


		@Override
		protected E getValue(SharedPreferences prefs, E defaultValue) {
			int i = prefs.getInt(getId(), -1);
			if(i < 0 || i >= values.length){
				return defaultValue;
			}
			return values[i];
		}
		
		@Override
		protected boolean setValue(SharedPreferences prefs,E val) {
			return prefs.edit().putInt(getId(), val.ordinal()).commit();
		}

	}
	/////////////// PREFERENCES classes ////////////////
	
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> USE_INTERNET_TO_DOWNLOAD_TILES =
		new BooleanPreference("use_internet_to_download_tiles", true, false, true);
	{
		USE_INTERNET_TO_DOWNLOAD_TILES.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	
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
		new EnumIntPreference<DayNightMode>("daynight_mode", DayNightMode.AUTO, false, DayNightMode.values()) {
		protected boolean setValue(SharedPreferences prefs, DayNightMode val) {
			ctx.getDaynightHelper().setDayNightMode(val);
			return super.setValue(prefs, val);
		}
	};
		
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<RouteService> ROUTER_SERVICE = 
		new EnumIntPreference<RouteService>("router_service", RouteService.OSMAND, false, RouteService.values());


	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_CURRENT_TRACK = "save_current_track"; //$NON-NLS-1$
	public static final String RELOAD_INDEXES = "reload_indexes"; //$NON-NLS-1$
	public static final String DOWNLOAD_INDEXES = "download_indexes"; //$NON-NLS-1$

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> SAVE_TRACK_TO_GPX = new BooleanPreference("save_track_to_gpx", false, false);
	{
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.CAR, true);
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.BICYCLE, true);
		SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> FAST_ROUTE_MODE = new BooleanPreference("fast_route_mode", true, false);

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> SAVE_TRACK_INTERVAL = new IntPreference("save_track_interval", 5, false);
	{
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.CAR, 5);
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.BICYCLE, 10);
		SAVE_TRACK_INTERVAL.setModeDefaultValue(ApplicationMode.PEDESTRIAN, 20);
	}

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
	
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> SHOW_VIEW_ANGLE = new BooleanPreference("show_view_angle", false, true, true);
	{
		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.BICYCLE, true);
		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
		SHOW_VIEW_ANGLE.setModeDefaultValue(ApplicationMode.CAR, false);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> AUTO_ZOOM_MAP = new BooleanPreference("auto_zoom_map", false, true);
	{
		AUTO_ZOOM_MAP.setModeDefaultValue(ApplicationMode.CAR, true);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final int ROTATE_MAP_NONE = 0;
	public static final int ROTATE_MAP_BEARING = 1;
	public static final int ROTATE_MAP_COMPASS = 2;
	public final CommonPreference<Integer> ROTATE_MAP = 
			new IntPreference("rotate_map", ROTATE_MAP_NONE, false, true);
	{
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.CAR, ROTATE_MAP_BEARING);
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, ROTATE_MAP_BEARING);
		ROTATE_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, ROTATE_MAP_COMPASS);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final int CENTER_CONSTANT = 0;
	public static final int BOTTOM_CONSTANT = 1;
	public final CommonPreference<Integer> POSITION_ON_MAP = new IntPreference("position_on_map", CENTER_CONSTANT, false);
	{
		POSITION_ON_MAP.setModeDefaultValue(ApplicationMode.CAR, BOTTOM_CONSTANT);
		POSITION_ON_MAP.setModeDefaultValue(ApplicationMode.BICYCLE, BOTTOM_CONSTANT);
		POSITION_ON_MAP.setModeDefaultValue(ApplicationMode.PEDESTRIAN, CENTER_CONSTANT);
		
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> MAX_LEVEL_TO_DOWNLOAD_TILE = new IntPreference("max_level_download_tile", 18, false, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Integer> LEVEL_TO_SWITCH_VECTOR_RASTER = new IntPreference("level_to_switch_vector_raster", 5, false, true);

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> MAP_VIEW_3D = new BooleanPreference("map_view_3d", false, false);

	// this value string is synchronized with settings_pref.xml preference name
	public final OsmandPreference<Boolean> USE_ENGLISH_NAMES = new BooleanPreference("use_english_names", false, true);
	
	public boolean usingEnglishNames(){
		return USE_ENGLISH_NAMES.get();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> USE_STEP_BY_STEP_RENDERING = new BooleanPreference("use_step_by_step_rendering",
			true, false);
	{
		USE_STEP_BY_STEP_RENDERING.setModeDefaultValue(ApplicationMode.CAR, true);
		USE_STEP_BY_STEP_RENDERING.setModeDefaultValue(ApplicationMode.BICYCLE, false);
		USE_STEP_BY_STEP_RENDERING.setModeDefaultValue(ApplicationMode.PEDESTRIAN, false);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Boolean> MAP_VECTOR_DATA = new BooleanPreference("map_vector_data",
			false, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> MAP_OVERLAY = new StringPreference("map_overlay",
			null, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> MAP_UNDERLAY = new StringPreference("map_underlay",
			null, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> MAP_OVERLAY_TRANSPARENCY = new IntPreference("overlay_transparency",
			200, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<Integer> MAP_TRANSPARENCY = new IntPreference("map_transparency",
			255, true);
	
	// this value string is synchronized with settings_pref.xml preference name
	public final CommonPreference<String> MAP_TILE_SOURCES = new StringPreference("map_tile_sources",
			TileSourceManager.getMapnikSource().getName(), true);
	
	public List<TileSourceTemplate> getInternetAvailableSourceTemplates(){
		if(internetAvailableSourceTemplates == null && isInternetConnectionAvailable()){
			internetAvailableSourceTemplates = TileSourceManager.downloadTileSourceTemplates();
		}
		return internetAvailableSourceTemplates;
	}
	
	public ITileSource getMapTileSource(boolean warnWhenSelected) {
		String tileName = MAP_TILE_SOURCES.get();
		if (tileName != null) {
			ITileSource ts = getTileSourceByName(tileName, warnWhenSelected);
			if(ts != null){
				return ts;
			}
		}
		return TileSourceManager.getMapnikSource();
	}
	
	private TileSourceTemplate checkAmongAvailableTileSources(File dir, List<TileSourceTemplate> list){
		for (TileSourceTemplate l : list) {
			if (dir.getName().equals(l.getName())) {
				try {
					dir.mkdirs();
					TileSourceManager.createMetaInfoFile(dir, l, true);
				} catch (IOException e) {
				}
				return l;
			}
			
		}
		return null;
	}
		
	

	public ITileSource getTileSourceByName(String tileName, boolean warnWhenSelected) {
		if(tileName == null || tileName.length() == 0){
			return null;
		}
		List<TileSourceTemplate> knownTemplates = TileSourceManager.getKnownSourceTemplates();
		File tPath = extendOsmandPath(ResourceManager.TILES_PATH);
		File dir = new File(tPath, tileName);
		if (!dir.exists()) {
			TileSourceTemplate ret = checkAmongAvailableTileSources(dir, knownTemplates);
			if (ret != null) {
				return ret;
			}
			// try to find among other templates
			ret = checkAmongAvailableTileSources(dir, getInternetAvailableSourceTemplates());
			if (ret != null) {
				return ret;
			}
		} else if (tileName.endsWith(SQLiteTileSource.EXT)) {
			return new SQLiteTileSource(dir, knownTemplates);
		} else if (dir.isDirectory() && !dir.getName().startsWith(".")) {
			TileSourceTemplate t = TileSourceManager.createTileSourceTemplate(dir);
			if (warnWhenSelected && !t.isRuleAcceptable()) {
				Toast.makeText(ctx, ctx.getString(R.string.warning_tile_layer_not_downloadable, dir.getName()), Toast.LENGTH_SHORT).show();
			}
			if (!TileSourceManager.isTileSourceMetaInfoExist(dir)) {
				TileSourceTemplate ret = checkAmongAvailableTileSources(dir, knownTemplates);
				if (ret != null) {
					t = ret;
				} else {
					// try to find among other templates
					ret = checkAmongAvailableTileSources(dir, getInternetAvailableSourceTemplates());
					if (ret != null) {
						t = ret;
					}
				}

			}

			return t;
		}
		return null;
	}
	
	public boolean installTileSource(TileSourceTemplate toInstall){
		File tPath = extendOsmandPath(ResourceManager.TILES_PATH);
		File dir = new File(tPath, toInstall.getName());
		dir.mkdirs();
		if(dir.exists() && dir.isDirectory()){
			try {
				TileSourceManager.createMetaInfoFile(dir, toInstall, true);
			} catch (IOException e) {
				return false;
			}
		}
		return true;
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
					} else if (f.isDirectory() && !f.getName().equals(ResourceManager.TEMP_SOURCE_TO_LOAD)
							&& !f.getName().startsWith(".")) {
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
	public final CommonPreference<String> RENDERER = new StringPreference("renderer", RendererRegistry.DEFAULT_RENDER, false) {
		protected boolean setValue(SharedPreferences prefs, String val) {
			if(val == null){
				val = RendererRegistry.DEFAULT_RENDER;
			}
			BaseOsmandRender loaded = ctx.getRendererRegistry().getRenderer(val);
			if (loaded != null) {
				ctx.getRendererRegistry().setCurrentSelectedRender(loaded);
				super.setValue(prefs, val);
				ctx.getResourceManager().getRenderer().clearCache();
				return true;
			}
			return false;
		};
	};
	{
		RENDERER.setModeDefaultValue(ApplicationMode.CAR, RendererRegistry.CAR_RENDER);
		RENDERER.setModeDefaultValue(ApplicationMode.PEDESTRIAN, RendererRegistry.PEDESTRIAN_RENDER);
		RENDERER.setModeDefaultValue(ApplicationMode.BICYCLE, RendererRegistry.BICYCLE_RENDER);
	}
	
	
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
