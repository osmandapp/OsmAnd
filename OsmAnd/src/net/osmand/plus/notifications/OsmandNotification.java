package net.osmand.plus.notifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationManagerCompat;

import net.osmand.plus.NotificationHelper;
import net.osmand.plus.OsmandApplication;

public abstract class OsmandNotification {

	public final static int NAVIGATION_NOTIFICATION_SERVICE_ID = 5;
	public final static int GPX_NOTIFICATION_SERVICE_ID = 6;
	public final static int ERROR_NOTIFICATION_SERVICE_ID = 7;
	public final static int DOWNLOAD_NOTIFICATION_SERVICE_ID = 8;
	public final static int TOP_NOTIFICATION_SERVICE_ID = 100;

	public final static int WEAR_NAVIGATION_NOTIFICATION_SERVICE_ID = 1005;
	public final static int WEAR_GPX_NOTIFICATION_SERVICE_ID = 1006;
	public final static int WEAR_ERROR_NOTIFICATION_SERVICE_ID = 1007;
	public final static int WEAR_DOWNLOAD_NOTIFICATION_SERVICE_ID = 1008;

	protected OsmandApplication app;
	protected boolean ongoing = true;
	protected int color;
	protected int icon;
	protected boolean top;

	private String groupName;

	private Notification currentNotification;

	public enum NotificationType {
		NAVIGATION,
		GPX,
		GPS,
		ERROR,
		DOWNLOAD,
	}

	public OsmandNotification(OsmandApplication app, String groupName) {
		this.app = app;
		this.groupName = groupName;
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
				PendingIntent.FLAG_UPDATE_CURRENT);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			app.getNotificationHelper().createNotificationChannel();
		}
		Builder builder = new Builder(app, NotificationHelper.NOTIFICATION_CHANEL_ID)
				.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
				.setPriority(top ? NotificationCompat.PRIORITY_HIGH : getPriority())
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

	public abstract Builder buildNotification(boolean wearable);

	public abstract int getOsmandNotificationId();

	public abstract int getOsmandWearableNotificationId();

	public abstract int getPriority();

	public abstract boolean isActive();

	public abstract boolean isEnabled();

	public abstract Intent getContentIntent();

	public void setupNotification(Notification notification) {
	}

	public void onNotificationDismissed() {
	}

	private void notifyWearable(NotificationManagerCompat notificationManager) {
		Builder wearNotificationBuilder = buildNotification(true);
		if (wearNotificationBuilder != null) {
			Notification wearNotification = wearNotificationBuilder.build();
			notificationManager.notify(getOsmandWearableNotificationId(), wearNotification);
		}
	}

	public boolean showNotification() {
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(app);
		if (isEnabled()) {
			Builder notificationBuilder = buildNotification(false);
			if (notificationBuilder != null) {
				Notification notification = getNotification(notificationBuilder, false);
				setupNotification(notification);
				notificationManager.notify(top ? TOP_NOTIFICATION_SERVICE_ID : getOsmandNotificationId(), notification);
				notifyWearable(notificationManager);
				return true;
			}
		}
		return false;
	}

	public boolean refreshNotification() {
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(app);
		if (isEnabled()) {
			Builder notificationBuilder = buildNotification(false);
			if (notificationBuilder != null) {
				Notification notification = getNotification(notificationBuilder, true);
				setupNotification(notification);
				if (top) {
					//notificationManager.cancel(getOsmandNotificationId());
					notificationManager.notify(TOP_NOTIFICATION_SERVICE_ID, notification);
				} else {
					notificationManager.notify(getOsmandNotificationId(), notification);
				}
				notifyWearable(notificationManager);
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
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(app);
		notificationManager.cancel(getOsmandNotificationId());
		notificationManager.cancel(getOsmandWearableNotificationId());
	}

	public void closeSystemDialogs(Context context) {
		Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		context.sendBroadcast(it);
	}
}

