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
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;

public class PerformLiveUpdateAsyncTask
		extends AsyncTask<String, Object, IncrementalChangesManager.IncrementalUpdateList> {
	private final Context context;
	private final LocalIndexInfo localIndexInfo;
	private final boolean forceUpdate;

	public PerformLiveUpdateAsyncTask(Context context, LocalIndexInfo localIndexInfo,
									  boolean forceUpdate) {
		this.context = context;
		this.localIndexInfo = localIndexInfo;
		this.forceUpdate = forceUpdate;
	}

	@Override
	protected void onPreExecute() {
		if (context instanceof AbstractDownloadActivity) {
			AbstractDownloadActivity activity = (AbstractDownloadActivity) context;
			activity.setSupportProgressBarIndeterminateVisibility(true);
			OsmandSettings settings = activity.getMyApplication().getSettings();
		}
		final OsmandApplication myApplication = getMyApplication();
		OsmandSettings.CommonPreference<Long> lastCheckPreference =
				LiveUpdatesHelper.preferenceLastCheck(localIndexInfo, myApplication.getSettings());
		lastCheckPreference.set(System.currentTimeMillis());
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) context.getApplicationContext();
	}

	@Override
	protected IncrementalChangesManager.IncrementalUpdateList doInBackground(String... params) {
		final OsmandApplication myApplication = getMyApplication();
		IncrementalChangesManager cm = myApplication.getResourceManager().getChangesManager();
		return cm.getUpdatesByMonth(params[0]);
	}

	@Override
	protected void onPostExecute(IncrementalChangesManager.IncrementalUpdateList result) {
		if (context instanceof AbstractDownloadActivity) {
			AbstractDownloadActivity activity = (AbstractDownloadActivity) context;
			activity.setSupportProgressBarIndeterminateVisibility(false);
		}
		final OsmandApplication application = getMyApplication();
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
				ArrayList<IndexItem> itemsToDownload = new ArrayList<>(ll.size());
				for (IncrementalChangesManager.IncrementalUpdate iu : ll) {
					IndexItem indexItem = new IndexItem(iu.fileName, "Incremental update",
							iu.timestamp, iu.sizeText, iu.contentSize,
							iu.containerSize, DownloadActivityType.LIVE_UPDATES_FILE);
					itemsToDownload.add(indexItem);
				}
				DownloadIndexesThread downloadThread = application.getDownloadThread();
				if (context instanceof DownloadIndexesThread.DownloadEvents) {
					downloadThread.setUiActivity((DownloadIndexesThread.DownloadEvents) context);
				}
				boolean downloadViaWiFi =
						LiveUpdatesHelper.preferenceDownloadViaWiFi(localIndexInfo, settings).get();
				if (getMyApplication().getSettings().isInternetConnectionAvailable()) {
					if (forceUpdate || settings.isWifiConnected() || !downloadViaWiFi) {
						long szLong = 0;
						int i = 0;
						for (IndexItem es : downloadThread.getCurrentDownloadingItems()) {
							szLong += es.getContentSize();
							i++;
						}
						for (IndexItem es : itemsToDownload) {
							szLong += es.getContentSize();
							i++;
						}
						double sz = ((double) szLong) / (1 << 20);
						// get availabile space
						double asz = downloadThread.getAvailableSpace();
						if (asz == -1 || asz <= 0 || sz / asz <= 0.4) {
							IndexItem[] itemsArray = new IndexItem[itemsToDownload.size()];
							itemsArray = itemsToDownload.toArray(itemsArray);
							downloadThread.runDownloadFiles(itemsArray);
							if (context instanceof DownloadIndexesThread.DownloadEvents) {
								((DownloadIndexesThread.DownloadEvents) context).downloadInProgress();
							}
						}
					}
				}
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
			intent.putExtra(LiveUpdatesHelper.LOCAL_INDEX_INFO, localIndexInfo);
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
