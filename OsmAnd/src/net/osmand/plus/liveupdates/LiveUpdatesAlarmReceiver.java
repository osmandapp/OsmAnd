package net.osmand.plus.liveupdates;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.LOCAL_INDEX_INFO;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.isUpdateOverdue;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceDownloadViaWiFi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

public class LiveUpdatesAlarmReceiver extends BroadcastReceiver {

	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesAlarmReceiver.class);

	// A background broadcast still open after 60 s is an ANR
	private static final long MAX_OPEN_BROADCAST_MS = 50_000;

	@Override
	public void onReceive(Context context, Intent intent) {
		String fileName = intent.getAction();
		String localIndexInfoFile = intent.getStringExtra(LOCAL_INDEX_INFO);
		if (localIndexInfoFile == null) {
			LOG.error("Unexpected: localIndexInfoFile is null");
			return;
		}
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = app.getSettings();

		if (app.getAppInitializer().isAppInitializing() && isUpdateOverdue(localIndexInfoFile, settings)) {
			return; // AppInitializer.checkLiveUpdatesAlerts runs overdue updates
		}
		if (!preferenceDownloadViaWiFi(localIndexInfoFile, settings).get() || wifi.isWifiEnabled()) {
			PerformLiveUpdateAsyncTask task = new PerformLiveUpdateAsyncTask(context, localIndexInfoFile, false);
			runAfterAppInit(app, task, fileName);
		} else {
			PerformLiveUpdateAsyncTask.tryRescheduleDownload(context, settings, localIndexInfoFile);
		}
	}

	// The update needs the maps indexed by the app initialization and the network, which the system cuts
	// for a cached process: the broadcast stays open until the update is done, at most 50 s
	private void runAfterAppInit(@NonNull OsmandApplication app, @NonNull PerformLiveUpdateAsyncTask task,
	                             String fileName) {
		Runnable finishBroadcast = finishOnce(goAsync());
		task.setRunOnFinish(finishBroadcast);
		app.runInUIThread(finishBroadcast, MAX_OPEN_BROADCAST_MS);
		app.getAppInitializer().addOnFinishListener(init -> OsmAndTaskManager.executeTask(task, fileName));
	}

	// Both the finished update and the time limit close the broadcast, which can be finished only once
	@NonNull
	private static Runnable finishOnce(@NonNull PendingResult broadcast) {
		return new Runnable() {
			private boolean finished;

			@Override
			public void run() {
				if (!finished) {
					finished = true;
					broadcast.finish();
				}
			}
		};
	}
}
