package net.osmand.plus.notifications;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
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

	public static final String GROUP_NAME = "DOWNLOAD";

	public DownloadNotification(OsmandApplication app) {
		super(app, GROUP_NAME);
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
		DownloadIndexesThread downloadThread = app.getDownloadThread();
		return downloadThread.isDownloading();
	}

	@Override
	public boolean isUsedByService(@Nullable Service service) {
		DownloadService downloadService = service instanceof DownloadService
				? (DownloadService) service : app.getDownloadService();
		return downloadService != null;
	}

	@Override
	public Intent getContentIntent() {
		Intent intent = new Intent(app, DownloadActivity.class);
		intent.setPackage(app.getPackageName());
		return intent;
	}

	@Override
	public NotificationCompat.Builder buildNotification(@Nullable Service service, boolean wearable) {
		icon = android.R.drawable.stat_sys_download;
		ongoing = true;
		DownloadIndexesThread downloadThread = app.getDownloadThread();

		BasicProgressAsyncTask<?, ?, ?, ?> task = downloadThread.getCurrentRunningTask();
		boolean isFinished = task == null || task.getStatus() == AsyncTask.Status.FINISHED;

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
				.setOnlyAlertOnce(true)
				.setOngoing(true);
		int progress = (int) downloadThread.getCurrentDownloadProgress();
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