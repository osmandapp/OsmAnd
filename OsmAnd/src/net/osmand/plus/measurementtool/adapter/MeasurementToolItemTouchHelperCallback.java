package net.osmand.plus.measurementtool.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;


public class MeasurementToolItemTouchHelperCallback extends ItemTouchHelper.Callback {

	private final ItemTouchHelperAdapter adapter;

	public MeasurementToolItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
		this.adapter = adapter;
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
		final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
		return makeMovementFlags(dragFlags, 0);
	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
		return adapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
	}

	@Override
	public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {

	}

	@Override
	public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		super.clearView(recyclerView, viewHolder);
		adapter.onItemDismiss(viewHolder);
	}

	interface ItemTouchHelperAdapter {

		boolean onItemMove(int from, int to);

		void onItemDismiss(RecyclerView.ViewHolder holder);
	}
}
