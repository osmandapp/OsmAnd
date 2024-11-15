package net.osmand.plus.routepreparationmenu.cards;

import static net.osmand.plus.settings.enums.TrackApproximationType.MANUAL;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.RouteDetailsFragment;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.TurnPathHelper.RouteDrawable;
import net.osmand.plus.views.mapwidgets.LanesDrawable;
import net.osmand.util.Algorithms;

import java.util.List;

public class RouteDirectionsCard extends MapBaseCard {

	private final RoutingHelper routingHelper;

	public RouteDirectionsCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		routingHelper = mapActivity.getRoutingHelper();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_directions_card;
	}

	@Override
	protected void updateContent() {
		setupAttachToRoadsBanner();
		setupRouteDirectins();
	}

	private void setupAttachToRoadsBanner() {
		FrameLayout container = view.findViewById(R.id.attach_to_roads_banner_container);
		container.removeAllViews();
		ApplicationMode appMode = routingHelper.getAppMode();
		GpxFile gpxFile = routingHelper.getCurrentGPX();
		if (gpxFile != null && !gpxFile.isAttachedToRoads() && settings.DETAILED_TRACK_GUIDANCE.getModeValue(appMode) == MANUAL) {
			AttachTrackToRoadsBannerCard card = new AttachTrackToRoadsBannerCard(mapActivity);
			card.setListener(getListener());
			container.addView(card.build(mapActivity));
			AndroidUiHelper.updateVisibility(container, true);
		} else {
			AndroidUiHelper.updateVisibility(container, false);
		}
	}

	private void setupRouteDirectins() {
		LinearLayout root = view.findViewById(R.id.items);
		root.removeAllViews();
		createRouteDirections(root);
	}

	private void createRouteDirections(LinearLayout cardsContainer) {
		List<RouteDirectionInfo> routeDirections = routingHelper.getRouteDirections();
		for (int i = 0; i < routeDirections.size(); i++) {
			RouteDirectionInfo routeDirectionInfo = routeDirections.get(i);
			View view = getRouteDirectionView(i, routeDirectionInfo, routeDirections);
			cardsContainer.addView(view);
		}
	}

	private static String getTimeDescription(OsmandApplication app, RouteDirectionInfo model) {
		int timeInSeconds = model.getExpectedTime();
		return Algorithms.formatDuration(timeInSeconds, app.accessibilityEnabled());
	}

	private View getRouteDirectionView(int directionInfoIndex, RouteDirectionInfo model, List<RouteDirectionInfo> directionsInfo) {
		MapActivity mapActivity = getMapActivity();
		View row = themedInflater.inflate(R.layout.route_info_list_item, null);

		TextView label = row.findViewById(R.id.description);
		TextView distanceLabel = row.findViewById(R.id.distance);
		TextView timeLabel = row.findViewById(R.id.time);
		TextView cumulativeDistanceLabel = row.findViewById(R.id.cumulative_distance);
		TextView cumulativeTimeLabel = row.findViewById(R.id.cumulative_time);
		ImageView directionIcon = row.findViewById(R.id.direction);
		ImageView lanesIcon = row.findViewById(R.id.lanes);
		row.findViewById(R.id.divider).setVisibility(directionInfoIndex == directionsInfo.size() - 1 ? View.INVISIBLE : View.VISIBLE);

		RouteDrawable drawable = new RouteDrawable(mapActivity, true);
		drawable.setColorFilter(new PorterDuffColorFilter(getActiveColor(), PorterDuff.Mode.SRC_ATOP));
		drawable.setRouteType(model.getTurnType());
		directionIcon.setImageDrawable(drawable);

		int[] lanes = model.getTurnType().getLanes();
		if (lanes != null) {
			LanesDrawable lanesDrawable = new LanesDrawable(mapActivity, 1);
			lanesDrawable.lanes = lanes;
			lanesDrawable.isTurnByTurn = true;
			lanesDrawable.isNightMode = nightMode;
			lanesDrawable.updateBounds();
			lanesIcon.setImageDrawable(lanesDrawable);
			lanesIcon.setVisibility(View.VISIBLE);
		}

		label.setText(model.getDescriptionRoutePart());
		if (model.distance > 0) {
			distanceLabel.setText(OsmAndFormatter.getFormattedDistance(model.distance, app));
			timeLabel.setText(getTimeDescription(app, model));
			row.setContentDescription(label.getText() + " " + timeLabel.getText());
		} else {
			if (Algorithms.isEmpty(label.getText().toString())) {
				label.setText(mapActivity.getString((directionInfoIndex != directionsInfo.size() - 1) ? R.string.arrived_at_intermediate_point : R.string.arrived_at_destination));
			}
			distanceLabel.setText("");
			timeLabel.setText("");
			row.setContentDescription("");
		}
		RouteDetailsFragment.CumulativeInfo cumulativeInfo = RouteDetailsFragment.getRouteDirectionCumulativeInfo(directionInfoIndex, directionsInfo);
		cumulativeDistanceLabel.setText(OsmAndFormatter.getFormattedDistance(cumulativeInfo.distance, app));
		cumulativeTimeLabel.setText(Algorithms.formatDuration(cumulativeInfo.time, app.accessibilityEnabled()));
		row.setOnClickListener(v -> notifyButtonPressed(directionInfoIndex));
		return row;
	}
}