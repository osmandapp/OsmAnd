package net.osmand.plus.download;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.notifications.OsmandNotification;


public class DownloadService extends Service {

	public static class DownloadServiceBinder extends Binder {

	}

	private DownloadServiceBinder binder = new DownloadServiceBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public void stopService(Context ctx) {
		final Intent serviceIntent = new Intent(ctx, DownloadService.class);
		ctx.stopService(serviceIntent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		final OsmandApplication app = (OsmandApplication) getApplication();
		app.setDownloadService(this);

		Notification notification = app.getNotificationHelper().buildDownloadNotification();
		startForeground(OsmandNotification.DOWNLOAD_NOTIFICATION_SERVICE_ID, notification);
		app.getNotificationHelper().refreshNotification(OsmandNotification.NotificationType.DOWNLOAD);

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		final OsmandApplication app = (OsmandApplication) getApplication();
		app.setDownloadService(null);

		// remove notification
		stopForeground(Boolean.TRUE);
		app.getNotificationHelper().refreshNotification(OsmandNotification.NotificationType.DOWNLOAD);
		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				app.getNotificationHelper().refreshNotification(OsmandNotification.NotificationType.DOWNLOAD);
			}
		}, 500);
	}
}