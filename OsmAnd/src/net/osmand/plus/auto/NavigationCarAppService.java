package net.osmand.plus.auto;

import android.app.Notification;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.validation.HostValidator;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;

/**
 * Entry point for the templated app.
 *
 * <p>{@link CarAppService} is the main interface between the app and the car host. For more
 * details, see the <a href="https://developer.android.com/training/cars/navigation">Android for
 * Cars Library developer guide</a>.
 */
public final class NavigationCarAppService extends CarAppService {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(NavigationCarAppService.class);

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
		OsmandApplication app = getApp();
		Notification notification = app.getNotificationHelper().buildCarAppNotification();
		startForeground(app.getNotificationHelper().getOsmandNotificationId(NotificationType.CAR_APP), notification);

		NavigationSession session = new NavigationSession();
		session.getLifecycle()
				.addObserver(new DefaultLifecycleObserver() {
					@Override
					public void onDestroy(@NonNull LifecycleOwner owner) {
						stopForeground(STOP_FOREGROUND_REMOVE);
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
