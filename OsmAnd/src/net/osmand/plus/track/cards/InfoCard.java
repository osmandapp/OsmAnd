package net.osmand.plus.track.cards;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.track.fragments.controller.SelectRouteActivityController;
import net.osmand.plus.track.helpers.RouteActivitySelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.primitives.Metadata;
import net.osmand.shared.gpx.primitives.RouteActivity;
import net.osmand.util.Algorithms;

public class InfoCard extends BaseMetadataCard {

	private final RouteActivitySelectionHelper routeActivitySelectionHelper;

	public InfoCard(@NonNull MapActivity mapActivity, @NonNull Metadata metadata,
	                @NonNull RouteActivitySelectionHelper routeActivitySelectionHelper) {
		super(mapActivity, metadata);
		this.routeActivitySelectionHelper = routeActivitySelectionHelper;
	}

	@Override
	@StringRes
	protected int getTitleId() {
		return R.string.info_button;
	}

	@Override
	public void updateContent() {
		super.updateContent();
		RouteActivityHelper helper = app.getRouteActivityHelper();
		RouteActivity routeActivity = routeActivitySelectionHelper.getSelectedActivity();
		String keywords = metadata != null ? metadata.getFilteredKeywords(helper.getActivities()) : null;
		String link = metadata != null ? metadata.getLink() : null;

		String label = routeActivity != null
				? routeActivity.getLabel()
				: app.getString(R.string.shared_string_none);

		Drawable icon = getContentIcon(routeActivity != null
				? AndroidUtils.getIconId(app, routeActivity.getIconName())
				: R.drawable.ic_action_activity);

		createItemRow(getString(R.string.shared_string_activity), label, icon).setOnClickListener(
				v -> SelectRouteActivityController.showDialog(activity, routeActivitySelectionHelper)
		);
		if (!Algorithms.isEmpty(keywords)) {
			createItemRow(getString(R.string.shared_string_keywords), keywords, getContentIcon(R.drawable.ic_action_label));
		}
		if (!Algorithms.isEmpty(link)) {
			createLinkItemRow(getString(R.string.shared_string_link), link, R.drawable.ic_action_link);
		}
	}
}