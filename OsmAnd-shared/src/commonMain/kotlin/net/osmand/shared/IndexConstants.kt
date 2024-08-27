package net.osmand.shared

object IndexConstants {

	const val SQLITE_EXT = ".sqlitedb"

	const val POI_INDEX_EXT = ".poi.odb"

	const val ZIP_EXT = ".zip"
	const val BINARY_MAP_INDEX_EXT = ".obf"
	const val BINARY_MAP_INDEX_EXT_ZIP = ".obf.zip"

	const val BINARY_WIKIVOYAGE_MAP_INDEX_EXT = ".sqlite"
	const val BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT = ".travel.obf"
	const val BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT_ZIP = ".travel.obf.zip"
	const val BINARY_WIKI_MAP_INDEX_EXT = ".wiki.obf"
	const val BINARY_WIKI_MAP_INDEX_EXT_ZIP = ".wiki.obf.zip"
	const val BINARY_ROAD_MAP_INDEX_EXT = ".road.obf"
	const val BINARY_ROAD_MAP_INDEX_EXT_ZIP = ".road.obf.zip"
	const val BINARY_SRTM_MAP_INDEX_EXT = ".srtm.obf"
	const val BINARY_SRTM_FEET_MAP_INDEX_EXT = ".srtmf.obf"
	const val BINARY_SRTM_MAP_INDEX_EXT_ZIP = ".srtm.obf.zip"
	const val BINARY_SRTM_FEET_MAP_INDEX_EXT_ZIP = ".srtmf.obf.zip"
	const val BINARY_DEPTH_MAP_INDEX_EXT = ".depth.obf"
	const val BINARY_DEPTH_MAP_INDEX_EXT_ZIP = ".depth.obf.zip"
	const val EXTRA_EXT = ".extra"
	const val EXTRA_ZIP_EXT = ".extra.zip"
	const val OSM_GZ_EXT = ".osm.gz"
	const val HTML_EXT = ".html"
	const val GEN_LOG_EXT = ".gen.log"
	const val DOWNLOAD_EXT = ".download"
	const val TIF_EXT = ".tif"
	const val TIFF_DB_EXT = ".tiff.db"
	const val WEATHER_EXT = ".tifsqlite"
	const val WEATHER_MAP_INDEX_EXT = ".tifsqlite.zip"

	const val VOICE_INDEX_EXT_ZIP = ".voice.zip"
	const val TTSVOICE_INDEX_EXT_JS = "tts.js"
	const val ANYVOICE_INDEX_EXT_ZIP = "voice.zip" // to catch both voices, .voice.zip and .ttsvoice.zip

	const val FONT_INDEX_EXT = ".otf"
	const val FONT_INDEX_EXT_ZIP = ".otf.zip"

	const val OSMAND_SETTINGS_FILE_EXT = ".osf"

	const val ROUTING_FILE_EXT = ".xml"

	const val RENDERER_INDEX_EXT = ".render.xml"
	const val ADDON_RENDERER_INDEX_EXT = ".addon$RENDERER_INDEX_EXT"

	const val GPX_FILE_EXT = ".gpx"
	const val GPX_GZ_FILE_EXT = ".gpx.gz"

	const val WPT_CHART_FILE_EXT = ".wpt.chart"
	const val SQLITE_CHART_FILE_EXT = ".3d.chart"
	const val HELP_ARTICLE_FILE_EXT = ".mht"

	const val AVOID_ROADS_FILE_EXT = ".geojson"

	const val OBJ_FILE_EXT = ".obj"

	const val POI_TABLE = "poi"

	const val INDEX_DOWNLOAD_DOMAIN = "download.osmand.net"
	const val APP_DIR = "osmand/"
	const val MAPS_PATH = ""
	const val HIDDEN_DIR = "hidden/"
	const val BACKUP_INDEX_DIR = "backup/"
	const val HIDDEN_BACKUP_DIR = HIDDEN_DIR + BACKUP_INDEX_DIR
	const val GPX_INDEX_DIR = "tracks/"
	const val FAVORITES_INDEX_DIR = "favorites/"
	const val MAP_MARKERS_INDEX_DIR = "/map markers"
	const val GPX_RECORDED_INDEX_DIR = GPX_INDEX_DIR + "rec/"
	const val GPX_IMPORT_DIR = GPX_INDEX_DIR + "import/"
	const val TILES_INDEX_DIR = "tiles/"
	const val LIVE_INDEX_DIR = "live/"
	const val TOURS_INDEX_DIR = "tours/"
	const val SRTM_INDEX_DIR = "srtm/"
	const val NAUTICAL_INDEX_DIR = "nautical/"
	const val ROADS_INDEX_DIR = "roads/"
	const val WIKI_INDEX_DIR = "wiki/"
	const val HELP_INDEX_DIR = "help/"
	const val ARTICLES_DIR = HELP_INDEX_DIR + "articles/"
	const val WIKIVOYAGE_INDEX_DIR = "travel/"
	const val GPX_TRAVEL_DIR = GPX_INDEX_DIR + WIKIVOYAGE_INDEX_DIR
	const val AV_INDEX_DIR = "avnotes/"
	const val FONT_INDEX_DIR = "fonts/"
	const val VOICE_INDEX_DIR = "voice/"
	const val RENDERERS_DIR = "rendering/"
	const val ROUTING_XML_FILE = "routing.xml"
	const val SETTINGS_DIR = "settings/"
	const val TEMP_DIR = "temp/"
	const val ROUTING_PROFILES_DIR = "routing/"
	const val PLUGINS_DIR = "plugins/"
	const val GEOTIFF_SQLITE_CACHE_DIR = "geotiff_sqlite_cache/"
	const val GEOTIFF_DIR = "geotiff/"
	const val CLR_PALETTE_DIR = "color-palette/"
	const val WEATHER_FORECAST_DIR = "weather_forecast/"
	const val MODEL_3D_DIR = "models/"

	const val VOICE_PROVIDER_SUFFIX = "-tts"
	const val MODEL_NAME_PREFIX = "model_"
	const val COLOR_PALETTE_DIR = "color-palette/"


}
