package net.osmand.plus.helpers;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import java.util.Locale;

public class LocaleHelper {

	private final OsmandApplication app;

	private Locale defaultLocale;
	private Locale preferredLocale;
	private Resources localizedResources;

	public LocaleHelper(OsmandApplication app) {
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
			config.locale = selectedLocale;
			config.setLayoutDirection(selectedLocale);

			Resources resources = app.getBaseContext().getResources();
			resources.updateConfiguration(config, resources.getDisplayMetrics());
			if (android.os.Build.VERSION.SDK_INT >= 17) {
				Configuration conf = new Configuration(config);
				conf.locale = selectedLocale;
				localizedResources = app.createConfigurationContext(conf).getResources();
			}
		}
	}

	public void setLanguage(Context context) {
		if (preferredLocale != null) {
			Configuration config = context.getResources().getConfiguration();
			String lang = preferredLocale.getLanguage();
			if (!Algorithms.isEmpty(lang) && !config.locale.getLanguage().equals(lang)) {
				preferredLocale = new Locale(lang);
				Locale.setDefault(preferredLocale);
				config.locale = preferredLocale;
				context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
			} else if (Algorithms.isEmpty(lang) && defaultLocale != null && Locale.getDefault() != defaultLocale) {
				Locale.setDefault(defaultLocale);
				config.locale = defaultLocale;
				Resources resources = app.getBaseContext().getResources();
				resources.updateConfiguration(config, resources.getDisplayMetrics());
			}
		}
	}

	public Resources getLocalizedResources() {
		return localizedResources;
	}

	public Locale getPreferredLocale() {
		return preferredLocale;
	}

	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	public String getCountry() {
		String country;
		if (preferredLocale != null) {
			country = preferredLocale.getCountry();
		} else {
			country = Locale.getDefault().getCountry();
		}
		return country;
	}

	public String getLanguage() {
		String lang;
		if (preferredLocale != null) {
			lang = preferredLocale.getLanguage();
		} else {
			lang = Locale.getDefault().getLanguage();
		}
		if (lang != null && lang.length() > 3) {
			lang = lang.substring(0, 2).toLowerCase();
		}
		return lang;
	}
}