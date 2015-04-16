package net.osmand.plus.download;


import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandExpandableListFragment;
import net.osmand.plus.base.BasicProgressAsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;


public class DownloadIndexFragment extends OsmandExpandableListFragment {
	
	/** menus **/
	private static boolean SHOW_ONLY_RELOAD = true;
	public static final int MORE_ID = 10;
	public static final int RELOAD_ID = 0;
	public static final int SELECT_ALL_ID = 1;
	public static final int DESELECT_ALL_ID = 2;
	public static final int FILTER_EXISTING_REGIONS = 3;
	
    private TextWatcher textWatcher ;
	private EditText filterText;

	DownloadIndexAdapter listAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.download_index, container, false);
		ExpandableListView listView = (ExpandableListView)view.findViewById(android.R.id.list);
		List<IndexItem> list = new ArrayList<IndexItem>();
		listAdapter = new DownloadIndexAdapter(this, list);
		listView.setAdapter(listAdapter);
		setListView(listView);

//		getDownloadActivity().getSupportActionBar().setTitle(R.string.shared_string_download);
		// recreation upon rotation is pgetaprevented in manifest file

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
		String filter = ((DownloadActivity)getActivity()).getInitialFilter();
		filterText.setText(filter);
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		getDownloadActivity().updateProgress(false);
		BasicProgressAsyncTask<?, ?, ?> t = DownloadActivity.downloadListIndexThread.getCurrentRunningTask();
		if(t instanceof DownloadIndexesThread.DownloadIndexesAsyncTask) {
			View mainView = getView().findViewById(R.id.MainLayout);
			if (mainView != null) {
				mainView.setKeepScreenOn(true);
			}
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
		if (getMyApplication().getAppCustomization().onlyTourDownload()){
			return;
		}

		ActionBar actionBar = getDownloadActivity().getSupportActionBar();
		final List<DownloadActivityType> downloadTypes = getDownloadActivity().getDownloadTypes();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(), R.layout.spinner_item,
				toString(downloadTypes)) ;
		spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(spinnerAdapter, new ActionBar.OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				getDownloadActivity().changeType(downloadTypes.get(itemPosition));
				return true;
			}
		});

		if (getMyApplication().getAppCustomization().showDownloadExtraActions()) {
			if (SHOW_ONLY_RELOAD) {
				MenuItem item = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
				item.setIcon(R.drawable.ic_action_refresh_dark);
				MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			} else {
				SubMenu s = menu.addSubMenu(0, MORE_ID, 0, R.string.shared_string_more_actions);
				s.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
				s.add(0, SELECT_ALL_ID, 0, R.string.shared_string_select_all);
				s.add(0, DESELECT_ALL_ID, 0, R.string.shared_string_deselect_all);

//				s.setIcon(isLightActionBar() ? R.drawable.abs__ic_menu_moreoverflow_holo_light
//						: R.drawable.abs__ic_menu_moreoverflow_holo_dark);
				MenuItemCompat.setShowAsAction(s.getItem(), MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
			}
		}
	}
	
	public String getFilterText() {
		return filterText.getText().toString();
	}


	public void deselectAll() {
		DownloadActivity.downloadListIndexThread.getEntriesToDownload().clear();
		listAdapter.notifyDataSetInvalidated();
		
		getDownloadActivity().findViewById(R.id.DownloadButton).setVisibility(View.GONE);
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
							getDownloadActivity().getDownloadType(), new ArrayList<DownloadEntry>(1)));
				}
			}
		}
		AccessibleToast.makeText(getDownloadActivity(), MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
		listAdapter.notifyDataSetInvalidated();
		if(selected > 0){
			getDownloadActivity().updateDownloadButton();
		}
	}



	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		final IndexItem e = listAdapter.getChild(groupPosition, childPosition);
		final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
		
		if(ch.isChecked()){
			ch.setChecked(!ch.isChecked());
			getDownloadActivity().getEntriesToDownload().remove(e);
			getDownloadActivity().updateDownloadButton();
			return true;
		}
		
		List<DownloadEntry> download = e.createDownloadEntry(getMyApplication(), getDownloadActivity().getDownloadType(), new ArrayList<DownloadEntry>());
		if (download.size() > 0) {
			// if(!fileToUnzip.exists()){
			// builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, extractDateAndSize(e.getValue())));
			getDownloadActivity().getEntriesToDownload().put(e, download);
			getDownloadActivity().updateDownloadButton();
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


	

	@Override
	public void onDestroy() {
		super.onDestroy();
	    if (textWatcher != null && getView() != null) {
	    	EditText filterText = (EditText) getView().findViewById(R.id.search_box);
	    	filterText.removeTextChangedListener(textWatcher);
	    }
	}

	public List<String> toString(List<DownloadActivityType> t) {
		ArrayList<String> items = new ArrayList<String>();
		for(DownloadActivityType ts : t) {
			items.add(ts.getString(getMyApplication()));
		}
		return items;
	}
	

	public DownloadActivity getDownloadActivity(){ return (DownloadActivity)getActivity();}

	public ExpandableListAdapter getExpandableListAdapter(){ return listAdapter;}

	public View findViewById(int id){ return getView().findViewById(id);}

	public void updateProgress(boolean b) {
		getDownloadActivity().updateProgress(b);
	}

	public void categorizationFinished(List<IndexItem> filtered, List<IndexItemCategory> cats) {
		Map<String, String> indexActivatedFileNames = getDownloadActivity().getIndexActivatedFileNames();
		Map<String, String> indexFileNames = getDownloadActivity().getIndexFileNames();
		DownloadIndexAdapter a = ((DownloadIndexAdapter) getExpandableListAdapter());
		if (a == null){
			return;
		}
		a.setLoadedFiles(indexActivatedFileNames, indexFileNames);
		a.setIndexFiles(filtered, cats);
		a.notifyDataSetChanged();
		a.getFilter().filter(getFilterText());
	}
}
