package net.osmand.plus.views.mapwidgets.configure.reorder;

import static net.osmand.plus.views.mapwidgets.MapWidgetInfo.DELIMITER;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ItemType;
import net.osmand.plus.views.mapwidgets.configure.reorder.ReorderWidgetsAdapter.ListItem;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddedWidgetViewHolder.AddedWidgetUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.PageViewHolder.PageUiInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReorderWidgetsAdapterHelper {

	private final OsmandApplication app;
	private final ReorderWidgetsAdapter adapter;
	private final WidgetsDataHolder dataHolder;
	public final MapWidgetRegistry widgetRegistry;
	private final List<ListItem> items;
	private final boolean nightMode;

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
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
	}

	public TreeMap<Integer, List<String>> getPagedOrderFromAdapterItems() {
		Map<Integer, List<String>> widgetsMap = new TreeMap<>();
		List<ListItem> items = adapter.getItems();

		int currentPage = -1;
		for (ListItem item : items) {
			if (item.value instanceof PageUiInfo) {
				PageUiInfo pageUiInfo = (PageUiInfo) item.value;
				currentPage = pageUiInfo.index;
			} else if (item.value instanceof AddedWidgetUiInfo) {
				AddedWidgetUiInfo widgetInfo = (AddedWidgetUiInfo) item.value;
				List<String> widgetsOrder = widgetsMap.get(currentPage);
				if (widgetsOrder == null) {
					widgetsOrder = new ArrayList<>();
					widgetsMap.put(currentPage, widgetsOrder);
				}
				widgetsOrder.add(widgetInfo.key);
			}
		}

		TreeMap<Integer, List<String>> pagedOrder = new TreeMap<>();
		int pageIndex = 0;
		for (Integer key : widgetsMap.keySet()) {
			pagedOrder.put(pageIndex, widgetsMap.get(key));
			pageIndex++;
		}
		return pagedOrder;
	}

	public void savePagedOrderInDataHolder() {
		dataHolder.setPages(getPagedOrderFromAdapterItems());
	}

	public void deleteWidget(int position) {
		if (position != RecyclerView.NO_POSITION) {
			items.remove(position);
			adapter.notifyItemRemoved(position);
		}
	}

	public void deletePage(int position) {
		if (position == RecyclerView.NO_POSITION) {
			return;
		}
		savePagedOrderInDataHolder();
		items.remove(position);
		adapter.notifyItemRemoved(position);
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
			ListItem newPageListItem = new ListItem(ItemType.PAGE, new PageUiInfo(maxPage));
			items.add(addPageButtonIndex, newPageListItem);
			adapter.notifyItemInserted(addPageButtonIndex);
		}
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

	private void swapWidgets(int from, int to) {
		Collections.swap(items, from, to);
		adapter.notifyItemMoved(from, to);
	}

	private void swapWidgetWithPage(int from, int to) {
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
		Collections.swap(items, from, to);
		adapter.notifyItemMoved(from, to);
	}

	public void addWidget(@NonNull MapWidgetInfo widgetInfo) {
		WidgetsPanel panel = dataHolder.getSelectedPanel();
		String widgetId = widgetInfo.key.contains(DELIMITER) ? widgetInfo.key : WidgetType.getDuplicateWidgetId(widgetInfo.key);

		int page = getLastPage();
		if (panel.isPanelVertical()) {
			page++;
			insertToEndOfAddedWidgets(new ListItem(ItemType.PAGE, new PageUiInfo(page)));
		}

		AddedWidgetUiInfo addedWidgetUiInfo = new AddedWidgetUiInfo(widgetId, widgetInfo.getTitle(app), widgetInfo, widgetInfo.getMapIconId(nightMode));
		insertToEndOfAddedWidgets(new ListItem(ItemType.ADDED_WIDGET, addedWidgetUiInfo));
	}


	private int getLastPage() {
		int maximumPageIndex = 0;
		for (ListItem item : items) {
			if (item.value instanceof PageUiInfo) {
				PageUiInfo pageUiInfo = (PageUiInfo) item.value;
				if (pageUiInfo.index > maximumPageIndex) {
					maximumPageIndex = pageUiInfo.index;
				}
			}
		}
		return maximumPageIndex;
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