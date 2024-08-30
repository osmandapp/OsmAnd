package net.osmand.plus.notifications;

import static net.osmand.plus.notifications.NotificationHelper.NOTIFICATION_CHANEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;

import net.osmand.plus.OsmandApplication;

public abstract class OsmandNotification {

	private static final boolean CLEAR_NOTIFICATION_IF_CHANGED = false;

	public static final int NAVIGATION_NOTIFICATION_SERVICE_ID = 5;
	public static final int GPX_NOTIFICATION_SERVICE_ID = 6;
	public static final int DOWNLOAD_NOTIFICATION_SERVICE_ID = 8;
	public static final int CAR_APP_NOTIFICATION_SERVICE_ID = 9;
	public static final int TOP_NOTIFICATION_SERVICE_ID = 100;

	public static final int WEAR_NAVIGATION_NOTIFICATION_SERVICE_ID = 1005;
	public static final int WEAR_GPX_NOTIFICATION_SERVICE_ID = 1006;
	public static final int WEAR_DOWNLOAD_NOTIFICATION_SERVICE_ID = 1008;
	public static final int WEAR_CAR_APP_NOTIFICATION_SERVICE_ID = 1009;

	protected OsmandApplication app;
	protected boolean ongoing = true;
	protected int color;
	protected int icon;
	protected boolean top;

	private final String groupName;

	private Notification currentNotification;
	protected boolean stateChanged;

	private final NotificationManagerCompat notificationManager;

	public enum NotificationType {
		NAVIGATION,
		GPX,
		GPS,
		ERROR,
		DOWNLOAD,
		CAR_APP,
	}

	public OsmandNotification(OsmandApplication app, String groupName) {
		this.app = app;
		this.groupName = groupName;
		this.notificationManager = NotificationManagerCompat.from(app);
		init();
	}

	public void init() {
	}

	public String getGroupName() {
		return groupName;
	}

	public abstract NotificationType getType();

	public boolean isTop() {
		return top;
	}

	public void setTop(boolean top) {
		this.top = top;
	}

	@SuppressLint("InlinedApi")
	protected Builder createBuilder(boolean wearable) {
		Intent contentIntent = getContentIntent();
		PendingIntent contentPendingIntent = PendingIntent.getActivity(app, 0, contentIntent,
				PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			app.getNotificationHelper().createNotificationChannel();
		}
		Builder builder = new Builder(app, NOTIFICATION_CHANEL_ID)
				.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
				.setPriority(top ? NotificationCompat.PRIORITY_HIGH : getPriority())
				.setLocalOnly(true) // Probably should be deleted to not limit notifications
				.setOnlyAlertOnce(true) // Many devices still don't treat that flag correct and keep spamming
				.setOngoing(ongoing && !wearable)
				.setContentIntent(contentPendingIntent)
				.setDeleteIntent(NotificationDismissReceiver.createIntent(app, getType()))
				.setGroup(groupName).setGroupSummary(!wearable);

		if (color != 0) {
			builder.setColor(color);
		}
		if (icon != 0) {
			builder.setSmallIcon(icon);
		}

		return builder;
	}

	public abstract Builder buildNotification(@Nullable Service service, boolean wearable);

	public abstract int getOsmandNotificationId();

	public abstract int getOsmandWearableNotificationId();

	public abstract int getPriority();

	public abstract boolean isActive();

	public abstract boolean isUsedByService(@Nullable Service service);

	public boolean isEnabled() {
		return isUsedByService(null) || isActive();
	}

	public boolean isEnabled(@Nullable Service service) {
		return isUsedByService(service) || isActive();
	}

	public abstract Intent getContentIntent();

	public void setupNotification(Notification notification) {
	}

	public void onNotificationDismissed() {
	}

	@SuppressLint("MissingPermission")
	private void notifyWearable(NotificationManagerCompat notificationManager, boolean stateChanged) {
		Builder wearNotificationBuilder = buildNotification(null, true);
		if (wearNotificationBuilder != null) {
			Notification wearNotification = wearNotificationBuilder.build();
			if (stateChanged && CLEAR_NOTIFICATION_IF_CHANGED) {
				notificationManager.cancel(getOsmandWearableNotificationId());
			}
			notificationManager.notify(getOsmandWearableNotificationId(), wearNotification);
		}
	}

	@SuppressLint("MissingPermission")
	public boolean showNotification() {
		if (isEnabled()) {
			Builder notificationBuilder = buildNotification(null, false);
			if (notificationBuilder != null) {
				Notification notification = getNotification(notificationBuilder, false);
				setupNotification(notification);
				int notificationId = top ? TOP_NOTIFICATION_SERVICE_ID : getOsmandNotificationId();
				if (stateChanged && CLEAR_NOTIFICATION_IF_CHANGED) {
					notificationManager.cancel(notificationId);
				}
				notificationManager.notify(notificationId, notification);
				notifyWearable(notificationManager, stateChanged);
				return true;
			}
		}
		return false;
	}

	@SuppressLint("MissingPermission")
	public boolean refreshNotification() {
		if (isEnabled()) {
			Builder notificationBuilder = buildNotification(null, false);
			if (notificationBuilder != null) {
				Notification notification = getNotification(notificationBuilder, true);
				setupNotification(notification);
				int notificationId = top ? TOP_NOTIFICATION_SERVICE_ID : getOsmandNotificationId();
				if (stateChanged && CLEAR_NOTIFICATION_IF_CHANGED) {
					notificationManager.cancel(notificationId);
				}
				notificationManager.notify(notificationId, notification);
				notifyWearable(notificationManager, stateChanged);
				return true;
			} else {
				notificationManager.cancel(getOsmandNotificationId());
			}
		} else {
			notificationManager.cancel(getOsmandNotificationId());
		}
		return false;
	}

	private Notification getNotification(Builder notificationBuilder, boolean forceBuild) {
		Notification notification = currentNotification;
		if (forceBuild || notification == null) {
			notification = notificationBuilder.build();
			currentNotification = notification;
		}
		return notification;
	}

	public void removeNotification() {
		currentNotification = null;
		notificationManager.cancel(getOsmandNotificationId());
		notificationManager.cancel(getOsmandWearableNotificationId());
	}
}

