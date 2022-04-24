package net.osmand.plus.views.mapwidgets.configure.reorder;

import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ItemType;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ListItem;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableItemViewHolder.AvailableWidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.PageViewHolder.PageUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddedWidgetViewHolder.AddedWidgetUiInfo;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ReorderWidgetsAdapterHelper {

	private final ReorderWidgetsAdapter adapter;
	private final WidgetsDataHolder dataHolder;
	private final List<ListItem> items;

	public ReorderWidgetsAdapterHelper(@NonNull ReorderWidgetsAdapter adapter,
	                                   @NonNull WidgetsDataHolder dataHolder,
	                                   @NonNull List<ListItem> items) {
		this.adapter = adapter;
		this.dataHolder = dataHolder;
		this.items = items;
	}

	public void deleteWidget(@NonNull AddedWidgetUiInfo addedWidgetUiInfo, int position) {
		if (position != RecyclerView.NO_POSITION) {
			dataHolder.deleteWidget(addedWidgetUiInfo.key);
			items.remove(position);
			adapter.notifyItemRemoved(position);

			int order = dataHolder.getSelectedPanel().getOriginalWidgetOrder(addedWidgetUiInfo.key);
			AvailableWidgetUiInfo availableWidgetInfo = new AvailableWidgetUiInfo(addedWidgetUiInfo, order);
			int insertIndex = getInsertIndexForAvailableWidget(order);
			if (insertIndex != -1) {
				items.add(insertIndex, new ListItem(ItemType.AVAILABLE_WIDGET, availableWidgetInfo));
				adapter.notifyItemChanged(insertIndex - 1, null); // Show bottom divider, without animation
				adapter.notifyItemInserted(insertIndex);
			}
		}
	}

	private int getInsertIndexForAvailableWidget(int order) {
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
			if (listItem.value instanceof PageUiInfo) {
				((PageUiInfo) listItem.value).onPreviousPageDeleted();
				changedPageItems.add(i);
			}
		}

		adapter.notifyItemRemoved(position);
		for (int itemIndex : changedPageItems) {
			adapter.notifyItemChanged(itemIndex);
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
		int pagesCount = 0;
		int addPageButtonIndex = -1;
		for (int i = 0; i < items.size() && addPageButtonIndex == -1; i++) {
			ItemType type = items.get(i).type;
			if (type == ItemType.PAGE) {
				pagesCount++;
			} else if (type == ItemType.ADD_PAGE_BUTTON) {
				addPageButtonIndex = i;
			}
		}

		if (addPageButtonIndex != -1) {
			dataHolder.addEmptyPage(pagesCount);
			ListItem newPageListItem = new ListItem(ItemType.PAGE, new PageUiInfo(pagesCount));
			items.add(addPageButtonIndex, newPageListItem);
			adapter.notifyItemInserted(addPageButtonIndex);
		}
	}

	public boolean swapItemsIfAllowed(int from, int to) {
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

	private void swapWidgets(int from, int to) {
		AddedWidgetUiInfo widgetFrom = ((AddedWidgetUiInfo) items.get(from).value);
		AddedWidgetUiInfo widgetTo = ((AddedWidgetUiInfo) items.get(to).value);

		int tempPage = widgetFrom.page;
		widgetFrom.page = widgetTo.page;
		widgetTo.page = tempPage;

		int tempOrder = widgetFrom.order;
		widgetFrom.order = widgetTo.order;
		widgetTo.order = tempOrder;

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

	public void addWidget(int position) {
		AvailableWidgetUiInfo availableWidgetInfo = ((AvailableWidgetUiInfo) items.get(position).value);

		items.remove(position);
		adapter.notifyItemRemoved(position);
		adapter.notifyItemChanged(position - 1, null); // Hide bottom divider, without animation

		int page = getLastPage();
		int order = dataHolder.getMaxOrderOfPage(page) + 1;

		dataHolder.addWidgetToPage(availableWidgetInfo.key, page);
		dataHolder.getOrders().put(availableWidgetInfo.key, order);

		AddedWidgetUiInfo addedWidgetInfo = new AddedWidgetUiInfo(availableWidgetInfo, page, order);
		ListItem newAddedWidgetItem = new ListItem(ItemType.ADDED_WIDGET, addedWidgetInfo);

		int insertIndex = getIndexOfLastWidgetOrPageItem() + 1;
		items.add(insertIndex, newAddedWidgetItem);
		adapter.notifyItemInserted(insertIndex);
	}

	private int getLastPage() {
		List<Integer> pages = new ArrayList<>(dataHolder.getPages().keySet());
		return Algorithms.isEmpty(pages) ? 0 : pages.get(pages.size() - 1);
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