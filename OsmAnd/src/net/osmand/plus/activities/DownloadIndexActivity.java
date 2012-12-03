package net.osmand.plus.activities;

import static net.osmand.data.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.data.IndexConstants.BINARY_MAP_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.BINARY_MAP_VERSION;
import static net.osmand.data.IndexConstants.EXTRA_EXT;
import static net.osmand.data.IndexConstants.EXTRA_ZIP_EXT;
import static net.osmand.data.IndexConstants.TTSVOICE_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.TTSVOICE_VERSION;
import static net.osmand.data.IndexConstants.VOICE_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.VOICE_VERSION;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.osmand.Algoritms;
import net.osmand.IProgress;
import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.access.AccessibleToast;
import net.osmand.data.IndexConstants;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadEntry;
import net.osmand.plus.download.DownloadFileHelper;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.download.DownloadIndexAdapter;
import net.osmand.plus.download.DownloadIndexListThread;
import net.osmand.plus.download.DownloadTracker;
import net.osmand.plus.download.IndexItem;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StatFs;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

public class DownloadIndexActivity extends OsmandExpandableListActivity {
	
	private static final org.apache.commons.logging.Log log = LogUtil.getLog(DownloadIndexActivity.class);
	
	/** menus **/
	private static final int RELOAD_ID = 0;
	private static final int SELECT_ALL_ID = 1;
	private static final int DESELECT_ALL_ID = 2;
	private static final int FILTER_EXISTING_REGIONS = 3;
	private static final int DOWNLOAD_FILES_TYPE = 4;
	
	/** dialogs **/
	public static final int DIALOG_MAP_VERSION_UPDATE = 0;
	public static final int DIALOG_PROGRESS_FILE = 1;
	public static final int DIALOG_PROGRESS_LIST = 2;
	
    public static final String FILTER_KEY = "filter";
	
	private static DownloadIndexListThread downloadListIndexThread;

	private ProgressDialog progressFileDlg = null;
	
	private TreeMap<String, DownloadEntry> entriesToDownload = new TreeMap<String, DownloadEntry>();
	private DownloadActivityType type = DownloadActivityType.NORMAL_FILE;

	private int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 10;
	 
	
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
		
		
		
		downloadFileHelper = new DownloadFileHelper(getClientContext());
		findViewById(R.id.DownloadButton).setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				downloadFilesCheckFreeVersion();
			}
			
		});
		
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
			DownloadIndexAdapter adapter = new DownloadIndexAdapter(this, getFilteredByType());
			setListAdapter(adapter);
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


	public void updateLoadedFiles() {
		if(getExpandableListAdapter() != null) {
			((DownloadIndexAdapter)getExpandableListAdapter()).updateLoadedFiles();
		}
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
			menu.add(0, DOWNLOAD_FILES_TYPE, 0, getType() == DownloadActivityType.NORMAL_FILE ? 
						R.string.download_roads_only_maps : R.string.download_regular_maps );
		}
		return true;
	}
	
	public DownloadActivityType getType() {
		return type;
	}
	
	public TreeMap<String, DownloadEntry> getEntriesToDownload() {
		return entriesToDownload;
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
				for (int j = 0; j < listAdapter.getGroupCount(); j++) {
					for (int i = 0; i < listAdapter.getChildrenCount(j); i++) {
						IndexItem es = listAdapter.getChild(j, i);
						if (!entriesToDownload.containsKey(es.getFileName())) {
							selected++;
							entriesToDownload.put(es.getFileName(), es.createDownloadEntry(getClientContext(), type));
						}
					}
				}
				AccessibleToast.makeText(this, MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
				listAdapter.notifyDataSetInvalidated();
				if(selected > 0){
					findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
				}
			} else if (item.getItemId() == FILTER_EXISTING_REGIONS) {
				final Set<String> listAlreadyDownloaded = listAlreadyDownloadedWithAlternatives();
				final List<IndexItem> filtered = new ArrayList<IndexItem>();
				for (IndexItem fileItem : listAdapter.getIndexFiles()) {
					if (listAlreadyDownloaded.contains(fileItem.convertServerFileNameToLocal())) {
						filtered.add(fileItem);
					}
				}
				listAdapter.setIndexFiles(filtered);
			} else if (item.getItemId() == DOWNLOAD_FILES_TYPE) {
				if(type == DownloadActivityType.ROADS_FILE){
					changeType(DownloadActivityType.NORMAL_FILE);
				} else {
					changeType(DownloadActivityType.ROADS_FILE);
				}
				
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


	public void changeType(final DownloadActivityType tp) {
		if (downloadListIndexThread != null) {
			type = tp;
			final List<IndexItem> filtered = getFilteredByType();
			entriesToDownload.clear();
			DownloadIndexAdapter a = ((DownloadIndexAdapter) getExpandableListAdapter());
			a.setIndexFiles(filtered);
			a.getFilter().filter(filterText.getText());
			
		}
	}


	public List<IndexItem> getFilteredByType() {
		final List<IndexItem> filtered = new ArrayList<IndexItem>();
		for (IndexItem file : downloadListIndexThread.getCachedIndexFiles()) {
			if (file.getType() == type) {
				filtered.add(file);
			}
		}
		return filtered;
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
		
		final DownloadEntry entry = e.createDownloadEntry(getClientContext(), type);
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
	
	
	private Set<String> listAlreadyDownloadedWithAlternatives() {
		Set<String> files = new TreeSet<String>();
		File externalStorageDirectory = settings.getExternalStorageDirectory();
		// files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.POI_PATH),POI_INDEX_EXT,POI_INDEX_EXT_ZIP,POI_TABLE_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.APP_DIR),BINARY_MAP_INDEX_EXT,BINARY_MAP_INDEX_EXT_ZIP,BINARY_MAP_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.APP_DIR),EXTRA_EXT, EXTRA_ZIP_EXT,0));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.BACKUP_PATH),BINARY_MAP_INDEX_EXT,BINARY_MAP_INDEX_EXT_ZIP,BINARY_MAP_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.VOICE_PATH),"",VOICE_INDEX_EXT_ZIP, VOICE_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.VOICE_PATH),"",TTSVOICE_INDEX_EXT_ZIP, TTSVOICE_VERSION));
		return files;
	}
	
	private Collection<? extends String> listWithAlternatives(File file, final String ext, final String downloadExt, final int version) {
		final List<String> files = new ArrayList<String>();
		if (file.isDirectory()) {
			file.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith(ext)) {
						files.add(filename);
						if (downloadExt != null) {
							files.add(filename.substring(0, filename.length() - ext.length()) + downloadExt);
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
	
	public class DownloadIndexesAsyncTask extends AsyncTask<String, Object, String> implements DownloadFileShowWarning {

		private IProgress progress;
		private OsmandPreference<Integer> downloads;

		public DownloadIndexesAsyncTask(ProgressDialogImplementation progressDialogImplementation) {
			this.progress = progressDialogImplementation;
			downloads = DownloadIndexActivity.this.getMyApplication().getSettings().NUMBER_OF_FREE_DOWNLOADS;
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			for (Object o : values) {
				if (o instanceof DownloadEntry) {
					String v = Version.getAppName(DownloadIndexActivity.this);
					if (Version.isProductionVersion(DownloadIndexActivity.this)) {
						v = Version.getFullVersion(DownloadIndexActivity.this);
					} else {
						v += " test";
					}

					new DownloadTracker().trackEvent(DownloadIndexActivity.this, v, Version.getAppName(DownloadIndexActivity.this),
							((DownloadEntry) o).baseName, 1, DownloadIndexActivity.this.getString(R.string.ga_api_key));
					DownloadIndexActivity.this.updateLoadedFiles();
					((DownloadIndexAdapter) getExpandableListAdapter()).notifyDataSetInvalidated();
					findViewById(R.id.DownloadButton).setVisibility(entriesToDownload.isEmpty() ? View.GONE : View.VISIBLE);
				} else if (o instanceof String) {
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
			if (result != null) {
				AccessibleToast.makeText(DownloadIndexActivity.this, result, Toast.LENGTH_LONG).show();
			}
			View mainView = DownloadIndexActivity.this.findViewById(R.id.MainLayout);
			if (mainView != null) {
				mainView.setKeepScreenOn(false);
			}
			DownloadIndexActivity.this.updateLoadedFiles();
			DownloadIndexAdapter adapter = ((DownloadIndexAdapter) getExpandableListAdapter());
			if (adapter != null) {
				adapter.notifyDataSetInvalidated();
			}
		}

		@Override
		protected String doInBackground(String... filesToDownload) {
			try {
				List<File> filesToReindex = new ArrayList<File>();
				boolean forceWifi = DownloadIndexActivity.this.downloadFileHelper.isWifiConnected();
				for (int i = 0; i < filesToDownload.length; i++) {
					String filename = filesToDownload[i];
					DownloadEntry entry = DownloadIndexActivity.this.entriesToDownload.get(filename);
					if (entry != null) {
						String indexOfAllFiles = filesToDownload.length <= 1 ? "" : (" [" + (i + 1) + "/" + filesToDownload.length + "]");
						boolean result = downloadFile(entry, filename, filesToReindex, indexOfAllFiles, forceWifi);
						if (result) {
							DownloadIndexActivity.this.entriesToDownload.remove(filename);
							downloads.set(downloads.get() + 1);
							if (entry.existingBackupFile != null) {
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
				if (DownloadIndexActivity.this.progressFileDlg != null) {
					removeDialog(DownloadIndexActivity.DIALOG_PROGRESS_FILE);
					DownloadIndexActivity.this.progressFileDlg = null;
				}
			}
			return null;
		}

		@Override
		public void showWarning(String warning) {
			publishProgress(warning);

		}

		public boolean downloadFile(DownloadEntry de, String filename, List<File> filesToReindex, String indexOfAllFiles, boolean forceWifi)
				throws InterruptedException {
			boolean res = false;
			if (de.isAsset) {
				try {
					ResourceManager.copyAssets(getAssets(), de.fileToSave.getPath(), de.fileToUnzip);
					de.fileToUnzip.setLastModified(de.dateModified);
					res = true;
				} catch (IOException e) {
					log.error("Copy exception", e);
				}
			} else {
				res = downloadFileHelper.downloadFile(filename, de, progress, filesToReindex, indexOfAllFiles, this, forceWifi);
			}
			if (res && de.attachedEntry != null) {
				return downloadFile(de.attachedEntry, filename, filesToReindex, indexOfAllFiles, forceWifi);
			}
			return res;
		}
	}
	
	public ClientContext getClientContext() {
		return getMyApplication().getClientContext();
	}
}
