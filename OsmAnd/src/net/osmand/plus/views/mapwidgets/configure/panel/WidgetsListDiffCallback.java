package net.osmand.plus.views.mapwidgets.configure.panel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil.Callback;

import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.PageItem;
import net.osmand.plus.views.mapwidgets.configure.panel.WidgetsListAdapter.WidgetItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class WidgetsListDiffCallback extends Callback {

	public static final int PAYLOAD_EDIT_MODE_CHANGED = 0;
	public static final int PAYLOAD_MOVE_STATE_CHANGED = 1;
	public static final int PAYLOAD_DIVIDER_STATE_CHANGED = 2;
	public static final int PAYLOAD_UPDATE_ALL = 3;
	public static final int PAYLOAD_UPDATE_POSITION = 4;

	private final List<Object> oldItems;
	private final List<Object> newItems;
	private final boolean oldEditMode;
	private final boolean newEditMode;

	private final boolean updatePosition;

	WidgetsListDiffCallback(@NonNull List<Object> oldItems, @NonNull List<Object> newItems,
			boolean oldEditMode, boolean newEditMode, boolean updatePosition) {
		this.oldItems = new ArrayList<>(oldItems);
		this.newItems = newItems;
		this.oldEditMode = oldEditMode;
		this.newEditMode = newEditMode;
		this.updatePosition = updatePosition;
	}

	@Override
	public int getOldListSize() {
		return oldItems.size();
	}

	@Override
	public int getNewListSize() {
		return newItems.size();
	}

	@Override
	public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
		Object oldItem = oldItems.get(oldItemPosition);
		Object newItem = newItems.get(newItemPosition);

		if (oldItem instanceof Integer && newItem instanceof Integer) {
			return oldItem.equals(newItem);
		}

		if (oldItem instanceof PageItem && newItem instanceof PageItem) {
			return ((PageItem) oldItem).pageNumber == ((PageItem) newItem).pageNumber;
		}

		if (oldItem instanceof WidgetItem && newItem instanceof WidgetItem) {
			return Objects.equals(((WidgetItem) oldItem).mapWidgetInfo.key, ((WidgetItem) newItem).mapWidgetInfo.key);
		}

		return false;
	}

	@Override
	public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
		Object oldItem = oldItems.get(oldItemPosition);
		Object newItem = newItems.get(newItemPosition);

		if (oldEditMode != newEditMode) {
			return false;
		}

		if (updatePosition) {
			return false;
		}

		if (oldItem instanceof Integer && newItem instanceof Integer) {
			return oldItem.equals(newItem);
		}

		if (oldItem instanceof PageItem oldPage && newItem instanceof PageItem newPage) {
			return oldPage.equals(newPage);
		}

		if (oldItem instanceof WidgetItem oldWidget && newItem instanceof WidgetItem newWidget) {
			return oldWidget.equals(newWidget);
		}

		return false;
	}

	@Nullable
	@Override
	public Object getChangePayload(int oldItemPosition, int newItemPosition) {
		Object oldItem = oldItems.get(oldItemPosition);
		Object newItem = newItems.get(newItemPosition);

		List<Integer> payloads = new ArrayList<>();

		if (oldEditMode != newEditMode) {
			payloads.add(PAYLOAD_EDIT_MODE_CHANGED);
		}

		if (updatePosition) {
			payloads.add(PAYLOAD_UPDATE_POSITION);
		}

		if (oldItem instanceof PageItem oldPage && newItem instanceof PageItem newPage) {
			if (oldPage.pageNumber != newPage.pageNumber) {
				payloads.add(PAYLOAD_UPDATE_ALL);
			}
			if ((oldPage.movable != newPage.movable) || (!Objects.equals(oldPage.deleteMessage, newPage.deleteMessage))) {
				payloads.add(PAYLOAD_MOVE_STATE_CHANGED);
			}
		}

		if (oldItem instanceof WidgetItem oldWidget && newItem instanceof WidgetItem newWidget) {
			if (!Objects.equals(oldWidget.mapWidgetInfo, newWidget.mapWidgetInfo)) {
				payloads.add(PAYLOAD_UPDATE_ALL);
			}
			if ((oldWidget.showBottomDivider != newWidget.showBottomDivider)
					|| (oldWidget.showBottomShadow != newWidget.showBottomShadow)) {
				payloads.add(PAYLOAD_DIVIDER_STATE_CHANGED);
			}
		}

		return payloads.isEmpty() ? null : payloads;
	}
}
