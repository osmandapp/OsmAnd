package net.osmand.plus.notifications;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.auto.CarAppNotification;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class NotificationHelper {

	public static final Log LOG = PlatformUtil.getLog(NotificationHelper.class);

	public static final String NOTIFICATION_CHANEL_ID = "osmand_background_service";
	private final OsmandApplication app;

	private NavigationNotification navigationNotification;
	private GpxNotification gpxNotification;
	private CarAppNotification carAppNotification;
	private DownloadNotification downloadNotification;
	private final List<OsmandNotification> all = new ArrayList<>();

	public NotificationHelper(@NonNull OsmandApplication app) {
		this.app = app;
		init();
	}

	private void init() {
		navigationNotification = new NavigationNotification(app);
		gpxNotification = new GpxNotification(app);
		downloadNotification = new DownloadNotification(app);
		carAppNotification = new CarAppNotification(app);
		all.add(navigationNotification);
		all.add(gpxNotification);
		all.add(downloadNotification);
		all.add(carAppNotification);
	}

	@Nullable
	public Notification buildTopNotification(@NonNull Service service, @NonNull NotificationType type) {
		List<OsmandNotification> notifications = acquireTopNotifications(service);
		for (OsmandNotification notification : notifications) {
			if (notification.getType() == type) {
				Notification topNotification = buildTopNotification(service, notification);
				if (topNotification != null) {
					return topNotification;
				}
			}
		}
		for (OsmandNotification notification : notifications) {
			if (notification.getType() != type) {
				Notification topNotification = buildTopNotification(service, notification);
				if (topNotification != null) {
					return topNotification;
				}
			}
		}
		return null;
	}

	@Nullable
	private Notification buildTopNotification(@NonNull Service service, @NonNull OsmandNotification notification) {
		removeNotification(notification.getType());
		setTopNotification(notification);
		Builder notificationBuilder = notification.buildNotification(service, false);
		if (notificationBuilder != null) {
			return notificationBuilder.build();
		}
		return null;
	}

	@NonNull
	public Notification buildDownloadNotification() {
		return downloadNotification.buildNotification(null, false).build();
	}

	@NonNull
	public Notification buildCarAppNotification() {
		return carAppNotification.buildNotification(null, false).build();
	}

	@NonNull
	private List<OsmandNotification> acquireTopNotifications(@Nullable Service service) {
		List<OsmandNotification> res = new ArrayList<>();
		if (navigationNotification.isEnabled(service)) {
			res.add(navigationNotification);
		}
		if (gpxNotification.isEnabled(service)) {
			res.add(gpxNotification);
		}
		return res;
	}

	public void resetTopNotification() {
		for (OsmandNotification n : all) {
			n.setTop(false);
		}
	}

	public void updateTopNotification() {
		List<OsmandNotification> notifications = acquireTopNotifications(null);
		if (!notifications.isEmpty()) {
			setTopNotification(notifications.get(0));
		}
	}

	private void setTopNotification(OsmandNotification notification) {
		for (OsmandNotification n : all) {
			n.setTop(n == notification);
		}
	}

	public void showNotifications() {
		if (!hasAnyTopNotification()) {
			removeTopNotification();
		}
		for (OsmandNotification notification : all) {
			notification.showNotification();
		}
	}

	public void refreshNotification(NotificationType notificationType) {
		for (OsmandNotification notification : all) {
			if (notification.getType() == notificationType) {
				notification.refreshNotification();
				break;
			}
		}
	}

	public void onNotificationDismissed(NotificationType notificationType) {
		for (OsmandNotification notification : all) {
			if (notification.getType() == notificationType) {
				notification.onNotificationDismissed();
				break;
			}
		}
	}

	public int getOsmandNotificationId(NotificationType notificationType) {
		for (OsmandNotification notification : all) {
			if (notification.getType() == notificationType) {
				return notification.getOsmandNotificationId();
			}
		}
		return -1;
	}

	public boolean hasAnyTopNotification() {
		for (OsmandNotification notification : all) {
			if (notification.isTop()) {
				return true;
			}
		}
		return false;
	}

	public void refreshNotifications() {
		if (!hasAnyTopNotification()) {
			removeTopNotification();
		}
		for (OsmandNotification notification : all) {
			notification.refreshNotification();
		}
	}

	public void removeTopNotification() {
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(app);
		notificationManager.cancel(OsmandNotification.TOP_NOTIFICATION_SERVICE_ID);
	}

	public void removeNotification(NotificationType notificationType) {
		for (OsmandNotification notification : all) {
			if (notification.getType() == notificationType) {
				notification.removeNotification();
				break;
			}
		}
	}

	public void removeNotifications(boolean inactiveOnly) {
		for (OsmandNotification notification : all) {
			if (!inactiveOnly || !notification.isEnabled()) {
				notification.removeNotification();
			}
		}
	}

	@TargetApi(26)
	public void createNotificationChannel() {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANEL_ID,
					app.getString(R.string.osmand_service), NotificationManager.IMPORTANCE_LOW);
			channel.enableVibration(false);
			channel.setDescription(app.getString(R.string.osmand_service_descr));
			NotificationManagerCompat.from(app).createNotificationChannel(channel);
		}
	}
}
