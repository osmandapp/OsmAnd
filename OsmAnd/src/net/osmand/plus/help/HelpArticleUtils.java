package net.osmand.plus.help;

import static net.osmand.plus.help.LoadArticlesTask.DOCS_LINKS_URL;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
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
	public static String getArticleName(@NonNull Context ctx, @NonNull HelpArticle article) {
		return getArticleName(ctx, article.url, article.label);
	}

	@NonNull
	public static String getArticleName(@NonNull Context ctx, @NonNull String url, @NonNull String defaultName) {
		String key = url.isEmpty() ? defaultName.toLowerCase(Locale.US) : url;
		String propertyName = getArticlePropertyName(key);

		String name = getArticlePairedName(ctx, propertyName);
		if (name == null) {
			name = AndroidUtils.getStringByProperty(ctx, "help_article_" + propertyName + "_name");
		}
		return name != null ? name : defaultName;
	}

	@NonNull
	public static String getArticlePropertyName(@NonNull String key) {
		if (key.endsWith("/")) {
			key = key.substring(0, key.length() - 1);
		}
		String name = key.replace(DOCS_LINKS_URL, "").replace("-", "_")
				.replace("/", "_").replace(" ", "_");

		if (!Algorithms.isEmpty(name) && name.charAt(name.length() - 1) == '_') {
			name = name.substring(0, name.length() - 1);
		}
		return name;
	}

	@Nullable
	private static String getArticlePairedName(@NonNull Context ctx, @NonNull String key) {
		switch (key) {
			case "plugins":
				return ctx.getString(R.string.plugins_menu_group);
			case "plugins_accessibility":
				return ctx.getString(R.string.shared_string_accessibility);
			case "plugins_audio_video_notes":
				return ctx.getString(R.string.audionotes_plugin_name);
			case "plugins_development":
				return ctx.getString(R.string.debugging_and_development);
			case "plugins_external_sensors":
				return ctx.getString(R.string.external_sensors_plugin_name);
			case "plugins_mapillary":
				return ctx.getString(R.string.mapillary);
			case "plugins_osm_editing":
				return ctx.getString(R.string.osm_editing_plugin_name);
			case "plugins_osmand_tracker":
				return ctx.getString(R.string.tracker_item);
			case "plugins_parking":
				return ctx.getString(R.string.osmand_parking_plugin_name);
			case "plugins_trip_recording":
				return ctx.getString(R.string.record_plugin_name);
			case "plugins_weather":
				return ctx.getString(R.string.shared_string_weather);
			case "plugins_wikipedia":
				return ctx.getString(R.string.shared_string_wikipedia);
			case "plugins_online_map":
				return ctx.getString(R.string.shared_string_online_maps);
			case "plugins_ski_maps":
				return ctx.getString(R.string.plugin_ski_name);
			case "plugins_nautical_charts":
				return ctx.getString(R.string.plugin_nautical_name);
			case "plugins_contour_lines":
				return ctx.getString(R.string.srtm_plugin_name);
			case "plugins_custom":
				return ctx.getString(R.string.custom_osmand_plugin);
			case "search":
				return ctx.getString(R.string.shared_string_search);
			case "map_legend":
				return ctx.getString(R.string.map_legend);
			case "map":
				return ctx.getString(R.string.shared_string_map);
			case "map_configure_map_menu":
				return ctx.getString(R.string.configure_map);
			case "map_public_transport":
				return ctx.getString(R.string.app_mode_public_transport);
			case "navigation":
			case "troubleshooting_navigation":
				return ctx.getString(R.string.shared_string_navigation);
			case "navigation_guidance_navigation_settings":
				return ctx.getString(R.string.routing_settings_2);
			case "navigation_routing":
				return ctx.getString(R.string.route_parameters);
			case "navigation_setup_route_details":
				return ctx.getString(R.string.show_route);
			case "personal_favorites":
				return ctx.getString(R.string.shared_string_favorites);
			case "personal_global_settings":
				return ctx.getString(R.string.global_settings);
			case "personal_maps":
				return ctx.getString(R.string.shared_string_maps);
			case "personal_markers":
				return ctx.getString(R.string.shared_string_markers);
			case "personal_myplaces":
				return ctx.getString(R.string.shared_string_my_places);
			case "personal_osmand_cloud":
				return ctx.getString(R.string.osmand_cloud);
			case "personal_tracks":
				return ctx.getString(R.string.shared_string_gpx_tracks);
			case "plan_route_create_route":
				return ctx.getString(R.string.plan_a_route);
			case "plan_route_travel_guides":
				return ctx.getString(R.string.wikivoyage_travel_guide);
			case "purchases":
				return ctx.getString(R.string.purchases);
			case "search_search_address":
				return ctx.getString(R.string.search_address);
			case "search_search_history":
				return ctx.getString(R.string.shared_string_search_history);
			case "start_with_download_maps":
				return ctx.getString(R.string.welmode_download_maps);
			case "troubleshooting":
				return ctx.getString(R.string.troubleshooting);
			case "navigation_setup":
			case "troubleshooting_setup":
				return ctx.getString(R.string.shared_string_setup);
			case "widgets_configure_screen":
				return ctx.getString(R.string.map_widget_config);
			case "widgets_quick_action":
				return ctx.getString(R.string.quick_action_item);
		}
		return null;
	}

	@DrawableRes
	public static int getArticleIconId(@NonNull HelpArticle article) {
		String key = getArticlePropertyName(article.url);
		switch (key) {
			case "track-recording-issues":
				return R.drawable.ic_action_track_recordable;
			case "navigation":
				return R.drawable.ic_action_gdirections_dark;
			case "maps-data":
				return R.drawable.ic_action_layers;
			case "setup":
				return R.drawable.ic_action_device_download;
			default:
				return R.drawable.ic_action_book_info;
		}
	}

	@NonNull
	public static ContextMenuItem createCategory(@NonNull String title) {
		return new ContextMenuItem(null)
				.setTitle(title)
				.setCategory(true)
				.setLayout(R.layout.help_category_header);
	}

	@NonNull
	public static ContextMenuItem createMenuItem(@NonNull String title, @Nullable String description,
	                                             @DrawableRes int iconId, @Nullable ItemClickListener listener) {
		return new ContextMenuItem(null)
				.setIcon(iconId)
				.setTitle(title)
				.setDescription(description)
				.setListener(listener);
	}

	@NonNull
	public static ContextMenuItem createArticleItem(@NonNull FragmentActivity activity, @NonNull HelpArticle article) {
		String title = HelpArticleUtils.getArticleName(activity, article);
		return new ContextMenuItem(null)
				.setTitle(title)
				.setIcon(R.drawable.ic_action_book_info)
				.setListener((uiAdapter, view, item, isChecked) -> {
					FragmentManager manager = activity.getSupportFragmentManager();
					if (Algorithms.isEmpty(article.articles)) {
						HelpArticleDialogFragment.showInstance(manager, article.url, title);
					} else {
						HelpArticlesFragment.showInstance(manager, article);
					}
					return false;
				});
	}

	@NonNull
	public static ItemClickListener getUrlItemClickListener(@NonNull FragmentActivity activity, @NonNull String url) {
		return (uiAdapter, view, item, isChecked) -> {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			AndroidUtils.startActivityIfSafe(activity, intent);
			return false;
		};
	}

	@NonNull
	public static ItemClickListener getArticleItemClickListener(@NonNull FragmentActivity activity, @NonNull String title, @NonNull String url) {
		return (uiAdapter, view, item, isChecked) -> {
			FragmentManager manager = activity.getSupportFragmentManager();
			HelpArticleDialogFragment.showInstance(manager, url, title);
			return false;
		};
	}
}