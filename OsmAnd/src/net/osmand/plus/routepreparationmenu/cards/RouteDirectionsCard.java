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
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteCalculationResult.IntermediatePointInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.TurnPathHelper.RouteDrawable;
import net.osmand.plus.views.mapwidgets.LanesDrawable;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
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
		RouteCalculationResult route = routingHelper.getRoute();
		List<RouteDirectionInfo> routeDirections = new ArrayList<>(route.getRouteDirections(app));
		List<IntermediatePointInfo> intermediatePointInfos = route.getIntermediatePointInfos();
		List<TargetPoint> intermediatePoints = app.getTargetPointsHelper().getIntermediatePointsNavigation();
		List<RouteDirectionItem> items = buildRouteDirectionItems(routeDirections,
				intermediatePointInfos, intermediatePoints);
		for (int i = 0; i < items.size(); i++) {
			View view = getRouteDirectionView(items.get(i), i, items.size());
			cardsContainer.addView(view);
		}
	}

	@NonNull
	static List<RouteDirectionItem> buildRouteDirectionItems(
			@NonNull List<RouteDirectionInfo> routeDirections,
			@NonNull List<IntermediatePointInfo> intermediatePointInfos,
			@NonNull List<TargetPoint> intermediatePoints) {
		List<RouteDirectionItem> items = new ArrayList<>();
		int intermediateIndex = 0;
		int cumulativeDistance = 0;
		int cumulativeTime = 0;
		for (int directionIndex = 0; directionIndex < routeDirections.size(); directionIndex++) {
			RouteDirectionInfo direction = routeDirections.get(directionIndex);
			while (intermediateIndex < intermediatePointInfos.size()
					&& intermediatePointInfos.get(intermediateIndex).getRoutePointOffset() <= direction.routePointOffset) {
				IntermediatePointInfo info = intermediatePointInfos.get(intermediateIndex);
				TargetPoint point = intermediateIndex < intermediatePoints.size()
						? intermediatePoints.get(intermediateIndex) : null;
				items.add(RouteDirectionItem.intermediate(point, intermediateIndex, directionIndex,
						info.getDistance(), info.getTime()));
				intermediateIndex++;
			}
			boolean destination = directionIndex == routeDirections.size() - 1 && direction.distance == 0;
			items.add(RouteDirectionItem.direction(direction, directionIndex,
					cumulativeDistance, cumulativeTime, destination));
			cumulativeDistance += direction.distance;
			cumulativeTime += direction.getExpectedTime();
		}
		while (intermediateIndex < intermediatePointInfos.size()) {
			IntermediatePointInfo info = intermediatePointInfos.get(intermediateIndex);
			TargetPoint point = intermediateIndex < intermediatePoints.size()
					? intermediatePoints.get(intermediateIndex) : null;
			items.add(RouteDirectionItem.intermediate(point, intermediateIndex,
					Math.max(0, routeDirections.size() - 1), info.getDistance(), info.getTime()));
			intermediateIndex++;
		}
		return items;
	}

	private static String getTimeDescription(OsmandApplication app, RouteDirectionInfo model) {
		int timeInSeconds = model.getExpectedTime();
		return Algorithms.formatDuration(timeInSeconds, app.accessibilityEnabled());
	}

	private View getRouteDirectionView(@NonNull RouteDirectionItem item, int itemIndex, int itemCount) {
		MapActivity mapActivity = getMapActivity();
		View row = themedInflater.inflate(R.layout.route_info_list_item, null);

		TextView label = row.findViewById(R.id.description);
		TextView distanceLabel = row.findViewById(R.id.distance);
		TextView timeLabel = row.findViewById(R.id.time);
		TextView cumulativeDistanceLabel = row.findViewById(R.id.cumulative_distance);
		TextView cumulativeTimeLabel = row.findViewById(R.id.cumulative_time);
		ImageView directionIcon = row.findViewById(R.id.direction);
		ImageView lanesIcon = row.findViewById(R.id.lanes);
		row.findViewById(R.id.divider).setVisibility(itemIndex == itemCount - 1 ? View.INVISIBLE : View.VISIBLE);

		if (item.isIntermediate()) {
			directionIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.list_intermediate));
			String pointType = mapActivity.getString(R.string.intermediate_point,
					String.valueOf(item.getIntermediateIndex() + 1));
			TargetPoint point = item.getTargetPoint();
			if (point != null) {
				String pointName = point.getRoutePointDescription(mapActivity, true);
				label.setText(mapActivity.getString(R.string.ltr_or_rtl_combine_via_colon, pointType, pointName));
			} else {
				label.setText(pointType);
			}
			distanceLabel.setText("");
			timeLabel.setText("");
		} else {
			RouteDirectionInfo model = item.getDirection();
			if (item.isDestination()) {
				directionIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.list_destination));
			} else {
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
			}

			label.setText(model.getDescriptionRoutePart(app, true));
			if (model.distance > 0) {
				distanceLabel.setText(OsmAndFormatter.getFormattedDistance(model.distance, app));
				timeLabel.setText(getTimeDescription(app, model));
			} else {
				if (Algorithms.isEmpty(model.getDescriptionRoutePart(app))) {
					label.setText(mapActivity.getString(R.string.arrived_at_destination));
				}
				distanceLabel.setText("");
				timeLabel.setText("");
			}
		}
		cumulativeDistanceLabel.setText(OsmAndFormatter.getFormattedDistance(item.getCumulativeDistance(), app));
		cumulativeTimeLabel.setText(Algorithms.formatDuration(item.getCumulativeTime(), app.accessibilityEnabled()));
		row.setContentDescription(label.getText() + " " + cumulativeTimeLabel.getText());
		TargetPoint targetPoint = item.getTargetPoint();
		if (targetPoint != null) {
			row.setOnClickListener(v -> WaypointDialogHelper.showOnMap(app, mapActivity, targetPoint, false));
		} else {
			row.setOnClickListener(v -> notifyButtonPressed(item.getDirectionIndex()));
		}
		return row;
	}

	static class RouteDirectionItem {

		@Nullable
		private final RouteDirectionInfo direction;
		@Nullable
		private final TargetPoint targetPoint;
		private final int intermediateIndex;
		private final int directionIndex;
		private final int cumulativeDistance;
		private final int cumulativeTime;
		private final boolean destination;

		private RouteDirectionItem(@Nullable RouteDirectionInfo direction, @Nullable TargetPoint targetPoint,
		                           int intermediateIndex, int directionIndex, int cumulativeDistance,
		                           int cumulativeTime, boolean destination) {
			this.direction = direction;
			this.targetPoint = targetPoint;
			this.intermediateIndex = intermediateIndex;
			this.directionIndex = directionIndex;
			this.cumulativeDistance = cumulativeDistance;
			this.cumulativeTime = cumulativeTime;
			this.destination = destination;
		}

		@NonNull
		static RouteDirectionItem direction(@NonNull RouteDirectionInfo direction, int directionIndex,
		                                    int cumulativeDistance, int cumulativeTime, boolean destination) {
			return new RouteDirectionItem(direction, null, -1, directionIndex,
					cumulativeDistance, cumulativeTime, destination);
		}

		@NonNull
		static RouteDirectionItem intermediate(@Nullable TargetPoint targetPoint, int intermediateIndex,
		                                       int directionIndex, int cumulativeDistance, int cumulativeTime) {
			return new RouteDirectionItem(null, targetPoint, intermediateIndex, directionIndex,
					cumulativeDistance, cumulativeTime, false);
		}

		boolean isIntermediate() {
			return direction == null;
		}

		@NonNull
		RouteDirectionInfo getDirection() {
			return direction;
		}

		@Nullable
		TargetPoint getTargetPoint() {
			return targetPoint;
		}

		int getIntermediateIndex() {
			return intermediateIndex;
		}

		int getDirectionIndex() {
			return directionIndex;
		}

		int getCumulativeDistance() {
			return cumulativeDistance;
		}

		int getCumulativeTime() {
			return cumulativeTime;
		}

		boolean isDestination() {
			return destination;
		}
	}
}
