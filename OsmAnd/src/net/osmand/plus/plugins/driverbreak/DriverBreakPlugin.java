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
import android.content.Context;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.routing.RouteCalculationProgressListener;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.router.RouteSegmentResult;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ScreenLayoutMode;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;

import net.osmand.plus.views.OsmandMapTileView;

import org.apache.commons.logging.Log;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Driver Break plugin entry point.
 *
 * TODO: EV/battery electric ECU backend — design only; no implementation in this release.
 * TODO: Weekly/bi-weekly truck rest accumulation — daily limits only.
 * TODO: Deep routing integration via routing.xml height_obstacle when energy routing should
 *       affect BinaryRoutePlanner directly (currently variants are ranked after calculation).
 */
public class DriverBreakPlugin extends OsmandPlugin implements OsmAndLocationListener {

	private static final Log LOG = PlatformUtil.getLog(DriverBreakPlugin.class);

	public static final String PLUGIN_ID = "osmand.driver.break";

	private DriverBreakSettings settings;
	private BreakTimer breakTimer;
	private EnergyModel energyModel;
	private SRTMElevationProvider elevationProvider;
	private ElevationDownloadManager elevationDownloadManager;
	private ElevationDownloadCoordinator elevationDownloadCoordinator;
	private PoiDiscovery poiDiscovery;
	private RestStopFinder restStopFinder;
	private RouteValidator routeValidator;
	private AdaptiveFuelLearner adaptiveFuelLearner;
	private OBDBackend obdBackend;
	private J1939Backend j1939Backend;

	@Nullable
	private RestStopMapLayer restStopLayer;

	private volatile boolean energyRoutingEnabled;
	@Nullable
	private List<RouteSegmentResult> pendingEnergyVariant;
	@Nullable
	private RouteCalculationResult pendingEnergyRoute;

	private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "driver-break-elevation");
		t.setDaemon(true);
		return t;
	});

	private final ExecutorService energyAnalysisExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "driver-break-energy");
		t.setDaemon(true);
		return t;
	});

	private final ExecutorService restStopExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "driver-break-rest-stops");
		t.setDaemon(true);
		return t;
	});

	@Nullable
	private MapActivity mapActivity;

	public DriverBreakPlugin(@NonNull OsmandApplication app) {
		super(app);
	}

	private void ensureInitialized() {
		if (settings != null) {
			return;
		}
		settings = new DriverBreakSettings(app);
		breakTimer = new BreakTimer(settings);
		energyModel = new EnergyModel();
		elevationProvider = new SRTMElevationProvider(app);
		elevationDownloadManager = new ElevationDownloadManager(app);
		elevationDownloadCoordinator = new ElevationDownloadCoordinator(app, settings, elevationProvider,
				elevationDownloadManager, downloadExecutor);
		poiDiscovery = new PoiDiscovery(app, settings);
		restStopFinder = new RestStopFinder(app, settings, poiDiscovery, restStopExecutor);
		routeValidator = new RouteValidator();
		adaptiveFuelLearner = new AdaptiveFuelLearner(settings);
		obdBackend = new OBDBackend(app);
		j1939Backend = new J1939Backend();
		settings.getDbExecutor().execute(() -> {
			settings.restoreBreakTimerSync(breakTimer);
			energyRoutingEnabled = settings.isUseEnergyRoutingSync();
		});
	}

	@NonNull
	@Override
	public String getId() {
		return PLUGIN_ID;
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.driver_break_plugin_name);
	}

	@Override
	public CharSequence getDescription(boolean linksEnabled) {
		return app.getString(R.string.driver_break_plugin_description);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_driver_break;
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.DRIVER_BREAK_SETTINGS;
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, @Nullable Activity activity) {
		if (!super.init(app, activity)) {
			return false;
		}
		ensureInitialized();
		return true;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		// Elevation tiles under app data are kept when the plugin is disabled.
		super.disable(app);
	}

	@Override
	public void mapActivityCreate(@NonNull MapActivity activity) {
		ensureInitialized();
		mapActivity = activity;
		registerLayers(activity, activity);
		if (elevationDownloadCoordinator != null) {
			elevationDownloadCoordinator.setMapActivity(activity);
			elevationDownloadCoordinator.onPluginEnabledFromUi(activity);
		}
		settings.getTravelModeAsync(mode -> {
			if (breakTimer != null) {
				breakTimer.setMode(mode);
			}
		});
		app.getRoutingHelper().addCalculationProgressListener(routeCalculationProgressListener);
	}

	@Override
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (restStopLayer == null) {
			restStopLayer = new RestStopMapLayer(context);
			app.getOsmandMap().getMapView().addLayer(restStopLayer, 4.5f);
		} else {
			OsmandMapTileView mapView = app.getOsmandMap().getMapView();
			if (!mapView.getLayers().contains(restStopLayer)) {
				mapView.addLayer(restStopLayer, 4.5f);
			}
		}
	}

	@Override
	public void mapActivityResume(@NonNull MapActivity activity) {
		ensureInitialized();
		app.getLocationProvider().addLocationListener(this);
		updateRestStopsForRoute(app.getRoutingHelper().getRoute());
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
		app.getLocationProvider().removeLocationListener(this);
	}

	@Override
	public void mapActivityDestroy(@NonNull MapActivity activity) {
		app.getRoutingHelper().removeCalculationProgressListener(routeCalculationProgressListener);
		persistTimerState();
		if (elevationDownloadCoordinator != null) {
			elevationDownloadCoordinator.setMapActivity(null);
		}
		mapActivity = null;
	}

	public void onElevationDownloadAccepted(@NonNull String regionId, boolean firstEnablePrompt) {
		if (elevationDownloadCoordinator != null) {
			elevationDownloadCoordinator.onDownloadAccepted(regionId);
		}
	}

	public void onElevationDownloadDeclined(@NonNull String regionId, boolean firstEnablePrompt) {
		if (elevationDownloadCoordinator != null) {
			elevationDownloadCoordinator.onDownloadDeclined(regionId);
		}
	}

	@Override
	public void updateLocation(@NonNull Location location) {
		if (breakTimer != null) {
			breakTimer.onLocationUpdate(location);
		}
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos,
			@NonNull ApplicationMode appMode, @Nullable ScreenLayoutMode layoutMode) {
		ensureInitialized();
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode, layoutMode);
		MapWidget widget = createMapWidgetForParams(mapActivity, WidgetType.DRIVER_BREAK_NEXT_STOP);
		if (widget != null) {
			widgetsInfos.add(creator.createWidgetInfo(widget));
		}
	}

	@Nullable
	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType,
			@Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		if (widgetType == WidgetType.DRIVER_BREAK_NEXT_STOP) {
			return new DriverBreakWidget(mapActivity, customId, widgetsPanel, this);
		}
		return null;
	}

	@Nullable
	public BreakStatus getBreakStatus() {
		return breakTimer != null ? breakTimer.getStatus() : null;
	}

	public ElevationDownloadManager getElevationDownloadManager() {
		ensureInitialized();
		return elevationDownloadManager;
	}

	@NonNull
	public ElevationDownloadCoordinator getElevationDownloadCoordinator() {
		ensureInitialized();
		return elevationDownloadCoordinator;
	}

	@NonNull
	public SRTMElevationProvider getElevationProvider() {
		ensureInitialized();
		return elevationProvider;
	}

	@NonNull
	public DriverBreakSettings getSettings() {
		ensureInitialized();
		return settings;
	}

	@NonNull
	public EnergyModel getEnergyModel() {
		ensureInitialized();
		return energyModel;
	}

	@NonNull
	public RouteValidator getRouteValidator() {
		ensureInitialized();
		return routeValidator;
	}

	@NonNull
	public RestStopFinder getRestStopFinder() {
		ensureInitialized();
		return restStopFinder;
	}

	/**
	 * Applies the pending lower-energy route variant chosen during the last energy analysis.
	 */
	public void applyPendingEnergyRoute() {
		List<RouteSegmentResult> variant = pendingEnergyVariant;
		RouteCalculationResult baseRoute = pendingEnergyRoute;
		if (variant == null || baseRoute == null) {
			return;
		}
		if (EnergyRouteApplier.applyVariant(app, variant, baseRoute)) {
			updateRestStopsForRoute(app.getRoutingHelper().getRoute());
			MapActivity activity = mapActivity;
			if (activity != null) {
				activity.refreshMap();
			}
		}
	}

	/**
	 * Called when travel mode changes in settings.
	 */
	public void onTravelModeChanged(@NonNull TravelMode mode) {
		if (breakTimer != null) {
			breakTimer.setMode(mode);
		}
		RouteCalculationResult route = app.getRoutingHelper().getRoute();
		updateRestStopsForRoute(route);
	}

	private void updateRestStopsForRoute(@Nullable RouteCalculationResult route) {
		if (restStopFinder == null || restStopLayer == null || settings == null) {
			return;
		}
		if (route == null || route.isEmpty()) {
			restStopLayer.setRestStops(Collections.emptyList());
			MapActivity activity = mapActivity;
			if (activity != null) {
				activity.refreshMap();
			}
			return;
		}
		settings.getTravelModeAsync(mode -> restStopFinder.findRestStops(route, mode, stops -> {
			if (restStopLayer != null) {
				restStopLayer.setRestStops(stops);
				MapActivity activity = mapActivity;
				if (activity != null) {
					activity.refreshMap();
				}
			}
		}));
	}

	/**
	 * Called when energy routing is toggled in settings.
	 */
	public void onEnergyRoutingChanged(boolean enabled) {
		energyRoutingEnabled = enabled;
	}

	private void persistTimerState() {
		if (settings == null || breakTimer == null) {
			return;
		}
		settings.getDbExecutor().execute(() -> settings.persistBreakTimerStateSync(
				breakTimer.getLastBreakElapsedMs(),
				breakTimer.getAccumulatedDistanceM(),
				breakTimer.isBreakInProgress()));
	}

	private final RouteCalculationProgressListener routeCalculationProgressListener =
			new RouteCalculationProgressListener() {
				@Override
				public void onCalculationStart() {
				}

				@Override
				public void onUpdateCalculationProgress(int progress) {
				}

				@Override
				public void onRequestPrivateAccessRouting() {
				}

				@Override
				public void onCalculationFinish() {
					RouteCalculationResult route = app.getRoutingHelper().getRoute();
					if (route != null && !route.isEmpty() && elevationDownloadCoordinator != null) {
						elevationDownloadCoordinator.onRouteCalculated(route);
					}
					updateRestStopsForRoute(route);
					if (energyModel == null || elevationProvider == null || settings == null) {
						return;
					}
					if (route == null || route.isEmpty()) {
						return;
					}
					scheduleEnergyAnalysis(route);
				}
			};

	private void scheduleEnergyAnalysis(@NonNull RouteCalculationResult route) {
		energyAnalysisExecutor.execute(() -> {
			LOG.info("DriverBreak: starting route energy analysis");
			energyRoutingEnabled = settings.readEnergyRoutingEnabledBlocking();
			if (!energyRoutingEnabled) {
				LOG.info("DriverBreak: energy routing disabled in settings");
				return;
			}
			EnergyComparisonResult result;
			try {
				EnergyParams params = settings.readEnergyParamsBlocking();
				result = analyzeRouteEnergy(route, params);
			} catch (RuntimeException e) {
				LOG.error("DriverBreak: route energy analysis failed", e);
				return;
			}
			app.runInUIThread(() -> handleEnergyAnalysisResult(result, route));
		});
	}

	private void handleEnergyAnalysisResult(@Nullable EnergyComparisonResult result,
			@NonNull RouteCalculationResult route) {
		if (result == null || !result.showSuggestion || result.bestVariant == null) {
			return;
		}
		pendingEnergyVariant = result.bestVariant;
		pendingEnergyRoute = route;
		boolean applied = EnergyRouteApplier.applyVariant(app, result.bestVariant, route);
		if (applied) {
			updateRestStopsForRoute(app.getRoutingHelper().getRoute());
		}
		MapActivity activity = mapActivity;
		if (activity == null) {
			return;
		}
		if (applied) {
			activity.refreshMap();
		}
		FragmentManager fm = activity.getSupportFragmentManager();
		EnergyRouteBottomSheet.showInstance(fm, null, app.getSettings().getApplicationMode(),
				true, result.energySavingFraction, result.distanceIncreaseFraction, applied);
	}

	@NonNull
	private EnergyComparisonResult analyzeRouteEnergy(@NonNull RouteCalculationResult route,
			@NonNull EnergyParams params) {
		List<List<RouteSegmentResult>> variants = EnergyRouteAlternativeExtractor.collectRouteVariants(route);
		if (variants.isEmpty()) {
			return new EnergyComparisonResult(false, 0.0, 0.0, null);
		}
		List<RouteSegmentResult> primarySegments = variants.get(0);
		List<SegmentData> primarySegmentData = EnergyRouteHelper.buildSegmentsFromRoute(primarySegments,
				elevationProvider);
		double primaryEnergyJ = energyModel.routeCost(primarySegmentData, params);
		double primaryDistanceM = EnergyRouteHelper.segmentListDistanceM(primarySegments);
		LOG.info("DriverBreak: primary route energy=" + primaryEnergyJ + " J, distance="
				+ primaryDistanceM + " m, energySegments=" + primarySegmentData.size()
				+ ", variants=" + variants.size());

		double bestAltEnergyJ = primaryEnergyJ;
		double bestAltDistanceM = primaryDistanceM;
		List<RouteSegmentResult> bestVariant = null;
		for (int i = 1; i < variants.size(); i++) {
			List<RouteSegmentResult> altSegments = variants.get(i);
			double altEnergyJ = energyModel.routeCost(
					EnergyRouteHelper.buildSegmentsFromRoute(altSegments, elevationProvider), params);
			double altDistanceM = EnergyRouteHelper.segmentListDistanceM(altSegments);
			if (EnergyRouteHelper.isCheaperAlternative(primaryEnergyJ, altEnergyJ, primaryDistanceM, altDistanceM)
					&& altEnergyJ < bestAltEnergyJ) {
				bestAltEnergyJ = altEnergyJ;
				bestAltDistanceM = altDistanceM;
				bestVariant = altSegments;
			}
		}
		if (bestVariant == null || bestAltEnergyJ >= primaryEnergyJ || primaryEnergyJ <= 0.0) {
			return new EnergyComparisonResult(false, 0.0, 0.0, null);
		}
		double saving = (primaryEnergyJ - bestAltEnergyJ) / primaryEnergyJ;
		double distanceIncrease = (bestAltDistanceM - primaryDistanceM) / primaryDistanceM;
		return new EnergyComparisonResult(true, saving, distanceIncrease, bestVariant);
	}

	private static final class EnergyComparisonResult {
		final boolean showSuggestion;
		final double energySavingFraction;
		final double distanceIncreaseFraction;
		@Nullable
		final List<RouteSegmentResult> bestVariant;

		EnergyComparisonResult(boolean showSuggestion, double energySavingFraction,
				double distanceIncreaseFraction, @Nullable List<RouteSegmentResult> bestVariant) {
			this.showSuggestion = showSuggestion;
			this.energySavingFraction = energySavingFraction;
			this.distanceIncreaseFraction = distanceIncreaseFraction;
			this.bestVariant = bestVariant;
		}
	}
}
