package net.osmand.plus.activities;


import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.map.RegionCountry;
import net.osmand.map.RegionRegistry;
import net.osmand.plus.ClientContext;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.base.SuggestExternalDirectoryDialog;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadEntry;
import net.osmand.plus.download.DownloadIndexAdapter;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.IndexItemCategory;
import net.osmand.plus.download.SrtmIndexItem;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import net.osmand.util.Algorithms;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.StatFs;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public class DownloadIndexActivity extends OsmandExpandableListActivity {
	
	/** menus **/
	private static final int MORE_ID = 10;
	private static final int RELOAD_ID = 0;
	private static final int SELECT_ALL_ID = 1;
	private static final int DESELECT_ALL_ID = 2;
	private static final int FILTER_EXISTING_REGIONS = 3;
	
    public static final String FILTER_KEY = "filter";
	
	private static DownloadIndexesThread downloadListIndexThread;

	private TreeMap<IndexItem, List<DownloadEntry>> entriesToDownload = new TreeMap<IndexItem, List<DownloadEntry>>();
	private DownloadActivityType type = DownloadActivityType.NORMAL_FILE;

	private int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 10;
	 
	
    private TextWatcher textWatcher ;
	private EditText filterText;
	
	private OsmandSettings settings;
	private ArrayAdapter<String> spinnerAdapter;

	private List<SrtmIndexItem> cachedSRTMFiles;


	private View progressView;
	private ProgressBar indeterminateProgressBar;
	private ProgressBar determinateProgressBar;
	private TextView progressMessage;
	private TextView progressPercent;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = ((OsmandApplication) getApplication()).getSettings();
		if(downloadListIndexThread == null) {
			downloadListIndexThread = new DownloadIndexesThread(this);
		}
		setContentView(R.layout.download_index);
		indeterminateProgressBar = (ProgressBar) findViewById(R.id.IndeterminateProgressBar);
		determinateProgressBar = (ProgressBar) findViewById(R.id.DeterminateProgressBar);
		progressView = findViewById(R.id.ProgressView);
		progressMessage = (TextView) findViewById(R.id.ProgressMessage);
		progressPercent = (TextView) findViewById(R.id.ProgressPercent);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		getSupportActionBar().setTitle(R.string.local_index_download);
		// recreation upon rotation is prevented in manifest file
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
		List<IndexItem> list = new ArrayList<IndexItem>();
		downloadListIndexThread.setUiActivity(this);
		if (downloadListIndexThread.getCachedIndexFiles() != null && downloadListIndexThread.isDownloadedFromInternet()) {
			list = getFilteredByType();
		} else {
			downloadListIndexThread.runReloadIndexFiles();
		}
		DownloadIndexAdapter adapter = new DownloadIndexAdapter(this, list);
		setListAdapter(adapter);
		if(getMyApplication().getResourceManager().getIndexFileNames().isEmpty()) {
			boolean showedDialog = SuggestExternalDirectoryDialog.showDialog(this, null, null);
			if(!showedDialog) {
				showDialogOfFreeDownloadsIfNeeded();
			}
		} else {
			showDialogOfFreeDownloadsIfNeeded();
		}
		final DownloadActivityType[] downloadTypes = getDownloadTypes();
		spinnerAdapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(), R.layout.sherlock_spinner_item, 
				new ArrayList<String>(Arrays.asList(toString(downloadTypes)))	
				);
		spinnerAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, new OnNavigationListener() {
			
			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				changeType(downloadTypes[itemPosition]);
				return true;
			}
		});
        
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateProgress(false, downloadListIndexThread.getCurrentRunningTask());
	}


	private void showDialogOfFreeDownloadsIfNeeded() {
		if (Version.isFreeVersion(getMyApplication())) {
			Builder msg = new AlertDialog.Builder(this);
			msg.setTitle(R.string.free_version_title);
			String m = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "", "") + "\n";
			m += getString(R.string.available_downloads_left, MAXIMUM_AVAILABLE_FREE_DOWNLOADS - settings.NUMBER_OF_FREE_DOWNLOADS.get());
			msg.setMessage(m);
			if (Version.isGooglePlayEnabled(getMyApplication())) {
				msg.setNeutralButton(R.string.install_paid, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:net.osmand.plus"));
						try {
							startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
			}
			msg.setPositiveButton(R.string.default_buttons_ok, null);
			msg.show();
		}
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
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == RELOAD_ID) {
			// re-create the thread
			downloadListIndexThread.runReloadIndexFiles();
			return true;
		} else if (item.getItemId() == SELECT_ALL_ID) {
			selectAll();
			return true;
		} else if (item.getItemId() == FILTER_EXISTING_REGIONS) {
			filterExisting();
			return true;
		} else if (item.getItemId() == DESELECT_ALL_ID) {
			deselectAll();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		SubMenu s = menu.addSubMenu(0, MORE_ID, 0, R.string.default_buttons_other_actions);
		s.add(0, RELOAD_ID, 0, R.string.update_downlod_list);
		s.add(0, FILTER_EXISTING_REGIONS, 0, R.string.filter_existing_indexes);
		s.add(0, SELECT_ALL_ID, 0, R.string.select_all);
		s.add(0, DESELECT_ALL_ID, 0, R.string.deselect_all);
		
		s.setIcon(isLightActionBar() ? R.drawable.abs__ic_menu_moreoverflow_holo_light : R.drawable.abs__ic_menu_moreoverflow_holo_dark);
        s.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
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



	private void deselectAll() {
		final DownloadIndexAdapter listAdapter = (DownloadIndexAdapter)getExpandableListAdapter();
		entriesToDownload.clear();
		listAdapter.notifyDataSetInvalidated();
		findViewById(R.id.DownloadButton).setVisibility(View.GONE);
	}


	private void filterExisting() {
		final DownloadIndexAdapter listAdapter = (DownloadIndexAdapter)getExpandableListAdapter();
		final Map<String, String> listAlreadyDownloaded = listAlreadyDownloadedWithAlternatives();
		final List<IndexItem> filtered = new ArrayList<IndexItem>();
		for (IndexItem fileItem : listAdapter.getIndexFiles()) {
			if(fileItem.isAlreadyDownloaded(listAlreadyDownloaded)){
				filtered.add(fileItem);
			}
		}
		listAdapter.setIndexFiles(filtered, IndexItemCategory.categorizeIndexItems(getClientContext(), filtered));
		listAdapter.notifyDataSetChanged();
	}


	private void selectAll() {
		final DownloadIndexAdapter listAdapter = (DownloadIndexAdapter)getExpandableListAdapter();
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
			makeDownloadVisible();
		}
	}


	public void makeDownloadVisible() {
		findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
		if(Version.isFreeVersion(getMyApplication())) {
			int left = MAXIMUM_AVAILABLE_FREE_DOWNLOADS - settings.NUMBER_OF_FREE_DOWNLOADS.get() - entriesToDownload.size();
			boolean excessLimit = left < 0;
			if(left < 0) left = 0;
			String t = getString(R.string.download_files);
			if (getType() != DownloadActivityType.HILLSHADE_FILE || getType() != DownloadActivityType.SRTM_FILE) {
				t += " (" +(excessLimit?"! ":"") + getString(R.string.files_limit, left).toLowerCase() + ")";
			}
			((Button) findViewById(R.id.DownloadButton)).setText(t);
		}
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
	
	
	
	
	public ExpandableListView getListView() {
		return super.getExpandableListView();
	}
	
	public ExpandableListAdapter getListAdapter() {
		return super.getExpandableListAdapter();
	}
	
	// TODO 
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
			} else {
				makeDownloadVisible();
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
			makeDownloadVisible();
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
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS || wiki) {
				String msgTx = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "", "(" + total + ") ");
				Builder msg = new AlertDialog.Builder(this);
				msg.setTitle(R.string.free_version_title);
				msg.setMessage(msgTx);
				msg.setPositiveButton(R.string.default_buttons_ok, null);
				msg.show();
			} else {
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
					downloadFilesCheckInternet(list);
				}
			});
			msg.show();
		} else {
			downloadFilesCheckInternet(list);
		}
	}
	
	protected void downloadFilesCheckInternet(final List<DownloadEntry> list) {
		if(!getMyApplication().getExternalServiceAPI().isWifiConnected()) {
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.download_using_mobile_internet));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadFilesPreCheckSpace(list);
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
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
		if(asz != -1 && asz < sz  ){
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
					downloadListIndexThread.runDownloadFiles(entriesToDownload);
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
		}
	}
	
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
	    if (textWatcher != null) {
	    	EditText filterText = (EditText) findViewById(R.id.search_box);
	    	filterText.removeTextChangedListener(textWatcher);
	    }
		downloadListIndexThread.setUiActivity(null);
	}
	
	
	
	public ClientContext getClientContext() {
		return getMyApplication();
	}


	public void updateProgress(boolean updateOnlyProgress, BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask) {
		if(updateOnlyProgress) {
			if(!basicProgressAsyncTask.isIndeterminate()) {
				progressPercent.setText(basicProgressAsyncTask.getProgressPercentage() +"%");
				determinateProgressBar.setProgress(basicProgressAsyncTask.getProgressPercentage());
			}
		} else {
			boolean visible = basicProgressAsyncTask != null && basicProgressAsyncTask.getStatus() != Status.FINISHED;
			progressView.setVisibility(visible ? View.VISIBLE : View.GONE);
			if (visible) {
				boolean indeterminate = basicProgressAsyncTask.isIndeterminate();
				indeterminateProgressBar.setVisibility(!indeterminate ? View.GONE : View.VISIBLE);
				determinateProgressBar.setVisibility(indeterminate ? View.GONE : View.VISIBLE);
				progressPercent.setVisibility(indeterminate ? View.GONE : View.VISIBLE);

				progressMessage.setText(basicProgressAsyncTask.getDescription());
				if (!indeterminate) {
					progressPercent.setText(basicProgressAsyncTask.getProgressPercentage() + "%");
					determinateProgressBar.setProgress(basicProgressAsyncTask.getProgressPercentage());
				}
			}
		}
	}
}
