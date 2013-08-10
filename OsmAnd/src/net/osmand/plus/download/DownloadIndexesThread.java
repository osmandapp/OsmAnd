package net.osmand.plus.download;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.TreeMap;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.map.RegionCountry;
import net.osmand.map.RegionRegistry;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.StatFs;
import android.view.View;
import android.widget.Toast;

public class DownloadIndexesThread {
	private DownloadIndexActivity uiActivity = null;
	private IndexFileList indexFiles = null;
	private List<SrtmIndexItem> cachedSRTMFiles;
	private Map<IndexItem, List<DownloadEntry>> entriesToDownload = new ConcurrentHashMap<IndexItem, List<DownloadEntry>>();
	private Set<DownloadEntry> currentDownloads = new HashSet<DownloadEntry>();
	private final Context ctx;
	private OsmandApplication app;
	private final static Log log = PlatformUtil.getLog(DownloadIndexesThread.class);
	private DownloadFileHelper downloadFileHelper;
	private List<BasicProgressAsyncTask<?, ?, ?> > currentRunningTask = Collections.synchronizedList(new ArrayList<BasicProgressAsyncTask<?, ?, ?>>());
	private Map<String, String> indexFileNames = new LinkedHashMap<String, String>();
	private Map<String, String> indexActivatedFileNames = new LinkedHashMap<String, String>();

	public DownloadIndexesThread(Context ctx) {
		this.ctx = ctx;
		app = (OsmandApplication) ctx.getApplicationContext();
		downloadFileHelper = new DownloadFileHelper(app);
	}
	
	public void setUiActivity(DownloadIndexActivity uiActivity) {
		this.uiActivity = uiActivity;
	}
	
	public List<DownloadEntry> flattenDownloadEntries() {
		List<DownloadEntry> res = new ArrayList<DownloadEntry>();
		for(List<DownloadEntry> ens : getEntriesToDownload().values()) {
			if(ens != null) {
				res.addAll(ens);
			}
		}
		return res;
	}

	public List<IndexItem> getCachedIndexFiles() {
		return indexFiles != null ? indexFiles.getIndexFiles() : null;
	}
	
	public void updateLoadedFiles() {
		Map<String, String> indexActivatedFileNames = app.getResourceManager().getIndexFileNames();
		DownloadIndexActivity.listWithAlternatives(app.getAppPath(""),
				IndexConstants.EXTRA_EXT, indexActivatedFileNames);
		Map<String, String> indexFileNames = app.getResourceManager().getIndexFileNames();
		DownloadIndexActivity.listWithAlternatives(app.getAppPath(""),
				IndexConstants.EXTRA_EXT, indexFileNames);
		DownloadIndexActivity.listWithAlternatives(app.getAppPath(IndexConstants.TILES_INDEX_DIR),
				IndexConstants.SQLITE_EXT, indexFileNames);
		app.getResourceManager().getBackupIndexes(indexFileNames);
		this.indexFileNames = indexFileNames;
		this.indexActivatedFileNames = indexActivatedFileNames;
	}

	public boolean isDownloadedFromInternet() {
		return indexFiles != null && indexFiles.isDownloadedFromInternet();
	}
	
	public class DownloadIndexesAsyncTask extends BasicProgressAsyncTask<IndexItem, Object, String> implements DownloadFileShowWarning {

		private OsmandPreference<Integer> downloads;

		public DownloadIndexesAsyncTask(Context ctx) {
			super(ctx);
			downloads = app.getSettings().NUMBER_OF_FREE_DOWNLOADS;
		}
		
		@Override
		public void setInterrupted(boolean interrupted) {
			super.setInterrupted(interrupted);
			if(interrupted) {
				downloadFileHelper.setInterruptDownloading(true);
			}
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			for (Object o : values) {
				if (o instanceof DownloadEntry) {
					if (uiActivity != null) {
						((DownloadIndexAdapter) uiActivity.getExpandableListAdapter()).notifyDataSetInvalidated();
						uiActivity.updateDownloadButton(false);
					}
				} else if (o instanceof IndexItem) {
					entriesToDownload.remove(o);
					if (uiActivity != null) {
						((DownloadIndexAdapter) uiActivity.getExpandableListAdapter()).notifyDataSetInvalidated();
						uiActivity.updateDownloadButton(false);
					}
				} else if (o instanceof String) {
					AccessibleToast.makeText(ctx, (String) o, Toast.LENGTH_LONG).show();
				}
			}
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPreExecute() {
			currentRunningTask.add( this);
			super.onPreExecute();
			if (uiActivity != null) {
				downloadFileHelper.setInterruptDownloading(false);
				View mainView = uiActivity.findViewById(R.id.MainLayout);
				if (mainView != null) {
					mainView.setKeepScreenOn(true);
				}
				startTask(ctx.getString(R.string.downloading), -1);
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null && result.length() > 0) {
				AccessibleToast.makeText(ctx, result, Toast.LENGTH_LONG).show();
			}
			currentDownloads.clear();
			if (uiActivity != null) {
				View mainView = uiActivity.findViewById(R.id.MainLayout);
				if (mainView != null) {
					mainView.setKeepScreenOn(false);
				}
				DownloadIndexAdapter adapter = ((DownloadIndexAdapter) uiActivity.getExpandableListAdapter());
				if (adapter != null) {
					adapter.setLoadedFiles(indexActivatedFileNames, indexFileNames);
				}
			}
			currentRunningTask.remove(this);
			if(uiActivity != null) { 
				uiActivity.updateProgress(false);
			}
		}
		

		@Override
		protected String doInBackground(IndexItem... filesToDownload) {
			try {
				List<File> filesToReindex = new ArrayList<File>();
				boolean forceWifi = downloadFileHelper.isWifiConnected();
				currentDownloads = new HashSet<DownloadEntry>();
				String breakDownloadMessage = null;
				downloadCycle : while(!entriesToDownload.isEmpty() ) {
					
					Iterator<Entry<IndexItem, List<DownloadEntry>>> it = entriesToDownload.entrySet().iterator();
					IndexItem file = null;
					List<DownloadEntry> list = null;
					while(it.hasNext()) {
						Entry<IndexItem, List<DownloadEntry>> n = it.next();
						if(!currentDownloads.containsAll(n.getValue())) {
							file = n.getKey();
							list = n.getValue();
							break;
						}
					}
					if(file == null) {
						break downloadCycle;
					}
					if (list != null) {
						boolean success = false;
						for (DownloadEntry entry : list) {
							if(currentDownloads.contains(entry)) {
								continue;
							}
							currentDownloads.add(entry);
							double asz = getAvailableSpace();
							// validate interrupted
							if(downloadFileHelper.isInterruptDownloading()) {
								break downloadCycle;
							}
							// validate enough space
							if (asz != -1 && entry.sizeMB > asz) {
								breakDownloadMessage =  app.getString(R.string.download_files_not_enough_space, entry.sizeMB, asz);
								break downloadCycle;
							} 
							if (exceedsFreelimit(entry)) {
								breakDownloadMessage = app.getString(R.string.free_version_message, DownloadIndexActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS
										+ "");
								break downloadCycle;
							}
							boolean result = downloadFile(entry, filesToReindex, forceWifi);
							success = result || success;
							if (result) {
								if (entry.type != DownloadActivityType.SRTM_FILE && entry.type != DownloadActivityType.HILLSHADE_FILE) {
									downloads.set(downloads.get() + 1);
								}
								if (entry.existingBackupFile != null) {
									Algorithms.removeAllFiles(entry.existingBackupFile);
								}
								trackEvent(entry);
								publishProgress(entry);
							}
						}
						if(success) {
							entriesToDownload.remove(file);
						}
					}
					
				}
				String warn = reindexFiles(filesToReindex);
				if(breakDownloadMessage != null) {
					if(warn != null) {
						warn = breakDownloadMessage + "\n" + warn;
					} else {
						warn = breakDownloadMessage;
					}
				}
				updateLoadedFiles();
				return warn;
			} catch (InterruptedException e) {
				log.info("Download Interrupted");
				// do not dismiss dialog
			}
			return null;
		}

		private boolean exceedsFreelimit(DownloadEntry entry) {
			return Version.isFreeVersion(app) &&
					entry.type != DownloadActivityType.SRTM_FILE && entry.type != DownloadActivityType.HILLSHADE_FILE
					&& downloads.get() >= DownloadIndexActivity.MAXIMUM_AVAILABLE_FREE_DOWNLOADS;
		}

		private String reindexFiles(List<File> filesToReindex) {
			boolean vectorMapsToReindex = false;
			for (File f : filesToReindex) {
				if (f.getName().endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)) {
					vectorMapsToReindex = true;
					break;
				}
			}
			// reindex vector maps all at one time
			ResourceManager manager = app.getResourceManager();
			manager.indexVoiceFiles(this);
			List<String> warnings = new ArrayList<String>();
			if (vectorMapsToReindex) {
				warnings = manager.indexingMaps(this);
			}
			if (cachedSRTMFiles != null) {
				for (SrtmIndexItem i : cachedSRTMFiles) {
					((SrtmIndexItem) i).updateExistingTiles(app.getResourceManager().getIndexFileNames());
				}
			}
			if (!warnings.isEmpty()) {
				return warnings.get(0);
			}
			return null;
		}

		private void trackEvent(DownloadEntry entry) {
			String v = Version.getAppName(app);
			if (Version.isProductionVersion(app)) {
				v = Version.getFullVersion(app);
			} else {
				v += " test";
			}
			new DownloadTracker().trackEvent(app, v, Version.getAppName(app),
					entry.baseName, 1, app.getString(R.string.ga_api_key));
		}

		@Override
		public void showWarning(String warning) {
			publishProgress(warning);

		}

		public boolean downloadFile(DownloadEntry de, List<File> filesToReindex, boolean forceWifi)
				throws InterruptedException {
			boolean res = false;
			if (de.isAsset) {
				try {
					if (uiActivity != null) {
						ResourceManager.copyAssets(uiActivity.getAssets(), de.assetName, de.targetFile);
						boolean changedDate = de.targetFile.setLastModified(de.dateModified);
						if(!changedDate) {
							log.error("Set last timestamp is not supported");
						}
						res = true;
					}
				} catch (IOException e) {
					log.error("Copy exception", e);
				}
			} else {
				res = downloadFileHelper.downloadFile(de, this, filesToReindex, this, forceWifi);
			}
			if (res && de.attachedEntry != null) {
				return downloadFile(de.attachedEntry, filesToReindex, forceWifi);
			}
			return res;
		}

		@Override
		protected void updateProgress(boolean updateOnlyProgress) {
			if(uiActivity != null) {
				uiActivity.updateProgress(updateOnlyProgress);
			}
		}
	}
	
	private boolean checkRunning() {
		if(getCurrentRunningTask() != null) {
			AccessibleToast.makeText(app, R.string.wait_current_task_finished, Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}
	
	public void runReloadIndexFiles() {
		checkRunning();
		final BasicProgressAsyncTask<Void, Void, IndexFileList> inst = new BasicProgressAsyncTask<Void, Void, IndexFileList>(ctx) {

			@Override
			protected IndexFileList doInBackground(Void... params) {
				return DownloadOsmandIndexesHelper.getIndexesList(ctx);
			};

			@Override
			protected void onPreExecute() {
				currentRunningTask.add(this);
				super.onPreExecute();
				this.message = ctx.getString(R.string.downloading_list_indexes);
			}

			protected void onPostExecute(IndexFileList result) {
				indexFiles = result;
				if (indexFiles != null && uiActivity != null) {
					boolean basemapExists = uiActivity.getMyApplication().getResourceManager().containsBasemap();
					IndexItem basemap = indexFiles.getBasemap();
					if (!basemapExists && basemap != null) {
						List<DownloadEntry> downloadEntry = basemap.createDownloadEntry(uiActivity.getClientContext(),
								uiActivity.getType(), new ArrayList<DownloadEntry>());
						uiActivity.getEntriesToDownload().put(basemap, downloadEntry);
						AccessibleToast.makeText(uiActivity, R.string.basemap_was_selected_to_download, Toast.LENGTH_LONG).show();
						uiActivity.findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
					}
					if (indexFiles.isIncreasedMapVersion()) {
						showWarnDialog();
					}
				} else {
					AccessibleToast.makeText(ctx, R.string.list_index_files_was_not_loaded, Toast.LENGTH_LONG).show();
				}
				currentRunningTask.remove(this);
				if (uiActivity != null) {
					uiActivity.updateProgress(false);
					runCategorization(uiActivity.getType());
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
				builder.setNegativeButton(R.string.default_buttons_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.show();

			}

			@Override
			protected void updateProgress(boolean updateOnlyProgress) {
				if (uiActivity != null) {
					uiActivity.updateProgress(updateOnlyProgress);
				}

			};

		};
		execute(inst, new Void[0]);

	}
	
	public void runDownloadFiles(){
		if(checkRunning()) {
			return;
		}
		DownloadIndexesAsyncTask task = new DownloadIndexesAsyncTask(ctx);
		execute(task, new IndexItem[0]);
	}
	
	private <P>void execute(BasicProgressAsyncTask<P, ?, ?> task, P... indexItems) {
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ) {
			// TODO check
		    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, indexItems);
		} else {
			task.execute(indexItems);
		}
	}

	public Map<IndexItem, List<DownloadEntry>> getEntriesToDownload() {
		return entriesToDownload;
	}

	public void runCategorization(final DownloadActivityType type) {
		final BasicProgressAsyncTask<Void, Void, List<IndexItem>> inst = new BasicProgressAsyncTask<Void, Void, List<IndexItem>>(ctx) {
			private List<IndexItemCategory> cats;
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				currentRunningTask.add(this);
				this.message = ctx.getString(R.string.downloading_list_indexes);
				if(uiActivity != null) { 
					uiActivity.updateProgress(false);
				}
			}
			
			@Override
			protected List<IndexItem> doInBackground(Void... params) {
				final List<IndexItem> filtered = getFilteredByType();
				cats = IndexItemCategory.categorizeIndexItems(app, filtered);
				updateLoadedFiles();
				return filtered;
			};
			
			public List<IndexItem> getFilteredByType() {
				final List<IndexItem> filtered = new ArrayList<IndexItem>();
				if (type == DownloadActivityType.SRTM_FILE) {
					Map<String, String> indexFileNames = app.getResourceManager().getIndexFileNames();
					if (cachedSRTMFiles == null) {
						cachedSRTMFiles = new ArrayList<SrtmIndexItem>();
						synchronized (cachedSRTMFiles) {
							List<RegionCountry> countries = RegionRegistry.getRegionRegistry().getCountries();
							for (RegionCountry rc : countries) {
								if (rc.getTileSize() > 35 && rc.getSubRegions().size() > 0) {
									for (RegionCountry ch : rc.getSubRegions()) {
										cachedSRTMFiles.add(new SrtmIndexItem(ch, indexFileNames));
									}
								} else {
									cachedSRTMFiles.add(new SrtmIndexItem(rc, indexFileNames));
								}
							}
							filtered.addAll(cachedSRTMFiles);
						}
					} else {
						synchronized (cachedSRTMFiles) {
							for (SrtmIndexItem s : cachedSRTMFiles) {
								s.updateExistingTiles(indexFileNames);
								filtered.add(s);
							}
						}
					}
				}
				List<IndexItem> cachedIndexFiles = getCachedIndexFiles();
				if (cachedIndexFiles != null) {
					for (IndexItem file : cachedIndexFiles) {
						if (file.getType() == type) {
							filtered.add(file);
						}
					}
				}
				return filtered;
			}
			

			@Override
			protected void onPostExecute(List<IndexItem> filtered) {
				if (uiActivity != null) {
					DownloadIndexAdapter a = ((DownloadIndexAdapter) uiActivity.getExpandableListAdapter());
					a.setLoadedFiles(indexActivatedFileNames, indexFileNames);
					a.setIndexFiles(filtered, cats);
					a.notifyDataSetChanged();
					a.getFilter().filter(uiActivity.getFilterText());
					if (type == DownloadActivityType.SRTM_FILE && OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) instanceof SRTMPlugin
							&& !OsmandPlugin.getEnabledPlugin(SRTMPlugin.class).isPaid()) {
						Builder msg = new AlertDialog.Builder(uiActivity);
						msg.setTitle(R.string.srtm_paid_version_title);
						msg.setMessage(R.string.srtm_paid_version_msg);
						msg.setNegativeButton(R.string.button_upgrade_osmandplus, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:net.osmand.srtmPlugin.paid"));
								try {
									ctx.startActivity(intent);
								} catch (ActivityNotFoundException e) {
								}
							}
						});
						msg.setPositiveButton(R.string.default_buttons_ok, null);
						msg.show();
					}
				}
				currentRunningTask.remove(this);
				if(uiActivity != null) { 
					uiActivity.updateProgress(false);
				}
			}

			@Override
			protected void updateProgress(boolean updateOnlyProgress) {
				if(uiActivity != null) {
					uiActivity.updateProgress(updateOnlyProgress);
				}
				
			};
			
		};
		execute(inst, new Void[0]);
	}

	
	public boolean isDownloadRunning(){
		for(int i =0; i<currentRunningTask.size(); i++) {
			if (currentRunningTask.get(i) instanceof DownloadIndexesAsyncTask) {
				return true;
				
			}
		}
		return false;
	}
	
	public BasicProgressAsyncTask<?, ?, ?> getCurrentRunningTask() {
		for(int i = 0; i< currentRunningTask.size(); ) {
			if(currentRunningTask.get(i).getStatus() == Status.FINISHED) {
				currentRunningTask.remove(i);
			} else {
				i++;
			}
		}
		if(currentRunningTask.size() > 0) {
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

	public int getDownloads() {
		int i = 0;
		Collection<List<DownloadEntry>> vs = getEntriesToDownload().values();
		for (List<DownloadEntry> v : vs) {
			for(DownloadEntry e : v) {
				if(!currentDownloads.contains(e)) {
					i++;
				}
			}
		}
		if(!currentDownloads.isEmpty()) {
			i++;
		}
		return i;
	}
	
	
}