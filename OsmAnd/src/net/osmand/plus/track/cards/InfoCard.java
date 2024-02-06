package net.osmand.plus.track.cards;

import static net.osmand.plus.utils.AndroidUtils.getActivityTypeStringPropertyName;
import static net.osmand.util.Algorithms.capitalizeFirstLetterAndLowercase;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXUtilities;
import net.osmand.osm.OsmRouteType;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.router.network.NetworkRouteSelector;
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
				String routeTypeName = routeKey.type.getName();
				String routeTypeToDisplay = capitalizeFirstLetterAndLowercase(routeTypeName);
				routeTypeToDisplay = getActivityTypeStringPropertyName(app, routeTypeName, routeTypeToDisplay);

				createItemRow(getString(R.string.shared_string_activity), routeTypeToDisplay, getContentIcon(getActivityTypeIcon(routeKey.type)));
			}
			if (!Algorithms.isEmpty(metadata.keywords)) {
				createItemRow(getString(R.string.shared_string_keywords), metadata.keywords, getContentIcon(R.drawable.ic_action_label));
			}
			createLinkItemRow(getString(R.string.shared_string_link), metadata.link, R.drawable.ic_action_link);
		}
	}

	@DrawableRes
	private int getActivityTypeIcon(OsmRouteType activityType) {
		int iconId = app.getResources().getIdentifier("mx_" + activityType.getIcon(), "drawable", app.getPackageName());
		return iconId != 0 ? iconId : R.drawable.mx_special_marker;
	}
}