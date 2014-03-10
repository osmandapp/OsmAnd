package net.osmand.plus;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.receivers.OnNavigationServiceAlarmReceiver;
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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class NavigationService extends Service implements LocationListener {

	public static class NavigationServiceBinder extends Binder {
		
	}
	// global id don't conflict with others
	private final static int NOTIFICATION_SERVICE_ID = 5;
	public final static String OSMAND_STOP_SERVICE_ACTION  = "OSMAND_STOP_SERVICE_ACTION"; //$NON-NLS-1$
	public final static String NAVIGATION_START_SERVICE_PARAM = "NAVIGATION_START_SERVICE_PARAM"; 
	
	private NavigationServiceBinder binder = new NavigationServiceBinder();

	
	private int serviceOffInterval;
	private String serviceOffProvider;
	private int serviceError;
	
	private OsmandSettings settings;
	
	private Handler handler;

	private static WakeLock lockStatic;
	private PendingIntent pendingIntent;
	private BroadcastReceiver broadcastReceiver;
	private boolean startedForNavigation;
	
	private static Method mStartForeground;
	private static Method mStopForeground;
	private static Method mSetForeground;
	private OsmAndLocationProvider locationProvider;

	private void checkForegroundAPI() {
		// check new API
		try {
			mStartForeground = getClass().getMethod("startForeground", new Class[] {int.class, Notification.class});
			mStopForeground = getClass().getMethod("stopForeground", new Class[] {boolean.class});
			Log.d(PlatformUtil.TAG, "startForeground and stopForeground available");
		} catch (NoSuchMethodException e) {
			mStartForeground = null;
			mStopForeground = null;
			Log.d(PlatformUtil.TAG, "startForeground and stopForeground not available");
		}

		// check old API
		try {
			mSetForeground = getClass().getMethod("setForeground", new Class[] {boolean.class});
			Log.d(PlatformUtil.TAG, "setForeground available");
		} catch (NoSuchMethodException e) {
			mSetForeground = null;
			Log.d(PlatformUtil.TAG, "setForeground not available");
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	public synchronized static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OsmandServiceLock");
		}
		return lockStatic;
	}

	public Handler getHandler() {
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
		OsmandApplication app = (OsmandApplication) getApplication();
		ClientContext cl = app;
		settings = cl.getSettings();
		
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
		
		
		locationProvider = app.getLocationProvider();
		app.setNavigationService(this);
		
		// requesting 
		if(isContinuous()){
			// request location updates
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			try {
				locationManager.requestLocationUpdates(serviceOffProvider, 0, 0, NavigationService.this);
			} catch (IllegalArgumentException e) {
				Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show();
				Log.d(PlatformUtil.TAG, "GPS location provider not available"); //$NON-NLS-1$
			}
		} else {
			AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
			pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, OnNavigationServiceAlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500, serviceOffInterval, pendingIntent);
		}
			
		// registering icon at top level
		// Leave icon visible even for navigation for proper display
//		if (!startedForNavigation) {
			showNotificationInStatusBar(cl);
//		}
		return START_REDELIVER_INTENT;
	}

	private void showNotificationInStatusBar(ClientContext cl) {
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
		notification.setLatestEventInfo(this, Version.getAppName(cl), getString(R.string.service_stop_background_service),
				PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (mStartForeground != null) {
			Log.d(PlatformUtil.TAG, "invoke startForeground");
			try {
				mStartForeground.invoke(this, NOTIFICATION_SERVICE_ID, notification);
			} catch (InvocationTargetException e) {
				Log.d(PlatformUtil.TAG, "invoke startForeground failed");
			} catch (IllegalAccessException e) {
				Log.d(PlatformUtil.TAG, "invoke startForeground failed");
			}
		} else {
			Log.d(PlatformUtil.TAG, "invoke setForeground");
			mNotificationManager.notify(NOTIFICATION_SERVICE_ID, notification);
			try {
				mSetForeground.invoke(this, Boolean.TRUE);
			} catch (InvocationTargetException e) {
				Log.d(PlatformUtil.TAG, "invoke setForeground failed");
			} catch (IllegalAccessException e) {
				Log.d(PlatformUtil.TAG, "invoke setForeground failed");
			}
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		// initializing variables
		checkForegroundAPI();
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
		removeNotification();
	}

	private void removeNotification() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_SERVICE_ID);
		if (broadcastReceiver != null) {
			unregisterReceiver(broadcastReceiver);
			broadcastReceiver = null;
		}

		if (mStopForeground != null) {
			Log.d(PlatformUtil.TAG, "invoke stopForeground");
			try {
				mStopForeground.invoke(this, Boolean.TRUE);
			} catch (InvocationTargetException e) {
				Log.d(PlatformUtil.TAG, "invoke stopForeground failed");
			} catch (IllegalAccessException e) {
				Log.d(PlatformUtil.TAG, "invoke stopForeground failed");
			}
		}
		else {
			Log.d(PlatformUtil.TAG, "invoke setForeground");
			try {
				mSetForeground.invoke(this, Boolean.FALSE);
			} catch (InvocationTargetException e) {
				Log.d(PlatformUtil.TAG, "invoke setForeground failed");
			} catch (IllegalAccessException e) {
				Log.d(PlatformUtil.TAG, "invoke setForeground failed");
			}
		}
	}

	@Override
	public void onLocationChanged(Location l) {
		if(l != null && !settings.MAP_ACTIVITY_ENABLED.get()){
			net.osmand.Location location = OsmAndLocationProvider.convertLocation(l,(OsmandApplication) getApplication());
			if(!isContinuous()){
				// unregister listener and wait next time
				LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
				locationManager.removeUpdates(this);
				WakeLock lock = getLock(this);
				if (lock.isHeld()) {
					lock.release();
				}
			}
			locationProvider.setLocationFromService(location, isContinuous());
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
