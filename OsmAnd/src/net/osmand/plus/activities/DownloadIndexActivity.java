package net.osmand.plus.activities;

import static net.osmand.data.IndexConstants.BINARY_MAP_INDEX_EXT;
import static net.osmand.data.IndexConstants.BINARY_MAP_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.BINARY_MAP_VERSION;
import static net.osmand.data.IndexConstants.POI_INDEX_EXT;
import static net.osmand.data.IndexConstants.POI_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.POI_TABLE_VERSION;
import static net.osmand.data.IndexConstants.TTSVOICE_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.TTSVOICE_VERSION;
import static net.osmand.data.IndexConstants.VOICE_INDEX_EXT_ZIP;
import static net.osmand.data.IndexConstants.VOICE_VERSION;

import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.osmand.access.AccessibleToast;
import net.osmand.IProgress;
import net.osmand.data.IndexConstants;
import net.osmand.plus.DownloadOsmandIndexesHelper;
import net.osmand.plus.DownloadOsmandIndexesHelper.IndexItem;
import net.osmand.plus.IndexFileList;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.activities.DownloadFileHelper.DownloadFileShowWarning;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadIndexActivity extends ListActivity {
	
	/** menus **/
	private static final int RELOAD_ID = 0;
	private static final int SELECT_ALL_ID = 1;
	private static final int DESELECT_ALL_ID = 2;
	private static final int FILTER_EXISTING_REGIONS = 3;
	
	/** dialogs **/
	protected static final int DIALOG_MAP_VERSION_UPDATE = 0;
	protected static final int DIALOG_PROGRESS_FILE = 1;
	protected static final int DIALOG_PROGRESS_LIST = 2;
	
	/** other **/
	private static final int MB = 1 << 20;
    public static final String FILTER_KEY = "filter";
	
	private static DownloadIndexListThread downloadListIndexThread = new DownloadIndexListThread();

	private ProgressDialog progressFileDlg = null;
	private Map<String, String> indexFileNames = null;
	private TreeMap<String, DownloadEntry> entriesToDownload = new TreeMap<String, DownloadEntry>();
	 
	
    private TextWatcher textWatcher ;
	private EditText filterText;
	private DownloadFileHelper downloadFileHelper = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// recreation upon rotation is prevented in manifest file
		setContentView(R.layout.download_index);
		downloadFileHelper = new DownloadFileHelper(this);
		findViewById(R.id.DownloadButton).setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				downloadFilesConfirmation();
			}
			
		});
		
		indexFileNames = ((OsmandApplication)getApplication()).getResourceManager().getIndexFileNames();

	    filterText = (EditText) findViewById(R.id.search_box);
	    textWatcher = new TextWatcher() {
	        public void afterTextChanged(Editable s) {
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count,
	                int after) {
	        }

	        public void onTextChanged(CharSequence s, int start, int before,
	                int count) {
	        	DownloadIndexAdapter adapter = ((DownloadIndexAdapter)getListAdapter());
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

		if(downloadListIndexThread.getCachedIndexFiles() != null){
			setListAdapter(new DownloadIndexAdapter(downloadListIndexThread.getCachedIndexFiles()));
		} else {
			downloadIndexList();
		}
	}

	private void downloadIndexList() {
		showDialog(DIALOG_PROGRESS_LIST);
	}
	
	@Override
	
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.clear();
		menu.add(0, RELOAD_ID, 0, R.string.reload);
		if (getListAdapter() != null) {
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
			downloadListIndexThread = new DownloadIndexListThread();
			downloadIndexList();
		} else {
			final DownloadIndexAdapter listAdapter = (DownloadIndexAdapter)getListAdapter();
			if(item.getItemId() == SELECT_ALL_ID){
				int selected = 0;
				for (int i = 0; i < listAdapter.getCount(); i++) {
					IndexItem es = listAdapter.getItem(i);
					if(!entriesToDownload.containsKey(es.getFileName())){
						selected++;
						entriesToDownload.put(es.getFileName(), createDownloadEntry(es));
					}
				}
				AccessibleToast.makeText(this, MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
				listAdapter.notifyDataSetInvalidated();
				if(selected > 0){
					findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
				}
			} else if(item.getItemId() == FILTER_EXISTING_REGIONS){
				final Collection<String> listAlreadyDownloaded = listAlreadyDownloadedWithAlternatives();
				final List<IndexItem> filtered = new ArrayList<IndexItem>();
				for(String file : listAlreadyDownloaded) {
					IndexItem fileItem = listAdapter.getIndexFiles().get(file);
					if (fileItem != null) {
						filtered.add(fileItem);
					}
				}
				listAdapter.clear();
				for (IndexItem fileItem : filtered) {
					listAdapter.add(fileItem);
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
	
	private static class DownloadIndexListThread extends Thread {
		private DownloadIndexActivity uiActivity = null;
		private IndexFileList indexFiles = null; 
		
		public DownloadIndexListThread(){
			super("DownloadIndexes"); //$NON-NLS-1$
		}
		public void setUiActivity(DownloadIndexActivity uiActivity) {
			this.uiActivity = uiActivity;
		}
		
		public Map<String, IndexItem> getCachedIndexFiles() {
			return indexFiles != null ? indexFiles.getIndexFiles() : null;
		}
		
		@Override
		public void run() {
			indexFiles = DownloadOsmandIndexesHelper.downloadIndexesListFromInternet();
			if(uiActivity != null) {
				uiActivity.removeDialog(DIALOG_PROGRESS_LIST);
				uiActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (indexFiles != null) {
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
						ProgressDialog.STYLE_HORIZONTAL);
				progressFileDlg = progress.getDialog();
				progressFileDlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
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
				task.execute(entriesToDownload.keySet().toArray(new String[0]));
				break;
			case DIALOG_PROGRESS_LIST:
				downloadListIndexThread.setUiActivity(this);
				if(downloadListIndexThread.getState() == Thread.State.NEW){
					downloadListIndexThread.start();
				} else if(downloadListIndexThread.getState() == Thread.State.TERMINATED){
					// possibly exception occurred we don't have cache of files
					downloadListIndexThread = new DownloadIndexListThread();
					downloadListIndexThread.setUiActivity(this);
					downloadListIndexThread.start();
				}
				break;
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		final IndexItem e = ((DownloadIndexAdapter)getListAdapter()).getItem(position);
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
			return;
		}
		
		final DownloadEntry entry = createDownloadEntry(e);
		if (entry != null) {
			// if(!fileToUnzip.exists()){
			// builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, extractDateAndSize(e.getValue())));
			if (entry.fileToUnzip.exists()) {
				Builder builder = new AlertDialog.Builder(this);
				MessageFormat format;
				if (entry.fileToUnzip.isDirectory()) {
					format = new MessageFormat("{0,date,dd.MM.yyyy}", Locale.US); //$NON-NLS-1$
				} else {
					format = new MessageFormat("{0,date,dd.MM.yyyy}, {1, number,##.#} MB", Locale.US); //$NON-NLS-1$
				}
				String description = format.format(new Object[] { new Date(entry.fileToUnzip.lastModified()),
						((float) entry.fileToUnzip.length() / MB) });
				String descriptionEx = e.getDate() + ", " +e.getSize() + " MB"; 
				builder.setMessage(MessageFormat.format(getString(R.string.download_question_exist), entry.baseName, description,
						descriptionEx));

				builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						entriesToDownload.put(e.getFileName(), entry);
						int x = getListView().getScrollX();
						int y = getListView().getScrollY();
						findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
						getListView().scrollTo(x, y);
						ch.setChecked(!ch.isChecked());
					}
				});
				builder.setNegativeButton(R.string.default_buttons_no, null);
				builder.show();
			} else {
				entriesToDownload.put(e.getFileName(), entry);
				int x = getListView().getScrollX();
				int y = getListView().getScrollY();
				findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
				getListView().scrollTo(x, y);
				ch.setChecked(!ch.isChecked());
			}

		}
		
	}

	private Collection<String> listAlreadyDownloadedWithAlternatives() {
		Set<String> files = new TreeSet<String>();
		File externalStorageDirectory = OsmandSettings.getOsmandSettings(getApplicationContext()).getExternalStorageDirectory();
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.POI_PATH),POI_INDEX_EXT,POI_INDEX_EXT_ZIP,POI_TABLE_VERSION));
		files.addAll(listWithAlternatives(new File(externalStorageDirectory, ResourceManager.APP_DIR),BINARY_MAP_INDEX_EXT,BINARY_MAP_INDEX_EXT_ZIP,BINARY_MAP_VERSION));
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

	private DownloadEntry createDownloadEntry(IndexItem item) {
		String fileName = item.getFileName();
		File parent = null;
		String toSavePostfix = null;
		String toCheckPostfix = null;
		boolean unzipDir = false;
		
		File externalStorageDirectory = OsmandSettings.getOsmandSettings(getApplicationContext()).getExternalStorageDirectory();
		if(fileName.endsWith(IndexConstants.POI_INDEX_EXT)){
			parent = new File(externalStorageDirectory, ResourceManager.POI_PATH);
			toSavePostfix = POI_INDEX_EXT;
			toCheckPostfix = POI_INDEX_EXT;
		} else if(fileName.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
			parent = new File(externalStorageDirectory, ResourceManager.POI_PATH);
			toSavePostfix = POI_INDEX_EXT_ZIP;
			toCheckPostfix = POI_INDEX_EXT;
		} else if(fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT)){
			parent = new File(externalStorageDirectory, ResourceManager.APP_DIR);
			toSavePostfix = BINARY_MAP_INDEX_EXT;
			toCheckPostfix = BINARY_MAP_INDEX_EXT;
		} else if(fileName.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
			parent = new File(externalStorageDirectory, ResourceManager.APP_DIR);
			toSavePostfix = BINARY_MAP_INDEX_EXT_ZIP;
			toCheckPostfix = BINARY_MAP_INDEX_EXT;
		} else if(fileName.endsWith(IndexConstants.VOICE_INDEX_EXT_ZIP)){
			parent = new File(externalStorageDirectory, ResourceManager.VOICE_PATH);
			toSavePostfix = VOICE_INDEX_EXT_ZIP;
			toCheckPostfix = ""; //$NON-NLS-1$
			unzipDir = true;
		} else if(fileName.endsWith(IndexConstants.TTSVOICE_INDEX_EXT_ZIP)){
			parent = new File(externalStorageDirectory, ResourceManager.VOICE_PATH);
			toSavePostfix = TTSVOICE_INDEX_EXT_ZIP;
			toCheckPostfix = ""; //$NON-NLS-1$
			unzipDir = true;
		}
		if(parent != null){
			parent.mkdirs();
		}
		final DownloadEntry entry;
		if(parent == null || !parent.exists()){
			AccessibleToast.makeText(DownloadIndexActivity.this, getString(R.string.sd_dir_not_accessible), Toast.LENGTH_LONG).show();
			entry = null;
		} else {
			entry = new DownloadEntry();
			int ls = fileName.lastIndexOf('_');
			entry.baseName = fileName.substring(0, ls);
			entry.fileToSave = new File(parent, entry.baseName + toSavePostfix);
			entry.unzip = unzipDir;
			SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy"); //$NON-NLS-1$
			try {
				Date d = format.parse(item.getDate());
				entry.dateModified = d.getTime();
			} catch (ParseException e1) {
			}
			try {
				entry.sizeMB = Double.parseDouble(item.getSize());
			} catch (NumberFormatException e1) {
			}
			entry.parts = 1;
			if(item.getParts() != null){
				entry.parts = Integer.parseInt(item.getParts());
			}
			entry.fileToUnzip = new File(parent, entry.baseName + toCheckPostfix);
		}
		return entry;
	}
	
	protected void downloadFilesConfirmation() {
		Builder builder = new AlertDialog.Builder(this);
		double sz = 0;
		for(DownloadEntry es : entriesToDownload.values()){
			sz += es.sizeMB;
		}
		builder.setMessage(MessageFormat.format(getString(R.string.download_files_question), entriesToDownload.size(), sz));
		builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				showDialog(DIALOG_PROGRESS_FILE);
			}
		});
		builder.setNegativeButton(R.string.default_buttons_no, null);
		builder.show();
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
		if(name.endsWith(IndexConstants.POI_INDEX_EXT) || name.endsWith(IndexConstants.POI_INDEX_EXT_ZIP)){
			s = IndexConstants.POI_INDEX_EXT;
		} else if(name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT) || name.endsWith(IndexConstants.BINARY_MAP_INDEX_EXT_ZIP)){
			s = IndexConstants.BINARY_MAP_INDEX_EXT;
		} else {
			s = ""; //$NON-NLS-1$
		}
		return name.substring(0, l) + s;
	}
	
private class DownloadIndexesAsyncTask extends  AsyncTask<String, Object, String> implements DownloadFileShowWarning {
		
		private IProgress progress;

		public DownloadIndexesAsyncTask(ProgressDialogImplementation progressDialogImplementation) {
			this.progress = progressDialogImplementation;
		}

		@Override
		protected void onProgressUpdate(Object... values) {
			for(Object o : values){
				if(o instanceof DownloadEntry){
					((DownloadIndexAdapter) getListAdapter()).notifyDataSetChanged();
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
			if(mainView != null){
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
		}
		
		@Override
		protected String doInBackground(String... filesToDownload) {
			try {
				List<File> filesToReindex = new ArrayList<File>();
				
				for (int i = 0; i < filesToDownload.length; i++) {
					String filename = filesToDownload[i];
					DownloadEntry entry = entriesToDownload.get(filename);
					if (entry != null) {
						String indexOfAllFiles = filesToDownload.length <= 1 ? "" : (" [" + (i + 1) + "/"
								+ filesToDownload.length + "]");
						boolean result = downloadFileHelper.downloadFile(filename, 
								entry.fileToSave, entry.fileToUnzip, entry.unzip, progress, entry.dateModified,
								entry.parts, filesToReindex, indexOfAllFiles, this);
						if (result) {
							entriesToDownload.remove(filename);
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
				if (vectorMapsToReindex) {
					ResourceManager manager = ((OsmandApplication) getApplication()).getResourceManager();
					List<String> warnings = manager.indexingMaps(progress);
					if (warnings.isEmpty() && !OsmandSettings.getOsmandSettings(getApplicationContext()).MAP_VECTOR_DATA.get()) {
						warnings.add(getString(R.string.binary_map_download_success));
						// Is it proper way to switch every tome to vector data?
						OsmandSettings.getOsmandSettings(getApplicationContext()).MAP_VECTOR_DATA.set(true);
					}
					if (!warnings.isEmpty()) {
						return warnings.get(0);
					}
				}
			} catch (InterruptedException e) {
				// do not dismiss dialog
				progressFileDlg = null;
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
	
	private static class DownloadEntry {
		public File fileToSave;
		public File fileToUnzip;
		public boolean unzip;
		public Long dateModified;
		public double sizeMB;
		public String baseName;
		public int parts;
	}

	protected class DownloadIndexAdapter extends ArrayAdapter<IndexItem> implements Filterable {
		
		private DownloadIndexFilter myFilter;
		private final Map<String, IndexItem> indexFiles;

		public DownloadIndexAdapter(Map<String, IndexItem> indexFiles) {
			super(DownloadIndexActivity.this, net.osmand.plus.R.layout.download_index_list_item);
			this.indexFiles = new LinkedHashMap<String, IndexItem>(indexFiles);
			for (Entry<String, IndexItem> entry : indexFiles.entrySet()) {
				add(entry.getValue());
			}
			getFilter().filter(filterText.getText());
		}

		public Map<String, IndexItem> getIndexFiles() {
			return indexFiles;
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = getLayoutInflater();
				v = inflater.inflate(net.osmand.plus.R.layout.download_index_list_item, parent, false);
			}
			final View row = v; 
			TextView item = (TextView) row.findViewById(R.id.download_item);
			TextView description = (TextView) row.findViewById(R.id.download_descr);
			IndexItem e = getItem(position);
			item.setText(e.getVisibleDescription(DownloadIndexActivity.this) + "\n" + e.getVisibleName()); //$NON-NLS-1$
			description.setText(e.getDate() + "\n" + e.getSize() + " MB");
			
			CheckBox ch = (CheckBox) row.findViewById(R.id.check_download_item);
			ch.setChecked(entriesToDownload.containsKey(e.getFileName()));
			ch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
					ch.setChecked(!ch.isChecked());
					DownloadIndexActivity.this.onListItemClick(getListView(), row, position, getItemId(position));
				}
			});
			
			if(indexFileNames != null){
				String sfName = convertServerFileNameToLocal(e.getFileName());
				if(!indexFileNames.containsKey(sfName)){
					item.setTextColor(Color.WHITE);
				} else {
					if(e.getDate() != null){
						if(e.getDate().equals(indexFileNames.get(sfName))){
							item.setTextColor(Color.GREEN);
						} else {
							item.setTextColor(Color.BLUE);
						}
					} else {
						item.setTextColor(Color.GREEN);
					}
				}
			}
			return row;
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
					CharSequence lowerCase = constraint.toString()
							.toLowerCase();
					List<IndexItem> filter = new ArrayList<IndexItem>();
					for (IndexItem item : indexFiles.values()) {
						if (item.getVisibleName().toLowerCase().contains(lowerCase)) {
							filter.add(item);
						} else if (item.getDescription().toLowerCase().contains(lowerCase)) {
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
				clear();
				for (IndexItem item : (Collection<IndexItem>) results.values) {
					add(item);
				}
			}
		}
	}
	

}
