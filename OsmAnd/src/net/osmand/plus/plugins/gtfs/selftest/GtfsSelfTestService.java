package net.osmand.plus.plugins.gtfs.selftest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;

/**
 * Foreground host of the self-test run, so the process keeps running with the screen off. Start it with the
 * broadcast of {@link GtfsSelfTestReceiver} or directly:
 * {@code adb shell am start-foreground-service -n net.osmand.dev/net.osmand.plus.plugins.gtfs.selftest.GtfsSelfTestService
 * --es scenarios all}. Debug builds only (see the manifest overlay); the only visible thing is the system's
 * foreground-service notification.
 */
public class GtfsSelfTestService extends Service {

	private static final String CHANNEL_ID = "gtfs_selftest";
	private static final int NOTIFICATION_ID = 0x6717;

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startInForeground();
		if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
			Log.w(GtfsSelfTestJob.TAG, "ignored: not a debuggable build");
			stopSelf(startId);
			return START_NOT_STICKY;
		}
		String scenarios = intent == null ? null : intent.getStringExtra(GtfsSelfTestReceiver.EXTRA_SCENARIOS);
		String out = intent == null ? null : intent.getStringExtra(GtfsSelfTestReceiver.EXTRA_OUT);
		String options = intent == null ? null : intent.getStringExtra(GtfsSelfTestReceiver.EXTRA_OPTIONS);
		OsmandApplication app = (OsmandApplication) getApplication();
		GtfsSelfTestJob.startThread(app, scenarios, out, options, "service", () -> {
			stopForeground(true);
			stopSelf(startId);
		});
		return START_NOT_STICKY;
	}

	private void startInForeground() {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
			nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "GTFS self-test", NotificationManager.IMPORTANCE_LOW));
		}
		Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, CHANNEL_ID)
				: new Notification.Builder(this);
		Notification n = b.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("GTFS self-test running")
				.setOngoing(true).build();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
		} else {
			startForeground(NOTIFICATION_ID, n);
		}
	}
}
