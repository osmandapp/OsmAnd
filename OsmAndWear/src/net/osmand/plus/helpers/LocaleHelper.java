package net.osmand.plus.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.ConfigurationCompat;
import androidx.core.os.LocaleListCompat;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class LocaleHelper {

	private final OsmandApplication app;

	private final Locale defaultLocale;
	private final StateChangedListener<String> localeListener;

	private Locale preferredLocale;
	private Resources localizedResources;
	private Configuration localizedConf;

	public LocaleHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.defaultLocale = Locale.getDefault();
		localeListener = change -> preferredLocaleChanged();
	}

	private void preferredLocaleChanged() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			String preferredLocale = app.getSettings().PREFERRED_LOCALE.get();
			if (!Algorithms.isEmpty(preferredLocale)) {
				Locale locale = new Locale(preferredLocale);
				Locale.setDefault(locale);
				app.runInUIThread(() -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale)));
			}
		}
	}

	public void checkPreferredLocale() {
		OsmandSettings settings = app.getSettings();
		String locale = settings.PREFERRED_LOCALE.get();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			String currentLocale = Locale.getDefault().toString();
			if (!Algorithms.stringsEqual(currentLocale, locale)) {
				locale = currentLocale;
				settings.PREFERRED_LOCALE.set(locale);
			}
		}
		settings.PREFERRED_LOCALE.addListener(localeListener);

		String[] splitScript = locale.split("\\+");
		String script = (splitScript.length > 1) ? splitScript[1] : "";
		String[] splitCountry = splitScript[0].split("_");
		String lang = splitCountry[0];
		String country = (splitCountry.length > 1) ? splitCountry[1] : "";

		if (!Algorithms.isEmpty(lang)) {
			Locale.Builder builder = new Locale.Builder();
			lang = backwardCompatibleNonIsoCodes(lang);
			String langLowerCase = lang.toLowerCase();
			for (String isoLang : Locale.getISOLanguages()) {
				if (isoLang.toLowerCase().equals(langLowerCase)) {
					builder.setLanguage(isoLang);
					break;
				}
			}
			if (!Algorithms.isEmpty(country)) {
				builder.setRegion(country);
			}
			if (!Algorithms.isEmpty(script)) {
				builder.setScript(script);
			}
			preferredLocale = builder.build();
		}

		Locale selectedLocale = null;
		Configuration config = app.getBaseContext().getResources().getConfiguration();
		if (!Algorithms.isEmpty(lang) && !config.locale.equals(preferredLocale)) {
			selectedLocale = preferredLocale;
		} else if (Algorithms.isEmpty(lang) && defaultLocale != null && Locale.getDefault() != defaultLocale) {
			selectedLocale = defaultLocale;
			preferredLocale = null;
		}

		updateTimeFormatting(selectedLocale != null ? selectedLocale : Locale.getDefault());
		if (selectedLocale != null) {
			Locale.setDefault(selectedLocale);
			config.locale = selectedLocale;
			config.setLayoutDirection(selectedLocale);

			Resources resources = app.getBaseContext().getResources();
			resources.updateConfiguration(config, resources.getDisplayMetrics());
			localizedConf = new Configuration(config);
			localizedConf.locale = selectedLocale;

		}
	}

	private String backwardCompatibleNonIsoCodes(String lang) {
		if (lang.equalsIgnoreCase("iw")) {
			return "he";
		} else if (lang.equalsIgnoreCase("ji")) {
			return "yi";
		} else if (lang.equalsIgnoreCase("id")) {
			return "in";
		}
		return lang;
	}

	public void setLanguage(@NonNull Context context) {
		Locale newLocale = null;
		if (preferredLocale != null) {
			Configuration config = context.getResources().getConfiguration();
			String lang = preferredLocale.getLanguage();
			boolean localeChanged = !config.locale.equals(preferredLocale);
			if (!Algorithms.isEmpty(lang) && localeChanged) {
				Locale.setDefault(preferredLocale);
				config.locale = preferredLocale;
				context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
			} else if (Algorithms.isEmpty(lang) && defaultLocale != null && Locale.getDefault() != defaultLocale) {
				newLocale = defaultLocale;
				Locale.setDefault(defaultLocale);
				config.locale = defaultLocale;
				Resources resources = app.getBaseContext().getResources();
				resources.updateConfiguration(config, resources.getDisplayMetrics());
			}
		}
		updateTimeFormatting(newLocale != null ? newLocale : Locale.getDefault());
	}

	@Nullable
	public Resources getLocalizedResources(Context ctx, Resources ctxRes) {
		if (localizedResources == null && localizedConf != null
				|| (localizedResources != null && localizedResources.getDisplayMetrics().density != ctxRes.getDisplayMetrics().density)) {
			localizedResources = ctx.createConfigurationContext(localizedConf).getResources();
		}
		return localizedResources;
	}

	@Nullable
	public Locale getPreferredLocale() {
		return preferredLocale;
	}

	@NonNull
	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	@NonNull
	public String getCountry() {
		String country;
		if (preferredLocale != null) {
			country = preferredLocale.getCountry();
		} else {
			country = Locale.getDefault().getCountry();
		}
		return country;
	}

	@NonNull
	public String getLanguage() {
		String lang = preferredLocale != null ? preferredLocale.getLanguage() : Locale.getDefault().getLanguage();
		if (lang.length() > 3) {
			lang = lang.substring(0, 2).toLowerCase();
		}
		return lang;
	}

	public void updateTimeFormatting() {
		updateTimeFormatting(Locale.getDefault());
	}

	public void updateTimeFormatting(@NonNull Locale locale) {
		updateTimeFormatting(!DateFormat.is24HourFormat(app), locale);
	}

	public void updateTimeFormatting(boolean twelveHoursFormatting, @NonNull Locale locale) {
		OpeningHoursParser.initLocalStrings(locale);
		OpeningHoursParser.setTwelveHourFormattingEnabled(twelveHoursFormatting, locale);
		OsmAndFormatter.setTwelveHoursFormatting(twelveHoursFormatting, locale);
	}

	public Resources getLocalizedResources(@NonNull String language) {
		return getLocalizedContext(new Locale(language)).getResources();
	}

	public Context getLocalizedContext(@NonNull Locale locale) {
		Configuration configuration = app.getResources().getConfiguration();
		configuration = new Configuration(configuration);
		configuration.setLocale(locale);
		return app.createConfigurationContext(configuration);
	}

	@Nullable
	public static Locale getPreferredNameLocale(@NonNull OsmandApplication app, @NonNull Collection<String> localeIds) {
		String preferredLocaleId = app.getSettings().PREFERRED_LOCALE.get();
		Locale availablePreferredLocale = getAvailablePreferredLocale(localeIds);

		return localeIds.contains(preferredLocaleId)
				? new Locale(preferredLocaleId)
				: availablePreferredLocale;
	}

	@Nullable
	private static Locale getAvailablePreferredLocale(@NonNull Collection<String> localeIds) {
		LocaleListCompat deviceLanguages = ConfigurationCompat.getLocales(Resources.getSystem().getConfiguration());

		for (int index = 0; index < deviceLanguages.size(); index++) {
			Locale locale = deviceLanguages.get(index);
			if (locale != null) {
				String localeId = locale.getLanguage();
				if (localeIds.contains(localeId)) {
					return locale;
				}
			}
		}
		return null;
	}

	@NonNull
	public static Map<String, String> getPreferredDisplayLanguages(@NonNull Context ctx) {
		// See language list and statistics at: https://hosted.weblate.org/projects/osmand/main/
		// Hardy maintenance 2016-05-29:
		//  - Include languages if their translation is >= ~10%    (but any language will be visible if it is the device's system locale)
		//  - Mark as "incomplete" if                    < ~80%
		String incompleteSuffix = " (" + ctx.getString(R.string.incomplete_locale) + ")";

		// Add " (Device language)" to system default entry in Latin letters, so it can be more easily identified if a foreign language has been selected by mistake
		String deviceLanguageInLatin = " (" + ctx.getString(R.string.system_locale_no_translate) + ")";
		String systemDeviceLanguage = ctx.getString(R.string.system_locale) + deviceLanguageInLatin;

		Map<String, String> languages = new HashMap<>();
		languages.put("", systemDeviceLanguage);
		languages.put("en", ctx.getString(R.string.lang_en));
		languages.put("af", ctx.getString(R.string.lang_af) + incompleteSuffix);
		languages.put("ar", ctx.getString(R.string.lang_ar));
		languages.put("ast", ctx.getString(R.string.lang_ast) + incompleteSuffix);
		languages.put("az", ctx.getString(R.string.lang_az));
		languages.put("be", ctx.getString(R.string.lang_be));
		languages.put("bg", ctx.getString(R.string.lang_bg));
		languages.put("ca", ctx.getString(R.string.lang_ca));
		languages.put("cs", ctx.getString(R.string.lang_cs));
		languages.put("cy", ctx.getString(R.string.lang_cy) + incompleteSuffix);
		languages.put("da", ctx.getString(R.string.lang_da));
		languages.put("de", ctx.getString(R.string.lang_de));
		languages.put("el", ctx.getString(R.string.lang_el));
		languages.put("en_GB", ctx.getString(R.string.lang_en_gb));
		languages.put("eo", ctx.getString(R.string.lang_eo));
		languages.put("es", ctx.getString(R.string.lang_es));
		languages.put("es_AR", ctx.getString(R.string.lang_es_ar));
		languages.put("es_US", ctx.getString(R.string.lang_es_us));
		languages.put("eu", ctx.getString(R.string.lang_eu));
		languages.put("fa", ctx.getString(R.string.lang_fa));
		languages.put("fi", ctx.getString(R.string.lang_fi) + incompleteSuffix);
		languages.put("fr", ctx.getString(R.string.lang_fr));
		languages.put("gl", ctx.getString(R.string.lang_gl));
		languages.put("iw", ctx.getString(R.string.lang_he));
		languages.put("hr", ctx.getString(R.string.lang_hr) + incompleteSuffix);
		languages.put("hsb", ctx.getString(R.string.lang_hsb) + incompleteSuffix);
		languages.put("hu", ctx.getString(R.string.lang_hu));
		languages.put("hy", ctx.getString(R.string.lang_hy));
		languages.put("id", ctx.getString(R.string.lang_id));
		languages.put("is", ctx.getString(R.string.lang_is));
		languages.put("it", ctx.getString(R.string.lang_it));
		languages.put("ja", ctx.getString(R.string.lang_ja));
		languages.put("ka", ctx.getString(R.string.lang_ka) + incompleteSuffix);
		languages.put("kab", ctx.getString(R.string.lang_kab) + incompleteSuffix);
		languages.put("kn", ctx.getString(R.string.lang_kn) + incompleteSuffix);
		languages.put("ko", ctx.getString(R.string.lang_ko));
		languages.put("lt", ctx.getString(R.string.lang_lt));
		languages.put("lv", ctx.getString(R.string.lang_lv));
		languages.put("mk", ctx.getString(R.string.lang_mk));
		languages.put("ml", ctx.getString(R.string.lang_ml));
		languages.put("mr", ctx.getString(R.string.lang_mr) + incompleteSuffix);
		languages.put("nb", ctx.getString(R.string.lang_nb));
		languages.put("nl", ctx.getString(R.string.lang_nl));
		languages.put("nn", ctx.getString(R.string.lang_nn) + incompleteSuffix);
		languages.put("oc", ctx.getString(R.string.lang_oc) + incompleteSuffix);
		languages.put("pl", ctx.getString(R.string.lang_pl));
		languages.put("pt", ctx.getString(R.string.lang_pt));
		languages.put("pt_BR", ctx.getString(R.string.lang_pt_br));
		languages.put("ro", ctx.getString(R.string.lang_ro) + incompleteSuffix);
		languages.put("ru", ctx.getString(R.string.lang_ru));
		languages.put("sat", ctx.getString(R.string.lang_sat) + incompleteSuffix);
		languages.put("sc", ctx.getString(R.string.lang_sc));
		languages.put("sk", ctx.getString(R.string.lang_sk));
		languages.put("sl", ctx.getString(R.string.lang_sl));
		languages.put("sr", ctx.getString(R.string.lang_sr));
		languages.put("sr+Latn", ctx.getString(R.string.lang_sr_latn));
		languages.put("sv", ctx.getString(R.string.lang_sv));
		languages.put("tr", ctx.getString(R.string.lang_tr));
		languages.put("uk", ctx.getString(R.string.lang_uk));
		languages.put("vi", ctx.getString(R.string.lang_vi) + incompleteSuffix);
		languages.put("zh_CN", ctx.getString(R.string.lang_zh_cn) + incompleteSuffix);
		languages.put("zh_TW", ctx.getString(R.string.lang_zh_tw));

		Map<String, String> sortedLanguages = new TreeMap<>(ConfigureMapUtils.getLanguagesComparator(languages));
		sortedLanguages.putAll(languages);
		return sortedLanguages;
	}
}