package net.osmand.plus;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;

import net.osmand.plus.notifications.DownloadNotification;
import net.osmand.plus.notifications.ErrorNotification;
import net.osmand.plus.notifications.GpxNotification;
import net.osmand.plus.notifications.NavigationNotification;
import net.osmand.plus.notifications.OsmandNotification;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;

import java.util.ArrayList;
import java.util.List;

public class NotificationHelper {

	public static final String NOTIFICATION_CHANEL_ID = "osmand_background_service";
	private OsmandApplication app;

	private NavigationNotification navigationNotification;
	private GpxNotification gpxNotification;
	private DownloadNotification downloadNotification;
	private ErrorNotification errorNotification;
	private List<OsmandNotification> all = new ArrayList<>();

	public NotificationHelper(OsmandApplication app) {
		this.app = app;
		init();
	}

	private void init() {
		navigationNotification = new NavigationNotification(app);
		gpxNotification = new GpxNotification(app);
		downloadNotification = new DownloadNotification(app);
		errorNotification = new ErrorNotification(app);
		all.add(navigationNotification);
		all.add(gpxNotification);
		all.add(downloadNotification);
	}

	public Notification buildTopNotification() {
		OsmandNotification notification = acquireTopNotification();
		if (notification != null) {
			removeNotification(notification.getType());
			setTopNotification(notification);
			Builder notificationBuilder = notification.buildNotification(false);
			if (notificationBuilder != null) {
				return notificationBuilder.build();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@NonNull
	public Notification buildDownloadNotification() {
		return downloadNotification.buildNotification(false).build();
	}

	public Notification buildErrorNotification() {
		removeNotification(errorNotification.getType());
		setTopNotification(errorNotification);
		return errorNotification.buildNotification(false).build();
	}

	@Nullable
	private OsmandNotification acquireTopNotification() {
		if (navigationNotification.isEnabled()) {
			return navigationNotification;
		} else if (gpxNotification.isEnabled() && gpxNotification.isActive()) {
			return gpxNotification;
		} else {
			return null;
		}
	}

	public void updateTopNotification() {
		OsmandNotification notification = acquireTopNotification();
		setTopNotification(notification);
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
			if (!inactiveOnly || !notification.isActive()) {
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
			NotificationManager mNotificationManager = (NotificationManager) app
					.getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.createNotificationChannel(channel);
		}
	}
}
