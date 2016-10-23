package net.osmand.plus;

import net.osmand.plus.notifications.GpsWakeUpNotification;
import net.osmand.plus.notifications.GpxNotification;
import net.osmand.plus.notifications.NavigationNotification;
import net.osmand.plus.notifications.OsMoNotification;
import net.osmand.plus.notifications.OsmandNotification;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;

public class NotificationHelper {

	private OsmandApplication app;

	private NavigationNotification navigationNotification;
	private GpxNotification gpxNotification;
	private OsMoNotification osMoNotification;
	private GpsWakeUpNotification gpsWakeUpNotification;

	public NotificationHelper(OsmandApplication app) {
		this.app = app;
		init();
	}

	public GpsWakeUpNotification getGpsWakeUpNotification() {
		return gpsWakeUpNotification;
	}

	private void init() {
		navigationNotification = new NavigationNotification(app);
		gpxNotification = new GpxNotification(app);
		osMoNotification = new OsMoNotification(app);
		gpsWakeUpNotification = new GpsWakeUpNotification(app);
	}

	public OsmandNotification getTopNotification() {
		if (navigationNotification.isEnabled()) {
			return navigationNotification;
		} else if (gpxNotification.isEnabled() && gpxNotification.isActive()) {
			return gpxNotification;
		} else if (gpsWakeUpNotification.isEnabled()) {
			return gpsWakeUpNotification;
		} else if (osMoNotification.isEnabled()) {
			return osMoNotification;
		}
		return null;
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
		switch (notificationType) {
			case NAVIGATION:
				navigationNotification.refreshNotification();
				break;
			case GPX:
				gpxNotification.refreshNotification();
				break;
			case OSMO:
				osMoNotification.refreshNotification();
				break;
			case GPS:
				gpsWakeUpNotification.refreshNotification();
				break;
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

	public void removeNotifications() {
		navigationNotification.removeNotification();
		gpxNotification.removeNotification();
		osMoNotification.removeNotification();
		gpsWakeUpNotification.removeNotification();
	}
}
