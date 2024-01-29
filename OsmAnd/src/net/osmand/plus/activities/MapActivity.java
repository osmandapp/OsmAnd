package net.osmand.plus.activities;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_DRAWER_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_STYLE_ID;
import static net.osmand.plus.firstusage.FirstUsageWizardFragment.FIRST_USAGE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.PLAN_ROUTE_MODE;
import static net.osmand.plus.views.AnimateDraggingMapThread.TARGET_NO_ROTATION;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.SecondSplashScreenFragment;
import net.osmand.StateChangedListener;
import net.osmand.aidl.AidlMapPointWrapper;
import net.osmand.aidl.OsmandAidlApi.AMapPointUpdateListener;
import net.osmand.core.android.MapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.ValueHolder;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.chooseplan.TripltekPromoFragment;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.WhatsNewDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.feedback.CrashBottomSheetDialogFragment;
import net.osmand.plus.feedback.RateUsHelper;
import net.osmand.plus.feedback.RenderInitErrorBottomSheet;
import net.osmand.plus.feedback.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.firstusage.FirstUsageWizardFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.DiscountHelper;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.helpers.LockHelper;
import net.osmand.plus.helpers.LockHelper.LockUIAdapter;
import net.osmand.plus.helpers.MapAppInitializeListener;
import net.osmand.plus.helpers.MapDisplayPositionManager;
import net.osmand.plus.helpers.MapFragmentsHelper;
import net.osmand.plus.helpers.MapPermissionsResultCallback;
import net.osmand.plus.helpers.MapRouteCalculationProgressListener;
import net.osmand.plus.helpers.MapScrollHelper;
import net.osmand.plus.helpers.RestoreNavigationHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ui.ImportGpxBottomSheetDialogFragment;
import net.osmand.plus.keyevent.KeyEventHelper;
import net.osmand.plus.keyevent.TrackballController;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.DestinationReachedFragment;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper.MapMarkerChangedListener;
import net.osmand.plus.mapmarkers.PlanRouteFragment;
import net.osmand.plus.measurementtool.GpxData;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.accessibility.MapAccessibilityActions;
import net.osmand.plus.render.UpdateVectorRendererAsyncTask;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RouteCalculationProgressListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper.TransportRouteCalculationProgressCallback;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.search.dialogs.QuickSearchDialogFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.datastorage.SharedStorageWarningFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.simulation.LoadSimulatedLocationsTask.LoadSimulatedLocationsListener;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.simulation.SimulatedLocation;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper.NewGpxPoint;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.MapViewWithLayers;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.OsmandMapTileView.OnDrawMapListener;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapActivity extends OsmandActionBarActivity implements DownloadEvents,
		IRouteInformationListener, AMapPointUpdateListener, MapMarkerChangedListener,
		OnDrawMapListener, OsmAndAppCustomizationListener, LockUIAdapter,
		OnPreferenceStartFragmentCallback {

	public static final String INTENT_KEY_PARENT_MAP_ACTIVITY = "intent_parent_map_activity_key";
	public static final String INTENT_PARAMS = "intent_prarams";

	private static final int ZOOM_LABEL_DISPLAY = 16;
	private static final int MAX_ZOOM_OUT_STEPS = 2;
	private static final int SECOND_SPLASH_TIME_OUT = 8000;

	private static final Log LOG = PlatformUtil.getLog(MapActivity.class);

	private static final MapContextMenu mapContextMenu = new MapContextMenu();
	private static final MapRouteInfoMenu mapRouteInfoMenu = new MapRouteInfoMenu();
	private static final TrackDetailsMenu trackDetailsMenu = new TrackDetailsMenu();
	@Nullable
	private static Intent prevActivityIntent = null;

	private final List<ActivityResultListener> activityResultListeners = new ArrayList<>();

	private BroadcastReceiver screenOffReceiver;

	private MapActivityActions mapActions;
	private WidgetsVisibilityHelper mapWidgetsVisibilityHelper;

	private ExtendedMapActivity extendedMapActivity;

	// App variables
	private OsmandApplication app;
	private OsmandSettings settings;

	private LockHelper lockHelper;
	private ImportHelper importHelper;
	private IntentHelper intentHelper;
	private MapScrollHelper mapScrollHelper;
	private RestoreNavigationHelper restoreNavigationHelper;

	private StateChangedListener<ApplicationMode> applicationModeListener;

	private final DashboardOnMap dashboardOnMap = new DashboardOnMap(this);
	private final MapFragmentsHelper fragmentsHelper = new MapFragmentsHelper(this);
	private final TrackballController trackballController = new TrackballController(this);
	private final MapPermissionsResultCallback permissionsResultCallback = new MapPermissionsResultCallback(this);

	private AppInitializeListener initListener;
	private MapViewWithLayers mapViewWithLayers;
	private DrawerLayout drawerLayout;
	private boolean drawerDisabled;

	private boolean mIsDestroyed;
	private boolean pendingPause;
	private Timer splashScreenTimer;
	private boolean activityRestartNeeded;
	private boolean stopped = true;

	private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

	private final StateChangedListener<Integer> mapScreenOrientationSettingListener = new StateChangedListener<Integer>() {
		@Override
		public void stateChanged(Integer change) {
			app.runInUIThread(() -> applyScreenOrientation());
		}
	};

	private final StateChangedListener<Boolean> useSystemScreenTimeoutListener = new StateChangedListener<Boolean>() {
		@Override
		public void stateChanged(Boolean change) {
			app.runInUIThread(() -> changeKeyguardFlags());
		}
	};
	private KeyEventHelper keyEventHelper;
	private RouteCalculationProgressListener routeCalculationProgressCallback;
	private TransportRouteCalculationProgressCallback transportRouteCalculationProgressCallback;
	private LoadSimulatedLocationsListener simulatedLocationsListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setRequestedOrientation(AndroidUiHelper.getScreenOrientation(this));
		long tm = System.currentTimeMillis();
		app = getMyApplication();
		settings = app.getSettings();
		lockHelper = app.getLockHelper();
		mapScrollHelper = new MapScrollHelper(app);
		keyEventHelper = app.getKeyEventHelper();
		restoreNavigationHelper = new RestoreNavigationHelper(this);
		app.applyTheme(this);
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

		mapContextMenu.setMapActivity(this);
		mapRouteInfoMenu.setMapActivity(this);
		trackDetailsMenu.setMapActivity(this);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		enterToFullScreen();
		// Navigation Drawer
		AndroidUtils.addStatusBarPadding21v(this, findViewById(R.id.menuItems));

		if (WhatsNewDialogFragment.shouldShowDialog(app)) {
			boolean showed = WhatsNewDialogFragment.showInstance(getSupportFragmentManager());
			if (showed) {
				SecondSplashScreenFragment.SHOW = false;
			}
		}
		mapActions = new MapActivityActions(this);
		mapWidgetsVisibilityHelper = new WidgetsVisibilityHelper(this);
		dashboardOnMap.createDashboardView();
		extendedMapActivity = new ExtendedMapActivity();

		getMapView().setMapActivity(this);
		getMapLayers().setMapActivity(this);

		intentHelper = new IntentHelper(this);
		intentHelper.parseLaunchIntents();

		OsmandMapTileView mapView = getMapView();

		mapView.setTrackBallDelegate(e -> {
			mapView.showAndHideMapPosition();
			return onTrackballEvent(e);
		});
		mapView.setAccessibilityActions(new MapAccessibilityActions(this));
		getMapViewTrackingUtilities().setMapView(mapView);
		getMapLayers().createAdditionalLayers(this);

		createProgressBarForRouting();
		updateStatusBarColor();

		if ((app.getRoutingHelper().isRouteCalculated() || app.getRoutingHelper().isRouteBeingCalculated())
				&& !app.getRoutingHelper().isRoutePlanningMode()
				&& !settings.FOLLOW_THE_ROUTE.get()
				&& app.getTargetPointsHelper().getAllPoints().size() > 0) {
			app.getRoutingHelper().clearCurrentRoute(null, new ArrayList<>());
			app.getTargetPointsHelper().removeAllWayPoints(false, false);
		}

		if (!settings.isLastKnownMapLocation()) {
			// show first time when application ran
			net.osmand.Location location = app.getLocationProvider().getFirstTimeRunDefaultLocation(loc -> {
				if (app.getLocationProvider().getLastKnownLocation() == null) {
					setMapInitialLatLon(getMapView(), loc);
				}
			});
			getMapViewTrackingUtilities().setMapLinkedToLocation(true);
			if (location != null) {
				setMapInitialLatLon(mapView, location);
			}
		}
		PluginsHelper.onMapActivityCreate(this);
		importHelper = app.getImportHelper();
		importHelper.setUiActivity(this);
		if (System.currentTimeMillis() - tm > 50) {
			LOG.error("OnCreate for MapActivity took " + (System.currentTimeMillis() - tm) + " ms");
		}
		mapView.refreshMap(true);

		drawerLayout = findViewById(R.id.drawer_layout);
		mapViewWithLayers = findViewById(R.id.map_view_with_layers);

		checkAppInitialization();

		mapActions.updateDrawerMenu();

		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		screenOffReceiver = new ScreenOffReceiver();
		registerReceiver(screenOffReceiver, filter);

		app.getAidlApi().onCreateMapActivity(this);

		lockHelper.setLockUIAdapter(this);
		keyEventHelper.setMapActivity(this);
		mIsDestroyed = false;
		if (mapViewWithLayers != null) {
			mapViewWithLayers.onCreate(savedInstanceState);
		}
		extendedMapActivity.onCreate(this, savedInstanceState);
	}

	private void setMapInitialLatLon(@NonNull OsmandMapTileView mapView, @Nullable Location location) {
		if (location != null) {
			mapView.setLatLon(location.getLatitude(), location.getLongitude());
			mapView.setIntZoom(14);
		}
	}

	public void exitFromFullScreen(View view) {
		if (!PluginsHelper.isDevelopment() || settings.TRANSPARENT_STATUS_BAR.get()) {
			AndroidUtils.exitFromFullScreen(this, view);
		}
	}

	public void enterToFullScreen() {
		if (!PluginsHelper.isDevelopment() || settings.TRANSPARENT_STATUS_BAR.get()) {
			AndroidUtils.enterToFullScreen(this, getLayout());
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (fragmentsHelper.removeFragment(PlanRouteFragment.TAG)) {
			app.getMapMarkersHelper().getPlanRouteContext().setFragmentVisible(true);
		}
		fragmentsHelper.removeFragment(ImportGpxBottomSheetDialogFragment.TAG);
		fragmentsHelper.removeFragment(AdditionalActionsBottomSheetDialogFragment.TAG);
		extendedMapActivity.onSaveInstanceState(this, outState);
		super.onSaveInstanceState(outState);
	}

	private void checkAppInitialization() {
		if (app.isApplicationInitializing()) {
			findViewById(R.id.init_progress).setVisibility(View.VISIBLE);

			initListener = new MapAppInitializeListener(this);
			getMyApplication().checkApplicationIsBeingInitialized(initListener);
		} else {
			app.getOsmandMap().setupRenderingView();
			restoreNavigationHelper.checkRestoreRoutingMode();
		}
	}

	private void createProgressBarForRouting() {
		routeCalculationProgressCallback = new MapRouteCalculationProgressListener(this);

		app.getRoutingHelper().addCalculationProgressListener(routeCalculationProgressCallback);

		transportRouteCalculationProgressCallback = new TransportRouteCalculationProgressCallback() {
			@Override
			public void start() {
				routeCalculationProgressCallback.onCalculationStart();
			}

			@Override
			public void updateProgress(int progress) {
				routeCalculationProgressCallback.onUpdateCalculationProgress(progress);
			}

			@Override
			public void finish() {
				routeCalculationProgressCallback.onCalculationFinish();
			}
		};
		app.getTransportRoutingHelper().setProgressBar(transportRouteCalculationProgressCallback);

		simulatedLocationsListener = new LoadSimulatedLocationsListener() {
			@Override
			public void onLocationsStartedLoading() {
				if (!isRouteBeingCalculated()) {
					ProgressBar progressBar = findViewById(R.id.map_horizontal_progress);
					AndroidUiHelper.updateVisibility(progressBar, true);
				}
			}

			@Override
			public void onLocationsLoadingProgress(int progress) {
				if (!isRouteBeingCalculated()) {
					updateProgress(progress);
				}
			}

			@Override
			public void onLocationsLoaded(@Nullable List<SimulatedLocation> locations) {
				if (!isRouteBeingCalculated()) {
					ProgressBar progressBar = findViewById(R.id.map_horizontal_progress);
					AndroidUiHelper.updateVisibility(progressBar, false);
				}
			}
		};
		app.getLocationProvider().getLocationSimulation().addListener(simulatedLocationsListener);
	}

	private void destroyProgressBarForRouting() {
		app.getLocationProvider().getLocationSimulation().removeListener(simulatedLocationsListener);
		simulatedLocationsListener = null;
		app.getTransportRoutingHelper().setProgressBar(null);
		transportRouteCalculationProgressCallback = null;
		app.getRoutingHelper().removeCalculationProgressListener(routeCalculationProgressCallback);
		routeCalculationProgressCallback = null;
	}

	public void updateProgress(int progress) {
		ProgressBar progressBar = findViewById(R.id.map_horizontal_progress);
		if (findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE) {
			if (mapRouteInfoMenu.isVisible() || dashboardOnMap.isVisible()) {
				AndroidUiHelper.updateVisibility(progressBar, false);
				return;
			}
			if (progressBar.getVisibility() == View.GONE) {
				AndroidUiHelper.updateVisibility(progressBar, true);
			}
			progressBar.setProgress(progress);
			progressBar.invalidate();
			progressBar.requestLayout();
		}
	}

	private boolean isRouteBeingCalculated() {
		return app.getRoutingHelper().isRouteBeingCalculated() || app.getTransportRoutingHelper().isRouteBeingCalculated();
	}

	public void setupRouteCalculationProgressBar(@NonNull ProgressBar pb) {
		RoutingHelper routingHelper = getRoutingHelper();
		setupProgressBar(pb, routingHelper.isPublicTransportMode() || !routingHelper.isOsmandRouting());
	}

	public void setupProgressBar(@NonNull ProgressBar pb, boolean indeterminate) {
		DayNightHelper dayNightHelper = getMyApplication().getDaynightHelper();

		boolean nightMode = dayNightHelper.isNightModeForMapControls();
		boolean useRouteLineColor = nightMode == dayNightHelper.isNightMode();

		int bgColorId = nightMode ? R.color.map_progress_bar_bg_dark : R.color.map_progress_bar_bg_light;
		int bgColor = ContextCompat.getColor(this, bgColorId);

		int progressColor = useRouteLineColor
				? getMapLayers().getRouteLayer().getRouteLineColor(nightMode)
				: ContextCompat.getColor(this, R.color.active_color_primary_light);

		pb.setProgressDrawable(AndroidUtils.createProgressDrawable(bgColor, progressColor));
		pb.setIndeterminate(indeterminate);
		pb.getIndeterminateDrawable().setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN);
	}

	public ImportHelper getImportHelper() {
		return importHelper;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

		importHelper.setUiActivity(this);
		if (!intentHelper.parseLaunchIntents()) {
			intentHelper.parseContentIntent();
		}
	}

	@Override
	public void startActivity(Intent intent) {
		clearPrevActivityIntent();
		super.startActivity(intent);
	}

	@Override
	public void onBackPressed() {
		if (dashboardOnMap.onBackPressed()) {
			return;
		}
		if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
			closeDrawer();
			return;
		}
		if (getMapLayers().getContextMenuLayer().isInAddGpxPointMode()) {
			quitAddGpxPointMode();
		}
		int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
		if (backStackEntryCount == 0 && launchPrevActivityIntent()) {
			return;
		}
		QuickSearchDialogFragment fragment = fragmentsHelper.getQuickSearchDialogFragment();
		if ((backStackEntryCount == 0 || mapContextMenu.isVisible()) && fragment != null && fragment.isSearchHidden()) {
			fragmentsHelper.showQuickSearch(ShowQuickSearchMode.CURRENT, false);
			return;
		}
		super.onBackPressed();
	}

	public boolean launchPrevActivityIntent() {
		if (prevActivityIntent != null) {
			prevActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			AndroidUtils.startActivityIfSafe(this, prevActivityIntent);
			prevActivityIntent = null;
			return true;
		}
		return false;
	}

	private void quitAddGpxPointMode() {
		getMapLayers().getContextMenuLayer().getAddGpxPointBottomSheetHelper().hide();
		getMapLayers().getContextMenuLayer().quitAddGpxPoint();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		intentHelper.parseLaunchIntents();
	}

	@Override
	protected void onResume() {
		super.onResume();

		MapActivity mapViewMapActivity = getMapView().getMapActivity();
		if (activityRestartNeeded || !getMapLayers().hasMapActivity()
				|| (mapViewMapActivity != null && mapViewMapActivity != this)) {
			activityRestartNeeded = false;
			recreate();
			return;
		}
		importHelper.setUiActivity(this);

		long time = System.currentTimeMillis();
		FragmentManager fragmentManager = getSupportFragmentManager();

		if (app.getMapMarkersHelper().getPlanRouteContext().isFragmentVisible()) {
			PlanRouteFragment.showInstance(this);
		}

		if (app.isApplicationInitializing() || DashboardOnMap.staticVisible) {
			if (!dashboardOnMap.isVisible() && settings.SHOW_DASHBOARD_ON_START.get()) {
				dashboardOnMap.setDashboardVisibility(true, DashboardOnMap.staticVisibleType);
			}
		}
		dashboardOnMap.updateLocation(true, true, false);

		if (!dashboardOnMap.isVisible()) {
			if (RenderInitErrorBottomSheet.shouldShow(settings, this)) {
				SecondSplashScreenFragment.SHOW = false;
				RenderInitErrorBottomSheet.showInstance(fragmentManager);
			} else if (CrashBottomSheetDialogFragment.shouldShow(settings, this)) {
				SecondSplashScreenFragment.SHOW = false;
				CrashBottomSheetDialogFragment.showInstance(fragmentManager);
			} else if (RateUsHelper.shouldShowRateDialog(app)) {
				SecondSplashScreenFragment.SHOW = false;
				RateUsHelper.showRateDialog(this);
			} else if (TripltekPromoFragment.shouldShow(app)) {
				SecondSplashScreenFragment.SHOW = false;
				TripltekPromoFragment.showInstance(fragmentManager);
			}
		}

		boolean showStorageMigrationScreen = false;
		if (fragmentsHelper.getFragment(WhatsNewDialogFragment.TAG) == null || WhatsNewDialogFragment.wasNotShown()) {
			if (fragmentsHelper.getFragment(SharedStorageWarningFragment.TAG) == null && SharedStorageWarningFragment.dialogShowRequired(app)) {
				showStorageMigrationScreen = true;
				SecondSplashScreenFragment.SHOW = false;
				SharedStorageWarningFragment.showInstance(getSupportFragmentManager(), true);
			}
		}

		getMyApplication().getNotificationHelper().refreshNotifications();
		// fixing bug with action bar appearing on android 2.3.3
		if (getSupportActionBar() != null) {
			getSupportActionBar().hide();
		}

		// for voice navigation. Lags behind routingAppMode changes, hence repeated under onCalculationFinish()
		ApplicationMode routingAppMode = getRoutingHelper().getAppMode();
		if (routingAppMode != null && settings.AUDIO_MANAGER_STREAM.getModeValue(routingAppMode) != null) {
			setVolumeControlStream(settings.AUDIO_MANAGER_STREAM.getModeValue(routingAppMode));
		}

		applicationModeListener = prevAppMode -> app.runInUIThread(() -> {
			if (settings.APPLICATION_MODE.get() != prevAppMode) {
				settings.setLastKnownMapRotation(prevAppMode, getMapRotateTarget());
				settings.setLastKnownMapElevation(prevAppMode, getMapElevationAngle());
				updateApplicationModeSettings();
			}
		});
		settings.APPLICATION_MODE.addListener(applicationModeListener);
		updateApplicationModeSettings();


		// if destination point was changed try to recalculate route
		TargetPointsHelper targets = app.getTargetPointsHelper();
		RoutingHelper routingHelper = app.getRoutingHelper();
		if (routingHelper.isFollowingMode()
				&& (!Algorithms.objectEquals(targets.getPointToNavigate().point, routingHelper.getFinalLocation()) || !Algorithms
				.objectEquals(targets.getIntermediatePointsLatLonNavigation(), routingHelper.getIntermediatePoints()))) {
			targets.updateRouteAndRefresh(true);
		}
		app.getLocationProvider().resumeAllUpdates();

		OsmandMapTileView mapView = getMapView();
		if (settings.isLastKnownMapLocation()) {
			LatLon l = settings.getLastKnownMapLocation();
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setHeight(settings.getLastKnownMapHeight());
			mapView.setZoomWithFloatPart(settings.getLastKnownMapZoom(), settings.getLastKnownMapZoomFloatPart());
			mapView.initMapRotationByCompassMode();
		}

		settings.MAP_ACTIVITY_ENABLED.set(true);
		mapView.showAndHideMapPosition();

		readLocationToShow();

		PluginsHelper.checkInstalledMarketPlugins(app, this);
		PluginsHelper.onMapActivityResume(this);

		intentHelper.parseContentIntent();
		mapView.refreshMap(true);

		if (mapViewWithLayers != null) {
			mapViewWithLayers.onResume();
		}
		app.getLauncherShortcutsHelper().updateLauncherShortcuts();
		app.getDownloadThread().setUiActivity(this);

		boolean routeWasFinished = routingHelper.isRouteWasFinished();
		if (routeWasFinished && !DestinationReachedFragment.wasShown()) {
			DestinationReachedFragment.show(this);
		}

		routingHelper.addListener(this);
		app.getMapMarkersHelper().addListener(this);

		if (System.currentTimeMillis() - time > 50) {
			LOG.error("onResume for MapActivity took " + (System.currentTimeMillis() - time) + " ms");
		}

		boolean showOsmAndWelcomeScreen = true;
		Intent intent = getIntent();
		if (intent != null && intent.hasExtra(FirstUsageWizardFragment.SHOW_OSMAND_WELCOME_SCREEN)) {
			showOsmAndWelcomeScreen = intent.getBooleanExtra(FirstUsageWizardFragment.SHOW_OSMAND_WELCOME_SCREEN, true);
		}
		boolean showWelcomeScreen = ((app.getAppInitializer().isFirstTime() && Version.isDeveloperVersion(app)) || !app.getResourceManager().isAnyMapInstalled())
				&& settings.SHOW_OSMAND_WELCOME_SCREEN.get()
				&& showOsmAndWelcomeScreen && !showStorageMigrationScreen;

		if (!showWelcomeScreen && !MapPermissionsResultCallback.permissionDone && !app.getAppInitializer().isFirstTime()) {
			if (!permissionsResultCallback.permissionAsked) {
				if (app.isExternalStorageDirectoryReadOnly() && !showStorageMigrationScreen
						&& fragmentManager.findFragmentByTag(SharedStorageWarningFragment.TAG) == null
						&& fragmentManager.findFragmentByTag(SettingsScreenType.DATA_STORAGE.fragmentName) == null) {
					if (AndroidUtils.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
						Bundle args = new Bundle();
						args.putBoolean(FIRST_USAGE, true);
						BaseSettingsFragment.showInstance(this, SettingsScreenType.DATA_STORAGE, null, args, null);
					} else {
						ActivityCompat.requestPermissions(this,
								new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
								DownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
					}
				}
			} else {
				if (permissionsResultCallback.permissionGranted) {
					RestartActivity.doRestart(this, getString(R.string.storage_permission_restart_is_required));
				} else if (fragmentManager.findFragmentByTag(SettingsScreenType.DATA_STORAGE.fragmentName) == null) {
					Bundle args = new Bundle();
					args.putBoolean(FIRST_USAGE, true);
					BaseSettingsFragment.showInstance(this, SettingsScreenType.DATA_STORAGE, null, args, null);
				}
				permissionsResultCallback.permissionAsked = false;
				permissionsResultCallback.permissionGranted = false;
				MapPermissionsResultCallback.permissionDone = true;
			}
		}
		if (isDrawerAvailable()) {
			enableDrawer();
		} else {
			disableDrawer();
		}

		if (showWelcomeScreen && FirstUsageWizardFragment.showFragment(this)) {
			SecondSplashScreenFragment.SHOW = false;
		} else if (SendAnalyticsBottomSheetDialogFragment.shouldShowDialog(app)) {
			SendAnalyticsBottomSheetDialogFragment.showInstance(app, fragmentManager, null);
		}
		if (fragmentsHelper.isFirstScreenShowing() && (!settings.SHOW_OSMAND_WELCOME_SCREEN.get() || !showOsmAndWelcomeScreen)) {
			fragmentsHelper.disableFirstUsageFragment();
		}
		if (SecondSplashScreenFragment.SHOW && SecondSplashScreenFragment.showInstance(fragmentManager)) {
			SecondSplashScreenFragment.SHOW = false;
			SecondSplashScreenFragment.VISIBLE = true;
			mapView.setOnDrawMapListener(this);
			splashScreenTimer = new Timer();
			splashScreenTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					app.runInUIThread(fragmentsHelper::dismissSecondSplashScreen);
				}
			}, SECOND_SPLASH_TIME_OUT);
		} else {
			if (SecondSplashScreenFragment.VISIBLE) {
				fragmentsHelper.dismissSecondSplashScreen();
			}
			applyScreenOrientation();
		}

		settings.MAP_SCREEN_ORIENTATION.addListener(mapScreenOrientationSettingListener);
		settings.USE_SYSTEM_SCREEN_TIMEOUT.addListener(useSystemScreenTimeoutListener);

		extendedMapActivity.onResume(this);

		getMapView().getAnimatedDraggingThread().allowAnimations();
	}

	@Override
	public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
		if (isTopResumedActivity) {
			PluginsHelper.onMapActivityResumeOnTop(this);
		}
	}

	public void applyScreenOrientation() {
		if (settings.MAP_SCREEN_ORIENTATION.get() != getRequestedOrientation()) {
			setRequestedOrientation(settings.MAP_SCREEN_ORIENTATION.get());
		}
	}

	public void setKeepScreenOn(boolean keepScreenOn) {
		if (mapViewWithLayers != null) {
			mapViewWithLayers.setKeepScreenOn(keepScreenOn);
		}
	}

	public void updateStatusBarColor() {
		UiUtilities.updateStatusBarColor(this);
	}

	public boolean isInAppPurchaseAllowed() {
		return true;
	}

	@Override
	public void onDrawOverMap() {
		getMapView().setOnDrawMapListener(null);
		cancelSplashScreenTimer();
		fragmentsHelper.dismissSecondSplashScreen();
	}

	private void cancelSplashScreenTimer() {
		if (splashScreenTimer != null) {
			splashScreenTimer.cancel();
			splashScreenTimer = null;
		}
	}

	public boolean isActivityDestroyed() {
		return mIsDestroyed;
	}

	public boolean isMapVisible() {
		if (fragmentsHelper.isFragmentVisible()) {
			return false;
		}
		return AndroidUtils.isActivityNotDestroyed(this) && settings.MAP_ACTIVITY_ENABLED.get()
				&& !dashboardOnMap.isVisible();
	}

	public void readLocationToShow() {
		showMapControls();
		OsmandMapTileView mapView = getMapView();
		LatLon currentLatLon = new LatLon(mapView.getLatitude(), mapView.getLongitude());
		LatLon latLonToShow = settings.getAndClearMapLocationToShow();
		PointDescription mapLabelToShow = settings.getAndClearMapLabelToShow(latLonToShow);
		Object toShow = settings.getAndClearObjectToShow();
		boolean editToShow = settings.getAndClearEditObjectToShow();
		int status = settings.isRouteToPointNavigateAndClear();
		String searchRequestToShow = settings.getAndClearSearchRequestToShow();
		if (status != 0 || searchRequestToShow != null || latLonToShow != null) {
			fragmentsHelper.dismissSettingsScreens();
		}
		if (status != 0) {
			// always enable and follow and let calculate it (i.e.GPS is not accessible in a garage)
			Location loc = new Location("map");
			loc.setLatitude(mapView.getLatitude());
			loc.setLongitude(mapView.getLongitude());
			getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, true, true);
			if (dashboardOnMap.isVisible()) {
				dashboardOnMap.hideDashboard();
			}
		}
		if (trackDetailsMenu.isVisible()) {
			trackDetailsMenu.show();
		}
		if (searchRequestToShow != null) {
			fragmentsHelper.showQuickSearch(searchRequestToShow);
		}
		if (latLonToShow != null) {
			if (dashboardOnMap.isVisible()) {
				dashboardOnMap.hideDashboard();
			}
			// remember if map should come back to isMapLinkedToLocation=true
			getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			if (mapLabelToShow != null && !mapLabelToShow.contextMenuDisabled()) {
				mapContextMenu.setMapCenter(latLonToShow);
				mapContextMenu.setCenterMarker(true);

				RotatedTileBox tb = mapView.getCurrentRotatedTileBox().copy();
				LatLon prevCenter = tb.getCenterLatLon();

				double border = 0.8;
				int tbw = (int) (tb.getPixWidth() * border);
				int tbh = (int) (tb.getPixHeight() * border);
				tb.setPixelDimensions(tbw, tbh);

				tb.setLatLonCenter(latLonToShow.getLatitude(), latLonToShow.getLongitude());

				int zoom = settings.hasMapZoomToShow() ? settings.getMapZoomToShow() : ZOOM_LABEL_DISPLAY;
				tb.setZoom(zoom);
				while (!tb.containsLatLon(prevCenter.getLatitude(), prevCenter.getLongitude()) && tb.getZoom() > zoom - MAX_ZOOM_OUT_STEPS) {
					tb.setZoom(tb.getZoom() - 1);
				}
				mapContextMenu.setMapZoom(tb.getZoom());
				if (toShow instanceof GpxDisplayItem) {
					trackDetailsMenu.setGpxItem((GpxDisplayItem) toShow);
					trackDetailsMenu.show();
				} else if (mapRouteInfoMenu.isVisible()) {
					mapContextMenu.showMinimized(latLonToShow, mapLabelToShow, toShow);
					mapRouteInfoMenu.updateMenu();
					MapRouteInfoMenu.showLocationOnMap(this, latLonToShow.getLatitude(), latLonToShow.getLongitude());
				} else if (toShow instanceof GPXFile) {
					hideContextAndRouteInfoMenues();
					GPXFile gpxFile = (GPXFile) toShow;
					SelectedGpxFile selectedGpxFile;
					if (gpxFile.showCurrentTrack) {
						selectedGpxFile = app.getSavingTrackHelper().getCurrentTrack();
					} else {
						GpxSelectionParams params = GpxSelectionParams.newInstance()
								.showOnMap().selectedAutomatically().saveSelection();
						selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpxFile, params);
					}

					TrackAppearanceFragment.showInstance(this, selectedGpxFile, null);
				} else if (toShow instanceof QuadRect) {
					QuadRect qr = (QuadRect) toShow;
					mapView.fitRectToMap(qr.left, qr.right, qr.top, qr.bottom, (int) qr.width(), (int) qr.height(), 0);
				} else if (toShow instanceof NewGpxPoint) {
					NewGpxPoint newGpxPoint = (NewGpxPoint) toShow;
					QuadRect qr = newGpxPoint.getRect();
					mapView.fitRectToMap(qr.left, qr.right, qr.top, qr.bottom, (int) qr.width(), (int) qr.height(), 0);
					getMapLayers().getContextMenuLayer().enterAddGpxPointMode(newGpxPoint);
				} else if (toShow instanceof GpxData) {
					hideContextAndRouteInfoMenues();

					GpxData gpxData = (GpxData) toShow;
					QuadRect qr = gpxData.getRect();
					mapView.fitRectToMap(qr.left, qr.right, qr.top, qr.bottom, (int) qr.width(), (int) qr.height(), 0);
					MeasurementEditingContext editingContext = new MeasurementEditingContext(app);
					editingContext.setGpxData(gpxData);
					MeasurementToolFragment.showInstance(getSupportFragmentManager(), editingContext, PLAN_ROUTE_MODE, true);
				} else {
					mapContextMenu.show(latLonToShow, mapLabelToShow, toShow);
				}
				if (editToShow) {
					mapContextMenu.openEditor();
				}
			} else if (!latLonToShow.equals(currentLatLon)) {
				mapView.getAnimatedDraggingThread().startMoving(latLonToShow.getLatitude(),
						latLonToShow.getLongitude(), settings.getMapZoomToShow());
			}
		}
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (trackballController.onTrackballEvent(event)) {
			return true;
		}
		return super.onTrackballEvent(event);
	}

	@Override
	protected void onStart() {
		super.onStart();
		stopped = false;
		lockHelper.onStart();
		getMyApplication().getNotificationHelper().showNotifications();
		extendedMapActivity.onStart(this);
	}

	@Override
	protected void onStop() {
		getMyApplication().getNotificationHelper().removeNotifications(true);
		if (pendingPause) {
			onPauseActivity();
		}
		stopped = true;
		lockHelper.onStop(this);
		extendedMapActivity.onStop(this);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyProgressBarForRouting();
		getMapLayers().setMapActivity(null);
		getMapView().setMapActivity(null);
		mapContextMenu.setMapActivity(null);
		mapRouteInfoMenu.setMapActivity(null);
		trackDetailsMenu.setMapActivity(null);
		unregisterReceiver(screenOffReceiver);
		app.getAidlApi().onDestroyMapActivity(this);
		app.getImportHelper().resetUIActivity(this);
		restoreNavigationHelper.quitRouteRestoreDialog();
		PluginsHelper.onMapActivityDestroy(this);
		app.unsubscribeInitListener(initListener);
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession == null || !carNavigationSession.hasStarted()) {
			getMapViewTrackingUtilities().setMapView(null);
		}
		if (mapViewWithLayers != null) {
			mapViewWithLayers.onDestroy();
		}
		lockHelper.setLockUIAdapter(null);
		keyEventHelper.setMapActivity(null);
		extendedMapActivity.onDestroy(this);

		mIsDestroyed = true;
	}

	public LatLon getMapLocation() {
		return getMapViewTrackingUtilities().getMapLocation();
	}

	public float getMapRotate() {
		return getMapView().getRotate();
	}

	public float getMapRotateTarget() {
		OsmandMapTileView mapView = getMapView();
		if (mapView.isAnimatingMapRotation()) {
			float targetRotate = mapView.getAnimatedDraggingThread().getTargetRotate();
			if (targetRotate != TARGET_NO_ROTATION) {
				return targetRotate;
			}
		}
		return mapView.getRotate();
	}

	public float getMapElevationAngle() {
		return getMapView().getElevationAngle();
	}

	// Duplicate methods to OsmAndApplication
	public TargetPoint getPointToNavigate() {
		return app.getTargetPointsHelper().getPointToNavigate();
	}

	public RoutingHelper getRoutingHelper() {
		return app.getRoutingHelper();
	}

	@Override
	protected void onPause() {
		super.onPause();
		settings.LAST_MAP_ACTIVITY_PAUSED_TIME.set(System.currentTimeMillis());
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode()) {
			pendingPause = true;
		} else {
			onPauseActivity();
		}
		extendedMapActivity.onPause(this);
	}

	private void onPauseActivity() {
		if (app.getCarNavigationSession() == null) {
			getMapView().getAnimatedDraggingThread().blockAnimations();
		}

		settings.MAP_SCREEN_ORIENTATION.removeListener(mapScreenOrientationSettingListener);
		settings.USE_SYSTEM_SCREEN_TIMEOUT.removeListener(useSystemScreenTimeoutListener);
		if (!app.getRoutingHelper().isRouteWasFinished()) {
			DestinationReachedFragment.resetShownState();
		}
		if (trackDetailsMenu.isVisible()) {
			trackDetailsMenu.dismiss(false);
		}
		pendingPause = false;
		OsmandMapTileView mapView = getMapView();
		mapView.setOnDrawMapListener(null);
		cancelSplashScreenTimer();
		app.getMapMarkersHelper().removeListener(this);
		app.getRoutingHelper().removeListener(this);
		app.getDownloadThread().resetUiActivity(this);

		if (mapViewWithLayers != null) {
			mapViewWithLayers.onPause();
		}
		app.getLocationProvider().pauseAllUpdates();
		app.getDaynightHelper().stopSensorIfNeeded();
		settings.APPLICATION_MODE.removeListener(applicationModeListener);

		LatLon mapLocation = new LatLon(mapView.getLatitude(), mapView.getLongitude());
		settings.setLastKnownMapLocation(mapLocation);
		AnimateDraggingMapThread animatedThread = mapView.getAnimatedDraggingThread();
		if (animatedThread.isAnimating() && animatedThread.getTargetIntZoom() != 0 && !getMapViewTrackingUtilities().isMapLinkedToLocation()) {
			settings.setMapLocationToShow(animatedThread.getTargetLatitude(), animatedThread.getTargetLongitude(),
					animatedThread.getTargetIntZoom());
		}

		MapRendererView mapRenderer = mapView.getMapRenderer();
		if (mapRenderer != null)
			settings.setLastKnownMapHeight(mapRenderer.getMapTargetHeightInMeters());
		settings.setLastKnownMapZoom(mapView.getZoom());
		settings.setLastKnownMapZoomFloatPart(mapView.getZoomFloatPart());
		settings.setLastKnownMapRotation(mapView.getRotate());
		settings.setLastKnownMapElevation(mapView.getElevationAngle());
		settings.MAP_ACTIVITY_ENABLED.set(false);
		app.getResourceManager().interruptRendering();
		PluginsHelper.onMapActivityPause(this);
	}

	public void updateApplicationModeSettings() {
		changeKeyguardFlags();
		updateMapSettings();
		app.getPoiFilters().loadSelectedPoiFilters();
		app.getSearchUICore().refreshCustomPoiFilters();
		getMapViewTrackingUtilities().appModeChanged();
		keyEventHelper.updateGlobalCommands();

		OsmandMapTileView mapView = getMapView();
		MapLayers mapLayers = getMapLayers();
		if (mapLayers.getMapInfoLayer() != null) {
			mapLayers.getMapInfoLayer().recreateAllControls(this);
		}
		if (mapLayers.getMapQuickActionLayer() != null) {
			mapLayers.getMapQuickActionLayer().refreshLayer();
		}
		MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
		if (mapControlsLayer != null) {
			mapControlsLayer.refreshButtons();
			if (!mapControlsLayer.isMapControlsVisible() && !settings.MAP_EMPTY_STATE_ALLOWED.get()) {
				showMapControls();
			}
		}

		mapLayers.updateLayers(this);
		mapActions.updateDrawerMenu();
		updateNavigationBarColor();
		//mapView.setComplexZoom(mapView.getZoom(), mapView.getSettingsMapDensity());
		mapView.setMapDensity(mapView.getSettingsMapDensity());
		app.getDaynightHelper().startSensorIfNeeded(change -> app.runInUIThread(() -> getMapView().refreshMap(true)));
		getMapView().refreshMap(true);
		applyScreenOrientation();
		app.getAppCustomization().updateMapMargins(this);
		dashboardOnMap.onAppModeChanged();
	}

	public void updateNavigationBarColor() {
		if (getMyApplication().getDaynightHelper().isNightModeForMapControls() || getMyApplication().getDaynightHelper().isNightMode()) {
			getWindow().setNavigationBarColor(ContextCompat.getColor(app, R.color.navigation_bar_bg_dark));
		} else {
			getWindow().setNavigationBarColor(ContextCompat.getColor(app, R.color.navigation_bar_bg_light));
		}
	}

	public void updateMapSettings() {
		if (!app.isApplicationInitializing()) {
			UpdateVectorRendererAsyncTask task = new UpdateVectorRendererAsyncTask(app, changed -> {
				if (changed) {
					PluginsHelper.registerRenderingPreferences(app);
					ConfigureMapFragment cm = ConfigureMapFragment.getVisibleInstance(this);
					if (cm != null) {
						cm.onRefreshItem(MAP_STYLE_ID);
					}
				}
				return true;
			});
			task.executeOnExecutor(singleThreadExecutor);
		}
	}

	public MapScrollHelper getMapScrollHelper() {
		return mapScrollHelper;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyEventHelper != null && keyEventHelper.onKeyDown(keyCode, event)) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyEventHelper != null && keyEventHelper.onKeyUp(keyCode, event)) {
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyEventHelper != null && keyEventHelper.onKeyLongPress(keyCode, event)) {
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		if (keyEventHelper != null && keyEventHelper.onKeyMultiple(keyCode, repeatCount, event)) {
			return true;
		}
		return super.onKeyMultiple(keyCode, repeatCount, event);
	}

	public void showMapControls() {
		MapLayers mapLayers = getMapLayers();
		if (!getDashboard().isVisible() && mapLayers.getMapControlsLayer() != null) {
			mapLayers.getMapControlsLayer().showMapControlsIfHidden();
		}
	}

	public boolean shouldHideTopControls() {
		boolean hideTopControls = !mapContextMenu.shouldShowTopControls();

		TrackMenuFragment fragment = fragmentsHelper.getTrackMenuFragment();
		if (fragment != null) {
			hideTopControls = hideTopControls || !fragment.shouldShowTopControls();
		}

		return hideTopControls;
	}

	@NonNull
	public OsmandMapTileView getMapView() {
		return app.getOsmandMap().getMapView();
	}

	public MapViewTrackingUtilities getMapViewTrackingUtilities() {
		return app.getMapViewTrackingUtilities();
	}

	public MapDisplayPositionManager getMapPositionManager() {
		return app.getMapViewTrackingUtilities().getMapDisplayPositionManager();
	}

	public MapActivityActions getMapActions() {
		return mapActions;
	}

	public MapLayers getMapLayers() {
		return app.getOsmandMap().getMapLayers();
	}

	@NonNull
	public WidgetsVisibilityHelper getWidgetsVisibilityHelper() {
		return mapWidgetsVisibilityHelper;
	}

	public static void launchMapActivityMoveToTop(@NonNull Context activity) {
		launchMapActivityMoveToTop(activity, null, null, null);
	}

	public static void launchMapActivityMoveToTop(@NonNull Context activity,
	                                              @Nullable Bundle prevIntentParams,
	                                              @Nullable Uri intentData,
	                                              @Nullable Bundle intentParams) {
		if (activity instanceof MapActivity) {
			if (((MapActivity) activity).getDashboard().isVisible()) {
				((MapActivity) activity).getDashboard().hideDashboard();
			}
			((MapActivity) activity).readLocationToShow();
		} else {
			int additionalFlags = 0;
			if (activity instanceof Activity) {
				Intent intent = ((Activity) activity).getIntent();
				if (intent != null) {
					prevActivityIntent = new Intent(intent);
					if (prevIntentParams != null) {
						prevActivityIntent.putExtra(INTENT_PARAMS, prevIntentParams);
						prevActivityIntent.putExtras(prevIntentParams);
					}
					prevActivityIntent.putExtra(INTENT_KEY_PARENT_MAP_ACTIVITY, true);
				} else {
					prevActivityIntent = null;
				}
			} else {
				prevActivityIntent = null;
				additionalFlags = Intent.FLAG_ACTIVITY_NEW_TASK;
			}

			Intent newIntent = new Intent(activity, ((OsmandApplication) activity.getApplicationContext())
					.getAppCustomization().getMapActivity());
			newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP | additionalFlags);
			if (intentData != null) {
				newIntent.setAction(Intent.ACTION_VIEW);
				newIntent.setData(intentData);
			}
			if (intentParams != null) {
				newIntent.putExtra(INTENT_PARAMS, intentParams);
				newIntent.putExtras(intentParams);
			}
			AndroidUtils.startActivityIfSafe(activity, newIntent);
		}
	}

	public static void clearPrevActivityIntent() {
		prevActivityIntent = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		for (ActivityResultListener listener : activityResultListeners) {
			if (listener.processResult(requestCode, resultCode, data)) {
				removeActivityResultListener(listener);
				return;
			}
		}
		PluginsHelper.onMapActivityResult(requestCode, resultCode, data);
		extendedMapActivity.onActivityResult(this, requestCode, resultCode, data);
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void refreshMap() {
		getMapView().refreshMap();
	}

	public void updateLayers() {
		getMapLayers().updateLayers(this);
	}

	public void refreshMapComplete() {
		getMyApplication().getResourceManager().getRenderer().clearCache();
		updateMapSettings();
		getMapView().refreshMap(true);
	}

	public View getLayout() {
		return getWindow().getDecorView().findViewById(android.R.id.content);
	}

	public void setMargins(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
		View layout = getLayout();
		if (layout != null) {
			ViewGroup.LayoutParams params = layout.getLayoutParams();
			if (params instanceof ViewGroup.MarginLayoutParams) {
				((ViewGroup.MarginLayoutParams) params).setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
			}
		}
	}

	@NonNull
	public DashboardOnMap getDashboard() {
		return dashboardOnMap;
	}

	@NonNull
	public MapContextMenu getContextMenu() {
		return mapContextMenu;
	}

	@NonNull
	public MapRouteInfoMenu getMapRouteInfoMenu() {
		return mapRouteInfoMenu;
	}

	@NonNull
	public TrackDetailsMenu getTrackDetailsMenu() {
		return trackDetailsMenu;
	}

	@NonNull
	public MapFragmentsHelper getFragmentsHelper() {
		return fragmentsHelper;
	}

	@NonNull
	public RestoreNavigationHelper getRestoreNavigationHelper() {
		return restoreNavigationHelper;
	}

	public void hideContextAndRouteInfoMenues() {
		mapContextMenu.hideMenus();
		mapRouteInfoMenu.hide();
	}

	public void openDrawer() {
		if (isDrawerAvailable()) {
			mapActions.updateDrawerMenu();
			boolean animate = !settings.DO_NOT_USE_ANIMATIONS.get();
			drawerLayout.openDrawer(GravityCompat.START, animate);
		}
	}

	public void disableDrawer() {
		drawerDisabled = true;
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			closeDrawer();
		}
		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
	}

	public void enableDrawer() {
		if (isDrawerAvailable()) {
			drawerDisabled = false;
			drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
		}
	}

	public boolean isDrawerDisabled() {
		return drawerDisabled;
	}

	public boolean isDrawerAvailable() {
		return app.getAppCustomization().isFeatureEnabled(FRAGMENT_DRAWER_ID);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
					int drawerWidth = AndroidUtils.dpToPx(this, 280);
					int screenWidth = AndroidUtils.getScreenWidth(this);
					boolean isLayoutRtl = AndroidUtils.isLayoutRtl(app);
					if ((!isLayoutRtl && event.getRawX() > drawerWidth)
							|| (isLayoutRtl && event.getRawX() <= screenWidth - drawerWidth)) {
						closeDrawer();
					}
				}
			}
		}
		return super.dispatchTouchEvent(event);
	}

	public void closeDrawer() {
		boolean animate = !settings.DO_NOT_USE_ANIMATIONS.get();
		drawerLayout.closeDrawer(GravityCompat.START, animate);
	}

	public void toggleDrawer() {
		if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
			closeDrawer();
		} else {
			openDrawer();
		}
	}

	// DownloadEvents
	@Override
	public void onUpdatedIndexesList() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof DownloadEvents && fragment.isAdded()) {
				((DownloadEvents) fragment).onUpdatedIndexesList();
			}
		}
		if (dashboardOnMap.isVisible()) {
			dashboardOnMap.onNewDownloadIndexes();
		}
		refreshMap();
	}

	@Override
	public void downloadInProgress() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof DownloadEvents && fragment.isAdded()) {
				((DownloadEvents) fragment).downloadInProgress();
			}
		}
		if (dashboardOnMap.isVisible()) {
			dashboardOnMap.onDownloadInProgress();
		}
	}

	@Override
	public void downloadHasFinished() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof DownloadEvents && fragment.isAdded()) {
				((DownloadEvents) fragment).downloadHasFinished();
			}
		}
		if (dashboardOnMap.isVisible()) {
			dashboardOnMap.onDownloadHasFinished();
		}
		refreshMapComplete();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		permissionsResultCallback.onRequestPermissionsResult(requestCode, permissions, grantResults);
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	public void onMapMarkerChanged(MapMarker mapMarker) {
		refreshMap();
	}

	@Override
	public void onMapMarkersChanged() {
		refreshMap();
	}

	@Override
	public void onAMapPointUpdated(AidlMapPointWrapper point, String layerId) {
		if (canUpdateAMapPointMenu(point, layerId)) {
			app.runInUIThread(() -> {
				LatLon latLon = point.getLocation();
				PointDescription pointDescription = new PointDescription(PointDescription.POINT_TYPE_MARKER, point.getFullName());
				mapContextMenu.update(latLon, pointDescription, point);
				mapContextMenu.centerMarkerLocation();
			});
		}
	}

	private boolean canUpdateAMapPointMenu(AidlMapPointWrapper point, String layerId) {
		Object object = mapContextMenu.getObject();
		if (!mapContextMenu.isVisible() || !(object instanceof AidlMapPointWrapper)) {
			return false;
		}
		AidlMapPointWrapper oldPoint = (AidlMapPointWrapper) object;
		return oldPoint.getLayerId().equals(layerId) && oldPoint.getId().equals(point.getId());
	}

	public void changeKeyguardFlags() {
		boolean enabled = settings.TURN_SCREEN_ON_TIME_INT.get() >= 0;
		boolean keepScreenOn = !settings.USE_SYSTEM_SCREEN_TIMEOUT.get();
		changeKeyguardFlags(enabled, keepScreenOn);
	}

	private void changeKeyguardFlags(boolean enable, boolean forceKeepScreenOn) {
		if (enable) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
					WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		} else {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		}
		setKeepScreenOn(forceKeepScreenOn);
	}

	@Override
	public void lock() {
		changeKeyguardFlags(false, false);
	}

	@Override
	public void unlock() {
		changeKeyguardFlags(true, true);
	}

	@Override
	public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
		return fragmentsHelper.onPreferenceStartFragment(caller, pref);
	}

	private class ScreenOffReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			PluginsHelper.onMapActivityScreenOff(MapActivity.this);
		}
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		if (mapRouteInfoMenu.isSelectFromMapTouch()) {
			return;
		}
		refreshMap();
		RoutingHelper rh = app.getRoutingHelper();
		if (newRoute && rh.isRoutePlanningMode() && !getMapView().isCarView()) {
			app.runInUIThread(this::fitCurrentRouteToMap, 300);
		}
		if (app.getSettings().simulateNavigation) {
			OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
			if (newRoute && rh.isFollowingMode() && !sim.isRouteAnimating()) {
				sim.startStopRouteAnimation(this);
			}
		}
	}

	private void fitCurrentRouteToMap() {
		boolean portrait = true;
		int leftBottomPaddingPx = 0;
		WeakReference<?> fragmentRef = mapRouteInfoMenu.findMenuFragment();
		if (fragmentRef == null) {
			fragmentRef = mapRouteInfoMenu.findFollowTrackFragment();
		}
		View mapBottomView = findViewById(R.id.map_bottom_widgets_panel);
		int mapBottomViewHeight = mapBottomView.getHeight();
		if (fragmentRef != null) {
			ContextMenuFragment f = (ContextMenuFragment) fragmentRef.get();
			portrait = f.isPortrait();
			if (!portrait) {
				leftBottomPaddingPx = f.getWidth();
			} else {
				leftBottomPaddingPx = Math.max(0, f.getHeight() - mapBottomViewHeight);
			}
		}
		app.getOsmandMap().fitCurrentRouteToMap(portrait, leftBottomPaddingPx);
	}

	@Override
	public void routeWasCancelled() {
		changeKeyguardFlags();
	}

	@Override
	public void routeWasFinished() {
		if (!mIsDestroyed) {
			DestinationReachedFragment.show(this);
			changeKeyguardFlags();
		}
	}

	public boolean isTopToolbarActive() {
		MapInfoLayer mapInfoLayer = getMapLayers().getMapInfoLayer();
		return mapInfoLayer.hasTopToolbar();
	}

	public TopToolbarController getTopToolbarController(@NonNull TopToolbarControllerType type) {
		MapInfoLayer mapInfoLayer = getMapLayers().getMapInfoLayer();
		return mapInfoLayer.getTopToolbarController(type);
	}

	public void showTopToolbar(@NonNull TopToolbarController controller) {
		MapInfoLayer mapInfoLayer = getMapLayers().getMapInfoLayer();
		mapInfoLayer.addTopToolbarController(controller);
		updateStatusBarColor();
	}

	public void hideTopToolbar(@NonNull TopToolbarController controller) {
		MapInfoLayer mapInfoLayer = getMapLayers().getMapInfoLayer();
		mapInfoLayer.removeTopToolbarController(controller);
		updateStatusBarColor();
	}

	public void hideTopToolbar(@NonNull TopToolbarControllerType type) {
		TopToolbarController controller = getTopToolbarController(type);
		if (controller != null) {
			hideTopToolbar(controller);
		}
	}

	@Nullable
	protected List<View> getHidingViews() {
		List<View> views = new ArrayList<>();
		View mainContainer = findViewById(R.id.MapHudButtonsOverlay);
		if (mainContainer != null) {
			views.add(mainContainer);
		}
		return views;
	}

	@Override
	public List<Fragment> getActiveTalkbackFragments() {
		return fragmentsHelper.getActiveTalkbackFragments();
	}

	public void registerActivityResultListener(ActivityResultListener listener) {
		activityResultListeners.add(listener);
	}

	public void removeActivityResultListener(ActivityResultListener listener) {
		activityResultListeners.remove(listener);
	}

	@Override
	public void onOsmAndSettingsCustomized() {
		restart();
	}

	public void restart() {
		if (stopped) {
			activityRestartNeeded = true;
		} else {
			recreate();
		}
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		app.getLocaleHelper().setLanguage(this);
		app.runInUIThread(fragmentsHelper::updateFragments);
	}

	@Override
	public void onInAppPurchaseGetItems() {
		DiscountHelper.checkAndDisplay(this);
	}
}