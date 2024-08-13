package net.osmand.plus.track.cards;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXActivityUtils;
import net.osmand.gpx.GPXUtilities;
import net.osmand.osm.OsmRouteType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.util.Algorithms;

public class InfoCard extends BaseMetadataCard {

	private final RouteKey routeKey;

	public InfoCard(@NonNull MapActivity mapActivity, @NonNull GPXUtilities.Metadata metadata, @Nullable RouteKey routeKey) {
		super(mapActivity, metadata);
		this.routeKey = routeKey;
	}

	@Override
	@StringRes
	protected int getTitleId() {
		return R.string.info_button;
	}

	@Override
	public void updateContent() {
		super.updateContent();

		OsmRouteType activityType = GPXActivityUtils.fetchActivityType(metadata, routeKey);
		String keywords = metadata != null ? metadata.keywords : null;
		String link = metadata != null ? metadata.link : null;
		boolean keywordsAvailable = !Algorithms.isEmpty(keywords);
		boolean linkAvailable = !Algorithms.isEmpty(link);

		String title = activityType != null
				? AndroidUtils.getActivityTypeTitle(app, activityType)
				: app.getString(R.string.shared_string_none);

		Drawable icon = activityType != null
				? getContentIcon(AndroidUtils.getActivityTypeIcon(app, activityType))
				: getContentIcon(R.drawable.ic_action_bicycle_dark); // todo use ic_action_activity

		createItemRow(getString(R.string.shared_string_activity), title, icon).setOnClickListener(v -> {
			// todo show select activity screen
		});
		if (keywordsAvailable) {
			createItemRow(getString(R.string.shared_string_keywords), metadata.keywords, getContentIcon(R.drawable.ic_action_label));
		}
		if (linkAvailable) {
			createLinkItemRow(getString(R.string.shared_string_link), metadata.link, R.drawable.ic_action_link);
		}
	}
}