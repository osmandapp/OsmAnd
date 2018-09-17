package net.osmand.plus.liveupdates;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.IncrementalChangesManager;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;

public class PerformLiveUpdateAsyncTask
		extends AsyncTask<String, Object, IncrementalChangesManager.IncrementalUpdateList> {
	private final static Log LOG = PlatformUtil.getLog(PerformLiveUpdateAsyncTask.class);

	@NonNull
	private final Context context;
	@NonNull
	private final String localIndexFileName;
	private final boolean userRequested;

	public PerformLiveUpdateAsyncTask(@NonNull Context context,
									  @NonNull String localIndexFileName,
									  boolean userRequested) {
		this.context = context;
		this.localIndexFileName = localIndexFileName;
		this.userRequested = userRequested;
	}

	@Override
	protected void onPreExecute() {
		LOG.debug("onPreExecute");
		if (context instanceof AbstractDownloadActivity) {
			AbstractDownloadActivity activity = (AbstractDownloadActivity) context;
			activity.setSupportProgressBarIndeterminateVisibility(true);
		}
		final OsmandApplication myApplication = getMyApplication();
		OsmandSettings.CommonPreference<Long> lastCheckPreference =
				LiveUpdatesHelper.preferenceLastCheck(localIndexFileName, myApplication.getSettings());
		lastCheckPreference.set(System.currentTimeMillis());
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) context.getApplicationContext();
	}

	@Override
	protected IncrementalChangesManager.IncrementalUpdateList doInBackground(String... params) {
		LOG.debug("doInBackground");
		final OsmandApplication myApplication = getMyApplication();
		IncrementalChangesManager cm = myApplication.getResourceManager().getChangesManager();
		return cm.getUpdatesByMonth(params[0]);
	}

	@Override
	protected void onPostExecute(IncrementalChangesManager.IncrementalUpdateList result) {
		LOG.debug("onPostExecute");
		if (context instanceof AbstractDownloadActivity) {
			AbstractDownloadActivity activity = (AbstractDownloadActivity) context;
			activity.setSupportProgressBarIndeterminateVisibility(false);
		}
		final OsmandApplication application = getMyApplication();
		final OsmandSettings settings = application.getSettings();
		if (result.errorMessage != null) {
			LOG.info(result.errorMessage);
			if (userRequested) {
				application.showShortToastMessage(result.errorMessage);
			}
			tryRescheduleDownload(context, settings, localIndexFileName);
		} else {
			settings.LIVE_UPDATES_RETRIES.resetToDefault();
			List<IncrementalChangesManager.IncrementalUpdate> ll = result.getItemsForUpdate();
			if (ll != null && !ll.isEmpty()) {
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
						LiveUpdatesHelper.preferenceDownloadViaWiFi(localIndexFileName, settings).get();
				if (getMyApplication().getSettings().isInternetConnectionAvailable()) {
					if (userRequested || settings.isWifiConnected() || !downloadViaWiFi) {
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
			} else {
				if (context instanceof DownloadIndexesThread.DownloadEvents) {
					((DownloadIndexesThread.DownloadEvents) context).downloadInProgress();
					if (userRequested && context instanceof OsmLiveActivity) {
						application.showShortToastMessage(R.string.no_updates_available);
					}
				}
			}
		}
	}

	public static void tryRescheduleDownload(@NonNull Context context,
											 @NonNull OsmandSettings settings,
											 @NonNull String localIndexFileName) {
		final OsmandSettings.CommonPreference<Integer> updateFrequencyPreference =
				preferenceUpdateFrequency(localIndexFileName, settings);
		final Integer frequencyOrdinal = updateFrequencyPreference.get();
		if (LiveUpdatesHelper.UpdateFrequency.values()[frequencyOrdinal]
				== LiveUpdatesHelper.UpdateFrequency.HOURLY) {
			return;
		}
		final Integer retriesLeft = settings.LIVE_UPDATES_RETRIES.get();
		if (retriesLeft > 0) {
			PendingIntent alarmIntent = LiveUpdatesHelper.getPendingIntent(context, localIndexFileName);

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
