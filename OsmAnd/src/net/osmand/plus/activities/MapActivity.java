package net.osmand.plus.activities;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SETTINGS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_CRASH_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_RATE_US_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.MAP_STYLE_ID;
import static net.osmand.plus.firstusage.FirstUsageWizardFragment.FIRST_USAGE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.PLAN_ROUTE_MODE;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.BackStackEntry;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.SecondSplashScreenFragment;
import net.osmand.StateChangedListener;
import net.osmand.aidl.AidlMapPointWrapper;
import net.osmand.aidl.OsmandAidlApi.AMapPointUpdateListener;
import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.ValueHolder;
import net.osmand.map.WorldRegion;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmAndLocationSimulation;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.base.ContextMenuFragment;
import net.osmand.plus.base.FailSafeFunctions;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.configmap.ConfigureMapFragment;
import net.osmand.plus.dashboard.DashBaseFragment;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.CrashBottomSheetDialogFragment;
import net.osmand.plus.dialogs.SendAnalyticsBottomSheetDialogFragment;
import net.osmand.plus.dialogs.WhatsNewDialogFragment;
import net.osmand.plus.dialogs.XMasDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.firstusage.FirstUsageWelcomeFragment;
import net.osmand.plus.firstusage.FirstUsageWizardFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.DiscountHelper;
import net.osmand.plus.helpers.IntentHelper;
import net.osmand.plus.helpers.LockHelper;
import net.osmand.plus.helpers.LockHelper.LockUIAdapter;
import net.osmand.plus.helpers.RateUsHelper;
import net.osmand.plus.helpers.ScrollHelper;
import net.osmand.plus.helpers.ScrollHelper.OnScrollEventListener;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.TargetPointsHelper.TargetPoint;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.importfiles.ui.ImportGpxBottomSheetDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialogFragment;
import net.osmand.plus.mapcontextmenu.other.DestinationReachedFragment;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersHelper.MapMarkerChangedListener;
import net.osmand.plus.mapmarkers.PlanRouteFragment;
import net.osmand.plus.measurementtool.GpxApproximationFragment;
import net.osmand.plus.measurementtool.GpxData;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.SnapTrackWarningFragment;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.accessibility.MapAccessibilityActions;
import net.osmand.plus.plugins.monitoring.TripRecordingStartingBottomSheet;
import net.osmand.plus.plugins.rastermaps.DownloadTilesFragment;
import net.osmand.plus.render.UpdateVectorRendererAsyncTask;
import net.osmand.plus.routepreparationmenu.ChooseRouteFragment;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.routing.RouteCalculationProgressListener;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper.TransportRouteCalculationProgressCallback;
import net.osmand.plus.search.QuickSearchDialogFragment;
import net.osmand.plus.search.QuickSearchDialogFragment.QuickSearchTab;
import net.osmand.plus.search.QuickSearchDialogFragment.QuickSearchType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmAndAppCustomization.OsmAndAppCustomizationListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.datastorage.SharedStorageWarningFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.settings.fragments.ConfigureProfileFragment;
import net.osmand.plus.track.GpxSelectionParams;
import net.osmand.plus.track.fragments.GpsFilterFragment;
import net.osmand.plus.track.fragments.TrackAppearanceFragment;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.AddGpxPointBottomSheetHelper.NewGpxPoint;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapLayers;
import net.osmand.plus.views.OsmAndMapLayersView;
import net.osmand.plus.views.OsmAndMapSurfaceView;
import net.osmand.plus.views.OsmandMap.OsmandMapListener;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.OsmandMapTileView.OnDrawMapListener;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.router.GeneralRouter;
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
		OnRequestPermissionsResultCallback, IRouteInformationListener, AMapPointUpdateListener,
		MapMarkerChangedListener, OnDrawMapListener,
		OsmAndAppCustomizationListener, LockUIAdapter, OnPreferenceStartFragmentCallback,
		OnScrollEventListener, OsmandMapListener {

	public static final String INTENT_KEY_PARENT_MAP_ACTIVITY = "intent_parent_map_activity_key";
	public static final String INTENT_PARAMS = "intent_prarams";

	private static final int SHOW_POSITION_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 1;
	private static final int ZOOM_LABEL_DISPLAY = 16;
	private static final int MIN_ZOOM_LABEL_DISPLAY = 12;
	private static final int SECOND_SPLASH_TIME_OUT = 8000;

	private static final int SMALL_SCROLLING_UNIT = 1;
	private static final int BIG_SCROLLING_UNIT = 200;

	private static final Log LOG = PlatformUtil.getLog(MapActivity.class);

	private static final MapContextMenu mapContextMenu = new MapContextMenu();
	private static final MapRouteInfoMenu mapRouteInfoMenu = new MapRouteInfoMenu();
	private static final TrackDetailsMenu trackDetailsMenu = new TrackDetailsMenu();
	private static Intent prevActivityIntent = null;

	private final List<ActivityResultListener> activityResultListeners = new ArrayList<>();

	private BroadcastReceiver screenOffReceiver;

	private AtlasMapRendererView atlasMapRendererView;

	private MapActivityActions mapActions;
	private WidgetsVisibilityHelper mapWidgetsVisibilityHelper;

	private ExtendedMapActivity extendedMapActivity;

	// App variables
	private OsmandApplication app;
	private OsmandSettings settings;

	private LockHelper lockHelper;
	private ImportHelper importHelper;
	private IntentHelper intentHelper;
	private ScrollHelper mapScrollHelper;

	private boolean landscapeLayout;

	private final List<DialogProvider> dialogProviders = new ArrayList<>(2);
	private StateChangedListener<ApplicationMode> applicationModeListener;
	private boolean intentLocation = false;

	private final DashboardOnMap dashboardOnMap = new DashboardOnMap(this);
	private AppInitializeListener initListener;
	private DrawerLayout drawerLayout;
	private boolean drawerDisabled;

	private static boolean permissionDone;
	private boolean permissionAsked;
	private boolean permissionGranted;

	private boolean mIsDestroyed = false;
	private boolean pendingPause = false;
	private Timer splashScreenTimer;
	private boolean activityRestartNeeded = false;
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
	private MapActivityKeyListener mapActivityKeyListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setRequestedOrientation(AndroidUiHelper.getScreenOrientation(this));
		long tm = System.currentTimeMillis();
		app = getMyApplication();
		settings = app.getSettings();
		lockHelper = app.getLockHelper();
		mapScrollHelper = new ScrollHelper(app);
		app.applyTheme(this);
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

		boolean portraitMode = AndroidUiHelper.isOrientationPortrait(this);
		boolean largeDevice = AndroidUiHelper.isXLargeDevice(this);
		landscapeLayout = !portraitMode && !largeDevice;

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

		intentHelper = new IntentHelper(this, getMyApplication());
		intentHelper.parseLaunchIntents();

		OsmandMapTileView mapView = getMapView();

		mapView.setTrackBallDelegate(e -> {
			showAndHideMapPosition();
			return MapActivity.this.onTrackballEvent(e);
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
		addDialogProvider(mapActions);
		OsmandPlugin.onMapActivityCreate(this);
		importHelper = new ImportHelper(this, getMyApplication());
		if (System.currentTimeMillis() - tm > 50) {
			System.err.println("OnCreate for MapActivity took " + (System.currentTimeMillis() - tm) + " ms");
		}
		mapView.refreshMap(true);
		app.getOsmandMap().addListener(this);

		checkAppInitialization();

		mapActions.updateDrawerMenu();
		drawerLayout = findViewById(R.id.drawer_layout);

		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		screenOffReceiver = new ScreenOffReceiver();
		registerReceiver(screenOffReceiver, filter);

		app.getAidlApi().onCreateMapActivity(this);

		lockHelper.setLockUIAdapter(this);
		mapActivityKeyListener = new MapActivityKeyListener(this);
		mIsDestroyed = false;

		extendedMapActivity.onCreate(this, savedInstanceState);
	}

	private void setMapInitialLatLon(@NonNull OsmandMapTileView mapView, @Nullable Location location) {
		if (location != null) {
			mapView.setLatLon(location.getLatitude(), location.getLongitude());
			mapView.setIntZoom(14);
		}
	}

	public void exitFromFullScreen(View view) {
		if (!OsmandPlugin.isDevelopment() || settings.TRANSPARENT_STATUS_BAR.get()) {
			AndroidUtils.exitFromFullScreen(this, view);
		}
	}

	public void enterToFullScreen() {
		if (!OsmandPlugin.isDevelopment() || settings.TRANSPARENT_STATUS_BAR.get()) {
			AndroidUtils.enterToFullScreen(this, getLayout());
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		if (removeFragment(PlanRouteFragment.TAG)) {
			app.getMapMarkersHelper().getPlanRouteContext().setFragmentVisible(true);
		}
		removeFragment(ImportGpxBottomSheetDialogFragment.TAG);
		removeFragment(AdditionalActionsBottomSheetDialogFragment.TAG);
		extendedMapActivity.onSaveInstanceState(this, outState);
		super.onSaveInstanceState(outState);
	}

	@MainThread
	private boolean removeFragment(String tag) {
		FragmentManager fm = getSupportFragmentManager();
		Fragment fragment = fm.findFragmentByTag(tag);
		if (fragment != null) {
			fm.beginTransaction()
					.remove(fragment)
					.commitNowAllowingStateLoss();
			return true;
		}
		return false;
	}

	private void checkAppInitialization() {
		if (app.isApplicationInitializing()) {
			findViewById(R.id.init_progress).setVisibility(View.VISIBLE);
			initListener = new AppInitializeListener() {
				boolean openGlSetup = false;

				@Override
				public void onStart(AppInitializer init) {

				}

				@Override
				public void onProgress(AppInitializer init, InitEvents event) {
					String tn = init.getCurrentInitTaskName();
					if (tn != null) {
						((TextView) findViewById(R.id.ProgressMessage)).setText(tn);
					}
					boolean openGlInitialized = event == InitEvents.NATIVE_OPEN_GL_INITIALIZED && NativeCoreContext.isInit();
					if ((openGlInitialized || event == InitEvents.NATIVE_INITIALIZED) && !openGlSetup) {
						app.getOsmandMap().setupOpenGLView(false);
						openGlSetup = true;
					}
					if (event == InitEvents.MAPS_INITIALIZED) {
						// TODO investigate if this false cause any issues!
						getMapView().refreshMap(false);
						if (dashboardOnMap != null) {
							dashboardOnMap.updateLocation(true, true, false);
						}
						app.getTargetPointsHelper().lookupAddressAll();
					}
					if (event == InitEvents.FAVORITES_INITIALIZED) {
						refreshMap();
					}
					if (event == InitEvents.ROUTING_CONFIG_INITIALIZED) {
						checkRestoreRoutingMode();
					}
				}

				@Override
				public void onFinish(AppInitializer init) {
					if (!openGlSetup) {
						app.getOsmandMap().setupOpenGLView(false);
					}
					getMapView().refreshMap(false);
					if (dashboardOnMap != null) {
						dashboardOnMap.updateLocation(true, true, false);
					}
					findViewById(R.id.init_progress).setVisibility(View.GONE);
					findViewById(R.id.drawer_layout).invalidate();
				}
			};
			getMyApplication().checkApplicationIsBeingInitialized(initListener);
		} else {
			app.getOsmandMap().setupOpenGLView(true);
			checkRestoreRoutingMode();
		}
	}

	private void checkRestoreRoutingMode() {
		// This situation could be when navigation suddenly crashed and after restarting
		// it tries to continue the last route
		if (settings.FOLLOW_THE_ROUTE.get()
				&& !app.getRoutingHelper().isRouteCalculated()
				&& !app.getRoutingHelper().isRouteBeingCalculated()) {
			FailSafeFunctions.restoreRoutingMode(MapActivity.this);
		}
	}

	private void setupOpenGLView(boolean init) {
		OsmandMapTileView mapView = getMapView();
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		View androidAutoPlaceholder = findViewById(R.id.AndroidAutoPlaceholder);
		boolean useAndroidAuto = carNavigationSession != null && carNavigationSession.hasStarted()
				&& InAppPurchaseHelper.isAndroidAutoAvailable(app);
		if (settings.USE_OPENGL_RENDER.get() && NativeCoreContext.isInit()) {
			ViewStub stub = findViewById(R.id.atlasMapRendererViewStub);
			if (atlasMapRendererView == null) {
				atlasMapRendererView = (AtlasMapRendererView) stub.inflate();
				atlasMapRendererView.setAzimuth(0);
				float elevationAngle = mapView.normalizeElevationAngle(app.getSettings().getLastKnownMapElevation());
				atlasMapRendererView.setElevationAngle(elevationAngle);
				NativeCoreContext.getMapRendererContext().setMapRendererView(atlasMapRendererView);
			}
			mapView.setMapRenderer(atlasMapRendererView);
			OsmAndMapLayersView ml = findViewById(R.id.MapLayersView);
			if (useAndroidAuto) {
				ml.setVisibility(View.GONE);
				ml.setMapView(null);
				androidAutoPlaceholder.setVisibility(View.VISIBLE);
			} else {
				ml.setVisibility(View.VISIBLE);
				ml.setMapView(mapView);
				androidAutoPlaceholder.setVisibility(View.GONE);
			}
			getMapViewTrackingUtilities().setMapView(mapView);
			OsmAndMapSurfaceView surf = findViewById(R.id.MapView);
			surf.setVisibility(View.GONE);
		} else {
			OsmAndMapSurfaceView surf = findViewById(R.id.MapView);
			if (useAndroidAuto) {
				surf.setVisibility(View.GONE);
				surf.setMapView(null);
				androidAutoPlaceholder.setVisibility(View.VISIBLE);
			} else {
				surf.setVisibility(View.VISIBLE);
				surf.setMapView(mapView);
				androidAutoPlaceholder.setVisibility(View.GONE);
			}
		}
	}

	public void showHorizontalProgressBar() {
		final ProgressBar pb = findViewById(R.id.map_horizontal_progress);
		setupProgressBar(pb, true);
		pb.setVisibility(View.VISIBLE);
	}

	public void hideHorizontalProgressBar() {
		final ProgressBar pb = findViewById(R.id.map_horizontal_progress);
		pb.setVisibility(View.GONE);
	}

	private void createProgressBarForRouting() {
		final ProgressBar pb = findViewById(R.id.map_horizontal_progress);
		final RouteCalculationProgressListener progressCallback = new RouteCalculationProgressListener() {

			@Override
			public void onCalculationStart() {
				setupRouteCalculationProgressBar(pb);
				mapRouteInfoMenu.routeCalculationStarted();
				RoutingHelper routingHelper = getRoutingHelper();
				if (routingHelper.isPublicTransportMode() || !routingHelper.isOsmandRouting()) {
					dashboardOnMap.updateRouteCalculationProgress(0);
				}
			}

			@Override
			public void onUpdateCalculationProgress(int progress) {
				mapRouteInfoMenu.updateRouteCalculationProgress(progress);
				dashboardOnMap.updateRouteCalculationProgress(progress);
				if (findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE) {
					if (mapRouteInfoMenu.isVisible() || dashboardOnMap.isVisible()) {
						pb.setVisibility(View.GONE);
						return;
					}
					if (pb.getVisibility() == View.GONE) {
						pb.setVisibility(View.VISIBLE);
					}
					pb.setProgress(progress);
					pb.invalidate();
					pb.requestLayout();
				}
			}

			@Override
			public void onRequestPrivateAccessRouting() {
				ApplicationMode routingProfile = getRoutingHelper().getAppMode();
				if (AndroidUtils.isActivityNotDestroyed(MapActivity.this)
						&& !settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.getModeValue(routingProfile)) {
					final List<ApplicationMode> modes = ApplicationMode.values(app);
					for (ApplicationMode mode : modes) {
						if (!getAllowPrivatePreference(mode).getModeValue(mode)) {
							settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.setModeValue(mode, true);
						}
					}
					OsmandPreference<Boolean> allowPrivate = getAllowPrivatePreference(routingProfile);
					if (!allowPrivate.getModeValue(routingProfile)) {
						AlertDialog.Builder dlg = new AlertDialog.Builder(MapActivity.this);
						dlg.setMessage(R.string.private_access_routing_req);
						dlg.setPositiveButton(R.string.shared_string_yes, (dialog, which) -> {
							for (ApplicationMode mode : modes) {
								OsmandPreference<Boolean> preference = getAllowPrivatePreference(mode);
								if (!preference.getModeValue(mode)) {
									preference.setModeValue(mode, true);
								}
							}
							getRoutingHelper().onSettingsChanged(null, true);
						});
						dlg.setNegativeButton(R.string.shared_string_no, null);
						dlg.show();
					}
				}
			}

			private OsmandPreference<Boolean> getAllowPrivatePreference(@NonNull ApplicationMode appMode) {
				String derivedProfile = appMode.getDerivedProfile();
				CommonPreference<Boolean> allowPrivate =
						settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE, false);
				CommonPreference<Boolean> allowPrivateForTruck =
						settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK, false);
				return Algorithms.objectEquals(derivedProfile, "truck") ? allowPrivateForTruck : allowPrivate;
			}

			@Override
			public void onUpdateMissingMaps(@Nullable List<WorldRegion> missingMaps, boolean onlineSearch) {
				mapRouteInfoMenu.updateSuggestedMissingMaps(missingMaps, onlineSearch);
			}

			@Override
			public void onCalculationFinish() {
				mapRouteInfoMenu.routeCalculationFinished();
				dashboardOnMap.routeCalculationFinished();
				pb.setVisibility(View.GONE);
			}
		};

		app.getRoutingHelper().addCalculationProgressListener(progressCallback);

		app.getTransportRoutingHelper().setProgressBar(new TransportRouteCalculationProgressCallback() {
			@Override
			public void start() {
				progressCallback.onCalculationStart();
			}

			@Override
			public void updateProgress(int progress) {
				progressCallback.onUpdateCalculationProgress(progress);
			}

			@Override
			public void finish() {
				progressCallback.onCalculationFinish();
			}
		});
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
				: ContextCompat.getColor(this, R.color.wikivoyage_active_light);

		pb.setProgressDrawable(AndroidUtils.createProgressDrawable(bgColor, progressColor));
		pb.setIndeterminate(indeterminate);
		pb.getIndeterminateDrawable().setColorFilter(progressColor, android.graphics.PorterDuff.Mode.SRC_IN);
	}

	public ImportHelper getImportHelper() {
		return importHelper;
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
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
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if ((backStackEntryCount == 0 || mapContextMenu.isVisible()) && fragment != null && fragment.isSearchHidden()) {
			showQuickSearch(ShowQuickSearchMode.CURRENT, false);
			return;
		}
		super.onBackPressed();
	}

	public boolean launchPrevActivityIntent() {
		if (prevActivityIntent != null) {
			prevActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			LatLon loc = getMapLocation();
			prevActivityIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
			prevActivityIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
			if (getMapViewTrackingUtilities().isMapLinkedToLocation()) {
				prevActivityIntent.putExtra(SearchActivity.SEARCH_NEARBY, true);
			}
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

		long tm = System.currentTimeMillis();
		FragmentManager fragmentManager = getSupportFragmentManager();

		if (app.getMapMarkersHelper().getPlanRouteContext().isFragmentVisible()) {
			PlanRouteFragment.showInstance(this);
		}

		if (app.isApplicationInitializing() || DashboardOnMap.staticVisible) {
			if (!dashboardOnMap.isVisible()) {
				if (settings.SHOW_DASHBOARD_ON_START.get()) {
					dashboardOnMap.setDashboardVisibility(true, DashboardOnMap.staticVisibleType);
				} else {
					OsmAndAppCustomization customization = app.getAppCustomization();
					if (customization.isFeatureEnabled(FRAGMENT_CRASH_ID)
							&& CrashBottomSheetDialogFragment.shouldShow(settings, this)) {
						SecondSplashScreenFragment.SHOW = false;
						CrashBottomSheetDialogFragment.showInstance(fragmentManager);
					} else if (customization.isFeatureEnabled(FRAGMENT_RATE_US_ID)
							&& RateUsHelper.shouldShowRateDialog(app)) {
						SecondSplashScreenFragment.SHOW = false;
						RateUsHelper.showRateDialog(this);
					}
				}
			} else {
				dashboardOnMap.updateDashboard();
			}
		}
		dashboardOnMap.updateLocation(true, true, false);

		boolean showStorageMigrationScreen = false;
		if (getFragment(WhatsNewDialogFragment.TAG) == null || WhatsNewDialogFragment.wasNotShown()) {
			if (getFragment(SharedStorageWarningFragment.TAG) == null && SharedStorageWarningFragment.dialogShowRequired(app)) {
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

		// for voice navigation
		ApplicationMode routingAppMode = getRoutingHelper().getAppMode();
		if (routingAppMode != null && settings.AUDIO_MANAGER_STREAM.getModeValue(routingAppMode) != null) {
			setVolumeControlStream(settings.AUDIO_MANAGER_STREAM.getModeValue(routingAppMode));
		} else {
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
		}

		applicationModeListener = prevAppMode -> app.runInUIThread(() -> {
			if (settings.APPLICATION_MODE.get() != prevAppMode) {
				MapActivity.this.updateApplicationModeSettings();
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
		if (settings.isLastKnownMapLocation() && !intentLocation) {
			LatLon l = settings.getLastKnownMapLocation();
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setIntZoom(settings.getLastKnownMapZoom());
		} else {
			intentLocation = false;
		}

		settings.MAP_ACTIVITY_ENABLED.set(true);
		showAndHideMapPosition();

		readLocationToShow();

		OsmandPlugin.checkInstalledMarketPlugins(app, this);
		OsmandPlugin.onMapActivityResume(this);

		intentHelper.parseContentIntent();
		mapView.refreshMap(true);
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnResume();
		}

		app.getLauncherShortcutsHelper().updateLauncherShortcuts();
		app.getDownloadThread().setUiActivity(this);

		boolean routeWasFinished = routingHelper.isRouteWasFinished();
		if (routeWasFinished && !DestinationReachedFragment.wasShown()) {
			DestinationReachedFragment.show(this);
		}

		routingHelper.addListener(this);
		app.getMapMarkersHelper().addListener(this);

		if (System.currentTimeMillis() - tm > 50) {
			System.err.println("OnCreate for MapActivity took " + (System.currentTimeMillis() - tm) + " ms");
		}

		boolean showOsmAndWelcomeScreen = true;
		final Intent intent = getIntent();
		if (intent != null && intent.hasExtra(FirstUsageWelcomeFragment.SHOW_OSMAND_WELCOME_SCREEN)) {
			showOsmAndWelcomeScreen = intent.getBooleanExtra(FirstUsageWelcomeFragment.SHOW_OSMAND_WELCOME_SCREEN, true);
		}
		boolean showWelcomeScreen = ((app.getAppInitializer().isFirstTime() && Version.isDeveloperVersion(app)) || !app.getResourceManager().isAnyMapInstalled())
				&& FirstUsageWelcomeFragment.SHOW && settings.SHOW_OSMAND_WELCOME_SCREEN.get()
				&& showOsmAndWelcomeScreen && !showStorageMigrationScreen;

		if (!showWelcomeScreen && !permissionDone && !app.getAppInitializer().isFirstTime()) {
			if (!permissionAsked) {
				if (app.isExternalStorageDirectoryReadOnly() && !showStorageMigrationScreen
						&& fragmentManager.findFragmentByTag(SharedStorageWarningFragment.TAG) == null
						&& fragmentManager.findFragmentByTag(SettingsScreenType.DATA_STORAGE.fragmentName) == null) {
					if (DownloadActivity.hasPermissionToWriteExternalStorage(this)) {
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
				if (permissionGranted) {
					RestartActivity.doRestart(this, getString(R.string.storage_permission_restart_is_required));
				} else if (fragmentManager.findFragmentByTag(SettingsScreenType.DATA_STORAGE.fragmentName) == null) {
					Bundle args = new Bundle();
					args.putBoolean(FIRST_USAGE, true);
					BaseSettingsFragment.showInstance(this, SettingsScreenType.DATA_STORAGE, null, args, null);
				}
				permissionAsked = false;
				permissionGranted = false;
				permissionDone = true;
			}
		}
		enableDrawer();

		if (showWelcomeScreen && FirstUsageWelcomeFragment.showInstance(fragmentManager)) {
			SecondSplashScreenFragment.SHOW = false;
		} else if (SendAnalyticsBottomSheetDialogFragment.shouldShowDialog(app)) {
			SendAnalyticsBottomSheetDialogFragment.showInstance(app, fragmentManager, null);
		}
		FirstUsageWelcomeFragment.SHOW = false;
		if (isFirstScreenShowing() && (!settings.SHOW_OSMAND_WELCOME_SCREEN.get() || !showOsmAndWelcomeScreen)) {
			FirstUsageWelcomeFragment welcomeFragment = getFirstUsageWelcomeFragment();
			if (welcomeFragment != null) {
				welcomeFragment.closeWelcomeFragment();
			}
			FirstUsageWizardFragment wizardFragment = getFirstUsageWizardFragment();
			if (wizardFragment != null) {
				wizardFragment.closeWizard();
			}
		}
		if (SecondSplashScreenFragment.SHOW && SecondSplashScreenFragment.showInstance(fragmentManager)) {
			SecondSplashScreenFragment.SHOW = false;
			SecondSplashScreenFragment.VISIBLE = true;
			mapView.setOnDrawMapListener(this);
			splashScreenTimer = new Timer();
			splashScreenTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					app.runInUIThread(() -> dismissSecondSplashScreen());
				}
			}, SECOND_SPLASH_TIME_OUT);
		} else {
			if (SecondSplashScreenFragment.VISIBLE) {
				dismissSecondSplashScreen();
			}
			applyScreenOrientation();
		}

		settings.MAP_SCREEN_ORIENTATION.addListener(mapScreenOrientationSettingListener);
		settings.USE_SYSTEM_SCREEN_TIMEOUT.addListener(useSystemScreenTimeoutListener);

		extendedMapActivity.onResume(this);
	}

	@Override
	public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
		if (isTopResumedActivity) {
			OsmandPlugin.onMapActivityResumeOnTop(this);
		}
	}

	public void applyScreenOrientation() {
		if (settings.MAP_SCREEN_ORIENTATION.get() != getRequestedOrientation()) {
			setRequestedOrientation(settings.MAP_SCREEN_ORIENTATION.get());
		}
	}

	public void setKeepScreenOn(boolean keepScreenOn) {
		View mainView = findViewById(R.id.MapViewWithLayers);
		if (mainView != null) {
			mainView.setKeepScreenOn(keepScreenOn);
		}
	}

	public void updateStatusBarColor() {
		int colorId = -1;
		MapLayers mapLayers = getMapLayers();
		BaseOsmAndFragment fragmentAboveDashboard = getVisibleBaseOsmAndFragment(R.id.fragmentContainer);
		BaseSettingsFragment settingsFragmentAboveDashboard = getVisibleBaseSettingsFragment(R.id.fragmentContainer);
		BaseOsmAndFragment fragmentBelowDashboard = getVisibleBaseOsmAndFragment(R.id.routeMenuContainer,
				R.id.topFragmentContainer, R.id.bottomFragmentContainer);
		if (fragmentAboveDashboard != null) {
			colorId = fragmentAboveDashboard.getStatusBarColorId();
		} else if (settingsFragmentAboveDashboard != null) {
			colorId = settingsFragmentAboveDashboard.getStatusBarColorId();
		} else if (dashboardOnMap.isVisible()) {
			colorId = dashboardOnMap.getStatusBarColor();
		} else if (fragmentBelowDashboard != null) {
			colorId = fragmentBelowDashboard.getStatusBarColorId();
		} else if (mapLayers.getMapQuickActionLayer() != null
				&& mapLayers.getMapQuickActionLayer().isWidgetVisible()) {
			colorId = R.color.status_bar_transparent_gradient;
		}
		if (colorId != -1) {
			getWindow().setStatusBarColor(ContextCompat.getColor(this, colorId));
			return;
		}
		int color = TopToolbarController.NO_COLOR;
		boolean mapControlsVisible = findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE;
		boolean topToolbarVisible = getMapLayers().getMapInfoLayer().isTopToolbarViewVisible();
		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		TopToolbarController toolbarController = getMapLayers().getMapInfoLayer().getTopToolbarController();
		if (toolbarController != null && mapControlsVisible && topToolbarVisible) {
			color = toolbarController.getStatusBarColor(this, night);
		}
		if (color == TopToolbarController.NO_COLOR) {
			int defaultColorId = night ? R.color.status_bar_transparent_dark : R.color.status_bar_transparent_light;
			int colorIdForTopWidget = mapLayers.getMapWidgetRegistry().getStatusBarColorForTopWidget(night);
			colorId = mapControlsVisible && colorIdForTopWidget != -1 ? colorIdForTopWidget : defaultColorId;
			color = ContextCompat.getColor(this, colorId);
		}
		getWindow().setStatusBarColor(color);
	}

	private BaseOsmAndFragment getVisibleBaseOsmAndFragment(int... ids) {
		for (int id : ids) {
			Fragment fragment = getSupportFragmentManager().findFragmentById(id);
			if (fragment != null && !fragment.isRemoving() && fragment instanceof BaseOsmAndFragment
					&& ((BaseOsmAndFragment) fragment).getStatusBarColorId() != -1) {
				return (BaseOsmAndFragment) fragment;
			}
		}
		return null;
	}

	private BaseSettingsFragment getVisibleBaseSettingsFragment(int... ids) {
		for (int id : ids) {
			Fragment fragment = getSupportFragmentManager().findFragmentById(id);
			if (fragment != null && !fragment.isRemoving() && fragment instanceof BaseSettingsFragment
					&& ((BaseSettingsFragment) fragment).getStatusBarColorId() != -1) {
				return (BaseSettingsFragment) fragment;
			}
		}
		return null;
	}

	public boolean isInAppPurchaseAllowed() {
		return true;
	}

	public void showXMasDialog() {
		SecondSplashScreenFragment.SHOW = false;
		dismissSecondSplashScreen();
		new XMasDialogFragment().show(getSupportFragmentManager(), XMasDialogFragment.TAG);
	}


	private void dismissSecondSplashScreen() {
		if (SecondSplashScreenFragment.VISIBLE) {
			SecondSplashScreenFragment.VISIBLE = false;
			SecondSplashScreenFragment.SHOW = false;
			removeFragment(SecondSplashScreenFragment.TAG);
			applyScreenOrientation();
		}
	}

	@Override
	public void onDrawOverMap() {
		getMapView().setOnDrawMapListener(null);
		cancelSplashScreenTimer();
		dismissSecondSplashScreen();
	}

	private void cancelSplashScreenTimer() {
		if (splashScreenTimer != null) {
			splashScreenTimer.cancel();
			splashScreenTimer = null;
		}
	}

	public void dismissCardDialog() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.popBackStack(ContextMenuCardDialogFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public boolean isActivityDestroyed() {
		return mIsDestroyed;
	}

	public boolean isMapVisible() {
		if (isFragmentVisible()) {
			return false;
		}
		return AndroidUtils.isActivityNotDestroyed(this) && settings.MAP_ACTIVITY_ENABLED.get()
				&& !dashboardOnMap.isVisible();
	}

	public boolean isFragmentVisible() {
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (!(fragment instanceof DashBaseFragment) && fragment.isVisible()
					|| dashboardOnMap.isVisible()) {
				return true;
			}
		}
		return false;
	}

	public void readLocationToShow() {
		showMapControls();
		OsmandMapTileView mapView = getMapView();
		LatLon cur = new LatLon(mapView.getLatitude(), mapView.getLongitude());
		LatLon latLonToShow = settings.getAndClearMapLocationToShow();
		PointDescription mapLabelToShow = settings.getAndClearMapLabelToShow(latLonToShow);
		Object toShow = settings.getAndClearObjectToShow();
		boolean editToShow = settings.getAndClearEditObjectToShow();
		int status = settings.isRouteToPointNavigateAndClear();
		String searchRequestToShow = settings.getAndClearSearchRequestToShow();
		if (status != 0 || searchRequestToShow != null || latLonToShow != null) {
			dismissSettingsScreens();
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
			showQuickSearch(searchRequestToShow);
		}
		if (latLonToShow != null) {
			if (dashboardOnMap.isVisible()) {
				dashboardOnMap.hideDashboard();
			}
			// remember if map should come back to isMapLinkedToLocation=true
			getMapViewTrackingUtilities().setMapLinkedToLocation(false);
			if (mapLabelToShow != null && !mapLabelToShow.contextMenuDisabled()) {
				mapContextMenu.setMapCenter(latLonToShow);
				mapContextMenu.setMapPosition(mapView.getMapPosition());
				mapContextMenu.setCenterMarker(true);

				RotatedTileBox tb = mapView.getCurrentRotatedTileBox().copy();
				LatLon prevCenter = tb.getCenterLatLon();

				double border = 0.8;
				int tbw = (int) (tb.getPixWidth() * border);
				int tbh = (int) (tb.getPixHeight() * border);
				tb.setPixelDimensions(tbw, tbh);

				tb.setLatLonCenter(latLonToShow.getLatitude(), latLonToShow.getLongitude());
				tb.setZoom(ZOOM_LABEL_DISPLAY);
				while (!tb.containsLatLon(prevCenter.getLatitude(), prevCenter.getLongitude()) && tb.getZoom() > MIN_ZOOM_LABEL_DISPLAY) {
					tb.setZoom(tb.getZoom() - 1);
				}
				//mapContextMenu.setMapZoom(settings.getMapZoomToShow());
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
			} else if (!latLonToShow.equals(cur)) {
				mapView.getAnimatedDraggingThread().startMoving(latLonToShow.getLatitude(),
						latLonToShow.getLongitude(), settings.getMapZoomToShow(), true);
			}
		}
	}

	public OsmandApplication getMyApplication() {
		return ((OsmandApplication) getApplication());
	}

	public void addDialogProvider(DialogProvider dp) {
		dialogProviders.add(dp);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		for (DialogProvider dp : dialogProviders) {
			dialog = dp.onCreateDialog(id);
			if (dialog != null) {
				return dialog;
			}
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		for (DialogProvider dp : dialogProviders) {
			dp.onPrepareDialog(id, dialog);
		}
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_MOVE && settings.USE_TRACKBALL_FOR_MOVEMENTS.get()) {
			float x = event.getX();
			float y = event.getY();
			final RotatedTileBox tb = getMapView().getCurrentRotatedTileBox();
			final QuadPoint cp = tb.getCenterPixelPoint();
			final LatLon l = NativeUtilities.getLatLonFromPixel(getMapView().getMapRenderer(), tb,
					cp.x + x * 15, cp.y + y * 15);
			app.getOsmandMap().setMapLocation(l.getLatitude(), l.getLongitude());
			return true;
		}
		return super.onTrackballEvent(event);
	}

	@Override
	protected void onStart() {
		super.onStart();
		stopped = false;
		lockHelper.onStart();
		mapScrollHelper.setListener(this);
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
		mapScrollHelper.setListener(null);
		extendedMapActivity.onStop(this);
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		app.getOsmandMap().removeListener(this);
		getMapLayers().setMapActivity(null);
		getMapView().setMapActivity(null);
		mapContextMenu.setMapActivity(null);
		mapRouteInfoMenu.setMapActivity(null);
		trackDetailsMenu.setMapActivity(null);
		unregisterReceiver(screenOffReceiver);
		app.getAidlApi().onDestroyMapActivity(this);
		FailSafeFunctions.quitRouteRestoreDialog();
		OsmandPlugin.onMapActivityDestroy(this);
		getMyApplication().unsubscribeInitListener(initListener);
		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession == null) {
			getMapViewTrackingUtilities().setMapView(null);
		}
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnDestroy();
		}
		lockHelper.setLockUIAdapter(null);
		extendedMapActivity.onDestroy(this);

		mIsDestroyed = true;
	}

	public LatLon getMapLocation() {
		return getMapViewTrackingUtilities().getMapLocation();
	}

	public float getMapRotate() {
		return getMapView().getRotate();
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
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnPause();
		}
		app.getLocationProvider().pauseAllUpdates();
		app.getDaynightHelper().stopSensorIfNeeded();
		settings.APPLICATION_MODE.removeListener(applicationModeListener);

		settings.setLastKnownMapLocation((float) mapView.getLatitude(), (float) mapView.getLongitude());
		AnimateDraggingMapThread animatedThread = mapView.getAnimatedDraggingThread();
		if (animatedThread.isAnimating() && animatedThread.getTargetIntZoom() != 0 && !getMapViewTrackingUtilities().isMapLinkedToLocation()) {
			settings.setMapLocationToShow(animatedThread.getTargetLatitude(), animatedThread.getTargetLongitude(),
					animatedThread.getTargetIntZoom());
		}

		settings.setLastKnownMapZoom(mapView.getZoom());
		settings.setLastKnownMapElevation(mapView.getElevationAngle());
		settings.MAP_ACTIVITY_ENABLED.set(false);
		app.getResourceManager().interruptRendering();
		OsmandPlugin.onMapActivityPause(this);
	}

	public void updateApplicationModeSettings() {
		changeKeyguardFlags();
		updateMapSettings();
		app.getPoiFilters().loadSelectedPoiFilters();
		getMapViewTrackingUtilities().appModeChanged();

		OsmandMapTileView mapView = getMapView();
		MapLayers mapLayers = getMapLayers();
		if (mapLayers.getMapInfoLayer() != null) {
			mapLayers.getMapInfoLayer().recreateAllControls(this);
		}
		if (mapLayers.getMapQuickActionLayer() != null) {
			mapLayers.getMapQuickActionLayer().refreshLayer();
		}
		MapControlsLayer mapControlsLayer = mapLayers.getMapControlsLayer();
		if (mapControlsLayer != null && (!mapControlsLayer.isMapControlsVisible() && !settings.MAP_EMPTY_STATE_ALLOWED.get())) {
			showMapControls();
		}

		mapLayers.updateLayers(this);
		mapActions.updateDrawerMenu();
		updateNavigationBarColor();
		mapView.setComplexZoom(mapView.getZoom(), mapView.getSettingsMapDensity());
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
					OsmandPlugin.registerRenderingPreferences(app);
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

	public ScrollHelper getMapScrollHelper() {
		return mapScrollHelper;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mapActivityKeyListener != null) {
			if (mapActivityKeyListener.onKeyDown(keyCode, event)) {
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (mapActivityKeyListener != null) {
			if (mapActivityKeyListener.onKeyUp(keyCode, event)) {
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	public void scrollMap(float dx, float dy) {
		final RotatedTileBox tb = getMapView().getCurrentRotatedTileBox();
		final QuadPoint cp = tb.getCenterPixelPoint();
		final LatLon l = NativeUtilities.getLatLonFromPixel(getMapView().getMapRenderer(), tb,
				cp.x + dx, cp.y + dy);
		app.getOsmandMap().setMapLocation(l.getLatitude(), l.getLongitude());
	}

	public void showAndHideMapPosition() {
		getMapView().setShowMapPosition(true);
		app.runMessageInUIThreadAndCancelPrevious(SHOW_POSITION_MSG_ID, () -> {
			OsmandMapTileView mapView = getMapView();
			if (mapView.isShowMapPosition()) {
				mapView.setShowMapPosition(false);
				mapView.refreshMap();
			}
		}, 2500);
	}

	public void showMapControls() {
		MapLayers mapLayers = getMapLayers();
		if (!getDashboard().isVisible() && mapLayers.getMapControlsLayer() != null) {
			mapLayers.getMapControlsLayer().showMapControlsIfHidden();
		}
	}

	public boolean shouldHideTopControls() {
		boolean hideTopControls = !mapContextMenu.shouldShowTopControls();

		TrackMenuFragment fragment = getTrackMenuFragment();
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

	public static void launchMapActivityMoveToTop(@NonNull Context activity) {
		launchMapActivityMoveToTop(activity, null);
	}

	public static void launchMapActivityMoveToTop(@NonNull Context activity, @Nullable Bundle prevIntentParams) {
		launchMapActivityMoveToTop(activity, prevIntentParams, null, null);
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
		OsmandPlugin.onMapActivityResult(requestCode, resultCode, data);
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

	public void hideContextAndRouteInfoMenues() {
		mapContextMenu.hideMenues();
		mapRouteInfoMenu.hide();
	}

	public void openDrawer() {
		mapActions.updateDrawerMenu();
		boolean animate = !settings.DO_NOT_USE_ANIMATIONS.get();
		drawerLayout.openDrawer(GravityCompat.START, animate);
	}

	public void disableDrawer() {
		drawerDisabled = true;
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			closeDrawer();
		}
		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
	}

	public void enableDrawer() {
		drawerDisabled = false;
		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
	}

	public boolean isDrawerDisabled() {
		return drawerDisabled;
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (settings.DO_NOT_USE_ANIMATIONS.get()) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
					int drawerWidth = AndroidUtils.dpToPx(this, 280);
					int screenWidth = AndroidUtils.getScreenWidth(MapActivity.this);
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

	public FirstUsageWelcomeFragment getFirstUsageWelcomeFragment() {
		FirstUsageWelcomeFragment fragment = (FirstUsageWelcomeFragment) getSupportFragmentManager()
				.findFragmentByTag(FirstUsageWelcomeFragment.TAG);
		return fragment != null && !fragment.isDetached() ? fragment : null;
	}

	public FirstUsageWizardFragment getFirstUsageWizardFragment() {
		FirstUsageWizardFragment fragment = (FirstUsageWizardFragment) getSupportFragmentManager()
				.findFragmentByTag(FirstUsageWizardFragment.TAG);
		return fragment != null && !fragment.isDetached() ? fragment : null;
	}

	public boolean isFirstScreenShowing() {
		return getFirstUsageWelcomeFragment() != null || getFirstUsageWizardFragment() != null;
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
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull final int[] grantResults) {
		if (grantResults.length > 0) {
			OsmandPlugin.onRequestPermissionsResult(requestCode, permissions, grantResults);

			MapControlsLayer mcl = getMapView().getLayerByClass(MapControlsLayer.class);
			if (mcl != null) {
				mcl.onRequestPermissionsResult(requestCode, permissions, grantResults);
			}

			if (requestCode == DownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
					&& permissions.length > 0
					&& Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[0])) {
				permissionAsked = true;
				permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
				if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
					Toast.makeText(this,
							R.string.missing_write_external_storage_permission,
							Toast.LENGTH_LONG).show();
				}
			} else if (requestCode == FirstUsageWizardFragment.FIRST_USAGE_LOCATION_PERMISSION) {
				app.runInUIThread(() -> {
					FirstUsageWizardFragment wizardFragment = getFirstUsageWizardFragment();
					if (wizardFragment != null) {
						wizardFragment.processLocationPermission(grantResults[0] == PackageManager.PERMISSION_GRANTED);
					}
				}, 1);
			} else if (requestCode == MapActivityActions.REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION
					&& permissions.length > 0
					&& Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[0])) {
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					LatLon latLon = getContextMenu().getLatLon();
					if (latLon != null) {
						mapActions.enterDirectionsFromPoint(latLon.getLatitude(), latLon.getLongitude());
					}
				} else {
					app.showToastMessage(R.string.ask_for_location_permission);
				}
			}
		}

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
	public void onAMapPointUpdated(final AidlMapPointWrapper point, String layerId) {
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
		try {
			FragmentManager manager = getSupportFragmentManager();
			String fragmentName = pref.getFragment();
			Fragment fragment = manager.getFragmentFactory().instantiate(this.getClassLoader(), fragmentName);
			if (caller instanceof BaseSettingsFragment) {
				fragment.setArguments(((BaseSettingsFragment) caller).buildArguments());
			}
			String tag = fragment.getClass().getName();
			if (AndroidUtils.isFragmentCanBeAdded(manager, tag)) {
				manager.beginTransaction()
						.replace(R.id.fragmentContainer, fragment, tag)
						.addToBackStack(DRAWER_SETTINGS_ID)
						.commitAllowingStateLoss();
				return true;
			}
		} catch (Exception e) {
			LOG.error(e);
		}
		return false;
	}

	public void dismissSettingsScreens() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.popBackStack(DRAWER_SETTINGS_ID, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	@Override
	public void onScrollEvent(boolean continuousScrolling, boolean up, boolean down, boolean left, boolean right) {
		int scrollingUnit = continuousScrolling ? SMALL_SCROLLING_UNIT : BIG_SCROLLING_UNIT;
		int dx = (left ? -scrollingUnit : 0) + (right ? scrollingUnit : 0);
		int dy = (up ? -scrollingUnit : 0) + (down ? scrollingUnit : 0);
		scrollMap(dx, dy);
	}

	@Override
	public void onChangeZoom(int stp) {
		showAndHideMapPosition();
	}

	@Override
	public void onSetMapElevation(float angle) {
		if (atlasMapRendererView != null) {
			atlasMapRendererView.setElevationAngle(angle);
		}
	}

	@Override
	public void onSetupOpenGLView(boolean init) {
		setupOpenGLView(init);
	}

	private class ScreenOffReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			OsmandPlugin.onMapActivityScreenOff(MapActivity.this);
		}

	}

	public boolean isLandscapeLayout() {
		return landscapeLayout;
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

	public void showQuickSearch(double latitude, double longitude) {
		hideVisibleMenu();
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.dismiss();
			refreshMap();
		}
		QuickSearchDialogFragment.showInstance(this, "", null,
				QuickSearchType.REGULAR, QuickSearchTab.CATEGORIES, new LatLon(latitude, longitude));
	}

	public void showQuickSearch(String searchQuery) {
		hideVisibleMenu();
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.dismiss();
			refreshMap();
		}
		QuickSearchDialogFragment.showInstance(this, searchQuery, null,
				QuickSearchType.REGULAR, QuickSearchTab.CATEGORIES, null);
	}

	public void showQuickSearch(Object object) {
		showQuickSearch(object, null);
	}

	public void showQuickSearch(Object object, @Nullable LatLon latLon) {
		hideVisibleMenu();
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.dismiss();
			refreshMap();
		}
		QuickSearchDialogFragment.showInstance(this, "", object,
				QuickSearchType.REGULAR, QuickSearchTab.CATEGORIES, latLon);
	}

	public void showQuickSearch(ShowQuickSearchMode mode, boolean showCategories) {
		showQuickSearch(mode, showCategories, "", null);
	}

	public void showQuickSearch(ShowQuickSearchMode mode, QuickSearchTab showSearchTab) {
		showQuickSearch(mode, showSearchTab, "", null);
	}

	public void showQuickSearch(@NonNull ShowQuickSearchMode mode, boolean showCategories,
	                            @NonNull String searchQuery, @Nullable LatLon searchLocation) {
		if (mode == ShowQuickSearchMode.CURRENT) {
			mapContextMenu.close();
		} else {
			hideVisibleMenu();
		}
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (mode.isPointSelection()) {
			if (fragment != null) {
				fragment.dismiss();
			}
			QuickSearchType searchType = null;
			switch (mode) {
				case START_POINT_SELECTION:
					searchType = QuickSearchType.START_POINT;
					break;
				case DESTINATION_SELECTION:
					searchType = QuickSearchType.DESTINATION;
					break;
				case DESTINATION_SELECTION_AND_START:
					searchType = QuickSearchType.DESTINATION_AND_START;
					break;
				case INTERMEDIATE_SELECTION:
					searchType = QuickSearchType.INTERMEDIATE;
					break;
				case HOME_POINT_SELECTION:
					searchType = QuickSearchType.HOME_POINT;
					break;
				case WORK_POINT_SELECTION:
					searchType = QuickSearchType.WORK_POINT;
					break;
			}
			if (searchType != null) {
				QuickSearchDialogFragment.showInstance(this, searchQuery, null,
						searchType, showCategories ? QuickSearchTab.CATEGORIES : QuickSearchTab.ADDRESS, searchLocation);
			}
		} else if (fragment != null) {
			if (mode == ShowQuickSearchMode.NEW
					|| (mode == ShowQuickSearchMode.NEW_IF_EXPIRED && fragment.isExpired())) {
				fragment.dismiss();
				QuickSearchDialogFragment.showInstance(this, searchQuery, null,
						QuickSearchType.REGULAR, showCategories ? QuickSearchTab.CATEGORIES : QuickSearchTab.HISTORY, searchLocation);
			} else {
				fragment.show();
			}
			refreshMap();
		} else {
			QuickSearchDialogFragment.showInstance(this, searchQuery, null,
					QuickSearchType.REGULAR, showCategories ? QuickSearchTab.CATEGORIES : QuickSearchTab.HISTORY, searchLocation);
		}
	}

	public void showQuickSearch(@NonNull ShowQuickSearchMode mode, QuickSearchTab showSearchTab,
	                            @NonNull String searchQuery, @Nullable LatLon searchLocation) {
		if (mode == ShowQuickSearchMode.CURRENT) {
			mapContextMenu.close();
		} else {
			hideVisibleMenu();
		}
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (mode.isPointSelection()) {
			if (fragment != null) {
				fragment.dismiss();
			}
			QuickSearchType searchType = null;
			switch (mode) {
				case START_POINT_SELECTION:
					searchType = QuickSearchType.START_POINT;
					break;
				case DESTINATION_SELECTION:
				case DESTINATION_SELECTION_AND_START:
					searchType = QuickSearchType.DESTINATION;
					break;
				case INTERMEDIATE_SELECTION:
					searchType = QuickSearchType.INTERMEDIATE;
					break;
				case HOME_POINT_SELECTION:
					searchType = QuickSearchType.HOME_POINT;
					break;
				case WORK_POINT_SELECTION:
					searchType = QuickSearchType.WORK_POINT;
					break;
			}
			QuickSearchDialogFragment.showInstance(this, searchQuery, null,
					searchType, showSearchTab, searchLocation);
		} else if (fragment != null) {
			if (mode == ShowQuickSearchMode.NEW
					|| (mode == ShowQuickSearchMode.NEW_IF_EXPIRED && fragment.isExpired())) {
				fragment.dismiss();
				QuickSearchDialogFragment.showInstance(this, searchQuery, null,
						QuickSearchType.REGULAR, showSearchTab, searchLocation);
			} else {
				fragment.show();
			}
			refreshMap();
		} else {
			QuickSearchDialogFragment.showInstance(this, searchQuery, null,
					QuickSearchType.REGULAR, showSearchTab, searchLocation);
		}
	}

	public void showSettings() {
		dismissSettingsScreens();
		BaseSettingsFragment.showInstance(this, SettingsScreenType.MAIN_SETTINGS);
	}

	private void hideVisibleMenu() {
		if (mapContextMenu.isVisible()) {
			mapContextMenu.hide();
		} else if (mapContextMenu.getMultiSelectionMenu().isVisible()) {
			mapContextMenu.getMultiSelectionMenu().hide();
		} else if (getTrackMenuFragment() != null) {
			dismissTrackMenu();
		}
	}

	public void closeQuickSearch() {
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.closeSearch();
			refreshMap();
		}
	}

	public QuickSearchDialogFragment getQuickSearchDialogFragment() {
		return getFragment(QuickSearchDialogFragment.TAG);
	}

	public PlanRouteFragment getPlanRouteFragment() {
		return getFragment(PlanRouteFragment.TAG);
	}

	public MeasurementToolFragment getMeasurementToolFragment() {
		return getFragment(MeasurementToolFragment.TAG);
	}

	public TripRecordingStartingBottomSheet getTripRecordingBottomSheet() {
		return getFragment(TripRecordingStartingBottomSheet.TAG);
	}

	public ChooseRouteFragment getChooseRouteFragment() {
		return getFragment(ChooseRouteFragment.TAG);
	}

	public GpxApproximationFragment getGpxApproximationFragment() {
		return getFragment(GpxApproximationFragment.TAG);
	}

	public SnapTrackWarningFragment getSnapTrackWarningBottomSheet() {
		return getFragment(SnapTrackWarningFragment.TAG);
	}

	public TrackMenuFragment getTrackMenuFragment() {
		return getFragment(TrackMenuFragment.TAG);
	}

	public TrackAppearanceFragment getTrackAppearanceFragment() {
		return getFragment(TrackAppearanceFragment.TAG);
	}

	@Nullable
	public GpsFilterFragment getGpsFilterFragment() {
		return getFragment(GpsFilterFragment.TAG);
	}

	@Nullable
	public DownloadTilesFragment getDownloadTilesFragment() {
		return getFragment(DownloadTilesFragment.TAG);
	}

	public void dismissTrackMenu() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.popBackStack(TrackMenuFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public void backToConfigureProfileFragment() {
		FragmentManager fragmentManager = getSupportFragmentManager();
		int backStackEntryCount = fragmentManager.getBackStackEntryCount();
		if (backStackEntryCount > 0 && !fragmentManager.isStateSaved()) {
			BackStackEntry entry = fragmentManager.getBackStackEntryAt(backStackEntryCount - 1);
			if (ConfigureProfileFragment.TAG.equals(entry.getName())) {
				fragmentManager.popBackStack();
			}
		}
	}

	@Nullable
	public <T> T getFragment(String fragmentTag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
		return fragment != null && !fragment.isDetached() && !fragment.isRemoving() ? (T) fragment : null;
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
	public void onInAppPurchaseGetItems() {
		DiscountHelper.checkAndDisplay(this);
	}

	public enum ShowQuickSearchMode {
		NEW,
		NEW_IF_EXPIRED,
		CURRENT,
		START_POINT_SELECTION,
		DESTINATION_SELECTION,
		DESTINATION_SELECTION_AND_START,
		INTERMEDIATE_SELECTION,
		HOME_POINT_SELECTION,
		WORK_POINT_SELECTION;

		public boolean isPointSelection() {
			return this != NEW && this != NEW_IF_EXPIRED && this != CURRENT;
		}
	}
}