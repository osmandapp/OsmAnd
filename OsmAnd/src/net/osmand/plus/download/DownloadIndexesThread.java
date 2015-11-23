package net.osmand.plus.download;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.StatFs;
import android.support.annotation.UiThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.support.v7.app.NotificationCompat.Builder;
import android.view.View;
import android.widget.Toast;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.helpers.DatabaseHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressLint({ "NewApi", "DefaultLocale" })
public class DownloadIndexesThread {
	private final static Log LOG = PlatformUtil.getLog(DownloadIndexesThread.class);
	private static final int NOTIFICATION_ID = 45;
	private OsmandApplication app;

	private DownloadEvents uiActivity = null;
	private DatabaseHelper dbHelper;
	private DownloadFileHelper downloadFileHelper;
	private List<BasicProgressAsyncTask<?, ?, ?, ?>> currentRunningTask = Collections.synchronizedList(new ArrayList<BasicProgressAsyncTask<?, ?, ?, ?>>());
	private ConcurrentLinkedQueue<IndexItem> indexItemDownloading = new ConcurrentLinkedQueue<IndexItem>();
	private IndexItem currentDownloadingItem = null;
	private int currentDownloadingItemProgress = 0;

	private DownloadResources indexes;
	private Notification notification;
	
	public interface DownloadEvents {
		
		void newDownloadIndexes();
		
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
	protected void downloadInProgress() {
		if (uiActivity != null) {
			uiActivity.downloadInProgress();
		}
		updateNotification();
	}
	
	private void updateNotification() {
		if(getCurrentDownloadingItem() != null) {
			BasicProgressAsyncTask<?, ?, ?, ?> task = getCurrentRunningTask();
			final boolean isFinished = task == null
					|| task.getStatus() == AsyncTask.Status.FINISHED;
			Intent contentIntent = new Intent(app, DownloadActivity.class);
			PendingIntent contentPendingIntent = PendingIntent.getActivity(app, 0, contentIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			Builder bld = new NotificationCompat.Builder(app);
			String msg = Version.getAppName(app);
			if(!isFinished) {
				msg = task.getDescription();
			}
			StringBuilder contentText = new StringBuilder();
			List<IndexItem> ii = getCurrentDownloadingItems();
			for(IndexItem i : ii) {
				if(contentText.length() > 0) {
					contentText.append(", ");
				}
				contentText.append(i.getVisibleName(app, app.getRegions()));
			}
			bld.setContentTitle(msg).setSmallIcon(R.drawable.ic_action_import).
				setContentText(contentText.toString()).
				setContentIntent(contentPendingIntent).setOngoing(true);
			int progress = getCurrentDownloadingItemProgress();
			bld.setProgress(100, Math.max(progress, 0), progress < 0);
			notification = bld.build();
			NotificationManager mNotificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.notify(NOTIFICATION_ID, notification);
		} else {
			if(notification != null) {
				NotificationManager mNotificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
				mNotificationManager.cancel(NOTIFICATION_ID);
				notification = null;
			}
		}
		
	}
	

	@UiThread
	protected void downloadHasFinished() {
		if (uiActivity != null) {
			uiActivity.downloadHasFinished();
		}
		updateNotification();
	}
	
	@UiThread
	protected void newDownloadIndexes() {
		if (uiActivity != null) {
			uiActivity.newDownloadIndexes();
		}
	}


	// PUBLIC API

	
	public DownloadResources getIndexes() {
		return indexes;
	}
	
	public List<IndexItem> getCurrentDownloadingItems() {
		List<IndexItem> res = new ArrayList<IndexItem>();
		IndexItem ii = currentDownloadingItem;
		if(ii != null) {
			res.add(ii);
		}
		res.addAll(indexItemDownloading);
		return res;
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
		if(currentDownloadingItem != null && DownloadActivityType.isCountedInDownloads(currentDownloadingItem)) {
			i++;
		}
		for(IndexItem ii : indexItemDownloading) {
			if (DownloadActivityType.isCountedInDownloads(ii)) {
				i++;
			}
		}
		return i;
	}
	
	public void runReloadIndexFiles() {
		if (checkRunning()) {
			return;
		}
		execute(new ReloadIndexesTask());
	}

	public void runDownloadFiles(IndexItem... items) {
		if (getCurrentRunningTask() instanceof ReloadIndexesTask) {
			if(checkRunning()) {
				return;
			}	
		}
		for(IndexItem i : items) {
			indexItemDownloading.add(i);
		}
		if (currentDownloadingItem == null) {
			execute(new DownloadIndexesAsyncTask());
		} else {
			downloadInProgress();
		}
	}

	public void cancelDownload(IndexItem item) {
		if(currentDownloadingItem == item) {
			downloadFileHelper.setInterruptDownloading(true);
		} else {
			indexItemDownloading.remove(item);
			downloadInProgress();
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

	@SuppressWarnings("deprecation")
	public double getAvailableSpace() {
		File dir = app.getAppPath("").getParentFile();
		double asz = -1;
		if (dir.canRead()) {
			StatFs fs = new StatFs(dir.getAbsolutePath());
			asz = (((long) fs.getAvailableBlocks()) * fs.getBlockSize()) / (1 << 20);
		}
		return asz;
	}
	
	/// PRIVATE IMPL

	private boolean checkRunning() {
		if (getCurrentRunningTask() != null) {
			AccessibleToast.makeText(app, R.string.wait_current_task_finished, Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private <P> void execute(BasicProgressAsyncTask<?, P, ?, ?> task, P... indexItems) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, indexItems);
		} else {
			task.execute(indexItems);
		}
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
			DownloadResources result = null;
			DownloadOsmandIndexesHelper.IndexFileList indexFileList = DownloadOsmandIndexesHelper.getIndexesList(ctx);
			if (indexFileList != null) {
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
				}
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
			newDownloadIndexes();
		}

		private void showWarnDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			builder.setMessage(R.string.map_version_changed_info);
			builder.setPositiveButton(R.string.button_upgrade_osmandplus, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:net.osmand.plus"));
					try {
						ctx.startActivity(intent);
					} catch (ActivityNotFoundException e) {
					}
				}
			});
			builder.setNegativeButton(R.string.shared_string_cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.show();

		}

		@Override
		protected void updateProgress(boolean updateOnlyProgress, Void tag) {
			downloadInProgress();
		}
	}


	private class DownloadIndexesAsyncTask extends BasicProgressAsyncTask<IndexItem, IndexItem, Object, String> implements DownloadFileShowWarning {

		private OsmandPreference<Integer> downloads;


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
					long count = dbHelper.getCount(name, DatabaseHelper.DOWNLOAD_ENTRY) + 1;
					item.setDownloaded(true);
					DatabaseHelper.HistoryDownloadEntry entry = new DatabaseHelper.HistoryDownloadEntry(name, count);
					if (count == 1) {
						dbHelper.add(entry, DatabaseHelper.DOWNLOAD_ENTRY);
					} else {
						dbHelper.update(entry, DatabaseHelper.DOWNLOAD_ENTRY);
					}
				} else if (o instanceof String) {
					String message = (String) o;
					// ctx.getString(R.string.shared_string_io_error) +": Interrupted";
					if (!message.toLowerCase().contains("interrupted")) {
						if (uiActivity == null ||
								!message.equals(app.getString(R.string.shared_string_download_successful))) {
							app.showToastMessage(message);
						}
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
			startTask(ctx.getString(R.string.shared_string_downloading) + ctx.getString(R.string.shared_string_ellipsis), -1);
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null && result.length() > 0) {
				AccessibleToast.makeText(ctx, result, Toast.LENGTH_LONG).show();
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
				List<File> filesToReindex = new ArrayList<File>();
				boolean forceWifi = downloadFileHelper.isWifiConnected();
				Set<IndexItem> currentDownloads = new HashSet<IndexItem>();
				String warn = "";
				try {
					downloadCycle: while (!indexItemDownloading.isEmpty()) {
						IndexItem item = indexItemDownloading.poll();
						currentDownloadingItem = item;
						currentDownloadingItemProgress = 0;
						if (currentDownloads.contains(item)) {
							continue;
						}
						currentDownloads.add(item);
						boolean success = false;
						if(!validateEnoughSpace(item)) {
							break downloadCycle;
						}
						if(!validateNotExceedsFreeLimit(item)) {
							break downloadCycle;
						}
						setTag(item);
						boolean result = downloadFile(item, filesToReindex, forceWifi);
						success = result || success;
						if (result) {
							if (DownloadActivityType.isCountedInDownloads(item)) {
								downloads.set(downloads.get() + 1);
							}
							File bf = item.getBackupFile(app);
							if (bf.exists()) {
								Algorithms.removeAllFiles(bf);
							}
							// trackEvent(entry);
							publishProgress(item);
							String wn = reindexFiles(filesToReindex);
							if(!Algorithms.isEmpty(wn)) {
								warn += " " + wn;
							}
							filesToReindex.clear();
							// slow down but let update all button work properly
							indexes.updateFilesToUpdate();;
						}
					}
				} finally {
					currentDownloadingItem = null;
					currentDownloadingItemProgress = 0;
				}
				//String warn = reindexFiles(filesToReindex);
				if(warn.trim().length() == 0) {
					return null;
				}
				return warn.trim();
			} catch (InterruptedException e) {
				LOG.info("Download Interrupted");
				// do not dismiss dialog
			}
			return null;
		}

		private boolean validateEnoughSpace(IndexItem item) {
			double asz = getAvailableSpace();
			double cs =(item.contentSize / (1 << 20));
			// validate enough space
			if (asz != -1 && cs > asz) {
				String breakDownloadMessage = app.getString(R.string.download_files_not_enough_space,
						cs, asz);
				publishProgress(breakDownloadMessage);
				return false;
			}
			return true;
		}
		
		private boolean validateNotExceedsFreeLimit(IndexItem item) {
			boolean exceed = Version.isFreeVersion(app) &&
					DownloadActivityType.isCountedInDownloads(item) && downloads.get() >= DownloadValidationManager.MAXIMUM_AVAILABLE_FREE_DOWNLOADS;
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
			List<String> warnings = new ArrayList<String>();
			manager.indexVoiceFiles(this);
			if (vectorMapsToReindex) {
				warnings = manager.indexingMaps(this);
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

//		private void trackEvent(DownloadEntry entry) {
//			String v = Version.getAppName(app);
//			if (Version.isProductionVersion(app)) {
//				v = Version.getFullVersion(app);
//			} else {
//				v += " test";
//			}
//			new DownloadTracker().trackEvent(app, v, Version.getAppName(app),
//					entry.baseName, 1, app.getString(R.string.ga_api_key));
//		}

		@Override
		public void showWarning(String warning) {
			publishProgress(warning);
		}

		public boolean downloadFile(IndexItem item, List<File> filesToReindex, boolean forceWifi)
				throws InterruptedException {
			downloadFileHelper.setInterruptDownloading(false);
			IndexItem.DownloadEntry de = item.createDownloadEntry(app);
			boolean res = false;
			if(de == null) {
				return res;
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
				res = downloadFileHelper.downloadFile(de, this, filesToReindex, this, forceWifi);
			}
			return res;
		}

		@Override
		protected void updateProgress(boolean updateOnlyProgress, IndexItem tag) {
			currentDownloadingItemProgress = getProgressPercentage();
			downloadInProgress();
		}
	}
}