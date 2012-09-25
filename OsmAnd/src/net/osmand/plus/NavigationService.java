package net.osmand.plus;


import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.activities.LiveMonitoringHelper;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.routing.RoutingHelper;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class NavigationService extends Service implements LocationListener {

	public static class NavigationServiceBinder extends Binder {
		
	}
	private final static int NOTIFICATION_SERVICE_ID = 1;
	public final static String OSMAND_STOP_SERVICE_ACTION  = "OSMAND_STOP_SERVICE_ACTION"; //$NON-NLS-1$
	public final static String NAVIGATION_START_SERVICE_PARAM = "NAVIGATION_START_SERVICE_PARAM"; 
	private static final int LOST_LOCATION_MSG_ID = 10;
	private static final long LOST_LOCATION_CHECK_DELAY = 20000;
	
	private NavigationServiceBinder binder = new NavigationServiceBinder();

	
	private int serviceOffInterval;
	private String serviceOffProvider;
	private int serviceError;
	
	private SavingTrackHelper savingTrackHelper;
	private RoutingHelper routingHelper;
	private OsmandSettings settings;
	
	private Handler handler;

	private static WakeLock lockStatic;
	private PendingIntent pendingIntent;
	private BroadcastReceiver broadcastReceiver;
	private LiveMonitoringHelper liveMonitoringHelper;
	private boolean startedForNavigation;
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	protected synchronized static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OsmandServiceLock");
		}
		return lockStatic;
	}

	protected Handler getHandler() {
		return handler;
	}
	
	public int getServiceError() {
		return serviceError;
	}
	
	public int getServiceOffInterval() {
		return serviceOffInterval;
	}
	
	public String getServiceOffProvider() {
		return serviceOffProvider;
	}
	
	public boolean startedForNavigation(){
		return startedForNavigation;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handler = new Handler();
		settings = ((OsmandApplication) getApplication()).getSettings();
		
		startedForNavigation = intent.getBooleanExtra(NAVIGATION_START_SERVICE_PARAM, false);
		if (startedForNavigation) {
			serviceOffInterval = 0;
		} else {
			serviceOffInterval = settings.SERVICE_OFF_INTERVAL.get();
		}
		// use only gps provider
		serviceOffProvider = LocationManager.GPS_PROVIDER;
		serviceError = serviceOffInterval / 5;
		// 1. not more than 12 mins
		serviceError = Math.min(serviceError, 12 * 60 * 1000);
		// 2. not less than 30 seconds
		serviceError = Math.max(serviceError, 30 * 1000);
		// 3. not more than serviceOffInterval
		serviceError = Math.min(serviceError, serviceOffInterval);
		
		
		savingTrackHelper = ((OsmandApplication) getApplication()).getSavingTrackHelper();
		liveMonitoringHelper = ((OsmandApplication) getApplication()).getLiveMonitoringHelper();
		
		routingHelper = ((OsmandApplication)getApplication()).getRoutingHelper();
		((OsmandApplication)getApplication()).setNavigationService(this);
		
		// requesting 
		if(isContinuous()){
			// request location updates
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			try {
				locationManager.requestLocationUpdates(serviceOffProvider, 0, 0, NavigationService.this);
			} catch (IllegalArgumentException e) {
				Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show();
				Log.d(LogUtil.TAG, "GPS location provider not available"); //$NON-NLS-1$
			}
		} else {
			AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, OnNavigationServiceAlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500, serviceOffInterval, pendingIntent);
		}
			
		// registering icon at top level
		// Leave icon visible even for navigation for proper testing
//		if (!startedForNavigation) {
			broadcastReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					NavigationService.this.stopSelf();
				}

			};
			registerReceiver(broadcastReceiver, new IntentFilter(OSMAND_STOP_SERVICE_ACTION));
			Intent notificationIntent = new Intent(OSMAND_STOP_SERVICE_ACTION);
			Notification notification = new Notification(R.drawable.bgs_icon, "", //$NON-NLS-1$
					System.currentTimeMillis());
			notification.flags = Notification.FLAG_NO_CLEAR;
			notification.setLatestEventInfo(this, Version.getAppName(this), getString(R.string.service_stop_background_service),
					PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT));
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			mNotificationManager.notify(NOTIFICATION_SERVICE_ID, notification);
//		}
		return START_REDELIVER_INTENT;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		// initializing variables
		setForeground(true);
	}
	
	private boolean isContinuous(){
		return serviceOffInterval == 0;
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		((OsmandApplication)getApplication()).setNavigationService(null);
		
		// remove updates
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		locationManager.removeUpdates(this);
		
		if (!isContinuous()) {
			WakeLock lock = getLock(this);
			if (lock.isHeld()) {
				lock.release();
			}
		}
		// remove alarm
		AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
		// remove notification
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_SERVICE_ID);
		if (broadcastReceiver != null) {
			unregisterReceiver(broadcastReceiver);
			broadcastReceiver = null;
		}
	}



	@Override
	public void onLocationChanged(Location location) {
		if(location != null && !settings.MAP_ACTIVITY_ENABLED.get()){
			if(!isContinuous()){
				// unregister listener and wait next time
				LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
				locationManager.removeUpdates(this);
				WakeLock lock = getLock(this);
				if (lock.isHeld()) {
					lock.release();
				}
			} else {
				// if continuous notify about lost location
				if (routingHelper.isFollowingMode() && routingHelper.getLeftDistance() > 0) {
					Message msg = Message.obtain(handler, new Runnable() {
    					@Override
    					public void run() {
							if (routingHelper.getLeftDistance() > 0 && !settings.MAP_ACTIVITY_ENABLED.get() &&
									!handler.hasMessages(LOST_LOCATION_MSG_ID)) {
								routingHelper.getVoiceRouter().gpsLocationLost();
							}
    					}
    				});
    				msg.what = LOST_LOCATION_MSG_ID;
    				handler.removeMessages(LOST_LOCATION_MSG_ID);
    				handler.sendMessageDelayed(msg, LOST_LOCATION_CHECK_DELAY);
				}
			}
			// use because there is a bug on some devices with location.getTime()
			long locationTime = System.currentTimeMillis();
			savingTrackHelper.insertData(location.getLatitude(), location.getLongitude(), location.getAltitude(),
					location.getSpeed(), location.getAccuracy(), locationTime, settings);
			liveMonitoringHelper.insertData(location.getLatitude(), location.getLongitude(), location.getAltitude(),
					location.getSpeed(), location.getAccuracy(), locationTime, settings);
			if(routingHelper.isFollowingMode()){
				routingHelper.setCurrentLocation(location, false);
			}
		}
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		AccessibleToast.makeText(this, getString(R.string.off_router_service_no_gps_available), Toast.LENGTH_LONG).show();
	}



	@Override
	public void onProviderEnabled(String provider) {
	}



	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	
}
