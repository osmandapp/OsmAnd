package net.osmand.plus.liveupdates;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.List;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;

public class PerformLiveUpdateAsyncTask
		extends AsyncTask<String, Object, IncrementalChangesManager.IncrementalUpdateList> {
	private final Context context;
	private final LocalIndexInfo localIndexInfo;

	public PerformLiveUpdateAsyncTask(Context context, LocalIndexInfo localIndexInfo) {
		this.context = context;
		this.localIndexInfo = localIndexInfo;
	}

	protected void onPreExecute() {
		if (context instanceof AbstractDownloadActivity) {
			AbstractDownloadActivity activity = (AbstractDownloadActivity) context;
			activity.setSupportProgressBarIndeterminateVisibility(true);
			OsmandSettings settings = activity.getMyApplication().getSettings();
		}
		final OsmandApplication myApplication = (OsmandApplication) context.getApplicationContext();
		OsmandSettings.CommonPreference<Long> lastCheckPreference =
				LiveUpdatesHelper.preferenceLastCheck(localIndexInfo, myApplication.getSettings());
		lastCheckPreference.set(System.currentTimeMillis());
	}

	@Override
	protected IncrementalChangesManager.IncrementalUpdateList doInBackground(String... params) {
		final OsmandApplication myApplication = (OsmandApplication) context.getApplicationContext();
		IncrementalChangesManager cm = myApplication.getResourceManager().getChangesManager();
		return cm.getUpdatesByMonth(params[0]);
	}

	protected void onPostExecute(IncrementalChangesManager.IncrementalUpdateList result) {
		if (context instanceof AbstractDownloadActivity) {
			AbstractDownloadActivity activity = (AbstractDownloadActivity) context;
			activity.setSupportProgressBarIndeterminateVisibility(false);
		}
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

	public static void tryRescheduleDownload(Context context, OsmandSettings settings,
											  LocalIndexInfo localIndexInfo) {
		final OsmandSettings.CommonPreference<Integer> updateFrequencyPreference =
				preferenceUpdateFrequency(localIndexInfo, settings);
		final Integer frequencyOrdinal = updateFrequencyPreference.get();
		if (LiveUpdatesHelper.UpdateFrequency.values()[frequencyOrdinal]
				== LiveUpdatesHelper.UpdateFrequency.HOURLY) {
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
