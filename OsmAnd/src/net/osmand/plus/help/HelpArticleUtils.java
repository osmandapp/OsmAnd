package net.osmand.plus.help;

import static net.osmand.plus.help.LoadArticlesTask.DOCS_LINKS_URL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import java.util.Locale;

public class HelpArticleUtils {

	@NonNull
	public static String getTelegramChatName(@NonNull OsmandApplication app, @NonNull String key) {
		int startIndex = key.indexOf("(");
		int endIndex = key.indexOf(")");

		if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
			String langKey = key.substring(startIndex + 1, endIndex);
			String value = AndroidUtils.getStringByProperty(app, "lang_" + langKey);
			if (!Algorithms.isEmpty(value)) {
				String telegram = app.getString(R.string.telegram);
				return app.getString(R.string.ltr_or_rtl_combine_via_space, telegram, value);
			}
		}
		return key;
	}

	@NonNull
	public static String getArticleName(@NonNull OsmandApplication app, @NonNull String url) {
		String propertyName = getArticlePropertyName(url);

		String name = getArticlePairedName(app, propertyName);
		if (name == null) {
			name = AndroidUtils.getStringByProperty(app, "help_article_" + propertyName + "_name");
		}
		return name != null ? name : Algorithms.capitalizeFirstLetterAndLowercase(propertyName.replace("_", " "));
	}

	@NonNull
	private static String getArticlePropertyName(@NonNull String url) {
		String propertyName = url.toLowerCase(Locale.US).replace(DOCS_LINKS_URL, "")
				.replace("-", "_").replace("/", "_");

		if (!Algorithms.isEmpty(propertyName) && propertyName.charAt(propertyName.length() - 1) == '_') {
			propertyName = propertyName.substring(0, propertyName.length() - 1);
		}
		return propertyName;
	}

	@Nullable
	private static String getArticlePairedName(@NonNull OsmandApplication app, @NonNull String key) {
		switch (key) {
			case "plugins":
				return app.getString(R.string.plugins_menu_group);
			case "plugins_accessibility":
				return app.getString(R.string.shared_string_accessibility);
			case "plugins_audio_video_notes":
				return app.getString(R.string.audionotes_plugin_name);
			case "plugins_development":
				return app.getString(R.string.debugging_and_development);
			case "plugins_external_sensors":
				return app.getString(R.string.external_sensors_plugin_name);
			case "plugins_mapillary":
				return app.getString(R.string.mapillary);
			case "plugins_openplacereviews":
				return app.getString(R.string.open_place_reviews);
			case "plugins_osm_editing":
				return app.getString(R.string.osm_editing_plugin_name);
			case "plugins_osmand_tracker":
				return app.getString(R.string.tracker_item);
			case "plugins_parking":
				return app.getString(R.string.osmand_parking_plugin_name);
			case "plugins_trip_recording":
				return app.getString(R.string.record_plugin_name);
			case "plugins_weather":
				return app.getString(R.string.shared_string_weather);
			case "plugins_wikipedia":
				return app.getString(R.string.shared_string_wikipedia);
			case "plugins_online_map":
				return app.getString(R.string.shared_string_online_maps);
			case "plugins_ski_maps":
				return app.getString(R.string.plugin_ski_name);
			case "plugins_nautical_charts":
				return app.getString(R.string.plugin_nautical_name);
			case "plugins_contour_lines":
				return app.getString(R.string.srtm_plugin_name);
			case "search":
				return app.getString(R.string.shared_string_search);
			case "map_legend":
				return app.getString(R.string.map_legend);
			case "map":
				return app.getString(R.string.shared_string_map);
			case "map_configure_map_menu":
				return app.getString(R.string.configure_map);
			case "map_public_transport":
				return app.getString(R.string.app_mode_public_transport);
			case "navigation":
			case "troubleshooting_navigation":
				return app.getString(R.string.shared_string_navigation);
			case "navigation_guidance_navigation_settings":
				return app.getString(R.string.routing_settings_2);
			case "navigation_routing":
				return app.getString(R.string.route_parameters);
			case "navigation_setup_route_details":
				return app.getString(R.string.show_route);
			case "personal_favorites":
				return app.getString(R.string.shared_string_favorites);
			case "personal_global_settings":
				return app.getString(R.string.global_settings);
			case "personal_maps":
				return app.getString(R.string.shared_string_maps);
			case "personal_markers":
				return app.getString(R.string.shared_string_markers);
			case "personal_myplaces":
				return app.getString(R.string.shared_string_my_places);
			case "personal_osmand_cloud":
				return app.getString(R.string.osmand_cloud);
			case "personal_tracks":
				return app.getString(R.string.shared_string_gpx_tracks);
			case "plan_route_create_route":
				return app.getString(R.string.plan_a_route);
			case "plan_route_travel_guides":
				return app.getString(R.string.wikivoyage_travel_guide);
			case "purchases":
				return app.getString(R.string.purchases);
			case "search_search_address":
				return app.getString(R.string.search_address);
			case "search_search_history":
				return app.getString(R.string.shared_string_search_history);
			case "start_with_download_maps":
				return app.getString(R.string.welmode_download_maps);
			case "troubleshooting":
				return app.getString(R.string.troubleshooting);
			case "navigation_setup":
			case "troubleshooting_setup":
				return app.getString(R.string.shared_string_setup);
			case "widgets_configure_screen":
				return app.getString(R.string.map_widget_config);
			case "widgets_quick_action":
				return app.getString(R.string.quick_action_item);
		}
		return null;
	}
}