package net.osmand.plus.download;

import static net.osmand.plus.notifications.OsmandNotification.DOWNLOAD_NOTIFICATION_SERVICE_ID;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.notifications.NotificationHelper;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;


public class DownloadService extends Service {

	public static class DownloadServiceBinder extends Binder {

	}

	private final DownloadServiceBinder binder = new DownloadServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public void stopService(@NonNull Context context) {
		context.stopService(new Intent(context, DownloadService.class));
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		OsmandApplication app = (OsmandApplication) getApplication();
		app.setDownloadService(this);

		NotificationHelper notificationHelper = app.getNotificationHelper();
		Notification notification = notificationHelper.buildDownloadNotification();
		startForeground(DOWNLOAD_NOTIFICATION_SERVICE_ID, notification);
		app.getNotificationHelper().refreshNotification(NotificationType.DOWNLOAD);

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		OsmandApplication app = (OsmandApplication) getApplication();
		app.setDownloadService(null);

		// remove notification
		stopForeground(Boolean.TRUE);
		app.getNotificationHelper().refreshNotification(NotificationType.DOWNLOAD);
		app.runInUIThread(() -> app.getNotificationHelper().refreshNotification(NotificationType.DOWNLOAD), 500);
	}
}