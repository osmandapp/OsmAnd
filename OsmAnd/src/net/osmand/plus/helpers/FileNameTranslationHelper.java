package net.osmand.plus.helpers;

import android.content.Context;
import net.osmand.IndexConstants;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import java.lang.reflect.Field;

/**
 * Created by Barsik on 07.07.2014.
 */
public class FileNameTranslationHelper {

	public static final String WIKI_NAME = "_wiki";
	public static final String HILL_SHADE = "Hillshade";

	public static String getFileName(Context ctx, OsmandRegions regions, String fileName) {
		String basename = getBasename(fileName);
		if (basename.endsWith(WIKI_NAME)) { //wiki files
			return getWikiName(ctx,basename);
		} else if (fileName.endsWith("tts")) { //tts files
			return getVoiceName(ctx, fileName);
		} else if (fileName.startsWith(HILL_SHADE)){
			return getHillShadeName(ctx, regions, basename);
		} else if (fileName.length() == 2) { //voice recorded files
			try {
				Field f = R.string.class.getField("lang_"+fileName);
				if (f != null) {
					Integer in = (Integer) f.get(null);
					return ctx.getString(in);
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}

		//if nothing else
		String lc = basename.toLowerCase();
		String std = getStandardMapName(ctx, lc);
		if (std != null) {
			return std;
		}

		if (regions != null) {
			return regions.getLocaleName(basename);
		}

		return null;
	}

	public static String getHillShadeName(Context ctx, OsmandRegions regions, String basename) {
		String intermName = basename.replace(HILL_SHADE,"");
		String hillsh = ctx.getString(R.string.download_hillshade_item) + " ";

		String locName = regions.getLocaleName(intermName.trim().replace(" ", "_"));
		return hillsh + locName;
	}

	public static String getWikiName(Context ctx, String basename){
		String cutted = basename.substring(0, basename.indexOf("_wiki"));
		String wikiName = getStandardLangName(ctx, cutted);
		if (wikiName == null){
			wikiName = cutted;
		}
		String wikiWord = ctx.getString(R.string.amenity_type_osmwiki);
		int index = wikiWord.indexOf("(");
		if (index >= 0) {
			//removing word in "()" from recourse file
			return wikiWord.substring(0, index) + wikiName;
		}
		return ctx.getString(R.string.amenity_type_osmwiki) + " " + wikiName;
	}

	public static String getVoiceName(Context ctx, String basename) {
		try {
			String nm = basename.replace('-', '_').replace(' ', '_');
			if (nm.endsWith("_tts") || nm.endsWith("-tts")) {
				nm = nm.substring(0, nm.length() - 4);
			}
			Field f = R.string.class.getField("lang_"+nm);
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return ctx.getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return basename;
	}

	private static String getBasename(String fileName) {
		if (fileName.endsWith(IndexConstants.EXTRA_ZIP_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.EXTRA_ZIP_EXT.length());
		}
		if (fileName.endsWith(IndexConstants.SQLITE_EXT)) {
			return fileName.substring(0, fileName.length() - IndexConstants.SQLITE_EXT.length()).replace('_', ' ');
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
		if (filename.equalsIgnoreCase("Croatian")) {
			return ctx.getString(R.string.lang_hr);
		} else if (filename.equalsIgnoreCase("Chinese")) {
			return ctx.getString(R.string.lang_zh);
		} else if (filename.equalsIgnoreCase("Portuguese")) {
			return ctx.getString(R.string.lang_pt_br);
		} else if (filename.equalsIgnoreCase("English")) {
			return ctx.getString(R.string.lang_en);
		} else if (filename.equalsIgnoreCase("Afrikaans") || filename.equalsIgnoreCase("Africaans")) {
			return ctx.getString(R.string.lang_af);
		} else if (filename.equalsIgnoreCase("Armenian")) {
			return ctx.getString(R.string.lang_hy);
		} else if (filename.equalsIgnoreCase("Basque")) {
			return ctx.getString(R.string.lang_eu);
		} else if (filename.equalsIgnoreCase("Belarusian")) {
			return ctx.getString(R.string.lang_be);
		} else if (filename.equalsIgnoreCase("Bosnian")) {
			return ctx.getString(R.string.lang_bs);
		} else if (filename.equalsIgnoreCase("Bulgarian")) {
			return ctx.getString(R.string.lang_bg);
		} else if (filename.equalsIgnoreCase("Catalan")) {
			return ctx.getString(R.string.lang_ca);
		} else if (filename.equalsIgnoreCase("Czech")) {
			return ctx.getString(R.string.lang_cs);
		} else if (filename.equalsIgnoreCase("Danish")) {
			return ctx.getString(R.string.lang_da);
		} else if (filename.equalsIgnoreCase("Dutch")) {
			return ctx.getString(R.string.lang_nl);
		} else if (filename.equalsIgnoreCase("Finnish")) {
			return ctx.getString(R.string.lang_fi);
		} else if (filename.equalsIgnoreCase("French")) {
			return ctx.getString(R.string.lang_fr);
		} else if (filename.equalsIgnoreCase("Georgian")) {
			return ctx.getString(R.string.lang_ka);
		} else if (filename.equalsIgnoreCase("German")) {
			return ctx.getString(R.string.lang_de);
		} else if (filename.equalsIgnoreCase("Greek")) {
			return ctx.getString(R.string.lang_el);
		} else if (filename.equalsIgnoreCase("Hebrew")) {
			return ctx.getString(R.string.lang_iw);
		} else if (filename.equalsIgnoreCase("Hindi")) {
			return ctx.getString(R.string.lang_hi);
		} else if (filename.equalsIgnoreCase("Hungarian")) {
			return ctx.getString(R.string.lang_hu);
		} else if (filename.equalsIgnoreCase("Indonesian")) {
			return ctx.getString(R.string.lang_id);
		} else if (filename.equalsIgnoreCase("Italian")) {
			return ctx.getString(R.string.lang_it);
		} else if (filename.equalsIgnoreCase("Japanese")) {
			return ctx.getString(R.string.lang_ja);
		} else if (filename.equalsIgnoreCase("Korean")) {
			return ctx.getString(R.string.lang_ko);
		} else if (filename.equalsIgnoreCase("Latvian")) {
			return ctx.getString(R.string.lang_lv);
		} else if (filename.equalsIgnoreCase("Lithuanian")) {
			return ctx.getString(R.string.lang_lt);
		} else if (filename.equalsIgnoreCase("Marathi")) {
			return ctx.getString(R.string.lang_mr);
		} else if (filename.equalsIgnoreCase("Norwegian")) {
			return ctx.getString(R.string.lang_no);
		} else if (filename.equalsIgnoreCase("Persian")) {
			return ctx.getString(R.string.lang_fa);
		} else if (filename.equalsIgnoreCase("Polish")) {
			return ctx.getString(R.string.lang_pl);
		} else if (filename.equalsIgnoreCase("Portuguese")) {
			return ctx.getString(R.string.lang_pt);
		} else if (filename.equalsIgnoreCase("Romanian")) {
			return ctx.getString(R.string.lang_ro);
		} else if (filename.equalsIgnoreCase("Russian")) {
			return ctx.getString(R.string.lang_ru);
		} else if (filename.equalsIgnoreCase("Slovak")) {
			return ctx.getString(R.string.lang_sk);
		} else if (filename.equalsIgnoreCase("Slovenian")) {
			return ctx.getString(R.string.lang_sl);
		} else if (filename.equalsIgnoreCase("Spanish")) {
			return ctx.getString(R.string.lang_es);
		} else if (filename.equalsIgnoreCase("Swedish")) {
			return ctx.getString(R.string.lang_sv);
		} else if (filename.equalsIgnoreCase("Turkish")) {
			return ctx.getString(R.string.lang_tr);
		} else if (filename.equalsIgnoreCase("Ukrainian")) {
			return ctx.getString(R.string.lang_uk);
		} else if (filename.equalsIgnoreCase("Vietnamese")) {
			return ctx.getString(R.string.lang_vi);
		} else if (filename.equalsIgnoreCase("Welsh")) {
			return ctx.getString(R.string.lang_cy);
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
		} else if (basename.equals("world_bitcoin_payments")) {
			return ctx.getString(R.string.index_item_world_bitcoin_payments);
		} else if (basename.equals("world_seamarks")) {
			return ctx.getString(R.string.index_item_world_seamarks);
		}
		return null;
	}
}
