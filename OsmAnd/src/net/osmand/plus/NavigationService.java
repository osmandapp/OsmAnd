package net.osmand.plus;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.osmo.OsMoPlugin;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.Toast;

public class NavigationService extends Service implements LocationListener {

	public static class NavigationServiceBinder extends Binder {

	}

	// global id don't conflict with others
	public static int USED_BY_NAVIGATION = 1;
	public static int USED_BY_GPX = 2;
	public static int USED_BY_LIVE = 4;
	public static int USED_BY_WAKE_UP = 8;
	public final static String USAGE_INTENT = "SERVICE_USED_BY";

	private NavigationServiceBinder binder = new NavigationServiceBinder();


	private int serviceOffInterval;
	private String serviceOffProvider;
	private int serviceError;
	private OsmandSettings settings;
	private Handler handler;

	private static WakeLock lockStatic;
	private PendingIntent pendingIntent;
	
	protected int usedBy = 0;
	private OsmAndLocationProvider locationProvider;

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
	
	public int getUsedBy() {
		return usedBy;
	}

	public String getServiceOffProvider() {
		return serviceOffProvider;
	}

	public boolean isUsed() {
		return usedBy != 0;
	}

	public void addUsageIntent(int usageIntent) {
		usedBy |= usageIntent;
	}

	public void stopIfNeeded(Context ctx, int usageIntent) {
		if ((usedBy & usageIntent) > 0) {
			usedBy -= usageIntent;
		}

		if (usedBy == 2) {
			//reset SERVICE_OFF_INTERVAL to automatic settings for USED_BY_GPX
			if (settings.SAVE_GLOBAL_TRACK_INTERVAL.get() < 30000) {
				settings.SERVICE_OFF_INTERVAL.set(0);
			} else {
				//Use SERVICE_OFF_INTERVAL > 0 to conserve power for longer GPX recording intervals
				settings.SERVICE_OFF_INTERVAL.set(settings.SAVE_GLOBAL_TRACK_INTERVAL.get());
			}
		}

		if (usedBy == 0) {
			final Intent serviceIntent = new Intent(ctx, NavigationService.class);
			ctx.stopService(serviceIntent);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handler = new Handler();
		final OsmandApplication app = (OsmandApplication) getApplication();
		settings = app.getSettings();
		usedBy = intent.getIntExtra(USAGE_INTENT, 0);
		if ((usedBy & USED_BY_NAVIGATION) != 0) {
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
		if (isContinuous()) {
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
		Builder ntf = app.getNotificationHelper().buildNotificationInStatusBar();
		if (ntf != null) {
			startForeground(NotificationHelper.NOTIFICATION_SERVICE_ID, ntf.build());
		}
		return START_REDELIVER_INTENT;
	}
	
	protected void stopService() {
		if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
			settings.SAVE_GLOBAL_TRACK_TO_GPX.set(false);
		}
		OsMoPlugin osmoPlugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
		if (osmoPlugin != null) {
			if (osmoPlugin.getTracker().isEnabledTracker()) {
				osmoPlugin.getTracker().disableTracker();
			}
		}
		OsmandMonitoringPlugin monitoringPlugin =
				OsmandPlugin.getEnabledPlugin(OsmandMonitoringPlugin.class);
		if (monitoringPlugin != null) {
			monitoringPlugin.stopRecording();
		}
		NavigationService.this.stopSelf();
	}

	
	

	@Override
	public void onCreate() {
		super.onCreate();
		// initializing variables
	}

	private boolean isContinuous() {
		return serviceOffInterval == 0;
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		((OsmandApplication) getApplication()).setNavigationService(null);
		usedBy = 0;
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
		((OsmandApplication) getApplication()).getNotificationHelper().removeServiceNotification();
		stopForeground(Boolean.TRUE);
	}

	@Override
	public void onLocationChanged(Location l) {
		if (l != null && !settings.MAP_ACTIVITY_ENABLED.get()) {
			net.osmand.Location location = OsmAndLocationProvider.convertLocation(l, (OsmandApplication) getApplication());
			if (!isContinuous()) {
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

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		OsmandApplication app = ((OsmandApplication) getApplication());
		if (app.getNavigationService() != null &&
				app.getSettings().DISABLE_RECORDING_ONCE_APP_KILLED.get()) {
			OsMoPlugin plugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
			if (plugin != null) {
				if (plugin.getTracker().isEnabledTracker()) {
					plugin.getTracker().disableTracker();
				}
			}
			app.getNotificationHelper().removeServiceNotificationCompletely();
			NavigationService.this.stopSelf();
		}
	}
}
