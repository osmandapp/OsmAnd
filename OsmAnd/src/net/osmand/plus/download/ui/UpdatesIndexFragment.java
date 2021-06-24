package net.osmand.plus.download.ui;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.liveupdates.LiveUpdatesClearBottomSheet.RefreshLiveUpdates;
import net.osmand.plus.liveupdates.LiveUpdatesFragment;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.LiveUpdateListener;
import net.osmand.plus.liveupdates.LoadLiveMapsTask;
import net.osmand.plus.liveupdates.LoadLiveMapsTask.LocalIndexInfoAdapter;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static net.osmand.plus.liveupdates.LiveUpdatesFragment.showUpdateDialog;
import static net.osmand.plus.liveupdates.LiveUpdatesFragment.updateCountEnabled;

public class UpdatesIndexFragment extends OsmAndListFragment implements DownloadEvents, RefreshLiveUpdates, LiveUpdateListener {
	private static final int RELOAD_ID = 5;
	private UpdateIndexAdapter listAdapter;
	private String errorMessage;
	private OsmandSettings settings;
	private LoadLiveMapsTask loadLiveMapsTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		settings = getMyApplication().getSettings();
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
		startLoadLiveMapsAsyncTask(getMyApplication());
	}

	@Override
	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}

	@Override
	public void downloadHasFinished() {
		invalidateListView(getMyActivity());
		updateUpdateAllButton();
		startLoadLiveMapsAsyncTask(getMyApplication());
	}

	@Override
	public void downloadInProgress() {
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void onUpdatedIndexesList() {
		invalidateListView(getMyActivity());
		updateUpdateAllButton();
	}

	public void invalidateListView(Activity a) {
		final OsmandApplication app = getMyApplication();
		OsmandSettings settings = app.getSettings();
		DownloadResources indexes = app.getDownloadThread().getIndexes();
		List<IndexItem> indexItems = indexes.getItemsToUpdate();

		final OsmandRegions osmandRegions = app.getResourceManager().getOsmandRegions();
		listAdapter = new UpdateIndexAdapter(a, R.layout.download_index_list_item, indexItems,
				!InAppPurchaseHelper.isSubscribedToLiveUpdates(app) || settings.SHOULD_SHOW_FREE_VERSION_BANNER.get());
		final Collator collator = OsmAndCollator.primaryCollator();
		listAdapter.sort(new Comparator<IndexItem>() {
			@Override
			public int compare(IndexItem indexItem, IndexItem indexItem2) {
				return collator.compare(indexItem.getVisibleName(app, osmandRegions),
						indexItem2.getVisibleName(app, osmandRegions));
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
			String updateAllText = getString(
					R.string.update_all, String.valueOf(downloadsSize >> 20));
			updateAllButton.setText(updateAllText);
			updateAllButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final DownloadActivity activity = getMyActivity();
					if (indexItems.size() > 3) {
						AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
						dialog.setTitle(R.string.update_all_maps);
						dialog.setMessage(getString(R.string.update_all_maps_q, indexItems.size()));
						dialog.setNegativeButton(R.string.shared_string_cancel, null);
						dialog.setPositiveButton(R.string.shared_string_update, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								activity.startDownload(indexItems.toArray(new IndexItem[0]));
							}
						});
						dialog.create().show();
					} else {
						activity.startDownload(indexItems.toArray(new IndexItem[0]));
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
	public void onPause() {
		super.onPause();
		stopLoadLiveMapsAsyncTask();
	}

	private void startLoadLiveMapsAsyncTask(OsmandApplication app) {
		loadLiveMapsTask = new LoadLiveMapsTask(listAdapter, app);
		loadLiveMapsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void stopLoadLiveMapsAsyncTask() {
		if (loadLiveMapsTask != null && loadLiveMapsTask.getStatus() == AsyncTask.Status.RUNNING) {
			loadLiveMapsTask.cancel(false);
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (position == 0) {
			DownloadActivity activity = getMyActivity();
			if (activity != null) {
				if (!listAdapter.isShowSubscriptionPurchaseBanner()) {
					LiveUpdatesFragment.showInstance(activity.getSupportFragmentManager(), this);
				}
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

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == RELOAD_ID) {
			// re-create the thread
			getMyActivity().getDownloadThread().runReloadIndexFiles();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onUpdateStates(Context context) {
		if (context instanceof OsmandApplication) {
			startLoadLiveMapsAsyncTask((OsmandApplication) context);
		}
	}

	private class UpdateIndexAdapter extends ArrayAdapter<IndexItem> implements LocalIndexInfoAdapter {

		static final int INDEX_ITEM = 0;
		static final int OSM_LIVE_BANNER = 1;
		List<IndexItem> items;
		private final ArrayList<LocalIndexInfo> mapsList = new ArrayList<>();
		private final boolean showSubscriptionPurchaseBanner;
		private TextView countView;
		private int countEnabled = 0;

		@Override
		public void addData(LocalIndexInfo info) {
			mapsList.add(info);
		}

		@Override
		public void clearData() {
			mapsList.clear();
		}

		@Override
		public void onDataUpdated() {
			countEnabled = updateCountEnabled(countView, mapsList, settings);
		}

		public UpdateIndexAdapter(Context context, int resource, List<IndexItem> items, boolean showSubscriptionPurchaseBanner) {
			super(context, resource, items);
			this.items = items;
			this.showSubscriptionPurchaseBanner = showSubscriptionPurchaseBanner;
		}

		public boolean isShowSubscriptionPurchaseBanner() {
			return showSubscriptionPurchaseBanner;
		}

		@Override
		public int getCount() {
			return super.getCount() + 1;
		}

		@Override
		public IndexItem getItem(int position) {
			if (position == 0) {
				return null;
			} else {
				return super.getItem(position - 1);
			}
		}

		@Override
		public int getPosition(IndexItem item) {
			return super.getPosition(item) + 1;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			return position == 0 ? OSM_LIVE_BANNER : INDEX_ITEM;
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			View view = convertView;
			int viewType = getItemViewType(position);
			if (view == null) {
				LayoutInflater inflater = LayoutInflater.from(getMyActivity());
				if (viewType == INDEX_ITEM) {
					view = inflater.inflate(R.layout.two_line_with_images_list_item, parent, false);
					view.setTag(new ItemViewHolder(view, getMyActivity()));
				} else if (viewType == OSM_LIVE_BANNER) {
					OsmandApplication app = getMyApplication();
					boolean nightMode = !app.getSettings().isLightContent();
					if (showSubscriptionPurchaseBanner) {
						view = inflater.inflate(R.layout.osm_live_banner_list_item, parent, false);
						ColorStateList stateList = AndroidUtils.createPressedColorStateList(app, nightMode,
								R.color.switch_button_active_light, R.color.switch_button_active_stroke_light,
								R.color.switch_button_active_dark, R.color.switch_button_active_stroke_dark);
						CardView cardView = ((CardView) view.findViewById(R.id.card_view));
						cardView.setCardBackgroundColor(stateList);
						cardView.setOnClickListener(v -> {
							FragmentActivity activity = getMyActivity();
							if (activity != null) {
								ChoosePlanFragment.showInstance(activity, OsmAndFeature.HOURLY_MAP_UPDATES);
							}
						});
					} else {
						view = inflater.inflate(R.layout.bottom_sheet_item_with_descr_switch_and_additional_button_56dp, parent, false);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
							view.setBackground(null);
						}
						AndroidUiHelper.setVisibility(View.GONE, view.findViewById(R.id.compound_button));
						((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.ic_action_subscription_osmand_live);
						TextView tvTitle = view.findViewById(R.id.title);
						tvTitle.setText(R.string.download_live_updates);
						AndroidUtils.setTextPrimaryColor(app, tvTitle, nightMode);
						countView = view.findViewById(R.id.description);
						AndroidUtils.setTextSecondaryColor(app, countView, nightMode);
						Drawable additionalIconDrawable = AppCompatResources.getDrawable(app, R.drawable.ic_action_update);
						UiUtilities.tintDrawable(additionalIconDrawable, ContextCompat.getColor(app, getDefaultIconColorId(nightMode)));
						((ImageView) view.findViewById(R.id.additional_button_icon)).setImageDrawable(additionalIconDrawable);
						LinearLayout additionalButton = view.findViewById(R.id.additional_button);
						TypedValue typedValue = new TypedValue();
						app.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
						additionalButton.setBackgroundResource(typedValue.resourceId);
						additionalButton.setOnClickListener(new OnClickListener() {
							@Override
							public void onClick(View v) {
								if (!listAdapter.isShowSubscriptionPurchaseBanner()) {
									showUpdateDialog(getActivity(), getFragmentManager(), UpdatesIndexFragment.this);
								}
							}
						});
					}
				}
			}
			if (viewType == INDEX_ITEM) {
				ItemViewHolder holder = (ItemViewHolder) view.getTag();
				holder.setShowRemoteDate(true);
				holder.setShowTypeInDesc(true);
				holder.setShowParentRegionName(true);
				holder.bindDownloadItem(getItem(position));
			}
			return view;
		}
	}

	@Override
	public void processFinish() {
	}

	@Override
	public List<LocalIndexInfo> getMapsToUpdate() {
		return LiveUpdatesFragment.getMapsToUpdate(listAdapter.mapsList, settings);
	}

	@ColorRes
	public static int getDefaultIconColorId(boolean nightMode) {
		return nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light;
	}
}
