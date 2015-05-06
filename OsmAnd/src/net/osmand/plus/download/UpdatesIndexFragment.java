package net.osmand.plus.download;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.osmand.access.AccessibleToast;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmAndListFragment;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Denis
 * on 09.09.2014.
 */
public class UpdatesIndexFragment extends OsmAndListFragment {

	private OsmandRegions osmandRegions;
	private java.text.DateFormat format;
	private UpdateIndexAdapter listAdapter;
	List<IndexItem> indexItems = new ArrayList<IndexItem>();


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.update_index, container, false);
		final CheckBox selectAll = (CheckBox) view.findViewById(R.id.select_all);
		selectAll.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (selectAll.isChecked()) {
					selectAll();
				} else {
					deselectAll();

				}
				listAdapter.notifyDataSetInvalidated();
			}
		});
		return view;
	}

	private void refreshSelectAll() {
		View view = getView();
		if (view == null) {
			return;
		}
		CheckBox selectAll = (CheckBox) view.findViewById(R.id.select_all);
		for (IndexItem item : indexItems) {
			if (!getDownloadActivity().getEntriesToDownload().containsKey(item)){
				selectAll.setChecked(false);
				return;
			}
		}
		selectAll.setChecked(true);
	}

	private void setSelectAllVisibility(boolean visible) {
		View view = getView();
		if (view == null) {
			return;
		}
		if (visible) {
			view.findViewById(R.id.header_layout).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.header_layout).setVisibility(View.GONE);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		format = getMyApplication().getResourceManager().getDateFormat();
		osmandRegions = getMyApplication().getResourceManager().getOsmandRegions();
		if (BaseDownloadActivity.downloadListIndexThread != null) {
			indexItems = new ArrayList<IndexItem>(DownloadActivity.downloadListIndexThread.getItemsToUpdate());
		}
		createListView();
		setHasOptionsMenu(true);
	}

	private void createListView() {
		updateHeader();
		if (indexItems.size() == 0) {
			if (DownloadActivity.downloadListIndexThread.isDownloadedFromInternet()) {
				indexItems.add(new IndexItem(getString(R.string.everything_up_to_date), "", 0, "", 0, 0, null));
			} else {
				indexItems.add(new IndexItem(getString(R.string.no_index_file_to_download), "", 0, "", 0, 0, null));
			}
		}
		listAdapter = new UpdateIndexAdapter(getDownloadActivity(), R.layout.download_index_list_item, indexItems);
		listAdapter.sort(new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem indexItem, IndexItem indexItem2) {
				return indexItem.getVisibleName(getMyApplication(), osmandRegions).compareTo(indexItem2.getVisibleName(getMyApplication(), osmandRegions));
			}
		});
		setListAdapter(listAdapter);
	}

	private void updateHeader(){
		View view = getView();
		if (getView() == null) {
			return;
		}
		String header = getActivity().getString(R.string.download_tab_updates) + " - " + indexItems.size();
		((TextView) view.findViewById(R.id.header)).
				setText(header);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	public void updateItemsList(List<IndexItem> items) {
		if (listAdapter == null) {
			return;
		}
		indexItems = new ArrayList<IndexItem>(items);
		createListView();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
		onItemSelected(ch, position);
	}

	private void onItemSelected(CheckBox ch, int position) {
		final IndexItem e = (IndexItem) getListAdapter().getItem(position);
		if (ch.isChecked()) {
			ch.setChecked(!ch.isChecked());
			getDownloadActivity().getEntriesToDownload().remove(e);
			getDownloadActivity().updateDownloadButton();
		} else {
			List<DownloadEntry> download = e.createDownloadEntry(getMyApplication(), e.getType(), new ArrayList<DownloadEntry>());
			if (download.size() > 0) {
				getDownloadActivity().getEntriesToDownload().put(e, download);
				getDownloadActivity().updateDownloadButton();
				ch.setChecked(!ch.isChecked());
			}
		}
		refreshSelectAll();
	}

	public DownloadActivity getDownloadActivity() {
		return (DownloadActivity) getActivity();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		updateHeader();
		ActionBar actionBar = getDownloadActivity().getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		if (getMyApplication().getAppCustomization().showDownloadExtraActions()) {
			MenuItem item = menu.add(0, DownloadIndexFragment.RELOAD_ID, 0, R.string.shared_string_refresh);
			item.setIcon(R.drawable.ic_action_refresh_dark);
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}

	public OsmandApplication getMyApplication() {
		return getDownloadActivity().getMyApplication();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == DownloadIndexFragment.RELOAD_ID) {
			// re-create the thread
			DownloadActivity.downloadListIndexThread.runReloadIndexFiles();
			return true;
		} else if (item.getItemId() == DownloadIndexFragment.SELECT_ALL_ID) {
			selectAll();
			return true;
		} else if (item.getItemId() == DownloadIndexFragment.FILTER_EXISTING_REGIONS) {
			filterExisting();
			return true;
		} else if (item.getItemId() == DownloadIndexFragment.DESELECT_ALL_ID) {
			deselectAll();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void selectAll() {
		int selected = 0;
		for (int i = 0; i < listAdapter.getCount(); i++) {
			IndexItem es = listAdapter.getItem(i);
			if (!getDownloadActivity().getEntriesToDownload().containsKey(es)) {
				selected++;
				getDownloadActivity().getEntriesToDownload().put(es, es.createDownloadEntry(getMyApplication(),
						getDownloadActivity().getDownloadType(), new ArrayList<DownloadEntry>(1)));

			}
		}
		AccessibleToast.makeText(getDownloadActivity(), MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
		listAdapter.notifyDataSetInvalidated();
		if (selected > 0) {
			getDownloadActivity().updateDownloadButton();
		}
	}

	public void deselectAll() {
		DownloadActivity.downloadListIndexThread.getEntriesToDownload().clear();
		listAdapter.notifyDataSetInvalidated();

		getDownloadActivity().findViewById(R.id.DownloadButton).setVisibility(View.GONE);
	}

	private void filterExisting() {
		final Map<String, String> listAlreadyDownloaded = DownloadActivity.downloadListIndexThread.getDownloadedIndexFileNames();

		final List<IndexItem> filtered = new ArrayList<IndexItem>();
		for (IndexItem fileItem : listAdapter.getIndexFiles()) {
			if (fileItem.isAlreadyDownloaded(listAlreadyDownloaded)) {
				filtered.add(fileItem);
			}
		}
		listAdapter.setIndexFiles(filtered);
		listAdapter.notifyDataSetChanged();
	}

	private class UpdateIndexAdapter extends ArrayAdapter<IndexItem> {
		List<IndexItem> items;

		public UpdateIndexAdapter(Context context, int resource, List<IndexItem> items) {
			super(context, resource, items);
			this.items = items;
		}

		public List<IndexItem> getIndexFiles() {
			return items;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = convertView;

			if (v == null) {
				LayoutInflater inflater = (LayoutInflater) getDownloadActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = inflater.inflate(R.layout.update_index_list_item, null);
			}

			TextView name = (TextView) v.findViewById(R.id.download_item);
			TextView description = (TextView) v.findViewById(R.id.download_descr);
			TextView updateDescr = (TextView) v.findViewById(R.id.update_descr);
			final CheckBox ch = (CheckBox) v.findViewById(R.id.check_download_item);
			IndexItem e = items.get(position);
			if (e.getFileName().equals(getString(R.string.everything_up_to_date)) ||
					e.getFileName().equals(getString(R.string.no_index_file_to_download))) {
				name.setText(e.getFileName());
				description.setText("");
				ch.setVisibility(View.INVISIBLE);
				setSelectAllVisibility(false);
				v.setOnClickListener(null);
				return v;
			} else {
				ch.setVisibility(View.VISIBLE);
			}

			String eName = e.getVisibleName(getMyApplication(), osmandRegions);

			name.setText(eName.trim().replace('\n', ' ').replace("TTS","")); //$NON-NLS-1$
			String d =  getMapDescription(e);
			description.setText(d);
			
			String sfName = e.getTargetFileName();
			Map<String, String> indexActivatedFileNames = getMyApplication().getResourceManager().getIndexFileNames();
			String dt = indexActivatedFileNames.get(sfName);
			updateDescr.setText("");
			if (dt != null) {
				try {
					Date tm = format.parse(dt);
					long days = Math.max(1, (e.getTimestamp() -  tm.getTime()) / (24 * 60 * 60 * 1000) + 1);  
					updateDescr.setText(days + " " + getString(R.string.days_behind));
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
			}
			

			ch.setChecked(getDownloadActivity().getEntriesToDownload().containsKey(e));
			ch.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ch.setChecked(!ch.isChecked());
					onItemSelected(ch, position);
				}
			});

			return v;
		}

		public void setIndexFiles(List<IndexItem> filtered) {
			clear();
			for (IndexItem item : filtered) {
				add(item);
			}
			sort(new Comparator<IndexItem>() {
				@Override
				public int compare(IndexItem indexItem, IndexItem indexItem2) {
					return indexItem.getVisibleName(getMyApplication(), osmandRegions).compareTo(indexItem2.getVisibleName(getMyApplication(), osmandRegions));
				}
			});
		}
	}

	private String getMapDescription(IndexItem item){
		String typeName = getTypeName(item.getType().getResource());
		String date = item.getDate(format);
		String size = item.getSizeDescription(getActivity());
		return typeName + "  " + date + "  " + size;

	}

	private String getTypeName(int resId){
		Activity activity = getActivity();
		if (resId == R.string.download_regular_maps){
			return activity.getString(R.string.shared_string_map);
		} else if (resId == R.string.voices){
			return activity.getString(R.string.ttsvoice);
		} else if (resId == R.string.download_roads_only_maps){
			return activity.getString(R.string.roads_only);
		} else if (resId == R.string.download_srtm_maps){
			return activity.getString(R.string.download_srtm_maps);
		} else if (resId == R.string.download_hillshade_maps){
			return activity.getString(R.string.download_hillshade_maps);
		}
		return "";
	}
}
