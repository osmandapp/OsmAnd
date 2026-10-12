package net.osmand.plus.helpers;

import static net.osmand.IndexConstants.WEATHER_MAP_INDEX_EXT;
import static net.osmand.map.WorldRegion.WORLD;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.SrtmDownloadItem;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

import java.lang.reflect.Field;

/**
 * Created by Barsik
 * on 07.07.2014.
 */
public class FileNameTranslationHelper {

	private static final Log LOG = PlatformUtil.getLog(FileNameTranslationHelper.class);

	public static final String WIKI_NAME = "_wiki";
	public static final String WIKIVOYAGE_NAME = "_wikivoyage";
	public static final String WEATHER = "Weather";
	public static final String HILL_SHADE = "Hillshade";
	public static final String SLOPE = "Slope";
	public static final String HEIGHTMAP = "Heightmap";
	public static final String SEA_DEPTH = "Depth_";
	public static final String TRAVEL_TOPICS = "travel_topics";

	public static String getFileNameWithRegion(OsmandApplication app, String fileName) {
		return getFileName(app, app.getResourceManager().getOsmandRegions(), fileName);
	}

	@Nullable
	public static String getFileName(Context ctx, OsmandRegions regions, String fileName) {
		return getFileName(ctx, regions, fileName, " ", true, false);
	}

	@Nullable
	public static String getFileName(Context ctx, OsmandRegions regions, String fileName,
	                                 String divider, boolean includingParent, boolean reversed) {
		String basename = getBasename(ctx, fileName);
		if (basename.endsWith(WIKI_NAME)) {
			return getWikiName(ctx, basename);
		} else if (basename.endsWith(WIKIVOYAGE_NAME)) {
			return getWikivoyageName(ctx, basename);
		} else if (fileName.endsWith(WEATHER_MAP_INDEX_EXT)) {
			basename = basename.replace("Weather_", "");
			return getWeatherName(ctx, regions, basename);
		} else if (fileName.endsWith("tts")) {
			return getVoiceName(ctx, fileName);
		} else if (fileName.endsWith(IndexConstants.FONT_INDEX_EXT)) {
			return getFontName(basename);
		} else if (fileName.startsWith(HILL_SHADE)) {
			basename = basename.replace(HILL_SHADE + " ", "");
			return getTerrainName(ctx, regions, basename, R.string.download_hillshade_maps);
		} else if (fileName.startsWith(HEIGHTMAP)) {
			basename = basename.replace(HEIGHTMAP + " ", "");
			return getTerrainName(ctx, regions, basename, R.string.terrain_map);
		} else if (fileName.startsWith(SLOPE)) {
			basename = basename.replace(SLOPE + " ", "");
			return getTerrainName(ctx, regions, basename, R.string.download_slope_maps);
		} else if (SrtmDownloadItem.isSrtmFile(fileName)) {
			return getTerrainName(ctx, regions, basename, R.string.download_srtm_maps);
		} else if (fileName.length() == 2) { //voice recorded files
			String name = getStringFromResName(ctx, "lang_" + fileName);
			if (name != null) {
				return name;
			}
		}
		//if nothing else
		String lc = basename.toLowerCase();
		String std = getStandardMapName(ctx, lc);
		if (std != null) {
			return std;
		}
		if (regions != null) {
			return regions.getLocaleName(basename, divider, includingParent, reversed);
		}
		return null;
	}

	public static String getTerrainName(Context ctx, OsmandRegions regions, String basename,
	                                    int terrainNameRes) {
		basename = basename.replace(" ", "_");
		String terrain = ctx.getString(terrainNameRes);
		String locName = regions.getLocaleName(basename.trim(), true);
		return ctx.getString(R.string.ltr_or_rtl_combine_via_space, locName, "(" + terrain + ")");
	}

	public static String getWikiName(Context ctx, String basename) {
		String cutted = basename.substring(0, basename.indexOf("_wiki"));
		String wikiName = getStandardLangName(ctx, cutted);
		if (wikiName == null) {
			wikiName = cutted;
		}
		String wikiWord = ctx.getString(R.string.amenity_type_osmwiki);
		int index = wikiWord.indexOf("(");
		if (index >= 0) {
			//removing word in "()" from recourse file
			return wikiName + " " + wikiWord.substring(0, index).trim();
		}
		return wikiName + " " + ctx.getString(R.string.amenity_type_osmwiki);
	}

	public static String getWikivoyageName(Context ctx, String basename) {
		String formattedName = basename.substring(0, basename.indexOf(WIKIVOYAGE_NAME))
				.replaceAll("-", "")
				.replaceAll("all", "");

		if ("Default".equals(formattedName)) {
			return ctx.getString(R.string.sample_wikivoyage);
		} else {
			String wikiVoyageName = getSuggestedWikivoyageMaps(ctx, formattedName);
			if (wikiVoyageName == null) {
				wikiVoyageName = formattedName;
			}
			String wikiVoyageWord = ctx.getString(R.string.shared_string_wikivoyage);
			return ctx.getString(R.string.ltr_or_rtl_combine_via_space, wikiVoyageName, wikiVoyageWord);
		}
	}

	public static String getWeatherName(Context ctx, OsmandRegions regions, String basename) {
		basename = basename.replace(" ", "_");
		if (WORLD.equalsIgnoreCase(basename)) {
			return ctx.getString(R.string.shared_string_all_world);
		} else {
			return regions.getLocaleName(basename.trim(), false);
		}
	}

	@NonNull
	public static String getVoiceName(@NonNull Context ctx, @NonNull String fileName) {
		String name = fileName.replace('-', '_').replace(' ', '_');
		if (name.endsWith("_tts") || name.endsWith(IndexConstants.VOICE_PROVIDER_SUFFIX)) {
			name = name.substring(0, name.length() - 4);
		}
		String voiceName = getStringFromResName(ctx, "lang_" + name);
		if (voiceName == null) {
			voiceName = getStringFromResName(ctx, "sound_" + name);
		}
		return voiceName != null ? voiceName : fileName;
	}

	@Nullable
	private static String getStringFromResName(@NonNull Context ctx, @NonNull String name) {
		try {
			Field f = R.string.class.getField(name);
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			//ignored
		}
		return null;
	}

	public static String getFontName(@NonNull String basename) {
		return basename.replace('-', ' ').replace('_', ' ');
	}

	public static String getBasename(@NonNull Context ctx, @NonNull String fileName) {
		if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.EXTRA_ZIP_EXT.length());
		}
		if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			OsmandApplication app = (OsmandApplication) ctx.getApplicationContext();
			OsmandSettings settings = app.getSettings();
			return settings.getTileSourceTitle(fileName);
		}
		if (fileName.endsWith(IndexConstants.WEATHER_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.WEATHER_EXT.length());
		}

		int ls = fileName.lastIndexOf("-roads");
		if (ls >= 0) {
			return fileName.substring(0, ls);
		} else {
			ls = fileName.indexOf(".");
			if (ls >= 0) {
				return fileName.substring(0, ls);
			}
		}
		return fileName;
	}

	private static String getStandardLangName(Context ctx, String filename) {
		if (filename.equalsIgnoreCase("Afrikaans") || filename.equalsIgnoreCase("Africaans")) {
			return ctx.getString(R.string.lang_af);
		} else if (filename.equalsIgnoreCase("Belarusian")) {
			return ctx.getString(R.string.lang_be);
		} else if (filename.equalsIgnoreCase("Bulgarian")) {
			return ctx.getString(R.string.lang_bg);
		} else if (filename.equalsIgnoreCase("Bosnian")) {
			return ctx.getString(R.string.lang_bs);
		} else if (filename.equalsIgnoreCase("Catalan")) {
			return ctx.getString(R.string.lang_ca);
		} else if (filename.equalsIgnoreCase("Czech")) {
			return ctx.getString(R.string.lang_cs);
		} else if (filename.equalsIgnoreCase("Welsh")) {
			return ctx.getString(R.string.lang_cy);
		} else if (filename.equalsIgnoreCase("Danish")) {
			return ctx.getString(R.string.lang_da);
		} else if (filename.equalsIgnoreCase("German")) {
			return ctx.getString(R.string.lang_de);
		} else if (filename.equalsIgnoreCase("Greek")) {
			return ctx.getString(R.string.lang_el);
		} else if (filename.equalsIgnoreCase("English")) {
			return ctx.getString(R.string.lang_en);
		} else if (filename.equalsIgnoreCase("Spanish")) {
			return ctx.getString(R.string.lang_es);
		} else if (filename.equalsIgnoreCase("Basque")) {
			return ctx.getString(R.string.lang_eu);
		} else if (filename.equalsIgnoreCase("Finnish")) {
			return ctx.getString(R.string.lang_fi);
		} else if (filename.equalsIgnoreCase("French")) {
			return ctx.getString(R.string.lang_fr);
		} else if (filename.equalsIgnoreCase("Hindi")) {
			return ctx.getString(R.string.lang_hi);
		} else if (filename.equalsIgnoreCase("Croatian")) {
			return ctx.getString(R.string.lang_hr);
		} else if (filename.equalsIgnoreCase("Hungarian")) {
			return ctx.getString(R.string.lang_hu);
		} else if (filename.equalsIgnoreCase("Armenian")) {
			return ctx.getString(R.string.lang_hy);
		} else if (filename.equalsIgnoreCase("Indonesian")) {
			return ctx.getString(R.string.lang_id);
		} else if (filename.equalsIgnoreCase("Italian")) {
			return ctx.getString(R.string.lang_it);
		} else if (filename.equalsIgnoreCase("Hebrew")) {
			return ctx.getString(R.string.lang_iw);
		} else if (filename.equalsIgnoreCase("Japanese")) {
			return ctx.getString(R.string.lang_ja);
		} else if (filename.equalsIgnoreCase("Georgian")) {
			return ctx.getString(R.string.lang_ka);
		} else if (filename.equalsIgnoreCase("Korean")) {
			return ctx.getString(R.string.lang_ko);
		} else if (filename.equalsIgnoreCase("Lithuanian")) {
			return ctx.getString(R.string.lang_lt);
		} else if (filename.equalsIgnoreCase("Latvian")) {
			return ctx.getString(R.string.lang_lv);
		} else if (filename.equalsIgnoreCase("Marathi")) {
			return ctx.getString(R.string.lang_mr);
		} else if (filename.equalsIgnoreCase("Dutch")) {
			return ctx.getString(R.string.lang_nl);
		} else if (filename.equalsIgnoreCase("Norwegian")) {
			return ctx.getString(R.string.lang_no);
		} else if (filename.equalsIgnoreCase("Polish")) {
			return ctx.getString(R.string.lang_pl);
		} else if (filename.equalsIgnoreCase("Portuguese")) {
			return ctx.getString(R.string.lang_pt);
			//} else if (filename.equalsIgnoreCase("Portuguese")) {
			//	return ctx.getString(R.string.lang_pt_br);
		} else if (filename.equalsIgnoreCase("Romanian")) {
			return ctx.getString(R.string.lang_ro);
		} else if (filename.equalsIgnoreCase("Russian")) {
			return ctx.getString(R.string.lang_ru);
		} else if (filename.equalsIgnoreCase("Slovak")) {
			return ctx.getString(R.string.lang_sk);
		} else if (filename.equalsIgnoreCase("Slovenian")) {
			return ctx.getString(R.string.lang_sl);
		} else if (filename.equalsIgnoreCase("Swedish")) {
			return ctx.getString(R.string.lang_sv);
		} else if (filename.equalsIgnoreCase("Turkish")) {
			return ctx.getString(R.string.lang_tr);
		} else if (filename.equalsIgnoreCase("Ukrainian")) {
			return ctx.getString(R.string.lang_uk);
		} else if (filename.equalsIgnoreCase("Vietnamese")) {
			return ctx.getString(R.string.lang_vi);
		} else if (filename.equalsIgnoreCase("Chinese")) {
			return ctx.getString(R.string.lang_zh);
		}
		return null;
	}

	public static String getStandardMapName(Context ctx, String basename) {
		if (basename.equals("world-ski")) {
			return ctx.getString(R.string.index_item_world_ski);
		} else if (basename.equals("world_altitude_correction_ww15mgh")) {
			return ctx.getString(R.string.index_item_world_altitude_correction);
		} else if (basename.equals("world_basemap")) {
			return ctx.getString(R.string.index_item_world_basemap);
		} else if (basename.equals("world_basemap_detailed")) {
			return ctx.getString(R.string.index_item_world_basemap_detailed);
		} else if (basename.equals("world_basemap_mini")) {
			String basemap = ctx.getString(R.string.index_item_world_basemap);
			String mini = "(" + ctx.getString(R.string.shared_string_mini) + ")";
			return ctx.getString(R.string.ltr_or_rtl_combine_via_space, basemap, mini);
		} else if (basename.equals("world_bitcoin_payments")) {
			return ctx.getString(R.string.index_item_world_bitcoin_payments);
		} else if (basename.equals(DownloadResources.WORLD_SEAMARKS_KEY) ||
				basename.equals(DownloadResources.WORLD_SEAMARKS_OLD_KEY)) {
			return ctx.getString(R.string.index_item_world_seamarks);
		} else if (basename.equals("world_wikivoyage")) {
			return ctx.getString(R.string.index_item_world_wikivoyage);
		} else if (basename.equals("depth_contours_osmand_ext")) {
			return ctx.getString(R.string.index_item_depth_contours_osmand_ext);
		} else if (basename.equals("depth_points_southern_hemisphere_osmand_ext")) {
			return ctx.getString(R.string.index_item_depth_points_southern_hemisphere);
		} else if (basename.equals("depth_points_northern_hemisphere_osmand_ext")) {
			return ctx.getString(R.string.index_item_depth_points_northern_hemisphere);
		}
		return null;
	}

	private static String getSuggestedWikivoyageMaps(Context ctx, String filename) {
		if (WorldRegion.AFRICA_REGION_ID.equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.index_name_africa);
		} else if (WorldRegion.AUSTRALIA_AND_OCEANIA_REGION_ID.replaceAll("-", "").equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.index_name_oceania);
		} else if (WorldRegion.ASIA_REGION_ID.equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.index_name_asia);
		} else if (WorldRegion.CENTRAL_AMERICA_REGION_ID.equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.index_name_central_america);
		} else if (WorldRegion.EUROPE_REGION_ID.equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.index_name_europe);
		} else if (WorldRegion.RUSSIA_REGION_ID.equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.index_name_russia);
		} else if (WorldRegion.NORTH_AMERICA_REGION_ID.equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.index_name_north_america);
		} else if (WorldRegion.SOUTH_AMERICA_REGION_ID.equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.index_name_south_america);
		} else if (WorldRegion.ANTARCTICA_REGION_ID.equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.index_name_antarctica);
		} else if (TRAVEL_TOPICS.equalsIgnoreCase(filename)) {
			return ctx.getString(R.string.travel_topics);
		}
		return null;
	}
}
