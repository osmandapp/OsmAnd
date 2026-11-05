package net.osmand.plus.helpers;

import static net.osmand.plus.settings.enums.TrackApproximationType.AUTOMATIC;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.measurementtool.GpxApproximationParams;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxFile;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;

public class RestoreNavigationHelper {

	private static final Log LOG = PlatformUtil.getLog(RestoreNavigationHelper.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final RoutingHelper routingHelper;
	private final TargetPointsHelper targetPointsHelper;

	private final MapActivity mapActivity;

	public RestoreNavigationHelper(@NonNull OsmandApplication app, @Nullable MapActivity mapActivity) {
		this.app = app;
		this.mapActivity = mapActivity;
		this.settings = app.getSettings();
		this.routingHelper = app.getRoutingHelper();
		this.targetPointsHelper = app.getTargetPointsHelper();
	}

	public void checkRestoreRoutingMode() {
		// This situation could be when navigation suddenly crashed and after restarting
		// it tries to continue the last route
		if (settings.FOLLOW_THE_ROUTE.get() && !routingHelper.isRouteCalculated() && !routingHelper.isRouteBeingCalculated()) {
			LOG.info("Try to restore route - proceed");
			restoreRoutingMode();
		} else {
			LOG.info("Try to restore route - nothing to restore");
		}
	}

	public void restoreRoutingMode() {
		String gpxPath = settings.FOLLOW_THE_GPX_ROUTE.get();
		TargetPoint pointToNavigate = targetPointsHelper.getPointToNavigate();
		if (pointToNavigate == null && gpxPath == null) {
			notRestoreRoutingMode();
		} else {
			restoreRoutingModeInner(pointToNavigate, gpxPath);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private void restoreRoutingModeInner(@Nullable TargetPoint pointToNavigate, @Nullable String gpxPath) {
		AsyncTask<String, Void, GpxFile> task = new AsyncTask<String, Void, GpxFile>() {
			@Override
			protected GpxFile doInBackground(String... params) {
				if (gpxPath != null) {
					// Reverse also should be stored ?
					GpxFile gpxFile = SharedUtil.loadGpxFile(new File(gpxPath));
					return gpxFile.getError() == null ? gpxFile : null;
				}
				return null;
			}

			@Override
			protected void onPostExecute(@Nullable GpxFile gpxFile) {
				if (pointToNavigate == null) {
					notRestoreRoutingMode();
				} else {
					enterRoutingMode(createGpxRouteParams(gpxFile));
				}
			}

			@Nullable
			private GPXRouteParamsBuilder createGpxRouteParams(@Nullable GpxFile gpxFile) {
				GPXRouteParamsBuilder builder = null;
				if (gpxFile != null) {
					builder = new GPXRouteParamsBuilder(gpxFile, settings);

					if (settings.GPX_ROUTE_CALC_OSMAND_PARTS.get()) {
						builder.setCalculateOsmAndRouteParts(true);
					}
					if (settings.GPX_ROUTE_CALC.get()) {
						builder.setCalculateOsmAndRoute(true);
					}
					int segmentIndex = settings.GPX_SEGMENT_INDEX.get();
					if (segmentIndex != -1) {
						builder.setSelectedSegment(segmentIndex);
					}
					int routeIndex = settings.GPX_ROUTE_INDEX.get();
					if (routeIndex != -1) {
						builder.setSelectedRoute(routeIndex);
					}
					ApplicationMode appMode = routingHelper.getAppMode();
					if (!gpxFile.isAttachedToRoads() && settings.DETAILED_TRACK_GUIDANCE.getModeValue(appMode) == AUTOMATIC) {
						GpxApproximationParams params = new GpxApproximationParams();
						params.setAppMode(appMode);
						params.setDistanceThreshold(settings.GPX_APPROXIMATION_DISTANCE.getModeValue(appMode));

						builder.setApproximationParams(params);
					}
				}
				return builder;
			}
		};
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, gpxPath);
	}

	public void enterRoutingMode(@Nullable GPXRouteParamsBuilder gpxRoute) {
		app.logRoutingEvent("enterRoutingMode gpxRoute " + gpxRoute);

		app.getMapViewTrackingUtilities().backToLocationImpl();
		settings.FOLLOW_THE_GPX_ROUTE.set(gpxRoute != null ? gpxRoute.getFile().getPath() : null);

		routingHelper.setGpxParams(gpxRoute);
		if (targetPointsHelper.getPointToStart() == null) {
			targetPointsHelper.setStartPoint(null, false, null);
		}
		settings.FOLLOW_THE_ROUTE.set(true);
		routingHelper.setFollowingMode(true);
		targetPointsHelper.updateRouteAndRefresh(true);
		app.initVoiceCommandPlayer(app, routingHelper.getAppMode(), null,
				true, false, false, false);
		if (mapActivity != null) {
			if (mapActivity.getDashboard().isVisible()) {
				mapActivity.getDashboard().hideDashboard();
			}
			AndroidUtils.requestNotificationPermissionIfNeeded(mapActivity);
		}
	}

	private void notRestoreRoutingMode() {
		if (mapActivity != null) {
			mapActivity.updateApplicationModeSettings();
		}
		routingHelper.clearCurrentRoute(null, new ArrayList<>());
		targetPointsHelper.removeAllWayPoints(false, false);
		app.getOsmandMap().refreshMap();
	}
}
