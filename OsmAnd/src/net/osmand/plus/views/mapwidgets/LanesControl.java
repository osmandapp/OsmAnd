package net.osmand.plus.views.mapwidgets;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.router.RouteResultPreparation;

import java.util.Arrays;

public class LanesControl {

	private MapViewTrackingUtilities trackingUtilities;
	private OsmAndLocationProvider locationProvider;
	private MapRouteInfoMenu mapRouteInfoMenu;
	private RoutingHelper rh;
	private OsmandSettings settings;
	private ImageView lanesView;
	private TextView lanesText;
	private TextView lanesShadowText;
	private OsmandApplication app;
	private int dist;
	private LanesDrawable lanesDrawable;
	private View centerInfo;
	private int shadowRadius;

	public LanesControl(MapActivity mapActivity, OsmandMapTileView view) {
		lanesView = mapActivity.findViewById(R.id.map_lanes);
		lanesText = mapActivity.findViewById(R.id.map_lanes_dist_text);
		lanesShadowText = mapActivity.findViewById(R.id.map_lanes_dist_text_shadow);
		centerInfo = mapActivity.findViewById(R.id.map_center_info);
		lanesDrawable = new LanesDrawable(mapActivity, mapActivity.getMapView().getScaleCoefficient());
		lanesView.setImageDrawable(lanesDrawable);
		trackingUtilities = mapActivity.getMapViewTrackingUtilities();
		locationProvider = mapActivity.getMyApplication().getLocationProvider();
		settings = mapActivity.getMyApplication().getSettings();
		mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
		rh = mapActivity.getMyApplication().getRoutingHelper();
		app = mapActivity.getMyApplication();
	}

	public void updateTextSize(boolean isNight, int textColor, int textShadowColor, boolean textBold, int shadowRadius) {
		this.shadowRadius = shadowRadius;
		TextInfoWidget.updateTextColor(lanesText, lanesShadowText, textColor, textShadowColor, textBold, shadowRadius);
	}

	public boolean updateInfo(DrawSettings drawSettings) {
		boolean visible = false;
		int locimminent = -1;
		int[] loclanes = null;
		int dist = 0;
		// TurnType primary = null;
		if ((rh == null || !rh.isFollowingMode() || rh.isDeviatedFromRoute() || (rh.getCurrentGPXRoute() != null && !rh.isCurrentGPXRouteV2()))
				&& trackingUtilities.isMapLinkedToLocation() && settings.SHOW_LANES.get()) {
			RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
			Location lp = locationProvider.getLastKnownLocation();
			if (ro != null) {
				float degree = lp == null || !lp.hasBearing() ? 0 : lp.getBearing();
				loclanes = RouteResultPreparation.parseTurnLanes(ro, degree / 180 * Math.PI);
				if (loclanes == null) {
					loclanes = RouteResultPreparation.parseLanes(ro, degree / 180 * Math.PI);
				}
			}
		} else if (rh != null && rh.isRouteCalculated()) {
			if (rh.isFollowingMode() && settings.SHOW_LANES.get()) {
				RouteCalculationResult.NextDirectionInfo r = rh.getNextRouteDirectionInfo(new RouteCalculationResult.NextDirectionInfo(), false);
				if (r != null && r.directionInfo != null && r.directionInfo.getTurnType() != null) {
					loclanes = r.directionInfo.getTurnType().getLanes();
					// primary = r.directionInfo.getTurnType();
					locimminent = r.imminent;
					// Do not show too far
					if ((r.distanceTo > 800 && r.directionInfo.getTurnType().isSkipToSpeak()) || r.distanceTo > 1200) {
						loclanes = null;
					}
					dist = r.distanceTo;
				}
			} else {
				int di = MapRouteInfoMenu.getDirectionInfo();
				if (di >= 0 && mapRouteInfoMenu.isVisible()
						&& di < rh.getRouteDirections().size()) {
					RouteDirectionInfo next = rh.getRouteDirections().get(di);
					if (next != null) {
						loclanes = next.getTurnType().getLanes();
						// primary = next.getTurnType();
					}
				} else {
					loclanes = null;
				}
			}
		}
		visible = loclanes != null && loclanes.length > 0 && !MapRouteInfoMenu.chooseRoutesVisible
				&& !MapRouteInfoMenu.waypointsVisible && !MapRouteInfoMenu.followTrackVisible;
		if (visible) {
			if (!Arrays.equals(lanesDrawable.lanes, loclanes) ||
					(locimminent == 0) != lanesDrawable.imminent) {
				lanesDrawable.imminent = locimminent == 0;
				lanesDrawable.lanes = loclanes;
				lanesDrawable.updateBounds();
				lanesView.setImageDrawable(null);
				lanesView.setImageDrawable(lanesDrawable);
				lanesView.requestLayout();
				lanesView.invalidate();
			}
			if (RouteInfoWidgetsFactory.distChanged(dist, this.dist)) {
				this.dist = dist;
				if (dist == 0) {
					lanesShadowText.setText("");
					lanesText.setText("");
				} else {
					lanesShadowText.setText(OsmAndFormatter.getFormattedDistance(dist, app));
					lanesText.setText(OsmAndFormatter.getFormattedDistance(dist, app));
				}
				lanesShadowText.invalidate();
				lanesText.invalidate();
			}
		}
		AndroidUiHelper.updateVisibility(lanesShadowText, visible && shadowRadius > 0);
		AndroidUiHelper.updateVisibility(lanesText, visible);
		AndroidUiHelper.updateVisibility(lanesView, visible);
		AndroidUiHelper.updateVisibility(centerInfo, visible);
		return true;
	}
}