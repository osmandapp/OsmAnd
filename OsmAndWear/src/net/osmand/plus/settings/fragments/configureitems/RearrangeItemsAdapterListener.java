package net.osmand.plus.settings.fragments.configureitems;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.R;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.ArrayList;
import java.util.List;

public class RearrangeItemsAdapterListener implements MenuItemsAdapterListener {

	private final ItemTouchHelper touchHelper;
	private final RearrangeItemsHelper itemsHelper;
	private final RearrangeMenuItemsAdapter adapter;

	private int fromPosition;
	private int toPosition;

	public RearrangeItemsAdapterListener(@NonNull ItemTouchHelper touchHelper,
	                                     @NonNull RearrangeItemsHelper itemsHelper,
	                                     @NonNull RearrangeMenuItemsAdapter adapter) {
		this.touchHelper = touchHelper;
		this.itemsHelper = itemsHelper;
		this.adapter = adapter;
	}

	@Override
	public void onDragStarted(@NonNull ViewHolder holder) {
		fromPosition = holder.getAdapterPosition();
		touchHelper.startDrag(holder);
	}

	@Override
	public void onDragOrSwipeEnded(@NonNull ViewHolder holder) {
		if (itemsHelper.getScreenType() == ScreenType.CONTEXT_MENU_ACTIONS) {
			itemsHelper.setMainActionItems(getMainActionsIds());
		}
		toPosition = holder.getAdapterPosition();
		if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onButtonClicked(int position) {
		Object value = adapter.getItem(position);
		if (value instanceof ContextMenuItem) {
			itemsHelper.toggleItemVisibility((ContextMenuItem) value);
			adapter.updateItems(itemsHelper.getAdapterItems());
		}
	}

	@Override
	public void onItemMoved(@Nullable String id, int position) {
		itemsHelper.onItemMoved(id, position);
	}

	@NonNull
	private List<String> getMainActionsIds() {
		List<String> ids = new ArrayList<>();
		for (Object item : adapter.getItems()) {
			if (item instanceof ContextMenuItem) {
				ids.add(((ContextMenuItem) item).getId());
			} else if (item instanceof RearrangeHeaderItem
					&& (((RearrangeHeaderItem) item).titleId == R.string.additional_actions
					|| ((RearrangeHeaderItem) item).titleId == R.string.shared_string_hidden)) {
				break;
			}
		}
		return ids;
	}
}