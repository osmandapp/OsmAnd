package net.osmand.test.common;

import android.content.res.Configuration;
import android.content.res.Resources;

import android.util.Log;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;

import java.util.Locale;

import java.util.Set;

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

	public static void setLocale(@NonNull OsmandApplication app, Locale locale) {
		Locale.setDefault(locale);
		app.getLocaleHelper().checkPreferredLocale();
	}

	public static boolean isShowWikiOnMap(@NonNull OsmandApplication app) {
		PoiFiltersHelper helper = app.getPoiFilters();
		if (helper != null) {
			Set<PoiUIFilter> filters = helper.getSelectedPoiFilters();
			for (PoiUIFilter filter : filters) {
				if (filter.isWikiFilter()) {
					return true;
				}
			}
		}
		return false;
	}
}