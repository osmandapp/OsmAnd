package net.osmand.plus.chooseplan;

import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.srtmplugin.SRTMPlugin;

public enum OsmAndFeatureOld {
	WIKIVOYAGE_OFFLINE(R.string.wikivoyage_offline, R.drawable.ic_action_explore),
	DAILY_MAP_UPDATES(R.string.daily_map_updates, R.drawable.ic_action_time_span),
	MONTHLY_MAP_UPDATES(R.string.monthly_map_updates, R.drawable.ic_action_sand_clock),
	UNLIMITED_DOWNLOADS(R.string.unlimited_downloads, R.drawable.ic_action_unlimited_download),
	WIKIPEDIA_OFFLINE(R.string.wikipedia_offline, R.drawable.ic_plugin_wikipedia),
	CONTOUR_LINES_HILLSHADE_MAPS(R.string.contour_lines_hillshade_maps, R.drawable.ic_plugin_srtm),
	SEA_DEPTH_MAPS(R.string.index_item_depth_contours_osmand_ext, R.drawable.ic_action_nautical_depth),
	UNLOCK_ALL_FEATURES(R.string.unlock_all_features, R.drawable.ic_action_osmand_logo),
	DONATION_TO_OSM(R.string.donation_to_osm, 0);

	private final int key;
	private final int iconId;

	OsmAndFeatureOld(int key, int iconId) {
		this.key = key;
		this.iconId = iconId;
	}

	public String toHumanString(Context ctx) {
		return ctx.getString(key);
	}

	public int getIconId() {
		return iconId;
	}

	public boolean isFeaturePurchased(OsmandApplication ctx) {
		if (InAppPurchaseHelper.isSubscribedToLiveUpdates(ctx)) {
			return true;
		}
		switch (this) {
			case DAILY_MAP_UPDATES:
			case DONATION_TO_OSM:
			case UNLOCK_ALL_FEATURES:
				return false;
			case MONTHLY_MAP_UPDATES:
			case UNLIMITED_DOWNLOADS:
			case WIKIPEDIA_OFFLINE:
			case WIKIVOYAGE_OFFLINE:
				return !Version.isFreeVersion(ctx) || InAppPurchaseHelper.isFullVersionPurchased(ctx);
			case SEA_DEPTH_MAPS:
				return InAppPurchaseHelper.isDepthContoursPurchased(ctx);
			case CONTOUR_LINES_HILLSHADE_MAPS:
				return OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null || InAppPurchaseHelper.isContourLinesPurchased(ctx);
		}
		return false;
	}
}