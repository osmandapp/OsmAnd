package net.osmand.plus.profiles;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

public class ProfilesItemTouchHelperCallback extends ItemTouchHelper.Callback {

	private ProfilesItemTouchHelperCallback.ItemTouchHelperAdapter adapter;

	public ProfilesItemTouchHelperCallback(ProfilesItemTouchHelperCallback.ItemTouchHelperAdapter adapter) {
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
	public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

	}

	@Override
	public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
		return makeMovementFlags(dragFlags, 0);
	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
		int from = source.getAdapterPosition();
		int to = target.getAdapterPosition();
		if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
			return false;
		}
		return adapter.onItemMove(from, to);
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