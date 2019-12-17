package net.osmand.plus.profiles;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;


public class ReorderItemTouchHelperCallback extends ItemTouchHelper.Callback {

	private OnItemMoveCallback itemMoveCallback;

	public ReorderItemTouchHelperCallback(OnItemMoveCallback itemMoveCallback) {
		this.itemMoveCallback = itemMoveCallback;
	}

	@Override
	public boolean isLongPressDragEnabled() {
		return false;
	}

	@Override
	public boolean isItemViewSwipeEnabled() {
		return false;
	}

	@Override
	public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		if (isMovingDisabled(viewHolder)) {
			return 0;
		}
		int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
		int swipeFlags = 0;
		return makeMovementFlags(dragFlags, swipeFlags);
	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
		int from = source.getAdapterPosition();
		int to = target.getAdapterPosition();
		if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION
				|| isMovingDisabled(source) || isMovingDisabled(target)) {
			return false;
		}
		return itemMoveCallback.onItemMove(from, to);
	}

	private boolean isMovingDisabled(RecyclerView.ViewHolder viewHolder) {
		return viewHolder instanceof UnmovableItem && ((UnmovableItem) viewHolder).isMovingDisabled();
	}

	@Override
	public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

	}

	@Override
	public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		super.clearView(recyclerView, viewHolder);
		itemMoveCallback.onItemDismiss(viewHolder);
	}

	public interface OnItemMoveCallback {

		boolean onItemMove(int from, int to);

		void onItemDismiss(RecyclerView.ViewHolder holder);
	}

	public interface UnmovableItem {

		boolean isMovingDisabled();
	}
}