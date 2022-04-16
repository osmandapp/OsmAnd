package net.osmand.plus.views.mapwidgets.configure.reorder;

import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ItemType;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ListItem;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.PageViewHolder.PageUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder.WidgetUiInfo;

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

	public void restorePage(int page, int position) {
		for (int i = position; i < adapter.getItemCount(); i++) {
			ListItem item = items.get(i);
			if (item.value instanceof WidgetUiInfo) {
				WidgetUiInfo widgetUiInfo = ((WidgetUiInfo) item.value);
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
			if (item.value instanceof WidgetUiInfo) {
				WidgetUiInfo widgetUiInfo = ((WidgetUiInfo) item.value);
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

		boolean swapWidgets = valueFrom instanceof WidgetUiInfo && valueTo instanceof WidgetUiInfo;
		boolean swapWidgetWithPage = valueFrom instanceof WidgetUiInfo
				&& valueTo instanceof PageUiInfo
				&& ((PageUiInfo) valueTo).index > 0;
		boolean swapPages = valueFrom instanceof PageUiInfo
				&& valueTo instanceof PageUiInfo
				&& ((PageUiInfo) valueFrom).index > 0
				&& ((PageUiInfo) valueTo).index > 0;
		boolean swapPageWithWidget = valueFrom instanceof PageUiInfo
				&& ((PageUiInfo) valueFrom).index > 0
				&& valueTo instanceof WidgetUiInfo;

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
		WidgetUiInfo widgetFrom = ((WidgetUiInfo) items.get(from).value);
		WidgetUiInfo widgetTo = ((WidgetUiInfo) items.get(to).value);

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
		WidgetUiInfo widgetFrom = ((WidgetUiInfo) items.get(from).value);
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
		WidgetUiInfo widgetTo = ((WidgetUiInfo) items.get(to).value);

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
}