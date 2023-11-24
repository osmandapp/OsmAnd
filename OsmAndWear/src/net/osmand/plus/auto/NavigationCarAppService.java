package net.osmand.plus.auto;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.plus.OsmandApplication;

/**
 * Entry point for the templated app.
 *
 * <p>{@link CarAppService} is the main interface between the app and the car host. For more
 * details, see the <a href="https://developer.android.com/training/cars/navigation">Android for
 * Cars Library developer guide</a>.
 */
public final class NavigationCarAppService extends CarAppService {

	private OsmandApplication getApp() {
		return (OsmandApplication) getApplication();
	}

	/**
	 * Create a deep link URL from the given deep link action.
	 */
	@NonNull
	public static Uri createDeepLinkUri(@NonNull String deepLinkAction) {
		return Uri.fromParts(NavigationSession.URI_SCHEME, NavigationSession.URI_HOST, deepLinkAction);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int result = super.onStartCommand(intent, flags, startId);
		getApp().setNavigationCarAppService(this);
		return result;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getApp().setNavigationCarAppService(null);
	}

	@Override
	@NonNull
	public Session onCreateSession() {
		NavigationSession session = new NavigationSession();
		getApp().getLocationProvider().addLocationListener(session);
		session.getLifecycle()
				.addObserver(
						new DefaultLifecycleObserver() {
							@Override
							public void onCreate(@NonNull LifecycleOwner owner) {
								getApp().setCarNavigationSession(session);
							}

							@Override
							public void onStart(@NonNull LifecycleOwner owner) {
								getApp().getOsmandMap().getMapView().setupRenderingView();
							}

							@Override
							public void onStop(@NonNull LifecycleOwner owner) {
								getApp().getOsmandMap().getMapView().setupRenderingView();
							}

							@Override
							public void onDestroy(@NonNull LifecycleOwner owner) {
								getApp().setCarNavigationSession(null);
								getApp().getLocationProvider().removeLocationListener(session);
							}
						});

		return session;
	}

	@NonNull
	@Override
	public HostValidator createHostValidator() {
		if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
			return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
		} else {
			return new HostValidator.Builder(getApplicationContext())
					.addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
					.build();
		}
	}
}
