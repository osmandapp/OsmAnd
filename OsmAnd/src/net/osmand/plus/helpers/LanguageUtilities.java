package net.osmand.plus.helpers;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LanguageUtilities {

	public static Pair<String[], String[]> getSupportedLocaleIdsAndValues(@NonNull Context ctx,
	                                                                      boolean includeSystemLanguage,
	                                                                      boolean includeSuffixes) {
		List<Language> languages = getSupportedLanguages(ctx, includeSystemLanguage, includeSuffixes);
		int size = languages.size();
		String[] ids = new String[size];
		String[] values = new String[size];
		for (int i = 0; i < size; i++) {
			ids[i] = languages.get(i).getTranslation();
			values[i] = languages.get(i).getLocale();
		}
		return Pair.create(ids, values);
	}

	public static List<Language> getSupportedLanguages(@NonNull Context ctx) {
		return getSupportedLanguages(ctx, false, false);
	}

	public static List<Language> getSupportedLanguages(@NonNull Context ctx, boolean includeSystemLanguage, boolean includeSuffixes) {
		// See language list and statistics at: https://hosted.weblate.org/projects/osmand/main/
		// Hardy maintenance 2016-05-29:
		//  - Include languages if their translation is >= ~10%    (but any language will be visible if it is the device's system locale)
		//  - Mark as "incomplete" if                    < ~80%
		String incompleteSuffix = !includeSuffixes ? "" : " (" + ctx.getString(R.string.incomplete_locale) + ")";
		List<Language> languages = new ArrayList<>();

		// Add " (Device language)" to system default entry in Latin letters, so it can be more easily identified if a foreign language has been selected by mistake
		String latinSystemDefaultSuffix =!includeSuffixes ? "" : " (" + ctx.getString(R.string.system_locale_no_translate) + ")";

		final String systemLocale = ctx.getString(R.string.system_locale) + latinSystemDefaultSuffix;
		final String englishLocale = ctx.getString(R.string.lang_en);

		//locales -> getResources().getAssets().getLocales();
		if (includeSystemLanguage) {
			add(ctx, languages, "", R.string.system_locale, latinSystemDefaultSuffix);
		}
		add(ctx, languages, "en", R.string.lang_en);
		add(ctx, languages, "af", R.string.lang_af, incompleteSuffix);
		add(ctx, languages, "ar", R.string.lang_ar);
		add(ctx, languages, "ast", R.string.lang_ast, incompleteSuffix);
		add(ctx, languages, "az", R.string.lang_az);
		add(ctx, languages, "be", R.string.lang_be);
//		add(languages, "be_BY", R.string.lang_be_by);
		add(ctx, languages, "bg", R.string.lang_bg);
		add(ctx, languages, "ca", R.string.lang_ca);
		add(ctx, languages, "cs", R.string.lang_cs);
		add(ctx, languages, "cy", R.string.lang_cy, incompleteSuffix);
		add(ctx, languages, "da", R.string.lang_da);
		add(ctx, languages, "de", R.string.lang_de);
		add(ctx, languages, "el", R.string.lang_el);
		add(ctx, languages, "en_GB", R.string.lang_en_gb);
		add(ctx, languages, "eo", R.string.lang_eo);
		add(ctx, languages, "es", R.string.lang_es);
		add(ctx, languages, "es_AR", R.string.lang_es_ar);
		add(ctx, languages, "es_US", R.string.lang_es_us);
		add(ctx, languages, "eu", R.string.lang_eu);
		add(ctx, languages, "fa", R.string.lang_fa);
		add(ctx, languages, "fi", R.string.lang_fi, incompleteSuffix);
		add(ctx, languages, "fr", R.string.lang_fr);
		add(ctx, languages, "gl", R.string.lang_gl);
		add(ctx, languages,"he", R.string.lang_he);
		add(ctx, languages,"hr", R.string.lang_hr, incompleteSuffix);
		add(ctx, languages,"hsb", R.string.lang_hsb, incompleteSuffix);
		add(ctx, languages,"hu", R.string.lang_hu);
		add(ctx, languages,"hy", R.string.lang_hy);
		add(ctx, languages,"is", R.string.lang_is);
		add(ctx, languages,"it", R.string.lang_it);
		add(ctx, languages,"ja", R.string.lang_ja);
		add(ctx, languages,"ka", R.string.lang_ka, incompleteSuffix);
		add(ctx, languages,"kab", R.string.lang_kab, incompleteSuffix);
		add(ctx, languages,"kn", R.string.lang_kn, incompleteSuffix);
		add(ctx, languages,"ko", R.string.lang_ko);
		add(ctx, languages,"lt", R.string.lang_lt);
		add(ctx, languages,"lv", R.string.lang_lv);
		add(ctx, languages,"ml", R.string.lang_ml);
		add(ctx, languages,"mr", R.string.lang_mr, incompleteSuffix);
		add(ctx, languages,"nb", R.string.lang_nb);
		add(ctx, languages,"nl", R.string.lang_nl);
		add(ctx, languages,"nn", R.string.lang_nn, incompleteSuffix);
		add(ctx, languages,"oc", R.string.lang_oc, incompleteSuffix);
		add(ctx, languages,"pl", R.string.lang_pl);
		add(ctx, languages,"pt", R.string.lang_pt);
		add(ctx, languages,"pt_BR", R.string.lang_pt_br);
		add(ctx, languages,"ro", R.string.lang_ro, incompleteSuffix);
		add(ctx, languages,"ru", R.string.lang_ru);
		add(ctx, languages,"sc", R.string.lang_sc);
		add(ctx, languages,"sk", R.string.lang_sk);
		add(ctx, languages,"sl", R.string.lang_sl);
		add(ctx, languages,"sr", R.string.lang_sr);
		add(ctx, languages,"sr+Latn", R.string.lang_sr_latn, incompleteSuffix);
		add(ctx, languages,"sv", R.string.lang_sv);
		add(ctx, languages,"tr", R.string.lang_tr);
		add(ctx, languages,"uk", R.string.lang_uk);
		add(ctx, languages,"vi", R.string.lang_vi, incompleteSuffix);
		add(ctx, languages,"zh_CN", R.string.lang_zh_cn, incompleteSuffix);
		add(ctx, languages,"zh_TW", R.string.lang_zh_tw);

		Collections.sort(languages, new Comparator<Language>() {
			@Override
			public int compare(Language o1, Language o2) {
				//TODO
				String lt = o1.getTranslation();
				String rt = o2.getTranslation();
				String ll = o1.getLocale();
				String rl = o2.getLocale();
				int i1 = Algorithms.isEmpty(ll) ? 0 : (ll.equals("en") ? 1 : 2);
				int i2 = Algorithms.isEmpty(rl) ? 0 : (rl.equals("en") ? 1 : 2);
				if (i1 != i2) {
					return i1 < i2 ? -1 : 1;
				}
				i1 = systemLocale.equals(lt) ? 0 : (englishLocale.equals(lt) ? 1 : 2);
				i2 = systemLocale.equals(rt) ? 0 : (englishLocale.equals(rt) ? 1 : 2);
				if (i1 != i2) {
					return i1 < i2 ? -1 : 1;
				}
				return lt.compareTo(rt);
			}
		});

		return languages;
	}

	public static void add(@NonNull Context ctx,
	                       @NonNull List<Language> list,
	                       @NonNull String locale,
	                       @StringRes int translationId) {
		add(ctx, list, locale, translationId, "");
	}

	public static void add(@NonNull Context ctx,
	                       @NonNull List<Language> languages,
	                       @NonNull String locale,
	                       @StringRes int translationId,
	                       @NonNull String suffix) {
		String translation = ctx.getString(translationId) + suffix;
		languages.add(new Language(locale, translation));
	}

	public static class Language {
		String locale;
		String translation;

		Language(@NonNull String locale, @NonNull String translation) {
			this.locale = locale;
			this.translation = translation;
		}

		public String getLocale() {
			return locale;
		}

		public String getTranslation() {
			return translation;
		}
	}
}