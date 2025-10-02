package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.WidgetType.isComplexWidget;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListDiffCallback.PAYLOAD_DIVIDER_STATE_CHANGED;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListDiffCallback.PAYLOAD_EDIT_MODE_CHANGED;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListDiffCallback.PAYLOAD_MOVE_STATE_CHANGED;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListDiffCallback.PAYLOAD_UPDATE_ALL;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListDiffCallback.PAYLOAD_UPDATE_POSITION;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListFragment.getRowWidgetIds;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListFragment.isFirstPage;
import static net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListFragment.rowHasComplexWidget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.OnItemMoveCallback;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.configure.WidgetIconsHelper;
import net.osmand.plus.views.mapwidgets.configure.panel.holders.AddPageViewHolder;
import net.osmand.plus.views.mapwidgets.configure.panel.holders.DividerViewHolder;
import net.osmand.plus.views.mapwidgets.configure.panel.holders.EmptyStateViewHolder;
import net.osmand.plus.views.mapwidgets.configure.panel.holders.PageViewHolder;
import net.osmand.plus.views.mapwidgets.configure.panel.holders.SpaceViewHolder;
import net.osmand.plus.views.mapwidgets.configure.panel.holders.WidgetViewHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WidgetsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnItemMoveCallback {

	public static final int VIEW_TYPE_PAGE = 0;
	public static final int VIEW_TYPE_WIDGET = 1;
	public static final int VIEW_TYPE_DIVIDER = 2;
	public static final int VIEW_TYPE_ADD_PAGE = 3;
	public static final int VIEW_TYPE_SPACE = 4;
	public static final int VIEW_TYPE_EMPTY_STATE = 5;

	private final List<Object> items = new ArrayList<>();
	private final List<Object> itemsBeforeAction = new ArrayList<>();
	private final OsmandApplication app;
	private final ApplicationMode selectedAppMode;
	private final MapActivity mapActivity;
	private final boolean nightMode;
	private final WidgetsAdapterListener listener;
	private final ItemTouchHelper itemTouchHelper;
	private final boolean isVerticalPanel;
	private final WidgetIconsHelper iconsHelper;

	private boolean isEditMode;

	public interface WidgetsAdapterListener {
		void onPageDeleted(int position, Object item);

		void onWidgetDeleted(int position, Object item);

		void onAddPageClicked();

		void addNewWidget();

		void onWidgetClick(@NonNull MapWidgetInfo widgetInfo);

		boolean isEditMode();

		void onMoveStarted();

		void refreshAll();

		void restoreBackup();
	}

	public WidgetsListAdapter(@NonNull MapActivity mapActivity, boolean nightMode,
	                          @NonNull WidgetsAdapterListener listener, boolean isVerticalPanel, @NonNull ApplicationMode selectedAppMode) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
		this.selectedAppMode = selectedAppMode;
		this.nightMode = nightMode;
		this.listener = listener;
		this.isVerticalPanel = isVerticalPanel;
		this.iconsHelper = new WidgetIconsHelper(app, selectedAppMode.getProfileColor(nightMode), nightMode);

		ItemTouchHelper.Callback callback = new ReorderItemTouchHelperCallback(this);
		this.itemTouchHelper = new ItemTouchHelper(callback);
	}

	public void attachToRecyclerView(RecyclerView recyclerView) {
		itemTouchHelper.attachToRecyclerView(recyclerView);
	}

	@NonNull
	public List<Object> getItems() {
		return items;
	}

	public void setEditMode(boolean editMode) {
		this.isEditMode = editMode;
	}

	public boolean isCurrentlyInEditMode() {
		return isEditMode;
	}

	@NonNull
	public List<Object> getBackupItems() {
		return itemsBeforeAction;
	}

	@Override
	public boolean onItemMove(int fromPosition, int toPosition) {
		if (isFirstPage(toPosition, items)) {
			return false;
		}

		if (isVerticalPanel) {
			List<Object> tempItems = new ArrayList<>(items);

			if (fromPosition < toPosition) {
				for (int i = fromPosition; i < toPosition; i++) {
					Object temp = tempItems.get(i);
					tempItems.set(i, tempItems.get(i + 1));
					tempItems.set(i + 1, temp);
				}
			} else {
				for (int i = fromPosition; i > toPosition; i--) {
					Object temp = tempItems.get(i);
					tempItems.set(i, tempItems.get(i - 1));
					tempItems.set(i - 1, temp);
				}
			}

			if (!isValidPageStructure(tempItems)) {
				return false;
			}
		}

		if (fromPosition < toPosition) {
			for (int i = fromPosition; i < toPosition; i++) {
				swapItems(i, i + 1);
			}
		} else {
			for (int i = fromPosition; i > toPosition; i--) {
				swapItems(i, i - 1);
			}
		}

		notifyItemMoved(fromPosition, toPosition);
		return true;
	}

	private void swapItems(int i, int j) {
		Object temp = items.get(i);
		items.set(i, items.get(j));
		items.set(j, temp);
	}

	private boolean isValidPageStructure(@NonNull List<Object> itemsList) {
		boolean hasComplexWidgetInCurrentPage = false;
		boolean hasRegularWidgetInCurrentPage = false;

		for (Object item : itemsList) {
			if (item instanceof PageItem) {
				hasComplexWidgetInCurrentPage = false;
				hasRegularWidgetInCurrentPage = false;
			} else if (item instanceof WidgetItem widgetItem) {
				if (isComplexWidget(widgetItem.mapWidgetInfo.key)) {
					if (hasComplexWidgetInCurrentPage) {
						return false;
					}
					hasComplexWidgetInCurrentPage = true;
					if (hasRegularWidgetInCurrentPage) {
						return false;
					}
				} else {
					hasRegularWidgetInCurrentPage = true;
					if (hasComplexWidgetInCurrentPage) {
						return false;
					}
				}
			}
		}

		return true;
	}

	@SuppressLint("NotifyDataSetChanged")
	@Override
	public void onItemDismiss(@NonNull RecyclerView.ViewHolder holder) {
		if (isVerticalPanel) {
			Integer multipleComplexIndex = multipleComplexWidgetsInRowIndex(items);
			if (multipleComplexIndex != null) {
				app.showToastMessage(R.string.complex_widget_alert, getComplexWidgetName(multipleComplexIndex, items));
				listener.restoreBackup();
				return;
			}
		}
		listener.refreshAll();
	}

	@Nullable
	private Integer multipleComplexWidgetsInRowIndex(@NonNull List<Object> checkItems) {
		for (int index = 0; index < checkItems.size(); index++) {
			Object item = checkItems.get(index);
			if (item instanceof PageItem && rowHasComplexWidget(index, checkItems) && getRowWidgetIds(index, checkItems).size() > 1) {
				return index;
			}
		}
		return null;
	}

	@Nullable
	private String getComplexWidgetName(int rowPosition, @NonNull List<Object> checkItems) {
		ArrayList<MapWidgetInfo> rowWidgetIds = getRowWidgetIds(rowPosition, checkItems);
		for (MapWidgetInfo widgetUiInfo : rowWidgetIds) {
			if (isComplexWidget(widgetUiInfo.key)) {
				return widgetUiInfo.getTitle(app);
			}
		}
		return null;
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setItems(@NonNull List<Object> items) {
		this.items.clear();
		this.items.addAll(items);
	}

	@NonNull
	public List<List<MapWidgetInfo>> getWidgetsData() {
		List<List<MapWidgetInfo>> result = new ArrayList<>();
		List<MapWidgetInfo> currentPage = null;

		for (Object item : items) {
			if (item instanceof PageItem) {
				if (currentPage != null) {
					result.add(currentPage);
				}
				currentPage = new ArrayList<>();
			} else if (item instanceof WidgetItem widgetItem && currentPage != null) {
				widgetItem.mapWidgetInfo.priority = 0;
				currentPage.add(widgetItem.mapWidgetInfo);
			}
		}

		if (currentPage != null && !currentPage.isEmpty()) {
			result.add(currentPage);
		}

		return result;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context ctx = parent.getContext();
		LayoutInflater inflater = UiUtilities.getInflater(ctx, nightMode);
		return switch (viewType) {
			case VIEW_TYPE_PAGE -> {
				View pageView = inflater.inflate(R.layout.configure_screen_list_item_page_reorder, parent, false);
				yield new PageViewHolder(app, selectedAppMode, pageView);
			}
			case VIEW_TYPE_WIDGET -> {
				View widgetView = inflater.inflate(R.layout.configure_screen_list_item_widget_reorder, parent, false);
				yield new WidgetViewHolder(app, selectedAppMode, widgetView);
			}
			case VIEW_TYPE_DIVIDER -> {
				View dividerView = inflater.inflate(R.layout.list_item_divider, parent, false);
				yield new DividerViewHolder(dividerView);
			}
			case VIEW_TYPE_ADD_PAGE -> {
				View addPageView = inflater.inflate(R.layout.configure_screen_list_item_add_page, parent, false);
				yield new AddPageViewHolder(addPageView);
			}
			case VIEW_TYPE_SPACE -> new SpaceViewHolder(new View(mapActivity));
			case VIEW_TYPE_EMPTY_STATE -> {
				View emptyStateView = inflater.inflate(R.layout.widgets_list_empty_state, parent, false);
				yield new EmptyStateViewHolder(emptyStateView);
			}
			default -> throw new IllegalArgumentException("Unknown view type: " + viewType);
		};
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object item = items.get(position);

		if (holder instanceof PageViewHolder pageViewHolder) {
			pageViewHolder.bind(mapActivity, listener, itemTouchHelper, this, position, (PageItem) item, isVerticalPanel, nightMode);
		} else if (holder instanceof WidgetViewHolder widgetViewHolder) {
			widgetViewHolder.bind(mapActivity, selectedAppMode, listener, itemTouchHelper, (WidgetItem) item, iconsHelper, position, nightMode);
		} else if (holder instanceof AddPageViewHolder addPageViewHolder) {
			addPageViewHolder.bind(app, listener, isVerticalPanel, selectedAppMode, nightMode);
		} else if (holder instanceof SpaceViewHolder spaceViewHolder) {
			spaceViewHolder.bind(mapActivity);
		} else if (holder instanceof EmptyStateViewHolder emptyStateViewHolder) {
			emptyStateViewHolder.bind(app, listener, nightMode);
		} else if(holder instanceof DividerViewHolder dividerViewHolder){
			dividerViewHolder.bind();
		}
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (payloads.isEmpty()) {
			super.onBindViewHolder(holder, position, payloads);
			return;
		}
		for (Object payload : payloads) {
			if (payload instanceof List<?> changes) {
				onPayload(changes, holder, position);

				return;
			}
		}
		super.onBindViewHolder(holder, position, payloads);
	}

	private void onPayload(List<?> changes, @NonNull RecyclerView.ViewHolder holder, int position){
		Object item = items.get(position);

		boolean editModeChanged = changes.contains(PAYLOAD_EDIT_MODE_CHANGED);
		boolean moveStateChanged = changes.contains(PAYLOAD_MOVE_STATE_CHANGED);
		boolean dividerChanged = changes.contains(PAYLOAD_DIVIDER_STATE_CHANGED);
		boolean updateAll = changes.contains(PAYLOAD_UPDATE_ALL);
		boolean updatePosition = changes.contains(PAYLOAD_UPDATE_POSITION);

		if (holder instanceof PageViewHolder pageViewHolder) {
			if (updateAll) {
				pageViewHolder.bind(mapActivity, listener, itemTouchHelper, this, position, (PageItem) item, isVerticalPanel, nightMode);
				return;
			}
			if (moveStateChanged || updatePosition) {
				pageViewHolder.updateButtons(app, listener, (PageItem) item, position, nightMode);
			}
			if (editModeChanged) {
				pageViewHolder.updateEditMode(listener, itemTouchHelper);
			}
			if (updatePosition) {
				pageViewHolder.updateTitle(app, (PageItem) item, isVerticalPanel);
			}

		} else if (holder instanceof WidgetViewHolder widgetViewHolder) {
			if (updateAll) {
				widgetViewHolder.bind(mapActivity, selectedAppMode, listener, itemTouchHelper, (WidgetItem) item, iconsHelper, position, nightMode);
				return;
			}
			if (updatePosition) {
				widgetViewHolder.updatePosition(listener, position, (WidgetItem) item);
			}
			if (dividerChanged) {
				widgetViewHolder.updateDivider((WidgetItem) item);
			}
			if (editModeChanged) {
				widgetViewHolder.updateEditMode(app, listener, itemTouchHelper, selectedAppMode, (WidgetItem) item, nightMode);
			}
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);

		if (item instanceof PageItem) {
			return VIEW_TYPE_PAGE;
		} else if (item instanceof WidgetItem) {
			return VIEW_TYPE_WIDGET;
		} else if (item instanceof Integer integer) {
			return integer;
		}

		return -1;
	}

	public void actionStarted() {
		if (isVerticalPanel) {
			itemsBeforeAction.clear();
			itemsBeforeAction.addAll(items);
		}
	}

	public void removeItem(int position) {
		if (position >= 0 && position < items.size()) {
			if (isVerticalPanel) {
				List<Object> tempItems = new ArrayList<>(items);
				tempItems.remove(position);
				Integer multipleComplexIndex = multipleComplexWidgetsInRowIndex(tempItems);
				if (multipleComplexIndex != null) {
					app.showToastMessage(R.string.complex_widget_alert, getComplexWidgetName(multipleComplexIndex, tempItems));
					return;
				}
			}
			items.remove(position);
			notifyItemRemoved(position);
			listener.refreshAll();
		}
	}

	public void addPage() {
		int page = getLastPage();
		page++;
		insertToEndOfAddedWidgets(new PageItem(page));
	}

	public void addWidget(@NonNull MapWidgetInfo widgetInfo) {
		int page = getLastPage();
		if (isVerticalPanel) {
			page++;
			insertToEndOfAddedWidgets(new PageItem(page));
		}

		insertToEndOfAddedWidgets(new WidgetItem(widgetInfo));
	}

	@SuppressLint("NotifyDataSetChanged")
	private void insertToEndOfAddedWidgets(@NonNull Object listItem) {
		int insertIndex = getIndexOfLastWidgetOrPageItem() + 1;
		items.add(insertIndex, listItem);
		notifyItemInserted(insertIndex);
		listener.refreshAll();
	}

	private int getIndexOfLastWidgetOrPageItem() {
		int index = -1;
		for (int i = 0; i < items.size(); i++) {
			Object item = items.get(i);
			boolean suitableIndex = (index == -1 && item instanceof Integer integer && VIEW_TYPE_DIVIDER == integer)
					|| item instanceof PageItem || item instanceof WidgetItem;
			if (suitableIndex) {
				index = i;
			}
		}

		return index;
	}

	private int getLastPage() {
		int maximumPageIndex = 0;
		for (Object item : items) {
			if (item instanceof PageItem pageItem && pageItem.pageNumber > maximumPageIndex) {
				maximumPageIndex = pageItem.pageNumber;
			}
		}
		return maximumPageIndex;
	}

	public static class PageItem {
		public int pageNumber;
		public boolean movable = false;
		public String deleteMessage = null;

		public PageItem(int pageNumber) {
			this.pageNumber = pageNumber;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;

			PageItem that = (PageItem) obj;

			return pageNumber == that.pageNumber &&
					movable == that.movable &&
					Objects.equals(deleteMessage, that.deleteMessage);
		}

		@Override
		public int hashCode() {
			return Objects.hash(pageNumber, movable, deleteMessage);
		}
	}

	public static class WidgetItem {
		public MapWidgetInfo mapWidgetInfo;
		public boolean showBottomDivider = false;
		public boolean showBottomShadow = false;

		public WidgetItem(@NonNull MapWidgetInfo mapWidgetInfo) {
			this.mapWidgetInfo = mapWidgetInfo;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) return false;

			WidgetItem that = (WidgetItem) obj;

			return showBottomDivider == that.showBottomDivider &&
					showBottomShadow == that.showBottomShadow &&
					Objects.equals(mapWidgetInfo, that.mapWidgetInfo);
		}

		@Override
		public int hashCode() {
			return Objects.hash(mapWidgetInfo, showBottomDivider, showBottomShadow);
		}
	}
}
