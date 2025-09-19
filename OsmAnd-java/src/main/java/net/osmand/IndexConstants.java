package net.osmand;


public class IndexConstants {

	// Important : Every time you change schema of db upgrade version!!! 
	// If you want that new application support old index : put upgrade code in android app ResourceManager
	public static final int POI_TABLE_VERSION = 2;
	public static final int BINARY_MAP_VERSION = 2; // starts with 1
	public static final int VOICE_VERSION = 0; //supported download versions
	public static final int TTSVOICE_VERSION = 1; //supported download versions

	public static final String SQLITE_EXT = ".sqlitedb";
	public static final String TEMP_SOURCE_TO_LOAD = "temp";

	public static final String POI_INDEX_EXT = ".poi.odb";

	public static final String ZIP_EXT = ".zip";
	public static final String BINARY_MAP_INDEX_EXT = ".obf";
	public static final String BINARY_MAP_INDEX_EXT_ZIP = ".obf.zip";

	public static final String BINARY_WIKIVOYAGE_MAP_INDEX_EXT = ".sqlite";
	public static final String BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT = ".travel.obf";
	public static final String BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT_ZIP = ".travel.obf.zip";
	public static final String BINARY_WIKI_MAP_INDEX_EXT = ".wiki.obf";
	public static final String BINARY_WIKI_MAP_INDEX_EXT_ZIP = ".wiki.obf.zip";
	public static final String BINARY_ROAD_MAP_INDEX_EXT = ".road.obf";
	public static final String BINARY_ROAD_MAP_INDEX_EXT_ZIP = ".road.obf.zip";
	public static final String BINARY_SRTM_MAP_INDEX_EXT = ".srtm.obf";
	public static final String BINARY_SRTM_FEET_MAP_INDEX_EXT = ".srtmf.obf";
	public static final String BINARY_SRTM_MAP_INDEX_EXT_ZIP = ".srtm.obf.zip";
	public static final String BINARY_SRTM_FEET_MAP_INDEX_EXT_ZIP = ".srtmf.obf.zip";
	public static final String BINARY_DEPTH_MAP_INDEX_EXT = ".depth.obf";
	public static final String BINARY_DEPTH_MAP_INDEX_EXT_ZIP = ".depth.obf.zip";
	public static final String EXTRA_EXT = ".extra";
	public static final String EXTRA_ZIP_EXT = ".extra.zip";
	public static final String OSM_GZ_EXT = ".osm.gz";
	public static final String HTML_EXT = ".html";
	public static final String GEN_LOG_EXT = ".gen.log";
	public static final String DOWNLOAD_EXT = ".download";
	public static final String TIF_EXT = ".tif";
	public static final String TIFF_DB_EXT = ".tiff.db";
	public static final String WEATHER_EXT = ".tifsqlite";
	public static final String WEATHER_MAP_INDEX_EXT = ".tifsqlite.zip";

	public static final String VOICE_INDEX_EXT_ZIP = ".voice.zip";
	public static final String TTSVOICE_INDEX_EXT_JS = "tts.js";
	public static final String ANYVOICE_INDEX_EXT_ZIP = "voice.zip";  //to cactch both voices, .voice.zip and .ttsvoice.zip

	public static final String FONT_INDEX_EXT = ".otf";
	public static final String FONT_INDEX_EXT_ZIP = ".otf.zip";

	public static final String OSMAND_SETTINGS_FILE_EXT = ".osf";

	public static final String ROUTING_FILE_EXT = ".xml";

	public static final String RENDERER_INDEX_EXT = ".render.xml";
	public static final String ADDON_RENDERER_INDEX_EXT = ".addon" + RENDERER_INDEX_EXT;

	public static final String GPX_FILE_EXT = ".gpx";
	public static final String GPX_GZ_FILE_EXT = ".gpx.gz";

	public static final String WPT_CHART_FILE_EXT = ".wpt.chart";
	public static final String SQLITE_CHART_FILE_EXT = ".3d.chart";
	public static final String HELP_ARTICLE_FILE_EXT = ".mht";

	public static final String AVOID_ROADS_FILE_EXT = ".geojson";

	public static final String OBJ_FILE_EXT = ".obj";
	public static final String TXT_EXT = ".txt";

	public static final String POI_TABLE = "poi";

	public static final String INDEX_DOWNLOAD_DOMAIN = "download.osmand.net";
	public static final String APP_DIR = "osmand/";
	public static final String MAPS_PATH = "";
	public static final String HIDDEN_DIR = "hidden/";
	public static final String BACKUP_INDEX_DIR = "backup/";
	public static final String HIDDEN_BACKUP_DIR = HIDDEN_DIR + BACKUP_INDEX_DIR;
	public static final String GPX_INDEX_DIR = "tracks/";
	public static final String FAVORITES_INDEX_DIR = "favorites/";
	public static final String MAP_MARKERS_INDEX_DIR = "/map markers";
	public static final String GPX_RECORDED_INDEX_DIR = GPX_INDEX_DIR + "rec/";
	public static final String GPX_IMPORT_DIR = GPX_INDEX_DIR + "import/";
	public static final String TILES_INDEX_DIR = "tiles/";
	public static final String LIVE_INDEX_DIR = "live/";
	public static final String TOURS_INDEX_DIR = "tours/";
	public static final String SRTM_INDEX_DIR = "srtm/";
	public static final String NAUTICAL_INDEX_DIR = "nautical/";
	public static final String ROADS_INDEX_DIR = "roads/";
	public static final String WIKI_INDEX_DIR = "wiki/";
	public static final String HELP_INDEX_DIR = "help/";
	public static final String ARTICLES_DIR = HELP_INDEX_DIR + "articles/";
	public static final String WIKIVOYAGE_INDEX_DIR = "travel/";
	public static final String GPX_TRAVEL_DIR = GPX_INDEX_DIR + WIKIVOYAGE_INDEX_DIR;
	public static final String AV_INDEX_DIR = "avnotes/";
	public static final String FONT_INDEX_DIR = "fonts/";
	public static final String VOICE_INDEX_DIR = "voice/";
	public static final String RENDERERS_DIR = "rendering/";
	public static final String ROUTING_XML_FILE = "routing.xml";
	public static final String SETTINGS_DIR = "settings/";
	public static final String TEMP_DIR = "temp/";
	public static final String ROUTING_PROFILES_DIR = "routing/";
	public static final String PLUGINS_DIR = "plugins/";
	public static final String GEOTIFF_SQLITE_CACHE_DIR = "geotiff_sqlite_cache/";
	public static final String GEOTIFF_DIR = "geotiff/";
	public static final String CLR_PALETTE_DIR = "color-palette/";
	public static final String WEATHER_FORECAST_DIR = "weather_forecast/";
	public static final String MODEL_3D_DIR = "models/";
	public static final String OPENGL_SHADERS_CACHE_DIR = "opengl_shaders_cache/";

	public static final String VOICE_PROVIDER_SUFFIX = "-tts";
	public static final String MODEL_NAME_PREFIX = "model_";
	public static final String COLOR_PALETTE_DIR = "color-palette/";
}
