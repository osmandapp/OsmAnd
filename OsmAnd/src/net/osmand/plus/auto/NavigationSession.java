package net.osmand.plus.auto;

import static androidx.car.app.CarContext.ACTION_NAVIGATE;
import static net.osmand.plus.AppInitEvents.ROUTING_CONFIG_INITIALIZED;
import static net.osmand.plus.NavigationService.DEEP_LINK_ACTION_OPEN_ROOT_SCREEN;
import static net.osmand.plus.OsmAndLocationProvider.NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS;
import static net.osmand.plus.OsmAndLocationProvider.isRunningOnEmulator;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.NavigationManagerCallback;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.ValueHolder;
import net.osmand.plus.AppInitEvents;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.screens.LandingScreen;
import net.osmand.plus.auto.screens.NavigationScreen;
import net.osmand.plus.auto.screens.RequestPermissionScreen;
import net.osmand.plus.auto.screens.RequestPermissionScreen.LocationPermissionCheckCallback;
import net.osmand.plus.auto.screens.RequestPurchaseScreen;
import net.osmand.plus.auto.screens.RoutePreviewScreen;
import net.osmand.plus.auto.screens.SearchResultsScreen;
import net.osmand.plus.auto.screens.SettingsScreen;
import net.osmand.plus.helpers.LocationCallback;
import net.osmand.plus.helpers.LocationServiceHelper;
import net.osmand.plus.helpers.RestoreNavigationHelper;
import net.osmand.plus.helpers.SearchHistoryHelper;
import net.osmand.plus.helpers.SearchHistoryHelper.HistoryEntry;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.settings.enums.LocationSource;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;
import net.osmand.util.GeoParsedPoint;
import net.osmand.util.GeoPointParserUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Session class for the Navigation sample app.
 */
public class NavigationSession extends Session implements NavigationListener, OsmAndLocationListener,
		DefaultLifecycleObserver, IRouteInformationListener {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(NavigationSession.class);

	static final String TAG = NavigationSession.class.getSimpleName();
	static final String URI_SCHEME = "osmand";
	static final String URI_HOST = "car_navigation";

	/**
	 * Invalid zoom focal point value, used for the zoom buttons.
	 */
	public static final float INVALID_FOCAL_POINT_VAL = -1f;

	/**
	 * Zoom-in scale factor, used for the zoom-in button.
	 */
	public static final float ZOOM_IN_BUTTON_SCALE_FACTOR = 1.1f;

	/**
	 * Zoom-out scale factor, used for the zoom-out button.
	 */
	public static final float ZOOM_OUT_BUTTON_SCALE_FACTOR = 0.9f;

	private OsmandSettings settings;
	private RoutingHelper routingHelper;

	NavigationScreen navigationScreen;
	LandingScreen landingScreen;

	RequestPurchaseScreen requestPurchaseScreen;
	SurfaceRenderer navigationCarSurface;
	Action settingsAction;

	private OsmandMapTileView mapView;
	private ApplicationMode defaultAppMode;

	private OsmAndLocationProvider locationProvider;
	private LocationServiceHelper locationServiceHelper;
	private StateChangedListener<LocationSource> locationSourceListener;
	private long lastTimeGPSLocationFixed;

	private CarContext carContext;
	private NavigationManager navigationManager;
	private boolean carNavigationActive;
	private TripHelper tripHelper;

	NavigationSession() {
		getLifecycle().addObserver(this);
	}

	@Nullable
	public NavigationScreen getNavigationScreen() {
		return navigationScreen;
	}

	public SurfaceRenderer getNavigationCarSurface() {
		return navigationCarSurface;
	}

	public Action getSettingsAction() {
		return settingsAction;
	}

	public OsmandMapTileView getMapView() {
		return mapView;
	}

	private final LocationPermissionCheckCallback locationPermissionGrantedCallback = this::requestLocationUpdates;

	public void setMapView(OsmandMapTileView mapView) {
		this.mapView = mapView;
		SurfaceRenderer navigationCarSurface = this.navigationCarSurface;
		if (navigationCarSurface != null) {
			navigationCarSurface.setMapView(hasStarted() ? mapView : null);
		}
	}

	private OsmandApplication getApp() {
		return (OsmandApplication) getCarContext().getApplicationContext();
	}

	@Override
	public void onCreate(@NonNull LifecycleOwner owner) {
		OsmandApplication app = getApp();
		settings = app.getSettings();
		routingHelper = app.getRoutingHelper();
		locationProvider = app.getLocationProvider();
		locationServiceHelper = app.createLocationServiceHelper();

		app.setCarNavigationSession(this);
		app.getLocationProvider().addLocationListener(this);
		setCarContext(getCarContext());
		requestLocationUpdates();
		addLocationSourceListener();
	}

	@Override
	public void onStart(@NonNull LifecycleOwner owner) {
		OsmandApplication app = getApp();
		routingHelper.addListener(this);

		defaultAppMode = settings.getApplicationMode();
		if (!isAppModeDerivedFromCar(defaultAppMode)) {
			List<ApplicationMode> availableAppModes = ApplicationMode.values(app);
			for (ApplicationMode availableAppMode : availableAppModes) {
				if (isAppModeDerivedFromCar(availableAppMode)) {
					settings.setApplicationMode(availableAppMode);
					break;
				}
			}
		}
		if (navigationCarSurface != null) {
			navigationCarSurface.handleRecenter();
		}

		app.onCarNavigationSessionStart(this);
		app.getOsmandMap().getMapView().setupRenderingView();

		if (!app.isAppInForegroundOnRootDevice()) {
			checkAppInitialization(new RestoreNavigationHelper(app, null));
		}
	}

	@Override
	public void onResume(@NonNull LifecycleOwner owner) {
		showRoutePreview();
	}

	@Override
	public void onStop(@NonNull LifecycleOwner owner) {
		OsmandApplication app = getApp();
		routingHelper.removeListener(this);
		settings.setLastKnownMapElevation(app.getOsmandMap().getMapView().getElevationAngle());
		boolean routing = settings.FOLLOW_THE_ROUTE.get() || routingHelper.isRouteCalculated() || routingHelper.isRouteBeingCalculated();
		if (defaultAppMode != null && !routing) {
			settings.setApplicationMode(defaultAppMode);
		}
		defaultAppMode = null;

		app.getOsmandMap().getMapView().setupRenderingView();
		app.onCarNavigationSessionStop(this);
	}

	@Override
	public void onDestroy(@NonNull LifecycleOwner owner) {
		OsmandApplication app = getApp();
		removeLocationUpdates();
		removeLocationSourceListener();

		getLifecycle().removeObserver(this);
		if (settings.simulateNavigationStartedFromAdb) {
			settings.simulateNavigation = false;
			OsmAndLocationSimulation locationSimulation = app.getLocationProvider().getLocationSimulation();
			if (locationSimulation.isRouteAnimating() || locationSimulation.isLoadingRouteLocations()) {
				locationSimulation.stop();
			}
		}
		settings.simulateNavigationStartedFromAdb = false;

		clearCarContext();
		app.getLocationProvider().removeLocationListener(this);
		app.setCarNavigationSession(null);
	}

	private boolean isAppModeDerivedFromCar(ApplicationMode appMode) {
		return appMode == ApplicationMode.CAR || appMode.isDerivedRoutingFrom(ApplicationMode.CAR);
	}

	public boolean hasStarted() {
		Lifecycle.State state = getLifecycle().getCurrentState();
		return state == Lifecycle.State.STARTED || state == Lifecycle.State.RESUMED;
	}

	public boolean isStateAtLeast(@NonNull State state) {
		return getLifecycle().getCurrentState().isAtLeast(state);
	}

	public boolean hasSurface() {
		SurfaceRenderer navigationCarSurface = this.navigationCarSurface;
		return navigationCarSurface != null && navigationCarSurface.hasSurface();
	}

	@Override
	@NonNull
	public Screen onCreateScreen(@NonNull Intent intent) {
		Log.i(TAG, "In onCreateScreen()");
		navigationCarSurface = new SurfaceRenderer(getCarContext(), getLifecycle());
		settingsAction = new Action.Builder()
				.setIcon(new CarIcon.Builder(
						IconCompat.createWithResource(getCarContext(), R.drawable.ic_action_settings_outlined))
						.build())
				.setOnClickListener(() -> getCarContext()
						.getCarService(ScreenManager.class)
						.push(new SettingsScreen(getCarContext())))
				.build();

		if (mapView != null) {
			navigationCarSurface.setMapView(mapView);
		}

		String action = intent.getAction();
		if (ACTION_NAVIGATE.equals(action)) {
			CarToast.makeText(getCarContext(), "Navigation intent: " + intent.getDataString(), CarToast.LENGTH_LONG).show();
		}
		landingScreen = new LandingScreen(getCarContext(), settingsAction);

		OsmandApplication app = getApp();
		if (!InAppPurchaseUtils.isAndroidAutoAvailable(app)) {
			getCarContext().getCarService(ScreenManager.class).push(landingScreen);
			requestPurchaseScreen = new RequestPurchaseScreen(getCarContext());
			return requestPurchaseScreen;
		}

		if (!isLocationPermissionAvailable()) {
			getCarContext().getCarService(ScreenManager.class).push(landingScreen);
			return new RequestPermissionScreen(getCarContext(), locationPermissionGrantedCallback);
		}
		return landingScreen;
	}

	public void onPurchaseDone() {
		OsmandApplication app = getApp();
		if (requestPurchaseScreen != null && InAppPurchaseUtils.isAndroidAutoAvailable(app)) {
			requestPurchaseScreen.finish();
			requestPurchaseScreen = null;
			app.getOsmandMap().getMapView().setupRenderingView();

			requestLocationPermission();
		}
	}

	public boolean isLocationPermissionAvailable() {
		boolean accessFineLocation = ActivityCompat.checkSelfPermission(getCarContext(), Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED;
		boolean accessCoarseLocation = ActivityCompat.checkSelfPermission(getCarContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
				== PackageManager.PERMISSION_GRANTED;
		return accessFineLocation || accessCoarseLocation;
	}

	private boolean requestLocationPermission() {
		if (!isLocationPermissionAvailable()) {
			getCarContext().getCarService(ScreenManager.class).push(
					new RequestPermissionScreen(getCarContext(), locationPermissionGrantedCallback));
			return true;
		}
		return false;
	}

	@Override
	public void onNewIntent(@NonNull Intent intent) {
		Log.i(TAG, "In onNewIntent() " + intent);
		Uri uri = intent.getData();
		if (uri != null) {
			if (ACTION_NAVIGATE.equals(intent.getAction())) {
				processNavigationIntent(uri);
			} else {
				processDeepLinkActions(uri);
			}
		}
	}

	private void processNavigationIntent(@NonNull Uri uri) {
		GeoParsedPoint point = GeoPointParserUtil.parse(uri.toString());
		if (point != null) {
			CarContext context = getCarContext();
			ScreenManager screenManager = context.getCarService(ScreenManager.class);
			screenManager.popToRoot();

			if (point.isGeoPoint()) {
				SearchResult result = new SearchResult();
				result.objectType = ObjectType.LOCATION;
				result.object = result.location = new LatLon(point.getLatitude(), point.getLongitude());

				String label = point.getLabel();
				if (Algorithms.isEmpty(label)) {
					PointDescription description = new PointDescription(point.getLatitude(), point.getLongitude());
					result.localeName = description.getSimpleName(getApp(), false);
				} else {
					result.localeName = label;
				}
				screenManager.pushForResult(new RoutePreviewScreen(context, settingsAction, result), (obj) -> {
					if (obj != null) {
						getApp().getOsmandMap().getMapLayers().getMapActionsHelper().startNavigation();
						if (hasStarted()) {
							startNavigation();
						}
					}
				});
			} else {
				String text = point.isGeoAddress() ? point.getQuery() : uri.toString();
				screenManager.pushForResult(new SearchResultsScreen(context, settingsAction, text), (obj) -> {});
			}
		}
	}

	private void processDeepLinkActions(@NonNull Uri uri) {
		// Process the intent from DeepLinkNotificationReceiver. Bring the routing screen back to
		// the top if any other screens were pushed onto it.
		if (URI_SCHEME.equals(uri.getScheme()) && URI_HOST.equals(uri.getSchemeSpecificPart())
				&& DEEP_LINK_ACTION_OPEN_ROOT_SCREEN.equals(uri.getFragment())) {
			ScreenManager screenManager = getCarContext().getCarService(ScreenManager.class);
			Screen top = screenManager.getTop();

			boolean followingMode = routingHelper.isFollowingMode();
			boolean routeCalculated = routingHelper.isRouteCalculated();
			boolean pauseNavigation = routingHelper.isPauseNavigation();

			boolean navigation = followingMode || routeCalculated && pauseNavigation;
			if (navigation && !(top instanceof NavigationScreen) || !navigation && !(top instanceof LandingScreen)) {
				screenManager.popToRoot();
			}
		}
	}

	@Override
	public void onCarConfigurationChanged(@NonNull Configuration newConfiguration) {
		if (navigationCarSurface != null) {
			navigationCarSurface.onCarConfigurationChanged();
		}
	}

	@Override
	public boolean requestLocationNavigation() {
		return requestLocationPermission();
	}

	public void startNavigation() {
		createNavigationScreen();
		getCarContext().getCarService(ScreenManager.class).push(navigationScreen);
	}

	private void createNavigationScreen() {
		navigationScreen = new NavigationScreen(getCarContext(), settingsAction, this);
		navigationCarSurface.setCallback(navigationScreen);
	}

	@Override
	public void stopNavigation() {
		OsmandApplication app = getApp();
		if (app != null) {
			app.stopNavigation();
			clearNavigationScreen();
		}
	}

	private void clearNavigationScreen() {
		if (navigationScreen != null) {
			navigationScreen.stopTrip();
			navigationScreen = null;
		}
	}

	@Override
	public void updateLocation(Location location) {
		SurfaceRenderer navigationCarSurface = this.navigationCarSurface;
		if (navigationCarSurface != null) {
			navigationCarSurface.updateLocation(location);
		}
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		showRoutePreview();
	}

	@Override
	public void routeWasCancelled() {
		clearNavigationScreen();
	}

	@Override
	public void routeWasFinished() {
		getApp().stopNavigation();
	}

	private void showRoutePreview() {
		OsmandApplication app = getApp();
		CarContext context = getCarContext();
		ScreenManager screenManager = context.getCarService(ScreenManager.class);
		Screen top = screenManager.getTop();
		TargetPoint pointToNavigate = app.getTargetPointsHelper().getPointToNavigate();
		if (app.getRoutingHelper().isRouteCalculated() && !settings.FOLLOW_THE_ROUTE.get()
				&& pointToNavigate != null && !(top instanceof RoutePreviewScreen)) {
			SearchResult result = new SearchResult();
			result.location = new LatLon(pointToNavigate.getLatitude(), pointToNavigate.getLongitude());
			GpxFile gpxFile = app.getRoutingHelper().getCurrentGPX();
			if (gpxFile != null) {
				String fileName = "";
				if (!Algorithms.isEmpty(gpxFile.getPath())) {
					fileName = new File(gpxFile.getPath()).getName();
				} else if (!Algorithms.isEmpty(gpxFile.getTracks())) {
					fileName = gpxFile.getTracks().get(0).getName();
				}
				if (Algorithms.isEmpty(fileName)) {
					fileName = app.getString(R.string.shared_string_gpx_track);
				}
				result.localeName = GpxUiHelper.getGpxTitle(fileName);
				result.object = gpxFile;
				result.objectType = ObjectType.GPX_TRACK;
			} else {
				result.localeName = pointToNavigate.getPointDescription(context).getSimpleName(app, false);
				result.object = new HistoryEntry(pointToNavigate.getLatitude(), pointToNavigate.getLongitude(),
						pointToNavigate.getPointDescription(context), HistorySource.NAVIGATION);
				result.objectType = ObjectType.RECENT_OBJ;
			}

			screenManager.popToRoot();
			screenManager.pushForResult(new RoutePreviewScreen(context, settingsAction, result), (obj) -> {
				if (obj != null) {
					app.getOsmandMap().getMapLayers().getMapActionsHelper().startNavigation();
					if (hasStarted()) {
						startNavigation();
					}
				}
			});
		}
	}

	private void addLocationSourceListener() {
		OsmandApplication app = getApp();
		locationSourceListener = change -> {
			removeLocationUpdates();
			locationServiceHelper = app.createLocationServiceHelper();
			requestLocationUpdates();
		};
		settings.LOCATION_SOURCE.addListener(locationSourceListener);
	}

	private void removeLocationSourceListener() {
		settings.LOCATION_SOURCE.removeListener(locationSourceListener);
	}

	private void requestLocationUpdates() {
		try {
			locationServiceHelper.requestLocationUpdates(new LocationCallback() {
				@Override
				public void onLocationResult(@NonNull List<Location> locations) {
					if (!locations.isEmpty()) {
						Location location = locations.get(locations.size() - 1);
						lastTimeGPSLocationFixed = System.currentTimeMillis();
						locationProvider.setLocationFromService(location);
					}
				}
			});
			// try to always ask for network provide : it is faster way to find location
			if (locationServiceHelper.isNetworkLocationUpdatesSupported()) {
				locationServiceHelper.requestNetworkLocationUpdates(new LocationCallback() {
					@Override
					public void onLocationResult(@NonNull List<net.osmand.Location> locations) {
						if (!locations.isEmpty() && !useOnlyGPS()) {
							locationProvider.setLocationFromService(locations.get(locations.size() - 1));
						}
					}
				});
			}
		} catch (SecurityException e) {
			Toast.makeText(getCarContext(), R.string.no_location_permission, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(getCarContext(), R.string.gps_not_available, Toast.LENGTH_LONG).show();
		}
	}

	private void removeLocationUpdates() {
		if (locationServiceHelper != null) {
			try {
				locationServiceHelper.removeLocationUpdates();
			} catch (SecurityException e) {
				// Location service permission not granted
			} finally {
				lastTimeGPSLocationFixed = 0;
			}
		}
	}

	private boolean useOnlyGPS() {
		if (routingHelper.isFollowingMode()) {
			return true;
		}
		if (lastTimeGPSLocationFixed > 0 && (System.currentTimeMillis() - lastTimeGPSLocationFixed) < NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS) {
			return true;
		}
		return isRunningOnEmulator();
	}

	/**
	 * Sets the {@link CarContext} to use while the service is running.
	 */
	public void setCarContext(@Nullable CarContext carContext) {
		this.carContext = carContext;
		if (carContext != null) {
			this.tripHelper = new TripHelper(getApp());
			this.navigationManager = carContext.getCarService(NavigationManager.class);
			this.navigationManager.setNavigationManagerCallback(new NavigationManagerCallback() {
				@Override
				public void onStopNavigation() {
					if (routingHelper.isRouteCalculated() && routingHelper.isFollowingMode()) {
						routingHelper.pauseNavigation();
					} else {
						getApp().stopNavigation();
					}
				}

				@Override
				public void onAutoDriveEnabled() {
					CarToast.makeText(carContext, "Auto drive enabled", CarToast.LENGTH_LONG).show();
					if (!settings.simulateNavigation) {
						OsmAndLocationSimulation sim = getApp().getLocationProvider().getLocationSimulation();
						sim.startStopRouteAnimation(null);
						settings.simulateNavigation = true;
						settings.simulateNavigationStartedFromAdb = true;
					}
				}
			});
			// Uncomment if navigating
			// mNavigationManager.navigationStarted();
		} else {
			this.navigationManager = null;
		}
	}

	/**
	 * Clears the currently used {@link CarContext}.
	 */
	public void clearCarContext() {
		carContext = null;
		if (navigationManager != null) {
			navigationManager.clearNavigationManagerCallback();
		}
		navigationManager = null;
		tripHelper = null;
	}

	/**
	 * Starts navigation.
	 */
	public void startCarNavigation() {
		if (navigationManager != null) {
			navigationManager.navigationStarted();
			carNavigationActive = true;
		}
	}

	/**
	 * Stops navigation.
	 */
	public void stopCarNavigation() {
		getApp().runInUIThread(() -> {
					if (navigationManager != null) {
						NavigationSession carNavigationSession = getApp().getCarNavigationSession();
						if (carNavigationSession != null) {
							NavigationScreen navigationScreen = carNavigationSession.getNavigationScreen();
							if (navigationScreen != null) {
								navigationScreen.stopTrip();
							}
						}
						carNavigationActive = false;
						navigationManager.navigationEnded();
					}
				}
		);
	}

	public void updateCarNavigation(Location currentLocation) {
		OsmandApplication app = getApp();
		TripHelper tripHelper = this.tripHelper;
		if (carNavigationActive && navigationManager != null && tripHelper != null
				&& routingHelper.isRouteCalculated() && routingHelper.isFollowingMode()) {
			NavigationSession carNavigationSession = app.getCarNavigationSession();
			if (carNavigationSession != null) {
				NavigationScreen navigationScreen = carNavigationSession.getNavigationScreen();
				if (navigationScreen == null) {
					carNavigationSession.startNavigation();
					navigationScreen = carNavigationSession.getNavigationScreen();
				}
				if (navigationScreen != null) {
					float density = carNavigationSession.getNavigationCarSurface().getDensity();
					if (density == 0) {
						density = 1;
					}
					Trip trip = tripHelper.buildTrip(currentLocation, density);
					navigationManager.updateTrip(trip);

					List<Destination> destinations = null;
					Destination destination = tripHelper.getLastDestination();
					TravelEstimate destinationTravelEstimate = tripHelper.getLastDestinationTravelEstimate();
					if (destination != null) {
						destinations = Collections.singletonList(destination);
					}
					TravelEstimate lastStepTravelEstimate = tripHelper.getLastStepTravelEstimate();
					navigationScreen.updateTrip(true, routingHelper.isRouteBeingCalculated(),
							false/*routingHelper.isRouteWasFinished()*/,
							destinations, trip.getSteps(), destinationTravelEstimate,
							lastStepTravelEstimate != null ? lastStepTravelEstimate.getRemainingDistance() : null,
							true, true, null);
				}
			}
		}
	}

	public boolean isCarNavigationActive() {
		return carNavigationActive;
	}

	private void checkAppInitialization(@NonNull RestoreNavigationHelper restoreNavigationHelper) {
		OsmandApplication app = getApp();
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(@NonNull AppInitializer init, @NonNull AppInitEvents event) {
					if (event == AppInitEvents.INDEX_REGION_BOUNDARIES) {
						if (app.getAppInitializer().isRoutingConfigInitialized()) {
							restoreNavigationHelper.checkRestoreRoutingMode();
						}
					}
					if (event == ROUTING_CONFIG_INITIALIZED) {
						if (app.getRegions() != null) {
							restoreNavigationHelper.checkRestoreRoutingMode();
						}
					}
				}
			});
		} else {
			restoreNavigationHelper.checkRestoreRoutingMode();
		}
	}
}
