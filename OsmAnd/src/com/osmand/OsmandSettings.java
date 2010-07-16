package com.osmand;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.location.LocationManager;

import com.osmand.activities.RouteProvider.RouteService;
import com.osmand.activities.search.SearchHistoryHelper;
import com.osmand.map.ITileSource;
import com.osmand.map.TileSourceManager;
import com.osmand.map.TileSourceManager.TileSourceTemplate;
import com.osmand.osm.LatLon;

public class OsmandSettings {

	public enum ApplicationMode {
		/*
		 * DEFAULT("Default"), CAR("Car"), BICYCLE("Bicycle"), PEDESTRIAN("Pedestrian");
		 */

		DEFAULT(R.string.app_mode_default), 
		CAR(R.string.app_mode_car), 
		BICYCLE(R.string.app_mode_bicycle), 
		PEDESTRIAN(R.string.app_mode_pedestrian);

		private final int key;
		
		ApplicationMode(int key) {
			this.key = key;
		}
		public static String toHumanString(ApplicationMode m, Context ctx){
			return ctx.getResources().getString(m.key);
		}

	}

	// These settings are stored in SharedPreferences
	public static final String SHARED_PREFERENCES_NAME = "com.osmand.settings"; //$NON-NLS-1$

	public static final int CENTER_CONSTANT = 0;
	public static final int BOTTOM_CONSTANT = 1;

	// this value string is synchronized with settings_pref.xml preference name
	public static final String USE_INTERNET_TO_DOWNLOAD_TILES = "use_internet_to_download_tiles"; //$NON-NLS-1$
	public static final boolean USE_INTERNET_TO_DOWNLOAD_TILES_DEF = true;

	public static boolean isUsingInternetToDownloadTiles(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(USE_INTERNET_TO_DOWNLOAD_TILES, USE_INTERNET_TO_DOWNLOAD_TILES_DEF);
	}
	

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_POI_OVER_MAP = "show_poi_over_map"; //$NON-NLS-1$
	public static final Boolean SHOW_POI_OVER_MAP_DEF = false;

	public static boolean isShowingPoiOverMap(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SHOW_POI_OVER_MAP, SHOW_POI_OVER_MAP_DEF);
	}
	
	public static boolean setShowPoiOverMap(Context ctx, boolean val) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SHOW_POI_OVER_MAP, val).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_TRANSPORT_OVER_MAP = "show_transport_over_map"; //$NON-NLS-1$
	public static final boolean SHOW_TRANSPORT_OVER_MAP_DEF = false;

	public static boolean isShowingTransportOverMap(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SHOW_TRANSPORT_OVER_MAP, SHOW_TRANSPORT_OVER_MAP_DEF);
	}
	
	public static boolean setShowTransortOverMap(Context ctx, boolean val) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SHOW_TRANSPORT_OVER_MAP, val).commit();
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String USER_NAME = "user_name"; //$NON-NLS-1$

	public static String getUserName(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(USER_NAME, "NoName"); //$NON-NLS-1$
	}

	public static boolean setUserName(Context ctx, String name) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(USER_NAME, name).commit();
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String USER_OSM_BUG_NAME = "user_osm_bug_name"; //$NON-NLS-1$

	public static String getUserNameForOsmBug(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(USER_OSM_BUG_NAME, "NoName"); //$NON-NLS-1$
	}

	public static boolean setUserNameForOsmBug(Context ctx, String name) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(USER_OSM_BUG_NAME, name).commit();
	}
	
	public static final String USER_PASSWORD = "user_password"; //$NON-NLS-1$
	public static String getUserPassword(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(USER_PASSWORD, ""); //$NON-NLS-1$
	}
	
	public static boolean setUserPassword(Context ctx, String name){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(USER_PASSWORD, name).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String APPLICATION_MODE = "application_mode"; //$NON-NLS-1$

	public static ApplicationMode getApplicationMode(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		String s = prefs.getString(APPLICATION_MODE, ApplicationMode.DEFAULT.name());
		try {
			return ApplicationMode.valueOf(s);
		} catch (IllegalArgumentException e) {
			return ApplicationMode.DEFAULT;
		}
	}

	public static boolean setApplicationMode(Context ctx, ApplicationMode p) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(APPLICATION_MODE, p.name()).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String ROUTER_SERVICE = "router_service"; //$NON-NLS-1$

	public static RouteService getRouterService(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		int ord = prefs.getInt(ROUTER_SERVICE, RouteService.CLOUDMADE.ordinal());
		if(ord < RouteService.values().length){
			return RouteService.values()[ord];
		} else {
			return RouteService.CLOUDMADE;
		}
	}

	public static boolean setRouterService(Context ctx, RouteService p) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putInt(ROUTER_SERVICE, p.ordinal()).commit();
	}	

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_CURRENT_TRACK = "save_current_track"; //$NON-NLS-1$
	public static final String RELOAD_INDEXES = "reload_indexes"; //$NON-NLS-1$
	public static final String DOWNLOAD_INDEXES = "download_indexes"; //$NON-NLS-1$

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_TRACK_TO_GPX = "save_track_to_gpx"; //$NON-NLS-1$
	public static final boolean SAVE_TRACK_TO_GPX_DEF = false; 

	public static boolean isSavingTrackToGpx(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SAVE_TRACK_TO_GPX, SAVE_TRACK_TO_GPX_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SAVE_TRACK_INTERVAL = "save_track_interval"; //$NON-NLS-1$

	public static int getSavingTrackInterval(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(SAVE_TRACK_INTERVAL, 5);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_OSM_BUGS = "show_osm_bugs"; //$NON-NLS-1$
	public static final boolean SHOW_OSM_BUGS_DEF = false;

	public static boolean isShowingOsmBugs(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SHOW_OSM_BUGS, SHOW_OSM_BUGS_DEF);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAP_SCREEN_ORIENTATION = "map_screen_orientation"; //$NON-NLS-1$
	
	public static int getMapOrientation(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(MAP_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SHOW_VIEW_ANGLE = "show_view_angle"; //$NON-NLS-1$
	public static final boolean SHOW_VIEW_ANGLE_DEF = false;

	public static boolean isShowingViewAngle(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SHOW_VIEW_ANGLE, SHOW_VIEW_ANGLE_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String AUTO_ZOOM_MAP = "auto_zoom_map"; //$NON-NLS-1$
	public static final boolean AUTO_ZOOM_MAP_DEF = false;

	public static boolean isAutoZoomEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(AUTO_ZOOM_MAP, AUTO_ZOOM_MAP_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String ROTATE_MAP_TO_BEARING = "rotate_map_to_bearing"; //$NON-NLS-1$
	public static final boolean ROTATE_MAP_TO_BEARING_DEF = false;
	
	public static boolean isRotateMapToBearing(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(ROTATE_MAP_TO_BEARING, ROTATE_MAP_TO_BEARING_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String POSITION_ON_MAP = "position_on_map"; //$NON-NLS-1$

	public static int getPositionOnMap(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(POSITION_ON_MAP, CENTER_CONSTANT);
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAX_LEVEL_TO_DOWNLOAD_TILE = "max_level_download_tile"; //$NON-NLS-1$

	public static int getMaximumLevelToDownloadTile(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(MAX_LEVEL_TO_DOWNLOAD_TILE, 18);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAP_VIEW_3D = "map_view_3d"; //$NON-NLS-1$
	public static final boolean MAP_VIEW_3D_DEF = false;

	public static boolean isMapView3D(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(MAP_VIEW_3D, MAP_VIEW_3D_DEF);
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String USE_ENGLISH_NAMES = "use_english_names"; //$NON-NLS-1$
	public static final boolean USE_ENGLISH_NAMES_DEF = false;

	public static boolean usingEnglishNames(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(USE_ENGLISH_NAMES, USE_ENGLISH_NAMES_DEF);
	}

	public static boolean setUseEnglishNames(Context ctx, boolean useEnglishNames) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(USE_ENGLISH_NAMES, useEnglishNames).commit();
	}

	// this value string is synchronized with settings_pref.xml preference name
	public static final String MAP_TILE_SOURCES = "map_tile_sources"; //$NON-NLS-1$

	public static ITileSource getMapTileSource(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		String tileName = prefs.getString(MAP_TILE_SOURCES, null);
		if (tileName != null) {
			List<TileSourceTemplate> list = TileSourceManager.getKnownSourceTemplates();
			for (TileSourceTemplate l : list) {
				if (l.getName().equals(tileName)) {
					return l;
				}
			}
		}
		return TileSourceManager.getMapnikSource();
	}

	public static String getMapTileSourceName(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		String tileName = prefs.getString(MAP_TILE_SOURCES, null);
		if (tileName != null) {
			return tileName;
		}
		return TileSourceManager.getMapnikSource().getName();
	}

	// This value is a key for saving last known location shown on the map
	public static final String LAST_KNOWN_MAP_LAT = "last_known_map_lat"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_LON = "last_known_map_lon"; //$NON-NLS-1$
	public static final String IS_MAP_SYNC_TO_GPS_LOCATION = "is_map_sync_to_gps_location"; //$NON-NLS-1$
	public static final String LAST_KNOWN_MAP_ZOOM = "last_known_map_zoom"; //$NON-NLS-1$
	
	public static final String MAP_LAT_TO_SHOW = "map_lat_to_show"; //$NON-NLS-1$
	public static final String MAP_LON_TO_SHOW = "map_lon_to_show"; //$NON-NLS-1$
	public static final String MAP_ZOOM_TO_SHOW = "map_zoom_to_show"; //$NON-NLS-1$

	public static LatLon getLastKnownMapLocation(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		float lat = prefs.getFloat(LAST_KNOWN_MAP_LAT, 0);
		float lon = prefs.getFloat(LAST_KNOWN_MAP_LON, 0);
		return new LatLon(lat, lon);
	}

	public static void setMapLocationToShow(Context ctx, double latitude, double longitude) {
		setMapLocationToShow(ctx, latitude, longitude, getLastKnownMapZoom(ctx), null);
	}
	
	public static void setMapLocationToShow(Context ctx, double latitude, double longitude, int zoom) {
		setMapLocationToShow(ctx, latitude, longitude, null);
	}
	
	public static LatLon getAndClearMapLocationToShow(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		if(!prefs.contains(MAP_LAT_TO_SHOW)){
			return null;
		}
		float lat = prefs.getFloat(MAP_LAT_TO_SHOW, 0);
		float lon = prefs.getFloat(MAP_LON_TO_SHOW, 0);
		prefs.edit().remove(MAP_LAT_TO_SHOW).commit();
		return new LatLon(lat, lon);
	}
	
	public static int getMapZoomToShow(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(MAP_ZOOM_TO_SHOW, 5);
	}
	
	public static void setMapLocationToShow(Context ctx, double latitude, double longitude, int zoom, String historyDescription) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		edit.putFloat(MAP_LAT_TO_SHOW, (float) latitude);
		edit.putFloat(MAP_LON_TO_SHOW, (float) longitude);
		edit.putInt(MAP_ZOOM_TO_SHOW, zoom);
		edit.putBoolean(IS_MAP_SYNC_TO_GPS_LOCATION, false);
		edit.commit();
		if(historyDescription != null){
			SearchHistoryHelper.getInstance().addNewItemToHistory(latitude, longitude, historyDescription, ctx);
		}
	}
	
	public static void setMapLocationToShow(Context ctx, double latitude, double longitude, String historyDescription) {
		setMapLocationToShow(ctx, latitude, longitude, getLastKnownMapZoom(ctx), historyDescription);
	}

	// Do not use that method if you want to show point on map. Use setMapLocationToShow
	public static void setLastKnownMapLocation(Context ctx, double latitude, double longitude) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		edit.putFloat(LAST_KNOWN_MAP_LAT, (float) latitude);
		edit.putFloat(LAST_KNOWN_MAP_LON, (float) longitude);
		edit.commit();
	}

	public static boolean setSyncMapToGpsLocation(Context ctx, boolean value) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(IS_MAP_SYNC_TO_GPS_LOCATION, value).commit();
	}

	public static boolean isMapSyncToGpsLocation(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(IS_MAP_SYNC_TO_GPS_LOCATION, true);
	}

	public static int getLastKnownMapZoom(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(LAST_KNOWN_MAP_ZOOM, 5);
	}

	public static void setLastKnownMapZoom(Context ctx, int zoom) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit();
		edit.putInt(LAST_KNOWN_MAP_ZOOM, zoom);
		edit.commit();
	}

	public final static String POINT_NAVIGATE_LAT = "point_navigate_lat"; //$NON-NLS-1$
	public final static String POINT_NAVIGATE_LON = "point_navigate_lon"; //$NON-NLS-1$

	public static LatLon getPointToNavigate(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		float lat = prefs.getFloat(POINT_NAVIGATE_LAT, 0);
		float lon = prefs.getFloat(POINT_NAVIGATE_LON, 0);
		if (lat == 0 && lon == 0) {
			return null;
		}
		return new LatLon(lat, lon);
	}

	public static boolean clearPointToNavigate(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().remove(POINT_NAVIGATE_LAT).remove(POINT_NAVIGATE_LON).commit();
	}

	public static boolean setPointToNavigate(Context ctx, double latitude, double longitude) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putFloat(POINT_NAVIGATE_LAT, (float) latitude).putFloat(POINT_NAVIGATE_LON, (float) longitude).commit();
	}

	public static final String LAST_SEARCHED_REGION = "last_searched_region"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_CITY = "last_searched_city"; //$NON-NLS-1$
	public static final String lAST_SEARCHED_POSTCODE= "last_searched_postcode"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_STREET = "last_searched_street"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_BUILDING = "last_searched_building"; //$NON-NLS-1$
	public static final String LAST_SEARCHED_INTERSECTED_STREET = "last_searched_intersected_street"; //$NON-NLS-1$

	public static String getLastSearchedRegion(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(LAST_SEARCHED_REGION, ""); //$NON-NLS-1$
	}

	public static boolean setLastSearchedRegion(Context ctx, String region) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putString(LAST_SEARCHED_REGION, region).putLong(LAST_SEARCHED_CITY, -1).putString(LAST_SEARCHED_STREET,
				"").putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$ //$NON-NLS-2$
		if (prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}
	
	public static String getLastSearchedPostcode(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(lAST_SEARCHED_POSTCODE, null);	
	}
	
	public static boolean setLastSearchedPostcode(Context ctx, String postcode){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putLong(LAST_SEARCHED_CITY, -1).putString(LAST_SEARCHED_STREET, "").putString( //$NON-NLS-1$
				LAST_SEARCHED_BUILDING, "").putString(lAST_SEARCHED_POSTCODE, postcode); //$NON-NLS-1$
		if(prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)){
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}

	public static Long getLastSearchedCity(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getLong(LAST_SEARCHED_CITY, -1);
	}

	public static boolean setLastSearchedCity(Context ctx, Long cityId) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putLong(LAST_SEARCHED_CITY, cityId).putString(LAST_SEARCHED_STREET, "").putString( //$NON-NLS-1$
				LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
		edit.remove(lAST_SEARCHED_POSTCODE);
		if(prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)){
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}

	public static String getLastSearchedStreet(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(LAST_SEARCHED_STREET, ""); //$NON-NLS-1$
	}

	public static boolean setLastSearchedStreet(Context ctx, String street) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		Editor edit = prefs.edit().putString(LAST_SEARCHED_STREET, street).putString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
		if (prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			edit.putString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
		}
		return edit.commit();
	}

	public static String getLastSearchedBuilding(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(LAST_SEARCHED_BUILDING, ""); //$NON-NLS-1$
	}

	public static boolean setLastSearchedBuilding(Context ctx, String building) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(LAST_SEARCHED_BUILDING, building).remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
	}

	public static String getLastSearchedIntersectedStreet(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		if (!prefs.contains(LAST_SEARCHED_INTERSECTED_STREET)) {
			return null;
		}
		return prefs.getString(LAST_SEARCHED_INTERSECTED_STREET, ""); //$NON-NLS-1$
	}

	public static boolean setLastSearchedIntersectedStreet(Context ctx, String street) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(LAST_SEARCHED_INTERSECTED_STREET, street).commit();
	}

	public static boolean removeLastSearchedIntersectedStreet(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().remove(LAST_SEARCHED_INTERSECTED_STREET).commit();
	}

	public static final String SELECTED_POI_FILTER_FOR_MAP = "selected_poi_filter_for_map"; //$NON-NLS-1$

	public static boolean setPoiFilterForMap(Context ctx, String filterId) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putString(SELECTED_POI_FILTER_FOR_MAP, filterId).commit();
	}

	public static PoiFilter getPoiFilterForMap(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		String filterId = prefs.getString(SELECTED_POI_FILTER_FOR_MAP, null);
		PoiFilter filter = PoiFiltersHelper.getFilterById(ctx, filterId);
		if (filter != null) {
			return filter;
		}
		return new PoiFilter(null);
	}
	

	// this value string is synchronized with settings_pref.xml preference name
	public static final String VOICE_PROVIDER = "voice_provider"; //$NON-NLS-1$
	
	public static String getVoiceProvider(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(VOICE_PROVIDER, null);
	}
	
	public static final String VOICE_MUTE = "voice_mute"; //$NON-NLS-1$
	public static final boolean VOICE_MUTE_DEF = false;
	
	public static boolean isVoiceMute(Context ctx){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(VOICE_MUTE, VOICE_MUTE_DEF);
	}
	
	public static boolean setVoiceMute(Context ctx, boolean mute){
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(VOICE_MUTE, mute).commit();
	}
	
	// for background service
	public static final String MAP_ACTIVITY_ENABLED = "map_activity_enabled"; //$NON-NLS-1$
	public static final boolean MAP_ACTIVITY_ENABLED_DEF = false; 
	public static boolean getMapActivityEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(MAP_ACTIVITY_ENABLED, MAP_ACTIVITY_ENABLED_DEF);
	}
	
	public static boolean setMapActivityEnabled(Context ctx, boolean en) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(MAP_ACTIVITY_ENABLED, en).commit();
	}
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_ENABLED = "service_off_enabled"; //$NON-NLS-1$
	public static final boolean SERVICE_OFF_ENABLED_DEF = false; 
	public static boolean getServiceOffEnabled(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getBoolean(SERVICE_OFF_ENABLED, SERVICE_OFF_ENABLED_DEF);
	}
	
	public static boolean setServiceOffEnabled(Context ctx, boolean en) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.edit().putBoolean(SERVICE_OFF_ENABLED, en).commit();
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_PROVIDER = "service_off_provider"; //$NON-NLS-1$
	public static String getServiceOffProvider(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getString(SERVICE_OFF_PROVIDER, LocationManager.GPS_PROVIDER);
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_INTERVAL = "service_off_interval"; //$NON-NLS-1$
	public static final int SERVICE_OFF_INTERVAL_DEF = 5 * 60 * 1000;
	public static int getServiceOffInterval(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(SERVICE_OFF_INTERVAL, SERVICE_OFF_INTERVAL_DEF);
	}
	
	
	// this value string is synchronized with settings_pref.xml preference name
	public static final String SERVICE_OFF_ERROR_INTERVAL = "service_off_error_interval"; //$NON-NLS-1$
	public static final int SERVICE_OFF_ERROR_INTERVAL_DEF = 60 * 1000;
	public static int getServiceOffErrorInterval(Context ctx) {
		SharedPreferences prefs = ctx.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_WORLD_READABLE);
		return prefs.getInt(SERVICE_OFF_ERROR_INTERVAL, SERVICE_OFF_ERROR_INTERVAL_DEF);
	}
	
	
	
	
}
