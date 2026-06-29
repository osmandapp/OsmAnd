/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import android.app.Activity;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Prompts for elevation tile downloads on first plugin enable and when routes cross uncovered areas.
 * Cached tiles are never removed when the plugin is disabled.
 */
public class ElevationDownloadCoordinator {

	private static final Log LOG = PlatformUtil.getLog(ElevationDownloadCoordinator.class);

	private final OsmandApplication app;
	private final DriverBreakSettings settings;
	private final SRTMElevationProvider elevationProvider;
	private final ElevationDownloadManager downloadManager;
	private final ExecutorService backgroundExecutor;

	private final Set<String> sessionPromptedRegionIds = new HashSet<>();
	private final Map<String, ElevationCoverageHelper.RegionMissingTiles> pendingDownloads = new HashMap<>();
	private volatile boolean firstEnableCheckScheduled;

	@Nullable
	private MapActivity mapActivity;

	public ElevationDownloadCoordinator(@NonNull OsmandApplication app, @NonNull DriverBreakSettings settings,
			@NonNull SRTMElevationProvider elevationProvider, @NonNull ElevationDownloadManager downloadManager,
			@NonNull ExecutorService backgroundExecutor) {
		this.app = app;
		this.settings = settings;
		this.elevationProvider = elevationProvider;
		this.downloadManager = downloadManager;
		this.backgroundExecutor = backgroundExecutor;
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	/**
	 * Called when the user enables the plugin from the UI.
	 */
	public void onPluginEnabledFromUi(@NonNull Activity activity) {
		if (settings.isElevationFirstEnablePromptDoneSync()) {
			return;
		}
		synchronized (this) {
			if (firstEnableCheckScheduled) {
				return;
			}
			firstEnableCheckScheduled = true;
		}
		if (!(activity instanceof FragmentActivity)) {
			settings.setElevationFirstEnablePromptDoneSync(true);
			return;
		}
		promptDownloadForLocation((FragmentActivity) activity, true, false);
	}

	/**
	 * User-initiated download from plugin settings (county or country at map position).
	 */
	public void promptDownloadForCurrentRegion(@NonNull FragmentActivity activity) {
		promptDownloadForLocation(activity, false, true);
	}

	private void promptDownloadForLocation(@NonNull FragmentActivity activity, boolean firstEnablePrompt,
			boolean ignoreDeclinedRegions) {
		LatLon location = resolveCurrentLocation();
		if (location == null) {
			app.showToastMessage(R.string.driver_break_elevation_region_unknown);
			if (firstEnablePrompt) {
				settings.setElevationFirstEnablePromptDoneSync(true);
			}
			return;
		}
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, ElevationCoverageHelper.RegionMissingTiles>() {
			@Override
			protected ElevationCoverageHelper.RegionMissingTiles doInBackground(Void... voids) {
				ElevationAdministrativeRegion region = ElevationRegionResolver.resolveAt(app, location);
				if (region == null) {
					return null;
				}
				if (!ignoreDeclinedRegions && settings.isElevationRegionDeclinedSync(region.getRegionId())) {
					return null;
				}
				return ElevationCoverageHelper.missingTilesForRegion(elevationProvider, region);
			}

			@Override
			protected void onPostExecute(ElevationCoverageHelper.RegionMissingTiles result) {
				if (result == null) {
					if (firstEnablePrompt) {
						settings.setElevationFirstEnablePromptDoneSync(true);
						firstEnableCheckScheduled = false;
					} else {
						app.showToastMessage(R.string.driver_break_elevation_region_unknown);
					}
					return;
				}
				if (result.getMissingTileCount() == 0) {
					app.showToastMessage(formatCompleteMessage(result));
					if (firstEnablePrompt) {
						settings.setElevationFirstEnablePromptDoneSync(true);
					}
					return;
				}
				if (showPrompt(activity, result, firstEnablePrompt)) {
					if (firstEnablePrompt) {
						settings.setElevationFirstEnablePromptDoneSync(true);
					}
				} else if (firstEnablePrompt) {
					firstEnableCheckScheduled = false;
				}
			}
		}, backgroundExecutor);
	}

	/**
	 * Updates the elevation settings summary with county/country name and missing tile count.
	 */
	public void refreshRegionSummary(@NonNull FragmentActivity activity,
			@NonNull RegionSummaryCallback callback) {
		LatLon location = resolveCurrentLocation();
		if (location == null) {
			activity.runOnUiThread(() -> callback.onRegionSummary(
					app.getString(R.string.driver_break_elevation_region_unknown), 0));
			return;
		}
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, ElevationCoverageHelper.RegionMissingTiles>() {
			@Override
			protected ElevationCoverageHelper.RegionMissingTiles doInBackground(Void... voids) {
				ElevationAdministrativeRegion region = ElevationRegionResolver.resolveAt(app, location);
				if (region == null) {
					return null;
				}
				return ElevationCoverageHelper.missingTilesForRegion(elevationProvider, region);
			}

			@Override
			protected void onPostExecute(ElevationCoverageHelper.RegionMissingTiles result) {
				if (result == null) {
					callback.onRegionSummary(app.getString(R.string.driver_break_elevation_region_unknown), 0);
					return;
				}
				String placeName = result.region.getDisplayName();
				if (!Algorithms.isEmpty(result.region.getCountryDisplayName())) {
					placeName = app.getString(R.string.driver_break_elevation_region_with_country, placeName,
							result.region.getCountryDisplayName());
				}
				int missing = result.getMissingTileCount();
				String summary = missing == 0
						? app.getString(R.string.driver_break_elevation_region_complete, placeName)
						: app.getString(R.string.driver_break_elevation_region_summary_value, placeName, missing);
				callback.onRegionSummary(summary, missing);
			}
		}, backgroundExecutor);
	}

	public interface RegionSummaryCallback {
		void onRegionSummary(@NonNull String summary, int missingTileCount);
	}

	/**
	 * Called after a route is calculated while the plugin is active.
	 */
	public void onRouteCalculated(@NonNull RouteCalculationResult route) {
		MapActivity activity = mapActivity;
		if (activity == null || route.isEmpty()) {
			return;
		}
		List<RouteSegmentResult> segments = route.getOriginalRoute();
		List<LatLon> points = ElevationCoverageHelper.routeSamplePoints(segments);
		if (points.isEmpty()) {
			return;
		}
		OsmAndTaskManager.executeTask(new AsyncTask<Void, Void, ElevationCoverageHelper.RegionMissingTiles>() {
			@Override
			protected ElevationCoverageHelper.RegionMissingTiles doInBackground(Void... voids) {
				Set<String> declined = settings.getElevationDeclinedRegionIdsSync();
				List<ElevationCoverageHelper.RegionMissingTiles> missing =
						ElevationCoverageHelper.missingTilesForRoute(app, elevationProvider, points, declined);
				for (ElevationCoverageHelper.RegionMissingTiles entry : missing) {
					if (!sessionPromptedRegionIds.contains(entry.region.getRegionId())) {
						return entry;
					}
				}
				return null;
			}

			@Override
			protected void onPostExecute(ElevationCoverageHelper.RegionMissingTiles result) {
				if (result == null) {
					return;
				}
				FragmentActivity host = mapActivity;
				if (host == null) {
					return;
				}
				showPrompt(host, result, false);
			}
		}, backgroundExecutor);
	}

	public void onDownloadAccepted(@NonNull String regionId) {
		LOG.info("DriverBreak: elevation download accepted for " + regionId);
		sessionPromptedRegionIds.add(regionId);
		ElevationCoverageHelper.RegionMissingTiles pending = pendingDownloads.remove(regionId);
		if (pending != null) {
			downloadManager.downloadTilesAsync(pending.missingTiles, null);
		}
	}

	public void onDownloadDeclined(@NonNull String regionId) {
		LOG.info("DriverBreak: elevation download declined for " + regionId);
		sessionPromptedRegionIds.add(regionId);
		pendingDownloads.remove(regionId);
		settings.getDbExecutor().execute(() -> settings.addElevationDeclinedRegionSync(regionId));
	}

	private boolean showPrompt(@NonNull FragmentActivity activity,
			@NonNull ElevationCoverageHelper.RegionMissingTiles missing, boolean firstEnablePrompt) {
		String regionId = missing.region.getRegionId();
		pendingDownloads.put(regionId, missing);
		sessionPromptedRegionIds.add(regionId);
		FragmentManager fm = activity.getSupportFragmentManager();
		if (!AndroidUtils.isFragmentCanBeAdded(fm, ElevationDownloadPromptBottomSheet.TAG)) {
			return false;
		}
		String message = formatPromptMessage(missing);
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		ElevationDownloadPromptBottomSheet.showInstance(fm, appMode, activity instanceof MapActivity, regionId,
				message, missing.getMissingTileCount(), firstEnablePrompt);
		return true;
	}

	@NonNull
	private String formatCompleteMessage(@NonNull ElevationCoverageHelper.RegionMissingTiles missing) {
		ElevationAdministrativeRegion region = missing.region;
		String placeName = region.getDisplayName();
		if (!Algorithms.isEmpty(region.getCountryDisplayName())) {
			placeName = app.getString(R.string.driver_break_elevation_region_with_country, placeName,
					region.getCountryDisplayName());
		}
		return app.getString(R.string.driver_break_elevation_region_complete, placeName);
	}

	@NonNull
	private String formatPromptMessage(@NonNull ElevationCoverageHelper.RegionMissingTiles missing) {
		ElevationAdministrativeRegion region = missing.region;
		String placeName = region.getDisplayName();
		if (!Algorithms.isEmpty(region.getCountryDisplayName())) {
			placeName = app.getString(R.string.driver_break_elevation_region_with_country, placeName,
					region.getCountryDisplayName());
		}
		return app.getString(R.string.driver_break_elevation_download_message, placeName,
				missing.getMissingTileCount());
	}

	@Nullable
	private LatLon resolveCurrentLocation() {
		Location location = app.getLocationProvider().getLastKnownLocation();
		if (location != null) {
			return new LatLon(location.getLatitude(), location.getLongitude());
		}
		if (app.getOsmandMap() != null && app.getOsmandMap().getMapView() != null) {
			return app.getOsmandMap().getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		}
		MapActivity activity = mapActivity;
		if (activity != null && activity.getMapView() != null) {
			return activity.getMapView().getCurrentRotatedTileBox().getCenterLatLon();
		}
		return null;
	}
}
