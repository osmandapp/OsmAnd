package net.osmand.plus.download;

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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmAndListFragment;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Denis
 * on 09.09.2014.
 */
public class UpdatesIndexFragment extends OsmAndListFragment {
	private OsmandRegions osmandRegions;
	private UpdateIndexAdapter listAdapter;
	List<IndexItem> indexItems = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (BaseDownloadActivity.downloadListIndexThread != null) {
			indexItems = new ArrayList<IndexItem>(DownloadActivity.downloadListIndexThread.getItemsToUpdate());
		}
		createListView();
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.update_index, container, false);
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
		listAdapter = new UpdateIndexAdapter(getMyActivity(), R.layout.download_index_list_item, indexItems);
		listAdapter.sort(new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem indexItem, IndexItem indexItem2) {
				return indexItem.getVisibleName(getMyApplication(), osmandRegions).compareTo(indexItem2.getVisibleName(getMyApplication(), osmandRegions));
			}
		});
		setListAdapter(listAdapter);
	}

	private void updateHeader() {
		osmandRegions = getMyApplication().getRegions();
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
		final IndexItem e = (IndexItem) getListAdapter().getItem(position);
		getMyActivity().startDownload(e);
	}

	public DownloadActivity getMyActivity() {
		return (DownloadActivity) getActivity();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		updateHeader();
		ActionBar actionBar = getMyActivity().getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		if (getMyApplication().getAppCustomization().showDownloadExtraActions()) {
			MenuItem item = menu.add(0, DownloadIndexFragment.RELOAD_ID, 0, R.string.shared_string_refresh);
			item.setIcon(R.drawable.ic_action_refresh_dark);
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}

	public OsmandApplication getMyApplication() {
		return getMyActivity().getMyApplication();
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
			if (!getMyActivity().getEntriesToDownload().containsKey(es)) {
				selected++;
				getMyActivity().getEntriesToDownload().put(es, es.createDownloadEntry(getMyApplication(),
						getMyActivity().getDownloadType(), new ArrayList<DownloadEntry>(1)));

			}
		}
		AccessibleToast.makeText(getMyActivity(), MessageFormat.format(getString(R.string.items_were_selected), selected), Toast.LENGTH_SHORT).show();
		listAdapter.notifyDataSetInvalidated();
		if (selected > 0) {
			getMyActivity().updateFragments();
		}
	}

	public void deselectAll() {
		DownloadActivity.downloadListIndexThread.getEntriesToDownload().clear();
		listAdapter.notifyDataSetInvalidated();
		getMyActivity().updateFragments();
	}

	private void filterExisting() {
		final Map<String, String> listAlreadyDownloaded = DownloadActivity.downloadListIndexThread.getDownloadedIndexFileNames();

		final List<IndexItem> filtered = new ArrayList<>();
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
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = LayoutInflater.from(getMyActivity());
				v = inflater.inflate(R.layout.two_line_with_images_list_item, null);
				v.setTag(new UpdateViewHolder(v, getMyActivity()));
			}
			UpdateViewHolder holder = (UpdateViewHolder) v.getTag();
			holder.bindUpdatesIndexItem(items.get(position));
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

	// TODO: 10/13/15 Extract viewholder parent
	private static class UpdateViewHolder {
		private final TextView nameTextView;
		private final TextView descrTextView;
		private final ImageView leftImageView;
		private final ImageView rightImageButton;
		private final Button rightButton;
		private final ProgressBar progressBar;
		private final TextView mapDateTextView;
		private final DownloadActivity context;
		private final java.text.DateFormat format;

		private UpdateViewHolder(View convertView,
								 final DownloadActivity context) {
			nameTextView = (TextView) convertView.findViewById(R.id.name);
			descrTextView = (TextView) convertView.findViewById(R.id.description);
			leftImageView = (ImageView) convertView.findViewById(R.id.leftImageView);
			rightImageButton = (ImageView) convertView.findViewById(R.id.rightImageButton);
			rightButton = (Button) convertView.findViewById(R.id.rightButton);
			progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);
			mapDateTextView = (TextView) convertView.findViewById(R.id.mapDateTextView);
			this.context = context;
			format = context.getMyApplication().getResourceManager().getDateFormat();
		}

		public void bindUpdatesIndexItem(IndexItem indexItem) {
			if (indexItem.getFileName().equals(context.getString(R.string.everything_up_to_date)) ||
					indexItem.getFileName().equals(context.getString(R.string.no_index_file_to_download))) {
				nameTextView.setText(indexItem.getFileName());
				descrTextView.setText("");
				return;
			}

			OsmandRegions osmandRegions =
					context.getMyApplication().getResourceManager().getOsmandRegions();
			String eName = indexItem.getVisibleName(context.getMyApplication(), osmandRegions);

			nameTextView.setText(eName.trim().replace('\n', ' ').replace("TTS", "")); //$NON-NLS-1$
			String d = getMapDescription(indexItem);
			descrTextView.setText(d);

			String sfName = indexItem.getTargetFileName();
			Map<String, String> indexActivatedFileNames = context.getMyApplication().getResourceManager().getIndexFileNames();
			String dt = indexActivatedFileNames.get(sfName);
			mapDateTextView.setText("");
			if (dt != null) {
				try {
					Date tm = format.parse(dt);
					long days = Math.max(1, (indexItem.getTimestamp() - tm.getTime()) / (24 * 60 * 60 * 1000) + 1);
					mapDateTextView.setText(days + " " + context.getString(R.string.days_behind));
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
			}
			rightImageButton.setVisibility(View.VISIBLE);
			rightImageButton.setImageDrawable(
					context.getMyApplication().getIconsCache()
							.getContentIcon(R.drawable.ic_action_import));
		}


		private String getMapDescription(IndexItem item) {
			String typeName = getTypeName(item, item.getType().getStringResource());
			String date = item.getDate(format);
			String size = item.getSizeDescription(context);
			return typeName + "  " + date + "  " + size;
		}

		private String getTypeName(IndexItem item, int resId) {
			Activity activity = context;
			if (resId == R.string.download_regular_maps) {
				return activity.getString(R.string.shared_string_map);
			} else if (resId == R.string.download_wikipedia_maps) {
				return activity.getString(R.string.shared_string_wikipedia);
			} else if (resId == R.string.voices) {
				return item.getTargetFileName().contains("tts") ? activity.getString(R.string.ttsvoice) : activity
						.getString(R.string.voice);
			} else if (resId == R.string.download_roads_only_maps) {
				return activity.getString(R.string.roads_only);
			} else if (resId == R.string.download_srtm_maps) {
				return activity.getString(R.string.download_srtm_maps);
			} else if (resId == R.string.download_hillshade_maps) {
				return activity.getString(R.string.download_hillshade_maps);
			}
			return "";
		}
	}
}
