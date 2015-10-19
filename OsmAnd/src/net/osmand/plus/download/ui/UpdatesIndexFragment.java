package net.osmand.plus.download.ui;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmAndListFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
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
import android.widget.ListView;
import android.widget.TextView;

public class UpdatesIndexFragment extends OsmAndListFragment implements DownloadEvents {
	private static final int RELOAD_ID = 5;
	private UpdateIndexAdapter listAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		invalidateListView();
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.update_index_frament, container, false);
	}
	
	@Override
	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}
	
	@Override
	public void downloadHasFinished() {
		invalidateListView();
	}
	
	@Override
	public void downloadInProgress() {
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void newDownloadIndexes() {
		invalidateListView();
	}

	public void invalidateListView() {
		DownloadResources indexes = getMyActivity().getDownloadThread().getIndexes();
		List<IndexItem> indexItems = indexes.getItemsToUpdate();
		if (indexItems.size() == 0) {
			if (indexes.isDownloadedFromInternet) {
				indexItems.add(new IndexItem(getString(R.string.everything_up_to_date), "", 0, "", 0, 0, null));
			} else {
				indexItems.add(new IndexItem(getString(R.string.no_index_file_to_download), "", 0, "", 0, 0, null));
			}
		}
		final OsmandRegions osmandRegions =
				getMyApplication().getResourceManager().getOsmandRegions();
		listAdapter = new UpdateIndexAdapter(getMyActivity(), R.layout.download_index_list_item, indexItems);
		listAdapter.sort(new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem indexItem, IndexItem indexItem2) {
				return indexItem.getVisibleName(getMyApplication(), osmandRegions)
						.compareTo(indexItem2.getVisibleName(getMyApplication(), osmandRegions));
			}
		});
		updateUpdateAllButton(indexes.getItemsToUpdate());
		setListAdapter(listAdapter);
	}

	private void updateUpdateAllButton(final List<IndexItem> indexItems) {
		View view = getView();
		if (getView() == null) {
			return;
		}
		final TextView updateAllButton = (TextView) view.findViewById(R.id.updateAllButton);
		if (indexItems.size() == 0 || indexItems.get(0).getType() == null) {
			updateAllButton.setVisibility(View.GONE);
		} else {
			updateAllButton.setVisibility(View.VISIBLE);
			long downloadsSize = 0;
			for (IndexItem indexItem : indexItems) {
				downloadsSize += indexItem.getSize();
			}
			String updateAllText = getActivity().getString(R.string.update_all, downloadsSize >> 20);
			updateAllButton.setText(updateAllText);
			updateAllButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getMyActivity().startDownload(indexItems.toArray(new IndexItem[indexItems.size()]));
				}
			});
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final IndexItem e = (IndexItem) getListAdapter().getItem(position);
		getMyActivity().startDownload(e);
	}

	public DownloadActivity getMyActivity() {
		return (DownloadActivity) getActivity();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		ActionBar actionBar = getMyActivity().getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

		if (getMyApplication().getAppCustomization().showDownloadExtraActions()) {
			MenuItem item = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
			item.setIcon(R.drawable.ic_action_refresh_dark);
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}

	public OsmandApplication getMyApplication() {
		return getMyActivity().getMyApplication();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == RELOAD_ID) {
			// re-create the thread
			getMyActivity().getDownloadThread().runReloadIndexFiles();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class UpdateIndexAdapter extends ArrayAdapter<IndexItem> {
		List<IndexItem> items;

		public UpdateIndexAdapter(Context context, int resource, List<IndexItem> items) {
			super(context, resource, items);
			this.items = items;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater inflater = LayoutInflater.from(getMyActivity());
				v = inflater.inflate(R.layout.two_line_with_images_list_item, parent, false);
				v.setTag(new UpdateViewHolder(v, getMyActivity()));
			}
			UpdateViewHolder holder = (UpdateViewHolder) v.getTag();
			holder.bindUpdatesIndexItem(items.get(position));
			return v;
		}
	}

	// FIXME review and delete if duplicate
	private static class UpdateViewHolder extends ItemViewHolder {
		private final java.text.DateFormat format;
		private UpdateViewHolder(View convertView,
								 final DownloadActivity context) {
			super(convertView, context);
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


		// FIXME general method for all maps
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
