package net.osmand.plus.liveupdates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.LocalIndexInfo;

import org.apache.commons.logging.Log;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceDownloadViaWiFi;

public class LiveUpdatesAlarmReceiver extends BroadcastReceiver {
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesAlarmReceiver.class);

	@Override
	public void onReceive(Context context, Intent intent) {
		String fileName = intent.getAction();
		LocalIndexInfo localIndexInfo =
				intent.getParcelableExtra(LiveUpdatesHelper.LOCAL_INDEX_INFO);
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		final OsmandApplication application = (OsmandApplication) context.getApplicationContext();
		final OsmandSettings settings = application.getSettings();

		if (!preferenceDownloadViaWiFi(localIndexInfo, settings).get() || wifi.isWifiEnabled()) {
			new PerformLiveUpdateAsyncTask(context, localIndexInfo).execute(fileName);
		} else {
			PerformLiveUpdateAsyncTask.tryRescheduleDownload(context, settings, localIndexInfo);
		}
	}
}
