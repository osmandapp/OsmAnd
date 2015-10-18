package net.osmand.plus.download;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.osmand.Collator;
import net.osmand.IndexConstants;
import net.osmand.OsmAndCollator;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.WorldRegion;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.download.DownloadOsmandIndexesHelper.AssetIndexItem;
import net.osmand.plus.helpers.DatabaseHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import android.view.View;
import android.widget.Toast;

@SuppressLint("NewApi")
public class DownloadIndexesThread {
	private final static Log LOG = PlatformUtil.getLog(DownloadIndexesThread.class);
	private final Context ctx;
	private OsmandApplication app;
	private java.text.DateFormat dateFormat;
	private BaseDownloadActivity uiActivity = null;
	private DatabaseHelper dbHelper;
	private DownloadFileHelper downloadFileHelper;
	private List<BasicProgressAsyncTask<?, ?, ?, ?>> currentRunningTask = Collections.synchronizedList(new ArrayList<BasicProgressAsyncTask<?, ?, ?, ?>>());
	private ConcurrentLinkedQueue<IndexItem> indexItemDownloading = new ConcurrentLinkedQueue<IndexItem>();
	private IndexItem currentDownloadingItem = null;
	private int currentDownloadingItemProgress = 0;
	
	
	private DownloadIndexes indexes = new DownloadIndexes();
	public static class DownloadIndexes {
		Map<WorldRegion, Map<String, IndexItem>> resourcesByRegions = new HashMap<>();
		List<IndexItem> voiceRecItems = new LinkedList<>();
		List<IndexItem> voiceTTSItems = new LinkedList<>();
		IndexFileList indexFiles = null;
	}
	private Map<String, String> indexFileNames = new LinkedHashMap<>();
	private Map<String, String> indexActivatedFileNames = new LinkedHashMap<>();
	private List<IndexItem> itemsToUpdate = new ArrayList<>();
	
	

	public DownloadIndexesThread(Context ctx) {
		this.ctx = ctx;
		app = (OsmandApplication) ctx.getApplicationContext();
		downloadFileHelper = new DownloadFileHelper(app);
		dateFormat = app.getResourceManager().getDateFormat();
		dbHelper = new DatabaseHelper(app);
	}


	public void setUiActivity(BaseDownloadActivity uiActivity) {
		this.uiActivity = uiActivity;
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
	
	public List<IndexItem> getCachedIndexFiles() {
		return indexes.indexFiles != null ? indexes.indexFiles.getIndexFiles() : null;
	}

	public Map<String, String> getIndexFileNames() {
		return indexFileNames;
	}

	public Map<String, String> getIndexActivatedFileNames() {
		return indexActivatedFileNames;
	}

	public void updateLoadedFiles() {
		Map<String, String> indexActivatedFileNames = app.getResourceManager().getIndexFileNames();
		listWithAlternatives(dateFormat, app.getAppPath(""), IndexConstants.EXTRA_EXT,
				indexActivatedFileNames);
		Map<String, String> indexFileNames = app.getResourceManager().getIndexFileNames();
		listWithAlternatives(dateFormat, app.getAppPath(""), IndexConstants.EXTRA_EXT,
				indexFileNames);
		app.getAppCustomization().updatedLoadedFiles(indexFileNames, indexActivatedFileNames);
		listWithAlternatives(dateFormat, app.getAppPath(IndexConstants.TILES_INDEX_DIR),
				IndexConstants.SQLITE_EXT, indexFileNames);
		app.getResourceManager().getBackupIndexes(indexFileNames);
		this.indexFileNames = indexFileNames;
		this.indexActivatedFileNames = indexActivatedFileNames;
		//updateFilesToDownload();
	}

	public Map<String, String> getDownloadedIndexFileNames() {
		return indexFileNames;
	}

	public boolean isDownloadedFromInternet() {
		return indexes.indexFiles != null && indexes.indexFiles.isDownloadedFromInternet();
	}

	public List<IndexItem> getItemsToUpdate() {
		return itemsToUpdate;
	}

	public Map<WorldRegion, Map<String, IndexItem>> getResourcesByRegions() {
		return indexes.resourcesByRegions;
	}

	public List<IndexItem> getVoiceRecItems() {
		return indexes.voiceRecItems;
	}

	public List<IndexItem> getVoiceTTSItems() {
		return indexes.voiceTTSItems;
	}

	private boolean prepareData(List<IndexItem> resources,
								Map<WorldRegion, Map<String, IndexItem>> resourcesByRegions,
								List<IndexItem> voiceRecItems, List<IndexItem> voiceTTSItems) {
		List<IndexItem> resourcesInRepository;
		if (resources != null) {
			resourcesInRepository = resources;
		} else {
			resourcesInRepository = DownloadActivity.downloadListIndexThread.getCachedIndexFiles();
		}
		if (resourcesInRepository == null) {
			return false;
		}

		for (WorldRegion region : app.getWorldRegion().getFlattenedSubregions()) {
			processRegion(resourcesInRepository, resourcesByRegions, voiceRecItems, voiceTTSItems, false, region);
		}
		processRegion(resourcesInRepository, resourcesByRegions, voiceRecItems, voiceTTSItems, true, app.getWorldRegion());

		final Collator collator = OsmAndCollator.primaryCollator();
		final OsmandRegions osmandRegions = app.getRegions();

		Collections.sort(voiceRecItems, new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem lhs, IndexItem rhs) {
				return collator.compare(lhs.getVisibleName(app.getApplicationContext(), osmandRegions),
						rhs.getVisibleName(app.getApplicationContext(), osmandRegions));
			}
		});

		Collections.sort(voiceTTSItems, new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem lhs, IndexItem rhs) {
				return collator.compare(lhs.getVisibleName(app.getApplicationContext(), osmandRegions),
						rhs.getVisibleName(app.getApplicationContext(), osmandRegions));
			}
		});

		return true;
	}

	// FIXME argument list
	private void processRegion(List<IndexItem> resourcesInRepository, Map<WorldRegion,
			Map<String, IndexItem>> resourcesByRegions,
							   List<IndexItem> voiceRecItems, List<IndexItem> voiceTTSItems,
							   boolean processVoiceFiles, WorldRegion region) {
		String downloadsIdPrefix = region.getDownloadsIdPrefix();
		Map<String, IndexItem> regionResources = new HashMap<>();
		Set<DownloadActivityType> typesSet = new TreeSet<>(new Comparator<DownloadActivityType>() {
			@Override
			public int compare(DownloadActivityType dat1, DownloadActivityType dat2) {
				return dat1.getTag().compareTo(dat2.getTag());
			}
		});
		for (IndexItem resource : resourcesInRepository) {
			if (processVoiceFiles) {
				if (resource.getSimplifiedFileName().endsWith(".voice.zip")) {
					voiceRecItems.add(resource);
					continue;
				} else if (resource.getSimplifiedFileName().contains(".ttsvoice.zip")) {
					voiceTTSItems.add(resource);
					continue;
				}
			}
			if (!resource.getSimplifiedFileName().startsWith(downloadsIdPrefix)) {
				continue;
			}

			if (resource.type == DownloadActivityType.NORMAL_FILE
					|| resource.type == DownloadActivityType.ROADS_FILE) {
				if (resource.isAlreadyDownloaded(indexFileNames)) {
					region.processNewMapState(checkIfItemOutdated(resource)
							? WorldRegion.MapState.OUTDATED : WorldRegion.MapState.DOWNLOADED);
				} else {
					region.processNewMapState(WorldRegion.MapState.NOT_DOWNLOADED);
				}
			}
			typesSet.add(resource.getType());
			regionResources.put(resource.getSimplifiedFileName(), resource);
		}

		if (region.getSuperregion() != null && region.getSuperregion().getSuperregion() != app.getWorldRegion()) {
			if (region.getSuperregion().getResourceTypes() == null) {
				region.getSuperregion().setResourceTypes(typesSet);
			} else {
				region.getSuperregion().getResourceTypes().addAll(typesSet);
			}
		}

		region.setResourceTypes(typesSet);
		resourcesByRegions.put(region, regionResources);
	}
	

	private boolean checkRunning() {
		if (getCurrentRunningTask() != null) {
			AccessibleToast.makeText(app, R.string.wait_current_task_finished, Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}

	public void runReloadIndexFiles() {
		if (checkRunning()) {
			return;
		}
		execute(new ReloadIndexesTask(ctx));
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
			execute(new DownloadIndexesAsyncTask(ctx));
		}
	}
	
	public void cancelDownload(IndexItem item) {
		if(currentDownloadingItem == item) {
			downloadFileHelper.setInterruptDownloading(true);;
		}
		// TODO Auto-generated method stub
		
	}

	private <P> void execute(BasicProgressAsyncTask<?, P, ?, ?> task, P... indexItems) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, indexItems);
		} else {
			task.execute(indexItems);
		}
	}

	private void prepareFilesToUpdate() {
		List<IndexItem> filtered = getCachedIndexFiles();
		if (filtered != null) {
			itemsToUpdate.clear();
			for (IndexItem item : filtered) {
				boolean outdated = checkIfItemOutdated(item);
				//include only activated files here
				if (outdated && indexActivatedFileNames.containsKey(item.getTargetFileName())) {
					itemsToUpdate.add(item);
				}
			}
		}
	}

	@UiThread
	private void notifyFilesToUpdateChanged() {
		List<IndexItem> filtered = getCachedIndexFiles();
		if (filtered != null) {
			if (uiActivity != null) {
				uiActivity.updateDownloadList();
			}
		}
	}

	public boolean checkIfItemOutdated(IndexItem item) {
		boolean outdated = false;
		String sfName = item.getTargetFileName();
		java.text.DateFormat format = app.getResourceManager().getDateFormat();
		String date = item.getDate(format);
		String indexactivateddate = indexActivatedFileNames.get(sfName);
		String indexfilesdate = indexFileNames.get(sfName);
		if (date != null &&
				!date.equals(indexactivateddate) &&
				!date.equals(indexfilesdate)) {
			if ((item.getType() == DownloadActivityType.NORMAL_FILE && !item.extra) ||
					item.getType() == DownloadActivityType.ROADS_FILE ||
					item.getType() == DownloadActivityType.WIKIPEDIA_FILE ||
					item.getType() == DownloadActivityType.SRTM_COUNTRY_FILE) {
				outdated = true;
			} else {
				long itemSize = item.getContentSize();
				long oldItemSize = 0;
				if (item.getType() == DownloadActivityType.VOICE_FILE) {
					if (item instanceof AssetIndexItem) {
						File file = new File(((AssetIndexItem) item).getDestFile());
						oldItemSize = file.length();
					} else {
						File fl = new File(item.getType().getDownloadFolder(app, item), sfName + "/_config.p");
						if (fl.exists()) {
							oldItemSize = fl.length();
							try {
								InputStream is = ctx.getAssets().open("voice/" + sfName + "/config.p");
								if (is != null) {
									itemSize = is.available();
									is.close();
								}
							} catch (IOException e) {
							}
						}
					}
				} else {
					oldItemSize = app.getAppPath(item.getTargetFileName()).length();
				}


				if (itemSize != oldItemSize) {
					outdated = true;
				}
			}
		}
		return outdated;
	}

	private void updateFilesToUpdate() {
		List<IndexItem> stillUpdate = new ArrayList<IndexItem>();
		for (IndexItem item : itemsToUpdate) {
			String sfName = item.getTargetFileName();
			java.text.DateFormat format = app.getResourceManager().getDateFormat();
			String date = item.getDate(format);
			String indexactivateddate = indexActivatedFileNames.get(sfName);
			String indexfilesdate = indexFileNames.get(sfName);
			if (date != null &&
					!date.equals(indexactivateddate) &&
					!date.equals(indexfilesdate) &&
					indexActivatedFileNames.containsKey(sfName)) {
				stillUpdate.add(item);
			}
		}
		itemsToUpdate = stillUpdate;
		if (uiActivity != null) {
			uiActivity.updateDownloadList();
		}
	}


	public boolean isDownloadRunning() {
		for (int i = 0; i < currentRunningTask.size(); i++) {
			if (currentRunningTask.get(i) instanceof DownloadIndexesAsyncTask) {
				return true;

			}
		}
		return false;
	}
	
	public IndexItem getCurrentDownloadingItem() {
		return currentDownloadingItem;
	}
	
	public int getCurrentDownloadingItemProgress() {
		return currentDownloadingItemProgress;
	}

	// FIXME deprecated
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
		File dir = app.getAppPath("").getParentFile();
		double asz = -1;
		if (dir.canRead()) {
			StatFs fs = new StatFs(dir.getAbsolutePath());
			asz = (((long) fs.getAvailableBlocks()) * fs.getBlockSize()) / (1 << 20);
		}
		return asz;
	}
	

	public Map<String, String> listWithAlternatives(final java.text.DateFormat dateFormat, File file, final String ext, 
			final Map<String, String> files) {
		if (file.isDirectory()) {
			file.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith(ext)) {
						String date = dateFormat.format(findFileInDir(new File(dir, filename)).lastModified());
						files.put(filename, date);
						return true;
					} else {
						return false;
					}
				}
			});

		}
		return files;
	}

	public File findFileInDir(File file) {
		if(file.isDirectory()) {
			File[] lf = file.listFiles();
			if(lf != null) {
				for(File f : lf) {
					if(f.isFile()) {
						return f;
					}
				}
			}
		}
		return file;
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
	

	public class ReloadIndexesTask extends BasicProgressAsyncTask<Void, Void, Void, DownloadIndexes> {

		public ReloadIndexesTask(Context ctx) {
			super(ctx);
		}

		@Override
		protected void onPreExecute() {
			currentRunningTask.add(this);
			super.onPreExecute();
			this.message = ctx.getString(R.string.downloading_list_indexes);
		}

		@Override
		protected DownloadIndexes doInBackground(Void... params) {
			DownloadIndexes result = new DownloadIndexes();
			IndexFileList indexFileList = DownloadOsmandIndexesHelper.getIndexesList(ctx);
			result.indexFiles = indexFileList;
			if (indexFileList != null) {
				updateLoadedFiles();
				prepareFilesToUpdate();
				prepareData(indexFileList.getIndexFiles(), result.resourcesByRegions, result.voiceRecItems, result.voiceTTSItems);
			}
			return result;
		}

		protected void onPostExecute(DownloadIndexes result) {
			indexes = result;
			notifyFilesToUpdateChanged();
			if (result.indexFiles != null) {
				if (result.indexFiles.isIncreasedMapVersion()) {
					showWarnDialog();
				}
			} else {
				AccessibleToast.makeText(ctx, R.string.list_index_files_was_not_loaded, Toast.LENGTH_LONG).show();
			}
			currentRunningTask.remove(this);
			if (uiActivity != null) {
				uiActivity.updateProgress(false);
				uiActivity.onCategorizationFinished();
			}
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
			if (uiActivity != null) {
				uiActivity.updateProgress(updateOnlyProgress);
			}
		}
	}
	
	
	
	public class DownloadIndexesAsyncTask extends BasicProgressAsyncTask<IndexItem, IndexItem, Object, String> implements DownloadFileShowWarning {

		private OsmandPreference<Integer> downloads;
		

		public DownloadIndexesAsyncTask(Context ctx) {
			super(ctx);
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
					if (uiActivity != null) {
						uiActivity.updateFragments();
						IndexItem item = (IndexItem) o;
						String name = item.getBasename();
						long count = dbHelper.getCount(name, DatabaseHelper.DOWNLOAD_ENTRY) + 1;
						DatabaseHelper.HistoryDownloadEntry entry = new DatabaseHelper.HistoryDownloadEntry(name, count);
						if (count == 1) {
							dbHelper.add(entry, DatabaseHelper.DOWNLOAD_ENTRY);
						} else {
							dbHelper.update(entry, DatabaseHelper.DOWNLOAD_ENTRY);
						}
					}
				} else if (o instanceof String) {
					String message = (String) o;
					if (!message.equals("I/O error occurred : Interrupted")) {
						if (uiActivity == null ||
								!message.equals(uiActivity.getString(R.string.shared_string_download_successful))) {
							AccessibleToast.makeText(ctx, message, Toast.LENGTH_LONG).show();
						}
					} else {
						if (uiActivity != null) {
							uiActivity.updateProgress(true);
						}
					}
				}
			}
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPreExecute() {
			currentRunningTask.add(this);
			super.onPreExecute();
			if (uiActivity != null) {
				downloadFileHelper.setInterruptDownloading(false);
				View mainView = uiActivity.findViewById(R.id.MainLayout);
				if (mainView != null) {
					mainView.setKeepScreenOn(true);
				}
				startTask(ctx.getString(R.string.shared_string_downloading) + ctx.getString(R.string.shared_string_ellipsis), -1);
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null && result.length() > 0) {
				AccessibleToast.makeText(ctx, result, Toast.LENGTH_LONG).show();
			}
			if (uiActivity != null) {
				View mainView = uiActivity.findViewById(R.id.MainLayout);
				if (mainView != null) {
					mainView.setKeepScreenOn(false);
				}
				uiActivity.downloadedIndexes();
			}
			currentRunningTask.remove(this);
			if (uiActivity != null) {
				uiActivity.updateProgress(false);
			}
			updateFilesToUpdate();
		}


		@Override
		protected String doInBackground(IndexItem... filesToDownload) {
			try {
				List<File> filesToReindex = new ArrayList<File>();
				boolean forceWifi = downloadFileHelper.isWifiConnected();
				Set<IndexItem> currentDownloads = new HashSet<IndexItem>();
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
						}
					}
				} finally {
					currentDownloadingItem = null;
					currentDownloadingItemProgress = 0;
				}
				String warn = reindexFiles(filesToReindex);
				updateLoadedFiles();
				return warn;
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
					DownloadActivityType.isCountedInDownloads(item) && downloads.get() >= DownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS;
			if(exceed) {
				String breakDownloadMessage = app.getString(R.string.free_version_message,
						DownloadActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "");
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
			DownloadEntry de = item.createDownloadEntry(app);
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
			uiActivity.updateProgress(true);
		}
	}

	
}