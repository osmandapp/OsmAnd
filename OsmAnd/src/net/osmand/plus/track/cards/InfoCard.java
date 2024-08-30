package net.osmand.plus.track.cards;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.OnCompleteCallback;
import net.osmand.gpx.GPXActivityUtils;
import net.osmand.gpx.GPXUtilities;
import net.osmand.osm.OsmRouteType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.fragments.controller.RouteActivityController;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.router.network.NetworkRouteSelector.RouteKey;
import net.osmand.shared.gpx.primitives.Metadata;
import net.osmand.util.Algorithms;

public class InfoCard extends BaseMetadataCard {

	private final RouteKey routeKey;
	private final OnCompleteCallback onActivitySelectionComplete;

	public InfoCard(@NonNull MapActivity mapActivity, @NonNull Metadata metadata,
	                @Nullable RouteKey routeKey, @NonNull OnCompleteCallback onActivitySelectionComplete) {
		super(mapActivity, metadata);
		this.routeKey = routeKey;
		this.onActivitySelectionComplete = onActivitySelectionComplete;
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
		String keywords = metadata != null ? metadata.getKeywords() : null;
		String link = metadata != null ? metadata.getLink() : null;
		boolean keywordsAvailable = !Algorithms.isEmpty(keywords);
		boolean linkAvailable = !Algorithms.isEmpty(link);

		String title = activityType != null
				? AndroidUtils.getActivityTypeTitle(app, activityType)
				: app.getString(R.string.shared_string_none);

		Drawable icon = activityType != null
				? getContentIcon(AndroidUtils.getActivityTypeIcon(app, activityType))
				: getContentIcon(R.drawable.ic_action_bicycle_dark); // todo use ic_action_activity

		createItemRow(getString(R.string.shared_string_activity), title, icon).setOnClickListener(v -> {
			if (metadata != null) {
				RouteActivityController.showDialog(activity, metadata, routeKey, onActivitySelectionComplete);
			}
		});
		if (keywordsAvailable) {
			createItemRow(getString(R.string.shared_string_keywords), metadata.getKeywords(), getContentIcon(R.drawable.ic_action_label));
		}
		if (linkAvailable) {
			createLinkItemRow(getString(R.string.shared_string_link), metadata.link, R.drawable.ic_action_link);
		}
	}
}