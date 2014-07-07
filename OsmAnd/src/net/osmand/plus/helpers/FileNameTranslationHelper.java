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

	public static String getFileName(Context ctx, OsmandRegions regions, String fileName) {
		String basename = getBasename(fileName);
		if (basename.endsWith("_wiki")) { //wiki files
			String wikiName = getStandardLangName(ctx, basename.substring(0, basename.indexOf("_wiki")));
			String wikiWord = ctx.getString(R.string.amenity_type_osmwiki);
			int index = wikiWord.indexOf("(");
			if (index >= 0) {
				//removing word in "()" from recourse file
				return wikiWord.substring(0, index) + wikiName;
			}
			return ctx.getString(R.string.amenity_type_osmwiki) + " " + wikiName;
		} else if (fileName.endsWith("tts")) { //tts files
			return getVoiceName(ctx, fileName);
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

		int ls = fileName.lastIndexOf('-');
		if (ls >= 0) {
			return fileName.substring(0, ls);
		} else {
			ls = fileName.lastIndexOf(".");
			if (ls >= 0) {
				return fileName.substring(0, ls);
			}
		}
		return fileName;
	}

	private static String getStandardLangName(Context ctx, String filename) {
		if (filename.equals("Croatian")) {
			return ctx.getString(R.string.lang_hr);
		} else if (filename.equals("Chinese")) {
			return ctx.getString(R.string.lang_zh);
		} else if (filename.equals("Portuguese")) {
			return ctx.getString(R.string.lang_pt_br);
		} else if (filename.equals("English")) {
			return ctx.getString(R.string.lang_en);
		} else if (filename.equals("Afrikaans") || filename.equals("Africaans")) {
			return ctx.getString(R.string.lang_af);
		} else if (filename.equals("Armenian")) {
			return ctx.getString(R.string.lang_hy);
		} else if (filename.equals("Basque")) {
			return ctx.getString(R.string.lang_eu);
		} else if (filename.equals("Belarusian")) {
			return ctx.getString(R.string.lang_be);
		} else if (filename.equals("Bosnian")) {
			return ctx.getString(R.string.lang_bs);
		} else if (filename.equals("Bulgarian")) {
			return ctx.getString(R.string.lang_bg);
		} else if (filename.equals("Catalan")) {
			return ctx.getString(R.string.lang_ca);
		} else if (filename.equals("Czech")) {
			return ctx.getString(R.string.lang_cs);
		} else if (filename.equals("Danish")) {
			return ctx.getString(R.string.lang_da);
		} else if (filename.equals("Dutch")) {
			return ctx.getString(R.string.lang_nl);
		} else if (filename.equals("Finnish")) {
			return ctx.getString(R.string.lang_fi);
		} else if (filename.equals("French")) {
			return ctx.getString(R.string.lang_fr);
		} else if (filename.equals("Georgian")) {
			return ctx.getString(R.string.lang_ka);
		} else if (filename.equals("German")) {
			return ctx.getString(R.string.lang_de);
		} else if (filename.equals("Greek")) {
			return ctx.getString(R.string.lang_el);
		} else if (filename.equals("Hebrew")) {
			return ctx.getString(R.string.lang_iw);
		} else if (filename.equals("Hindi")) {
			return ctx.getString(R.string.lang_hi);
		} else if (filename.equals("Hungarian")) {
			return ctx.getString(R.string.lang_hu);
		} else if (filename.equals("Indonesian")) {
			return ctx.getString(R.string.lang_id);
		} else if (filename.equals("Italian")) {
			return ctx.getString(R.string.lang_it);
		} else if (filename.equals("Japanese")) {
			return ctx.getString(R.string.lang_ja);
		} else if (filename.equals("Korean")) {
			return ctx.getString(R.string.lang_ko);
		} else if (filename.equals("Latvian")) {
			return ctx.getString(R.string.lang_lv);
		} else if (filename.equals("Lithuanian")) {
			return ctx.getString(R.string.lang_lt);
		} else if (filename.equals("Marathi")) {
			return ctx.getString(R.string.lang_mr);
		} else if (filename.equals("Norwegian")) {
			return ctx.getString(R.string.lang_no);
		} else if (filename.equals("Polish")) {
			return ctx.getString(R.string.lang_pl);
		} else if (filename.equals("Portuguese")) {
			return ctx.getString(R.string.lang_pt);
		} else if (filename.equals("Romanian")) {
			return ctx.getString(R.string.lang_ro);
		} else if (filename.equals("Russian")) {
			return ctx.getString(R.string.lang_ru);
		} else if (filename.equals("Slovak")) {
			return ctx.getString(R.string.lang_sk);
		} else if (filename.equals("Slovenian")) {
			return ctx.getString(R.string.lang_sl);
		} else if (filename.equals("Spanish")) {
			return ctx.getString(R.string.lang_es);
		} else if (filename.equals("Swedish")) {
			return ctx.getString(R.string.lang_sv);
		} else if (filename.equals("Turkish")) {
			return ctx.getString(R.string.lang_tr);
		} else if (filename.equals("Ukrainian")) {
			return ctx.getString(R.string.lang_uk);
		} else if (filename.equals("Vietnamese")) {
			return ctx.getString(R.string.lang_vi);
		} else if (filename.equals("Welsh")) {
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
