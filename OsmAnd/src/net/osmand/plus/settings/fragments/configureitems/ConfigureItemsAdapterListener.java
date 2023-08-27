package net.osmand.plus.settings.fragments.configureitems;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

public class ConfigureItemsAdapterListener implements MenuItemsAdapterListener {

	private final ItemTouchHelper touchHelper;
	private final RearrangeItemsHelper itemsHelper;
	private final RearrangeMenuItemsAdapter adapter;

	private int fromPosition;
	private int toPosition;

	public ConfigureItemsAdapterListener(@NonNull ItemTouchHelper touchHelper, @NonNull RearrangeItemsHelper itemsHelper, @NonNull RearrangeMenuItemsAdapter adapter) {
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
			itemsHelper.setMainActionItems(adapter.getMainActionsIds());
		}
		toPosition = holder.getAdapterPosition();
		if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onButtonClicked(int position) {
		RearrangeMenuAdapterItem rearrangeItem = adapter.getItem(position);
		Object value = rearrangeItem.value;
		if (value instanceof ContextMenuItem) {
			itemsHelper.toggleItemVisibility((ContextMenuItem) value);
			adapter.updateItems(itemsHelper.getAdapterItems());
		}
	}

	@Override
	public void onItemMoved(@Nullable String id, int position) {
		itemsHelper.onItemMoved(id, position);
	}
}