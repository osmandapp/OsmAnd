package net.osmand.plus.download.ui;

import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;
import static net.osmand.plus.liveupdates.LiveUpdatesFragment.showUpdateDialog;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseNestedListFragment;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalItemType;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.download.local.dialogs.DeleteConfirmationDialogController;
import net.osmand.plus.download.local.dialogs.DeleteConfirmationDialogController.ConfirmDeletionListener;
import net.osmand.plus.download.local.dialogs.LocalItemFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.liveupdates.LiveUpdatesClearBottomSheet.RefreshLiveUpdates;
import net.osmand.plus.liveupdates.LiveUpdatesFragment;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.LiveUpdateListener;
import net.osmand.plus.liveupdates.LoadLiveMapsTask;
import net.osmand.plus.liveupdates.LoadLiveMapsTask.LocalIndexInfoAdapter;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.plus.widgets.popup.PopUpMenuWidthMode;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UpdatesIndexFragment extends BaseNestedListFragment implements DownloadEvents,
		OperationListener, ConfirmDeletionListener, RefreshLiveUpdates, LiveUpdateListener, InAppPurchaseListener {
	private static final int RELOAD_ID = 5;
	private UpdateIndexAdapter listAdapter;
	private String errorMessage;
	private LoadLiveMapsTask loadLiveMapsTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		invalidateListView(requireContext());
		startLoadLiveMapsAsyncTask();
		setHasOptionsMenu(true);
		DeleteConfirmationDialogController.askUpdateListener(app, this);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.update_index_frament, container, false);
		requireMyActivity().getAccessibilityAssistant().registerPage(view, DownloadActivity.UPDATES_TAB_NUMBER);
		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setupOnItemLongClickListener();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		updateErrorMessage();
	}

	private void setupOnItemLongClickListener() {
		getListView().setOnItemLongClickListener((parent, v, position, id) -> {
			if (position > 0) {
				IndexItem indexItem = (IndexItem) getListAdapter().getItem(position);
				LocalItem localItem = indexItem.toLocalItem(app);
				if (localItem != null) {
					askShowContextMenu(v, indexItem, localItem);
					return true;
				}
			}
			return false;
		});
	}

	@Override
	public ArrayAdapter<?> getAdapter() {
		return listAdapter;
	}

	@Override
	public void downloadHasFinished() {
		invalidateListView(requireMyActivity());
		updateUpdateAllButton();
		startLoadLiveMapsAsyncTask();
	}

	@Override
	public void downloadInProgress() {
		listAdapter.notifyDataSetChanged();
	}

	@Override
	public void onUpdatedIndexesList() {
		invalidateListView(requireMyActivity());
		updateUpdateAllButton();
	}

	public void invalidateListView(@NonNull Context context) {
		DownloadResources indexes = app.getDownloadThread().getIndexes();
		List<IndexItem> indexItems = indexes.getItemsToUpdate();

		OsmandRegions osmandRegions = app.getResourceManager().getOsmandRegions();
		listAdapter = new UpdateIndexAdapter(context, R.layout.download_index_list_item, indexItems,
				!InAppPurchaseUtils.isLiveUpdatesAvailable(app) || settings.SHOULD_SHOW_FREE_VERSION_BANNER.get());
		Collator collator = OsmAndCollator.primaryCollator();
		listAdapter.sort((indexItem, indexItem2) -> collator.compare(indexItem.getVisibleName(app, osmandRegions),
				indexItem2.getVisibleName(app, osmandRegions)));
		setListAdapter(listAdapter);
		updateErrorMessage();
	}

	private void updateErrorMessage() {
		View view = getView();
		if (view == null) return;

		DownloadResources indexes = app.getDownloadThread().getIndexes();
		List<IndexItem> indexItems = indexes.getItemsToUpdate();
		if (getListAdapter() != null && indexItems.isEmpty()) {
			errorMessage = getString(indexes.isDownloadedFromInternet
					? R.string.everything_up_to_date
					: R.string.no_index_file_to_download);
		} else {
			errorMessage = null;
		}
		updateUpdateAllButton();
	}

	private void updateUpdateAllButton() {
		View view = getView();
		if (view == null) return;

		DownloadResources indexes = requireMyActivity().getDownloadThread().getIndexes();
		List<IndexItem> indexItems = indexes.getItemsToUpdate();
		TextView updateAllButton = view.findViewById(R.id.updateAllButton);
		if (indexItems.isEmpty() || indexItems.get(0).getType() == null) {
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
			updateAllButton.setOnClickListener(v -> {
				DownloadActivity activity = getMyActivity();
				if (AndroidUtils.isActivityNotDestroyed(activity)) {
					if (indexItems.size() > 3) {
						AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
						dialog.setTitle(R.string.update_all_maps);
						dialog.setMessage(getString(R.string.update_all_maps_q, indexItems.size()));
						dialog.setNegativeButton(R.string.shared_string_cancel, null);
						dialog.setPositiveButton(R.string.shared_string_update, (d, which) -> activity.startDownload(indexItems.toArray(new IndexItem[0])));
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

	private void startLoadLiveMapsAsyncTask() {
		loadLiveMapsTask = new LoadLiveMapsTask(listAdapter, app);
		OsmAndTaskManager.executeTask(loadLiveMapsTask);
	}

	private void stopLoadLiveMapsAsyncTask() {
		if (loadLiveMapsTask != null && loadLiveMapsTask.getStatus() == AsyncTask.Status.RUNNING) {
			loadLiveMapsTask.cancel(false);
		}
	}

	@Override
	public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
		if (position == 0) {
			callActivity(activity -> {
				if (!listAdapter.isShowSubscriptionPurchaseBanner()) {
					LiveUpdatesFragment.showInstance(activity.getSupportFragmentManager(), this);
				}
			});
		} else {
			IndexItem e = (IndexItem) getListAdapter().getItem(position);
			ItemViewHolder vh = (ItemViewHolder) v.getTag();
			OnClickListener ls = vh.getRightButtonAction(e, vh.getClickAction(e));
			ls.onClick(v);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		ActionBar actionBar = requireMyActivity().getSupportActionBar();
		if (actionBar != null) {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		}
		if (app.getAppCustomization().showDownloadExtraActions()) {
			int colorResId = ColorUtilities.getActiveButtonsAndLinksTextColorId(nightMode);
			MenuItem item = menu.add(0, RELOAD_ID, 0, R.string.shared_string_refresh);
			Drawable icRefresh = getIcon(R.drawable.ic_action_refresh_dark, colorResId);
			item.setIcon(icRefresh);
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == RELOAD_ID) {
			// re-create the thread
			requireMyActivity().getDownloadThread().runReloadIndexFiles();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void askShowContextMenu(@NonNull View view, @NonNull IndexItem indexItem,
	                                @NonNull LocalItem localItem) {
		callActivity(activity -> showContextMenu(activity, view, indexItem, localItem));
	}

	private void showContextMenu(@NonNull FragmentActivity activity, @NonNull View view,
	                             @NonNull IndexItem indexItem, @NonNull LocalItem localItem) {
		List<PopUpMenuItem> items = new ArrayList<>();

		items.add(new PopUpMenuItem.Builder(activity)
				.setTitleId(R.string.info_button)
				.setIcon(getContentIcon(R.drawable.ic_action_info_outlined))
				.setOnClickListener(v -> showInfoScreen(localItem))
				.create());

		LocalItemType type = localItem.getType();
		if (type.isUpdateSupported()) {
			items.add(new PopUpMenuItem.Builder(activity)
					.setTitleId(R.string.shared_string_update)
					.setIcon(getContentIcon(R.drawable.ic_action_update))
					.setOnClickListener(v -> updateItem(indexItem))
					.create());
		}

		boolean backuped = localItem.isBackuped(app);
		if (type.isBackupSupported() || backuped) {
			OperationType operationType = backuped ? RESTORE_OPERATION : BACKUP_OPERATION;
			items.add(new PopUpMenuItem.Builder(activity)
					.setTitleId(operationType.getTitleId())
					.setIcon(getContentIcon(operationType.getIconId()))
					.setOnClickListener(v -> performOperation(operationType, localItem))
					.create());
		}

		if (type.isDeletionSupported()) {
			items.add(new PopUpMenuItem.Builder(activity)
					.setTitleId(R.string.shared_string_remove)
					.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
					.setOnClickListener(v -> showDeleteConfirmationDialog(localItem))
					.showTopDivider(!Algorithms.isEmpty(items))
					.create());
		}

		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = view;
		displayData.menuItems = items;
		displayData.nightMode = nightMode;
		displayData.widthMode = PopUpMenuWidthMode.STANDARD;
		PopUpMenu.show(displayData);
	}

	private void showInfoScreen(@NonNull LocalItem localItem) {
		callActivity(activity -> {
			FragmentManager manager = activity.getSupportFragmentManager();
			LocalItemFragment.showInstance(manager, localItem, null);
		});
	}

	private void updateItem(@NonNull IndexItem indexItem) {
		DownloadActivity activity = getMyActivity();
		if (activity != null) {
			activity.startDownload(indexItem);
		}
	}

	private void showDeleteConfirmationDialog(@NonNull LocalItem localItem) {
		callActivity(activity -> {
			FragmentManager manager = activity.getSupportFragmentManager();
			DeleteConfirmationDialogController.showDialog(app, manager, localItem, this);
		});
	}

	@Override
	public void onDeletionConfirmed(@NonNull BaseLocalItem localItem) {
		performOperation(OperationType.DELETE_OPERATION, localItem);
	}

	public void performOperation(@NonNull OperationType type, @NonNull BaseLocalItem... items) {
		OsmAndTaskManager.executeTask(new LocalOperationTask(app, type, this), items);
	}

	@Override
	public void onOperationStarted() {
		updateProgressVisibility(true);
	}

	@Override
	public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
		updateProgressVisibility(false);
		if (!Algorithms.isEmpty(result)) {
			app.showToastMessage(result);
		}
		DownloadActivity activity = getMyActivity();
		if (AndroidUtils.isActivityNotDestroyed(activity)) {
			if (CollectionUtils.equalsToAny(type, RESTORE_OPERATION, BACKUP_OPERATION)) {
				activity.getDownloadThread().runReloadIndexFiles();
			} else {
				activity.onUpdatedIndexesList();
			}
		}
	}

	protected void updateProgressVisibility(boolean visible) {
		DownloadActivity activity = getMyActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	@Override
	public void onUpdateStates(Context context) {
		if (context instanceof OsmandApplication) {
			startLoadLiveMapsAsyncTask();
		}
	}

	@Override
	public void onItemPurchased(String sku, boolean active) {
		callActivity(activity -> {
			invalidateListView(activity);
			updateUpdateAllButton();
			startLoadLiveMapsAsyncTask();
		});
	}

	@NonNull
	public DownloadActivity requireMyActivity() {
		return Objects.requireNonNull(getMyActivity());
	}

	@Nullable
	public DownloadActivity getMyActivity() {
		return (DownloadActivity) getActivity();
	}

	private class UpdateIndexAdapter extends ArrayAdapter<IndexItem> implements LocalIndexInfoAdapter {

		private static final int INDEX_ITEM = 0;
		private static final int OSM_LIVE_BANNER = 1;

		private final List<LocalItem> localItems = new ArrayList<>();
		private final boolean showSubscriptionPurchaseBanner;

		@Override
		public void addData(@NonNull List<LocalItem> indexes) {
			localItems.addAll(indexes);
			notifyDataSetChanged();
		}

		@Override
		public void clearData() {
			localItems.clear();
			notifyDataSetChanged();
		}

		@Override
		public void onDataUpdated() {

		}

		public UpdateIndexAdapter(Context context, int resource, List<IndexItem> items, boolean showSubscriptionPurchaseBanner) {
			super(context, resource, items);
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

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			View view = convertView;
			int viewType = getItemViewType(position);
			if (view == null) {
				if (viewType == INDEX_ITEM) {
					view = inflate(R.layout.two_line_with_images_list_item, parent, false);
					view.setTag(new ItemViewHolder(view, requireMyActivity()));
				} else if (viewType == OSM_LIVE_BANNER) {
					if (showSubscriptionPurchaseBanner) {
						view = inflate(R.layout.osm_subscription_banner_list_item, parent, false);
						ColorStateList stateList = AndroidUtils.createPressedColorStateList(app, nightMode,
								R.color.switch_button_active_light, R.color.switch_button_active_stroke_light,
								R.color.switch_button_active_dark, R.color.switch_button_active_stroke_dark);
						CardView cardView = view.findViewById(R.id.card_view);
						cardView.setCardBackgroundColor(stateList);
						cardView.setOnClickListener(v -> callActivity(activity ->
								ChoosePlanFragment.showInstance(activity, OsmAndFeature.HOURLY_MAP_UPDATES))
						);
					} else {
						view = inflate(R.layout.bottom_sheet_item_with_descr_switch_and_additional_button_56dp, parent, false);
						view.setBackground(null);
						AndroidUiHelper.setVisibility(View.GONE, view.findViewById(R.id.compound_button));
						((ImageView) view.findViewById(R.id.icon)).setImageResource(R.drawable.ic_action_subscription_osmand_live);
						TextView tvTitle = view.findViewById(R.id.title);
						tvTitle.setText(R.string.download_live_updates);
						AndroidUtils.setTextPrimaryColor(app, tvTitle, nightMode);
						TextView countView = view.findViewById(R.id.description);
						AndroidUtils.setTextSecondaryColor(app, countView, nightMode);
						Drawable additionalIconDrawable = getContentIcon(R.drawable.ic_action_update);
						((ImageView) view.findViewById(R.id.additional_button_icon)).setImageDrawable(additionalIconDrawable);
						LinearLayout additionalButton = view.findViewById(R.id.additional_button);
						TypedValue typedValue = new TypedValue();
						app.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
						additionalButton.setBackgroundResource(typedValue.resourceId);
						additionalButton.setOnClickListener(v -> {
							if (!listAdapter.isShowSubscriptionPurchaseBanner()) {
								showUpdateDialog(getActivity(), getFragmentManager(), UpdatesIndexFragment.this);
							}
						});
					}
				}
			}
			if (viewType == INDEX_ITEM) {
				IndexItem indexItem = Objects.requireNonNull(getItem(position));
				ItemViewHolder holder = (ItemViewHolder) view.getTag();
				holder.setShowRemoteDate(true);
				holder.setShowTypeInDesc(true);
				holder.setShowParentRegionName(true);
				holder.bindDownloadItem(indexItem);
			}
			return view;
		}
	}

	@Override
	public void processFinish() {
	}

	@Override
	public List<LocalItem> getMapsToUpdate() {
		return LiveUpdatesFragment.getMapsToUpdate(listAdapter.localItems, settings);
	}
}
