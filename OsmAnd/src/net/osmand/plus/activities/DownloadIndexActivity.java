package net.osmand.plus.activities;

import static net.osmand.data.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.data.IndexConstants.BINARY_MAP_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.BINARY_MAP_VERSION;
import static net.osmand.data.IndexConstants.TTSVOICE_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.TTSVOICE_VERSION;
import static net.osmand.data.IndexConstants.VOICE_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.VOICE_VERSION;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.access.AccessibleToast;
import net.osmand.data.IndexConstants;
import net.osmand.plus.DownloadOsmandIndexesHelper;
import net.osmand.plus.DownloadOsmandIndexesHelper.IndexItem;
import net.osmand.plus.IndexFileList;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.DownloadFileHelper.DownloadFileShowWarning;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadIndexActivity extends OsmandExpandableListActivity {
	
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(DownloadIndexActivity.class);
	
	/** menus **/
	private static final int RELOAD_ID = 0;
	private static final int SELECT_ALL_ID = 1;
	private static final int DESELECT_ALL_ID = 2;
	private static final int FILTER_EXISTING_REGIONS = 3;
	
	/** dialogs **/
	protected static final int DIALOG_MAP_VERSION_UPDATE = 0;
	protected static final int DIALOG_PROGRESS_FILE = 1;
	protected static final int DIALOG_PROGRESS_LIST = 2;
	
    public static final String FILTER_KEY = "filter";
	
	private static DownloadIndexListThread downloadListIndexThread;

	private ProgressDialog progressFileDlg = null;
	private Map<String, String> indexFileNames = null;
	private Map<String, String> indexActivatedFileNames = null;
	private TreeMap<String, DownloadEntry> entriesToDownload = new TreeMap<String, DownloadEntry>();

	private int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 16;
	 
	
    private TextWatcher textWatcher ;
	private EditText filterText;
	private DownloadFileHelper downloadFileHelper = null;
	private OsmandSettings settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = ((OsmandApplication) getApplication()).getSettings();
		if(downloadListIndexThread == null) {
			downloadListIndexThread = new DownloadIndexListThread(this);
		}
		// recreation upon rotation is prevented in manifest file
		CustomTitleBar titleBar = new CustomTitleBar(this, R.string.local_index_download, R.drawable.tab_download_screen_icon);
		setContentView(R.layout.download_index);
		titleBar.afterSetContentView();
		
		
		
		downloadFileHelper = new DownloadFileHelper(this);
		findViewById(R.id.DownloadButton).setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				downloadFilesCheckFreeVersion();
			}
			
		});
		
		updateLoadedFiles();

	    filterText = (EditText) findViewById(R.id.search_box);
	    textWatcher = new TextWatcher() {
	        @Override
			public void afterTextChanged(Editable s) {
	        }
	        @Override
			public void beforeTextChanged(CharSequence s, int start, int count,
	                int after) {
	        }

	        @Override
			public void onTextChanged(CharSequence s, int start, int before,
	                int count) {
	        	DownloadIndexAdapter adapter = ((DownloadIndexAdapter)getExpandableListAdapter());
				if(adapter != null){
					adapter.getFilter().filter(s);
				}
	        }

	    };
		filterText.addTextChangedListener(textWatcher);
		final Intent intent = getIntent();
		if (intent != null && intent.getExtras() != null) {
			final String filter = intent.getExtras().getString(FILTER_KEY);
			if (filter != null) {
				filterText.setText(filter);
			}
		}

		if (downloadListIndexThread.getCachedIndexFiles() != null && downloadListIndexThread.isDownloadedFromInternet()) {
			setListAdapter(new DownloadIndexAdapter(downloadListIndexThread.getCachedIndexFiles()));
		} else {
			downloadIndexList();
		}
		if(Version.isFreeVersion(this) && settings.checkFreeDownloadsNumberZero()){
			Builder msg = new AlertDialog.Builder(this);
			msg.setTitle(R.string.free_version_title);
			msg.setMessage(getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS+"", ""));
			msg.show();
		}
	}


	private void updateLoadedFiles() {
		indexActivatedFileNames = getMyApplication().getResourceManager().getIndexFileNames();
		indexFileNames = getMyApplication().getResourceManager().getIndexFileNames();
		getMyApplication().getResourceManager().getBackupIndexes(indexFileNames);
	}

	private void downloadIndexList() {
		showDialog(DIALOG_PROGRESS_LIST);
	}
	
	@Override
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.clear();
		menu.add(0, RELOAD_ID, 0, R.string.update_downlod_list);
		if (getExpandableListAdapter() != null) {
			// item.setIcon(R.drawable.ic_menu_refresh);
			menu.add(0, SELECT_ALL_ID, 0, R.string.select_all);
			menu.add(0, DESELECT_ALL_ID, 0, R.string.deselect_all);
			menu.add(0, FILTER_EXISTING_REGIONS, 0, R.string.filter_existing_indexes);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == RELOAD_ID){
			//re-create the thread
			downloadListIndexThread = new DownloadIndexListThread(this);
			downloadIndexList();
		} else {
			final DownloadIndexAdapter listAdapter = (DownloadIndexAdapter)getExpandableListAdapter();
			if(item.getItemId() == SELECT_ALL_ID){
				int selected = 0;
				for (int i = 0; i < listAdapter.getChildrenCount(0); i++) {
					IndexItem es = listAdapter.getChild(0, i);
					if(!entriesToDownload.containsKey(es.getFileName())){
						selected++;
						entriesToDownload.put(es.getFileName(), es.createDownloadEntry(DownloadIndexActivity.this));
					}
				}
				AccessibleToast.makeText(this, MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
				listAdapter.notifyDataSetInvalidated();
				if(selected > 0){
					findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
				}
			} else if (item.getItemId() == FILTER_EXISTING_REGIONS) {
				final Collection<String> listAlreadyDownloaded = listAlreadyDownloadedWithAlternatives();
				final List<IndexItem> filtered = new ArrayList<IndexItem>();
				for (String file : listAlreadyDownloaded) {
					IndexItem fileItem = listAdapter.getIndexFiles().get(file);
					if (fileItem != null) {
						filtered.add(fileItem);
					}
				}
				listAdapter.setIndexFiles(filtered);
			} else if(item.getItemId() == DESELECT_ALL_ID){
				entriesToDownload.clear();
				listAdapter.notifyDataSetInvalidated();
				findViewById(R.id.DownloadButton).setVisibility(View.GONE);
			} else {
				return false;
			}
		}
		return true;
	}
	
	private static class DownloadIndexListThread extends Thread {
		private DownloadIndexActivity uiActivity = null;
		private IndexFileList indexFiles = null;
		private final Context ctx; 
		
		public DownloadIndexListThread(Context ctx){
			super("DownloadIndexes");
			this.ctx = ctx;
			
		}
		public void setUiActivity(DownloadIndexActivity uiActivity) {
			this.uiActivity = uiActivity;
		}
		
		public Map<String, IndexItem> getCachedIndexFiles() {
			return indexFiles != null ? indexFiles.getIndexFiles() : null;
		}
		
		public boolean isDownloadedFromInternet(){
			return indexFiles != null && indexFiles.isDownloadedFromInternet();
		}
		
		@Override
		public void run() {
			indexFiles = DownloadOsmandIndexesHelper.getIndexesList(ctx);
			if(uiActivity != null) {
				uiActivity.removeDialog(DIALOG_PROGRESS_LIST);
				uiActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (indexFiles != null) {
							boolean basemapExists = ((OsmandApplication) uiActivity.getApplication()).getResourceManager().containsBasemap();
							if(!basemapExists && indexFiles.getBasemap() != null) {
								uiActivity.entriesToDownload.put(indexFiles.getBasemap().getFileName(), 
										indexFiles.getBasemap().createDownloadEntry(ctx));
								AccessibleToast.makeText(uiActivity, R.string.basemap_was_selected_to_download, 
										Toast.LENGTH_LONG).show();
								uiActivity.findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
							}
							uiActivity.setListAdapter(uiActivity.new DownloadIndexAdapter(indexFiles.getIndexFiles()));
							if (indexFiles.isIncreasedMapVersion()) {
								uiActivity.showDialog(DownloadIndexActivity.DIALOG_MAP_VERSION_UPDATE);
							}
						} else {
							AccessibleToast.makeText(uiActivity, R.string.list_index_files_was_not_loaded, Toast.LENGTH_LONG).show();
						}
					}
				});
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_MAP_VERSION_UPDATE:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.map_version_changed_info);
				builder.setPositiveButton(R.string.button_upgrade_osmandplus, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:net.osmand.plus"));
						try {
							startActivity(intent);
						} catch (ActivityNotFoundException e) {
							log.error("Exception", e);
						}
					}
				});
				builder.setNegativeButton(R.string.default_buttons_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						removeDialog(DIALOG_MAP_VERSION_UPDATE); 
					}
				});
				return builder.create();
			case DIALOG_PROGRESS_LIST:
				ProgressDialog dialog = new ProgressDialog(this);
				dialog.setTitle(R.string.downloading);
				dialog.setMessage(getString(R.string.downloading_list_indexes));
				dialog.setCancelable(true);
				return dialog;
			case DIALOG_PROGRESS_FILE:
				ProgressDialogImplementation progress = ProgressDialogImplementation.createProgressDialog(
						DownloadIndexActivity.this,
						getString(R.string.downloading),
						getString(R.string.downloading_file),
						ProgressDialog.STYLE_HORIZONTAL,
						new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								makeSureUserCancelDownload(dialog);
							}
						});
				progressFileDlg = progress.getDialog();
				progressFileDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						downloadFileHelper.setInterruptDownloading(true);
					}
				});
				return progress.getDialog();
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
			case DIALOG_PROGRESS_FILE:
				DownloadIndexesAsyncTask task = new DownloadIndexesAsyncTask(new ProgressDialogImplementation(progressFileDlg,true));
				String[] indexes = entriesToDownload.keySet().toArray(new String[0]);
				task.execute(indexes);
				break;
			case DIALOG_PROGRESS_LIST:
				downloadListIndexThread.setUiActivity(this);
				if(downloadListIndexThread.getState() == Thread.State.NEW){
					downloadListIndexThread.start();
				} else if(downloadListIndexThread.getState() == Thread.State.TERMINATED){
					// possibly exception occurred we don't have cache of files
					downloadListIndexThread = new DownloadIndexListThread(this);
					downloadListIndexThread.setUiActivity(this);
					downloadListIndexThread.start();
				}
				break;
		}
	}
	
	public ExpandableListView getListView() {
		return super.getExpandableListView();
	}
	
	public ExpandableListAdapter getListAdapter() {
		return super.getExpandableListAdapter();
	}
	
	private void makeSureUserCancelDownload(final DialogInterface dlg) {
		Builder bld = new AlertDialog.Builder(this);
		bld.setTitle(getString(R.string.default_buttons_cancel));
		bld.setMessage(R.string.confirm_interrupt_download);
		bld.setPositiveButton(R.string.default_buttons_yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dlg.dismiss();
			}
		});
		bld.setNegativeButton(R.string.default_buttons_no, null);
		bld.show();
	}
	
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		final IndexItem e = (IndexItem) ((DownloadIndexAdapter)getListAdapter()).getChild(groupPosition, childPosition);
		String key = e.getFileName();
		final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
		
		if(ch.isChecked()){
			ch.setChecked(!ch.isChecked());
			entriesToDownload.remove(key);
			if(entriesToDownload.isEmpty()){
				int x = getListView().getScrollX();
				int y = getListView().getScrollY();
				findViewById(R.id.DownloadButton).setVisibility(View.GONE);
				getListView().scrollTo(x, y);
			}
			return true;
		}
		
		final DownloadEntry entry = e.createDownloadEntry(DownloadIndexActivity.this);
		if (entry != null) {
			// if(!fileToUnzip.exists()){
			// builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, extractDateAndSize(e.getValue())));
			entriesToDownload.put(e.getFileName(), entry);
			int x = getListView().getScrollX();
			int y = getListView().getScrollY();
			findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
			getListView().scrollTo(x, y);
			ch.setChecked(!ch.isChecked());
		}
		return true;
	}
	
	
	private Collection<String> listAlreadyDownloadedWithAlternatives() {
		Set<String> files = new TreeSet<String>();
		File externalStorageDirectory = settings.getExternalStorageDirectory();
		// files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.POI_PATH),POI_INDEX_EXT,POI_INDEX_EXT_ZIP,POI_TABLE_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.APP_DIR),BINARY_MAP_INDEX_EXT,BINARY_MAP_INDEX_EXT_ZIP,BINARY_MAP_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.BACKUP_PATH),BINARY_MAP_INDEX_EXT,BINARY_MAP_INDEX_EXT_ZIP,BINARY_MAP_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.VOICE_PATH),"",VOICE_INDEX_EXT_ZIP, VOICE_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.VOICE_PATH),"",TTSVOICE_INDEX_EXT_ZIP, TTSVOICE_VERSION));
		return files;
	}
	
	private Collection<? extends String> listWithAlternatives(File file, final String ext, final String secondaryExt, final int version) {
		final List<String> files = new ArrayList<String>();
		if (file.isDirectory()) {
			file.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith(ext)) {
						files.add(filename);
						files.add(filename.substring(0, filename.length() - ext.length()) + "_" + version + ext);
						if (secondaryExt != null) {
							files.add(filename.substring(0, filename.length() - ext.length()) + "_" + version + secondaryExt);
						}
						return true;
					} else {
						return false;
					}
				}
			});

		}
		return files;
	}

	protected void downloadFilesCheckFreeVersion() {
		if (Version.isFreeVersion(this)) {
			int total = settings.NUMBER_OF_FREE_DOWNLOADS.get() + entriesToDownload.size();
			boolean wiki = false;
			for (DownloadEntry es : entriesToDownload.values()) {
				if (es.baseName != null && es.baseName.contains("_wiki")) {
					wiki = true;
					break;
				}
			}
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS || wiki) {
				Builder msg = new AlertDialog.Builder(this);
				msg.setTitle(R.string.free_version_title);
				msg.setMessage(getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "", "( =" + total + ") "));
				msg.setPositiveButton(R.string.default_buttons_ok, null);
				msg.show();
			} else {
				downloadFilesPreCheckSpace();
			}
		} else {
			downloadFilesPreCheckSpace();
		}
	}
	
	protected void downloadFilesPreCheckSpace() {
		double sz = 0;
		for(DownloadEntry es : entriesToDownload.values()){
			sz += es.sizeMB;
		}
		// get availabile space 
		File dir = settings.extendOsmandPath("");
		double asz = -1;
		if(dir.canRead()){
			StatFs fs = new StatFs(dir.getAbsolutePath());
			asz = (((long) fs.getAvailableBlocks()) * fs.getBlockSize()) / (1 << 20);
		}
		if(asz != -1 && asz < sz ){
			AccessibleToast.makeText(this, getString(R.string.download_files_not_enough_space, sz, asz), Toast.LENGTH_LONG).show();
		} else {
			Builder builder = new AlertDialog.Builder(this);
			if (asz > 0 && sz/asz > 0.8) {
				builder.setMessage(MessageFormat.format(getString(R.string.download_files_question_space), entriesToDownload.size(), sz,
						asz));
			} else {
				builder.setMessage(MessageFormat.format(getString(R.string.download_files_question), entriesToDownload.size(), sz));
			}
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					showDialog(DIALOG_PROGRESS_FILE);
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
		}
	}

	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(isFinishing()){
			downloadFileHelper.setInterruptDownloading(true);
		}
	    if (textWatcher != null) {
	    	EditText filterText = (EditText) findViewById(R.id.search_box);
	    	filterText.removeTextChangedListener(textWatcher);
	    }
		downloadListIndexThread.setUiActivity(null);
	}
	

	private String convertServerFileNameToLocal(String name){
		int l = name.lastIndexOf('_');
		String s;
		if(name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
			s = IndexConstants.BINARY_MAP_INDEX_EXT;
		} else {
			s = ""; //$NON-NLS-1$
		}
		return name.substring(0, l) + s;
	}
	
	private class DownloadIndexesAsyncTask extends  AsyncTask<String, Object, String> implements DownloadFileShowWarning {
		
		private IProgress progress;
		private OsmandPreference<Integer> downloads;

		public DownloadIndexesAsyncTask(ProgressDialogImplementation progressDialogImplementation) {
			this.progress = progressDialogImplementation;
			downloads = settings.NUMBER_OF_FREE_DOWNLOADS;
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			for(Object o : values){
				if(o instanceof DownloadEntry){
					String v = Version.getAppName(DownloadIndexActivity.this);
					if(Version.isProductionVersion(DownloadIndexActivity.this)){
						v = Version.getFullVersion(DownloadIndexActivity.this);
					} else {
						v +=" test";
					}
					trackEvent(v, Version.getAppName(DownloadIndexActivity.this),
							((DownloadEntry)o).baseName, 1, getString(R.string.ga_api_key));
					updateLoadedFiles();
					((DownloadIndexAdapter) getExpandableListAdapter()).notifyDataSetInvalidated();
					findViewById(R.id.DownloadButton).setVisibility(
							entriesToDownload.isEmpty() ? View.GONE : View.VISIBLE);
				} else if(o instanceof String) {
					AccessibleToast.makeText(DownloadIndexActivity.this, (String) o, Toast.LENGTH_LONG).show();
				}
			}
			super.onProgressUpdate(values);
		}
		
		@Override
		protected void onPreExecute() {
			downloadFileHelper.setInterruptDownloading(false);
			View mainView = findViewById(R.id.MainLayout);
			if (mainView != null) {
				mainView.setKeepScreenOn(true);
			}
		}
		
		
		@Override
		protected void onPostExecute(String result) {
			if(result != null){
				AccessibleToast.makeText(DownloadIndexActivity.this, result, Toast.LENGTH_LONG).show();
			}
			View mainView = findViewById(R.id.MainLayout);
			if(mainView != null){
				mainView.setKeepScreenOn(false);
			}
			updateLoadedFiles();
			((DownloadIndexAdapter) getExpandableListAdapter()).notifyDataSetInvalidated();
		}
		
		@Override
		protected String doInBackground(String... filesToDownload) {
			try {
				List<File> filesToReindex = new ArrayList<File>();
				boolean forceWifi = downloadFileHelper.isWifiConnected();
				for (int i = 0; i < filesToDownload.length; i++) {
					String filename = filesToDownload[i];
					DownloadEntry entry = entriesToDownload.get(filename);
					if (entry != null) {
						String indexOfAllFiles = filesToDownload.length <= 1 ? "" : (" [" + (i + 1) + "/"
								+ filesToDownload.length + "]");
						boolean result = entry.downloadFile(downloadFileHelper, filename, filesToReindex, progress, indexOfAllFiles, this, forceWifi, getAssets()); 
						if (result) {
							entriesToDownload.remove(filename);
							downloads.set(downloads.get() + 1);
							if(entry.existingBackupFile != null) {
								Algoritms.removeAllFiles(entry.existingBackupFile);
							}
							publishProgress(entry);
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
				ResourceManager manager = getMyApplication().getResourceManager();
				manager.indexVoiceFiles(progress);
				if (vectorMapsToReindex) {
					List<String> warnings = manager.indexingMaps(progress);
					if (!warnings.isEmpty()) {
						return warnings.get(0);
					}
				}
			} catch (InterruptedException e) {
				// do not dismiss dialog
			} finally {
				if (progressFileDlg != null) {
					removeDialog(DIALOG_PROGRESS_FILE);
					progressFileDlg = null;
				}
			}
			return null;
		}

		@Override
		public void showWarning(String warning) {
			publishProgress(warning);
			
		}
	}
	
	public static class DownloadEntry {
		public File fileToSave;
		public File fileToUnzip;
		public boolean unzip;
		public Long dateModified;
		public double sizeMB;
		public String baseName;
		public int parts;
		public File existingBackupFile;
		public DownloadEntry attachedEntry;
		public boolean isAsset;
		
		public DownloadEntry() {
			// default
		}
		public DownloadEntry(String assetName, String fileName, long dateModified) {
			this.dateModified = dateModified;
			fileToUnzip = new File(fileName);
			fileToSave = new File(assetName);
			isAsset = true;
		}
		
		public boolean downloadFile(DownloadFileHelper downloadFileHelper, String filename, List<File> filesToReindex,
				IProgress progress, String indexOfAllFiles, DownloadIndexesAsyncTask downloadIndexesAsyncTask,
				boolean forceWifi, AssetManager assetManager)
				throws InterruptedException {
			boolean res = false;
			if (isAsset) {
				try {
					ResourceManager.copyAssets(assetManager, fileToSave.getPath(), fileToUnzip);
					fileToUnzip.setLastModified(this.dateModified);
					res = true;
				} catch (IOException e) {
					log.error("Copy exception", e);
				}
			} else {
				res = downloadFileHelper.downloadFile(filename, fileToSave, fileToUnzip, unzip, progress, dateModified, parts,
						filesToReindex, indexOfAllFiles, downloadIndexesAsyncTask, forceWifi);
			}
			if (res && attachedEntry != null) {
				return attachedEntry.downloadFile(downloadFileHelper, filename, filesToReindex, progress, indexOfAllFiles,
						downloadIndexesAsyncTask, forceWifi, assetManager);
			}
			return res;
		}
	}
	
	
	private static class IndexItemCategory implements Comparable<IndexItemCategory> {
		public final String name; 
		public final List<IndexItem> items = new ArrayList<IndexItem>();
		private final int order;
		
		public IndexItemCategory(String name, int order) {
			this.name = name;
			this.order = order;
		}
		
		@Override
		public int compareTo(IndexItemCategory another) {
			return order < another.order ? -1 : 1;
		}
	}
	
	
	public List<IndexItemCategory> categorizeIndexItems(Collection<IndexItem> indexItems) {
		final Map<String, IndexItemCategory> cats = new TreeMap<String, DownloadIndexActivity.IndexItemCategory>();
		for(IndexItem i : indexItems){
			int nameId = R.string.index_name_other;
			int order = 0;
			String lc = i.getFileName().toLowerCase();
			if(lc.endsWith(".voice.zip")) {
				nameId = R.string.index_name_voice;
				order = 1; 
			} else if(lc.contains(".ttsvoice.zip")) {
				nameId = R.string.index_name_tts_voice;
				order = 2;
			} else if(lc.startsWith("us")) {
				nameId = R.string.index_name_us;
				order = 31;
			} else if(lc.contains("_northamerica_")) {
				nameId = R.string.index_name_north_america;
				order = 30;
			} else if(lc.contains("_centralamerica_") || lc.contains("central-america")) {
				nameId = R.string.index_name_central_america;
				order = 40;
			} else if(lc.contains("_southamerica_") || lc.contains("south-america")) {
				nameId = R.string.index_name_south_america;
				order = 45;
			} else if(lc.startsWith("france_")) {
				nameId = R.string.index_name_france;
				order = 17;
			} else if(lc.startsWith("germany_")) {
				nameId = R.string.index_name_germany;
				order = 16;
			} else if(lc.contains("_europe_")) {
				nameId = R.string.index_name_europe;
				order = 15;
			} else if(lc.startsWith("russia_")) {
				nameId = R.string.index_name_russia;
				order = 18;
			} else if(lc.contains("africa")) {
				nameId = R.string.index_name_africa;
				order = 80;
			} else if(lc.contains("_asia_")) {
				nameId = R.string.index_name_asia;
				order = 50;
			} else if(lc.contains("_oceania_") || lc.contains("australia") ) {
				nameId = R.string.index_name_oceania;
				order = 70;
			} else if(lc.contains("_wiki_")) {
				nameId = R.string.index_name_wiki;
				order = 10;
			}
			
			String name = getString(nameId);
			if (!cats.containsKey(name)) {
				cats.put(name, new IndexItemCategory(name, order));
			}
			cats.get(name).items.add(i);	
		}	
		ArrayList<IndexItemCategory> r = new ArrayList<DownloadIndexActivity.IndexItemCategory>(cats.values());
		Collections.sort(r);
		return r;
	}
	

	protected class DownloadIndexAdapter extends OsmandBaseExpandableListAdapter implements Filterable {
		
		private DownloadIndexFilter myFilter;
		private final Map<String, IndexItem> indexFiles;
		private final List<IndexItemCategory> list = new ArrayList<IndexItemCategory>();

		public DownloadIndexAdapter(Map<String, IndexItem> indexFiles) {
			this.indexFiles = new LinkedHashMap<String, IndexItem>(indexFiles);
			List<IndexItemCategory> cats = categorizeIndexItems(indexFiles.values());
			synchronized (this) {
				list.clear();
				list.addAll(cats);
			}
			getFilter().filter(filterText.getText());
		}
		
		public void collapseTrees(final CharSequence constraint){
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					synchronized (DownloadIndexAdapter.this) {
						final ExpandableListView expandableListView = getExpandableListView();
						for (int i = 0; i < getGroupCount(); i++) {
							int cp = getChildrenCount(i);
							if (cp < 7) {
								expandableListView.expandGroup(i);
							} else {
								expandableListView.collapseGroup(i);
							}
						}
					}
				}
			});
			
		}

		public Map<String, IndexItem> getIndexFiles() {
			return indexFiles;
		}
		
		public void setIndexFiles(List<IndexItem> indexFiles) {
			this.indexFiles.clear();
			for(IndexItem i : indexFiles) {
				this.indexFiles.put(i.getFileName(), i);
			}
			List<IndexItemCategory> cats = categorizeIndexItems(indexFiles);
			synchronized (this) {
				list.clear();
				list.addAll(cats);
			}
			notifyDataSetChanged();
		}
		
		
		@Override
		public Filter getFilter() {
			if (myFilter == null) {
				myFilter = new DownloadIndexFilter();
			}
			return myFilter;
		}
		
		private final class DownloadIndexFilter extends Filter {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				if (constraint == null || constraint.length() == 0) {
					results.values = indexFiles.values();
					results.count = indexFiles.size();
				} else {
					String[] vars = constraint.toString().split("\\s");
					for(int i=0; i< vars.length; i++){
						vars[i] = vars[i].trim().toLowerCase();
					}
					List<IndexItem> filter = new ArrayList<IndexItem>();
					for (IndexItem item : indexFiles.values()) {
						boolean add = true;
						for (String var : vars) {
							if (var.length() > 0) {
								if (!item.getVisibleName().toLowerCase().contains(var)
										&& !item.getDescription().toLowerCase().contains(var)) {
									add = false;
								}
							}
						}
						if(add){
							filter.add(item);
						}
							
					}
					results.values = filter;
					results.count = filter.size();
				}
				return results;
			}
			
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				synchronized (DownloadIndexAdapter.this) {
					list.clear();
					Collection<IndexItem> items = (Collection<IndexItem>) results.values;
					if (items != null && !items.isEmpty()) {
						list.addAll(categorizeIndexItems(items));
					} else {
						list.add(new IndexItemCategory(getResources().getString(R.string.select_index_file_to_download), 1));
					}
				}
				notifyDataSetChanged();
				collapseTrees(constraint);
			}
		}

		@Override
		public int getGroupCount() {
			return list.size();
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return list.get(groupPosition).items.size();
		}

		@Override
		public IndexItemCategory getGroup(int groupPosition) {
			return list.get(groupPosition);
		}

		@Override
		public IndexItem getChild(int groupPosition, int childPosition) {
			return list.get(groupPosition).items.get(childPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition + (childPosition + 1)* 10000;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			View v = convertView;
			IndexItemCategory group = getGroup(groupPosition);
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.download_index_list_item_category, parent, false);
			}
			final View row = v; 
			TextView item = (TextView) row.findViewById(R.id.download_index_category_name);
			item.setText(group.name);
			item.setLinkTextColor(Color.YELLOW);
			adjustIndicator(groupPosition, isExpanded, v);
			return row;
		}

		@Override
		public View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.download_index_list_item, parent, false);
			}
			final View row = v; 
			TextView item = (TextView) row.findViewById(R.id.download_item);
			TextView description = (TextView) row.findViewById(R.id.download_descr);
			IndexItem e = (IndexItem) getChild(groupPosition, childPosition);
			item.setText((e.getVisibleDescription(DownloadIndexActivity.this) + "\n" + e.getVisibleName()).trim()); //$NON-NLS-1$
			description.setText(e.getDate() + "\n" + e.getSize() + " MB");
			
			CheckBox ch = (CheckBox) row.findViewById(R.id.check_download_item);
			ch.setChecked(entriesToDownload.containsKey(e.getFileName()));
			ch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
					ch.setChecked(!ch.isChecked());
					DownloadIndexActivity.this.onChildClick(getListView(), row, groupPosition, childPosition, getChildId(groupPosition, childPosition));
				}
			});
			
			if(indexFileNames != null){
				String sfName = convertServerFileNameToLocal(e.getFileName());
				if(!indexFileNames.containsKey(sfName)){
					item.setTextColor(getResources().getColor(R.color.index_unknown));
				} else {
					if(e.getDate() != null){
						if(e.getDate().equals(indexActivatedFileNames.get(sfName))){
							item.setText(item.getText() + "\n" + getResources().getString(R.string.local_index_installed) + " : " + indexActivatedFileNames.get(sfName));
							item.setTextColor(getResources().getColor(R.color.act_index_uptodate)); //GREEN
						} else if (e.getDate().equals(indexFileNames.get(sfName))) {
							item.setText(item.getText() + "\n" + getResources().getString(R.string.local_index_installed) + " : " + indexFileNames.get(sfName));
							item.setTextColor(getResources().getColor(R.color.deact_index_uptodate)); //DARK_GREEN
						} else if (indexActivatedFileNames.containsKey(sfName)) {
							item.setText(item.getText() + "\n" + getResources().getString(R.string.local_index_installed) + " : " +  indexActivatedFileNames.get(sfName));
							item.setTextColor(getResources().getColor(R.color.act_index_updateable)); //LIGHT_BLUE
						} else {
							item.setText(item.getText() + "\n" + getResources().getString(R.string.local_index_installed) + " : " +  indexFileNames.get(sfName));
							item.setTextColor(getResources().getColor(R.color.deact_index_updateable)); //DARK_BLUE
						}
					} else {
						item.setTextColor(getResources().getColor(R.color.act_index_uptodate));
					}
				}
			}
			return row;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}
	
	private Map<String, String> getCustomVars(){
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("App", Version.getFullVersion(this));
		map.put("Device", Build.DEVICE);
		map.put("Brand", Build.BRAND);
		map.put("Model", Build.MODEL);
		map.put("Package", getPackageName());
		
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			if (info != null) {
				map.put("Version name", info.versionName);
				map.put("Version code", info.versionCode+"");
			}
		} catch (NameNotFoundException e) {
		}
		return map;
	}
	

	private String randomNumber() {
		return (new Random(System.currentTimeMillis()).nextInt(100000000) + 100000000) + "";
	}

    final String beaconUrl = "http://www.google-analytics.com/__utm.gif";
    final String analyticsVersion = "4.3"; // Analytics version - AnalyticsVersion
	public void trackEvent(String category, String action, String label, int value, String trackingAcount) {
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		try {
			Map<String, String> customVariables = getCustomVars();
			parameters.put("AnalyticsVersion", analyticsVersion);
			parameters.put("utmn", randomNumber());
			parameters.put("utmhn", "http://app.osmand.net");
			parameters.put("utmni", "1");
			parameters.put("utmt", "event");

			StringBuilder customVars = new StringBuilder();
			Iterator<Entry<String, String>> customs = customVariables.entrySet().iterator();
			for (int i = 0; i < customVariables.size(); i++) {
				Entry<String, String> n = customs.next();
				if (i > 0) {
					customVars.append("*");
				}
				// "'" => "'0", ')' => "'1", '*' => "'2", '!' => "'3",
				customVars.append((i + 1) + "!").append((n.getKey() + n.getValue()));
			}

			parameters.put("utmcs", "UTF-8");
			parameters.put("utmul", "en");
			parameters.put("utmhid", (System.currentTimeMillis()/ 1000) + "");
			parameters.put("utmac", trackingAcount);
			String domainHash = "app.osmand.net".hashCode()+"";

			String utma = domainHash + ".";
			File fl = getMyApplication().getSettings().extendOsmandPath(ResourceManager.APP_DIR + ".nomedia");
			if(fl.exists()) {
				utma += (fl.lastModified())+ ".";
			} else {
				utma += (randomNumber())+ ".";
			}
			utma += ((System.currentTimeMillis()/ 1000) + ".");
			utma += ((System.currentTimeMillis()/ 1000) + ".");
			utma += ((System.currentTimeMillis()/ 1000) + ".");
			utma += "1";
			parameters.put("utmcc", "__utma=" + utma + ";");
			parameters.put("utme", MessageFormat.format("5({0}*{1}*{2})({3})", category, action, label == null ? "" : label, value)
					+ customVars);

			StringBuilder urlString = new StringBuilder(beaconUrl + "?");
			Iterator<Entry<String, String>> it = parameters.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> e = it.next();
				urlString.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), "UTF-8"));
				if (it.hasNext()) {
					urlString.append("&");
				}
			}
			

			log.debug(urlString);
			URL url = new URL(urlString.toString());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setDoInput(false);
			conn.setDoOutput(false);
			conn.connect();
			log.info("Response analytics is " + conn.getResponseCode() + " " + conn.getResponseMessage());
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	

}
