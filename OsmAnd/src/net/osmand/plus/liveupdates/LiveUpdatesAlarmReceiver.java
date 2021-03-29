package net.osmand.plus.liveupdates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceDownloadViaWiFi;

public class LiveUpdatesAlarmReceiver extends BroadcastReceiver {
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesAlarmReceiver.class);

	@Override
	public void onReceive(Context context, Intent intent) {
		String fileName = intent.getAction();
		String localIndexInfoFile =
				intent.getStringExtra(LiveUpdatesHelper.LOCAL_INDEX_INFO);
		if (localIndexInfoFile == null) {
			LOG.error("Unexpected: localIndexInfoFile is null");
			return;
		}
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		final OsmandApplication application = (OsmandApplication) context.getApplicationContext();
		final OsmandSettings settings = application.getSettings();

		if (!preferenceDownloadViaWiFi(localIndexInfoFile, settings).get() || wifi.isWifiEnabled()) {
			new PerformLiveUpdateAsyncTask(context, localIndexInfoFile, false)
					.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fileName);
		} else {
			PerformLiveUpdateAsyncTask.tryRescheduleDownload(context, settings, localIndexInfoFile);
		}
	}
}
