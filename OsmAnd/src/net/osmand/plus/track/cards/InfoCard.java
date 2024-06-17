package net.osmand.plus.track.cards;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

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

		boolean visible = metadata != null && (!Algorithms.isEmpty(metadata.keywords)
				|| !Algorithms.isEmpty(metadata.link) || routeKey != null);

		updateVisibility(visible);

		if (visible) {
			if (routeKey != null) {
				OsmRouteType activityType = routeKey.type;
				String routeTypeToDisplay = AndroidUtils.getActivityTypeTitle(app, activityType);
				createItemRow(getString(R.string.shared_string_activity), routeTypeToDisplay, 
						getContentIcon(AndroidUtils.getActivityTypeIcon(app, activityType)));
			}
			if (!Algorithms.isEmpty(metadata.keywords)) {
				createItemRow(getString(R.string.shared_string_keywords), metadata.keywords, getContentIcon(R.drawable.ic_action_label));
			}
			createLinkItemRow(getString(R.string.shared_string_link), metadata.link, R.drawable.ic_action_link);
		}
	}
}