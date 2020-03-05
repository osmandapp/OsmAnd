package net.osmand.plus.notifications;

import android.content.Intent;
import android.os.AsyncTask;

import androidx.core.app.NotificationCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.Version;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadService;
import net.osmand.plus.download.IndexItem;

import java.util.List;

public class DownloadNotification extends OsmandNotification {

	public final static String GROUP_NAME = "DOWNLOAD";

	public DownloadNotification(OsmandApplication app) {
		super(app, GROUP_NAME);
	}

	@Override
	public void init() {
	}

	@Override
	public NotificationType getType() {
		return NotificationType.DOWNLOAD;
	}

	@Override
	public int getPriority() {
		return NotificationCompat.PRIORITY_DEFAULT;
	}

	@Override
	public boolean isActive() {
		DownloadService service = app.getDownloadService();
		return isEnabled() && service != null;
	}

	@Override
	public boolean isEnabled() {
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		return downloadThread.isDownloading();
	}

	@Override
	public Intent getContentIntent() {
		return new Intent(app, DownloadActivity.class);
	}

	@Override
	public NotificationCompat.Builder buildNotification(boolean wearable) {
		icon = android.R.drawable.stat_sys_download;
		ongoing = true;
		DownloadIndexesThread downloadThread = app.getDownloadThread();

		BasicProgressAsyncTask<?, ?, ?, ?> task = downloadThread.getCurrentRunningTask();
		final boolean isFinished = task == null || task.getStatus() == AsyncTask.Status.FINISHED;

		NotificationCompat.Builder notificationBuilder = createBuilder(wearable);
		String msg = Version.getAppName(app);
		if (!isFinished) {
			msg = task.getDescription();
		}
		StringBuilder contentText = new StringBuilder();
		List<IndexItem> ii = downloadThread.getCurrentDownloadingItems();
		for (IndexItem i : ii) {
			if (!isFinished && task.getTag() == i) {
				continue;
			}
			if (contentText.length() > 0) {
				contentText.append(", ");
			}
			contentText.append(i.getVisibleName(app, app.getRegions()));
			contentText.append(" ").append(i.getType().getString(app));
		}
		notificationBuilder.setContentTitle(msg)
				.setContentText(contentText.toString())
				.setOngoing(true);
		int progress = downloadThread.getCurrentDownloadingItemProgress();
		notificationBuilder.setProgress(100, Math.max(progress, 0), progress < 0);
		return notificationBuilder;
	}

	@Override
	public int getOsmandNotificationId() {
		return DOWNLOAD_NOTIFICATION_SERVICE_ID;
	}

	@Override
	public int getOsmandWearableNotificationId() {
		return WEAR_DOWNLOAD_NOTIFICATION_SERVICE_ID;
	}
}