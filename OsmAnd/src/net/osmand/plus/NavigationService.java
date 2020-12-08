package net.osmand.plus;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.plus.notifications.OsmandNotification;
import net.osmand.plus.settings.backend.OsmandSettings;

public class NavigationService extends Service implements LocationListener {

	public static class NavigationServiceBinder extends Binder {
	}

	// global id don't conflict with others
	public static int USED_BY_NAVIGATION = 1;
	public static int USED_BY_GPX = 2;
	public final static String USAGE_INTENT = "SERVICE_USED_BY";

	private final NavigationServiceBinder binder = new NavigationServiceBinder();

	private String serviceOffProvider;
	private OsmandSettings settings;

	protected int usedBy = 0;
	private OsmAndLocationProvider locationProvider;

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public int getUsedBy() {
		return usedBy;
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
		if (usedBy == 0) {
			final Intent serviceIntent = new Intent(ctx, NavigationService.class);
			ctx.stopService(serviceIntent);
		} else {
			final OsmandApplication app = (OsmandApplication) getApplication();
			app.getNotificationHelper().updateTopNotification();
			app.getNotificationHelper().refreshNotifications();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final OsmandApplication app = (OsmandApplication) getApplication();
		settings = app.getSettings();
		usedBy = intent.getIntExtra(USAGE_INTENT, 0);

		// use only gps provider
		serviceOffProvider = LocationManager.GPS_PROVIDER;
		locationProvider = app.getLocationProvider();
		app.setNavigationService(this);

		// request location updates
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		try {
			locationManager.requestLocationUpdates(serviceOffProvider, 0, 0, NavigationService.this);
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show();
			Log.d(PlatformUtil.TAG, "Location service permission not granted"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show();
			Log.d(PlatformUtil.TAG, "GPS location provider not available"); //$NON-NLS-1$
		}

		// registering icon at top level
		// Leave icon visible even for navigation for proper display
		Notification notification = app.getNotificationHelper().buildTopNotification();
		if (notification != null) {
			startForeground(OsmandNotification.TOP_NOTIFICATION_SERVICE_ID, notification);
			app.getNotificationHelper().refreshNotifications();
			return START_REDELIVER_INTENT;
		} else {
			notification = app.getNotificationHelper().buildErrorNotification();
			startForeground(OsmandNotification.TOP_NOTIFICATION_SERVICE_ID, notification);
			stopSelf();
			return START_NOT_STICKY;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// initializing variables
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		final OsmandApplication app = (OsmandApplication) getApplication();
		app.setNavigationService(null);
		usedBy = 0;
		// remove updates
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		try {
			locationManager.removeUpdates(this);
		} catch (SecurityException e) {
			Log.d(PlatformUtil.TAG, "Location service permission not granted"); //$NON-NLS-1$
		}
		// remove notification
		stopForeground(Boolean.TRUE);
		app.getNotificationHelper().updateTopNotification();
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				app.getNotificationHelper().refreshNotifications();
			}
		}, 500);
	}

	@Override
	public void onLocationChanged(Location l) {
		if (l != null && !settings.MAP_ACTIVITY_ENABLED.get()) {
			net.osmand.Location location = OsmAndLocationProvider.convertLocation(l, (OsmandApplication) getApplication());
			locationProvider.setLocationFromService(location);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(this, getString(R.string.off_router_service_no_gps_available), Toast.LENGTH_LONG).show();
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
		app.getNotificationHelper().removeNotifications(false);
		if (app.getNavigationService() != null &&
				app.getSettings().DISABLE_RECORDING_ONCE_APP_KILLED.get()) {
			NavigationService.this.stopSelf();
		}
	}
}
