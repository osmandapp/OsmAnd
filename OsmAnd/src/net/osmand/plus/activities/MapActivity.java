package net.osmand.plus.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.access.AccessibilityPlugin;
import net.osmand.access.AccessibleActivity;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.access.NavigationInfo;
import net.osmand.data.MapTileDownloader.DownloadRequest;
import net.osmand.data.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.map.IMapLocationListener;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;
import net.osmand.plus.BusyIndicator;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.routing.RouteAnimation;
import net.osmand.plus.routing.RouteProvider.GPXRouteParams;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.AnimateDraggingMapThread;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.PointLocationLayer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;
import android.widget.Toast;

public class MapActivity extends AccessibleActivity implements IMapLocationListener, SensorEventListener {
	
	// stupid error but anyway hero 2.1 : always lost gps signal (temporarily unavailable) for timeout = 2000
	private static final int GPS_TIMEOUT_REQUEST = 0;
	private static final int GPS_DIST_REQUEST = 0;
	// use only gps (not network) for 12 seconds 
	private static final int USE_ONLY_GPS_INTERVAL = 12000; 
	
	private static final int SHOW_POSITION_MSG_ID = 7;
	private static final int SHOW_POSITION_DELAY = 2500;
	public static final float ACCURACY_FOR_GPX_AND_ROUTING = 50;
	
	private static final int AUTO_FOLLOW_MSG_ID = 8; 
	private static final int LOST_LOCATION_MSG_ID = 10;
	private static final long LOST_LOCATION_CHECK_DELAY = 18000;
	
	private static final int LONG_KEYPRESS_MSG_ID = 28;
	private static final int LONG_KEYPRESS_DELAY = 500;
	
	private long lastTimeAutoZooming = 0;
	private long lastTimeSensorRotation = 0;
	private long lastTimeGPSLocationFixed = 0;
	
    /** Called when the activity is first created. */
	private OsmandMapTileView mapView;
	private MapActivityActions mapActions;
	private MapActivityLayers mapLayers;
	private NavigationInfo navigationInfo;
	
	private SavingTrackHelper savingTrackHelper;
	private LiveMonitoringHelper liveMonitoringHelper;
	private RoutingHelper routingHelper;
	
	private boolean sensorRegistered = false;
	private float previousSensorValue = 0;

	// Notification status
	private NotificationManager mNotificationManager;
	private int APP_NOTIFICATION_ID;
	// handler to show/hide trackball position and to link map with delay
	private Handler uiHandler = new Handler();
	// Current screen orientation
	private int currentScreenOrientation;
	// 
	private Dialog progressDlg = null;
	// App settings
	private OsmandSettings settings;

	private RouteAnimation routeAnimation = new RouteAnimation();

	private boolean isMapLinkedToLocation = false;
	private ProgressDialog startProgressDialog;
	private List<DialogProvider> dialogProviders = new ArrayList<DialogProvider>(2);
	
	private Notification getNotification(){
		Intent notificationIndent = new Intent(this, OsmandIntents.getMapActivity());
		notificationIndent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		Notification notification = new Notification(R.drawable.icon, "", //$NON-NLS-1$
				System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.setLatestEventInfo(this, Version.getAppName(this),
				getString(R.string.go_back_to_osmand), PendingIntent.getActivity(
						this, 0, notificationIndent,
						PendingIntent.FLAG_UPDATE_CURRENT));
		return notification;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = getMyApplication().getSettings();	
		mapActions = new MapActivityActions(this);
		mapLayers = new MapActivityLayers(this);
		navigationInfo = new NavigationInfo(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE); 
		// Full screen is not used here
		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);
		startProgressDialog = new ProgressDialog(this);
		startProgressDialog.setCancelable(true);
		((OsmandApplication) getApplication()).checkApplicationIsBeingInitialized(this, startProgressDialog);
		parseLaunchIntentLocation();
		
		mapView = (OsmandMapTileView) findViewById(R.id.MapView);
		mapView.setTrackBallDelegate(new OsmandMapTileView.OnTrackBallListener(){
			@Override
			public boolean onTrackBallEvent(MotionEvent e) {
				showAndHideMapPosition();
				return MapActivity.this.onTrackballEvent(e);
			}
		});
		
		// Do some action on close
		startProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				getMyApplication().getResourceManager().getRenderer().clearCache();
				mapView.refreshMap(true);
			}
		});
		
		getMyApplication().getResourceManager().getMapTileDownloader().addDownloaderCallback(new IMapDownloaderCallback(){
			@Override
			public void tileDownloaded(DownloadRequest request) {
				if(request != null && !request.error && request.fileToSave != null){
					ResourceManager mgr = getMyApplication().getResourceManager();
					mgr.tileDownloaded(request);
				}
				if(request == null || !request.error){
					mapView.tileDownloaded(request);
				}
			}
		});
		
				
		savingTrackHelper = getMyApplication().getSavingTrackHelper();
		liveMonitoringHelper = getMyApplication().getLiveMonitoringHelper();
		routingHelper = getMyApplication().getRoutingHelper();
		// This situtation could be when navigation suddenly crashed and after restarting
		// it tries to continue the last route
		if(settings.FOLLOW_THE_ROUTE.get() && !routingHelper.isRouteCalculated()){
			restoreRoutingMode(settings.getPointToNavigate(), settings.getIntermediatePoints());
		}
		
		mapView.setMapLocationListener(this);
		mapLayers.createLayers(mapView);
		
		if(!settings.isLastKnownMapLocation()){
			// show first time when application ran
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			Location location = null;
			for (String provider : service.getAllProviders()) {
				try {
					Location loc = service.getLastKnownLocation(provider);
					if (location == null) {
						location = loc;
					} else if (loc != null && location.getTime() < loc.getTime()) {
						location = loc;
					}
				} catch (IllegalArgumentException e) {
					Log.d(LogUtil.TAG, "Location provider not available"); //$NON-NLS-1$
				}
			}
			if(location != null){
				mapView.setLatLon(location.getLatitude(), location.getLongitude());
				mapView.setZoom(14);
			}
		}
		
		addDialogProvider(mapActions);
		OsmandPlugin.onMapActivityCreate(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		cancelNotification();
		
		if (settings.MAP_SCREEN_ORIENTATION.get() != getRequestedOrientation()) {
			setRequestedOrientation(settings.MAP_SCREEN_ORIENTATION.get());
			// can't return from this method we are not sure if activity will be recreated or not
		}
		mapLayers.getNavigationLayer().setPointToNavigate(settings.getPointToNavigate(), settings.getIntermediatePoints());
		
		Location loc = getLastKnownLocation();
		if (loc != null && (System.currentTimeMillis() - loc.getTime()) > 30 * 1000) {
			setLocation(null);
		}

		currentScreenOrientation = getWindow().getWindowManager().getDefaultDisplay().getOrientation();

		// for voice navigation
		if (settings.AUDIO_STREAM_GUIDANCE.get() != null) {
			setVolumeControlStream(settings.AUDIO_STREAM_GUIDANCE.get());
		} else {
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
		}

		updateApplicationModeSettings();

		mapLayers.getPoiMapLayer().setFilter(settings.getPoiFilterForMap((OsmandApplication) getApplication()));

		mapLayers.getMapInfoLayer().getBackToLocation().setEnabled(false);
		// by default turn off causing unexpected movements due to network establishing
		// best to show previous location
		setMapLinkedToLocation(false);

		// if destination point was changed try to recalculate route 
		if (routingHelper.isFollowingMode() && (
				!Algoritms.objectEquals(settings.getPointToNavigate(), routingHelper.getFinalLocation() )||
				!Algoritms.objectEquals(settings.getIntermediatePoints(), routingHelper.getIntermediatePoints())
				)) {
			routingHelper.setFinalAndCurrentLocation(settings.getPointToNavigate(),
					settings.getIntermediatePoints(),
					getLastKnownLocation(), routingHelper.getCurrentGPXRoute());
		}

		startLocationRequests();

		if (settings != null && settings.isLastKnownMapLocation()) {
			LatLon l = settings.getLastKnownMapLocation();
			mapView.setLatLon(l.getLatitude(), l.getLongitude());
			mapView.setZoom(settings.getLastKnownMapZoom());
		}

		settings.MAP_ACTIVITY_ENABLED.set(true);
		checkExternalStorage();
		showAndHideMapPosition();

		LatLon cur = new LatLon(mapView.getLatitude(), mapView.getLongitude());
		LatLon latLonToShow = settings.getAndClearMapLocationToShow();
		String mapLabelToShow = settings.getAndClearMapLabelToShow();
		Object toShow = settings.getAndClearObjectToShow();
		if(settings.isRouteToPointNavigateAndClear()){
			// always enable and follow and let calculate it (GPS is not accessible in garage)
			mapActions.getDirections(getLastKnownLocation(), true);
		}
		if(mapLabelToShow != null && latLonToShow != null){
			mapLayers.getContextMenuLayer().setSelectedObject(toShow);
			mapLayers.getContextMenuLayer().setLocation(latLonToShow, mapLabelToShow);
		}
		if (latLonToShow != null && !latLonToShow.equals(cur)) {
			mapView.getAnimatedDraggingThread().startMoving(latLonToShow.getLatitude(), latLonToShow.getLongitude(), 
					settings.getMapZoomToShow(), true);
			
		}

		View progress = mapLayers.getMapInfoLayer().getProgressBar();
		if (progress != null) {
			getMyApplication().getResourceManager().setBusyIndicator(new BusyIndicator(this, progress));
		}

		OsmandPlugin.onMapActivityResume(this);
		getMyApplication().getDaynightHelper().onMapResume();
		mapView.refreshMap(true);
	}

	public void startLocationRequests() {
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		try {
			service.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, gpsListener);
		} catch (IllegalArgumentException e) {
			Log.d(LogUtil.TAG, "GPS location provider not available"); //$NON-NLS-1$
		}
		// try to always ask for network provide : it is faster way to find location
		try {
			service.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPS_TIMEOUT_REQUEST, GPS_DIST_REQUEST, networkListener);
		} catch (IllegalArgumentException e) {
			Log.d(LogUtil.TAG, "Network location provider not available"); //$NON-NLS-1$
		}
	}

	private void notRestoreRoutingMode(){
		boolean changed = settings.APPLICATION_MODE.set(settings.PREV_APPLICATION_MODE.get());
		updateApplicationModeSettings();
		routingHelper.clearCurrentRoute(null, new ArrayList<LatLon>());
		mapView.refreshMap(changed);	
	}

	private void restoreRoutingMode(final LatLon pointToNavigate, final List<LatLon> intermediates) {
		final String gpxPath = settings.FOLLOW_THE_GPX_ROUTE.get();
		if (pointToNavigate == null && gpxPath == null) {
			notRestoreRoutingMode();
		} else {
			Runnable encapsulate = new Runnable() {
				int delay = 7;
				boolean quit = false;
				Runnable delayDisplay = null;

				@Override
				public void run() {
					Builder builder = new AccessibleAlertBuilder(MapActivity.this);
					final TextView tv = new TextView(MapActivity.this);
					tv.setText(getString(R.string.continue_follow_previous_route_auto, delay + ""));
					tv.setPadding(7, 5, 7, 5);
					builder.setView(tv);
					builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							quit = true;
							restoreRoutingModeInner();

						}
					});
					builder.setNegativeButton(R.string.default_buttons_no, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							quit = true;
							notRestoreRoutingMode();
						}
					});
					final AlertDialog dlg = builder.show();
					dlg.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							quit = true;
						}
					});
					dlg.setOnCancelListener(new OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							quit = true;
						}
					});
					delayDisplay = new Runnable() {
						@Override
						public void run() {
							if(!quit) {
								delay --;
								tv.setText(getString(R.string.continue_follow_previous_route_auto, delay + ""));
								if(delay <= 0) {
									if(dlg.isShowing()) {
										dlg.dismiss();
									}
									quit = true;
									restoreRoutingModeInner();
								} else {
									uiHandler.postDelayed(delayDisplay, 1000);
								}
							}
						}
					};
					delayDisplay.run();
				}

				private void restoreRoutingModeInner() {
					AsyncTask<String, Void, GPXFile> task = new AsyncTask<String, Void, GPXFile>() {
						@Override
						protected GPXFile doInBackground(String... params) {
							if (gpxPath != null) {
								// Reverse also should be stored ?
								GPXFile f = GPXUtilities.loadGPXFile(MapActivity.this, new File(gpxPath), false);
								if (f.warning != null) {
									return null;
								}
								return f;
							} else {
								return null;
							}
						}

						@Override
						protected void onPostExecute(GPXFile result) {
							final GPXRouteParams gpxRoute = result == null ? null : new GPXRouteParams(result, false, settings);
							LatLon endPoint = pointToNavigate != null ? pointToNavigate : gpxRoute.getLastPoint();
							Location startPoint = gpxRoute == null ? null : gpxRoute.getStartPointForRoute();
							if (endPoint == null) {
								notRestoreRoutingMode();
							} else {
								routingHelper.setFollowingMode(true);
								routingHelper.setFinalAndCurrentLocation(endPoint, intermediates, startPoint, gpxRoute);
								getMyApplication().showDialogInitializingCommandPlayer(MapActivity.this);
							}
						}
					};
					task.execute(gpxPath);

				}
			};
			encapsulate.run();
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
		if (id == OsmandApplication.PROGRESS_DIALOG) {
			return startProgressDialog;
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

	public void changeZoom(float newZoom){
		newZoom = Math.round(newZoom * OsmandMapTileView.ZOOM_DELTA) * OsmandMapTileView.ZOOM_DELTA_1;
		boolean changeLocation = settings.AUTO_ZOOM_MAP.get();
		mapView.getAnimatedDraggingThread().startZooming(newZoom, changeLocation);
		if (getMyApplication().accessibilityEnabled())
			AccessibleToast.makeText(this, getString(R.string.zoomIs) + " " + String.valueOf(newZoom), Toast.LENGTH_SHORT).show(); //$NON-NLS-1$
		showAndHideMapPosition();
	}

   
	public void backToMainMenu() {
		final Dialog dlg = new Dialog(this, R.style.Dialog_Fullscreen);
		final View menuView = (View) getLayoutInflater().inflate(R.layout.menu, null);
		menuView.setBackgroundColor(Color.argb(200, 150, 150, 150));
		dlg.setContentView(menuView);
		MainMenuActivity.onCreateMainMenu(dlg.getWindow(), this);
		Animation anim = new Animation() {
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				ColorDrawable colorDraw = ((ColorDrawable) menuView.getBackground());
				colorDraw.setAlpha((int) (interpolatedTime * 200));
			}
		};
		anim.setDuration(700);
		anim.setInterpolator(new AccelerateInterpolator());
		menuView.setAnimation(anim);

		View showMap = dlg.findViewById(R.id.MapButton);
		showMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dlg.dismiss();
			}
		});
		View settingsButton = dlg.findViewById(R.id.SettingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent settings = new Intent(MapActivity.this, OsmandIntents.getSettingsActivity());
				MapActivity.this.startActivity(settings);
				dlg.dismiss();
			}
		});

		View favouritesButton = dlg.findViewById(R.id.FavoritesButton);
		favouritesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent favorites = new Intent(MapActivity.this, OsmandIntents.getFavoritesActivity());
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				MapActivity.this.startActivity(favorites);
				dlg.dismiss();
			}
		});

		View closeButton = dlg.findViewById(R.id.CloseButton);
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dlg.dismiss();

				getMyApplication().closeApplication();
				// 1. Work for almost all cases when user open apps from main menu
				Intent newIntent = new Intent(MapActivity.this, OsmandIntents.getMainMenuActivity());
				newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				newIntent.putExtra(MainMenuActivity.APP_EXIT_KEY, MainMenuActivity.APP_EXIT_CODE);
				startActivity(newIntent);
				// 2. good analogue but user will come back to the current activity onResume()
				// so application is not reloaded !!!
				// moveTaskToBack(true);
				// 3. bad results if user comes from favorites
				// MapActivity.this.setResult(MainMenuActivity.APP_EXIT_CODE);
				// MapActivity.this.finish();
			}
		});

		View searchButton = dlg.findViewById(R.id.SearchButton);
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent search = new Intent(MapActivity.this, OsmandIntents.getSearchActivity());
				LatLon loc = getMapLocation();
				search.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
				search.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
				// causes wrong position caching:  search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				search.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				MapActivity.this.startActivity(search);
				dlg.dismiss();
			}
		});
		menuView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dlg.dismiss();
			}
		});

		dlg.show();
		// Intent newIntent = new Intent(MapActivity.this, MainMenuActivity.class);
		// newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// startActivity(newIntent);
	}
 
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			//some application/hardware needs that back button reacts on key up, so
			//that they could do some key combinations with it...
			// Victor : doing in that way doesn't close dialog properly!
			//return true;
		} else if (getMyApplication().accessibilityEnabled() && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER)) {
			if (!uiHandler.hasMessages(LONG_KEYPRESS_MSG_ID)) {
				Message msg = Message.obtain(uiHandler, new Runnable() {
						@Override
						public void run() {
							emitNavigationHint();
						}
					});
				msg.what = LONG_KEYPRESS_MSG_ID;
				uiHandler.sendMessageDelayed(msg, LONG_KEYPRESS_DELAY);
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
			Intent newIntent = new Intent(MapActivity.this, OsmandIntents.getSearchActivity());
			// causes wrong position caching:  newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			LatLon loc = getMapLocation();
			newIntent.putExtra(SearchActivity.SEARCH_LAT, loc.getLatitude());
			newIntent.putExtra(SearchActivity.SEARCH_LON, loc.getLongitude());
			startActivity(newIntent);
			newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			return true;
		} else if (!routingHelper.isFollowingMode() && OsmandPlugin.getEnabledPlugin(AccessibilityPlugin.class) != null) {
			// Find more appropriate plugin for it?
			if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && event.getRepeatCount() == 0) {
				if (mapView.isZooming()) {
					changeZoom(mapView.getZoom() + 2);
				} else {
					changeZoom(mapView.getZoom() + 1);
				}
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.getRepeatCount() == 0) {
				changeZoom(mapView.getZoom() - 1);
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

    public String getNavigationHint(LatLon point) {
        String hint = navigationInfo.getDirectionString(point, mapLayers.getLocationLayer().getHeading());
        if (hint == null)
            hint = getString(R.string.no_info);
        return hint;
    }

    private void emitNavigationHint() {
        final LatLon point = settings.getPointToNavigate();
        if (point != null) {
            if (routingHelper.isRouteCalculated()) {
                routingHelper.getVoiceRouter().announceCurrentDirection(getLastKnownLocation());
            } else {
                AccessibleToast.makeText(this, getNavigationHint(point), Toast.LENGTH_LONG).show();
            }
        } else {
            AccessibleToast.makeText(this, R.string.mark_final_location_first, Toast.LENGTH_SHORT).show();
        }
    }

	public void setMapLocation(double lat, double lon){
		mapView.setLatLon(lat, lon);
		locationChanged(lat, lon, this);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_MOVE && settings.USE_TRACKBALL_FOR_MOVEMENTS.get()){
			float x = event.getX();
			float y = event.getY();
			LatLon l = mapView.getLatLonFromScreenPoint(mapView.getCenterPointX() + x * 15, mapView.getCenterPointY() + y * 15);
			setMapLocation(l.getLatitude(), l.getLongitude());
			return true;
		}
		return super.onTrackballEvent(event);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	protected void setProgressDlg(Dialog progressDlg) {
		this.progressDlg = progressDlg;
	}

	protected Dialog getProgressDlg() {
		return progressDlg;
	}

	@Override
	protected void onStop() {
		if(routingHelper.isFollowingMode()){
			mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mNotificationManager.notify(APP_NOTIFICATION_ID, getNotification());
		}
		if(progressDlg != null){
			progressDlg.dismiss();
			progressDlg = null;
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		OsmandPlugin.onMapActivityDestroy(this);
		savingTrackHelper.close();
		routeAnimation.close();
		cancelNotification();
		getMyApplication().getResourceManager().getMapTileDownloader().removeDownloaderCallback(mapView);
	}

	private void cancelNotification() {
		if(mNotificationManager == null){
			mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		}
		mNotificationManager.cancel(APP_NOTIFICATION_ID);
	}



	private void registerUnregisterSensor(Location location, boolean overruleRegister){
		boolean currentShowingAngle = settings.SHOW_VIEW_ANGLE.get(); 
		int currentMapRotation = settings.ROTATE_MAP.get();
		boolean show = overruleRegister || (currentShowingAngle && location != null) || currentMapRotation == OsmandSettings.ROTATE_MAP_COMPASS;
		// show point view only if gps enabled
		if (sensorRegistered && !show) {
			Log.d(LogUtil.TAG, "Disable sensor"); //$NON-NLS-1$
			((SensorManager) getSystemService(SENSOR_SERVICE)).unregisterListener(this);
			sensorRegistered = false;
			previousSensorValue = 0;
			mapLayers.getLocationLayer().setHeading(null);
		} else if (!sensorRegistered && show) {
			Log.d(LogUtil.TAG, "Enable sensor"); //$NON-NLS-1$
			SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
			Sensor s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			if (s != null) {
				if(!sensorMgr.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL)) {
					Log.e(LogUtil.TAG, "Sensor could not be enabled");
				}
			}
			sensorRegistered = true;
		}
	}

	public void backToLocationImpl() {
		mapLayers.getMapInfoLayer().getBackToLocation().setEnabled(false);
		PointLocationLayer locationLayer = mapLayers.getLocationLayer();
		if(!isMapLinkedToLocation()){
			setMapLinkedToLocation(true);
			if(locationLayer.getLastKnownLocation() != null){
				Location lastKnownLocation = locationLayer.getLastKnownLocation();
				AnimateDraggingMapThread thread = mapView.getAnimatedDraggingThread();
				float fZoom = mapView.getFloatZoom() < 13 ? 13 : mapView.getFloatZoom();
				thread.startMoving( lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), fZoom, false);
			}
		}
		if(locationLayer.getLastKnownLocation() == null){
			AccessibleToast.makeText(this, R.string.unknown_location, Toast.LENGTH_LONG).show();
		}
	}

	// location not null!
	private void updateSpeedBearingEmulator(Location location) {
		// For network/gps it's bad way (not accurate). It's widely used for testing purposes
		// possibly keep using only for emulator case
		PointLocationLayer locationLayer = mapLayers.getLocationLayer();
		if (locationLayer.getLastKnownLocation() != null) {
			if (locationLayer.getLastKnownLocation().distanceTo(location) > 3) {
				float d = location.distanceTo(locationLayer.getLastKnownLocation());
				long time = location.getTime() - locationLayer.getLastKnownLocation().getTime();
				float speed;
				if (time == 0) {
					speed = 0;
				} else {
					speed = ((float) d * 1000) / time ;
				}
				// Be aware only for emulator ! code is incorrect in case of airplane
				if (speed > 100) {
					speed = 100;
				}
				location.setSpeed(speed);
			}
		}
		if(locationLayer.getLastKnownLocation() != null && !location.hasBearing()){
			if(locationLayer.getLastKnownLocation().distanceTo(location) > 10 && !isRunningOnEmulator()){
				// very innacurate
				// location.setBearing(locationLayer.getLastKnownLocation().bearingTo(location));
			}
		}
	}
	
	public boolean isPointAccurateForRouting(Location loc) {
		return loc != null && loc.getAccuracy() < ACCURACY_FOR_GPX_AND_ROUTING * 3 /2;
	}

	public void setLocation(Location location) {
		if (Log.isLoggable(LogUtil.TAG, Log.DEBUG)) {
			Log.d(LogUtil.TAG, "Location changed " + location.getProvider()); //$NON-NLS-1$
		}
		// 1. Logging services
		if (location != null) {
			// use because there is a bug on some devices with location.getTime()
			long locationTime = System.currentTimeMillis();
			// write only with 50 meters accuracy
			if (!location.hasAccuracy() || location.getAccuracy() < ACCURACY_FOR_GPX_AND_ROUTING) {
				if (settings.SAVE_TRACK_TO_GPX.get()) {
					savingTrackHelper.insertData(location.getLatitude(), location.getLongitude(), location.getAltitude(),
							location.getSpeed(), location.getAccuracy(), locationTime, settings);
				}
				// live monitoring is aware of accuracy (it would be good to create an option)
				if (settings.LIVE_MONITORING.get()) {
					liveMonitoringHelper.insertData(location.getLatitude(), location.getLongitude(), location.getAltitude(),
							location.getSpeed(), location.getAccuracy(), location.getTime(), settings);
				}

			}

		}
		
		if(location != null && isRunningOnEmulator()) {
			// only for emulator
			updateSpeedBearingEmulator(location);
		}
		// 2. accessibility routing
		navigationInfo.setLocation(location);

		// 3. routing
		boolean enableSensorNavigation = routingHelper.isFollowingMode() && settings.USE_COMPASS_IN_NAVIGATION.get() ? location == null
				|| !location.hasBearing() : false;
		registerUnregisterSensor(location, enableSensorNavigation);
		Location updatedLocation = location;
		if (routingHelper.isFollowingMode()) {
			if (location == null || !location.hasAccuracy() || location.getAccuracy() < ACCURACY_FOR_GPX_AND_ROUTING) {
				// Update routing position and get location for sticking mode
				updatedLocation = routingHelper.setCurrentLocation(location, settings.SNAP_TO_ROAD.get());
				if(!routingHelper.isFollowingMode()) {
					// finished
					Message msg = Message.obtain(uiHandler, new Runnable() {
						@Override
						public void run() {
							settings.APPLICATION_MODE.set(settings.PREV_APPLICATION_MODE.get());
							updateApplicationModeSettings();
						}
					});
					uiHandler.sendMessage(msg);
				}
				// Check with delay that gps location is not lost
				if (location != null && routingHelper.getLeftDistance() > 0) {
					final long fixTime = location.getTime();
					Message msg = Message.obtain(uiHandler, new Runnable() {
						@Override
						public void run() {
							Location lastKnown = getLastKnownLocation();
							if (lastKnown != null && lastKnown.getTime() - fixTime < LOST_LOCATION_CHECK_DELAY / 2) {
								// false positive case, still strange how we got here with removeMessages
								return;
							}
							if (routingHelper.getLeftDistance() > 0 && settings.MAP_ACTIVITY_ENABLED.get()) {
								routingHelper.getVoiceRouter().gpsLocationLost();
							}
						}
					});
					msg.what = LOST_LOCATION_MSG_ID;
					uiHandler.removeMessages(LOST_LOCATION_MSG_ID);
					uiHandler.sendMessageDelayed(msg, LOST_LOCATION_CHECK_DELAY);
				}
			}
		}
		

		// Update information 
		mapLayers.getLocationLayer().setLastKnownLocation(updatedLocation);
		if (updatedLocation != null) {
			updateAutoMapViewConfiguration(updatedLocation);
		} else {
			if (mapLayers.getMapInfoLayer().getBackToLocation().isEnabled()) {
				mapLayers.getMapInfoLayer().getBackToLocation().setEnabled(false);
			}
		}
		
		// When location is changed we need to refresh map in order to show movement!
		mapView.refreshMap();
	}

	private void updateAutoMapViewConfiguration(Location location) {
		long now = System.currentTimeMillis();
		if (isMapLinkedToLocation()) {
			if (settings.AUTO_ZOOM_MAP.get() && location.hasSpeed()) {
				float zdelta = defineZoomFromSpeed(location.getSpeed());
				if (Math.abs(zdelta) >= OsmandMapTileView.ZOOM_DELTA_1) {
					// prevent ui hysteresis (check time interval for autozoom)
					if (zdelta >= 2) {
						// decrease a bit
						zdelta -= 3 * OsmandMapTileView.ZOOM_DELTA_1;
					} else if (zdelta <= -2) {
						// decrease a bit
						zdelta += 3 * OsmandMapTileView.ZOOM_DELTA_1;
					}
					if (now - lastTimeAutoZooming > 4500) {
						lastTimeAutoZooming = now;
						mapView.setZoom(mapView.getFloatZoom() + zdelta);
						// mapView.getAnimatedDraggingThread().startZooming(mapView.getFloatZoom() + zdelta, false);
					}
				}
			}
			int currentMapRotation = settings.ROTATE_MAP.get();
			if (currentMapRotation == OsmandSettings.ROTATE_MAP_BEARING) {
				if (location.hasBearing()) {
					if(location.getBearing() != 0f) {
						mapView.setRotate(-location.getBearing());
					}
				} else if (routingHelper.isFollowingMode() && settings.USE_COMPASS_IN_NAVIGATION.get()) {
					if (previousSensorValue != 0 && Math.abs(MapUtils.degreesDiff(mapView.getRotate(), -previousSensorValue)) > 15) {
						if(now - lastTimeSensorRotation > 1500 && now - lastTimeSensorRotation < 15000) {
							lastTimeSensorRotation = now;
							mapView.setRotate(-previousSensorValue);
						}
					}
				}
			}
			mapView.setLatLon(location.getLatitude(), location.getLongitude());
		} else {
			if (!mapLayers.getMapInfoLayer().getBackToLocation().isEnabled()) {
				mapLayers.getMapInfoLayer().getBackToLocation().setEnabled(true);
			}
			if (settings.AUTO_FOLLOW_ROUTE.get() > 0 && routingHelper.isFollowingMode() && !uiHandler.hasMessages(AUTO_FOLLOW_MSG_ID)) {
				backToLocationWithDelay(1);
			}
		}
	}

	public float defineZoomFromSpeed(float speed) {
		if (speed < 7f / 3.6) {
			return 0;
		}
		double topLat = mapView.calcLatitude(-mapView.getCenterPointY());
		double cLat = mapView.calcLatitude(0);
		double visibleDist = MapUtils.getDistance(cLat, mapView.getLongitude(), topLat, mapView.getLongitude());
		float time = 75f;
		if (speed < 83f / 3.6) {
			time = 60f;
		}
		double distToSee = speed * time;
		float zoomDelta = (float) (Math.log(visibleDist / distToSee) / Math.log(2.0f));
		zoomDelta = Math.round(zoomDelta * OsmandMapTileView.ZOOM_DELTA) * OsmandMapTileView.ZOOM_DELTA_1;
		// check if 17, 18 is correct?
		if(zoomDelta + mapView.getFloatZoom() > 18 - OsmandMapTileView.ZOOM_DELTA_1) {
			return 18 - OsmandMapTileView.ZOOM_DELTA_1 - mapView.getFloatZoom();
		}
		return zoomDelta;
	}

	public void navigateToPoint(LatLon point, boolean updateRoute, boolean intermediate){
		if(point != null){
			if(!intermediate) {
				settings.setPointToNavigate(point.getLatitude(), point.getLongitude(), null);
			} else {
				int sz = mapLayers.getNavigationLayer().getIntermediatePoints().size();
				settings.setIntermediatePoint(point.getLatitude(), point.getLongitude(), null, sz);
			}
		} else {
			settings.clearPointToNavigate();
			settings.clearIntermediatePoints();
		}
		if(updateRoute && ( routingHelper.isRouteBeingCalculated() || routingHelper.isRouteCalculated() ||
				routingHelper.isFollowingMode())) {
			routingHelper.setFinalAndCurrentLocation(settings.getPointToNavigate(),
					settings.getIntermediatePoints(), getLastKnownLocation(), routingHelper.getCurrentGPXRoute());
		}
		mapLayers.getNavigationLayer().setPointToNavigate(settings.getPointToNavigate(), settings.getIntermediatePoints());
	}
	
	public Location getLastKnownLocation(){
		return mapLayers.getLocationLayer().getLastKnownLocation();
	}
	
	public LatLon getMapLocation(){
		return new LatLon(mapView.getLatitude(), mapView.getLongitude());
	}
	
	public LatLon getPointToNavigate(){
		return mapLayers.getNavigationLayer().getPointToNavigate();
	}
	
	public List<LatLon> getIntermediatePoitns(){
		return mapLayers.getNavigationLayer().getIntermediatePoints();
	}
	
	public LatLon getFirstIntermediatePoint(){
		List<LatLon> ip = mapLayers.getNavigationLayer().getIntermediatePoints();
		if(ip.size() > 0) {
			return ip.get(0);
		}
		return null;
	}
	
	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}
	
	private boolean isRunningOnEmulator(){
		if (Build.DEVICE.equals("generic")) { //$NON-NLS-1$ 
			return true;
		}  
		return false;
	}
	
	private boolean useOnlyGPS() {
		return (routingHelper != null && routingHelper.isFollowingMode())
				|| (System.currentTimeMillis() - lastTimeGPSLocationFixed) < USE_ONLY_GPS_INTERVAL || isRunningOnEmulator();
	}


	// Working with location listeners
	private LocationListener networkListener = new LocationListener(){
		
		@Override
		public void onLocationChanged(Location location) {
			// double check about use only gps
			// that strange situation but it could happen?
			if(!useOnlyGPS()){
				setLocation(location);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			if(!useOnlyGPS()){
				setLocation(null);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if(LocationProvider.OUT_OF_SERVICE == status && !useOnlyGPS()){
				setLocation(null);
			}
		}
	};
	
	
	private LocationListener gpsListener = new LocationListener(){
		@Override
		public void onLocationChanged(Location location) {
			if (location != null) {
				lastTimeGPSLocationFixed = location.getTime();
			}
			setLocation(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
			if (!useOnlyGPS() && service.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				Location loc = service.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
				if(loc != null && (System.currentTimeMillis() - loc.getTime()) < USE_ONLY_GPS_INTERVAL){
					setLocation(loc);
				}
			} else {
				setLocation(null);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (LocationProvider.TEMPORARILY_UNAVAILABLE == status) {
				if(routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0){
					// Suppress gpsLocationLost() prompt here for now, as it causes duplicate announcement and then also prompts when signal is found again
					//routingHelper.getVoiceRouter().gpsLocationLost();
				}
			} else if (LocationProvider.OUT_OF_SERVICE == status) {
				if(routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0){
					routingHelper.getVoiceRouter().gpsLocationLost();
				}
			} else if (LocationProvider.AVAILABLE == status) {
				// Do not remove right now network listener
				// service.removeUpdates(networkListener);
			}

		}
	};
	
	public LocationListener getGpsListener() {
		return gpsListener;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		stopLocationRequests();
		
		SensorManager sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensorMgr.unregisterListener(this);
		sensorRegistered = false;
		
		getMyApplication().getDaynightHelper().onMapPause();
		
		settings.setLastKnownMapLocation((float) mapView.getLatitude(), (float) mapView.getLongitude());
		AnimateDraggingMapThread animatedThread = mapView.getAnimatedDraggingThread();
		if(animatedThread.isAnimating() && animatedThread.getTargetZoom() != 0){
			settings.setMapLocationToShow(animatedThread.getTargetLatitude(), animatedThread.getTargetLongitude(), 
					(int) animatedThread.getTargetZoom());
		}
		
		settings.setLastKnownMapZoom(mapView.getZoom());
		settings.MAP_ACTIVITY_ENABLED.set(false);
		getMyApplication().getResourceManager().interruptRendering();
		getMyApplication().getResourceManager().setBusyIndicator(null);
		OsmandPlugin.onMapActivityPause(this);
	}

	public void stopLocationRequests() {
		LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
		service.removeUpdates(gpsListener);
		service.removeUpdates(networkListener);
	}
	
	public void updateApplicationModeSettings(){
		int currentMapRotation = settings.ROTATE_MAP.get();
		if(currentMapRotation == OsmandSettings.ROTATE_MAP_NONE){
			mapView.setRotate(0);
		}
		routingHelper.setAppMode(settings.getApplicationMode());
		// mapView.setMapPosition(settings.POSITION_ON_MAP.get());
		mapView.setMapPosition(settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING ? OsmandSettings.BOTTOM_CONSTANT : 
			 OsmandSettings.CENTER_CONSTANT);
		registerUnregisterSensor(getLastKnownLocation(), false);
		mapLayers.getMapInfoLayer().recreateControls();
		mapLayers.updateLayers(mapView);
		
		getMyApplication().getDaynightHelper().setDayNightMode(settings.DAYNIGHT_MODE.get());
	}
	
	
	public void switchRotateMapMode(){
		int vl = (settings.ROTATE_MAP.get() + 1) % 3;
		settings.ROTATE_MAP.set(vl);
		registerUnregisterSensor(getLastKnownLocation(), false);
		if(settings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_COMPASS){
			mapView.setRotate(0);
		}
		int resId = R.string.rotate_map_none_opt;
		if(settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS){
			resId = R.string.rotate_map_compass_opt;
		} else if(settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING){
			resId = R.string.rotate_map_bearing_opt;
		}
		mapView.setMapPosition(settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_BEARING ? OsmandSettings.BOTTOM_CONSTANT : 
			 OsmandSettings.CENTER_CONSTANT);
		
		AccessibleToast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show();
		mapView.refreshMap();
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			//some application/hardware needs that back button reacts on key up, so
			//that they could do some key combinations with it...
			// Android 1.6 doesn't have onBackPressed() method it should be finish instead!
   			//onBackPressed();
			//return true;
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			if (!getMyApplication().accessibilityEnabled()) {
				mapActions.contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
			} else if (uiHandler.hasMessages(LONG_KEYPRESS_MSG_ID)) {
				uiHandler.removeMessages(LONG_KEYPRESS_MSG_ID);
				mapActions.contextMenuPoint(mapView.getLatitude(), mapView.getLongitude());
			}
			return true;
		} else if (settings.ZOOM_BY_TRACKBALL.get()) {
			// Parrot device has only dpad left and right
			if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
				changeZoom(mapView.getZoom() - 1);
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
				changeZoom(mapView.getZoom() + 1);
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || 
				keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||keyCode == KeyEvent.KEYCODE_DPAD_DOWN || 
				keyCode == KeyEvent.KEYCODE_DPAD_UP) {
			int dx = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ? 15 : (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? - 15 : 0);
			int dy = keyCode == KeyEvent.KEYCODE_DPAD_DOWN ? 15 : (keyCode == KeyEvent.KEYCODE_DPAD_UP ? -15 : 0);
			LatLon l = mapView.getLatLonFromScreenPoint(mapView.getCenterPointX() + dx, mapView.getCenterPointY() + dy);
			setMapLocation(l.getLatitude(), l.getLongitude());
			return true;
		}
		return super.onKeyUp(keyCode,event);
	}
	
	public void checkExternalStorage(){
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state)){
			// ok
		} else if(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)){
			AccessibleToast.makeText(this, R.string.sd_mounted_ro, Toast.LENGTH_LONG).show();
		} else {
			AccessibleToast.makeText(this, R.string.sd_unmounted, Toast.LENGTH_LONG).show();
		}
	}
	
	
	public void showAndHideMapPosition() {
		mapView.setShowMapPosition(true);
		Message msg = Message.obtain(uiHandler, new Runnable() {
			@Override
			public void run() {
				if (mapView.isShowMapPosition()) {
					mapView.setShowMapPosition(false);
					mapView.refreshMap();
				}
			}

		});
		msg.what = SHOW_POSITION_MSG_ID;
		uiHandler.removeMessages(SHOW_POSITION_MSG_ID);
		uiHandler.sendMessageDelayed(msg, SHOW_POSITION_DELAY);
	}
	
	

	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		// when user start dragging 
		if(mapLayers.getLocationLayer().getLastKnownLocation() != null){
			setMapLinkedToLocation(false);
			if (!mapLayers.getMapInfoLayer().getBackToLocation().isEnabled()) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mapLayers.getMapInfoLayer().getBackToLocation().setEnabled(true);
					}
				});
			}
		}
	}
	
	public OsmandMapTileView getMapView() {
		return mapView;
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		// Attention : sensor produces a lot of events & can hang the system
		float val = event.values[0];
		if(currentScreenOrientation == 1){
			val += 90;
		}
		previousSensorValue = val;
		if (settings.ROTATE_MAP.get() == OsmandSettings.ROTATE_MAP_COMPASS) {
			if(Math.abs(MapUtils.degreesDiff(mapView.getRotate(), -val)) > 15) {
				mapView.setRotate(-val);
			}
		}
		if(settings.SHOW_VIEW_ANGLE.get().booleanValue()){
			if(mapLayers.getLocationLayer().getHeading() == null || Math.abs(mapLayers.getLocationLayer().getHeading() - val) > 10){
				mapLayers.getLocationLayer().setHeading(val);
			}
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return mapActions.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean val = super.onPrepareOptionsMenu(menu);
		mapActions.onPrepareOptionsMenu(menu);
		return val;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return mapActions.onOptionsItemSelected(item) == true ? true : super.onOptionsItemSelected(item);
	}
	

	protected void parseLaunchIntentLocation(){
   	 	Intent intent = getIntent();
		if (intent != null && intent.getData() != null) {
			Uri data = intent.getData();
			if ("http".equalsIgnoreCase(data.getScheme()) && "download.osmand.net".equals(data.getHost()) && "/go".equals(data.getPath())) {
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
						settings.setMapLocationToShow(lt, ln, z, getString(R.string.shared_location));
					} catch (NumberFormatException e) {
					}
				}
			}
		}
	}
	
	public FavouritesDbHelper getFavoritesHelper() {
		return getMyApplication().getFavorites();
	}
	
	public MapActivityActions getMapActions() {
		return mapActions;
	}

	public MapActivityLayers getMapLayers() {
		return mapLayers;
	}

	public SavingTrackHelper getSavingTrackHelper() {
		return savingTrackHelper;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}
	
	public static void launchMapActivityMoveToTop(Activity activity){
		Intent newIntent = new Intent(activity, OsmandIntents.getMapActivity());
		newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		activity.startActivity(newIntent);
	}

	
	private boolean isMapLinkedToLocation(){
		return isMapLinkedToLocation;
	}
	
	
	public void setMapLinkedToLocation(boolean isMapLinkedToLocation) {
		if(!isMapLinkedToLocation){
			int autoFollow = settings.AUTO_FOLLOW_ROUTE.get();
			if(autoFollow > 0 && routingHelper.isFollowingMode()){
				backToLocationWithDelay(autoFollow);
			}
		}
		this.isMapLinkedToLocation = isMapLinkedToLocation;
	}

	private void backToLocationWithDelay(int delay) {
		uiHandler.removeMessages(AUTO_FOLLOW_MSG_ID);
		Message msg = Message.obtain(uiHandler, new Runnable() {
			@Override
			public void run() {
				if (settings.MAP_ACTIVITY_ENABLED.get()) {
					AccessibleToast.makeText(MapActivity.this, R.string.auto_follow_location_enabled, Toast.LENGTH_SHORT).show();
					backToLocationImpl();
				}
			}
		});
		msg.what = AUTO_FOLLOW_MSG_ID;
		uiHandler.sendMessageDelayed(msg, delay * 1000);
	}

	public NavigationInfo getNavigationInfo() {
		return navigationInfo;
	}

}
