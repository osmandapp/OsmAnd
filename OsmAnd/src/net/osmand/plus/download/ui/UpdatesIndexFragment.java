package net.osmand.plus.download.ui;

import java.util.Comparator;
import java.util.List;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.util.Algorithms;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class UpdatesIndexFragment extends OsmAndListFragment implements DownloadEvents {
	private static final int RELOAD_ID = 5;
	private UpdateIndexAdapter listAdapter;
	private String errorMessage;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.update_index_frament, container, false);
		getMyActivity().getAccessibilityAssistant().registerPage(view, DownloadActivity.UPDATES_TAB_NUMBER);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		updateErrorMessage();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		invalidateListView(activity);
	}

	@Override
	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}
	
	@Override
	public void downloadHasFinished() {
		invalidateListView(getMyActivity());
		updateUpdateAllButton();
	}
	
	@Override
	public void downloadInProgress() {
		listAdapter.notifyDataSetChanged();
	}
	
	@Override
	public void newDownloadIndexes() {
		invalidateListView(getMyActivity());
		updateUpdateAllButton();
	}

	public void invalidateListView(Activity a) {
		DownloadResources indexes = getMyApplication().getDownloadThread().getIndexes();
		List<IndexItem> indexItems = indexes.getItemsToUpdate();

		final OsmandRegions osmandRegions =
				getMyApplication().getResourceManager().getOsmandRegions();
		OsmandSettings settings = getMyApplication().getSettings();
		listAdapter = new UpdateIndexAdapter(a, R.layout.download_index_list_item, indexItems,
				!InAppPurchaseHelper.isSubscribedToLiveUpdates(getMyApplication()) || settings.SHOULD_SHOW_FREE_VERSION_BANNER.get());
		final Collator collator = OsmAndCollator.primaryCollator();
		listAdapter.sort(new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem indexItem, IndexItem indexItem2) {
				return collator.compare(indexItem.getVisibleName(getMyApplication(), osmandRegions), 
						indexItem2.getVisibleName(getMyApplication(), osmandRegions));
			}
		});
		setListAdapter(listAdapter);
		updateErrorMessage();

	}

	private void updateErrorMessage() {
		final View view = getView();
		if (view == null) return;

		DownloadResources indexes = getMyApplication().getDownloadThread().getIndexes();
		List<IndexItem> indexItems = indexes.getItemsToUpdate();
		if (getListAdapter() != null && indexItems.size() == 0) {
			int messageId = indexes.isDownloadedFromInternet ? R.string.everything_up_to_date
					: R.string.no_index_file_to_download;
			errorMessage = getString(messageId);
		} else {
			errorMessage = null;
		}
		updateUpdateAllButton();
	}

	private void updateUpdateAllButton() {
		
		View view = getView();
		if (view == null) {
			return;
		}
		DownloadResources indexes = getMyActivity().getDownloadThread().getIndexes();
		final List<IndexItem> indexItems = indexes.getItemsToUpdate();
		final TextView updateAllButton = (TextView) view.findViewById(R.id.updateAllButton);
		if (indexItems.size() == 0 || indexItems.get(0).getType() == null) {
			if (!Algorithms.isEmpty(errorMessage)) {
				updateAllButton.setText(errorMessage);
				updateAllButton.setEnabled(false);
				updateAllButton.setVisibility(View.VISIBLE);
			} else {
				updateAllButton.setVisibility(View.GONE);
			}
		} else {
			updateAllButton.setVisibility(View.VISIBLE);
			updateAllButton.setEnabled(true);
			long downloadsSize = 0;
			for (IndexItem indexItem : indexItems) {
				downloadsSize += indexItem.getSize();
			}
			String updateAllText = getActivity().getString(R.string.update_all, downloadsSize >> 20);
			updateAllButton.setText(updateAllText);
			updateAllButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (indexItems.size() > 3) {
						AlertDialog.Builder dialog = new AlertDialog.Builder(getMyActivity());
						dialog.setTitle(R.string.update_all_maps);
						dialog.setMessage(getString(R.string.update_all_maps_q, indexItems.size()));
						dialog.setNegativeButton(R.string.shared_string_cancel, null);
						dialog.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								getMyActivity().startDownload(indexItems.toArray(new IndexItem[indexItems.size()]));
							}
						});
						dialog.create().show();
					} else {
						getMyActivity().startDownload(indexItems.toArray(new IndexItem[indexItems.size()]));
					}
				}
			});
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		updateUpdateAllButton();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (listAdapter.isShowOsmLiveBanner() && position == 0) {
			DownloadActivity activity = getMyActivity();
			if (activity != null) {
				ChoosePlanDialogFragment.showOsmLiveInstance(activity.getSupportFragmentManager());
			}
		} else {
			final IndexItem e = (IndexItem) getListAdapter().getItem(position);
			ItemViewHolder vh = (ItemViewHolder) v.getTag();
			OnClickListener ls = vh.getRightButtonAction(e, vh.getClickAction(e));
			ls.onClick(v);
		}
	}

	public DownloadActivity getMyActivity() {
		return (DownloadActivity) getActivity();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		ActionBar actionBar = getMyActivity().getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		OsmandApplication app = getMyApplication();

		if (app.getAppCustomization().showDownloadExtraActions()) {
			int colorResId = app.getSettings().isLightContent() ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark;
			MenuItem item = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
			Drawable icRefresh = app.getUIUtilities().getIcon(R.drawable.ic_action_refresh_dark, colorResId);
			item.setIcon(icRefresh);
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

		static final int INDEX_ITEM = 0;
		static final int OSM_LIVE_BANNER = 1;

		List<IndexItem> items;
		boolean showOsmLiveBanner;

		public UpdateIndexAdapter(Context context, int resource, List<IndexItem> items, boolean showOsmLiveBanner) {
			super(context, resource, items);
			this.items = items;
			this.showOsmLiveBanner = showOsmLiveBanner;
		}

		public boolean isShowOsmLiveBanner() {
			return showOsmLiveBanner;
		}

		@Override
		public int getCount() {
			return super.getCount() + (showOsmLiveBanner ? 1 : 0);
		}

		@Override
		public IndexItem getItem(int position) {
			if (showOsmLiveBanner && position == 0) {
				return null;
			} else {
				return super.getItem(position - (showOsmLiveBanner ? 1 : 0));
			}
		}

		@Override
		public int getPosition(IndexItem item) {
			return super.getPosition(item) + (showOsmLiveBanner ? 1 : 0);
		}

		@Override
		public int getViewTypeCount() {
			return showOsmLiveBanner ? 2 : 1;
		}

		@Override
		public int getItemViewType(int position) {
			return showOsmLiveBanner && position == 0 ? OSM_LIVE_BANNER : INDEX_ITEM;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View v = convertView;
			int viewType = getItemViewType(position);
			if (v == null) {
				if (viewType == INDEX_ITEM) {
					LayoutInflater inflater = LayoutInflater.from(getMyActivity());
					v = inflater.inflate(R.layout.two_line_with_images_list_item, parent, false);
					v.setTag(new ItemViewHolder(v, getMyActivity()));
				} else if (viewType == OSM_LIVE_BANNER) {
					LayoutInflater inflater = LayoutInflater.from(getMyActivity());
					v = inflater.inflate(R.layout.osm_live_banner_list_item, parent, false);
				}
			}
			if (viewType == INDEX_ITEM) {
				ItemViewHolder holder = (ItemViewHolder) v.getTag();
				holder.setShowRemoteDate(true);
				holder.setShowTypeInDesc(true);
				holder.setShowParentRegionName(true);
				holder.bindIndexItem(getItem(position));
			}
			return v;
		}
	}
}
