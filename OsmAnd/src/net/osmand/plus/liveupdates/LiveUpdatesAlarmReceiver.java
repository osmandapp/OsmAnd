package net.osmand.plus.liveupdates;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.LOCAL_INDEX_INFO;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.isUpdateOverdue;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceDownloadViaWiFi;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

public class LiveUpdatesAlarmReceiver extends BroadcastReceiver {

	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesAlarmReceiver.class);

	@Override
	public void onReceive(Context context, Intent intent) {
		String fileName = intent.getAction();
		String localIndexInfoFile = intent.getStringExtra(LOCAL_INDEX_INFO);
		if (localIndexInfoFile == null) {
			LOG.error("Unexpected: localIndexInfoFile is null");
			return;
		}
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		OsmandApplication application = (OsmandApplication) context.getApplicationContext();
		OsmandSettings settings = application.getSettings();
		AppInitializer initializer = application.getAppInitializer();

		UpdateFrequency updateFrequency = UpdateFrequency.values()[preferenceUpdateFrequency(localIndexInfoFile, settings).get()];
		if (initializer.isAppInitializing() && isUpdateOverdue(localIndexInfoFile, updateFrequency, settings)) {
			// The alarm started the app: AppInitializer.checkLiveUpdatesAlerts runs this overdue update
			return;
		}
		if (!preferenceDownloadViaWiFi(localIndexInfoFile, settings).get() || wifi.isWifiEnabled()) {
			// Updates are found only after the maps are indexed during app initialization
			initializer.addOnFinishListener(init ->
					OsmAndTaskManager.executeTask(new PerformLiveUpdateAsyncTask(context, localIndexInfoFile, false), fileName));
		} else {
			PerformLiveUpdateAsyncTask.tryRescheduleDownload(context, settings, localIndexInfoFile);
		}
	}
}
