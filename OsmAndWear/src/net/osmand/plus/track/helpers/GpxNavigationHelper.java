package net.osmand.plus.track.helpers;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;

import java.lang.ref.WeakReference;

public class GpxNavigationHelper {

	public static void startNavigationForSegment(@NonNull GpxFile gpxFile,
	                                             int selectedSegment,
	                                             @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		app.getSettings().GPX_SEGMENT_INDEX.set(selectedSegment);
		app.getSettings().GPX_ROUTE_INDEX.resetToDefault();
		startNavigationForGpx(gpxFile, mapActivity);
		GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (paramsBuilder != null) {
			paramsBuilder.setSelectedSegment(selectedSegment);
			app.getRoutingHelper().onSettingsChanged(true);
		}
	}

	public static void startNavigationForRoute(@NonNull GpxFile gpxFile,
	                                           int selectedRoute,
	                                           @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		app.getSettings().GPX_ROUTE_INDEX.set(selectedRoute);
		app.getSettings().GPX_SEGMENT_INDEX.resetToDefault();
		startNavigationForGpx(gpxFile, mapActivity);
		GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (paramsBuilder != null) {
			paramsBuilder.setSelectedRoute(selectedRoute);
			app.getRoutingHelper().onSettingsChanged(true);
		}
	}

	public static void startNavigationForGpx(@NonNull GpxFile gpxFile, @NonNull MapActivity mapActivity) {
		MapActivityActions mapActions = mapActivity.getMapActions();
		if (mapActivity.getMyApplication().getRoutingHelper().isFollowingMode()) {
			WeakReference<MapActivity> activityRef = new WeakReference<>(mapActivity);
			mapActions.stopNavigationActionConfirm(null, () -> {
				MapActivity activity = activityRef.get();
				if (activity != null) {
					enterRoutePlanningMode(gpxFile, activity.getMapActions());
				}
			});
		} else {
			mapActions.stopNavigationWithoutConfirm();
			enterRoutePlanningMode(gpxFile, mapActions);
		}
	}

	private static void enterRoutePlanningMode(@NonNull GpxFile gpxFile, @NonNull MapActivityActions mapActions) {
		mapActions.enterRoutePlanningModeGivenGpx(gpxFile, null, null,
				null, true, true, MenuState.HEADER_ONLY);
	}
}