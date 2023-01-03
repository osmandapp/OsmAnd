package net.osmand.plus.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.Locale;

public class LocaleHelper {

	private final OsmandApplication app;

	private final Locale defaultLocale;
	private Locale preferredLocale;
	private Resources localizedResources;

	public LocaleHelper(@NonNull OsmandApplication app) {
		this.app = app;
		this.defaultLocale = Locale.getDefault();
	}

	public void checkPreferredLocale() {
		Configuration config = app.getBaseContext().getResources().getConfiguration();

		String pl = app.getSettings().PREFERRED_LOCALE.get();
		String[] splitScript = pl.split("\\+");
		String script = (splitScript.length > 1) ? splitScript[1] : "";
		String[] splitCountry = splitScript[0].split("_");
		String lang = splitCountry[0];
		String country = (splitCountry.length > 1) ? splitCountry[1] : "";

		if (!Algorithms.isEmpty(lang)) {
			Locale.Builder builder = new Locale.Builder();
			lang = backwardCompatibleNonIsoCodes(lang);
			String langLowerCase = lang.toLowerCase();
			for (String locale : Locale.getISOLanguages()) {
				if (locale.toLowerCase().equals(langLowerCase)) {
					builder.setLanguage(lang);
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
			Configuration conf = new Configuration(config);
			conf.locale = selectedLocale;
			localizedResources = app.createConfigurationContext(conf).getResources();
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
	public Resources getLocalizedResources() {
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
}