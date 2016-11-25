package net.osmand.plus;

import android.app.Notification;
import android.support.v4.app.NotificationCompat.Builder;

import net.osmand.plus.notifications.GpsWakeUpNotification;
import net.osmand.plus.notifications.GpxNotification;
import net.osmand.plus.notifications.NavigationNotification;
import net.osmand.plus.notifications.OsMoNotification;
import net.osmand.plus.notifications.OsmandNotification;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;

import java.util.ArrayList;
import java.util.List;

public class NotificationHelper {

	private OsmandApplication app;

	private NavigationNotification navigationNotification;
	private GpxNotification gpxNotification;
	private OsMoNotification osMoNotification;
	private GpsWakeUpNotification gpsWakeUpNotification;
	private List<OsmandNotification> all = new ArrayList<>();

	public NotificationHelper(OsmandApplication app) {
		this.app = app;
		init();
	}

	private void init() {
		navigationNotification = new NavigationNotification(app);
		gpxNotification = new GpxNotification(app);
		osMoNotification = new OsMoNotification(app);
		gpsWakeUpNotification = new GpsWakeUpNotification(app);
		all.add(navigationNotification);
		all.add(gpxNotification);
		all.add(osMoNotification);
		all.add(gpsWakeUpNotification);
	}

	public Notification buildTopNotification() {
		OsmandNotification notification = acquireTopNotification();
		if (notification != null) {
			removeNotification(notification.getType());
			setTopNotification(notification);
			Builder notificationBuilder = notification.buildNotification(false);
			return notificationBuilder.build();
		}
		return null;
	}

	private OsmandNotification acquireTopNotification() {
		OsmandNotification notification = null;
		if (navigationNotification.isEnabled()) {
			notification = navigationNotification;
		} else if (gpxNotification.isEnabled() && gpxNotification.isActive()) {
			notification = gpxNotification;
		} else if (gpsWakeUpNotification.isEnabled()) {
			notification = gpsWakeUpNotification;
		} else if (osMoNotification.isEnabled() && osMoNotification.isActive()) {
			notification = osMoNotification;
		}
		return notification;
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
		boolean navNotificationVisible = navigationNotification.showNotification();
		gpxNotification.showNotification();
		osMoNotification.showNotification();
		if (!navNotificationVisible && !gpxNotification.isActive()) {
			gpsWakeUpNotification.showNotification();
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

	public void refreshNotifications() {
		boolean navNotificationVisible = navigationNotification.refreshNotification();
		gpxNotification.refreshNotification();
		osMoNotification.refreshNotification();
		if (!navNotificationVisible && !gpxNotification.isActive()) {
			gpsWakeUpNotification.refreshNotification();
		} else {
			gpsWakeUpNotification.removeNotification();
		}
	}

	public void removeNotification(NotificationType notificationType) {
		for (OsmandNotification notification : all) {
			if (notification.getType() == notificationType) {
				notification.removeNotification();
				break;
			}
		}
	}

	public void removeNotifications() {
		for (OsmandNotification notification : all) {
			notification.removeNotification();
		}
	}
}
