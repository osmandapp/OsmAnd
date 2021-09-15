package net.osmand.plus.auto;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.Session;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarIcon;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.IconCompat;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapTileView;

/**
 * Session class for the Navigation sample app.
 */
public class NavigationSession extends Session implements NavigationScreen.Listener, OsmAndLocationListener {
	static final String TAG = NavigationSession.class.getSimpleName();

	static final String URI_SCHEME = "samples";

	static final String URI_HOST = "navigation";

	NavigationScreen mNavigationScreen;
	SurfaceRenderer mNavigationCarSurface;
	Action mSettingsAction;

	private OsmandMapTileView mapView;

	NavigationSession() {
	}

	public NavigationScreen getNavigationScreen() {
		return mNavigationScreen;
	}

	public SurfaceRenderer getNavigationCarSurface() {
		return mNavigationCarSurface;
	}

	public OsmandMapTileView getMapView() {
		return mapView;
	}

	public void setMapView(OsmandMapTileView mapView) {
		this.mapView = mapView;
		SurfaceRenderer navigationCarSurface = this.mNavigationCarSurface;
		if (navigationCarSurface != null) {
			navigationCarSurface.setMapView(mapView);
		}
	}

	public boolean hasSurface() {
		SurfaceRenderer navigationCarSurface = this.mNavigationCarSurface;
		return navigationCarSurface != null && navigationCarSurface.hasSurface();
	}

	@Override
	@NonNull
	public Screen onCreateScreen(@NonNull Intent intent) {
		Log.i(TAG, "In onCreateScreen()");

		mSettingsAction =
				new Action.Builder()
						.setIcon(new CarIcon.Builder(
								IconCompat.createWithResource(getCarContext(), R.drawable.ic_action_settings))
								.build())
						.setOnClickListener(() -> getCarContext()
								.getCarService(ScreenManager.class)
								.push(new SettingsScreen(getCarContext())))
						.build();

		mNavigationCarSurface = new SurfaceRenderer(getCarContext(), getLifecycle());
		if (mapView != null) {
			mNavigationCarSurface.setMapView(mapView);
		}
		mNavigationScreen = new NavigationScreen(getCarContext(), mSettingsAction, this, mNavigationCarSurface);
		mNavigationCarSurface.callback = mNavigationScreen;

		String action = intent.getAction();
		if (CarContext.ACTION_NAVIGATE.equals(action)) {
			CarToast.makeText(
					getCarContext(),
					"Navigation intent: " + intent.getDataString(),
					CarToast.LENGTH_LONG)
					.show();
		}

		if (ActivityCompat.checkSelfPermission(getCarContext(), Manifest.permission.ACCESS_FINE_LOCATION)
				!= PackageManager.PERMISSION_GRANTED) {
			getCarContext().getCarService(ScreenManager.class).push(mNavigationScreen);
			return new RequestPermissionScreen(getCarContext(), null);
		}

		return mNavigationScreen;
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
							mSettingsAction,
							mNavigationCarSurface,
							query),
					(obj) -> {
						if (obj != null) {
                            /*
                            // Need to copy over each element to satisfy Java type safety.
                            List<?> results = (List<?>) obj;
                            List<Instruction> instructions = new ArrayList<Instruction>();
                            for (Object result : results) {
                                instructions.add((Instruction) result);
                            }
                            executeScript(instructions);
                             */
						}
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

			/*
			Screen top = screenManager.getTop();
			if (NavigationService.DEEP_LINK_ACTION.equals(uri.getFragment()) && !(top instanceof NavigationScreen)) {
				screenManager.popToRoot();
			}
			 */
		}
	}

	@Override
	public void onCarConfigurationChanged(@NonNull Configuration newConfiguration) {
		if (mNavigationCarSurface != null) {
			mNavigationCarSurface.onCarConfigurationChanged();
		}
	}

	@Override
	public void updateNavigation(boolean navigating) {
		OsmandMapTileView mapView = this.mapView;
		if (mapView != null) {
			mapView.setMapPositionX(navigating ? 1 : 0);
		}
	}

	@Override
	public void stopNavigation() {
		OsmandApplication app = (OsmandApplication) getCarContext().getApplicationContext();
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
		mNavigationCarSurface.updateLocation(location);
	}
}
