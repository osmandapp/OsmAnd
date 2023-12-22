package net.osmand.plus.views.mapwidgets.configure.reorder;

import static net.osmand.plus.views.mapwidgets.WidgetType.getDuplicateWidgetId;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ItemType;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ListItem;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddedWidgetViewHolder.AddedWidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableItemViewHolder.AvailableWidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.PageViewHolder.PageUiInfo;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ReorderWidgetsAdapterHelper {

	private final OsmandApplication app;
	private final ReorderWidgetsAdapter adapter;
	private final WidgetsDataHolder dataHolder;
	private final MapWidgetRegistry widgetRegistry;
	private final List<ListItem> items;
	private final boolean nightMode;
	private final boolean verticalPanel;

	public ReorderWidgetsAdapterHelper(@NonNull OsmandApplication app,
									   @NonNull ReorderWidgetsAdapter adapter,
									   @NonNull WidgetsDataHolder dataHolder,
									   @NonNull List<ListItem> items,
									   boolean nightMode) {
		this.app = app;
		this.adapter = adapter;
		this.dataHolder = dataHolder;
		this.items = items;
		this.nightMode = nightMode;
		this.verticalPanel = dataHolder.getSelectedPanel().isPanelVertical();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
	}

	public void deleteWidget(@NonNull AddedWidgetUiInfo widgetUiInfo, int position) {
		if (position != RecyclerView.NO_POSITION) {
			dataHolder.deleteWidget(widgetUiInfo.key);
			items.remove(position);
			adapter.notifyItemRemoved(position);
		}
	}

	private void updateAvailableWidget(@NonNull AddedWidgetUiInfo addedWidgetUiInfo) {
		WidgetType widgetType = WidgetType.getById(addedWidgetUiInfo.key);
		WidgetGroup widgetGroup = widgetType != null ? widgetType.getGroup() : null;
		if (widgetGroup != null) {
			boolean addGroupItem = areAllGroupWidgetsAdded(widgetGroup);
			if (addGroupItem) {
				addGroupToAvailableList(widgetGroup);
			}
		} else {
			moveWidgetToAvailableList(addedWidgetUiInfo);
		}
	}

	private void addGroupToAvailableList(@NonNull WidgetGroup widgetGroup) {
		int order = widgetGroup.getOrder();
		int insertIndex = getInsertIndexForAvailableItem(order);
		if (insertIndex != -1) {
			ListItem groupItem = new ListItem(ItemType.AVAILABLE_GROUP, widgetGroup);
			insertAvailableItem(groupItem, insertIndex);
		}
	}

	private void moveWidgetToAvailableList(@NonNull AddedWidgetUiInfo addedWidgetUiInfo) {
		int order = dataHolder.getSelectedPanel().getOriginalWidgetOrder(addedWidgetUiInfo.key);
		AvailableWidgetUiInfo availableWidgetInfo = new AvailableWidgetUiInfo(addedWidgetUiInfo, order);
		int insertIndex = getInsertIndexForAvailableItem(order);
		if (insertIndex != -1) {
			ListItem widgetItem = new ListItem(ItemType.AVAILABLE_WIDGET, availableWidgetInfo);
			insertAvailableItem(widgetItem, insertIndex);
		}
	}

	private int getInsertIndexForAvailableItem(int order) {
		int passedHeaders = 0;
		int secondHeaderIndex = -1;
		int closestHigherIndex = -1;
		for (int i = 0; i < items.size(); i++) {
			ListItem item = items.get(i);
			Object value = item.value;

			if (item.type == ItemType.HEADER) {
				passedHeaders++;
				if (passedHeaders == 2) {
					secondHeaderIndex = i;
				}
			} else if (value instanceof WidgetGroup || value instanceof AvailableWidgetUiInfo) {
				int availableItemOrder = item.value instanceof WidgetGroup
						? ((WidgetGroup) item.value).getOrder()
						: ((AvailableWidgetUiInfo) item.value).order;
				if (availableItemOrder > order) {
					return i;
				} else {
					closestHigherIndex = i;
				}
			}
		}

		return closestHigherIndex == -1 ? secondHeaderIndex + 1 : closestHigherIndex + 1;
	}

	private void insertAvailableItem(@NonNull ListItem listItem, int insertIndex) {
		items.add(insertIndex, listItem);
		adapter.notifyItemChanged(insertIndex - 1, null); // Show bottom divider, without animation
		adapter.notifyItemInserted(insertIndex);
	}

	public void restorePage(int page, int position) {
		for (int i = position; i < adapter.getItemCount(); i++) {
			ListItem item = items.get(i);
			if (item.value instanceof AddedWidgetUiInfo) {
				AddedWidgetUiInfo widgetUiInfo = ((AddedWidgetUiInfo) item.value);
				widgetUiInfo.page += 1;
				dataHolder.addWidgetToPage(widgetUiInfo.key, widgetUiInfo.page);
			} else if (item.value instanceof PageUiInfo) {
				((PageUiInfo) item.value).onPreviousPageRestored();
				adapter.notifyItemChanged(i);
			} else {
				break;
			}
		}

		ListItem restoredPageItem = new ListItem(ItemType.PAGE, new PageUiInfo(page));
		items.add(position, restoredPageItem);
		adapter.notifyItemInserted(position);
	}

	public void deletePage(int position, int pageToDelete) {
		if (position == RecyclerView.NO_POSITION) {
			return;
		}

		moveWidgetsToPreviousPage(pageToDelete);
		dataHolder.deletePage(pageToDelete);
		items.remove(position);

		List<Integer> changedPageItems = new ArrayList<>();
		for (int i = position; i < adapter.getItemCount(); i++) {
			ListItem listItem = items.get(i);
			if (listItem.value instanceof PageUiInfo && !verticalPanel) {
				((PageUiInfo) listItem.value).onPreviousPageDeleted();
				changedPageItems.add(i);
			}
		}

		adapter.notifyItemRemoved(position);
		for (int itemIndex : changedPageItems) {
			adapter.notifyItemChanged(itemIndex);
		}
	}

	private void deletePageWithWidgets(int pageToDelete) {
		List<String> pageWidgets = dataHolder.getPages().get(pageToDelete);
		if (pageWidgets == null) {
			return;
		}

		List<String> widgetsToDelete = new ArrayList<>(pageWidgets);
		for (String widgetId : widgetsToDelete) {
			dataHolder.deleteWidget(widgetId);
		}
		List<ListItem> listItemsToDelete = new ArrayList<>();
		for (ListItem item : items) {
			if (item.value instanceof AddedWidgetUiInfo) {
				AddedWidgetUiInfo widgetUiInfo = (AddedWidgetUiInfo) item.value;
				if (widgetsToDelete.contains(widgetUiInfo.key)) {
					listItemsToDelete.add(item);
				}
			}
		}
		for (ListItem item : listItemsToDelete) {
			items.remove(item);
		}
	}

	private void moveWidgetsToPreviousPage(int pageToMoveFrom) {
		int previousPage = pageToMoveFrom - 1;
		int previousPageSize = 0;
		for (ListItem item : items) {
			if (item.value instanceof AddedWidgetUiInfo) {
				AddedWidgetUiInfo widgetUiInfo = ((AddedWidgetUiInfo) item.value);
				if (widgetUiInfo.page == previousPage) {
					previousPageSize++;
				} else if (widgetUiInfo.page == pageToMoveFrom) {
					widgetUiInfo.page = previousPage;
					widgetUiInfo.order = previousPageSize;
					previousPageSize++;

					dataHolder.addWidgetToPage(widgetUiInfo.key, widgetUiInfo.page);
					dataHolder.getOrders().put(widgetUiInfo.key, widgetUiInfo.order);
				}
			}
		}
	}

	public void addPage() {
		int maxPage = 0;
		int addPageButtonIndex = -1;
		for (int i = 0; i < items.size() && addPageButtonIndex == -1; i++) {
			ItemType type = items.get(i).type;
			if (type == ItemType.PAGE) {
				int itemIndex = ((PageUiInfo) items.get(i).value).index;
				maxPage = Math.max(maxPage, itemIndex);
			} else if (type == ItemType.ADD_PAGE_BUTTON) {
				addPageButtonIndex = i;
			}
		}
		if (addPageButtonIndex != -1) {
			maxPage++;
			dataHolder.addEmptyPage(maxPage);
			ListItem newPageListItem = new ListItem(ItemType.PAGE, new PageUiInfo(maxPage));
			items.add(addPageButtonIndex, newPageListItem);
			adapter.notifyItemInserted(addPageButtonIndex);
		}
	}

	private int getTargetRowId(int fromWidgetId, int toRowId) {
		ListIterator<ListItem> iterator = items.listIterator(toRowId);
		int targetRowId = toRowId;
		ListItem itemTo = items.get(toRowId);

		if (itemTo.value instanceof PageUiInfo && fromWidgetId > toRowId) {
			while (iterator.hasPrevious()) {
				targetRowId = iterator.previousIndex();
				ListItem item = iterator.previous();
				if (item.value instanceof PageUiInfo) {
					return targetRowId;
				}
			}
		} else if (itemTo.value instanceof PageUiInfo && fromWidgetId < toRowId) {
			return targetRowId;
		}
		return -1;
	}

	private List<MapWidget> getTargetRowWidgets(int targetRowId) {
		List<MapWidget> targetRowWidgets = new ArrayList<>();
		ListItem obj = items.get(targetRowId);
		if (obj.value instanceof PageUiInfo && items.size() > targetRowId + 1) {
			Iterator<ListItem> iterator = items.listIterator(targetRowId + 1);
			while (iterator.hasNext()) {
				ListItem listItem = iterator.next();
				if (listItem.value instanceof AddedWidgetUiInfo) {
					AddedWidgetUiInfo widgetUiInfo = (AddedWidgetUiInfo) listItem.value;
					targetRowWidgets.add(widgetUiInfo.info.widget);
				} else {
					return targetRowWidgets;
				}
			}
		}
		return targetRowWidgets;
	}

	private boolean verticalWidgetCanBeAdded(@NonNull List<MapWidget> targetRowWidgets, @NonNull MapWidget fromWidget) {
		if (Algorithms.isEmpty(targetRowWidgets)) {
			return true;
		}

		boolean containsSimpleWidget = true;
		for (MapWidget widget : targetRowWidgets) {
			if (!(widget instanceof SimpleWidget)) {
				containsSimpleWidget = false;
				break;
			}
		}
		return fromWidget instanceof SimpleWidget && containsSimpleWidget;
	}

	private boolean swapVerticalWidgets(int from, int to) {
		ListItem itemFrom = items.get(from);
		ListItem itemTo = items.get(to);
		Object valueFrom = itemFrom.value;
		Object valueTo = itemTo.value;

		boolean swapWidgets = valueFrom instanceof AddedWidgetUiInfo && valueTo instanceof AddedWidgetUiInfo;
		boolean swapWidgetWithPage = valueFrom instanceof AddedWidgetUiInfo
				&& valueTo instanceof PageUiInfo
				&& ((PageUiInfo) valueTo).index > 0;

		if (swapWidgets) {
			AddedWidgetUiInfo fromWidget = (AddedWidgetUiInfo) valueFrom;
			AddedWidgetUiInfo toWidget = (AddedWidgetUiInfo) valueTo;
			if (fromWidget.info.getClass() == toWidget.info.getClass()) {
				swapWidgets(from, to);
				return true;
			}
		}
		if (swapWidgetWithPage) {
			int targetRowId = getTargetRowId(from, to);
			if (targetRowId == -1) {
				return false;
			}
			List<MapWidget> targetRowWidgets = getTargetRowWidgets(targetRowId);
			AddedWidgetUiInfo fromWidget = (AddedWidgetUiInfo) valueFrom;
			PageUiInfo pageUiInfo = (PageUiInfo) items.get(targetRowId).value;
			if (verticalWidgetCanBeAdded(targetRowWidgets, fromWidget.info.widget)) {
				addVerticalWidgetsToRow(from, to, targetRowId, pageUiInfo);
				return true;
			}
		}
		return false;
	}

	private boolean swapHorizontalWidgets(int from, int to) {
		ListItem itemFrom = items.get(from);
		ListItem itemTo = items.get(to);
		Object valueFrom = itemFrom.value;
		Object valueTo = itemTo.value;

		boolean swapWidgets = valueFrom instanceof AddedWidgetUiInfo && valueTo instanceof AddedWidgetUiInfo;
		boolean swapWidgetWithPage = valueFrom instanceof AddedWidgetUiInfo
				&& valueTo instanceof PageUiInfo
				&& ((PageUiInfo) valueTo).index > 0;
		boolean swapPages = valueFrom instanceof PageUiInfo
				&& valueTo instanceof PageUiInfo
				&& ((PageUiInfo) valueFrom).index > 0
				&& ((PageUiInfo) valueTo).index > 0;
		boolean swapPageWithWidget = valueFrom instanceof PageUiInfo
				&& ((PageUiInfo) valueFrom).index > 0
				&& valueTo instanceof AddedWidgetUiInfo;

		if (swapWidgets) {
			swapWidgets(from, to);
			return true;
		} else if (swapWidgetWithPage) {
			swapWidgetWithPage(from, to);
			return true;
		} else if (swapPages) {
			swapPages(from, to);
			return true;
		} else if (swapPageWithWidget) {
			swapPageWithWidget(from, to);
			return true;
		}

		return false;
	}

	public boolean swapItemsIfAllowed(int from, int to) {
		return swapHorizontalWidgets(from, to);
	}

	private void addVerticalWidgetsToRow(int from, int to, int pageId, PageUiInfo pageUiInfo) {
		if (to > pageId) {
			AddedWidgetUiInfo widgetFrom = ((AddedWidgetUiInfo) items.get(from).value);
			dataHolder.deleteWidget(widgetFrom.key);
			widgetFrom.page = pageUiInfo.index;
			dataHolder.addWidgetToPage(widgetFrom.key, widgetFrom.page);
			widgetFrom.order = dataHolder.getMaxOrderOfPage(widgetFrom.page) + 1;
			ListItem fromItem = items.get(from);
			items.remove(fromItem);
			items.add(to, fromItem);
		} else {
			AddedWidgetUiInfo widgetFrom = ((AddedWidgetUiInfo) items.get(from).value);
			dataHolder.deleteWidget(widgetFrom.key);
			dataHolder.shiftPageOrdersToLeft(widgetFrom.page, widgetFrom.order);

			widgetFrom.page = pageUiInfo.index;
			dataHolder.addWidgetToPage(widgetFrom.key, widgetFrom.page, 0);

			widgetFrom.order = 0;
			dataHolder.shiftPageOrdersToRight(widgetFrom.page);
			dataHolder.getOrders().put(widgetFrom.key, 0);

			ListItem fromItem = items.get(from);
			items.remove(fromItem);
			items.add(to, fromItem);
		}

		adapter.notifyItemMoved(from, to);
	}

	private void swapWidgets(int from, int to) {
		AddedWidgetUiInfo widgetFrom = ((AddedWidgetUiInfo) items.get(from).value);
		AddedWidgetUiInfo widgetTo = ((AddedWidgetUiInfo) items.get(to).value);

		int tempPage = widgetFrom.page;
		widgetFrom.page = widgetTo.page;
		widgetTo.page = tempPage;

		int tempOrder = widgetFrom.order;
		widgetFrom.order = widgetTo.order;
		widgetTo.order = tempOrder;

		dataHolder.deleteWidget(widgetFrom.key);
		dataHolder.deleteWidget(widgetTo.key);
		dataHolder.addWidgetToPage(widgetFrom.key, widgetFrom.page);
		dataHolder.addWidgetToPage(widgetTo.key, widgetTo.page);

		dataHolder.getOrders().put(widgetFrom.key, widgetFrom.order);
		dataHolder.getOrders().put(widgetTo.key, widgetTo.order);

		Collections.swap(items, from, to);
		adapter.notifyItemMoved(from, to);
	}

	private void swapWidgetWithPage(int from, int to) {
		AddedWidgetUiInfo widgetFrom = ((AddedWidgetUiInfo) items.get(from).value);
		int pageIndex = ((PageUiInfo) items.get(to).value).index;

		int currentWidgetPage = widgetFrom.page;
		boolean moveToAnotherPageStart = currentWidgetPage != pageIndex;

		int newPage = moveToAnotherPageStart ? pageIndex : pageIndex - 1;
		widgetFrom.page = newPage;

		if (moveToAnotherPageStart) {
			widgetFrom.order = 0;
			dataHolder.shiftPageOrdersToRight(newPage);
		} else {
			widgetFrom.order = dataHolder.getMaxOrderOfPage(newPage) + 1;
		}

		dataHolder.addWidgetToPage(widgetFrom.key, newPage);
		dataHolder.getOrders().put(widgetFrom.key, widgetFrom.order);

		Collections.swap(items, from, to);
		adapter.notifyItemMoved(from, to);
	}

	private void swapPages(int from, int to) {
		PageUiInfo pageFrom = ((PageUiInfo) items.get(from).value);
		PageUiInfo pageTo = ((PageUiInfo) items.get(to).value);

		int tempPage = pageFrom.index;
		pageFrom.index = pageTo.index;
		pageTo.index = tempPage;

		Collections.swap(items, from, to);
		adapter.notifyItemMoved(from, to);
		adapter.notifyItemChanged(from);
		adapter.notifyItemChanged(to);
	}

	private void swapPageWithWidget(int from, int to) {
		PageUiInfo pageFrom = ((PageUiInfo) items.get(from).value);
		AddedWidgetUiInfo widgetTo = ((AddedWidgetUiInfo) items.get(to).value);

		boolean movingUp = from > to;
		if (movingUp) {
			widgetTo.page = pageFrom.index;
			widgetTo.order = 0;
			dataHolder.shiftPageOrdersToRight(widgetTo.page);
		} else {
			widgetTo.page = pageFrom.index - 1;
			widgetTo.order = dataHolder.getMaxOrderOfPage(widgetTo.page) + 1;
		}

		dataHolder.addWidgetToPage(widgetTo.key, widgetTo.page);
		dataHolder.getOrders().put(widgetTo.key, widgetTo.order);

		Collections.swap(items, from, to);
		adapter.notifyItemMoved(from, to);
	}

	public void addWidget(@NonNull MapWidgetInfo widgetInfo) {
		WidgetsPanel panel = dataHolder.getSelectedPanel();

		String widgetId = getDuplicateWidgetId(widgetInfo.key);

		int page = getLastPage();
		int order = dataHolder.getMaxOrderOfPage(page) + 1;

		List<String> lastPageOrder = dataHolder.getPages().get(page);
		if (lastPageOrder != null && panel.isPanelVertical()) {
			page++;
			order = 0;
			insertToEndOfAddedWidgets(new ListItem(ItemType.PAGE, new PageUiInfo(page)));
		}

		dataHolder.addWidgetToPage(widgetId, page);
		dataHolder.getOrders().put(widgetId, order);

		AddedWidgetUiInfo addedWidgetUiInfo = new AddedWidgetUiInfo();
		addedWidgetUiInfo.key = widgetId;
		addedWidgetUiInfo.title = widgetInfo.getTitle(app);
		addedWidgetUiInfo.info = widgetInfo;
		addedWidgetUiInfo.page = page;
		addedWidgetUiInfo.order = order;
		addedWidgetUiInfo.iconId = widgetInfo.getMapIconId(nightMode);
		addedWidgetUiInfo.newWidgetToCreate = true;

		insertToEndOfAddedWidgets(new ListItem(ItemType.ADDED_WIDGET, addedWidgetUiInfo));
	}

	private void removeAddedItemFromAvailable(@NonNull String widgetId) {
		WidgetType widgetType = WidgetType.getById(widgetId);
		WidgetGroup group = widgetType != null ? widgetType.getGroup() : null;
		if (group != null) {
			if (areAllGroupWidgetsAdded(group)) {
				int indexToRemove = getIndexOfAvailableGroup(group);
				if (indexToRemove != -1) {
					removeItemFromAvailable(indexToRemove);
				}
			}
		} else {
			int indexToRemove = getIndexOfAvailableWidget(widgetId);
			if (indexToRemove != -1) {
				removeItemFromAvailable(indexToRemove);
			}
		}
	}

	private void removeItemFromAvailable(int indexToRemove) {
		items.remove(indexToRemove);
		adapter.notifyItemRemoved(indexToRemove);
		adapter.notifyItemChanged(indexToRemove - 1, null); // Hide bottom divider, without animation
	}

	private int getLastPage() {
		List<Integer> pages = new ArrayList<>(dataHolder.getPages().keySet());
		return Algorithms.isEmpty(pages) ? 0 : pages.get(pages.size() - 1);
	}

	private boolean areAllGroupWidgetsAdded(@NonNull WidgetGroup group) {
		int addedGroupWidgetsCount = 0;
		for (int i = 0; i < items.size(); i++) {
			ListItem listItem = items.get(i);
			if (listItem.value instanceof AddedWidgetUiInfo) {
				AddedWidgetUiInfo addedWidgetUiInfo = (AddedWidgetUiInfo) listItem.value;
				WidgetType widgetType = WidgetType.getById(addedWidgetUiInfo.key);
				if (widgetType != null && group == widgetType.getGroup()) {
					addedGroupWidgetsCount++;
				}
			}
		}

		return addedGroupWidgetsCount == group.getWidgets().size();
	}

	private int getIndexOfAvailableGroup(@NonNull WidgetGroup group) {
		for (int i = 0; i < items.size(); i++) {
			ListItem listItem = items.get(i);
			if (group.equals(listItem.value)) {
				return i;
			}
		}

		return -1;
	}

	private int getIndexOfAvailableWidget(@NonNull String widgetId) {
		for (int i = 0; i < items.size(); i++) {
			ListItem listItem = items.get(i);
			if (listItem.value instanceof AvailableWidgetUiInfo) {
				String availableWidgetId = ((AvailableWidgetUiInfo) listItem.value).key;
				if (widgetId.equals(availableWidgetId)) {
					return i;
				}
			}
		}

		return -1;
	}

	private void insertToEndOfAddedWidgets(@NonNull ListItem listItem) {
		int insertIndex = getIndexOfLastWidgetOrPageItem() + 1;
		items.add(insertIndex, listItem);
		adapter.notifyItemInserted(insertIndex);
	}

	private int getIndexOfLastWidgetOrPageItem() {
		int index = -1;
		for (int i = 0; i < items.size(); i++) {
			ListItem item = items.get(i);
			boolean suitableIndex = (index == -1 && item.type == ItemType.HEADER)
					|| (item.type == ItemType.ADDED_WIDGET)
					|| (item.type == ItemType.PAGE);
			if (suitableIndex) {
				index = i;
			}
		}

		return index;
	}
}