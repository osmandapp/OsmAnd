package net.osmand.plus.download;


import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.view.MenuInflater;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.activities.SettingsGeneralActivity.MoveFilesToDifferentDirectory;
import net.osmand.plus.base.BasicProgressAsyncTask;
import net.osmand.plus.base.SuggestExternalDirectoryDialog;
import net.osmand.plus.download.*;
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public class DownloadIndexFragment extends OsmandExpandableListFragment {
	
	/** menus **/
	private static final int MORE_ID = 10;
	private static final int RELOAD_ID = 0;
	private static final int SELECT_ALL_ID = 1;
	private static final int DESELECT_ALL_ID = 2;
	private static final int FILTER_EXISTING_REGIONS = 3;
	
    public static final String FILTER_KEY = "filter";
    public static final String FILTER_CAT = "filter_cat";
	
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

	DownloadIndexAdapter listAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final List<DownloadActivityType> downloadTypes = getDownloadTypes();
		getDownloadActivity().setType(downloadTypes.get(0));
		View view = inflater.inflate(R.layout.download_index, container, false);
		ExpandableListView listView = (ExpandableListView)view.findViewById(android.R.id.list);
		List<IndexItem> list = new ArrayList<IndexItem>();
		listAdapter = new DownloadIndexAdapter(this, list);
		listView.setAdapter(listAdapter);
		setListView(listView);
		indeterminateProgressBar = (ProgressBar) view.findViewById(R.id.IndeterminateProgressBar);
		determinateProgressBar = (ProgressBar) view.findViewById(R.id.DeterminateProgressBar);
		progressView = view.findViewById(R.id.ProgressView);
		progressMessage = (TextView) view.findViewById(R.id.ProgressMessage);
		progressPercent = (TextView) view.findViewById(R.id.ProgressPercent);
		cancel = (ImageView) view.findViewById(R.id.Cancel);
		int d = settings.isLightContent() ? R.drawable.a_1_navigation_cancel_small_light : R.drawable.a_1_navigation_cancel_small_dark;
		cancel.setImageDrawable(getResources().getDrawable(d));
		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				makeSureUserCancelDownload();
			}
		});
		//getSupportActionBar().setTitle(R.string.local_index_download);
		// recreation upon rotation is pgetaprevented in manifest file
		view.findViewById(R.id.DownloadButton).setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				downloadFilesCheckFreeVersion();
			}

		});

		filterText = (EditText) view.findViewById(R.id.search_box);
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
				if(listAdapter != null){
					listAdapter.getFilter().filter(s);
				}
			}

		};
		filterText.addTextChangedListener(textWatcher);
		//final Intent intent = getIntent();
		final Intent intent = null;
		if (intent != null && intent.getExtras() != null) {
			final String filter = intent.getExtras().getString(FILTER_KEY);
			if (filter != null) {
				filterText.setText(filter);
			}
			final String filterCat = intent.getExtras().getString(FILTER_CAT);
			if (filterCat != null) {
				DownloadActivityType type = DownloadActivityType.getIndexType(filterCat.toLowerCase());
				if (type != null) {
					getDownloadActivity().setType(type);
					downloadTypes.remove(type);
					downloadTypes.add(0, type);
				}
			}
		}
		getMyApplication().getAppCustomization().preDownloadActivity(getDownloadActivity(), downloadTypes, getDownloadActivity().getSupportActionBar());

		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		DownloadActivity.downloadListIndexThread.setUiActivity(this);

		settings = getMyApplication().getSettings();


		if(getMyApplication().getResourceManager().getIndexFileNames().isEmpty()) {
			boolean showedDialog = false;
			if(Build.VERSION.SDK_INT < OsmandSettings.VERSION_DEFAULTLOCATION_CHANGED) {
				SuggestExternalDirectoryDialog.showDialog(getActivity(), null, null);
			}
			if(!showedDialog) {
				showDialogOfFreeDownloadsIfNeeded();
			}
		} else {
			showDialogOfFreeDownloadsIfNeeded();
		}
		

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
						AccessibleAlertBuilder ab = new AccessibleAlertBuilder(getDownloadActivity());
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
				new MoveFilesToDifferentDirectory(getDownloadActivity(),
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
	public void onResume() {
		super.onResume();
		getMyApplication().setDownloadActivity(this);
		updateProgress(false);
		BasicProgressAsyncTask<?, ?, ?> t = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
		if(t instanceof DownloadIndexesThread.DownloadIndexesAsyncTask) {
			View mainView = getView().findViewById(R.id.MainLayout);
			if (mainView != null) {
				mainView.setKeepScreenOn(true);
			}
		}
	}

	public void showDialogToDownloadMaps(Collection<String> maps) {
		int count = 0;
		int sz = 0;
		String s = "";
		for (IndexItem i : DownloadActivity.downloadListIndexThread.getCachedIndexFiles()) {
			for (String map : maps) {
				if (i.getFileName().equals(map + ".obf.zip") && i.getType() == DownloadActivityType.NORMAL_FILE) {
					final List<DownloadEntry> de = i.createDownloadEntry(getMyApplication(), i.getType(), new ArrayList<DownloadEntry>(1));
					for(DownloadEntry d : de ) {
						count++;
						sz += d.sizeMB;
					}
					if(s.length() > 0) {
						s +=", ";
					}
					s += i.getVisibleName(getMyApplication(), getMyApplication().getResourceManager().getOsmandRegions());
					getDownloadActivity().getEntriesToDownload().put(i, de);
				}
			}
		}
		if(count > 0){
			Builder builder = new AlertDialog.Builder(getDownloadActivity());
			builder.setMessage(getString(R.string.download_additional_maps, s, sz));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					downloadFilesCheckInternet();
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getDownloadActivity().getEntriesToDownload().clear();
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					getDownloadActivity().getEntriesToDownload().clear();
				}
			});
			builder.show();
			
		}
	}


	private void showDialogOfFreeDownloadsIfNeeded() {
		if (Version.isFreeVersion(getMyApplication())) {
			Builder msg = new AlertDialog.Builder(getDownloadActivity());
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
			DownloadActivity.downloadListIndexThread.runReloadIndexFiles();
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (getMyApplication().getAppCustomization().showDownloadExtraActions()) {
			SubMenu s = menu.addSubMenu(0, MORE_ID, 0, R.string.default_buttons_other_actions);
			s.add(0, RELOAD_ID, 0, R.string.update_downlod_list);
			s.add(0, FILTER_EXISTING_REGIONS, 0, R.string.filter_existing_indexes);
			s.add(0, SELECT_ALL_ID, 0, R.string.select_all);
			s.add(0, DESELECT_ALL_ID, 0, R.string.deselect_all);

			s.setIcon(isLightActionBar() ? R.drawable.abs__ic_menu_moreoverflow_holo_light
					: R.drawable.abs__ic_menu_moreoverflow_holo_dark);
			s.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		}
	}
	
	public String getFilterText() {
		return filterText.getText().toString();
	}


	public void deselectAll() {
		DownloadActivity.downloadListIndexThread.getEntriesToDownload().clear();
		listAdapter.notifyDataSetInvalidated();
		
		getView().findViewById(R.id.DownloadButton).setVisibility(View.GONE);
	}


	private void filterExisting() {
		final Map<String, String> listAlreadyDownloaded =  DownloadActivity.downloadListIndexThread.getDownloadedIndexFileNames();
		
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
		int selected = 0;
		for (int j = 0; j < listAdapter.getGroupCount(); j++) {
			for (int i = 0; i < listAdapter.getChildrenCount(j); i++) {
				IndexItem es = listAdapter.getChild(j, i);
				if (!getDownloadActivity().getEntriesToDownload().containsKey(es)) {
					selected++;
					getDownloadActivity().getEntriesToDownload().put(es, es.createDownloadEntry(getMyApplication(),
							getDownloadActivity().getType(), new ArrayList<DownloadEntry>(1)));
				}
			}
		}
		AccessibleToast.makeText(getDownloadActivity(), MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
		listAdapter.notifyDataSetInvalidated();
		if(selected > 0){
			updateDownloadButton(true);
		}
	}


	public void updateDownloadButton(boolean scroll) {
		View view = getView();
		int x = getExpandableListView().getScrollX();
		int y = getExpandableListView().getScrollY();
		if (getDownloadActivity().getEntriesToDownload().isEmpty()) {
			view.findViewById(R.id.DownloadButton).setVisibility(View.GONE);
		} else {
			BasicProgressAsyncTask<?, ?, ?> task = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
			boolean running = task instanceof DownloadIndexesThread.DownloadIndexesAsyncTask; 
			((Button) view.findViewById(R.id.DownloadButton)).setEnabled(!running);
			String text;
			int downloads = DownloadActivity.downloadListIndexThread.getDownloads();
			if (!running) {
				text = getString(R.string.download_files) + "  (" + downloads + ")"; //$NON-NLS-1$
			} else {
				text = getString(R.string.downloading_file_new) + "  (" + downloads + ")"; //$NON-NLS-1$
			}
			view.findViewById(R.id.DownloadButton).setVisibility(View.VISIBLE);
			if (Version.isFreeVersion(getMyApplication())) {
				int countedDownloads = DownloadActivity.downloadListIndexThread.getDownloads();
				int left = MAXIMUM_AVAILABLE_FREE_DOWNLOADS - settings.NUMBER_OF_FREE_DOWNLOADS.get() - downloads;
				boolean excessLimit = left < 0;
				if (left < 0)
					left = 0;
				if (DownloadActivityType.isCountedInDownloads(getDownloadActivity().getType())) {
					text += " (" + (excessLimit ? "! " : "") + getString(R.string.files_limit, left).toLowerCase() + ")";
				}
			}
			((Button) view.findViewById(R.id.DownloadButton)).setText(text);
		}
		if (scroll) {
			getExpandableListView().scrollTo(x, y);
		}
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

	private void makeSureUserCancelDownload() {
		Builder bld = new AlertDialog.Builder(getDownloadActivity());
		bld.setTitle(getString(R.string.default_buttons_cancel));
		bld.setMessage(R.string.confirm_interrupt_download);
		bld.setPositiveButton(R.string.default_buttons_yes, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				BasicProgressAsyncTask<?, ?, ?> t = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
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
		final IndexItem e = (IndexItem) listAdapter.getChild(groupPosition, childPosition);
		final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
		
		if(ch.isChecked()){
			ch.setChecked(!ch.isChecked());
			getDownloadActivity().getEntriesToDownload().remove(e);
			updateDownloadButton(true);
			return true;
		}
		
		List<DownloadEntry> download = e.createDownloadEntry(getMyApplication(), getDownloadActivity().getType(), new ArrayList<DownloadEntry>());
		if (download.size() > 0) {
			// if(!fileToUnzip.exists()){
			// builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, extractDateAndSize(e.getValue())));
			getDownloadActivity().getEntriesToDownload().put(e, download);
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
			for (IndexItem es : DownloadActivity.downloadListIndexThread.getEntriesToDownload().keySet()) {
				if (es.getBasename() != null && es.getBasename().contains("_wiki")) {
					wiki = true;
					break;
				} else if (DownloadActivityType.isCountedInDownloads(es.getType())) {
					total++;
				}
			}
			if (total > MAXIMUM_AVAILABLE_FREE_DOWNLOADS || wiki) {
				String msgTx = getString(R.string.free_version_message, MAXIMUM_AVAILABLE_FREE_DOWNLOADS + "");
				Builder msg = new AlertDialog.Builder(getDownloadActivity());
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
			Builder builder = new AlertDialog.Builder(getDownloadActivity());
			builder.setMessage(getString(R.string.download_using_mobile_internet));
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					getDownloadActivity().downloadFilesPreCheckSpace();
				}
			});
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.show();
		} else {
			getDownloadActivity().downloadFilesPreCheckSpace();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	    if (textWatcher != null && getView() != null) {
	    	EditText filterText = (EditText) getView().findViewById(R.id.search_box);
	    	filterText.removeTextChangedListener(textWatcher);
	    }
		DownloadActivity.downloadListIndexThread.setUiActivity(null);
	}
	

	public void updateProgress(boolean updateOnlyProgress) {
		BasicProgressAsyncTask<?, ?, ?> basicProgressAsyncTask = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
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

	public DownloadActivity getDownloadActivity(){ return (DownloadActivity)getActivity();}

	public ExpandableListAdapter getExpandableListAdapter(){ return listAdapter;}

	public View findViewById(int id){ return getView().findViewById(id);}
}
