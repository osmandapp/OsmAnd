package net.osmand.test.common;

import android.content.res.Configuration;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;

import java.util.Locale;

public class AppSettings {
	public static void showFavorites(@NonNull OsmandApplication app, boolean show) {
		app.getSettings().SHOW_FAVORITES.set(show);
	}

	public static void showWikiOnMap(@NonNull OsmandApplication app) {
		PoiFiltersHelper helper = app.getPoiFilters();
		PoiUIFilter filter = helper.getFilterById("std_osmwiki");
		if (filter != null) {
			helper.loadSelectedPoiFilters();
			helper.addSelectedPoiFilter(filter);
		}
	}

	public static void setLocale(@NonNull OsmandApplication app, String language, String country) {
		Locale locale = new Locale(language, country);
		Locale.setDefault(locale);
		Resources res = app.getResources();
		Configuration config = res.getConfiguration();
		config.locale = locale;
		res.updateConfiguration(config, res.getDisplayMetrics());
	}
}