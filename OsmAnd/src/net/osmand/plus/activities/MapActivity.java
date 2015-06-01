package net.osmand.plus.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.osmand.Location;
import net.osmand.StateChangedListener;
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
import net.osmand.plus.helpers.GpxImportHelper;
import net.osmand.plus.helpers.WakeLockHelper;
import net.osmand.plus.poi.PoiLegacyFilter;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
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
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MapActivity extends AccessibleActivity {

	private static final int SHOW_POSITION_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 1;
	private static final int LONG_KEYPRESS_MSG_ID = OsmAndConstants.UI_HANDLER_MAP_VIEW + 2;
	private static final int LONG_KEYPRESS_DELAY = 500;

	private static MapViewTrackingUtilities mapViewTrackingUtilities;

	/** Called when the activity is first created. */
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

	boolean firstTime;

	private Dialog progressDlg = null;

	private List<DialogProvider> dialogProviders = new ArrayList<DialogProvider>(2);
	private StateChangedListener<ApplicationMode> applicationModeListener;
	private GpxImportHelper gpxImportHelper;
	private WakeLockHelper wakeLockHelper;
	private boolean intentLocation = false;

	private DashboardOnMap dashboardOnMap = new DashboardOnMap(this);
	private AppInitializeListener initListener;

	private Notification getNotification() {
		Intent notificationIndent = new Intent(this, getMyApplication().getAppCustomization().getMapActivity());
		notificationIndent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Notification notification = new Notification(R.drawable.icon, "", //$NON-NLS-1$
				System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(this, Version.getAppName(app), getString(R.string.go_back_to_osmand),
				PendingIntent.getActivity(this, 0, notificationIndent, PendingIntent.FLAG_UPDATE_CURRENT));
		return notification;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		long tm = System.currentTimeMillis();
		app = getMyApplication();
		settings = app.getSettings();
		app.applyTheme(this);
		supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		// Full screen is not used here
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);

		mapView = new OsmandMapTileView(this, getWindow().getDecorView().getWidth(),
				getWindow().getDecorView().getHeight());
		app.getAppInitializer().checkAppVersionChanged(this);
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

		app.getResourceManager().getMapTileDownloader().addDownloaderCallback(new IMapDownloaderCallback() {
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
		});
		createProgressBarForRouting();
		mapLayers.createLayers(mapView);
		// This situtation could be when navigation suddenly crashed and after restarting
		// it tries to continue the last route
		if (settings.FOLLOW_THE_ROUTE.get() && !app.getRoutingHelper().isRouteCalculated()
				&& !app.getRoutingHelper().isRouteBeingCalculated()) {
			FailSafeFuntions.restoreRoutingMode(this);
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
		if(System.currentTimeMillis() - tm > 50) {
			System.err.println("OnCreate for MapActivity took " + (System.currentTimeMillis() - tm) + " ms");
		}
		mapView.refreshMap(true);
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
					if(event == InitEvents.NATIVE_INITIALIZED) {
						setupOpenGLView(false);
						openGlSetup = true;
					}
					if(event == InitEvents.MAPS_INITIALIZED) {
						// TODO investigate if this false cause any issues!
						mapView.refreshMap(false);
						if(dashboardOnMap != null) {
							dashboardOnMap.updateLocation(true, true, false);
						}
					}
				}

				@Override
				public void onFinish(AppInitializer init) {
					if(!openGlSetup) {
						setupOpenGLView(false);
					}
					mapView.refreshMap(false);
					if(dashboardOnMap != null) {
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
		app.getRoutingHelper().setProgressBar(new RouteCalculationProgressCallback() {

			@Override
			public void updateProgress(int progress) {
				pb.setVisibility(View.VISIBLE);
				pb.setProgress(progress);
				pb.requestLayout();
			}

			@Override
			public void finish() {
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
		LinkedHashMap<String, Object> l = new LinkedHashMap<String, Object>();
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
	public void onBackPressed() {
		if(dashboardOnMap.onBackPressed()) {
			return;
		}
		super.onBackPressed();
	}

	@Override
	protected void onResume() {
		super.onResume();
		long tm = System.currentTimeMillis();
		if (app.isApplicationInitializing() || DashboardOnMap.staticVisible) {
			if(!dashboardOnMap.isVisible()) {
				dashboardOnMap.setDashboardVisibility(true, DashboardOnMap.staticVisibleType);
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
						.objectEquals(targets.getIntermediatePointsLatLon(), routingHelper.getIntermediatePoints()))) {
			targets.updateRouteAndReferesh(true);
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

		OsmandPlugin.onMapActivityResume(this);
		mapView.refreshMap(true);
		if (atlasMapRendererView != null) {
			atlasMapRendererView.handleOnResume();
		}
		getMyApplication().getAppCustomization().resumeActivity(MapActivity.class, this);
		if(System.currentTimeMillis() - tm > 50) {
			System.err.println("OnCreate for MapActivity took " + (System.currentTimeMillis() - tm) + " ms");
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
				getMapActions().enterRoutePlanningMode(null, null, false);
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
			getMapActions().enterRoutePlanningMode(null, null, status == OsmandSettings.NAVIGATE_CURRENT_GPX);
			if(dashboardOnMap.isVisible()) {
				dashboardOnMap.hideDashboard();
			}
		}
		if (latLonToShow != null) {
			if(dashboardOnMap.isVisible()) {
				dashboardOnMap.hideDashboard();
			}
			if (mapLabelToShow != null) {
				mapLayers.getContextMenuLayer().setSelectedObject(toShow);
				mapLayers.getContextMenuLayer().setLocation(latLonToShow,
						mapLabelToShow.getFullPlainName(this));
			}
			if (!latLonToShow.equals(cur)) {
				mapView.getAnimatedDraggingThread().startMoving(latLonToShow.getLatitude(),
						latLonToShow.getLongitude(), settings.getMapZoomToShow(), true);
			}
			// remember if map should come back to isMapLinkedToLocation=true
			mapViewTrackingUtilities.setMapLinkedToLocation(false);
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
		if(newZoom < 1) {
			AccessibleToast.makeText(this, R.string.edit_tilesource_minzoom, Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
			return;
		}
		mapView.getAnimatedDraggingThread().startZooming(newZoom, zoomFrac, changeLocation);
		if (app.accessibilityEnabled())
			AccessibleToast.makeText(this, getString(R.string.zoomIs) + " " + newZoom, Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
		showAndHideMapPosition();
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
		} else if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0) {
			dashboardOnMap.onMenuPressed();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
			Intent newIntent = new Intent(MapActivity.this, getMyApplication().getAppCustomization()
					.getSearchActivity());
			// causes wrong position caching: newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			LatLon loc = getMapLocation();
			newIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
			newIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
			if(mapViewTrackingUtilities.isMapLinkedToLocation()) {
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
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
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
		if(mapView == null) {
			return settings.getLastKnownMapLocation();
		}
		return new LatLon(mapView.getLatitude(), mapView.getLongitude());
	}
	
	public float getMapRotate() {
		if(mapView == null) {
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
		app.getRoutingHelper().setAppMode(settings.getApplicationMode());
		if (mapLayers.getMapInfoLayer() != null) {
			mapLayers.getMapInfoLayer().recreateControls();
		}
		mapLayers.updateLayers(mapView);
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
		if(app.isApplicationInitializing()) {
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
			protected void onPostExecute(Void result) {};
		}.execute((Void) null);
		
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
		if(Build.VERSION.SDK_INT >= 19) {
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
			Intent newIntent = new Intent(activity, ((OsmandApplication) activity.getApplicationContext())
					.getAppCustomization().getMapActivity());
			newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			activity.startActivity(newIntent);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		OsmandPlugin.onMapActivityResult(requestCode, resultCode, data);
		MapControlsLayer mcl = mapView.getLayerByClass(MapControlsLayer.class);
		if(mcl != null) {
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
}
