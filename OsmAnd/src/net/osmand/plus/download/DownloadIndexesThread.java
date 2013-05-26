package net.osmand.plus.download;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.view.View;
import android.widget.Toast;

public class DownloadIndexesThread {
	private DownloadIndexActivity uiActivity = null;
	private IndexFileList indexFiles = null;
	private final Context ctx;
	private OsmandApplication app;
	private final static Log log = PlatformUtil.getLog(DownloadIndexesThread.class);
	private DownloadFileHelper downloadFileHelper;
	private BasicProgressAsyncTask<?, ?, ?> currentRunningTask;

	public DownloadIndexesThread(Context ctx) {
		this.ctx = ctx;
		app = (OsmandApplication) ctx.getApplicationContext();
		downloadFileHelper = new DownloadFileHelper(app);
	}
	
	public void setUiActivity(DownloadIndexActivity uiActivity) {
		this.uiActivity = uiActivity;
	}

	public List<IndexItem> getCachedIndexFiles() {
		return indexFiles != null ? indexFiles.getIndexFiles() : null;
	}

	public boolean isDownloadedFromInternet() {
		return indexFiles != null && indexFiles.isDownloadedFromInternet();
	}
	
	public class DownloadIndexesAsyncTask extends BasicProgressAsyncTask<IndexItem, Object, String> implements DownloadFileShowWarning {

		private OsmandPreference<Integer> downloads;
		private TreeMap<IndexItem, List<DownloadEntry>> entriesToDownload;

		public DownloadIndexesAsyncTask(Context ctx, TreeMap<IndexItem, List<DownloadEntry>> entriesToDownload) {
			super(ctx);
			this.entriesToDownload = entriesToDownload;
			downloads = app.getSettings().NUMBER_OF_FREE_DOWNLOADS;
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			for (Object o : values) {
				if (o instanceof DownloadEntry) {
					if (uiActivity != null) {
						((DownloadIndexAdapter) uiActivity.getExpandableListAdapter()).notifyDataSetInvalidated();
						if (entriesToDownload.isEmpty()) {
							uiActivity.findViewById(R.id.DownloadButton).setVisibility(View.GONE);
						} else {
							uiActivity.makeDownloadVisible();
						}
					}
				} else if (o instanceof String) {
					AccessibleToast.makeText(ctx, (String) o, Toast.LENGTH_LONG).show();
				}
			}
			super.onProgressUpdate(values);
		}

		@Override
		protected void onPreExecute() {
			currentRunningTask = this;
			super.onPreExecute();
			if (uiActivity != null) {
				downloadFileHelper.setInterruptDownloading(false);
				View mainView = uiActivity.findViewById(R.id.MainLayout);
				if (mainView != null) {
					mainView.setKeepScreenOn(true);
				}
			}
		}

		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				AccessibleToast.makeText(ctx, result, Toast.LENGTH_LONG).show();
			}
			if (uiActivity != null) {
				View mainView = uiActivity.findViewById(R.id.MainLayout);
				if (mainView != null) {
					mainView.setKeepScreenOn(false);
				}
				uiActivity.updateLoadedFiles();
				DownloadIndexAdapter adapter = ((DownloadIndexAdapter) uiActivity.getExpandableListAdapter());
				if (adapter != null) {
					adapter.notifyDataSetInvalidated();
				}
			}
			if(uiActivity != null) { 
				uiActivity.updateProgress(false, null);
			}
		}
		
		private int countAllDownloadEntry(IndexItem... filesToDownload){
			int t = 0;
			for(IndexItem  i : filesToDownload){
				List<DownloadEntry> list = entriesToDownload.get(i);
				if(list != null){
					t += list.size();
				}
			}
			return t;
		}

		@Override
		protected String doInBackground(IndexItem... filesToDownload) {
			try {
				List<File> filesToReindex = new ArrayList<File>();
				boolean forceWifi = downloadFileHelper.isWifiConnected();
				int counter = 1;
				int all = countAllDownloadEntry(filesToDownload);
				for (int i = 0; i < filesToDownload.length; i++) {
					IndexItem filename = filesToDownload[i];
					List<DownloadEntry> list = entriesToDownload.get(filename);
					if (list != null) {
						for (DownloadEntry entry : list) {
							String indexOfAllFiles = all <= 1 ? "" : (" [" + counter + "/" + all + "]");
							counter++;
							boolean result = downloadFile(entry, filesToReindex, indexOfAllFiles, forceWifi);
							if (result) {
								entriesToDownload.remove(filename);
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
					}
				}
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
				if (vectorMapsToReindex) {
					List<String> warnings = manager.indexingMaps(this);
					if (!warnings.isEmpty()) {
						return warnings.get(0);
					}
				}
			} catch (InterruptedException e) {
				// do not dismiss dialog
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

		public boolean downloadFile(DownloadEntry de, List<File> filesToReindex, String indexOfAllFiles, boolean forceWifi)
				throws InterruptedException {
			boolean res = false;
			if (de.isAsset) {
				try {
					ResourceManager.copyAssets(app.getAssets(), de.targetFile.getPath(), de.targetFile);
					de.targetFile.setLastModified(de.dateModified);
					res = true;
				} catch (IOException e) {
					log.error("Copy exception", e);
				}
			} else {
				res = downloadFileHelper.downloadFile(de, this, filesToReindex, this, forceWifi);
			}
			if (res && de.attachedEntry != null) {
				return downloadFile(de.attachedEntry, filesToReindex, indexOfAllFiles, forceWifi);
			}
			return res;
		}

		@Override
		protected void updateProgress(boolean updateOnlyProgress) {
			if(uiActivity != null) {
				uiActivity.updateProgress(updateOnlyProgress, this);
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
	
	public void runDownloadFiles(TreeMap<IndexItem, List<DownloadEntry>> entriesToDownload){
		if(checkRunning()) {
			return;
		}
		IndexItem[] indexes = entriesToDownload.keySet().toArray(new IndexItem[0]);
		DownloadIndexesAsyncTask task = new DownloadIndexesAsyncTask(ctx, entriesToDownload);
		task.execute(indexes);
	}

	public void runReloadIndexFiles() {
		if(checkRunning()) {
			return;
		}
		final BasicProgressAsyncTask<Void, Void, IndexFileList> inst = new BasicProgressAsyncTask<Void, Void, IndexFileList>(ctx) {
			
			@Override
			protected IndexFileList doInBackground(Void... params) {
				return DownloadOsmandIndexesHelper.getIndexesList(ctx);
			};
			
			@Override
			protected void onPreExecute() {
				currentRunningTask = this;
				super.onPreExecute();
				this.message = ctx.getString(R.string.downloading_list_indexes);
				if(uiActivity != null) { 
					uiActivity.updateProgress(false, this);
				}
			}
			
			protected void onPostExecute(IndexFileList result) {
				indexFiles = result;
				if (indexFiles != null && uiActivity != null) {
					boolean basemapExists = uiActivity.getMyApplication().getResourceManager().containsBasemap();
					IndexItem basemap = indexFiles.getBasemap();
					if (!basemapExists && basemap != null) {
						List<DownloadEntry> downloadEntry = basemap
								.createDownloadEntry(uiActivity.getClientContext(), uiActivity.getType(), new ArrayList<DownloadEntry>());
						uiActivity.getEntriesToDownload().put(basemap, downloadEntry);
						AccessibleToast.makeText(uiActivity, R.string.basemap_was_selected_to_download, Toast.LENGTH_LONG).show();
						uiActivity.findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
					}
					uiActivity.setListAdapter(new DownloadIndexAdapter(uiActivity, uiActivity.getFilteredByType()));
					if (indexFiles.isIncreasedMapVersion()) {
						showWarnDialog();
					}
				} else {
					AccessibleToast.makeText(ctx, R.string.list_index_files_was_not_loaded, Toast.LENGTH_LONG).show();
				}
				if(uiActivity != null) { 
					uiActivity.updateProgress(false, null);
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
				if(uiActivity != null) {
					uiActivity.updateProgress(updateOnlyProgress, this);
				}
				
			};
			
		};
		inst.execute(new Void[0]);
	}

	
	public BasicProgressAsyncTask<?, ?, ?> getCurrentRunningTask() {
		if(currentRunningTask != null && currentRunningTask.getStatus() == Status.FINISHED) {
			currentRunningTask = null;
		}
		return currentRunningTask;
	}
	
	
}