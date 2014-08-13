package net.osmand.plus.activities;


import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.osmand.IndexConstants;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.SettingsGeneralActivity.MoveFilesToDifferentDirectory;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.base.SuggestExternalDirectoryDialog;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadEntry;
import net.osmand.plus.download.DownloadIndexAdapter;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.IndexItemCategory;
import net.osmand.plus.srtmplugin.SRTMPlugin;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
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
    public static final String FILTER_CAT = "filter_cat";
	
	private static DownloadIndexesThread downloadListIndexThread;
	private DownloadActivityType type = DownloadActivityType.NORMAL_FILE;
	public static final int MAXIMUM_AVAILABLE_FREE_DOWNLOADS = 10;
	 
	
    private TextWatcher textWatcher ;
	private EditText filterText;
	private OsmandSettings settings;

	private View progressView;
	private ProgressBar indeterminateProgressBar;
	private ProgressBar determinateProgressBar;
	private TextView progressMessage;
	private TextView progressPercent;
	private ImageView cancel;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final List<DownloadActivityType> downloadTypes = getDownloadTypes();
		type = downloadTypes.get(0);
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
		cancel = (ImageView) findViewById(R.id.Cancel);
		int d = settings.isLightContent() ? R.drawable.a_1_navigation_cancel_small_light : R.drawable.a_1_navigation_cancel_small_dark;
		cancel.setImageDrawable(getResources().getDrawable(d));
		cancel.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				makeSureUserCancelDownload();
			}
		});
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		getSupportActionBar().setTitle(R.string.local_index_download);
		// recreation upon rotation is prevented in manifest file
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
			final String filterCat = intent.getExtras().getString(FILTER_CAT);
			if (filterCat != null) {
				DownloadActivityType type = DownloadActivityType.getIndexType(filterCat.toLowerCase());
				if (type != null) {
					this.type = type;
					downloadTypes.remove(type);
					downloadTypes.add(0, type);
				}
			}
		}
		List<IndexItem> list = new ArrayList<IndexItem>();
		downloadListIndexThread.setUiActivity(this);
		if (downloadListIndexThread.getCachedIndexFiles() != null && downloadListIndexThread.isDownloadedFromInternet()) {
			downloadListIndexThread.runCategorization(type);
		} else {
			downloadListIndexThread.runReloadIndexFiles();
		}
		DownloadIndexAdapter adapter = new DownloadIndexAdapter(this, list);
		setListAdapter(adapter);
		if(getMyApplication().getResourceManager().getIndexFileNames().isEmpty()) {
			boolean showedDialog = false;
			if(Build.VERSION.SDK_INT < OsmandSettings.VERSION_DEFAULTLOCATION_CHANGED) {
				SuggestExternalDirectoryDialog.showDialog(this, null, null);
			}
			if(!showedDialog) {
				showDialogOfFreeDownloadsIfNeeded();
			}
		} else {
			showDialogOfFreeDownloadsIfNeeded();
		}
		
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getSupportActionBar().getThemedContext(), R.layout.sherlock_spinner_item, 
				toString(downloadTypes)	
				);
		spinnerAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        getSupportActionBar().setListNavigationCallbacks(spinnerAdapter, new OnNavigationListener() {
			
			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				changeType(downloadTypes.get(itemPosition));
				return true;
			}
		});
		if (Build.VERSION.SDK_INT >= OsmandSettings.VERSION_DEFAULTLOCATION_CHANGED) {
			final String currentStorage = settings.getExternalStorageDirectory().getAbsolutePath();
			String primaryStorage = settings.getDefaultExternalStorageLocation();
			if (!currentStorage.startsWith(primaryStorage)) {
				// secondary storage
				boolean currentDirectoryNotWritable = true;
				for (String writeableDirectory : settings.getWritableSecondaryStorageDirectorys()) {
					if (currentStorage.startsWith(writeableDirectory)) {
						currentDirectoryNotWritable = false;
						break;
					}
				}
				if (currentDirectoryNotWritable) {
					currentDirectoryNotWritable = !OsmandSettings.isWritable(settings.getExternalStorageDirectory());
				}
				if (currentDirectoryNotWritable) {
					final String newLoc = settings.getMatchingExternalFilesDir(currentStorage);
					if (newLoc != null && newLoc.length() != 0) {
						AccessibleAlertBuilder ab = new AccessibleAlertBuilder(this);
						ab.setMessage(getString(R.string.android_19_location_disabled,
								settings.getExternalStorageDirectory()));
						ab.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								copyFilesForAndroid19(newLoc);
							}
						});
						ab.setNegativeButton(R.string.default_buttons_cancel, null);
						ab.show();
					}
				}
			}
		}
	}
	
	private void copyFilesForAndroid19(final String newLoc) {
		MoveFilesToDifferentDirectory task = 
				new MoveFilesToDifferentDirectory(DownloadIndexActivity.this, 
						new File(settings.getExternalStorageDirectory(), IndexConstants.APP_DIR), 
						new File(newLoc, IndexConstants.APP_DIR)) {
			protected Boolean doInBackground(Void[] params) {
				Boolean result = super.doInBackground(params);
				if(result) {
					settings.setExternalStorageDirectory(newLoc);
					getMyApplication().getResourceManager().resetStoreDirectory();
					getMyApplication().getResourceManager().reloadIndexes(progress)	;
				}
				return result;
			};
		};
		task.execute();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		getMyApplication().setDownloadActivity(this);
		BasicProgressAsyncTask<?, ?, ?> t = downloadListIndexThread.getCurrentRunningTask();
		updateProgress(false);
		if(t instanceof DownloadIndexesThread.DownloadIndexesAsyncTask) {
			View mainView = findViewById(R.id.MainLayout);
			if (mainView != null) {
				mainView.setKeepScreenOn(true);
			}
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		getMyApplication().setDownloadActivity(null);
	}
	
	public void showDialogToDownloadMaps(List<String> maps) {
		DownloadIndexAdapter a = (DownloadIndexAdapter) getListAdapter();
		boolean e = true;
		for (IndexItem i : a.getIndexFiles()) {
			for (String map : maps) {
				if (i.getFileName().equals(map + ".obf.zip")) {
					e = false;
					getEntriesToDownload().put(i, i.createDownloadEntry(getMyApplication(), type, new ArrayList<DownloadEntry>(1)));
				}
			}
		}
		if(!e){
			downloadFilesCheckInternet();
		}
	}


	private void showDialogOfFreeDownloadsIfNeeded() {
		if (Version.isFreeVersion(getMyApplication())) {
			Builder msg = new AlertDialog.Builder(this);
			msg.setTitle(R.string.free_version_title);
			String m = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "", "") + "\n";
			m += getString(R.string.available_downloads_left, MAXIMUM_AVAILABLE_FREE_DOWNLOADS - settings.NUMBER_OF_FREE_DOWNLOADS.get());
			msg.setMessage(m);
			if (Version.isMarketEnabled(getMyApplication())) {
				msg.setNeutralButton(R.string.install_paid, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.marketPrefix(getMyApplication()) +  "net.osmand.plus"));
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
	
	public Map<IndexItem, List<DownloadEntry>> getEntriesToDownload() {
		if(downloadListIndexThread == null) {
			return new LinkedHashMap<IndexItem, List<DownloadEntry>>();
		}
		return downloadListIndexThread.getEntriesToDownload();
	}

	public String getFilterText() {
		return filterText.getText().toString();
	}


	public void deselectAll() {
		final DownloadIndexAdapter listAdapter = (DownloadIndexAdapter)getExpandableListAdapter();
		downloadListIndexThread.getEntriesToDownload().clear();
		listAdapter.notifyDataSetInvalidated();
		
		findViewById(R.id.DownloadButton).setVisibility(View.GONE);
	}


	private void filterExisting() {
		final DownloadIndexAdapter listAdapter = (DownloadIndexAdapter)getExpandableListAdapter();
		final Map<String, String> listAlreadyDownloaded =  downloadListIndexThread.getDownloadedIndexFileNames();
		
		final List<IndexItem> filtered = new ArrayList<IndexItem>();
		for (IndexItem fileItem : listAdapter.getIndexFiles()) {
			if(fileItem.isAlreadyDownloaded(listAlreadyDownloaded)){
				filtered.add(fileItem);
			}
		}
		listAdapter.setIndexFiles(filtered, IndexItemCategory.categorizeIndexItems(getMyApplication(), filtered));
		listAdapter.notifyDataSetChanged();
	}


	private void selectAll() {
		final DownloadIndexAdapter listAdapter = (DownloadIndexAdapter)getExpandableListAdapter();
		int selected = 0;
		for (int j = 0; j < listAdapter.getGroupCount(); j++) {
			for (int i = 0; i < listAdapter.getChildrenCount(j); i++) {
				IndexItem es = listAdapter.getChild(j, i);
				if (!getEntriesToDownload().containsKey(es)) {
					selected++;
					getEntriesToDownload().put(es, es.createDownloadEntry(getMyApplication(), type, new ArrayList<DownloadEntry>(1)));
				}
			}
		}
		AccessibleToast.makeText(this, MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
		listAdapter.notifyDataSetInvalidated();
		if(selected > 0){
			updateDownloadButton(true);
		}
	}


	public void updateDownloadButton(boolean scroll) {
		int x = getListView().getScrollX();
		int y = getListView().getScrollY();
		if (getEntriesToDownload().isEmpty()) {
			findViewById(R.id.DownloadButton).setVisibility(View.GONE);
		} else {
			BasicProgressAsyncTask<?, ?, ?> task = downloadListIndexThread.getCurrentRunningTask();
			boolean running = task instanceof DownloadIndexesThread.DownloadIndexesAsyncTask; 
			((Button) findViewById(R.id.DownloadButton)).setEnabled(!running);
			String text;
			int downloads = downloadListIndexThread.getDownloads();
			if (!running) {
				text = getString(R.string.download_files) + "  (" + downloads + ")"; //$NON-NLS-1$
			} else {
				text = getString(R.string.downloading_file_new) + "  (" + downloads + ")"; //$NON-NLS-1$
			}
			findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
			if (Version.isFreeVersion(getMyApplication())) {
				int countedDownloads = downloadListIndexThread.getDownloads();
				int left = MAXIMUM_AVAILABLE_FREE_DOWNLOADS - settings.NUMBER_OF_FREE_DOWNLOADS.get() - downloads;
				boolean excessLimit = left < 0;
				if (left < 0)
					left = 0;
				if (DownloadActivityType.isCountedInDownloads(getType())) {
					text += " (" + (excessLimit ? "! " : "") + getString(R.string.files_limit, left).toLowerCase() + ")";
				}
			}
			((Button) findViewById(R.id.DownloadButton)).setText(text);
		}
		if (scroll) {
			getListView().scrollTo(x, y);
		}
	}


	private List<String> toString(List<DownloadActivityType> t) {
		ArrayList<String> items = new ArrayList<String>();
		for(DownloadActivityType ts : t) {
			items.add(ts.getString(getMyApplication()));
		}
		return items;
	}

	private List<DownloadActivityType> getDownloadTypes() {
		List<DownloadActivityType> items = new ArrayList<DownloadActivityType>();
		items.add(DownloadActivityType.NORMAL_FILE);
		items.add(DownloadActivityType.VOICE_FILE);
		items.add(DownloadActivityType.ROADS_FILE);
		if(OsmandPlugin.getEnabledPlugin(SRTMPlugin.class) != null){
			items.add(DownloadActivityType.HILLSHADE_FILE);
			items.add(DownloadActivityType.SRTM_COUNTRY_FILE);
		}
		getMyApplication().getAppCustomization().getDownloadTypes(items);
		return items;
	}


	public void changeType(final DownloadActivityType tp) {
		invalidateOptionsMenu();
		if (downloadListIndexThread != null && type != tp) {
			type = tp;
			downloadListIndexThread.runCategorization(tp);
		}
	}


	public ExpandableListView getListView() {
		return super.getExpandableListView();
	}
	
	public ExpandableListAdapter getListAdapter() {
		return super.getExpandableListAdapter();
	}
	
	private void makeSureUserCancelDownload() {
		Builder bld = new AlertDialog.Builder(this);
		bld.setTitle(getString(R.string.default_buttons_cancel));
		bld.setMessage(R.string.confirm_interrupt_download);
		bld.setPositiveButton(R.string.default_buttons_yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				BasicProgressAsyncTask<?, ?, ?> t = downloadListIndexThread.getCurrentRunningTask();
				if(t != null) {
					t.setInterrupted(true);
				}
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
			getEntriesToDownload().remove(e);
			updateDownloadButton(true);
			return true;
		}
		
		List<DownloadEntry> download = e.createDownloadEntry(getMyApplication(), type, new ArrayList<DownloadEntry>());
		if (download.size() > 0) {
			// if(!fileToUnzip.exists()){
			// builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, extractDateAndSize(e.getValue())));
			getEntriesToDownload().put(e, download);
			updateDownloadButton(true);
			ch.setChecked(!ch.isChecked());
		}
		return true;
	}
	
	
	public static Map<String, String> listWithAlternatives(final java.text.DateFormat dateFormat, File file, final String ext, 
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
	
	public static File findFileInDir(File file) {
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

	protected void downloadFilesCheckFreeVersion() {
		if (Version.isFreeVersion(getMyApplication()) ) {
			int total = settings.NUMBER_OF_FREE_DOWNLOADS.get();
			boolean wiki = false;
			for (IndexItem es : downloadListIndexThread.getEntriesToDownload().keySet()) {
				if (es.getBasename() != null && es.getBasename().contains("_wiki")) {
					wiki = true;
					break;
				} else if (DownloadActivityType.isCountedInDownloads(es.getType())) {
					total++;
				}
			}
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS || wiki) {
				String msgTx = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "");
				Builder msg = new AlertDialog.Builder(this);
				msg.setTitle(R.string.free_version_title);
				msg.setMessage(msgTx);
				msg.setPositiveButton(R.string.default_buttons_ok, null);
				msg.show();
			} else {
				downloadFilesCheckInternet();
			}
		} else {
			downloadFilesCheckInternet();
		}
	}
	
	
	protected void downloadFilesCheckInternet() {
		if(!getMyApplication().getSettings().isWifiConnected()) {
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.download_using_mobile_internet));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadFilesPreCheckSpace();
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
		} else {
			downloadFilesPreCheckSpace();
		}
	}
	
	protected void downloadFilesPreCheckSpace() {
		double sz = 0;
		List<DownloadEntry> list = downloadListIndexThread.flattenDownloadEntries();
		for (DownloadEntry es :  list) {
			sz += es.sizeMB;
		}
		// get availabile space
		double asz = downloadListIndexThread.getAvailableSpace();
		if (asz != -1 && asz > 0 && sz / asz > 0.4) {
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(MessageFormat.format(getString(R.string.download_files_question_space), list.size(), sz, asz));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadListIndexThread.runDownloadFiles();
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
		} else {
			downloadListIndexThread.runDownloadFiles();
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
	
	
	

	public void updateProgress(boolean updateOnlyProgress) {
		BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask = downloadListIndexThread.getCurrentRunningTask();
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
				cancel.setVisibility(indeterminate ? View.GONE : View.VISIBLE);
				progressPercent.setVisibility(indeterminate ? View.GONE : View.VISIBLE);

				progressMessage.setText(basicProgressAsyncTask.getDescription());
				if (!indeterminate) {
					progressPercent.setText(basicProgressAsyncTask.getProgressPercentage() + "%");
					determinateProgressBar.setProgress(basicProgressAsyncTask.getProgressPercentage());
				}
			}
			updateDownloadButton(false);
		}
	}
}
