package net.osmand.plus;

import android.app.AlarmManager;
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
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.osmo.OsMoPlugin;

public class NavigationService extends Service implements LocationListener {

	public static class NavigationServiceBinder extends Binder {

	}

	// global id don't conflict with others
	private final static int NOTIFICATION_SERVICE_ID = 5;
	public final static String OSMAND_STOP_SERVICE_ACTION = "OSMAND_STOP_SERVICE_ACTION"; //$NON-NLS-1$
	public final static String OSMAND_SAVE_SERVICE_ACTION = "OSMAND_SAVE_SERVICE_ACTION";
	public static int USED_BY_NAVIGATION = 1;
	public static int USED_BY_GPX = 2;
	public static int USED_BY_LIVE = 4;
	public final static String USAGE_INTENT = "SERVICE_USED_BY";

	private NavigationServiceBinder binder = new NavigationServiceBinder();


	private int serviceOffInterval;
	private String serviceOffProvider;
	private int serviceError;

	private OsmandSettings settings;

	private Handler handler;

	private static WakeLock lockStatic;
	private PendingIntent pendingIntent;
	private BroadcastReceiver broadcastReceiver;
	private BroadcastReceiver saveBroadcastReceiver;
	private int usedBy = 0;
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
		OsmandApplication app = (OsmandApplication) getApplication();
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
//		if (!startedForNavigation) {
		showNotificationInStatusBar(app);
//		}
		return START_REDELIVER_INTENT;
	}

	private void showNotificationInStatusBar(OsmandApplication cl) {
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (settings.SAVE_GLOBAL_TRACK_TO_GPX.get()) {
					settings.SAVE_GLOBAL_TRACK_TO_GPX.set(false);
				}
				OsMoPlugin plugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
				if (plugin != null) {
					if (plugin.getTracker().isEnabledTracker()) {
						plugin.getTracker().disableTracker();
					}
				}
				NavigationService.this.stopSelf();
			}

		};
		registerReceiver(broadcastReceiver, new IntentFilter(OSMAND_STOP_SERVICE_ACTION));
		saveBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				final OsmandMonitoringPlugin plugin = OsmandPlugin
						.getEnabledPlugin(OsmandMonitoringPlugin.class);
				plugin.saveCurrentTrack();
			}
		};
		registerReceiver(saveBroadcastReceiver, new IntentFilter(OSMAND_SAVE_SERVICE_ACTION));

		//Show currently active wake-up interval
		int soi = settings.SERVICE_OFF_INTERVAL.get();
		String nt = getString(R.string.service_stop_background_service) + ". " + getString(R.string.gps_wake_up_timer) + ": ";
		if (soi == 0) {
			nt = nt + getString(R.string.int_continuosly);
		} else if (soi <= 90000) {
			nt = nt + Integer.toString(soi / 1000) + " " + getString(R.string.int_seconds);
		} else {
			nt = nt + Integer.toString(soi / 1000 / 60) + " " + getString(R.string.int_min);
		}
//		Notification notification = new Notification(R.drawable.bgs_icon, "", //$NON-NLS-1$
//				System.currentTimeMillis());
//		
//		notification.setLatestEventInfo(this, Version.getAppName(cl) + " " + getString(R.string.osmand_service), nt,
//				broadcast);
//		notification.flags = Notification.FLAG_NO_CLEAR;
//		startForeground(NOTIFICATION_SERVICE_ID, notification);

		String stop = getResources().getString(R.string.shared_string_control_stop);
		Intent stopIntent = new Intent(OSMAND_STOP_SERVICE_ACTION);
		PendingIntent stopPendingIntent = PendingIntent.getBroadcast(this, 0, stopIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		String pause = getResources().getString(R.string.shared_string_save);
		Intent saveIntent = new Intent(OSMAND_SAVE_SERVICE_ACTION);
		PendingIntent savePendingIntent = PendingIntent.getBroadcast(this, 0, saveIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		Intent contentIntent = new Intent(this, MapActivity.class);
		PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, contentIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		final Builder notificationBuilder = new NotificationCompat.Builder(
				this).setContentTitle(Version.getAppName(cl))
				.setContentText(getString(R.string.osmand_service))
				.setSmallIcon(R.drawable.bgs_icon)
//			.setLargeIcon(Helpers.getBitmap(R.drawable.mirakel, getBaseContext()))
				.setContentIntent(contentPendingIntent)
				.setOngoing(true)
				.addAction(R.drawable.ic_action_rec_stop, stop, stopPendingIntent)
				.addAction(R.drawable.ic_action_save, pause, savePendingIntent);
		startForeground(NOTIFICATION_SERVICE_ID, notificationBuilder.build());
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
		removeNotification();
	}

	private void removeNotification() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationManager.cancel(NOTIFICATION_SERVICE_ID);
		if (broadcastReceiver != null) {
			unregisterReceiver(broadcastReceiver);
			broadcastReceiver = null;
		}
		if (saveBroadcastReceiver != null) {
			unregisterReceiver(saveBroadcastReceiver);
			saveBroadcastReceiver = null;
		}

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
		if (((OsmandApplication) getApplication()).getNavigationService() != null &&
				((OsmandApplication) getApplication()).getSettings().DISABLE_RECORDING_ONCE_APP_KILLED.get()) {
			OsMoPlugin plugin = OsmandPlugin.getEnabledPlugin(OsMoPlugin.class);
			if (plugin != null) {
				if (plugin.getTracker().isEnabledTracker()) {
					plugin.getTracker().disableTracker();
				}
			}
			NavigationService.this.stopSelf();
		}
	}
}
