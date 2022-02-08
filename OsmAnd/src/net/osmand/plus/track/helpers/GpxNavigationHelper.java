package net.osmand.plus.track.helpers;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.base.ContextMenuFragment.MenuState;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GpxNavigationHelper {

	public static void startNavigationForSegment(@NonNull GPXFile gpxFile,
	                                             int selectedSegment,
	                                             @NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		app.getSettings().GPX_ROUTE_SEGMENT.set(selectedSegment);
		startNavigationForGpx(gpxFile, mapActivity);
		GPXRouteParamsBuilder paramsBuilder = app.getRoutingHelper().getCurrentGPXRoute();
		if (paramsBuilder != null) {
			paramsBuilder.setSelectedSegment(selectedSegment);
			app.getRoutingHelper().onSettingsChanged(true);
		}
	}


	public static void startNavigationForGpx(@NonNull GPXFile gpxFile, @NonNull MapActivity mapActivity) {
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

	private static void enterRoutePlanningMode(@NonNull GPXFile gpxFile, @NonNull MapActivityActions mapActions) {
		mapActions.enterRoutePlanningModeGivenGpx(gpxFile, null, null,
				null, true, true, MenuState.HEADER_ONLY);
	}
}