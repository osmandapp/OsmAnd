package net.osmand.plus.chooseplan;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;

import java.util.Arrays;
import java.util.List;

public enum OsmAndFeature {

	OSMAND_CLOUD(R.string.osmand_cloud, R.string.purchases_feature_desc_osmand_cloud, R.drawable.ic_action_cloud_upload_colored_day, R.drawable.ic_action_cloud_upload_colored_night),
	ADVANCED_WIDGETS(R.string.pro_features, R.string.purchases_feature_desc_pro_widgets, R.drawable.ic_action_pro_features_colored, R.drawable.ic_action_pro_features_colored_night),
	HOURLY_MAP_UPDATES(R.string.daily_map_updates, R.string.purchases_feature_desc_hourly_map_updates, R.drawable.ic_action_map_updates_colored_day, R.drawable.ic_action_map_updates_colored_night),
	CROSS_BUY(R.string.shared_string_cross_buy, R.string.purchases_feature_desc_cross_buy, R.drawable.ic_action_cross_buy_colored_day, R.drawable.ic_action_cross_buy_colored_night),
	WEATHER(R.string.shared_string_weather, R.string.weather_plugin_description, R.drawable.ic_action_umbrella_colored, R.drawable.ic_action_umbrella_colored_night),
	RELIEF_3D(R.string.relief_3d, R.string.relief_3d_description, R.drawable.ic_action_3d_relief_colored_day, R.drawable.ic_action_3d_relief_colored_night),
	MONTHLY_MAP_UPDATES(R.string.monthly_map_updates, R.string.purchases_feature_desc_monthly_map_updates, R.drawable.ic_action_monthly_map_updates_colored_day, R.drawable.ic_action_monthly_map_updates_colored_night),
	UNLIMITED_MAP_DOWNLOADS(R.string.unlimited_map_downloads, R.string.purchases_feature_desc_unlimited_map_download, R.drawable.ic_action_unlimited_downloads_colored_day, R.drawable.ic_action_unlimited_download_colored_night),
	ANDROID_AUTO(R.string.android_auto, R.string.purchases_feature_desc_android_auto, R.drawable.ic_action_android_auto_colored, R.drawable.ic_action_android_auto_colored_night),
	COMBINED_WIKI(R.string.wikipedia_and_wikivoyage_offline, R.string.purchases_feature_desc_combined_wiki, R.drawable.ic_action_wikipedia_download_colored_day, R.drawable.ic_action_wikipedia_download_colored_night),
	WIKIPEDIA(R.string.offline_wikipeadia, R.string.purchases_feature_desc_wikipedia, R.drawable.ic_action_wikipedia_download_colored_day, R.drawable.ic_action_wikipedia_download_colored_night),
	WIKIVOYAGE(R.string.offline_wikivoyage, R.string.purchases_feature_desc_wikivoyage, R.drawable.ic_action_backpack_colored_day, R.drawable.ic_action_backpack_colored_night),
	EXTERNAL_SENSORS_SUPPORT(R.string.external_sensors_support, R.string.purchases_feature_desc_external_sensors, R.drawable.ic_action_external_sensor_colored_day, R.drawable.ic_action_external_sensor_colored_night),
	TERRAIN(R.string.srtm_plugin_name, R.string.contour_lines_hillshade_slope, R.string.purchases_feature_desc_terrain, R.drawable.ic_action_srtm_colored_day, R.drawable.ic_action_srtm_colored_day),
	NAUTICAL(R.string.nautical_depth, R.string.purchases_feature_desc_nautical, R.drawable.ic_action_nautical_depth_colored_day, R.drawable.ic_action_nautical_depth_colored_night);

	private final int titleId;
	private final int listTitleId;
	private final int descriptionId;
	private final int iconDayId;
	private final int iconNightId;

	// constructor for features with the same titles in header and list
	OsmAndFeature(int titleId, int descriptionId, int iconDayId, int iconNightId) {
		this(titleId, titleId, descriptionId, iconDayId, iconNightId);
	}

	OsmAndFeature(int titleId, int listTitleId, int descriptionId, int iconDayId, int iconNightId) {
		this.titleId = titleId;
		this.listTitleId = listTitleId;
		this.descriptionId = descriptionId;
		this.iconDayId = iconDayId;
		this.iconNightId = iconNightId;
	}

	@StringRes
	public int getTitleId() {
		return titleId;
	}

	@StringRes
	public int getListTitleId() {
		return listTitleId;
	}

	public String getDescription(@NonNull Context ctx) {
		if (this == WIKIPEDIA) {
			return ctx.getString(descriptionId, R.string.weather_global_forecast_system);
		}
		return ctx.getString(descriptionId);
	}

	@DrawableRes
	public int getIconId(boolean nightMode) {
		return nightMode ? iconNightId : iconDayId;
	}

	public boolean isAvailableInMapsPlus() {
		return MAPS_PLUS_FEATURES.contains(this);
	}

	public static final List<OsmAndFeature> OSMAND_PRO_FEATURES = Arrays.asList(
			OSMAND_CLOUD,
			ADVANCED_WIDGETS,
			HOURLY_MAP_UPDATES,
			CROSS_BUY,
			MONTHLY_MAP_UPDATES,
			UNLIMITED_MAP_DOWNLOADS,
			ANDROID_AUTO,
			COMBINED_WIKI,
			WIKIPEDIA,
			WIKIVOYAGE,
			EXTERNAL_SENSORS_SUPPORT,
			TERRAIN,
			NAUTICAL,
			WEATHER,
			RELIEF_3D
	);

	public static final List<OsmAndFeature> OSMAND_PRO_PREVIEW_FEATURES = Arrays.asList(
			OSMAND_CLOUD,
			ADVANCED_WIDGETS,
			HOURLY_MAP_UPDATES,
			CROSS_BUY,
			UNLIMITED_MAP_DOWNLOADS,
			ANDROID_AUTO,
			COMBINED_WIKI,
			EXTERNAL_SENSORS_SUPPORT,
			TERRAIN,
			NAUTICAL,
			WEATHER,
			RELIEF_3D
	);

	public static final List<OsmAndFeature> MAPS_PLUS_FEATURES = Arrays.asList(
			MONTHLY_MAP_UPDATES,
			UNLIMITED_MAP_DOWNLOADS,
			ANDROID_AUTO,
			COMBINED_WIKI,
			WIKIPEDIA,
			WIKIVOYAGE,
			EXTERNAL_SENSORS_SUPPORT,
			TERRAIN,
			NAUTICAL);

	public static final List<OsmAndFeature> MAPS_PLUS_PREVIEW_FEATURES = Arrays.asList(
			OSMAND_CLOUD,
			ADVANCED_WIDGETS,
			HOURLY_MAP_UPDATES,
			CROSS_BUY,
			MONTHLY_MAP_UPDATES,
			UNLIMITED_MAP_DOWNLOADS,
			ANDROID_AUTO,
			COMBINED_WIKI,
			EXTERNAL_SENSORS_SUPPORT,
			TERRAIN,
			NAUTICAL
	);
}
