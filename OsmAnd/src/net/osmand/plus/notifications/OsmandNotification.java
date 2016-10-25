package net.osmand.plus.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat.Builder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

public abstract class OsmandNotification {

	public final static int NAVIGATION_NOTIFICATION_SERVICE_ID = 5;
	public final static int GPX_NOTIFICATION_SERVICE_ID = 6;
	public final static int OSMO_NOTIFICATION_SERVICE_ID = 7;
	public final static int GPS_WAKE_UP_NOTIFICATION_SERVICE_ID = 8;

	protected OsmandApplication app;
	protected boolean ongoing = true;
	protected int color;
	protected int icon;

	public enum NotificationType {
		NAVIGATION,
		GPX,
		OSMO,
		GPS
	}

	public OsmandNotification(OsmandApplication app) {
		this.app = app;
		init();
	}

	public void init() {
	}

	public abstract NotificationType getType();

	protected Builder createBuilder() {
		Intent contentIntent = new Intent(app, MapActivity.class);
		PendingIntent contentPendingIntent = PendingIntent.getActivity(app, 0, contentIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		Builder builder = new Builder(app)
				.setVisibility(android.support.v7.app.NotificationCompat.VISIBILITY_PUBLIC)
				.setPriority(getPriority())
				.setOngoing(ongoing)
				.setContentIntent(contentPendingIntent)
				.setDeleteIntent(NotificationDismissReceiver.createIntent(app, getType()));

		if (color != 0) {
			builder.setColor(color);
		}
		if (icon != 0) {
			builder.setSmallIcon(icon);
		}

		return builder;
	}

	public abstract Builder buildNotification();

	public abstract int getUniqueId();

	public abstract int getPriority();

	public abstract boolean isActive();

	public abstract boolean isEnabled();

	public void setupNotification(Notification notification) {
	}

	public void onNotificationDismissed() {
	}

	public boolean showNotification() {
		NotificationManager notificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
		if (isEnabled()) {
			Builder newNotification = buildNotification();
			if (newNotification != null) {
				Notification notification = newNotification.build();
				setupNotification(notification);
				notificationManager.notify(getUniqueId(), notification);
				return true;
			}
		}
		return false;
	}

	public boolean refreshNotification() {
		NotificationManager notificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
		if (isEnabled()) {
			Builder newNotification = buildNotification();
			if (newNotification != null) {
				Notification notification = newNotification.build();
				setupNotification(notification);
				notificationManager.notify(getUniqueId(), notification);
				return true;
			}
		} else {
			notificationManager.cancel(getUniqueId());
		}
		return false;
	}

	public void removeNotification() {
		NotificationManager notificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(getUniqueId());
	}

	public void closeSystemDialogs(Context context) {
		Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		context.sendBroadcast(it);
	}
}

