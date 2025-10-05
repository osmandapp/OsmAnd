package net.osmand.plus.download.ui;

import static net.osmand.plus.download.local.OperationType.BACKUP_OPERATION;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;

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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Collator;
import net.osmand.OsmAndCollator;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadItem;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.MultipleDownloadItem;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class UpdatesIndexFragment extends BaseOsmAndFragment implements DownloadEvents,
		OperationListener, ConfirmDeletionListener, RefreshLiveUpdates, LiveUpdateListener, InAppPurchaseListener {
	private static final int RELOAD_ID = 5;

	private UpdatesAdapter adapter;
	private String errorMessage;
	private LoadLiveMapsTask loadLiveMapsTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		invalidateListView();
		startLoadLiveMapsAsyncTask();
		setHasOptionsMenu(true);
		DeleteConfirmationDialogController.askUpdateListener(app, this);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();

		View view = inflater.inflate(R.layout.update_index_frament, container, false);
		app = (OsmandApplication) requireActivity().getApplication();

		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerView.setAdapter(adapter);

		requireMyActivity().getAccessibilityAssistant().registerPage(view, DownloadActivity.UPDATES_TAB_NUMBER);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		updateErrorMessage();
	}

	@Override
	public void downloadHasFinished() {
		invalidateListView();
		updateUpdateAllButton();
		startLoadLiveMapsAsyncTask();
	}

	@Override
	public void downloadInProgress() {
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onUpdatedIndexesList() {
		invalidateListView();
		updateUpdateAllButton();
	}

	public void invalidateListView() {
		DownloadResources indexes = app.getDownloadThread().getIndexes();
		List<DownloadItem> downloadItems = indexes.getGroupedItemsToUpdate();

		OsmandRegions osmandRegions = app.getResourceManager().getOsmandRegions();
		List<DownloadItem> items = new ArrayList<>(indexes.getGroupedItemsToUpdate());

		Collator collator = OsmAndCollator.primaryCollator();
		items.sort((o1, o2) -> collator.compare(
				o1.getVisibleName(app, osmandRegions),
				o2.getVisibleName(app, osmandRegions)
		));

		if (adapter == null) {
			boolean showBanner = !InAppPurchaseUtils.isLiveUpdatesAvailable(app)
					|| settings.SHOULD_SHOW_FREE_VERSION_BANNER.get();
			adapter = new UpdatesAdapter(requireContext(), items, showBanner);
		} else {
			adapter.setVisibleItems(downloadItems);
		}
		updateErrorMessage();
	}

	private void updateErrorMessage() {
		View view = getView();
		if (view == null) return;

		DownloadResources indexes = app.getDownloadThread().getIndexes();
		List<DownloadItem> downloadItems = indexes.getGroupedItemsToUpdate();
		if (adapter != null && downloadItems.isEmpty()) {
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
		List<IndexItem> indexItems = indexes.getIndividualItemsToUpdate();
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
		loadLiveMapsTask = new LoadLiveMapsTask(adapter, app);
		OsmAndTaskManager.executeTask(loadLiveMapsTask);
	}

	private void stopLoadLiveMapsAsyncTask() {
		if (loadLiveMapsTask != null && loadLiveMapsTask.getStatus() == AsyncTask.Status.RUNNING) {
			loadLiveMapsTask.cancel(false);
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
			invalidateListView();
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

	private class UpdatesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements LocalIndexInfoAdapter  {

		private static final int TYPE_MULTIPLE_DOWNLOAD = 0;
		private static final int TYPE_INDEX_ITEM = 1;
		private static final int TYPE_OSM_LIVE_BANNER = 2;

		private final Context context;
		private final List<Object> visibleItems = new ArrayList<>();
		private final List<LocalItem> localItems = new ArrayList<>();
		private final Set<String> nestedItemsKeys = new HashSet<>();
		private final Set<String> expandedKeys = new HashSet<>();
		private final boolean showSubscriptionPurchaseBanner;

		public UpdatesAdapter(Context context, List<DownloadItem> items, boolean showSubscriptionPurchaseBanner) {
			this.context = context;
			this.showSubscriptionPurchaseBanner = showSubscriptionPurchaseBanner;
			setVisibleItems(items);
		}

		public void setVisibleItems(@NonNull List<DownloadItem> items) {
			visibleItems.clear();
			nestedItemsKeys.clear();
			visibleItems.add(TYPE_OSM_LIVE_BANNER);
			for (DownloadItem downloadItem : items) {
				if (downloadItem instanceof MultipleDownloadItem mdi) {
					visibleItems.add(mdi);
					if (isCategoryExpanded(mdi)) {
						visibleItems.addAll(mdi.getAllIndexes());
					}
					for (IndexItem indexItem : mdi.getAllIndexes()) {
						nestedItemsKeys.add(getIndexItemId(indexItem));
					}
				} else if (downloadItem instanceof IndexItem indexItem) {
					visibleItems.add(indexItem);
				}
			}
			notifyDataSetChanged();
		}

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

		public boolean isShowSubscriptionPurchaseBanner() {
			return showSubscriptionPurchaseBanner;
		}

		@Override
		public int getItemViewType(int position) {
			Object item = visibleItems.get(position);
			if (position == 0) {
				return TYPE_OSM_LIVE_BANNER;
			} else if (item instanceof MultipleDownloadItem) {
				return TYPE_MULTIPLE_DOWNLOAD;
			} else {
				return TYPE_INDEX_ITEM;
			}
		}

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater inflater = getThemedInflater();
			if (viewType == TYPE_OSM_LIVE_BANNER) {
				View v = inflater.inflate(showSubscriptionPurchaseBanner
						? R.layout.osm_subscription_banner_list_item
						: R.layout.bottom_sheet_item_with_descr_switch_and_additional_button_56dp,
						parent, false);
				return new OsmLiveBannerVH(v, showSubscriptionPurchaseBanner);
			} else if (viewType == TYPE_MULTIPLE_DOWNLOAD) {
				View v = inflater.inflate(R.layout.two_line_with_images_list_item, parent, false);
				return new MultiItemVH(v);
			} else {
				View v = inflater.inflate(R.layout.two_line_with_images_list_item, parent, false);
				return new IndexItemVH(v);
			}
		}

		@Override
		public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
			Object item = visibleItems.get(position);
			if (holder instanceof MultiItemVH multiItemVH && item instanceof MultipleDownloadItem mdi) {
				multiItemVH.bindView(mdi);
			} else if (holder instanceof IndexItemVH indexItemVH && item instanceof IndexItem indexItem) {
				indexItemVH.bindView(indexItem);
			}
		}

		@Override
		public int getItemCount() {
			return visibleItems.size();
		}

		class OsmLiveBannerVH extends RecyclerView.ViewHolder {

			OsmLiveBannerVH(View v, boolean showSubscriptionPurchaseBanner) {
				super(v);
				if (showSubscriptionPurchaseBanner) {
					ColorStateList stateList = AndroidUtils.createPressedColorStateList(app, nightMode,
							R.color.switch_button_active_light, R.color.switch_button_active_stroke_light,
							R.color.switch_button_active_dark, R.color.switch_button_active_stroke_dark);
					CardView cardView = v.findViewById(R.id.card_view);
					cardView.setCardBackgroundColor(stateList);
					cardView.setOnClickListener(click -> callActivity(activity ->
							ChoosePlanFragment.showInstance((FragmentActivity) context, OsmAndFeature.HOURLY_MAP_UPDATES))
					);
				} else {
					v.setBackground(null);
					v.findViewById(R.id.compound_button).setVisibility(View.GONE);
					AndroidUiHelper.setVisibility(View.GONE, v.findViewById(R.id.compound_button));
					((ImageView) v.findViewById(R.id.icon)).setImageResource(R.drawable.ic_action_subscription_osmand_live);

					TextView tvTitle = v.findViewById(R.id.title);
					tvTitle.setText(R.string.download_live_updates);

					AndroidUtils.setTextPrimaryColor(app, tvTitle, nightMode);
					TextView countView = v.findViewById(R.id.description);
					AndroidUtils.setTextSecondaryColor(app, countView, nightMode);

					Drawable additionalIconDrawable = getContentIcon(R.drawable.ic_action_update);
					((ImageView) v.findViewById(R.id.additional_button_icon)).setImageDrawable(additionalIconDrawable);
					LinearLayout additionalButton = v.findViewById(R.id.additional_button);
					TypedValue typedValue = new TypedValue();
					app.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
					additionalButton.setBackgroundResource(typedValue.resourceId);

					OnClickListener onClickListener = click -> callActivity(activity -> {
						if (!adapter.isShowSubscriptionPurchaseBanner()) {
							LiveUpdatesFragment.showInstance(activity.getSupportFragmentManager(), UpdatesIndexFragment.this);
						}
					});
					additionalButton.setOnClickListener(onClickListener);
					v.setOnClickListener(onClickListener);
				}
			}
		}

		class MultiItemVH extends RecyclerView.ViewHolder {
			TextView title;
			ImageView expandIcon;

			MultiItemVH(@NonNull View itemView) {
				super(itemView);
				title = itemView.findViewById(R.id.title);
				expandIcon = itemView.findViewById(R.id.expandIcon);
				expandIcon.setVisibility(View.VISIBLE);
				itemView.findViewById(R.id.expand_button_divider).setVisibility(View.VISIBLE);
				itemView.setTag(new ItemViewHolder(itemView, requireMyActivity()));
				itemView.setOnClickListener(v -> toggleExpanded(getAdapterPosition()));
			}

			void bindView(@NonNull MultipleDownloadItem downloadItem) {
				ItemViewHolder holder = (ItemViewHolder) itemView.getTag();
				holder.setShowRemoteDate(true);
				holder.setShowTypeInDesc(true);
				holder.setShowParentRegionName(true);
				holder.setUpdatesMode(true);
				holder.bindDownloadItem(downloadItem);
				int indicatorIconId = isCategoryExpanded(downloadItem)
						? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down;
				expandIcon.setImageDrawable(getContentIcon(indicatorIconId));
			}
		}

		class IndexItemVH extends RecyclerView.ViewHolder {

			IndexItemVH(@NonNull View itemView) {
				super(itemView);
				itemView.setTag(new ItemViewHolder(itemView, requireMyActivity()));
			}

			void bindView(@NonNull IndexItem item) {
				ItemViewHolder holder = (ItemViewHolder) itemView.getTag();
				holder.setShowRemoteDate(true);
				holder.setShowTypeInDesc(true);
				holder.setShowParentRegionName(true);
				holder.setShowStartIcon(!isNestedItem(item));
				holder.setUpdatesMode(true);
				holder.bindDownloadItem(item);
				itemView.setOnClickListener(v -> {
					ItemViewHolder vh = (ItemViewHolder) v.getTag();
					OnClickListener ls = vh.getRightButtonAction(item, vh.getClickAction(item));
					ls.onClick(v);
				});
				itemView.setOnLongClickListener(v -> {
					LocalItem localItem = item.toLocalItem(app);
					if (localItem != null) {
						askShowContextMenu(v, item, localItem);
						return true;
					}
					return false;
				});
			}
		}

		private void toggleExpanded(int position) {
			Object item = visibleItems.get(position);
			if (!(item instanceof MultipleDownloadItem mdi)) return;

			if (isCategoryExpanded(mdi)) {
				visibleItems.removeAll(mdi.getAllIndexes());
				setCategoryExpanded(mdi, false);
			} else {
				int insertPos = position + 1;
				List<IndexItem> children = mdi.getAllIndexes();
				visibleItems.addAll(insertPos, children);
				setCategoryExpanded(mdi, true);
			}
			notifyDataSetChanged();
		}

		private boolean isCategoryExpanded(@NonNull MultipleDownloadItem mdi) {
			return expandedKeys.contains(getCategoryId(mdi));
		}

		private void setCategoryExpanded(@NonNull MultipleDownloadItem mdi, boolean expanded) {
			String key = getCategoryId(mdi);
			if (expanded) {
				expandedKeys.add(key);
			} else {
				expandedKeys.remove(key);
			}
		}

		private boolean isNestedItem(@NonNull IndexItem indexItem) {
			return nestedItemsKeys.contains(getIndexItemId(indexItem));
		}

		@NonNull
		private String getCategoryId(@NonNull MultipleDownloadItem mdi) {
			return mdi.getType().getTag()  + "_" + mdi.getRelatedRegion().getRegionId();
		}

		@NonNull
		private String getIndexItemId(@NonNull IndexItem indexItem) {
			return indexItem.getBasename();
		}
	}

	@Override
	public List<LocalItem> getMapsToUpdate() {
		return LiveUpdatesFragment.getMapsToUpdate(adapter.localItems, settings);
	}
}
