package net.osmand.plus;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.NavigationManagerCallback;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;

import net.osmand.Location;
import net.osmand.StateChangedListener;
import net.osmand.plus.auto.screens.NavigationScreen;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.TripHelper;
import net.osmand.plus.helpers.LocationServiceHelper;
import net.osmand.plus.helpers.LocationCallback;
import net.osmand.plus.notifications.OsmandNotification;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.LocationSource;
import net.osmand.plus.simulation.OsmAndLocationSimulation;

import java.util.Collections;
import java.util.List;

public class NavigationService extends Service {

	public static class NavigationServiceBinder extends Binder {
	}

	public static final String DEEP_LINK_ACTION_OPEN_ROOT_SCREEN = "net.osmand.plus.navigation.car.OpenRootScreen";

	// global id don't conflict with others
	public static int USED_BY_NAVIGATION = 1;
	public static int USED_BY_GPX = 2;
	public static int USED_BY_CAR_APP = 4;
	public static final String USAGE_INTENT = "SERVICE_USED_BY";

	private final NavigationServiceBinder binder = new NavigationServiceBinder();

	private OsmandSettings settings;

	protected int usedBy;
	private OsmAndLocationProvider locationProvider;
	private LocationServiceHelper locationServiceHelper;
	private StateChangedListener<LocationSource> locationSourceListener;

	// Android Auto
	private CarContext carContext;
	private NavigationManager navigationManager;
	private boolean carNavigationActive;
	private TripHelper tripHelper;

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

	public boolean isUsedByNavigation() {
		return (usedBy & USED_BY_NAVIGATION) == USED_BY_NAVIGATION;
	}

	public void addUsageIntent(int usageIntent) {
		usedBy |= usageIntent;
	}

	public void stopIfNeeded(@NonNull Context context, int usageIntent) {
		if ((usedBy & usageIntent) > 0) {
			usedBy -= usageIntent;
		}
		if (!isUsedByNavigation()) {
			stopCarNavigation();
		}
		if (usageIntent == USED_BY_CAR_APP) {
			setCarContext(null);
		}
		if (usedBy == 0) {
			context.stopService(new Intent(context, NavigationService.class));
		} else {
			OsmandApplication app = getApp();
			app.getNotificationHelper().updateTopNotification();
			app.getNotificationHelper().refreshNotifications();
		}
	}

	private OsmandApplication getApp() {
		return (OsmandApplication) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		OsmandApplication app = getApp();
		settings = app.getSettings();
		usedBy = intent.getIntExtra(USAGE_INTENT, 0);

		locationProvider = app.getLocationProvider();
		locationServiceHelper = app.createLocationServiceHelper();
		app.setNavigationService(this);

		NavigationSession carNavigationSession = app.getCarNavigationSession();
		if (carNavigationSession != null) {
			setCarContext(carNavigationSession.getCarContext());
		}

		Notification notification = app.getNotificationHelper().buildTopNotification();
		if (notification != null) {
			if (isUsedByNavigation()) {
				startCarNavigation();
			}
			startForeground(OsmandNotification.TOP_NOTIFICATION_SERVICE_ID, notification);
			app.getNotificationHelper().refreshNotifications();
		} else {
			notification = app.getNotificationHelper().buildErrorNotification();
			startForeground(OsmandNotification.TOP_NOTIFICATION_SERVICE_ID, notification);
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
		setCarContext(null);
		app.setNavigationService(null);
		usedBy = 0;
		removeLocationUpdates();
		removeLocationSourceListener();

		// remove notification
		stopForeground(Boolean.TRUE);
		app.getNotificationHelper().updateTopNotification();
		app.runInUIThread(() -> app.getNotificationHelper().refreshNotifications(), 500);
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
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
		OsmandApplication app = getApp();
		try {
			locationServiceHelper.requestLocationUpdates(new LocationCallback() {
				@Override
				public void onLocationResult(@NonNull List<net.osmand.Location> locations) {
					if (!locations.isEmpty()) {
						Location location = locations.get(locations.size() - 1);
						NavigationSession carNavigationSession = app.getCarNavigationSession();
						boolean hasCarSurface = carNavigationSession != null && carNavigationSession.hasStarted();
						if (!settings.MAP_ACTIVITY_ENABLED.get() || hasCarSurface) {
							locationProvider.setLocationFromService(location);
						}
					}
				}

				@Override
				public void onLocationAvailability(boolean locationAvailable) {
				}
			});
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show();
		}
	}

	private void removeLocationUpdates() {
		if (locationServiceHelper != null) {
			try {
				locationServiceHelper.removeLocationUpdates();
			} catch (SecurityException e) {
				// Location service permission not granted
			}
		}
	}

	/** Sets the {@link CarContext} to use while the service is running. */
	public void setCarContext(@Nullable CarContext carContext) {
		this.carContext = carContext;
		if (carContext != null) {
			this.tripHelper = new TripHelper(getApp());
			this.navigationManager = carContext.getCarService(NavigationManager.class);
			this.navigationManager.setNavigationManagerCallback(
					new NavigationManagerCallback() {
						@Override
						public void onStopNavigation() {
							getApp().stopNavigation();
						}

						@Override
						public void onAutoDriveEnabled() {
							CarToast.makeText(carContext, "Auto drive enabled", CarToast.LENGTH_LONG).show();
							OsmAndLocationSimulation sim = getApp().getLocationProvider().getLocationSimulation();
							RoutingHelper helper = getApp().getRoutingHelper();
							if (!sim.isRouteAnimating() && helper.isFollowingMode()
									&& helper.isRouteCalculated() && !helper.isRouteBeingCalculated()) {
								sim.startStopRouteAnimation(null);
							}
						}
					});

			// Uncomment if navigating
			// mNavigationManager.navigationStarted();
		} else {
			this.navigationManager = null;
		}
	}

	/** Clears the currently used {@link CarContext}. */
	public void clearCarContext() {
		carContext = null;
		navigationManager = null;
		tripHelper = null;
	}

	/** Starts navigation. */
	public void startCarNavigation() {
		if (navigationManager != null) {
			navigationManager.navigationStarted();
			carNavigationActive = true;
		}
	}

	/** Stops navigation. */
	public void stopCarNavigation() {
		getApp().runInUIThread(() -> {
					if (navigationManager != null) {
						NavigationSession carNavigationSession = getApp().getCarNavigationSession();
						if (carNavigationSession != null) {
							NavigationScreen navigationScreen = carNavigationSession.getNavigationScreen();
							if (navigationScreen != null) {
								navigationScreen.stopTrip();
							}
						}
						carNavigationActive = false;
						navigationManager.navigationEnded();
					}
				}
		);
	}

	public void updateCarNavigation(Location currentLocation) {
		OsmandApplication app = getApp();
		RoutingHelper routingHelper = app.getRoutingHelper();
		TripHelper tripHelper = this.tripHelper;
		if (carNavigationActive && tripHelper != null
				&& routingHelper.isRouteCalculated() && routingHelper.isFollowingMode()) {
			NavigationSession carNavigationSession = app.getCarNavigationSession();
			if (carNavigationSession != null) {
				NavigationScreen navigationScreen = carNavigationSession.getNavigationScreen();
				if (navigationScreen == null) {
					carNavigationSession.startNavigation();
					navigationScreen = carNavigationSession.getNavigationScreen();
				}
				if (navigationScreen != null) {
					float density = carNavigationSession.getNavigationCarSurface().getDensity();
					if (density == 0) {
						density = 1;
					}
					Trip trip = tripHelper.buildTrip(currentLocation, density);
					navigationManager.updateTrip(trip);

					List<Destination> destinations = null;
					Destination destination = tripHelper.getLastDestination();
					TravelEstimate destinationTravelEstimate = tripHelper.getLastDestinationTravelEstimate();
					if (destination != null) {
						destinations = Collections.singletonList(destination);
					}
					TravelEstimate lastStepTravelEstimate = tripHelper.getLastStepTravelEstimate();
					navigationScreen.updateTrip(true, routingHelper.isRouteBeingCalculated(),
							false/*routingHelper.isRouteWasFinished()*/,
							destinations, trip.getSteps(), destinationTravelEstimate,
							lastStepTravelEstimate != null ? lastStepTravelEstimate.getRemainingDistance() : null,
							false, true, null);
				}
			}
		}
	}

	public boolean isCarNavigationActive() {
		return carNavigationActive;
	}
}
