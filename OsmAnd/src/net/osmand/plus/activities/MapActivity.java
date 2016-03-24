package net.osmand.plus.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.ValueHolder;
import net.osmand.access.AccessibilityPlugin;
import net.osmand.access.AccessibleActivity;
import net.osmand.access.AccessibleToast;
import net.osmand.access.MapAccessibilityActions;
import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.AppInitializer.InitEvents;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.BusyIndicator;
import net.osmand.plus.FirstUsageFragment;
import net.osmand.plus.MapMarkersHelper.MapMarker;
import net.osmand.plus.MapMarkersHelper.MapMarkerChangedListener;
import net.osmand.plus.OsmAndConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.Version;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.base.FailSafeFuntions;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.dialogs.ErrorBottomSheetDialog;
import net.osmand.plus.dialogs.RateUsBottomSheetDialog;
import net.osmand.plus.dialogs.WhatsNewDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.ui.DataStoragePlaceDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.ExternalApiHelper;
import net.osmand.plus.helpers.GpxImportHelper;
import net.osmand.plus.helpers.WakeLockHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.MapContextMenuFragment;
import net.osmand.plus.mapcontextmenu.other.DestinationReachedMenu;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenu;
import net.osmand.plus.mapcontextmenu.other.MapRouteInfoMenuFragment;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelper.IRouteInformationListener;
import net.osmand.plus.routing.RoutingHelper.RouteCalculationProgressCallback;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.OsmAndMapLayersView;
import net.osmand.plus.views.OsmAndMapSurfaceView;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapActivity extends AccessibleActivity implements DownloadEvents,
		ActivityCompat.OnRequestPermissionsResultCallback, IRouteInformationListener,
		MapMarkerChangedListener {
	public static final String INTENT_KEY_PARENT_MAP_ACTIVITY = "intent_parent_map_activity_key";

	private static final int SHOW_POSITION_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 1;
	private static final int LONG_KEYPRESS_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 2;
	private static final int LONG_KEYPRESS_DELAY = 500;

	private static final Log LOG = PlatformUtil.getLog(MapActivity.class);

	private static MapViewTrackingUtilities mapViewTrackingUtilities;
	private static MapContextMenu mapContextMenu = new MapContextMenu();
	private static Intent prevActivityIntent = null;

	private BroadcastReceiver screenOffReceiver;

	/**
	 * Called when the activity is first created.
	 */
	private OsmandMapTileView mapView;
	private AtlasMapRendererView atlasMapRendererView;

	private MapActivityActions mapActions;
	private MapActivityLayers mapLayers;

	// Notification status
	private NotificationManager mNotificationManager;
	private int APP_NOTIFICATION_ID = 1;

	// handler to show/hide trackball position and to link map with delay
	private Handler uiHandler = new Handler();
	// App variables
	private OsmandApplication app;
	private OsmandSettings settings;

	private boolean landscapeLayout;

	private Dialog progressDlg = null;

	private List<DialogProvider> dialogProviders = new ArrayList<>(2);
	private StateChangedListener<ApplicationMode> applicationModeListener;
	private GpxImportHelper gpxImportHelper;
	private WakeLockHelper wakeLockHelper;
	private boolean intentLocation = false;

	private DashboardOnMap dashboardOnMap = new DashboardOnMap(this);
	private AppInitializeListener initListener;
	private IMapDownloaderCallback downloaderCallback;
	private DrawerLayout drawerLayout;
	private boolean drawerDisabled;

	private static boolean permissionDone;
	private boolean permissionAsked;
	private boolean permissionGranted;

	private Notification getNotification() {
		Intent notificationIndent = new Intent(this, getMyApplication().getAppCustomization().getMapActivity());
		notificationIndent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIndent, PendingIntent.FLAG_UPDATE_CURRENT);
//		Notification notification = new Notification(R.drawable.bgs_icon_drive, "", //$NON-NLS-1$
//				System.currentTimeMillis());
//		notification.flags |= Notification.FLAG_AUTO_CANCEL;
//		notification.setLatestEventInfo(this, Version.getAppName(app), getString(R.string.go_back_to_osmand),
//				pi);
		int smallIcon = app.getSettings().getApplicationMode().getSmallIconDark();
		final Builder noti = new NotificationCompat.Builder(this).setContentTitle(Version.getAppName(app))
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.setContentText(getString(R.string.go_back_to_osmand))
				.setSmallIcon(smallIcon)
//	        .setLargeIcon(Helpers.getBitmap(R.drawable.mirakel, getBaseContext()))
				.setContentIntent(pi).setOngoing(true);
		return noti.build();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		long tm = System.currentTimeMillis();
		app = getMyApplication();
		settings = app.getSettings();
		app.applyTheme(this);
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

		boolean portraitMode = AndroidUiHelper.isOrientationPortrait(this);
		boolean largeDevice = AndroidUiHelper.isXLargeDevice(this);
		landscapeLayout = !portraitMode && !largeDevice;

		mapContextMenu.setMapActivity(this);

		super.onCreate(savedInstanceState);
		// Full screen is not used here
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);

		int statusBarHeight = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			statusBarHeight = getResources().getDimensionPixelSize(resourceId);
		}
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int w = dm.widthPixels;
		int h = dm.heightPixels - statusBarHeight;

		mapView = new OsmandMapTileView(this, w, h);
		if (app.getAppInitializer().checkAppVersionChanged() && WhatsNewDialogFragment.SHOW) {
			WhatsNewDialogFragment.SHOW = false;
			new WhatsNewDialogFragment().show(getSupportFragmentManager(), null);
		}
		mapActions = new MapActivityActions(this);
		mapLayers = new MapActivityLayers(this);
		if (mapViewTrackingUtilities == null) {
			mapViewTrackingUtilities = new MapViewTrackingUtilities(app);
		}
		dashboardOnMap.createDashboardView();
		checkAppInitialization();
		parseLaunchIntentLocation();
		mapView.setTrackBallDelegate(new OsmandMapTileView.OnTrackBallListener() {
			@Override
			public boolean onTrackBallEvent(MotionEvent e) {
				showAndHideMapPosition();
				return MapActivity.this.onTrackballEvent(e);
			}
		});
		mapView.setAccessibilityActions(new MapAccessibilityActions(this));
		mapViewTrackingUtilities.setMapView(mapView);

		// to not let it gc
		downloaderCallback = new IMapDownloaderCallback() {
			@Override
			public void tileDownloaded(DownloadRequest request) {
				if (request != null && !request.error && request.fileToSave != null) {
					ResourceManager mgr = app.getResourceManager();
					mgr.tileDownloaded(request);
				}
				if (request == null || !request.error) {
					mapView.tileDownloaded(request);
				}
			}
		};
		app.getResourceManager().getMapTileDownloader().addDownloaderCallback(downloaderCallback);
		createProgressBarForRouting();
		mapLayers.createLayers(mapView);
		// This situtation could be when navigation suddenly crashed and after restarting
		// it tries to continue the last route
		if (settings.FOLLOW_THE_ROUTE.get() && !app.getRoutingHelper().isRouteCalculated()
				&& !app.getRoutingHelper().isRouteBeingCalculated()) {
			FailSafeFuntions.restoreRoutingMode(this);
		} else if (app.getSettings().USE_MAP_MARKERS.get()
				&& !app.getRoutingHelper().isRoutePlanningMode()
				&& !settings.FOLLOW_THE_ROUTE.get()
				&& app.getTargetPointsHelper().getAllPoints().size() > 0) {
			app.getRoutingHelper().clearCurrentRoute(null, new ArrayList<LatLon>());
			app.getTargetPointsHelper().removeAllWayPoints(false, false);
		}

		if (!settings.isLastKnownMapLocation()) {
			// show first time when application ran
			net.osmand.Location location = app.getLocationProvider().getFirstTimeRunDefaultLocation();
			mapViewTrackingUtilities.setMapLinkedToLocation(true);
			if (location != null) {
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
				mapView.setIntZoom(14);
			}
		}
		addDialogProvider(mapActions);
		OsmandPlugin.onMapActivityCreate(this);
		gpxImportHelper = new GpxImportHelper(this, getMyApplication(), getMapView());
		wakeLockHelper = new WakeLockHelper(getMyApplication());
		if (System.currentTimeMillis() - tm > 50) {
			System.err.println("OnCreate for MapActivity took " + (System.currentTimeMillis() - tm) + " ms");
		}
		mapView.refreshMap(true);

		if (getMyApplication().getAppInitializer().isFirstTime() && FirstUsageFragment.SHOW) {
			FirstUsageFragment.SHOW = false;
			getSupportFragmentManager().beginTransaction()
					.add(R.id.fragmentContainer, new FirstUsageFragment(),
							FirstUsageFragment.TAG).commit();
		}
		mapActions.updateDrawerMenu();
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
		screenOffReceiver = new ScreenOffReceiver();
		registerReceiver(screenOffReceiver, filter);
	}


	private void checkAppInitialization() {
		if (app.isApplicationInitializing()) {
			findViewById(R.id.init_progress).setVisibility(View.VISIBLE);
			initListener = new AppInitializeListener() {
				boolean openGlSetup = false;

				@Override
				public void onProgress(AppInitializer init, InitEvents event) {
					String tn = init.getCurrentInitTaskName();
					if (tn != null) {
						((TextView) findViewById(R.id.ProgressMessage)).setText(tn);
					}
					if (event == InitEvents.NATIVE_INITIALIZED) {
						setupOpenGLView(false);
						openGlSetup = true;
					}
					if (event == InitEvents.MAPS_INITIALIZED) {
						// TODO investigate if this false cause any issues!
						mapView.refreshMap(false);
						if (dashboardOnMap != null) {
							dashboardOnMap.updateLocation(true, true, false);
						}
					}
				}

				@Override
				public void onFinish(AppInitializer init) {
					if (!openGlSetup) {
						setupOpenGLView(false);
					}
					mapView.refreshMap(false);
					if (dashboardOnMap != null) {
						dashboardOnMap.updateLocation(true, true, false);
					}
					findViewById(R.id.init_progress).setVisibility(View.GONE);
					findViewById(R.id.drawer_layout).invalidate();
				}
			};
			getMyApplication().checkApplicationIsBeingInitialized(this, initListener);
		} else {
			setupOpenGLView(true);
		}
	}

	private void setupOpenGLView(boolean init) {
		if (settings.USE_OPENGL_RENDER.get() && NativeCoreContext.isInit()) {
			ViewStub stub = (ViewStub) findViewById(R.id.atlasMapRendererViewStub);
			atlasMapRendererView = (AtlasMapRendererView) stub.inflate();
			OsmAndMapLayersView ml = (OsmAndMapLayersView) findViewById(R.id.MapLayersView);
			ml.setVisibility(View.VISIBLE);
			atlasMapRendererView.setAzimuth(0);
			atlasMapRendererView.setElevationAngle(90);
			NativeCoreContext.getMapRendererContext().setMapRendererView(atlasMapRendererView);
			ml.setMapView(mapView);
			mapViewTrackingUtilities.setMapView(mapView);
			mapView.setMapRender(atlasMapRendererView);
			OsmAndMapSurfaceView surf = (OsmAndMapSurfaceView) findViewById(R.id.MapView);
			surf.setVisibility(View.GONE);
		} else {
			OsmAndMapSurfaceView surf = (OsmAndMapSurfaceView) findViewById(R.id.MapView);
			surf.setVisibility(View.VISIBLE);
			surf.setMapView(mapView);
		}
	}

	private void createProgressBarForRouting() {
		final ProgressBar pb = (ProgressBar) findViewById(R.id.map_horizontal_progress);
		final View pbExtView = findViewById(R.id.progress_layout_external);
		final ProgressBar pbExt = (ProgressBar) findViewById(R.id.map_horizontal_progress_external);

		app.getRoutingHelper().setProgressBar(new RouteCalculationProgressCallback() {

			@Override
			public void updateProgress(int progress) {
				if (findViewById(R.id.MapHudButtonsOverlay).getVisibility() == View.VISIBLE) {
					pbExtView.setVisibility(View.GONE);
					pb.setVisibility(View.VISIBLE);
					pb.setProgress(progress);
					pb.requestLayout();
				} else {
					pb.setVisibility(View.GONE);
					pbExtView.setVisibility(View.VISIBLE);
					pbExt.setProgress(progress);
					pbExt.requestLayout();
				}
			}

			@Override
			public void finish() {
				pbExtView.setVisibility(View.GONE);
				pb.setVisibility(View.GONE);
			}
		});
	}

	private void changeKeyguardFlags() {
		if (settings.WAKE_ON_VOICE_INT.get() > 0) {
			getWindow()
					.setFlags(
							WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
									| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
							WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
									| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		} else {
			getWindow()
					.clearFlags(
							WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
									| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		}
	}

	@SuppressWarnings("rawtypes")
	public Object getLastNonConfigurationInstanceByKey(String key) {
		Object k = super.getLastNonConfigurationInstance();
		if (k instanceof Map) {

			return ((Map) k).get(key);
		}
		return null;
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		LinkedHashMap<String, Object> l = new LinkedHashMap<>();
		for (OsmandMapLayer ml : mapView.getLayers()) {
			ml.onRetainNonConfigurationInstance(l);
		}
		return l;
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		setIntent(intent);
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
		if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
			closeDrawer();
			return;
		}

		if (prevActivityIntent != null && getSupportFragmentManager().getBackStackEntryCount() == 0) {
			prevActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			LatLon loc = getMapLocation();
			prevActivityIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
			prevActivityIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
			if (mapViewTrackingUtilities.isMapLinkedToLocation()) {
				prevActivityIntent.putExtra(SearchActivity.SEARCH_NEARBY, true);
			}
			this.startActivity(prevActivityIntent);
			prevActivityIntent = null;
		} else {
			super.onBackPressed();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		long tm = System.currentTimeMillis();

		if (app.isApplicationInitializing() || DashboardOnMap.staticVisible) {
			if (!dashboardOnMap.isVisible()) {
				if (settings.SHOW_DASHBOARD_ON_START.get()) {
					dashboardOnMap.setDashboardVisibility(true, DashboardOnMap.staticVisibleType);
				} else {
					if (ErrorBottomSheetDialog.shouldShow(settings, this)) {
						new ErrorBottomSheetDialog().show(getSupportFragmentManager(), "dialog");
					} else if (RateUsBottomSheetDialog.shouldShow(app)) {
						new RateUsBottomSheetDialog().show(getSupportFragmentManager(), "dialog");
					}
				}
			} else {
				dashboardOnMap.updateDashboard();
			}
		}
		dashboardOnMap.updateLocation(true, true, false);

		cancelNotification();
		// fixing bug with action bar appearing on android 2.3.3
		if (getSupportActionBar() != null) {
			getSupportActionBar().hide();
		}
		if (settings.MAP_SCREEN_ORIENTATION.get() != getRequestedOrientation()) {
			setRequestedOrientation(settings.MAP_SCREEN_ORIENTATION.get());
			// can't return from this method we are not sure if activity will be recreated or not
		}

		app.getLocationProvider().checkIfLastKnownLocationIsValid();
		// for voice navigation
		if (settings.AUDIO_STREAM_GUIDANCE.get() != null) {
			setVolumeControlStream(settings.AUDIO_STREAM_GUIDANCE.get());
		} else {
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
		}

		changeKeyguardFlags();

		applicationModeListener = new StateChangedListener<ApplicationMode>() {
			@Override
			public void stateChanged(ApplicationMode change) {
				updateApplicationModeSettings();
			}
		};
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

		if (settings != null && settings.isLastKnownMapLocation() && !intentLocation) {
			LatLon l = settings.getLastKnownMapLocation();
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setIntZoom(settings.getLastKnownMapZoom());
		} else {
			intentLocation = false;
		}

		settings.MAP_ACTIVITY_ENABLED.set(true);
		checkExternalStorage();
		showAndHideMapPosition();

		readLocationToShow();

		OsmandPlugin.onMapActivityResume(this);

		final Intent intent = getIntent();
		if (intent != null) {
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				if (intent.getData() != null) {
					final Uri data = intent.getData();
					final String scheme = data.getScheme();
					if ("file".equals(scheme)) {
						gpxImportHelper.handleFileImport(data, new File(data.getPath()).getName());
						setIntent(null);
					} else if ("content".equals(scheme)) {
						gpxImportHelper.handleContenImport(data);
						setIntent(null);
					} else if ("google.navigation".equals(scheme) || "osmand.navigation".equals(scheme)) {
						parseNavigationIntent(data);
					} else if ("osmand.api".equals(scheme)) {
						ExternalApiHelper apiHelper = new ExternalApiHelper(this);
						Intent result = apiHelper.processApiRequest(intent);
						setResult(apiHelper.getResultCode(), result);
						if (apiHelper.needFinish()) {
							finish();
						}
					}
				}
			}
			if (intent.getStringExtra(ShowRouteInfoActivity.START_NAVIGATION) != null) {
				mapLayers.getMapControlsLayer().startNavigation();
			}
		}

		View progress = mapLayers.getMapInfoLayer().getProgressBar();
		if (progress != null) {
			app.getResourceManager().setBusyIndicator(new BusyIndicator(this, progress));
		}

		mapView.refreshMap(true);
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnResume();
		}

		app.getDownloadThread().setUiActivity(this);

		if (mapViewTrackingUtilities.getShowRouteFinishDialog()) {
			DestinationReachedMenu.show(this);
			mapViewTrackingUtilities.setShowRouteFinishDialog(false);
		}

		routingHelper.addListener(this);
		app.getMapMarkersHelper().addListener(this);

		getMyApplication().getAppCustomization().resumeActivity(MapActivity.class, this);
		if (System.currentTimeMillis() - tm > 50) {
			System.err.println("OnCreate for MapActivity took " + (System.currentTimeMillis() - tm) + " ms");
		}

		if (!permissionDone && !app.getAppInitializer().isFirstTime()) {
			if (!permissionAsked) {
				if (app.isExternalStorageDirectoryReadOnly()
						&& getSupportFragmentManager().findFragmentByTag(DataStoragePlaceDialogFragment.TAG) == null) {
					if (DownloadActivity.hasPermissionToWriteExternalStorage(this)) {
						DataStoragePlaceDialogFragment.showInstance(getSupportFragmentManager(), true);
					} else {
						ActivityCompat.requestPermissions(this,
								new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
								DownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
					}
				}
			} else {
				if (permissionGranted) {
					restartApp();
				} else if (getSupportFragmentManager().findFragmentByTag(DataStoragePlaceDialogFragment.TAG) == null) {
					DataStoragePlaceDialogFragment.showInstance(getSupportFragmentManager(), true);
				}
				permissionAsked = false;
				permissionGranted = false;
				permissionDone = true;
			}
		}
	}

	private void restartApp() {
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		bld.setMessage(R.string.storage_permission_restart_is_required);
		bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				doRestart(MapActivity.this);
				//android.os.Process.killProcess(android.os.Process.myPid());
			}
		});
		bld.show();
	}

	public static void doRestart(Context c) {
		boolean res = false;
		try {
			//check if the context is given
			if (c != null) {
				//fetch the packagemanager so we can get the default launch activity
				// (you can replace this intent with any other activity if you want
				PackageManager pm = c.getPackageManager();
				//check if we got the PackageManager
				if (pm != null) {
					//create the intent with the default start activity for your application
					Intent mStartActivity = pm.getLaunchIntentForPackage(
							c.getPackageName()
					);
					if (mStartActivity != null) {
						mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						//create a pending intent so the application is restarted after System.exit(0) was called.
						// We use an AlarmManager to call this intent in 100ms
						int mPendingIntentId = 84523443;
						PendingIntent mPendingIntent = PendingIntent
								.getActivity(c, mPendingIntentId, mStartActivity,
										PendingIntent.FLAG_CANCEL_CURRENT);
						AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
						mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
						//kill the application
						res = true;
						android.os.Process.killProcess(android.os.Process.myPid());
						//System.exit(0);
					} else {
						LOG.error("Was not able to restart application, mStartActivity null");
					}
				} else {
					LOG.error("Was not able to restart application, PM null");
				}
			} else {
				LOG.error("Was not able to restart application, Context null");
			}
		} catch (Exception ex) {
			LOG.error("Was not able to restart application");
		}
		if (!res) {
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	private void parseNavigationIntent(final Uri data) {
		final String schemeSpecificPart = data.getSchemeSpecificPart();

		final Matcher matcher = Pattern.compile("(?:q|ll)=([\\-0-9.]+),([\\-0-9.]+)(?:.*)").matcher(
				schemeSpecificPart);
		if (matcher.matches()) {
			try {
				final double lat = Double.valueOf(matcher.group(1));
				final double lon = Double.valueOf(matcher.group(2));

				getMyApplication().getTargetPointsHelper().navigateToPoint(new LatLon(lat, lon), false,
						-1);
				getMapActions().enterRoutePlanningModeGivenGpx(null, null, null, false, true);
			} catch (NumberFormatException e) {
				AccessibleToast.makeText(this,
						getString(R.string.navigation_intent_invalid, schemeSpecificPart),
						Toast.LENGTH_LONG).show(); //$NON-NLS-1$
			}
		} else {
			AccessibleToast.makeText(this,
					getString(R.string.navigation_intent_invalid, schemeSpecificPart),
					Toast.LENGTH_LONG).show(); //$NON-NLS-1$
		}
		setIntent(null);
	}

	public void readLocationToShow() {
		LatLon cur = new LatLon(mapView.getLatitude(), mapView.getLongitude());
		LatLon latLonToShow = settings.getAndClearMapLocationToShow();
		PointDescription mapLabelToShow = settings.getAndClearMapLabelToShow(latLonToShow);
		Object toShow = settings.getAndClearObjectToShow();
		int status = settings.isRouteToPointNavigateAndClear();
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
		if (latLonToShow != null) {
			if (dashboardOnMap.isVisible()) {
				dashboardOnMap.hideDashboard();
			}
			// remember if map should come back to isMapLinkedToLocation=true
			mapViewTrackingUtilities.setMapLinkedToLocation(false);

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
				while (!tb.containsLatLon(prevCenter.getLatitude(), prevCenter.getLongitude())) {
					tb.setZoom(tb.getZoom() - 1);
				}
				//mapContextMenu.setMapZoom(settings.getMapZoomToShow());
				mapContextMenu.setMapZoom(tb.getZoom());
				if (mapLayers.getMapControlsLayer().getMapRouteInfoMenu().isVisible()) {
					mapContextMenu.showMinimized(latLonToShow, mapLabelToShow, toShow);
					mapLayers.getMapControlsLayer().getMapRouteInfoMenu().updateMenu();
				} else {
					mapContextMenu.show(latLonToShow, mapLabelToShow, toShow);
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

	public void changeZoom(int stp, long time) {
		mapViewTrackingUtilities.setZoomTime(time);
		changeZoom(stp);
	}

	public void changeZoom(int stp) {
		// delta = Math.round(delta * OsmandMapTileView.ZOOM_DELTA) * OsmandMapTileView.ZOOM_DELTA_1;
		boolean changeLocation = false;
		// if (settings.AUTO_ZOOM_MAP.get() == AutoZoomMap.NONE) {
		// changeLocation = false;
		// }

		// double curZoom = mapView.getZoom() + mapView.getZoomFractionalPart() + stp * 0.3;
		// int newZoom = (int) Math.round(curZoom);
		// double zoomFrac = curZoom - newZoom;

		final int newZoom = mapView.getZoom() + stp;
		final double zoomFrac = mapView.getZoomFractionalPart();
		if (newZoom > 22) {
			AccessibleToast.makeText(this, R.string.edit_tilesource_maxzoom, Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
			return;
		}
		if (newZoom < 1) {
			AccessibleToast.makeText(this, R.string.edit_tilesource_minzoom, Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
			return;
		}
		mapView.getAnimatedDraggingThread().startZooming(newZoom, zoomFrac, changeLocation);
		if (app.accessibilityEnabled())
			AccessibleToast.makeText(this, getString(R.string.zoomIs) + " " + newZoom, Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
		showAndHideMapPosition();
	}

	public void setMapLocation(double lat, double lon) {
		mapView.setLatLon(lat, lon);
		mapViewTrackingUtilities.locationChanged(lat, lon, this);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_MOVE && settings.USE_TRACKBALL_FOR_MOVEMENTS.get()) {
			float x = event.getX();
			float y = event.getY();
			final RotatedTileBox tb = mapView.getCurrentRotatedTileBox();
			final QuadPoint cp = tb.getCenterPixelPoint();
			final LatLon l = tb.getLatLonFromPixel(cp.x + x * 15, cp.y + y * 15);
			setMapLocation(l.getLatitude(), l.getLongitude());
			return true;
		}
		return super.onTrackballEvent(event);
	}


	@Override
	protected void onStart() {
		super.onStart();
		wakeLockHelper.onStart(this);
		getMyApplication().getNotificationHelper().showNotification();
	}

	protected void setProgressDlg(Dialog progressDlg) {
		this.progressDlg = progressDlg;
	}

	protected Dialog getProgressDlg() {
		return progressDlg;
	}

	@Override
	protected void onStop() {
		if (app.getRoutingHelper().isFollowingMode()) {
			mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			if (mNotificationManager != null) {
				mNotificationManager.notify(APP_NOTIFICATION_ID, getNotification());
			}
		}
		wakeLockHelper.onStop(this);
		if (getMyApplication().getNavigationService() == null) {
			getMyApplication().getNotificationHelper().removeServiceNotificationCompletely();
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(screenOffReceiver);
		FailSafeFuntions.quitRouteRestoreDialog();
		OsmandPlugin.onMapActivityDestroy(this);
		getMyApplication().unsubscribeInitListener(initListener);
		mapViewTrackingUtilities.setMapView(null);
		cancelNotification();
		app.getResourceManager().getMapTileDownloader().removeDownloaderCallback(mapView);
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnDestroy();
		}
	}

	private void cancelNotification() {
		if (mNotificationManager == null) {
			mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		}
		if (mNotificationManager != null) {
			mNotificationManager.cancel(APP_NOTIFICATION_ID);
		}
	}

	public LatLon getMapLocation() {
		if (mapView == null) {
			return settings.getLastKnownMapLocation();
		}
		return new LatLon(mapView.getLatitude(), mapView.getLongitude());
	}

	public float getMapRotate() {
		if (mapView == null) {
			return 0;
		}
		return mapView.getRotate();
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
		app.getMapMarkersHelper().removeListener(this);
		app.getRoutingHelper().removeListener(this);
		app.getDownloadThread().resetUiActivity(this);
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnPause();
		}
		super.onPause();
		app.getLocationProvider().pauseAllUpdates();
		app.getDaynightHelper().stopSensorIfNeeded();
		settings.APPLICATION_MODE.removeListener(applicationModeListener);

		settings.setLastKnownMapLocation((float) mapView.getLatitude(), (float) mapView.getLongitude());
		AnimateDraggingMapThread animatedThread = mapView.getAnimatedDraggingThread();
		if (animatedThread.isAnimating() && animatedThread.getTargetIntZoom() != 0) {
			settings.setMapLocationToShow(animatedThread.getTargetLatitude(), animatedThread.getTargetLongitude(),
					animatedThread.getTargetIntZoom());
		}

		settings.setLastKnownMapZoom(mapView.getZoom());
		settings.MAP_ACTIVITY_ENABLED.set(false);
		getMyApplication().getAppCustomization().pauseActivity(MapActivity.class);
		app.getResourceManager().interruptRendering();
		app.getResourceManager().setBusyIndicator(null);
		OsmandPlugin.onMapActivityPause(this);
	}

	public void updateApplicationModeSettings() {
		changeKeyguardFlags();
		updateMapSettings();
		mapViewTrackingUtilities.updateSettings();
		//app.getRoutingHelper().setAppMode(settings.getApplicationMode());
		if (mapLayers.getMapInfoLayer() != null) {
			mapLayers.getMapInfoLayer().recreateControls();
		}
		mapLayers.updateLayers(mapView);
		mapActions.updateDrawerMenu();
		mapView.setComplexZoom(mapView.getZoom(), mapView.getSettingsMapDensity());
		app.getDaynightHelper().startSensorIfNeeded(new StateChangedListener<Boolean>() {

			@Override
			public void stateChanged(Boolean change) {
				getMapView().refreshMap(true);
			}
		});
		getMapView().refreshMap(true);
	}

	public void updateMapSettings() {
		if (app.isApplicationInitializing()) {
			return;
		}
		// update vector renderer
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				RendererRegistry registry = app.getRendererRegistry();
				RenderingRulesStorage newRenderer = registry.getRenderer(settings.RENDERER.get());
				if (newRenderer == null) {
					newRenderer = registry.defaultRender();
				}
				if (mapView.getMapRenderer() != null) {
					NativeCoreContext.getMapRendererContext().updateMapSettings();
				}
				if (registry.getCurrentSelectedRenderer() != newRenderer) {
					registry.setCurrentSelectedRender(newRenderer);
					app.getResourceManager().getRenderer().clearCache();
					mapView.refreshMap(true);
				}
				return null;
			}

			protected void onPostExecute(Void result) {
			}
		}.execute((Void) null);

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && app.accessibilityEnabled()) {
			if (!uiHandler.hasMessages(LONG_KEYPRESS_MSG_ID)) {
				Message msg = Message.obtain(uiHandler, new Runnable() {
					@Override
					public void run() {
						app.getLocationProvider().emitNavigationHint();
					}
				});
				msg.what = LONG_KEYPRESS_MSG_ID;
				uiHandler.sendMessageDelayed(msg, LONG_KEYPRESS_DELAY);
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
			Intent newIntent = new Intent(MapActivity.this, getMyApplication().getAppCustomization()
					.getSearchActivity());
			// causes wrong position caching: newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			LatLon loc = getMapLocation();
			newIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
			newIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
			if (mapViewTrackingUtilities.isMapLinkedToLocation()) {
				newIntent.putExtra(SearchActivity.SEARCH_NEARBY, true);
			}
			startActivity(newIntent);
			newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			return true;
		} else if (!app.getRoutingHelper().isFollowingMode()
				&& OsmandPlugin.getEnabledPlugin(AccessibilityPlugin.class) != null) {
			// Find more appropriate plugin for it?
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.getRepeatCount() == 0) {
				if (mapView.isZooming()) {
					changeZoom(+2);
				} else {
					changeZoom(+1);
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.getRepeatCount() == 0) {
				changeZoom(-1);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			if (!app.accessibilityEnabled()) {
				mapActions.contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
			} else if (uiHandler.hasMessages(LONG_KEYPRESS_MSG_ID)) {
				uiHandler.removeMessages(LONG_KEYPRESS_MSG_ID);
				mapActions.contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU /*&& event.getRepeatCount() == 0*/) {
			// repeat count 0 doesn't work for samsung, 1 doesn't work for lg
			toggleDrawer();
			return true;
		} else if (settings.ZOOM_BY_TRACKBALL.get()) {
			// Parrot device has only dpad left and right
			if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
				changeZoom(-1);
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
				changeZoom(1);
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
				|| keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			int dx = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 15 : (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -15 : 0);
			int dy = keyCode == KeyEvent.KEYCODE_DPAD_DOWN ? 15 : (keyCode == KeyEvent.KEYCODE_DPAD_UP ? -15 : 0);
			final RotatedTileBox tb = mapView.getCurrentRotatedTileBox();
			final QuadPoint cp = tb.getCenterPixelPoint();
			final LatLon l = tb.getLatLonFromPixel(cp.x + dx, cp.y + dy);
			setMapLocation(l.getLatitude(), l.getLongitude());
			return true;
		} else if (OsmandPlugin.onMapActivityKeyUp(this, keyCode)) {
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	public void checkExternalStorage() {
		if (Build.VERSION.SDK_INT >= 19) {
			return;
		}
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// ok
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			AccessibleToast.makeText(this, R.string.sd_mounted_ro, Toast.LENGTH_LONG).show();
		} else {
			AccessibleToast.makeText(this, R.string.sd_unmounted, Toast.LENGTH_LONG).show();
		}
	}

	public void showAndHideMapPosition() {
		mapView.setShowMapPosition(true);
		app.runMessageInUIThreadAndCancelPrevious(SHOW_POSITION_MSG_ID, new Runnable() {
			@Override
			public void run() {
				if (mapView.isShowMapPosition()) {
					mapView.setShowMapPosition(false);
					mapView.refreshMap();
				}
			}
		}, 2500);
	}

	public OsmandMapTileView getMapView() {
		return mapView;
	}

	public MapViewTrackingUtilities getMapViewTrackingUtilities() {
		return mapViewTrackingUtilities;
	}

	public static MapViewTrackingUtilities getSingleMapViewTrackingUtilities() {
		return mapViewTrackingUtilities;
	}

	protected void parseLaunchIntentLocation() {
		Intent intent = getIntent();
		if (intent != null && intent.getData() != null) {
			Uri data = intent.getData();
			if ("http".equalsIgnoreCase(data.getScheme()) && data.getHost() != null && data.getHost().contains("osmand.net") &&
					data.getPath() != null && data.getPath().startsWith("/go")) {
				String lat = data.getQueryParameter("lat");
				String lon = data.getQueryParameter("lon");
				if (lat != null && lon != null) {
					try {
						double lt = Double.parseDouble(lat);
						double ln = Double.parseDouble(lon);
						String zoom = data.getQueryParameter("z");
						int z = settings.getLastKnownMapZoom();
						if (zoom != null) {
							z = Integer.parseInt(zoom);
						}
						settings.setMapLocationToShow(lt, ln, z, new PointDescription(
								PointDescription.POINT_TYPE_MARKER, getString(R.string.shared_location)));
					} catch (NumberFormatException e) {
						LOG.error("error", e);
					}
				}
			}
		}
	}

	public MapActivityActions getMapActions() {
		return mapActions;
	}

	public MapActivityLayers getMapLayers() {
		return mapLayers;
	}

	public static void launchMapActivityMoveToTop(Context activity) {
		if (activity instanceof MapActivity) {
			if (((MapActivity) activity).getDashboard().isVisible()) {
				((MapActivity) activity).getDashboard().hideDashboard();
			}
			((MapActivity) activity).readLocationToShow();
		} else {
			if (activity instanceof Activity) {
				Intent intent = ((Activity) activity).getIntent();
				if (intent != null) {
					prevActivityIntent = new Intent(intent);
					prevActivityIntent.putExtra(INTENT_KEY_PARENT_MAP_ACTIVITY, true);
				} else {
					prevActivityIntent = null;
				}
			} else {
				prevActivityIntent = null;
			}

			Intent newIntent = new Intent(activity, ((OsmandApplication) activity.getApplicationContext())
					.getAppCustomization().getMapActivity());
			newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			activity.startActivity(newIntent);
		}
	}

	public static void clearPrevActivityIntent() {
		prevActivityIntent = null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		OsmandPlugin.onMapActivityResult(requestCode, resultCode, data);
		MapControlsLayer mcl = mapView.getLayerByClass(MapControlsLayer.class);
		if (mcl != null) {
			mcl.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void refreshMap() {
		getMapView().refreshMap();
	}

	public View getLayout() {
		return getWindow().getDecorView().findViewById(android.R.id.content);
	}

	public DashboardOnMap getDashboard() {
		return dashboardOnMap;
	}

	public MapContextMenu getContextMenu() {
		return mapContextMenu;
	}

	public void openDrawer() {
		mapActions.updateDrawerMenu();
		drawerLayout.openDrawer(Gravity.LEFT);
	}

	public void disableDrawer() {
		drawerDisabled = true;
		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
	}

	public void enableDrawer() {
		drawerDisabled = false;
		drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
	}

	public boolean isDrawerDisabled() {
		return drawerDisabled;
	}

	public void closeDrawer() {
		drawerLayout.closeDrawer(Gravity.LEFT);
	}

	public void toggleDrawer() {
		if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
			closeDrawer();
		} else {
			openDrawer();
		}
	}

	// DownloadEvents
	@Override
	public void newDownloadIndexes() {
		WeakReference<MapContextMenuFragment> fragmentRef = getContextMenu().findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().newDownloadIndexes();
		}
		refreshMap();
	}

	@Override
	public void downloadInProgress() {
		WeakReference<MapContextMenuFragment> fragmentRef = getContextMenu().findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().downloadInProgress();
		}
	}

	@Override
	public void downloadHasFinished() {
		WeakReference<MapContextMenuFragment> fragmentRef = getContextMenu().findMenuFragment();
		if (fragmentRef != null) {
			fragmentRef.get().downloadHasFinished();
		}
		refreshMap();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		OsmandPlugin.onRequestPermissionsResult(requestCode, permissions, grantResults);

		MapControlsLayer mcl = mapView.getLayerByClass(MapControlsLayer.class);
		if (mcl != null) {
			mcl.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}

		if (requestCode == DownloadActivity.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE
				&& grantResults.length > 0 && permissions.length > 0
				&& Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[0])) {
			permissionAsked = true;
			permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
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

	private class ScreenOffReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			OsmandPlugin.onMapActivityScreenOff(MapActivity.this);
		}

	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
		RoutingHelper rh = app.getRoutingHelper();
		if (newRoute && rh.isRoutePlanningMode() && mapView != null) {
			Location lt = rh.getLastProjection();
			if (lt == null) {
				lt = app.getTargetPointsHelper().getPointToStartLocation();
			}
			if (lt != null) {
				double left = lt.getLongitude(), right = lt.getLongitude();
				double top = lt.getLatitude(), bottom = lt.getLatitude();
				List<Location> list = rh.getCurrentCalculatedRoute();
				for (Location l : list) {
					left = Math.min(left, l.getLongitude());
					right = Math.max(right, l.getLongitude());
					top = Math.max(top, l.getLatitude());
					bottom = Math.min(bottom, l.getLatitude());
				}
				List<TargetPoint> targetPoints = app.getTargetPointsHelper().getIntermediatePointsWithTarget();
				for (TargetPoint l : targetPoints) {
					left = Math.min(left, l.getLongitude());
					right = Math.max(right, l.getLongitude());
					top = Math.max(top, l.getLatitude());
					bottom = Math.min(bottom, l.getLatitude());
				}

				RotatedTileBox tb = mapView.getCurrentRotatedTileBox().copy();
				int tileBoxWidthPx = 0;
				int tileBoxHeightPx = 0;

				MapRouteInfoMenu routeInfoMenu = mapLayers.getMapControlsLayer().getMapRouteInfoMenu();
				WeakReference<MapRouteInfoMenuFragment> fragmentRef = routeInfoMenu.findMenuFragment();
				if (fragmentRef != null) {
					MapRouteInfoMenuFragment f = fragmentRef.get();
					if (landscapeLayout) {
						tileBoxWidthPx = tb.getPixWidth() - f.getWidth();
					} else {
						tileBoxHeightPx = tb.getPixHeight() - f.getHeight();
					}
				}
				mapView.fitRectToMap(left, right, top, bottom, tileBoxWidthPx, tileBoxHeightPx, 0);
			}
		}
	}

	@Override
	public void routeWasCancelled() {
	}

	@Override
	public void routeWasFinished() {
		DestinationReachedMenu.show(this);
	}
}
