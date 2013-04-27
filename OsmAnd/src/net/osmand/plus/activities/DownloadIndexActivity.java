package net.osmand.plus.activities;


import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibleToast;
import net.osmand.map.RegionCountry;
import net.osmand.map.RegionRegistry;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadEntry;
import net.osmand.plus.download.DownloadFileHelper;
import net.osmand.plus.download.DownloadFileHelper.DownloadFileShowWarning;
import net.osmand.plus.download.DownloadIndexAdapter;
import net.osmand.plus.download.DownloadIndexListThread;
import net.osmand.plus.download.DownloadTracker;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.IndexItemCategory;
import net.osmand.plus.download.SrtmIndexItem;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.util.Algorithms;
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
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadIndexActivity extends OsmandExpandableListActivity {
	
	private static final org.apache.commons.logging.Log log = PlatformUtil.getLog(DownloadIndexActivity.class);
	
	/** menus **/
	private static final int RELOAD_ID = 0;
	private static final int SELECT_ALL_ID = 1;
	private static final int DESELECT_ALL_ID = 2;
	private static final int FILTER_EXISTING_REGIONS = 3;
	
	/** dialogs **/
	public static final int DIALOG_MAP_VERSION_UPDATE = 0;
	public static final int DIALOG_PROGRESS_FILE = 1;
	public static final int DIALOG_PROGRESS_LIST = 2;
	
    public static final String FILTER_KEY = "filter";
	
	private static DownloadIndexListThread downloadListIndexThread;

	private ProgressDialog progressFileDlg = null;
	
	private TreeMap<IndexItem, List<DownloadEntry>> entriesToDownload = new TreeMap<IndexItem, List<DownloadEntry>>();
	private DownloadActivityType type = DownloadActivityType.NORMAL_FILE;

	private int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 10;
	 
	
    private TextWatcher textWatcher ;
	private EditText filterText;
	private DownloadFileHelper downloadFileHelper = null;
	private OsmandSettings settings;
	private ArrayAdapter<String> spinnerAdapter;

	private List<SrtmIndexItem> cachedSRTMFiles;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = ((OsmandApplication) getApplication()).getSettings();
		if(downloadListIndexThread == null) {
			downloadListIndexThread = new DownloadIndexListThread(this);
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.download_index);
		// recreation upon rotation is prevented in manifest file
		View b = findViewById(R.id.search_back_button);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();				
			}
		});
				
		downloadFileHelper = new DownloadFileHelper(getClientContext());
		findViewById(R.id.DownloadButton).setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				downloadFilesCheckFreeVersion(flattenDownloadEntries());
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
		if(Version.isFreeVersion(getMyApplication()) && settings.checkFreeDownloadsNumberZero()){
			Builder msg = new AlertDialog.Builder(this);
			msg.setTitle(R.string.free_version_title);
			msg.setMessage(getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS+"", ""));
			msg.setPositiveButton(R.string.default_buttons_ok, null);
			msg.show();
		}
		
		Spinner spinner = (Spinner) findViewById(R.id.SpinnerLocation);
		final DownloadActivityType[] downloadTypes = getDownloadTypes();
		spinnerAdapter = new ArrayAdapter<String>(this, R.layout.my_spinner_text, 
				new ArrayList<String>(Arrays.asList(toString(downloadTypes)))
				) {
			@Override
			public View getDropDownView(int position, View convertView,
					ViewGroup parent) {
				View dropDownView = super.getDropDownView(position,
						convertView, parent);
				if (dropDownView instanceof TextView) {
					((TextView) dropDownView).setTextColor(getResources()
							.getColor(R.color.color_black));
				}
				return dropDownView;
			}
		};
		spinnerAdapter.setDropDownViewResource(R.layout.my_spinner_text);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		
		spinner.setAdapter(spinnerAdapter);
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				changeType(downloadTypes[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}


	public void updateLoadedFiles() {
		if (type == DownloadActivityType.SRTM_FILE) {
			if (cachedSRTMFiles != null) {
				for (SrtmIndexItem i : cachedSRTMFiles) {
					((SrtmIndexItem) i).updateExistingTiles(getMyApplication().getResourceManager().getIndexFileNames());
				}
			}
			((DownloadIndexAdapter) getExpandableListAdapter()).notifyDataSetInvalidated();
		}
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
			menu.add(0, SELECT_ALL_ID, 0, R.string.select_all);
			menu.add(0, DESELECT_ALL_ID, 0, R.string.deselect_all);
			menu.add(0, FILTER_EXISTING_REGIONS, 0, R.string.filter_existing_indexes);
		}
		return true;
	}
	
	public DownloadActivityType getType() {
		return type;
	}
	
	public List<DownloadEntry> flattenDownloadEntries() {
		List<DownloadEntry> res = new ArrayList<DownloadEntry>();
		for(List<DownloadEntry> ens : entriesToDownload.values()) {
			if(ens != null) {
				res.addAll(ens);
			}
		}
		return res;
	}
	
	public TreeMap<IndexItem, List<DownloadEntry>> getEntriesToDownload() {
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
						if (!entriesToDownload.containsKey(es)) {
							selected++;
							entriesToDownload.put(es, es.createDownloadEntry(getClientContext(), type, new ArrayList<DownloadEntry>(1)));
						}
					}
				}
				AccessibleToast.makeText(this, MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
				listAdapter.notifyDataSetInvalidated();
				if(selected > 0){
					findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
				}
			} else if (item.getItemId() == FILTER_EXISTING_REGIONS) {
				final Map<String, String> listAlreadyDownloaded = listAlreadyDownloadedWithAlternatives();
				final List<IndexItem> filtered = new ArrayList<IndexItem>();
				for (IndexItem fileItem : listAdapter.getIndexFiles()) {
					if(fileItem.isAlreadyDownloaded(listAlreadyDownloaded)){
						filtered.add(fileItem);
					}
				}
				listAdapter.setIndexFiles(filtered, IndexItemCategory.categorizeIndexItems(getClientContext(), filtered));
				listAdapter.notifyDataSetChanged();
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


	public void selectDownloadType() {
		Builder bld = new AlertDialog.Builder(this);
		final DownloadActivityType[] items = getDownloadTypes();
		bld.setItems(toString(items), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				changeType(items[which]);
			}
		});
		bld.show();
	}

	private String[] toString(DownloadActivityType[] t) {
		String[] items = new String[t.length];
		for (int i = 0; i < t.length; i++) {
			if (t[i] == DownloadActivityType.NORMAL_FILE) {
				items[i] = getString(R.string.download_regular_maps);
			} else if (t[i] == DownloadActivityType.ROADS_FILE) {
				items[i] = getString(R.string.download_roads_only_maps);
			} else if (t[i] == DownloadActivityType.SRTM_FILE) {
				items[i] = getString(R.string.download_srtm_maps);
			} else if (t[i] == DownloadActivityType.HILLSHADE_FILE) {
				items[i] = getString(R.string.download_hillshade_maps);
			}
		}
		return items;
	}

	private DownloadActivityType[] getDownloadTypes() {
		DownloadActivityType[] items;
		if(OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null){
			items = new DownloadActivityType[]{
					DownloadActivityType.NORMAL_FILE,
					DownloadActivityType.ROADS_FILE,
					DownloadActivityType.SRTM_FILE,
					DownloadActivityType.HILLSHADE_FILE};
		} else {
			items = new DownloadActivityType[]{
					DownloadActivityType.NORMAL_FILE,
					DownloadActivityType.ROADS_FILE,
					};
		}
		return items;
	}


	public void changeType(final DownloadActivityType tp) {
		if (downloadListIndexThread != null && type != tp) {
			type = tp;
			AsyncTask<Void, Void, List<IndexItem>> t = new AsyncTask<Void, Void, List<IndexItem>>(){
				
				private List<IndexItemCategory> cats;
				private ProgressDialog progressDialog;

				@Override
				protected void onPreExecute() {
					super.onPreExecute();
					progressDialog = ProgressDialog.show(DownloadIndexActivity.this,
							getString(R.string.downloading), getString(R.string.downloading_list_indexes));
				}
				@Override
				protected List<IndexItem> doInBackground(Void... params) {
					final List<IndexItem> filtered = getFilteredByType();
					cats = IndexItemCategory.categorizeIndexItems(getClientContext(), filtered);
					return filtered;
				}
				
				@Override
				protected void onPostExecute(List<IndexItem> filtered) {
					entriesToDownload.clear();
					DownloadIndexAdapter a = ((DownloadIndexAdapter) getExpandableListAdapter());
					// Strange null pointer fix (reproduce?)
					if (a != null) {
						a.setIndexFiles(filtered, cats);
						a.notifyDataSetChanged();
						a.getFilter().filter(filterText.getText());
					}
					progressDialog.dismiss();					
				}
				
			};
			t.execute();
		}
	}


	public List<IndexItem> getFilteredByType() {
		final List<IndexItem> filtered = new ArrayList<IndexItem>();
		if (type == DownloadActivityType.SRTM_FILE) {
			Map<String, String> indexFileNames = getMyApplication().getResourceManager().getIndexFileNames();
			if (cachedSRTMFiles == null) {
				cachedSRTMFiles = new ArrayList<SrtmIndexItem>();
				List<RegionCountry> countries = RegionRegistry.getRegionRegistry().getCountries();
				for (RegionCountry rc : countries) {
					if (rc.tiles.size() > 35 && rc.getSubRegions().size() > 0) {
						for (RegionCountry ch : rc.getSubRegions()) {
							cachedSRTMFiles.add(new SrtmIndexItem(ch, indexFileNames));
						}
					} else {
						cachedSRTMFiles.add(new SrtmIndexItem(rc, indexFileNames));
					}
				}
				filtered.addAll(cachedSRTMFiles);
			} else {
				for (SrtmIndexItem s : cachedSRTMFiles) {
					s.updateExistingTiles(indexFileNames);
					filtered.add(s);
				}
			}
		}
		List<IndexItem> cachedIndexFiles = downloadListIndexThread.getCachedIndexFiles();
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
				IndexItem[] indexes = entriesToDownload.keySet().toArray(new IndexItem[0]);
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
		final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
		
		if(ch.isChecked()){
			ch.setChecked(!ch.isChecked());
			entriesToDownload.remove(e);
			if(entriesToDownload.isEmpty()){
				int x = getListView().getScrollX();
				int y = getListView().getScrollY();
				findViewById(R.id.DownloadButton).setVisibility(View.GONE);
				getListView().scrollTo(x, y);
			}
			return true;
		}
		
		List<DownloadEntry> download = e.createDownloadEntry(getClientContext(), type, new ArrayList<DownloadEntry>());
		if (download.size() > 0) {
			// if(!fileToUnzip.exists()){
			// builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, extractDateAndSize(e.getValue())));
			entriesToDownload.put(e, download);
			int x = getListView().getScrollX();
			int y = getListView().getScrollY();
			findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
			getListView().scrollTo(x, y);
			ch.setChecked(!ch.isChecked());
		}
		return true;
	}
	
	
	private Map<String, String> listAlreadyDownloadedWithAlternatives() {
		Map<String, String> files = new TreeMap<String, String>();
		listWithAlternatives(getMyApplication().getAppPath(IndexConstants.BACKUP_INDEX_DIR),IndexConstants.BINARY_MAP_INDEX_EXT, files);
		listWithAlternatives(getMyApplication().getAppPath(IndexConstants.MAPS_PATH),IndexConstants.BINARY_MAP_INDEX_EXT, files);
		listWithAlternatives(getMyApplication().getAppPath(IndexConstants.MAPS_PATH),IndexConstants.EXTRA_EXT, files);
		if(OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null) {
			listWithAlternatives(getMyApplication().getAppPath(IndexConstants.SRTM_INDEX_DIR),IndexConstants.BINARY_MAP_INDEX_EXT, files);
		}
		listWithAlternatives(getMyApplication().getAppPath(IndexConstants.VOICE_INDEX_DIR),"", files);
		return files;
	}
	
	public static Map<String, String> listWithAlternatives(File file, final String ext, 
			final Map<String, String> files) {
		if (file.isDirectory()) {
			file.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					if (filename.endsWith(ext)) {
						String date = Algorithms.formatDate(new File(dir, filename).lastModified());
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

	protected void downloadFilesCheckFreeVersion(List<DownloadEntry> list) {
		if (Version.isFreeVersion(getMyApplication()) ) {
			int total = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			boolean wiki = false;
			for (DownloadEntry es : list) {
				if (es.baseName != null && es.baseName.contains("_wiki")) {
					wiki = true;
					break;
				} else if (es.type != DownloadActivityType.SRTM_FILE) {
					total++;
				}
			}
			String msgTx = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "", "( =" + total + ") ");
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS || wiki) {
				Builder msg = new AlertDialog.Builder(this);
				msg.setTitle(R.string.free_version_title);
				msg.setMessage(msgTx);
				msg.setPositiveButton(R.string.default_buttons_ok, null);
				msg.show();
			} else {
				AccessibleToast.makeText(this, msgTx, Toast.LENGTH_LONG).show();
				downloadFilesPreCheckSRTM( list);
			}
		} else {
			downloadFilesPreCheckSRTM( list);
		}
	}
	
	protected void downloadFilesPreCheckSRTM(final List<DownloadEntry> list) {
		if (type == DownloadActivityType.SRTM_FILE && 
				OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) instanceof SRTMPlugin && 
				!OsmandPlugin.getEnabledPlugin(SRTMPlugin.class).isPaid()) {
			Builder msg = new AlertDialog.Builder(this);
			msg.setTitle(R.string.srtm_paid_version_title);
			msg.setMessage(getString(R.string.srtm_paid_version_msg));
			msg.setPositiveButton(R.string.default_buttons_ok, new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadFilesPreCheckSpace(list);
				}
			});
			msg.show();
		} else {
			downloadFilesPreCheckSpace(list);
		}
	}
	
	protected void downloadFilesPreCheckSpace(List<DownloadEntry> list) {
		double sz = 0;
		for(DownloadEntry es : list){
			sz += es.sizeMB;
		}
		// get availabile space 
		File dir = getMyApplication().getAppPath("").getParentFile();
		double asz = -1;
		if(dir.canRead()){
			StatFs fs = new StatFs(dir.getAbsolutePath());
			asz = (((long) fs.getAvailableBlocks()) * fs.getBlockSize()) / (1 << 20);
		}
		if(asz != -1 && asz < sz * 2 ){
			AccessibleToast.makeText(this, getString(R.string.download_files_not_enough_space, sz, asz), Toast.LENGTH_LONG).show();
		} else {
			Builder builder = new AlertDialog.Builder(this);
			if (asz > 0 && sz/asz > 0.4) {
				builder.setMessage(MessageFormat.format(getString(R.string.download_files_question_space), list.size(), sz,
						asz));
			} else {
				builder.setMessage(MessageFormat.format(getString(R.string.download_files_question), list.size(), sz));
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
	
	public class DownloadIndexesAsyncTask extends AsyncTask<IndexItem, Object, String> implements DownloadFileShowWarning {

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
		
		private int countAllDownloadEntry(IndexItem... filesToDownload){
			int t = 0;
			for(IndexItem  i : filesToDownload){
				List<DownloadEntry> list = DownloadIndexActivity.this.entriesToDownload.get(i);
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
				boolean forceWifi = DownloadIndexActivity.this.downloadFileHelper.isWifiConnected();
				int counter = 1;
				int all = countAllDownloadEntry(filesToDownload);
				for (int i = 0; i < filesToDownload.length; i++) {
					IndexItem filename = filesToDownload[i];
					List<DownloadEntry> list = DownloadIndexActivity.this.entriesToDownload.get(filename);
					if (list != null) {
						for (DownloadEntry entry : list) {
							String indexOfAllFiles = all <= 1 ? "" : (" [" + counter + "/" + all + "]");
							counter++;
							boolean result = downloadFile(entry, filesToReindex, indexOfAllFiles, forceWifi);
							if (result) {
								DownloadIndexActivity.this.entriesToDownload.remove(filename);
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

		private void trackEvent(DownloadEntry entry) {
			String v = Version.getAppName(getMyApplication());
			if (Version.isProductionVersion(getMyApplication())) {
				v = Version.getFullVersion(getMyApplication());
			} else {
				v += " test";
			}
			new DownloadTracker().trackEvent(getMyApplication(), v, Version.getAppName(getMyApplication()),
					entry.baseName, 1, DownloadIndexActivity.this.getString(R.string.ga_api_key));
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
					ResourceManager.copyAssets(getAssets(), de.fileToSave.getPath(), de.fileToUnzip);
					de.fileToUnzip.setLastModified(de.dateModified);
					res = true;
				} catch (IOException e) {
					log.error("Copy exception", e);
				}
			} else {
				res = downloadFileHelper.downloadFile(de, progress, filesToReindex, indexOfAllFiles, this, forceWifi);
			}
			if (res && de.attachedEntry != null) {
				return downloadFile(de.attachedEntry, filesToReindex, indexOfAllFiles, forceWifi);
			}
			return res;
		}
	}
	
	public ClientContext getClientContext() {
		return getMyApplication();
	}
}
