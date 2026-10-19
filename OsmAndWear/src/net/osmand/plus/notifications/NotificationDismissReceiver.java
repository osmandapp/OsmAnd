package net.osmand.plus.notifications;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;
import net.osmand.util.Algorithms;

public class NotificationDismissReceiver extends BroadcastReceiver {

	public static final String NOTIFICATION_TYPE_KEY_NAME = "net.osmand.plus.notifications.NotificationType";

	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationHelper helper = ((OsmandApplication) context.getApplicationContext()).getNotificationHelper();
		String notificationTypeStr = intent.getExtras().getString(NOTIFICATION_TYPE_KEY_NAME);
		if (!Algorithms.isEmpty(notificationTypeStr)) {
			try {
				NotificationType notificationType = NotificationType.valueOf(notificationTypeStr);
				helper.onNotificationDismissed(notificationType);
			} catch (Exception e) {
				//ignored
			}
		}
	}

	public static PendingIntent createIntent(Context context, NotificationType notificationType) {
		Intent intent = new Intent(context, NotificationDismissReceiver.class);
		intent.putExtra(NOTIFICATION_TYPE_KEY_NAME, notificationType.name());
		return PendingIntent.getBroadcast(context.getApplicationContext(),
				0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
	}
}