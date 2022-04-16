package net.osmand.plus.views.mapwidgets.configure.reorder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.OnItemMoveCallback;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ActionButtonViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.ActionButtonViewHolder.ActionButtonInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AddPageButtonViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.DividerViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.HeaderViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.PageViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.PageViewHolder.PageUiInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.SpaceViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder.ItemMovableCallback;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.WidgetViewHolder.WidgetUiInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import static net.osmand.plus.utils.UiUtilities.getColoredSelectableDrawable;

public class ReorderWidgetsAdapter extends Adapter<ViewHolder> implements OnItemMoveCallback, ItemMovableCallback {

	private final OsmandApplication app;
	private final WidgetsDataHolder dataHolder;
	private final List<ListItem> items = new ArrayList<>();

	private WidgetAdapterListener listener;
	private final boolean nightMode;
	private final int profileColor;
	private final int defaultIconColor;

	public enum ItemType {
		HEADER,
		PAGE,
		WIDGET,
		ADD_PAGE_BUTTON,
		CARD_DIVIDER,
		CARD_TOP_DIVIDER,
		CARD_BOTTOM_DIVIDER,
		SPACE,
		ACTION_BUTTON
	}

	public ReorderWidgetsAdapter(@NonNull OsmandApplication app, @NonNull WidgetsDataHolder dataHolder, boolean nightMode) {
		setHasStableIds(true);
		this.app = app;
		this.nightMode = nightMode;
		this.dataHolder = dataHolder;

		ApplicationMode mode = app.getSettings().getApplicationMode();
		profileColor = mode.getProfileColor(nightMode);
		defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
	}

	@NonNull
	public List<ListItem> getItems() {
		return items;
	}

	public void setItems(@NonNull List<ListItem> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	public void setListener(@NonNull WidgetAdapterListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Context ctx = parent.getContext();
		LayoutInflater inflater = UiUtilities.getInflater(ctx, nightMode);

		switch (ItemType.values()[viewType]) {
			case HEADER:
				return new HeaderViewHolder(inflater.inflate(R.layout.configure_screen_list_item_header, parent, false));
			case PAGE:
				View itemView = inflater.inflate(R.layout.configure_screen_list_item_page_reorder, parent, false);
				return new PageViewHolder(itemView);
			case WIDGET:
				itemView = inflater.inflate(R.layout.configure_screen_list_item_widget_reorder, parent, false);
				return new WidgetViewHolder(itemView, ReorderWidgetsAdapter.this);
			case ADD_PAGE_BUTTON:
				itemView = inflater.inflate(R.layout.configure_screen_list_item_add_page, parent, false);
				return new AddPageButtonViewHolder(itemView, profileColor);
			case CARD_DIVIDER:
				return new DividerViewHolder(inflater.inflate(R.layout.list_item_divider, parent, false));
			case CARD_TOP_DIVIDER:
				itemView = inflater.inflate(R.layout.list_item_divider, parent, false);
				View bottomShadow = itemView.findViewById(R.id.bottomShadowView);
				bottomShadow.setVisibility(View.INVISIBLE);
				return new DividerViewHolder(itemView);
			case CARD_BOTTOM_DIVIDER:
				return new DividerViewHolder(inflater.inflate(R.layout.card_bottom_divider, parent, false));
			case SPACE:
				return new SpaceViewHolder(new View(ctx));
			case ACTION_BUTTON:
				return new ActionButtonViewHolder(inflater.inflate(R.layout.configure_screen_list_item_button, parent, false));
			default:
				throw new IllegalArgumentException("Unsupported view type");
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
		ListItem item = items.get(position);
		if (viewHolder instanceof PageViewHolder) {
			bindPageViewHolder(((PageViewHolder) viewHolder), position);
		} else if (viewHolder instanceof WidgetViewHolder) {
			WidgetUiInfo widgetInfo = (WidgetUiInfo) item.value;
			WidgetViewHolder holder = (WidgetViewHolder) viewHolder;

			holder.title.setText(widgetInfo.title);
			holder.moveIcon.setOnTouchListener((view, event) -> {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					listener.onDragStarted(viewHolder);
				}
				return false;
			});
			holder.icon.setImageResource(widgetInfo.iconId);
			WidgetViewHolder.updateWidgetIcon(holder.icon, widgetInfo.info, profileColor, defaultIconColor, widgetInfo.isActive, nightMode);
		} else if (viewHolder instanceof HeaderViewHolder) {
			HeaderViewHolder holder = (HeaderViewHolder) viewHolder;
			holder.title.setText((String) item.value);
		} else if (viewHolder instanceof ActionButtonViewHolder) {
			ActionButtonInfo actionButtonInfo = (ActionButtonInfo) item.value;

			ActionButtonViewHolder holder = (ActionButtonViewHolder) viewHolder;
			holder.buttonView.setOnClickListener(actionButtonInfo.listener);
			holder.icon.setImageResource(actionButtonInfo.iconRes);
			holder.title.setText(actionButtonInfo.title);

			AndroidUtils.setBackground(holder.buttonView, getColoredSelectableDrawable(app, profileColor, 0.3f));
		} else if (viewHolder instanceof SpaceViewHolder) {
			SpaceViewHolder holder = (SpaceViewHolder) viewHolder;
			holder.setHeight((int) item.value);
		} else if (viewHolder instanceof AddPageButtonViewHolder) {
			((AddPageButtonViewHolder) viewHolder).buttonContainer.setOnClickListener(v -> addPage());
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void bindPageViewHolder(@NonNull PageViewHolder viewHolder, int position) {
		int pageIndex = ((PageUiInfo) items.get(position).value).index;

		OnClickListener actionListener;
		Drawable actionIcon;
		boolean firstPage = pageIndex == 0;
		if (firstPage) {
			actionListener = null;
			actionIcon = app.getUIUtilities().getIcon(R.drawable.ic_action_remove, nightMode);
		} else {
			actionListener = v -> deletePage(viewHolder.getAdapterPosition(), pageIndex);
			actionIcon = app.getUIUtilities().getIcon(R.drawable.ic_action_remove, R.color.color_osm_edit_delete);
		}
		viewHolder.actionButton.setOnClickListener(actionListener);
		viewHolder.actionButton.setImageDrawable(actionIcon);

		AndroidUiHelper.updateVisibility(viewHolder.topDivider, !firstPage);
		viewHolder.pageText.setText(app.getString(R.string.page_number, String.valueOf(pageIndex + 1)));

		AndroidUiHelper.updateVisibility(viewHolder.moveIcon, !firstPage);
		viewHolder.moveIcon.setOnTouchListener((v, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				listener.onDragStarted(viewHolder);
			}
			return false;
		});
	}

	private void deletePage(int position, int pageToDelete) {
		if (position == RecyclerView.NO_POSITION) {
			return;
		}

		moveWidgetsToPreviousPage(pageToDelete);
		dataHolder.deletePage(pageToDelete);

		items.remove(position);
		List<Integer> changedPageItems = new ArrayList<>();
		for (int i = position; i < getItemCount(); i++) {
			ListItem listItem = items.get(i);
			if (listItem.value instanceof PageUiInfo) {
				((PageUiInfo) listItem.value).onPreviousPageDeleted();
				changedPageItems.add(i);
			}
		}

		notifyItemRemoved(position);
		for (int itemIndex : changedPageItems) {
			notifyItemChanged(itemIndex);
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

	private void addPage() {
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
			notifyItemInserted(addPageButtonIndex);
		}
	}

	@Override
	public boolean onItemMove(int from, int to) {
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
			WidgetUiInfo widgetFrom = (WidgetUiInfo) valueFrom;
			WidgetUiInfo widgetTo = (WidgetUiInfo) valueTo;

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
			notifyItemMoved(from, to);
			return true;
		} else if (swapWidgetWithPage) {
			WidgetUiInfo widgetFrom = ((WidgetUiInfo) valueFrom);

			int currentWidgetPage = widgetFrom.page;
			int pageIndex = ((PageUiInfo) itemTo.value).index;
			boolean moveToAnotherPageStart = currentWidgetPage != pageIndex;

			int newPage = moveToAnotherPageStart ? pageIndex : pageIndex - 1;
			widgetFrom.page = newPage;

			if (moveToAnotherPageStart) {
				widgetFrom.order = 0;
				shiftPageOrdersToRight(newPage);
			} else {
				widgetFrom.order = getMaxOrderOfPage(newPage) + 1;
			}

			dataHolder.addWidgetToPage(widgetFrom.key, newPage);
			dataHolder.getOrders().put(widgetFrom.key, widgetFrom.order);

			Collections.swap(items, from, to);
			notifyItemMoved(from, to);
			return true;
		} else if (swapPages) {
			PageUiInfo pageFrom = ((PageUiInfo) valueFrom);
			PageUiInfo pageTo = ((PageUiInfo) valueTo);

			int tempPage = pageFrom.index;
			pageFrom.index = pageTo.index;
			pageTo.index = tempPage;

			Collections.swap(items, from, to);
			notifyItemMoved(from, to);
			notifyItemChanged(from);
			notifyItemChanged(to);
			return true;
		} else if (swapPageWithWidget) {
			PageUiInfo pageFrom = ((PageUiInfo) valueFrom);
			WidgetUiInfo widgetTo = ((WidgetUiInfo) valueTo);

			boolean movingUp = from > to;
			if (movingUp) {
				widgetTo.page = pageFrom.index;
				widgetTo.order = 0;
				shiftPageOrdersToRight(widgetTo.page);
			} else {
				widgetTo.page = pageFrom.index - 1;
				widgetTo.order = getMaxOrderOfPage(widgetTo.page) + 1;
			}

			dataHolder.addWidgetToPage(widgetTo.key, widgetTo.page);
			dataHolder.getOrders().put(widgetTo.key, widgetTo.order);

			Collections.swap(items, from, to);
			notifyItemMoved(from, to);
			return true;
		}

		return false;
	}

	private void shiftPageOrdersToRight(int page) {
		for (Entry<String, Integer> entry : dataHolder.getOrders().entrySet()) {
			String widgetId = entry.getKey();
			int widgetPage = dataHolder.getWidgetPage(widgetId);
			int widgetOrder = entry.getValue();

			if (widgetPage == page) {
				dataHolder.getOrders().put(widgetId, widgetOrder + 1);
			}
		}
	}

	private int getMaxOrderOfPage(int page) {
		int maxOrder = -1;
		for (Entry<String, Integer> entry : dataHolder.getOrders().entrySet()) {
			String widgetId = entry.getKey();
			int widgetPage = dataHolder.getWidgetPage(widgetId);
			int widgetOrder = entry.getValue();

			if (widgetPage == page && widgetOrder > maxOrder) {
				maxOrder = widgetOrder;
			}
		}

		return maxOrder;
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		ListItem item = items.get(position);
		return item.type.ordinal();
	}

	@Override
	public void onItemDismiss(ViewHolder holder) {
		listener.onDragOrSwipeEnded(holder);
	}

	@Override
	public long getItemId(int position) {
		ListItem item = items.get(position);
		if (item.value instanceof WidgetUiInfo) {
			return ((WidgetUiInfo) item.value).key.hashCode();
		} else if (item.value instanceof ActionButtonInfo) {
			return ((ActionButtonInfo) item.value).title.hashCode();
		} else if (item.value != null) {
			return item.value.hashCode();
		}
		return item.hashCode();
	}

	@Override
	public boolean isListItemMovable(int position) {
		Object itemValue = items.get(position).value;
		return itemValue instanceof PageUiInfo || itemValue instanceof WidgetUiInfo;
	}

	public static class ListItem {

		public final ItemType type;
		public final Object value;

		public ListItem(@NonNull ItemType type, @Nullable Object value) {
			this.type = type;
			this.value = value;
		}
	}

	public interface WidgetAdapterListener {

		void onDragStarted(ViewHolder holder);

		void onDragOrSwipeEnded(ViewHolder holder);
	}
}