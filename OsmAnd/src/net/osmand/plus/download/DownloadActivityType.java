package net.osmand.plus.download;

import android.content.Context;

import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.helpers.FileNameTranslationHelper;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;

public class DownloadActivityType {
	private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.US);
	private static Map<String, DownloadActivityType> byTag = new HashMap<>();
	
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
	public static final DownloadActivityType HILLSHADE_FILE =
			new DownloadActivityType(R.string.download_hillshade_maps, R.drawable.ic_action_hillshade_dark, "hillshade", 50);
	public static final DownloadActivityType WIKIPEDIA_FILE =
			new DownloadActivityType(R.string.download_wikipedia_maps, R.drawable.ic_plugin_wikipedia, "wikimap", 60);
	public static final DownloadActivityType LIVE_UPDATES_FILE =
			new DownloadActivityType(R.string.download_live_updates, "live_updates", 70);
	private final int stringResource;
	private final int iconResource;

	private String tag;
	private int orderIndex;

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

	public int getStringResource(){
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

	public static boolean isCountedInDownloads(IndexItem es) {
		DownloadActivityType tp = es.getType();
		if(tp == NORMAL_FILE || tp == ROADS_FILE){
			if (!es.extra) {
				return true;
			}
		}
		return false;
	}

	public String getString(Context c) {
		return c.getString(stringResource);
	}

	public static DownloadActivityType getIndexType(String tagName) {
		return byTag.get(tagName);
	}
	
	protected static String addVersionToExt(String ext, int version) {
		return "_" + version + ext;
	}
	
	public boolean isAccepted(String fileName) {
		if(NORMAL_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION)) 
					|| fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)
					|| fileName.endsWith(IndexConstants.SQLITE_EXT);
		} else if(ROADS_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_ROAD_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION));
		} else if (VOICE_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.VOICE_INDEX_EXT_ZIP, IndexConstants.VOICE_VERSION));
		} else if (FONT_FILE == this) {
			return fileName.endsWith(IndexConstants.FONT_INDEX_EXT_ZIP);
		} else if (WIKIPEDIA_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT_ZIP,
					IndexConstants.BINARY_MAP_VERSION));
		} else if (SRTM_COUNTRY_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_SRTM_MAP_INDEX_EXT_ZIP,
					IndexConstants.BINARY_MAP_VERSION));
		} else if (HILLSHADE_FILE == this) {
			return fileName.endsWith(IndexConstants.SQLITE_EXT);
		} else if (DEPTH_CONTOUR_FILE == this) {
			return fileName.endsWith(addVersionToExt(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP, IndexConstants.BINARY_MAP_VERSION));
		}
		return false;
	}
	
	public File getDownloadFolder(OsmandApplication ctx, IndexItem indexItem) {
		if (NORMAL_FILE == this) {
			if (indexItem.fileName.endsWith(IndexConstants.SQLITE_EXT)) {
				return ctx.getAppPath(IndexConstants.TILES_INDEX_DIR);
			}
			return ctx.getAppPath(IndexConstants.MAPS_PATH);
		} else if (VOICE_FILE == this) {
			return ctx.getAppPath(IndexConstants.VOICE_INDEX_DIR);
		} else if (FONT_FILE == this) {
			return ctx.getAppPath(IndexConstants.FONT_INDEX_DIR);
		} else if (ROADS_FILE == this) {
			return ctx.getAppPath(IndexConstants.ROADS_INDEX_DIR);
		} else if (SRTM_COUNTRY_FILE == this) {
			return ctx.getAppPath(IndexConstants.SRTM_INDEX_DIR);
		} else if (WIKIPEDIA_FILE == this) {
			return ctx.getAppPath(IndexConstants.WIKI_INDEX_DIR);
		} else if (LIVE_UPDATES_FILE == this) {
			return ctx.getAppPath(IndexConstants.LIVE_INDEX_DIR);
		} else if (HILLSHADE_FILE == this) {
			return ctx.getAppPath(IndexConstants.TILES_INDEX_DIR);
		} else if (DEPTH_CONTOUR_FILE == this) {
			return ctx.getAppPath(IndexConstants.MAPS_PATH);
		}
		throw new UnsupportedOperationException();
	}
	
	public boolean isZipStream(OsmandApplication ctx, IndexItem indexItem) {
		return HILLSHADE_FILE != this;
	}
	
	public boolean isZipFolder(OsmandApplication ctx, IndexItem indexItem) {
		return this == VOICE_FILE;
	}
	
	public boolean preventMediaIndexing(OsmandApplication ctx, IndexItem indexItem) {
		return this == VOICE_FILE && indexItem.fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP);
	}
	
	public String getUnzipExtension(OsmandApplication ctx, IndexItem indexItem) {
		if (NORMAL_FILE == this) {
			if (indexItem.fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
				return BINARY_MAP_INDEX_EXT;
			} else if (indexItem.fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
				return BINARY_MAP_INDEX_EXT;
			} else if (indexItem.fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
				return IndexConstants.EXTRA_EXT;
			} else if (indexItem.fileName.endsWith(IndexConstants.SQLITE_EXT)) {
				return IndexConstants.SQLITE_EXT;
			} else if (indexItem.fileName.endsWith(IndexConstants.ANYVOICE_INDEX_EXT_ZIP)){
				return "";
			}
		} else if (ROADS_FILE == this) {
			return IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
		} else if (VOICE_FILE == this) {
			return "";
		} else if (FONT_FILE == this) {
			return IndexConstants.FONT_INDEX_EXT;
		} else if (SRTM_COUNTRY_FILE == this) {
			return IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
		} else if (WIKIPEDIA_FILE == this) {
			return IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
		} else if (LIVE_UPDATES_FILE == this) {
			return BINARY_MAP_INDEX_EXT;
		} else if (HILLSHADE_FILE == this) {
			return IndexConstants.SQLITE_EXT;
		} else if (DEPTH_CONTOUR_FILE == this) {
			return BINARY_MAP_INDEX_EXT;
		}
		throw new UnsupportedOperationException();
	}
	
	public String getUrlSuffix(OsmandApplication ctx) {
		if (this== ROADS_FILE) {
			return "&road=yes";
		} else if (this == LIVE_UPDATES_FILE) {
			return "&aosmc=yes";
		} else if (this == SRTM_COUNTRY_FILE) {
			return "&srtmcountry=yes";
		} else if (this == WIKIPEDIA_FILE) {
			return "&wiki=yes";
		} else if (this == HILLSHADE_FILE) {
			return "&hillshade=yes";
		} else if (this == FONT_FILE) {
			return "&fonts=yes";
		} else if (this == DEPTH_CONTOUR_FILE) {
			return "&inapp=depth";
		}
		return "";
	}

	public String getBaseUrl(OsmandApplication ctx, String fileName) {
		return "http://" + IndexConstants.INDEX_DOWNLOAD_DOMAIN + "/download?event=2&"
				+ Version.getVersionAsURLParam(ctx) + "&file=" + encode(fileName);
	}


	protected String encode(String fileName) {
		try {
			return URLEncoder.encode(fileName, "UTF-8");
		} catch (IOException e) {
			return fileName;
		}
	}


	public IndexItem parseIndexItem(Context ctx, XmlPullParser parser) {
		String name = parser.getAttributeValue(null, "name"); //$NON-NLS-1$
		if(!isAccepted(name)) {
			return null;
		}
		String size = parser.getAttributeValue(null, "size"); //$NON-NLS-1$
		String description = parser.getAttributeValue(null, "description"); //$NON-NLS-1$
		long containerSize = Algorithms.parseLongSilently(
				parser.getAttributeValue(null, "containerSize"), 0);
		long contentSize = Algorithms.parseLongSilently(
				parser.getAttributeValue(null, "contentSize"), 0);
		long timestamp = Algorithms.parseLongSilently(
				parser.getAttributeValue(null, "timestamp"), 0);
		IndexItem it = new IndexItem(name, description, timestamp, size, contentSize, containerSize, this);
		it.extra = FileNameTranslationHelper.getStandardMapName(ctx, it.getBasename().toLowerCase()) != null;
		return it;
	}

	protected static String reparseDate(Context ctx, String date) {
		try {
			Date d = simpleDateFormat.parse(date);
			return AndroidUtils.formatDate(ctx, d.getTime());
		} catch (ParseException e) {
			return date;
		}
	}

	public String getVisibleDescription(IndexItem indexItem, Context ctx) {
		if (this == SRTM_COUNTRY_FILE) {
			return ctx.getString(R.string.download_srtm_maps);
		} else if (this == WIKIPEDIA_FILE) {
			return ctx.getString(R.string.shared_string_wikipedia);
		} else if (this == ROADS_FILE) {
			return ctx.getString(R.string.download_roads_only_item);
		} else if (this == DEPTH_CONTOUR_FILE) {
			return ctx.getString(R.string.download_depth_countours);
		} else if (this == FONT_FILE) {
			return ctx.getString(R.string.fonts_header);
		}
		return "";
	}
	
	public String getVisibleName(IndexItem indexItem, Context ctx, OsmandRegions osmandRegions, boolean includingParent) {
		if (this == VOICE_FILE) {
			String fileName = indexItem.fileName;
			if (fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)) {
				return FileNameTranslationHelper.getVoiceName(ctx, getBasename(indexItem));
			} else if (fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)) {
				return FileNameTranslationHelper.getVoiceName(ctx, getBasename(indexItem));
			}
			return getBasename(indexItem);
		}
		if (this == FONT_FILE) {
			return FileNameTranslationHelper.getFontName(ctx, getBasename(indexItem));
		}
		final String basename = getBasename(indexItem);
		if (basename.endsWith(FileNameTranslationHelper.WIKI_NAME)){
			return FileNameTranslationHelper.getWikiName(ctx, basename);
		}
//		if (this == HILLSHADE_FILE){
//			return FileNameTranslationHelper.getHillShadeName(ctx, osmandRegions, bn);
//		}
		final String lc = basename.toLowerCase();
		String std = FileNameTranslationHelper.getStandardMapName(ctx, lc);
		if (std != null) {
			return std;
		}
		if (basename.contains("addresses-nationwide")) {
			final int ind = basename.indexOf("addresses-nationwide");
			String downloadName = basename.substring(0, ind - 1) + basename.substring(ind + "addresses-nationwide".length());
			return osmandRegions.getLocaleName(downloadName, includingParent) +
					" "+ ctx.getString(R.string.index_item_nation_addresses);
		} else if (basename.startsWith("Depth_")) {
			final int extInd = basename.indexOf("osmand_ext");
			String downloadName = extInd == -1 ? basename.substring(6, basename.length()).replace('_', ' ')
					: basename.substring(6, extInd).replace('_', ' ');
			return ctx.getString(R.string.download_depth_countours) + " " + downloadName;
		}

		return osmandRegions.getLocaleName(basename, includingParent);
	}
	
	public String getTargetFileName(IndexItem item) {
		String fileName = item.fileName;
		// if(fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP) ||
		// fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)) {
		if (this == VOICE_FILE) {
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
		} else if (this == LIVE_UPDATES_FILE) {
			int l = fileName.lastIndexOf('.');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.substring(0, l) + IndexConstants.BINARY_MAP_INDEX_EXT;
		} else if (fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)
				|| fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)) {
			int l = fileName.lastIndexOf('_');
			if (l == -1) {
				l = fileName.length();
			}
			String baseNameWithoutVersion = fileName.substring(0, l);
			if (this == SRTM_COUNTRY_FILE) {
				return baseNameWithoutVersion + IndexConstants.BINARY_SRTM_MAP_INDEX_EXT;
			}
			if (this == WIKIPEDIA_FILE) {
				return baseNameWithoutVersion + IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;
			}
			if (this == ROADS_FILE) {
				return baseNameWithoutVersion + IndexConstants.BINARY_ROAD_MAP_INDEX_EXT;
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


	public String getBasename(IndexItem indexItem) {
		String fileName = indexItem.fileName;
		if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.EXTRA_ZIP_EXT.length());
		}
		if (this == HILLSHADE_FILE) {
			return fileName.substring(0, fileName.length() - IndexConstants.SQLITE_EXT.length())
					.replace(FileNameTranslationHelper.HILL_SHADE, "");
		}
		if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.SQLITE_EXT.length());
		}
		if (this == VOICE_FILE) {
			int l = fileName.lastIndexOf('_');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.substring(0, l);
		}
		if (this == FONT_FILE) {
			int l = fileName.indexOf('.');
			if (l == -1) {
				l = fileName.length();
			}
			return fileName.substring(0, l);
		}
		if (this == LIVE_UPDATES_FILE) {
			if(fileName.indexOf('.') > 0){
				return fileName.substring(0, fileName.indexOf('.'));
			}
			return fileName;
		}
		int ls = fileName.lastIndexOf('_');
		if (ls >= 0) {
			return fileName.substring(0, ls);
		} else if(fileName.indexOf('.') > 0){
			return fileName.substring(0, fileName.indexOf('.'));
		}
		return fileName;
	}

}