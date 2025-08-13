package net.osmand.plus.download;

import static net.osmand.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.plus.Version.FULL_VERSION_NAME;
import static net.osmand.plus.download.DownloadOsmandIndexesHelper.getSupportedTtsByLanguages;
import static net.osmand.plus.download.DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.WorldRegion;
import net.osmand.map.WorldRegion.RegionParams;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DatabaseHelper.HistoryDownloadEntry;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.download.IndexItem.DownloadEntry;
import net.osmand.plus.notifications.OsmandNotification.NotificationType;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressLint({"NewApi", "DefaultLocale"})
public class DownloadIndexesThread {

	private static final Log LOG = PlatformUtil.getLog(DownloadIndexesThread.class);

	private final OsmandApplication app;

	private final DatabaseHelper dbHelper;
	private final DownloadFileHelper downloadFileHelper;
	private final List<BasicProgressAsyncTask<?, ?, ?, ?>> currentRunningTask = Collections.synchronizedList(new ArrayList<>());
	private final ConcurrentLinkedQueue<IndexItem> indexItemDownloading = new ConcurrentLinkedQueue<>();

	private DownloadEvents uiActivity;
	private IndexItem currentDownloadingItem;
	private float currentDownloadProgress;
	private DownloadResources indexes;
	private static final int THREAD_ID = 10103;

	public interface DownloadEvents {

		default void onUpdatedIndexesList() {
		}

		default void downloadInProgress() {
		}

		default void downloadingError(@NonNull String error) {
		}

		default void downloadHasFinished() {
		}
	}

	public DownloadIndexesThread(OsmandApplication app) {
		this.app = app;
		indexes = new DownloadResources(app);
		updateLoadedFiles();
		downloadFileHelper = new DownloadFileHelper(app);
		dbHelper = new DatabaseHelper(app);
	}

	public void updateLoadedFiles() {
		indexes.updateLoadedFiles();
	}

	/// UI notifications methods
	public void setUiActivity(DownloadEvents uiActivity) {
		this.uiActivity = uiActivity;
	}

	public void resetUiActivity(DownloadEvents uiActivity) {
		if (this.uiActivity == uiActivity) {
			this.uiActivity = null;
		}
	}

	@UiThread
	protected void downloadHasStarted() {
		boolean shouldStartService = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || uiActivity != null;
		if (shouldStartService) {
			if (app.getDownloadService() == null) {
				startDownloadService();
			}
			if (uiActivity instanceof FragmentActivity) {
				AndroidUtils.requestNotificationPermissionIfNeeded((FragmentActivity) uiActivity);
			}
		}
		updateNotification();
	}

	private void startDownloadService() {
		Intent intent = new Intent(app, DownloadService.class);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			app.startForegroundService(intent);
		} else {
			app.startService(intent);
		}
	}

	@UiThread
	protected void downloadInProgress() {
		if (uiActivity != null) {
			uiActivity.downloadInProgress();
		}
		updateNotification();
	}

	@UiThread
	protected void downloadingError(@NonNull String error) {
		if (uiActivity != null) {
			uiActivity.downloadingError(error);
		}
	}

	@UiThread
	protected void downloadHasFinished() {
		if (uiActivity != null) {
			uiActivity.downloadHasFinished();
		}
		updateNotification();
		if (app.getDownloadService() != null) {
			app.getDownloadService().stopService(app);
		}
		app.getAvoidSpecificRoads().initRouteObjects(true);
	}

	public void initSettingsFirstMap(WorldRegion reg) {
		if (app.getSettings().FIRST_MAP_IS_DOWNLOADED.get() || reg == null) {
			return;
		}
		app.getSettings().FIRST_MAP_IS_DOWNLOADED.set(true);
		RegionParams params = reg.getParams();
		if (!app.getSettings().DRIVING_REGION_AUTOMATIC.get()) {
			app.setupDrivingRegion(reg);
		}
		String lang = params.getRegionLang();
		if (lang != null) {
			String lng = lang.split(",")[0];
			String setTts = null;
			Set<String> supportedTtsLanguages = getSupportedTtsByLanguages(app).keySet();
			for (String s : supportedTtsLanguages) {
				if (lng.startsWith(s)) {
					setTts = s + IndexConstants.VOICE_PROVIDER_SUFFIX;
					break;
				} else if (lng.contains("," + s)) {
					setTts = s + IndexConstants.VOICE_PROVIDER_SUFFIX;
				}
			}
			if (setTts != null) {
				app.getSettings().VOICE_PROVIDER.set(setTts);
			}
		}
	}

	@UiThread
	protected void onUpdatedIndexesList() {
		if (uiActivity != null) {
			uiActivity.onUpdatedIndexesList();
		}
	}


	// PUBLIC API


	public DownloadResources getIndexes() {
		return indexes;
	}

	public List<IndexItem> getCurrentDownloadingItems() {
		List<IndexItem> res = new ArrayList<>();
		IndexItem ii = currentDownloadingItem;
		if (ii != null) {
			res.add(ii);
		}
		res.addAll(indexItemDownloading);
		return res;
	}

	public boolean isDownloading() {
		return !indexItemDownloading.isEmpty() || currentDownloadingItem != null;
	}

	public boolean isDownloading(IndexItem item) {
		if (item == currentDownloadingItem) {
			return true;
		}
		for (IndexItem ii : indexItemDownloading) {
			if (ii == item) {
				return true;
			}
		}
		return false;
	}

	public int getCountedDownloads() {
		int i = 0;
		if (currentDownloadingItem != null && DownloadActivityType.isCountedInDownloads(currentDownloadingItem)) {
			i++;
		}
		for (IndexItem ii : indexItemDownloading) {
			if (DownloadActivityType.isCountedInDownloads(ii)) {
				i++;
			}
		}
		return i;
	}

	public void runReloadIndexFilesSilent() {
		if (checkRunning(true)) {
			return;
		}
		OsmAndTaskManager.executeTask(new ReloadIndexesTask());
	}

	public void runReloadIndexFiles() {
		if (checkRunning(false)) {
			return;
		}
		OsmAndTaskManager.executeTask(new ReloadIndexesTask());
	}

	public void runDownloadFiles(IndexItem... items) {
		if (getCurrentRunningTask() instanceof ReloadIndexesTask) {
			if (checkRunning(false)) {
				return;
			}
		}
		if (uiActivity instanceof Activity) {
			app.logEvent("download_files");
		}
		for (IndexItem item : items) {
			if (!isCurrentDownloading(item) && !indexItemDownloading.contains(item)) {
				indexItemDownloading.add(item);
			}
		}
		if (currentDownloadingItem == null) {
			OsmAndTaskManager.executeTask(new DownloadIndexesAsyncTask());
		} else {
			downloadInProgress();
		}
	}

	public void cancelDownload(DownloadItem item) {
		if (item instanceof MultipleDownloadItem) {
			MultipleDownloadItem multipleDownloadItem = (MultipleDownloadItem) item;
			cancelDownload(multipleDownloadItem.getAllIndexes());
		} else if (item instanceof SrtmDownloadItem) {
			IndexItem indexItem = ((SrtmDownloadItem) item).getIndexItem(this);
			cancelDownload(indexItem);
		} else if (item instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) item;
			cancelDownload(indexItem);
		}
	}

	public void cancelDownload(List<IndexItem> items) {
		if (items == null) return;
		boolean updateProgress = false;
		for (IndexItem item : items) {
			if (!isCurrentDownloading(item)) {
				updateProgress = true;
			}
			cancelDownload(item, false);
		}
		if (updateProgress) {
			downloadInProgress();
		}
	}

	public void cancelDownload(IndexItem item) {
		cancelDownload(item, true);
	}

	public void cancelDownload(IndexItem item, boolean forceUpdateProgress) {
		app.logMapDownloadEvent("cancel", item);
		if (isCurrentDownloading(item)) {
			downloadFileHelper.setInterruptDownloading(true);
		} else {
			indexItemDownloading.remove(item);
			if (forceUpdateProgress) {
				downloadInProgress();
			}
		}
	}

	public boolean isCurrentDownloading(@NonNull DownloadItem downloadItem) {
		return downloadItem.equals(getCurrentDownloadingItem());
	}

	public IndexItem getCurrentDownloadingItem() {
		return currentDownloadingItem;
	}

	public float getCurrentDownloadProgress() {
		return currentDownloadProgress;
	}

	public BasicProgressAsyncTask<?, ?, ?, ?> getCurrentRunningTask() {
		for (int i = 0; i < currentRunningTask.size(); ) {
			if (currentRunningTask.get(i).getStatus() == Status.FINISHED) {
				currentRunningTask.remove(i);
			} else {
				i++;
			}
		}
		if (currentRunningTask.size() > 0) {
			return currentRunningTask.get(0);
		}
		return null;
	}

	public double getAvailableSpace() {
		return (double) AndroidUtils.getAvailableSpace(app) / (1 << 20);
	}

	public boolean shouldDownloadIndexes() {
		return app.getSettings().isInternetConnectionAvailable()
				&& !indexes.isDownloadedFromInternet
				&& !indexes.downloadFromInternetFailed;
	}

	/// PRIVATE IMPL

	private boolean checkRunning(boolean silent) {
		if (getCurrentRunningTask() != null) {
			if (!silent) {
				app.showShortToastMessage(R.string.wait_current_task_finished);
			}
			return true;
		}
		return false;
	}

	private void updateNotification() {
		app.getNotificationHelper().refreshNotification(NotificationType.DOWNLOAD);
	}

	private class ReloadIndexesTask extends BasicProgressAsyncTask<Void, Void, Void, DownloadResources> {

		public ReloadIndexesTask() {
			super(app);
		}

		@Override
		protected void onPreExecute() {
			currentRunningTask.add(this);
			super.onPreExecute();
			this.message = ctx.getString(R.string.downloading_list_indexes);
			indexes.downloadFromInternetFailed = false;
		}

		@Override
		protected DownloadResources doInBackground(Void... params) {
			TrafficStats.setThreadStatsTag(THREAD_ID);
			DownloadResources result = null;
			DownloadOsmandIndexesHelper.IndexFileList indexFileList = DownloadOsmandIndexesHelper.getIndexesList(ctx);
			try {
				while (app.isApplicationInitializing()) {
					Thread.sleep(200);
				}
				result = new DownloadResources(app);
				result.isDownloadedFromInternet = indexFileList.isDownloadedFromInternet();
				result.mapVersionIsIncreased = indexFileList.isIncreasedMapVersion();
				app.getSettings().LAST_CHECKED_UPDATES.set(System.currentTimeMillis());
				result.prepareData(indexFileList.getIndexFiles());
			} catch (Exception e) {
				LOG.error(e);
			}
			return result == null ? new DownloadResources(app) : result;
		}

		protected void onPostExecute(DownloadResources result) {
			indexes = result;
			result.downloadFromInternetFailed = !result.isDownloadedFromInternet;
			if (result.mapVersionIsIncreased) {
				showWarnDialog();
			}
			currentRunningTask.remove(this);
			onUpdatedIndexesList();
		}

		private void showWarnDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setMessage(R.string.map_version_changed_info);
			builder.setPositiveButton(R.string.button_upgrade_osmandplus, (dialog, which) -> {
				Uri uri = Uri.parse(Version.getUrlWithUtmRef(app, FULL_VERSION_NAME));
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				AndroidUtils.startActivityIfSafe(ctx, intent);
			});
			builder.setNegativeButton(R.string.shared_string_cancel, (dialog, which) -> dialog.dismiss());
			builder.show();
		}

		@Override
		protected void updateProgress(boolean updateOnlyProgress, Void tag) {
			downloadInProgress();
		}
	}


	private class DownloadIndexesAsyncTask extends BasicProgressAsyncTask<IndexItem, IndexItem, Object, String> implements DownloadFileShowWarning {

		private final OsmandPreference<Integer> downloads;


		public DownloadIndexesAsyncTask() {
			super(app);
			downloads = app.getSettings().NUMBER_OF_FREE_DOWNLOADS;
		}

		@Override
		public void setInterrupted(boolean interrupted) {
			super.setInterrupted(interrupted);
			if (interrupted) {
				downloadFileHelper.setInterruptDownloading(true);
			}
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			for (Object o : values) {
				if (o instanceof IndexItem) {
					IndexItem item = (IndexItem) o;
					String name = item.getBasename();
					int count = dbHelper.getCount(name, DatabaseHelper.DOWNLOAD_ENTRY) + 1;
					item.setDownloaded(true);
					HistoryDownloadEntry entry = new HistoryDownloadEntry(name, count);
					if (count == 1) {
						dbHelper.add(entry, DatabaseHelper.DOWNLOAD_ENTRY);
					} else {
						dbHelper.update(entry, DatabaseHelper.DOWNLOAD_ENTRY);
					}
				} else if (o instanceof String) {
					String message = (String) o;
					boolean success = message.equals(app.getString(R.string.shared_string_download_successful));
					if (!success) {
						if (!message.toLowerCase().contains("interrupted")
								&& !message.equals(DownloadValidationManager.getFreeVersionMessage(app))) {
							app.showToastMessage(message);
						}
						downloadingError(message);
					}
				}
			}
			downloadInProgress();
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPreExecute() {
			currentRunningTask.add(this);
			super.onPreExecute();
			downloadFileHelper.setInterruptDownloading(false);
			if (uiActivity instanceof Activity) {
				View mainView = ((Activity) uiActivity).findViewById(R.id.MainLayout);
				if (mainView != null) {
					mainView.setKeepScreenOn(true);
				}
			}
			startTask(ctx.getString(R.string.shared_string_downloading), -1);
			downloadHasStarted();
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null && result.length() > 0) {
				app.showToastMessage(result);
			}
			if (uiActivity instanceof Activity) {
				View mainView = ((Activity) uiActivity).findViewById(R.id.MainLayout);
				if (mainView != null) {
					mainView.setKeepScreenOn(false);
				}
			}
			currentRunningTask.remove(this);
			indexes.updateFilesToUpdate();
			downloadHasFinished();
		}

		@Override
		protected String doInBackground(IndexItem... filesToDownload) {
			try {
				List<File> filesToReindex = new ArrayList<>();
				boolean forceWifi = downloadFileHelper.isWifiConnected();
				Set<IndexItem> currentDownloads = new HashSet<>();
				StringBuilder warnings = new StringBuilder();
				try {
					while (!indexItemDownloading.isEmpty()) {
						IndexItem item = indexItemDownloading.poll();
						currentDownloadingItem = item;
						resetDownloadProgress();
						if (item == null || currentDownloads.contains(item)) {
							continue;
						}
						currentDownloads.add(item);
						if (!validateEnoughSpace(item) || !validateNotExceedsFreeLimit(item)) {
							break;
						}
						setTag(item);
						boolean updatingFile = item.isDownloaded();
						boolean success = downloadFile(item, filesToReindex, forceWifi);
						if (success) {
							if (DownloadActivityType.isCountedInDownloads(item)) {
								downloads.set(downloads.get() + 1);
							}
							if (item.getBasename().equalsIgnoreCase(DownloadResources.WORLD_SEAMARKS_KEY)) {
								File folder = app.getAppPath(IndexConstants.MAPS_PATH);
								String fileName = DownloadResources.WORLD_SEAMARKS_OLD_NAME
										+ BINARY_MAP_INDEX_EXT;
								File oldFile = new File(folder, fileName);
								Algorithms.removeAllFiles(oldFile);
							}
							PluginsHelper.onIndexItemDownloaded(item, updatingFile);

							File bf = item.getBackupFile(app);
							if (bf.exists()) {
								Algorithms.removeAllFiles(bf);
							}
							publishProgress(item);
							String warning = reindexFiles(filesToReindex);
							if (!Algorithms.isEmpty(warning)) {
								warnings.append(" ").append(warning);
							}
							filesToReindex.clear();
							// slow down but let update all button work properly
							indexes.updateFilesToUpdate();
						}
					}
				} finally {
					currentDownloadingItem = null;
					resetDownloadProgress();
				}
				if (warnings.toString().trim().length() == 0) {
					return null;
				}
				return warnings.toString().trim();
			} catch (InterruptedException e) {
				LOG.info("Download Interrupted");
				// do not dismiss dialog
			}
			return null;
		}

		private boolean validateEnoughSpace(IndexItem item) {
			double asz = getAvailableSpace();
			double cs = ((double) item.contentSize / (1 << 20));
			// validate enough space
			if (asz != -1 && cs > asz) {
				String breakDownloadMessage = app.getString(R.string.download_files_not_enough_space,
						String.valueOf(cs), String.valueOf(asz));
				publishProgress(breakDownloadMessage);
				return false;
			}
			return true;
		}

		private boolean validateNotExceedsFreeLimit(@NonNull IndexItem item) {
			boolean exceed = !Version.isPaidVersion(app)
					&& DownloadActivityType.isCountedInDownloads(item)
					&& downloads.get() >= MAXIMUM_AVAILABLE_FREE_DOWNLOADS;
			if (exceed) {
				publishProgress(DownloadValidationManager.getFreeVersionMessage(app));
			}
			return !exceed;
		}

		private String reindexFiles(List<File> filesToReindex) {
			boolean vectorMapsToReindex = false;
			// reindex vector maps all at one time
			ResourceManager manager = app.getResourceManager();
			for (File f : filesToReindex) {
				if (f.getName().endsWith(BINARY_MAP_INDEX_EXT)) {
					vectorMapsToReindex = true;
				}
			}
			List<String> warnings = new ArrayList<>();
			manager.indexVoiceFiles(this);
			manager.indexFontFiles(this);
			if (vectorMapsToReindex) {
				warnings = manager.indexingMaps(this, filesToReindex);
			}
			List<String> wns = manager.indexAdditionalMaps(this);
			if (wns != null) {
				warnings.addAll(wns);
			}

			if (!warnings.isEmpty()) {
				return warnings.get(0);
			}
			return null;
		}

		@Override
		public void showWarning(String warning) {
			publishProgress(warning);
		}

		public boolean downloadFile(IndexItem item, List<File> filesToReindex, boolean forceWifi)
				throws InterruptedException {
			downloadFileHelper.setInterruptDownloading(false);
			DownloadEntry de = item.createDownloadEntry(app);
			boolean result = false;
			if (de == null) {
				return false;
			} else if (de.isAsset) {
				try {
					if (ctx != null) {
						boolean changedDate = ResourceManager.copyAssets(ctx.getAssets(), de.assetName, de.targetFile, de.dateModified);
						if (!changedDate) {
							LOG.error("Set last timestamp is not supported");
						}
						result = true;
					}
				} catch (IOException e) {
					LOG.error("Copy exception", e);
				}
			} else {
				long start = System.currentTimeMillis();
				app.logMapDownloadEvent("start", item);
				result = downloadFileHelper.downloadFile(de, this, filesToReindex, this, forceWifi);
				long time = System.currentTimeMillis() - start;
				if (result) {
					app.logMapDownloadEvent("done", item, time);
					if (item.isHidden()) {
						File nonHiddenFile = item.getDefaultTargetFile(app);
						if (nonHiddenFile.exists()) {
							nonHiddenFile.delete();
						}
					}
					checkDownload(item, time);
				} else {
					app.logMapDownloadEvent("failed", item, time);
				}
			}
			return result;
		}

		@Override
		protected void updateProgress(boolean updateOnlyProgress, IndexItem tag) {
			currentDownloadProgress = getDownloadProgress();
			downloadInProgress();
		}

		private void resetDownloadProgress() {
			currentDownloadProgress = 0;
		}
	}

	private void checkDownload(IndexItem item, long downloadTime) {
		Map<String, String> params = new HashMap<>();
		params.put("file_name", item.fileName);
		params.put("file_size", item.size);
		int downloadTimeSec = (int) (downloadTime / 1000L);
		params.put("download_time", String.valueOf(downloadTimeSec));

		String url = AndroidNetworkUtils.getHttpProtocol() + "osmand.net/api/check_download";
		AndroidNetworkUtils.sendRequestAsync(app, url, params, "Check download", false, false, null);
	}
}
