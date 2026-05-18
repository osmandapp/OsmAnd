package net.osmand.plus.views.controls;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;


public class ReorderItemTouchHelperCallback extends ItemTouchHelper.Callback {

	private final OnItemMoveCallback moveCallback;

	public ReorderItemTouchHelperCallback(@NonNull OnItemMoveCallback moveCallback) {
		this.moveCallback = moveCallback;
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
	public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
		if (isMovingDisabled(viewHolder)) {
			return 0;
		}
		int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
		int swipeFlags = 0;
		return makeMovementFlags(dragFlags, swipeFlags);
	}

	@Override
	public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull ViewHolder source, @NonNull ViewHolder target) {
		int from = source.getAdapterPosition();
		int to = target.getAdapterPosition();
		if (from == NO_POSITION || to == NO_POSITION || isMovingDisabled(source) || isMovingDisabled(target)) {
			return false;
		}
		return moveCallback.onItemMove(from, to);
	}

	private boolean isMovingDisabled(@NonNull ViewHolder viewHolder) {
		return viewHolder instanceof UnmovableItem && ((UnmovableItem) viewHolder).isMovingDisabled();
	}

	@Override
	public void onSwiped(@NonNull ViewHolder viewHolder, int direction) {

	}

	@Override
	public void clearView(@NonNull RecyclerView recyclerView, @NonNull ViewHolder viewHolder) {
		super.clearView(recyclerView, viewHolder);
		moveCallback.onItemDismiss(viewHolder);
	}

	public interface OnItemMoveCallback {

		boolean onItemMove(int from, int to);

		void onItemDismiss(@NonNull ViewHolder holder);
	}

	public interface UnmovableItem {

		boolean isMovingDisabled();
	}
}