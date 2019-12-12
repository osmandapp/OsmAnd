package net.osmand.plus.quickaction;

import android.support.v7.widget.RecyclerView;

import net.osmand.plus.profiles.ReorderItemTouchHelperCallback;

/**
 * Created by okorsun on 21.12.16.
 */

public class QuickActionItemTouchHelperCallback extends ReorderItemTouchHelperCallback {

	QuickActionItemTouchHelperCallback(OnItemMoveCallback itemMoveCallback) {
		super(itemMoveCallback);
	}

	@Override
	public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		return !isaHeaderType(viewHolder) ? super.getMovementFlags(recyclerView, viewHolder) : 0;
	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
		if (isaHeaderType(viewHolder) || isaHeaderType(target)) {
			return false;
		}
		return super.onMove(recyclerView, viewHolder, target);
	}

	private boolean isaHeaderType(RecyclerView.ViewHolder viewHolder) {
		return viewHolder.getItemViewType() == QuickActionListFragment.QuickActionAdapter.SCREEN_HEADER_TYPE;
	}
}
