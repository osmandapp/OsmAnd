package net.osmand.plus.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.format.DateFormat;

import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LocaleHelper {

	private final OsmandApplication app;

	private Locale defaultLocale;
	private Locale preferredLocale;
	private Resources localizedResources;

	public LocaleHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void checkPreferredLocale() {
		Configuration config = app.getBaseContext().getResources().getConfiguration();

		String pl = app.getSettings().PREFERRED_LOCALE.get();
		String[] split = pl.split("_");
		String lang = split[0];
		String country = (split.length > 1) ? split[1] : "";

		if (defaultLocale == null) {
			defaultLocale = Locale.getDefault();
		}
		if (!Algorithms.isEmpty(lang)) {
			if (!Algorithms.isEmpty(country)) {
				preferredLocale = new Locale(lang, country);
			} else {
				preferredLocale = new Locale(lang);
			}
		}
		Locale selectedLocale = null;

		if (!Algorithms.isEmpty(lang) && !config.locale.equals(preferredLocale)) {
			selectedLocale = preferredLocale;
		} else if (Algorithms.isEmpty(lang) && defaultLocale != null && Locale.getDefault() != defaultLocale) {
			selectedLocale = defaultLocale;
			preferredLocale = null;
		}
		if (selectedLocale != null) {
			Locale.setDefault(selectedLocale);
			updateOpeningHoursParser(selectedLocale);
			config.locale = selectedLocale;
			config.setLayoutDirection(selectedLocale);

			Resources resources = app.getBaseContext().getResources();
			resources.updateConfiguration(config, resources.getDisplayMetrics());
			Configuration conf = new Configuration(config);
			conf.locale = selectedLocale;
			localizedResources = app.createConfigurationContext(conf).getResources();
		}
	}

	public void setLanguage(@NonNull Context context) {
		if (preferredLocale != null) {
			Configuration config = context.getResources().getConfiguration();
			String lang = preferredLocale.getLanguage();
			if (!Algorithms.isEmpty(lang) && !config.locale.getLanguage().equals(lang)) {
				preferredLocale = new Locale(lang);
				Locale.setDefault(preferredLocale);
				updateOpeningHoursParser(preferredLocale);
				config.locale = preferredLocale;
				context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
			} else if (Algorithms.isEmpty(lang) && defaultLocale != null && Locale.getDefault() != defaultLocale) {
				Locale.setDefault(defaultLocale);
				updateOpeningHoursParser(defaultLocale);
				config.locale = defaultLocale;
				Resources resources = app.getBaseContext().getResources();
				resources.updateConfiguration(config, resources.getDisplayMetrics());
			}
		}
	}

	@Nullable
	public Resources getLocalizedResources() {
		return localizedResources;
	}

	@Nullable
	public Locale getPreferredLocale() {
		return preferredLocale;
	}

	@Nullable
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

	private void updateOpeningHoursParser(@NonNull Locale locale) {
		OpeningHoursParser.setTwelveHourFormattingEnabled(!DateFormat.is24HourFormat(app), locale);
	}
}