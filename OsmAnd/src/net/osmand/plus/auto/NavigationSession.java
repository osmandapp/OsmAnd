package net.osmand.plus.auto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.Location;
import net.osmand.data.ValueHolder;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.RequestPermissionScreen.LocationPermissionCheckCallback;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.routing.IRouteInformationListener;
import net.osmand.plus.views.OsmandMapTileView;

/**
 * Session class for the Navigation sample app.
 */
public class NavigationSession extends Session implements NavigationScreen.Listener, OsmAndLocationListener,
		DefaultLifecycleObserver, IRouteInformationListener {
	static final String TAG = NavigationSession.class.getSimpleName();
	static final String URI_SCHEME = "osmand";
	static final String URI_HOST = "car_navigation";

	NavigationScreen navigationScreen;
	RequestPurchaseScreen requestPurchaseScreen;
	SurfaceRenderer navigationCarSurface;
	Action settingsAction;

	private OsmandMapTileView mapView;

	NavigationSession() {
		getLifecycle().addObserver(this);
	}

	public NavigationScreen getNavigationScreen() {
		return navigationScreen;
	}

	public SurfaceRenderer getNavigationCarSurface() {
		return navigationCarSurface;
	}

	public OsmandMapTileView getMapView() {
		return mapView;
	}

	private final LocationPermissionCheckCallback locationPermissionGrantedCallback =
			() -> getApp().startNavigationService(NavigationService.USED_BY_CAR_APP);

	public void setMapView(OsmandMapTileView mapView) {
		this.mapView = mapView;
		SurfaceRenderer navigationCarSurface = this.navigationCarSurface;
		if (navigationCarSurface != null) {
			navigationCarSurface.setMapView(mapView);
		}
	}

	private OsmandApplication getApp() {
		return (OsmandApplication) getCarContext().getApplicationContext();
	}

	@Override
	public void onStart(@NonNull LifecycleOwner owner) {
		getApp().getRoutingHelper().addListener(this);
	}

	@Override
	public void onStop(@NonNull LifecycleOwner owner) {
		getApp().getRoutingHelper().removeListener(this);
	}

	@Override
	public void onDestroy(@NonNull LifecycleOwner owner) {
		getLifecycle().removeObserver(this);
	}

	public boolean hasStarted() {
		Lifecycle.State state = getLifecycle().getCurrentState();
		return state == Lifecycle.State.STARTED || state == Lifecycle.State.RESUMED;
	}

	public boolean isStateAtLeast(@NonNull State state) {
		return getLifecycle().getCurrentState().isAtLeast(state);
	}

	public boolean hasSurface() {
		SurfaceRenderer navigationCarSurface = this.navigationCarSurface;
		return navigationCarSurface != null && navigationCarSurface.hasSurface();
	}

	@Override
	@NonNull
	public Screen onCreateScreen(@NonNull Intent intent) {
		Log.i(TAG, "In onCreateScreen()");
		settingsAction =
				new Action.Builder()
						.setIcon(new CarIcon.Builder(
								IconCompat.createWithResource(getCarContext(), R.drawable.ic_action_settings))
								.build())
						.setOnClickListener(() -> getCarContext()
								.getCarService(ScreenManager.class)
								.push(new SettingsScreen(getCarContext())))
						.build();

		navigationCarSurface = new SurfaceRenderer(getCarContext(), getLifecycle());
		if (mapView != null) {
			navigationCarSurface.setMapView(mapView);
		}
		navigationScreen = new NavigationScreen(getCarContext(), settingsAction, this, navigationCarSurface);
		navigationCarSurface.callback = navigationScreen;

		String action = intent.getAction();
		if (CarContext.ACTION_NAVIGATE.equals(action)) {
			CarToast.makeText(getCarContext(), "Navigation intent: " + intent.getDataString(), CarToast.LENGTH_LONG).show();
		}

		OsmandApplication app = getApp();
		if (!InAppPurchaseHelper.isAndroidAutoAvailable(app)) {
			getCarContext().getCarService(ScreenManager.class).push(navigationScreen);
			requestPurchaseScreen = new RequestPurchaseScreen(getCarContext());
			return requestPurchaseScreen;
		}

		if (!isLocationPermissionAvailable()) {
			getCarContext().getCarService(ScreenManager.class).push(navigationScreen);
			return new RequestPermissionScreen(getCarContext(), locationPermissionGrantedCallback);
		}

		return navigationScreen;
	}

	public void onPurchaseDone() {
		OsmandApplication app = getApp();
		if (requestPurchaseScreen != null && InAppPurchaseHelper.isAndroidAutoAvailable(app)) {
			requestPurchaseScreen.finish();
			requestPurchaseScreen = null;
			app.getOsmandMap().getMapView().setupRenderingView();

			requestLocationPermission();
		}
	}

	public boolean isLocationPermissionAvailable() {
		boolean accessFineLocation = ActivityCompat.checkSelfPermission(getCarContext(), Manifest.permission.ACCESS_FINE_LOCATION)
				== PackageManager.PERMISSION_GRANTED;
		boolean accessCoarseLocation = ActivityCompat.checkSelfPermission(getCarContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
				== PackageManager.PERMISSION_GRANTED;
		return accessFineLocation || accessCoarseLocation;
	}

	private boolean requestLocationPermission() {
		if (!isLocationPermissionAvailable()) {
			getCarContext().getCarService(ScreenManager.class).push(
					new RequestPermissionScreen(getCarContext(), locationPermissionGrantedCallback));
			return true;
		}
		return false;
	}

	@Override
	public void onNewIntent(@NonNull Intent intent) {
		Log.i(TAG, "In onNewIntent() " + intent);
		ScreenManager screenManager = getCarContext().getCarService(ScreenManager.class);
		if (CarContext.ACTION_NAVIGATE.equals(intent.getAction())) {
			Uri uri = Uri.parse("http://" + intent.getDataString());
			screenManager.popToRoot();
			String query = uri.getQueryParameter("q");
			if (query == null) {
				query = "";
			}
			screenManager.pushForResult(
					new SearchResultsScreen(
							getCarContext(),
							settingsAction,
							navigationCarSurface,
							query),
					(obj) -> {
					});

			return;
		}

		// Process the intent from DeepLinkNotificationReceiver. Bring the routing screen back to
		// the
		// top if any other screens were pushed onto it.
		Uri uri = intent.getData();
		if (uri != null
				&& URI_SCHEME.equals(uri.getScheme())
				&& URI_HOST.equals(uri.getSchemeSpecificPart())) {

			Screen top = screenManager.getTop();
			if (NavigationService.DEEP_LINK_ACTION_OPEN_ROOT_SCREEN.equals(uri.getFragment()) && !(top instanceof NavigationScreen)) {
				screenManager.popToRoot();
			}
		}
	}

	@Override
	public void onCarConfigurationChanged(@NonNull Configuration newConfiguration) {
		if (navigationCarSurface != null) {
			navigationCarSurface.onCarConfigurationChanged();
		}
	}

	@Override
	public boolean requestLocationNavigation() {
		return requestLocationPermission();
	}

	@Override
	public void updateNavigation(boolean navigating) {
	}

	@Override
	public void stopNavigation() {
		OsmandApplication app = getApp();
		if (app != null) {
			app.stopNavigation();
			NavigationScreen navigationScreen = getNavigationScreen();
			if (navigationScreen != null) {
				navigationScreen.stopTrip();
			}
		}
	}

	@Override
	public void updateLocation(Location location) {
		navigationCarSurface.updateLocation(location);
	}

	@Override
	public void newRouteIsCalculated(boolean newRoute, ValueHolder<Boolean> showToast) {
	}

	@Override
	public void routeWasCancelled() {
	}

	@Override
	public void routeWasFinished() {
		getApp().stopNavigation();
	}
}
