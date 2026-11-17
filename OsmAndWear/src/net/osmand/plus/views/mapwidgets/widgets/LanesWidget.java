package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.LANES;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.LanesDrawable;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.router.RouteResultPreparation;
import net.osmand.router.TurnType;

import java.util.Arrays;
import java.util.List;

public class LanesWidget extends MapWidget {

	private static final int MAX_METERS_NOT_SPOKEN_TURN = 800;
	private static final int MAX_METERS_SPOKEN_TURN = 1200;
	private static final int DISTANCE_CHANGE_THRESHOLD = 10;

	private final RoutingHelper routingHelper;

	private final ImageView lanesImage;
	private final TextView lanesText;
	private final TextView lanesShadowText;

	private final LanesDrawable lanesDrawable;

	private int cachedDist;
	private int shadowRadius;
	boolean specialPosition;

	public LanesWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, LANES);

		routingHelper = mapActivity.getMyApplication().getRoutingHelper();

		lanesImage = view.findViewById(R.id.map_lanes);
		lanesText = view.findViewById(R.id.map_lanes_dist_text);
		lanesShadowText = view.findViewById(R.id.map_lanes_dist_text_shadow);

		lanesDrawable = new LanesDrawable(mapActivity, mapActivity.getMapView().getScaleCoefficient());
		lanesImage.setImageDrawable(lanesDrawable);

		updateVisibility(false);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.lanes_widget;
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		int imminent = -1;
		int[] lanes = null;
		int distance = 0;

		boolean followingMode = routingHelper.isFollowingMode();
		boolean deviatedFromRoute = routingHelper.isDeviatedFromRoute();
		boolean notOsmAndGpxRoute = routingHelper.getCurrentGPXRoute() != null && !routingHelper.isCurrentGPXRouteV2();
		boolean mapLinkedToLocation = mapActivity.getMapViewTrackingUtilities().isMapLinkedToLocation();

		if (mapLinkedToLocation && (!followingMode || deviatedFromRoute || notOsmAndGpxRoute)) {
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
			if (ro != null) {
				Location lastKnownLocation = locationProvider.getLastKnownLocation();
				float degree = lastKnownLocation == null || !lastKnownLocation.hasBearing()
						? 0
						: lastKnownLocation.getBearing();
				lanes = RouteResultPreparation.parseTurnLanes(ro, degree / 180 * Math.PI);
				if (lanes == null) {
					lanes = RouteResultPreparation.parseLanes(ro, degree / 180 * Math.PI);
				}
			}
		} else if (routingHelper.isRouteCalculated() && followingMode) {
			NextDirectionInfo directionInfo = routingHelper.getNextRouteDirectionInfo(new NextDirectionInfo(), false);
			RouteDirectionInfo routeDirectionInfo = directionInfo != null ? directionInfo.directionInfo : null;
			TurnType turnType = routeDirectionInfo != null ? routeDirectionInfo.getTurnType() : null;
			boolean tooFar = (turnType != null)
					&& (directionInfo.distanceTo > MAX_METERS_NOT_SPOKEN_TURN && turnType.isSkipToSpeak()
					|| directionInfo.distanceTo > MAX_METERS_SPOKEN_TURN);

			if (turnType != null && !tooFar) {
				lanes = directionInfo.directionInfo.getTurnType().getLanes();
				imminent = directionInfo.imminent;
				distance = directionInfo.distanceTo;
			}
		}

		boolean visible = lanes != null && lanes.length > 0
				&& !MapRouteInfoMenu.chooseRoutesVisible
				&& !MapRouteInfoMenu.waypointsVisible
				&& !MapRouteInfoMenu.followTrackVisible
				&& !mapActivity.getWidgetsVisibilityHelper().shouldHideVerticalWidgets();
		if (visible) {
			updateLanes(lanes, imminent, distance);
		}

		updateVisibility(visible);
	}

	@Override
	public boolean updateVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(lanesShadowText, visible && shadowRadius > 0);
		boolean updatedVisibility = super.updateVisibility(visible);

		if (specialPosition && updatedVisibility) {
			ViewGroup specialContainer = getSpecialContainer();
			specialContainer.removeAllViews();
			if (visible) {
				specialContainer.addView(view);
			}
		}
		return updatedVisibility;
	}

	private void updateLanes(@NonNull int[] lanes, int imminent, int distance) {
		boolean updateDrawable = !Arrays.equals(lanesDrawable.lanes, lanes) || (imminent == 0) != lanesDrawable.imminent;
		if (updateDrawable) {
			lanesDrawable.imminent = imminent == 0;
			lanesDrawable.lanes = lanes;
			lanesDrawable.updateBounds();
			lanesImage.setImageDrawable(null);
			lanesImage.setImageDrawable(lanesDrawable);
			lanesImage.requestLayout();
			lanesImage.invalidate();
		}

		if (cachedDist == 0 || Math.abs(cachedDist - distance) > DISTANCE_CHANGE_THRESHOLD) {
			cachedDist = distance;
			if (distance == 0) {
				lanesShadowText.setText("");
				lanesText.setText("");
			} else {
				String formattedDistance = OsmAndFormatter.getFormattedDistance(distance, app,
						OsmAndFormatter.OsmAndFormatterParams.USE_LOWER_BOUNDS);
				lanesText.setText(formattedDistance);
				lanesShadowText.setText(formattedDistance);
			}
			lanesShadowText.invalidate();
			lanesText.invalidate();
		}
	}

	public void updateColors(@NonNull TextState textState) {
		super.updateColors(textState);

		view.setBackgroundResource(textState.boxFree);

		shadowRadius = textState.textShadowRadius / 2;
		updateTextColor(lanesText, lanesShadowText, textState.textColor,
				textState.textShadowColor, textState.textBold, shadowRadius);
	}

	@Override
	public void attachView(@NonNull ViewGroup container, @NonNull WidgetsPanel widgetsPanel,
	                       @NonNull List<MapWidget> followingWidgets) {
		ViewGroup specialContainer = getSpecialContainer();
		specialContainer.removeAllViews();

		specialPosition = followingWidgets.isEmpty();
		if (specialPosition) {
			specialContainer.removeAllViews();
			specialContainer.addView(view);
		} else {
			container.addView(view);
		}
	}

	@Override
	public void detachView(@NonNull WidgetsPanel widgetsPanel) {
		super.detachView(widgetsPanel);
		// Clear in case link to previous view of LanesWidget is lost
		getSpecialContainer().removeView(view);
	}

	@NonNull
	private ViewGroup getSpecialContainer() {
		return mapActivity.findViewById(R.id.lanes_widget_special_position);
	}
}