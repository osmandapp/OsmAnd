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

import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import org.apache.commons.logging.Log;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

public class LocaleHelper {

	private final static Log log = PlatformUtil.getLog(LocaleHelper.class);

	private final OsmandApplication app;

	private final Locale defaultLocale;
	private final StateChangedListener<String> localeListener;

	private Locale preferredLocale;
	private Resources localizedResources;
	private Configuration localizedConf;

	public LocaleHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.defaultLocale = Locale.getDefault();
		localeListener = change -> onPreferredLocaleChanged();
	}

	public void onCreateApplication() {
		app.getSettings().PREFERRED_LOCALE.addListener(localeListener);
		checkPreferredLocale();
	}

	private void onPreferredLocaleChanged() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			String preferredLocale = app.getSettings().PREFERRED_LOCALE.get();
			if (!Algorithms.isEmpty(preferredLocale)) {
				Locale locale = parseLanguageTag(preferredLocale);
				if (locale != null) {
					Locale.setDefault(locale);
					app.runInUIThread(() -> AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale)));
				}
			}
		}
	}

	public void checkPreferredLocale() {
		OsmandSettings settings = app.getSettings();
		String locale = settings.PREFERRED_LOCALE.get();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			String currentLocale = Locale.getDefault().toLanguageTag();
			if (!Algorithms.stringsEqual(currentLocale, locale)) {
				locale = currentLocale;
				settings.PREFERRED_LOCALE.set(locale);
			}
		}

		boolean useSystemDefault = Algorithms.isEmpty(locale);
		if (!useSystemDefault) {
			Locale parsed = parseLanguageTag(locale);
			if (parsed != null) {
				preferredLocale = parsed;
			}
		}

		Locale selectedLocale = null;
		Configuration config = app.getBaseContext().getResources().getConfiguration();

		if (!useSystemDefault && !Objects.equals(config.getLocales().get(0), preferredLocale)) {
			selectedLocale = preferredLocale;
		} else if (useSystemDefault && defaultLocale != null && !Objects.equals(Locale.getDefault(), defaultLocale)) {
			selectedLocale = defaultLocale;
			preferredLocale = null;
		}

		updateTimeFormatting(selectedLocale != null ? selectedLocale : Locale.getDefault());

		if (selectedLocale != null) {
			Locale.setDefault(selectedLocale);
			Configuration newConfig = new Configuration(config);

			newConfig.setLocales(new android.os.LocaleList(selectedLocale));
			newConfig.setLayoutDirection(selectedLocale);

			Resources resources = app.getBaseContext().getResources();
			resources.updateConfiguration(newConfig, resources.getDisplayMetrics());

			localizedConf = new Configuration(newConfig);
		}
	}

	/**
	 * @deprecated Use {@link SupportedLocale#createLocale(String)} instead.
	 */
	@Nullable
	private Locale parseLanguageTag(@NonNull String languageTag) {
		Locale locale = Locale.forLanguageTag(languageTag);
		return Algorithms.isEmpty(locale.toString()) ? parseLegacyLanguageTag(languageTag) : locale;
	}

	/**
	 * @deprecated Use {@link SupportedLocale#createLocale(String)} instead.
	 */
	@Nullable
	private Locale parseLegacyLanguageTag(@NonNull String locale) {
		// Split locale into language, region, and script
		String[] scriptSplit = locale.split("\\+");
		String baseLocale = scriptSplit[0];
		String script = (scriptSplit.length > 1) ? scriptSplit[1] : "";

		String[] localeSplit = baseLocale.split("_");
		String lang = localeSplit[0];
		String country = (localeSplit.length > 1) ? localeSplit[1] : "";

		// Construct Locale using Builder
		if (!Algorithms.isEmpty(lang)) {
			Locale.Builder builder = new Locale.Builder();
			lang = backwardCompatibleNonIsoCodes(lang);
			for (String isoLang : Locale.getISOLanguages()) {
				if (isoLang.equalsIgnoreCase(lang)) {
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
			return builder.build();
		}
		return null;
	}

	/**
	 * @deprecated Hardcoded language mappings are now handled inside {@link SupportedLocale}.
	 */
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
		return getLanguage(preferredLocale != null ? preferredLocale : Locale.getDefault());
	}

	@NonNull
	public String getLanguage(@NonNull Locale locale) {
		String lang = locale.getLanguage();
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

	@NonNull
	public Resources getLocalizedResources(@NonNull Locale locale) {
		return getLocalizedContext(locale).getResources();
	}

	@NonNull
	public Context getLocalizedContext(@NonNull Locale locale) {
		Configuration configuration = app.getResources().getConfiguration();
		configuration = new Configuration(configuration);
		configuration.setLocale(locale);
		return app.createConfigurationContext(configuration);
	}

	@Nullable
	public static Locale getPreferredNameLocale(@NonNull OsmandApplication app,
			@NonNull Collection<String> localeIds) {
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
	public static String getPreferredPlacesLanguage(@NonNull OsmandApplication app) {
		String locale = app.getSettings().MAP_PREFERRED_LOCALE.get();
		return Algorithms.isEmpty(locale) ? app.getLanguage() : locale;
	}
}