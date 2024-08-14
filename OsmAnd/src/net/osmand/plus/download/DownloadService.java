package net.osmand.plus.download;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
import static net.osmand.plus.notifications.OsmandNotification.DOWNLOAD_NOTIFICATION_SERVICE_ID;
import static net.osmand.plus.notifications.OsmandNotification.TOP_NOTIFICATION_SERVICE_ID;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.notifications.NotificationHelper;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;

import org.apache.commons.logging.Log;


public class DownloadService extends Service {

	public static final Log LOG = PlatformUtil.getLog(DownloadService.class);

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
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				startForeground(DOWNLOAD_NOTIFICATION_SERVICE_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC);
			} else {
				startForeground(DOWNLOAD_NOTIFICATION_SERVICE_ID, notification);
			}
			app.getNotificationHelper().refreshNotification(NotificationType.DOWNLOAD);
		} catch (Exception e) {
			app.setDownloadService(null);
			LOG.error("Failed to start DownloadService", e);
		}

		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		OsmandApplication app = (OsmandApplication) getApplication();
		app.setDownloadService(null);

		// remove notification
		stopForeground(STOP_FOREGROUND_REMOVE);
		app.getNotificationHelper().refreshNotification(NotificationType.DOWNLOAD);
		app.runInUIThread(() -> app.getNotificationHelper().refreshNotification(NotificationType.DOWNLOAD), 500);
	}
}