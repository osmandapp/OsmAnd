package net.osmand.plus.liveupdates;

import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastOsmChange;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastSuccessfulUpdateCheck;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.AbstractDownloadActivity;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import net.osmand.plus.resources.IncrementalChangesManager;
import net.osmand.plus.resources.IncrementalChangesManager.IncrementalUpdate;
import net.osmand.plus.resources.IncrementalChangesManager.IncrementalUpdateList;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class PerformLiveUpdateAsyncTask
		extends AsyncTask<String, Object, IncrementalChangesManager.IncrementalUpdateList> {

	private static final Log LOG = PlatformUtil.getLog(PerformLiveUpdateAsyncTask.class);

	private final OsmandApplication app;
	private final OsmandSettings settings;
	@NonNull
	private final Context context;
	@NonNull
	private final String localIndexFileName;
	private final boolean userRequested;
	private Runnable runOnSuccess;

	public PerformLiveUpdateAsyncTask(@NonNull Context context,
	                                  @NonNull String localIndexFileName,
	                                  boolean userRequested) {
		this.context = context;
		this.app = getMyApplication();
		this.settings = app.getSettings();
		this.localIndexFileName = localIndexFileName;
		this.userRequested = userRequested;
	}

	public void setRunOnSuccess(Runnable runOnSuccess) {
		this.runOnSuccess = runOnSuccess;
	}

	@Override
	protected void onPreExecute() {
		LOG.debug("onPreExecute");
		if (context instanceof AbstractDownloadActivity) {
			AbstractDownloadActivity activity = (AbstractDownloadActivity) context;
			activity.setSupportProgressBarIndeterminateVisibility(true);
		}
	}

	@Override
	protected IncrementalUpdateList doInBackground(String... params) {
		LOG.debug("doInBackground");
		IncrementalChangesManager changesManager = app.getResourceManager().getChangesManager();
		return changesManager.getUpdatesByMonth(params[0]);
	}

	@Override
	protected void onPostExecute(IncrementalUpdateList result) {
		if (context instanceof AbstractDownloadActivity) {
			AbstractDownloadActivity activity = (AbstractDownloadActivity) context;
			activity.setSupportProgressBarIndeterminateVisibility(false);
		}
		if (result.errorMessage != null) {
			LOG.info("Error message: " + result.errorMessage);
			if (userRequested) {
				app.showShortToastMessage(result.errorMessage);
			}
			tryRescheduleDownload(context, settings, localIndexFileName);
		} else {
			settings.LIVE_UPDATES_RETRIES.resetToDefault();
			List<IncrementalUpdate> updates = result.getItemsForUpdate();
			LOG.debug("Updates quantity: " + (updates == null ? "null" : updates.size()));
			boolean hasUpdates = !Algorithms.isEmpty(updates);
			if (hasUpdates) {
				long lastMapUpdateTimestamp = 0;
				List<IndexItem> itemsToDownload = new ArrayList<>(updates.size());
				for (IncrementalUpdate update : updates) {
					if (update.timestamp > lastMapUpdateTimestamp) {
						lastMapUpdateTimestamp = update.timestamp;
					}
					IndexItem indexItem = new IndexItem(update.fileName, "Incremental update",
							update.timestamp, update.sizeText, update.contentSize,
							update.containerSize, DownloadActivityType.LIVE_UPDATES_FILE, false, null, false);
					itemsToDownload.add(indexItem);
				}
				LOG.debug("Items to download size: " + itemsToDownload.size());
				DownloadIndexesThread downloadThread = app.getDownloadThread();
				if (context instanceof DownloadEvents) {
					downloadThread.setUiActivity((DownloadEvents) context);
				}
				boolean downloadViaWiFi =
						LiveUpdatesHelper.preferenceDownloadViaWiFi(localIndexFileName, settings).get();

				LOG.debug("Internet connection available: " + settings.isInternetConnectionAvailable());
				LOG.debug("Download via Wifi: " + downloadViaWiFi);
				LOG.debug("Is wifi available: " + settings.isWifiConnected());
				if (settings.isInternetConnectionAvailable()) {
					if (userRequested || settings.isWifiConnected() || !downloadViaWiFi) {
						long szLong = 0;
						for (IndexItem es : downloadThread.getCurrentDownloadingItems()) {
							szLong += es.getContentSize();
						}
						for (IndexItem es : itemsToDownload) {
							szLong += es.getContentSize();
						}
						double sz = ((double) szLong) / (1 << 20);
						double asz = downloadThread.getAvailableSpace();

						LOG.debug("Download size: " + sz + ", available space: " + asz);
						if (asz <= 0 || sz / asz <= 0.4) {
							IndexItem[] itemsArray = new IndexItem[itemsToDownload.size()];
							itemsArray = itemsToDownload.toArray(itemsArray);
							downloadThread.runDownloadFiles(itemsArray);
							if (context instanceof DownloadEvents) {
								((DownloadEvents) context).downloadInProgress();
							}
							updateTimestamps(lastMapUpdateTimestamp);
						} else {
							LOG.debug("onPostExecute: Not enough space for updates");
						}
					}
					LOG.debug("onPostExecute: No internet connection");
				}
			} else if (context instanceof DownloadEvents) {
				((DownloadEvents) context).downloadInProgress();
				if (userRequested && context instanceof DownloadActivity) {
					updateTimestamps(0);
					app.showShortToastMessage(R.string.no_updates_available);
				}
			}
		}
	}

	public static void tryRescheduleDownload(@NonNull Context context,
	                                         @NonNull OsmandSettings settings,
	                                         @NonNull String localIndexFileName) {
		CommonPreference<Integer> updateFrequencyPreference =
				preferenceUpdateFrequency(localIndexFileName, settings);
		Integer frequencyOrdinal = updateFrequencyPreference.get();
		if (UpdateFrequency.values()[frequencyOrdinal] == UpdateFrequency.HOURLY) {
			return;
		}
		Integer retriesLeft = settings.LIVE_UPDATES_RETRIES.get();
		if (retriesLeft > 0) {
			PendingIntent alarmIntent = LiveUpdatesHelper.getPendingIntent(context, localIndexFileName);

			long timeToRetry = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR;

			AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			alarmMgr.set(AlarmManager.RTC, timeToRetry, alarmIntent);
			settings.LIVE_UPDATES_RETRIES.set(retriesLeft - 1);
		} else {
			settings.LIVE_UPDATES_RETRIES.resetToDefault();
		}
	}

	private void updateTimestamps(long lastMapChangeTimestamp) {
		AndroidNetworkUtils.sendRequestAsync(
				app, LiveUpdatesFragment.URL, null, "Requesting map updates info...", false, false, (result, error, resultCode) -> {
					long lastServerUpdate = 0;
					if (!Algorithms.isEmpty(result)) {
						SimpleDateFormat source = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
						source.setTimeZone(TimeZone.getTimeZone("UTC"));
						try {
							Date parsed = source.parse(result);
							if (parsed != null) {
								lastServerUpdate = parsed.getTime();
							}
						} catch (ParseException e) {
							LOG.error(e.getMessage(), e);
						}
					}

					updateTimestamps(lastMapChangeTimestamp, lastServerUpdate);
					if (runOnSuccess != null) {
						runOnSuccess.run();
					}
				});
	}

	private void updateTimestamps(long lastMapChangeTimestamp, long lastServerUpdate) {
		long time = System.currentTimeMillis();
		preferenceLastSuccessfulUpdateCheck(localIndexFileName, settings).set(time);

		long lastOsmChangeTimestamp = Math.max(lastMapChangeTimestamp, lastServerUpdate);
		if (lastOsmChangeTimestamp > 0) {
			preferenceLastOsmChange(localIndexFileName, settings).set(lastOsmChangeTimestamp);
		}
	}

	@NonNull
	private OsmandApplication getMyApplication() {
		return (OsmandApplication) context.getApplicationContext();
	}
}
