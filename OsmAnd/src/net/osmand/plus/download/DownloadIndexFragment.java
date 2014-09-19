package net.osmand.plus.download;


import java.io.File;
import java.io.FilenameFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
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
import net.osmand.plus.srtmplugin.SRTMPlugin;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
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
	public static final int MORE_ID = 10;
	public static final int RELOAD_ID = 0;
	public static final int SELECT_ALL_ID = 1;
	public static final int DESELECT_ALL_ID = 2;
	public static final int FILTER_EXISTING_REGIONS = 3;
	
    private TextWatcher textWatcher ;
	private EditText filterText;
	private OsmandSettings settings;

	DownloadIndexAdapter listAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.download_index, container, false);
		ExpandableListView listView = (ExpandableListView)view.findViewById(android.R.id.list);
		List<IndexItem> list = new ArrayList<IndexItem>();
		listAdapter = new DownloadIndexAdapter(this, list);
		listView.setAdapter(listAdapter);
		setListView(listView);

		getDownloadActivity().getSupportActionBar().setTitle(R.string.local_index_download);
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

		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		DownloadActivity.downloadListIndexThread.setUiFragment(this);

		settings = getMyApplication().getSettings();


	}
	
	@Override
	public void onResume() {
		super.onResume();
		getMyApplication().setDownloadActivity(this);
		getDownloadActivity().updateProgress(false);
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
				 	getDownloadActivity().downloadFilesCheckInternet();
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
		ActionBar actionBar = getDownloadActivity().getSupportActionBar();
		final List<DownloadActivityType> downloadTypes = getDownloadActivity().getDownloadTypes();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(actionBar.getThemedContext(), R.layout.sherlock_spinner_item,
				toString(downloadTypes));
		spinnerAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(spinnerAdapter, new ActionBar.OnNavigationListener() {

			@Override
			public boolean onNavigationItemSelected(int itemPosition, long itemId) {
				getDownloadActivity().changeType(downloadTypes.get(itemPosition));
				return true;
			}
		});

		if (getMyApplication().getAppCustomization().showDownloadExtraActions()) {
			SubMenu s = menu.addSubMenu(0, MORE_ID, 0, R.string.default_buttons_other_actions);
			s.add(0, RELOAD_ID, 0, R.string.update_downlod_list);
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
							getDownloadActivity().getType(), new ArrayList<DownloadEntry>(1)));
				}
			}
		}
		AccessibleToast.makeText(getDownloadActivity(), MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
		listAdapter.notifyDataSetInvalidated();
		if(selected > 0){
			getDownloadActivity().updateDownloadButton(true);
		}
	}



	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		final IndexItem e = listAdapter.getChild(groupPosition, childPosition);
		final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
		
		if(ch.isChecked()){
			ch.setChecked(!ch.isChecked());
			getDownloadActivity().getEntriesToDownload().remove(e);
			getDownloadActivity().updateDownloadButton(true);
			return true;
		}
		
		List<DownloadEntry> download = e.createDownloadEntry(getMyApplication(), getDownloadActivity().getType(), new ArrayList<DownloadEntry>());
		if (download.size() > 0) {
			// if(!fileToUnzip.exists()){
			// builder.setMessage(MessageFormat.format(getString(R.string.download_question), baseName, extractDateAndSize(e.getValue())));
			getDownloadActivity().getEntriesToDownload().put(e, download);
			getDownloadActivity().updateDownloadButton(true);
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
		DownloadActivity.downloadListIndexThread.setUiFragment(null);
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
}
