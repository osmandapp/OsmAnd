package net.osmand.plus.views.mapwidgets.configure.panel;

import static net.osmand.plus.views.mapwidgets.WidgetType.isComplexWidget;

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
import net.osmand.plus.views.mapwidgets.configure.panel.holders.ShadowViewHolder;
import net.osmand.plus.views.mapwidgets.configure.panel.holders.SpaceViewHolder;
import net.osmand.plus.views.mapwidgets.configure.panel.holders.WidgetViewHolder;

import java.util.ArrayList;
import java.util.List;

public class WidgetsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements OnItemMoveCallback {

	private static final int VIEW_TYPE_PAGE = 0;
	private static final int VIEW_TYPE_WIDGET = 1;
	private static final int VIEW_TYPE_DIVIDER = 2;
	private static final int VIEW_TYPE_ADD_PAGE = 3;
	private static final int VIEW_TYPE_SPACE = 4;
	private static final int VIEW_TYPE_EMPTY_STATE = 5;
	private static final int VIEW_TYPE_SHADOW = 6;

	private final List<Object> items = new ArrayList<>();
	private final List<Object> itemsBeforeAction = new ArrayList<>();
	private final OsmandApplication app;
	private final MapActivity mapActivity;
	private final boolean nightMode;
	private final WidgetsAdapterListener listener;
	private final ItemTouchHelper itemTouchHelper;
	private final boolean isVerticalPanel;
	private final WidgetIconsHelper iconsHelper;

	public interface WidgetsAdapterListener {
		void onPageDeleted(int position, Object item);

		void onWidgetDeleted(int position, Object item);

		void onAddPageClicked();

		void addNewWidget();

		void onWidgetClick(@NonNull MapWidgetInfo widgetInfo);

		boolean isEditMode();

		void onMoveStarted();
	}

	public WidgetsListAdapter(@NonNull MapActivity mapActivity, boolean nightMode,
	                          @NonNull WidgetsAdapterListener listener, boolean isVerticalPanel, @NonNull ApplicationMode selectedAppMode) {
		this.mapActivity = mapActivity;
		this.app = mapActivity.getMyApplication();
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

	public List<Object> getItems() {
		return items;
	}

	@Override
	public boolean onItemMove(int fromPosition, int toPosition) {
		if (isFirstPage(toPosition)) {
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
			} else if (item instanceof MapWidgetInfo widget) {
				if (isComplexWidget(widget.key)) {
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
				app.showToastMessage(app.getString(R.string.complex_widget_alert, getComplexWidgetName(multipleComplexIndex, items)));
				items.clear();
				items.addAll(itemsBeforeAction);
			}
		}
		updatePageIndexes();
		notifyDataSetChanged();
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

	private void updatePageIndexes() {
		int pageNumber = 1;
		for (Object item : items) {
			if (item instanceof PageItem pageItem) {
				pageItem.pageNumber = pageNumber;
				pageNumber++;
			}
		}
	}

	@SuppressLint("NotifyDataSetChanged")
	private void updateItemsForEditMode(List<List<MapWidgetInfo>> pagedWidgets) {
		boolean isEmpty = pagedWidgets.stream().allMatch(List::isEmpty);
		if (isEmpty) {
			items.clear();
			items.add(VIEW_TYPE_DIVIDER);
			items.add(VIEW_TYPE_EMPTY_STATE);
			items.add(VIEW_TYPE_SHADOW);
			notifyDataSetChanged();
			return;
		}

		if (listener.isEditMode()) {
			items.clear();
			items.add(VIEW_TYPE_DIVIDER);

			int pageNumber = 1;
			for (List<MapWidgetInfo> widgetsOnPage : pagedWidgets) {
				PageItem pageItem = new PageItem(pageNumber);
				items.add(pageItem);
				items.addAll(widgetsOnPage);

				pageNumber++;
			}

			items.add(VIEW_TYPE_ADD_PAGE);
			items.add(VIEW_TYPE_SHADOW);
			items.add(VIEW_TYPE_SPACE);
		} else {
			items.clear();

			int pageNumber = 1;
			for (List<MapWidgetInfo> widgetsOnPage : pagedWidgets) {
				items.add(VIEW_TYPE_DIVIDER);

				PageItem pageItem = new PageItem(pageNumber);
				items.add(pageItem);
				items.addAll(widgetsOnPage);

				pageNumber++;
			}

			items.add(VIEW_TYPE_SHADOW);
			items.add(VIEW_TYPE_SPACE);
		}
		notifyDataSetChanged();
	}

	public void setData(List<List<MapWidgetInfo>> pagedWidgets) {
		updateItemsForEditMode(pagedWidgets);
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setItems(@NonNull List<Object> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
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
			} else if (item instanceof MapWidgetInfo widgetInfo && currentPage != null) {
				widgetInfo.priority = 0;
				currentPage.add(widgetInfo);
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
				yield new PageViewHolder(pageView);
			}
			case VIEW_TYPE_WIDGET -> {
				View widgetView = inflater.inflate(R.layout.configure_screen_list_item_widget_reorder, parent, false);
				yield new WidgetViewHolder(widgetView);
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
			case VIEW_TYPE_SHADOW -> {
				View shadowViewHolder = inflater.inflate(R.layout.card_bottom_divider, parent, false);
				yield new ShadowViewHolder(shadowViewHolder);
			}
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
			boolean showDivider = false;
			int nextItemIndex = position + 1;
			if (items.size() > nextItemIndex && items.get(nextItemIndex) instanceof MapWidgetInfo) {
				showDivider = true;
			}
			widgetViewHolder.bind(mapActivity, listener, itemTouchHelper, (MapWidgetInfo) item, iconsHelper, position, nightMode, showDivider);
		} else if (holder instanceof AddPageViewHolder addPageViewHolder) {
			addPageViewHolder.bind(listener);
		} else if (holder instanceof SpaceViewHolder spaceViewHolder) {
			spaceViewHolder.bind(mapActivity);
		} else if (holder instanceof EmptyStateViewHolder emptyStateViewHolder) {
			emptyStateViewHolder.bind(app, listener, nightMode);
		}
	}

	@NonNull
	public ArrayList<MapWidgetInfo> getRowWidgetIds(int rowPosition, @Nullable List<Object> searchItems) {
		List<Object> list = searchItems != null ? searchItems : items;
		ArrayList<MapWidgetInfo> rowWidgetIds = new ArrayList<>();
		Object item = list.get(++rowPosition);

		while (rowPosition < list.size() && item instanceof MapWidgetInfo) {
			item = list.get(rowPosition);
			if (item instanceof MapWidgetInfo) {
				rowWidgetIds.add((MapWidgetInfo) item);
			}
			rowPosition++;
		}
		return rowWidgetIds;
	}

	public int getPreviousRowPosition(int currentRowPosition) {
		currentRowPosition--;
		while (currentRowPosition >= 0) {
			if (items.get(currentRowPosition) instanceof PageItem) {
				return currentRowPosition;
			}
			currentRowPosition--;
		}
		return -1;
	}

	public boolean rowHasComplexWidget(int rowPosition, @Nullable List<Object> searchItems) {
		ArrayList<MapWidgetInfo> rowWidgetIds = getRowWidgetIds(rowPosition, searchItems != null ? searchItems : items);
		for (MapWidgetInfo widgetUiInfo : rowWidgetIds) {
			if (isComplexWidget(widgetUiInfo.key)) {
				return true;
			}
		}
		return false;
	}

	public void actionStarted() {
		if (isVerticalPanel) {
			itemsBeforeAction.clear();
			itemsBeforeAction.addAll(items);
		}
	}

	public boolean isFirstPage(int position) {
		for (int i = 0; i < position; i++) {
			if (items.get(i) instanceof PageItem) {
				return false;
			}
		}
		return true;
	}

	public void removeItem(int position) {
		if (position >= 0 && position < items.size()) {
			if (isVerticalPanel) {
				List<Object> tempItems = new ArrayList<>(items);
				tempItems.remove(position);
				Integer multipleComplexIndex = multipleComplexWidgetsInRowIndex(tempItems);
				if (multipleComplexIndex != null) {
					app.showToastMessage(app.getString(R.string.complex_widget_alert, getComplexWidgetName(multipleComplexIndex, tempItems)));
					return;
				}
			}
			items.remove(position);
			notifyItemRemoved(position);
			updatePageIndexes();
			notifyItemRangeChanged(0, items.size());
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

		insertToEndOfAddedWidgets(widgetInfo);
	}

	@SuppressLint("NotifyDataSetChanged")
	private void insertToEndOfAddedWidgets(@NonNull Object listItem) {
		int insertIndex = getIndexOfLastWidgetOrPageItem() + 1;
		items.add(insertIndex, listItem);
		notifyItemInserted(insertIndex);
		notifyDataSetChanged();
	}

	private int getIndexOfLastWidgetOrPageItem() {
		int index = -1;
		for (int i = 0; i < items.size(); i++) {
			Object item = items.get(i);
			boolean suitableIndex = (index == -1 && item instanceof Integer integer && VIEW_TYPE_DIVIDER == integer)
					|| item instanceof PageItem || item instanceof MapWidgetInfo;
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

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		Object item = items.get(position);

		if (item instanceof PageItem) {
			return VIEW_TYPE_PAGE;
		} else if (item instanceof MapWidgetInfo) {
			return VIEW_TYPE_WIDGET;
		} else if (item instanceof Integer integer) {
			return integer;
		}

		return -1;
	}

	public static class PageItem {
		public int pageNumber;

		public PageItem(int pageNumber) {
			this.pageNumber = pageNumber;
		}
	}
}
