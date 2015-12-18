package net.osmand.plus.liveupdates;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.widget.Toast;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.List;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceDownloadViaWiFi;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;

public class LiveUpdatesAlarmReceiver extends BroadcastReceiver {
	private static final Log LOG = PlatformUtil.getLog(LiveUpdatesAlarmReceiver.class);

	@Override
	public void onReceive(Context context, Intent intent) {
		String fileName = intent.getAction();
		LocalIndexInfo localIndexInfo =
				intent.getParcelableExtra(LiveUpdatesSettingsDialogFragment.LOCAL_INDEX_INFO);
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

		final OsmandApplication application = (OsmandApplication) context.getApplicationContext();
		final OsmandSettings settings = application.getSettings();

		if (!preferenceDownloadViaWiFi(localIndexInfo, settings).get() || wifi.isWifiEnabled()) {
			new PerformLiveUpdateAsyncTask(context, localIndexInfo).execute(fileName);
		} else {
			tryRescheduleDownload(context, settings, localIndexInfo);
		}
	}

	public static class PerformLiveUpdateAsyncTask
			extends AsyncTask<String, Object, IncrementalChangesManager.IncrementalUpdateList> {
		private final Context context;
		private final LocalIndexInfo localIndexInfo;

		public PerformLiveUpdateAsyncTask(Context context, LocalIndexInfo localIndexInfo) {
			this.context = context;
			this.localIndexInfo = localIndexInfo;
		}

		@Override
		protected IncrementalChangesManager.IncrementalUpdateList doInBackground(String... params) {
			final OsmandApplication myApplication = (OsmandApplication) context.getApplicationContext();
			IncrementalChangesManager cm = myApplication.getResourceManager().getChangesManager();
			return cm.getUpdatesByMonth(params[0]);
		}

		protected void onPostExecute(IncrementalChangesManager.IncrementalUpdateList result) {
			final OsmandApplication application = (OsmandApplication) context.getApplicationContext();
			final OsmandSettings settings = application.getSettings();
			if (result.errorMessage != null) {
				Toast.makeText(context, result.errorMessage, Toast.LENGTH_SHORT).show();
				tryRescheduleDownload(context, settings, localIndexInfo);
			} else {
				settings.LIVE_UPDATES_RETRIES.resetToDefault();
				List<IncrementalChangesManager.IncrementalUpdate> ll = result.getItemsForUpdate();
				if (ll.isEmpty()) {
					Toast.makeText(context, R.string.no_updates_available, Toast.LENGTH_SHORT).show();
				} else {
					int i = 0;
					IndexItem[] is = new IndexItem[ll.size()];
					for (IncrementalChangesManager.IncrementalUpdate iu : ll) {
						IndexItem ii = new IndexItem(iu.fileName, "Incremental update",
								iu.timestamp, iu.sizeText, iu.contentSize,
								iu.containerSize, DownloadActivityType.LIVE_UPDATES_FILE);
						is[i++] = ii;
					}
					DownloadValidationManager downloadValidationManager =
							new DownloadValidationManager(application);
					downloadValidationManager.startDownload(context, is);
				}
			}
		}
	}

	private static void tryRescheduleDownload(Context context, OsmandSettings settings,
											  LocalIndexInfo localIndexInfo) {
		final OsmandSettings.CommonPreference<Integer> updateFrequencyPreference =
				preferenceUpdateFrequency(localIndexInfo, settings);
		final Integer frequencyOrdinal = updateFrequencyPreference.get();
		if (UpdateFrequency.values()[frequencyOrdinal]
				== UpdateFrequency.HOURLY) {
			return;
		}
		final Integer retriesLeft = settings.LIVE_UPDATES_RETRIES.get();
		if (retriesLeft > 0) {
			Intent intent = new Intent(context, LiveUpdatesAlarmReceiver.class);
			final File file = new File(localIndexInfo.getFileName());
			final String fileName = Algorithms.getFileNameWithoutExtension(file);
			intent.putExtra(LiveUpdatesSettingsDialogFragment.LOCAL_INDEX_INFO, localIndexInfo);
			intent.setAction(fileName);
			PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

			long timeToRetry = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR;

			AlarmManager alarmMgr = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			alarmMgr.set(AlarmManager.RTC, timeToRetry, alarmIntent);
			settings.LIVE_UPDATES_RETRIES.set(retriesLeft - 1);
		} else {
			settings.LIVE_UPDATES_RETRIES.resetToDefault();
		}
	}
}
