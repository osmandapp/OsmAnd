package net.osmand.plus.download;

import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.INDEX_DOWNLOAD_DOMAIN;
import static net.osmand.IndexConstants.WEATHER_FORECAST_DIR;
import static net.osmand.IndexConstants.WEATHER_MAP_INDEX_EXT;
import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.download.DownloadResourceGroupType.NAUTICAL_DEPTH_HEADER;
import static net.osmand.plus.download.DownloadResourceGroupType.NAUTICAL_POINTS_HEADER;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DownloadActivityType {

	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
	private static final Map<String, DownloadActivityType> byTag = new HashMap<>();

	public static final DownloadActivityType NORMAL_FILE =
			new DownloadActivityType(R.string.download_regular_maps, "map", 10);
	public static final DownloadActivityType VOICE_FILE =
			new DownloadActivityType(R.string.voices, R.drawable.ic_action_volume_up, "voice", 20);
	public static final DownloadActivityType FONT_FILE =
			new DownloadActivityType(R.string.fonts_header, R.drawable.ic_action_map_language, "fonts", 25);
	public static final DownloadActivityType ROADS_FILE =
			new DownloadActivityType(R.string.download_roads_only_maps, "road_map", 30);
	public static final DownloadActivityType SRTM_COUNTRY_FILE =
			new DownloadActivityType(R.string.download_srtm_maps, R.drawable.ic_plugin_srtm, "srtm_map", 40);
	public static final DownloadActivityType DEPTH_CONTOUR_FILE =
			new DownloadActivityType(R.string.download_regular_maps, "depth", 45);
	public static final DownloadActivityType DEPTH_MAP_FILE =
			new DownloadActivityType(R.string.nautical_depth, "depthmap", 46);
	public static final DownloadActivityType HILLSHADE_FILE =
			new DownloadActivityType(R.string.download_hillshade_maps, R.drawable.ic_action_hillshade_dark, "hillshade", 50);
	public static final DownloadActivityType SLOPE_FILE =
			new DownloadActivityType(R.string.download_slope_maps, R.drawable.ic_action_hillshade_dark, "slope", 55);
	public static final DownloadActivityType WIKIPEDIA_FILE =
			new DownloadActivityType(R.string.download_wikipedia_maps, R.drawable.ic_plugin_wikipedia, "wikimap", 60);
	public static final DownloadActivityType WIKIVOYAGE_FILE =
			new DownloadActivityType(R.string.shared_string_wikivoyage, R.drawable.ic_plugin_wikipedia, "wikivoyage", 65);
	public static final DownloadActivityType TRAVEL_FILE =
			new DownloadActivityType(R.string.shared_string_wikivoyage, R.drawable.ic_plugin_wikipedia, "travel", 66);
	public static final DownloadActivityType LIVE_UPDATES_FILE =
			new DownloadActivityType(R.string.download_live_updates, "live_updates", 70);
	public static final DownloadActivityType GPX_FILE =
			new DownloadActivityType(R.string.shared_string_gpx_tracks, R.drawable.ic_action_polygom_dark, "gpx", 75);
	public static final DownloadActivityType SQLITE_FILE =
			new DownloadActivityType(R.string.shared_string_online_maps, "sqlite", 80);
	public static final DownloadActivityType WEATHER_FORECAST =
			new DownloadActivityType(R.string.weather_forecast, R.drawable.ic_action_umbrella, "weather", 90);
	public static final DownloadActivityType GEOTIFF_FILE =
			new DownloadActivityType(R.string.terrain_map, R.drawable.ic_action_terrain, "geotiff", 85);

	private final int stringResource;
	private final int iconResource;

	private final String tag;
	private final int orderIndex;

	public DownloadActivityType(int stringResource, int iconResource, String tag, int orderIndex) {
		this.stringResource = stringResource;
		this.tag = tag;
		this.orderIndex = orderIndex;
		byTag.put(tag, this);
		this.iconResource = iconResource;
	}

	public DownloadActivityType(int stringResource, String tag, int orderIndex) {
		this.stringResource = stringResource;
		this.tag = tag;
		this.orderIndex = orderIndex;
		byTag.put(tag, this);
		iconResource = R.drawable.ic_map;
	}

	public int getStringResource() {
		return stringResource;
	}

	public int getIconResource() {
		return iconResource;
	}

	public String getTag() {
		return tag;
	}

	public int getOrderIndex() {
		return orderIndex;
	}

	public static boolean isCountedInDownloads(@NonNull IndexItem item) {
		DownloadActivityType type = item.getType();
		if (type == NORMAL_FILE || type == ROADS_FILE) {
			return !item.extra && !item.free;
		}
		return false;
	}

	public String getString(Context c) {
		return c.getString(stringResource);
	}

	public static DownloadActivityType getIndexType(String tagName) {
		return byTag.get(tagName);
	}

	public static Collection<DownloadActivityType> values() {
		return byTag.values();
	}

	protected static String addVersionToExt(String ext, int version) {
		return "_" + version + ext;
	}

	public boolean isAccepted(String fileName) {
		if (NORMAL_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION))
					|| fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)
					|| fileName.endsWith(IndexConstants.SQLITE_EXT);
		} else if (ROADS_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION));
		} else if (VOICE_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.VOICE_INDEX_EXT_ZIP, IndexConstants.VOICE_VERSION));
		} else if (FONT_FILE == this) {
			return fileName.endsWith(IndexConstants.FONT_INDEX_EXT_ZIP);
		} else if (WIKIPEDIA_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT_ZIP,
					IndexConstants.BINARY_MAP_VERSION));
		} else if (WIKIVOYAGE_FILE == this) {
			return fileName.endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT);
		} else if (TRAVEL_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT_ZIP,
					IndexConstants.BINARY_MAP_VERSION));
		} else if (SRTM_COUNTRY_FILE == this) {
			boolean srtm = fileName.endsWith(addVersionToExt(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION));
			boolean srtmf = fileName.endsWith(addVersionToExt(IndexConstants.BINARY_SRTM_FEET_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION));
			return srtm || srtmf;
		} else if (HILLSHADE_FILE == this) {
			return fileName.endsWith(IndexConstants.SQLITE_EXT);
		} else if (SLOPE_FILE == this) {
			return fileName.endsWith(IndexConstants.SQLITE_EXT);
		} else if (GEOTIFF_FILE == this) {
			return fileName.endsWith(IndexConstants.TIF_EXT);
		} else if (DEPTH_CONTOUR_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION));
		} else if (GPX_FILE == this) {
			return fileName.endsWith(IndexConstants.GPX_FILE_EXT);
		} else if (SQLITE_FILE == this) {
			return fileName.endsWith(IndexConstants.SQLITE_EXT);
		} else if (DEPTH_MAP_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION));
		} else if (WEATHER_FORECAST == this) {
			return fileName.endsWith(WEATHER_MAP_INDEX_EXT);
		}
		return false;
	}

	public File getDownloadFolder(OsmandApplication app, IndexItem indexItem) {
		if (indexItem.hidden) {
			return app.getAppInternalPath(IndexConstants.HIDDEN_DIR);
		} else {
			return getDefaultDownloadFolder(app, indexItem);
		}
	}

	@NonNull
	public File getDefaultDownloadFolder(OsmandApplication app, IndexItem indexItem) {
		if (NORMAL_FILE == this) {
			if (indexItem.fileName.endsWith(IndexConstants.SQLITE_EXT)) {
				return app.getAppPath(IndexConstants.TILES_INDEX_DIR);
			}
			return app.getAppPath(IndexConstants.MAPS_PATH);
		} else if (VOICE_FILE == this) {
			return app.getAppPath(IndexConstants.VOICE_INDEX_DIR);
		} else if (FONT_FILE == this) {
			return app.getAppPath(IndexConstants.FONT_INDEX_DIR);
		} else if (ROADS_FILE == this) {
			return app.getAppPath(IndexConstants.ROADS_INDEX_DIR);
		} else if (SRTM_COUNTRY_FILE == this) {
			return app.getAppPath(IndexConstants.SRTM_INDEX_DIR);
		} else if (WIKIPEDIA_FILE == this) {
			return app.getAppPath(IndexConstants.WIKI_INDEX_DIR);
		} else if (WIKIVOYAGE_FILE == this || TRAVEL_FILE == this) {
			return app.getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR);
		} else if (LIVE_UPDATES_FILE == this) {
			return app.getAppPath(IndexConstants.LIVE_INDEX_DIR);
		} else if (HILLSHADE_FILE == this) {
			return app.getAppPath(IndexConstants.TILES_INDEX_DIR);
		} else if (SLOPE_FILE == this) {
			return app.getAppPath(IndexConstants.TILES_INDEX_DIR);
		} else if (GEOTIFF_FILE == this) {
			return app.getAppPath(IndexConstants.GEOTIFF_DIR);
		} else if (DEPTH_CONTOUR_FILE == this) {
			return app.getAppPath(IndexConstants.MAPS_PATH);
		} else if (GPX_FILE == this) {
			return app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		} else if (SQLITE_FILE == this) {
			return app.getAppPath(IndexConstants.TILES_INDEX_DIR);
		} else if (DEPTH_MAP_FILE == this) {
			return app.getAppPath(IndexConstants.NAUTICAL_INDEX_DIR);
		} else if (WEATHER_FORECAST == this) {
			return app.getAppPath(WEATHER_FORECAST_DIR);
		}
		throw new UnsupportedOperationException();
	}

	public boolean isZipStream() {
		return HILLSHADE_FILE != this
				&& SLOPE_FILE != this
				&& GEOTIFF_FILE != this
				&& SQLITE_FILE != this
				&& WIKIVOYAGE_FILE != this
				&& GPX_FILE != this;
	}

	public boolean isZipFolder() {
		return this == VOICE_FILE;
	}

	public boolean preventMediaIndexing(OsmandApplication ctx, IndexItem indexItem) {
		return this == VOICE_FILE && indexItem.fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP);
	}

	public String getUnzipExtension(OsmandApplication ctx, IndexItem indexItem) {
		if (NORMAL_FILE == this) {
			if (indexItem.fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
				return BINARY_MAP_INDEX_EXT;
			} else if (indexItem.fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
				return BINARY_MAP_INDEX_EXT;
			} else if (indexItem.fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
				return IndexConstants.EXTRA_EXT;
			} else if (indexItem.fileName.endsWith(IndexConstants.SQLITE_EXT)) {
				return IndexConstants.SQLITE_EXT;
			} else if (indexItem.fileName.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)) {
				return "";
			}
		} else if (ROADS_FILE == this) {
			return IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
		} else if (VOICE_FILE == this) {
			return "";
		} else if (FONT_FILE == this) {
			return IndexConstants.FONT_INDEX_EXT;
		} else if (SRTM_COUNTRY_FILE == this) {
			return SrtmDownloadItem.getExtension(indexItem);
		} else if (WIKIPEDIA_FILE == this) {
			return IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
		} else if (WIKIVOYAGE_FILE == this) {
			return IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT;
		} else if (TRAVEL_FILE == this) {
			return IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
		} else if (LIVE_UPDATES_FILE == this) {
			return BINARY_MAP_INDEX_EXT;
		} else if (HILLSHADE_FILE == this) {
			return IndexConstants.SQLITE_EXT;
		} else if (SLOPE_FILE == this) {
			return IndexConstants.SQLITE_EXT;
		} else if (SQLITE_FILE == this) {
			return IndexConstants.SQLITE_EXT;
		} else if (GEOTIFF_FILE == this) {
			return IndexConstants.TIF_EXT;
		} else if (DEPTH_CONTOUR_FILE == this) {
			return IndexConstants.BINARY_MAP_INDEX_EXT;
		} else if (GPX_FILE == this) {
			return IndexConstants.GPX_FILE_EXT;
		} else if (DEPTH_MAP_FILE == this) {
			return IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT;
		} else if (WEATHER_FORECAST == this) {
			return IndexConstants.WEATHER_EXT;
		}
		throw new UnsupportedOperationException();
	}

	public String getUrlSuffix(OsmandApplication ctx) {
		if (this == ROADS_FILE) {
			return "&road=yes";
		} else if (this == LIVE_UPDATES_FILE) {
			return "&aosmc=yes";
		} else if (this == SRTM_COUNTRY_FILE) {
			return "&srtmcountry=yes";
		} else if (this == WIKIPEDIA_FILE) {
			return "&wiki=yes";
		} else if (this == WIKIVOYAGE_FILE) {
			return "&wikivoyage=yes";
		} else if (this == TRAVEL_FILE) {
			return "&wikivoyage=yes";
		} else if (this == HILLSHADE_FILE) {
			return "&hillshade=yes";
		} else if (this == SLOPE_FILE) {
			return "&slope=yes";
		} else if (this == FONT_FILE) {
			return "&fonts=yes";
		} else if (this == DEPTH_CONTOUR_FILE) {
			return "&inapp=depth";
		} else if (this == GPX_FILE) {
			return "&gpx=yes";
		} else if (this == GEOTIFF_FILE) {
			return "&heightmap=yes";
		} else if (this == DEPTH_MAP_FILE) {
			return "&depth=yes";
		} else if (this == WEATHER_FORECAST) {
			return "&weather=yes";
		}
		return "";
	}

	public String getBaseUrl(OsmandApplication ctx, String fileName) {
		String url = AndroidNetworkUtils.getHttpProtocol() + INDEX_DOWNLOAD_DOMAIN + "/download?event=2&"
				+ Version.getVersionAsURLParam(ctx) + "&file=" + encode(fileName);
		if (this == LIVE_UPDATES_FILE && fileName.length() > 16) {
			// DATE_AND_EXT_STR_LEN = "_18_06_02.obf.gz".length()
			String region = fileName.substring(0, fileName.length() - 16).toLowerCase();
			url += "&region=" + encode(region);
		}
		return url;
	}

	protected String encode(String fileName) {
		try {
			return URLEncoder.encode(fileName, "UTF-8");
		} catch (IOException e) {
			return fileName;
		}
	}

	@Nullable
	public IndexItem parseIndexItem(@NonNull OsmandApplication app, @NonNull XmlPullParser parser) {
		String name = parser.getAttributeValue(null, "name");
		if (!isAccepted(name)) {
			return null;
		}
		String size = parser.getAttributeValue(null, "size");
		boolean isHidden = Boolean.parseBoolean(parser.getAttributeValue(null, "isHidden"));
		String description = parser.getAttributeValue(null, "description");
		long containerSize = Algorithms.parseLongSilently(parser.getAttributeValue(null, "containerSize"), 0);
		long contentSize = Algorithms.parseLongSilently(parser.getAttributeValue(null, "contentSize"), 0);
		long timestamp = Algorithms.parseLongSilently(parser.getAttributeValue(null, "timestamp"), 0);
		boolean free = Boolean.parseBoolean(parser.getAttributeValue(null, "free"));
		String freeMessage = parser.getAttributeValue(null, "freeMessage");
		IndexItem item = new IndexItem(name, description, timestamp, size, contentSize, containerSize, this, free, freeMessage, isHidden);
		item.extra = FileNameTranslationHelper.getStandardMapName(app, item.getBasename().toLowerCase()) != null;
		return item;
	}

	protected static String reparseDate(Context ctx, String date) {
		try {
			Date d = simpleDateFormat.parse(date);
			return AndroidUtils.formatDate(ctx, d.getTime());
		} catch (ParseException e) {
			return date;
		}
	}

	public String getVisibleDescription(DownloadItem downloadItem, Context ctx) {
		if (this == SRTM_COUNTRY_FILE) {
			return ctx.getString(R.string.download_srtm_maps);
		} else if (this == WIKIPEDIA_FILE) {
			return ctx.getString(R.string.shared_string_wikipedia);
		} else if (this == WIKIVOYAGE_FILE) {
			return ctx.getString(R.string.shared_string_wikivoyage);
		} else if (this == ROADS_FILE) {
			return ctx.getString(R.string.download_roads_only_item);
		} else if (this == DEPTH_CONTOUR_FILE) {
			return ctx.getString(R.string.download_depth_countours);
		} else if (this == FONT_FILE) {
			return ctx.getString(R.string.fonts_header);
		} else if (this == GPX_FILE) {
			return ctx.getString(R.string.shared_string_gpx_tracks);
		} else if (this == DEPTH_MAP_FILE) {
			return ctx.getString(R.string.download_depth_countours);
		} else if (this == WEATHER_FORECAST) {
			return ctx.getString(R.string.shared_string_weather);
		}
		return "";
	}

	public String getVisibleName(DownloadItem downloadItem, Context ctx, OsmandRegions osmandRegions, boolean includeParent, boolean useShortName) {
		if (this == VOICE_FILE) {
			if (isVoiceTTS(downloadItem) || isVoiceRec(downloadItem)) {
				return FileNameTranslationHelper.getVoiceName(ctx, getBasename(downloadItem));
			}
			return getBasename(downloadItem);
		}
		if (this == FONT_FILE) {
			return FileNameTranslationHelper.getFontName(getBasename(downloadItem));
		}
		if (this == WEATHER_FORECAST) {
			return FileNameTranslationHelper.getFileName(ctx, osmandRegions, downloadItem.getFileName());
		}
		String basename = getBasename(downloadItem);
		if (basename.endsWith(FileNameTranslationHelper.WIKI_NAME)) {
			return FileNameTranslationHelper.getWikiName(ctx, basename);
		}
		if (basename.endsWith(FileNameTranslationHelper.WIKIVOYAGE_NAME)) {
			return FileNameTranslationHelper.getWikivoyageName(ctx, basename);
		}
//		if (this == HILLSHADE_FILE){
//			return FileNameTranslationHelper.getHillShadeName(ctx, osmandRegions, bn);
//		}
		String lc = basename.toLowerCase();
		DownloadResourceGroup relatedGroup = downloadItem.getRelatedGroup();
		if (relatedGroup != null) {
			DownloadResourceGroupType type = relatedGroup.getType();
			if (type == NAUTICAL_POINTS_HEADER || type == NAUTICAL_DEPTH_HEADER) {
				String contoursPrefix = "_contours";
				String pointsPrefix = "_points";
				String prefix = basename.contains(pointsPrefix) ? pointsPrefix : contoursPrefix;
				int endInd = basename.indexOf(prefix);
				String downloadName = basename.substring(0, endInd).replace('_', ' ');
				String name = Algorithms.capitalizeFirstLetter(downloadName);
				if (useShortName) {
					return name;
				} else if (type == NAUTICAL_DEPTH_HEADER) {
					return ctx.getString(R.string.download_depth_countours) + " " + name;
				}
			}
		}
		String std = FileNameTranslationHelper.getStandardMapName(ctx, lc);
		if (std != null) {
			return std;
		}
		if (basename.contains("addresses-nationwide")) {
			int ind = basename.indexOf("addresses-nationwide");
			String downloadName = basename.substring(0, ind - 1) + basename.substring(ind + "addresses-nationwide".length());
			return osmandRegions.getLocaleName(downloadName, includeParent) +
					" " + ctx.getString(R.string.index_item_nation_addresses);
		}

		return osmandRegions.getLocaleName(basename, includeParent);
	}

	public String getTargetFileName(IndexItem item) {
		String fileName = item.fileName;
		// if(fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP) ||
		// fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)) {
		if (this == VOICE_FILE) {
			if (fileName.contains(".js")) {
				return fileName.replace('_', '-').replaceAll(".js", "");
			}
			int l = fileName.lastIndexOf('_');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.substring(0, l);
		} else if (this == FONT_FILE) {
			int l = fileName.indexOf('.');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.substring(0, l) + IndexConstants.FONT_INDEX_EXT;
		} else if (this == HILLSHADE_FILE) {
			return fileName.replace('_', ' ');
		} else if (this == SLOPE_FILE) {
			return fileName.replace('_', ' ');
		} else if (this == GEOTIFF_FILE) {
			return fileName.replace('_', ' ');
		} else if (this == SQLITE_FILE) {
			return fileName.replace('_', ' ');
		} else if (this == LIVE_UPDATES_FILE) {
			int l = fileName.lastIndexOf('.');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.substring(0, l) + IndexConstants.BINARY_MAP_INDEX_EXT;
		} else if (this == WEATHER_FORECAST) {
			return fileName.substring(0, fileName.length() - ZIP_EXT.length())
					.replace(FileNameTranslationHelper.WEATHER + "_", "");
		} else if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)
				|| fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
			int l = fileName.lastIndexOf('_');
			if (l == -1) {
				l = fileName.length();
			}
			String baseNameWithoutVersion = fileName.substring(0, l);
			if (this == SRTM_COUNTRY_FILE) {
				return baseNameWithoutVersion + SrtmDownloadItem.getExtension(item);
			}
			if (this == WIKIPEDIA_FILE) {
				return baseNameWithoutVersion + IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
			}
			if (this == WIKIVOYAGE_FILE) {
				return baseNameWithoutVersion + IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT;
			}
			if (this == TRAVEL_FILE) {
				return baseNameWithoutVersion + IndexConstants.BINARY_TRAVEL_GUIDE_MAP_INDEX_EXT;
			}
			if (this == ROADS_FILE) {
				return baseNameWithoutVersion + IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
			}
			if (this == DEPTH_MAP_FILE) {
				return baseNameWithoutVersion + IndexConstants.BINARY_DEPTH_MAP_INDEX_EXT;
			}
			baseNameWithoutVersion += IndexConstants.BINARY_MAP_INDEX_EXT;
			return baseNameWithoutVersion;
		} else if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			return fileName.replace('_', ' ');
		} else if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.EXTRA_ZIP_EXT.length())
					+ IndexConstants.EXTRA_EXT;
		}
		return fileName;
	}

	@NonNull
	public String getBasename(@NonNull DownloadItem downloadItem) {
		String fileName = downloadItem.getFileName();
		if (Algorithms.isEmpty(fileName)) return fileName;

		if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.EXTRA_ZIP_EXT.length());
		}
		if (this == HILLSHADE_FILE) {
			return fileName.substring(0, fileName.length() - IndexConstants.SQLITE_EXT.length())
					.replace(FileNameTranslationHelper.HILL_SHADE + "_", "");
		}
		if (this == SLOPE_FILE) {
			return fileName.substring(0, fileName.length() - IndexConstants.SQLITE_EXT.length())
					.replace(FileNameTranslationHelper.SLOPE + "_", "");
		}
		if (this == GEOTIFF_FILE) {
			return fileName.substring(0, fileName.length() - IndexConstants.TIF_EXT.length())
					.replace(FileNameTranslationHelper.HEIGHTMAP + "_", "");
		}
		if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.SQLITE_EXT.length());
		}
		if (downloadItem.getType() == WIKIVOYAGE_FILE &&
				fileName.endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT.length());
		}
		if (this == VOICE_FILE) {
			int l = fileName.lastIndexOf('_');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_JS) ? fileName.replace('_', '-')
					.replaceAll(".js", "") : fileName.substring(0, l);
		}
		if (this == FONT_FILE) {
			int l = fileName.indexOf('.');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.substring(0, l);
		}
		if (this == LIVE_UPDATES_FILE) {
			if (fileName.indexOf('.') > 0) {
				return fileName.substring(0, fileName.indexOf('.'));
			}
			return fileName;
		}
		if (this == WEATHER_FORECAST) {
			return fileName.substring(0, fileName.length() - WEATHER_MAP_INDEX_EXT.length())
					.replace(FileNameTranslationHelper.WEATHER + "_", "");
		}
		int ls = fileName.lastIndexOf('_');
		if (ls >= 0) {
			return fileName.substring(0, ls);
		} else if (fileName.indexOf('.') > 0) {
			return fileName.substring(0, fileName.indexOf('.'));
		}
		return fileName;
	}

	public static boolean isVoiceTTS(@NonNull DownloadItem downloadItem) {
		String fileName = downloadItem.getFileName();
		return !Algorithms.isEmpty(fileName) && fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_JS);
	}

	public static boolean isVoiceRec(@NonNull DownloadItem downloadItem) {
		String fileName = downloadItem.getFileName();
		return !Algorithms.isEmpty(fileName) && fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP);
	}

	public static boolean isDefaultVoiceTTS(@NonNull OsmandApplication app, @NonNull DownloadItem item) {
		return isVoiceTTS(item) && Algorithms.stringsEqual(app.getLanguage(), item.getBasename().replaceAll("-tts", ""));
	}
}