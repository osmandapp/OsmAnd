package net.osmand.test.common;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.wikipedia.WikipediaPlugin;

public class AppSettings {
	public static void showFavorites(@NonNull OsmandApplication app, boolean show) {
		app.getSettings().SHOW_FAVORITES.set(show);
	}

	public static void showWikiOnMap(@NonNull OsmandApplication app) {
		PoiFiltersHelper helper = app.getPoiFilters();
		WikipediaPlugin plugin = PluginsHelper.getActivePlugin(WikipediaPlugin.class);
		if (plugin != null) {
			PoiUIFilter filter = plugin.getTopWikiPoiFilter();
			if (filter != null) {
				helper.loadSelectedPoiFilters();
				helper.addSelectedPoiFilter(filter);
			}
		}
	}
}