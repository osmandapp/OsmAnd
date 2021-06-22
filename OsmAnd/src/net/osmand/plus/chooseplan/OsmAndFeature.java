package net.osmand.plus.chooseplan;

import net.osmand.plus.R;

public enum OsmAndFeature {

	OSMAND_CLOUD(R.string.osmand_cloud, R.string.purchases_feature_desc_osmand_cloud, R.drawable.ic_action_cloud_upload_colored_day, R.drawable.ic_action_cloud_upload_colored_night, false),
	ADVANCED_WIDGETS(R.string.advanced_widgets, R.string.purchases_feature_desc_advanced_widgets, R.drawable.ic_action_advanced_widgets_colored_day, R.drawable.ic_action_advanced_widgets_colored_night, false),
	HOURLY_MAP_UPDATES(R.string.daily_map_updates, R.string.purchases_feature_desc_hourly_map_updates, R.drawable.ic_action_map_updates_colored_day, R.drawable.ic_action_map_updates_colored_night, false),
	MONTHLY_MAP_UPDATES(R.string.monthly_map_updates, R.string.purchases_feature_desc_monthly_map_updates, R.drawable.ic_action_monthly_map_updates_colored_day, R.drawable.ic_action_monthly_map_updates_colored_night, true),
	UNLIMITED_MAP_DOWNLOADS(R.string.unlimited_map_downloads, R.string.purchases_feature_desc_unlimited_map_download, R.drawable.ic_action_unlimited_downloads_colored_day, R.drawable.ic_action_unlimited_download_colored_night, true),
	COMBINED_WIKI(R.string.wikipedia_and_wikivoyage_offline, R.string.purchases_feature_desc_combined_wiki, R.drawable.ic_action_wikipedia_download_colored_day, R.drawable.ic_action_wikipedia_download_colored_night, true),
	WIKIPEDIA(R.string.shared_string_wikipedia, R.string.offline_wikipeadia, R.string.purchases_feature_desc_wikipedia, R.drawable.ic_action_wikipedia_download_colored_day, R.drawable.ic_action_wikipedia_download_colored_night, true),
	WIKIVOYAGE(R.string.shared_string_wikivoyage, R.string.offline_wikivoyage, R.string.purchases_feature_desc_wikivoyage, R.drawable.ic_action_backpack_colored_day, R.drawable.ic_action_backpack_colored_night, true),
	TERRAIN(R.string.terrain_maps, R.string.terrain_maps_contour_lines_hillshade_slope, R.string.purchases_feature_desc_terrain, R.drawable.ic_action_srtm_colored_day, R.drawable.ic_action_srtm_colored_day, true),
	NAUTICAL(R.string.nautical_depth, R.string.purchases_feature_desc_nautical, R.drawable.ic_action_nautical_depth_colored_day, R.drawable.ic_action_nautical_depth_colored_night, true);


	private final int headerTitleId;
	private final int listTitleId;
	private final int descriptionId;
	private final int iconDayId;
	private final int iconNightId;
	private final boolean availableInMapsPlus;

	OsmAndFeature(int titleId,
				  int descriptionId,
				  int iconDayId,
				  int iconNightId,
				  boolean availableInMapsPlus) {
		// constructor for features with the same titles in header and list
		this(titleId, titleId, descriptionId, iconDayId, iconNightId, availableInMapsPlus);
	}

	OsmAndFeature(int headerTitleId,
				  int listTitleId,
				  int descriptionId,
				  int iconDayId,
				  int iconNightId,
				  boolean availableInMapsPlus) {
		this.headerTitleId = headerTitleId;
		this.listTitleId = listTitleId;
		this.descriptionId = descriptionId;
		this.iconDayId = iconDayId;
		this.iconNightId = iconNightId;
		this.availableInMapsPlus = availableInMapsPlus;
	}

	public int getHeaderTitleId() {
		return headerTitleId;
	}

	public int getListTitleId() {
		return listTitleId;
	}

	public int getDescriptionId() {
		return descriptionId;
	}

	public boolean isAvailableInMapsPlus() {
		return availableInMapsPlus;
	}

	public int getIconId(boolean nightMode) {
		return nightMode ? iconNightId : iconDayId;
	}
}