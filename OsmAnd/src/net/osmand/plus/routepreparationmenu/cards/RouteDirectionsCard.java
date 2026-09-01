package net.osmand.plus.routepreparationmenu.cards;

import static net.osmand.plus.settings.enums.TrackApproximationType.MANUAL;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.LocationPointWrapper;
import net.osmand.plus.helpers.TargetPoint;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteCalculationResult.IntermediatePointInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.TurnPathHelper.RouteDrawable;
import net.osmand.plus.views.mapwidgets.LanesDrawable;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.routing.details.RouteCumulativeInfo;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RouteDirectionsCard extends MapBaseCard {

	private static final Set<RouteDetailsItem.Type> ALL_ALONG_ROUTE_TYPES =
			Collections.unmodifiableSet(EnumSet.of(
					RouteDetailsItem.Type.TRAFFIC_WARNING,
					RouteDetailsItem.Type.POI,
					RouteDetailsItem.Type.FAVORITE));

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
		List<RouteDetailsItem> coreItems = buildRouteDirectionItems(routeDirections,
				intermediatePointInfos, intermediatePoints);
		List<RouteDetailsItem> alongRouteItems = buildAlongRouteItems(route);
		List<RouteDetailsItem> items = RouteDetailsListBuilder.mergeAlongRouteItems(coreItems,
				alongRouteItems, ALL_ALONG_ROUTE_TYPES, route.getCurrentRoute());
		for (int i = 0; i < items.size(); i++) {
			View view = getRouteDirectionView(items.get(i), i, items.size());
			cardsContainer.addView(view);
		}
	}

	@NonNull
	static List<RouteDetailsItem> buildRouteDirectionItems(
			@NonNull List<RouteDirectionInfo> routeDirections,
			@NonNull List<IntermediatePointInfo> intermediatePointInfos,
			@NonNull List<TargetPoint> intermediatePoints) {
		return RouteDetailsListBuilder.buildCoreItems(routeDirections, intermediatePointInfos,
				intermediatePoints);
	}

	@NonNull
	private List<RouteDetailsItem> buildAlongRouteItems(@NonNull RouteCalculationResult route) {
		WaypointHelper waypointHelper = app.getWaypointHelper();
		List<LocationPointWrapper> points = new ArrayList<>();
		points.addAll(waypointHelper.getWaypoints(WaypointHelper.ALARMS));
		points.addAll(waypointHelper.getWaypoints(WaypointHelper.POI));
		points.addAll(waypointHelper.getWaypoints(WaypointHelper.FAVORITES));

		int routePointCount = route.getImmutableAllLocations().size();
		if (points.isEmpty() || routePointCount == 0) {
			return Collections.emptyList();
		}
		int currentRoutePoint = route.getCurrentRoute();
		int lastRoutePoint = routePointCount - 1;
		points.removeIf(point -> point.getRouteIndex() < 0
				|| Math.min(point.getRouteIndex(), lastRoutePoint) <= currentRoutePoint);
		points.sort(Comparator.comparingInt(LocationPointWrapper::getRouteIndex));
		if (points.isEmpty()) {
			return Collections.emptyList();
		}

		int[] routePointOffsets = new int[points.size()];
		for (int index = 0; index < points.size(); index++) {
			routePointOffsets[index] = Math.min(points.get(index).getRouteIndex(), lastRoutePoint);
		}
		List<RouteCumulativeInfo> cumulativeInfo =
				route.getCumulativeInfoAtRoutePoints(routePointOffsets);
		List<RouteDetailsItem> items = new ArrayList<>(points.size());
		for (int index = 0; index < points.size(); index++) {
			items.add(RouteDetailsItem.alongRoute(points.get(index), cumulativeInfo.get(index)));
		}
		return items;
	}

	private static String getTimeDescription(OsmandApplication app, RouteDirectionInfo model) {
		int timeInSeconds = model.getExpectedTime();
		return Algorithms.formatDuration(timeInSeconds, app.accessibilityEnabled());
	}

	private View getRouteDirectionView(@NonNull RouteDetailsItem item, int itemIndex, int itemCount) {
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

		if (item.isAlongRoute()) {
			LocationPointWrapper locationPoint = Objects.requireNonNull(item.getLocationPoint());
			Drawable icon = locationPoint.getDrawable(mapActivity, app, nightMode);
			if (icon == null) {
				icon = getAlongRouteFallbackIcon(item.getType());
			}
			directionIcon.setImageDrawable(icon);

			PointDescription pointDescription = locationPoint.getPoint().getPointDescription(app);
			String description = Algorithms.isEmpty(pointDescription.getName())
					? pointDescription.getTypeName() : pointDescription.getName();
			label.setText(description);
			timeLabel.setText("");
			if (locationPoint.getDeviationDistance() > 0) {
				distanceLabel.setText("+" + OsmAndFormatter.getFormattedDistance(
						locationPoint.getDeviationDistance(), app));
				int directionIconId = locationPoint.isDeviationDirectionRight()
						? R.drawable.ic_small_turn_right : R.drawable.ic_small_turn_left;
				distanceLabel.setCompoundDrawablesWithIntrinsicBounds(
						app.getUIUtilities().getPaintedIcon(directionIconId, getActiveColor()),
						null, null, null);
			} else {
				distanceLabel.setText("");
			}
		} else if (item.isIntermediate()) {
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
		LocationPointWrapper locationPoint = item.getLocationPoint();
		TargetPoint targetPoint = item.getTargetPoint();
		if (locationPoint != null) {
			row.setOnClickListener(v -> WaypointDialogHelper.showOnMap(app, mapActivity,
					locationPoint.getPoint(), false));
		} else if (targetPoint != null) {
			row.setOnClickListener(v -> WaypointDialogHelper.showOnMap(app, mapActivity, targetPoint, false));
		} else {
			row.setOnClickListener(v -> notifyButtonPressed(item.getDirectionIndex()));
		}
		return row;
	}

	@NonNull
	private Drawable getAlongRouteFallbackIcon(@NonNull RouteDetailsItem.Type type) {
		int iconId;
		switch (type) {
			case TRAFFIC_WARNING:
				iconId = R.drawable.ic_action_warning_colored;
				break;
			case POI:
				iconId = R.drawable.ic_action_search_dark;
				break;
			case FAVORITE:
				iconId = R.drawable.ic_action_favorite;
				break;
			default:
				throw new IllegalArgumentException("Unsupported along-route type: " + type);
		}
		return app.getUIUtilities().getPaintedIcon(iconId, getActiveColor());
	}

}
