package net.osmand.plus;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import static net.osmand.plus.OsmAndLocationProvider.NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS;
import static net.osmand.plus.OsmAndLocationProvider.isRunningOnEmulator;
import static net.osmand.plus.notifications.OsmandNotification.TOP_NOTIFICATION_SERVICE_ID;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.auto.NavigationCarAppService;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.helpers.LocationCallback;
import net.osmand.plus.helpers.LocationServiceHelper;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.LocationSource;

import org.apache.commons.logging.Log;

import java.util.List;

public class NavigationService extends Service {

	public static final Log LOG = PlatformUtil.getLog(NavigationService.class);

	public static class NavigationServiceBinder extends Binder {
	}

	public static final String DEEP_LINK_ACTION_OPEN_ROOT_SCREEN = "net.osmand.plus.navigation.car.OpenRootScreen";

	// global id don't conflict with others
	public static int USED_BY_NAVIGATION = 1;
	public static int USED_BY_GPX = 2;
	public static final String USAGE_INTENT = "SERVICE_USED_BY";

	private final NavigationServiceBinder binder = new NavigationServiceBinder();

	private OsmandSettings settings;
	private RoutingHelper routingHelper;

	protected int usedBy;
	private OsmAndLocationProvider locationProvider;
	private LocationServiceHelper locationServiceHelper;
	private StateChangedListener<LocationSource> locationSourceListener;
	private long lastTimeGPSLocationFixed;

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	private OsmandApplication getApp() {
		return (OsmandApplication) getApplication();
	}

	public int getUsedBy() {
		return usedBy;
	}

	public boolean isUsed() {
		return usedBy != 0;
	}

	public boolean isUsedBy(int type) {
		return (usedBy & type) == type;
	}

	private void onServiceChanged(boolean start) {
		OsmandApplication app = getApp();
		NavigationSession carSession = app.getCarNavigationSession();
		if (start) {
			if (carSession != null) {
				if (isUsedBy(USED_BY_NAVIGATION)) {
					carSession.startCarNavigation();
				}
				if (routingHelper.isRouteCalculated() && routingHelper.isPauseNavigation()) {
					routingHelper.resumeNavigation();
				}
			}
		} else {
			if (!isUsedBy(USED_BY_NAVIGATION)) {
				if (carSession != null) {
					carSession.stopCarNavigation();
				}
			}
		}
		app.getNotificationHelper().refreshNotifications();
	}

	public void addUsageIntent(int usageIntent) {
		usedBy |= usageIntent;
		onServiceChanged(true);
	}

	public void stopIfNeeded(@NonNull Context context, int usageIntent) {
		LOG.info(">>>> NavigationService stopIfNeeded = " + usageIntent);
		OsmandApplication app = getApp();
		if ((usedBy & usageIntent) > 0) {
			usedBy -= usageIntent;
		}
		onServiceChanged(false);
		if (usedBy == 0) {
			context.stopService(new Intent(context, NavigationService.class));
		} else {
			app.getNotificationHelper().updateTopNotification();
			app.runInUIThread(() -> app.getNotificationHelper().refreshNotifications(), 500);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LOG.info(">>>> NavigationService onStartCommand");
		if (isUsed()) {
			LOG.info(">>>> NavigationService is used by = " + usedBy);
			addUsageIntent(intent.getIntExtra(USAGE_INTENT, 0));
			return START_REDELIVER_INTENT;
		}

		OsmandApplication app = getApp();
		settings = app.getSettings();
		routingHelper = app.getRoutingHelper();
		usedBy = intent.getIntExtra(USAGE_INTENT, 0);

		locationProvider = app.getLocationProvider();
		locationServiceHelper = app.createLocationServiceHelper();
		app.setNavigationService(this);

		Notification notification = app.getNotificationHelper().buildTopNotification(this,
				isUsedBy(USED_BY_NAVIGATION) ? NotificationType.NAVIGATION : NotificationType.GPX);
		boolean hasNotification = notification != null;
		if (hasNotification) {
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
					startForeground(TOP_NOTIFICATION_SERVICE_ID, notification, FOREGROUND_SERVICE_TYPE_LOCATION);
				} else {
					startForeground(TOP_NOTIFICATION_SERVICE_ID, notification);
				}
			} catch (Exception e) {
				app.setNavigationService(null);
				LOG.error("Failed to start NavigationService (usedBy=" + usedBy + ")", e);
				usedBy = 0;
				return START_NOT_STICKY;
			}
			try {
				onServiceChanged(true);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		} else {
			LOG.error("NavigationService could not be started because the notification is null. usedBy=" + usedBy);
			stopSelf();
			return START_NOT_STICKY;
		}
		requestLocationUpdates();
		return START_REDELIVER_INTENT;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		addLocationSourceListener();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		OsmandApplication app = getApp();
		app.setNavigationService(null);
		usedBy = 0;
		removeLocationUpdates();
		removeLocationSourceListener();

		LOG.info(">>>> NavigationService onDestroy");
		// remove notification
		stopForeground(STOP_FOREGROUND_REMOVE);
		app.getNotificationHelper().resetTopNotification();
		app.runInUIThread(() -> app.getNotificationHelper().refreshNotifications(), 500);
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		LOG.info(">>>> NavigationService onTaskRemoved");
		OsmandApplication app = getApp();
		app.getNotificationHelper().removeNotifications(false);
		if (app.getNavigationService() != null && app.getSettings().DISABLE_RECORDING_ONCE_APP_KILLED.get()) {
			stopSelf();
		}
	}

	private void addLocationSourceListener() {
		OsmandApplication app = getApp();
		locationSourceListener = change -> {
			removeLocationUpdates();
			locationServiceHelper = app.createLocationServiceHelper();
			requestLocationUpdates();
		};
		app.getSettings().LOCATION_SOURCE.addListener(locationSourceListener);
	}

	private void removeLocationSourceListener() {
		getApp().getSettings().LOCATION_SOURCE.removeListener(locationSourceListener);
	}

	private void requestLocationUpdates() {
		try {
			LOG.info(">>>> requestLocationUpdates from NavigationService");

			locationServiceHelper.requestLocationUpdates(new LocationCallback() {
				@Override
				public void onLocationResult(@NonNull List<net.osmand.Location> locations) {
					if (!locations.isEmpty()) {
						Location location = locations.get(locations.size() - 1);
						NavigationCarAppService navigationCarAppService = getApp().getNavigationCarAppService();
						lastTimeGPSLocationFixed = System.currentTimeMillis();
						if (!settings.MAP_ACTIVITY_ENABLED && navigationCarAppService == null) {
							locationProvider.setLocationFromService(location);
						}
					}
				}
			});
			// try to always ask for network provide : it is faster way to find location
			if (locationServiceHelper.isNetworkLocationUpdatesSupported()) {
				locationServiceHelper.requestNetworkLocationUpdates(new LocationCallback() {
					@Override
					public void onLocationResult(@NonNull List<net.osmand.Location> locations) {
						NavigationCarAppService navigationCarAppService = getApp().getNavigationCarAppService();
						if (!settings.MAP_ACTIVITY_ENABLED && navigationCarAppService == null && !locations.isEmpty() && !useOnlyGPS()) {
							locationProvider.setLocationFromService(locations.get(locations.size() - 1));
						}
					}
				});
			}
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show();
		}
	}

	private void removeLocationUpdates() {
		LOG.info(">>>> removeLocationUpdates from NavigationService");
		if (locationServiceHelper != null) {
			try {
				locationServiceHelper.removeLocationUpdates();
			} catch (SecurityException e) {
				// Location service permission not granted
			} finally {
				lastTimeGPSLocationFixed = 0;
			}
		}
	}

	private boolean useOnlyGPS() {
		if (routingHelper.isFollowingMode()) {
			return true;
		}
		if (lastTimeGPSLocationFixed > 0 && (System.currentTimeMillis() - lastTimeGPSLocationFixed) < NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS) {
			return true;
		}
		return isRunningOnEmulator();
	}
}
