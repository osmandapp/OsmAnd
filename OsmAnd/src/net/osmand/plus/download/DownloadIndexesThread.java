package net.osmand.plus.download;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.view.View;
import android.widget.Toast;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.map.WorldRegion;
import net.osmand.map.WorldRegion.RegionParams;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.helpers.DatabaseHelper;
import net.osmand.plus.notifications.OsmandNotification;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;

@SuppressLint({ "NewApi", "DefaultLocale" })
public class DownloadIndexesThread {

	private final static Log LOG = PlatformUtil.getLog(DownloadIndexesThread.class);

	private final OsmandApplication app;

	private final DatabaseHelper dbHelper;
	private final DownloadFileHelper downloadFileHelper;
	private final List<BasicProgressAsyncTask<?, ?, ?, ?>> currentRunningTask = Collections.synchronizedList(new ArrayList<>());
	private final ConcurrentLinkedQueue<IndexItem> indexItemDownloading = new ConcurrentLinkedQueue<>();

	private DownloadEvents uiActivity = null;
	private IndexItem currentDownloadingItem = null;
	private int currentDownloadingItemProgress = 0;
	private DownloadResources indexes;
	private static final int THREAD_ID = 10103;

	public interface DownloadEvents {
		
		void onUpdatedIndexesList();
		
		void downloadInProgress();
		
		void downloadHasFinished();
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
		if (app.getDownloadService() == null) {
			app.startDownloadService();
		}
		updateNotification();
	}

	@UiThread
	protected void downloadInProgress() {
		if (uiActivity != null) {
			uiActivity.downloadInProgress();
		}
		updateNotification();
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
			for (String s : OsmandSettings.TTS_AVAILABLE_VOICES) {
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
		if(item == currentDownloadingItem) {
			return true;
		}
		for(IndexItem ii : indexItemDownloading) {
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
		execute(new ReloadIndexesTask());
	}

	public void runReloadIndexFiles() {
		if (checkRunning(false)) {
			return;
		}
		execute(new ReloadIndexesTask());
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
			if (!item.equals(currentDownloadingItem) && !indexItemDownloading.contains(item)) {
				indexItemDownloading.add(item);
			}
		}
		if (currentDownloadingItem == null) {
			execute(new DownloadIndexesAsyncTask());
		} else {
			downloadInProgress();
		}
	}

	public void cancelDownload(DownloadItem item) {
		if (item instanceof MultipleDownloadItem) {
			MultipleDownloadItem multipleDownloadItem = (MultipleDownloadItem) item;
			cancelDownload(multipleDownloadItem.getAllIndexes());
		} else if (item instanceof SrtmDownloadItem) {
			IndexItem indexItem = ((SrtmDownloadItem) item).getIndexItem();
			cancelDownload(indexItem);
		} else if (item instanceof IndexItem) {
			IndexItem indexItem = (IndexItem) item;
			cancelDownload(indexItem);
		}
	}

	public void cancelDownload(IndexItem item) {
		app.logMapDownloadEvent("cancel", item);
		if (currentDownloadingItem == item) {
			downloadFileHelper.setInterruptDownloading(true);
		} else {
			indexItemDownloading.remove(item);
			downloadInProgress();
		}
	}

	public void cancelDownload(List<IndexItem> items) {
		if (items != null) {
			boolean updateProgress = false;
			for (IndexItem item : items) {
				app.logMapDownloadEvent("cancel", item);
				if (currentDownloadingItem == item) {
					downloadFileHelper.setInterruptDownloading(true);
				} else {
					indexItemDownloading.remove(item);
					updateProgress = true;
				}
			}
			if (updateProgress) {
				downloadInProgress();
			}
		}
	}

	public IndexItem getCurrentDownloadingItem() {
		return currentDownloadingItem;
	}

	public int getCurrentDownloadingItemProgress() {
		return currentDownloadingItemProgress;
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
				Toast.makeText(app, R.string.wait_current_task_finished, Toast.LENGTH_SHORT).show();
			}
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private <P> void execute(BasicProgressAsyncTask<?, P, ?, ?> task, P... indexItems) {
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, indexItems);
	}

	private void updateNotification() {
		app.getNotificationHelper().refreshNotification(OsmandNotification.NotificationType.DOWNLOAD);
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
				Uri uri = Uri.parse(Version.getUrlWithUtmRef(app, "net.osmand.plus"));
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
					DatabaseHelper.HistoryDownloadEntry entry = new DatabaseHelper.HistoryDownloadEntry(name, count);
					if (count == 1) {
						dbHelper.add(entry, DatabaseHelper.DOWNLOAD_ENTRY);
					} else {
						dbHelper.update(entry, DatabaseHelper.DOWNLOAD_ENTRY);
					}
				} else if (o instanceof String) {
					String message = (String) o;
					if (!message.toLowerCase().contains("interrupted") && !message.equals(app.getString(R.string.shared_string_download_successful))) {
						app.showToastMessage(message);
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
				Toast.makeText(ctx, result, Toast.LENGTH_LONG).show();
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
						currentDownloadingItemProgress = 0;
						if (item == null || currentDownloads.contains(item)) {
							continue;
						}
						currentDownloads.add(item);
						if (!validateEnoughSpace(item) || !validateNotExceedsFreeLimit(item)) {
							break;
						}
						setTag(item);
						boolean success = downloadFile(item, filesToReindex, forceWifi);
						if (success) {
							if (DownloadActivityType.isCountedInDownloads(item)) {
								downloads.set(downloads.get() + 1);
							}
							if (item.getBasename().equalsIgnoreCase(DownloadResources.WORLD_SEAMARKS_KEY)) {
								File folder = app.getAppPath(IndexConstants.MAPS_PATH);
								String fileName = DownloadResources.WORLD_SEAMARKS_OLD_NAME
										+ IndexConstants.BINARY_MAP_INDEX_EXT;
								File oldFile = new File(folder, fileName);
								Algorithms.removeAllFiles(oldFile);
							}
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
					currentDownloadingItemProgress = 0;
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
		
		private boolean validateNotExceedsFreeLimit(IndexItem item) {
			boolean exceed = !Version.isPaidVersion(app)
					&& DownloadActivityType.isCountedInDownloads(item)
					&& downloads.get() >= DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS;
			if(exceed) {
				String breakDownloadMessage = app.getString(R.string.free_version_message,
						DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "");
				publishProgress(breakDownloadMessage);
			}
			return !exceed;
		}

		private String reindexFiles(List<File> filesToReindex) {
			boolean vectorMapsToReindex = false;
			// reindex vector maps all at one time
			ResourceManager manager = app.getResourceManager();
			for (File f : filesToReindex) {
				if (f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
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
			IndexItem.DownloadEntry de = item.createDownloadEntry(app);
			boolean res = false;
			if (de == null) {
				return false;
			}
			if (de.isAsset) {
				try {
					if (ctx != null) {
						ResourceManager.copyAssets(ctx.getAssets(), de.assetName, de.targetFile);
						boolean changedDate = de.targetFile.setLastModified(de.dateModified);
						if (!changedDate) {
							LOG.error("Set last timestamp is not supported");
						}
						res = true;
					}
				} catch (IOException e) {
					LOG.error("Copy exception", e);
				}
			} else {
				long start = System.currentTimeMillis();
				app.logMapDownloadEvent("start", item);
				res = downloadFileHelper.downloadFile(de, this, filesToReindex, this, forceWifi);
				long time = System.currentTimeMillis() - start;
				if (res) {
					app.logMapDownloadEvent("done", item, time);
					checkDownload(item);
				} else {
					app.logMapDownloadEvent("failed", item, time);
				}
			}
			return res;
		}

		@Override
		protected void updateProgress(boolean updateOnlyProgress, IndexItem tag) {
			currentDownloadingItemProgress = getProgressPercentage();
			downloadInProgress();
		}
	}

	private void checkDownload(IndexItem item) {
		Map<String, String> params = new HashMap<>();
		params.put("file_name", item.fileName);
		params.put("file_size", item.size);
		AndroidNetworkUtils.sendRequestAsync(app, "https://osmand.net/api/check_download", params, "Check download", false, false, null);
	}
}
